import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.text.DecimalFormat;
import java.util.UUID;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.collections4.map.MultiKeyMap;

// import net.sourceforge.argparse4j.ArgumentParsers;
// import net.sourceforge.argparse4j.inf.ArgumentParser;
// import net.sourceforge.argparse4j.inf.ArgumentParserException;
// import net.sourceforge.argparse4j.inf.Namespace;

public class Simulation {

    private static final DecimalFormat df = new DecimalFormat("0.000");
    private static final DecimalFormat df4 = new DecimalFormat("0.0000");

    // public static final int CI = 0;
    // public static final int GI_ACTION = 1;
    // public static final int GI_ASSESSMENT = 2;
    // public static final int FI_ACTION = 3;
    // public static final int FI_ASSESSMENT = 4;

    // private static void swap(int[] array, int i, int j) {
    //     int temp = array[i];
    //     array[i] = array[j];
    //     array[j] = temp;
    // }

    // private static void writex(final String fileName, String s) throws IOException {
    //     String dataDir = "data";
    //     // String dataFile = "test.csv";
    //     Path path = Paths.get(".", dataDir, fileName);
    //     System.out.println("path " + path);
    //     System.out.println(System.getProperty("java.io.tmpdir"));
    //     Files.writeString(path,
    //         // Path.of(System.getProperty("java.io.tmpdir"), "filename.txt"),
    //         s + System.lineSeparator(),
    //         StandardOpenOption.CREATE, StandardOpenOption.APPEND
    //     );
    // }
    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption(new Option("h", "help", false, "Display this message"));
        options.addOption(new Option("n", "size", true, "Population size"));
        options.addOption(new Option("m", "pairs", true, "Number of donor-recipient pairs per generation"));
        options.addOption(new Option("is", "imageSpread", true, "Spread (i.e., [-is, +is]) of the image score space (5 in Nowak and Sigmund's case)"));
        options.addOption(new Option("q", "observation", true, "Probability of observing an interaction"));
        options.addOption(new Option("mr", "mutation", true, "Mutation rate"));
        options.addOption(new Option("er", "recallNoise", true, "Recall noise in retrieving image score"));
        options.addOption(new Option("ea", "actionNoise", true, "Probability of donation action failing (action noise)"));
        options.addOption(new Option("ep", "perceptionNoise", true, "Probability of an observer incorrectly perceiving donor\'s action (perception noise)"));
        options.addOption(new Option("ns", "preventNegativePayoffs", false, "prevent negative payoffs (use base Nowak and Sigmund formulation)"));
        options.addOption(new Option("generosity", false, "Enable generosity [Schmid et al., Scientific Reports, 2021]"));
        options.addOption(new Option("g1", true, "Probability of assessment generosity [Schmid et al.]"));
        options.addOption(new Option("g2", true, "Probability of action generosity [Schmid et al.]"));
        options.addOption(new Option("fa", "forgiveness_action", false, "Enable action forgiveness"));
        // options.addOption(new Option("afa", "ascribed_forgiveness_action", false, "Enable ascribed action forgiveness (i.e., without public knowledge of forgiveness)"));
        options.addOption(new Option("ajd", "ascribed_justified_defection", false, "Enable ascribed justified defection (i.e., do not reduce image score if observer would defect)"));
        options.addOption(new Option("fr", "forgiveness_reputation", false, "Enable reputation (assessment) forgiveness"));
        options.addOption(new Option("fd", "forgiveness_disclosure", false, "Enable disclosure forgiveness"));
        // options.addOption(new Option("rf", "image_forgiveness", true, "Image score (reputation) benefit associated with forgiving"));
        options.addOption(new Option("g", "generations", true, "Generations to run simulation for"));
        options.addOption(new Option("l", "learning", false, "Use imitation learning"));
        options.addOption(new Option("b", "beta", true, "Beta parameter"));
        // options.addOption(new Option("quiet", false, "Run with minimal output"));
        options.addOption(new Option("freq", false, "Output frequency distributions to files"));
        options.addOption(new Option("trace", false, "Output traces (average reward, CI, image, donation strategy, and forgiveness strategy) to files"));
        options.addOption(new Option("d", "data", true, "data directory for results storage"));
        CommandLineParser parser = new DefaultParser(false);
        CommandLine cmd = parser.parse(options, args);
        HelpFormatter formatter = new HelpFormatter();
        
