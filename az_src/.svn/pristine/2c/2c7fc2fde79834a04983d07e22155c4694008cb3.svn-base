/*
 * File    : OpenUrlWindow.java
 * Created : 3 nov. 2003 15:30:46
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
 
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.aelitis.azureus.core.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderCallBackInterface;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;

/**
 * @author Olivier
 * 
 */
public class 
OpenUrlWindow 
{
  protected static String	CONFIG_REFERRER_DEFAULT = "openUrl.referrer.default";
	
  protected static String	last_referrer = null;
  
  static{
  	last_referrer = COConfigurationManager.getStringParameter( CONFIG_REFERRER_DEFAULT, "" );
  }
  
  /**
   * Init
	 * 
	 * @param azureus_core
	 * @param parent
	 * @param linkURL
	 * @param referrer
	 */
	public OpenUrlWindow(final Shell parent,
			String linkURL, final String referrer) {
		this(parent, linkURL, referrer, null);
	}

	/**
	 * Init with listener
	 * 
	 * @param azureus_core
	 * @param parent
	 * @param linkURL
	 * @param referrer
	 * @param listener
	 */
	public OpenUrlWindow(final Shell parent,
			String linkURL, final String referrer,
			final TorrentDownloaderCallBackInterface listener) {

    final Shell shell = ShellFactory.createShell(parent, SWT.DIALOG_TRIM
				| SWT.APPLICATION_MODAL | SWT.RESIZE);
    shell.setText(MessageText.getString("openUrl.title"));
    Utils.setShellIcon(shell);
    
    GridData gridData;
    GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    shell.setLayout(layout);
    
    	// URL field
    
    Label label = new Label(shell, SWT.NULL);
    label.setText(MessageText.getString("openUrl.url"));
    gridData = new GridData();
    label.setLayoutData(gridData);
    
    final Text url = new Text(shell, SWT.BORDER);

    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.widthHint=400;
    gridData.horizontalSpan	= 2;
    url.setLayoutData(gridData);
    if(linkURL == null)
      Utils.setTextLinkFromClipboard(shell, url, true);
    else
    	url.setText(linkURL);
    url.setSelection(url.getText().length());
    
    
    
    // help field
    Label help_label = new Label(shell, SWT.NULL);
    help_label.setText(MessageText.getString("openUrl.url.info"));
    gridData = new GridData();
    gridData.horizontalSpan	= 3;
    help_label.setLayoutData(gridData);
       
    Label space = new Label(shell, SWT.NULL);
    gridData = new GridData();
    gridData.horizontalSpan	= 3;
    space.setLayoutData(gridData);
    
    	// referrer field
    
    Label referrer_label = new Label(shell, SWT.NULL);
    referrer_label.setText(MessageText.getString("openUrl.referrer"));
    gridData = new GridData();
    referrer_label.setLayoutData(gridData);
    
    final Combo referrer_combo = new Combo(shell, SWT.BORDER);

    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.widthHint=150;
	gridData.grabExcessHorizontalSpace = true;
	referrer_combo.setLayoutData(gridData);
    
    final StringList referrers = COConfigurationManager.getStringListParameter("url_open_referrers");
    StringIterator iter = referrers.iterator();
    while(iter.hasNext()) {
    	referrer_combo.add(iter.next());
    }
    
    if ( referrer != null && referrer.length() > 0 ){
    	
    	referrer_combo.setText( referrer );
    	
    }else if ( last_referrer != null ){
    	
    	referrer_combo.setText( last_referrer );
    }
    
    Label referrer_info = new Label(shell, SWT.NULL);
    referrer_info.setText(MessageText.getString("openUrl.referrer.info"));
    
	// line
	
	Label labelSeparator = new Label(shell,SWT.SEPARATOR | SWT.HORIZONTAL);
	gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_END);
	gridData.horizontalSpan = 3;
	labelSeparator.setLayoutData(gridData);

    	// buttons
    
    Composite panel = new Composite(shell, SWT.NULL);
    layout = new GridLayout();
    layout.numColumns = 3;
    panel.setLayout(layout);        
    gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_END);
    gridData.horizontalSpan = 3;
	gridData.grabExcessHorizontalSpace = true;
    panel.setLayoutData(gridData);
 	
    new Label(panel, SWT.NULL);
    
    Button ok = new Button(panel,SWT.PUSH);
    gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_END);
    gridData.widthHint = 70;    
	gridData.grabExcessHorizontalSpace = true;
    ok.setLayoutData(gridData);
    ok.setText(MessageText.getString("Button.ok"));
    ok.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {     
      	last_referrer	= referrer_combo.getText().trim();
      	
      	if(! referrers.contains(last_referrer)) {
      		referrers.add(last_referrer);
      		COConfigurationManager.setParameter("url_open_referrers",referrers);
      		COConfigurationManager.save();
      	}
      	
      	COConfigurationManager.setParameter( CONFIG_REFERRER_DEFAULT, last_referrer );
      	COConfigurationManager.save();
      	
      	String	url_str = url.getText();
      	
      	url_str = UrlUtils.parseTextForURL( url_str, true );
      	
      	if (url_str == null) {
      		url_str = UrlUtils.parseTextForMagnets(url.getText());
      	}
      	
      	if ( url_str == null ){
      		
      		url_str = url.getText();
      	}
      	
        new FileDownloadWindow(parent,url_str, last_referrer, null, listener );
        shell.dispose();
      }
    }); 
    
    shell.setDefaultButton (ok);
    
    Button cancel = new Button(panel,SWT.PUSH);
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
	gridData.grabExcessHorizontalSpace = false;
    gridData.widthHint = 70;
    cancel.setLayoutData(gridData);
    cancel.setText(MessageText.getString("Button.cancel"));
    cancel.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
        shell.dispose();
      }
    });        
    
	shell.addListener(SWT.Traverse, new Listener() {
 		
		public void handleEvent(Event e) {
			
			if ( e.character == SWT.ESC){
				shell.dispose();
			}
		}
	});
	
	
	Point p = shell.computeSize( SWT.DEFAULT, SWT.DEFAULT );
	
	if ( p.x > 800 ){
		
		p.x = 800;
	}
	
    shell.setSize( p );    
    
    Utils.createURLDropTarget(shell, url);
    
    Utils.centreWindow( shell );
    
    shell.open();
  }
}
