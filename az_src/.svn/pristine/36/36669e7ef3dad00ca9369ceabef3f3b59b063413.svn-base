package com.aelitis.azureus.core.neuronal;

public class NeuralNetwork {

	NeuralNetworkLayer inputLayer;
	NeuralNetworkLayer hiddenLayer;
	NeuralNetworkLayer outputLayer;
	
	public NeuralNetwork(int nbInputNodes, int nbHiddenNodes, int nbOutputNodes) {
		inputLayer = new NeuralNetworkLayer(nbInputNodes);
		hiddenLayer = new NeuralNetworkLayer(nbHiddenNodes);
		outputLayer = new NeuralNetworkLayer(nbOutputNodes);
		
		inputLayer.initialize(null, hiddenLayer);
		inputLayer.randomizeWeights();
		
		hiddenLayer.initialize(inputLayer, outputLayer);
		hiddenLayer.randomizeWeights();
		
		outputLayer.initialize(hiddenLayer, null);
		
		
	}
	
	public void setActivationFunction(ActivationFunction activationFunction) {
		inputLayer.setActivationFunction(activationFunction);
		hiddenLayer.setActivationFunction(activationFunction);
		outputLayer.setActivationFunction(activationFunction);
		
	}
	
	public void setInput(int i, double value) {
		if(i >= 0 && i < inputLayer.getNumberOfNodes()) {
			inputLayer.neuronValues[i] = value;
		}
	}
	
	public double getOutput(int i) {
		if(i >= 0 && i < outputLayer.getNumberOfNodes()) {
			return outputLayer.neuronValues[i];
		}
		
		return Double.NaN;
	}
	
	public void setDesiredOutput(int i, double value) {
		if(i >= 0 && i < outputLayer.getNumberOfNodes()) {
			outputLayer.desiredValues[i] = value;
		}
	}
	
	public void setMomentum(boolean useMomentum, double factor) {
		inputLayer.setMomentum(useMomentum,factor);
		hiddenLayer.setMomentum(useMomentum,factor);
		outputLayer.setMomentum(useMomentum,factor);
	}
	
	public void setLearningRate(double rate) {
		inputLayer.setLearningRate(rate);
		hiddenLayer.setLearningRate(rate);
		outputLayer.setLearningRate(rate);
	}
	
	public void feedForward() {
		inputLayer.calculateNeuronValues();
		hiddenLayer.calculateNeuronValues();
		outputLayer.calculateNeuronValues();
	}
	
	public void backPropagate() {
		outputLayer.calculateErrors();
		hiddenLayer.calculateErrors();
		
		hiddenLayer.adjustWeights();
		inputLayer.adjustWeights();
	}
	
	public double calculateError() {
		double error = 0.0;
		
		for(int i = 0 ; i < outputLayer.numberOfNodes ; i++) {
			error += Math.pow(outputLayer.neuronValues[i] - outputLayer.desiredValues[i], 2.0);
		}
		
		error /= outputLayer.numberOfNodes;
		
		return error;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("Input Layer :\n");
		sb.append(inputLayer.toString());
		sb.append("\n\n");
		
		sb.append("Hidden Layer :\n");
		sb.append(hiddenLayer.toString());
		sb.append("\n\n");
		
		return sb.toString();
	}
	
}
