/*
 * File    : DonationWindow2.java
 * Created : 5 mars 2004
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
package org.gudy.azureus2.ui.swt.donations;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.stats.transfer.OverallStats;
import org.gudy.azureus2.core3.stats.transfer.StatsFactory;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.core3.util.*;

/**
 * @author Olivier
 * 
 */
public class OldDonationWindow {

  
  
  private Display display;
  private Shell shell;
  
  private Button radioDonate;
  private Button radioNoDonate;
  private Button radioLater;
  private Button radioAlready;
  
  private Button ok;
  
  private String mainText,footerText;
  int timeToWait;
  OverallStats stats;
  
  Image workingImage;
  Image background;
  
  Font mainFont;
  Font smallFont;
  Font mediumFont;
  Animator animator;    
  PaintListener listener;
  
  private static final String donationUrl = "http://donate.aelitis.com/donate/";
  private static final String donationUrlShort = "http://donate.aelitis.com/donate/";
  
  private static final int DONATIONS_ASK_AFTER = 168;
  private static final AEMonitor	class_mon	= new AEMonitor( "DonationWindow:class");
  
  public OldDonationWindow(Display display) {
      this.display = display;   
      stats = StatsFactory.getStats();
      
      mainText = MessageText.getString("DonationWindow.text");
      footerText = MessageText.getString("DonationWindow.text.footer");
      
      timeToWait = mainText.length() / 29;
  }  

  public void show() {
    shell = ShellFactory.createShell(SWT.BORDER | SWT.APPLICATION_MODAL | SWT.TITLE);
    FormLayout layout = new FormLayout();
    shell.setLayout(layout);
    
    Utils.setShellIcon(shell);
    shell.setText(MessageText.getString("DonationWindow.title"));
    shell.setBackground(Colors.white);
    
    background = ImageRepository.getImage("donation");    
    
    Font tempFont;
    FontData fontDataMain[];
    
    tempFont = shell.getFont();
    fontDataMain = tempFont.getFontData();
    
    boolean isMacLinux = Constants.isOSX || Constants.isUnix;    
    
    for(int i=0 ; i < fontDataMain.length ; i++) {
      if(!isMacLinux)
      	fontDataMain[i].setHeight((int) (fontDataMain[i].getHeight() * 1.4));
      else
        fontDataMain[i].setHeight((int) (fontDataMain[i].getHeight() * 1.1));
      fontDataMain[i].setStyle(SWT.BOLD);     
    }
    mainFont = new Font(display,fontDataMain);
    
    tempFont = shell.getFont();
    fontDataMain= tempFont.getFontData();
    for(int i=0 ; i < fontDataMain.length ; i++) {
      if(!isMacLinux)
      	fontDataMain[i].setHeight((int) (fontDataMain[i].getHeight() * 1.2));
      //fontDataMain[i].setStyle(SWT.BOLD);     
    }
    mediumFont = new Font(display,fontDataMain);
    
    tempFont = shell.getFont();
    fontDataMain = tempFont.getFontData();
    for(int i=0 ; i < fontDataMain.length ; i++) {
      if(isMacLinux)
      	fontDataMain[i].setHeight((int) (fontDataMain[i].getHeight() * 0.75));
      else
        fontDataMain[i].setHeight((int) (fontDataMain[i].getHeight() * 0.90));
    }
    smallFont = new Font(display,fontDataMain);
    
    listener = new PaintListener() {
      public void paintControl(PaintEvent event) {
        if(shell == null || shell.isDisposed())
          return;
        paint();
      }};
      
    shell.addPaintListener(listener);
    
    /*shell.addMouseListener(new MouseAdapter() {
  		public void mouseUp(MouseEvent arg0) {
  			close();
  		} 
    });*/
    
    ImageData data = background.getImageData();
    Rectangle shellSize = shell.computeTrim(0,0, data.width, data.height);
    shell.setSize(shellSize.width, shellSize.height);
    Utils.centreWindow(shell);
    
    addControls();
    
    shell.open();
    
    animator = new Animator();
    animator.start();
  }
  
  private class Animator extends AEThread {
   
    boolean ended = false;
    boolean drawingDone;
    int nbchars = 0;
    
    public Animator() {
     super("Donation animator"); 
    }
    
