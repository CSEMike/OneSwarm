/*
 * File    : ErrorPopupShell.java
 * Created : 15 mars 2004
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
package org.gudy.azureus2.ui.swt.shells;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.animations.Animator;
import org.gudy.azureus2.ui.swt.animations.shell.AnimableShell;
import org.gudy.azureus2.ui.swt.animations.shell.LinearAnimator;

import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;


/**
 * @author Olivier Chalouhi
 *
 */
public class MessagePopupShell implements AnimableShell {
  
  private Shell shell;
  private Shell detailsShell;  
  Image shellImg;
  private Display display;

  public static final String ICON_ERROR 	= "error";
  public static final String ICON_WARNING 	= "warning";
  public static final String ICON_INFO	 	= "info";

   private static LinkedList viewStack;
   private Timer closeTimer;

    private String icon;

  static {
      viewStack = new LinkedList();
  }
  
	/** Open a popup using resource keys for title/text
	 * 
	 * @param keyPrefix message bundle key prefix used to get title and text.  
	 *         Title will be keyPrefix + ".title", and text will be set to
	 *         keyPrefix + ".text"
	 * @param details actual text for details (not a key)
	 * @param textParams any parameters for text
	 * 
	 * @note Display moved to end to remove conflict in constructors
	 */
  public MessagePopupShell(String icon, String keyPrefix,
			String details, String[] textParams, Display display) {
  	this(display, icon, MessageText.getString(keyPrefix + ".title"),
				MessageText.getString(keyPrefix + ".text", textParams), details);
	}

  public MessagePopupShell(Display display,String icon,String title,String errorMessage,String details) {
    closeTimer = new Timer(true);

    this.display = display;
    this.icon = icon;
    detailsShell = new Shell(display,SWT.BORDER | SWT.ON_TOP);
    Utils.setShellIcon(detailsShell);
    
    detailsShell.setLayout(new FillLayout());
    StyledText textDetails = new StyledText(detailsShell, SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);  
    textDetails.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
    textDetails.setWordWrap( true );
    detailsShell.layout();    
    detailsShell.setSize(550,300);    
    

    int popupWidth = 280;
    int popupHeight = 170;

    shell = new Shell(display,SWT.ON_TOP);
    Utils.setShellIcon(shell);

    FormLayout layout = new FormLayout();
    layout.marginHeight = 0; layout.marginWidth = 0; 
    try {
      layout.spacing = 0;
    } catch (NoSuchFieldError e) {
      /* Ignore for Pre 3.0 SWT.. */
    }
    shell.setLayout(layout);
    
    Image popup_image = ImageRepository.getImage("popup");

		// this code is here to ensure that we can still show error messages even if images
		// are failing to load (e.g. coz there's a ! in AZ install dir... )

		GC gcImage = null;
		if (popup_image != null) {
			shellImg = new Image(display, popup_image, SWT.IMAGE_COPY);
	    popupWidth = popup_image.getBounds().width; 
	    popupHeight = popup_image.getBounds().height;
		} else {
			shellImg = new Image(display,
					new Rectangle(0, 0, popupWidth, popupHeight));
		}

    shell.setSize(popupWidth, popupHeight);

		gcImage = new GC(shellImg);

		Image imgIcon = ImageRepository.getImage(icon);
		int iconWidth = 0;
		int iconHeight = 15;
		if (imgIcon != null) {
			imgIcon.setBackground(shell.getBackground());
			gcImage.drawImage(imgIcon, 5, 5);
	    iconWidth = imgIcon.getBounds().width;
			iconHeight = imgIcon.getBounds().height;
		}
	    

		Font tempFont = shell.getFont();
		FontData[] fontDataMain = tempFont.getFontData();
		for (int i = 0; i < fontDataMain.length; i++) {
			fontDataMain[i].setStyle(SWT.BOLD);
			fontDataMain[i].setHeight((int) (fontDataMain[i].getHeight() * 1.2));
		}

		Font fontTitle = new Font(display, fontDataMain);
		gcImage.setFont(fontTitle);

		Rectangle rect = new Rectangle(iconWidth + 10, 5, popupWidth - iconWidth
				- 15, iconHeight);
		GCStringPrinter.printString(gcImage, title, rect);

		gcImage.setFont(tempFont);
		fontTitle.dispose();

		rect = new Rectangle(5, iconHeight + 5, popupWidth - 10, popupHeight
				- iconHeight - 60);
		boolean bItFit = GCStringPrinter.printString(gcImage, errorMessage, rect);

		gcImage.dispose(); 
		if (!bItFit && details == null)
			details = errorMessage;
    
    if(details != null)
      textDetails.setText(details);

    final Button btnDetails = new Button(shell,SWT.TOGGLE);
    Messages.setLanguageText(btnDetails,"popup.error.details");    
    btnDetails.setEnabled(details != null);
    
    final Button btnHide = new Button(shell,SWT.PUSH);
    Messages.setLanguageText(btnHide,"popup.error.hide");
    
    Label lblImage = new Label(shell,SWT.NULL);
	
	if ( shellImg != null ){
		lblImage.setImage(shellImg);
	}
    
    FormData formData;
    
    formData = new FormData();    
    formData.right = new FormAttachment(btnHide,-5);
    formData.bottom = new FormAttachment(100,-5);
    btnDetails.setLayoutData(formData);
    
    formData = new FormData();
    formData.right = new FormAttachment(100,-5);
    formData.bottom = new FormAttachment(100,-5);
    btnHide.setLayoutData(formData);
    
    formData = new FormData();
    formData.left = new FormAttachment(0,0);
    formData.top = new FormAttachment(0,0);
    lblImage.setLayoutData(formData);
    
    Button btnHideAll = null;
    if (viewStack.size() > 0) {
    	btnHideAll = new Button(shell, SWT.PUSH);
    	btnHideAll.moveAbove(btnDetails);
    	Messages.setLanguageText(btnHideAll, "popup.error.hideall");
    	
    	formData = new FormData();
    	formData.right = new FormAttachment(btnDetails, -5);
    	formData.bottom  = new FormAttachment(100,-5);
    	btnHideAll.setLayoutData(formData);
    	
    	btnHideAll.addListener(SWT.MouseUp, new Listener() {
    		public void handleEvent(Event event) {
          btnHide.setEnabled(false);
          btnDetails.setEnabled(false);
          
          for (Iterator iter = viewStack.iterator(); iter.hasNext();) {
						WeakReference wr = (WeakReference) iter.next();
						MessagePopupShell popup = (MessagePopupShell) wr.get();
						iter.remove();

						if (popup == null)
							return;

						popup.shell.dispose();
						popup.detailsShell.dispose();
						if (popup.shellImg != null) {
							popup.shellImg.dispose();
						}
					}
    		}
    	});
    }
    
    shell.layout();
    shell.setTabList(new Control[] {btnDetails, btnHide});

    btnHide.addListener(SWT.MouseUp, new Listener() {
      public void handleEvent(Event arg0) {
          btnHide.setEnabled(false);
          btnDetails.setEnabled(false);
          hideShell();
      }
    });
    
    btnDetails.addListener(SWT.MouseUp, new Listener() {
      public void handleEvent(Event arg0) {
       detailsShell.setVisible(btnDetails.getSelection());
      }
    });
    
    Rectangle bounds = null;
    try {
    	UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
    	if (uiFunctions != null) {
				Shell mainShell = uiFunctions.getMainShell();
				bounds = mainShell.getMonitor().getClientArea();
    	}
    } catch (Exception e) {
    }
    if (bounds == null) {
    	bounds = display.getClientArea();
    }

    x0 = bounds.x + bounds.width - popupWidth - 5;
    x1 = bounds.x + bounds.width;

    y0 = bounds.y + bounds.height;
    y1 = bounds.y + bounds.height - popupHeight - 5;
    
    	// currently always animate
    
    if ( true ){
	    shell.setLocation(x0,y0);
	    viewStack.addFirst(new WeakReference(this));
	    detailsShell.setLocation(x1-detailsShell.getSize().x,y1-detailsShell.getSize().y);
	    currentAnimator = new LinearAnimator(this,new Point(x0,y0),new Point(x0,y1),20,30);
	    currentAnimator.start();
	    shell.open();
    }else{
        shell.setLocation(x0,y1);
	    viewStack.addFirst(new WeakReference(this));
	    detailsShell.setLocation(x1-detailsShell.getSize().x,y1-detailsShell.getSize().y);
	    currentAnimator = new LinearAnimator(this,new Point(x0,y1),new Point(x0,y1),20,30);
	    animationStarted(currentAnimator);
	    shell.open();
	    animationEnded(currentAnimator);
    }
    }

