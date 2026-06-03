import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.DoubleStream;

public class ForgivenessDonationGame extends NoisyDonationGame {
    boolean fa;
    boolean ajd;
    boolean fr;
    boolean fd;
    double[] forgivenessStrategies;
    static double[] forgivenessStrategySpace = {0.001, 0.5, 1.0, 1.355, 1.67}; // ECAI - F_md

    public ForgivenessDonationGame(int n, int m, double q, double mr, 
        int s_lower_bound, int s_upper_bound, boolean preventNegativePayoffs, 
        double er, double ea, double ep, boolean fa, boolean ajd, boolean fr, 
        boolean learning, double beta) {
            super(n, m, q, mr, s_lower_bound, s_upper_bound, preventNegativePayoffs, er, ea, ep, learning, beta);
            this.fa = fa;
            this.ajd = ajd;
            this.fr = fr;
            this.forgivenessStrategies = new double[n];
            for (int i = 0; i < forgivenessStrategies.length; i++) {
                forgivenessStrategies[i] = forgivenessStrategySpace[rand.nextInt(forgivenessStrategySpace.length)];
            }
        }       
        
    private UpdateResults cooperateImageUpdate(int donor, int recipient, boolean actionForgiveness) {
        int tmpAssessmentOp = 0;
        int tmpAssessmentCnt = 0;
        int tmpAjdOp = 0;
        int tmpAjdCnt = 0;
        if (q < 1.0) {
            for (int i = 0; i < n; i++) {
                // the recipient and donor always observe interaction with no misperception
                // donor maintains image score of self for use in forgiveness
                if (i == recipient || i == donor) {
                    super.incrementImage(i, donor);
                } else {
                    // check whether interaction observed
                    if (rand.nextDouble() < q) {
                        // if misperceived as defection
                        if (ep > 0.0 && DonationGame.rand.nextDouble() < ep) {
                            boolean forgive = false;
                            // check for ascribed justified defection forgiveness
                            if (ajd) {
                                tmpAjdOp += 1;
                                double rImageScore = super.getImageScore(i, recipient);
                                double dImageScore = super.getImageScore(i, donor);
                                // if the observer would not donate then forgive
                                if (rImageScore < strategies[i] && DonationGame.rand.nextDouble() < 
                                    Math.exp(-((-dImageScore + s_upper_bound) / forgivenessStrategies[i]))) {
                                        forgive = true;
                                        tmpAjdCnt += 1;
                                } 
                            }
                            // check for reputation (assessment) forgiveness 
                            if (fr) {
                                tmpAssessmentOp += 1;
                                double imageScore = super.getImageScore(i, donor);
                                if (DonationGame.rand.nextDouble() <
                                    Math.exp(-((-imageScore + s_upper_bound) / forgivenessStrategies[i]))) {
                                        forgive = true;
                                        tmpAssessmentCnt += 1;
                                }
                            }
                            if (!forgive) {
                                // not forgiving so reduce image score
                                super.decrementImage(i, donor);
                            }
                        } else {
                            // otherwise perceived correctly as cooperation
                            super.incrementImage(i, donor);
                        }
                    }
                }
            }
        }
        else {
            for (int i = 0; i < n; i++) {
                // the recipient and donor always observe interaction with no misperception
                // donor maintains image score of self for use in forgiveness
                if (i == recipient || i == donor) {
                    super.incrementImage(i, donor);
                } else {
                    boolean forgive = false;
                    // if misperceived as defection
                    if (ep > 0.0 && DonationGame.rand.nextDouble() < ep) {
                        // check for ascribed justified defection forgiveness
                        if (ajd) {
                            tmpAjdOp += 1;
                            double rImageScore = super.getImageScore(i, recipient);
                            double dImageScore = super.getImageScore(i, donor);
                            if (rImageScore < strategies[i] && DonationGame.rand.nextDouble() < 
                                Math.exp(-((-dImageScore + s_upper_bound) / forgivenessStrategies[i]))) {
                                    forgive = true;
                                    tmpAjdCnt += 1; 
                            }
                        }
                        // check whether potential for reputation (assessment) forgiveness 
                        if (fr) {
                            tmpAssessmentOp += 1;
                            double imageScore = super.getImageScore(i, donor);
                            if (DonationGame.rand.nextDouble() <
                                Math.exp(-((-imageScore + s_upper_bound) / forgivenessStrategies[i]))) {
                                    forgive = true;
                                    tmpAssessmentCnt += 1;
                                }
                        }
                        if (!forgive) {
                            // not forgiving so reduce image score
                            super.decrementImage(i, donor);
                        }
                    } else {
                        // otherwise perceived correctly as cooperation
                        super.incrementImage(i, donor);
                    }
                }
                
            }
        }

        if (tmpAjdCnt > tmpAjdOp) {
            System.exit(0);
        }
        UpdateResults results = new UpdateResults();
        results.setUpdateResults(tmpAssessmentOp, tmpAssessmentCnt, tmpAjdOp, tmpAjdCnt);
        return results;
    }

