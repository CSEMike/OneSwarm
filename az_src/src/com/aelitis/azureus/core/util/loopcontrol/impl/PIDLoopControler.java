package com.aelitis.azureus.core.util.loopcontrol.impl;

import com.aelitis.azureus.core.util.loopcontrol.LoopControler;

public class PIDLoopControler implements LoopControler {

	double pGain;
	double iGain;
	double dGain;
	
	double iState;
	double iMin = -5000;
	double iMax =  5000;
	
	double dState;
	
	public PIDLoopControler(double pGain,double iGain, double dGain) {
		this.pGain = pGain;
		this.iGain = iGain;
		this.dGain = dGain;
	}
	
	public double updateControler(double error, double position) {
		
		//Proportional
		double pTerm = pGain * error;
		
		
		iState += error;
		if(iState > iMax) iState = iMax;
		if(iState < iMin) iState = iMin;
		
		double iTerm = iGain * iState;
		
		double d = dState - position;
		
		double dTerm = dGain * d;
		dState = position;

		double result = pTerm + iTerm - dTerm;
		
		System.out.println("PID p,i,d (" + pGain + "," + iGain + "," + dGain +") : is,ds (" + iState + "," + d + ") p,i,d (" + pTerm + "," + iTerm + "," + dTerm + ") => " + result);
		
		return result;
	}
	
	public void reset() {
		dState = 0;
		iState = 0;
	}

}
