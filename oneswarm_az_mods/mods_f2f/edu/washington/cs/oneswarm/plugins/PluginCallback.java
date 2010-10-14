package edu.washington.cs.oneswarm.plugins;

public interface PluginCallback<T> {
	public void requestCompleted(T data);

	public void progressUpdate(int progress);

	public void dataRecieved(long bytes);
	
	public void errorOccured(String string);
}