        int n;
        int m;
        int imageSpread;
        double q;
        double mr;
        double er;
        double ea;
        double ep;
        boolean preventNegativePayoffs;
        boolean generosity;
        double g1;
        double g2;
        boolean fa;
        // boolean afa;
        boolean ajd;
        boolean fr;
        boolean fd;
        // double rf;
        boolean learning;
        double beta;
        int generations;
        // boolean quiet;
        boolean freq;
        boolean trace;
        String dataDir;
        if (cmd.hasOption("h")) {
            formatter.printHelp("Simulation", options);
            System.out.println();
            System.exit(0);
        }
        if(cmd.hasOption("n")) {
            n = Integer.parseInt(cmd.getOptionValue("n"));
        } else {
            n = 100;
        }
        if(cmd.hasOption("m")) {
            m = Integer.parseInt(cmd.getOptionValue("m"));
        } else {
            m = 300;
        }
        if(cmd.hasOption("is")) {
            imageSpread = Integer.parseInt(cmd.getOptionValue("is"));
        } else {
            imageSpread = 5;
        }
        if(cmd.hasOption("q")) {
            q = Double.parseDouble(cmd.getOptionValue("q"));
        } else {
            q = 1.0;
        }
        if(cmd.hasOption("mr")) {
            mr = Double.parseDouble(cmd.getOptionValue("mr"));
        } else {
            mr = 0.001;
        }
        if(cmd.hasOption("er")) {
            er = Double.parseDouble(cmd.getOptionValue("er"));
        } else {
            er = 0.0;
        }
        if(cmd.hasOption("ea")) {
            ea = Double.parseDouble(cmd.getOptionValue("ea"));
        } else {
            ea = 0.0;
        }
        if(cmd.hasOption("ep")) {
            ep = Double.parseDouble(cmd.getOptionValue("ep"));
        } else {
            ep = 0.0;
        }
        if (cmd.hasOption("ns")) {
            preventNegativePayoffs = true;
        } else {
            preventNegativePayoffs = false;
        }
        if (cmd.hasOption("generosity")) {
            generosity = true;
        } else {
            generosity = false;
        }
        if(cmd.hasOption("g1")) {
            g1 = Double.parseDouble(cmd.getOptionValue("g1"));
            if (!generosity) {
                System.out.println("Error: attempted to set g1 without enabling generosity");
                formatter.printHelp("Simulation", options);
                System.out.println();
                System.exit(1);
            }
        } else {
            g1 = 0.0;
        }
        if(cmd.hasOption("g2")) {
            g2 = Double.parseDouble(cmd.getOptionValue("g2"));
            if (!generosity) {
                System.out.println("Error: attempted to set g2 without enabling generosity");
                formatter.printHelp("Simulation", options);
                System.out.println();
                System.exit(1);
            }
        } else {
            g2 = 0.0;
        }
        if (cmd.hasOption("fa")) {
            fa = true;
        } else {
            fa = false;
        }
        // if (cmd.hasOption("afa")) {
        //     afa = true;
        // } else {
        //     afa = false;
        // }
        if (cmd.hasOption("ajd")) {
            ajd = true;
        } else {
            ajd = false;
        }
        if (cmd.hasOption("fr")) {
            fr = true;
        } else {
            fr = false;
        }
        if (cmd.hasOption("fd")) {
            fd = true;
        } else {
            fd = false;
        }
        // if(cmd.hasOption("rf")) {
        //     rf = Double.parseDouble(cmd.getOptionValue("rf"));
        // } else {
        //     rf = 0.0;
        // }
        if(cmd.hasOption("g")) {
            generations = Integer.parseInt(cmd.getOptionValue("g"));
        } else {
            generations = 100;
        }
        if (cmd.hasOption("l")) {
            learning = true;
        } else {
            learning = false;
        }
        if (cmd.hasOption("b")) {
            beta = Double.parseDouble(cmd.getOptionValue("b"));
        } else {
            beta = 1.0;
        }
        // if (cmd.hasOption("quiet")) {
        //     quiet = true;
        // } else {
        //     quiet = false;
        // }
        if (cmd.hasOption("freq")) {
            freq = true;
        } else {
            freq = false;
        }
        if (cmd.hasOption("trace")) {
            trace = true;
        } else {
            trace = false;
        }
        if(cmd.hasOption("d")) {
            dataDir = cmd.getOptionValue("d");
        } else {
            dataDir = "data";
        }
        boolean quiet = false;
        if (!quiet) {
            System.out.println("Configuration: n=" + n + " m=" + m + " is=" + imageSpread + " q=" + q + " mr=" + df4.format(mr) 
            + " er=" + df.format(er) + " ea=" + df.format(ea) + " ep=" + df.format(ep)
            + " ns=" + preventNegativePayoffs
            + " generosity=" + generosity + " g1=" + g1 + " g2=" + g2
            // + " fa=" + fa + " afa=" + afa + " fr=" + fr + " fd=" + fd + " rf=" + rf 
            + " fa=" + fa + " ajd=" + ajd + " fr=" + fr + " fd=" + fd  
            + " l=" + learning + " b=" + beta
            + " generations=" + generations);
        }

