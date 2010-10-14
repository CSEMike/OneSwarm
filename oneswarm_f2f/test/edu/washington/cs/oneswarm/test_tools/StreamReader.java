package edu.washington.cs.oneswarm.test_tools;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * This class reads everything that comes from an inputstream source, and makes
 * is available as either a byte[] calls to the read funtions are blocking,
 * waiting for a read form the inputstream to return -1
 * 
 * @author isdal
 * 
 */

public class StreamReader implements Runnable {

	private InputStream source;

	private Thread t;

	private ArrayList<byte[]> data;

	public StreamReader(InputStream source) {
		this.data = new ArrayList<byte[]>();
		this.source = source;

		t = new Thread(this);
		t.setName("BufferReader");
		t.setDaemon(true);
		t.start();
	}

	public void run() {
		try {
			int read = 0;
			byte[] buffer = new byte[1000];
			while ((read = source.read(buffer, 0, buffer.length)) != -1) {
				byte[] toSave = new byte[read];
				System.arraycopy(buffer, 0, toSave, 0, toSave.length);
				data.add(toSave);
			}
			source.close();
		} catch (IOException e) {
			System.out.println("Buffer reader stopped");
		}
	}

	public byte[] read() throws InterruptedException {

		t.join();

		byte[] allData = new byte[this.length()];
		int pos = 0;
		for (int i = 0; i < data.size(); i++) {
			byte[] b = data.get(i);
			System.arraycopy(b, 0, allData, pos, b.length);
			pos += b.length;
		}
		return allData;
	}

	public int length() {
		int len = 0;

		for (byte[] element : data) {
			len += element.length;
		}
		return len;
	}
}