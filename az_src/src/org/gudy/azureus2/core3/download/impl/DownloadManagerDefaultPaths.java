package org.gudy.azureus2.core3.download.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.FileUtil;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.savelocation.DefaultSaveLocationManager;
import org.gudy.azureus2.plugins.download.savelocation.SaveLocationChange;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadImpl;


public class DownloadManagerDefaultPaths extends DownloadManagerMoveHandlerUtils {
	
	public final static DefaultSaveLocationManager DEFAULT_HANDLER = new DefaultSaveLocationManager() {
		public SaveLocationChange onInitialization(Download d, boolean for_move, boolean on_event) {
			
			/**
			 * This manager object isn't the sort of object which decides on
			 * an alternate initialisation place for a download - if a user
			 * has chosen a path for it, we don't interfere with it under any
			 * circumstances (though if plugins want to, then that's up to them). 
			 */
			if (on_event) {return null;}
			
			DownloadManager dm = ((DownloadImpl)d).getDownload();
			return determinePaths(dm, UPDATE_FOR_MOVE_DETAILS[1], for_move, false); // 1 - incomplete downloads
		}
		public SaveLocationChange onCompletion(Download d, boolean for_move, boolean on_event) {
			DownloadManager dm = ((DownloadImpl)d).getDownload();
			return determinePaths(dm, COMPLETION_DETAILS, for_move, false);
		}
		public SaveLocationChange testOnCompletion(Download d, boolean for_move, boolean on_event) {
			DownloadManager dm = ((DownloadImpl)d).getDownload();
			return determinePaths(dm, COMPLETION_DETAILS, for_move, true );
		}
		public SaveLocationChange onRemoval(Download d, boolean for_move, boolean on_event) {
			DownloadManager dm = ((DownloadImpl)d).getDownload();
			return determinePaths(dm, REMOVAL_DETAILS, for_move, false );
		}
		public boolean isInDefaultSaveDir(Download d) {
			DownloadManager dm = ((DownloadImpl)d).getDownload();
			return DownloadManagerDefaultPaths.isInDefaultDownloadDir(dm);
		}
	};
	
    private final static MovementInformation COMPLETION_DETAILS;
    private final static MovementInformation REMOVAL_DETAILS;
    private final static MovementInformation[] UPDATE_FOR_MOVE_DETAILS;
    private final static TargetSpecification[] DEFAULT_DIRS;

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
		source.setBoolean("incomplete dl", false);

		dest = new TargetSpecification();
		dest.setBoolean("enabled", "Move Completed When Done");
		dest.setString("target", "Completed Files Directory");
		dest.setContext("completed files dir");

		trans = new TransferSpecification();
		trans.setBoolean("torrent", "Move Torrent When Done");

		mi_1 = new MovementInformation(source, dest, trans, "Move on completion");
		COMPLETION_DETAILS = mi_1;
		DEFAULT_DIRS[1] = dest;

		// Next - download removal details.
		source = new SourceSpecification();
		source.setBoolean("default dir", "File.move.download.removed.only_in_default");
		source.setBoolean("default subdir", SUBDIR_PARAM);
		source.setBoolean("incomplete dl", false);

		dest = new TargetSpecification();
		dest.setBoolean("enabled", "File.move.download.removed.enabled");
		dest.setString("target", "File.move.download.removed.path");
		dest.setContext("removed files dir");

		trans = new TransferSpecification();
		trans.setBoolean("torrent", "File.move.download.removed.move_torrent");

		mi_1 = new MovementInformation(source, dest, trans, "Move on removal");
		REMOVAL_DETAILS = mi_1;
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
		source.updateSettings(COMPLETION_DETAILS.source.getSettings());
		source.setBoolean("default dir", true);

		mi_1 = new MovementInformation(source, COMPLETION_DETAILS.target,
				COMPLETION_DETAILS.transfer, "Update completed download");
		
		// Now incomplete downloads. We have to define completely new settings for
		// it, since we've never defined it before.
		source = new SourceSpecification();
		source.setBoolean("default dir", true); // Must be in default directory to update.
		source.setBoolean("default subdir", SUBDIR_PARAM);
		source.setBoolean("incomplete dl", true);

		dest = new TargetSpecification();
		dest.setBoolean("enabled", true);
		dest.setString("target", "Default save path");

		trans = new TransferSpecification();
		trans.setBoolean("torrent", false);