    private void hideShell()
    {
    	try {
        if(currentAnimator == null) {
          closeTimer.cancel();
          detailsShell.setVisible(false);
          detailsShell.forceActive();
          if(!Constants.isOSX){detailsShell.forceFocus();}
          currentAnimator = new LinearAnimator(this,new Point(x0,y1),new Point(x1,y1),20,30);
          currentAnimator.start();
          closeAfterAnimation = true;
        }
    	} catch (Exception e) {
    		closeAfterAnimation = true;
    		animationEnded(null);
    	}
    }


  private Animator currentAnimator;
  private boolean closeAfterAnimation;
  int x0,y0,x1,y1;
  
  public void animationEnded(Animator source) {
    if(source == currentAnimator) {
      currentAnimator = null;
    }
    if(closeAfterAnimation) {   
      if(display == null || display.isDisposed())
        return;
      display.asyncExec(new AERunnable(){
        public void runSupport() {
          viewStack.removeFirst();
          shell.dispose();
          detailsShell.dispose();
		  if ( shellImg != null ){
			  shellImg.dispose();
		  }
        }
      });     
    }
    else {
        scheduleAutocloseTask();
    }
  }

   private void scheduleAutocloseTask() {
       final int delay = COConfigurationManager.getIntParameter("Message Popup Autoclose in Seconds") * 1000;
        if(delay < 1000)
            return;

       closeTimer.scheduleAtFixedRate(new TimerTask() {
           public void run() {
               display.syncExec(new AERunnable() {
                    public void runSupport() {
                        if(shell.isDisposed()) {
                            closeTimer.cancel();
                            return;
                        }

                        final boolean notInfoType = MessagePopupShell.this.icon != ICON_INFO;
                        if(notInfoType) {
                            closeTimer.cancel();
                            return;
                        }

                        final boolean notTopWindow = ((WeakReference)viewStack.getFirst()).get() != MessagePopupShell.this;
                        final boolean animationInProgress = currentAnimator != null;
                        final boolean detailsOpen = (!detailsShell.isDisposed() && detailsShell.isVisible());

                        final Control cc = display.getCursorControl();
                        boolean mouseOver = (cc == shell);
                        if(!mouseOver) {
                            final Control[] childControls = shell.getChildren();
                            for(int i = 0; i < childControls.length; i++) {
                                if(childControls[i] == cc) {
                                    mouseOver = true;
                                    break;
                                }
                            }
                        }

                        if(notTopWindow || mouseOver || animationInProgress || detailsOpen)
                            return;

                        hideShell();
                    }
                });
           }
       }, delay, delay);
   }

  public void animationStarted(Animator source) {
  }

  
  public Shell getShell() {
    return shell;
  }
  
  public void reportPercent(int percent) {    
  }
}
