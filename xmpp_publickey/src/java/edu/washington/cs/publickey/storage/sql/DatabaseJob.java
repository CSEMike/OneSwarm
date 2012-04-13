package edu.washington.cs.publickey.storage.sql;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.PriorityBlockingQueue;

@SuppressWarnings("unchecked")
abstract class DatabaseJob<T> implements Callable<T> ,Comparable<DatabaseJob>{

	public final static int PRIO_INTERACTIVE = 10;
	public final static int PRIO_LOW = 0;
	private final int prio;
	private final long startTime;

	public DatabaseJob(int prio) {
		this.prio = prio;
		this.startTime = System.currentTimeMillis();
	}

	public int getPrio() {
		return prio;
	}

	public long getTime() {
		return this.startTime;
	}

	public int compareTo(DatabaseJob o) {
		if (o.getPrio() > this.getPrio()) {
			// o is better, return -1
			return -1;
		} else if (o.getPrio() < this.getPrio()) {
			return 1;
		} else {
			if (o.getTime() < this.getTime()) {
				return 1;
			} else if (o.getTime() > this.getTime()) {
				return -1;
			} else {
				return 0;
			}
		}
	}

	public String toString() {
		return prio + " " + startTime;
	}

	public static void main(String[] args) throws InterruptedException {
		Random r = new Random();
		PriorityBlockingQueue<DatabaseJob<Object>> jobs = new PriorityBlockingQueue<DatabaseJob<Object>>();

		for (int i = 0; i < 100; i++) {
			Thread.sleep(2);
			jobs.add(new DatabaseJob<Object>(r.nextInt(10)) {

				public Object call() throws Exception {
					// TODO Auto-generated method stub
					return null;
				}

			});
		}

		while (jobs.peek() != null) {
			System.out.println(jobs.poll());
		}

	}
}
