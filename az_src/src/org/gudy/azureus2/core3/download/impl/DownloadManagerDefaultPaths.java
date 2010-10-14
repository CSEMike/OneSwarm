package org.gudy.azureus2.core3.download.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.FileUtil;

public class DownloadManagerDefaultPaths {

    private final static MovementInformation[] COMPLETION_DETAILS;
    private final static MovementInformation[] REMOVAL_DETAILS;
    private final static MovementInformation[] UPDATE_FOR_MOVE_DETAILS;
    private final static MovementInformation[] UPDATE_FOR_LOGIC_DETAILS;
    private final static TargetSpecification[] DEFAULT_DIRS;

    private final static String STATE_INCOMPLETE = "incomplete download";
    private final static String STATE_COMPLETE_DND = "dnd-complete download";
    private final static String STATE_COMPLETE = "fully-complete download";

    private final static String SUBDIR_PARAM = "File.move.subdir_is_default";


    static {
        SourceSpecification source;
        TargetSpecification dest;
        TransferSpecification trans;
        MovementInformation mi_1, mi_2;

        /**
         * There are three sets of directories that we consider a "default"
         * directory (perhaps it should just be two):
         *
         * - default save dir
         * - completed save dir
         * - removed save dir
         */
        DEFAULT_DIRS = new TargetSpecification[3];
        dest = new TargetSpecification();
        dest.setBoolean("enabled", true);
        dest.setString("target", "Default save path");
        dest.setContext("default save dir");
        DEFAULT_DIRS[0] = dest;


        // First - download completion details.
		source = new SourceSpecification();
		source.setBoolean("default dir", "Move Only When In Default Save Dir");
		source.setBoolean("default subdir", SUBDIR_PARAM);
		source.setBoolean("persistent only", true);
		source.setBoolean("check exclusion flag", true);
		source.setBoolean("check completion flag", true);
		source.setBoolean(STATE_INCOMPLETE, false);
		source.setBoolean(STATE_COMPLETE_DND, true);
		source.setBoolean(STATE_COMPLETE, true); // Only handle fully complete downloads at moment.

		dest = new TargetSpecification();
		dest.setBoolean("enabled", "Move Completed When Done");
		dest.setString("target", "Completed Files Directory");
		dest.setContext("completed files dir");

		trans = new TransferSpecification();
		trans.setBoolean("torrent", "Move Torrent When Done");

		mi_1 = new MovementInformation(source, dest, trans, "Move on completion");
		COMPLETION_DETAILS = new MovementInformation[] {mi_1};
		DEFAULT_DIRS[1] = dest;

		// Next - download removal details.
		source = new SourceSpecification();
		source.setBoolean("default dir", "File.move.download.removed.only_in_default");
		source.setBoolean("default subdir", SUBDIR_PARAM);
		source.setBoolean("persistent only", true);
		source.setBoolean("check exclusion flag", true);
		source.setBoolean("check completion flag", false);
		source.setBoolean(STATE_INCOMPLETE, false);
		source.setBoolean(STATE_COMPLETE_DND, "File.move.download.removed.move_partial");
		source.setBoolean(STATE_COMPLETE, true);

		dest = new TargetSpecification();
		dest.setBoolean("enabled", "File.move.download.removed.enabled");
		dest.setString("target", "File.move.download.removed.path");
		dest.setContext("removed files dir");

		trans = new TransferSpecification();
		trans.setBoolean("torrent", "File.move.download.removed.move_torrent");

		mi_1 = new MovementInformation(source, dest, trans, "Move on removal");
		REMOVAL_DETAILS = new MovementInformation[] {mi_1};
		DEFAULT_DIRS[2] = dest;

	    /**
	     * Next - updating the current path (complete dl's first)
	     * 
	     * We instantiate the "update incomplete download" source first, and then
	     * we instantiate the "update complete download", but when we process, we
	     * will do the complete download bit first.
	     * 
	     * We do this, because in the "update incomplete download" section, completed
	     * downloads are enabled for it. And the reason it is, is because this will
	     * allow the code to behave properly if move on completion is not enabled.
	     *
	     * Complete downloads apply to this bit, just in case the "move on completion"
	     * section isn't active.
	     */
		source = new SourceSpecification();
		source.updateSettings(COMPLETION_DETAILS[0].source.getSettings());
		source.setBoolean("default dir", true);

		mi_1 = new MovementInformation(source, COMPLETION_DETAILS[0].target,
				COMPLETION_DETAILS[0].transfer, "Update completed download");
		
		// Now incomplete downloads. We have to define completely new settings for
		// it, since we've never defined it before.
		source = new SourceSpecification();
		source.setBoolean("default dir", true); // Must be in default directory to update.
		source.setBoolean("default subdir", SUBDIR_PARAM);
		source.setBoolean("persistent only", true);
		source.setBoolean("check exclusion flag", true);
		source.setBoolean("check completion flag", false);
		source.setBoolean(STATE_INCOMPLETE, true);
		source.setBoolean(STATE_COMPLETE_DND, true);
		source.setBoolean(STATE_COMPLETE, true);

		dest = new TargetSpecification();
		dest.setBoolean("enabled", true);
		dest.setString("target", "Default save path");

		trans = new TransferSpecification();
		trans.setBoolean("torrent", false);

        // Rest of the settings are the same.
		mi_2 = new MovementInformation(source, dest, trans, "Update incomplete download");
		UPDATE_FOR_MOVE_DETAILS = new MovementInformation[] {mi_1, mi_2};

		/**
		 * Now we have a copy of the exact same settings for updates, except we
		 * disable the logic regarding default directory requirements.
		 */
		UPDATE_FOR_LOGIC_DETAILS = new MovementInformation[UPDATE_FOR_MOVE_DETAILS.length];
		for (int i=0; i<UPDATE_FOR_MOVE_DETAILS.length; i++) {
			MovementInformation mi = UPDATE_FOR_MOVE_DETAILS[i];
		    source = new SourceSpecification();
		    source.updateSettings(mi.source.getSettings());
		    source.setBoolean("default dir", false);
		    source.setBoolean("persistent only", false);
		    source.setBoolean("check exclusion flag", false);
		    UPDATE_FOR_LOGIC_DETAILS[i] = new MovementInformation(source,
		        mi.target, mi.transfer, mi.title.replaceAll("Update", "Calculate path for"));
	    }

    }
    
