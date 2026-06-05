"""Generate the experiment grid of .ini files and a run.sh driver.

For each cell of the grid this writes one .ini file (consumed by main.py)
and adds a line to ``run.sh`` that runs main.py against it. The grid is the
Cartesian product of:

* MAX_IMAGE values in IMAGE_SIZES
* the two FORGIVE_VALUE shapes:
    - a flat single-value list (FORGIVE_EXPONENTS = False),
    - the list of exponents (FORGIVE_EXPONENTS = True, expanded by main.py),
* noise levels in NOISE_LEVELS (used for both action and perception noise),
* the six combinations of (action, assessment, AJD) forgiveness flags.

The "no forgiveness at all" combination is a baseline: forgiveness values
are irrelevant, so we force the simplest representation (a single zero) to
keep the matrix small and avoid spurious strategy axes.
"""

# ---------------------------------------------------------------------------
# Experiment grid
# ---------------------------------------------------------------------------

IMAGE_SIZES = [3, 5, 7, 9]
FLAT_FORGIVE_VALUES = [0.05]
EXPONENTIAL_FORGIVE_VALUES = [0.001, 0.5, 1, 1.355, 1.67]
NOISE_LEVELS = [0, 0.05, 0.1, 0.15, 0.2]

# (action_forgiveness, assessment_forgiveness, ajd_forgiveness) combinations.
FORGIVENESS_COMBOS = [
    (False, True,  False),   # assessment only
    (True,  False, False),   # action only
    (False, False, True),    # AJD only
    (True,  True,  False),   # action + assessment
    (True,  False, True),    # action + AJD
    (False, False, False),   # baseline: no forgiveness
]

SCRIPT_FILE = "run.sh"

INI_TEMPLATE = """
[DEFAULT]
REMOTE = True
ASSESSMENT_FORGIVENESS = {assessment}
ACTION_FORGIVENESS = {action}
AJD_FORGIVENESS = {ajd}
MAX_IMAGE = {max_image}
MIN_IMAGE = 0
OBS_PROB = 0.8
FORGIVE_EXPONENTS = {forgive_exponents}
FORGIVE_VALUE = {forgive_value}
REPUTATION_NOISE = 0.0
ACTION_NOISE = {noise}
PERCEPTION_NOISE = {noise}
DONATE_UTILITY = -0.1
RECEIVE_UTILITY = 1
BETA = 1000
NUM_AGENTS = 8
BURN_IN = 600
MIN_SIM_ITERS = 2000
MAX_SIM_ITERS = 5000
NUM_SIM_RUNS = 200

[SYSTEM]
NUM_CPUS = 14
LOG_LEVEL = INFO
"""


def _filename(max_image, forgive_exponents, forgive_values,
              action, assessment, ajd, noise):
    """Stable, human-readable filename for one experiment cell.

    Matches the parsing pattern in make_graphs.ipynb, so renaming any of
    these tokens requires updating the notebook as well.
    """
    forgive_value_token = "_".join(map(str, forgive_values))
    return (
        f"max_image_{max_image}"
        f"_forgive_exp_{forgive_exponents}"
        f"_forgive_value_{forgive_value_token}"
        f"_action_{action}"
        f"_assessment_{assessment}"
        f"_ajd_{ajd}"
        f"_noise_{noise}"
    )


def main():
    # Order-preserving dedup: the no-forgiveness case collapses across the
    # FORGIVE_EXPONENTS axis, so the same filename would otherwise be added
    # twice (and the experiment run twice from run.sh).
    ini_file_names = []
    seen = set()

    for max_image in IMAGE_SIZES:
        for forgive_exponents in (False, True):
            for noise in NOISE_LEVELS:
                for action, assessment, ajd in FORGIVENESS_COMBOS:
                    # No forgiveness at all: collapse the forgiveness axis so
                    # the strategy space stays as small as possible.
                    if not (action or assessment or ajd):
                        effective_forgive_exponents = False
                        forgive_values = [0.0]
                    else:
                        effective_forgive_exponents = forgive_exponents
                        forgive_values = (EXPONENTIAL_FORGIVE_VALUES
                                          if forgive_exponents
                                          else FLAT_FORGIVE_VALUES)

                    ini_content = INI_TEMPLATE.format(
                        assessment=assessment,
                        action=action,
                        ajd=ajd,
                        max_image=max_image,
                        forgive_exponents=effective_forgive_exponents,
                        forgive_value=forgive_values,
                        noise=noise,
                    )

                    filename = _filename(
                        max_image=max_image,
                        forgive_exponents=effective_forgive_exponents,
                        forgive_values=forgive_values,
                        action=action,
                        assessment=assessment,
                        ajd=ajd,
                        noise=noise,
                    )

                    if filename in seen:
                        continue
                    seen.add(filename)

                    with open(filename + ".ini", "w") as f:
                        f.write(ini_content)
                    ini_file_names.append(filename)

    # Write the driver script. ``RAY_SCHEDULER_EVENTS=0`` silences a noisy
    # Ray warning that shows up on some platforms.
    with open(SCRIPT_FILE, "w") as script:
        script.write("#!/bin/bash\n")
        script.write("export RAY_SCHEDULER_EVENTS=0\n")
        for ini_file in ini_file_names:
            script.write(
                f"python3 main.py -c {ini_file}.ini -e {ini_file}.png -v "
                f"> {ini_file}.txt\n"
            )

    print(f"Wrote {len(ini_file_names)} .ini files and {SCRIPT_FILE}.")


if __name__ == "__main__":
    main()