        // if (fa && afa) {
        // if (fa) {
        //     System.out.println("Error: Cannot use public and ascribed (i.e., private information) action forgiveness simultaneously: either use -fa or -afa.");
        //     System.exit(1);
        // }
        // String dataDir = "data";
        StringBuilder sb = new StringBuilder();
        sb.append("n" + n);
        sb.append("_m" + m);
        sb.append("_is" + imageSpread);
        sb.append("_q" + q);
        sb.append("_mr" + df4.format(mr));
        sb.append("_er" + df.format(er));
        sb.append("_ea" + df.format(ea));
        sb.append("_ep" + df.format(ep));
        sb.append("_ns" + (preventNegativePayoffs ? "True" : "False"));
        sb.append("_gen" + (generosity ? "True" : "False"));
        if (generosity) {
            sb.append("_g1" + g1);
            sb.append("_g2" + g2);
        }
        sb.append("_fa" + (fa ? "True" : "False"));
        // sb.append("_afa" + (afa ? "True" : "False"));
        sb.append("_ajd" + (ajd ? "True" : "False"));
        sb.append("_fr" + (fr ? "True" : "False"));
        sb.append("_fd" + (fd ? "True" : "False"));
        // if (fa || afa || fr || fd) {
        //     sb.append("_rf" + df.format(rf));
        // }
        sb.append("_l" + (learning ? "True" : "False"));
        sb.append("_b" + df.format(beta));
        sb.append("_g" + generations );
        String fileName = sb.toString();
        Path rewardAvPath = Paths.get(".", dataDir, fileName + "_reward-averages.csv");
        Path ciAvPath = Paths.get(".", dataDir, fileName + "_ci-averages.csv");
        Path kAvFreqPath = Paths.get(".", dataDir, fileName + "_kAvFreq.csv");
        Path fAvFreqPath = Paths.get(".", dataDir, fileName + "_fAvFreq.csv");
        String uuid = UUID.randomUUID().toString();
        if (!quiet) {
            System.out.println("UUID: " + uuid);
        }
         
