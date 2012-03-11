package com.aelitis.azureus.core.util.loopcontrol;

/**
 * XXX: I can't find anywhere where this is used...
 */
public interface LoopControler {
	
	public double updateControler(double error,double position);
	
	public void reset();

}
