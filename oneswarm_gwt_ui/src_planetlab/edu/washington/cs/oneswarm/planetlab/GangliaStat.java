package edu.washington.cs.oneswarm.planetlab;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

public class GangliaStat {

	private static final int DEFAULT_INITIAL_DELAY = 60;
	private static final int DEFAULT_UPDATE_PERIOD = 60;
	private static final int DEFAULT_PORT = 8649;
	private final long delay;
	private final long period;
	private int updateCount = 0;
	private final String collector;
	private final int port;

	private LinkedList<StatReporter> reportTasks = new LinkedList<StatReporter>();

	public GangliaStat(String collector) {
		this(collector, DEFAULT_PORT, DEFAULT_UPDATE_PERIOD, DEFAULT_INITIAL_DELAY);
	}

	/**
	 * 
	 * @param collector
	 * @param port
	 *            port of the
	 * @param period
	 *            frequency of updates in seconds
	 * @param delay
	 *            number of seconds to wait before doing the first udpate
	 */
	public GangliaStat(String collector, int port, int period, int delay) {
		this.period = 1000 * period;
		this.delay = 1000 * delay;
		this.port = port;
		this.collector = collector;
		setMonitor();
	}

	private void setMonitor() {
		Timer timer = new Timer("Ganglia Monitor", true);
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				updateCount++;
				synchronized (reportTasks) {
					try {
						InetAddress col = InetAddress.getByName(collector);
						for (StatReporter sr : reportTasks) {
							GMetricSender.send(col, port, sr.getName(), sr.getValue(), sr.getUnit(), GMetricSender.SLOPE_BOTH, 60, 300);
						}
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}, delay, period);
	}

	public void addMetric(StatReporter stat) {
		synchronized (reportTasks) {
			reportTasks.add(stat);
		}
	}

	public abstract static class StatReporter {
		private final String name;
		private final String unit;

		public StatReporter(String name, String unit) {
			this.name = name;
			this.unit = unit;
		}

		public StatReporter(G25Metric metric) {
			this(metric.name, metric.units);
		}

		public String getName() {
			return name;
		}

		public String getUnit() {
			return unit;
		}

		public abstract double getValue();
	}

	public enum G25Metric {
		METRIC_NET_UPLOAD(29, "bytes_out", 300, Type.FLOAT, "bytes/sec", Slope.BOTH, "%.2f"),

		METRIC_NET_DOWNLOAD(30, "bytes_in", 300, Type.FLOAT, "bytes/sec", Slope.BOTH, "%.2f");

		final int key;
		final String name;
		final int tmax;
		final Type type;
		final String units;
		final Slope slope;
		final String fmt;
		final int messageSize;

		G25Metric(int key, String name, int tmax, Type type, String units, Slope slope, String fmt) {
			this.key = key;
			this.name = name;
			this.tmax = tmax;
			this.type = type;
			this.units = units;
			this.slope = slope;
			this.fmt = fmt;
			this.messageSize = type.size;
		}

		enum Type {
			DOUBLE("double", 7, 16), FLOAT("float", 6, 8), INT32("int32", 5, 8), STRING("string", 1, 32), UNINT32("uint32", 4, 8);
			String type;
			int size;
			int code;

			Type(String type, int code, int size) {
				this.type = type;
				this.code = code;
				this.size = size;
			}
		}

		enum Slope {
			BOTH("both", 3), ZERO("zero", 0);
			String slope;
			int key;

			Slope(String s, int key) {
				this.slope = s;
				this.key = key;
			}
		}
	}

	private static class GMetricSender {
		private final static int METRIC_USER_DEFINED_ID = 0;

		public final static int SLOPE_ZERO = 0;
		public final static int SLOPE_POSITIVE = 1;
		public final static int SLOPE_NEGATIVE = 2;
		public final static int SLOPE_BOTH = 3;
		public final static int SLOPE_UNSPECIFIED = 4;

		public final static String VALUE_STRING = "string";
		public final static String VALUE_UNSIGNED_SHORT = "uint16";
		public final static String VALUE_SHORT = "int16";
		public final static String VALUE_UNSIGNED_INT = "uint32";
		public final static String VALUE_INT = "int32";
		public final static String VALUE_FLOAT = "float";
		public final static String VALUE_DOUBLE = "double";

