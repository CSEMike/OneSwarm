/*
 * Created on Jul 12, 2006 2:47:29 PM
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
package com.aelitis.azureus.ui;

import java.util.*;

import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;


/**
 * @author TuxPaper
 * @created Jul 12, 2006
 *
 */
public class UIFunctionsManager
{
	private static UIFunctions instance = null;
	
	private static List<UIFRunnable>	pending;
	
	public static UIFunctions 
	getUIFunctions() 
	{
		return instance;
	}
	
	public static void 
	setUIFunctions(
		UIFunctions uiFunctions )
	{
		final List<UIFRunnable> to_run;
		
		synchronized( UIFunctionsManager.class ){
		
			instance = uiFunctions;
							
			to_run = pending;
				
			pending = null;
		}
		
		if ( to_run != null ){
			
			new AEThread2( "UIFM:set" )
			{
				public void
				run()
				{
					for ( UIFRunnable r: to_run ){
						
						try{
							r.run( instance );
							
						}catch( Throwable e ){
							
							Debug.out( e );
						}
					}
				}
			}.start();
		}
	}
	
	public static void
	runWithUIF(
		UIFRunnable		target )
	{
		synchronized( UIFunctionsManager.class ){
			
			if ( instance == null ){
				
				if ( pending == null ){
					
					pending = new ArrayList<UIFRunnable>();
				}
				
				pending.add( target );
				
				return;
			}
		}
		
		target.run( instance );
	}
	
	public interface
	UIFRunnable
	{
		public void
		run(
			UIFunctions	uif );	
	}
}
