"""Simulator for forgiveness in indirect reciprocity (image scoring) games.

Three flavours of forgiveness are supported and can be enabled independently
via the configuration file:

* Assessment forgiveness (g_1 in the paper) — the likelihood that an observer
  does *not* lower a donor's image after seeing them defect.
* Action forgiveness (g_2 in the paper) — the likelihood that a donor donates
  even though its donation strategy says it should not.
* AJD ("Action-Justified Defection") forgiveness — a variant of assessment
  forgiveness that only forgives a donor's defection if the observer would
  itself have defected against the same recipient.

For each pair of (donation_strategy, forgiveness_strategy) the script samples
the stationary image-score and utility distributions in a finite population
of NUM_AGENTS, then computes the Fermi-learning fixation probabilities for
every pair of strategies. From those we derive the cooperation index and a
heatmap of strategy abundance.

Run with::

    python main.py -c config.ini -e heatmap_out.png [-v]

A worked configuration template can be produced by ``make_ini_files.py``.
"""

import argparse
import ast
import configparser
import logging
import math
import random
import sys

import matplotlib.pyplot as plt
import numpy as np
import ray
from deeptime.markov.msm import MarkovStateModel

# ---------------------------------------------------------------------------
# Default simulation parameters.
# Each of these may be overridden via the [DEFAULT] section of the config file.
# ---------------------------------------------------------------------------

REMOTE = True                    # use Ray for distributed execution
ASSESSMENT_FORGIVENESS = True    # g_1 in the paper
ACTION_FORGIVENESS = True        # g_2 in the paper
AJD_FORGIVENESS = False          # action-justified-defection variant

MAX_IMAGE = 4                    # max image score; strategies range 0..MAX_IMAGE+1
                                 # (the extra value represents "never donate").
MIN_IMAGE = 0

OBS_PROB = 1                     # probability of observing a given interaction

# Forgiveness values. If a single-element list it is interpreted as a flat
# generosity probability. Otherwise it should be an NxM matrix where
# N = number of forgiveness strategies and M = number of image scores; the
# entry FORGIVE_VALUE[strategy][image] is the forgiveness likelihood.
# See ``print_forgiveness_values`` for an exponential parametrisation, and
# the FORGIVE_EXPONENTS config flag for auto-expansion.
FORGIVE_VALUE = [0.05]

REPUTATION_NOISE = 0.025         # P(observer reads a random image of an agent)
ACTION_NOISE = 0.025             # P(intended donation becomes a non-donation)
PERCEPTION_NOISE = 0.025         # P(observer mis-perceives a donation event)

DONATE_UTILITY = -0.1            # cost paid by the donor
RECEIVE_UTILITY = 1              # benefit received by the recipient

BETA = 10                        # selection strength for Fermi learning

NUM_AGENTS = 20

# Sampling controls. We run NUM_SIM_RUNS simulations and sample once per run
# (after BURN_IN steps, at a random step in [MIN_SIM_ITERS, MAX_SIM_ITERS]).
BURN_IN = 1000
MIN_SIM_ITERS = 2000
MAX_SIM_ITERS = 5000
NUM_SIM_RUNS = 2000


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def print_forgiveness_values(forgive_exponents):
    """Print a forgiveness matrix derived from a list of exponents.

    For each exponent ``e`` and image score ``i`` the entry is
    ``exp((i - MAX_IMAGE) / e)`` — a smooth ramp that approaches 1 as the
    image score approaches the maximum. The output is formatted so it can
    be copy-pasted as the value of ``FORGIVE_VALUE``.
    """
    fv = {}
    for i in range(MIN_IMAGE, MAX_IMAGE + 1):
        for j in range(len(forgive_exponents)):
            fv[(j, i)] = math.exp((i - MAX_IMAGE) / forgive_exponents[j])
    print("FORGIVE_VALUE = [")
    for j in range(len(forgive_exponents)):
        row = [fv[(j, i)] for i in range(MIN_IMAGE, MAX_IMAGE + 1)]
        print("    " + str(row) + ",")
    print("]")


def powermethod(m):
    """Return the stationary distribution of the row-stochastic matrix ``m``."""
    return MarkovStateModel(m).stationary_distribution


# ---------------------------------------------------------------------------
# Agent
# ---------------------------------------------------------------------------

