/*
 * Created on 2 mai 2004 Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details (
 * see the LICENSE file ).
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * AELITIS, SAS au capital de 46,603.30 euros, 8 Alle Lenotre, La Grille Royale,
 * 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.mainwindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.swt.utils.ColorCache;
/**
 * @author Olivier Chalouhi
 * @author MjrTom
 *			2005/Dec/08: green
 *  
 */
public class Colors implements ParameterListener {
	private static final LogIDs LOGID = LogIDs.GUI;
  private static Colors instance = null;
  public static final int BLUES_LIGHTEST = 0;
  public static final int BLUES_DARKEST = 9;
  public static final int BLUES_MIDLIGHT = (BLUES_DARKEST + 1) / 4;
  public static final int BLUES_MIDDARK = ((BLUES_DARKEST + 1) / 2)
      + BLUES_MIDLIGHT;
  public static final int FADED_LIGHTEST = 0;
  public static final int FADED_DARKEST = 9;
  
  public static Color[] blues = new Color[BLUES_DARKEST + 1];
  public static Color[] faded = new Color[FADED_DARKEST + 1];
  public static Color colorProgressBar;
  public static Color colorInverse;
  public static Color colorShiftLeft;
  public static Color colorShiftRight;
  public static Color colorError;
  public static Color colorErrorBG;
  public static Color colorAltRow;
  public static Color colorWarning;
  public static Color black;
  public static Color light_grey;
  public static Color blue;
  public static Color green;
  public static Color fadedGreen;
  public static Color grey;
  public static Color red;
  public static Color fadedRed;
  public static Color yellow;
  public static Color fadedYellow;
  public static Color white;
  public static Color background;
  public static Color red_ConsoleView;
  
  private static AEMonitor	class_mon	= new AEMonitor( "Colors" );
	public static int diffHue;
	public static float diffSatPct;
	public static float diffLumPct;
  
  private void allocateBlues() {    
    int r = 0;
    int g = 128;
    int b = 255;
    try {
      r = COConfigurationManager.getIntParameter("Color Scheme.red", r);
      g = COConfigurationManager.getIntParameter("Color Scheme.green", g);
      b = COConfigurationManager.getIntParameter("Color Scheme.blue", b);
      
      boolean bGrayScale = (r == b) && (b == g);
      
      HSLColor hslDefault = new HSLColor();
      hslDefault.initHSLbyRGB(0, 128, 255);
      
      HSLColor hslScheme = new HSLColor();
      hslScheme.initHSLbyRGB(r, g, b);
      
      diffHue = hslScheme.getHue() - hslDefault.getHue();
      diffSatPct = hslScheme.getSaturation() == 0 ? 0 : (float) hslDefault.getSaturation() / hslScheme.getSaturation();
      diffLumPct = hslScheme.getLuminence() == 0 ? 0 : (float) hslDefault.getLuminence() / hslScheme.getLuminence();

      HSLColor hslColor = new HSLColor();
      Color colorTables = display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
      int tR = colorTables.getRed();
      int tG = colorTables.getGreen();
      int tB = colorTables.getBlue();
      
      // 0 == window background (white)
      // [blues.length-1] == rgb
      // in between == blend
      for (int i = 0; i < blues.length; i++) {
        hslColor.initHSLbyRGB(r, g, b);
        float blendBy = (i == 0) ? 1 : (float) 1.0
            - ((float) i / (float) (blues.length - 1));
        hslColor.blend(tR, tG, tB, blendBy);
        blues[i] = ColorCache.getColor(display, hslColor.getRed(),
						hslColor.getGreen(), hslColor.getBlue());
        int iSat = hslColor.getSaturation();
        int luminence = hslColor.getLuminence();
        if (luminence < 20) {
          if (iSat > 10) {
            hslColor.setSaturation(iSat / 2);
            hslColor.brighten(1.25f);
          } else if (bGrayScale) {
          	// gray
          	hslColor.brighten(1.2f);
          }
        } else {
          if (iSat > 10) {
            hslColor.setSaturation(iSat / 2);
            hslColor.brighten(0.75f);
          } else if (bGrayScale) {
          	// gray
          	hslColor.brighten(0.8f);
          }
        }

        faded[i] = ColorCache.getColor(display, hslColor.getRed(),
						hslColor.getGreen(), hslColor.getBlue());
      }
      
      if (bGrayScale) {
      	if (b > 200)
      		b -= 20;
      	else
      		b += 20;
      }
      hslColor.initHSLbyRGB(r, g, b);
      hslColor.reverseColor();
      colorInverse = ColorCache.getColor(display, hslColor.getRed(),
					hslColor.getGreen(), hslColor.getBlue());

      hslColor.initHSLbyRGB(r, g, b);
      hslColor.setHue(hslColor.getHue() + 25);
      colorShiftRight = ColorCache.getColor(display, hslColor.getRed(),
					hslColor.getGreen(), hslColor.getBlue());

      hslColor.initHSLbyRGB(r, g, b);
      hslColor.setHue(hslColor.getHue() - 25);
      colorShiftLeft = ColorCache.getColor(display, hslColor.getRed(),
					hslColor.getGreen(), hslColor.getBlue());
    } catch (Exception e) {
    	Logger.log(new LogEvent(LOGID, "Error allocating colors", e));
    }
  }
  
