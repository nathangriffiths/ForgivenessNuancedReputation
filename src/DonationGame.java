import java.util.random.*;
import java.util.stream.DoubleStream;
import java.util.Arrays;
import java.text.DecimalFormat;

public class DonationGame {
    int n;
    int m;
    double q;
    double mr;
    int[] strategies;
    int s_lower_bound;
    int s_upper_bound;
    int k_lower_bound;
    int k_upper_bound;
    double[][] imageScores;
    double[] rewards;
    boolean preventNegativePayoffs;
    boolean learning;
    double beta;
    protected static RandomGenerator rand = RandomGenerator.of("Xoroshiro128PlusPlus");
    protected final static double b = 1;
    protected final static double c = 0.1;

    protected static final DecimalFormat df = new DecimalFormat("0.00");


    public DonationGame(int n, int m, double q, double mr, int s_lower_bound, 
        int s_upper_bound, boolean preventNegativePayoffs, boolean learning, double beta) {        
        this.n = n;
        this.m = m;
        this.q = q;
        this.mr = mr;
        this.s_lower_bound = s_lower_bound;
        this.s_upper_bound = s_upper_bound;
        this.k_lower_bound = s_lower_bound;
        this.k_upper_bound = s_upper_bound + 1;
        this.preventNegativePayoffs = preventNegativePayoffs;
        this.learning = learning;
        this.beta = beta;
        this.strategies = new int[n];
        for (int i = 0; i < strategies.length; i++) {
            // NB strategy upper bound is 1 greater than image score upper bound
            // and RandomGenerator.nextInt() excludes upper bound
            strategies[i] = rand.nextInt(k_lower_bound, k_upper_bound + 1);
        }
        this.imageScores = new double[n][n];
        this.rewards = new double[n];
    }

    public int[] getStrategies() {
        return strategies;
    }

    public double[][] getImageScores() {
        return imageScores;
    }

    public double[] getRewards() {
        return rewards;
    }

    public double getAverageReward() {
        return Arrays.stream(rewards).average().orElse(Double.NaN);
    }

    public double getAverageImage() {
       return Arrays.stream(imageScores).flatMapToDouble(Arrays::stream).average().orElse(Double.NaN);
    }

    public double getAverageDonationStrategy() {
        return Arrays.stream(strategies).average().orElse(Double.NaN);
    }

    private double getImageScore(int maintainer, int target) {
        return imageScores[maintainer][target];
    }

    static void printMatrix(double[][] grid) {
        for(int r=0; r<grid.length; r++) {
           for(int c=0; c<grid[r].length; c++)
               System.out.print(df.format(grid[r][c]) + " ");
           System.out.println();
        }
    }
    static void printMatrix(double[] grid) {
        for(int c=0; c<grid.length; c++)
            System.out.print(df.format(grid[c]) + " ");
        System.out.println();
    }
    
    protected void incrementImage(int maintainer, int donor) {
        imageScores[maintainer][donor] += 1.0;
        if (imageScores[maintainer][donor] > (double) s_upper_bound) {
            imageScores[maintainer][donor] = (double) s_upper_bound;    
        }
    }

    protected void incrementImage(int maintainer, int donor, double delta) {
        imageScores[maintainer][donor] += delta;
        if (imageScores[maintainer][donor] > (double) s_upper_bound) {
            imageScores[maintainer][donor] = (double) s_upper_bound;    
        }
    }

    protected void decrementImage(int maintainer, int donor) {
        imageScores[maintainer][donor] -= 1.0;
        if (imageScores[maintainer][donor] < (double) s_lower_bound) {
            imageScores[maintainer][donor] = (double) s_lower_bound;    
        }
    }

    private void cooperateImageUpdate(int donor) {
        if (q < 1.0) {
            for (int i = 0; i < n; i++) {
                if (i != donor && rand.nextDouble() < q) {
                    incrementImage(i, donor);
                }
            }
        }
        else {
            for (int i = 0; i < n; i++) {
                if (i != donor) {
                    incrementImage(i, donor);
                }
            }
        }
    }

