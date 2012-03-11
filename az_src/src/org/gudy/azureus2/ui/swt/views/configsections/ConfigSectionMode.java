
/*
 * File    : ConfigPanel*.java
 * Created : 11 mar. 2004
 * By      : TuxPaper
 * 
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
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
 *
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package org.gudy.azureus2.ui.swt.views.configsections;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;

import com.aelitis.azureus.ui.UserPrompterResultListener;



public class ConfigSectionMode implements UISWTConfigSection {
	  public String configSectionGetParentSection() {
		    return ConfigSection.SECTION_ROOT;
		  }

	public String configSectionGetName() {
		return "mode";
	}

  public void configSectionSave() {
  }

  public void configSectionDelete() {
  }
  
	public int maxUserMode() {
		return 0;
	}

  

  public Composite configSectionCreate(final Composite parent) {
    GridData gridData;
    GridLayout layout;
    String initsMode = "";
    final String[] text = {""};
    final String[] messTexts = {"ConfigView.section.mode.beginner.wiki.definitions",
    		"ConfigView.section.mode.intermediate.wiki.host",
    		"ConfigView.section.mode.advanced.wiki.main",
    		"ConfigView.section.mode.intermediate.wiki.publish"
    };
    final String[] links = {"http://wiki.vuze.com/w/This_funny_word",
    		"http://wiki.vuze.com/w/HostingFiles",
    		"http://wiki.vuze.com/w/Main_Page",
    		"http://wiki.vuze.com/w/PublishingFiles"
    };
    
    int userMode = COConfigurationManager.getIntParameter("User Mode");

    final Composite cMode = new Composite(parent, SWT.WRAP);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    cMode.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 4;
    layout.marginHeight = 0;
    cMode.setLayout(layout);
    
    gridData = new GridData();
    gridData.horizontalSpan = 4;
    final Group gRadio = new Group(cMode, SWT.WRAP);
    Messages.setLanguageText(gRadio, "ConfigView.section.mode.title");
    gRadio.setLayoutData(gridData);
    gRadio.setLayout(new RowLayout(SWT.HORIZONTAL));

    Button button0 = new Button (gRadio, SWT.RADIO);
    Messages.setLanguageText(button0, "ConfigView.section.mode.beginner");
    button0.setData("iMode", "0");
    button0.setData("sMode", "beginner.text");
    
    Button button1 = new Button (gRadio, SWT.RADIO);
    Messages.setLanguageText(button1, "ConfigView.section.mode.intermediate");
    button1.setData("iMode", "1");
    button1.setData("sMode", "intermediate.text");
    
    Button button2 = new Button (gRadio, SWT.RADIO);
    Messages.setLanguageText(button2, "ConfigView.section.mode.advanced");
    button2.setData("iMode", "2");
    button2.setData("sMode", "advanced.text");
    
    if ( userMode == 0) {
    	initsMode = "beginner.text";
    	button0.setSelection(true);
    } else if ( userMode == 1) {
    	initsMode = "intermediate.text";
    	button1.setSelection(true);
    } else {
    	initsMode = "advanced.text";
    	button2.setSelection(true);
    }

    
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    final Label label = new Label(cMode, SWT.WRAP);
    gridData.horizontalSpan = 4;
    label.setLayoutData(gridData);
	text[0] = MessageText.getString("ConfigView.section.mode." + initsMode);
	label.setText(text[0]);
	label.addListener (SWT.Selection, new Listener () {
		public void handleEvent(Event event) {
			Utils.launch(event.text);
		}
	});
	
	Group gWiki = new Group(cMode, SWT.WRAP);
    gridData = new GridData();
    gridData.widthHint = 350;
    gWiki.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 1;
    layout.marginHeight = 1;
    gWiki.setLayout(layout);
    
    gWiki.setText(MessageText.getString("Utils.link.visit"));

	    final Label linkLabel = new Label(gWiki, SWT.NULL);
	    linkLabel.setText( MessageText.getString( messTexts[userMode] ) );
	    linkLabel.setData( links[userMode] );
	    linkLabel.setCursor(linkLabel.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
	    linkLabel.setForeground(Colors.blue);
	    gridData = new GridData(GridData.FILL_HORIZONTAL);
	    gridData.horizontalIndent = 10;
	    linkLabel.setLayoutData( gridData );
	    linkLabel.addMouseListener(new MouseAdapter() {
	      public void mouseDoubleClick(MouseEvent arg0) {
	      	Utils.launch((String) ((Label) arg0.widget).getData());
	      }
	      public void mouseUp(MouseEvent arg0) {
	      	Utils.launch((String) ((Label) arg0.widget).getData());
	      }
	    });
	    
	    final Label linkLabel1 = new Label(gWiki, SWT.NULL);
	    linkLabel1.setText( (userMode == 1)?MessageText.getString(messTexts[3]):"");
	    linkLabel1.setData( links[3] );
	    linkLabel1.setCursor(linkLabel1.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
	    linkLabel1.setForeground(Colors.blue);
	    gridData = new GridData(GridData.FILL_HORIZONTAL);
	    gridData.horizontalIndent = 10;
	    linkLabel1.setLayoutData( gridData );
	    linkLabel1.addMouseListener(new MouseAdapter() {
	      public void mouseDoubleClick(MouseEvent arg0) {
	      	Utils.launch((String) ((Label) arg0.widget).getData());
	      }
	      public void mouseUp(MouseEvent arg0) {
	      	Utils.launch((String) ((Label) arg0.widget).getData());
	      }
	    });

    
    Listener radioGroup = new Listener () {
    	public void handleEvent (Event event) {
    		
    		Control [] children = gRadio.getChildren ();
    		
    		for (int j=0; j<children.length; j++) {
    			 Control child = children [j];
    			 if (child instanceof Button) {
    				 Button button = (Button) child;
    				 if ((button.getStyle () & SWT.RADIO) != 0) button.setSelection (false);
    			 }
    		}

		    Button button = (Button) event.widget;
		    button.setSelection (true);
		    int mode = Integer.parseInt((String)button.getData("iMode"));
		    text[0] = MessageText.getString("ConfigView.section.mode." + (String)button.getData("sMode"));
		    label.setText(text[0]);
		    linkLabel.setText( MessageText.getString(messTexts[mode]) );
		    linkLabel.setData( links[mode] );
		    if(mode == 1){
			    linkLabel1.setText( MessageText.getString(messTexts[3]) );
			    linkLabel1.setData( links[3] );
		    } else{
			    linkLabel1.setText( "" );
			    linkLabel1.setData( "" );
		    }
		    COConfigurationManager.setParameter("User Mode", Integer.parseInt((String)button.getData("iMode")));
		    }
    };
    
    button0.addListener (SWT.Selection, radioGroup);
    button1.addListener (SWT.Selection, radioGroup);
    button2.addListener (SWT.Selection, radioGroup);

    Label padding = new Label(cMode, SWT.NULL );
    gridData = new GridData();
    gridData.horizontalSpan = 3;
    padding.setLayoutData( gridData );
    
    	// reset to defaults
    
	Composite gReset = new Composite(cMode, SWT.WRAP);
    gridData = new GridData();
    gridData.horizontalSpan = 4;
    gReset.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 3;
    gReset.setLayout(layout);

    Label reset_label = new Label(gReset, SWT.NULL );
    Messages.setLanguageText(reset_label, "ConfigView.section.mode.resetdefaults" );
    
    Button reset_button = new Button(gReset, SWT.PUSH);

    Messages.setLanguageText(reset_button, "Button.reset" );

    reset_button.addListener(SWT.Selection, 
		new Listener() 
		{
	        public void 
			handleEvent(Event event) 
	        {
	        	MessageBoxShell mb = new MessageBoxShell(
	        			SWT.ICON_WARNING | SWT.OK | SWT.CANCEL,
	        			MessageText.getString("resetconfig.warn.title"),
	        			MessageText.getString("resetconfig.warn"));
	        	
	        	mb.setDefaultButtonUsingStyle(SWT.CANCEL);
	        	
	        	mb.setParent(parent.getShell());

	        	mb.open(
	        		new UserPrompterResultListener() 
	        		{
	        			public void 
	        			prompterClosed(
	        				int returnVal ) 
	        			{
							if (returnVal != SWT.OK) {
								return;
							}

							COConfigurationManager.resetToDefaults();
	        			}
	        		});
	        }
	    });
    
    padding = new Label(gReset, SWT.NULL );
    gridData = new GridData();
    padding.setLayoutData( gridData );
    
    return cMode;
  }
}
