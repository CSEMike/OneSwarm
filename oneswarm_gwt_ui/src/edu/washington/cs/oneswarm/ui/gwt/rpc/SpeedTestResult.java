package edu.washington.cs.oneswarm.ui.gwt.rpc;

import java.util.HashMap;

import com.google.gwt.user.client.rpc.IsSerializable;

public class SpeedTestResult implements IsSerializable {

    private boolean closed;

    private boolean completed;

    private int goodServers;

    private double localEstimate;

    private double progress;

    private double remoteEstimate;

    private int totalServers;

    public SpeedTestResult() {

    }

    public SpeedTestResult(HashMap<String, Double> map) {
        progress = map.get("progress");
        localEstimate = map.get("local");
        remoteEstimate = map.get("remote");
        completed = map.get("completed") > 0;
        closed = map.get("closed") > 0;
        goodServers = map.get("good_servers").intValue();
        totalServers = map.get("total_servers").intValue();
    }

    public int getGoodServers() {
        return goodServers;
    }

    public double getLocalEstimate() {
        return localEstimate;
    }

    public double getProgress() {
        return progress;
    }

    public double getRemoteEstimate() {
        return remoteEstimate;
    }

    public int getTotalServers() {
        return totalServers;
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void setGoodServers(int goodServers) {
        this.goodServers = goodServers;
    }

    public void setLocalEstimate(double localEstimate) {
        this.localEstimate = localEstimate;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public void setRemoteEstimate(double remoteEstimate) {
        this.remoteEstimate = remoteEstimate;
    }

    public void setTotalServers(int totalServers) {
        this.totalServers = totalServers;
    }

    public double getEstimatedUploadRate() {
        // go for the remote estimate unless it seems completely off
        if (localEstimate < 2 * remoteEstimate && localEstimate > 0.5 * remoteEstimate) {
            return remoteEstimate;
        } else {
            return localEstimate;
        }
    }
}