    private UpdateResults defectImageUpdate(int donor, int recipient) {
        int tmpAssessmentOp = 0;
        int tmpAssessmentCnt = 0;
        int tmpAjdOp = 0;
        int tmpAjdCnt = 0;
        if (q < 1.0) {
            for (int i = 0; i < n; i++) {
                // revised as recipient should be able to use (assessment) forgiveness
                // the recipient always observes interaction with no misperception
                if (i == recipient) {
                    // check for reputation (assessment) forgiveness
                    // NB the recipient does not consider justified defection forgiveness
                    if (fr) {
                        tmpAssessmentOp += 1;
                        double imageScore = super.getImageScore(i, donor);
                        // if not forgiving reduce image score
                        if (DonationGame.rand.nextDouble() >= 
                            Math.exp(-((-imageScore + s_upper_bound) / forgivenessStrategies[i]))) {
                                super.decrementImage(i, donor);
                        } else {
                            // otherwise forgive and do nothing
                            tmpAssessmentCnt += 1;
                        }
                    } else {
                        // else not forgiving so reduce image score
                        super.decrementImage(i, donor);
                    }
                } else if (i == donor) {
                    // maintain image score of self for potential use in forgiveness calculations
                    super.decrementImage(i, donor);
                } else {
                    // check whether interaction observed
                    if (rand.nextDouble() < q) {
                        // if misperceived as cooperation
                        if (ep > 0.0 && DonationGame.rand.nextDouble() < ep) {
                            super.incrementImage(i, donor);
                        } else {
                            boolean forgive = false;
                            // check for ascribed justified defection forgiveness 
                            if (ajd) {
                                tmpAjdOp += 1;
                                double rImageScore = super.getImageScore(i, recipient);
                                double dImageScore = super.getImageScore(i, donor);
                                // if the observer would not donate then forgive
                                if (rImageScore < strategies[i] && DonationGame.rand.nextDouble() < 
                                    Math.exp(-((-dImageScore + s_upper_bound) / forgivenessStrategies[i]))) {
                                        forgive = true;
                                        tmpAjdCnt += 1;
                                }
                            }
                            // check for reputation (assessment) forgiveness 
                            if (fr) {
                                tmpAssessmentOp += 1;
                                double imageScore = super.getImageScore(i, donor);
                                if (DonationGame.rand.nextDouble() <
                                    Math.exp(-((-imageScore + s_upper_bound) / forgivenessStrategies[i]))) {
                                        forgive = true;
                                        tmpAssessmentCnt += 1;
                                }                                 
                            } 
                            if (!forgive) {
                                // not forgiving so reduce image score
                                super.decrementImage(i, donor);
                            }
                        }
                    }
                }
            }
        } else {
            for (int i = 0; i < n; i++) {
                // the recipient always observes interaction with no misperception
                if (i == recipient) {
                    // check for reputation (assessment) forgiveness 
                    if (fr) {
                        tmpAssessmentOp += 1;
                        double imageScore = super.getImageScore(i, donor);
                        // if not forgiving reduce image score
                        if (DonationGame.rand.nextDouble() >= 
                            Math.exp(-((-imageScore + s_upper_bound) / forgivenessStrategies[i]))) {
                                super.decrementImage(i, donor);
                        } else {
                            // otherwise forgive and do nothing
                            tmpAssessmentCnt += 1;
                        }
                    } else {
                        // else not forgiving so reduce image score
                        super.decrementImage(i, donor);
                    }
                } else if (i == donor) {
                    // maintain image score of self for potential use in forgiveness calculations
                    super.decrementImage(i, donor);
                } else {
                    // if misperceived as cooperation
                    if (ep > 0.0 && DonationGame.rand.nextDouble() < ep) {
                        super.incrementImage(i, donor);
                    } else {
                        boolean forgive = false;                        
                        // check for ascribed justified defection forgiveness 
                        if (ajd) {
                            tmpAjdOp += 1;
                            double rImageScore = super.getImageScore(i, recipient);
                            double dImageScore = super.getImageScore(i, donor);
                            // if the observer would not donate then forgive
                            if (rImageScore < strategies[i] && DonationGame.rand.nextDouble() < 
                                Math.exp(-((-dImageScore + s_upper_bound) / forgivenessStrategies[i]))) {                                   
                                    forgive = true;
                                    tmpAjdCnt += 1;
                                    // System.out.println("YYY d2" + tmpAjdCnt);
                            }
                        }
                        // check for reputation (assessment) forgiveness 
                        if (fr) {
                            tmpAssessmentOp += 1;
                            double imageScore = super.getImageScore(i, donor);
                            if (DonationGame.rand.nextDouble() <
                                Math.exp(-((-imageScore + s_upper_bound) / forgivenessStrategies[i]))) {
                                    forgive = true;
                                    tmpAssessmentCnt += 1;
                            } 
                        }
                        if (!forgive) {
                            // not forgiving so reduce image score
                            super.decrementImage(i, donor);
                        }
                    }
                }
            }
        }
        if (tmpAjdCnt > tmpAjdOp) {
            System.exit(0);
        }
        UpdateResults results = new UpdateResults();
        results.setUpdateResults(tmpAssessmentOp, tmpAssessmentCnt, tmpAjdOp, tmpAjdCnt);
        return results;
    }

