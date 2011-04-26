package edu.washington.cs.oneswarm.util;

/** A generic container. */
public class Box<T> {

	private T boxed;

	public Box() {
		set(null);
	}

	public Box(T boxed) {
		set(boxed);
	}

	public void set(T boxed) {
		this.boxed = boxed;
	}

	public T get() {
		return boxed;
	}
}
