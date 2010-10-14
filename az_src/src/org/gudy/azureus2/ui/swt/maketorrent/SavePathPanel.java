/*
 * File    : SavePathPanel.java
 * Created : 30 sept. 2003 17:06:45
 * By      : Olivier 
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
 
package org.gudy.azureus2.ui.swt.maketorrent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;

import java.io.File;

/**
 * @author Olivier
 * 
 */
public class SavePathPanel extends AbstractWizardPanel {

	protected long	file_size;
	protected long	piece_size;
	protected long	piece_count;	

  public SavePathPanel(NewTorrentWizard _wizard,AbstractWizardPanel _previousPanel) {
    super(_wizard,_previousPanel);
  }
  
  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.maketorrent.IWizardPanel#show()
   */
  public void show() {
  	
  	final NewTorrentWizard _wizard = (NewTorrentWizard)wizard;
  	
  	try{
  		file_size = TOTorrentFactory.getTorrentDataSizeFromFileOrDir(new File(_wizard.create_from_dir?_wizard.directoryPath:_wizard.singlePath));
  		
  		piece_size = TOTorrentFactory.getComputedPieceSize( file_size );
  		
  		piece_count = TOTorrentFactory.getPieceCount( file_size, piece_size );
  	}catch( Throwable e ){
  		Debug.printStackTrace( e );
  	}
    wizard.setTitle(MessageText.getString("wizard.torrentFile"));
    wizard.setCurrentInfo(MessageText.getString("wizard.choosetorrent"));
    Composite panel = wizard.getPanel();
    GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    panel.setLayout(layout);
    Label label;/* = new Label(panel,SWT.NULL);
    Messages.setLanguageText(label,"wizard.file");*/
    final Text file = new Text(panel,SWT.BORDER);
    
    file.addModifyListener(new ModifyListener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
       */
      public void modifyText(ModifyEvent arg0) {       
        String fName = file.getText();
        ((NewTorrentWizard)wizard).savePath = fName;
        String error = "";
        if(! fName.equals("")) {          
          File f = new File(file.getText());
          if(f.exists() || f.isDirectory()) {
            error = MessageText.getString("wizard.invalidfile");
          }else{           
            String	parent = f.getParent();
            
            if ( parent != null ){
            	
            	((NewTorrentWizard) wizard).setDefaultSaveDir( parent );
            }
          }
        }
        wizard.setErrorMessage(error);
        wizard.setFinishEnabled(!((NewTorrentWizard)wizard).savePath.equals("") && error.equals(""));
      }
    });
    
    	// if we have a default save dir then use this as the basis for save location
    
    String	target_file;
    
    if(((NewTorrentWizard)wizard).create_from_dir) {
    	target_file = ((NewTorrentWizard)wizard).directoryPath + ".torrent";
    } else {      
    	target_file = ((NewTorrentWizard)wizard).singlePath + ".torrent";
    }
    
    String	default_save = ((NewTorrentWizard)wizard).getDefaultSaveDir();
    
    if (default_save.length() > 0 ){
    
    	File temp = new File( target_file );
    	
    	String	existing_parent = temp.getParent();
    	
    	if ( existing_parent != null ){
    		
    		target_file	= new File( default_save, temp.getName()).toString();
    	}
    }
    
    ((NewTorrentWizard)wizard).savePath = target_file;
    