class Agent:
    """One simulated agent.

    Each agent holds a donation strategy (an image threshold; donate iff the
    perceived image of the recipient is >= threshold), a forgiveness strategy
    index into ``FORGIVE_VALUE``, and a private map of image scores it has
    assigned to other agents.
    """

    def __init__(self, donate_strategy, forgive_strategy, **kwargs):
        assert MIN_IMAGE <= donate_strategy <= MAX_IMAGE + 1, (
            "donate_strategy must lie in [MIN_IMAGE, MAX_IMAGE+1]")
        self.donate_strategy = donate_strategy
        self.forgive_strategy = forgive_strategy
        self.images = {}          # other agent -> image score
        self.utility = 0
        self.num_interactions = 0

    def forgive_likelihood(self, image):
        """Forgiveness probability used in this agent's strategy.

        A single-value ``FORGIVE_VALUE`` is treated as a flat generosity
        probability; otherwise we look up the per-(strategy, image) entry.
        """
        if len(FORGIVE_VALUE) == 1:
            return FORGIVE_VALUE[0]
        return FORGIVE_VALUE[self.forgive_strategy][image]

    def get_image(self, agent):
        """Return the stored image of ``agent``, or a uniform random score if
        we have never interacted with them."""
        return self.images.get(agent, random.randint(MIN_IMAGE, MAX_IMAGE))

    def change_image(self, agent, amount):
        """Adjust ``agent``'s image score, clipped to [MIN_IMAGE, MAX_IMAGE]."""
        i = self.get_image(agent) + amount
        i = max(MIN_IMAGE, min(MAX_IMAGE, i))
        self.images[agent] = i

    def do_donate(self, agent):
        """Attempt a donation toward ``agent``. Returns True iff it happened.

        Both agents' interaction counts are incremented unconditionally;
        utility is updated only on a successful donation.
        """
        self.num_interactions += 1
        agent.num_interactions += 1

        image = self.get_image(agent)
        if random.random() < REPUTATION_NOISE:
            image = random.randint(MIN_IMAGE, MAX_IMAGE)

        # Donate if the strategy says so, or if action forgiveness kicks in;
        # then suppress with probability ACTION_NOISE.
        wants_to_donate = (
            self.donate_strategy <= image
            or (ACTION_FORGIVENESS and random.random() < self.forgive_likelihood(image))
        )
        if wants_to_donate and random.random() > ACTION_NOISE:
            self.utility += DONATE_UTILITY
            agent.utility += RECEIVE_UTILITY
            return True
        return False

    def try_observe_interaction(self, donor, recipient, donation):
        """Update this agent's image of ``donor`` based on an observed event.

        ``donation`` is True if ``donor`` actually donated to ``recipient``.
        Self-observation always succeeds; otherwise the observation may be
        dropped (OBS_PROB), mis-perceived (PERCEPTION_NOISE), or filtered
        through assessment / AJD forgiveness.
        """
        if donor == self or recipient == self:
            # Self-observation is noise-free and deterministic.
            self.change_image(donor, 1 if donation else -1)
            return

        if random.random() > OBS_PROB:
            return  # observation missed

        if random.random() < PERCEPTION_NOISE:
            donation = not donation

        donor_image = self.get_image(donor)
        if random.random() < REPUTATION_NOISE:
            donor_image = random.randint(MIN_IMAGE, MAX_IMAGE)

        if donation:
            self.change_image(donor, 1)
            return

        # Defection observed: maybe forgive.
        if ASSESSMENT_FORGIVENESS and random.random() < self.forgive_likelihood(donor_image):
            # Note: forgiveness suppresses the *decrement*; image is not
            # incremented in this case.
            return

        if AJD_FORGIVENESS:
            # AJD: forgive iff we would also have defected against the
            # recipient given our perception of their image. We use the
            # recipient's own self-view rather than re-sampling here, to
            # match the original (Nathan's) implementation; reputation
            # noise is intentionally not re-applied.
            recipient_image = recipient.get_image(recipient)
            if (self.donate_strategy > recipient_image
                    and random.random() < self.forgive_likelihood(donor_image)):
                return

        self.change_image(donor, -1)


# ---------------------------------------------------------------------------
# Environment
# ---------------------------------------------------------------------------

