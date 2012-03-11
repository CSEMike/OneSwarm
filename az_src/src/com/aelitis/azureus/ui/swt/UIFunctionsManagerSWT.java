/*
 * Created on Jul 13, 2006 12:28:36 PM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 */
package com.aelitis.azureus.ui.swt;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;

/**
 * @author TuxPaper
 * @created Jul 13, 2006
 *
 */
public class UIFunctionsManagerSWT extends UIFunctionsManager
{
	public static UIFunctionsSWT 
	getUIFunctionsSWT() 
	{
		UIFunctions uiFunctions = getUIFunctions();
		
		if (uiFunctions instanceof UIFunctionsSWT){
			
			return (UIFunctionsSWT)uiFunctions;
		}
		
		return null;
	}
	
	public static void
	runWithUIFSWT(
		final UIFSWTRunnable		target )
	{
		final boolean was_swt = Utils.isSWTThread();
		
		UIFunctionsManager.runWithUIF(
			new UIFRunnable()
			{
				public void 
				run(
					final UIFunctions uif )
				{
					if ( uif instanceof UIFunctionsSWT ){
					
						boolean is_swt = Utils.isSWTThread();
						
						if ( was_swt && !is_swt ){
							
							Utils.execSWTThread(
								new AERunnable() 
								{
									public void 
									runSupport()
									{
										target.run((UIFunctionsSWT)uif);
									}
								});
						}else{
						
							target.run((UIFunctionsSWT)uif);
						}
					}else{
						
						Debug.out( "Couldn't run " + target + " as uif not swt" );
					}
				}
			});
	}
	
	public interface
	UIFSWTRunnable
	{
		public void
		run(
			UIFunctionsSWT	uif_swt );	
	}
}
