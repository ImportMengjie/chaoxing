package pers.cz.chaoxing.common.task;

public class ExamData extends TaskData<ExamDataProperty> {
    private String enc;
    private boolean job;
    private String utEnc;

    public String getEnc() {
        return enc;
    }

    public void setEnc(String enc) {
        this.enc = enc;
    }

    public boolean isJob() {
        return job;
    }

    public void setJob(boolean job) {
        this.job = job;
    }

    public String getUtEnc() {
        return utEnc;
    }

    public void setUtEnc(String utEnc) {
        this.utEnc = utEnc;
    }
}