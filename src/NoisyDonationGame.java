import java.util.Arrays;

public class NoisyDonationGame extends DonationGame {
    double er;
    double ea;
    double ep;

    public NoisyDonationGame(int n, int m, double q, double mr, 
        int s_lower_bound, int s_upper_bound, boolean preventNegativePayoffs, 
        double er, double ea, double ep, boolean learning, double beta) {        
            super(n, m, q, mr, s_lower_bound, s_upper_bound, preventNegativePayoffs, learning, beta);
            this.er = er;
            this.ea = ea;
            this.ep = ep;
        }

    protected double getImageScore(int maintainer, int target) {
        if (er > 0.0 && DonationGame.rand.nextDouble() < er) {
            return DonationGame.rand.nextDouble(s_lower_bound, s_upper_bound);
        } else {
            return imageScores[maintainer][target];
        }
    }
        
    private void cooperateImageUpdate(int donor, int recipient) {
        if (q < 1.0) {
            for (int i = 0; i < n; i++) {
                if (i == recipient) {
                    super.incrementImage(i, donor);
                } else {
                    if (i != donor && rand.nextDouble() < q) {
                        if (ep > 0.0 && DonationGame.rand.nextDouble() < ep) {
                            super.decrementImage(i, donor);
                        } else {
                            super.incrementImage(i, donor);
                        }
                    }
                }
            }
        }
        else {
            for (int i = 0; i < n; i++) {
                if (i == recipient) {
                    super.incrementImage(i, donor);
                } else {
                    if (i != donor) {
                        if (ep > 0.0 && DonationGame.rand.nextDouble() < ep) {
                            super.decrementImage(i, donor);
                        } else {
                            super.incrementImage(i, donor);
                        }
                    }
                }
            }
        }
    }

    private void defectImageUpdate(int donor, int recipient) {
        if (q < 1.0) {
            for (int i = 0; i < n; i++) {
                if (i == recipient) {
                    super.decrementImage(i, donor);
                } else {
                    if (i != donor && rand.nextDouble() < q) {
                        if (ep > 0.0 && DonationGame.rand.nextDouble() < ep) {
                            super.incrementImage(i, donor);
                        } else {
                            super.decrementImage(i, donor);
                        }
                    }
                }
            }
        }
        else {
            for (int i = 0; i < n; i++) {
                if (i == recipient) {
                    super.decrementImage(i, donor);
                } else {
                    if (i != donor) {
                        if (ep > 0.0 && DonationGame.rand.nextDouble() < ep) {
                            super.incrementImage(i, donor);
                        } else {
                            super.decrementImage(i, donor);
                        }
                    }
                }
            }
        }
    }

    
    public TickResults tick() {
        int cooperationCount = 0;
        for (int i = 0; i < m; i++) {
            int donor = DonationGame.rand.nextInt(n);
            int recipient = DonationGame.rand.nextInt(n);
            while (recipient == donor) {
                recipient = DonationGame.rand.nextInt(n);
            }

            double imageScore = getImageScore(donor, recipient);

            boolean intendToDonate;
            if (imageScore >= strategies[donor]) {
                intendToDonate = true;
            } else {
                intendToDonate = false;
            }
            // impose action noise
            if (intendToDonate && ea > 0.0 && DonationGame.rand.nextDouble() < ea) {
                intendToDonate = false;
            }
            // perform the intended action
            if (intendToDonate) {
                rewards[donor] -= DonationGame.c;
                rewards[recipient] += DonationGame.b;
                cooperateImageUpdate(donor, recipient);
                cooperationCount += 1;
            } else {
                defectImageUpdate(donor, recipient);
            }
            if (preventNegativePayoffs) {
                rewards[donor] += 0.1;
                rewards[recipient] += 0.1;
            }
        }
        return new TickResults((double) cooperationCount / (double) m);
    }
    
}