class Environment:
    """A population of agents split between two strategies.

    ``strat1`` and ``strat2`` are (donate_strategy, forgive_strategy) tuples.
    The first ``num_strat1`` agents follow ``strat1``; the rest follow
    ``strat2``.
    """

    def __init__(self, strat1, strat2, num_strat1, num_strat2):
        self.agents = []
        self.num_strat1 = num_strat1
        self.num_strat2 = num_strat2
        self.strat1 = strat1
        self.strat2 = strat2

        for _ in range(num_strat1):
            self.agents.append(Agent(donate_strategy=strat1[0],
                                     forgive_strategy=strat1[1]))
        for _ in range(num_strat2):
            self.agents.append(Agent(donate_strategy=strat2[0],
                                     forgive_strategy=strat2[1]))

    def interact(self):
        """Run one interaction: pick two agents at random, attempt a donation,
        then let every agent (including donor and recipient) try to observe
        the outcome."""
        donor, recipient = random.sample(self.agents, 2)
        donation = donor.do_donate(recipient)
        for agent in self.agents:
            agent.try_observe_interaction(donor, recipient, donation)

    def reputation_likelihood_sampling(self, burn_in, min_iters, max_iters, num_times):
        """Sample image and utility distributions over ``num_times`` runs.

        Each run: clear images, run ``burn_in`` warm-up interactions, reset
        utility and interaction counters, then run a random number of
        further interactions in [min_iters, max_iters] before sampling.

        Returns a tuple ``(strat1_images, strat2_images, strat1_utility,
        strat2_utility)``, where the image arrays are normalised so they sum
        (approximately) to one over image-score bins.
        """
        strat1_images = np.zeros(MAX_IMAGE - MIN_IMAGE + 1)
        strat2_images = np.zeros(MAX_IMAGE - MIN_IMAGE + 1)
        strat1_utility = 0
        strat2_utility = 0

        for _ in range(num_times):
            for a in self.agents:
                a.images = {}
            for _ in range(burn_in):
                self.interact()
            for a in self.agents:
                a.utility = 0
                a.num_interactions = 0

            for _ in range(random.randint(min_iters, max_iters)):
                self.interact()

            current_util1 = 0
            current_util2 = 0
            for a in self.agents[:self.num_strat1]:
                current_util1 += a.utility / a.num_interactions if a.num_interactions > 0 else 0
                for b in self.agents:
                    # Image score distribution for strategy-1 agents from every
                    # observer's perspective (self-observation included).
                    strat1_images[b.images[a]] += 1
            for a in self.agents[self.num_strat1:]:
                current_util2 += a.utility / a.num_interactions if a.num_interactions > 0 else 0
                for b in self.agents:
                    strat2_images[b.images[a]] += 1

            if self.num_strat1 > 0:
                strat1_utility += current_util1 / self.num_strat1
            if self.num_strat2 > 0:
                strat2_utility += current_util2 / self.num_strat2

        if self.num_strat1 > 0:
            strat1_images /= (num_times * len(self.agents) * self.num_strat1)
        if self.num_strat2 > 0:
            strat2_images /= (num_times * len(self.agents) * self.num_strat2)

        strat1_utility /= num_times
        strat2_utility /= num_times

        return strat1_images, strat2_images, strat1_utility, strat2_utility


@ray.remote(num_cpus=1)
class EnvironmentRemote(Environment):
    """Ray-actor wrapper around ``Environment`` for distributed sampling."""
    pass


# ---------------------------------------------------------------------------
# Analytical helpers
# ---------------------------------------------------------------------------

def likelihood_donate(donor_strategy, recipient_image):
    """Probability that a donor with ``donor_strategy`` donates to a recipient
    whose true image is ``recipient_image``.

    Combines REPUTATION_NOISE (a chance of reading a uniformly random image,
    over which we average), action forgiveness, and ACTION_NOISE. Called
    once per (strategy, image) pair at the end of a run, so it is not
    cached.
    """
    p_d = 0  # P(donate | observe a uniform-random image)
    p_a = 0  # P(donate | observe the recipient's true image)

    if ACTION_FORGIVENESS:
        if REPUTATION_NOISE > 0.0:
            for i in range(MIN_IMAGE, MAX_IMAGE + 1):
                if i < donor_strategy[0]:
                    if len(FORGIVE_VALUE) == 1:
                        p_d += FORGIVE_VALUE[0]
                    else:
                        p_d += FORGIVE_VALUE[donor_strategy[1]][i]
                else:
                    p_d += 1
            p_d /= (MAX_IMAGE - MIN_IMAGE + 1)

        if recipient_image < donor_strategy[0]:
            if len(FORGIVE_VALUE) == 1:
                p_a = FORGIVE_VALUE[0]
            else:
                p_a = FORGIVE_VALUE[donor_strategy[1]][recipient_image]
        else:
            p_a = 1

        return (1 - ACTION_NOISE) * (REPUTATION_NOISE * (p_d - p_a) + p_a)

    # No action forgiveness: donation is purely image-threshold driven.
    if REPUTATION_NOISE > 0.0:
        for i in range(MIN_IMAGE, MAX_IMAGE + 1):
            if i <= donor_strategy[0]:
                p_d += 1
        p_d /= (MAX_IMAGE - MIN_IMAGE + 1)

    if recipient_image >= donor_strategy[0]:
        p_a = 1

    return (1 - ACTION_NOISE) * (REPUTATION_NOISE * (p_d - p_a) + p_a)