    public void runSupport() {
    	while(!ended) {
        if(display == null || display.isDisposed())
          return;
        drawingDone = false;
        Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						if (display == null || display.isDisposed())
							return;
						Image tempImage = new Image(display, background, SWT.IMAGE_COPY);

						nbchars++;
						if (nbchars <= mainText.length()) {
							String textToSet = mainText.substring(0, nbchars);
							GC tempGC = new GC(tempImage);
							if (mainFont == null || mainFont.isDisposed())
								return;
							tempGC.setFont(mainFont);
							if (stats != null) {
								tempGC.drawText(DisplayFormatters.formatByteCountToKiBEtc(stats
										.getDownloadedBytes()), 80, 14, true);
								tempGC.drawText(DisplayFormatters.formatByteCountToKiBEtc(stats
										.getUploadedBytes()), 235, 14, true);
								tempGC.drawText(stats.getTotalUpTime() / (60 * 60) + " "
										+ MessageText.getString("DonationWindow.text.hours"), 465,
										14, true);
							}
							tempGC.drawText(textToSet, 10, 60, true);
							tempGC.setFont(null);
							tempGC.drawText(MessageText
									.getString("DonationWindow.text.downloaded"), 70, 32, true);
							tempGC.drawText(MessageText
									.getString("DonationWindow.text.uploaded"), 235, 32, true);
							tempGC.dispose();
							Image oldImage = workingImage;
							workingImage = tempImage;

							if (oldImage != null && !oldImage.isDisposed())
								oldImage.dispose();
							paint();
						} else {
							ended = true;
						}
						drawingDone = true;
					}
				});
    		try {
          Thread.sleep(30);
          while(!drawingDone)
            Thread.sleep(20);     
        } catch (InterruptedException e) {
         ended = true; 
        }            
      } 
      enableOk();
   }
    
    public void dispose() {
     ended = true; 
    }
  }
  
  private void close() {
   animator.dispose();
   mainFont.dispose();
   mediumFont.dispose();
   smallFont.dispose();
   workingImage.dispose();
   shell.dispose();
   ImageRepository.unloadImage("donation");
  }
  
  private void paint() {
    if(shell == null || shell.isDisposed()) return;
    if(workingImage == null || workingImage.isDisposed()) return;
    GC gcShell = new GC(shell);
    gcShell.drawImage(workingImage,0,0);
    gcShell.dispose();
  }
  
  private void enableOk() {
    if(display == null || display.isDisposed()) return;
     display.asyncExec(new AERunnable() {
       public void runSupport() {
         if(shell == null || shell.isDisposed())
           return;
         ok.setEnabled(true);
       }
    });
  }
  
  private void addControls() {
   /*if(display == null || display.isDisposed()) return;
   display.asyncExec(new AERunnable() {
		public void runSupport() {
      if(shell == null || shell.isDisposed())
        return;     */       
                  
      FormData formData;
      
      
      //We're adding the OK button First so that it's on TOP
      //Of other controls (Not sure about this, but should be right)
      //Gudy :p
      ok = new Button(shell,SWT.PUSH);     
      ok.setEnabled(false);
      Messages.setLanguageText(ok,"DonationWindow.ok");
      
      formData = new FormData();
      formData.bottom = new FormAttachment(100,-5);
      formData.right = new FormAttachment(100,-5);
      formData.width = 100;
      ok.setLayoutData(formData);
      
      radioDonate = new Button(shell,SWT.RADIO);
      Messages.setLanguageText(radioDonate,"DonationWindow.options.donate");
      radioDonate.setFont(mainFont);
      radioDonate.setBackground(Colors.white);
      formData = new FormData();
      formData.top = new FormAttachment(65, 0); // added ",0" for Pre 3.0 SWT
      formData.left = new FormAttachment(0,140);
      formData.right = new FormAttachment(100,-5);
      radioDonate.setLayoutData(formData);        
      
      
      final Label textFooter = new Label(shell,SWT.NULL);    
      textFooter.setFont(smallFont);
      textFooter.setText(footerText);
      textFooter.setForeground(Colors.black);
      textFooter.setBackground(Colors.white);
      formData = new FormData();
      formData.top = new FormAttachment(radioDonate);
      formData.left = new FormAttachment(0,140);
      formData.right = new FormAttachment(100,-5);
      textFooter.setLayoutData(formData);
      
      
      radioNoDonate = new Button(shell,SWT.RADIO);
      Messages.setLanguageText(radioNoDonate,"DonationWindow.options.nodonate");
      radioNoDonate.setFont(mediumFont);
      radioNoDonate.setBackground(Colors.white);
      formData = new FormData();
      formData.top = new FormAttachment(textFooter);
      formData.left = new FormAttachment(0,140);
      formData.right = new FormAttachment(100,-5);
      radioNoDonate.setLayoutData(formData);
      
      radioLater = new Button(shell,SWT.RADIO);
      Messages.setLanguageText(radioLater,"DonationWindow.options.later");
      radioLater.setFont(mediumFont);
      radioLater.setBackground(Colors.white);
      formData = new FormData();
      formData.top = new FormAttachment(radioNoDonate);
      formData.left = new FormAttachment(0,140);
      formData.right = new FormAttachment(100,-5);
      radioLater.setLayoutData(formData);
      
      radioAlready = new Button(shell,SWT.RADIO);
      Messages.setLanguageText(radioAlready,"DonationWindow.options.already");      
      radioAlready.setFont(mediumFont);
      radioAlready.setBackground(Colors.white);
      formData = new FormData();
      formData.top = new FormAttachment(radioLater);
      formData.left = new FormAttachment(0,140);
      formData.right = new FormAttachment(100,-5);
      radioAlready.setLayoutData(formData);
      
      
      final Text textForCopy = new Text(shell,SWT.BORDER);
      textForCopy.setText(donationUrlShort);      
      textForCopy.setFont(smallFont);
      formData = new FormData();
      formData.bottom = new FormAttachment(100,-7);
      formData.left = new FormAttachment(0,5);
      formData.right = new FormAttachment(ok,-5);
      textForCopy.setLayoutData(formData);
      
      
      //By default, donate is selected (of course)
      radioDonate.setSelection(true);    
      
      
      //allow OK from the start
      ok.setEnabled(true);
      
      ok.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event evt) {          
          handleChoice();
        }
      });
      
      Listener keyListener =  new Listener() {
        public void handleEvent(Event e) {
          System.out.println(e.character);
          if(e.character == SWT.ESC) {
            if(ok.getEnabled()) close();                
          }
        }
      };
      
      shell.addListener(SWT.KeyUp,keyListener);
      textForCopy.addListener(SWT.KeyUp,keyListener);
      
      shell.layout();
		}
  
  private void handleChoice() {
    if(radioDonate.getSelection()) {
    	Utils.launch(donationUrl);
    }
    if(radioAlready.getSelection()) {
      thanks();
      stopAsking(); 
    }
    if(radioNoDonate.getSelection()){
      stopAsking(); 
    }
    if(!radioDonate.getSelection()) {
      close();
    }       
  }
  
  
  private void thanks() {
    MessageBox msgThanks = new MessageBox(shell,SWT.OK);
    msgThanks.setText(MessageText.getString("DonationWindow.thanks.title"));
    msgThanks.setMessage(MessageText.getString("DonationWindow.thanks.text"));
    msgThanks.open();
    COConfigurationManager.setParameter("donations.donated",true);    
    COConfigurationManager.save();
  }
  
  private void stopAsking() {
    COConfigurationManager.setParameter("donations.nextAskTime",-1);
    COConfigurationManager.setParameter("donations.lastVersion",Constants.AZUREUS_VERSION);
    COConfigurationManager.save();
  }
  
  public static void checkForDonationPopup() {
  	try{
  		class_mon.enter();
  
	    //Check if user has already donated first
	    boolean alreadyDonated = COConfigurationManager.getBooleanParameter("donations.donated",false);
	    if(alreadyDonated)
	      return;
	    
	    //Check for last asked version
	    String lastVersionAsked = COConfigurationManager.getStringParameter("donations.lastVersion","");
	         
	    long upTime = StatsFactory.getStats().getTotalUpTime();
	    int hours = (int) (upTime / (60*60)); //secs * mins
	    
	    //Ask every DONATIONS_ASK_AFTER hours.
	    int nextAsk = (COConfigurationManager.getIntParameter("donations.nextAskTime",0) + 1) * DONATIONS_ASK_AFTER;
	    
	    //if donations.nextAskTime == -1 , then no more ask for same version
	    if(nextAsk == 0) {
	     if(lastVersionAsked.equals(Constants.AZUREUS_VERSION)) {
	      return; 
	     }
	     else {
	      //Set the re-ask so that we ask in the next %DONATIONS_ASK_AFTER hours
	       COConfigurationManager.setParameter("donations.nextAskTime",hours / DONATIONS_ASK_AFTER);
	       COConfigurationManager.save();
	
	       return;
	     }
	    }
	    
	    //If we're still under the ask time, return
	    if(hours < nextAsk)
	     return;
	    
	    //Here we've got to ask !!!
	    COConfigurationManager.setParameter("donations.nextAskTime",hours / DONATIONS_ASK_AFTER);
	    COConfigurationManager.save();
	
	    final Display display = SWTThread.getInstance().getDisplay();
	    
	    if(display != null && !display.isDisposed()) {
	    	Utils.execSWTThread(new AERunnable() {
	      public void runSupport() {
	      	if(display != null && !display.isDisposed())
	         new OldDonationWindow(display).show();    
	      }
	     });
	    }          
	  
  	}finally{
  		class_mon.exit();
  	}
  }
}
