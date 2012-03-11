/*
 * Created on 22 juin 2005
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.views.stats;

import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;

import com.aelitis.azureus.core.dht.control.DHTControlContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPosition;
import com.aelitis.azureus.core.dht.netcoords.vivaldi.ver1.*;
import com.aelitis.azureus.core.dht.netcoords.vivaldi.ver1.impl.*;
import com.aelitis.azureus.ui.swt.utils.ColorCache;

public class VivaldiPanel {
  private static final int ALPHA_FOCUS = 255;
  private static final int ALPHA_NOFOCUS = 150;

  Display display;
  Composite parent;
  
  Canvas canvas;
  Scale scale;
  
  private boolean mouseLeftDown = false;
  private boolean mouseRightDown = false;
  private int xDown;
  private int yDown;
  
  private boolean antiAliasingAvailable = true;
	private List<DHTControlContact> lastContacts;
	private DHTTransportContact lastSelf;
	
	private Image img;
	
  private int alpha = 255;
  
  private boolean autoAlpha = false;

  
  private class Scale {
    int width;
    int height;
    
    float minX = -1000;
    float maxX = 1000;
    float minY = -1000;
    float maxY = 1000;
    double rotation = 0;
    
    float saveMinX;
    float saveMaxX;
    float saveMinY;
    float saveMaxY;
    double saveRotation;
    
    public int getX(float x,float y) {
      return (int) (((x * Math.cos(rotation) + y * Math.sin(rotation))-minX)/(maxX - minX) * width);
    }
    
    public int getY(float x,float y) {
      return (int) (((y * Math.cos(rotation) - x * Math.sin(rotation))-minY)/(maxY-minY) * height);
    }
  }
  
  public VivaldiPanel(Composite parent) {
    this.parent = parent;
    this.display = parent.getDisplay();
    this.canvas = new Canvas(parent,SWT.NO_BACKGROUND);
    
    this.scale = new Scale();
    
  	canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				if (img != null && !img.isDisposed()) {
					Rectangle bounds = img.getBounds();
					if (bounds.width >= e.width && bounds.height >= e.height) {
						if (alpha != 255) {
							try {
								e.gc.setAlpha(alpha);
						  } catch (Exception ex) {
						  	// Ignore ERROR_NO_GRAPHICS_LIBRARY error or any others
						  }
						}
						e.gc.drawImage(img, e.x, e.y, e.width, e.height, e.x, e.y,
								e.width, e.height);
					}
				} else {
					e.gc.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
					e.gc.fillRectangle(e.x, e.y, e.width, e.height);
					e.gc.drawText(MessageText.getString("VivaldiView.notAvailable"), 10,
							10, true);
				}
			}
		});
    
    canvas.addMouseListener(new MouseAdapter() {

      public void mouseDown(MouseEvent event) {
        if(event.button == 1) mouseLeftDown = true;
        if(event.button == 3) mouseRightDown = true;
        xDown = event.x;
        yDown = event.y;
        scale.saveMinX = scale.minX;
        scale.saveMaxX = scale.maxX;
        scale.saveMinY = scale.minY;
        scale.saveMaxY = scale.maxY;
        scale.saveRotation = scale.rotation;
      }
      
      public void mouseUp(MouseEvent event) {
        if(event.button == 1) mouseLeftDown = false;
        if(event.button == 3) mouseRightDown = false;
        refreshContacts(lastContacts, lastSelf);
      }                  
    });
    
    canvas.addListener(SWT.KeyDown, new Listener() {
			public void handleEvent(Event event) {
			}
		});

    canvas.addListener(SWT.MouseWheel, new Listener() {
			public void handleEvent(Event event) {
				// System.out.println(event.count);
        scale.saveMinX = scale.minX;
        scale.saveMaxX = scale.maxX;
        scale.saveMinY = scale.minY;
        scale.saveMaxY = scale.maxY;
        
        int deltaY = event.count * 5;
        // scaleFactor>1 means zoom in, this happens when
        // deltaY<0 which happens when the mouse is moved up.
        float scaleFactor = 1 - (float) deltaY / 300;
        if(scaleFactor <= 0) scaleFactor = 0.01f;

        // Scalefactor of e.g. 3 makes elements 3 times larger
        float moveFactor = 1 - 1/scaleFactor;
        
        float centerX = (scale.saveMinX + scale.saveMaxX)/2;
        scale.minX = scale.saveMinX + moveFactor * (centerX - scale.saveMinX);
        scale.maxX = scale.saveMaxX - moveFactor * (scale.saveMaxX - centerX);

        float centerY = (scale.saveMinY + scale.saveMaxY)/2;
        scale.minY = scale.saveMinY + moveFactor * (centerY - scale.saveMinY);
        scale.maxY = scale.saveMaxY - moveFactor * (scale.saveMaxY - centerY);
        refreshContacts(lastContacts, lastSelf);
			}
		});

    canvas.addMouseMoveListener(new MouseMoveListener() {
      public void mouseMove(MouseEvent event) {
        if(mouseLeftDown && (event.stateMask & SWT.MOD4) == 0) {
          int deltaX = event.x - xDown;
          int deltaY = event.y - yDown;
          float width = scale.width;
          float height = scale.height;
          float ratioX = (scale.saveMaxX - scale.saveMinX) / width;
          float ratioY = (scale.saveMaxY - scale.saveMinY) / height;
          float realDeltaX = deltaX * ratioX;
          float realDeltaY  = deltaY * ratioY;
          scale.minX = scale.saveMinX - realDeltaX;
          scale.maxX = scale.saveMaxX - realDeltaX;
          scale.minY = scale.saveMinY - realDeltaY;
          scale.maxY = scale.saveMaxY - realDeltaY;
          refreshContacts(lastContacts, lastSelf);
        }
        if(mouseRightDown || (mouseLeftDown && (event.stateMask & SWT.MOD4) > 0)) {
          int deltaX = event.x - xDown;
          scale.rotation = scale.saveRotation - (float) deltaX / 100;

          int deltaY = event.y - yDown;
          // scaleFactor>1 means zoom in, this happens when
          // deltaY<0 which happens when the mouse is moved up.
          float scaleFactor = 1 - (float) deltaY / 300;
          if(scaleFactor <= 0) scaleFactor = 0.01f;

          // Scalefactor of e.g. 3 makes elements 3 times larger
          float moveFactor = 1 - 1/scaleFactor;
          
          float centerX = (scale.saveMinX + scale.saveMaxX)/2;
          scale.minX = scale.saveMinX + moveFactor * (centerX - scale.saveMinX);
          scale.maxX = scale.saveMaxX - moveFactor * (scale.saveMaxX - centerX);

          float centerY = (scale.saveMinY + scale.saveMaxY)/2;
          scale.minY = scale.saveMinY + moveFactor * (centerY - scale.saveMinY);
          scale.maxY = scale.saveMaxY - moveFactor * (scale.saveMaxY - centerY);
          refreshContacts(lastContacts, lastSelf);
        }
      }
    });

  	canvas.addMouseTrackListener(new MouseTrackListener() {
			public void mouseHover(MouseEvent e) {
			}
		
			public void mouseExit(MouseEvent e) {
				if (autoAlpha) {
					setAlpha(ALPHA_NOFOCUS);
				}
			}
		
			public void mouseEnter(MouseEvent e) {
				if (autoAlpha) {
					setAlpha(ALPHA_FOCUS);
				}
			}
		});
  }
  
  public void setLayoutData(Object data) {
    canvas.setLayoutData(data);
  }
  
	public void refreshContacts(List<DHTControlContact> contacts,
			DHTTransportContact self) {
  	if (contacts == null || self == null) {
  		return;
  	}
    lastContacts = contacts;
    lastSelf = self;
  	
    if(canvas.isDisposed()) return;
    Rectangle size = canvas.getBounds();
    
    if (size.isEmpty()) {
    	return;
    }
    
    scale.width = size.width;
    scale.height = size.height;
    
    Color white = ColorCache.getColor(display,255,255,255);
    Color blue = ColorCache.getColor(display,66,87,104);
    
    if (img != null && !img.isDisposed()) {
    	img.dispose();
    }
    
    img = new Image(display,size);
    
    GC gc = new GC(img);    
    
    gc.setForeground(white);
    gc.setBackground(white);
    
    gc.fillRectangle(size);
    
    if(SWT.getVersion() >= 3138 && antiAliasingAvailable) {
    	try {
    		//gc.setTextAntialias(SWT.ON);
    		//gc.setAntialias(SWT.ON);
      } catch(Exception e) {
        antiAliasingAvailable = false;
      }
    }
    
    
    gc.setForeground(blue);
    gc.setBackground(white);     
    
    DHTNetworkPosition _ownPosition = self.getNetworkPosition(DHTNetworkPosition.POSITION_TYPE_VIVALDI_V1);

    if ( _ownPosition == null ){
    	return;
    }
    
    VivaldiPosition ownPosition = (VivaldiPosition)_ownPosition;
    float ownErrorEstimate = ownPosition.getErrorEstimate();
    HeightCoordinatesImpl ownCoords =
    	(HeightCoordinatesImpl) ownPosition.getCoordinates();
    
    gc.drawText("Our error: " + ownErrorEstimate,10,10);
    
    Color black = ColorCache.getColor(display, 0, 0, 0);
    gc.setBackground(black); // Color of the squares

    // Draw all known positions of other contacts
    for (DHTControlContact contact : contacts) {
      DHTNetworkPosition _position = contact.getTransportContact().getNetworkPosition(DHTNetworkPosition.POSITION_TYPE_VIVALDI_V1);
      if ( _position == null ){
    	  continue;
      }
      VivaldiPosition position = (VivaldiPosition)_position;
      HeightCoordinatesImpl coord = (HeightCoordinatesImpl) position.getCoordinates();
      if(coord.isValid()) {
        draw(gc,coord.getX(),coord.getY(),coord.getH(),contact,(int)ownCoords.distance(coord),position.getErrorEstimate());
      }
    }
    
    // Mark our own position
    Color red = ColorCache.getColor(display, 255, 0, 0);
		gc.setForeground(red);
    drawSelf(gc, ownCoords.getX(), ownCoords.getY(),
						 ownCoords.getH(), ownErrorEstimate);
    
    
    gc.dispose();
    
    canvas.redraw();
  }
  
  public void refresh(List<VivaldiPosition> vivaldiPositions) {
    if(canvas.isDisposed()) return;
    Rectangle size = canvas.getBounds();
    
    scale.width = size.width;
    scale.height = size.height;
    
    if (img != null && !img.isDisposed()) {
    	img.dispose();
    }
    
    img = new Image(display,size);
    GC gc = new GC(img);
    
    Color white = ColorCache.getColor(display,255,255,255);
    gc.setForeground(white);
    gc.setBackground(white);
    gc.fillRectangle(size);
    
    Color blue = ColorCache.getColor(display,66,87,104);
    gc.setForeground(blue);
    gc.setBackground(blue);
    
    
    
    for (VivaldiPosition position : vivaldiPositions) {
      HeightCoordinatesImpl coord = (HeightCoordinatesImpl) position.getCoordinates();
      
      float error = position.getErrorEstimate() - VivaldiPosition.ERROR_MIN;
      if(error < 0) error = 0;
      if(error > 1) error = 1;
      int blueComponent = (int) (255 - error * 255);
      int redComponent = (int) (255*error);
      // Don't use ColorCache, as our color creation is temporary and
      // varying
      Color drawColor = new Color(display,redComponent,50,blueComponent);      
      gc.setForeground(drawColor);
      draw(gc,coord.getX(),coord.getY(),coord.getH());
      drawColor.dispose();
    }
    
    gc.dispose();
    
    canvas.redraw();
  }
  
  private void draw(GC gc,float x,float y,float h) {
    int x0 = scale.getX(x,y);
    int y0 = scale.getY(x,y);   
    gc.fillRectangle(x0-1,y0-1,3,3);   
    gc.drawLine(x0,y0,x0,(int)(y0-200*h/(scale.maxY-scale.minY)));
  }
  
  private void draw(GC gc,float x,float y,float h,DHTControlContact contact,int distance,float error) {
    if(x == 0 && y == 0) return;    
    if(error > 1) error = 1;
    int errDisplay = (int) (100 * error);
    int x0 = scale.getX(x,y);
    int y0 = scale.getY(x,y);   
    gc.fillRectangle(x0-1,y0-1,3,3);   
    //int elevation =(int) ( 200*h/(scale.maxY-scale.minY));
    //gc.drawLine(x0,y0,x0,y0-elevation);
    String text = /*contact.getTransportContact().getAddress().getAddress().getHostAddress() + " (" + */distance + " ms \nerr:"+errDisplay+"%";
    int lineReturn = text.indexOf("\n");
    int xOffset = gc.getFontMetrics().getAverageCharWidth() * (lineReturn != -1 ? lineReturn:text.length()) / 2;
    gc.drawText(text,x0-xOffset,y0,true);
  }
  
  // Mark our own position
  private void drawSelf(GC gc, float x, float y, float h, float errorEstimate){
  	int x0 = scale.getX(x, y);
		int y0 = scale.getY(x, y);
		//gc.drawOval(x0-50, y0-50, 100, 100);
		gc.drawLine(x0-15, y0, x0+15, y0); // Horizontal
		gc.drawLine(x0, y0-15, x0, y0+15); // Vertical
  }
  
	public int getAlpha() {
		return alpha;
	}

	public void setAlpha(int alpha) {
		this.alpha = alpha;
		if (canvas != null && !canvas.isDisposed()) {
			canvas.redraw();
		}
	}

	public void setAutoAlpha(boolean autoAlpha) {
		this.autoAlpha = autoAlpha;
		if (autoAlpha) {
			setAlpha(canvas.getDisplay().getCursorControl() == canvas ? ALPHA_FOCUS : ALPHA_NOFOCUS);
		}
	}
	
	public void delete()
	{
		if(img != null && !img.isDisposed())
		{
			img.dispose();
		}
	}
}
