/*
 * File    : ConfigureWizard.java
 * Created : 13-Oct-2003
 * By      : stuff
 * 
 * Azureus - a Java Bittorrent client
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
 */

package org.gudy.azureus2.ui.swt.exporttorrent.wizard;

/**
 * @author parg
 *
 */

import java.io.File;
import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.ui.swt.wizard.Wizard;

public class 
ExportTorrentWizard 
	extends Wizard 
{  
	String torrent_file = "";
	String export_file	= "";
  
	public 
	ExportTorrentWizard()
	{
		super("exportTorrentWizard.title");
	
		ExportTorrentWizardInputPanel input_panel = new ExportTorrentWizardInputPanel(this,null);
	
		setFirstPanel(input_panel);
	}
  
	public 
	ExportTorrentWizard(
		Display 		display,
		DownloadManager	dm )
	{
		super("exportTorrentWizard.title");
	
		setTorrentFile( dm.getTorrentFileName());
		
		ExportTorrentWizardOutputPanel output_panel = new ExportTorrentWizardOutputPanel(this,null);
	
		setFirstPanel(output_panel);
	}
	
 	public void 
 	onClose() 
 	{
 		// Call the parent class to clean up resources
 		super.onClose();	
 	}

	protected void
	setTorrentFile(
		String		str )
	{
		torrent_file = str;
		
		export_file = str + ".xml";
	}
  	
	protected String
	getTorrentFile()
	{
		return( torrent_file );
	}
	
	protected void
	setExportFile(
		String		str )
	{
		export_file = str;
	}
  	
	protected String
	getExportFile()
	{
		return( export_file );
	}
	
	protected boolean
	performExport()
	{
		File input_file;
		
		try{
			input_file = new File( getTorrentFile()).getCanonicalFile();
			
		}catch( IOException e ){
			
			MessageBox mb = new MessageBox(getWizardWindow(),SWT.ICON_ERROR | SWT.OK );
		
			mb.setText(MessageText.getString("exportTorrentWizard.process.inputfilebad.title"));
		
			mb.setMessage(	MessageText.getString("exportTorrentWizard.process.inputfilebad.message")+"\n" +
							e.toString());
			
			mb.open();
			
			return( false );
		}
		
		File output_file = new File( export_file );
		
		if ( output_file.exists()){
			
			MessageBox mb = new MessageBox(this.getWizardWindow(),SWT.ICON_QUESTION | SWT.YES | SWT.NO);
			
			mb.setText(MessageText.getString("exportTorrentWizard.process.outputfileexists.title"));
			
			mb.setMessage(MessageText.getString("exportTorrentWizard.process.outputfileexists.message"));
			
			int result = mb.open();
		
			if( result == SWT.NO ){
				
				return( false );
			}
		}

		String	error_title;
		String	error_detail;
		
		try{
		
			TOTorrent	torrent;
			
			try{
				
				torrent = TOTorrentFactory.deserialiseFromBEncodedFile( input_file );
	
				try{
					
					torrent.serialiseToXMLFile( output_file );
					
					return( true );
								
				}catch( TOTorrentException e ){
				
					error_title 	= MessageText.getString("exportTorrentWizard.process.exportfail.title");
				
					error_detail	= TorrentUtils.exceptionToText( e ); 
				}
			}catch( TOTorrentException e ){
				
				error_title 	= MessageText.getString("exportTorrentWizard.process.torrentfail.title");
				
				error_detail	= TorrentUtils.exceptionToText( e ); 
			}
			
		}catch( Throwable e ){

			error_title 	= MessageText.getString("exportTorrentWizard.process.unknownfail.title");
		
			error_detail 	= e.toString();
		}

		MessageBox mb = new MessageBox(this.getWizardWindow(),SWT.ICON_ERROR | SWT.OK );
			
		mb.setText(error_title);
			
		mb.setMessage(error_detail);
			
		mb.open();
		
		return( false );
	}
}