		public static void send(InetAddress address, int port, String name, String value, String type, String units, int slope, int tmax, int dmax) {
			try {
				DatagramSocket socket = new DatagramSocket();
				byte[] buf = write(name, value, type, units, slope, tmax, dmax);
				DatagramPacket p = new DatagramPacket(buf, buf.length, address, port);
				socket.send(p);
			} catch (IOException e) {
				// who cares
			}
		}

		public static void send(InetAddress address, int port, String name, double dvalue, String units, int slope, int tmax, int dmax) {
			try {
				String value = Double.toString(dvalue);
				DatagramSocket socket = new DatagramSocket();
				byte[] buf = write(name, value, VALUE_DOUBLE, units, slope, tmax, dmax);
				DatagramPacket p = new DatagramPacket(buf, buf.length, address, port);
				socket.send(p);
			} catch (IOException e) {
				// who cares
			}
		}

		public static void send(InetAddress address, int port, String name, int dvalue, String units, int slope, int tmax, int dmax) {
			try {
				String value = Integer.toString(dvalue);
				DatagramSocket socket = new DatagramSocket();
				byte[] buf = write(name, value, VALUE_INT, units, slope, tmax, dmax);
				DatagramPacket p = new DatagramPacket(buf, buf.length, address, port);
				socket.send(p);
			} catch (IOException e) {
				// who cares
			}
		}

		public static void send(InetAddress address, int port, G25Metric metric, String value) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);

			try {
				dos.writeInt(METRIC_USER_DEFINED_ID);
				writeXDRString(dos, metric.type.type);
				writeXDRString(dos, metric.name);
				writeXDRString(dos, value);
				writeXDRString(dos, metric.units);
				dos.writeInt(metric.slope.key);
				dos.writeInt(metric.tmax);
				dos.writeInt(metric.tmax);
				byte[] buf = baos.toByteArray();
				DatagramSocket socket = new DatagramSocket();
				DatagramPacket p = new DatagramPacket(buf, buf.length, address, port);
				socket.send(p);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		/*
		 * EVERYTHING BELOW HERE YOU DON"T NEED TO USE
		 */

		private static byte[] write(String name, String value, String type, String units, int slope, int tmax, int dmax) {
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos);
				dos.writeInt(METRIC_USER_DEFINED_ID);
				writeXDRString(dos, type);
				writeXDRString(dos, name);
				writeXDRString(dos, value);
				writeXDRString(dos, units);
				dos.writeInt(slope);
				dos.writeInt(tmax);
				dos.writeInt(dmax);
				return baos.toByteArray();
			} catch (IOException e) {
				// really this is impossible
				return null;
			}
		}

		private static void writeXDRString(DataOutputStream dos, String s) throws IOException {
			dos.writeInt(s.length());
			dos.writeBytes(s);
			int offset = s.length() % 4;
			if (offset != 0) {
				for (int i = offset; i < 4; ++i) {
					dos.writeByte(0);
				}
			}
		}

		public static void main(String args[]) throws Exception {
			InetAddress remote = InetAddress.getByName(args[0]);
			int port = 8649;
			int iter = 0;

			while (true) {
				send(remote, port, "test47", 9.99 + iter++, "req/sec", SLOPE_BOTH, 100, 100);
				System.out.println("updated " + iter);
				Thread.sleep(60 * 1000);

			}
		}
	}

	public static void main(String[] args) throws InterruptedException {
		GangliaStat stat = new GangliaStat("jermaine", DEFAULT_PORT, 10, 0);

		stat.addMetric(new StatReporter("Test", "req/s") {
			double iter = 47;

			@Override
			public double getValue() {
				iter += 1.5;
				return iter;
			}
		});
		stat.addMetric(new StatReporter(G25Metric.METRIC_NET_DOWNLOAD) {
			double iter = 1000;

			@Override
			public double getValue() {
				iter += 1.5 * 1000;
				return iter;
			}
		});
		stat.addMetric(new StatReporter(G25Metric.METRIC_NET_UPLOAD) {
			double iter = 1000;

			@Override
			public double getValue() {
				iter += 1.5 * 1000;
				return iter;
			}
		});

		Thread.sleep(1000000);

	}
}
