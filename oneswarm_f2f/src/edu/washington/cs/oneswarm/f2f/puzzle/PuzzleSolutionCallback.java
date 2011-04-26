package edu.washington.cs.oneswarm.f2f.puzzle;

public interface PuzzleSolutionCallback {
	void solved(byte[] offeredBytes, byte[] bestSolution, int matchingBits);
}