    private void defectImageUpdate(int donor) {
        if (q < 1.0) {
            for (int i = 0; i < n; i++) {
                if (i != donor && rand.nextDouble() < q) {
                    decrementImage(i, donor);
                }
            }
        }
        else {
            for (int i = 0; i < n; i++) {
                if (i != donor) {
                    decrementImage(i, donor);
                }
            }
        }
    }

    public TickResults tick() {
        int cooperationCount = 0;
        for (int i = 0; i < m; i++) {
            int donor = rand.nextInt(n);
            int recipient = rand.nextInt(n);
            while (recipient == donor) {
                recipient = rand.nextInt(n);
            }
            double imageScore = getImageScore(donor, recipient);
            if (imageScore >= strategies[donor]) {
                rewards[donor] -= c;
                rewards[recipient] += b;
                cooperateImageUpdate(donor);
                cooperationCount += 1;
            } else {
                defectImageUpdate(donor);
            }
            if (preventNegativePayoffs) {
                rewards[donor] += 0.1;
                rewards[recipient] += 0.1;
            }
        }
        return new TickResults((double) cooperationCount / (double) m);
    }

    protected double[] scaleRewards() {
        boolean debug = false;
        double[] result = rewards;
        if (debug) System.out.println("x1 " + Arrays.toString(result));
        double min = Arrays.stream(rewards).summaryStatistics().getMin();
        if (min < 0.0) {
            if (debug) System.out.println("min < 0.0");
            double delta = min;
            result = Arrays.stream(rewards).map(i -> i - delta).toArray();
            min = Arrays.stream(result).summaryStatistics().getMin();
            if (debug) System.out.println("new min " + min);
        } 
        if (debug) System.out.println("x2 " + Arrays.toString(result));

        if (min == 0) {
            if (debug) System.out.println("min == 0.0");
            double delta = 0.1;
            result = Arrays.stream(result).map(i -> i + delta).toArray();
        }
        if (debug) System.out.println("x3 " + Arrays.toString(result));

        return result;
    }

    public void mutation() {
        if (mr == 0.0) return;
        for (int i = 0; i < n; i++) {
            if (rand.nextDouble() < mr) {
                int k = rand.nextInt(k_lower_bound, k_upper_bound + 1);
                while (k == strategies[i]) {
                    k = rand.nextInt(k_lower_bound, k_upper_bound + 1);
                }
                strategies[i] = k;
            }
        }
    }

    public static int weightedRandomChoice(double[] rewardsScaled, double totalReward) {
        int selected = 0;
        double total = rewardsScaled[0];
        for (int i = 1; i < rewardsScaled.length; i++) {
            total += rewardsScaled[i];            
            if( rand.nextDouble() <= (rewardsScaled[i] / total)) selected = i;
        }
    
        return selected;    
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
        int[] newStrategies = new int[n];
        for (int i = 0; i < n; i++) {
            newStrategies[i] = strategies[weightedRandomChoice(rewardsScaled, totalReward)];
        }
        strategies = newStrategies;
        Arrays.fill(rewards, 0.0);
        Arrays.stream(imageScores).forEach(a -> Arrays.fill(a, 0));
    }

    public void imitationLearning() {
        int[] newStrategies = new int[n];
        int j;
        for (int i = 0; i < n; i++) {
            j = rand.nextInt(n);
            while (j == i) {
                j = rand.nextInt(n);
            }
            if (DonationGame.rand.nextDouble() < Math.pow((1 + Math.exp(beta * (rewards[i] - rewards[j]))), -1)) {
               newStrategies[i] = strategies[j];
            } else {
                newStrategies[i] = strategies[i];
            }
        }
        strategies = newStrategies;
        Arrays.fill(rewards, 0.0);
        Arrays.stream(imageScores).forEach(a -> Arrays.fill(a, 0));
    }

}