    public double getAverageForgivenessStrategy() {
        return Arrays.stream(forgivenessStrategies).average().orElse(Double.NaN);
    }


    public static double sumArray(double[][]array){
        return  Arrays.stream(array)
                      .flatMapToDouble(Arrays::stream)
                      .sum();
    }
            
    public TickResults tick() {
        int cooperationCount = 0;
        int fActionOpportunityCount = 0;
        int fActionCount = 0;
        int fAssessmentOpportunityCount = 0;
        int fAssessmentCount = 0;
        int fAjdOpportunityCount = 0;
        int fAdjCount = 0;
        UpdateResults tmpUpdateResults;
        
        for (int i = 0; i < m; i++) {
            int donor = DonationGame.rand.nextInt(n);
            int recipient = DonationGame.rand.nextInt(n);
            while (recipient == donor) {
                recipient = DonationGame.rand.nextInt(n);
            }
            double imageScore = super.getImageScore(donor, recipient);
            boolean intendToDonate;
            boolean forgiving = false;
            if (imageScore >= strategies[donor]) {
                intendToDonate = true;
            } else {
                intendToDonate = false;
                fActionOpportunityCount += 1;
            }
            // potential to donate through action forgiveness
            if (!intendToDonate && fa && DonationGame.rand.nextDouble() < 
                Math.exp(-((-imageScore + s_upper_bound) / forgivenessStrategies[donor]))) {
                    intendToDonate = true;
                    forgiving = true;
                }
            // impose action noise
            if (intendToDonate && ea > 0.0 && DonationGame.rand.nextDouble() < ea) {
                intendToDonate = false;
            }
            // perform the intended action
            if (intendToDonate) {
                rewards[donor] -= DonationGame.c;
                rewards[recipient] += DonationGame.b;
                if (forgiving) {
                    fActionCount += 1;
                }
                tmpUpdateResults = cooperateImageUpdate(donor, recipient, forgiving);
                fAssessmentOpportunityCount += tmpUpdateResults.fAssessmentOpportunityCount;
                fAssessmentCount += tmpUpdateResults.fAssessmentCount;
                fAjdOpportunityCount += tmpUpdateResults.fAjdOpportunityCount;
                fAdjCount += tmpUpdateResults.fAjdCount;
                cooperationCount += 1;
                if (fAdjCount > fAjdOpportunityCount) {
                    System.out.println("X4");
                    System.exit(0);
                }
            } else {
                // check whether potential for disclosure forgiveness
                if (fd) {
                    // if not forgiving then disclose defection
                    if (DonationGame.rand.nextDouble() >= 
                        // Math.exp(-((-imageScore + 5) / forgivenessStrategies[i]))) {
                        Math.exp(-((-imageScore + s_upper_bound) / forgivenessStrategies[donor]))) {
                            super.decrementImage(donor, recipient);
                    }
                    // otherwise forgive and do nothing
                } else {
                    tmpUpdateResults = defectImageUpdate(donor, recipient);
                    fAssessmentOpportunityCount += tmpUpdateResults.fAssessmentOpportunityCount;
                    fAssessmentCount += tmpUpdateResults.fAssessmentCount;
                    fAjdOpportunityCount += tmpUpdateResults.fAjdOpportunityCount;
                    fAdjCount += tmpUpdateResults.fAjdCount;
                    if (fAdjCount > fAjdOpportunityCount) {
                        System.out.println("X5");
                        System.exit(0);
                    }
                }
            }
            if (preventNegativePayoffs) {
                rewards[donor] += 0.1;
                rewards[recipient] += 0.1;
            }
        }

        TickResults results = new TickResults((double) cooperationCount / (double) m);

        if (fAdjCount > fAjdOpportunityCount) {
            System.out.println("X6");
            System.exit(0);
        }

        results.setForgivenessTickResults(fActionOpportunityCount, fActionCount,
            fAssessmentOpportunityCount, fAssessmentCount, fAjdOpportunityCount, fAdjCount);
        return results;

    }

