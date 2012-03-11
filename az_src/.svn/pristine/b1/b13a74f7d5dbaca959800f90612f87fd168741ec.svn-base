/*
 * Created on 2004/Apr/18
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
package org.gudy.azureus2.ui.swt.views.table.impl;

import java.lang.reflect.Method;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.config.impl.ConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.internat.MessageText.MessageTextListener;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.pluginsimpl.local.ui.tables.TableContextMenuItemImpl;
import org.gudy.azureus2.ui.common.util.MenuItemManager;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.debug.ObfusticateImage;
import org.gudy.azureus2.ui.swt.debug.UIDebugGenerator;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.*;
import org.gudy.azureus2.ui.swt.views.columnsetup.TableColumnSetupWindow;
import org.gudy.azureus2.ui.swt.views.table.*;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnSWTUtils;
import org.gudy.azureus2.ui.swt.views.table.utils.TableContextMenuManager;

import com.aelitis.azureus.ui.common.table.*;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;
import com.aelitis.azureus.ui.common.table.impl.TableViewImpl;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

/** 
 * An IView with a sortable table.  Handles composite/menu/table creation
 * and management.
 *
 * @author Olivier (Original PeersView/MyTorrentsView/etc code)
 * @author TuxPaper
 *         2004/Apr/20: Remove need for tableItemToObject
 *         2005/Oct/07: Virtual Table
 *         2005/Nov/16: Moved TableSorter into TableView
 *         
 * @note From TableSorter.java:<br>
 *   <li>2004/Apr/20: Remove need for tableItemToObject (store object in tableItem.setData)
 *   <li>2004/May/11: Use Comparable instead of SortableItem
 *   <li>2004/May/14: moved from org.gudy.azureus2.ui.swt.utils
 *   <li>2005/Oct/10: v2307 : Sort SWT.VIRTUAL Tables, Column Indicator
 *   
 * @future TableView should be split into two.  One for non SWT functions, and
 *          the other extending the first, with extra SWT stuff. 
 *
 * @future dataSourcesToRemove should be removed after a certain amount of time
 *          has passed.  Currently, dataSourcesToRemove is processed every
 *          refresh IF the table is visible, or it is processed when we collect
 *          20 items to remove.
 *          
 * @note 4005: We set a text cell's measured width to the columns prefered width
 *             instead of setting it to the actual space needed for the text.
 *             We should really store the last measured width in TableCell and
 *             use that.
 */
public class TableViewSWTImpl<DATASOURCETYPE>
	extends TableViewImpl<DATASOURCETYPE>
	implements ParameterListener, TableViewSWT<DATASOURCETYPE>,
	TableStructureModificationListener<DATASOURCETYPE>, ObfusticateImage,
	KeyListener, MessageTextListener
{
	protected final static boolean DRAW_VERTICAL_LINES = Constants.isWindows;

	protected static final boolean DRAW_FULL_ROW = Constants.isWindows;

	private final static LogIDs LOGID = LogIDs.GUI;

	private static final boolean DEBUG_SORTER = false;

	// Shorter name for ConfigManager, easier to read code
	private static final ConfigurationManager configMan = ConfigurationManager.getInstance();

	private static final String CFG_SORTDIRECTION = "config.style.table.defaultSortOrder";

	private static final int ASYOUTYPE_MODE_FIND = 0;
	private static final int ASYOUTYPE_MODE_FILTER = 1;
	private static final int ASYOUTYPE_MODE = ASYOUTYPE_MODE_FILTER;
	private static final int ASYOUTYPE_UPDATEDELAY = 300;

	private static final Color COLOR_FILTER_REGEX	= Colors.fadedYellow;
	
	protected static final boolean DEBUG_CELL_CHANGES = false;

	private static final boolean DEBUG_SELECTION = false;

	private static final boolean DEBUG_ROWCHANGE = false;

	private static final boolean OBEY_COLUMN_MINWIDTH = false;

	/** TableID (from {@link org.gudy.azureus2.plugins.ui.tables.TableManager}) 
	 * of the table this class is
	 * handling.  Config settings are stored with the prefix of 
	 * "Table.<i>TableID</i>"
	 */
	protected String sTableID;

	/** Prefix for retrieving text from the properties file (MessageText)
	 * Typically <i>TableID</i> + "View"
	 */
	protected String sPropertiesPrefix;

	/** Column name to sort on if user hasn't chosen one yet 
	 */
	protected String sDefaultSortOn;

	/** 1st column gap problem (Eclipse Bug 43910).  Set to true when table is 
	 * using TableItem.setImage 
	 */
	protected boolean bSkipFirstColumn = true;

	private Point ptIconSize = null;

	/** Basic (pre-defined) Column Definitions */
	private TableColumnCore[] basicItems;

	/** All Column Definitions.  The array is not necessarily in column order */
	private TableColumnCore[] tableColumns;

	/** Composite for IView implementation */
	private Composite mainComposite;

	/** Composite that stores the table (sometimes the same as mainComposite) */
	private Composite tableComposite;

	/** Table for SortableTable implementation */
	private TableOrTreeSWT table;

	private ControlEditor editor;

	/** SWT style options for the creation of the Table */
	protected int iTableStyle;

	/** Context Menu */
	private Menu menu;

	/** Link DataSource to their row in the table.
	 * key = DataSource
	 * value = TableRowSWT
	 */
	private Map<DATASOURCETYPE, TableRowCore> mapDataSourceToRow;

	private AEMonitor listUnfilteredDatasources_mon = new AEMonitor("TableView:uds");

	private Set<DATASOURCETYPE> listUnfilteredDataSources;

	private AEMonitor dataSourceToRow_mon = new AEMonitor("TableView:OTSI");

	private List<TableRowSWT> sortedRows;

	private AEMonitor sortedRows_mon = new AEMonitor("TableView:sR");

	private AEMonitor sortColumn_mon = new AEMonitor("TableView:sC");

	/** Sorting functions */
	protected TableColumnCore sortColumn;

	/** TimeStamp of when last sorted all the rows was */
	private long lLastSortedOn;

	/** For updating GUI.  
	 * Some UI objects get updating every X cycles (user configurable) 
	 */
	protected int loopFactor;

	/** How often graphic cells get updated
	 */
	protected int graphicsUpdate = configMan.getIntParameter("Graphics Update");

	protected int reOrderDelay = configMan.getIntParameter("ReOrder Delay");

	/**
	 * Cache of selected table items to bypass insufficient drawing on Mac OS X
	 */
	//private ArrayList oldSelectedItems;
	/** We need to remember the order of the columns at the time we added them
	 * in case the user drags the columns around.
	 */
	private TableColumnCore[] columnsOrdered;

	private boolean[] columnsVisible;

	private ColumnMoveListener columnMoveListener = new ColumnMoveListener();

	/** Queue added datasources and add them on refresh */
	private LightHashSet dataSourcesToAdd = new LightHashSet(4);

	/** Queue removed datasources and add them on refresh */
	private LightHashSet dataSourcesToRemove = new LightHashSet(4);

	private boolean bReallyAddingDataSources = false;

	/** TabViews */
	public boolean bEnableTabViews = false;

	/** TabViews */
	private CTabFolder tabFolder;

	/** TabViews */
	private ArrayList<UISWTViewCore> tabViews = new ArrayList<UISWTViewCore>(1);

	TableRowSWT[] visibleRows;

	private long lCancelSelectionTriggeredOn = -1;

	private long lastSelectionTriggeredOn = -1;

	private List<TableViewSWTMenuFillListener> listenersMenuFill = new ArrayList<TableViewSWTMenuFillListener>(
			1);
	
	private ArrayList<TableRowSWTPaintListener> rowPaintListeners;

  private static AEMonitor mon_RowPaintListener = new AEMonitor( "rpl" );

	private ArrayList<TableRowMouseListener> rowMouseListeners;

  private static AEMonitor mon_RowMouseListener = new AEMonitor( "rml" );

	private TableViewSWTPanelCreator mainPanelCreator;

	private List<KeyListener> listenersKey = new ArrayList<KeyListener>(1);

	private boolean columnPaddingAdjusted = false;

	private boolean columnVisibilitiesChanged = true;

	// What type of data is stored in this table
	private final Class<?> classPluginDataSourceType;

	private AEMonitor listeners_mon = new AEMonitor("tablelisteners");

	private ArrayList<TableRowRefreshListener> refreshListeners;

	/**
	 * Up to date table client area.  So far, the best places to refresh
	 * this variable are in the PaintItem event and the scrollbar's events.
	 * Typically table.getClientArea() is time consuming 
	 */
	protected Rectangle clientArea;

	private boolean isVisible;

	private boolean menuEnabled = true;

	private boolean headerVisible = true;

	/**
	 * Up to date list of selected rows, so we can access rows without being on SWT Thread.
	 * Guaranteed to have no nulls
	 */
	private List<TableRowCore> selectedRows = new ArrayList<TableRowCore>(1);

	private List<Object> listSelectedCoreDataSources;

	private Utils.addDataSourceCallback processDataSourceQueueCallback = new Utils.addDataSourceCallback() {
		public void process() {
			processDataSourceQueue();
		}

		public void debug(String str) {
			TableViewSWTImpl.this.debug(str);
		}
	};

	// private Rectangle firstClientArea;

	private int lastHorizontalPos;
	

	// class used to keep filter stuff in a nice readable parcel
	class filter {
		Text widget = null;
		
		TimerEvent eventUpdate;
		
		String text = "";
		
		long lastFilterTime;
		
		boolean regex = false;
		
		TableViewFilterCheck<DATASOURCETYPE> checker;
		
		String nextText = "";
		
		ModifyListener widgetModifyListener;
	};
	
	filter filter;

	private boolean useTree;

	protected int headerHeight;

	private Shell shell;


	/**
	 * Main Initializer
	 * @param _sTableID Which table to handle (see 
	 *                   {@link org.gudy.azureus2.plugins.ui.tables.TableManager}).
	 *                   Config settings are stored with the prefix of  
	 *                   "Table.<i>TableID</i>"
	 * @param _sPropertiesPrefix Prefix for retrieving text from the properties
	 *                            file (MessageText).  Typically 
	 *                            <i>TableID</i> + "View"
	 * @param _basicItems Column Definitions
	 * @param _sDefaultSortOn Column name to sort on if user hasn't chosen one yet
	 * @param _iTableStyle SWT style constants used when creating the table
	 */
	public TableViewSWTImpl(Class<?> pluginDataSourceType, String _sTableID,
			String _sPropertiesPrefix, TableColumnCore[] _basicItems,
			String _sDefaultSortOn, int _iTableStyle) {
		boolean wantTree = (_iTableStyle & SWT.CASCADE) != 0;
		_iTableStyle &= ~SWT.CASCADE;
		if (wantTree) {
			useTree = COConfigurationManager.getBooleanParameter("Table.useTree")
					&& !Utils.isCarbon;
		}
		classPluginDataSourceType = pluginDataSourceType;
		sTableID = _sTableID;
		basicItems = _basicItems;
		sPropertiesPrefix = _sPropertiesPrefix;
		sDefaultSortOn = _sDefaultSortOn;
		iTableStyle = _iTableStyle | SWT.V_SCROLL | SWT.DOUBLE_BUFFERED;

		mapDataSourceToRow = new LightHashMap<DATASOURCETYPE, TableRowCore>();
		sortedRows = new ArrayList<TableRowSWT>();
		listUnfilteredDataSources = new HashSet<DATASOURCETYPE>();
	}

	/**
	 * Main Initializer. Table Style will be SWT.SINGLE | SWT.FULL_SELECTION
	 *
	 * @param _sTableID Which table to handle (see 
	 *                   {@link org.gudy.azureus2.plugins.ui.tables.TableManager}
	 *                   ).  Config settings are stored with the prefix of 
	 *                   "Table.<i>TableID</i>"
	 * @param _sPropertiesPrefix Prefix for retrieving text from the properties
	 *                            file (MessageText).  
	 *                            Typically <i>TableID</i> + "View"
	 * @param _sDefaultSortOn Column name to sort on if user hasn't chosen one
	 *                         yet
	 */
	public TableViewSWTImpl(Class<?> pluginDataSourceType, String _sTableID,
			String _sPropertiesPrefix, String _sDefaultSortOn) {

		this(pluginDataSourceType, _sTableID, _sPropertiesPrefix,
				new TableColumnCore[0], _sDefaultSortOn, SWT.SINGLE
						| SWT.FULL_SELECTION | SWT.VIRTUAL);
	}

	private void initializeColumnDefs() {
		// XXX Adding Columns only has to be done once per TableID.  
		// Doing it more than once won't harm anything, but it's a waste.
		TableColumnManager tcManager = TableColumnManager.getInstance();

		if (basicItems != null) {
			if (tcManager.getTableColumnCount(sTableID) != basicItems.length) {
				tcManager.addColumns(basicItems);
			}
			basicItems = null;
		}

		tableColumns = tcManager.getAllTableColumnCoreAsArray(classPluginDataSourceType,
				sTableID);

		// fixup order
		tcManager.ensureIntegrety(sTableID);
	}

	// AbstractIView::initialize
	public void initialize(Composite composite) {
		composite.setRedraw(false);
		shell = composite.getShell();
		mainComposite = createSashForm(composite);
		table = createTable(tableComposite);
		menu = createMenu(table);
		clientArea = table.getClientArea();
		editor = TableOrTreeUtils.createTableOrTreeEditor(table);
		editor.minimumWidth = 80;
		editor.grabHorizontal = true;
		initializeTable(table);

		triggerLifeCycleListener(TableLifeCycleListener.EVENT_INITIALIZED);

		configMan.addParameterListener("Graphics Update", this);
		configMan.addParameterListener("ReOrder Delay", this);
		Colors.getInstance().addColorsChangedListener(this);

		// So all TableView objects of the same TableID have the same columns,
		// and column widths, etc
		TableStructureEventDispatcher.getInstance(sTableID).addListener(this);
		composite.setRedraw(true);
	}

	private Composite createSashForm(final Composite composite) {
		if (!bEnableTabViews) {
			tableComposite = createMainPanel(composite);
			return tableComposite;
		}

		int iNumViews = 0;

		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		UISWTViewEventListenerHolder[] pluginViews = null;
		if (uiFunctions != null) {
			UISWTInstanceImpl pluginUI = uiFunctions.getSWTPluginInstanceImpl();

			if (pluginUI != null) {
				pluginViews = pluginUI.getViewListeners(sTableID);
				iNumViews += pluginViews.length;
			}
		}

		if (iNumViews == 0) {
			tableComposite = createMainPanel(composite);
			return tableComposite;
		}

		FormData formData;

		final Composite form = new Composite(composite, SWT.NONE);
		FormLayout flayout = new FormLayout();
		flayout.marginHeight = 0;
		flayout.marginWidth = 0;
		form.setLayout(flayout);
		GridData gridData;
		gridData = new GridData(GridData.FILL_BOTH);
		form.setLayoutData(gridData);

		// Create them in reverse order, so we can have the table auto-grow, and
		// set the tabFolder's height manually

		final int TABHEIGHT = 20;
		tabFolder = new CTabFolder(form, SWT.TOP | SWT.BORDER);
		tabFolder.setMinimizeVisible(true);
		tabFolder.setTabHeight(TABHEIGHT);
		final int iFolderHeightAdj = tabFolder.computeSize(SWT.DEFAULT, 0).y;

		final Sash sash = new Sash(form, SWT.HORIZONTAL);

		tableComposite = createMainPanel(form);
		Composite cFixLayout = tableComposite;
		while (cFixLayout != null && cFixLayout.getParent() != form) {
			cFixLayout = cFixLayout.getParent();
		}
		if (cFixLayout == null) {
			cFixLayout = tableComposite;
		}
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		cFixLayout.setLayout(layout);

		// FormData for Folder
		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.bottom = new FormAttachment(100, 0);
		int iSplitAt = configMan.getIntParameter(sPropertiesPrefix + ".SplitAt",
				3000);
		// Was stored at whole
		if (iSplitAt < 100) {
			iSplitAt *= 100;
		}

		double pct = iSplitAt / 10000.0;
		if (pct < 0.03) {
			pct = 0.03;
		} else if (pct > 0.97) {
			pct = 0.97;
		}

		// height will be set on first resize call
		sash.setData("PCT", new Double(pct));
		tabFolder.setLayoutData(formData);
		final FormData tabFolderData = formData;

		// FormData for Sash
		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.bottom = new FormAttachment(tabFolder);
		formData.height = 5;
		sash.setLayoutData(formData);

		// FormData for table Composite
		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.top = new FormAttachment(0, 0);
		formData.bottom = new FormAttachment(sash);
		cFixLayout.setLayoutData(formData);

		// Listeners to size the folder
		sash.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				final boolean FASTDRAG = true;

				if (FASTDRAG && e.detail == SWT.DRAG) {
					return;
				}

				if (tabFolder.getMinimized()) {
					tabFolder.setMinimized(false);
					refreshSelectedSubView();
					configMan.setParameter(sPropertiesPrefix + ".subViews.minimized",
							false);
				}

				Rectangle area = form.getClientArea();
				tabFolderData.height = area.height - e.y - e.height - iFolderHeightAdj;
				form.layout();

				Double l = new Double((double) tabFolder.getBounds().height
						/ form.getBounds().height);
				sash.setData("PCT", l);
				if (e.detail != SWT.DRAG) {
					configMan.setParameter(sPropertiesPrefix + ".SplitAt",
							(int) (l.doubleValue() * 10000));
				}
			}
		});

		final CTabFolder2Adapter folderListener = new CTabFolder2Adapter() {
			public void minimize(CTabFolderEvent event) {
				tabFolder.setMinimized(true);
				tabFolderData.height = iFolderHeightAdj;
				CTabItem[] items = tabFolder.getItems();
				for (int i = 0; i < items.length; i++) {
					CTabItem tabItem = items[i];
					tabItem.getControl().setVisible(false);
				}
				form.layout();

				UISWTViewCore view = getActiveSubView();
				if (view != null) {
					view.triggerEvent(UISWTViewEvent.TYPE_FOCUSLOST, null);
				}

				
				configMan.setParameter(sPropertiesPrefix + ".subViews.minimized", true);
			}

			public void restore(CTabFolderEvent event) {
				tabFolder.setMinimized(false);
				CTabItem selection = tabFolder.getSelection();
				if (selection != null) {
					selection.getControl().setVisible(true);
				}
				form.notifyListeners(SWT.Resize, null);

				UISWTViewCore view = getActiveSubView();
				if (view != null) {
					view.triggerEvent(UISWTViewEvent.TYPE_FOCUSGAINED, null);
				}
				refreshSelectedSubView();

				configMan.setParameter(sPropertiesPrefix + ".subViews.minimized", false);
			}

		};
		tabFolder.addCTabFolder2Listener(folderListener);

		tabFolder.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				// make sure its above
				try {
					((CTabItem) e.item).getControl().setVisible(true);
					((CTabItem) e.item).getControl().moveAbove(null);

					// TODO: Need to viewDeactivated old one
					UISWTViewCore view = getActiveSubView();
					if (view != null) {
						view.triggerEvent(UISWTViewEvent.TYPE_FOCUSGAINED, null);
					}
					
				} catch (Exception t) {
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		tabFolder.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				if (tabFolder.getMinimized()) {
					folderListener.restore(null);
					// If the user clicked down on the restore button, and we restore
					// before the CTabFolder does, CTabFolder will minimize us again
					// There's no way that I know of to determine if the mouse is 
					// on that button!

					// one of these will tell tabFolder to cancel
					e.button = 0;
					tabFolder.notifyListeners(SWT.MouseExit, null);
				}
			}
		});

		form.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event e) {
				if (tabFolder.getMinimized()) {
					return;
				}

				Double l = (Double) sash.getData("PCT");
				if (l != null) {
					tabFolderData.height = (int) (form.getBounds().height * l.doubleValue())
							- iFolderHeightAdj;
					form.layout();
				}
			}
		});

		// Call plugin listeners
		if (pluginViews != null) {
			for (UISWTViewEventListenerHolder l : pluginViews) {
				if (l != null) {
					try {
						UISWTViewImpl view = new UISWTViewImpl(sTableID, l.getViewID(), l, null);
						addTabView(view);
					} catch (Exception e) {
						// skip, plugin probably specifically asked to not be added
					}
				}
			}
		}

		if (configMan.getBooleanParameter(
				sPropertiesPrefix + ".subViews.minimized", false)) {
			tabFolder.setMinimized(true);
			tabFolderData.height = iFolderHeightAdj;
		} else {
			tabFolder.setMinimized(false);
		}

		tabFolder.setSelection(0);

		return form;
	}

	/** Creates a composite within the specified composite and sets its layout
	 * to a default FillLayout().
	 *
	 * @param composite to create your Composite under
	 * @return The newly created composite
	 */
	public Composite createMainPanel(Composite composite) {
		TableViewSWTPanelCreator mainPanelCreator = getMainPanelCreator();
		if (mainPanelCreator != null) {
			return mainPanelCreator.createTableViewPanel(composite);
		}
		Composite panel = new Composite(composite, SWT.NO_FOCUS);
		composite.getLayout();
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		panel.setLayout(layout);

		Object parentLayout = composite.getLayout();
		if (parentLayout == null || (parentLayout instanceof GridLayout)) {
			panel.setLayoutData(new GridData(GridData.FILL_BOTH));
		}

		return panel;
	}

	/** Creates the Table.
	 *
	 * @return The created Table.
	 */
	public TableOrTreeSWT createTable(Composite panel) {
		table = TableOrTreeUtils.createGrid(panel, iTableStyle, useTree);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));

		return table;
	}

	/** Sets up the sorter, columns, and context menu.
	 *
	 * @param table Table to be initialized
	 */
	public void initializeTable(final TableOrTreeSWT table) {
		initializeColumnDefs();

		iTableStyle = table.getStyle();
		if ((iTableStyle & SWT.VIRTUAL) == 0) {
			throw new Error("Virtual Table Required");
		}

		table.setLinesVisible(Utils.TABLE_GRIDLINE_IS_ALTERNATING_COLOR);
		table.setMenu(menu);
		table.setData("Name", sTableID);
		table.setData("TableView", this);
		
		// On Windows, TreeItems POSTPAINT event spendsabout 7% of it's time in getFont()
		// calling the OS API.  If we set the font, it skips the API call.
		// This could be optimized further by setting the font on each table item,
		// however, it's unknown what performace hit we'd get on row creation.
		table.setFont(table.getFont());

		// Setup table
		// -----------

		table.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent event) {
				swt_changeColumnIndicator();
				// This fixes the scrollbar not being long enough on Win2k
				// There may be other methods to get it to refresh right, but
				// layout(true, true) didn't work.
				table.setRedraw(false);
				table.setRedraw(true);
				table.removePaintListener(this);
			}
		});

		table.addListener(SWT.PaintItem, new TableViewSWT_PaintItem(this, table));

		if (Constants.isWindows) {
			TableViewSWT_EraseItem eraseItemListener = new TableViewSWT_EraseItem(this, table);
			table.addListener(SWT.EraseItem, eraseItemListener);
			table.addListener(SWT.Paint, eraseItemListener);
		}

		ScrollBar horizontalBar = table.getHorizontalBar();
		if (horizontalBar != null) {
			horizontalBar.addSelectionListener(new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent e) {
					Utils.execSWTThreadLater(0, new AERunnable() {
						public void runSupport() {
							swt_calculateClientArea();
						}
					});
					//updateColumnVisibilities();
				}

				public void widgetSelected(SelectionEvent e) {
					Utils.execSWTThreadLater(0, new AERunnable() {
						public void runSupport() {
							swt_calculateClientArea();
						}
					});
					//updateColumnVisibilities();
				}
			});
		}

		table.addListener(SWT.MeasureItem, new Listener() {
			public void handleEvent(Event event) {
				int iColumnNo = event.index;

				if (bSkipFirstColumn) {
					iColumnNo--;
				}

				if (iColumnNo >= 0 && iColumnNo < columnsOrdered.length) {
					TableColumnCore tc = columnsOrdered[iColumnNo];
					int preferredWidth = tc.getPreferredWidth();
					event.width = preferredWidth;
				}

				int defaultHeight = getRowDefaultHeight();
				if (event.height < defaultHeight) {
					event.height = defaultHeight;
				}
			}
		});

		// Deselect rows if user clicks on a blank spot (a spot with no row)
		table.addMouseListener(new MouseAdapter() {
			long lastMouseDblClkEventTime = 0;
			public void mouseDoubleClick(MouseEvent e) {
				long time = e.time & 0xFFFFFFFFL;
				long diff = time - lastMouseDblClkEventTime;
				// We fake a double click on MouseUp.. this traps 2 double clicks
				// in quick succession and ignores the 2nd.
				if (diff <= e.display.getDoubleClickTime() && diff >= 0) {
					return;
				}
				lastMouseDblClkEventTime = time;

				TableColumnCore tc = getTableColumnByOffset(e.x);
				TableCellCore cell = getTableCell(e.x, e.y);
				if (cell != null && tc != null) {
					TableCellMouseEvent event = createMouseEvent(cell, e,
							TableCellMouseEvent.EVENT_MOUSEDOUBLECLICK, false);
					if (event != null) {
						tc.invokeCellMouseListeners(event);
						cell.invokeMouseListeners(event);
						if (event.skipCoreFunctionality) {
							lCancelSelectionTriggeredOn = System.currentTimeMillis();
						}
					}
				}
			}

			long lastMouseUpEventTime = 0;
			Point lastMouseUpPos = new Point(0, 0);
			boolean mouseDown = false;
			public void mouseUp(MouseEvent e) {
				// SWT OSX Bug: two mouseup events when app not in focus and user
				// clicks on the table.  Only one mousedown, so track that and ignore
				if (!mouseDown) {
					return;
				}
				mouseDown = false;
				if (e.button == 1) {
  				long time = e.time & 0xFFFFFFFFL;
  				long diff = time - lastMouseUpEventTime;
					if (diff <= e.display.getDoubleClickTime() && diff >= 0
							&& lastMouseUpPos.x == e.x && lastMouseUpPos.y == e.y) {
						// Fake double click because Cocoa SWT 3650 doesn't always trigger
						// DefaultSelection listener on a Tree on dblclick (works find in Table)
						runDefaultAction(e.stateMask);
						return;
					}
  				lastMouseUpEventTime = time;
  				lastMouseUpPos = new Point(e.x, e.y);
				}

				TableColumnCore tc = getTableColumnByOffset(e.x);
				TableCellCore cell = getTableCell(e.x, e.y);
				if (cell != null && tc != null) {
					TableCellMouseEvent event = createMouseEvent(cell, e,
							TableCellMouseEvent.EVENT_MOUSEUP, false);
					if (event != null) {
						tc.invokeCellMouseListeners(event);
						cell.invokeMouseListeners(event);
						if (event.skipCoreFunctionality) {
							lCancelSelectionTriggeredOn = System.currentTimeMillis();
						}
					}
				}
			}

			TableRowCore lastClickRow;

			public void mouseDown(MouseEvent e) {
				mouseDown = true;
				// we need to fill the selected row indexes here because the
				// dragstart event can occur before the SWT.SELECTION event and
				// our drag code needs to know the selected rows..
				TableRowSWT row = getTableRow(e.x, e.y, false);
				if (row == null) {
					setSelectedRows(new TableRowCore[0]);
				} else if (!row.isRowDisposed()) {
					selectRow(row, true);
				}

				TableColumnCore tc = getTableColumnByOffset(e.x);
				TableCellCore cell = getTableCell(e.x, e.y);

				editCell(-1, -1); // clear out current cell editor

				if (cell != null && tc != null) {
					if (e.button == 2 && e.stateMask == SWT.CONTROL) {
						((TableCellImpl) cell).bDebug = !((TableCellImpl) cell).bDebug;
						System.out.println("Set debug for " + cell + " to "
								+ ((TableCellImpl) cell).bDebug);
					}
					TableCellMouseEvent event = createMouseEvent(cell, e,
							TableCellMouseEvent.EVENT_MOUSEDOWN, false);
					if (event != null) {
						tc.invokeCellMouseListeners(event);
						cell.invokeMouseListeners(event);
						invokeRowMouseListener(event);
						if (event.skipCoreFunctionality) {
							lCancelSelectionTriggeredOn = System.currentTimeMillis();
						}
					}
					if (tc.isInplaceEdit() && e.button == 1
							&& lastClickRow == cell.getTableRowCore()) {
						editCell(getColumnNo(e.x), cell.getTableRowCore().getIndex());
					}
					if (e.button == 1) {
						lastClickRow = cell.getTableRowCore();
					}
				} else if (row != null) {
					TableRowMouseEvent event = createMouseEvent(row, e,
							TableCellMouseEvent.EVENT_MOUSEDOWN, false);
					if (event != null) {
						invokeRowMouseListener(event);
					}
				}
			}
		});

		table.addMouseMoveListener(new MouseMoveListener() {
			TableCellCore lastCell = null;

			int lastCursorID = 0;

			public void mouseMove(MouseEvent e) {
				lCancelSelectionTriggeredOn = -1;
				if (isDragging) {
					return;
				}
				try {
					TableCellCore cell = getTableCell(e.x, e.y);
					
					if (lastCell != null && cell != lastCell && !lastCell.isDisposed()) {
						TableCellMouseEvent event = createMouseEvent(lastCell, e,
								TableCellMouseEvent.EVENT_MOUSEEXIT, true);
						if (event != null) {
							TableColumnCore tc = ((TableColumnCore) lastCell.getTableColumn());
							if (tc != null) {
								tc.invokeCellMouseListeners(event);
							}
							lastCell.invokeMouseListeners(event);
						}
					}

					int iCursorID = 0;
					if (cell == null) {
						lastCell = null;
					} else if (cell == lastCell) {
						iCursorID = lastCursorID;
					} else {
						iCursorID = cell.getCursorID();
						lastCell = cell;
					}

					if (iCursorID < 0) {
						iCursorID = 0;
					}

					if (iCursorID != lastCursorID) {
						lastCursorID = iCursorID;

						if (iCursorID >= 0) {
							table.setCursor(table.getDisplay().getSystemCursor(iCursorID));
						} else {
							table.setCursor(null);
						}
					}

					if (cell != null) {
						TableCellMouseEvent event = createMouseEvent(cell, e,
								TableCellMouseEvent.EVENT_MOUSEMOVE, false);
						if (event != null) {
							TableColumnCore tc = ((TableColumnCore) cell.getTableColumn());
							if (tc.hasCellMouseMoveListener()) {
								((TableColumnCore) cell.getTableColumn()).invokeCellMouseListeners(event);
							}
							cell.invokeMouseListeners(event);

							// listener might have changed it

							int cellCursorID = cell.getCursorID();
							if (cellCursorID != -1) {
								lastCursorID = cellCursorID;
							}
						}
					}
				} catch (Exception ex) {
					Debug.out(ex);
				}
			}
		});

		table.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				updateSelectedRows(table.getSelection(), true);
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				if (lCancelSelectionTriggeredOn > 0
						&& System.currentTimeMillis() - lCancelSelectionTriggeredOn < 200) {
					e.doit = false;
				} else {
					runDefaultAction(e.stateMask);
				}
			}
		});

		// we are sent a SWT.Settings event when the language changes and
		// when System fonts/colors change.  In both cases, invalidate
		table.addListener(SWT.Settings, new Listener() {
			public void handleEvent(Event e) {
				tableInvalidate();
			}
		});
		
		if (useTree) {
  		Listener listenerExpandCollapse = new Listener() {
  			public void handleEvent(Event event) {
  				TableItemOrTreeItem item = TableOrTreeUtils.getEventItem(event.item);
  				if (item == null) {
  					return;
  				}
  				TableRowCore row = getRow(item);
  				if (row == null || row.isRowDisposed()) {
  					return;
  				}
  				row.setExpanded(event.type == SWT.Expand ? true : false);
  				Utils.execSWTThreadLater(0, new AERunnable() {
						public void runSupport() {
							visibleRowsChanged();
						}
					});
  			}
  		};
  		table.addListener(SWT.Expand, listenerExpandCollapse);
  		table.addListener(SWT.Collapse, listenerExpandCollapse);
		}

		new TableTooltips(this, table.getComposite());

		table.addKeyListener(this);
		
		table.addDisposeListener(new DisposeListener(){
			public void widgetDisposed(DisposeEvent e) {
				if (filter != null && filter.widget != null && !filter.widget.isDisposed()) {
					filter.widget.removeKeyListener(TableViewSWTImpl.this);
					filter.widget.removeModifyListener(filter.widgetModifyListener);
				}
				Utils.disposeSWTObjects(new Object[] { sliderArea } );
			}
		});