    file.setText(((NewTorrentWizard)wizard).savePath);
    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    file.setLayoutData(gridData);
    Button browse = new Button(panel,SWT.PUSH);
    browse.addListener(SWT.Selection,new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event arg0) {
        FileDialog fd = new FileDialog(wizard.getWizardWindow(),SWT.SAVE);
        final String path = ((NewTorrentWizard)wizard).savePath;
        if(wizard.getErrorMessage().equals("") && !path.equals("")) {
            File fsPath = new File(path);
            if(!path.endsWith(File.separator)) {
                fd.setFilterPath(fsPath.getParent());
                fd.setFileName(fsPath.getName());
            }
            else {
                fd.setFileName(path);
            }
        }
        String f = fd.open();
        if (f != null){
            file.setText(f);
            
            File	ff = new File(f);

            String	parent = ff.getParent();

            if ( parent != null )
                ((NewTorrentWizard) wizard).setDefaultSaveDir( parent );
          }
      }
    });   
    Messages.setLanguageText(browse,"wizard.browse");
 
    	// ----------------------
    
    label = new Label(panel, SWT.SEPARATOR | SWT.HORIZONTAL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 3;
    label.setLayoutData(gridData);
  
    Composite gFileStuff = new Composite(panel, SWT.NULL);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gridData.horizontalSpan = 3;
    gFileStuff.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 4;
    gFileStuff.setLayout(layout);
    
    	// file size
    
    label = new Label(gFileStuff, SWT.NULL);
    Messages.setLanguageText(label, "wizard.maketorrent.filesize");
    
    Label file_size_label = new Label(gFileStuff, SWT.NULL);
    file_size_label.setText( DisplayFormatters.formatByteCountToKiBEtc(file_size));
 
    label = new Label(gFileStuff, SWT.NULL);
    label = new Label(gFileStuff, SWT.NULL);
    
    	// piece count
    
    label = new Label(gFileStuff, SWT.NULL);
    Messages.setLanguageText(label, "wizard.maketorrent.piececount");
    
    final Label piece_count_label = new Label(gFileStuff, SWT.NULL);
    piece_count_label.setText( ""+piece_count );
    label = new Label(gFileStuff, SWT.NULL);
    label = new Label(gFileStuff, SWT.NULL);
    
   		// piece size
    
    label = new Label(gFileStuff, SWT.NULL);
    Messages.setLanguageText(label, "wizard.maketorrent.piecesize");
    
    final Label piece_size_label = new Label(gFileStuff, SWT.NULL);
    gridData = new GridData();
    gridData.widthHint = 75;
    piece_size_label.setLayoutData(gridData);
    piece_size_label.setText( DisplayFormatters.formatByteCountToKiBEtc( piece_size ));
    
    final Combo manual = new Combo(gFileStuff, SWT.SINGLE | SWT.READ_ONLY);
 
    final long[] sizes = TOTorrentFactory.STANDARD_PIECE_SIZES;
 
    manual.add( MessageText.getString( "wizard.maketorrent.auto"));
    
    for (int i=0;i<sizes.length;i++){
    	manual.add(DisplayFormatters.formatByteCountToKiBEtc(sizes[i]));
    }
    
    manual.select(0);
    
    manual.addListener(SWT.Selection, new Listener() {
    	public void 
    	handleEvent(
    			Event e) 
    	{
    		int	index = manual.getSelectionIndex();
    		
    		if ( index == 0 ){
    			
    			_wizard.setPieceSizeComputed();
    			
    			piece_size = TOTorrentFactory.getComputedPieceSize( file_size );
    			
     		}else{
    			piece_size = sizes[index-1];
    			
    			_wizard.setPieceSizeManual(piece_size);	
    		}
    		
    		piece_count = TOTorrentFactory.getPieceCount( file_size, piece_size );
 
    		piece_size_label.setText( DisplayFormatters.formatByteCountToKiBEtc(piece_size));
    		piece_count_label.setText( ""+piece_count );
    	}
    });
    
    label = new Label(gFileStuff, SWT.NULL);
    
    // ------------------------
    label = new Label(panel, SWT.SEPARATOR | SWT.HORIZONTAL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 3;
    label.setLayoutData(gridData);
    
    final Button bAutoOpen = new Button(panel,SWT.CHECK);
    Messages.setLanguageText(bAutoOpen,"wizard.maketorrents.autoopen");
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 3;
    bAutoOpen.setLayoutData(gridData);
    
    final Button bAutoHost = new Button(panel,SWT.CHECK);
    Messages.setLanguageText(bAutoHost,"wizard.maketorrents.autohost");
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 3;
    bAutoHost.setLayoutData(gridData);
    bAutoHost.setEnabled( false );
    
    bAutoOpen.addListener(SWT.Selection,new Listener() {
        public void handleEvent(Event event) {
          _wizard.autoOpen = bAutoOpen.getSelection();
          
          bAutoHost.setEnabled( _wizard.autoOpen && _wizard.getTrackerType() != NewTorrentWizard.TT_EXTERNAL );
        }
      });
    
    bAutoHost.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event event) {
        _wizard.autoHost = bAutoHost.getSelection();
      }
    });
    
    final Button bPrivateTorrent = new Button(panel,SWT.CHECK);
    Messages.setLanguageText(bPrivateTorrent,"ConfigView.section.sharing.privatetorrent");
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 3;
	bPrivateTorrent.setLayoutData(gridData);
   
		
    final Button bAllowDHT = new Button(panel,SWT.CHECK);
    Messages.setLanguageText(bAllowDHT,"ConfigView.section.sharing.permitdht");
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 3;
    bAllowDHT.setLayoutData(gridData);
    bAllowDHT.setSelection( true );
    
    bAllowDHT.addListener(SWT.Selection,new Listener() {
        public void handleEvent(Event event) {
          _wizard.permitDHT = bAllowDHT.getSelection();
        }
      });
      
	
	bPrivateTorrent.addListener(SWT.Selection,new Listener() {
        public void handleEvent(Event event) {
          _wizard.privateTorrent = bPrivateTorrent.getSelection();
		  
          if ( _wizard.privateTorrent ){
        	  
        	  bAllowDHT.setSelection( false );
        	  _wizard.permitDHT = false;
          }
		  bAllowDHT.setEnabled( !_wizard.privateTorrent );
        }
      });

    if ( _wizard.getTrackerType() == NewTorrentWizard.TT_DECENTRAL ){

		bAllowDHT.setEnabled( false );
		bPrivateTorrent.setEnabled( false );
    }
  }
  
  public IWizardPanel getFinishPanel() {
    return new ProgressPanel((NewTorrentWizard)wizard,this);
  }

}