        Path kfAvFreqPath = Paths.get(".", dataDir, fileName + "_kfAvFreq-" + uuid + ".csv");
        // working here
        Path ciTracePath = Paths.get(".", dataDir, fileName + "_ci-" + uuid + ".csv");
        Path giActionTracePath = Paths.get(".", dataDir, fileName + "_gi-action-" + uuid + ".csv");
        Path giAssessmentTracePath = Paths.get(".", dataDir, fileName + "_gi-assessment-" + uuid + ".csv");
        // Path gActionOpPath = Paths.get(".", dataDir, fileName + "_g-actionOp-" + uuid + ".csv");
        Path gActionCntPath = Paths.get(".", dataDir, fileName + "_g-actionCnt-" + uuid + ".csv");
        // Path gAssessmentOpPath = Paths.get(".", dataDir, fileName + "_g-assessmentOp-" + uuid + ".csv");
        Path gAssessmentCntPath = Paths.get(".", dataDir, fileName + "_g-assessmentCnt-" + uuid + ".csv");
        Path fiActionTracePath = Paths.get(".", dataDir, fileName + "_fi-action-" + uuid + ".csv");
        Path fiAssessmentTracePath = Paths.get(".", dataDir, fileName + "_fi-assessment-" + uuid + ".csv");
        Path fiAJDTracePath = Paths.get(".", dataDir, fileName + "_fi-ajd-" + uuid + ".csv");
        Path fActionCntPath = Paths.get(".", dataDir, fileName + "_f-actionCnt-" + uuid + ".csv");
        Path fAssessmentCntPath = Paths.get(".", dataDir, fileName + "_f-assessmentCnt-" + uuid + ".csv");
        Path fAjdCntPath = Paths.get(".", dataDir, fileName + "_f-ajdCnt-" + uuid + ".csv");
        Path avRewardTracePath = Paths.get(".", dataDir, fileName + "_avReward-" + uuid + ".csv");
        Path avImageTracePath = Paths.get(".", dataDir, fileName + "_avImage-" + uuid + ".csv");
        Path avDonationStrategyTracePath = Paths.get(".", dataDir, fileName + "_avDonationStrategy-" + uuid + ".csv");
        // Path avForgivenessStrategyTracePath = Paths.get(".", dataDir, fileName + "_avForgivenessStrategy-" + uuid + ".csv");
        // reward, CI, average image, average donation and forgiveness strategies


        
        DonationGame game;
        // if (!generosity && !fa && !afa && !fr && !fd && er == 0.0 && ea == 0.0 && ep == 0.0) {
        if (!generosity && !fa && !ajd && !fr && !fd && er == 0.0 && ea == 0.0 && ep == 0.0) {
            System.out.println("Using DonationGame, i.e., without noise, generosity or forgiveness");
            if (learning) System.out.println("(with learning)");
            // game = new DonationGame(n, m, q, mr, -5, 5, preventNegativePayoffs);
            game = new DonationGame(n, m, q, mr, -imageSpread, imageSpread, preventNegativePayoffs, learning, beta);
        } else {
            if (generosity && (g1 > 0.0 || g2 > 0.0)) {
                // if (fa || afa || fr || fd) {
                if (fa || fr || fd) {
                        System.out.println("Error: Cannot use generosity alongside forgiveness: disable either generosity or forgiveness.");
                    System.exit(1);
                }
                System.out.println("Using GenerosityDonationGame, i.e., with noise and generosity");
                if (learning) System.out.println("(with learning)");
                // game = new GenerosityDonationGame(n, m, q, mr, -5, 5, preventNegativePayoffs, er, ea, ep, g1, g2);
                game = new GenerosityDonationGame(n, m, q, mr, -imageSpread, imageSpread, preventNegativePayoffs, er, ea, ep, g1, g2, learning, beta);
            // } else if (fa || afa || fr || fd) {
            } else if (fa || ajd || fr || fd) {
                System.out.println("Using ForgivenessDonationGame, i.e., with noise and forgiveness");
                if (learning) System.out.println("(with learning)");
            // game = new ForgivenessDonationGame(n, m, q, mr, -5, 5, preventNegativePayoffs, er, ea, ep, fa, afa, fr, fd, rf);                
                // game = new ForgivenessDonationGame(n, m, q, mr, -imageSpread, imageSpread, preventNegativePayoffs, er, ea, ep, fa, afa, fr, fd, rf, learning, beta);                
                game = new ForgivenessDonationGame(n, m, q, mr, -imageSpread, imageSpread, preventNegativePayoffs, er, ea, ep, fa, ajd, fr, fd, learning, beta);                
                if (learning) System.out.println("(with learning)");
            } else {
                System.out.println("Using NoisyDonationGame, i.e., with noise");
                if (learning) System.out.println("(with learning)");
                // game = new NoisyDonationGame(n, m, q, mr, -5, 5, preventNegativePayoffs, er, ea, ep);
                game = new NoisyDonationGame(n, m, q, mr, -imageSpread, imageSpread, preventNegativePayoffs, er, ea, ep, learning, beta);
            }
        }