    public void mutation() {
        if (mr == 0.0) return;
        for (int i = 0; i < n; i++) {
            if (rand.nextDouble() < mr) {
                int k = rand.nextInt(k_lower_bound, k_upper_bound + 1);
                while (k == strategies[i]) {
                    k = rand.nextInt(k_lower_bound, k_upper_bound + 1);
                }
                int index = rand.nextInt(forgivenessStrategySpace.length);
                while (forgivenessStrategySpace[index] == forgivenessStrategies[i]) {
                    index = rand.nextInt(forgivenessStrategySpace.length);
                }
                strategies[i] = k;
                forgivenessStrategies[i] = forgivenessStrategySpace[index];
            }
        }
    }

    public void rouletteWheelSelection() {
        double[] rewardsScaled = scaleRewards();
        double totalReward = DoubleStream.of(rewardsScaled).sum();
        if (totalReward == 0.0) {
            System.out.println("ZERO REWARD");
            System.out.println("x1" + Arrays.toString(rewardsScaled));
            System.out.println("x1" + Arrays.toString(strategies));
            System.exit(1);
        }
        int[] newDonationStrategies = new int[n];
        double[] newForgivenessStrategies = new double[n];
        int index;
        for (int i = 0; i < n; i++) {
            index = weightedRandomChoice(rewardsScaled, totalReward);
            newDonationStrategies[i] = strategies[index];
            newForgivenessStrategies[i] = forgivenessStrategies[index];
        }
        strategies = newDonationStrategies;
        forgivenessStrategies = newForgivenessStrategies;

        Arrays.fill(rewards, 0.0);
        Arrays.stream(imageScores).forEach(a -> Arrays.fill(a, 0));
    }

    public void imitationLearning() {
        int[] newStrategies = new int[n];
        double[] newForgivenessStrategies = new double[n];
        int j;
        for (int i = 0; i < n; i++) {
            j = rand.nextInt(n);
            while (j == i) {
                j = rand.nextInt(n);
            }
            if (DonationGame.rand.nextDouble() < Math.pow((1 + Math.exp(beta * (rewards[i] - rewards[j]))), -1)) {
               newStrategies[i] = strategies[j];
               newForgivenessStrategies[i] = forgivenessStrategies[j];
            } else {
                newStrategies[i] = strategies[i];
                newForgivenessStrategies[i] = forgivenessStrategies[i];
            }
        }
        strategies = newStrategies;
        forgivenessStrategies = newForgivenessStrategies;
        Arrays.fill(rewards, 0.0);
        Arrays.stream(imageScores).forEach(a -> Arrays.fill(a, 0));
    }
}