def compute_fixedpoints():
    """Sample fixed points for every strategy-pair / population-split.

    Returns a dict keyed by
    ``(strat1, strat2, forgiveness1, forgiveness2, num_s1, num_s2)`` mapping
    to ``(strat1_images, strat2_images, strat1_utility, strat2_utility)``.

    By symmetry we only sample the upper triangle ``strat1 <= strat2`` and
    fill in the swapped key directly. When the two strategies are identical
    we only sample the all-strat1 population.
    """
    ret = {}
    futures = {}  # Used uniformly for both Ray and local paths.

    for strat1 in range(MIN_IMAGE, MAX_IMAGE + 2):
        for strat2 in range(strat1, MAX_IMAGE + 2):
            for forgiveness1 in range(len(FORGIVE_VALUE)):
                for forgiveness2 in range(len(FORGIVE_VALUE)):
                    if strat1 == strat2 and forgiveness1 == forgiveness2:
                        # Monomorphic population: only one split is meaningful.
                        num_s1, num_s2 = NUM_AGENTS, 0
                        env = (EnvironmentRemote.remote((strat1, forgiveness1),
                                                        (strat2, forgiveness2),
                                                        num_s1, num_s2)
                               if REMOTE
                               else Environment((strat1, forgiveness1),
                                                (strat2, forgiveness2),
                                                num_s1, num_s2))
                        key = (strat1, strat2, forgiveness1, forgiveness2,
                               num_s1, num_s2)
                        futures[key] = (env.reputation_likelihood_sampling.remote(
                            BURN_IN, MIN_SIM_ITERS, MAX_SIM_ITERS, NUM_SIM_RUNS)
                            if REMOTE
                            else env.reputation_likelihood_sampling(
                                BURN_IN, MIN_SIM_ITERS, MAX_SIM_ITERS, NUM_SIM_RUNS))
                    else:
                        for num_s1 in range(1, NUM_AGENTS):
                            num_s2 = NUM_AGENTS - num_s1
                            env = (EnvironmentRemote.remote((strat1, forgiveness1),
                                                            (strat2, forgiveness2),
                                                            num_s1, num_s2)
                                   if REMOTE
                                   else Environment((strat1, forgiveness1),
                                                    (strat2, forgiveness2),
                                                    num_s1, num_s2))
                            key = (strat1, strat2, forgiveness1, forgiveness2,
                                   num_s1, num_s2)
                            futures[key] = (env.reputation_likelihood_sampling.remote(
                                BURN_IN, MIN_SIM_ITERS, MAX_SIM_ITERS, NUM_SIM_RUNS)
                                if REMOTE
                                else env.reputation_likelihood_sampling(
                                    BURN_IN, MIN_SIM_ITERS, MAX_SIM_ITERS, NUM_SIM_RUNS))

    remaining = len(futures)
    logger.debug("Total number of futures: %d", remaining)

    for strat1 in range(MIN_IMAGE, MAX_IMAGE + 2):
        for strat2 in range(strat1, MAX_IMAGE + 2):
            for forgiveness1 in range(len(FORGIVE_VALUE)):
                for forgiveness2 in range(len(FORGIVE_VALUE)):
                    if strat1 == strat2 and forgiveness1 == forgiveness2:
                        num_s1, num_s2 = NUM_AGENTS, 0
                        key = (strat1, strat2, forgiveness1, forgiveness2,
                               num_s1, num_s2)
                        result = ray.get(futures[key]) if REMOTE else futures[key]
                        ret[key] = result
                        ret[(strat2, strat1, forgiveness2, forgiveness1, num_s2, num_s1)] = (
                            result[1], result[0], result[3], result[2])
                        remaining -= 1
                        logger.debug("Remaining futures: %d", remaining)
                    else:
                        for num_s1 in range(1, NUM_AGENTS):
                            num_s2 = NUM_AGENTS - num_s1
                            key = (strat1, strat2, forgiveness1, forgiveness2,
                                   num_s1, num_s2)
                            result = ray.get(futures[key]) if REMOTE else futures[key]
                            ret[key] = result
                            ret[(strat2, strat1, forgiveness2, forgiveness1,
                                 num_s2, num_s1)] = (
                                result[1], result[0], result[3], result[2])
                            remaining -= 1
                            logger.debug("Remaining futures: %d", remaining)

    return ret


