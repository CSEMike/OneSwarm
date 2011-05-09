package edu.washington.cs.oneswarm.ui.gwt.rpc;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

public class BackendTask implements IsSerializable {
    int taskID;
    String shortname;
    String summary;
    String progress;
    Date started;
    boolean good;
    IsSerializable result;

    public BackendTask() {
    }

    public int getTaskID() {
        return taskID;
    }

    public void setTaskID(int taskID) {
        this.taskID = taskID;
    }

    public String getShortname() {
        return shortname;
    }

    public void setShortname(String shortname) {
        this.shortname = shortname;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getProgress() {
        return progress;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }

    public Date getStarted() {
        return started;
    }

    public void setStarted(Date started) {
        this.started = started;
    }

    public boolean isGood() {
        return good;
    }

    public IsSerializable getResult() {
        return result;
    }

    public void setResult(IsSerializable inResult) {
        result = inResult;
    }

    public void setGood(boolean good) {
        this.good = good;
    }

    public String toString() {
        return getTaskID() + " " + getShortname() + " " + getSummary();
    }
}
