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

import java.net.URL;
import java.util.*;
import java.util.List;
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
import org.gudy.azureus2.core3.disk.DiskManagerFileInfoSet;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentAnnounceURLSet;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.download.DownloadTypeComplete;
import org.gudy.azureus2.plugins.download.DownloadTypeIncomplete;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.plugins.ui.tables.TableRowRefreshListener;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarActivationListener;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.URLTransfer;
import org.gudy.azureus2.ui.swt.components.CompositeMinSize;
import org.gudy.azureus2.ui.swt.help.HealthHelpWindow;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.minibar.DownloadBar;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.views.table.*;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewSWTImpl;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab;
import org.gudy.azureus2.ui.swt.views.utils.CategoryUIUtils;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.util.RegExUtil;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.ui.common.table.*;
import com.aelitis.azureus.ui.selectedcontent.SelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

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
       extends TableViewTab<DownloadManager>
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
                  TableViewFilterCheck.TableViewFilterCheckEx<DownloadManager>,
                  TableRowRefreshListener
{
	private static final LogIDs LOGID = LogIDs.GUI;
	
	private AzureusCore		azureus_core;

  private GlobalManager globalManager;
  protected boolean isSeedingView;

  private Composite cTablePanel;
  private Font fontButton = null;
  protected Composite cCategories;
  private DragSource dragSource = null;
  private DropTarget dropTarget = null;
  protected Text txtFilter = null;
  private TimerEventPeriodic	txtFilterUpdateEvent;

  
  private Category currentCategory;

  // table item index, where the drag has started
  private int drag_drop_line_start = -1;
  private TableRowCore[] drag_drop_rows = null;

	private boolean bDNDalwaysIncomplete;
	private TableViewSWT<DownloadManager> tv;
	private Composite cTableParentPanel;
	protected boolean viewActive;
	private boolean forceHeaderVisible = false;
	private TableSelectionListener defaultSelectedListener;

	private Composite filterParent;

	private boolean neverShowCatButtons;
	
	private boolean rebuildListOnFocusGain = false;

	public MyTorrentsView() {
		super("MyTorrentsView");
	}
	
  /**
   * Initialize
   * 
   * @param _azureus_core
   * @param isSeedingView
   * @param basicItems
   * @param cCats 
   */
  public 
  MyTorrentsView(
  		AzureusCore				_azureus_core,
  		String						tableID,
  		boolean 					isSeedingView,
      TableColumnCore[]	basicItems,
      Text txtFilter, Composite cCats) 
  {
		super("MyTorrentsView");
		this.txtFilter = txtFilter;
		this.cCategories = cCats;
		init(_azureus_core, tableID, isSeedingView, isSeedingView
				? DownloadTypeComplete.class : DownloadTypeIncomplete.class, basicItems);
  }
  
  // @see org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab#initYourTableView()
  public TableViewSWT<DownloadManager> initYourTableView() {
  	return tv;
  }
  
  public void init(AzureusCore _azureus_core, String tableID,
			boolean isSeedingView, Class<?> forDataSourceType, TableColumnCore[] basicItems) {

  	this.isSeedingView 	= isSeedingView;
  	
    tv = createTableView(forDataSourceType, tableID, basicItems);
    tv.setRowDefaultIconSize(new Point(16, 16));
    
    /*
     * 'Big' table has taller row height
     */
    if (getRowDefaultHeight() > 0) {
			tv.setRowDefaultHeight(getRowDefaultHeight());
		}
    
    azureus_core		= _azureus_core;
    this.globalManager 	= azureus_core.getGlobalManager();
    

    if (currentCategory == null) {
    	currentCategory = CategoryManager.getCategory(Category.TYPE_ALL);
    }
    tv.addLifeCycleListener(this);
    tv.setMainPanelCreator(this);
    tv.addSelectionListener(this, false);
    tv.addMenuFillListener(this);
    tv.addRefreshListener(this, false);
    if (tv.canHaveSubItems()) {
    	tv.addRefreshListener(this);
    }
    
    tv.addTableDataSourceChangedListener(new TableDataSourceChangedListener() {
			public void tableDataSourceChanged(Object newDataSource) {
				if (newDataSource instanceof Category) {
		    	neverShowCatButtons = true;
					activateCategory((Category) newDataSource);
				}
			}
		}, true);

    forceHeaderVisible = COConfigurationManager.getBooleanParameter("MyTorrentsView.alwaysShowHeader");
		if (txtFilter != null) {
			filterParent = txtFilter.getParent();
			if (Constants.isWindows) {
				// dirty hack because window's filter box is within a bubble of it's own
				filterParent = filterParent.getParent();
			}
			Menu menuFilterHeader = new Menu(filterParent);
			final MenuItem menuItemAlwaysShow = new MenuItem(menuFilterHeader,
					SWT.CHECK);
			Messages.setLanguageText(menuItemAlwaysShow,
					"ConfigView.label.alwaysShowLibraryHeader");
			menuFilterHeader.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent e) {
					menuItemAlwaysShow.setSelection(forceHeaderVisible);
				}

				public void menuHidden(MenuEvent e) {
				}
			});
			menuItemAlwaysShow.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					COConfigurationManager.setParameter(
							"MyTorrentsView.alwaysShowHeader", !forceHeaderVisible);
				}

				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
			filterParent.setMenu(menuFilterHeader);
			Control[] children = filterParent.getChildren();
			for (Control control : children) {
				if (control != txtFilter) {
					control.setMenu(menuFilterHeader);
				}
			}
		}

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

    if (txtFilter == null) {
    	tv.enableFilterCheck(null, this);
    }

    createDragDrop();

    Utils.getOffOfSWTThread(new AERunnable() {
			
			public void runSupport() {
		    COConfigurationManager.addAndFireParameterListeners(new String[] {
					"DND Always In Incomplete",
					"MyTorrentsView.alwaysShowHeader",
					"User Mode"
				}, MyTorrentsView.this);

		    if (currentCategory != null) {
		    	currentCategory.addCategoryListener(MyTorrentsView.this);
		    }
		    CategoryManager.addCategoryManagerListener(MyTorrentsView.this);
		    globalManager.addListener(MyTorrentsView.this, false);
		    DownloadManager[] dms = globalManager.getDownloadManagers().toArray(new DownloadManager[0]);
		    for (int i = 0; i < dms.length; i++) {
					DownloadManager dm = dms[i];
					dm.addListener(MyTorrentsView.this);
					if (!isOurDownloadManager(dm)) {
						dms[i] = null;
					}
				}
		    tv.addDataSources(dms);
		    tv.processDataSourceQueue();
			}
		});
    
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
    COConfigurationManager.removeParameterListener("User Mode", this);
  }
  
  
  // @see org.gudy.azureus2.ui.swt.views.table.TableViewSWTPanelCreator#createTableViewPanel(org.eclipse.swt.widgets.Composite)
  public Composite createTableViewPanel(Composite composite) {
  	composite.addListener(SWT.Activate, new Listener() {
			public void handleEvent(Event event) {
				viewActive = true;
		    updateSelectedContent();
		    //refreshIconBar();
			}
		});
  	composite.addListener(SWT.Deactivate, new Listener() {
			public void handleEvent(Event event) {
				viewActive = false;
				// don't updateSelectedContent() because we may have switched
				// to a button or a text field, and we still want out content to be
				// selected
			}
		});
  	
    GridData gridData;
    cTableParentPanel = new Composite(composite, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.horizontalSpacing = 0;
    layout.verticalSpacing = 0;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    cTableParentPanel.setLayout(layout);
    if (composite.getLayout() instanceof GridLayout) {
    	cTableParentPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
    }
    
    cTablePanel = new Composite(cTableParentPanel, SWT.NULL);

    gridData = new GridData(GridData.FILL_BOTH);
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
  
  public void setForceHeaderVisible(boolean forceHeaderVisible) {
		this.forceHeaderVisible  = forceHeaderVisible;
		if (cTablePanel != null && !cTablePanel.isDisposed()) {
			createTabs();
		}
  }

  private void createTabs() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				swt_createTabs();
			}
		});
  }
  
  private void swt_createTabs() {
    Category[] categories = CategoryManager.getCategories();
    Arrays.sort(categories);
    boolean showCat = false;
    if (!neverShowCatButtons) {
      for(int i = 0; i < categories.length; i++) {
          if(categories[i].getType() == Category.TYPE_USER) {
              showCat = true;
              break;
          }
      }
    }

   	buildHeaderArea();
  	if (cCategories != null && !cCategories.isDisposed()) {
      Control[] controls = cCategories.getChildren();
      for (int i = 0; i < controls.length; i++) {
        controls[i].dispose();
      }
  	}
    
    if (showCat) {
    	buildCat(categories);
    } else if (cTableParentPanel != null && !cTableParentPanel.isDisposed()) {
  		cTableParentPanel.layout();
  	}
  }
  
	private void buildHeaderArea() {
		if (cCategories == null) {
			cCategories = new CompositeMinSize(cTableParentPanel, SWT.NONE);
			((CompositeMinSize) cCategories).setMinSize(new Point(SWT.DEFAULT, 24));
			GridData gridData = new GridData(SWT.RIGHT, SWT.TOP, true, false);
			cCategories.setLayoutData(gridData);
			cCategories.moveAbove(null);
		}else if ( cCategories.isDisposed()){
			return;
		}
		
		if (!(cCategories.getLayout() instanceof RowLayout)) {
      RowLayout rowLayout = new RowLayout();
      rowLayout.marginTop = 0;
      rowLayout.marginBottom = 0;
      rowLayout.marginLeft = 3;
      rowLayout.marginRight = 0;
      rowLayout.spacing = 0;
      rowLayout.wrap = true;
      cCategories.setLayout(rowLayout);
		}

    tv.enableFilterCheck(txtFilter, this);
	}

  /**
	 * 
	 *
	 * @param categories 
   * @since 3.1.1.1
	 */
	private void buildCat(Category[] categories) {
		int iFontPixelsHeight = 10;
		int iFontPointHeight = (iFontPixelsHeight * 72)
				/ cCategories.getDisplay().getDPI().y;
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
			if (catButton.computeSize(100, SWT.DEFAULT).y > 0) {
				RowData rd = new RowData();
				rd.height = catButton.computeSize(100, SWT.DEFAULT).y - 2
						+ catButton.getBorderWidth() * 2;
				catButton.setLayoutData(rd);
			}

			final String name = category.getName();
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
					Button curButton = (Button) e.widget;
					boolean isEnabled = curButton.getSelection();
					Control[] controls = cCategories.getChildren();
					if (!isEnabled) {
						for (int i = 0; i < controls.length; i++) {
							if (controls[i] instanceof Button) {
								curButton = (Button) controls[i];
								break;
							}
						}
					}

					for (int i = 0; i < controls.length; i++) {
						if (!(controls[i] instanceof Button)) {
							continue;
						}
						Button b = (Button) controls[i];
						if (b != curButton && b.getSelection())
							b.setSelection(false);
						else if (b == curButton && !b.getSelection())
							b.setSelection(true);
					}
					activateCategory((Category) curButton.getData("Category"));
				}
			});

			catButton.addListener(SWT.MouseHover, new Listener() {
				public void handleEvent(Event event) {
					Button curButton = (Button) event.widget;
					Category curCategory = (Category) curButton.getData("Category");
					List<DownloadManager> dms = curCategory.getDownloadManagers(globalManager.getDownloadManagers());

					long ttlActive = 0;
					long ttlSize = 0;
					long ttlRSpeed = 0;
					long ttlSSpeed = 0;
					int count = 0;
					for (DownloadManager dm : dms) {

						if (!isInCategory(dm, currentCategory))
							continue;

						count++;
						if (dm.getState() == DownloadManager.STATE_DOWNLOADING
								|| dm.getState() == DownloadManager.STATE_SEEDING)
							ttlActive++;
						ttlSize += dm.getSize();
						ttlRSpeed += dm.getStats().getDataReceiveRate();
						ttlSSpeed += dm.getStats().getDataSendRate();
					}

					String up_details = "";
					String down_details = "";

					if (category.getType() != Category.TYPE_ALL) {

						String up_str = MessageText.getString("GeneralView.label.maxuploadspeed");
						String down_str = MessageText.getString("GeneralView.label.maxdownloadspeed");
						String unlimited_str = MessageText.getString("MyTorrentsView.menu.setSpeed.unlimited");

						int up_speed = category.getUploadSpeed();
						int down_speed = category.getDownloadSpeed();

						up_details = up_str
								+ ": "
								+ (up_speed == 0 ? unlimited_str
										: DisplayFormatters.formatByteCountToKiBEtc(up_speed));
						down_details = down_str
								+ ": "
								+ (down_speed == 0 ? unlimited_str
										: DisplayFormatters.formatByteCountToKiBEtc(down_speed));
					}

					if (count == 0) {
						curButton.setToolTipText(down_details + "\n" + up_details
								+ "\nTotal: 0");
						return;
					}

					curButton.setToolTipText((up_details.length() == 0 ? ""
							: (down_details + "\n" + up_details + "\n"))
							+ "Total: "
							+ count
							+ "\n"
							+ "Downloading/Seeding: "
							+ ttlActive
							+ "\n"
							+ "\n"
							+ "Speed: "
							+ DisplayFormatters.formatByteCountToKiBEtcPerSec(ttlRSpeed
									/ count)
							+ "/"
							+ DisplayFormatters.formatByteCountToKiBEtcPerSec(ttlSSpeed
									/ count)
							+ "\n"
							+ "Size: "
							+ DisplayFormatters.formatByteCountToKiBEtc(ttlSize));
				}
			});

			final DropTarget tabDropTarget = new DropTarget(catButton,
					DND.DROP_DEFAULT | DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);
			Transfer[] types = new Transfer[] {
				TextTransfer.getInstance()
			};
			tabDropTarget.setTransfer(types);
			tabDropTarget.addDropListener(new DropTargetAdapter() {
				public void dragOver(DropTargetEvent e) {
					//System.out.println("dragging over: " + drag_drop_line_start);
					if (drag_drop_line_start >= 0) {
						e.detail = DND.DROP_MOVE;
					} else {
						e.detail = DND.DROP_NONE;
					}
				}

				public void drop(DropTargetEvent e) {
					e.detail = DND.DROP_NONE;
					//System.out.println("DragDrop on Button:" + drag_drop_line_start);
					if (drag_drop_line_start >= 0) {
						drag_drop_line_start = -1;
						drag_drop_rows = null;

						TorrentUtil.assignToCategory(tv.getSelectedDataSources().toArray(),
								(Category) catButton.getData("Category"));
					}
				}
			});

			catButton.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					if (!tabDropTarget.isDisposed()) {
						tabDropTarget.dispose();
					}
				}
			});

			final Menu menu = new Menu(catButton.getShell(), SWT.POP_UP);

			catButton.setMenu(menu);

			CategoryUIUtils.setupCategoryMenu(menu, category);
		}
		
		cCategories.getParent().layout(true, true);
	}

	public boolean isOurDownloadManager(DownloadManager dm) {
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

		return bOurs;
	}

	public boolean filterCheck(DownloadManager dm, String sLastSearch, boolean bRegexSearch) {
		boolean bOurs = true;

		if (sLastSearch.length() > 0) {
			try {
				String[][] names = {
					{
						"",
						dm.getDisplayName()
					},
					{
						"t:",
						"", // defer this as costly dm.getTorrent().getAnnounceURL().getHost()
					},
					{
						"st:",
						"" + dm.getState()
					},
					{
						"c:",
						"" + dm.getDownloadState().getUserComment()
					}
				};

				String name = names[0][1];
				String tmpSearch = sLastSearch;

				for (int i = 1; i < names.length; i++) {
					if (tmpSearch.startsWith(names[i][0])) {
						tmpSearch = tmpSearch.substring(names[i][0].length());
						
						if ( i == 1 ){
							TOTorrent t = dm.getTorrent();
							
							StringBuffer name_b = new StringBuffer( 512);
							
							name_b.append( t.getAnnounceURL().getHost());
							
							TOTorrentAnnounceURLSet[] sets = t.getAnnounceURLGroup().getAnnounceURLSets();
							
							for ( TOTorrentAnnounceURLSet set: sets ){
								
								URL[] urls = set.getAnnounceURLs();
								
								for ( URL u: urls ){
									
									name_b.append( ";" );
									name_b.append( u.getHost());
								}
							}
							
							name = name_b.toString();
						}else{
							name = names[i][1];
						}
					}
				}

				String s = bRegexSearch ? tmpSearch : "\\Q"
						+ tmpSearch.replaceAll("[|;]", "\\\\E|\\\\Q") + "\\E";
				Pattern pattern = RegExUtil.getCachedPattern( "tv:search", s, Pattern.CASE_INSENSITIVE);

				if (!pattern.matcher(name).find())
					bOurs = false;
			} catch (Exception e) {
				// Future: report PatternSyntaxException message to user.
			}
		}
		return bOurs;
	}
	
	// @see org.gudy.azureus2.ui.swt.views.table.TableViewFilterCheck#filterSet(java.lang.String)
	public void filterSet(final String filter) {
		Utils.execSWTThread(new AERunnable() {
						
			public void runSupport() {
				if (txtFilter != null) {
					boolean visible = forceHeaderVisible || filter.length() > 0;
					Object layoutData = filterParent.getLayoutData();
					if (layoutData instanceof FormData) {
						FormData fd = (FormData) layoutData;
						boolean wasVisible = fd.height != 0;
						if (visible != wasVisible) {
  						fd.height = visible ? SWT.DEFAULT : 0;
  						filterParent.setLayoutData(layoutData);
  						filterParent.getParent().layout();
						}
					}
					if (!visible) {
						tv.setFocus();
					}
					
					Object x = filterParent.getData( "ViewUtils:ViewTitleExtraInfo" );
					
					if ( x instanceof ViewUtils.ViewTitleExtraInfo ){
						
						boolean	enabled = filter.length() > 0;
						
						if ( enabled ){
							
							if ( txtFilterUpdateEvent == null ){
								
								txtFilterUpdateEvent = 
									SimpleTimer.addPeriodicEvent(
										"MTV:updater",
										1000,
										new TimerEventPerformer()
										{
											public void 
											perform(
												TimerEvent event )
											{
												Utils.execSWTThread(
													new AERunnable() 
													{
														public void
														runSupport()
														{
															if ( txtFilterUpdateEvent != null ){
																
																if ( tv.isDisposed()){
																	
																	txtFilterUpdateEvent.cancel();
																	
																	txtFilterUpdateEvent = null;
																	
																}else{
																	
																	viewChanged( tv );
																}
															}
														}
													});
											}
										});
							}
						}else{
							
							if ( txtFilterUpdateEvent != null ){
								
								txtFilterUpdateEvent.cancel();
								
								txtFilterUpdateEvent = null;
							}
						}
						
						((ViewUtils.ViewTitleExtraInfo)x).setEnabled( tv.getComposite(), isSeedingView, enabled );
					}
				}
			}
		});
	}
	
	public void 
	viewChanged(
		TableView<DownloadManager> view ) 
	{
		if ( filterParent != null ){
			Object x = filterParent.getData( "ViewUtils:ViewTitleExtraInfo" );
		
			if ( x instanceof ViewUtils.ViewTitleExtraInfo ){
				
				TableRowCore[] rows = view.getRows();
				
				int	active = 0;
				
				for ( TableRowCore row: rows ){
					
					DownloadManager dm = (DownloadManager)row.getDataSource( true );
					
					int	state = dm.getState();
					
					if ( state == DownloadManager.STATE_DOWNLOADING || state == DownloadManager.STATE_SEEDING ){
						
						active++;
					}
				}
				
				((ViewUtils.ViewTitleExtraInfo)x).update( tv.getComposite(), isSeedingView, rows.length, active );
			}
		}
	}

  // @see com.aelitis.azureus.ui.common.table.TableSelectionListener#selected(com.aelitis.azureus.ui.common.table.TableRowCore[])
  public void selected(TableRowCore[] rows) {
  	updateSelectedContent();
  	refreshTorrentMenu();
  }

	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#deselected(com.aelitis.azureus.ui.common.table.TableRowCore[])
	public void deselected(TableRowCore[] rows) {
  	updateSelectedContent();
  	refreshTorrentMenu();
	}

	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#focusChanged(com.aelitis.azureus.ui.common.table.TableRowCore)
	public void focusChanged(TableRowCore focus) {
		updateSelectedContent();
  	refreshTorrentMenu();
	}

	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#mouseEnter(com.aelitis.azureus.ui.common.table.TableRowCore)
	public void mouseEnter(TableRowCore row) {
	}

	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#mouseExit(com.aelitis.azureus.ui.common.table.TableRowCore)
	public void mouseExit(TableRowCore row) {
	}

	public void updateSelectedContent() {
		updateSelectedContent( false );
	}
	
	public void updateSelectedContent( boolean force ) {
		if (cTablePanel == null || cTablePanel.isDisposed()) {
			return;
		}
			// if we're not active then ignore this update as we don't want invisible components
			// updating the toolbar with their invisible selection. Note that unfortunately the 
			// call we get here when activating a view does't yet have focus
		
		if ( !isTableFocus()){
			if ( !force ){
				return;
			}
		}
		Object[] dataSources = tv.getSelectedDataSources(true);
		List<SelectedContent> listSelected = new ArrayList<SelectedContent>(dataSources.length);
		for (Object ds : dataSources) {
			if (ds instanceof DownloadManager) {
				listSelected.add(new SelectedContent((DownloadManager) ds));
			} else if (ds instanceof DiskManagerFileInfo) {
				DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) ds;
				listSelected.add(new SelectedContent(fileInfo.getDownloadManager(), fileInfo.getIndex()));
			}
		}
		SelectedContent[] content = listSelected.toArray(new SelectedContent[0]);
		SelectedContentManager.changeCurrentlySelectedContent(tv.getTableID(), content, tv);
	}
	
	private void refreshTorrentMenu() {
		UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
		if (uiFunctions != null && uiFunctions instanceof UIFunctionsSWT) {
			((UIFunctionsSWT)uiFunctions).refreshTorrentMenu();
		}
	}
	
	public DownloadManager[] getSelectedDownloads() {
		Object[] data_sources = tv.getSelectedDataSources().toArray();
		List<DownloadManager> list = new ArrayList<DownloadManager>();
		for (Object ds : data_sources) {
			if (ds instanceof DownloadManager) {
				list.add((DownloadManager) ds);
			}
		}
		return list.toArray(new DownloadManager[0]);
	}

  // @see com.aelitis.azureus.ui.common.table.TableSelectionListener#defaultSelected(com.aelitis.azureus.ui.common.table.TableRowCore[])
  public void defaultSelected(TableRowCore[] rows, int keyMask) {
  	if (defaultSelectedListener != null) {
  		defaultSelectedListener.defaultSelected(rows, keyMask);
  		return;
  	}
  	showSelectedDetails();
	}
  
  private void showSelectedDetails() {
		Object[] dm_sources = tv.getSelectedDataSources().toArray();
		UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
		for (int i = 0; i < dm_sources.length; i++) {
			if (!(dm_sources[i] instanceof DownloadManager)) {
				continue;
			}
			if (uiFunctions != null) {
				uiFunctions.openView(UIFunctions.VIEW_DM_DETAILS, dm_sources[i]);
			}
		}  	
  }
  
  public void overrideDefaultSelected(TableSelectionListener defaultSelectedListener) {
		this.defaultSelectedListener = defaultSelectedListener;
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

    }
  }

	// @see org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener#fillMenu(java.lang.String, org.eclipse.swt.widgets.Menu)
	public void fillMenu(String sColumnName, final Menu menu) {
		Object[] dataSources = tv.getSelectedDataSources(true);
		DownloadManager[] dms = getSelectedDownloads();
		
		if (dms.length == 0 && dataSources.length > 0) {
  		List<DiskManagerFileInfo> listFileInfos = new ArrayList<DiskManagerFileInfo>();
  		DownloadManager firstFileDM = null;
  		for (Object ds : dataSources) {
  			if (ds instanceof DiskManagerFileInfo) {
  				DiskManagerFileInfo info = (DiskManagerFileInfo) ds;
  				// for now, FilesViewMenuUtil.fillmenu can only handle one DM
  				if (firstFileDM != null && !firstFileDM.equals(info.getDownloadManager())) {
  					break;
  				}
  				firstFileDM = info.getDownloadManager();
  				listFileInfos.add(info);
  			}
  		}
  		if (listFileInfos.size() > 0) {
  			FilesViewMenuUtil.fillMenu(tv, menu, firstFileDM,
  					listFileInfos.toArray(new DiskManagerFileInfo[0]));
  			return;
  		}
		}
		
		boolean hasSelection = (dms.length > 0);

		if (hasSelection) {
			TorrentUtil.fillTorrentMenu(menu, dms, azureus_core, cTablePanel, true,
					(isSeedingView) ? 2 : 1, tv);

			// ---
			new MenuItem(menu, SWT.SEPARATOR);
		}
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

			dragSource = tv.createDragSource(DND.DROP_MOVE | DND.DROP_COPY);
			if (dragSource != null) {
				dragSource.setTransfer(types);
				dragSource.addDragListener(new DragSourceAdapter() {
					private String eventData;

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

						// Build eventData here because on OSX, selection gets cleared
						// by the time dragSetData occurs
						boolean onlyDMs = true;
						StringBuffer sb = new StringBuffer();
						Object[] selectedDataSources = tv.getSelectedDataSources(true);
						for (Object ds : selectedDataSources) {
							if (ds instanceof DownloadManager) {
								DownloadManager dm = (DownloadManager) ds;
								TOTorrent torrent = dm.getTorrent();
								if (torrent != null) {
									try {
										sb.append(torrent.getHashWrapper().toBase32String());
										sb.append('\n');
									} catch (TOTorrentException e) {
									}
								}
							} else if (ds instanceof DiskManagerFileInfo) {
								DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) ds;
								DownloadManager dm = fileInfo.getDownloadManager();
								TOTorrent torrent = dm.getTorrent();
								if (torrent != null) {
									try {
										sb.append(torrent.getHashWrapper().toBase32String());
										sb.append(';');
										sb.append(fileInfo.getIndex());
										sb.append('\n');
										onlyDMs = false;
									} catch (TOTorrentException e) {
									}
								}
							}
						}
						
						eventData = (onlyDMs ? "DownloadManager\n" : "DiskManagerFileInfo\n") + sb.toString();
					}

					public void dragSetData(DragSourceEvent event) {
						// System.out.println("DragSetData");
						event.data = eventData;
					}
				});
			}

			dropTarget = tv.createDropTarget(DND.DROP_DEFAULT | DND.DROP_MOVE
					| DND.DROP_COPY | DND.DROP_LINK | DND.DROP_TARGET_MOVE);
			if (dropTarget != null) {
				dropTarget.setTransfer(new Transfer[] { HTMLTransfer.getInstance(),
						URLTransfer.getInstance(), FileTransfer.getInstance(),
						TextTransfer.getInstance() });

				dropTarget.addDropListener(new DropTargetAdapter() {
					Point enterPoint = null;
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
							event.feedback = DND.FEEDBACK_SCROLL;
							enterPoint = new Point(event.x, event.y);
						}
					}

					public void dragOver(DropTargetEvent event) {
						if (drag_drop_line_start >= 0) {
							if (drag_drop_rows.length > 0
									&& !(drag_drop_rows[0].getDataSource(true) instanceof DownloadManager)) {
								event.detail = DND.DROP_NONE;
								return;
							}
							event.detail = event.item == null ? DND.DROP_NONE : DND.DROP_MOVE;
							event.feedback = DND.FEEDBACK_SCROLL
									| ((enterPoint != null && enterPoint.y > event.y)
											? DND.FEEDBACK_INSERT_BEFORE : DND.FEEDBACK_INSERT_AFTER);
						}
					}

					public void drop(DropTargetEvent event) {
						if (!(event.data instanceof String)) {
							TorrentOpener.openDroppedTorrents(event, true);
							return;
						}
						String data = (String) event.data;
						if (data.startsWith("DiskManagerFileInfo\n")) {
							return;
						}
						if (!data.startsWith("DownloadManager\n")) {
							TorrentOpener.openDroppedTorrents(event, true);
							return;
						}

						event.detail = DND.DROP_NONE;
						// Torrent file from shell dropped
						if (drag_drop_line_start >= 0) { // event.data == null
							event.detail = DND.DROP_NONE;
							TableRowCore row = tv.getRow(event);
							if (row == null)
								return;
							if (row.getParentRowCore() != null) {
								row = row.getParentRowCore();
							}
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
      Object ds = row.getDataSource(true);
      if (!(ds instanceof DownloadManager)) {
      	continue;
      }
      DownloadManager dm = (DownloadManager) ds;
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

    boolean bForceSort = sortColumn == null ? false : sortColumn.getName().equals("#");
    tv.columnInvalidate("#");
    tv.refreshTable(bForceSort);
  }

  // @see com.aelitis.azureus.ui.common.table.TableRefreshListener#tableRefresh()
  public void tableRefresh() {
    if (tv.isDisposed())
      return;
    
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
				case 'i': // CTRL+I Info/Details
					showSelectedDetails();
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
					TorrentUtil.resumeTorrents(tv.getSelectedDataSources().toArray());
					e.doit = false;
					break;
				case 's': // CTRL+S stop selected Torrents
					Utils.getOffOfSWTThread(new AERunnable() {
						public void runSupport() {
							TorrentUtil.stopDataSources(tv.getSelectedDataSources().toArray());
						}
					});
					e.doit = false;
					break;
			}

			if (!e.doit)
				return;
		}
		
		// DEL remove selected Torrents
		if (e.stateMask == 0 && e.keyCode == SWT.DEL && e.widget != txtFilter) {
			Utils.getOffOfSWTThread(new AERunnable() {
				public void runSupport() {
					TorrentUtil.removeDataSources(tv.getSelectedDataSources().toArray());
				}
			});
			e.doit = false;
			return;
		}

		if (e.keyCode != SWT.BS) {
			if ((e.stateMask & (~SWT.SHIFT)) != 0 || e.character < 32)
				return;
		}
	}

	public void keyReleased(KeyEvent e) {
		// ignore
	}





  private void moveSelectedTorrentsDown() {
    // Don't use runForSelectDataSources to ensure the order we want
  	DownloadManager[] dms = getSelectedDownloads();
    Arrays.sort(dms, new Comparator<DownloadManager>() {
			public int compare(DownloadManager a, DownloadManager b) {
        return a.getPosition() - b.getPosition();
			}
    });
    for (int i = dms.length - 1; i >= 0; i--) {
      DownloadManager dm = dms[i];
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
  	DownloadManager[] dms = getSelectedDownloads();
    Arrays.sort(dms, new Comparator<DownloadManager>() {
    	public int compare(DownloadManager a, DownloadManager b) {
        return a.getPosition() - b.getPosition();
      }
    });
    for (int i = 0; i < dms.length; i++) {
      DownloadManager dm = dms[i];
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
  	DownloadManager[] dms = getSelectedDownloads();
		if (dms.length <= 0)
			return;

		int[] newPositions = new int[dms.length];

		if (by < 0) {
			Arrays.sort(dms, new Comparator<DownloadManager>() {
				public int compare(DownloadManager a, DownloadManager b) {
					return a.getPosition() - b.getPosition();
				}
			});
		} else {
			Arrays.sort(dms, new Comparator<DownloadManager>() {
				public int compare(DownloadManager a, DownloadManager b) {
					return b.getPosition() - a.getPosition();
				}
			});
		}

		int count = globalManager.downloadManagerCount(isSeedingView); 
		for (int i = 0; i < dms.length; i++) {
			DownloadManager dm = dms[i];
			int pos = dm.getPosition() + by;
			if (pos < i + 1)
				pos = i + 1;
			else if (pos > count - i)
				pos = count - i;

			newPositions[i] = pos;
		}

		for (int i = 0; i < dms.length; i++) {
			DownloadManager dm = dms[i];
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
  	DownloadManager[] dms = getSelectedDownloads();
    if (dms.length == 0)
      return;

    if(moveToTop)
      globalManager.moveTop(dms);
    else
      globalManager.moveEnd(dms);

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
		if (parameterName == null
				|| parameterName.equals("DND Always In Incomplete")) {
			bDNDalwaysIncomplete = COConfigurationManager.getBooleanParameter("DND Always In Incomplete");
		}
		if (parameterName == null || parameterName.equals("MyTorrentsView.alwaysShowHeader")) {
			setForceHeaderVisible(COConfigurationManager.getBooleanParameter("MyTorrentsView.alwaysShowHeader"));
		}
	}


  public void refreshToolBarItems(Map<String, Long> list) {
		Map<String, Long> states = TorrentUtil.calculateToolbarStates(
				SelectedContentManager.getCurrentlySelectedContent(), tv.getTableID());
		list.putAll(states);
  }  

  public boolean toolBarItemActivated(ToolBarItem item, long activationType, Object datasource) {
  	String itemKey = item.getID();
  	if (activationType == UIToolBarActivationListener.ACTIVATIONTYPE_HELD) {
      if(itemKey.equals("up")) {
        moveSelectedTorrentsTop();
        return true;
      }
      if(itemKey.equals("down")){
        moveSelectedTorrentsEnd();
        return true;
      }
      return false;
  	}

  	if (activationType != UIToolBarActivationListener.ACTIVATIONTYPE_NORMAL) {
  		return false;
  	}
    if(itemKey.equals("top")) {
      moveSelectedTorrentsTop();
      return true;
    }
    if(itemKey.equals("bottom")){
      moveSelectedTorrentsEnd();
      return true;
    }
    if(itemKey.equals("up")) {
      moveSelectedTorrentsUp();
      return true;
    }
    if(itemKey.equals("down")){
      moveSelectedTorrentsDown();
      return true;
    }
    if(itemKey.equals("run")){
      TorrentUtil.runDataSources(tv.getSelectedDataSources().toArray());
      return true;
    }
    if(itemKey.equals("start")){
      TorrentUtil.queueDataSources(tv.getSelectedDataSources().toArray(), true);
      return true;
    }
    if(itemKey.equals("stop")){
      TorrentUtil.stopDataSources(tv.getSelectedDataSources().toArray());
      return true;
    }
    if (itemKey.equals("startstop")) {
    	TorrentUtil.stopOrStartDataSources(tv.getSelectedDataSources().toArray());
    	return true;
    }
    if(itemKey.equals("remove")){
      TorrentUtil.removeDataSources(tv.getSelectedDataSources().toArray());
      return true;
    }
    return false;
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
    	Utils.getOffOfSWTThread(new AERunnable() {
				public void runSupport() {
		    	row.refresh(true);
		    	if (row.isSelected()) {
		    		updateSelectedContent();
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
					updateSelectedContent();
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
		
		tv.processDataSourceQueue();
		Object[] managers = globalManager.getDownloadManagers().toArray();
		List<DownloadManager> listRemoves = new ArrayList<DownloadManager>();
		List<DownloadManager> listAdds = new ArrayList<DownloadManager>();
		
		for (int i = 0; i < managers.length; i++) {
			DownloadManager dm = (DownloadManager) managers[i];
		
			boolean bHave = tv.isUnfilteredDataSourceAdded(dm);
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
		tv.removeDataSources(listRemoves.toArray(new DownloadManager[0]));
		tv.addDataSources(listAdds.toArray(new DownloadManager[0]));
		
  	tv.processDataSourceQueue();
		//tv.refreshTable(false);
	}
  
  public boolean isInCurrentCategory(DownloadManager manager) {
  	return isInCategory(manager, currentCategory);
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
		createTabs();
	}

	public void categoryRemoved(Category category) {
		if (currentCategory == category) {
			activateCategory(CategoryManager.getCategory(Category.TYPE_ALL));
		} else {
			// always activate as deletion of this one might have
			// affected the current view 
			activateCategory(currentCategory);
		}

		createTabs();
	}
  
  public void categoryChanged(Category category) {	
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
  public void seedingStatusChanged( boolean seeding_only_mode, boolean b ){}       

  // End of globalmanagerlistener Functions
  



	public void updateLanguage() {
		super.updateLanguage();
		getComposite().layout(true, true);
	}

	public boolean isTableFocus() {
		return viewActive;
		//return tv.isTableFocus();
	}
	
	public Image obfusticatedImage(final Image image) {
		return tv.obfusticatedImage(image);
	}
	
	/**
	 * Creates and return an <code>TableViewSWT</code>
	 * Subclasses my override to return a different TableViewSWT if needed
	 * @param basicItems
	 * @return
	 */
	protected TableViewSWT<DownloadManager> createTableView(
			Class<?> forDataSourceType, String tableID, TableColumnCore[] basicItems) {
		int tableExtraStyle = COConfigurationManager.getIntParameter("MyTorrentsView.table.style");
		return new TableViewSWTImpl<DownloadManager>(forDataSourceType, tableID,
				getPropertiesPrefix(), basicItems, "#", tableExtraStyle | SWT.MULTI
						| SWT.FULL_SELECTION | SWT.VIRTUAL | SWT.CASCADE) {
			public void setSelectedRows(TableRowCore[] rows) {
				super.setSelectedRows(rows);
				updateSelectedContent();
			}
		};
	}

	/**
	 * Returns the default row height for the table
	 * Subclasses my override to return a different height if needed; a height of -1 means use default
	 * @return
	 */
	protected int getRowDefaultHeight(){
		return -1;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableRowRefreshListener#rowRefresh(org.gudy.azureus2.plugins.ui.tables.TableRow)
	public void rowRefresh(TableRow row) {
		if (!(row instanceof TableRowCore)) {
			return;
		}

		TableRowCore rowCore = (TableRowCore) row;
		Object ds = rowCore.getDataSource(true);
		if (!(ds instanceof DownloadManager)) {
			return;
		}

		DownloadManager dm = (DownloadManager) ds;
		if (rowCore.getSubItemCount() == 0 && dm.getTorrent() != null
				&& !dm.getTorrent().isSimpleTorrent() && rowCore.isVisible()
				&& dm.getNumFileInfos() > 0) {
			DiskManagerFileInfoSet fileInfos = dm.getDiskManagerFileInfoSet();
			if (fileInfos != null) {
				DiskManagerFileInfo[] files = fileInfos.getFiles();
				boolean copied = false;
				int pos = 0;
				for (int i = 0; i < files.length; i++) {
					DiskManagerFileInfo fileInfo = files[i];
					if (fileInfo.isSkipped()
							&& (fileInfo.getStorageType() == DiskManagerFileInfo.ST_COMPACT || fileInfo.getStorageType() == DiskManagerFileInfo.ST_REORDER_COMPACT)) {
						continue;
					}
					if (pos != i) {
						if ( !copied ){
								// we *MUSTN'T* modify the returned array!!!!
							
							DiskManagerFileInfo[] oldFiles = files;
							files = new DiskManagerFileInfo[files.length];
							System.arraycopy(oldFiles, 0, files, 0, files.length);
							
							copied = true;
						}
						
						files[pos] = files[i];
					}
					pos++;
				}
				if (pos != files.length) {
					DiskManagerFileInfo[] oldFiles = files;
					files = new DiskManagerFileInfo[pos];
					System.arraycopy(oldFiles, 0, files, 0, pos);
				}
				rowCore.setSubItems(files);
			}
		}
	}

	public boolean eventOccurred(UISWTViewEvent event) {
		boolean b = super.eventOccurred(event);
		if (event.getType() == UISWTViewEvent.TYPE_FOCUSGAINED) {
			if (rebuildListOnFocusGain) {
  			List<?> dms = globalManager.getDownloadManagers();
  			for (Iterator<?> iter = dms.iterator(); iter.hasNext();) {
  				DownloadManager dm = (DownloadManager) iter.next();
  
  				if (!isOurDownloadManager(dm)) {
  					tv.removeDataSource(dm);
  				} else {
  					tv.addDataSource(dm);
  				}
  			}
			}
	    updateSelectedContent();
		} else if (event.getType() == UISWTViewEvent.TYPE_FOCUSLOST) {
		}
		return b;
	}

	public void setRebuildListOnFocusGain(boolean rebuildListOnFocusGain) {
		this.rebuildListOnFocusGain = rebuildListOnFocusGain;
	}
}