        if (!quiet) {
            System.out.println("Output file for reward averages: " + rewardAvPath);
            System.out.println("Output file for cooperation index: " + ciAvPath);
            System.out.println("Output file for average reward: " + avRewardTracePath);
            System.out.println("Output file for average image score: " + avImageTracePath);
            System.out.println("Output file for average donation strategy: " + avDonationStrategyTracePath);
        if (freq) {
                System.out.println("Output file for frequency of donation strategies: " + kAvFreqPath);
                System.out.println("Output file for frequency of forgiveness strategies: " + fAvFreqPath);
                System.out.println("Output file for frequency of donations and forgiveness strategies: " + kfAvFreqPath);
            }
            if (trace) {
                System.out.println("Output file for average CI: " + ciTracePath);
                if (game instanceof ForgivenessDonationGame) {
                    System.out.println("Output file for average action FI: " + fiActionTracePath);
                    System.out.println("Output file for average assessment FI: " + fiAssessmentTracePath);
                    System.out.println("Output file for average AJD FI: " + fiAJDTracePath);
                    System.out.println("Output file for action forgiveness count: " + fActionCntPath);
                    System.out.println("Output file for assessment forgiveness count: " + fAssessmentCntPath);
                    System.out.println("Output file for ajd forgiveness count: " + fAjdCntPath);
                }
                if (game instanceof GenerosityDonationGame) {
                    System.out.println("Output file for average action GI: " + giActionTracePath);
                    System.out.println("Output file for average assessment GI: " + giAssessmentTracePath);
                    // System.out.println("Output file for average action generosity opportunities: " + gActionOpPath);
                    System.out.println("Output file for average action generosity count: " + gActionCntPath);
                    // System.out.println("Output file for average assessment generosity opportunities: " + gAssessmentOpPath);
                    System.out.println("Output file for average assessment generosity count: " + gAssessmentCntPath);
                }
                // System.out.println("Output file for average forgiveness strategy: " + avForgivenessStrategyTracePath);

            }
        }



        TickResults tmpResults;
        double[] rewardAverages = new double[generations];
        double[] cooperationIndex = new double[generations];

        double[] actionGenerosityIndex = new double[generations];
        double[] assessmentGenerosityIndex = new double[generations];
        // int[] actionGenerosityOpportunities = new int[generations];
        int[] actionGenerosityCount = new int[generations];
        // int[] assessmentGenerosityOpportunities = new int[generations];
        int[] assessmentGenerosityCount = new int[generations];

        double[] actionForgivenessIndex = new double[generations];
        double[] assessmentForgivenessIndex = new double[generations];
        double[] ajdForgivenessIndex = new double[generations];
        int[] actionForgivenessCount = new int[generations];
        int[] assessmentForgivenessCount = new int[generations];
        int[] ajdForgivenessCount = new int[generations];

        double[] avImage = new double[generations];
        double[] avDonationStrategy = new double[generations];
        // double[] avForgivenessStrategy = new double[generations];

