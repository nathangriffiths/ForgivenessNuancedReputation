# Forgiveness in Indirect Reciprocity

A small simulator for studying three forms of forgiveness in indirect
reciprocity (image-scoring) games on finite populations, with Fermi-learning
fixation analysis on top.

The simulator supports:

- **Assessment forgiveness** (`g_1` in the paper) — when an observer sees a
  donor defect, this is the probability that the observer does *not* lower
  the donor's image score.
- **Action forgiveness** (`g_2` in the paper) — the probability that a donor
  donates even when its strategy says it should not.
- **AJD ("Action-Justified Defection") forgiveness** — a variant of
  assessment forgiveness in which a defection is forgiven only if the
  observer would itself have defected against the same recipient given
  what it knows about the recipient.

Forgiveness probabilities can be either a flat generosity value (a single
probability used everywhere) or a per-image table of values; the table can
be supplied directly or auto-expanded from a list of exponents.

For each pair of (donation, forgiveness) strategies the script samples
stationary image-score and utility distributions in a population of
`NUM_AGENTS`, builds the Fermi-learning fixation matrix, and from that
computes a **cooperation index** and a **strategy-abundance heatmap**.

## Files

| File                  | Purpose                                                               |
|-----------------------|-----------------------------------------------------------------------|
| `main.py`             | Core simulator; takes an `.ini` config and writes a heatmap PNG.      |
| `make_ini_files.py`   | Generates the experiment grid: `.ini` files plus a `run.sh` driver.   |
| `make_graphs.ipynb`   | Parses the `.txt` log files produced by `run.sh` and plots results.   |
| `sample_config.ini`   | Tiny single-threaded config for smoke-testing the install.            |
| `requirements.txt`    | Python dependencies.                                                  |
| `LICENSE`             | MIT license.                                                          |

## Requirements

- Python 3.9 or newer (tested on 3.10).
- The packages listed in [`requirements.txt`](requirements.txt). Ray is
  required even when running locally because the script imports it
  unconditionally; if you want to disable distributed execution, set
  `REMOTE = False` in your `.ini` (Ray is still imported but not started).

Install with:

```bash
pip install -r requirements.txt
```

## Quick start

Generate the experiment grid, run every experiment, then open the notebook
to draw plots:

```bash
python make_ini_files.py            # writes ~220 .ini files and run.sh
bash run.sh                         # runs them all (this takes a while)
jupyter notebook make_graphs.ipynb  # plot cooperation index vs. noise
```

Each `.ini` file produces:

- a PNG heatmap of strategy abundances (`<name>.png`),
- a text log (`<name>.txt`) containing the cooperation index, fixation
  matrix, and image-score distributions used by the notebook.

If you just want to run a single experiment by hand:

```bash
python main.py -c my_config.ini -e my_heatmap.png -v
```

### Smoke test

A tiny [`sample_config.ini`](sample_config.ini) is included for verifying the
install. It runs single-threaded (no Ray cluster needed) with a 5-agent
population and finishes in a few seconds:

```bash
python main.py -c sample_config.ini -e sample_heatmap.png -v
```

## Configuration

The `.ini` files have two sections, `[DEFAULT]` and `[SYSTEM]`. Every key
falls back to a sensible default if omitted.

### `[DEFAULT]` — simulation parameters

| Key                       | Type    | Meaning                                                                                                                            |
|---------------------------|---------|------------------------------------------------------------------------------------------------------------------------------------|
| `REMOTE`                  | bool    | Use Ray actors for parallel sampling.                                                                                              |
| `ASSESSMENT_FORGIVENESS`  | bool    | Enable `g_1`.                                                                                                                      |
| `ACTION_FORGIVENESS`      | bool    | Enable `g_2`.                                                                                                                      |
| `AJD_FORGIVENESS`         | bool    | Enable AJD forgiveness.                                                                                                            |
| `MAX_IMAGE`               | int     | Maximum image score. Donation strategies range over `0..MAX_IMAGE+1` (the extra value represents "never donate").                  |
| `MIN_IMAGE`               | int     | Minimum image score. Typically `0`.                                                                                                |
| `OBS_PROB`                | float   | Probability that an agent successfully observes any given interaction.                                                             |
| `FORGIVE_VALUE`           | list    | Either `[p]` (a flat generosity probability) or an `NxM` list-of-lists indexed `[forgiveness_strategy][image]`.                    |
| `FORGIVE_EXPONENTS`       | bool    | If true, treat `FORGIVE_VALUE` as a list of exponents `e_j` and expand it to entries `exp((image - MAX_IMAGE) / e_j)` at load time. |
| `REPUTATION_NOISE`        | float   | Probability that an agent reads a uniformly random image instead of the true one.                                                  |
| `ACTION_NOISE`            | float   | Probability that an intended donation fails to occur.                                                                              |
| `PERCEPTION_NOISE`        | float   | Probability that an observer mis-perceives a donation event.                                                                       |
| `DONATE_UTILITY`          | float   | Per-interaction utility for the donor (negative = cost).                                                                           |
| `RECEIVE_UTILITY`         | float   | Per-interaction utility for the recipient.                                                                                         |
| `BETA`                    | float   | Selection strength for Fermi learning.                                                                                             |
| `NUM_AGENTS`              | int     | Population size.                                                                                                                   |
| `BURN_IN`                 | int     | Warm-up interactions before each sample.                                                                                           |
| `MIN_SIM_ITERS`           | int     | Lower bound on post-warm-up iterations per sample.                                                                                 |
| `MAX_SIM_ITERS`           | int     | Upper bound on post-warm-up iterations per sample.                                                                                 |
| `NUM_SIM_RUNS`            | int     | Number of independent runs to sample from.                                                                                         |

### `[SYSTEM]` — runtime knobs

| Key         | Type   | Meaning                                                  |
|-------------|--------|----------------------------------------------------------|
| `NUM_CPUS`  | int    | Number of CPUs to hand to Ray when `REMOTE = True`.      |
| `LOG_LEVEL` | string | Standard `logging` level (`INFO`, `DEBUG`, ...).         |

## Command-line flags

```
python main.py -c CONFIG -e HEATMAP [flags]
```

| Flag                          | Effect                                                          |
|-------------------------------|-----------------------------------------------------------------|
| `-c`, `--config`              | Path to the `.ini` config file (required).                      |
| `-e`, `--heatmap`             | Output filename for the heatmap PNG (required).                 |
| `-v`, `--verbose`             | Print the resolved config and turn on every print flag below.   |
| `-f`, `--fixation_matrix`     | Print the fixation matrix to stdout.                            |
| `-i`, `--cooperation_index`   | Print the cooperation index to stdout.                          |
| `-p`, `--print_heatmap`       | Print the heatmap array to stdout.                              |
| `-m`, `--print_images`        | Print the per-strategy image distributions to stdout.           |

The `run.sh` produced by `make_ini_files.py` uses `-v` and redirects stdout
into a `.txt` file per experiment; `make_graphs.ipynb` parses those files.

## Citation

If you use this code in academic work, please cite:

> _TODO: title, authors, venue, year, DOI/URL. BibTeX block goes here._

## License

MIT — see [LICENSE](LICENSE).