# ---------------------------------------------------------------------------
# Fixation matrix and downstream metrics
# ---------------------------------------------------------------------------

def make_strategy_index_map():
    """Map each (donate_strategy, forgive_strategy) pair to a unique index."""
    strategy_index_map = {}
    index = 0
    for donate_strategy in range(MIN_IMAGE, MAX_IMAGE + 2):
        for forgive_strategy in range(len(FORGIVE_VALUE)):
            strategy_index_map[(donate_strategy, forgive_strategy)] = index
            index += 1
    return strategy_index_map


def fixation_matrix():
    """Compute the Fermi-learning fixation matrix.

    Entry (i, j) is the probability that strategy ``i`` invades a population
    of strategy ``j``. Diagonal entries are filled so each row sums to 1.

    Returns the matrix and the underlying fixed-point dict.
    """
    strategy_index_map = make_strategy_index_map()
    fixed_points = compute_fixedpoints()
    logger.debug("%d fixed points computed.", len(fixed_points))
    n = len(strategy_index_map)
    matrix = np.zeros((n, n))

    for strat1 in range(MIN_IMAGE, MAX_IMAGE + 2):
        for strat2 in range(MIN_IMAGE, MAX_IMAGE + 2):
            for forgiveness1 in range(len(FORGIVE_VALUE)):
                for forgiveness2 in range(len(FORGIVE_VALUE)):
                    s1 = (strat1, forgiveness1)
                    s2 = (strat2, forgiveness2)

                    if strat1 == strat2 and forgiveness1 == forgiveness2:
                        matrix[strategy_index_map[s1], strategy_index_map[s2]] = 0.0
                        continue

                    s = 0
                    for i in range(1, NUM_AGENTS):
                        prob = 1
                        for j in range(1, i + 1):
                            u1, u2 = fixed_points[
                                (strat1, strat2, forgiveness1, forgiveness2,
                                 j, NUM_AGENTS - j)][2:4]
                            # Equivalent to (1+exp(BETA*(u2-u1))) / (1+exp(BETA*(u1-u2)))
                            # but faster.
                            prob *= math.exp(-BETA * (u1 - u2))
                        s += prob
                    matrix[strategy_index_map[s1], strategy_index_map[s2]] = 1 / (1 + s)

    matrix /= n  # normalise
    # Fill the diagonal so each row sums to 1.
    for i in range(n):
        matrix[i, i] = 1 - np.sum(matrix[i, :]) - matrix[i, i]

    return matrix, fixed_points


def cooperation_index(fixation_matrix, fixed_points, strategy_index_map,
                      print_images=False):
    """Expected donation rate at the fixation-matrix stationary distribution."""
    index = 0
    pm = powermethod(fixation_matrix)

    for s in range(MIN_IMAGE, MAX_IMAGE + 2):
        for f in range(len(FORGIVE_VALUE)):
            idx = strategy_index_map[(s, f)]
            image = fixed_points[(s, s, f, f, NUM_AGENTS, 0)][0]
            if print_images:
                image_p = [float(round(i, 3)) for i in image]
                print(f"Image distribution for strategy ({s}, {f}): {image_p}")

            for i in range(len(image)):
                index += likelihood_donate((s, f), i) * pm[idx] * image[i]
    return index


