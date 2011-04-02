package edu.washington.cs.oneswarm.f2f.dht;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.google.common.collect.Lists;

import edu.washington.cs.oneswarm.f2f.dht.CHTClientUDP.CHTCallback;
import edu.washington.cs.oneswarm.ui.gwt.rpc.CommunityRecord;
import edu.washington.cs.oneswarm.ui.gwt.server.community.CHTPutOp;
import edu.washington.cs.oneswarm.ui.gwt.server.community.CommunityServerManager;

public class CHTClientHTTP implements CHTClientInterface {

	private final String url;
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

	List<PutOp> pendingPuts = Collections.synchronizedList(new ArrayList<PutOp>());
	List<GetOp> pendingGets = Collections.synchronizedList(new ArrayList<GetOp>());
	private final CommunityRecord record;

	public CHTClientHTTP(String url) {
		this.url = url;
		batchFlusher = new Timer("Batch flusher - " + url, true);
		batchFlusher.schedule(new TimerTask() {
			@Override
			public void run() {
				flushPuts();
				flushGets();
			}
		}, 1000, 1000);

		record = CommunityServerManager.get().getRecordForUrl(url);
	}

	public void put(byte[] key, byte[] value) throws IOException {
		pendingPuts.add(new PutOp(key, value));
	}

	public void get(byte[] key, CHTCallback callback) {
		pendingGets.add(new GetOp(key, callback));
	}

	private void flushPuts() {
		List<byte[]> keys = Lists.newArrayList();
		List<byte[]> values = Lists.newArrayList();
		while (!pendingPuts.isEmpty()) {
			PutOp op = pendingPuts.remove(0);
			keys.add(op.key);
			values.add(op.val);
		}
		new CHTPutOp(record, keys, values).start();
	}

	private void flushGets() {

	}
}
