package edu.washington.cs.oneswarm.test.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/** Utility class which forks a thread to print the logs of a {@code Process}. */
public class ProcessLogConsumer extends Thread {
	
	/** The process from which to consume logs. */
	Process process;
	
	/** A label for the log of this process. */
	String label;
	
	public ProcessLogConsumer(String label, Process process) {
		this.label = label;
		this.process = process;
	}
	
	/** The consumer thread task. */
	public void run() {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			while (true) {
				String line = reader.readLine();
				if (line == null) {
					break;
				}
				System.out.println("[" + label + "]: " + line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("[" + label + "]: Ended.");
	}
}
