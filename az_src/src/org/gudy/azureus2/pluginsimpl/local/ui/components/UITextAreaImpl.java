/*
 * Created on 27-Apr-2004
 * Created by Paul Gardner
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

package org.gudy.azureus2.pluginsimpl.local.ui.components;

/**
 * @author parg
 *
 */

import java.io.*;
import java.util.Iterator;
import java.util.LinkedList;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AETemporaryFileHandler;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.FrequencyLimitedDispatcher;

import org.gudy.azureus2.plugins.ui.components.*;


public class 
UITextAreaImpl	
	extends		UIComponentImpl
	implements 	UITextArea
{
	private int	max_size		= DEFAULT_MAX_SIZE;
	
	PrintWriter pw;
	
	File file;
	
	boolean useFile = true;
	
	AEMonitor file_mon = new AEMonitor("filemon");
	
	LinkedList	delay_text	= new LinkedList();
	int			delay_size	= 0;
	
	FrequencyLimitedDispatcher	dispatcher = 
		new FrequencyLimitedDispatcher(
			new AERunnable()
			{
				public void
				runSupport()
				{
					delayAppend();
				}
			},
			500 );
	
	public
	UITextAreaImpl()
	{
		setText("");
	}
	
	public void
	setText(
		String		text )
	{
		if (useFile) {
			try {
				file_mon.enter();
				if (pw == null) {
					try {
						file = AETemporaryFileHandler.createTempFile();

						FileWriter fr = new FileWriter(file);
						pw = new PrintWriter(fr);

						pw.print(text);
						pw.flush();

						return;
					} catch (IOException e) {
					}
				}
			} finally {
				file_mon.exit();
			}
		}
		
		// has property change listener, or error while doing file (fallthrough)
		
		if ( text.length() > max_size ){
				
			int	size_to_show = max_size - 10000;
			
			if ( size_to_show < 0 ){
				
				size_to_show	= max_size;
			}
			
			text = text.substring( text.length() - size_to_show );
		}
		
		setProperty( PT_VALUE, text );
	}
		
	public void
	appendText(
		String		text )
	{
		if (useFile && pw != null) {
			try {
				file_mon.enter();
				pw.print(text);
				pw.flush();
				return;
			} finally {
				file_mon.exit();
			}
		}

		synchronized( this ){
			
			delay_text.addLast( text );
			
			delay_size += text.length();
			
			while( delay_size > max_size ){
		
				if ( delay_text.size() == 0 ){
					
					break;
				}
				
				String	s = (String)delay_text.removeFirst();
				
				delay_size -= s.length();
			}
		}
		
		dispatcher.dispatch();
	}
	
	protected void
	delayAppend()
	{
		String	str = getText();

		String	text;
		
		synchronized( this ){

			if ( delay_text.size() == 1 ){
				
				text = (String)delay_text.get(0);
				
			}else{
				
				StringBuffer sb = new StringBuffer( delay_size );
				
				Iterator	it = delay_text.iterator();
				
				while( it.hasNext()){
				
					sb.append((String)it.next());
				}
				
				text = sb.toString();
			}
			
			delay_text.clear();
			delay_size = 0;
		}
		
		if ( str == null ){
			
			setText( text );
			
		}else{
			
			setText( str + text );
		}
	}
	
	public String
	getText()
	{
		if (useFile && pw != null) {
			return getFileText();
		}

		return((String)getProperty( PT_VALUE ));
	}
	
	public void
	setMaximumSize(
		int	_max_size )
	{
		max_size	= _max_size;
	}
	
	private String getFileText() {
		boolean recreate = pw != null;

		try {
			file_mon.enter();

			if (recreate) {
				pw.close();
			}

			String text = null;
			try {
				text = FileUtil.readFileEndAsString(file, max_size);
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (text == null) {
				text = "";
			}

			if (recreate) {
				try {
					FileWriter fr = new FileWriter(file, true);
					pw = new PrintWriter(fr);
				} catch (IOException e) {
					useFile = false;
					e.printStackTrace();
				}
			}
			return text;
		} finally {
			file_mon.exit();
		}
	}
	
	public void addPropertyChangeListener(UIPropertyChangeListener l) {
		if (useFile) {
			if (pw != null) {
				try {
					file_mon.enter();

					pw.close();
					pw = null;
				} finally {
					file_mon.exit();
				}
			}

			useFile = false;
			setText(getFileText());
		}

		super.addPropertyChangeListener(l);
	}
}