    private static String normaliseRelativePathPart(String name) {
    	name = name.trim();
    	if (name.length() == 0) {return "";}
    	if (name.equals(".") || name.equals("..")) {
    		return null;
    	}
    	return FileUtil.convertOSSpecificChars(name).trim();
    }
    
    public static File normaliseRelativePath(File path) {
    	if (path.isAbsolute()) {return null;}
    	
    	File parent = path.getParentFile();
    	String child_name = normaliseRelativePathPart(path.getName());
    	if (child_name == null) {
    		return null;
    	}
    	
    	//  Simple one-level path.
    	if (parent == null) {
    		return new File(child_name);
    	}
    	
    	ArrayList parts = new ArrayList();
    	parts.add(child_name);
    	
    	String filepart = null;
    	while (parent != null) {
    		filepart = normaliseRelativePathPart(parent.getName());
    		if (filepart == null) {return null;}
    		else if (filepart.length()==0) {/* continue */}
    		else {parts.add(0, filepart);} 
    		parent = parent.getParentFile();
    	}
    	
    	StringBuffer sb = new StringBuffer((String)parts.get(0));
    	for (int i=1; i<parts.size(); i++) {
    		sb.append(File.separatorChar);
    		sb.append(parts.get(i));
    	}
    	
    	return new File(sb.toString());
    }
    	
    private static File[] getDefaultDirs(LogRelation lr) {
		List results = new ArrayList();
		File location = null;
		TargetSpecification ts = null;
		for (int i=0; i<DEFAULT_DIRS.length; i++) {
			ts = (TargetSpecification)DEFAULT_DIRS[i];
			location = ts.getTarget(null, lr, ts);
			if (location != null) {
				results.add(location);
			}
		}
		return (File[])results.toArray(new File[results.size()]);
	}

	private static String getStateDescriptor(DownloadManager dm) {
		if (dm.isDownloadComplete(true)) {return STATE_COMPLETE;}
		else if (dm.isDownloadComplete(false)) {return STATE_COMPLETE_DND;}
		else {return STATE_INCOMPLETE;}
	}

    // Helper log functions.
	private static void logInfo(String message, LogRelation lr) {
		if (lr == null) {return;}
		if (!Logger.isEnabled()) {return;}
		Logger.log(new LogEvent(lr, LogIDs.CORE, LogEvent.LT_INFORMATION, message));
	}

	private static void logWarn(String message, LogRelation lr) {
		if (lr == null) {return;}
		if (!Logger.isEnabled()) {return;}
		Logger.log(new LogEvent(lr, LogIDs.CORE, LogEvent.LT_WARNING, message));
	}

	private static String describe(DownloadManager dm, ContextDescriptor cs) {
		if (cs == null) {
			if (dm == null) {return "";}
			return "\"" + dm.getDisplayName() + "\"";
		}
		if (dm == null) {
			return "\"" + cs.getContext() + "\"";
		}
		return "\"" + dm.getDisplayName() + "\" with regard to \"" + cs.getContext() + "\"";
	}

