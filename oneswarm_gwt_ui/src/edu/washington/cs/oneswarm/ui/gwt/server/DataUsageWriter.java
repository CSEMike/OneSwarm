package edu.washington.cs.oneswarm.ui.gwt.server;

import java.io.File;
import java.io.FileWriter;

public class DataUsageWriter extends Thread implements Runnable {
    long mNextUpdate = System.currentTimeMillis();
    String writeData = new String();
    File storage;
    boolean running = false;

    public DataUsageWriter(String initialData, File storagelocation) {
        writeData = initialData;
        storage = storagelocation;
    }

    public void updateData(String newData) {
        writeData = newData;
    }

    public boolean isRunning() {
        return running;
    }

    Thread datawriter = new Thread(new Runnable() {
        public void run() {
            while (true) {
                try {
                    FileWriter out = new FileWriter(storage);
                    out.write(writeData);
                    out.close();

                } catch (Exception E) {
                    E.printStackTrace();
                }
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    });

    public void run() {
        datawriter.start();
        running = true;
    }
}
