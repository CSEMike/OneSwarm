package org.gudy.azureus2.ui.swt.components;

/*
 * Created on 8-Feb-2005
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

import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.config.COConfigurationManager;

/**
 * General utility methods for SWT controls and components
 * @version 1.0
 * @author James Yeh
 * @deprecated JFace has higher-level classes and fields to cover this
 */
public final class ControlUtils
{
    private static boolean smallOSXControl = COConfigurationManager.getBooleanParameter("enable_small_osx_fonts");

    /**
     * <p>Gets the margin between buttons</p>
     * <p>The margin may vary between platforms, as specified by their guidelines</p>
     * @return Margin
     */
    public static int getButtonMargin()
    {
        if(Constants.isOSX)
            return (smallOSXControl) ? 10 : 12;
        else if(Constants.isWindows)
            return 6;
        else
            return 6; // this is gnome's
    }

    /**
     * <p>Gets the minimum width of a button in a dialog (usually for alerts)</p>
     * <p>The size may vary between platforms, as specified by their guidelines</p>
     * @return Width
     */
    public static int getDialogButtonMinWidth()
    {
        if(Constants.isOSX)
            return 90;
        else
            return 70;
    }

    /**
     * <p>Gets the minimum height of a button in a dialog (usually for alerts)</p>
     * <p>The size may vary between platforms, as specified by their guidelines</p>
     * @return Height
     */
    public static int getDialogButtonMinHeight()
    {
        return 20;
    }
}
