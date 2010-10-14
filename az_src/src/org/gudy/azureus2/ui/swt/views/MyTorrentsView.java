/*
 * Created on 30 juin 2003
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

package org.gudy.azureus2.ui.swt.views;

import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.category.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.tracker.util.TRTrackerUtils;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.URLTransfer;
import org.gudy.azureus2.ui.swt.help.HealthHelpWindow;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.minibar.DownloadBar;
import org.gudy.azureus2.ui.swt.shells.InputShell;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.ViewUtils.SpeedAdapter;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTPanelCreator;
import org.gudy.azureus2.ui.swt.views.table.impl.TableCellImpl;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewSWTImpl;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.common.table.*;

import org.gudy.azureus2.plugins.ui.tables.TableManager;

/** Displays a list of torrents in a table view.
 *
 * @author Olivier
 * @author TuxPaper
 *         2004/Apr/18: Use TableRowImpl instead of PeerRow
 *         2004/Apr/20: Remove need for tableItemToObject
 *         2004/Apr/21: extends TableView instead of IAbstractView
 *         2005/Oct/01: Column moving in SWT >= 3.1
 */
public class MyTorrentsView
       extends TableViewTab
       implements GlobalManagerListener,
                  ParameterListener,
                  DownloadManagerListener,
                  CategoryManagerListener,
                  CategoryListener,
                  KeyListener,
                  TableLifeCycleListener, 
                  TableViewSWTPanelCreator,
                  TableSelectionListener,
                  TableViewSWTMenuFillListener,
                  TableRefreshListener,
                  TableCountChangeListener
{
	private static final LogIDs LOGID = LogIDs.GUI;
	private static final int ASYOUTYPE_MODE_FIND = 0;
	private static final int ASYOUTYPE_MODE_FILTER = 1;
	private static final int ASYOUTYPE_MODE = ASYOUTYPE_MODE_FILTER;
	private static final int ASYOUTYPE_UPDATEDELAY = 300;
	
	/** Experimental Table UI.  When setting to true, some code needs 
	 *  uncommenting as well */
	private static final boolean EXPERIMENT = false;
	
	private AzureusCore		azureus_core;

  private GlobalManager globalManager;
  private boolean isSeedingView;

  private Composite cTablePanel;
  private Font fontButton = null;
  private Composite cCategories;
  private ControlAdapter catResizeAdapter;
  private DragSource dragSource = null;
  private DropTarget dropTarget = null;
  private Composite cHeader = null;
  private Label lblHeader = null;
  private Text txtFilter = null;
  private Label lblX = null;
  
  int userMode;
  boolean isTrackerOn;

  private Category currentCategory;

  // table item index, where the drag has started
  private int drag_drop_line_start = -1;
  private TableRowCore[] drag_drop_rows = null;

	private TimerEvent searchUpdateEvent;
  private String sLastSearch = "";
  private long lLastSearchTime;
  private boolean bRegexSearch = false;
	private boolean bDNDalwaysIncomplete;
	private TableViewSWT tv;
	private Composite cTableParentPanel;

  /**
   * Initialize
   * 
   * @param _azureus_core
   * @param isSeedingView
   * @param basicItems
   */
  public 
  MyTorrentsView(
  		AzureusCore			_azureus_core, 
		boolean 			isSeedingView,
        TableColumnCore[] 	basicItems) 
  {
  	if (EXPERIMENT) {
//      tv = new ListView(isSeedingView ? TableManager.TABLE_MYTORRENTS_COMPLETE
//  				: TableManager.TABLE_MYTORRENTS_INCOMPLETE, SWT.V_SCROLL);
//      tv.setColumnList(basicItems, "#", false);
  	} else {
      tv = new TableViewSWTImpl(isSeedingView
  				? TableManager.TABLE_MYTORRENTS_COMPLETE
  				: TableManager.TABLE_MYTORRENTS_INCOMPLETE, "MyTorrentsView",
  				basicItems, "#", SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL);
  	}
    setTableView(tv);
    tv.setRowDefaultIconSize(new Point(16, 16));
    azureus_core		= _azureus_core;
    this.globalManager 	= azureus_core.getGlobalManager();
    this.isSeedingView 	= isSeedingView;

    currentCategory = CategoryManager.getCategory(Category.TYPE_ALL);
    tv.addLifeCycleListener(this);
    tv.setMainPanelCreator(this);
    tv.addSelectionListener(this, false);
    tv.addMenuFillListener(this);
    tv.addRefreshListener(this, false);
    tv.addCountChangeListener(this);

    // experiment
		//tv.setEnableTabViews(true);
		//IView views[] = { new GeneralView(), new PeersView(),
		//	new PeersGraphicView(), new PiecesView(), new FilesView(),
		//	new LoggerView() };
    //tv.setCoreTabViews(views);
	}

  // @see com.aelitis.azureus.ui.common.table.TableLifeCycleListener#tableViewInitialized()
  public void tableViewInitialized() {
    tv.addKeyListener(this);

    createTabs();

    createDragDrop();

    COConfigurationManager.addAndFireParameterListeners(new String[] {
				"DND Always In Incomplete",
				"Confirm Data Delete",
				"User Mode" }, this);

    if (currentCategory != null) {
    	currentCategory.addCategoryListener(this);
    }
    CategoryManager.addCategoryManagerListener(this);
    globalManager.addListener(this, false);
    Object[] dms = globalManager.getDownloadManagers().toArray();
    for (int i = 0; i < dms.length; i++) {
			DownloadManager dm = (DownloadManager) dms[i];
			dm.addListener(this);
			if (!isOurDownloadManager(dm)) {
				dms[i] = null;
			}
		}
    tv.addDataSources(dms);
    tv.processDataSourceQueue();
    
    cTablePanel.layout();
  }

  // @see com.aelitis.azureus.ui.common.table.TableLifeCycleListener#tableViewDestroyed()
  public void tableViewDestroyed() {
  	tv.removeKeyListener(this);
  	
  	Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				try {
					Utils.disposeSWTObjects(new Object[] {
						dragSource,
						dropTarget,
						fontButton
					});
					dragSource = null;
					dropTarget = null;
					fontButton = null;
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		});
    Object[] dms = globalManager.getDownloadManagers().toArray();
    for (int i = 0; i < dms.length; i++) {
			DownloadManager dm = (DownloadManager) dms[i];
			dm.removeListener(this);
		}
    if (currentCategory != null) {
    	currentCategory.removeCategoryListener(this);
    }
    CategoryManager.removeCategoryManagerListener(this);
    globalManager.removeListener(this);
    COConfigurationManager.removeParameterListener("DND Always In Incomplete", this);
    COConfigurationManager.removeParameterListener("Confirm Data Delete", this);
    COConfigurationManager.removeParameterListener("User Mode", this);
  }
  
  
  // @see org.gudy.azureus2.ui.swt.views.table.TableViewSWTPanelCreator#createTableViewPanel(org.eclipse.swt.widgets.Composite)
  public Composite createTableViewPanel(Composite composite) {
    GridData gridData;
    cTableParentPanel = new Composite(composite, SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.horizontalSpacing = 0;
    layout.verticalSpacing = 0;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    cTableParentPanel.setLayout(layout);
    if (composite.getLayout() instanceof GridLayout) {
    	cTableParentPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
    }
    
    if (EXPERIMENT) {
    	Composite cHeaders = new Composite(cTableParentPanel, SWT.NONE);
    	gridData = new GridData(GridData.FILL_HORIZONTAL);
    	gridData.horizontalSpan = 2;
    	GC gc = new GC(cHeaders);
    	int h = gc.textExtent("alyup").y + 2;
    	gc.dispose();
    	gridData.heightHint = h;
    	cHeaders.setLayoutData(gridData);
    	//((ListView)tv).setHeaderArea(cHeaders, null, null);
    }

    cTablePanel = new Composite(cTableParentPanel, SWT.NULL);
    cTablePanel.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
    cTablePanel.setForeground(composite.getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));

    gridData = new GridData(GridData.FILL_BOTH);
    gridData.horizontalSpan = 2;
    cTablePanel.setLayoutData(gridData);

    layout = new GridLayout(1, false);
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.verticalSpacing = 0;
    layout.horizontalSpacing = 0;
    cTablePanel.setLayout(layout);

    cTablePanel.layout();
    return cTablePanel;
  }

  private void createTabs() {
    GridData gridData;
    Category[] categories = CategoryManager.getCategories();
    Arrays.sort(categories);
    boolean showCat = sLastSearch.length() > 0;
    if (!showCat)
	    for(int i = 0; i < categories.length; i++) {
	        if(categories[i].getType() == Category.TYPE_USER) {
	            showCat = true;
	            break;
	        }
	    }

    if (!showCat) {
    	if (cCategories != null && !cCategories.isDisposed()) {
        Control[] controls = cCategories.getChildren();
        for (int i = 0; i < controls.length; i++) {
          controls[i].dispose();
        }
    	}
    } else {
      if (cCategories == null) {
        Composite parent = cTableParentPanel;

        cCategories = new Composite(parent, SWT.NONE);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
        cCategories.setLayoutData(gridData);
        RowLayout rowLayout = new RowLayout();
        rowLayout.marginTop = 0;
        rowLayout.marginBottom = 0;
        rowLayout.marginLeft = 3;
        rowLayout.marginRight = 0;
        rowLayout.spacing = 0;
        rowLayout.wrap = true;
        cCategories.setLayout(rowLayout);

        cHeader = new Composite(parent, SWT.NONE);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalIndent = 5;
        cHeader.setLayoutData(gridData);
        GridLayout layout = new GridLayout();
        layout.numColumns = 6;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.horizontalSpacing = 2;
        layout.verticalSpacing = 0;
        cHeader.setLayout(layout);
        
        lblHeader = new Label(cHeader, SWT.WRAP);
        gridData = new GridData();
        lblHeader.setLayoutData(gridData);
        updateTableLabel();

        Label lblSep = new Label(cHeader, SWT.SEPARATOR | SWT.VERTICAL);
        gridData = new GridData(GridData.FILL_VERTICAL);
        gridData.heightHint = 5;
        lblSep.setLayoutData(gridData);
        
        Label lblFilter = new Label(cHeader, SWT.WRAP);
        gridData = new GridData(GridData.BEGINNING);
        lblFilter.setLayoutData(gridData);
        Messages.setLanguageText(lblFilter, "MyTorrentsView.filter");

		lblX = new Label(cHeader, SWT.WRAP);
        Messages.setLanguageTooltip(lblX, "MyTorrentsView.clearFilter.tooltip");
        gridData = new GridData(SWT.TOP);
        lblX.setLayoutData(gridData);
        lblX.setImage(ImageRepository.getImage("smallx-gray"));
        lblX.addMouseListener(new MouseAdapter() {
        	public void mouseUp(MouseEvent e) {
        		if (e.y <= 10) {
          		sLastSearch = "";
          		updateLastSearch();
        		}
        	}
        });
        
        txtFilter = new Text(cHeader, SWT.BORDER);
        Messages.setLanguageTooltip(txtFilter, "MyTorrentsView.filter.tooltip");
        txtFilter.addKeyListener(this);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        txtFilter.setLayoutData(gridData);
        txtFilter.addModifyListener(new ModifyListener() {
        	public void modifyText(ModifyEvent e) {
        		sLastSearch = ((Text)e.widget).getText();
        		updateLastSearch();
        	}
        });
        txtFilter.addKeyListener(new KeyAdapter() {
        	public void keyPressed(KeyEvent e) {
        		if (e.keyCode == SWT.ARROW_DOWN) {
        			tv.setFocus();
        			e.doit = false;
        		} else if (e.character == 13) {
        			if (searchUpdateEvent != null) {
        				searchUpdateEvent.cancel();
        			}
        			searchUpdateEvent = null;
        			activateCategory(currentCategory);
        		}
        	}
        });
        
        lblSep = new Label(cHeader, SWT.SEPARATOR | SWT.VERTICAL);
        gridData = new GridData(GridData.FILL_VERTICAL);
        gridData.heightHint = 5;
        lblSep.setLayoutData(gridData);
        
        cHeader.moveAbove(null);
        cCategories.moveBelow(cHeader);
      } else {
        Control[] controls = cCategories.getChildren();
        for (int i = 0; i < controls.length; i++) {
          controls[i].dispose();
        }
      }

      int iFontPixelsHeight = 11;
      int iFontPointHeight = (iFontPixelsHeight * 72) / cCategories.getDisplay().getDPI().y;
      for (int i = 0; i < categories.length; i++) {
    	final Category category = categories[i];
    	
        final Button catButton = new Button(cCategories, SWT.TOGGLE);
        catButton.addKeyListener(this);
        if (i == 0 && fontButton == null) {
          Font f = catButton.getFont();
          FontData fd = f.getFontData()[0];
          fd.setHeight(iFontPointHeight);
          fontButton = new Font(cCategories.getDisplay(), fd);
        }
        catButton.setText("|");
        catButton.setFont(fontButton);
        catButton.pack(true);
        if (catButton.computeSize(100,SWT.DEFAULT).y > 0) {
          RowData rd = new RowData();
          rd.height = catButton.computeSize(100,SWT.DEFAULT).y - 2 + catButton.getBorderWidth() * 2;
          catButton.setLayoutData(rd);
        }

        String name = category.getName();
        if (category.getType() == Category.TYPE_USER)
          catButton.setText(name);
        else
          Messages.setLanguageText(catButton, name);

        catButton.setData("Category", category);
        if (category == currentCategory) {
          catButton.setSelection(true);
        }

        catButton.addSelectionListener(new SelectionAdapter() {
          public void widgetSelected(SelectionEvent e) {
            Button curButton = (Button)e.widget;
            boolean isEnabled = curButton.getSelection();
            Control[] controls = cCategories.getChildren();
            if (!isEnabled)
              curButton = (Button)controls[0];

            for (int i = 0; i < controls.length; i++) {
              Button b = (Button)controls[i];
              if (b != curButton && b.getSelection())
                b.setSelection(false);
              else if (b == curButton && !b.getSelection())
                b.setSelection(true);
            }
            activateCategory( (Category)curButton.getData("Category") );
          }
        });
        
        catButton.addListener(SWT.MouseHover, new Listener() {
        	public void handleEvent(Event event) {
            Button curButton = (Button)event.widget;
            Category curCategory = (Category)curButton.getData("Category");
            List dms = curCategory.getDownloadManagers(globalManager.getDownloadManagers());
            
            long ttlActive = 0;
            long ttlSize = 0;
            long ttlRSpeed = 0;
            long ttlSSpeed = 0;
            int count = 0;
            for (Iterator iter = dms.iterator(); iter.hasNext();) {
            	DownloadManager dm = (DownloadManager) iter.next();

            	if (!isInCategory(dm, currentCategory ))
            		continue;

            	count++;
            	if (dm.getState() == DownloadManager.STATE_DOWNLOADING
            			|| dm.getState() == DownloadManager.STATE_SEEDING)
            		ttlActive++;
            	ttlSize += dm.getSize();
            	ttlRSpeed += dm.getStats().getDataReceiveRate();
            	ttlSSpeed += dm.getStats().getDataSendRate();
            }

            String 	up_details		= "";
            String	down_details	= "";
            
		    if ( category.getType() != Category.TYPE_ALL ){
		        	 
	            String up_str 	= MessageText.getString( "GeneralView.label.maxuploadspeed" );
	            String down_str = MessageText.getString( "GeneralView.label.maxdownloadspeed" );
	            String unlimited_str = MessageText.getString( "MyTorrentsView.menu.setSpeed.unlimited" );
	             
	            int	up_speed 	= category.getUploadSpeed();
	            int	down_speed 	= category.getDownloadSpeed();
	            
	            up_details 		= up_str + ": " + (up_speed==0?unlimited_str:DisplayFormatters.formatByteCountToKiBEtc(up_speed));
	            down_details 	= down_str + ": " + (down_speed==0?unlimited_str:DisplayFormatters.formatByteCountToKiBEtc(down_speed));
		    }
		    
            if (count == 0) {
            	curButton.setToolTipText( down_details + "\n" + up_details + "\nTotal: 0" );
            	return;
            }
            
            curButton.setToolTipText(
            		(up_details.length()==0?"":( down_details + "\n" + up_details + "\n" )) +
            		"Total: " + count + "\n"
            		+ "Downloading/Seeding: " + ttlActive + "\n"
            		+ "\n"
            		+ "Speed: "
            		+ DisplayFormatters.formatByteCountToKiBEtcPerSec(ttlRSpeed / count) + "/" 
            		+ DisplayFormatters.formatByteCountToKiBEtcPerSec(ttlSSpeed / count) + "\n"
            		+ "Size: " + DisplayFormatters.formatByteCountToKiBEtc(ttlSize));
        	}
        });

        final DropTarget tabDropTarget = new DropTarget(catButton, DND.DROP_DEFAULT | DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);
        Transfer[] types = new Transfer[] { TextTransfer.getInstance()};
        tabDropTarget.setTransfer(types);
        tabDropTarget.addDropListener(new DropTargetAdapter() {
          public void dragOver(DropTargetEvent e) {
            if(drag_drop_line_start >= 0)
              e.detail = DND.DROP_MOVE;
            else
              e.detail = DND.DROP_NONE;
          }

          public void drop(DropTargetEvent e) {
            e.detail = DND.DROP_NONE;
            //System.out.println("DragDrop on Button:" + drag_drop_line_start);
            if(drag_drop_line_start >= 0) {
              drag_drop_line_start = -1;
              drag_drop_rows = null;

              TorrentUtil.assignToCategory(tv.getSelectedDataSources(), (Category)catButton.getData("Category"));
            }
          }
        });
        
        catButton.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent e) {
						if (tabDropTarget != null && !tabDropTarget.isDisposed()) {
							tabDropTarget.dispose();
						}
					}
        });

    	final Menu menu = new Menu(getComposite().getShell(), SWT.POP_UP);

        catButton.setMenu(menu);
        
    	menu.addMenuListener(
    		new MenuListener() 
    		{
    	    	boolean bShown = false;
    	    	
    			public void 
    			menuHidden(
    				MenuEvent e )
    			{
    				bShown = false;

    				if (Constants.isOSX)
    					return;

    				// Must dispose in an asyncExec, otherwise SWT.Selection doesn't
    				// get fired (async workaround provided by Eclipse Bug #87678)

    				e.widget.getDisplay().asyncExec(new AERunnable() {
    					public void runSupport() {
    						if (bShown || menu.isDisposed())
    							return;
    						MenuItem[] items = menu.getItems();
    						for (int i = 0; i < items.length; i++) {
    							items[i].dispose();
    						}
    					}
    				});
    			}

    			public void 
    			menuShown(
    				MenuEvent e) 
    			{
    				MenuItem[] items = menu.getItems();
    				for (int i = 0; i < items.length; i++)
    					items[i].dispose();

    				bShown = true;

    		        if ( category.getType() == Category.TYPE_USER ){
    		        	
	    		        final MenuItem itemDelete = new MenuItem(menu, SWT.PUSH);
	    		       
	    		        Messages.setLanguageText(itemDelete, "MyTorrentsView.menu.category.delete");
	    		        
	    		        menu.setDefaultItem(itemDelete);

	       		        itemDelete.addListener(SWT.Selection, new Listener() {
	    		        	public void handleEvent(Event event) {
	    		        		Category catToDelete = (Category)catButton.getData("Category");
	    		        		if (catToDelete != null) {
	    		        			java.util.List managers = catToDelete.getDownloadManagers(globalManager.getDownloadManagers());
	    		        			// move to array,since setcategory removed it from the category,
	    		        			// which would mess up our loop
	    		        			DownloadManager dms[] = (DownloadManager [])managers.toArray(new DownloadManager[managers.size()]);
	    		        			for (int i = 0; i < dms.length; i++) {
	    		        				dms[i].getDownloadState().setCategory(null);
	    		        			}
	    		        			if (currentCategory == catToDelete){

	    		        				activateCategory(CategoryManager.getCategory(Category.TYPE_ALL));

	    		        			}else{
	    		        				// always activate as deletion of this one might have
	    		        				// affected the current view 
	    		        				activateCategory(  currentCategory );
	    		        			}
	    		        			CategoryManager.removeCategory(catToDelete);
	    		        		}
	    		        	}
	    		        });
    		        }
    		        
    		        if ( category.getType() != Category.TYPE_ALL ){

	    				long maxDownload = COConfigurationManager.getIntParameter("Max Download Speed KBs", 0) * 1024;
	    				long maxUpload = COConfigurationManager.getIntParameter("Max Upload Speed KBs", 0) * 1024;
	
	       				int	down_speed 	= category.getDownloadSpeed();
	       				int	up_speed 	= category.getUploadSpeed();
	       			        				
	    		        ViewUtils.addSpeedMenu( 
	    		        		menu.getShell(), menu, true, true, 
	    		        		false, down_speed==0, down_speed, down_speed, maxDownload, 
	    		        		false, up_speed==0, up_speed, up_speed, maxUpload, 
	    		        		1, 
	    		        		new SpeedAdapter()
	    		        		{
	    		        			public void 
	    		        			setDownSpeed(int val) 
	    		        			{
	    		        				category.setDownloadSpeed( val );
	    		        			}
	    		        			public void 
	    		        			setUpSpeed(int val) 
	    		        			{
	    		        				category.setUploadSpeed( val );
	
	    		        			}
	    		        		});
    		        }
    		        
	        		java.util.List managers = category.getDownloadManagers(globalManager.getDownloadManagers());

	        		final DownloadManager dms[] = (DownloadManager [])managers.toArray(new DownloadManager[managers.size()]);

	        		boolean	start 	= false;
	        		boolean	stop	= false;
	        		
	        		for (int i=0;i<dms.length;i++){
	        			
	        			DownloadManager dm = dms[i];
	        			
	    				stop = stop || ManagerUtils.isStopable(dm);

	    				start = start || ManagerUtils.isStartable(dm);

	        		}
	        		
	        		// Queue
	        		
	        		final MenuItem itemQueue = new MenuItem(menu, SWT.PUSH);
	        		Messages.setLanguageText(itemQueue, "MyTorrentsView.menu.queue"); //$NON-NLS-1$
	        		Utils.setMenuItemImage(itemQueue, "start");
	        		itemQueue.addListener(SWT.Selection, new Listener(){
    					public void handleEvent(Event event) {
    						TorrentUtil.queueTorrents(dms, menu.getShell());
    					}
    				});
	        		itemQueue.setEnabled(start);
	        		
	        		// Stop
	        		
	        		final MenuItem itemStop = new MenuItem(menu, SWT.PUSH);
	        		Messages.setLanguageText(itemStop, "MyTorrentsView.menu.stop"); //$NON-NLS-1$
	        		Utils.setMenuItemImage(itemStop, "stop");
	        		itemStop.addListener(SWT.Selection, new Listener(){
    					public void handleEvent(Event event) {
    						TorrentUtil.stopTorrents(dms, menu.getShell());
    					}
    				});
	        		itemStop.setEnabled(stop);
	        		
	        		// options
	        		
    				MenuItem itemOptions = new MenuItem(menu, SWT.PUSH);
    				
    				Messages.setLanguageText(itemOptions, "MainWindow.menu.view.configuration");
    				itemOptions.addListener(SWT.Selection, new Listener(){
    					public void handleEvent(Event event) {
    						UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
 
    						uiFunctions.showMultiOptionsView( dms );
    					}
    				});
	        		
    				if ( dms.length == 0 ){
    					
    					itemOptions.setEnabled( false );
    				}
    			}
    		});
      }
     

      cCategories.layout();
      getComposite().layout();

      // layout hack - relayout
			if (catResizeAdapter == null) {
				catResizeAdapter = new ControlAdapter() {
					public void controlResized(ControlEvent event) {
						if (getComposite().isDisposed() || cCategories.isDisposed())
							return;

						GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);

						int parentWidth = cCategories.getParent().getClientArea().width;
						int catsWidth = cCategories.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
						// give text a 5 pixel right padding
						int textWidth = 5
								+ cHeader.computeSize(SWT.DEFAULT, SWT.DEFAULT).x
								+ cHeader.getBorderWidth() * 2;

						Object layoutData = cHeader.getLayoutData();
						if (layoutData instanceof GridData) {
							GridData labelGrid = (GridData) layoutData;
							textWidth += labelGrid.horizontalIndent;
						}

						if (textWidth + catsWidth > parentWidth) {
							gridData.widthHint = parentWidth - textWidth;
						}
						cCategories.setLayoutData(gridData);
						cCategories.getParent().layout(true);

					}
				};

				tv.getTableComposite().addControlListener(catResizeAdapter);
			}

      catResizeAdapter.controlResized(null);
    }
  }
  
  private boolean isOurDownloadManager(DownloadManager dm) {
  	if (!isInCategory(dm, currentCategory)) {
  		return false;
  	}

		boolean bCompleted =  dm.isDownloadComplete(bDNDalwaysIncomplete);
		boolean bOurs = (bCompleted && isSeedingView)
				|| (!bCompleted && !isSeedingView);
		
//		System.out.println("ourDM? " + sTableID + "; " + dm.getDisplayName()
//				+ "; Complete=" + bCompleted + ";Ours=" + bOurs + ";bc"
//				+ dm.getStats().getDownloadCompleted(false) + ";"
//				+ dm.getStats().getDownloadCompleted(true));

		if (bOurs && sLastSearch.length() > 0) {
			try {
				String[][] names = {	{"", 		dm.getDisplayName()},
												{"t:", 	dm.getTorrent().getAnnounceURL().getHost()},
												{"st:", 	"" + dm.getState()}
											};
				
				String name = names[0][1];
				String tmpSearch = sLastSearch;
				
				for(int i = 0; i < names.length; i++){
					if (tmpSearch.startsWith(names[i][0])) {
						tmpSearch = tmpSearch.substring(names[i][0].length());
						name = names[i][1];
					}
				}
				
				String s = bRegexSearch ? tmpSearch : "\\Q"
						+ tmpSearch.replaceAll("[|;]", "\\\\E|\\\\Q") + "\\E";
				Pattern pattern = Pattern.compile(s, Pattern.CASE_INSENSITIVE);

				if (!pattern.matcher(name).find())
					bOurs = false;
			} catch (Exception e) {
				// Future: report PatternSyntaxException message to user.
			}
		}

		return bOurs;
	}

  // @see com.aelitis.azureus.ui.common.table.TableSelectionListener#selected(com.aelitis.azureus.ui.common.table.TableRowCore[])
  public void selected(TableRowCore[] rows) {
  	refreshIconBar();
  	refreshTorrentMenu();
  }

	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#deselected(com.aelitis.azureus.ui.common.table.TableRowCore[])
	public void deselected(TableRowCore[] rows) {
  	refreshIconBar();
  	refreshTorrentMenu();
	}

	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#focusChanged(com.aelitis.azureus.ui.common.table.TableRowCore)
	public void focusChanged(TableRowCore focus) {
  	refreshIconBar();
  	refreshTorrentMenu();
	}

  private void refreshIconBar() {
  	computePossibleActions();
  	UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
  	if (uiFunctions != null) {
  		uiFunctions.refreshIconBar();
  	}
  }
  
	private void refreshTorrentMenu() {
		UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
		if (uiFunctions != null && uiFunctions instanceof UIFunctionsSWT) {
			((UIFunctionsSWT)uiFunctions).refreshTorrentMenu();
		}
	}
	
	public DownloadManager[] getSelectedDownloads() {
		Object[] data_sources = tv.getSelectedDataSources();
		DownloadManager[] result = new DownloadManager[data_sources.length];
		System.arraycopy(data_sources, 0, result, 0, result.length);
		return result;
	}

  // @see com.aelitis.azureus.ui.common.table.TableSelectionListener#defaultSelected(com.aelitis.azureus.ui.common.table.TableRowCore[])
  public void defaultSelected(TableRowCore[] rows) {
	Object[] dm_sources = tv.getSelectedDataSources();
	UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
	for (int i=0; i<dm_sources.length; i++) {
		if (dm_sources[i] == null) {continue;}
		if (uiFunctions != null) {
	  		uiFunctions.openManagerView((DownloadManager)dm_sources[i]);
	  	}
    }
  }



  /* SubMenu for column specific tasks.
   */
  // @see org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener#addThisColumnSubMenu(java.lang.String, org.eclipse.swt.widgets.Menu)
  public void addThisColumnSubMenu(String sColumnName, Menu menuThisColumn) {
    if (sColumnName.equals("health")) {
      MenuItem item = new MenuItem(menuThisColumn, SWT.PUSH);
      Messages.setLanguageText(item, "MyTorrentsView.menu.health");
      Utils.setMenuItemImage(item, "st_explain");
      item.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          HealthHelpWindow.show(Display.getDefault());
        }
      });

    } else if (sColumnName.equals("maxuploads")) {
      int iStart = COConfigurationManager.getIntParameter("Max Uploads") - 2;
      if (iStart < 2) iStart = 2;
      for (int i = iStart; i < iStart + 6; i++) {
        MenuItem item = new MenuItem(menuThisColumn, SWT.PUSH);
        item.setText(String.valueOf(i));
        item.setData("MaxUploads", new Long(i));
        item.addListener(SWT.Selection,
                         new TableSelectedRowsListener(tv) {
          public void run(TableRowCore row) {
            DownloadManager dm = (DownloadManager)row.getDataSource(true);
            MenuItem item = (MenuItem)event.widget;
            if (item != null) {
              int value = ((Long)item.getData("MaxUploads")).intValue();
              dm.setMaxUploads(value);
            }
          } // run
        }); // listener
      } // for
    }
  }
  
	  public void fillMenu(final Menu menu) {
			Object[] dm_items = tv.getSelectedDataSources();
			boolean hasSelection = (dm_items.length > 0);

			if (hasSelection) {
				DownloadManager[] dms = new DownloadManager[dm_items.length];
				for (int i=0; i<dm_items.length; i++) {
					dms[i] = (DownloadManager)dm_items[i];
				}
				TorrentUtil.fillTorrentMenu(menu, dms, azureus_core, cTablePanel, true,
					(isSeedingView) ? 2 : 1, tv);

				// ---
				new MenuItem(menu, SWT.SEPARATOR);
			}
			
			final MenuItem itemFilter = new MenuItem(menu, SWT.PUSH);
			Messages.setLanguageText(itemFilter, "MyTorrentsView.menu.filter");
			itemFilter.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					openFilterDialog();
				}
			});

  }

	private void createDragDrop() {
		try {

			Transfer[] types = new Transfer[] { TextTransfer.getInstance() };

			if (dragSource != null && !dragSource.isDisposed()) {
				dragSource.dispose();
			}

			if (dropTarget != null && !dropTarget.isDisposed()) {
				dropTarget.dispose();
			}

			dragSource = tv.createDragSource(DND.DROP_MOVE);
			if (dragSource != null) {
				dragSource.setTransfer(types);
				dragSource.addDragListener(new DragSourceAdapter() {
					public void dragStart(DragSourceEvent event) {
						TableRowCore[] rows = tv.getSelectedRows();
						if (rows.length != 0) {
							event.doit = true;
							// System.out.println("DragStart");
							drag_drop_line_start = rows[0].getIndex();
							drag_drop_rows = rows;
						} else {
							event.doit = false;
							drag_drop_line_start = -1;
							drag_drop_rows = null;
						}
					}

					public void dragSetData(DragSourceEvent event) {
						// System.out.println("DragSetData");
						event.data = "moveRow";
					}
				});
			}

			dropTarget = tv.createDropTarget(DND.DROP_DEFAULT | DND.DROP_MOVE
					| DND.DROP_COPY | DND.DROP_LINK | DND.DROP_TARGET_MOVE);
			if (dropTarget != null) {
				if (SWT.getVersion() >= 3107) {
					dropTarget.setTransfer(new Transfer[] { HTMLTransfer.getInstance(),
							URLTransfer.getInstance(), FileTransfer.getInstance(),
							TextTransfer.getInstance() });
				} else {
					dropTarget.setTransfer(new Transfer[] { URLTransfer.getInstance(),
							FileTransfer.getInstance(), TextTransfer.getInstance() });
				}

				dropTarget.addDropListener(new DropTargetAdapter() {
					public void dropAccept(DropTargetEvent event) {
						event.currentDataType = URLTransfer.pickBestType(event.dataTypes,
								event.currentDataType);
					}

					public void dragEnter(DropTargetEvent event) {
						// no event.data on dragOver, use drag_drop_line_start to determine
						// if ours
						if (drag_drop_line_start < 0) {
							if (event.detail != DND.DROP_COPY) {
								if ((event.operations & DND.DROP_LINK) > 0)
									event.detail = DND.DROP_LINK;
								else if ((event.operations & DND.DROP_COPY) > 0)
									event.detail = DND.DROP_COPY;
							}
						} else if (TextTransfer.getInstance().isSupportedType(
								event.currentDataType)) {
							event.detail = event.item == null ? DND.DROP_NONE : DND.DROP_MOVE;
							event.feedback = DND.FEEDBACK_SCROLL | DND.FEEDBACK_INSERT_BEFORE;
						}
					}

					public void dragOver(DropTargetEvent event) {
						if (drag_drop_line_start >= 0) {
							event.detail = event.item == null ? DND.DROP_NONE : DND.DROP_MOVE;
							event.feedback = DND.FEEDBACK_SCROLL | DND.FEEDBACK_INSERT_BEFORE;
						}
					}

					public void drop(DropTargetEvent event) {
						if (!(event.data instanceof String)
								|| !((String) event.data).equals("moveRow")) {
							TorrentOpener.openDroppedTorrents(azureus_core, event, true);
							return;
						}

						event.detail = DND.DROP_NONE;
						// Torrent file from shell dropped
						if (drag_drop_line_start >= 0) { // event.data == null
							event.detail = DND.DROP_NONE;
							TableRowCore row = tv.getRow(event);
							if (row == null)
								return;
							int drag_drop_line_end = row.getIndex();
							if (drag_drop_line_end != drag_drop_line_start) {
								DownloadManager dm = (DownloadManager) row.getDataSource(true);
								moveRowsTo(drag_drop_rows, dm.getPosition());
								event.detail = DND.DROP_MOVE;
							}
							drag_drop_line_start = -1;
							drag_drop_rows = null;
						}
					}
				});
			}

		} catch (Throwable t) {
			Logger.log(new LogEvent(LOGID, "failed to init drag-n-drop", t));
		}
	}
  
  private void moveRowsTo(TableRowCore[] rows, int iNewPos) {
    if (rows == null || rows.length == 0) {
      return;
    }
    
    TableColumnCore sortColumn = tv.getSortColumn();
    boolean isSortAscending = sortColumn == null ? true
				: sortColumn.isSortAscending();

    for (int i = 0; i < rows.length; i++) {
			TableRowCore row = rows[i];
      DownloadManager dm = (DownloadManager)row.getDataSource(true);
      int iOldPos = dm.getPosition();
      
      globalManager.moveTo(dm, iNewPos);
      if (isSortAscending) {
        if (iOldPos > iNewPos)
          iNewPos++;
      } else {
        if (iOldPos < iNewPos)
          iNewPos--;
      }
    }

    boolean bForceSort = sortColumn.getName().equals("#");
    tv.columnInvalidate("#");
    tv.refreshTable(bForceSort);
  }

  // @see com.aelitis.azureus.ui.common.table.TableRefreshListener#tableRefresh()
  public void tableRefresh() {
    if (tv.isDisposed())
      return;
    
    isTrackerOn = TRTrackerUtils.isTrackerEnabled();
    
    refreshIconBar();
    refreshTorrentMenu();
  }


	// @see org.eclipse.swt.events.KeyListener#keyPressed(org.eclipse.swt.events.KeyEvent)
	public void keyPressed(KeyEvent e) {
		int key = e.character;
		if (key <= 26 && key > 0)
			key += 'a' - 1;

		if (e.stateMask == (SWT.CTRL | SWT.SHIFT)) {
			// CTRL+SHIFT+S stop all Torrents
			if (key == 's') {
				ManagerUtils.asyncStopAll();
				e.doit = false;
				return;
			}

			// Can't capture Ctrl-PGUP/DOWN for moving up/down in chunks
			// (because those keys move through tabs), so use shift-ctrl-up/down
			if (e.keyCode == SWT.ARROW_DOWN) {
				moveSelectedTorrents(10);
				e.doit = false;
				return;
			}

			if (e.keyCode == SWT.ARROW_UP) {
				moveSelectedTorrents(-10);
				e.doit = false;
				return;
			}
		}
		
		if (e.stateMask == SWT.MOD1) {
			switch (key) {
				case 'a': // CTRL+A select all Torrents
					if (e.widget != txtFilter) {
						tv.selectAll();
						e.doit = false;
					}
					break;
				case 'c': // CTRL+C
					if (e.widget != txtFilter) {
						tv.clipboardSelected();
						e.doit = false;
					}
					break;
				case 'f': // CTRL+F Find/Filter
					openFilterDialog();
					e.doit = false;
					break;
			}

			if (!e.doit)
				return;
		}

		if (e.stateMask == SWT.CTRL) {
			switch (e.keyCode) {
				case SWT.ARROW_UP:
					moveSelectedTorrentsUp();
					e.doit = false;
					break;
				case SWT.ARROW_DOWN:
					moveSelectedTorrentsDown();
					e.doit = false;
					break;
				case SWT.HOME:
					moveSelectedTorrentsTop();
					e.doit = false;
					break;
				case SWT.END:
					moveSelectedTorrentsEnd();
					e.doit = false;
					break;
			}
			if (!e.doit)
				return;

			switch (key) {
				case 'r': // CTRL+R resume/start selected Torrents
					TorrentUtil.resumeTorrents(tv.getSelectedDataSources());
					e.doit = false;
					break;
				case 's': // CTRL+S stop selected Torrents
					TorrentUtil.stopTorrents(tv.getSelectedDataSources(), cTablePanel.getShell());
					e.doit = false;
					break;
				case 'x': // CTRL+X: RegEx search switch
					bRegexSearch = !bRegexSearch;
					e.doit = false;
					updateLastSearch();
					break;
			}

			if (!e.doit)
				return;
		}

		// DEL remove selected Torrents
		if (e.stateMask == 0 && e.keyCode == SWT.DEL && e.widget != txtFilter) {
			TorrentUtil.removeTorrents(tv.getSelectedDataSources(), cTablePanel.getShell());
			e.doit = false;
			return;
		}

		if (e.keyCode != SWT.BS) {
			if ((e.stateMask & (~SWT.SHIFT)) != 0 || e.character < 32)
				return;
		}
		
		if (e.widget == txtFilter)
			return;

		// normal character: jump to next item with a name beginning with this character
		if (ASYOUTYPE_MODE == ASYOUTYPE_MODE_FIND) {
			if (System.currentTimeMillis() - lLastSearchTime > 3000)
				sLastSearch = "";
		}

		if (e.keyCode == SWT.BS) {
			if (e.stateMask == SWT.CONTROL)
				sLastSearch = "";
			else if (sLastSearch.length() > 0)
				sLastSearch = sLastSearch.substring(0, sLastSearch.length() - 1);
		} else
			sLastSearch += String.valueOf(e.character);

		if (ASYOUTYPE_MODE == ASYOUTYPE_MODE_FILTER) {
			if (txtFilter != null && !txtFilter.isDisposed()) {
				txtFilter.setFocus();
			}
			updateLastSearch();
		} else {
			TableCellCore[] cells = tv.getColumnCells("name");

			//System.out.println(sLastSearch);

			Arrays.sort(cells, TableCellImpl.TEXT_COMPARATOR);
			int index = Arrays.binarySearch(cells, sLastSearch,
					TableCellImpl.TEXT_COMPARATOR);
			if (index < 0) {

				int iEarliest = -1;
				String s = bRegexSearch ? sLastSearch : "\\Q" + sLastSearch + "\\E"; 
				Pattern pattern = Pattern.compile(s, Pattern.CASE_INSENSITIVE);
				for (int i = 0; i < cells.length; i++) {
					Matcher m = pattern.matcher(cells[i].getText());
					if (m.find() && (m.start() < iEarliest || iEarliest == -1)) {
						iEarliest = m.start();
						index = i;
					}
				}

				if (index < 0)
					// Insertion Point (best guess)
					index = -1 * index - 1;
			}

			if (index >= 0) {
				if (index >= cells.length)
					index = cells.length - 1;
				TableRowCore row = cells[index].getTableRowCore();
				int iTableIndex = row.getIndex();
				if (iTableIndex >= 0) {
					tv.setSelectedRows(new TableRowCore[] { row });
				}
			}
			lLastSearchTime = System.currentTimeMillis();
			updateTableLabel();
		}
		e.doit = false;
	}

	private void openFilterDialog() {
		InputShell is = new InputShell("MyTorrentsView.dialog.setFilter.title",
				"MyTorrentsView.dialog.setFilter.text");
		is.setTextValue(sLastSearch);
		is.setLabelParameters(new String[] { MessageText.getString(tv.getTableID() + "View"
				+ ".header")
		});

		String sReturn = is.open();
		if (sReturn == null)
			return;
		
		sLastSearch = sReturn;
		updateLastSearch();
	}
	
	private void updateLastSearch() {
		if (lblHeader == null || lblHeader.isDisposed())
			createTabs();

		if (txtFilter != null && !txtFilter.isDisposed()) {
			if (!sLastSearch.equals(txtFilter.getText())) { 
				txtFilter.setText(sLastSearch);
				txtFilter.setSelection(sLastSearch.length());
			}

				if (bRegexSearch) {
				try {
					Pattern.compile(sLastSearch, Pattern.CASE_INSENSITIVE);
					txtFilter.setBackground(Colors.colorAltRow);
					Messages.setLanguageTooltip(txtFilter,
							"MyTorrentsView.filter.tooltip");
				} catch (Exception e) {
					txtFilter.setBackground(Colors.colorErrorBG);
					txtFilter.setToolTipText(e.getMessage());
				}
			} else {
				txtFilter.setBackground(null);
				Messages.setLanguageTooltip(txtFilter, "MyTorrentsView.filter.tooltip");
			}
		}
		if (lblX != null && !lblX.isDisposed()) {
			Image img = ImageRepository.getImage(sLastSearch.length() > 0 ? "smallx"
					: "smallx-gray");

			lblX.setImage(img);
		}

		if (searchUpdateEvent != null) {
			searchUpdateEvent.cancel();
		}
		searchUpdateEvent = SimpleTimer.addEvent("SearchUpdate",
				SystemTime.getOffsetTime(ASYOUTYPE_UPDATEDELAY),
				new TimerEventPerformer() {
					public void perform(TimerEvent event) {
						if (searchUpdateEvent.isCancelled()) {
							searchUpdateEvent = null;
							return;
						}
						searchUpdateEvent = null;
						activateCategory(currentCategory);
					}
				});
	}

	public void keyReleased(KeyEvent e) {
		// ignore
	}





  private void moveSelectedTorrentsDown() {
    // Don't use runForSelectDataSources to ensure the order we want
    Object[] dataSources = tv.getSelectedDataSources();
    Arrays.sort(dataSources, new Comparator() {
      public int compare (Object a, Object b) {
        return ((DownloadManager)a).getPosition() - ((DownloadManager)b).getPosition();
      }
    });
    for (int i = dataSources.length - 1; i >= 0; i--) {
      DownloadManager dm = (DownloadManager)dataSources[i];
      if (dm.getGlobalManager().isMoveableDown(dm)) {
        dm.getGlobalManager().moveDown(dm);
      }
    }

    boolean bForceSort = tv.getSortColumn().getName().equals("#");
    tv.columnInvalidate("#");
    tv.refreshTable(bForceSort);
  }

  private void moveSelectedTorrentsUp() {
    // Don't use runForSelectDataSources to ensure the order we want
    Object[] dataSources = tv.getSelectedDataSources();
    Arrays.sort(dataSources, new Comparator() {
      public int compare (Object a, Object b) {
        return ((DownloadManager)a).getPosition() - ((DownloadManager)b).getPosition();
      }
    });
    for (int i = 0; i < dataSources.length; i++) {
      DownloadManager dm = (DownloadManager)dataSources[i];
      if (dm.getGlobalManager().isMoveableUp(dm)) {
        dm.getGlobalManager().moveUp(dm);
      }
    }

    boolean bForceSort = tv.getSortColumn().getName().equals("#");
    tv.columnInvalidate("#");
    tv.refreshTable(bForceSort);
  }

	private void moveSelectedTorrents(int by) {
		// Don't use runForSelectDataSources to ensure the order we want
		Object[] dataSources = tv.getSelectedDataSources();
		if (dataSources.length <= 0)
			return;

		int[] newPositions = new int[dataSources.length];

		if (by < 0) {
			Arrays.sort(dataSources, new Comparator() {
				public int compare(Object a, Object b) {
					return ((DownloadManager) a).getPosition()
							- ((DownloadManager) b).getPosition();
				}
			});
		} else {
			Arrays.sort(dataSources, new Comparator() {
				public int compare(Object a, Object b) {
					return ((DownloadManager) b).getPosition()
							- ((DownloadManager) a).getPosition();
				}
			});
		}

		int count = globalManager.downloadManagerCount(isSeedingView); 
		for (int i = 0; i < dataSources.length; i++) {
			DownloadManager dm = (DownloadManager) dataSources[i];
			int pos = dm.getPosition() + by;
			if (pos < i + 1)
				pos = i + 1;
			else if (pos > count - i)
				pos = count - i;

			newPositions[i] = pos;
		}

		for (int i = 0; i < dataSources.length; i++) {
			DownloadManager dm = (DownloadManager) dataSources[i];
			globalManager.moveTo(dm, newPositions[i]);
		}

    boolean bForceSort = tv.getSortColumn().getName().equals("#");
    tv.columnInvalidate("#");
    tv.refreshTable(bForceSort);
	}

  private void moveSelectedTorrentsTop() {
    moveSelectedTorrentsTopOrEnd(true);
  }

  private void moveSelectedTorrentsEnd() {
    moveSelectedTorrentsTopOrEnd(false);
  }

  private void moveSelectedTorrentsTopOrEnd(boolean moveToTop) {
  	Object[] datasources = tv.getSelectedDataSources();
    if (datasources.length == 0)
      return;
  	DownloadManager[] downloadManagers = new DownloadManager[datasources.length];
  	System.arraycopy(datasources, 0, downloadManagers, 0, datasources.length);

    if(moveToTop)
      globalManager.moveTop(downloadManagers);
    else
      globalManager.moveEnd(downloadManagers);

    boolean bForceSort = tv.getSortColumn().getName().equals("#");
    if (bForceSort) {
    	tv.columnInvalidate("#");
    	tv.refreshTable(bForceSort);
    }
  }

  /**
   * @param parameterName the name of the parameter that has changed
   * @see org.gudy.azureus2.core3.config.ParameterListener#parameterChanged(java.lang.String)
   */
  public void parameterChanged(String parameterName) {
		if (parameterName == null || parameterName.equals("User Mode")) {
			userMode = COConfigurationManager.getIntParameter("User Mode");
		}

		if (parameterName == null
				|| parameterName.equals("DND Always In Incomplete")) {
			bDNDalwaysIncomplete = COConfigurationManager.getBooleanParameter("DND Always In Incomplete");
		}
	}

  private boolean top,bottom,up,down,run,host,publish,start,stop,remove;

  private void computePossibleActions() {
    Object[] dataSources = tv.getSelectedDataSources();
    // enable up and down so that we can do the "selection rotate trick"
    up = down = run =  remove = (dataSources.length > 0);
    top = bottom = start = stop = host = publish = false;
    for (int i = 0; i < dataSources.length; i++) {
      DownloadManager dm = (DownloadManager)dataSources[i];

      if(!start && ManagerUtils.isStartable(dm))
        start =  true;
      if(!stop && ManagerUtils.isStopable(dm))
        stop = true;
      if(!top && dm.getGlobalManager().isMoveableUp(dm))
        top = true;
      if(!bottom && dm.getGlobalManager().isMoveableDown(dm))
        bottom = true;
      
      if(userMode>0 && isTrackerOn)
    	  host = publish = true;
    }
  }

  public boolean isEnabled(String itemKey) {
    if(itemKey.equals("run"))
      return run;
    if(itemKey.equals("host"))
      return host;
    if(itemKey.equals("publish"))
      return publish;
    if(itemKey.equals("start"))
      return start;
    if(itemKey.equals("stop"))
      return stop;
    if(itemKey.equals("remove"))
      return remove;
    if(itemKey.equals("top"))
      return top;
    if(itemKey.equals("bottom"))
      return bottom;
    if(itemKey.equals("up"))
      return up;
    if(itemKey.equals("down"))
      return down;
    return false;
  }

  public void itemActivated(String itemKey) {
    if(itemKey.equals("top")) {
      moveSelectedTorrentsTop();
      return;
    }
    if(itemKey.equals("bottom")){
      moveSelectedTorrentsEnd();
      return;
    }
    if(itemKey.equals("up")) {
      moveSelectedTorrentsUp();
      return;
    }
    if(itemKey.equals("down")){
      moveSelectedTorrentsDown();
      return;
    }
    if(itemKey.equals("run")){
      TorrentUtil.runTorrents(tv.getSelectedDataSources());
      return;
    }
    if(itemKey.equals("host")){
    	TorrentUtil.hostTorrents(tv.getSelectedDataSources(), azureus_core, cTablePanel);
      return;
    }
    if(itemKey.equals("publish")){
    	TorrentUtil.publishTorrents(tv.getSelectedDataSources(), azureus_core, cTablePanel);
      return;
    }
    if(itemKey.equals("start")){
      TorrentUtil.queueTorrents(tv.getSelectedDataSources(), cTablePanel.getShell());
      return;
    }
    if(itemKey.equals("stop")){
      TorrentUtil.stopTorrents(tv.getSelectedDataSources(), cTablePanel.getShell());
      return;
    }
    if(itemKey.equals("remove")){
      TorrentUtil.removeTorrents(tv.getSelectedDataSources(), cTablePanel.getShell());
      return;
    }
  }
  

  // categorymanagerlistener Functions
  public void downloadManagerAdded(Category category, final DownloadManager manager)
  {
  	if (isOurDownloadManager(manager)) {
      tv.addDataSource(manager);
    }
  }

  public void downloadManagerRemoved(Category category, DownloadManager removed)
  {
    tv.removeDataSource(removed);
  }


  // DownloadManagerListener Functions
  public void stateChanged(DownloadManager manager, int state) {
    final TableRowCore row = tv.getRow(manager);
    if (row != null) {
    	Utils.execSWTThreadLater(0, new AERunnable() {
				public void runSupport() {
		    	row.refresh(true);
		    	if (row.isSelected()) {
		    		refreshIconBar();
		    	}
				}
    	});
    }
  }

  // DownloadManagerListener
  public void positionChanged(DownloadManager download, int oldPosition, int newPosition) {
  	if (isOurDownloadManager(download)) {
    	Utils.execSWTThreadLater(0, new AERunnable() {
				public void runSupport() {
					refreshIconBar();
				}
    	});
  	}
  }
  
  // DownloadManagerListener
  public void filePriorityChanged(DownloadManager download,
			DiskManagerFileInfo file) {
	}

  // DownloadManagerListener
	public void completionChanged(final DownloadManager manager,
			boolean bCompleted) {
		// manager has moved lists

		if (isOurDownloadManager(manager)) {

			// only make the download visible if it satisfies the category selection

			if (currentCategory == null
					|| currentCategory.getType() == Category.TYPE_ALL) {

				tv.addDataSource(manager);

			} else {

				int catType = currentCategory.getType();

				Category manager_category = manager.getDownloadState().getCategory();

				if (manager_category == null) {

					if (catType == Category.TYPE_UNCATEGORIZED) {

						tv.addDataSource(manager);
					}
				} else {

					if (currentCategory.getName().equals(manager_category.getName()))

						tv.addDataSource(manager);
				}
			}
		} else if ((isSeedingView && !bCompleted) || (!isSeedingView && bCompleted)) {

			tv.removeDataSource(manager);
		}
	}

  // DownloadManagerListener
  public void downloadComplete(DownloadManager manager) {
  }


  /**
   * Rebuild the table based on the category activated
   * 
   * @param category
   */
  private void activateCategory(Category category) {
		if (category != currentCategory) {
			if (currentCategory != null)
				currentCategory.removeCategoryListener(this);
			if (category != null)
				category.addCategoryListener(this);

			currentCategory = category;
		}
		
		Object[] managers = globalManager.getDownloadManagers().toArray();
		List list = Arrays.asList(tv.getDataSources());
		List listRemoves = new ArrayList();
		List listAdds = new ArrayList();
		
		for (int i = 0; i < managers.length; i++) {
			DownloadManager dm = (DownloadManager) managers[i];
		
			boolean bHave = list.contains(dm);
			if (!isOurDownloadManager(dm)) {
				if (bHave) {
					listRemoves.add(dm);
				}
			} else {
				if (!bHave) {
					listAdds.add(dm);
				}
			}
		}
		tv.removeDataSources(listRemoves.toArray());
		tv.addDataSources(listAdds.toArray());
		
  	tv.processDataSourceQueue();
		//tv.refreshTable(false);
	}
  
  private boolean isInCategory(DownloadManager manager, Category category) {
  	if (category == null) {
  		return true;
  	}
		int type = category.getType();
		if (type == Category.TYPE_ALL) {
			return true;
		}

  	Category dmCategory = manager.getDownloadState().getCategory();
  	if (dmCategory == null) {
  		return type == Category.TYPE_UNCATEGORIZED;
  	}
  	
  	return category.equals(dmCategory);
  }


  // CategoryManagerListener Functions
  public void categoryAdded(Category category) {
  	Utils.execSWTThread(
	  		new AERunnable() 
			{
	  			public void 
				runSupport() 
	  			{
	  				createTabs();
	  			}
			});
  }

  public void categoryRemoved(Category category) {
  	Utils.execSWTThread(
	  		new AERunnable() 
			{
	  			public void 
				runSupport() 
	  			{
	  				createTabs();
	  			}
			});
  }

  // globalmanagerlistener Functions
  public void downloadManagerAdded( DownloadManager dm ) {
    dm.addListener( this );
    downloadManagerAdded(null, dm);
  }

  public void downloadManagerRemoved( DownloadManager dm ) {
    dm.removeListener( this );
    DownloadBar.close(dm);
    downloadManagerRemoved(null, dm);
  }

  public void destroyInitiated() {  }
  public void destroyed() { }
  public void seedingStatusChanged( boolean seeding_only_mode ){}       

  // End of globalmanagerlistener Functions
  


  // @see com.aelitis.azureus.ui.common.table.TableCountChangeListener#rowAdded(com.aelitis.azureus.ui.common.table.TableRowCore)
  public void rowAdded(TableRowCore row) {
		updateTableLabel();
  }

  // @see com.aelitis.azureus.ui.common.table.TableCountChangeListener#rowRemoved(com.aelitis.azureus.ui.common.table.TableRowCore)
  public void rowRemoved(TableRowCore row) {
		updateTableLabel();
	}
	

	public void updateLanguage() {
		super.updateLanguage();
		updateTableLabel();
		getComposite().layout(true, true);
	}

	/**
	 * 
	 */
	private boolean refreshingTableLabel = false;
	private void updateTableLabel() {
		if (refreshingTableLabel || lblHeader == null || lblHeader.isDisposed()) {
			return;
		}
		refreshingTableLabel = true;
		lblHeader.getDisplay().asyncExec(new AERunnable() {
			public void runSupport() {
				try {
					if (lblHeader != null && !lblHeader.isDisposed()) {
						String sText = MessageText.getString(tv.getTableID() + "View"
								+ ".header")
								+ " (" + tv.size(true) + ")";
						lblHeader.setText(sText);
						lblHeader.getParent().layout();
					}
				} finally {
					refreshingTableLabel = false;
				}
			}
		});
	}

	public boolean isTableFocus() {
		return tv.isTableFocus();
	}
	
	public Image obfusticatedImage(final Image image, Point shellOffset) {
		return tv.obfusticatedImage(image, shellOffset);
	}
}
