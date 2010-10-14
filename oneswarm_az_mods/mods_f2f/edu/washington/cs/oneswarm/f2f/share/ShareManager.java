package edu.washington.cs.oneswarm.f2f.share;

import java.util.logging.Logger;

public class ShareManager
{
	private static Logger						 logger	 = Logger.getLogger(ShareManager.class.getName());

	private final static ShareManager instance = new ShareManager();

	public static ShareManager getInstance() {
		return instance;
	}

	private ShareManager() {

	}

	

}
