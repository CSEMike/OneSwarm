package edu.washington.cs.oneswarm.f2f.datagram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatagramRateLimiter {
    public final static Logger logger = Logger.getLogger(DatagramRateLimiter.class.getName());

    protected volatile int availableTokens = 0;

    protected int maxAvailableTokens;

    protected final ArrayList<DatagramRateLimiter> queues = new ArrayList<DatagramRateLimiter>();
    private final Comparator<DatagramRateLimiter> comparator = new Comparator<DatagramRateLimiter>() {
        @Override
        public int compare(DatagramRateLimiter o1, DatagramRateLimiter o2) {
            return o1.getAvailableTokens() - o2.getAvailableTokens();
        }
    };

    /**
     * Add tokens to this bucket.
     * 
     * @param tokens
     *            The maximum number of tokens to add.
     * 
     * @return The number of tokens actually added.
     */
    public int refillBucket(int tokens) {
        int toRefill = Math.min(tokens, maxAvailableTokens - availableTokens);
        availableTokens += toRefill;
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(toString() + ": " + toRefill + " tokens added, available: "
                    + availableTokens);
        }
        return toRefill;
    }

    public synchronized void allocateTokens() {
        logger.finer(toString() + ": allocating tokens");
        Collections.sort(queues, comparator);
        int queueNum = queues.size();
        if (queueNum == 0) {
            logger.finest(toString() + ": no queues, returning");
            return;
        }
        int fairShare = availableTokens / queueNum;
        logger.finest(toString() + ": fair share: " + fairShare);
        for (int i = 0; i < queueNum; i++) {
            DatagramRateLimiter queue = queues.get(i);
            int tokensUsed = queue.refillBucket(fairShare);
            availableTokens -= tokensUsed;
            queue.allocateTokens();
            int queuesLeft = queueNum - (i + 1);
            if (tokensUsed < fairShare && queuesLeft > 0) {
                fairShare = availableTokens / queuesLeft;
                logger.finest(toString() + ": fair share updated: " + fairShare + " available="
                        + availableTokens + " queues_left=" + queuesLeft);
            }
        }
    }

    protected synchronized void addQueue(DatagramRateLimiter queue) {
        queues.add(queue);
        queue.setTokenBucketSize(maxAvailableTokens);
        logger.fine(toString() + ": queue added: " + queue.toString());
    }

    protected synchronized void removeQueue(DatagramRateLimiter queue) {
        queue.transferTokens(this, getAvailableTokens());
        queues.remove(queue);
        logger.fine(toString() + ": queue removed: " + queue.toString());
    }

    public int getAvailableTokens() {
        return availableTokens;
    }

    public int getTokenBucketSize() {
        return this.maxAvailableTokens;
    }

    public void transferTokens(DatagramRateLimiter target, int maxToTransfer) {
        int amount = target.refillBucket(Math.min(maxToTransfer, availableTokens));
        availableTokens -= amount;
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(toString() + ": transfered " + amount + " tokens, target="
                    + target.toString());
        }
    }

    public boolean isFull() {
        return availableTokens == maxAvailableTokens;
    }

    public void setTokenBucketSize(int tokens) {
        this.maxAvailableTokens = tokens;
    }

    @Override
    public String toString() {
        return "RateLimitedQueue, tokens: " + availableTokens + "/" + maxAvailableTokens;
    }
}
