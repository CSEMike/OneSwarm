package edu.washington.cs.oneswarm.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class DiscoverTestClasses extends Task {

	/*
	 * The need for this hack as opposed to ordinary JUnit execution is because
	 * we can't use the JUnit ant task directly. Due to OSX SWT issues, we need
	 * to run each test as an individual java execution.
	 */

	List<String> classes = new ArrayList<String>();
	final File root = new File("test-bin");

	@Override
	public void execute() throws BuildException {
		recursiveScan(root);

		StringBuilder prop = new StringBuilder();
		for (String c : classes) {
			prop.append(transform(c) + ",");
		}
		getProject().setProperty("oneswarm.test.cases", prop.substring(0, prop.length()-1));
	}

	private String transform(String inFullPath) {
		return inFullPath.replace(root.getAbsolutePath(), "").replace(File.separator, ".").replace(".class", "").substring(1);
	}

	private void recursiveScan(File root) {

		if (!root.isDirectory()) {
			if (root.getName().endsWith("Test.class")) {
				classes.add(root.getAbsolutePath());
			}
		} else {
			for (File kid : root.listFiles()) {
				recursiveScan(kid);
			}
		}
	}
}
