package org.gudy.azureus2.ui.swt.views.utils;

import org.eclipse.swt.graphics.Rectangle;

public class CoordinateTransform
{
		public CoordinateTransform(Rectangle exteriorBounds)
		{
			extWidth = exteriorBounds.width;
			extHeight = exteriorBounds.height;
		}
	
		final int extWidth;
		final int extHeight;
		
		int offsetX = 0;
		int offsetY = 0;
		double scaleX = 1.0;
		double scaleY = 1.0;
		
		public int x(int x)
		{
			return (int)(Math.round(offsetX+x*scaleX));
		}
		
		public int y(int y)
		{
			return (int)(Math.round(offsetY+y*scaleY));
		}

        private int w(int w)
		{
			return (int)Math.round(w*scaleX);
		}		

        private int h(int h)
		{
			return (int)Math.ceil(h*scaleY);
		}
		
		public void scale(double x, double y)
		{
			scaleX *= x;
			scaleY *= y;
		}
		
		public void shiftExternal(int x, int y)
		{
			offsetX += x;
			offsetY += y;
		}
		
		
		public void shiftInternal(int x, int y)
		{
			offsetX += x*scaleX;
			offsetY += y*scaleY;
		}

        private void calcFromDimensions(int internalWidth, int internalHeight, int marginLeft, int marginRight, int marginTop, int marginBottom, boolean leftToRight, boolean topDown)
		{
			shiftExternal(leftToRight ? 0 : extWidth,topDown ? 0 : extHeight);
			scale(leftToRight ? 1.0 : -1.0, topDown ? 1.0 : -1.0);
			shiftInternal(leftToRight ? marginLeft : marginRight, topDown ? marginTop : marginBottom);
			scale(Math.round((extWidth-marginLeft-marginRight)/(1.0*internalWidth)),Math.round((extHeight-marginTop-marginBottom)/(1.0*internalHeight)));
		}
	}