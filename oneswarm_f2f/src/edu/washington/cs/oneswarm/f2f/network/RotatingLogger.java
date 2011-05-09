/**
 * 
 */
package edu.washington.cs.oneswarm.f2f.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import org.gudy.azureus2.core3.util.SystemProperties;

class RotatingLogger {

    private static Logger slogger = Logger.getLogger(RotatingLogger.class.getName());
    private LinkedBlockingQueue<String> queuedLines = new LinkedBlockingQueue<String>();

    final String mLogName;

    public void log(String line) {
        if (slogger.isLoggable(Level.FINEST)) {
            queuedLines.add(line);
        }
    }

    File logFile;
    BufferedWriter logWriter;

    private File getLogFileName() {
        Calendar d = Calendar.getInstance();
        DecimalFormat f = new DecimalFormat("00");
        return new File(SystemProperties.getApplicationPath(), mLogName + "_"
                + d.get(Calendar.YEAR) + "-" + f.format(d.get(Calendar.MONTH) + 1) + "-"
                + f.format(d.get(Calendar.DAY_OF_MONTH)) + ".log");
    }

    private void rotateLogs() throws IOException {
        File oldFile = logFile;
        synchronized (RotatingLogger.this) {
            if (logWriter != null) {
                logWriter.close();
            }
            logFile = getLogFileName();
            logWriter = new BufferedWriter(new FileWriter(logFile, true));
            slogger.fine("rotating log, new file: " + logFile.getName());
        }

        /*
         * file rotated, lets gzip the old one
         */

        if (oldFile != null) {
            slogger.fine("gzipping the old file");
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(oldFile));
            final File zippedFile = new File(oldFile.getCanonicalPath() + ".gz");
            BufferedOutputStream out = new BufferedOutputStream(new GZIPOutputStream(
                    new FileOutputStream(zippedFile)));
            byte[] buffer = new byte[1024 * 1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.close();
            in.close();
            slogger.finer("gzipped old log, new size: " + zippedFile.length() + " compression: "
                    + ((100 * zippedFile.length()) / oldFile.length()) + " %");
            slogger.finer("deleting old file: " + oldFile.delete());
        }
    }

    public RotatingLogger(final String logname) {
        mLogName = logname;
        Thread startLaterThread = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(60 * 1000);
                } catch (InterruptedException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

                if (slogger.isLoggable(Level.FINEST)) {
                    slogger.fine("Starting search timing logger");
                    try {
                        rotateLogs();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    Thread t = new Thread(new Runnable() {

                        long lastFlush = 0;

                        public void run() {
                            try {
                                while (true) {
                                    String line = queuedLines.poll(5, TimeUnit.SECONDS);
                                    if (line != null) {
                                        synchronized (RotatingLogger.this) {
                                            logWriter.append(line + "\n");

                                            if (lastFlush + 5000 < System.currentTimeMillis()) {
                                                logWriter.flush();
                                                lastFlush = System.currentTimeMillis();
                                            }
                                        }
                                    }
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    t.setName("RotatingLogger - " + logname);
                    t.setDaemon(true);
                    t.start();

                    Timer logRotator = new Timer("RotatingLoggerTimer - " + logname);
                    Calendar startDate = Calendar.getInstance();

                    startDate.set(Calendar.DAY_OF_YEAR, startDate.get(Calendar.DAY_OF_YEAR) + 1);
                    startDate.set(Calendar.HOUR, 0);
                    startDate.set(Calendar.MINUTE, 1);
                    startDate.set(Calendar.SECOND, 0);
                    startDate.set(Calendar.MILLISECOND, 0);
                    startDate.set(Calendar.AM_PM, Calendar.AM);
                    slogger.finer("next log rotate will be at: " + startDate.getTime());
                    logRotator.scheduleAtFixedRate(new TimerTask() {

                        @Override
                        public void run() {
                            synchronized (RotatingLogger.this) {
                                try {
                                    rotateLogs();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                        }
                    }, startDate.getTime(), 1000 * 60 * 60 * 24);

                }

            }
        });
        startLaterThread.setDaemon(true);
        startLaterThread.setName("StartLaterRotatingLogger - " + logname);
        startLaterThread.start();
    }

    public boolean isEnabled() {
        return slogger.isLoggable(Level.FINEST);
    }
}