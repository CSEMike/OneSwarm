package edu.washington.cs.oneswarm.ant; 

import java.io.File;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

public class GWTRecompileCheck extends Task {

	String directory = null;
	String lastBuilt = null;
    boolean done = false;

    @Override
	public void execute() throws BuildException {
	
		if (directory == null) {
			throw new BuildException("Directory not specified.");
		}
		
		if (lastBuilt == null) {
			log("No prior built record, building...");
			doBuild(null);
			return;
		}

		// If the OSMessages.java file is missing, we definitely need to recompile.
		File osMessages = new File(
				"oneswarm_gwt_ui/src/edu/washington/cs/oneswarm/ui/gwt/client/i18n/OSMessages.java");
		if (osMessages.exists() == false) {
			log("OSMessages.java not found at: " + osMessages.getAbsolutePath() + " ... rebuilding");
			doBuild(null);
			return;
		}
	
		File dirFile = new File(directory);
		if (dirFile.isDirectory() == false) {
			throw new BuildException("Provided GWT source root is not a directory!");
		}
	
		log("Scanning directory: " + directory + " for changes since " + lastBuilt);
		
		Date last = new SimpleDateFormat("MM/dd/yyyy HH:mm").parse(lastBuilt, new ParsePosition(0));
		
		recursiveScan(dirFile, last);
    }

	private void recursiveScan(File f, Date thresh) {
		
		log(f.getAbsolutePath(), Project.MSG_DEBUG);
		
		if (f.isFile() && (f.getName().endsWith(".java") || f.getName().endsWith(".properties"))) {
			if (thresh.before(new Date(f.lastModified()))) {
				doBuild(f);
			}
		}
		
		if( !done && f.isDirectory()) {
			
			if (f.getName().startsWith(".svn")) {
				return;
			}
			
			for (File kid : f.listFiles()) {
				recursiveScan(kid, thresh);
				
				if (done) {
					break;
				}
			}
		}
	}

	private void doBuild(File f) {
		getProject().setProperty("gwt.recompile", "true");
		done = true;
		log("Performing build");
		if (f != null) {
			log(f.getName() + " modified " + (new Date(f.lastModified())));
		}
	}
 
    public void setDirectory(String directory) {
    	this.directory = directory;
    }

	public void setLastBuild(String lastBuilt) {
		if (lastBuilt.startsWith("${")) {
			return;
		}
		this.lastBuilt = lastBuilt;
	}
}
