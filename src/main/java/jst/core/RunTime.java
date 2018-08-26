package jst.core;

public class RunTime {

    private long startTime;

    private long endTime;

    private int iter;

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public void setIter(int iter) {
        this.iter = iter;
    }

    public double totalTimeSeconds(){
        return endTime-startTime;
    }

    public double timeEachGibbsSampling(){
        return (endTime-startTime)*1.0/iter;
    }

}