        Map<Integer,Integer> k_counts = new TreeMap<Integer,Integer>();
        // Note, this is only used for forgiveness games
        // This is a hack to inspect forgiveness strategy frequencies, and
        // if used for more than occasional debugging should be refactored. 
        Map<Double,Integer> f_counts = new TreeMap<Double,Integer>();

        int k_offset = Math.abs(game.k_lower_bound); // offset to allow 0-indexing of array by strategy
        int tmp_k_idx; // tmp variable for storing strategy when updating stats
        Double tmp_f; // tmp variable for storing forgiveness when updating stats

        // Initialise the kf counts map
        ArrayList<Map<Double,Integer>> kf_counts = new ArrayList<Map<Double,Integer>>();
        if (freq) {
            if (game instanceof ForgivenessDonationGame) {
                for (int i = game.k_lower_bound; i < game.k_upper_bound + 1; i++) {
                    kf_counts.add(new HashMap<Double,Integer>());
                    for (int j = 0; j < ForgivenessDonationGame.forgivenessStrategySpace.length; j++) {
                        kf_counts.get(kf_counts.size() - 1).put(Double.valueOf(ForgivenessDonationGame.forgivenessStrategySpace[j]), Integer.valueOf(0));
                    }
                }
            }
        }

        for (int i = 0; i < generations; i++) {
            tmpResults = game.tick();
            // System.out.println(i + " " + tmpResults.g_assessmentCnt);
            // cooperationIndex[i] = tmpResult[Simulation.CI];
            cooperationIndex[i] = tmpResults.ci;
            
            rewardAverages[i] = game.getAverageReward();
            if (freq) {
                for (int k : game.strategies) {
                    if (k_counts.containsKey(k)) {
                        k_counts.put(k, k_counts.get(k) + 1);
                    } else {
                        k_counts.put(k, 1);
                    }
                }
                // if (fa || afa || fr || fd) {
                if (game instanceof ForgivenessDonationGame) {
                    for (double f : ((ForgivenessDonationGame) game).forgivenessStrategies) {
                        if (f_counts.containsKey(f)) {
                            f_counts.put(f, f_counts.get(f) + 1);
                        } else {
                            f_counts.put(f, 1);
                        }
                    }
                }

                if (game instanceof ForgivenessDonationGame) {
                    for (int agt = 0; agt < n; agt++) {
                        tmp_k_idx = game.strategies[agt] + k_offset;
                        tmp_f = ((ForgivenessDonationGame) game).forgivenessStrategies[agt];
                        kf_counts.get(tmp_k_idx).put(tmp_f, (kf_counts.get(tmp_k_idx).get(tmp_f) + 1));
                    }
                }
            }
            if (trace) {
                avImage[i] = game.getAverageImage();
                avDonationStrategy[i] = game.getAverageDonationStrategy();
                if (game instanceof ForgivenessDonationGame) {
                    // System.out.println("AJD " + tmpResults.f_ajdCnt + " of " + tmpResults.f_ajdOp);

                    if (tmpResults.f_ajdCnt > tmpResults.f_ajdOp) {
                        System.out.println("X8");
                        System.exit(0);
                    }

                    // System.out.println(tmpResults.fi_action + " " + tmpResults.fi_assessment + " " + tmpResults.fi_ajd);
                    // avForgivenessStrategy[i] = game.getAverageForgivenessStrategy();
                    // forgivenessIndex[i] = tmpResult[1];
                    // forgivenessIndex[i] = tmpResult[Simulation.FI];
                    // actionForgivenessIndex[i] = tmpResults.fi_action;
                    // assessmentForgivenessIndex[i] = tmpResults.fi_assessment;
                    // ajdForgivenessIndex[i] = tmpResults.fi_ajd;
                    if (tmpResults.f_actionOp > 0) {
                        actionForgivenessIndex[i] = (double) tmpResults.f_actionCnt / (double) tmpResults.f_actionOp;
                    } else {
                        actionForgivenessIndex[i] = 0.0;
                    }
                    if (tmpResults.f_assessmentOp > 0) {
                        assessmentForgivenessIndex[i] = (double) tmpResults.f_assessmentCnt / (double) tmpResults.f_assessmentOp;
                    } else {
                        assessmentForgivenessIndex[i] = 0.0;
                    }
                    // System.out.println("AJD " + tmpResults.f_ajdCnt + " of " + tmpResults.f_ajdOp);
                    if (tmpResults.f_ajdOp > 0) {
                        ajdForgivenessIndex[i] = (double) tmpResults.f_ajdCnt / (double) tmpResults.f_ajdOp;
                    } else {
                        ajdForgivenessIndex[i] = 0.0;
                    }
                    // XX
                    // System.out.println("cnt:" + tmpResults.f_ajdCnt + " op:" + tmpResults.f_ajdOp + " av: " + ajdForgivenessIndex[i]);
                    actionForgivenessCount[i] = tmpResults.f_actionCnt;
                    assessmentForgivenessCount[i] = tmpResults.f_assessmentCnt;
                    ajdForgivenessCount[i] = tmpResults.f_ajdCnt;
                    
                }
                if (game instanceof GenerosityDonationGame) {
                    // avForgivenessStrategy[i] = game.getAverageForgivenessStrategy();
                    // forgivenessIndex[i] = tmpResult[1];
                    // forgivenessIndex[i] = tmpResult[Simulation.FI];
                    // actionGenerosityIndex[i] = tmpResults.gi_action;
                    // assessmentGenerosityIndex[i] = tmpResults.gi_assessment;
                    actionGenerosityIndex[i] = (double) tmpResults.g_actionCnt / (double) tmpResults.g_actionOp;
                    assessmentGenerosityIndex[i] = (double) tmpResults.g_assessmentCnt / (double) tmpResults.g_assessmentOp;
                    // actionGenerosityOpportunities[i] = tmpResults.g_actionOp;
                    actionGenerosityCount[i] = tmpResults.g_actionCnt;
                    // assessmentGenerosityOpportunities[i] = tmpResults.g_assessmentOp;
                    assessmentGenerosityCount[i] = tmpResults.g_assessmentCnt;
                }                
            }
            if (learning) {
                game.imitationLearning();
                game.mutation();
            } else {
                game.rouletteWheelSelection();
                game.mutation();
            }
        }
        if (freq) {
            if (game instanceof ForgivenessDonationGame) {
                sb.delete(0, sb.length());
                sb.append("fs");
                // System.out.print("fs");
                for (int k = game.k_lower_bound; k < game.k_upper_bound + 1; k++) {
                    // System.out.print("," + k);
                    sb.append("," + k);
                }
                Files.writeString(kfAvFreqPath, sb.toString() + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                // System.out.println();
                for (int fs = 0; fs < ForgivenessDonationGame.forgivenessStrategySpace.length; fs++) {
                    sb.delete(0, sb.length()); 
                    sb.append(ForgivenessDonationGame.forgivenessStrategySpace[fs]);
                    // System.out.print(ForgivenessDonationGame.forgivenessStrategySpace[fs]);
                    for (int k = game.k_lower_bound; k < game.k_upper_bound + 1; k++) {
                        // System.out.print("," + ((double) kf_counts.get(k + k_offset).get(ForgivenessDonationGame.forgivenessStrategySpace[fs])) / (game.n * generations));
                        sb.append("," + df4.format(((double) kf_counts.get(k + k_offset).get(ForgivenessDonationGame.forgivenessStrategySpace[fs])) / (game.n * generations)));
                    }
                    Files.writeString(kfAvFreqPath, sb.toString() + System.lineSeparator(), StandardOpenOption.APPEND);
                    // System.out.println();
                }
            }
        }
        if (trace) {
            for (int i = 0; i < generations; i++) {
                Files.writeString(avRewardTracePath, (i + ", " + df.format(rewardAverages[i])) + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                Files.writeString(ciTracePath, (i + ", " + df.format(cooperationIndex[i])) + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                Files.writeString(avImageTracePath, (i + ", " + df.format(avImage[i])) + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                Files.writeString(avDonationStrategyTracePath, (i + ", " + df.format(avDonationStrategy[i])) + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                if (game instanceof ForgivenessDonationGame) {
                    Files.writeString(fiActionTracePath, (i + ", " + df.format(actionForgivenessIndex[i])) + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    Files.writeString(fiAssessmentTracePath, (i + ", " + df.format(assessmentForgivenessIndex[i])) + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    Files.writeString(fiAJDTracePath, (i + ", " + df.format(ajdForgivenessIndex[i])) + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    Files.writeString(fActionCntPath, (i + ", " + actionForgivenessCount[i]) + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    Files.writeString(fAssessmentCntPath, (i + ", " + assessmentForgivenessCount[i]) + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    Files.writeString(fAjdCntPath, (i + ", " + ajdForgivenessCount[i]) + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

                }
                if (game instanceof GenerosityDonationGame) {
                    Files.writeString(giActionTracePath, (i + ", " + df.format(actionGenerosityIndex[i])) + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    Files.writeString(giAssessmentTracePath, (i + ", " + df.format(assessmentGenerosityIndex[i])) + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    // Files.writeString(gActionOpPath, (i + ", " + actionGenerosityOpportunities[i]) + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    Files.writeString(gActionCntPath, (i + ", " + actionGenerosityCount[i]) + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    // Files.writeString(gAssessmentOpPath, (i + ", " + assessmentGenerosityOpportunities[i]) + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    Files.writeString(gAssessmentCntPath, (i + ", " + assessmentGenerosityCount[i]) + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                }
                // Files.writeString(avRewardTracePath, (i + ", " + df.format(rewardAverages[i])), StandardOpenOption.APPEND);
                // System.out.println(i + ", " + df.format(avDonationStrategy[i]));
            }
        }
        quiet = true;
        double averageReward = Arrays.stream(rewardAverages).sum() / (double) generations;
        if (!quiet) {
            System.out.println("Average reward: " + averageReward);
            System.out.println(rewardAverages.toString());
        }
        Files.writeString(rewardAvPath, averageReward + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        double averageCI = Arrays.stream(cooperationIndex).sum() / (double) generations;
        if (!quiet) {
            System.out.println("Cooperation index: " + averageCI);
            System.out.println(cooperationIndex.toString());
        }
        Files.writeString(ciAvPath, averageCI + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        if (freq) {
            Map<Integer,Double> av_k_frequency = new TreeMap<Integer,Double>();
            for (int k = game.k_lower_bound; k <= game.k_upper_bound; k++) {
                if (k_counts.containsKey(k)) {
                    av_k_frequency.put(k, ((double) k_counts.get(k)) / (n * generations));
                } else {
                    av_k_frequency.put(k, 0.0);
                }
            }
            // sanity check that frequencies sum to 1.0
            // System.out.println(av_k_frequency.values().stream().mapToDouble(d -> d).sum());
            if (!quiet) {
                System.out.println("Average k frequencies:" + av_k_frequency);
            }
            Files.writeString(kAvFreqPath, av_k_frequency.values().toString().replace("[", "").replace("]", "") + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        
            if (game instanceof ForgivenessDonationGame) {
                Map<Double,Double> av_f_frequency = new TreeMap<Double,Double>();
                for (double fs : ((ForgivenessDonationGame) game).forgivenessStrategySpace) {
                    if (f_counts.containsKey(fs)) {
                        av_f_frequency.put(fs, ((double) f_counts.get(fs)) / (n * generations));
                    } else {
                        av_f_frequency.put(fs, 0.0);
                    }
                }
                if (!quiet) {
                    System.out.println("Average f frequencies:" + av_f_frequency);
                }
                Files.writeString(fAvFreqPath, av_f_frequency.values().toString().replace("[", "").replace("]", "") + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } 
        }
    }
}
