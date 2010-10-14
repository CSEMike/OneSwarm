package edu.washington.cs.oneswarm.watchdir;

public interface DirectoryWatcherListener {

	public void newFileObserved( DirectoryWatcher watcher, UpdatingFileTree inTree );
	
	public void deleteFileObserved( DirectoryWatcher watcher, UpdatingFileTree inTree );
	
}
