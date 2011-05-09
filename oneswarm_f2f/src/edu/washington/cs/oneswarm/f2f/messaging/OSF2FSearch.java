/**
 * 
 */
package edu.washington.cs.oneswarm.f2f.messaging;

/**
 * @author isdal
 * 
 */
public interface OSF2FSearch extends OSF2FMessage {

    public int getSearchID();

    public int getValueID();

    public OSF2FSearch clone();
}