    /**
     * This does the guts of determining appropriate file paths.
     */
    private static TransferDetails determinePaths(DownloadManager dm, MovementInformation mi) {
		LogRelation lr = (dm instanceof LogRelation) ? (LogRelation)dm : null;
		boolean proceed = mi.source.matchesDownload(dm, lr, mi);
		if (!proceed) {
			logInfo("Cannot consider " + describe(dm, mi) +
			    " - does not match source criteria.", lr);
			return null;
		}

		File target_path = mi.target.getTarget(dm, lr, mi);
		if (target_path == null) {
			logInfo("Unable to determine an appropriate target for " +
			    describe(dm, mi) + ".", lr);
			return null;
		}

        logInfo("Determined path for " + describe(dm, mi) + ".", lr);
		return mi.transfer.getTransferDetails(dm, lr, mi, target_path);
	}

	private static TransferDetails determinePaths(DownloadManager dm, MovementInformation[] mis) {
	    TransferDetails result = null;
		for (int i=0; i<mis.length; i++) {
			result = determinePaths(dm, mis[i]);
			if (result != null) {return result;}
		}
		return null;
	}
	
	public static boolean isInDefaultDownloadDir(DownloadManager dm) {
		// We don't create this object properly, but just enough to get it
		// to be usable.
		SourceSpecification source = new SourceSpecification();
		source.setBoolean("default subdir", SUBDIR_PARAM);
		return source.checkDefaultDir(dm.getSaveLocation().getParentFile(), getDefaultDirs(null));
	}

    public static class TransferDetails {
		public File transfer_destination;
		public boolean move_torrent;
		
		public String toString() {
			return "TransferDetails [dest: " + this.transfer_destination.getPath() +
				", move-torrent: " + this.move_torrent + "]";
		}
		
	}
    
    private static interface ContextDescriptor {
    	public String getContext(); 
    }
    
    private static class MovementInformation implements ContextDescriptor {
        final SourceSpecification source;
        final TargetSpecification target;
        final TransferSpecification transfer;
        final String title;

        MovementInformation(SourceSpecification source, TargetSpecification target,
            TransferSpecification transfer, String title) {
            this.source = source;
            this.target = target;
            this.transfer = transfer;
            this.title = title;
        }
        
        public String getContext() {return title;}
    }

    private abstract static class ParameterHelper implements ContextDescriptor {
		private Map settings = new HashMap();
		private String context = null;

		protected boolean getBoolean(String key) {
			Object result = this.settings.get(key);
			if (result == null) {throw new RuntimeException("bad key: " + key);}
			if (result instanceof Boolean) {return ((Boolean)result).booleanValue();}
            return COConfigurationManager.getBooleanParameter((String)result);
        }

        protected void setBoolean(String key, boolean value) {
        	settings.put(key, Boolean.valueOf(value));
        }

        protected void setBoolean(String key, String param) {
        	settings.put(key, param);
        }

        protected void setString(String key, String param) {
        	settings.put(key, param);
        }
        
        protected String getString(String key) {
			String result = (String)this.settings.get(key);
			if (result == null) {throw new RuntimeException("bad key: " + key);}
			
			// This try-catch should be removed, it's only here for debugging purposes.
        	return COConfigurationManager.getStringParameter(result);
        }
        
        public Map getSettings() {return this.settings;}
        public void updateSettings(Map settings) {this.settings.putAll(settings);}
        
        public String getContext() {return this.context;}
        public void setContext(String context) {this.context = context;}
    }

    private static class SourceSpecification extends ParameterHelper {

		public boolean matchesDownload(DownloadManager dm, LogRelation lr, ContextDescriptor context) {
			if (this.getBoolean("persistent only") && !dm.isPersistent()) {
				logWarn(describe(dm, context) + " is not persistent.", lr);
				return false;
			}
			
			if (this.getBoolean("check exclusion flag") && dm.getDownloadState().getFlag(DownloadManagerState.FLAG_DISABLE_AUTO_FILE_MOVE)) {
				logWarn(describe(dm, context) + " has exclusion flag set.", lr);
				return false;
			}
			
			if (this.getBoolean("check completion flag") && dm.getDownloadState().getFlag(DownloadManagerState.FLAG_MOVE_ON_COMPLETION_DONE)) {
				logInfo(describe(dm, context) + " has completion flag set.", lr);
				return false;
			}
			
			if (this.getBoolean("default dir")) {
				logInfo("Checking if " + describe(dm, context) + " is inside default dirs.", lr);
				File[] default_dirs = getDefaultDirs(lr);
				File current_location = dm.getSaveLocation().getParentFile();
				
				/**
                 * Very very rare, but I have seen this on fscked up downloads which don't appear
                 * to have a blank / malformed download path.
				 */ 
				if (current_location == null) {
					logWarn(describe(dm, context) + " appears to have a malformed save directory, skipping.", lr);
					return false;
				}
				
				if (!this.checkDefaultDir(current_location, default_dirs)) {
					logWarn(describe(dm, context) +
					    " doesn't exist in any of the following default directories" +
					    " (current dir: " + current_location + ", subdirectories checked: " +
					    this.getBoolean("default subdir") + ") - " + Arrays.asList(default_dirs), lr);
					return false;
				}
				logInfo(describe(dm, context) + " does exist inside default dirs.", lr);
			}

  			String current_state = getStateDescriptor(dm);
  			boolean can_move = this.getBoolean(current_state);
  			String log_message = describe(dm, context) + " is " +
  			    ((can_move) ? "" : "not ") + "in an appropriate state (is " +
  			    "currently \"" + current_state + "\").";
  			if (!can_move) {
  				logWarn(log_message, lr);
  				return false;
  			}
  			logInfo(log_message, lr);
			return true;
		}
		
