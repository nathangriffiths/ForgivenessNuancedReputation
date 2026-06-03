public class UpdateResults {
    public int fAssessmentOpportunityCount;
    public int fAssessmentCount;
    public int fAjdOpportunityCount;
    public int fAjdCount;
    
    public UpdateResults() {
        int fAssessmentOpportunityCount = 0;
        int fAssessmentCount = 0;
        int fAjdOpportunityCount = 0;    
        int fAjdCount = 0;
    }

    void setUpdateResults(int tmpAssessmentOp, int tmpAssessmentCnt, int tmpAjdOp, int tmpAjdCnt) {
        this.fAssessmentOpportunityCount = tmpAssessmentOp;
        this.fAssessmentCount = tmpAssessmentCnt;
        this.fAjdOpportunityCount = tmpAjdOp;
        this.fAjdCount = tmpAjdCnt;
        if (tmpAjdCnt > tmpAjdOp) {
            System.out.println("X3");
            System.exit(0);
        }
    }
}