/*
		if (Utils.isCocoa) {
			table.addListener(SWT.MouseVerticalWheel, new Listener() {
				public void handleEvent(Event event) {
					calculateClientArea();
					visibleRowsChanged();
				}
			});
		}
*/		
		ScrollBar bar = table.getVerticalBar();
		if (bar != null) {
			bar.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					Utils.execSWTThreadLater(0, new AERunnable() {
						public void runSupport() {
							// need to calc later as getClientArea isn't up to date yet
							// on Win
							swt_calculateClientArea();
							visibleRowsChanged();
						}
					});
					// Bug: Scroll is slow when table is not focus
					if (!table.isFocusControl()) {
						table.setFocus();
					}
				}
			});
		}

		table.setHeaderVisible(getHeaderVisible());
		headerHeight = table.getHeaderHeight();

		clientArea = table.getClientArea();
		//firstClientArea = table.getClientArea();
		table.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event event) {
				swt_calculateClientArea();
			}
		});

		swt_initializeTableColumns(table);

		MessageText.addListener(this);
	}
	
	public void localeChanged(Locale old_locale, Locale new_locale) {
		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				if (tabViews != null && tabViews.size() > 0) {
					for (int i = 0; i < tabViews.size(); i++) {
						UISWTViewCore view = tabViews.get(i);
						if (view != null) {
							view.triggerEvent(UISWTViewEvent.TYPE_LANGUAGEUPDATE, null);
						}
					}
				}
				tableInvalidate();
				refreshTable(true);

				TableColumnOrTreeColumn[] tableColumnsSWT = table.getColumns();
				for (int i = 0; i < tableColumnsSWT.length; i++) {
					TableColumnCore column = (TableColumnCore) tableColumnsSWT[i].getData("TableColumnCore");
					if (column != null) {
						Messages.setLanguageText(tableColumnsSWT[i].getColumn(),
								column.getTitleLanguageKey());
					}
				}

			}
		});
	}


	public void keyPressed(KeyEvent event) {
		// Note: Both table key presses and txtFilter keypresses go through this
		//       method.

		Object[] listeners = listenersKey.toArray();
		for (int i = 0; i < listeners.length; i++) {
			KeyListener l = (KeyListener) listeners[i];
			l.keyPressed(event);
			if (!event.doit) {
				lCancelSelectionTriggeredOn = SystemTime.getCurrentTime();
				return;
			}
		}

		if (event.keyCode == SWT.F5) {
			if ((event.stateMask & SWT.SHIFT) > 0) {
				runForSelectedRows(new TableGroupRowRunner() {
					public void run(TableRowCore row) {
						row.invalidate();
						row.refresh(true);
					}
				});
			} else if ((event.stateMask & SWT.CONTROL) > 0) {
				runForAllRows(new TableGroupRowRunner() {
					public void run(TableRowCore row) {
						row.invalidate();
						row.refresh(true);
					}
				});
			} else {
				sortColumn(true);
			}
			event.doit = false;
			return;
		}

		int key = event.character;
		if (key <= 26 && key > 0) {
			key += 'a' - 1;
		}

		if (event.stateMask == SWT.MOD1) {
			switch (key) {
				case 'a': // CTRL+A select all Torrents
					if (filter == null || event.widget != filter.widget) {
						if ((table.getStyle() & SWT.MULTI) > 0) {
							selectAll();
							event.doit = false;
						}
					} else {
						filter.widget.selectAll();
						event.doit = false;
					}
					break;

				case '+': {
					if (Constants.isUnix) {
						TableColumnOrTreeColumn[] tableColumnsSWT = table.getColumns();
						for (int i = 0; i < tableColumnsSWT.length; i++) {
							TableColumnCore tc = (TableColumnCore) tableColumnsSWT[i].getData("TableColumnCore");
							if (tc != null) {
								int w = tc.getPreferredWidth();
								if (w <= 0) {
									w = tc.getMinWidth();
									if (w <= 0) {
										w = 100;
									}
								}
								tc.setWidth(w);
							}
						}
						event.doit = false;
					}
					break;
				}
				case 'f': // CTRL+F Find/Filter
					openFilterDialog();
					event.doit = false;
					break;
				case 'x': // CTRL+X: RegEx search switch
					if (filter != null && event.widget == filter.widget) {
						filter.regex = !filter.regex;
						filter.widget.setBackground(filter.regex?COLOR_FILTER_REGEX:null);
						validateFilterRegex();
						refilter();
						return;
					}
					break;
				case 'g':
					System.out.println("force sort");
					lLastSortedOn = 0;
					sortColumn(true);
					break;
			}

		}

		if (event.stateMask == 0) {
			if (filter != null && filter.widget == event.widget) {
				if (event.keyCode == SWT.ARROW_DOWN) {
					setFocus();
					event.doit = false;
				} else if (event.character == 13) {
					refilter();
				}
			}
		}

		if (!event.doit) {
			return;
		}

		handleSearchKeyPress(event);
	}

	public void keyReleased(KeyEvent event) {
		swt_calculateClientArea();
		visibleRowsChanged();

		Object[] listeners = listenersKey.toArray();
		for (int i = 0; i < listeners.length; i++) {
			KeyListener l = (KeyListener) listeners[i];
			l.keyReleased(event);
			if (!event.doit) {
				return;
			}
		}
	}
	
	
	// @see com.aelitis.azureus.ui.common.table.TableView#getHeaderVisible()
	public boolean getHeaderVisible() {
		return headerVisible;
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#setHeaderVisible(boolean)
	public void setHeaderVisible(boolean visible) {
		headerVisible = visible;

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (table != null && !table.isDisposed()) {
					table.setHeaderVisible(headerVisible);
					headerHeight = table.getHeaderHeight();
				}
			}
		});
	}

	protected void swt_calculateClientArea() {
		Rectangle oldClientArea = clientArea;
		clientArea = table.getClientArea();
		ScrollBar horizontalBar = table.getHorizontalBar();
		boolean clientAreaCausedVisibilityChanged = false;
		if (horizontalBar != null) {
			int pos = horizontalBar.getSelection();
			if (pos != lastHorizontalPos) {
				lastHorizontalPos = pos;
				clientAreaCausedVisibilityChanged = true;
			}
		}
		if (oldClientArea != null
				&& (oldClientArea.x != clientArea.x || oldClientArea.width != clientArea.width)) {
			clientAreaCausedVisibilityChanged = true;
		}
		if (oldClientArea != null
				&& (oldClientArea.y != clientArea.y || oldClientArea.height != clientArea.height)) {
			visibleRowsChanged();
		}
		if (oldClientArea != null
				&& (oldClientArea.height < table.getHeaderHeight())) {
			clientAreaCausedVisibilityChanged = true;
		}
		if (clientAreaCausedVisibilityChanged) {
			columnVisibilitiesChanged = true;
			Utils.execSWTThreadLater(50, new AERunnable() {
				public void runSupport() {
					if (columnVisibilitiesChanged) {
						refreshTable(false);
					}
				}
			});
		}
	}

	protected void triggerTabViewDataSourceChanged(UISWTViewCore view) {
		if (view != null) {
			view.triggerEvent(UISWTViewEvent.TYPE_DATASOURCE_CHANGED, getParentDataSource());

			if (view.useCoreDataSource()) {
				Object[] dataSourcesCore = getSelectedDataSources(true);
				if (dataSourcesCore.length > 0) {
					view.triggerEvent(UISWTViewEvent.TYPE_DATASOURCE_CHANGED,
							dataSourcesCore.length == 0 ? getParentDataSource()
									: dataSourcesCore);
				}
			} else {
				Object[] dataSourcesPlugin = getSelectedDataSources(false);
				if (dataSourcesPlugin.length > 0) {
					view.triggerEvent(
							UISWTViewEvent.TYPE_DATASOURCE_CHANGED,
							dataSourcesPlugin.length == 0 ? PluginCoreUtils.convert(
									getParentDataSource(), false) : dataSourcesPlugin);
				}
			}
		}
		
	}

	protected void triggerTabViewsDataSourceChanged(boolean sendParent) {
		if (tabViews == null || tabViews.size() == 0) {
			return;
		}
		
		if (sendParent) {
			for (int i = 0; i < tabViews.size(); i++) {
				UISWTViewCore view = tabViews.get(i);
				if (view != null) {
					view.triggerEvent(UISWTViewEvent.TYPE_DATASOURCE_CHANGED,
							getParentDataSource());
				}
			}
			return;
		}

		// Set Data Object for all tabs.  

		Object[] dataSourcesCore = getSelectedDataSources(true);
		Object[] dataSourcesPlugin = null;

		for (int i = 0; i < tabViews.size(); i++) {
			UISWTViewCore view = tabViews.get(i);
			if (view != null) {
				if (view.useCoreDataSource()) {
					view.triggerEvent(UISWTViewEvent.TYPE_DATASOURCE_CHANGED,
							dataSourcesCore.length == 0 ? getParentDataSource()
									: dataSourcesCore);
				} else {
					if (dataSourcesPlugin == null) {
						dataSourcesPlugin = getSelectedDataSources(false);
					}

					view.triggerEvent(
							UISWTViewEvent.TYPE_DATASOURCE_CHANGED,
							dataSourcesPlugin.length == 0 ? PluginCoreUtils.convert(
									getParentDataSource(), false) : dataSourcesPlugin);
				}
			}
		}
	}

	private interface SourceReplaceListener
	{
		void sourcesChanged();

		void cleanup(Text toClean);
	}

	private SourceReplaceListener cellEditNotifier;

	private Control sliderArea;

	private boolean isDragging;

	private int maxItemShown = -1;

	private void editCell(final int column, final int row) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				swt_editCell(column, row);
			}
		});
	}

	private void swt_editCell(final int column, final int row) {
		Text oldInput = (Text) editor.getEditor();
		if (column >= table.getColumnCount() || row < 0
				|| row >= table.getItemCount()) {
			cellEditNotifier = null;
			if (oldInput != null && !oldInput.isDisposed()) {
				editor.getEditor().dispose();
			}
			return;
		}

		TableColumnOrTreeColumn tcColumn = table.getColumn(column);
		final TableItemOrTreeItem item = table.getItem(row);

		String cellName = (String) tcColumn.getData("Name");
		final TableRowSWT rowSWT = (TableRowSWT) getRow(row);
		final TableCellSWT cell = rowSWT.getTableCellSWT(cellName);

		// reuse widget if possible, this way we'll keep the focus all the time on jumping through the rows
		final Text newInput = oldInput == null || oldInput.isDisposed() ? new Text(
				table.getComposite(), Constants.isOSX ? SWT.NONE : SWT.BORDER) : oldInput;
		final DATASOURCETYPE datasource = (DATASOURCETYPE) cell.getDataSource();
		if (cellEditNotifier != null) {
			cellEditNotifier.cleanup(newInput);
		}

		table.showItem(item);
		table.showColumn(tcColumn);

		newInput.setText(cell.getText());

		newInput.setSelection(0);
		newInput.selectAll();
		newInput.setFocus();

		class QuickEditListener
			implements ModifyListener, SelectionListener, KeyListener,
			TraverseListener, SourceReplaceListener, ControlListener
		{
			boolean resizing = true;

			public QuickEditListener(Text toAttach) {
				toAttach.addModifyListener(this);
				toAttach.addSelectionListener(this);
				toAttach.addKeyListener(this);
				toAttach.addTraverseListener(this);
				toAttach.addControlListener(this);

				cellEditNotifier = this;
			}

			public void modifyText(ModifyEvent e) {
				if (item.isDisposed()) {
					sourcesChanged();
					return;
				}
				if (((TableColumnCore) cell.getTableColumn()).inplaceValueSet(cell,
						newInput.getText(), false)) {
					newInput.setBackground(null);
				} else {
					newInput.setBackground(Colors.colorErrorBG);
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				if (item.isDisposed()) {
					sourcesChanged();
					newInput.traverse(SWT.TRAVERSE_RETURN);
					return;
				}
				((TableColumnCore) cell.getTableColumn()).inplaceValueSet(cell,
						newInput.getText(), true);
				rowSWT.invalidate();
				editCell(column, row + 1);
			}

			public void widgetSelected(SelectionEvent e) {
			}

			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN || e.keyCode == SWT.ARROW_UP) {
					e.doit = false;
					editCell(column, row + (e.keyCode == SWT.ARROW_DOWN ? 1 : -1));
				}
			}

			public void keyReleased(KeyEvent e) {
			}

			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE) {
					e.doit = false;
					editCell(column, -1);
				}
			}

			public void sourcesChanged() {
				if (getRow(datasource) == rowSWT || getRow(datasource) == null
						|| newInput.isDisposed()) {
					return;
				}
				String newVal = newInput.getText();
				Point sel = newInput.getSelection();
				editCell(column, getRow(datasource).getIndex());
				if (newInput.isDisposed()) {
					return;
				}
				newInput.setText(newVal);
				newInput.setSelection(sel);
			}

			public void cleanup(Text oldText) {
				if (!oldText.isDisposed()) {
					oldText.removeModifyListener(this);
					oldText.removeSelectionListener(this);
					oldText.removeKeyListener(this);
					oldText.removeTraverseListener(this);
					oldText.removeControlListener(this);
				}
			}

			public void controlMoved(ControlEvent e) {
				table.showItem(item);
				if (resizing) {
					return;
				}
				resizing = true;

				Point sel = newInput.getSelection();

				TableOrTreeUtils.setEditorItem(editor, newInput, column, item);

				editor.minimumWidth = newInput.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;

				Rectangle leftAlignedBounds = item.getBounds(column);
				leftAlignedBounds.width = editor.minimumWidth = newInput.computeSize(
						SWT.DEFAULT, SWT.DEFAULT).x;
				if (leftAlignedBounds.intersection(clientArea).equals(leftAlignedBounds)) {
					editor.horizontalAlignment = SWT.LEFT;
				} else {
					editor.horizontalAlignment = SWT.RIGHT;
				}

				editor.layout();

				newInput.setSelection(0);
				newInput.setSelection(sel);

				resizing = false;
			}

			public void controlResized(ControlEvent e) {
			}
		}

		QuickEditListener l = new QuickEditListener(newInput);

		l.modifyText(null);

		TableOrTreeUtils.setEditorItem(editor, newInput, column, item);
		table.deselectAll();
		table.select(table.getItem(row));
		updateSelectedRows(new TableRowCore[] { sortedRows.get(row) }, true);

		l.resizing = false;

		l.controlMoved(null);
	}

	private TableCellMouseEvent createMouseEvent(TableCellCore cell, MouseEvent e,
			int type, boolean allowOOB) {
		TableCellMouseEvent event = new TableCellMouseEvent();
		event.cell = cell;
		if (cell != null) {
			event.row = cell.getTableRow();
		}
		event.eventType = type;
		event.button = e.button;
		// TODO: Change to not use SWT masks
		event.keyboardState = e.stateMask;
		event.skipCoreFunctionality = false;
		if (cell instanceof TableCellSWT) {
			Rectangle r = ((TableCellSWT) cell).getBounds();
			if (r == null) {
				return event;
			}
			event.x = e.x - r.x;
			if (!allowOOB && event.x < 0) {
				return null;
			}
			event.y = e.y - r.y;
			if (!allowOOB && event.y < 0) {
				return null;
			}
		}

		return event;
	}

	private TableRowMouseEvent createMouseEvent(TableRowSWT row, MouseEvent e,
			int type, boolean allowOOB) {
		TableCellMouseEvent event = new TableCellMouseEvent();
		event.row = row;
		event.eventType = type;
		event.button = e.button;
		// TODO: Change to not use SWT masks
		event.keyboardState = e.stateMask;
		event.skipCoreFunctionality = false;
		if (row != null) {
			Rectangle r = row.getBounds();
			event.x = e.x - r.x;
			if (!allowOOB && event.x < 0) {
				return null;
			}
			event.y = e.y - r.y;
			if (!allowOOB && event.y < 0) {
				return null;
			}
		}

		return event;
	}

	public void runDefaultAction(int stateMask) {
		// Don't allow mutliple run defaults in quick succession
		if (lastSelectionTriggeredOn > 0
				&& System.currentTimeMillis() - lastSelectionTriggeredOn < 200) {
			return;
		}
		
		// plugin may have cancelled the default action
		if (System.currentTimeMillis() - lCancelSelectionTriggeredOn > 200) {
			lastSelectionTriggeredOn = System.currentTimeMillis();
			TableRowCore[] selectedRows = getSelectedRows();
			triggerDefaultSelectedListeners(selectedRows, stateMask);
		}
	}

	private void swt_updateColumnVisibilities(boolean doInvalidate) {
		TableColumnOrTreeColumn[] columns = table.getColumns();
		if (table.getItemCount() < 1 || columns.length == 0 || !table.isVisible()) {
			return;
		}
		columnVisibilitiesChanged = false;
		TableItemOrTreeItem topRow = table.getTopItem();
		if (topRow == null) {
			return;
		}
		for (int i = 0; i < columns.length; i++) {
			final TableColumnCore tc = (TableColumnCore) columns[i].getData("TableColumnCore");
			if (tc == null) {
				continue;
			}

			int position = tc.getPosition();
			if (position < 0 || position >= columnsVisible.length) {
				continue;
			}

			Rectangle size = topRow.getBounds(i);
			//System.out.println(sTableID + ": column " + i + ":" + tc.getName() + ": size="  + size + "; ca=" + clientArea + "; pos=" + position);
			size.intersect(clientArea);
			boolean nowVisible = !size.isEmpty();
			//System.out.println("  visible; was=" + columnsVisible[position] + "; now=" + nowVisible + ";doValidae=" + doInvalidate);
			if (columnsVisible[position] != nowVisible) {
				columnsVisible[position] = nowVisible;
				if (nowVisible && doInvalidate) {
					swt_runForVisibleRows(new TableGroupRowRunner() {
						public void run(TableRowCore row) {
							TableCellCore cell = row.getTableCellCore(tc.getName());
							if (cell != null) {
  							cell.invalidate();
  							cell.redraw();
							}
						}
					});
				}
			}
		}
	}

	public boolean isColumnVisible(
			org.gudy.azureus2.plugins.ui.tables.TableColumn column) {
		int position = column.getPosition();
		if (position < 0 || position >= columnsVisible.length) {
			return false;
		}
		return columnsVisible[position];

	}
	
	public boolean isUnfilteredDataSourceAdded(Object ds) {
		return listUnfilteredDataSources.contains(ds); 
	}

	protected void swt_initializeTableColumns(final TableOrTreeSWT table) {
		TableColumnOrTreeColumn[] oldColumns = table.getColumns();

		for (int i = 0; i < oldColumns.length; i++) {
			oldColumns[i].removeListener(SWT.Move, columnMoveListener);
		}

		for (int i = oldColumns.length - 1; i >= 0; i--) {
			oldColumns[i].dispose();
		}

		columnPaddingAdjusted = false;

		// Pre 3.0RC1 SWT on OSX doesn't call this!! :(
		ControlListener resizeListener = new ControlAdapter() {
			// Bug: getClientArea() eventually calls back to controlResized,
			//      creating a loop until a stack overflow
			private boolean bInFunction = false;

			public void controlResized(ControlEvent e) {
				TableColumnOrTreeColumn column = TableOrTreeUtils.getTableColumnEventItem(e.widget);
				if (column == null || column.isDisposed() || bInFunction) {
					return;
				}

				try {
					bInFunction = true;

					TableColumnCore tc = (TableColumnCore) column.getData("TableColumnCore");
					if (tc != null) {
						Long lPadding = (Long) column.getData("widthOffset");
						int padding = (lPadding == null) ? 0 : lPadding.intValue();
						int newWidth = column.getWidth();
						if (OBEY_COLUMN_MINWIDTH) {
  						int minWidth = tc.getMinWidth();
  						if (minWidth > 0 && newWidth - padding < minWidth) {
  							newWidth = minWidth + padding;
  							column.setWidth(minWidth);
  						}
						}
						tc.setWidth(newWidth - padding);
					}

					int columnNumber = table.indexOf(column);
					locationChanged(columnNumber);
				} finally {
					bInFunction = false;
				}
			}
		};

		// Add 1 to position because we make a non resizable 0-sized 1st column
		// to fix the 1st column gap problem (Eclipse Bug 43910)

		// SWT does not set 0 column width as expected in OS X; see bug 43910
		// this will be removed when a SWT-provided solution is available to satisfy all platforms with identation issue
		//bSkipFirstColumn = bSkipFirstColumn && !Constants.isOSX;

		if (bSkipFirstColumn) {
			TableColumnOrTreeColumn tc = table.createNewColumn(SWT.NULL);
			//tc.setWidth(useTree ? 25 : 0);
			tc.setWidth(0);
			tc.setResizable(false);
			tc.setMoveable(false);
		}

		TableColumnCore[] tmpColumnsOrdered = new TableColumnCore[tableColumns.length];
		//Create all columns
		int columnOrderPos = 0;
		Arrays.sort(tableColumns,
				TableColumnManager.getTableColumnOrderComparator());
		for (int i = 0; i < tableColumns.length; i++) {
			int position = tableColumns[i].getPosition();
			if (position != -1 && tableColumns[i].isVisible()) {
				table.createNewColumn(SWT.NULL);
				//System.out.println(i + "] " + tableColumns[i].getName() + ";" + position);
				tmpColumnsOrdered[columnOrderPos++] = tableColumns[i];
			}
		}
		int numSWTColumns = table.getColumnCount();
		int iNewLength = numSWTColumns - (bSkipFirstColumn ? 1 : 0);
		columnsOrdered = new TableColumnCore[iNewLength];
		System.arraycopy(tmpColumnsOrdered, 0, columnsOrdered, 0, iNewLength);
		columnsVisible = new boolean[tableColumns.length];

		ColumnSelectionListener columnSelectionListener = new ColumnSelectionListener();

		//Assign length and titles
		//We can only do it after ALL columns are created, as position (order)
		//may not be in the natural order (if the user re-order the columns).
		int swtColumnPos = (bSkipFirstColumn ? 1 : 0);
		for (int i = 0; i < tableColumns.length; i++) {
			TableColumnCore columnCore = tableColumns[i];
			int position = columnCore.getPosition();
			if (position == -1 || !columnCore.isVisible()) {
				continue;
			}

			columnsVisible[i] = false;

			String sName = columnCore.getName();
			// +1 for Eclipse Bug 43910 (see above)
			// user has reported a problem here with index-out-of-bounds - not sure why
			// but putting in a preventative check so that hopefully the view still opens
			// so they can fix it

			if (swtColumnPos >= numSWTColumns) {
				Debug.out("Incorrect table column setup, skipping column '" + sName
						+ "', position=" + swtColumnPos + ";numCols=" + numSWTColumns);
				continue;
			}

			TableColumnOrTreeColumn column = table.getColumn(swtColumnPos);
			try {
				column.setMoveable(true);
			} catch (NoSuchMethodError e) {
				// Ignore < SWT 3.1
			}
			column.setAlignment(TableColumnSWTUtils.convertColumnAlignmentToSWT(columnCore.getAlignment()));
			String iconReference = columnCore.getIconReference();
			if (iconReference != null) {
				Image image = ImageLoader.getInstance().getImage(iconReference);
				column.setImage(image);
			} else {
				Messages.setLanguageText(column.getColumn(), columnCore.getTitleLanguageKey());
			}
			if (!Constants.isUnix && !Utils.isCarbon) {
				column.setWidth(columnCore.getWidth());
			} else {
				column.setData("widthOffset", new Long(1));
				column.setWidth(columnCore.getWidth() + 1);
			}
			if (columnCore.getMinWidth() == columnCore.getMaxWidth()
					&& columnCore.getMinWidth() > 0) {
				column.setResizable(false);
			}
			column.setData("TableColumnCore", columnCore);
			column.setData("configName", "Table." + sTableID + "." + sName);
			column.setData("Name", sName);

			column.addControlListener(resizeListener);
			// At the time of writing this SWT (3.0RC1) on OSX doesn't call the 
			// selection listener for tables
			column.addListener(SWT.Selection, columnSelectionListener);
			

			swtColumnPos++;
		}

		// Initialize the sorter after the columns have been added
		TableColumnManager tcManager = TableColumnManager.getInstance();

		String sSortColumn = tcManager.getDefaultSortColumnName(sTableID);
		if (sSortColumn == null || sSortColumn.length() == 0) {
			sSortColumn = sDefaultSortOn;
		}

		TableColumnCore tc = tcManager.getTableColumnCore(sTableID, sSortColumn);
		if (tc == null && tableColumns.length > 0) {
			tc = tableColumns[0];
		}
		sortColumn = tc;
		fixAlignment(tc, true);
		swt_changeColumnIndicator();

		// Add move listener at the very end, so we don't get a bazillion useless 
		// move triggers
		TableColumnOrTreeColumn[] columns = table.getColumns();
		for (int i = 0; i < columns.length; i++) {
			TableColumnOrTreeColumn column = columns[i];
			column.addListener(SWT.Move, columnMoveListener);
		}

		columnVisibilitiesChanged = true;
	}

	public void fixAlignment(TableColumnCore tc, boolean sorted) {
		if (Constants.isOSX) {
			if (table.isDisposed() || tc == null) {
				return;
			}
			int[] columnOrder = table.getColumnOrder();
			int i = tc.getPosition() - (bSkipFirstColumn ? 1 : 0);
			if (i < 0 || i >= columnOrder.length) {
				return;
			}
			TableColumnOrTreeColumn swtColumn = table.getColumn(columnOrder[i]);
			if (swtColumn != null) {
				if (swtColumn.getAlignment() == SWT.RIGHT && sorted) {
					swtColumn.setText("   " + swtColumn.getText() + "   ");
				} else {
					swtColumn.setText(swtColumn.getText().trim());
				}
			}
		}
	}

	/** Creates the Context Menu.
	 * @param table 
	 *
	 * @return a new Menu object
	 */
	private Menu createMenu(final TableOrTreeSWT table) {
		if (!isMenuEnabled()) {
			return null;
		}
		
		final Menu menu = new Menu(shell, SWT.POP_UP);
		table.addListener(SWT.MenuDetect, new Listener() {
			public void handleEvent(Event event) {
				Point pt = event.display.map(null, table.getComposite(), new Point(event.x, event.y));
				boolean noRow = table.getItem(pt) == null;

				Rectangle clientArea = table.getClientArea();
				boolean inHeader = clientArea.y <= pt.y && pt.y < (clientArea.y + headerHeight);
				if (!noRow) {
					noRow = inHeader;
				}
				
				menu.setData("inBlankArea", (!inHeader && noRow));

				menu.setData("isHeader", new Boolean(noRow));

				int columnNo = getColumnNo(pt.x);
				menu.setData("column", columnNo < 0
						|| columnNo >= table.getColumnCount() ? null
						: table.getColumn(columnNo)); 
			}
		});
		MenuBuildUtils.addMaintenanceListenerForMenu(menu,
				new MenuBuildUtils.MenuBuilder() {
					public void buildMenu(Menu menu, MenuEvent menuEvent) {
						Object oIsHeader = menu.getData("isHeader");
						boolean isHeader = (oIsHeader instanceof Boolean)
								? ((Boolean) oIsHeader).booleanValue() : false;
						Object oInBlankArea = menu.getData("inBlankArea");
						boolean inBlankArea = (oInBlankArea instanceof Boolean)
								? ((Boolean) oInBlankArea).booleanValue() : false;

						TableColumnOrTreeColumn tcColumn = (TableColumnOrTreeColumn) menu.getData("column");
						
						if (isHeader) {
							fillColumnMenu(tcColumn, inBlankArea);
						} else {
							fillMenu(menu, tcColumn);
						}

					}
				});

		return menu;
	}

	/** Fill the Context Menu with items.  Called when menu is about to be shown.
	 *
	 * By default, a "Edit Columns" menu and a Column specific menu is set up.
	 *
	 * @param menu Menu to fill
	 * @param tcColumn 
	 */
	private void fillMenu(final Menu menu, final TableColumnOrTreeColumn tcColumn) {
		String columnName = tcColumn == null ? null : (String) tcColumn.getData("Name");

		Object[] listeners = listenersMenuFill.toArray();
		for (int i = 0; i < listeners.length; i++) {
			TableViewSWTMenuFillListener l = (TableViewSWTMenuFillListener) listeners[i];
			l.fillMenu(columnName, menu);
		}

		boolean hasLevel1 = false;
		boolean hasLevel2 = false;
		// quick hack so we don't show plugin menus on selections of subitems
		synchronized (selectedRows) {
  		for (TableRowCore row : selectedRows) {
  			if (row.getParentRowCore() != null) {
  				hasLevel2 = true;
  			} else {
  				hasLevel1 = true;
  			}
  		}
		}
		
		String sMenuID = hasLevel1 ? sTableID : TableManager.TABLE_TORRENT_FILES;
		
		// Add Plugin Context menus..
		boolean enable_items = table != null && table.getSelection().length > 0;

		TableContextMenuItem[] items = TableContextMenuManager.getInstance().getAllAsArray(
				sMenuID);

		// We'll add download-context specific menu items - if the table is download specific.
		// We need a better way to determine this...
		org.gudy.azureus2.plugins.ui.menus.MenuItem[] menu_items = null;
		if ("MySeeders".equals(sTableID) || "MyTorrents".equals(sTableID)) {
			menu_items = MenuItemManager.getInstance().getAllAsArray(
					"download_context");
		} else {
			menu_items = MenuItemManager.getInstance().getAllAsArray((String) null);
		}
		
		if (columnName == null) {
			MenuItem itemChangeTable = new MenuItem(menu, SWT.PUSH);
			Messages.setLanguageText(itemChangeTable,
					"MyTorrentsView.menu.editTableColumns");
			Utils.setMenuItemImage(itemChangeTable, "columns");

			itemChangeTable.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					showColumnEditor();
				}
			});

		} else {

  		MenuItem item = new MenuItem(menu, SWT.PUSH);
  		Messages.setLanguageText(item, "MyTorrentsView.menu.thisColumn.toClipboard");
  		item.addListener(SWT.Selection, new Listener() {
  			public void handleEvent(Event e) {
  				String sToClipboard = "";
  				if (tcColumn == null) {
  					return;
  				}
  				String columnName = (String) tcColumn.getData("Name");
  				if (columnName == null) {
  					return;
  				}
  				TableRowCore[] rows = getSelectedRows();
  				for (TableRowCore row : rows) {
  					if (row != rows[0]) {
  						sToClipboard += "\n";
  					}
  					TableCellCore cell = row.getTableCellCore(columnName);
  					if (cell != null) {
  						sToClipboard += cell.getClipboardText();
  					}
  				}
  				if (sToClipboard.length() == 0) {
  					return;
  				}
  				new Clipboard(mainComposite.getDisplay()).setContents(new Object[] {
  					sToClipboard
  				}, new Transfer[] {
  					TextTransfer.getInstance()
  				});
  			}
  		});
		}
		
		if (items.length > 0 || menu_items.length > 0) {
			new org.eclipse.swt.widgets.MenuItem(menu, SWT.SEPARATOR);

			// Add download context menu items.
			if (menu_items != null) {
				// getSelectedDataSources(false) returns us plugin items.
				MenuBuildUtils.addPluginMenuItems(getComposite(), menu_items, menu,
						true, true, new MenuBuildUtils.MenuItemPluginMenuControllerImpl(
								getSelectedDataSources(false)));
			}

			if (items.length > 0) {
				MenuBuildUtils.addPluginMenuItems(getComposite(), items, menu, true,
						enable_items, new MenuBuildUtils.PluginMenuController() {
							public Listener makeSelectionListener(
									final org.gudy.azureus2.plugins.ui.menus.MenuItem plugin_menu_item) {
								return new TableSelectedRowsListener(TableViewSWTImpl.this, false) {
									public boolean run(TableRowCore[] rows) {
										if (rows.length != 0) {
											((TableContextMenuItemImpl) plugin_menu_item).invokeListenersMulti(rows);
										}
										return true;
									}
								};
							}

							public void notifyFillListeners(
									org.gudy.azureus2.plugins.ui.menus.MenuItem menu_item) {
								((TableContextMenuItemImpl) menu_item).invokeMenuWillBeShownListeners(getSelectedRows());
							}
						});
			}
		}
		
		if (hasLevel1) {
		// Add Plugin Context menus..
		if (tcColumn != null) {
  		TableColumnCore tc = (TableColumnCore) tcColumn.getData("TableColumnCore");
  		TableContextMenuItem[] columnItems = tc.getContextMenuItems(TableColumnCore.MENU_STYLE_COLUMN_DATA);
  		if (columnItems.length > 0) {
  			new MenuItem(menu, SWT.SEPARATOR);
  
  			MenuBuildUtils.addPluginMenuItems(getComposite(), columnItems, menu,
  					true, true, new MenuBuildUtils.MenuItemPluginMenuControllerImpl(
  							getSelectedDataSources(true)));
  
  		}
		}

		if (filter != null) {
  		final MenuItem itemFilter = new MenuItem(menu, SWT.PUSH);
  		Messages.setLanguageText(itemFilter, "MyTorrentsView.menu.filter");
  		itemFilter.addListener(SWT.Selection, new Listener() {
  			public void handleEvent(Event event) {
  				openFilterDialog();
  			}
  		});
		}
		}
	}

	void showColumnEditor() {
		TableRowCore focusedRow = getFocusedRow();
		if (focusedRow == null || focusedRow.isRowDisposed()) {
			focusedRow = getRow(0);
		}
		new TableColumnSetupWindow(classPluginDataSourceType, sTableID, focusedRow,
				TableStructureEventDispatcher.getInstance(sTableID)).open();
	}

	/**
	 * SubMenu for column specific tasks. 
	 *
	 * @param iColumn Column # that tasks apply to.
	 */
	private void fillColumnMenu(final TableColumnOrTreeColumn tcColumn,
			boolean isBlankArea) {
		if (!isBlankArea) {
  		TableColumnManager tcm = TableColumnManager.getInstance();
  		TableColumnCore[] allTableColumns = tcm.getAllTableColumnCoreAsArray(
  				classPluginDataSourceType, sTableID);
  		
		Arrays.sort(allTableColumns,
				TableColumnManager.getTableColumnOrderComparator());

  		for (final TableColumnCore tc : allTableColumns) {
  			boolean visible = tc.isVisible();
  			if (!visible) {
  				TableColumnInfo columnInfo = tcm.getColumnInfo(classPluginDataSourceType, sTableID, tc.getName());
  				if (columnInfo.getProficiency() != TableColumnInfo.PROFICIENCY_BEGINNER) {
  					continue;
  				}
  			}
  			MenuItem menuItem = new MenuItem(menu, SWT.CHECK);
  			Messages.setLanguageText(menuItem, tc.getTitleLanguageKey());
  			if (visible) {
  				menuItem.setSelection(true);
  			}
  			menuItem.addListener(SWT.Selection, new Listener() {
  				public void handleEvent(Event e) {
  					tc.setVisible(!tc.isVisible());
  					tableStructureChanged(true, null);
  				}
  			});
  		}
		}

		if (menu.getItemCount() > 0) {
			new MenuItem(menu, SWT.SEPARATOR);
		}

		final MenuItem itemResetColumns = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemResetColumns, "table.columns.reset");
		itemResetColumns.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				TableColumnManager tcm = TableColumnManager.getInstance();
				String[] defaultColumnNames = tcm.getDefaultColumnNames(sTableID);
				if (defaultColumnNames != null) {
					for (TableColumnCore column : tableColumns) {
						column.setVisible(false);
					}
					int i = 0;
					for (String name : defaultColumnNames) {
						TableColumnCore column = tcm.getTableColumnCore(sTableID, name);
						if (column != null) {
							column.reset();
							column.setVisible(true);
							column.setPositionNoShift(i++);
						}
					}
					tcm.saveTableColumns(classPluginDataSourceType, sTableID);
					TableStructureEventDispatcher.getInstance(sTableID).tableStructureChanged(true, classPluginDataSourceType);
				}
			}
		});


		final MenuItem itemChangeTable = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemChangeTable,
				"MyTorrentsView.menu.editTableColumns");
		Utils.setMenuItemImage(itemChangeTable, "columns");

		itemChangeTable.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				showColumnEditor();
			}
		});

		if (menu != null) {
			menu.setData("column", tcColumn);
		}
		
		if (tcColumn == null) {
			return;
		}

		String sColumnName = (String) tcColumn.getData("Name");
		if (sColumnName != null) {
			Object[] listeners = listenersMenuFill.toArray();
			for (int i = 0; i < listeners.length; i++) {
				TableViewSWTMenuFillListener l = (TableViewSWTMenuFillListener) listeners[i];
				l.addThisColumnSubMenu(sColumnName, menu);
			}
		}

		final MenuItem at_item = new MenuItem(menu, SWT.CHECK);
		Messages.setLanguageText(at_item,
				"MyTorrentsView.menu.thisColumn.autoTooltip");
		at_item.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				TableColumnOrTreeColumn tc = (TableColumnOrTreeColumn) menu.getData("column");
				TableColumnCore tcc = (TableColumnCore) tc.getData("TableColumnCore");
				tcc.setAutoTooltip(at_item.getSelection());
				tcc.invalidateCells();
			}
		});
		at_item.setSelection(((TableColumnCore) tcColumn.getData("TableColumnCore")).doesAutoTooltip());


		// Add Plugin Context menus..
		TableColumnCore tc = (TableColumnCore) tcColumn.getData("TableColumnCore");
		TableContextMenuItem[] items = tc.getContextMenuItems(TableColumnCore.MENU_STYLE_HEADER);
		if (items.length > 0) {
			new MenuItem(menu, SWT.SEPARATOR);

			MenuBuildUtils.addPluginMenuItems(getComposite(), items, menu,
					true, true, new MenuBuildUtils.MenuItemPluginMenuControllerImpl(
							getSelectedDataSources(true)));

		}
	}

	/** IView.getComposite()
	 * @return the composite for this TableView
	 */
	public Composite getComposite() {
		return mainComposite;
	}

	public Composite getTableComposite() {
		return tableComposite;
	}
	
	public TableOrTreeSWT getTableOrTreeSWT() {
		return table;
	}

	public UISWTViewCore getActiveSubView() {
		if (!bEnableTabViews || tabFolder == null || tabFolder.isDisposed()
				|| tabFolder.getMinimized()) {
			return null;
		}

		CTabItem item = tabFolder.getSelection();
		if (item != null) {
			return (UISWTViewCore) item.getData("IView");
		}

		return null;
	}

	public void refreshSelectedSubView() {
		UISWTViewCore view = getActiveSubView();
		if (view != null && view.getComposite().isVisible()) {
			view.triggerEvent(UISWTViewEvent.TYPE_REFRESH, null);
		}
	}

	// see common.TableView
	public void refreshTable(final boolean bForceSort) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				swt_refreshTable(bForceSort);

				if (bEnableTabViews && tabFolder != null && !tabFolder.isDisposed()
						&& !tabFolder.getMinimized()) {
					refreshSelectedSubView();
				}
			}
		});

		triggerTableRefreshListeners();
	}

	private void swt_refreshTable(boolean bForceSort) {
		// don't refresh while there's no table
		if (table == null) {
			return;
		}

		// call to trigger invalidation if visibility changes
		isVisible();

		// XXX Try/Finally used to be there for monitor.enter/exit, however
		//     this doesn't stop re-entry from the same thread while already in
		//     process.. need a bAlreadyRefreshing variable instead
		try {
			if (getComposite() == null || getComposite().isDisposed()) {
				return;
			}

			if (columnVisibilitiesChanged == true) {
				swt_updateColumnVisibilities(true);
			}

			final boolean bDoGraphics = (loopFactor % graphicsUpdate) == 0;
			final boolean bWillSort = bForceSort || (reOrderDelay != 0)
					&& ((loopFactor % reOrderDelay) == 0);
			//System.out.println("Refresh.. WillSort? " + bWillSort);

			if (bWillSort) {
				if (bForceSort && sortColumn != null) {
					lLastSortedOn = 0;
					sortColumn.setLastSortValueChange(SystemTime.getCurrentTime());
				}
				_sortColumn(true, false, false);
			}

			long lTimeStart = SystemTime.getMonotonousTime();

			Utils.getOffOfSWTThread(new AERunnable() {
				public void runSupport() {
					//Refresh all visible items in table...
					runForAllRows(new TableGroupRowVisibilityRunner() {
						public void run(TableRowCore row, boolean bVisible) {
							row.refresh(bDoGraphics, bVisible);
						}
					});
				}
			});
			

			if (DEBUGADDREMOVE) {
				long lTimeDiff = (SystemTime.getMonotonousTime() - lTimeStart);
				if (lTimeDiff > 500) {
					debug(lTimeDiff + "ms to refresh rows");
				}
			}

			loopFactor++;
		} finally {
		}
	}

	private void swt_refreshVisibleRows() {
		if (getComposite() == null || getComposite().isDisposed()) {
			return;
		}

		swt_runForVisibleRows(new TableGroupRowRunner() {
			public void run(TableRowCore row) {
				row.refresh(false, true);
			}
		});
	}

	// see common.TableView
	public void processDataSourceQueue() { 
		Utils.getOffOfSWTThread(new AERunnable() {
			public void runSupport() {
				_processDataSourceQueue();
			}
		});
	}

	public void processDataSourceQueueSync() { 
		_processDataSourceQueue();
	}

	
	private void _processDataSourceQueue() { 
		Object[] dataSourcesAdd = null;
		Object[] dataSourcesRemove = null;

		try {
			dataSourceToRow_mon.enter();
			if (dataSourcesToAdd.size() > 0) {
				if (dataSourcesToAdd.removeAll(dataSourcesToRemove) && DEBUGADDREMOVE) {
					debug("Saved time by not adding a row that was removed");
				}
				dataSourcesAdd = dataSourcesToAdd.toArray();

				dataSourcesToAdd.clear();
			}

			if (dataSourcesToRemove.size() > 0) {
				dataSourcesRemove = dataSourcesToRemove.toArray();
				if (DEBUGADDREMOVE && dataSourcesRemove.length > 1) {
					debug("Streamlining removing " + dataSourcesRemove.length + " rows");
				}
				dataSourcesToRemove.clear();
			}
		} finally {
			dataSourceToRow_mon.exit();
		}

		if (dataSourcesAdd != null && dataSourcesAdd.length > 0) {
			reallyAddDataSources(dataSourcesAdd);
			if (DEBUGADDREMOVE && dataSourcesAdd.length > 1) {
				debug("Streamlined adding " + dataSourcesAdd.length + " rows");
			}
		}

		if (dataSourcesRemove != null && dataSourcesRemove.length > 0) {
			reallyRemoveDataSources(dataSourcesRemove);
		}
	}

	private void locationChanged(final int iStartColumn) {
		if (getComposite() == null || getComposite().isDisposed()) {
			return;
		}

		columnVisibilitiesChanged = true;

		runForAllRows(new TableGroupRowRunner() {
			public void run(TableRowCore row) {
				row.locationChanged(iStartColumn);
			}
		});
	}

	/*
	private void doPaint(final GC gc, final Rectangle dirtyArea) {
		if (getComposite() == null || getComposite().isDisposed()) {
			return;
		}

		swt_runForVisibleRows(new TableGroupRowRunner() {
			public void run(TableRowCore row) {
				if (!(row instanceof TableRowSWT)) {
					return;
				}
				TableRowSWT rowSWT = (TableRowSWT) row;
				Rectangle bounds = rowSWT.getBounds();
				if (bounds.intersects(dirtyArea)) {

					if (Constants.isWindowsVistaOrHigher) {
						Image imgBG = new Image(gc.getDevice(), bounds.width, bounds.height);
						gc.copyArea(imgBG, bounds.x, bounds.y);
						rowSWT.setBackgroundImage(imgBG);
					}

					//System.out.println("paint " + row);
					Color oldBG = (Color) row.getData("bgColor");
					Color newBG = rowSWT.getBackground();
					if (oldBG == null || !oldBG.equals(newBG)) {
						//System.out.println("redraw " + row + "; " + oldBG + ";" + newBG);
						row.invalidate();
						row.redraw();
						row.setData("bgColor", newBG);
					} else {
						rowSWT.doPaint(gc, true);
					}
				}
			}
		});
	}
	*/

	/** IView.delete: This method is called when the view is destroyed.
	 * Each color instanciated, images and such things should be disposed.
	 * The caller is the GUI thread.
	 */
	public void delete() {
		triggerLifeCycleListener(TableLifeCycleListener.EVENT_DESTROYED);

		if (tabViews != null && tabViews.size() > 0) {
			for (int i = 0; i < tabViews.size(); i++) {
				UISWTViewCore view = tabViews.get(i);
				if (view != null) {
      		view.triggerEvent(UISWTViewEvent.TYPE_DESTROY, null);
				}
			}
		}

		TableStructureEventDispatcher.getInstance(sTableID).removeListener(this);
		TableColumnManager tcManager = TableColumnManager.getInstance();
		if (tcManager != null) {
			tcManager.saveTableColumns(classPluginDataSourceType, sTableID);
		}

		if (table != null && !table.isDisposed()) {
			table.dispose();
		}
		removeAllTableRows();
		configMan.removeParameterListener("ReOrder Delay", this);
		configMan.removeParameterListener("Graphics Update", this);
		Colors.getInstance().removeColorsChangedListener(this);

		processDataSourceQueueCallback = null;

		//oldSelectedItems =  null;
		Composite comp = getComposite();
		if (comp != null && !comp.isDisposed()) {
			comp.dispose();
		}
		
		MessageText.removeListener(this);
	}

	// see common.TableView
	public void addDataSource(DATASOURCETYPE dataSource) {
		addDataSource(dataSource, false);
	}

	private void addDataSource(DATASOURCETYPE dataSource, boolean skipFilterCheck) {

		if (dataSource == null) {
			return;
		}

		listUnfilteredDatasources_mon.enter();
		try {
			listUnfilteredDataSources.add(dataSource);
		} finally {
			listUnfilteredDatasources_mon.exit();
		}

		if (!skipFilterCheck && filter != null
				&& !filter.checker.filterCheck(dataSource, filter.text, filter.regex)) {
			return;
		}

		if (Utils.IMMEDIATE_ADDREMOVE_DELAY == 0) {
			reallyAddDataSources(new Object[] {
				dataSource
			});
			return;
		}

		// In order to save time, we cache entries to be added and process them
		// in a refresh cycle.  This is a huge benefit to tables that have
		// many rows being added and removed in rapid succession

		try {
			dataSourceToRow_mon.enter();

				if ( dataSourcesToRemove.remove( dataSource )){
					// we're adding, override any pending removal
					if (DEBUGADDREMOVE) {
						debug("AddDS: Removed from toRemove.  Total Removals Queued: " + dataSourcesToRemove.size());
					}
				}

				if ( dataSourcesToAdd.contains(dataSource)){
					// added twice.. ensure it's not in the remove list
					if (DEBUGADDREMOVE) {
						debug("AddDS: Already There.  Total Additions Queued: " + dataSourcesToAdd.size());
					}
				} else {
					dataSourcesToAdd.add(dataSource);
					if (DEBUGADDREMOVE) {
						debug("Queued 1 dataSource to add.  Total Additions Queued: " + dataSourcesToAdd.size() + "; already=" + sortedRows.size());
					}
					refreshenProcessDataSourcesTimer();
				}

		} finally {

			dataSourceToRow_mon.exit();
		}
	}

	// see common.TableView
	public void addDataSources(final DATASOURCETYPE dataSources[]) {
		addDataSources(dataSources, false);
	}

	public void addDataSources(final DATASOURCETYPE dataSources[],
			boolean skipFilterCheck) {

		if (dataSources == null) {
			return;
		}

		listUnfilteredDatasources_mon.enter();
		try {
			listUnfilteredDataSources.addAll(Arrays.asList(dataSources));
		} finally {
			listUnfilteredDatasources_mon.exit();
		}

		if (Utils.IMMEDIATE_ADDREMOVE_DELAY == 0) {
			if (!skipFilterCheck && filter!= null) {
  			for (int i = 0; i < dataSources.length; i++) {
  				if (!filter.checker.filterCheck(dataSources[i], filter.text,
  						filter.regex)) {
  					dataSources[i] = null;
  				}
  			}
			}
			reallyAddDataSources(dataSources);
			return;
		}

		// In order to save time, we cache entries to be added and process them
		// in a refresh cycle.  This is a huge benefit to tables that have
		// many rows being added and removed in rapid succession

		try {
			dataSourceToRow_mon.enter();

			int count = 0;

			for (int i = 0; i < dataSources.length; i++) {
				DATASOURCETYPE dataSource = dataSources[i];
				if (dataSource == null) {
					continue;
				}
				if (!skipFilterCheck
						&& filter != null
						&& !filter.checker.filterCheck(dataSource, filter.text,
								filter.regex)) {
					continue;
				}
				dataSourcesToRemove.remove(dataSource);	// may be pending removal, override

				if (dataSourcesToAdd.contains(dataSource)){
				} else {
					count++;
					dataSourcesToAdd.add(dataSource);
				}
			}

			if (DEBUGADDREMOVE) {
				debug("Queued " + count + " of " + dataSources.length
						+ " dataSources to add.  Total Queued: " + dataSourcesToAdd.size());
			}

		} finally {

			dataSourceToRow_mon.exit();
		}

		refreshenProcessDataSourcesTimer();
	}

	private void refreshenProcessDataSourcesTimer() {
		if (bReallyAddingDataSources || processDataSourceQueueCallback == null) {
			// when processDataSourceQueueCallback is null, we are disposing
			return;
		}

		if (cellEditNotifier != null) {
			cellEditNotifier.sourcesChanged();
		}

		boolean processQueueImmediately = Utils.addDataSourceAggregated(processDataSourceQueueCallback);

		if (processQueueImmediately) {
			processDataSourceQueue();
		}
	}

	private void reallyAddDataSources(final Object dataSources[]) {
		// Note: We assume filterCheck has already run, and the list of dataSources
		//       all passed the filter
		
		if (mainComposite == null || table == null || mainComposite.isDisposed()
				|| table.isDisposed()) {
			return;
		}

		bReallyAddingDataSources = true;
		if (DEBUGADDREMOVE) {
			debug(">>" + " Add " + dataSources.length + " rows;");
		}

		// Create row, and add to map immediately
		try {
			dataSourceToRow_mon.enter();

			//long lStartTime = SystemTime.getCurrentTime();

			for (int i = 0; i < dataSources.length; i++) {
				if (dataSources[i] == null) {
					continue;
				}

				if (mapDataSourceToRow.containsKey(dataSources[i])) {
					dataSources[i] = null;
				} else {
					TableRowImpl row = new TableRowImpl(this, table, columnsOrdered,
							dataSources[i], bSkipFirstColumn);
					mapDataSourceToRow.put((DATASOURCETYPE) dataSources[i], row);
				}
			}
		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "Error while added row to Table "
					+ sTableID, e));
		} finally {
			dataSourceToRow_mon.exit();
		}

		if (DEBUGADDREMOVE) {
			debug("--" + " Add " + dataSources.length + " rows;");
		}

		addDataSourcesToSWT(dataSources, true);
	}

	private void addDataSourcesToSWT(final Object dataSources[], boolean async) {
		try {
			if (isDisposed()) {
				return;
			}
			if (DEBUGADDREMOVE) {
				debug("--" + " Add " + dataSources.length + " rows to SWT "
						+ (async ? " async " : " NOW"));
			}

			
			if (async) {
				Utils.execSWTThreadLater(0, new AERunnable() {
					public void runSupport() {
						_addDataSourcesToSWT(dataSources);
					}
				});
			} else {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						_addDataSourcesToSWT(dataSources);
					}
				}, false);
			}

			for (int i = 0; i < dataSources.length; i++) {
				Object dataSource = dataSources[i];
				if (dataSource == null) {
					continue;
				}
  			TableRowImpl row = (TableRowImpl) mapDataSourceToRow.get(dataSource);
  			if (row != null && sortColumn != null) {
  				TableCellCore cell = row.getTableCellCore(sortColumn.getName());
  				if (cell != null) {
  					try {
  						cell.invalidate();
  						cell.refresh(true);
  					} catch (Exception e) {
  						Logger.log(new LogEvent(LOGID,
  								"Minor error adding a row to table " + sTableID, e));
  					}
  				}
  			}
			}

		} catch (Exception e) {
			bReallyAddingDataSources = false;
			e.printStackTrace();
		}
	}

	private void _addDataSourcesToSWT(final Object dataSources[]) {
		if (table == null || table.isDisposed()) {
			bReallyAddingDataSources = false;
			return;
		}

		mainComposite.getParent().setCursor(
				table.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));

		TableRowCore[] selectedRows = getSelectedRows();
			
		int	rows_added = 0;
		
		boolean bReplacedVisible = false;
		boolean bWas0Rows = table.getItemCount() == 0;
		try {
			dataSourceToRow_mon.enter();
			sortedRows_mon.enter();

			if (DEBUGADDREMOVE) {
				debug("--" + " Add " + dataSources.length + " rows to SWT");
			}

			// purposefully not included in time check 
			if (!Constants.isWindows) {
				// Bug in Windows (7).  If you add 10 rows by setItemCount,
				// Windows will do some crappy shifting down of the non-row area
				table.setItemCount(sortedRows.size() + dataSources.length);
			}

			long lStartTime = SystemTime.getCurrentTime();
			int iTopIndex = table.getTopIndex();
			int iBottomIndex = Utils.getTableBottomIndex(table, iTopIndex);
			
			// add to sortedRows list in best position.  
			// We need to be in the SWT thread because the rowSorter may end up
			// calling SWT objects.
			for (int i = 0; i < dataSources.length; i++) {
				Object dataSource = dataSources[i];
				if (dataSource == null) {
					continue;
				}

				TableRowImpl row = (TableRowImpl) mapDataSourceToRow.get(dataSource);
				// We used to check if row already existed in sortedRows, but this
				// was always false, assuming dataSources only contains newly created
				// rows
				// ABOVE IS WRONG! It's not always false.  There's a case where
				// table is filled, cleared, filled again
				if ((row == null) || row.isRowDisposed() || sortedRows.indexOf(row) >= 0) {
				//if (row == null || row.isRowDisposed()) {
					continue;
				}
//				if (sortColumn != null) {
//					TableCellCore cell = row.getTableCellCore(sortColumn.getName());
//					if (cell != null) {
//						try {
//							cell.invalidate();
//							cell.refresh(true);
//						} catch (Exception e) {
//							Logger.log(new LogEvent(LOGID,
//									"Minor error adding a row to table " + sTableID, e));
//						}
//					}
//				}

				try {
					int index = 0;
					if (sortedRows.size() > 0) {
						// If we are >= to the last item, then just add it to the end
						// instead of relying on binarySearch, which may return an item
						// in the middle that also is equal.
						TableRowSWT lastRow = sortedRows.get(sortedRows.size() - 1);
						if (sortColumn == null || sortColumn.compare(row, lastRow) >= 0) {
							index = sortedRows.size();
							sortedRows.add(row);
							if (DEBUGADDREMOVE) {
								debug("Adding new row to bottom");
							}
						} else {
							index = Collections.binarySearch(sortedRows, row, sortColumn);
							if (index < 0) {
								index = -1 * index - 1; // best guess
							}

							if (index > sortedRows.size()) {
								index = sortedRows.size();
							}

							if (DEBUGADDREMOVE) {
								debug("Adding new row at position " + index + " of "
										+ (sortedRows.size() - 1));
							}
							sortedRows.add(index, row);
						}
					} else {
						if (DEBUGADDREMOVE) {
							debug("Adding new row to bottom (1st Entry)");
						}
						index = sortedRows.size();
						sortedRows.add(row);
					}

					// NOTE: if the listener tries to do something like setSelected,
					// it will fail because we aren't done adding.
					// we should trigger after fillRowGaps()
					
					rows_added++;
					
					triggerListenerRowAdded(row);

					if (!bReplacedVisible
							&& ((index >= iTopIndex && index <= iBottomIndex) || (index == sortedRows.size() - 1))) {
						bReplacedVisible = true;
					}

					// XXX Don't set table item here, it will mess up selected rows
					//     handling (which is handled in fillRowGaps called later on)
					//row.setTableItem(index);
					row.setIconSize(ptIconSize);
				} catch (Exception e) {
					Logger.log(new LogEvent(LOGID, "Error adding a row to table "
							+ sTableID, e));
					try {
						if (!sortedRows.contains(row)) {
							sortedRows.add(row);
						}
					} catch (Exception e2) {
						Debug.out(e2);
					}
				}
			} // for dataSources

			if (DEBUGADDREMOVE) {
				debug("Adding took " + (SystemTime.getCurrentTime() - lStartTime)
						+ "ms");
			}

			// Sanity Check: Make sure # of rows in table and in array match
			if (table.getItemCount() != sortedRows.size()) {
				// This could happen if one of the datasources was null, or
				// an error occured
				table.setItemCount(sortedRows.size());
			}
			if (sortedRows.size() == 1) {
				columnVisibilitiesChanged = true;
			}

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "Error while adding row to Table "
					+ sTableID, e));
		} finally {
			sortedRows_mon.exit();
			dataSourceToRow_mon.exit();

			bReallyAddingDataSources = false;
			refreshenProcessDataSourcesTimer();
		}

		fillRowGaps(false);
		if (bReplacedVisible) {
			visibleRowsChanged();
		}

		if (!columnPaddingAdjusted && table.getItemCount() > 0 && bWas0Rows) {
			TableColumnOrTreeColumn[] tableColumnsSWT = table.getColumns();
			TableItemOrTreeItem item = table.getItem(0);
			// on *nix, the last column expands to fill remaining space.. let's just not touch it
			int len = Constants.isUnix ? tableColumnsSWT.length - 1
					: tableColumnsSWT.length;
			for (int i = 0; i < len; i++) {
				TableColumnCore tc = (TableColumnCore) tableColumnsSWT[i].getData("TableColumnCore");
				if (tc != null) {
					boolean foundOne = false;

					Rectangle bounds = item.getBounds(i);
					int tcWidth = tc.getWidth();
					if (tcWidth != 0 && bounds.width != 0) {
						Object oOldOfs = tableColumnsSWT[i].getData("widthOffset");
						int oldOfs = (oOldOfs instanceof Number) ? ((Number)oOldOfs).intValue() : 0;
						int ofs = tc.getWidth() - bounds.width + oldOfs;
						if (ofs > 0 && ofs != oldOfs) {
							foundOne = true;
							tableColumnsSWT[i].setResizable(true);
							tableColumnsSWT[i].setData("widthOffset", new Long(ofs + oldOfs));
						}
					}
					if (foundOne) {
						tc.triggerColumnSizeChange();
					}
				}
			}
			columnPaddingAdjusted = true;
		} 
		if (bWas0Rows) {
			swt_updateColumnVisibilities(false);
		}

		setSelectedRows(selectedRows);
		if (DEBUGADDREMOVE) {
			debug("<< " + sortedRows.size());
		}

		mainComposite.getParent().setCursor(null);
		
		if ( rows_added > 0 ){
			
			tableMutated();
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#removeDataSource(java.lang.Object)
	public void removeDataSource(final DATASOURCETYPE dataSource) {
		if (dataSource == null) {
			return;
		}

		listUnfilteredDatasources_mon.enter();
		try {
			listUnfilteredDataSources.remove(dataSource);
		} finally {
			listUnfilteredDatasources_mon.exit();
		}

		if (Utils.IMMEDIATE_ADDREMOVE_DELAY == 0) {
			reallyRemoveDataSources(new Object[]{dataSource});
			return;
		}

		try {
			dataSourceToRow_mon.enter();

			dataSourcesToAdd.remove(dataSource);	// override any pending addition
			dataSourcesToRemove.add(dataSource);

			if (DEBUGADDREMOVE) {
				debug("Queued 1 dataSource to remove.  Total Queued: " + dataSourcesToRemove.size());
			}
		} finally {
			dataSourceToRow_mon.exit();
		}

		refreshenProcessDataSourcesTimer();
	}

	/** Remove the specified dataSource from the table.
	 *
	 * @param dataSources data sources to be removed
	 * @param bImmediate Remove immediately, or queue and remove at next refresh
	 */
	public void removeDataSources(final DATASOURCETYPE[] dataSources) {
		if (dataSources == null || dataSources.length == 0) {
			return;
		}

		listUnfilteredDatasources_mon.enter();
		try {
			listUnfilteredDataSources.removeAll(Arrays.asList(dataSources));
		} finally {
			listUnfilteredDatasources_mon.exit();
		}

		if (Utils.IMMEDIATE_ADDREMOVE_DELAY == 0) {
			reallyRemoveDataSources(dataSources);
			return;
		}

		try {
			dataSourceToRow_mon.enter();

			for (int i = 0; i < dataSources.length; i++) {
				DATASOURCETYPE dataSource = dataSources[i];
				dataSourcesToAdd.remove(dataSource);	// override any pending addition
				dataSourcesToRemove.add(dataSource);
			}

			if (DEBUGADDREMOVE) {
				debug("Queued " + dataSources.length
						+ " dataSources to remove.  Total Queued: "
						+ dataSourcesToRemove.size());
			}
		} finally {
			dataSourceToRow_mon.exit();
		}

		refreshenProcessDataSourcesTimer();
	}

	private void reallyRemoveDataSources(final Object[] dataSources) {

		if (DEBUGADDREMOVE) {
			debug(">> Remove rows");
		}

		final long lStart = SystemTime.getCurrentTime();

		boolean ok = Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				swt_reallyRemoveDataSources(dataSources, lStart);
			}
		});

		if (!ok) {
			// execRunnable will only fail if we are closing
			for (int i = 0; i < dataSources.length; i++) {
				if (dataSources[i] == null) {
					continue;
				}

				TableRowSWT item = (TableRowSWT) mapDataSourceToRow.get(dataSources[i]);
				mapDataSourceToRow.remove(dataSources[i]);
				if (item != null) {
					sortedRows.remove(item);
				}
			}

			if (DEBUGADDREMOVE) {
				debug("<< Remove row(s), noswt");
			}
		}
	}
	
	private void swt_reallyRemoveDataSources(Object[] dataSources, long lStart) {
		if (table == null || table.isDisposed()) {
			return;
		}

		TableRowCore[] oldSelectedRows = getSelectedRows();

		mainComposite.getParent().setCursor(
				table.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));

		int rows_removed = 0;
		
		try {
			StringBuffer sbWillRemove = null;
			if (DEBUGADDREMOVE) {
				debug(">>> Remove rows.  Start w/" + mapDataSourceToRow.size()
						+ "ds; tc=" + table.getItemCount() + ";"
						+ (SystemTime.getCurrentTime() - lStart) + "ms wait");

				sbWillRemove = new StringBuffer("Will soon remove row #");
			}

			ArrayList<TableRowSWT> itemsToRemove = new ArrayList<TableRowSWT>();
			ArrayList<Long> swtItemsToRemove = new ArrayList<Long>();
			int iTopIndex = table.getTopIndex();
			int iBottomIndex = Utils.getTableBottomIndex(table, iTopIndex);
			boolean bRefresh = false;

			if (DEBUGADDREMOVE) {
				debug("--- Remove: vis rows " + iTopIndex + " to " + iBottomIndex);
			}

			// pass one: get the SWT indexes of the items we are going to remove
			//           This will re-link them if they lost their link
			for (int i = 0; i < dataSources.length; i++) {
				if (dataSources[i] == null) {
					continue;
				}

				TableRowSWT item = (TableRowSWT) mapDataSourceToRow.get(dataSources[i]);
				if (item != null) {
					// use sortedRows position instead of item.getIndex(), because
					// getIndex may have a wrong value (unless we fillRowGaps() which
					// is more time consuming and we do afterwards anyway)
					int index = sortedRows.indexOf(item);
					if (!bRefresh) {
						bRefresh = index >= iTopIndex && index <= iBottomIndex;
					}
					if (DEBUGADDREMOVE) {
						if (i != 0) {
							sbWillRemove.append(", ");
						}
						sbWillRemove.append(index);
					}
					if (index >= 0) {
						swtItemsToRemove.add(new Long(index));
					}
				}
			}

			if (DEBUGADDREMOVE) {
				debug(sbWillRemove.toString());
				debug("#swtItemsToRemove=" + swtItemsToRemove.size());
			}

			int numRemovedHavingSelection = 0;
			// pass 2: remove from map and list, add removed to seperate list
			for (int i = 0; i < dataSources.length; i++) {
				if (dataSources[i] == null) {
					continue;
				}

				// Must remove from map before deleted from gui
				TableRowSWT item = (TableRowSWT) mapDataSourceToRow.remove(dataSources[i]);
				if (item != null) {
					if (item.isSelected()) {
						numRemovedHavingSelection++;
					}
					itemsToRemove.add(item);
					sortedRows.remove(item);
					triggerListenerRowRemoved(item);
					
					rows_removed++;
				}
			}

			if (DEBUGADDREMOVE) {
				debug("-- Removed from map and list");
			}
			// Remove the rows from SWT first.  On SWT 3.2, this currently has 
			// zero perf gain, and a small perf gain on Windows.  However, in the
			// future it may be optimized.
			if (swtItemsToRemove.size() > 0) {
				//					int[] swtRowsToRemove = new int[swtItemsToRemove.size()];
				//					for (int i = 0; i < swtItemsToRemove.size(); i++) {
				//						swtRowsToRemove[i] = ((Long) swtItemsToRemove.get(i)).intValue();
				//					}
				//					table.remove(swtRowsToRemove);
				// refreshVisibleRows should fix up the display
				table.setItemCount(mapDataSourceToRow.size());
				// Bug in Cocoa SWT: On setItemCOunt(0), table doesn't do
				// a repaint so the rows appear to still be there.
				if (Utils.isCocoa && mapDataSourceToRow.size() == 0) {
					table.redraw();
				}
			}

			if (DEBUGADDREMOVE) {
				debug("-- Removed from SWT");
			}

			// Finally, delete the rows
			for (Iterator<TableRowSWT> iter = itemsToRemove.iterator(); iter.hasNext();) {
				TableRowCore row = iter.next();
				row.delete();
			}

			if (bRefresh) {
				visibleRowsChanged();
				fillRowGaps(false);
				swt_refreshVisibleRows();
				if (DEBUGADDREMOVE) {
					debug("-- Fill row gaps and refresh after remove");
				}
			}

			if (DEBUGADDREMOVE) {
				debug("<< Remove " + itemsToRemove.size() + " rows. now "
						+ mapDataSourceToRow.size() + "ds; tc=" + table.getItemCount());
			}

			// if we removed all selected rows, select a row closest to the
			// first one
			/**  This is bad if the row was auto-removed and we select a new
			 * row that the user doesn't know about, and then he does some bad
			 * command to it.
			if (numRemovedHavingSelection == numSelected && numSelected >= 0
					&& oldSelection.length > 0 && oldSelection[0] < table.getItemCount()
					&& oldSelection[0] < sortedRows.size()) {
				oldSelectedRows = new TableRowCore[] {
					sortedRows.get(oldSelection[0])
				};
				setSelectedRows(oldSelectedRows);
				triggerSelectionListeners(getSelectedRows());
				return;
			}
			*/
			if (oldSelectedRows.length > 0) {
				setSelectedRows(oldSelectedRows);
			}
		} finally {
			mainComposite.getParent().setCursor(null);
		}
		
		if ( rows_removed > 0 ){
			
			tableMutated();
		}
	}
	
	// from common.TableView
	public void removeAllTableRows() {
		long lTimeStart = System.currentTimeMillis();

		final TableRowCore[] rows = getRows();

		try {
			dataSourceToRow_mon.enter();
			sortedRows_mon.enter();

			mapDataSourceToRow.clear();
			sortedRows.clear();

			dataSourcesToAdd.clear();
			dataSourcesToRemove.clear();

			if (DEBUGADDREMOVE) {
				debug("removeAll");
			}

		} finally {

			sortedRows_mon.exit();
			dataSourceToRow_mon.exit();
		}

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (DEBUGADDREMOVE) {
					debug("removeAll (SWT)");
				}

				if (table != null && !table.isDisposed()) {
					table.removeAll();
				}

				// Image Disposal handled by each cell

				for (int i = 0; i < rows.length; i++) {
					rows[i].delete();
				}
			}
		});

		if (DEBUGADDREMOVE) {
			long lTimeDiff = (System.currentTimeMillis() - lTimeStart);
			if (lTimeDiff > 10) {
				debug("RemovaAll took " + lTimeDiff + "ms");
			}
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getTableID()
	public String getTableID() {
		return sTableID;
	}

	/* ParameterListener Implementation */

	public void parameterChanged(String parameterName) {
		if (parameterName == null || parameterName.equals("Graphics Update")) {
			graphicsUpdate = configMan.getIntParameter("Graphics Update");
		}
		if (parameterName == null || parameterName.equals("ReOrder Delay")) {
			reOrderDelay = configMan.getIntParameter("ReOrder Delay");
		}
		if (parameterName == null || parameterName.startsWith("Color")) {
			tableInvalidate();
		}
	}

	// ITableStructureModificationListener
	public void tableStructureChanged(final boolean columnAddedOrRemoved,
			Class forPluginDataSourceType) {
		if (forPluginDataSourceType == null
				|| forPluginDataSourceType.equals(classPluginDataSourceType)) {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					if (table.isDisposed()) {
						return;
					}
  				_tableStructureChanged(columnAddedOrRemoved);
  			}
  		});
		}
	}
	
	private void _tableStructureChanged(boolean columnAddedOrRemoved) {
		triggerLifeCycleListener(TableLifeCycleListener.EVENT_DESTROYED);

		removeAllTableRows();

		if (columnAddedOrRemoved) {
			tableColumns = TableColumnManager.getInstance().getAllTableColumnCoreAsArray(
					classPluginDataSourceType, sTableID);
		}

		swt_initializeTableColumns(table);
		refreshTable(false);

		triggerLifeCycleListener(TableLifeCycleListener.EVENT_INITIALIZED);
	}

	// ITableStructureModificationListener
	public void columnOrderChanged(final int[] positions) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				try {
					if (table.isDisposed()) {
						return;
					}
					table.setColumnOrder(positions);
					swt_updateColumnVisibilities(true);
				} catch (NoSuchMethodError e) {
					// Pre SWT 3.1
					// This shouldn't really happen, since this function only gets triggered
					// from SWT >= 3.1
					tableStructureChanged(false, null);
				}
			}
		});
	}

	/** 
	 * The Columns width changed
	 */
	// ITableStructureModificationListener
	public void columnSizeChanged(final TableColumnCore tableColumn) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				swt_columnSizeChanged(tableColumn);
			}
		});
	}

	public void swt_columnSizeChanged(TableColumnCore tableColumn) {
		int newWidth = tableColumn.getWidth();
		if (table == null || table.isDisposed()) {
			return;
		}

		TableColumnOrTreeColumn column = null;
		TableColumnOrTreeColumn[] tableColumnsSWT = table.getColumns();
		for (int i = 0; i < tableColumnsSWT.length; i++) {
			if (tableColumnsSWT[i].getData("TableColumnCore") == tableColumn) {
				column = tableColumnsSWT[i];
				break;
			}
		}
		if (column == null) {
			return;
		}
		Long lOfs = (Long) column.getData("widthOffset");
		if (lOfs != null) {
			newWidth += lOfs.intValue();
		}
		swt_refreshVisibleRows();
		if (column.isDisposed() || (column.getWidth() == newWidth)) {
			return;
		}

		if (Constants.isUnix) {
			final int fNewWidth = newWidth;
			final TableColumnOrTreeColumn fTableColumn = column;
			column.getDisplay().asyncExec(new AERunnable() {
				public void runSupport() {
					if (!fTableColumn.isDisposed()) {
						fTableColumn.setWidth(fNewWidth);
					}
				}
			});
		} else {
			column.setWidth(newWidth);
		}
	}

	// ITableStructureModificationListener
	// TableView
	public void columnInvalidate(TableColumnCore tableColumn) {
		// We are being called from a plugin (probably), so we must refresh
		columnInvalidate(tableColumn, true);
	}

	// @see com.aelitis.azureus.ui.common.table.TableStructureModificationListener#cellInvalidate(com.aelitis.azureus.ui.common.table.TableColumnCore, java.lang.Object)
	public void cellInvalidate(TableColumnCore tableColumn,
			DATASOURCETYPE data_source) {
		cellInvalidate(tableColumn, data_source, true);
	}

	public void columnRefresh(TableColumnCore tableColumn) {
		final String sColumnName = tableColumn.getName();
		runForAllRows(new TableGroupRowVisibilityRunner() {
			public void run(TableRowCore row, boolean bVisible) {
				TableCellCore cell = row.getTableCellCore(sColumnName);
				if (cell != null) {
					cell.refresh(true, bVisible);
				}
			}
		});
	}

	/**
	 * Invalidate and refresh whole table
	 */
	public void tableInvalidate() {
		runForAllRows(new TableGroupRowVisibilityRunner() {
			public void run(TableRowCore row, boolean bVisible) {
				row.invalidate();
				row.refresh(true, bVisible);
			}
		});
	}

	// see common.TableView
	public void columnInvalidate(final String sColumnName) {
		TableColumnCore tc = TableColumnManager.getInstance().getTableColumnCore(
				sTableID, sColumnName);
		if (tc != null) {
			columnInvalidate(tc, tc.getType() == TableColumnCore.TYPE_TEXT_ONLY);
		}
	}

	public void columnInvalidate(TableColumnCore tableColumn,
			final boolean bMustRefresh) {
		final String sColumnName = tableColumn.getName();

		runForAllRows(new TableGroupRowRunner() {
			public void run(TableRowCore row) {
				TableCellCore cell = row.getTableCellCore(sColumnName);
				if (cell != null) {
					cell.invalidate(bMustRefresh);
				}
			}
		});
		lLastSortedOn = 0;
		tableColumn.setLastSortValueChange(SystemTime.getCurrentTime());
	}

	public void cellInvalidate(TableColumnCore tableColumn,
			final DATASOURCETYPE data_source, final boolean bMustRefresh) {
		final String sColumnName = tableColumn.getName();

		runForAllRows(new TableGroupRowRunner() {
			public void run(TableRowCore row) {
				TableCellCore cell = row.getTableCellCore(sColumnName);
				if (cell != null && cell.getDataSource() != null
						&& cell.getDataSource().equals(data_source)) {
					cell.invalidate(bMustRefresh);
				}
			}
		});
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getColumnCells(java.lang.String)
	public TableCellCore[] getColumnCells(String sColumnName) {
		TableCellCore[] cells = new TableCellCore[sortedRows.size()];

		try {
			sortedRows_mon.enter();

			int i = 0;
			for (Iterator<TableRowSWT> iter = sortedRows.iterator(); iter.hasNext();) {
				TableRowCore row = iter.next();
				cells[i++] = row.getTableCellCore(sColumnName);
			}

		} finally {
			sortedRows_mon.exit();
		}

		return cells;
	}

	public org.gudy.azureus2.plugins.ui.tables.TableColumn getTableColumn(
			String sColumnName) {
		for (int i = 0; i < tableColumns.length; i++) {
			TableColumnCore tc = tableColumns[i];
			if (tc.getName().equals(sColumnName)) {
				return tc;
			}
		}
		return null;
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getRows()
	public TableRowCore[] getRows() {
		try {
			sortedRows_mon.enter();

			return sortedRows.toArray(new TableRowCore[0]);

		} finally {
			sortedRows_mon.exit();
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getRow(java.lang.Object)
	public TableRowCore getRow(DATASOURCETYPE dataSource) {
		return mapDataSourceToRow.get(dataSource);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableViewSWT#getRowSWT(java.lang.Object)
	public TableRowSWT getRowSWT(DATASOURCETYPE dataSource) {
		return (TableRowSWT) mapDataSourceToRow.get(dataSource);
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getRow(int)
	public TableRowCore getRow(int iPos) {
		try {
			sortedRows_mon.enter();

			if (iPos >= 0 && iPos < sortedRows.size()) {
				TableRowCore row = sortedRows.get(iPos);

				if (row.getIndex() != iPos && Utils.isThisThreadSWT()) {
					row.setTableItem(iPos);
				}
				return row;
			}
		} finally {
			sortedRows_mon.exit();
		}
		return null;
	}

	protected TableRowCore getRowQuick(int iPos) {
		try {
			return sortedRows.get(iPos);
		} catch (Exception e) {
			return null;
		}
	}

	public int indexOf(TableRowCore row) {
		if (!Utils.isThisThreadSWT()) {
			return sortedRows.indexOf(row);
		}
		int i = ((TableRowImpl) row).getRealIndex();
		if (i == -1) {
			i = sortedRows.indexOf(row);
			if (i >= 0) {
				row.setTableItem(i);
			}
		}
		return i;
	}

	/** Warning: this method may require SWT Thread! 
	 * 
	 * TODO: Make sure callers are okay with that
	 */
	protected TableRowCore getRow(TableItemOrTreeItem item) {
		if (item == null) {
			return null;
		}
		try {
			Object o = item.getData("TableRow");
			if ((o instanceof TableRowCore) && !((TableRowCore) o).isRowDisposed()) {
				return (TableRowCore) o;
			}

			if (item.getParentItem() != null) {
				TableRowCore row = getRow(item.getParentItem());
				return row.linkSubItem(item.getParentItem().indexOf(item));
			}
			
			int iPos = table.indexOf(item);
			//System.out.println(iPos + " has no table row.. associating. " + Debug.getCompressedStackTrace(4));
			if (iPos >= 0 && iPos < sortedRows.size()) {
				TableRowSWT row = sortedRows.get(iPos);
				//System.out.print(".. associating to " + row);
				if (row != null && !row.isRowDisposed()) {
					row.setTableItem(iPos);
					//System.out.println(", now " + row);
					return row;
				}
				return null;
			}
		} catch (Exception e) {
			Debug.out(e);
		}
		return null;
	}

	public int getRowCount() {
		// don't use sortedRows here, it's not always up to date 
		return mapDataSourceToRow.size();
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getDataSources()
	public ArrayList<DATASOURCETYPE> getDataSources() {
		return new ArrayList<DATASOURCETYPE>(mapDataSourceToRow.keySet());
	}

	/* various selected rows functions */
	/***********************************/

	public List<Object> getSelectedDataSourcesList() {
		if (listSelectedCoreDataSources != null) {
			return listSelectedCoreDataSources;
		}
		synchronized (selectedRows) {
			if (table == null || table.isDisposed() || selectedRows.size() == 0) {
  			return Collections.emptyList();
  		}
  
  		final ArrayList<Object> l = new ArrayList<Object>(
  				selectedRows.size());
  		for (TableRowCore row : selectedRows) {
  			if (row != null && !row.isRowDisposed()) {
  				Object ds = row.getDataSource(true);
  				if (ds != null) {
  					l.add(ds);
  				}
  			}
  		}
 
  		listSelectedCoreDataSources = l;
  		return l;
		}
	}

	/** Returns an array of all selected Data Sources.  Null data sources are
	 * ommitted.
	 *
	 * @return an array containing the selected data sources
	 * 
	 * @TODO TuxPaper: Virtual row not created when using getSelection?
	 *                  computePossibleActions isn't being calculated right
	 *                  because of non-created rows when select user selects all
	 */
	public List<Object> getSelectedPluginDataSourcesList() {
		synchronized (selectedRows) {
  		if (table == null || table.isDisposed() || selectedRows.size() == 0) {
  			return Collections.emptyList();
  		}
  
  		final ArrayList<Object> l = new ArrayList<Object>(selectedRows.size());
  		for (TableRowCore row : selectedRows) {
  			if (row != null && !row.isRowDisposed()) {
  				Object ds = row.getDataSource(false);
  				if (ds != null) {
  					l.add(ds);
  				}
  			}
  		}
  		return l;
		}
	}

	/** Returns an array of all selected Data Sources.  Null data sources are
	 * ommitted.
	 *
	 * @return an array containing the selected data sources
	 *
	 **/
	// see common.TableView
	public List<Object> getSelectedDataSources() {
		return new ArrayList<Object>(getSelectedDataSourcesList());
	}

	// see common.TableView
	public Object[] getSelectedDataSources(boolean bCoreDataSource) {
		if (bCoreDataSource) {
			return getSelectedDataSourcesList().toArray();
		}
		return getSelectedPluginDataSourcesList().toArray();
	}

	/** @see com.aelitis.azureus.ui.common.table.TableView#getSelectedRows() */
	public TableRowCore[] getSelectedRows() {
		synchronized (selectedRows) {
			return selectedRows.toArray(new TableRowCore[0]);
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getSelectedRowsSize()
	public int getSelectedRowsSize() {
		synchronized (selectedRows) {
			return selectedRows.size();
		}
	}

	/** Returns an list of all selected TableRowSWT objects.  Null data sources are
	 * ommitted.
	 *
	 * @return an list containing the selected TableRowSWT objects
	 */
	public List<TableRowCore> getSelectedRowsList() {
		synchronized (selectedRows) {
  		final ArrayList<TableRowCore> l = new ArrayList<TableRowCore>(
  				selectedRows.size());
  		for (TableRowCore row : selectedRows) {
  			if (row != null && !row.isRowDisposed()) {
  				l.add(row);
  			}
  		}
  
  		return l;
		}
	}
	
	public boolean isSelected(TableRow row) {
		synchronized (selectedRows) {
			return selectedRows.contains(row);
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getFocusedRow()
	public TableRowCore getFocusedRow() {
		synchronized (selectedRows) {
			if (selectedRows.size() == 0) {
				return null;
			}
			return selectedRows.get(0);
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getFirstSelectedDataSource()
	public Object getFirstSelectedDataSource() {
		return getFirstSelectedDataSource(true);
	}

	public TableRowSWT[] swt_getVisibleRows() {
		if (!isVisible()) {
			return new TableRowSWT[0];
		}
		
		synchronized (this) {
  		if (visibleRows == null) {
  			visibleRowsChanged();
  		}
  		
  		return visibleRows;
		}
	}

	/** Returns the first selected data sources.
	 *
	 * @return the first selected data source, or null if no data source is 
	 *         selected
	 */
	public Object getFirstSelectedDataSource(boolean bCoreObject) {
		synchronized (selectedRows) {
			if (selectedRows.size() > 0) {
				return selectedRows.get(0).getDataSource(bCoreObject);
			}
		}
		return null;
	}

	/** For each row source that the user has selected, run the code
	 * provided by the specified parameter.
	 *
	 * @param runner Code to run for each selected row/datasource
	 */
	public void runForSelectedRows(TableGroupRowRunner runner) {
		if (table == null || table.isDisposed()) {
			return;
		}

		TableRowCore[] rows;
		synchronized (selectedRows) {
			rows = selectedRows.toArray(new TableRowCore[0]);
		}
		boolean ran = runner.run(rows);
		if (!ran) {
			for (int i = 0; i < rows.length; i++) {
				TableRowCore row = rows[i];
				runner.run(row);
			}
		}
	}

	/** For each visible row source, run the code provided by the specified 
	 * parameter.
	 *
	 * @param runner Code to run for each selected row/datasource
	 */
	public void swt_runForVisibleRows(TableGroupRowRunner runner) {
		TableRowSWT[] rows = swt_getVisibleRows();
		if (runner.run(rows)) {
			return;
		}

		for (int i = 0; i < rows.length; i++) {
			runner.run(rows[i]);
		}
	}

	// see common.tableview
	public void runForAllRows(TableGroupRowVisibilityRunner runner) {
		if (table == null || table.isDisposed()) {
			return;
		}

		// put to array instead of synchronised iterator, so that runner can remove
		TableRowCore[] rows = getRows();

		for (int i = 0; i < rows.length; i++) {
			boolean isRowVisible = isRowVisible(rows[i]);
			runner.run(rows[i], isRowVisible);
			
			int numSubRows = rows[i].getSubItemCount();
			if (numSubRows > 0) {
				TableRowCore[] subRows = rows[i].getSubRowsWithNull();
				for (TableRowCore subRow : subRows) {
					if (subRow != null) {
						runner.run(subRow, isRowVisible && isRowVisible(subRow));
					}
				}
			}
		}
	}

	/**
	 * Runs a specified task for a list of table items that the table contains
	 * @param items A list of TableItems that are part of the table view
	 * @param runner A task
	 */
	public void runForTableItems(List<TableItemOrTreeItem> items, TableGroupRowRunner runner) {
		if (table == null || table.isDisposed()) {
			return;
		}

		final Iterator<TableItemOrTreeItem> iter = items.iterator();
		List<TableRowCore> rows_to_use = new ArrayList<TableRowCore>(items.size());
		while (iter.hasNext()) {
			TableItemOrTreeItem tableItem = iter.next();
			if (tableItem.isDisposed()) {
				continue;
			}

			TableRowSWT row = (TableRowSWT) getRow(tableItem);
			if (row != null && !row.isRowDisposed()) {
				rows_to_use.add(row);
			}
		}
		if (rows_to_use.size() > 0) {
			TableRowCore[] rows = rows_to_use.toArray(new TableRowCore[rows_to_use.size()]);
			boolean ran = runner.run(rows);
			if (!ran) {
				for (int i = 0; i < rows.length; i++) {
					TableRowCore row = rows[i];
					runner.run(row);
				}
			}
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#clipboardSelected()
	public void clipboardSelected() {
		String sToClipboard = "";
		for (int j = 0; j < table.getColumnCount(); j++) {
			if (j != 0) {
				sToClipboard += "\t";
			}
			sToClipboard += table.getColumn(j).getText();
		}

		TableRowCore[] rows = getSelectedRows();
		for (TableRowCore row : rows) {
			sToClipboard += "\n";
			TableColumnCore[] visibleColumns = getVisibleColumns();
			for (int j = 0; j < visibleColumns.length; j++) {
				TableColumnCore column = visibleColumns[j];
				if (column.isVisible()) {
  				if (j != 0) {
  					sToClipboard += "\t";
  				}
  				TableCellCore cell = row.getTableCellCore(column.getName());
  				if (cell != null) {
  					sToClipboard += cell.getClipboardText();
  				}
				}
			}
		}
		new Clipboard(getComposite().getDisplay()).setContents(new Object[] {
			sToClipboard
		}, new Transfer[] {
			TextTransfer.getInstance()
		});
	}

	/** Handle sorting of a column based on clicking the Table Header */
	private class ColumnSelectionListener
		implements Listener
	{
		/** Process a Table Header click
		 * @param event event information
		 */
		public void handleEvent(final Event event) {
			int maskNoButton = (event.stateMask & ~SWT.BUTTON_MASK);
			if (maskNoButton != 0) {
				return;
			}
			TableColumnOrTreeColumn column = TableOrTreeUtils.getTableColumnEventItem(event.widget);
			if (column == null) {
				return;
			}
			TableColumnCore tableColumnCore = (TableColumnCore) column.getData("TableColumnCore");
			if (tableColumnCore != null) {
				sortColumnReverse(tableColumnCore);
				columnVisibilitiesChanged = true;
				refreshTable(true);
			}
		}
	}

	/**
	 * Handle movement of a column based on user dragging the Column Header.
	 * SWT >= 3.1
	 */
	private class ColumnMoveListener
		implements Listener
	{
		public void handleEvent(Event event) {
			TableColumnOrTreeColumn column = TableOrTreeUtils.getTableColumnEventItem(event.widget);
			if (column == null) {
				return;
			}

			TableColumnCore tableColumnCore = (TableColumnCore) column.getData("TableColumnCore");
			if (tableColumnCore == null) {
				return;
			}

			TableOrTreeSWT table = column.getParent();

			// Get the 'added position' of column
			// It would have been easier if event (.start, .end) contained the old
			// and new position..
			TableColumnOrTreeColumn[] tableColumns = table.getColumns();
			int iAddedPosition;
			for (iAddedPosition = 0; iAddedPosition < tableColumns.length; iAddedPosition++) {
				if (column.getColumn() == tableColumns[iAddedPosition].getColumn()) {
					break;
				}
			}
			if (iAddedPosition >= tableColumns.length) {
				return;
			}

			// Find out position in the order list
			int iColumnOrder[];
			try {
				iColumnOrder = table.getColumnOrder();
			} catch (NoSuchMethodError e) {
				// Ignore < SWT 3.1
				return;
			}
			for (int i = 0; i < iColumnOrder.length; i++) {
				if (iColumnOrder[i] == iAddedPosition) {
					int iNewPosition = i - (bSkipFirstColumn ? 1 : 0);
					if (tableColumnCore.getPosition() != iNewPosition) {
						if (iNewPosition == -1) {
							iColumnOrder[0] = 0;
							iColumnOrder[1] = iAddedPosition;
							table.setColumnOrder(iColumnOrder);
							iNewPosition = 0;
						}
						//System.out.println("Moving " + tableColumnCore.getName() + " to Position " + i);
						tableColumnCore.setPositionNoShift(iNewPosition);
						tableColumnCore.saveSettings(null);
						TableStructureEventDispatcher.getInstance(sTableID).columnOrderChanged(
								iColumnOrder);
					}
					break;
				}
			}
		}
	}

	private int getColumnNo(int iMouseX) {
		int iColumn = -1;
		int itemCount = table.getItemCount();
		if (table.getItemCount() > 0) {
			//Using  table.getTopIndex() instead of 0, cause
			//the first row has no bounds when it's not visible under OS X.
			int topIndex = table.getTopIndex();
			if (topIndex >= itemCount || topIndex < 0) {
				topIndex = itemCount - 1;
			}
			TableItemOrTreeItem ti = table.getItem(topIndex);
			if (ti.isDisposed()) {
				return -1;
			}
			for (int i = bSkipFirstColumn ? 1 : 0; i < table.getColumnCount(); i++) {
				// M8 Fixes SWT GTK Bug 51777:
				//  "TableItem.getBounds(int) returns the wrong values when table scrolled"
				Rectangle cellBounds = ti.getBounds(i);
				//System.out.println("i="+i+";Mouse.x="+iMouseX+";cellbounds="+cellBounds);
				if (iMouseX >= cellBounds.x
						&& iMouseX < cellBounds.x + cellBounds.width
						&& cellBounds.width > 0) {
					iColumn = i;
					break;
				}
			}
		}
		return iColumn;
	}

	public TableRowCore getRow(int x, int y) {
		int iColumn = getColumnNo(x);
		if (iColumn < 0) {
			return null;
		}

		TableItemOrTreeItem item = table.getItem(new Point(2, y));
		if (item == null) {
			return null;
		}
		return getRow(item);
	}

	public TableCellCore getTableCell(int x, int y) {
		int iColumn = getColumnNo(x);
		if (iColumn < 0) {
			return null;
		}

		TableItemOrTreeItem item = table.getItem(new Point(2, y));
		if (item == null) {
			item = table.getItem(new Point(x, y));
		}

		if (item == null) {
			return null;
		}
		TableRowSWT row = (TableRowSWT) getRow(item);

		if (row == null || row.isRowDisposed()) {
			return null;
		}

		TableColumnOrTreeColumn tcColumn = table.getColumn(iColumn);
		String sCellName = (String) tcColumn.getData("Name");
		if (sCellName == null) {
			return null;
		}

		return row.getTableCellCore(sCellName);
	}

	public TableRowSWT getTableRow(int x, int y, boolean anyX) {
		TableItemOrTreeItem item = table.getItem(new Point(anyX ? 2 : x, y));
		if (item == null) {
			return null;
		}
		return (TableRowSWT) getRow(item);
	}

	private TableColumnCore getTableColumnByOffset(int x) {
		int iColumn = getColumnNo(x);
		if (iColumn < 0) {
			return null;
		}

		TableColumnOrTreeColumn column = table.getColumn(iColumn);
		return (TableColumnCore) column.getData("TableColumnCore");
	}

	// @see org.gudy.azureus2.core3.util.AEDiagnosticsEvidenceGenerator#generate(org.gudy.azureus2.core3.util.IndentWriter)
	public void generate(IndentWriter writer) {
		writer.println("Diagnostics for " + this + " (" + sTableID + ")");

		try {
			dataSourceToRow_mon.enter();

			writer.println("DataSources scheduled to Add/Remove: "
					+ dataSourcesToAdd.size() + "/" + dataSourcesToRemove.size());

			writer.println("TableView: " + mapDataSourceToRow.size() + " datasources");
			Iterator<DATASOURCETYPE> it = mapDataSourceToRow.keySet().iterator();

			while (it.hasNext()) {

				Object key = it.next();

				writer.println("  " + key + " -> " + mapDataSourceToRow.get(key));
			}

			writer.println("# of SubViews: " + tabViews.size());
			writer.indent();
			try {
				for (Iterator<UISWTViewCore> iter = tabViews.iterator(); iter.hasNext();) {
					UISWTViewCore view = iter.next();
					writer.println(view.getTitleID() + ": " + view.getFullTitle());
				}
			} finally {
				writer.exdent();
			}

			writer.println("Columns:");
			writer.indent();
			try {
				TableColumnOrTreeColumn[] tableColumnsSWT = table.getColumns();
				for (int i = 0; i < tableColumnsSWT.length; i++) {
					final TableColumnCore tc = (TableColumnCore) tableColumnsSWT[i].getData("TableColumnCore");
					if (tc != null) {
						writer.println(tc.getName() + ";w=" + tc.getWidth() + ";w-offset="
								+ tableColumnsSWT[i].getData("widthOffset"));
					}
				}
			} catch (Throwable t) {
			} finally {
				writer.exdent();
			}

		} finally {

			dataSourceToRow_mon.exit();
		}
	}

	public boolean getSkipFirstColumn() {
		return bSkipFirstColumn;
	}

	// see common.TableView
	public void setRowDefaultHeight(int iHeight) {
		if (ptIconSize == null) {
			ptIconSize = new Point(1, iHeight);
		} else {
			ptIconSize.y = iHeight;
		}
		if (!Constants.isOSX) {
			bSkipFirstColumn = true;
		}
	}

	public int getRowDefaultHeight() {
		if (ptIconSize == null) {
			return 0;
		}
		return ptIconSize.y;
	}

	// from common.TableView
	public void setRowDefaultIconSize(Point size) {
		ptIconSize = size;
		if (!Constants.isOSX) {
			bSkipFirstColumn = true;
		}
	}

	// TabViews Functions
	public void addTabView(UISWTViewCore view) {
		if (view == null || tabFolder == null) {
			return;
		}
		
		triggerTabViewDataSourceChanged(view);

		CTabItem item = new CTabItem(tabFolder, SWT.NULL);
		item.setData("IView", view);
		Messages.setLanguageText(item, view.getTitleID());
		view.initialize(tabFolder);
		item.setControl(view.getComposite());
		tabViews.add(view);
	}

	private void fillRowGaps(boolean bForceDataRefresh) {
		_sortColumn(bForceDataRefresh, true, false);
	}

	private void sortColumn(boolean bForceDataRefresh) {
		_sortColumn(bForceDataRefresh, false, false);
	}

	private void _sortColumn(boolean bForceDataRefresh, boolean bFillGapsOnly,
			boolean bFollowSelected) {
		if (table == null || table.isDisposed()) {
			return;
		}

		try {
			sortColumn_mon.enter();

			long lTimeStart;
			if (DEBUG_SORTER) {
				//System.out.println(">>> Sort.. ");
				lTimeStart = System.currentTimeMillis();
			}

			int iNumMoves = 0;

			int iTopIndex = table.getTopIndex();
			int iBottomIndex = Utils.getTableBottomIndex(table, iTopIndex);

			boolean needsUpdate = false;

			try {
				sortedRows_mon.enter();

				if (bForceDataRefresh && sortColumn != null) {
					int i = 0;
					String sColumnID = sortColumn.getName();
					for (Iterator<TableRowSWT> iter = sortedRows.iterator(); iter.hasNext();) {
						TableRowSWT row = iter.next();
						TableCellSWT cell = row.getTableCellSWT(sColumnID);
						if (cell != null) {
							cell.refresh(true, i >= iTopIndex && i <= iBottomIndex);
						}
						i++;
					}
				}

				if (!bFillGapsOnly) {
					if (sortColumn != null
							&& sortColumn.getLastSortValueChange() >= lLastSortedOn) {
						lLastSortedOn = SystemTime.getCurrentTime();
						Collections.sort(sortedRows, sortColumn);
						if (DEBUG_SORTER) {
							long lTimeDiff = (System.currentTimeMillis() - lTimeStart);
							if (lTimeDiff >= 0) {
								System.out.println("--- Build & Sort took " + lTimeDiff + "ms");
							}
						}
					} else {
						if (DEBUG_SORTER) {
							System.out.println("Skipping sort :)");
						}
					}
				}

				int count = sortedRows.size();
				if (iBottomIndex >= count) {
					iBottomIndex = count - 1;
				}

				for (int i = 0; i < sortedRows.size(); i++) {
					TableRowSWT row = sortedRows.get(i);
					boolean visible = i >= iTopIndex && i <= iBottomIndex;
					if (row.setTableItem(i, visible)) {
						if (visible) {
							needsUpdate = true;
						}
						iNumMoves++;
					}
				}
			} finally {
				sortedRows_mon.exit();
			}

			if (DEBUG_SORTER && iNumMoves > 0) {
				System.out.println("numMoves= " + iNumMoves + ";top=" + iTopIndex
						+ ";bottom=" + iBottomIndex + ";needUpdate?" + needsUpdate);
			}

			if (needsUpdate) {
				visibleRowsChanged();
			}

			if (DEBUG_SORTER) {
				long lTimeDiff = (System.currentTimeMillis() - lTimeStart);
				if (lTimeDiff >= 500) {
					System.out.println("<<< Sort & Assign took " + lTimeDiff + "ms with "
							+ iNumMoves + " rows (of " + sortedRows.size() + ") moved. "
							+ Debug.getCompressedStackTrace());
				}
			}
		} finally {
			sortColumn_mon.exit();
		}
	}

	protected void selectRow(final TableRowCore row, boolean trigger) {
		if (row == null || row.isRowDisposed()) {
			return;
		}
		synchronized (selectedRows) {
  		if (selectedRows.contains(row)) {
  			return;
  		}
  		selectedRows.add(row);
  		
  		listSelectedCoreDataSources = null;
		}
		
		if (trigger) {
			triggerSelectionListeners(new TableRowCore[] { row });
			triggerTabViewsDataSourceChanged(false);
		}
		
		((TableRowSWT) row).setWidgetSelected(true);
	}

	protected void updateSelectedRows(TableItemOrTreeItem[] newSelectionArray, boolean trigger) {
		List<TableRowCore> newSelectionList = new ArrayList<TableRowCore>(1);

		//System.out.print("Selected Items: ");
		for (TableItemOrTreeItem item : newSelectionArray) {
			//System.out.print(table.indexOf(item));
			TableRowCore row = getRow(item);
			if (row != null && !row.isRowDisposed()) {
				newSelectionList.add(row);
			}// else { System.out.print("( NO ROW)"); }
			//System.out.print(", ");
		}
		//System.out.println();
		updateSelectedRows(newSelectionList.toArray(new TableRowCore[0]), trigger);
	}
	
	protected void updateSelectedRows(final TableRowCore[] newSelectionArray,
			final boolean trigger) {
		if (table.isDisposed()) {
			return;
		}

		/**
		System.out.print(newSelectionArray.length + " Selected Rows: ");
		for (TableRowCore row : newSelectionArray) {
			System.out.print(indexOf(row));
			System.out.print(", ");
		}
		System.out.println(" via " + Debug.getCompressedStackTrace(4));
		/**/

		final List<TableRowCore> oldSelectionList = new ArrayList<TableRowCore>();
		synchronized (selectedRows) {
			oldSelectionList.addAll(selectedRows);

			final TableItemOrTreeItem[] newSelectionItems = new TableItemOrTreeItem[newSelectionArray.length];
			int i = 0;
			listSelectedCoreDataSources = null;
			selectedRows.clear();
			if (newSelectionArray.length > 0) {
  			for (TableRowCore row : newSelectionArray) {
  				if (row != null && !row.isRowDisposed()) {
  					newSelectionItems[i] = ((TableRowImpl)row).getItem();
  					if (newSelectionItems[i] != null && !newSelectionItems[i].isDisposed()) {
  						i++;
  					}
  					selectedRows.add(row);
  				}
  			}
  			final int numItems = i;
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						TableItemOrTreeItem[] selection = table.getSelection();
						for (int i = 0; i < numItems; i++) {
							boolean alreadySelected = false;
							for (int j = 0; j < selection.length; j++) {
								if (selection[j] == newSelectionItems[i]) {
									alreadySelected = true;
									selection[j] = null;
									break;
								}
							}
							if (!alreadySelected) {
								table.select(newSelectionItems[i]);
							}
						}
						for (int j = 0; j < selection.length; j++) {
							if (selection[j] != null) {
								table.deselect(selection[j]);
							}
						}
					}
				});
			} else {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						table.deselectAll();
					}
				});
			}
		}

		Utils.getOffOfSWTThread(new AERunnable() {
			public void runSupport() {
				List<TableRowCore> listNewlySelected;
				boolean somethingChanged;
				synchronized (selectedRows) {
					List<TableRowCore> newSelectionList = new ArrayList<TableRowCore>(1);
					listNewlySelected = new ArrayList<TableRowCore>(1);

					// We'll remove items still selected from oldSelectionLeft, leaving
					// it with a list of items that need to fire the deselection event.
					for (TableRowCore row : newSelectionArray) {
						if (row == null || row.isRowDisposed()) {
							continue;
						}

						boolean existed = false;
						for (TableRowCore oldRow : oldSelectionList) {
							if (oldRow == row) {
								existed = true;
								newSelectionList.add(row);
								oldSelectionList.remove(row);
								break;
							}
						}
						if (!existed) {
							newSelectionList.add(row);
							listNewlySelected.add(row);
						}
					}

					somethingChanged = listNewlySelected.size() > 0
							|| oldSelectionList.size() > 0;
					if (DEBUG_SELECTION) {
  					System.out.println(somethingChanged + "] +"
  							+ listNewlySelected.size() + "/-" + oldSelectionList.size()
  							+ ";  UpdateSelectedRows via " + Debug.getCompressedStackTrace());
					}
				}

				if (trigger && somethingChanged) {
					if (listNewlySelected.size() > 0) {
						triggerSelectionListeners(listNewlySelected.toArray(new TableRowCore[0]));
					}
					if (oldSelectionList.size() > 0) {
						triggerDeselectionListeners(oldSelectionList.toArray(new TableRowCore[0]));
					}

					triggerTabViewsDataSourceChanged(false);
				}

			}
		});
	}

	public void sortColumnReverse(final TableColumnCore sorter) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				swt_sortColumnReverse(sorter);
			}
		});
	}
	
	public void swt_sortColumnReverse(TableColumnCore sorter) {
		if (sortColumn == null) {
			return;
		}
		boolean bSameColumn = sortColumn.equals(sorter);
		if (!bSameColumn) {
			fixAlignment(sortColumn, false);
			sortColumn = sorter;
			fixAlignment(sorter, true);
			int iSortDirection = configMan.getIntParameter(CFG_SORTDIRECTION);
			if (iSortDirection == 0) {
				sortColumn.setSortAscending(true);
			} else if (iSortDirection == 1) {
				sortColumn.setSortAscending(false);
			} else {
				sortColumn.setSortAscending(!sortColumn.isSortAscending());
			}

			TableColumnManager.getInstance().setDefaultSortColumnName(sTableID,
					sortColumn.getName());
		} else {
			sortColumn.setSortAscending(!sortColumn.isSortAscending());
		}

		swt_changeColumnIndicator();
		sortColumn(!bSameColumn);
	}

	private void swt_changeColumnIndicator() {
		if (table == null || table.isDisposed()) {
			return;
		}

		try {
			// can't use TableColumnCore.getPosition, because user may have moved
			// columns around, messing up the SWT column indexes.  
			// We can either use search columnsOrdered, or search table.getColumns()
			TableColumnOrTreeColumn[] tcs = table.getColumns();
			for (int i = 0; i < tcs.length; i++) {
				String sName = (String) tcs[i].getData("Name");
				if (sName != null && sortColumn != null
						&& sName.equals(sortColumn.getName())) {
					table.setSortDirection(sortColumn.isSortAscending() ? SWT.UP
							: SWT.DOWN);
					table.setSortColumn(tcs[i]);
					return;
				}
			}

			table.setSortColumn(null);
		} catch (NoSuchMethodError e) {
			// sWT < 3.2 doesn't have column indicaters
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#isRowVisible(com.aelitis.azureus.ui.common.table.TableRowCore)
	public boolean isRowVisible(TableRowCore row) {
		if (row.isInPaintItem()) {
			return true;
		}
		if (visibleRows == null) {
			return false;
		}
		for (TableRowCore visibleRow : visibleRows) {
			if (row == visibleRow) {
				if (Utils.isThisThreadSWT() && !isVisible()) {
					return false;
				}
				return true;
			}
		}
		return false;
	}

	protected void visibleRowsChanged() {
		//debug("VRC " + Debug.getCompressedStackTrace());


		final List<TableRowSWT> newlyVisibleRows = new ArrayList<TableRowSWT>();
		final List<TableRowSWT> nowInVisibleRows;
		synchronized (this) {
  		List<TableItemOrTreeItem> visibleTableItems;
  		if (isVisible()) {
  			visibleTableItems = Utils.getVisibleTableItems(table);
  		} else {
  			visibleTableItems = Collections.emptyList();
  		}
			nowInVisibleRows = new ArrayList<TableRowSWT>(0);
  		if (visibleRows != null) {
  			nowInVisibleRows.addAll(Arrays.asList(visibleRows));
  		}
  		TableRowSWT[] rows = new TableRowSWT[visibleTableItems.size()];
  		int pos = 0;
  		for (TableItemOrTreeItem item : visibleTableItems) {
  			TableRowCore row = getRow(item);
  			if (row instanceof TableRowSWT) {
  				rows[pos++] = (TableRowSWT) row;
  				boolean removed = nowInVisibleRows.remove(row);
  				if (!removed) {
  					newlyVisibleRows.add((TableRowSWT) row);
  				}
  			}
  		}
  
  		if (pos < rows.length) {
  			// Some were null, shrink array
  			TableRowSWT[] temp = new TableRowSWT[pos];
  			System.arraycopy(rows, 0, temp, 0, pos);
  			visibleRows = temp;
  		} else {
  			visibleRows = rows;
  		}
		}
		
		if (DEBUG_ROWCHANGE) {
			System.out.println("visRowsChanged; shown=" + visibleRows.length + "; +"
					+ newlyVisibleRows.size() + "/-" + nowInVisibleRows.size() + " via "
					+ Debug.getCompressedStackTrace(8));
		}
		Utils.getOffOfSWTThread(new AERunnable() {
			
			public void runSupport() {
				boolean bTableUpdate = false;

				for (TableRowSWT row : newlyVisibleRows) {
					row.refresh(true, true);
					if (row instanceof TableRowImpl) {
						((TableRowImpl) row).setShown(true, false);
					}
					if (Constants.isOSX) {
						bTableUpdate = true;
					}
				}

				for (TableRowSWT row : nowInVisibleRows) {
					if (row instanceof TableRowImpl) {
						((TableRowImpl) row).setShown(false, false);
					}
				}

				if (bTableUpdate) {
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							table.update();
						}
					});
				}

			}
		});

	}

	public Image obfusticatedImage(final Image image) {
		if (table.getItemCount() == 0 || !isVisible()) {
			return image;
		}

		TableColumnOrTreeColumn[] tableColumnsSWT = table.getColumns();
		for (int i = 0; i < tableColumnsSWT.length; i++) {
			final TableColumnCore tc = (TableColumnCore) tableColumnsSWT[i].getData("TableColumnCore");

			if (tc != null && tc.isObfusticated()) {
				int iTopIndex = table.getTopIndex();
				int iBottomIndex = Utils.getTableBottomIndex(table, iTopIndex);

				int size = iBottomIndex - iTopIndex + 1;
				if (size <= 0 || iTopIndex < 0) {
					continue;
				}

				for (int j = iTopIndex; j <= iBottomIndex; j++) {
					TableItemOrTreeItem rowSWT = table.getItem(j);
					TableRowSWT row = (TableRowSWT) table.getItem(j).getData("TableRow");
					if (row != null && !row.isRowDisposed()) {
						TableCellSWT cell = row.getTableCellSWT(tc.getName());

						String text = cell.getObfusticatedText();

						if (text != null) {
							final Rectangle columnBounds = rowSWT.getBounds(i);
							if (columnBounds.y + columnBounds.height > clientArea.y
									+ clientArea.height) {
								columnBounds.height -= (columnBounds.y + columnBounds.height)
										- (clientArea.y + clientArea.height);
							}
							if (columnBounds.x + columnBounds.width > clientArea.x
									+ clientArea.width) {
								columnBounds.width -= (columnBounds.x + columnBounds.width)
										- (clientArea.x + clientArea.width);
							}
							
							Point location = Utils.getLocationRelativeToShell(table.getComposite());
							
							columnBounds.x += location.x;
							columnBounds.y += location.y;

							UIDebugGenerator.obfusticateArea(image, columnBounds, text);
						}
					}
				}

				//UIDebugGenerator.offusticateArea(image, columnBounds);
			}
		}

		UISWTViewCore view = getActiveSubView();
		if (view instanceof ObfusticateImage) {
			try {
				((ObfusticateImage) view).obfusticatedImage(image);
			} catch (Exception e) {
				Debug.out("Obfusticating " + view, e);
			}
		}
		return image;
	}

	void debug(String s) {
		AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger("table");
		diag_logger.log(SystemTime.getCurrentTime() + ":" + sTableID + ": " + s);

		System.out.println(SystemTime.getCurrentTime() + ": " + sTableID + ": " + s);
	}

	// from common.TableView
	public boolean isEnableTabViews() {
		return bEnableTabViews;
	}

	// from common.TableView
	public void setEnableTabViews(boolean enableTabViews) {
		bEnableTabViews = enableTabViews;
	}

	public void addMenuFillListener(TableViewSWTMenuFillListener l) {
		listenersMenuFill.add(l);
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#isDisposed()
	public boolean isDisposed() {
		return mainComposite == null || mainComposite.isDisposed() || table == null
				|| table.isDisposed();
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#size(boolean)
	/**
	 * @note bIncludeQueue can return an invalid number, such as a negative :(
	 */
	public int size(boolean bIncludeQueue) {
		int size = sortedRows.size();

		if (bIncludeQueue) {
			if (dataSourcesToAdd != null) {
				size += dataSourcesToAdd.size();
			}
			if (dataSourcesToRemove != null) {
				size -= dataSourcesToRemove.size();
			}
		}
		return size;
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getPropertiesPrefix()
	public String getPropertiesPrefix() {
		return sPropertiesPrefix;
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#setFocus()
	public void setFocus() {
		if (table != null && !table.isDisposed()) {
			table.setFocus();
		}
	}

	// @see org.gudy.azureus2.ui.swt.views.TableViewSWT#addKeyListener(org.eclipse.swt.events.KeyListener)
	public void addKeyListener(KeyListener listener) {
		if (listenersKey.contains(listener)) {
			return;
		}

		listenersKey.add(listener);
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#removeKeyListener(org.eclipse.swt.events.KeyListener)
	public void removeKeyListener(KeyListener listener) {
		listenersKey.remove(listener);
	}

	// @see org.gudy.azureus2.ui.swt.views.TableViewSWT#getSortColumn()
	public TableColumnCore getSortColumn() {
		return sortColumn;
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#selectAll()
	public void selectAll() {
		if (table != null && !table.isDisposed()) {
			// Used to ensure all rows have index, but I don't see a reason why.
			// Uses a lot of CPU, so kill it :)
			//ensureAllRowsHaveIndex();
			table.selectAll();
			updateSelectedRows(getRows(), true);
		}
	}

	/**
	 * 
	 *
	 * @since 3.0.0.7
	 *
	private void ensureAllRowsHaveIndex() {
		for (int i = 0; i < sortedRows.size(); i++) {
			TableRowSWT row = sortedRows.get(i);
			row.setTableItem(i);
		}
	}
	*/


	// @see com.aelitis.azureus.ui.common.table.TableView#isTableFocus()
	public boolean isTableFocus() {
		return table.isFocusControl();
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableViewSWT#createDragSource(int)
	public DragSource createDragSource(int style) {
		final DragSource dragSource = new DragSource(table.getComposite(), style);
		dragSource.addDragListener(new DragSourceAdapter() {
			public void dragStart(DragSourceEvent event) {
				table.setCursor(null);
				isDragging = true;
			}
			
			public void dragFinished(DragSourceEvent event) {
				isDragging = false;
			}
		});
		table.addDisposeListener(new DisposeListener() {
			// @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
			public void widgetDisposed(DisposeEvent e) {
				if (!dragSource.isDisposed()) {
					dragSource.dispose();
				}
			}
		});
		return dragSource;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableViewSWT#createDropTarget(int)
	public DropTarget createDropTarget(int style) {
		final DropTarget dropTarget = new DropTarget(table.getComposite(), style);
		table.addDisposeListener(new DisposeListener() {
			// @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
			public void widgetDisposed(DisposeEvent e) {
				if (!dropTarget.isDisposed()) {
					dropTarget.dispose();
				}
			}
		});
		return dropTarget;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableViewSWT#indexOf(org.eclipse.swt.widgets.Widget)
	public TableRowCore getRow(DropTargetEvent event) {
		TableItemOrTreeItem ti = TableOrTreeUtils.getEventItem(event.item);
		if (ti != null) {
			return (TableRowCore) ti.getData("TableRow");
		}
		return null;
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#dataSourceExists(java.lang.Object)
	public boolean dataSourceExists(DATASOURCETYPE dataSource) {
		return mapDataSourceToRow.containsKey(dataSource)
				|| dataSourcesToAdd.contains(dataSource);
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getVisibleColumns()
	public TableColumnCore[] getVisibleColumns() {
		return tableColumns;
	}

	/**
	 * @return
	 */
	protected TableViewSWTPanelCreator getMainPanelCreator() {
		return mainPanelCreator;
	}

	// @see org.gudy.azureus2.ui.swt.views.TableViewSWT#setMainPanelCreator(org.gudy.azureus2.ui.swt.views.TableViewMainPanelCreator)
	public void setMainPanelCreator(TableViewSWTPanelCreator mainPanelCreator) {
		this.mainPanelCreator = mainPanelCreator;
	}

	public TableCellCore getTableCellWithCursor() {
		Point pt = table.getDisplay().getCursorLocation();
		pt = table.toControl(pt);
		return getTableCell(pt.x, pt.y);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableViewSWT#getTableRowWithCursor()
	public TableRowCore getTableRowWithCursor() {
		Point pt = table.getDisplay().getCursorLocation();
		pt = table.toControl(pt);
		return getTableRow(pt.x, pt.y, true);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableViewSWT#getTableCellMouseOffset()
	public Point getTableCellMouseOffset(TableCellSWT tableCell) {
		if (tableCell == null) {
			return null;
		}
		Point pt = table.getDisplay().getCursorLocation();
		pt = table.toControl(pt);

		Rectangle bounds = tableCell.getBounds();
		int x = pt.x - bounds.x;
		if (x < 0 || x > bounds.width) {
			return null;
		}
		int y = pt.y - bounds.y;
		if (y < 0 || y > bounds.height) {
			return null;
		}
		return new Point(x, y);
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getDataSourceType()
	public Class<?> getDataSourceType() {
		return classPluginDataSourceType;
	}

	public void addRefreshListener(TableRowRefreshListener listener) {
		try {
			listeners_mon.enter();

			if (refreshListeners == null) {
				refreshListeners = new ArrayList<TableRowRefreshListener>(1);
			}

			refreshListeners.add(listener);

		} finally {
			listeners_mon.exit();
		}
	}

	public void removeRefreshListener(TableRowRefreshListener listener) {
		try {
			listeners_mon.enter();

			if (refreshListeners == null) {
				return;
			}

			refreshListeners.remove(listener);

		} finally {
			listeners_mon.exit();
		}
	}

	public void invokeRefreshListeners(TableRowCore row) {
		Object[] listeners;
		try {
			listeners_mon.enter();
			if (refreshListeners == null) {
				return;
			}
			listeners = refreshListeners.toArray();

		} finally {
			listeners_mon.exit();
		}

		for (int i = 0; i < listeners.length; i++) {
			try {
				TableRowRefreshListener l = (TableRowRefreshListener) listeners[i];

				l.rowRefresh(row);

			} catch (Throwable e) {
				Debug.printStackTrace(e);
			}
		}
	}

	/** Note: Callers need to be on SWT Thread */
	public boolean isVisible() {
		boolean wasVisible = isVisible;
		isVisible = table != null && !table.isDisposed() && table.isVisible() && !shell.getMinimized();
		if (isVisible != wasVisible) {
			visibleRowsChanged();
			if (isVisible) {
				loopFactor = 0;

				UISWTViewCore view = getActiveSubView();
				if (view != null) {
					view.triggerEvent(UISWTViewEvent.TYPE_FOCUSGAINED, null);
				}
			} else {
				UISWTViewCore view = getActiveSubView();
				if (view != null) {
					view.triggerEvent(UISWTViewEvent.TYPE_FOCUSLOST, null);
				}
			}
		}
		return isVisible;
	}

	public void showRow(final TableRowCore row) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if ( table==null||table.isDisposed()){
					return;
				}
				int index = row.getIndex();
				if (index >= 0 && index < table.getItemCount()) {
					table.showItem(table.getItem(index));
				}
			}
		});
	}

	public boolean isMenuEnabled() {
		return menuEnabled;
	}

	public void setMenuEnabled(boolean menuEnabled) {
		this.menuEnabled = menuEnabled;
	}

	private void openFilterDialog() {
		if (filter == null) {
			return;
		}
		SimpleTextEntryWindow entryWindow = new SimpleTextEntryWindow();
		entryWindow.initTexts("MyTorrentsView.dialog.setFilter.title", null,
				"MyTorrentsView.dialog.setFilter.text", new String[] {
					MessageText.getString(getTableID() + "View" + ".header")
				});
		entryWindow.setPreenteredText(filter.text, false);
		entryWindow.prompt();
		if (!entryWindow.hasSubmittedInput()) {
			return;
		}
		String message = entryWindow.getSubmittedInput();

		if (message == null) {
			message = "";
		}

		setFilterText(message);
	}

	private void handleSearchKeyPress(KeyEvent e) {
		if (filter == null || e.widget == filter.widget) {
			return;
		}

		String newText = null;

		// normal character: jump to next item with a name beginning with this character
		if (ASYOUTYPE_MODE == ASYOUTYPE_MODE_FIND) {
			if (System.currentTimeMillis() - filter.lastFilterTime > 3000)
				newText = "";
		}

		if (e.keyCode == SWT.BS) {
			if (e.stateMask == SWT.CONTROL) {
				newText = "";
			} else if (filter.nextText.length() > 0) {
				newText = filter.nextText.substring(0, filter.nextText.length() - 1);
			}
		} else if ((e.stateMask & ~SWT.SHIFT) == 0 && e.character > 32) {
			newText = filter.nextText + String.valueOf(e.character);
		}

		if (newText == null) {
			return;
		}

		if (ASYOUTYPE_MODE == ASYOUTYPE_MODE_FILTER) {
			if (filter != null && filter.widget != null && !filter.widget.isDisposed()) {
				filter.widget.setFocus();
			}
			setFilterText(newText);
//		} else {
//			TableCellCore[] cells = getColumnCells("name");
//
//			//System.out.println(sLastSearch);
//
//			Arrays.sort(cells, TableCellImpl.TEXT_COMPARATOR);
//			int index = Arrays.binarySearch(cells, filter.text,
//					TableCellImpl.TEXT_COMPARATOR);
//			if (index < 0) {
//
//				int iEarliest = -1;
//				String s = filter.regex ? filter.text : "\\Q" + filter.text + "\\E";
//				Pattern pattern = Pattern.compile(s, Pattern.CASE_INSENSITIVE);
//				for (int i = 0; i < cells.length; i++) {
//					Matcher m = pattern.matcher(cells[i].getText());
//					if (m.find() && (m.start() < iEarliest || iEarliest == -1)) {
//						iEarliest = m.start();
//						index = i;
//					}
//				}
//
//				if (index < 0)
//					// Insertion Point (best guess)
//					index = -1 * index - 1;
//			}
//
//			if (index >= 0) {
//				if (index >= cells.length)
//					index = cells.length - 1;
//				TableRowCore row = cells[index].getTableRowCore();
//				int iTableIndex = row.getIndex();
//				if (iTableIndex >= 0) {
//					setSelectedRows(new TableRowCore[] {
//						row
//					});
//				}
//			}
//			filter.lastFilterTime = System.currentTimeMillis();
		}
		e.doit = false;
	}

	private void
	validateFilterRegex()
	{
		if (filter.regex) {
			try {
				Pattern.compile(filter.nextText, Pattern.CASE_INSENSITIVE);
				filter.widget.setBackground(COLOR_FILTER_REGEX);
				Messages.setLanguageTooltip(filter.widget,
						"MyTorrentsView.filter.tooltip");
			} catch (Exception e) {
				filter.widget.setBackground(Colors.colorErrorBG);
				filter.widget.setToolTipText(e.getMessage());
			}
		} else {
			filter.widget.setBackground(null);
			Messages.setLanguageTooltip(filter.widget,
					"MyTorrentsView.filter.tooltip");
		}
	}
	
	public void setFilterText(String s) {
		if (filter == null) {
			return;
		}
		filter.nextText = s;
		if (filter != null && filter.widget != null && !filter.widget.isDisposed()) {
			if (!filter.nextText.equals(filter.widget.getText())) {
				filter.widget.setText(filter.nextText);
				filter.widget.setSelection(filter.nextText.length());
			}

			validateFilterRegex();
		}

		if (filter.eventUpdate != null) {
			filter.eventUpdate.cancel();
		}
		filter.eventUpdate = SimpleTimer.addEvent("SearchUpdate",
				SystemTime.getOffsetTime(ASYOUTYPE_UPDATEDELAY),
				new TimerEventPerformer() {
					public void perform(TimerEvent event) {
						if (filter == null) {
							return;
						}
						if (filter.eventUpdate == null || filter.eventUpdate.isCancelled()) {
							filter.eventUpdate = null;
							return;
						}
						filter.eventUpdate = null;
						if (filter.nextText != null && !filter.nextText.equals(filter.text)) {
							filter.text = filter.nextText;
							filter.checker.filterSet(filter.text);
							refilter();
						}
					}
				});
	}

	public String getFilterText() {
		return filter == null ? "" : filter.text;
	}

	private void
	tableMutated()
	{
		filter f = filter;
		
		if ( f != null ){
			TableViewFilterCheck<DATASOURCETYPE> checker = f.checker;
			
			if ( checker instanceof TableViewFilterCheck.TableViewFilterCheckEx ){
				
				((TableViewFilterCheck.TableViewFilterCheckEx)checker).viewChanged( this );
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public void refilter() {
		if (filter == null) {
			return;
		}
		if (filter.eventUpdate != null) {
			filter.eventUpdate.cancel();
		}
		filter.eventUpdate = null;

		listUnfilteredDatasources_mon.enter();
		try {
			DATASOURCETYPE[] unfilteredArray = (DATASOURCETYPE[]) listUnfilteredDataSources.toArray();

			Set<DATASOURCETYPE> existing = new HashSet<DATASOURCETYPE>(
					getDataSources());
			List<DATASOURCETYPE> listRemoves = new ArrayList<DATASOURCETYPE>();
			List<DATASOURCETYPE> listAdds = new ArrayList<DATASOURCETYPE>();

			for (int i = 0; i < unfilteredArray.length; i++) {
				boolean bHave = existing.contains(unfilteredArray[i]);
				boolean isOurs = filter.checker.filterCheck(
						unfilteredArray[i], filter.text, filter.regex);
				if (!isOurs) {
					if (bHave) {
						listRemoves.add(unfilteredArray[i]);
					}
				} else {
					if (!bHave) {
						listAdds.add(unfilteredArray[i]);
					}
				}
			}
			removeDataSources((DATASOURCETYPE[]) listRemoves.toArray());
			addDataSources((DATASOURCETYPE[]) listAdds.toArray(), true);

			// add back the ones removeDataSources removed
			listUnfilteredDataSources.addAll(listRemoves);
		} finally {
			listUnfilteredDatasources_mon.exit();
			processDataSourceQueue();
		}
	}

	public boolean
	isFiltered(
		DATASOURCETYPE	ds )
	{
		if ( filter == null ){
			return( true );
		}
		
		return( filter.checker.filterCheck( ds, filter.text, filter.regex ));
	}
	
	// @see org.gudy.azureus2.ui.swt.views.table.TableViewSWT#enableFilterCheck(org.eclipse.swt.widgets.Text, org.gudy.azureus2.ui.swt.views.table.TableViewFilterCheck)
	public void enableFilterCheck(Text txtFilter,
			TableViewFilterCheck<DATASOURCETYPE> filterCheck) {
		if (filter != null) {
			if (filter.widget != null && !filter.widget.isDisposed()) {
				filter.widget.removeKeyListener(TableViewSWTImpl.this);
				filter.widget.removeModifyListener(filter.widgetModifyListener);
			}
		} else{
			filter = new filter();
		}
		filter.widget = txtFilter;
		if (txtFilter != null) {
			txtFilter.setMessage("Filter");
  		txtFilter.addKeyListener(this);
  
  		filter.widgetModifyListener = new ModifyListener() {
  			public void modifyText(ModifyEvent e) {
  				setFilterText(((Text) e.widget).getText());
  			}
  		};
  		txtFilter.addModifyListener(filter.widgetModifyListener);
  		
  		if (txtFilter.getText().length() == 0) {
  			txtFilter.setText(filter.text);
  		} else {
  			filter.text = filter.nextText = txtFilter.getText();
  		}
		} else {
			filter.text = filter.nextText = "";
		}
		
		filter.checker = filterCheck;

		filter.checker.filterSet(filter.text);
		refilter();
	}
	
	public void disableFilterCheck()
	{
		if ( filter == null ){
			return;
		}
		
		if (filter.widget != null && !filter.widget.isDisposed()) {
			filter.widget.removeKeyListener(TableViewSWTImpl.this);
			filter.widget.removeModifyListener(filter.widgetModifyListener);
		}
		filter = null;
	}
	
	public boolean enableSizeSlider(Composite composite, final int min, final int max) {
		try {
			if (sliderArea != null && !sliderArea.isDisposed()) {
				sliderArea.dispose();
			}
			Class<?> claTable = Class.forName("org.eclipse.swt.widgets."
					+ (useTree ? "Tree" : "Table"));
			final Method method = claTable.getDeclaredMethod("setItemHeight", new Class<?>[] {
				int.class
			});
			method.setAccessible(true);

			composite.setLayout(new FormLayout());
			sliderArea = new Label(composite, SWT.NONE);
			((Label)sliderArea).setImage(ImageLoader.getInstance().getImage("zoom"));
			sliderArea.addListener(SWT.MouseUp, new Listener() {
				public void handleEvent(Event event) {
					final Shell shell = new Shell(sliderArea.getShell(), SWT.BORDER);
					Listener l = new Listener() {
						public void handleEvent(Event event) {
							if (event.type == SWT.MouseExit) {
								Control curControl = event.display.getCursorControl();
								Point curPos = event.display.getCursorLocation();
								Point curPosRelShell = shell.toControl(curPos);
								Rectangle bounds = shell.getBounds();
								bounds.x = bounds.y = 0;
								if (!bounds.contains(curPosRelShell)) {
									shell.dispose();
									return;
								}
								if (curControl != null
										&& (curControl == shell || curControl.getParent() == shell)) {
									return;
								}
							}
							shell.dispose();
						}
					};
					shell.setBackgroundMode(SWT.INHERIT_FORCE);
					shell.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
					shell.addListener(SWT.MouseExit, l);
					shell.addListener(SWT.Deactivate, l);
					FillLayout fillLayout = new FillLayout();
					fillLayout.marginHeight = 4;
					shell.setLayout(fillLayout);
					final Scale slider = new Scale(shell, SWT.VERTICAL);
					slider.addListener(SWT.MouseExit, l);
					slider.addListener(SWT.Deactivate, l);
					slider.setMinimum(min);
					slider.setMaximum(max);
					slider.setSelection(getRowDefaultHeight());
					try {
						method.invoke(table.getComposite(), new Object[] { slider.getSelection() } );
					} catch (Throwable e1) {
					}
					slider.addSelectionListener(new SelectionListener() {
						public void widgetSelected(SelectionEvent e) {
							setRowDefaultHeight(slider.getSelection());
							try {
								method.invoke(table.getComposite(), new Object[] { slider.getSelection() } );
							} catch (Throwable e1) {
								e1.printStackTrace();
							}
							tableInvalidate();
						}
						
						public void widgetDefaultSelected(SelectionEvent e) {
						}
					});
					Point pt = sliderArea.toDisplay(event.x - 2, event.y - 5);
					int width = Constants.isOSX ? 20 : 50;
					shell.setBounds(pt.x - (width / 2), pt.y, width, 120);
					shell.open();
				}
			});
			
			sliderArea.setLayoutData(Utils.getFilledFormData());
			composite.layout();
		} catch (Throwable t) {
			return false;
		}
		return true;
	}
	
	public void disableSizeSlider() {
		Utils.disposeSWTObjects(new Object[] { sliderArea });
	}
	
	public void setEnabled(final boolean enable) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (!isDisposed()) {
					table.setEnabled(enable);
					/*
					if (enable) {
						Image oldImage = table.getBackgroundImage();
						table.setBackgroundImage(null);
						Utils.disposeSWTObjects(new Object[] { oldImage } );
					} else {
						final Image image = new Image(table.getDisplay(), 50, 50);
						
						GC gc = new GC(image);
						gc.setBackground(ColorCache.getColor(gc.getDevice(), 0xee, 0xee, 0xee));
						gc.fillRectangle(0, 0, 50, 50);
						gc.dispose();
						table.addDisposeListener(new DisposeListener() {
							public void widgetDisposed(DisposeEvent e) {
								Utils.disposeSWTObjects(new Object[] { image } );
							}
						});
						
						table.setBackgroundImage(image);
					}
					*/
				}
			}
		});
	}
	
	public void addRowMouseListener(TableRowMouseListener listener) {
		try {
			mon_RowMouseListener.enter();

			if (rowMouseListeners == null)
				rowMouseListeners = new ArrayList<TableRowMouseListener>(1);

			rowMouseListeners.add(listener);

		} finally {
			mon_RowMouseListener.exit();
		}
	}

	public void removeRowMouseListener(TableRowMouseListener listener) {
		try {
			mon_RowMouseListener.enter();

			if (rowMouseListeners == null)
				return;

			rowMouseListeners.remove(listener);

		} finally {
			mon_RowMouseListener.exit();
		}
	}
	
	private void invokeRowMouseListener(TableRowMouseEvent event) {
		if (rowPaintListeners == null) {
			return;
		}
		ArrayList<TableRowMouseListener> listeners = new ArrayList<TableRowMouseListener>(
				rowMouseListeners);

		for (int i = 0; i < listeners.size(); i++) {
			try {
				TableRowMouseListener l = (listeners.get(i));

				l.rowMouseTrigger(event);

			} catch (Throwable e) {
				Debug.printStackTrace(e);
			}
		}
	}


	public void addRowPaintListener(TableRowSWTPaintListener listener) {
		try {
			mon_RowPaintListener.enter();

			if (rowPaintListeners == null)
				rowPaintListeners = new ArrayList<TableRowSWTPaintListener>(1);

			rowPaintListeners.add(listener);

		} finally {
			mon_RowPaintListener.exit();
		}
	}

	public void removeRowPaintListener(TableRowSWTPaintListener listener) {
		try {
			mon_RowPaintListener.enter();

			if (rowPaintListeners == null)
				return;

			rowPaintListeners.remove(listener);

		} finally {
			mon_RowPaintListener.exit();
		}
	}

	protected void invokePaintListeners(GC gc, TableRowCore row,
			TableColumnCore column, Rectangle cellArea) {
		if (rowPaintListeners == null) {
			return;
		}
		ArrayList<TableRowSWTPaintListener> listeners = new ArrayList<TableRowSWTPaintListener>(
				rowPaintListeners);

		for (int i = 0; i < listeners.size(); i++) {
			try {
				TableRowSWTPaintListener l = (listeners.get(i));

				l.rowPaint(gc, row, column, cellArea);

			} catch (Throwable e) {
				Debug.printStackTrace(e);
			}
		}
	}

	public boolean canHaveSubItems() {
		return useTree;
	}
	
	protected TableColumnCore[] getColumnsOrdered() {
		return columnsOrdered;
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#setSelectedRows(com.aelitis.azureus.ui.common.table.TableRowCore[])
	public void setSelectedRows(TableRowCore[] rows) {
		updateSelectedRows(rows, true);
	}
	
	public void setParentDataSource(Object newDataSource) {
		super.setParentDataSource(newDataSource);

		triggerTabViewsDataSourceChanged(true);
	}
	
	public void packColumns() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (table != null && !table.isDisposed()) {
					table.pack(true);
				}
			}
		});
	}
	
	public int getMaxItemShown() {
		return maxItemShown;
	}
	
	public void setMaxItemShown(int i) {
		maxItemShown  = i;
	}
}