		public boolean checkDefaultDir(File location, File[] default_dirs) {
			location = FileUtil.canonise(location);
			boolean subdir = this.getBoolean("default subdir");
			for (int i=0; i<default_dirs.length; i++) {
				if (subdir) {
					if (FileUtil.isAncestorOf(default_dirs[i], location)) {return true;}
				}
				else {
					if (default_dirs[i].equals(location)) {return true;}
				}
			}
			return false;
		}
		
    }
			
	private static class TargetSpecification extends ParameterHelper {

		public File getTarget(DownloadManager dm, LogRelation lr, ContextDescriptor cd) {
			//logInfo("Calculating target location for " + describe(dm, cd), lr);
			if (!this.getBoolean("enabled")) {
				logInfo("Target for " + describe(dm, cd) + " is not enabled.", lr);
				return null;
			}
		    String location = this.getString("target").trim();
		    if (location.length() == 0) {
				logInfo("No explicit target for " + describe(dm, cd) + ".", lr);
				return null;
			}
		    
		    File target = new File(FileUtil.getCanonicalFileName(location));
		    String relative_path = null;
          
          if( dm != null && dm.getDownloadState() != null ) {
             relative_path = dm.getDownloadState().getRelativeSavePath();
          }
		    
          if (relative_path != null && relative_path.length() > 0) {
		    	logInfo("Consider relative save path: " + relative_path, lr);
		    	
		    	// Doesn't matter if File.separator is required or not, it seems to
		    	// remove duplicate file separators.
		    	target = new File(target.getPath() + File.separator + relative_path);
		    }
			return target;
		}

	}

	private static class TransferSpecification extends ParameterHelper {

		public TransferDetails getTransferDetails(DownloadManager dm, LogRelation lr,
			ContextDescriptor cd, File target_path) {
			
			if (target_path == null) {throw new NullPointerException();}

			TransferDetails result = new TransferDetails();
			result.transfer_destination = target_path;
			result.move_torrent = this.getBoolean("torrent");
			return result;
		}

	}
	
	public static File getCompletionDirectory(DownloadManager dm) {
		return COMPLETION_DETAILS[0].target.getTarget(dm, null, null);
	}
	
	public static TransferDetails onInitialisation(DownloadManager dm) {
		return determinePaths(dm, UPDATE_FOR_MOVE_DETAILS[1]); // 1 - incomplete downloads
	}

	public static TransferDetails onCompletion(DownloadManager dm, boolean set_on_completion_flag) {
		TransferDetails td = determinePaths(dm, COMPLETION_DETAILS);
		
		// Not sure what we should do if we don't have any transfer details w.r.t the
		// completion flag. I think we probably should - we only want to consider the
		// settings once - when completion has actually occurred.
		if (set_on_completion_flag) {
			LogRelation lr = (dm instanceof LogRelation) ? (LogRelation)dm : null;
			logInfo("Setting completion flag on " + describe(dm, null) + ", may have been set before.", lr);
			dm.getDownloadState().setFlag(DownloadManagerState.FLAG_MOVE_ON_COMPLETION_DONE, true);
		}
		return td;
	}

	public static TransferDetails onRemoval(DownloadManager dm) {
		return determinePaths(dm, REMOVAL_DETAILS);
	}

	public static File[] getDefaultSavePaths(DownloadManager dm, boolean for_moving) {
		MovementInformation[] mi = (for_moving) ? UPDATE_FOR_MOVE_DETAILS : UPDATE_FOR_LOGIC_DETAILS;
		TransferDetails details = determinePaths(dm, mi);

		// Always return an array of size two.
		File[] result = new File[2];

		// Set default values first.
		if (!for_moving) {
			result[0] = dm.getSaveLocation();
			result[1] = new File(dm.getTorrentFileName()).getParentFile();
		}

		if (details != null) {
			result[0] = details.transfer_destination;
			if (details.move_torrent) {
				result[1] = result[0];
			}
		}

		return result;

	}

}