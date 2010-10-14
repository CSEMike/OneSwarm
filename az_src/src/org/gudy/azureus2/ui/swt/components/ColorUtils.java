package org.gudy.azureus2.ui.swt.components;

/*
 * Created on 1-Feb-2005
 * Created by James Yeh
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * Facilitates utility methods for handling SWT Colors
 * @author James Yeh
 */
public final class ColorUtils
{
    /**
     * <p>Gets a new SWT color that has been uniformly added with delta</p>
     * <p>There is range checking such that the new RGB values are 0 <= x <= 255</p>
     * @param original Original color
     * @param delta Delta in 8bit RGB values
     * @return New color
     */
    public static final Color getShade(final Color original, final int delta)
    {
        final RGB newRGB = new RGB(
                Math.min(255, Math.max(0, original.getRed() + delta)),
                Math.min(255, Math.max(0, original.getGreen() + delta)),
                Math.min(255, Math.max(0, original.getBlue() + delta))
        );
        return new Color(Display.getDefault(), newRGB);
    }

    /**
     * <p>Gets a new SWT color that has been uniformly added with delta</p>
     * <p>If the a RGB value goes out of bounds, the delta will be applied negatively</p>
     * @param original Original color
     * @param delta Delta in 8bit RGB values
     * @return New color
     */
    public static final Color getBoundedShade(final Color original, final int delta)
    {
        final RGB newRGB = new RGB(
                getBoundedShade(original.getRed(), delta),
                getBoundedShade(original.getGreen(), delta),
                getBoundedShade(original.getBlue(), delta)
        );

        return new Color(Display.getDefault(), newRGB);
    }

    /**
     * <p>Gets a 8bit RGB value that has been added with delta</p>
     * <p>If the RGB value goes out of bounds, the delta will be applied negatively</p>
     * @param origValue Original RGB value
     * @param delta Delta
     * @return New RGB value
     */
    private static int getBoundedShade(final int origValue, final int delta)
    {
        int result = origValue + delta;
        if(result > 255)
        {
            result = origValue - delta;

            if(result < 0)
            {
                result = origValue;
            }
        }
        else if(result < 0)
        {
            result = origValue - delta;

            if(result > 255)
            {
                result = origValue;
            }
        }

        return result;
    }
}
