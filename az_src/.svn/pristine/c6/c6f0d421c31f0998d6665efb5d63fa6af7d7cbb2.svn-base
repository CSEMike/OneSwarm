/*
 * Created on 13-Sep-2005
 * Created by Paul Gardner
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.ui.swt.components;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.config.impl.ConfigurationManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.swt.utils.ColorCache;

/**
 * 
 * TODO: have a callback when color changes
 */
public class Legend {
	/**
	 * Create a legend containing a modifyable color box and description
	 * 
	 * @param panel Where to add legend to
	 * @param blockColors array of colors for each legend entry.  This
	 *                     array WILL BE modified if the user changes the color
	 * @param keys array of keys for each legend entry
	 * @return The composite containing the legend
	 */
	public static Composite createLegendComposite(Composite panel,
			Color[] blockColors, String[] keys) {
		Object layout = panel.getLayout();
		Object layoutData = null;
		if (layout instanceof GridLayout)
			layoutData = new GridData(GridData.FILL_HORIZONTAL);

		return createLegendComposite(panel, blockColors, keys, layoutData);
	}


	/**
	 * Create a legend containing a modifyable color box and description
	 * 
	 * @param panel Where to add legend to
	 * @param blockColors array of colors for each legend entry.  This
	 *                     array WILL BE modified if the user changes the color
	 * @param keys array of keys for each legend entry
	 * @param layoutData How to layout the legend (ie. GridData, LayoutData, etc)
	 * @return The composite containing the legend
	 */
	public static Composite createLegendComposite(final Composite panel,
			final Color[] blockColors, final String[] keys, Object layoutData) {
		
		final ConfigurationManager config = ConfigurationManager.getInstance();

		if (blockColors.length != keys.length)
			return null;

		final Color[] defaultColors = new Color[blockColors.length];
		final ParameterListener[] paramListeners = new ParameterListener[keys.length];
		System.arraycopy(blockColors, 0, defaultColors, 0, blockColors.length);

		Composite legend = new Composite(panel, SWT.NONE);
		if (layoutData != null)
			legend.setLayoutData(layoutData);

		RowLayout layout = new RowLayout(SWT.HORIZONTAL);
		layout.wrap = true;
		layout.marginBottom = 0;
		layout.marginTop = 0;
		layout.marginLeft = 0;
		layout.marginRight = 0;
		layout.spacing = 0;
		legend.setLayout(layout);

		RowData data;
		for (int i = 0; i < blockColors.length; i++) {
			int r = config.getIntParameter(keys[i] + ".red", -1);
			if (r >= 0) {
				int g = config.getIntParameter(keys[i] + ".green");
				int b = config.getIntParameter(keys[i] + ".blue");
				
				Color color = ColorCache.getColor(panel.getDisplay(), r, g, b);
				blockColors[i] = color;
			}

			Composite colorSet = new Composite(legend, SWT.NONE);

			colorSet.setLayout(new RowLayout(SWT.HORIZONTAL));

			final Canvas cColor = new Canvas(colorSet, SWT.BORDER);
			cColor.setData("Index", new Integer(i));
			// XXX Use paint instead of setBackgrond, because OSX does translucent
			// crap
			cColor.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent e) {
					int i = ((Integer)cColor.getData("Index")).intValue();
					e.gc.setBackground(blockColors[i]);
					e.gc.fillRectangle(e.x, e.y, e.width, e.height);
				}
			});

			cColor.addMouseListener(new MouseAdapter() {
				public void mouseUp(MouseEvent e) {
					Integer iIndex = (Integer)cColor.getData("Index");
					if (iIndex == null)
						return;
					int index = iIndex.intValue();

					if (e.button == 1) {
						ColorDialog cd = new ColorDialog(panel.getShell());
						cd.setRGB(blockColors[index].getRGB());
						
						RGB rgb = cd.open();
						if (rgb != null)
							config.setRGBParameter(keys[index], rgb.red, rgb.green, rgb.blue);
					} else {
						config.removeRGBParameter(keys[index]);
					}
				}
			});

			Label lblDesc = new Label(colorSet, SWT.NULL);
			Messages.setLanguageText(lblDesc, keys[i]);

			data = new RowData();
			data.width = 20;
			data.height = lblDesc.computeSize(SWT.DEFAULT, SWT.DEFAULT).y - 3;
			cColor.setLayoutData(data);
			
			// If color changes, update our legend
			config.addParameterListener(keys[i],paramListeners[i] = new ParameterListener() {
				public void parameterChanged(String parameterName) {
					for (int j = 0; j < keys.length; j++) {
						if (keys[j].equals(parameterName)) {
							final int index = j;

							final int r = config.getIntParameter(keys[j] + ".red", -1);
							if (r >= 0) {
								final int g = config.getIntParameter(keys[j] + ".green");
								final int b = config.getIntParameter(keys[j] + ".blue");
								
								final RGB rgb = new RGB(r, g, b);
								if (blockColors[j].isDisposed()
										|| !rgb.equals(blockColors[j].getRGB())) {

									Utils.execSWTThread(new AERunnable() {
										public void runSupport() {
											if (panel == null || panel.isDisposed())
												return;
											Color color = ColorCache.getColor(panel.getDisplay(), r, g, b);
											blockColors[index] = color;
											cColor.redraw();
										}
									});
								}
							} else {
								if (blockColors[j].isDisposed()
										|| !blockColors[j].equals(defaultColors[j])) {
									Utils.execSWTThread(new AERunnable() {
										public void runSupport() {
											if (panel == null || panel.isDisposed())
												return;
											blockColors[index] = defaultColors[index];
											cColor.redraw();
										}
									});
								}
							}
						}
					}
				}
			});
		}
		
		legend.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				// We don't want to give them disposed colors
				// Restore defaults in case blockColors is a static or is used
				// afterwards, or if the view wants to dispose of the old colors.
				for (int i = 0; i < blockColors.length; i++)
					blockColors[i] = defaultColors[i];
				for (int i = 0; i < keys.length;i++)
					config.removeParameterListener(keys[i], paramListeners[i]);
			}
		});

		return legend;
	}
}
