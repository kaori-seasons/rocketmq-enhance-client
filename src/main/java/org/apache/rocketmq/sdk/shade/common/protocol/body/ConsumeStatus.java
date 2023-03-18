package org.apache.rocketmq.sdk.shade.common.protocol.body;

public class ConsumeStatus {
    private double pullRT;
    private double pullTPS;
    private double consumeRT;
    private double consumeOKTPS;
    private double consumeFailedTPS;
    private long consumeFailedMsgs;

    public double getPullRT() {
        return this.pullRT;
    }

    public void setPullRT(double pullRT) {
        this.pullRT = pullRT;
    }

    public double getPullTPS() {
        return this.pullTPS;
    }

    public void setPullTPS(double pullTPS) {
        this.pullTPS = pullTPS;
    }

    public double getConsumeRT() {
        return this.consumeRT;
    }

    public void setConsumeRT(double consumeRT) {
        this.consumeRT = consumeRT;
    }

    public double getConsumeOKTPS() {
        return this.consumeOKTPS;
    }

    public void setConsumeOKTPS(double consumeOKTPS) {
        this.consumeOKTPS = consumeOKTPS;
    }

    public double getConsumeFailedTPS() {
        return this.consumeFailedTPS;
    }

    public void setConsumeFailedTPS(double consumeFailedTPS) {
        this.consumeFailedTPS = consumeFailedTPS;
    }

    public long getConsumeFailedMsgs() {
        return this.consumeFailedMsgs;
    }

    public void setConsumeFailedMsgs(long consumeFailedMsgs) {
        this.consumeFailedMsgs = consumeFailedMsgs;
    }
}