        // Rest of the settings are the same.
		mi_2 = new MovementInformation(source, dest, trans, "Update incomplete download");
		UPDATE_FOR_MOVE_DETAILS = new MovementInformation[] {mi_1, mi_2};

    }
    
    private static interface ContextDescriptor {
    	public String getContext(); 
    }
    
    private static String normaliseRelativePathPart(String name) {
    	name = name.trim();
    	if (name.length() == 0) {return "";}
    	if (name.equals(".") || name.equals("..")) {
    		return null;
    	}
    	return FileUtil.convertOSSpecificChars(name, false).trim();
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
    	
    private static File[] getDefaultDirs() {
		List results = new ArrayList();
		File location = null;
		TargetSpecification ts = null;
		for (int i=0; i<DEFAULT_DIRS.length; i++) {
			ts = DEFAULT_DIRS[i];
			location = ts.getTarget(null, ts);
			if (location != null) {
				results.add(location);
			}
		}
		return (File[])results.toArray(new File[results.size()]);
	}



    /**
     * This does the guts of determining appropriate file paths.
     */
    private static SaveLocationChange determinePaths(DownloadManager dm, MovementInformation mi, boolean check_source, boolean is_test) {
		boolean proceed = !check_source || mi.source.matchesDownload(dm, mi, is_test );
		if (!proceed) {
			logInfo("Cannot consider " + describe(dm, mi) +
			    " - does not match source criteria.", dm);
			return null;
		}

		File target_path = mi.target.getTarget(dm, mi);
		if (target_path == null) {
			logInfo("Unable to determine an appropriate target for " +
			    describe(dm, mi) + ".", dm);
			return null;
		}

        logInfo("Determined path for " + describe(dm, mi) + ".", dm);
		return mi.transfer.getTransferDetails(dm, mi, target_path);
	}

	static boolean isInDefaultDownloadDir(DownloadManager dm) {
		// We don't create this object properly, but just enough to get it
		// to be usable.
		SourceSpecification source = new SourceSpecification();
		source.setBoolean("default subdir", SUBDIR_PARAM);
		return source.checkDefaultDir(dm.getSaveLocation().getParentFile(), getDefaultDirs());
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

		public boolean matchesDownload(DownloadManager dm, ContextDescriptor context, boolean ignore_completeness ) {
			if (this.getBoolean("default dir")) {
				logInfo("Checking if " + describe(dm, context) + " is inside default dirs.", dm);
				File[] default_dirs = getDefaultDirs();
				File current_location = dm.getSaveLocation().getParentFile();
				
				/**
                 * Very very rare, but I have seen this on fscked up downloads which don't appear
                 * to have a blank / malformed download path.
				 */ 
				if (current_location == null) {
					logWarn(describe(dm, context) + " appears to have a malformed save directory, skipping.", dm);
					return false;
				}
				
				if (!this.checkDefaultDir(current_location, default_dirs)) {
					logWarn(describe(dm, context) +
					    " doesn't exist in any of the following default directories" +
					    " (current dir: " + current_location + ", subdirectories checked: " +
					    this.getBoolean("default subdir") + ") - " + Arrays.asList(default_dirs), dm);
					return false;
				}
				logInfo(describe(dm, context) + " does exist inside default dirs.", dm);
			}

			// Does it work for incomplete downloads?
  			if (!dm.isDownloadComplete(false)) {
  				boolean can_move = ignore_completeness || this.getBoolean("incomplete dl");
  				String log_message = describe(dm, context) + " is incomplete which is " +
  			    	((can_move) ? "" : "not ") + "an appropriate state.";
  				if (!can_move) {
  					logInfo(log_message, dm);
  					return false;
  				}
  			}
  			
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

		public File getTarget(DownloadManager dm, ContextDescriptor cd) {
			//logInfo("Calculating target location for " + describe(dm, cd), lr);
			if (!this.getBoolean("enabled")) {
				logInfo("Target for " + describe(dm, cd) + " is not enabled.", dm);
				return null;
			}
		    String location = this.getString("target").trim();
		    if (location.length() == 0) {
				logInfo("No explicit target for " + describe(dm, cd) + ".", dm);
				return null;
			}
		    
		    File target = new File(FileUtil.getCanonicalFileName(location));
		    String relative_path = null;
          
          if( dm != null && dm.getDownloadState() != null ) {
             relative_path = dm.getDownloadState().getRelativeSavePath();
          }
		    
          if (relative_path != null && relative_path.length() > 0) {
		    	logInfo("Consider relative save path: " + relative_path, dm);
		    	
		    	// Doesn't matter if File.separator is required or not, it seems to
		    	// remove duplicate file separators.
		    	target = new File(target.getPath() + File.separator + relative_path);
		    }
			return target;
		}

	}

	private static class TransferSpecification extends ParameterHelper {

		public SaveLocationChange getTransferDetails(DownloadManager dm, 
				ContextDescriptor cd, File target_path) {
			
			if (target_path == null) {throw new NullPointerException();}

			SaveLocationChange result = new SaveLocationChange();
			result.download_location = target_path;
			if (this.getBoolean("torrent")) {
				result.torrent_location = target_path;
			}
			return result;
		}

	}

	public static File getCompletionDirectory(DownloadManager dm) {
		return COMPLETION_DETAILS.target.getTarget(dm, null);
	}
	
	static String describe(DownloadManager dm, ContextDescriptor cs) {
		if (cs == null) {return describe(dm);}
		if (dm == null) {
			return "\"" + cs.getContext() + "\"";
		}
		return "\"" + dm.getDisplayName() + "\" with regard to \"" + cs.getContext() + "\"";
	}

}