  private void allocateColorProgressBar() {
		if (display == null || display.isDisposed())
			return;

		colorProgressBar = new AllocateColor("progressBar", colorShiftRight,
				colorProgressBar).getColor();
	}

  private void allocateColorErrorBG() {
		if (display == null || display.isDisposed())
			return;

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				Color colorTables = display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
				HSLColor hslColor = new HSLColor();
				hslColor.initHSLbyRGB(colorTables.getRed(), colorTables.getGreen(),
						colorTables.getBlue());
				int lum = hslColor.getLuminence();
				int sat = hslColor.getSaturation();

				lum = (int)((lum > 127) ? lum * 0.8 : lum * 1.3);
				
				if (sat == 0) {
					sat = 80;
				}
				
				hslColor.initRGBbyHSL(0, sat, lum);
				
				colorErrorBG = new AllocateColor("errorBG", new RGB(hslColor.getRed(),
						hslColor.getGreen(), hslColor.getBlue()), colorErrorBG).getColor();
			}
		}, false);
	}

	private void allocateColorError() {
		if (display == null || display.isDisposed())
			return;

		colorError = new AllocateColor("error", new RGB(255, 68, 68), colorError)
				.getColor();
	}

	private void allocateColorWarning() {
		if (display == null || display.isDisposed())
			return;

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				Color colorTables = display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
				HSLColor hslBG = new HSLColor();
				hslBG.initHSLbyRGB(colorTables.getRed(), colorTables.getGreen(),
						colorTables.getBlue());
				int lum = hslBG.getLuminence();

				HSLColor hslColor = new HSLColor();
				hslColor.initRGBbyHSL(25, 200, 128 + (lum < 160 ? 10 : -10));
				colorWarning = new AllocateColor("warning", new RGB(hslColor.getRed(),
						hslColor.getGreen(), hslColor.getBlue()), colorWarning).getColor();
			}
		}, false);
	}

	private void allocateColorAltRow() {
		if (display == null || display.isDisposed())
			return;

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				Color colorTables = display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
				HSLColor hslColor = new HSLColor();
				hslColor.initHSLbyRGB(colorTables.getRed(), colorTables.getGreen(),
						colorTables.getBlue());

				int lum = hslColor.getLuminence();
				int sat = hslColor.getSaturation();
				int hue = hslColor.getHue();
				if (lum > 127) {
					lum -= 10;
					sat = 127;
					hue = 155;
				} else {
					lum += 30; // it's usually harder to see difference in darkness
				}
				hslColor.setLuminence(lum);
				hslColor.setHue(hue);
				hslColor.setSaturation(sat);
				
				//HSLColor blueHSL = new HSLColor();
				//RGB rgb = blues[BLUES_DARKEST].getRGB();
				//blueHSL.initHSLbyRGB(rgb.red, rgb.green, rgb.blue);
				//int blueHue = blueHSL.getHue();
				//int altHue = hslColor.getHue();
				//if (blueHue > altHue) {
				//	altHue += 11;
				//} else {
				//	altHue -= 11;
				//}
				//hslColor.setHue(blueHue);
				colorAltRow = new AllocateColor("altRow", new RGB(hslColor.getRed(),
						hslColor.getGreen(), hslColor.getBlue()), colorAltRow).getColor();
			}
		}, false);
	}

  /** Allocates a color */
  private class AllocateColor extends AERunnable {
    private String sName;
    private RGB rgbDefault;
    private Color newColor;
    
    public AllocateColor(String sName, RGB rgbDefault, Color colorOld) {
      this.sName = sName;
      this.rgbDefault = rgbDefault;
    }
    
    public AllocateColor(String sName, final Color colorDefault, Color colorOld) {
			this.sName = sName;
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					if (!colorDefault.isDisposed())
						AllocateColor.this.rgbDefault = colorDefault.getRGB();
					else
						AllocateColor.this.rgbDefault = new RGB(0, 0, 0);
				}
			}, false);
		}
    
    public Color getColor() {
    	Utils.execSWTThread(this, false);
      return newColor;
    }

    public void runSupport() {
      if (COConfigurationManager.getBooleanParameter("Colors." + sName + ".override")) {
        newColor = ColorCache.getColor(display,
           COConfigurationManager.getIntParameter("Colors." + sName + ".red", 
                                                  rgbDefault.red),
           COConfigurationManager.getIntParameter("Colors." + sName + ".green",
                                                  rgbDefault.green),
           COConfigurationManager.getIntParameter("Colors." + sName + ".blue",
                                                  rgbDefault.blue));
      } else {
        newColor = ColorCache.getColor(display, rgbDefault.red,
        		rgbDefault.green, rgbDefault.blue);
        // Since the color is not longer overriden, reset back to default
        // so that the user sees the correct color in Config.
        COConfigurationManager.setRGBParameter("Colors." + sName,
						rgbDefault.red, rgbDefault.green, rgbDefault.blue); 
      }
    }
  }
  
  private void allocateDynamicColors() {
    if(display == null || display.isDisposed())
      return;
    
    Utils.execSWTThread(new AERunnable(){
      public void runSupport() {
        allocateBlues();
        allocateColorProgressBar();
        allocateColorErrorBG();
      }
    }, false);
  }

  private void allocateNonDynamicColors() {
		allocateColorWarning();
		allocateColorError();
		allocateColorAltRow();

		black = ColorCache.getColor(display, 0, 0, 0);
		light_grey = ColorCache.getColor(display, 192, 192, 192);
		blue = ColorCache.getColor(display, 0, 0, 170);
		green = ColorCache.getColor(display, 0, 170, 0);
		fadedGreen = ColorCache.getColor(display, 96, 160, 96);
		grey = ColorCache.getColor(display, 170, 170, 170);
		red = ColorCache.getColor(display, 255, 0, 0);
		fadedRed = ColorCache.getColor(display, 160, 96, 96);
		yellow = ColorCache.getColor(display, 255, 255, 0);
		fadedYellow = ColorCache.getColor(display, 255, 255, 221);
		white = ColorCache.getColor(display, 255, 255, 255);
		background = ColorCache.getColor(display, 248, 248, 248);
		red_ConsoleView = ColorCache.getColor(display, 255, 192, 192);
	}   
  
  private Display display;
  
  private Colors() {
    instance = this;
    try {
    	display = SWTThread.getInstance().getDisplay();
    } catch (Exception e) {
    	display = Display.getDefault();
    }
    allocateDynamicColors();
    allocateNonDynamicColors();

    addColorsChangedListener(this);
  }
  
  public static Colors getInstance() {
  	try{
  		class_mon.enter();
	    if (instance == null)
	      instance = new Colors();
	
	    return instance;
  	}finally{
  		
  		class_mon.exit();
  	}
  }
  
  public void addColorsChangedListener(ParameterListener l) {
    COConfigurationManager.addParameterListener("Color Scheme", l);
    COConfigurationManager.addParameterListener("Colors.progressBar.override", l);
    COConfigurationManager.addParameterListener("Colors.progressBar", l);
    COConfigurationManager.addParameterListener("Colors.error.override", l);
    COConfigurationManager.addParameterListener("Colors.error", l);
    COConfigurationManager.addParameterListener("Colors.warning.override", l);
    COConfigurationManager.addParameterListener("Colors.warning", l);
    COConfigurationManager.addParameterListener("Colors.altRow.override", l);
    COConfigurationManager.addParameterListener("Colors.altRow", l);
  }

  public void removeColorsChangedListener(ParameterListener l) {
    COConfigurationManager.removeParameterListener("Color Scheme", l);
    COConfigurationManager.removeParameterListener("Colors.progressBar.override", l);
    COConfigurationManager.removeParameterListener("Colors.progressBar", l);
    COConfigurationManager.removeParameterListener("Colors.error.override", l);
    COConfigurationManager.removeParameterListener("Colors.error", l);
    COConfigurationManager.removeParameterListener("Colors.warning.override", l);
    COConfigurationManager.removeParameterListener("Colors.warning", l);
    COConfigurationManager.removeParameterListener("Colors.altRow.override", l);
    COConfigurationManager.removeParameterListener("Colors.altRow", l);
  }
  
  public void parameterChanged(String parameterName) {
    if (parameterName.equals("Color Scheme")) {
      allocateDynamicColors();
    }

    if(parameterName.startsWith("Colors.progressBar")) {
      allocateColorProgressBar();      
    }
    if(parameterName.startsWith("Colors.error")) {
      allocateColorError();
    }
    if(parameterName.startsWith("Colors.warning")) {
      allocateColorWarning();
    }
    if(parameterName.startsWith("Colors.altRow")) {
      allocateColorAltRow();
    }
  }
}