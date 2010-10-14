package edu.washington.cs.oneswarm.f2f.network;

import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import junit.framework.TestCase;
import edu.washington.cs.oneswarm.f2f.network.DelayedExecutorService.DelayedExecutor;

public class DelayedExecutorServiceTest extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testQueueLongLongTimerTask() throws InterruptedException {
		final long delay = 300;
		final int slack = 100;

		final ConcurrentLinkedQueue<Long> executionOrder = new ConcurrentLinkedQueue<Long>();
		final long time = System.currentTimeMillis();
		
		DelayedExecutor fixedDelayExecutor = DelayedExecutorService.getInstance().getFixedDelayExecutor(delay);

		int TOTAL_NUM = 1000;
		for (int i = 0; i < TOTAL_NUM; i++) {
			final long expectedTime = System.currentTimeMillis() + delay;
			final long num = i;
			if (i % 10 == 0) {
				Thread.sleep(10);
			}
			fixedDelayExecutor.queue(delay, slack, new TimerTask() {
				@Override
				public void run() {
					executionOrder.add(num);
					if (System.currentTimeMillis() - slack > expectedTime) {
						fail("executing too early");
					}
					long relTime = System.currentTimeMillis() - time;
					long relExpextedTime = expectedTime - time;
					System.out.println(num + " " + relTime + " " + relExpextedTime);
				}
			});
		}

		while (!fixedDelayExecutor.isEmpty()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (TOTAL_NUM != executionOrder.size()) {
			System.out.println("textentrue");
			fail("size error: " + TOTAL_NUM + "!=" + executionOrder.size());
		}

		long prev = -1;
		for (Long l : executionOrder) {
			if (l <= prev) {
				fail("order error");
			}
			prev = l;
		}

	}
}