def compute_heatmap(fixation_matrix, strategy_index_map):
    """Return a 2-D array of strategy abundances under the stationary
    distribution of the fixation matrix.

    Rows index the donation strategy; columns index the forgiveness strategy.
    """
    pm = powermethod(fixation_matrix)
    heatmap_array = np.zeros((MAX_IMAGE - MIN_IMAGE + 2, len(FORGIVE_VALUE)))
    for i in range(MIN_IMAGE, MAX_IMAGE + 2):
        for j in range(len(FORGIVE_VALUE)):
            heatmap_array[i - MIN_IMAGE, j] = pm[strategy_index_map[(i, j)]]
    return heatmap_array


def plot_heatmap(heatmap_array, filename):
    """Render and save the strategy-abundance heatmap to ``filename``."""
    plt.rcParams.update({'font.size': 12})
    plt.imshow(heatmap_array, cmap='hot', interpolation='nearest')
    plt.colorbar()
    plt.xticks(range(len(FORGIVE_VALUE)),
               [f"{i}" for i in range(len(FORGIVE_VALUE))])
    plt.yticks(range(MIN_IMAGE, MAX_IMAGE + 2),
               [f"{i}" for i in range(MIN_IMAGE, MAX_IMAGE + 2)])
    plt.xlabel("Forgiveness Strategy")
    plt.ylabel("Donation Strategy")
    plt.savefig(filename)
    plt.close()
    return heatmap_array


# ---------------------------------------------------------------------------
# CLI / entry point
# ---------------------------------------------------------------------------

def _parse_args():
    parser = argparse.ArgumentParser(description="AJD Forgiveness Simulation")
    parser.add_argument('-c', '--config', type=str, required=True,
                        help='Path to the configuration file')
    parser.add_argument('-e', '--heatmap', type=str, required=True,
                        help='Filename for the heatmap output (PNG)')
    parser.add_argument('-v', '--verbose', action='store_true',
                        help='Enable verbose logging and turn on every print flag')
    parser.add_argument('-f', '--fixation_matrix', action='store_true',
                        help='Print the fixation matrix to the console')
    parser.add_argument('-i', '--cooperation_index', action='store_true',
                        help='Print the cooperation index to the console')
    parser.add_argument('-p', '--print_heatmap', action='store_true',
                        help='Print the heatmap to the console')
    parser.add_argument('-m', '--print_images', action='store_true',
                        help='Print the per-strategy image distributions')
    args = parser.parse_args()
    if args.verbose:
        # In verbose mode every print toggle defaults on.
        args.fixation_matrix = True
        args.cooperation_index = True
        args.print_heatmap = True
        args.print_images = True
    return args


