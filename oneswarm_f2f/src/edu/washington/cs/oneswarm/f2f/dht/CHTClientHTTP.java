package edu.washington.cs.oneswarm.f2f.dht;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import com.google.common.collect.Lists;

import edu.washington.cs.oneswarm.ui.gwt.rpc.CommunityRecord;
import edu.washington.cs.oneswarm.ui.gwt.server.community.CHTGetOp;
import edu.washington.cs.oneswarm.ui.gwt.server.community.CHTPutOp;
import edu.washington.cs.oneswarm.ui.gwt.server.community.CommunityServerManager;

public class CHTClientHTTP implements CHTClientInterface {

    private static Logger logger = Logger.getLogger(CHTClientHTTP.class.getName());

    Timer batchFlusher;

    class PutOp {
        public byte[] key;
        public byte[] val;

        public PutOp(byte[] key, byte[] val) {
            this.key = key;
            this.val = val;
        }
    }

    class GetOp {
        public byte[] key;
        CHTCallback callback;

        public GetOp(byte[] key, CHTCallback callback) {
            this.key = key;
            this.callback = callback;
        }
    }

    List<PutOp> pendingPuts = Collections.synchronizedList(new LinkedList<PutOp>());
    List<GetOp> pendingGets = Collections.synchronizedList(new LinkedList<GetOp>());
    private final CommunityRecord record;

    public CHTClientHTTP(String url) {
        this(CommunityServerManager.get().getRecordForUrl(url));
    }

    public CHTClientHTTP(CommunityRecord server) {
        this.record = server;

        batchFlusher = new Timer("Batch GET/PUT flusher - " + record.getCht_path(), true);
        batchFlusher.schedule(new TimerTask() {
            @Override
            public void run() {
                flushPuts();
                flushGets();
            }
        }, 1000, 1000);

    }

    public CommunityRecord getServerRecord() {
        return record;
    }

    @Override
    public void put(byte[] key, byte[] value) throws IOException {
        pendingPuts.add(new PutOp(key, value));
        logger.fine("CHT put queue length: " + pendingPuts.size());
    }

    @Override
    public void get(byte[] key, CHTCallback callback) {
        pendingGets.add(new GetOp(key, callback));
    }

    public void shutdown() {
        logger.fine("Shutting down CHTClientHTTP: " + record);
        batchFlusher.cancel();
    }

    private void flushPuts() {
        List<byte[]> keys = Lists.newArrayList();
        List<byte[]> values = Lists.newArrayList();
        while (!pendingPuts.isEmpty()) {
            PutOp op = pendingPuts.remove(0);
            if (op == null) {
                break;
            }
            keys.add(op.key);
            values.add(op.val);
        }
        if (keys.size() > 0) {
            logger.info("Sending " + keys.size() + " CHT puts to " + record.getRealUrl());
            new CHTPutOp(record, keys, values).start();
        }
    }

    private void flushGets() {
        List<byte[]> keys = Lists.newArrayList();
        List<CHTCallback> callbacks = Lists.newArrayList();
        while (!pendingGets.isEmpty()) {
            GetOp op = pendingGets.remove(0);
            if (op == null) {
                break;
            }
            keys.add(op.key);
            callbacks.add(op.callback);
        }
        if (keys.size() > 0) {
            logger.info("Sending " + keys.size() + " CHT gets to " + record.getCht_path());
            new CHTGetOp(record, keys, callbacks).start();
        }
    }
}
