/*
 * Created on 9 sept. 2003
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
package org.gudy.azureus2.applet;

import java.applet.Applet;
import java.awt.GridLayout;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.gudy.azureus2.core3.download.DownloadManager;

/**
 * @author Olivier
 * 
 */
public class Main extends Applet {
  
  JPanel appletPanel;
  JLabel lFileName;
  JLabel lDownSpeed;
  JLabel lPercentDone;
  
  String url;
  String savePath;
  
  DownloadManager dm;    
  
  public Main() {
    appletPanel = new JPanel();
    GridLayout layout = new GridLayout(3,2);
    appletPanel.setLayout(layout);
    
    appletPanel.add(new JLabel("File :"));
    lFileName = new JLabel();
    appletPanel.add(lFileName);
    
    appletPanel.add(new JLabel("Down Speed :"));
    lDownSpeed = new JLabel();
    appletPanel.add(lDownSpeed);
        
    appletPanel.add(new JLabel("% done :"));
    lPercentDone = new JLabel();
    appletPanel.add(lPercentDone);  
    
    
    //Initialize parameters
    url = this.getParameter("torrentUrl");
    JFileChooser fileChooser = new JFileChooser();
    int result = fileChooser.showSaveDialog(this);
    if(result == JFileChooser.APPROVE_OPTION)
      savePath =  fileChooser.getSelectedFile().getName();
      
    System.out.println(url + "\n" + savePath);   
      
    
    this.add(appletPanel);
    this.setSize(200,100);
  }
  
  

}
