import java.util.Arrays;

public class GenerosityDonationGame extends NoisyDonationGame {
    boolean generosity;
    double g1;
    double g2;

    public GenerosityDonationGame(int n, int m, double q, double mr, 
        int s_lower_bound, int s_upper_bound, boolean preventNegativePayoffs, 
        double er, double ea, double ep, double g1, double g2, boolean learning, double beta) {
            super(n, m, q, mr, s_lower_bound, s_upper_bound, preventNegativePayoffs, er, ea, ep, learning, beta);
            this.g1 = g1;
            this.g2 = g2;
        }       
        
    private int[] cooperateImageUpdate(int donor, int recipient) {
        int tmpGenerosityCount = 0;
        int tmpGenerosityOpportunityCount = 0;
        if (q < 1.0) {
            for (int i = 0; i < n; i++) {
                // the recipient always observes interaction with no misperception
                if (i == recipient) {
                    super.incrementImage(i, donor);
                } else {
                    // check whether interaction observed
                    if (i != donor && rand.nextDouble() < q) {
                        // if misperceived as defection
                        if (ep > 0.0 && DonationGame.rand.nextDouble() < ep) {
                            tmpGenerosityOpportunityCount += 1;
                            // assessment generosity check
                            if (DonationGame.rand.nextDouble() < g1) {
                                super.incrementImage(i, donor);
                                tmpGenerosityCount += 1;
                            } else {
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
                // the recipient always observes interaction with no misperception
                if (i == recipient) {
                    super.incrementImage(i, donor);
                } else {
                    if (i != donor) {
                        // if misperceived as defection
                        if (ep > 0.0 && DonationGame.rand.nextDouble() < ep) {
                            tmpGenerosityOpportunityCount += 1;
                            // assessment generosity check
                            if (DonationGame.rand.nextDouble() < g1) {
                                super.incrementImage(i, donor);
                                tmpGenerosityCount += 1;
                            } else {
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
        return new int[] {tmpGenerosityCount, tmpGenerosityOpportunityCount};
    }

    private int[] defectImageUpdate(int donor, int recipient) {
        int tmpGenerosityCount = 0;
        int tmpGenerosityOpportunityCount = 0;
        if (q < 1.0) {
            for (int i = 0; i < n; i++) {
                // the recipient always observes interaction with no misperception
                if (i == recipient) {
                    super.decrementImage(i, donor);
                } else {
                    // check whether interaction observed
                    if (i != donor && rand.nextDouble() < q) {
                        // if misperceived as cooperation
                        if (ep > 0.0 && DonationGame.rand.nextDouble() < ep) {
                            super.incrementImage(i, donor);
                        } else {
                            tmpGenerosityOpportunityCount += 1;
                            // assessment generosity check
                            if (DonationGame.rand.nextDouble() < g1) {
                                super.incrementImage(i, donor);
                                tmpGenerosityCount += 1;
                            } else {
                                super.decrementImage(i, donor);
                            }
                            super.decrementImage(i, donor);
                        }
                    }
                }
            }
        }
        else {
            for (int i = 0; i < n; i++) {
                // the recipient always observes interaction with no misperception
                if (i == recipient) {
                    super.decrementImage(i, donor);
                } else {
                    if (i != donor) {
                        // if misperceived as cooperation
                        if (ep > 0.0 && DonationGame.rand.nextDouble() < ep) {
                            super.incrementImage(i, donor);
                        } else {
                            tmpGenerosityOpportunityCount += 1;
                            // assessment generosity check
                            if (DonationGame.rand.nextDouble() < g1) {
                                super.incrementImage(i, donor);
                                tmpGenerosityCount += 1;
                            } else {
                                super.decrementImage(i, donor);
                            }
                            super.decrementImage(i, donor);
                        }
                    }
                }
            }
        }
        return new int[] {tmpGenerosityCount, tmpGenerosityOpportunityCount};
    }

            
    public TickResults tick() {
        int cooperationCount = 0;
        int[] tmpCooperateResults;
        int[] tmpDefectResults;
        int actionGenerosityCount = 0;
        int actionGenerosityOpportunityCount = 0;
        int assessmentGenerosityCount = 0;
        int assessmentGenerosityOpportunityCount = 0;
        for (int i = 0; i < m; i++) {
            int donor = DonationGame.rand.nextInt(n);
            int recipient = DonationGame.rand.nextInt(n);
            while (recipient == donor) {
                recipient = DonationGame.rand.nextInt(n);
            }
            double imageScore = super.getImageScore(donor, recipient);
            boolean intendToDonate;
            boolean generous = false;
            if (imageScore >= strategies[donor]) {
                intendToDonate = true;
            } else {
                intendToDonate = false;
                actionGenerosityOpportunityCount += 1;
            }
            // generosity check
            if (!intendToDonate && DonationGame.rand.nextDouble() < g2) {
                intendToDonate = true;
                generous = true;
            }
            // impose action noise
            if (intendToDonate && ea > 0.0 && DonationGame.rand.nextDouble() < ea) {
                intendToDonate = false;
                generous = true;
            }
            // perform the intended action
            if (intendToDonate) {
                rewards[donor] -= DonationGame.c;
                rewards[recipient] += DonationGame.b;
                if (generous) {
                    actionGenerosityCount += 1;
                }
                tmpCooperateResults = cooperateImageUpdate(donor, recipient);
                assessmentGenerosityCount += tmpCooperateResults[0];
                assessmentGenerosityOpportunityCount += tmpCooperateResults[1];
                cooperationCount += 1;
            } else {
                tmpDefectResults = defectImageUpdate(donor, recipient);
                assessmentGenerosityCount += tmpDefectResults[0];
                assessmentGenerosityOpportunityCount += tmpDefectResults[1];
            }
            if (preventNegativePayoffs) {
                rewards[donor] += 0.1;
                rewards[recipient] += 0.1;
            }
        }
        TickResults results = new TickResults((double) cooperationCount / (double) m);
        results.setGenerosityTickResults(actionGenerosityOpportunityCount, actionGenerosityCount,
            assessmentGenerosityOpportunityCount, assessmentGenerosityCount);
        return results;
    }
}