def _apply_config(config):
    """Overlay values from a parsed ConfigParser onto this module's globals."""
    global REMOTE, ASSESSMENT_FORGIVENESS, ACTION_FORGIVENESS, AJD_FORGIVENESS
    global MAX_IMAGE, MIN_IMAGE, OBS_PROB, FORGIVE_VALUE
    global REPUTATION_NOISE, ACTION_NOISE, PERCEPTION_NOISE
    global DONATE_UTILITY, RECEIVE_UTILITY, BETA, NUM_AGENTS
    global BURN_IN, MIN_SIM_ITERS, MAX_SIM_ITERS, NUM_SIM_RUNS

    if 'DEFAULT' not in config:
        return

    REMOTE = config.getboolean('DEFAULT', 'REMOTE', fallback=REMOTE)
    ASSESSMENT_FORGIVENESS = config.getboolean(
        'DEFAULT', 'ASSESSMENT_FORGIVENESS', fallback=ASSESSMENT_FORGIVENESS)
    ACTION_FORGIVENESS = config.getboolean(
        'DEFAULT', 'ACTION_FORGIVENESS', fallback=ACTION_FORGIVENESS)
    AJD_FORGIVENESS = config.getboolean(
        'DEFAULT', 'AJD_FORGIVENESS', fallback=AJD_FORGIVENESS)
    MAX_IMAGE = config.getint('DEFAULT', 'MAX_IMAGE', fallback=MAX_IMAGE)
    MIN_IMAGE = config.getint('DEFAULT', 'MIN_IMAGE', fallback=MIN_IMAGE)
    OBS_PROB = config.getfloat('DEFAULT', 'OBS_PROB', fallback=OBS_PROB)

    fv_raw = config.get('DEFAULT', 'FORGIVE_VALUE', fallback=str(FORGIVE_VALUE))
    FORGIVE_VALUE = ast.literal_eval(fv_raw) if isinstance(fv_raw, str) else fv_raw

    # If FORGIVE_EXPONENTS is set, treat FORGIVE_VALUE as a list of exponents
    # and expand it into the per-(strategy, image) matrix used at runtime.
    if config.getboolean('DEFAULT', 'FORGIVE_EXPONENTS', fallback=False):
        expanded = []
        for j in range(len(FORGIVE_VALUE)):
            row = [math.exp((i - MAX_IMAGE) / FORGIVE_VALUE[j])
                   for i in range(MIN_IMAGE, MAX_IMAGE + 1)]
            expanded.append(row)
        FORGIVE_VALUE = expanded

    REPUTATION_NOISE = config.getfloat('DEFAULT', 'REPUTATION_NOISE',
                                       fallback=REPUTATION_NOISE)
    ACTION_NOISE = config.getfloat('DEFAULT', 'ACTION_NOISE',
                                   fallback=ACTION_NOISE)
    PERCEPTION_NOISE = config.getfloat('DEFAULT', 'PERCEPTION_NOISE',
                                       fallback=PERCEPTION_NOISE)
    DONATE_UTILITY = config.getfloat('DEFAULT', 'DONATE_UTILITY',
                                     fallback=DONATE_UTILITY)
    RECEIVE_UTILITY = config.getfloat('DEFAULT', 'RECEIVE_UTILITY',
                                      fallback=RECEIVE_UTILITY)
    BETA = config.getfloat('DEFAULT', 'BETA', fallback=BETA)
    NUM_AGENTS = config.getint('DEFAULT', 'NUM_AGENTS', fallback=NUM_AGENTS)
    BURN_IN = config.getint('DEFAULT', 'BURN_IN', fallback=BURN_IN)
    MIN_SIM_ITERS = config.getint('DEFAULT', 'MIN_SIM_ITERS', fallback=MIN_SIM_ITERS)
    MAX_SIM_ITERS = config.getint('DEFAULT', 'MAX_SIM_ITERS', fallback=MAX_SIM_ITERS)
    NUM_SIM_RUNS = config.getint('DEFAULT', 'NUM_SIM_RUNS', fallback=NUM_SIM_RUNS)


def _print_config():
    print("Configuration values:")
    for name in ("ASSESSMENT_FORGIVENESS", "ACTION_FORGIVENESS",
                 "AJD_FORGIVENESS", "MAX_IMAGE", "MIN_IMAGE", "OBS_PROB",
                 "FORGIVE_VALUE", "REPUTATION_NOISE", "ACTION_NOISE",
                 "PERCEPTION_NOISE", "DONATE_UTILITY", "RECEIVE_UTILITY",
                 "BETA", "NUM_AGENTS", "BURN_IN", "MIN_SIM_ITERS",
                 "MAX_SIM_ITERS", "NUM_SIM_RUNS"):
        print(f"{name}: {globals()[name]}")
    print()


args = _parse_args()

config = configparser.ConfigParser()
config.read(args.config)
_apply_config(config)

if REMOTE:
    ray.init(num_cpus=config.getint('SYSTEM', 'NUM_CPUS', fallback=8),
             logging_level=logging.FATAL)

logger = logging.getLogger(__name__)
logging.basicConfig(level=config.get('SYSTEM', 'LOG_LEVEL', fallback='INFO').upper())

if args.verbose:
    _print_config()

# ---------------- Run the simulation ----------------
matrix, fixed_points = fixation_matrix()
strategy_index_map = make_strategy_index_map()
coop_index = cooperation_index(matrix, fixed_points, strategy_index_map,
                               print_images=args.print_images)

if args.fixation_matrix:
    print("Fixation Matrix:")
    matrix_p = np.round(matrix, 3)
    opt = np.get_printoptions()
    np.set_printoptions(threshold=sys.maxsize)
    print(matrix_p)
    np.set_printoptions(**opt)
    print()

if args.cooperation_index:
    print(f"Cooperation index: {coop_index}")
    print()

heatmap_array = compute_heatmap(matrix, strategy_index_map)
if args.print_heatmap:
    print("Heatmap Array:")
    for i in range(heatmap_array.shape[0]):
        for j in range(heatmap_array.shape[1]):
            print(f"{heatmap_array[i,j]:.4f}", end=' ')
        print()

plot_heatmap(heatmap_array, args.heatmap)
