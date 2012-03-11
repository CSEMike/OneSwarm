package org.gudy.azureus2.ui.swt.components;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.core3.util.Debug;

public class CompositeMinSize
	extends Composite
{
	int minWidth = SWT.DEFAULT;
	int minHeight = SWT.DEFAULT;

	public CompositeMinSize(Composite parent, int style) {
		super(parent, style);
	}

	public void setMinSize(Point pt) {
		minWidth = pt.x;
		minHeight = pt.y;
	}
	
	public Point computeSize(int wHint, int hHint, boolean changed) {
		try {
			Point size = super.computeSize(wHint, hHint, changed);
			return betterComputeSize(this, size, wHint, hHint, changed);
		} catch (Throwable t) {
			Debug.out(t);
			return new Point(wHint == -1 ? 10 : wHint, hHint == -1 ? 10
					: hHint);
		}
	}
	
	public Point computeSize(int wHint, int hHint) {
		try {
			Point size = super.computeSize(wHint, hHint);
			return betterComputeSize(this, size, wHint, hHint);
		} catch (Throwable t) {
			Debug.out(t);
			return new Point(wHint == -1 ? 10 : wHint, hHint == -1 ? 10
					: hHint);
		}
	}

	protected Point betterComputeSize(Composite c, Point size, int wHint,
			int hHint) {
		if (c.getChildren().length == 0 && (size.x == 64 || size.y == 64)) {
			Object ld = c.getLayoutData();
			if (ld instanceof FormData) {
				FormData fd = (FormData) ld;
				if (fd.width != 0 && fd.height != 0) {
					Rectangle trim = c.computeTrim (0, 0, fd.width, fd.height);
					return new Point(trim.width, trim.height);
				}
			}
			return new Point(1, 1);
		}
		if (size.x == 0 || size.y == 0) {
			return size;
		}
		if (minWidth > 0 && size.x < minWidth) {
			size.x = minWidth;
		}
		if (minHeight > 0 && size.y < minHeight) {
			size.y = minHeight;
		}
		return size;
	}

	protected Point betterComputeSize(Composite c, Point size, int wHint, int hHint, boolean changed) {
		if (c.getChildren().length == 0 && (size.x == 64 || size.y == 64)) {
			Object ld = c.getLayoutData();
			if (ld instanceof FormData) {
				FormData fd = (FormData) ld;
				if (fd.width != 0 && fd.height != 0) {
					Rectangle trim = c.computeTrim (0, 0, fd.width, fd.height);
					return new Point(trim.width, trim.height);
				}
			}
			return new Point(1, 1);
		}
		if (size.x == 0 || size.y == 0) {
			return size;
		}
		if (minWidth > 0 && size.x < minWidth) {
			size.x = minWidth;
		}
		if (minHeight > 0 && size.y < minHeight) {
			size.y = minHeight;
		}
		return size;
	}
}
