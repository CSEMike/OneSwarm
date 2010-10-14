package edu.washington.cs.oneswarm.test_tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class PlotCdf {

	public final static int SUBSAMPLE_NUM = 1000;

	public static void main(String[] args) throws IOException, InterruptedException {

		ArrayList<Double> val = new ArrayList<Double>();
		for (int i = 0; i < 100; i++) {
			val.add(new Random().nextInt(1000) * 1.0);
		}
		PlotCdf plot = new PlotCdf("test", "random", "cdf", false);
		plot.add(val);
		plot.plot();

		PlotCdf logPlot = new PlotCdf("test", "random", "cdf", true);
		logPlot.add(val);
		logPlot.plot();

	}

	List<List<Double>> values = new LinkedList<List<Double>>();
	final String xAxis;
	final String yAxis;
	final String title;
	final boolean log;

	public PlotCdf(String title, String xAxis, String yAxis, boolean logX) {
		this(title, xAxis, yAxis, logX, null);
	}

	public PlotCdf(String title, String xAxis, String yAxis, boolean logX, List<Double> list) {
		this.xAxis = xAxis;
		this.yAxis = yAxis;
		this.title = title;
		this.log = logX;
		if (list != null) {
			values.add(list);
		}
	}

	public void add(List<Double> list) {
		values.add(list);
	}

	public void plot() throws IOException, InterruptedException {
		List<File> files = new LinkedList<File>();
		for (List<Double> list : values) {
			files.add(printAsCdf(list));
		}

		for (File f : files) {
			String script = "/Users/isdal/scripts/genericCdf.sh";
			if (log) {
				script = "/Users/isdal/scripts/genericCdfLogX.sh";
			}
			String cmd = script + " " + title + " " + xAxis + " " + yAxis + " " + f.getCanonicalPath();
			System.out.println("execuing: " + cmd);
			Process p = Runtime.getRuntime().exec(cmd);
			new StreamDumper(p.getInputStream(), 0, false);
			new StreamDumper(p.getErrorStream(), 0, false);
			p.waitFor();
			// f.delete();
		}

	}

	private File printAsCdf(List<Double> values) throws IOException {
		File f = new File("/tmp/plotcdf_temp_" + title + "_" + Integer.toHexString(new Random().nextInt()));
		System.out.println("writing file: " + f.getCanonicalPath() + " values=" + values.size());

		ArrayList<Double> all = new ArrayList<Double>(values);
		Collections.sort(all);
		ArrayList<Double> sampled = subsample(all);
		BufferedWriter out = new BufferedWriter(new FileWriter(f));
		for (int i = 0; i < sampled.size(); i++) {
			out.write(((double) i) / sampled.size() + " " + sampled.get(i) + "\n");
		}
		out.close();
		return f;
	}

	private ArrayList<Double> subsample(ArrayList<Double> all) {
		if (all.size() < SUBSAMPLE_NUM) {
			return all;
		}
		ArrayList<Double> sampled = new ArrayList<Double>(SUBSAMPLE_NUM);
		int nextPosToPrint = 0;
		double subsampleRate = (1.0 * all.size()) / SUBSAMPLE_NUM;

		for (int i = 0; i < all.size(); i++) {
			double currentRelPos = i * subsampleRate;
			if (currentRelPos > nextPosToPrint) {
				nextPosToPrint++;
				sampled.add(all.get(i));
			}
		}
		return sampled;
	}

	class StreamDumper implements Runnable {

		private InputStream source;

		private boolean print;

		private final StringBuffer store = new StringBuffer();
		private final int bytesToStore;

		public StreamDumper(InputStream source, int bytesToStore, boolean print) {

			this.source = source;
			this.print = print;
			this.bytesToStore = bytesToStore;

			Thread t = new Thread(this);
			t.setName("BufferDumper");
			t.setDaemon(true);
			t.start();
		}

		public void run() {
			try {
				int read = 0;
				long totalRead = 0;
				byte[] buffer = new byte[1000];
				while ((read = source.read(buffer, 0, buffer.length)) != -1) {
					if (print) {
						System.out.println(new String(buffer, 0, read));
					}
					if (totalRead < bytesToStore) {
						store.append(new String(buffer, 0, read));
					}
					totalRead += read;

				}
				source.close();
			} catch (IOException e) {
				System.out.println("Buffer dumper stopped");
			}
		}

		public String getStoredOutput() {
			return store.toString();
		}
	}

	public static double count(List<Double> counts) {
		double count = 0;
		for (Double d : counts) {
			count += d.doubleValue();
		}
		return count;
	}
}
