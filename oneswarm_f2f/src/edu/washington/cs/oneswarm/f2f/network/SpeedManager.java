/**
 * 
 */
package edu.washington.cs.oneswarm.f2f.network;

import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

class SpeedManager {
    private static Logger logger = Logger.getLogger(SpeedManager.class.getName());
    /**
     * These constants define startup cases for which queueing is immediate
     * (i.e., we don't need to bother checking the queue size because we're
     * uploading so slowly or there's so little data in-queue)
     */
    public static final int MAX_SPEED0_QUEUE_LEN_BYTES = 100 * 1024;
    public final static double SPEED0 = 500;

    /**
     * We keep a history of the amount of data added that extends this far into
     * the past to compute the average upload speed. (This is the window of that
     * average)
     */
    public final static int NUM_MS_AVERAGE = 2000;

    /**
     * These support the average speed computation described above and store
     * history bounded by NUM_MS_AVERAGE
     */
    private int bytesSentLastAvgWindow = 0;
    /**
     * NOTE: --- timestamps is used to synchronize access to BOTH packetSizes
     * and timestamps ---
     */
    public final LinkedList<Integer> packetSizes = new LinkedList<Integer>();
    public final LinkedList<Long> timestamps = new LinkedList<Long>();

    /**
     * The finest level of logging prints out data regarding every message,
     * which is too fine-grained for many real-world debugging cases. For these,
     * we support periodic logging at the FINER level. This defines the period.
     */
    public static final long FINER_LOG_REPORTING_PERIOD = 5000;
    private long lastPeriodicLog = 0;

    // private final QueueManager queueManager;
    private final boolean global;

    public SpeedManager(QueueManager queueManager, boolean global) {
        // this.queueManager = queueManager;
        this.global = global;
    }

    /**
     * @returns true if we can queue more packets, false if the queue is full
     */
    boolean canQueuePacket(int currentQueueLengthBytes, int maxQueueLenMs) {

        if (logger.isLoggable(Level.FINER)) {
            if (lastPeriodicLog + FINER_LOG_REPORTING_PERIOD < System.currentTimeMillis()) {
                printPeriodicLog(currentQueueLengthBytes);
                lastPeriodicLog = System.currentTimeMillis();
            }
        }

        double currentUploadSpeed = getCurrentUploadSpeed();
        logger.finest("canQueue? speed=" + currentUploadSpeed + " queue length="
                + currentQueueLengthBytes);

        /**
         * If the queue length is tiny, immediately accept.
         */
        if (currentQueueLengthBytes < QueueManager.MIN_GLOBAL_QUEUE_LEN_BYTES) {
            logger.finest("canQueue: yes queue, " + currentQueueLengthBytes + "bytes < "
                    + maxQueueLenMs + "bytes");
            return true;
        }
        /**
         * If we're uploading slowly and the queue length isn't too large
         * (100k), also immediately accept.
         */
        else if (currentUploadSpeed < SPEED0
                && currentQueueLengthBytes < MAX_SPEED0_QUEUE_LEN_BYTES) {
            logger.finest("canQueue: yes, speed=" + currentUploadSpeed + " < " + SPEED0);
            return true;
        }
        /**
         * The previous cases are optimizations.
         * 
         * THE COMMON CASE: We're uploading at a reasonable rate and the global
         * queue has a fair bit of data. Here, we enforce our global queue
         * resource consumption limits: ** no more than maxQueueLenMs worth of
         * data ** This bounds the time data stays in the queue at our current
         * upload speed.
         */
        else {
            double currentQueueLenMs = (currentQueueLengthBytes / currentUploadSpeed) * 1000.0;
            if (currentQueueLenMs < maxQueueLenMs) {
                logger.finest("yes queue, " + currentQueueLenMs + "ms < " + maxQueueLenMs + "ms");
                return true;
            } else {
                logger.finest("no, queue len ms=" + currentQueueLenMs + " > " + maxQueueLenMs);
                return false;
            }
        }
    }

    public void dataUploaded(int bytes) {
        synchronized (timestamps) {
            timestamps.addLast(System.currentTimeMillis());
            packetSizes.addLast(bytes);
        }

        bytesSentLastAvgWindow += bytes;
    }

    /**
     * This function is only used when the Java logging level at least FINER
     * granularity.
     */
    private void printPeriodicLog(int currentQueueLengthBytes) {
        synchronized (timestamps) {
            double currentUploadSpeed = getCurrentUploadSpeed();
            double currentQueueLenMs = (currentQueueLengthBytes / currentUploadSpeed) * 1000.0;

            String tstamp = "";
            if (timestamps.size() > 0) {
                /**
                 * If this has positive size, we can compute the actual average
                 * window (how closely does it match target?) Since we just
                 * called getCurrentUploadSpeed(), all of the old packet
                 * sizes/times have been pruned
                 */
                tstamp = " averager_window=" + (System.currentTimeMillis() - timestamps.get(0));
            }

            String bytesSuf = "";
            if (packetSizes.size() > 0) {
                bytesSuf = " oldest_bytes=" + packetSizes.get(0);
            }

            logger.finer("Speed manager log: global=" + global + " speed=" + currentUploadSpeed
                    + " len(bytes)=" + currentQueueLengthBytes + " len(time)="
                    + (Double.isNaN(currentQueueLenMs) ? "0" : currentQueueLenMs)
                    + " timestamps.size()=" + timestamps.size() + tstamp + " packetSizes.size()="
                    + packetSizes.size() + bytesSuf);

            if (timestamps.size() != packetSizes.size()) {
                logger.warning("** Timestmps and packetSizes have different sizes. (These should be coupled 1-1)");
            }
        }
    }

    public double getCurrentUploadSpeed() {
        double currentSpeedBytesPerSecond = 0;
        synchronized (timestamps) {
            // first, remove the timestamps that are to old
            while (timestamps.size() > 0
                    && timestamps.getFirst() + NUM_MS_AVERAGE < System.currentTimeMillis()) {
                timestamps.removeFirst();
                bytesSentLastAvgWindow -= packetSizes.removeFirst();
            }

            // then calculate the speed
            if (bytesSentLastAvgWindow > 0) {
                if (timestamps.size() >= 1) {
                    long windowMs = System.currentTimeMillis() - timestamps.getFirst();
                    if (windowMs > 0) {
                        currentSpeedBytesPerSecond = ((double) 1000 * bytesSentLastAvgWindow)
                                / (double) (windowMs);
                        if (logger.isLoggable(Level.FINEST)) {
                            logger.finest("current upload speed: " + currentSpeedBytesPerSecond
                                    + " B/s");
                        }
                    }
                }
            }
            /**
             * This should never happen, and is just error checking code.
             */
            else if (bytesSentLastAvgWindow < 0) {
                logger.warning("** Bytes sent last window was _negative_: "
                        + bytesSentLastAvgWindow);

                /**
                 * Bad juju, reset state.
                 */
                bytesSentLastAvgWindow = 0;
                timestamps.clear();
                packetSizes.clear();
            }
        }
        return currentSpeedBytesPerSecond;
    }
}