public class TickResults {
    public double ci;
    public int g_actionOp;
    public int g_actionCnt;
    public int g_assessmentOp;
    public int g_assessmentCnt;
    public int f_actionOp;
    public int f_actionCnt;
    public int f_assessmentOp;
    public int f_assessmentCnt;
    public int f_ajdOp;
    public int f_ajdCnt;

    public TickResults(double ci) {
        this.ci = ci;
        this.g_actionOp = 0;
        this.g_actionCnt = 0;
        this.g_assessmentOp = 0;
        this.g_assessmentCnt = 0;
        this.f_actionOp = 0;
        this.f_actionCnt = 0;
        this.f_assessmentOp = 0;
        this.f_assessmentCnt = 0;
        this.f_ajdOp = 0;
        this.f_ajdCnt = 0;
    }

    public void setGenerosityTickResults(int actionOp, int actionCnt, int assessmentOp, int assessmentCnt) {
        this.g_actionOp = actionOp;
        this.g_actionCnt = actionCnt;
        this.g_assessmentOp = assessmentOp;
        this.g_assessmentCnt = assessmentCnt;
    }

    public void setForgivenessTickResults(int actionOp, int actionCnt, int assessmentOp, int assessmentCnt, 
        int ajdOp, int ajdCnt) {
            this.f_actionOp = actionOp;
            this.f_actionCnt = actionCnt;
            this.f_assessmentOp = assessmentOp;
            this.f_assessmentCnt = assessmentCnt;
            this.f_ajdOp = ajdOp;
            this.f_ajdCnt = ajdCnt;

            if (f_ajdCnt > f_ajdOp) {
                System.out.println("X7");
                System.exit(0);
            }
        }

}