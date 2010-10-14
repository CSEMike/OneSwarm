package com.aelitis.azureus.core.util.loopcontrol;

public interface LoopControler {
	
	public double updateControler(double error,double position);
	
	public void reset();

}
