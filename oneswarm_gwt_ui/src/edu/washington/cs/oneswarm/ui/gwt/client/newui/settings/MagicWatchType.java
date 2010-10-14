package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;


public enum MagicWatchType {
	Magic('m'), 
	Everything('e');
	
	char mTag;
	
	MagicWatchType( char tag ) {
		mTag = tag;
	}
	
	public char getTag() { 
		return mTag;
	}
	
	public static MagicWatchType matchTag( char tag ) {
		for( MagicWatchType t : MagicWatchType.values() ) { 
			if( t.mTag == tag ) {
				return t;
			}
		}
		return null;
	}
};