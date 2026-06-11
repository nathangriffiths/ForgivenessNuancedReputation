# ForgivenessNuancedReputation

This repository contains the implementation of the donation game with generosity and forgiveness using nuanced reputation, as reported in "Generosity and Forgiveness: The Importance of Nuanced Reputation" (to appear). Note that this is a refactored version of the code to increase readability, and extends the work from a previous ECAI paper (in the ECAI-Forgiveness repository). Please get in touch if you spot any issues.

The analysis code is in Python, the main simulation is in Java (tested with version 25).

## Donation Game Simulation

The code has been tested on Mac and Linux, but is untested on Windows. The following directory structure is assumed for the main simulation:

    src/ - the code itself
    bin/ - the compiled Java code
    data/ - for storing the output of simulations as csv files
    lib/ - contains libraries used by the main simulation class
    scripts/ - contains example scripts for running the simulation (based on the results in the ECAI paper)
    t-test-results/ - contains the statistical significance data as reported in the paper

To compile the main simulation code use:

    javac -d bin -classpath .:./src:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar:./lib/commons-collections4-4.4/commons-collections4-4.4.jar src/Simulation.java

To run the simulation use (with appropriate arguments):

    java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar:./lib/commons-collections4-4.4/commons-collections4-4.4.jar Simulation

To see the possible arguments run with:

    java -classpath .:./bin:./lib/commons-cli-1.5.0/commons-cli-1.5.0.jar:./lib/commons-collections4-4.4/commons-collections4-4.4.jar Simulation -help

which should give the following output:

    usage: Simulation
     -ajd,--ascribed_justified_defection   Enable ascribed justified defection
                                           (i.e., do not reduce image score if
                                           observer would defect)
     -b,--beta <arg>                       Beta parameter
     -d,--data <arg>                       data directory for results storage
     -ea,--actionNoise <arg>               Probability of donation action
                                           failing (action noise)
     -ep,--perceptionNoise <arg>           Probability of an observer
                                           incorrectly perceiving donor's
                                           action (perception noise)
     -er,--recallNoise <arg>               Recall noise in retrieving image
                                           score
     -fa,--forgiveness_action              Enable action forgiveness
     -fr,--forgiveness_reputation          Enable reputation (assessment)
                                           forgiveness
     -freq                                 Output frequency distributions to
                                           files
     -g,--generations <arg>                Generations to run simulation for
     -g1 <arg>                             Probability of assessment
                                           generosity [Schmid et al.]
     -g2 <arg>                             Probability of action generosity
                                           [Schmid et al.]
     -generosity                           Enable generosity [Schmid et al.,
                                           Scientific Reports, 2021]
     -h,--help                             Display this message
     -is,--imageSpread <arg>               Spread (i.e., [-is, +is]) of the
                                           image score space (5 in Nowak and
                                           Sigmund's case)
     -l,--learning                         Use imitation learning
     -m,--pairs <arg>                      Number of donor-recipient pairs per
                                           generation
     -mr,--mutation <arg>                  Mutation rate
     -n,--size <arg>                       Population size
     -ns,--preventNegativePayoffs          prevent negative payoffs (use base
                                           Nowak and Sigmund formulation)
     -q,--observation <arg>                Probability of observing an
                                           interaction
     -trace                                Output traces (average reward, CI,
                                           image, donation strategy, and
                                           forgiveness strategy) to files

The scripts used to generate the main results in the paper are contained in the scripts directory. Note that the scripts will try to run several simulations simultaneously, so if you have limited cores you may wish to run them individually.

## Analytical code

Please see the separate readme in the analytical subdirectory.

## Citation

If you use this code in academic work, please cite:

Generosity and Forgiveness: The Importance of Nuanced Reputation, Nathan Griffiths and Nir Oren, Journal of Autonomous Agents and Multi-Agent Systems (to appear)

