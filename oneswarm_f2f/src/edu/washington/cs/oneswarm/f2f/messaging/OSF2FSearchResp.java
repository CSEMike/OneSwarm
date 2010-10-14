/**
 * 
 */
package edu.washington.cs.oneswarm.f2f.messaging;

/**
 * @author isdal
 * 
 */
public interface OSF2FSearchResp extends OSF2FMessage {
	public int getSearchID();

	public int getChannelID();

	public OSF2FSearchResp clone();
}
