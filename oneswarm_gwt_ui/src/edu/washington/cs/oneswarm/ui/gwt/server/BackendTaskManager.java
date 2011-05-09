package edu.washington.cs.oneswarm.ui.gwt.server;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.google.gwt.user.client.rpc.IsSerializable;

import edu.washington.cs.oneswarm.ui.gwt.rpc.BackendTask;
import edu.washington.cs.oneswarm.ui.gwt.server.community.PublishSwarmsThread;

public class BackendTaskManager {

    private static Logger logger = Logger.getLogger(BackendTaskManager.class.getName());

    private static BackendTaskManager inst = new BackendTaskManager();

    private BackendTaskManager() {
    }

    int currID = 0;

    public static BackendTaskManager get() {
        return inst;
    }

    public interface CancellationListener {
        public void cancelled(int inID);
    }

    Map<Integer, BackendTask> task_map = Collections
            .synchronizedMap(new HashMap<Integer, BackendTask>());
    Map<Integer, CancellationListener> cancel_map = Collections
            .synchronizedMap(new HashMap<Integer, CancellationListener>());

    // public BackendTask getTask( long inID ) {
    // return task_map.get(inID);
    // }

    public synchronized int createTask(String shortName, CancellationListener listener) {
        BackendTask task = new BackendTask();
        task.setTaskID(currID);
        task.setShortname(shortName);
        task.setProgress("0%");
        task.setSummary("");
        task.setStarted(new Date());
        task.setGood(true);

        task_map.put(currID, task);
        cancel_map.put(currID, listener);

        currID++;

        logger.fine("Created task: " + task + " (sizes: " + task_map.size() + " / "
                + cancel_map.size() + ")");

        return task.getTaskID();
    }

    public BackendTask getTask(int inID) {
        return task_map.get(inID);
    }

    public synchronized boolean removeTask(int inID) {
        logger.fine("Remove task: " + inID);
        cancel_map.remove(inID);
        return task_map.remove(inID) != null;
    }

    public synchronized void cancelTask(int inID) {
        if (cancel_map.containsKey(inID)) {
            cancel_map.remove(inID).cancelled(inID);
        }
        removeTask(inID);
    }

    public BackendTask[] getTasks() {
        return task_map.values().toArray(new BackendTask[0]);
    }
}
