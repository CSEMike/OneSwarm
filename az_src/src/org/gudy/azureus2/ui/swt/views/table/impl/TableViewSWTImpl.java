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

import java.util.*;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.config.impl.ConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.util.Timer;
import org.gudy.azureus2.ui.common.util.MenuItemManager;
import org.gudy.azureus2.ui.swt.MenuBuildUtils;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.debug.ObfusticateImage;
import org.gudy.azureus2.ui.swt.debug.UIDebugGenerator;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTInstanceImpl;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.IView;
import org.gudy.azureus2.ui.swt.views.table.*;
import org.gudy.azureus2.ui.swt.views.table.utils.*;
import org.gudy.azureus2.ui.swt.views.utils.VerticalAligner;

import com.aelitis.azureus.ui.common.table.*;
import com.aelitis.azureus.ui.common.table.impl.TableViewImpl;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

import org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;

import org.gudy.azureus2.pluginsimpl.local.ui.tables.TableContextMenuItemImpl;

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
 */
public class TableViewSWTImpl
	extends TableViewImpl
	implements ParameterListener, TableViewSWT,
	TableStructureModificationListener, ObfusticateImage
	
{
	private final static LogIDs LOGID = LogIDs.GUI;

	/** Virtual Tables still a work in progress */
	// Non-Virtual tables scroll faster with they keyboard
	// Virtual tables don't flicker when updating a cell (Windows)
	private final static boolean DISABLEVIRTUAL = SWT.getVersion() < 3138;

	private final static boolean COLUMN_CLICK_DELAY = Constants.isOSX
			&& SWT.getVersion() >= 3221 && SWT.getVersion() <= 3222;

	private static final boolean DEBUG_SORTER = false;

	// Shorter name for ConfigManager, easier to read code
	private static final ConfigurationManager configMan = ConfigurationManager.getInstance();

	private static final String CFG_SORTDIRECTION = "config.style.table.defaultSortOrder";

	private static final long IMMEDIATE_ADDREMOVE_DELAY = 150;

	private static final long IMMEDIATE_ADDREMOVE_MAXDELAY = 2000;

	private static final long BREAKOFF_ADDTOMAP = 1000;

	private static final long BREAKOFF_ADDROWSTOSWT = 800;

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
	private boolean bSkipFirstColumn;

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
	private Table table;
	
	private TableEditor editor;

	/** SWT style options for the creation of the Table */
	protected int iTableStyle;

	/** Whether the Table is Virtual */
	private boolean bTableVirtual;

	/** Context Menu */
	private Menu menu;

	/** Context Menu specific to the column the mouse was on */
	private Menu menuThisColumn;

	/** Link DataSource to their row in the table.
	 * key = DataSource
	 * value = TableRowSWT
	 */
	private Map mapDataSourceToRow;

	private AEMonitor dataSourceToRow_mon = new AEMonitor("TableView:OTSI");

	private List sortedRows;

	private AEMonitor sortedRows_mon = new AEMonitor("TableView:sR");

	private AEMonitor sortColumn_mon = new AEMonitor("TableView:sC");

	/** Sorting functions */
	protected TableColumnCore sortColumn;

	/** TimeStamp of when last sorted all the rows was */
	private long lLastSortedOn;

	/* position of mouse in table.  Used for context menu. */
	private int iMouseX = -1;

	/** For updating GUI.  
	 * Some UI objects get updating every X cycles (user configurable) 
	 */
	protected int loopFactor;

	/** How often graphic cells get updated
	 */
	protected int graphicsUpdate = configMan.getIntParameter("Graphics Update");

	protected int reOrderDelay = configMan.getIntParameter("ReOrder Delay");

	/** Check Column Widths every 10 seconds on Pre 3.0RC1 on OSX if view is active.  
	 * Other OSes can capture column width changes automatically */
	private int checkColumnWidthsEvery = (Constants.isOSX && SWT.getVersion() < 3054)
			? 10000 / configMan.getIntParameter("GUI Refresh") : 0;

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
	private List dataSourcesToAdd = new ArrayList(4);

	/** Queue removed datasources and add them on refresh */
	private List dataSourcesToRemove = new ArrayList(4);

	private Timer timerProcessDataSources = new Timer("Process Data Sources");

	private TimerEvent timerEventProcessDS;

	private boolean bReallyAddingDataSources = false;

	/** TabViews */
	public boolean bEnableTabViews = false;

	/** TabViews */
	private CTabFolder tabFolder;

	/** TabViews */
	private ArrayList tabViews = new ArrayList(1);

	private int lastTopIndex = 0;

	private int lastBottomIndex = -1;

	protected IView[] coreTabViews = null;

	private long lCancelSelectionTriggeredOn = -1;

	// XXX Remove after column selection is no longered triggered on column resize (OSX)
	private long lLastColumnResizeOn = -1;

	private List listenersMenuFill = new ArrayList();

	private TableViewSWTPanelCreator mainPanelCreator;

	private List listenersKey = new ArrayList();
	
	private boolean columnPaddingAdjusted = false;
	
	private boolean columnVisibilitiesChanged = true;

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
	public TableViewSWTImpl(String _sTableID, String _sPropertiesPrefix,
			TableColumnCore[] _basicItems, String _sDefaultSortOn, int _iTableStyle) {
		sTableID = _sTableID;
		basicItems = _basicItems;
		sPropertiesPrefix = _sPropertiesPrefix;
		sDefaultSortOn = _sDefaultSortOn;
		iTableStyle = _iTableStyle | SWT.V_SCROLL;
		if (DISABLEVIRTUAL)
			iTableStyle &= ~(SWT.VIRTUAL);
		bTableVirtual = (iTableStyle & SWT.VIRTUAL) != 0;

		mapDataSourceToRow = new LightHashMap();
		sortedRows = new ArrayList();
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
	 * @param _basicItems Column Definitions
	 * @param _sDefaultSortOn Column name to sort on if user hasn't chosen one
	 *                         yet
	 */
	public TableViewSWTImpl(String _sTableID, String _sPropertiesPrefix,
			TableColumnCore[] _basicItems, String _sDefaultSortOn) {
		this(_sTableID, _sPropertiesPrefix, _basicItems, _sDefaultSortOn,
				SWT.SINGLE | SWT.FULL_SELECTION | SWT.VIRTUAL);
	}

	private void initializeColumnDefs() {
		// XXX Adding Columns only has to be done once per TableID.  
		// Doing it more than once won't harm anything, but it's a waste.
		TableColumnManager tcManager = TableColumnManager.getInstance();
		if (tcManager.getTableColumnCount(sTableID) != basicItems.length) {
			for (int i = 0; i < basicItems.length; i++) {
				tcManager.addColumn(basicItems[i]);
			}
		}

		// fixup order
		tcManager.ensureIntegrety(sTableID);

		tableColumns = tcManager.getAllTableColumnCoreAsArray(sTableID);
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#setColumnList(com.aelitis.azureus.ui.common.table.TableColumnCore[], java.lang.String)
	// XXX This isn't right
	public void setColumnList(TableColumnCore[] columns,
			String defaultSortColumnID, boolean defaultSortOrder,
			boolean titleIsMinWidth) {
		// XXX Adding Columns only has to be done once per TableID.  
		// Doing it more than once won't harm anything, but it's a waste.
		TableColumnManager tcManager = TableColumnManager.getInstance();
		if (tcManager.getTableColumnCount(sTableID) != columns.length) {
			for (int i = 0; i < columns.length; i++) {
				columns[i].setTableID(sTableID);
				tcManager.addColumn(columns[i]);
			}
		}

		// fixup order
		tcManager.ensureIntegrety(sTableID);

		tableColumns = tcManager.getAllTableColumnCoreAsArray(sTableID);
	}

	// AbstractIView::initialize
	public void initialize(Composite composite) {
		composite.setRedraw(false);
		mainComposite = createSashForm(composite);
		menu = createMenu();
		table = createTable(tableComposite);
		editor = new TableEditor(table);
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

		int iNumViews = coreTabViews == null ? 0 : coreTabViews.length;

		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		Map pluginViews = null;
		if (uiFunctions != null) {
			UISWTInstanceImpl pluginUI = uiFunctions.getSWTPluginInstanceImpl();

			if (pluginUI != null) {
				pluginViews = pluginUI.getViewListeners(sTableID);
				if (pluginViews != null)
					iNumViews += pluginViews.size();
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
		if (iSplitAt < 100)
			iSplitAt *= 100;

		double pct = iSplitAt / 10000.0;
		if (pct < 0.03)
			pct = 0.03;
		else if (pct > 0.97)
			pct = 0.97;

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

				if (FASTDRAG && e.detail == SWT.DRAG)
					return;

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
				if (e.detail != SWT.DRAG)
					configMan.setParameter(sPropertiesPrefix + ".SplitAt",
							(int) (l.doubleValue() * 10000));
			}
		});

		final CTabFolder2Adapter folderListener = new CTabFolder2Adapter() {
			public void minimize(CTabFolderEvent event) {
				tabFolder.setMinimized(true);
				tabFolderData.height = iFolderHeightAdj;
				form.layout();

				configMan.setParameter(sPropertiesPrefix + ".subViews.minimized", true);
			}

			public void restore(CTabFolderEvent event) {
				tabFolder.setMinimized(false);
				form.notifyListeners(SWT.Resize, null);

				refreshSelectedSubView();

				configMan.setParameter(sPropertiesPrefix + ".subViews.minimized", false);
			}
		};
		tabFolder.addCTabFolder2Listener(folderListener);

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
				if (tabFolder.getMinimized())
					return;

				Double l = (Double) sash.getData("PCT");
				if (l != null) {
					tabFolderData.height = (int) (form.getBounds().height * l.doubleValue())
							- iFolderHeightAdj;
					form.layout();
				}
			}
		});

		if (coreTabViews != null)
			for (int i = 0; i < coreTabViews.length; i++)
				addTabView(coreTabViews[i]);

		// Call plugin listeners
		if (pluginViews != null) {
			String[] sNames = (String[]) pluginViews.keySet().toArray(new String[0]);
			for (int i = 0; i < sNames.length; i++) {
				UISWTViewEventListener l = (UISWTViewEventListener) pluginViews.get(sNames[i]);
				if (l != null) {
					try {
						UISWTViewImpl view = new UISWTViewImpl(sTableID, sNames[i], l);
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
		Composite panel = new Composite(composite, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		panel.setLayout(layout);

		return panel;
	}

	/** Creates the Table.
	 *
	 * @return The created Table.
	 */
	public Table createTable(Composite panel) {
		table = new Table(panel, iTableStyle);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));

		return table;
	}

	/** Sets up the sorter, columns, and context menu.
	 *
	 * @param table Table to be initialized
	 */
	public void initializeTable(final Table table) {
		initializeColumnDefs();

		iTableStyle = table.getStyle();
		bTableVirtual = (iTableStyle & SWT.VIRTUAL) != 0;

		table.setLinesVisible(Utils.TABLE_GRIDLINE_IS_ALTERNATING_COLOR);
		table.setMenu(menu);
		table.setData("Name", sTableID);
		table.setData("TableView", this);

		// Setup table
		// -----------

		// XXX On linux (an other OSes?), changing the column indicator doesn't 
		//     work until the table is shown.  Since SWT.Show doesn't trigger,
		//     use the first paint trigger.
		if (!Utils.SWT32_TABLEPAINT) {
			table.addPaintListener(new PaintListener() {
				boolean first = true;

				public void paintControl(PaintEvent event) {
					if (first) {
						changeColumnIndicator();
						// This fixes the scrollbar not being long enough on Win2k
						// There may be other methods to get it to refresh right, but
						// layout(true, true) didn't work.
						table.setRedraw(false);
						table.setRedraw(true);
						first = false;
					}
					if (event.width == 0 || event.height == 0)
						return;
					visibleRowsChanged();
					doPaint(event.gc);
				}
			});
		}

		if (Utils.SWT32_TABLEPAINT) {
			// SWT 3.2 only.  Code Ok -- Only called in SWT 3.2 mode
			table.addListener(SWT.PaintItem, new Listener() {
				public void handleEvent(Event event) {
					paintItem(event);
				}
			});
			table.addListener(SWT.EraseItem, new Listener() {
				public void handleEvent(Event event) {
				}
			});
		}
		
		table.getHorizontalBar().addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				columnVisibilitiesChanged = true;				
			}
			
			public void widgetSelected(SelectionEvent e) {
				columnVisibilitiesChanged = true;
			}
		});
		
		table.addListener(SWT.MeasureItem, new Listener() {
			public void handleEvent(Event event) {
				int defaultHeight = getRowDefaultHeight();
				if (event.height < defaultHeight) {
					event.height = defaultHeight;
				}
			}
		});

		// Deselect rows if user clicks on a blank spot (a spot with no row)
		table.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				TableColumnCore tc = getTableColumnByOffset(e.x);
				TableCellSWT cell = getTableCell(e.x, e.y);
				if (cell != null && tc != null) {
					TableCellMouseEvent event = createMouseEvent(cell, e,
							TableCellMouseEvent.EVENT_MOUSEDOUBLECLICK);
					if (event != null) {
						tc.invokeCellMouseListeners(event);
						cell.invokeMouseListeners(event);
						if (event.skipCoreFunctionality) {
							lCancelSelectionTriggeredOn = System.currentTimeMillis();
						}
					}
				}
			}

			public void mouseUp(MouseEvent e) {
				TableColumnCore tc = getTableColumnByOffset(e.x);
				TableCellSWT cell = getTableCell(e.x, e.y);
				if (cell != null && tc != null) {
					TableCellMouseEvent event = createMouseEvent(cell, e,
							TableCellMouseEvent.EVENT_MOUSEUP);
					if (event != null) {
						tc.invokeCellMouseListeners(event);
						cell.invokeMouseListeners(event);
						if (event.skipCoreFunctionality) {
							lCancelSelectionTriggeredOn = System.currentTimeMillis();
						}
					}
				}
			}

			public void mouseDown(MouseEvent e) {
				TableColumnCore tc = getTableColumnByOffset(e.x);
				TableCellSWT cell = getTableCell(e.x, e.y);
				
				editCell(-1, -1); // clear out current cell editor
				
				if (cell != null && tc != null) {
					if (e.button == 2 && e.stateMask == SWT.CONTROL) {
						((TableCellImpl) cell).bDebug = !((TableCellImpl) cell).bDebug;
						System.out.println("Set debug for " + cell + " to "
								+ ((TableCellImpl) cell).bDebug);
					}
					TableCellMouseEvent event = createMouseEvent(cell, e,
							TableCellMouseEvent.EVENT_MOUSEDOWN);
					if (event != null) {
						tc.invokeCellMouseListeners(event);
						cell.invokeMouseListeners(event);
						if (event.skipCoreFunctionality) {
							lCancelSelectionTriggeredOn = System.currentTimeMillis();
						}
					}
					if(tc.isInplaceEdit() && e.button == 1)
						editCell(getColumnNo(e.x), cell.getTableRowCore().getIndex());
				}

				iMouseX = e.x;
				try {
					if (table.getItemCount() <= 0)
						return;

					// skip if outside client area (ie. scrollbars)
					Rectangle rTableArea = table.getClientArea();
					//System.out.println("Mouse="+iMouseX+"x"+e.y+";TableArea="+rTableArea);
					Point pMousePosition = new Point(e.x, e.y);
					if (rTableArea.contains(pMousePosition)) {
						
						int[] columnOrder = table.getColumnOrder();
						if (columnOrder.length == 0) {
							return;
						}
						TableItem ti = table.getItem(table.getItemCount() - 1);
						Rectangle cellBounds = ti.getBounds(columnOrder[columnOrder.length - 1]);
						// OSX returns 0 size if the cell is not on screen (sometimes? all the time?)
						if (cellBounds.width <= 0 || cellBounds.height <= 0)
							return;
						//System.out.println("cellbounds="+cellBounds);
						if (e.x > cellBounds.x + cellBounds.width
								|| e.y > cellBounds.y + cellBounds.height) {
							table.deselectAll();
						}
						/*        // This doesn't work because of OS inconsistencies when table is scrolled
						 // Re-enable once SWT fixes the problem
						 // Bug 103934: Table.getItem(Point) uses incorrect calculation on Motif
						 //             Fixed 20050718 SWT 3.2M1 (3201) & SWT 3.1.1 (3139)
						 // TODO: Get Build IDs and use this code (if it works)
						 TableItem ti = table.getItem(pMousePosition);
						 if (ti == null)
						 table.deselectAll();
						 */
					}
				} catch (Exception ex) {
					System.out.println("MouseDownError");
					Debug.printStackTrace(ex);
				}
			}
		});

		table.addMouseMoveListener(new MouseMoveListener() {
			TableCellSWT lastCell = null;

			int lastCursorID = -1;

			public void mouseMove(MouseEvent e) {
				try {
					iMouseX = e.x;

					TableCellSWT cell = getTableCell(e.x, e.y);
					int iCursorID = -1;
					if (cell == null) {
						lastCell = null;
					} else if (cell != lastCell) {
						iCursorID = cell.getCursorID();
						lastCell = cell;
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
								TableCellMouseEvent.EVENT_MOUSEMOVE);
						if (event != null) {
  						TableColumnCore tc = ((TableColumnCore) cell.getTableColumn());
  						if (tc.hasCellMouseMoveListener()) {
  							((TableColumnCore) cell.getTableColumn()).invokeCellMouseListeners(event);
  						}
  						cell.invokeMouseListeners(event);
						}
					}
				} catch (Exception ex) {
					Debug.out(ex);
				}
			}
		});

		table.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				triggerSelectionListeners(new TableRowCore[] {
					getRow((TableItem) event.item)
				});

				if (tabViews == null || tabViews.size() == 0)
					return;

				// Set Data Object for all tabs.  Tabs of PluginView are sent the plugin
				// Peer object, while Tabs of IView are sent the core PEPeer object.

				// TODO: Send all datasources
				Object[] dataSourcesCore = getSelectedDataSources(true);
				Object[] dataSourcesPlugin = null;

				for (int i = 0; i < tabViews.size(); i++) {
					IView view = (IView) tabViews.get(i);
					if (view != null) {
						if (view instanceof UISWTViewImpl) {
							if (dataSourcesPlugin == null)
								dataSourcesPlugin = getSelectedDataSources(false);

							((UISWTViewImpl) view).dataSourceChanged(dataSourcesPlugin.length == 0
									? null : dataSourcesPlugin);
						} else {
							view.dataSourceChanged(dataSourcesCore.length == 0 ? null
									: dataSourcesCore);
						}
					}
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				if (lCancelSelectionTriggeredOn > 0
						&& System.currentTimeMillis() - lCancelSelectionTriggeredOn < 200) {
					e.doit = false;
					lCancelSelectionTriggeredOn = -1;
				} else {
					runDefaultAction();
				}
			}
		});

		// we are sent a SWT.Settings event when the language changes and
		// when System fonts/colors change.  In both cases, invalidate
		if (SWT.getVersion() > 3200) {
			table.addListener(SWT.Settings, new Listener() {
				public void handleEvent(Event e) {
					tableInvalidate();
				}
			});
		}

		new TableTooltips(this, table);

		table.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent event) {
				Object[] listeners = listenersKey.toArray();
				for (int i = 0; i < listeners.length; i++) {
					KeyListener l = (KeyListener) listeners[i];
					l.keyPressed(event);
					if (!event.doit) {
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
					} else {
						sortColumn(true);
					}
					event.doit = false;
					return;
				}

				int key = event.character;
				if (key <= 26 && key > 0)
					key += 'a' - 1;

				if (event.stateMask == SWT.MOD1) {
					switch (key) {
						case 'a': // CTRL+A select all Torrents
							if ((table.getStyle() & SWT.MULTI) > 0) {
								selectAll();
								event.doit = false;
							}
							break;

						case '+': {
							if (Constants.isUnix) {
								TableColumn[] tableColumnsSWT = table.getColumns();
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
					}

					if (!event.doit)
						return;
				}
			}

			public void keyReleased(KeyEvent event) {
				Object[] listeners = listenersKey.toArray();
				for (int i = 0; i < listeners.length; i++) {
					KeyListener l = (KeyListener) listeners[i];
					l.keyReleased(event);
					if (!event.doit) {
						return;
					}
				}
			}
		});

		ScrollBar bar = table.getVerticalBar();
		if (bar != null) {
			bar.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					// Bug: Scroll is slow when table is not focus
					if (!table.isFocusControl()) {
						table.setFocus();
					}
				}
			});
		}

		table.setHeaderVisible(true);

		initializeTableColumns(table);
	}
	
	private interface SourceReplaceListener
	{
		void sourcesChanged();
		
		void cleanup(Text toClean);
	}
	
	private SourceReplaceListener cellEditNotifier;	
	
	private void editCell(final int column, final int row)
	{
		Text oldInput = (Text)editor.getEditor();
		if(column >= table.getColumnCount() || row < 0 || row >= table.getItemCount())
		{
			cellEditNotifier = null;
			if(oldInput != null && !oldInput.isDisposed())
				editor.getEditor().dispose();
			return;
		}
		
		TableColumn tcColumn = table.getColumn(column);
		final TableItem item = table.getItem(row);

		String cellName = (String) tcColumn.getData("Name");
		final TableRowSWT rowSWT = (TableRowSWT)getRow(row);
		final TableCellSWT cell = rowSWT.getTableCellSWT(cellName);
		
		// reuse widget if possible, this way we'll keep the focus all the time on jumping through the rows
		final Text newInput = oldInput == null || oldInput.isDisposed() ? new Text(table,SWT.BORDER) : oldInput;
		final Object datasource = cell.getDataSource();
		if(cellEditNotifier != null )
			cellEditNotifier.cleanup(newInput);
		
		table.showItem(item);
		table.showColumn(tcColumn);
		
		newInput.setText(cell.getText());
		
		newInput.setSelection(0);
		newInput.selectAll();
		newInput.setFocus();

		class QuickEditListener implements ModifyListener, SelectionListener, KeyListener, TraverseListener, SourceReplaceListener, ControlListener
		{
			boolean resizing = true;
			
			public QuickEditListener(Text toAttach)
			{
				toAttach.addModifyListener(this);
				toAttach.addSelectionListener(this);
				toAttach.addKeyListener(this);
				toAttach.addTraverseListener(this);
				toAttach.addControlListener(this);
				
				cellEditNotifier = this;				
			}
			
			public void modifyText(ModifyEvent e) {
				if(item.isDisposed())
				{
					sourcesChanged();
					return;
				}
				if(((TableColumnCore)cell.getTableColumn()).inplaceValueSet(cell, newInput.getText(), false))
					newInput.setBackground(null);
				else
					newInput.setBackground(Colors.colorErrorBG);
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
				if(item.isDisposed())
				{
					sourcesChanged();
					newInput.traverse(SWT.TRAVERSE_RETURN);
					return;
				}
				((TableColumnCore)cell.getTableColumn()).inplaceValueSet(cell, newInput.getText(), true);
				rowSWT.invalidate();
				editCell(column, row + 1);
			}
			
			public void widgetSelected(SelectionEvent e) {}
			
			public void keyPressed(KeyEvent e) {
				if(e.keyCode == SWT.ARROW_DOWN || e.keyCode == SWT.ARROW_UP)
				{
					e.doit = false;
					editCell(column, row + (e.keyCode == SWT.ARROW_DOWN ? 1 : -1));
				}
			}
			
			public void keyReleased(KeyEvent e) {}
			
			public void keyTraversed(TraverseEvent e) {
				if(e.detail == SWT.TRAVERSE_ESCAPE)
				{
					e.doit = false;
					editCell(column, -1);
				}
			}
			
			public void sourcesChanged() {
				if(getRow(datasource) == rowSWT || getRow(datasource) == null || newInput.isDisposed())
					return;
				String newVal = newInput.getText();
				Point  sel = newInput.getSelection();
				editCell(column, getRow(datasource).getIndex());
				if(newInput.isDisposed())
					return;
				newInput.setText(newVal);
				newInput.setSelection(sel);
			}
			
			public void cleanup(Text oldText) {
				if(!oldText.isDisposed())
				{
					oldText.removeModifyListener(this);
					oldText.removeSelectionListener(this);
					oldText.removeKeyListener(this);
					oldText.removeTraverseListener(this);
					oldText.removeControlListener(this);
				}
			}
			
			public void controlMoved(ControlEvent e) {
				table.showItem(item);
				if(resizing)
					return;
				resizing = true;
				
				Point sel = newInput.getSelection();
				
				editor.setEditor(newInput, item, column);
				
				editor.minimumWidth = newInput.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
				
				Rectangle leftAlignedBounds = item.getBounds(column);
				leftAlignedBounds.width = editor.minimumWidth = newInput.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
				if(leftAlignedBounds.intersection(table.getClientArea()).equals(leftAlignedBounds))
					editor.horizontalAlignment = SWT.LEFT;
				else
					editor.horizontalAlignment = SWT.RIGHT;
				
				editor.layout();
				
				newInput.setSelection(0);				
				newInput.setSelection(sel);
				
				resizing = false;
			}
			
			public void controlResized(ControlEvent e) {}
		}
		
		QuickEditListener l = new QuickEditListener(newInput);
		
		l.modifyText(null);
		
		editor.setEditor(newInput, item, column);
		table.deselectAll();
		table.select(row);
		
		l.resizing = false;
		
		l.controlMoved(null);
	}

	private TableCellMouseEvent createMouseEvent(TableCellSWT cell, MouseEvent e,
			int type) {
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
		Rectangle r = cell.getBounds();
		event.x = e.x - r.x + VerticalAligner.getTableAdjustHorizontallyBy(table);
		if (event.x < 0) {
			return null;
		}
		event.y = e.y - r.y + VerticalAligner.getTableAdjustVerticalBy(table);
		if (event.y < 0) {
			return null;
		}

		return event;
	}

	/**
	 * @param event
	 */
	protected void paintItem(Event event) {
		TableItem item = (TableItem) event.item;
		if (item == null || item.isDisposed()) {
			return;
		}

		TableRowSWT row = (TableRowSWT) getRow(item);
		if (row == null) {
			return;
		}

		// SWT 3.2 only.  Code Ok -- Only called in SWT 3.2 mode
		Rectangle cellBounds = item.getBounds(event.index);

		cellBounds.x += 3;
		cellBounds.width -= 6;

		try {
			// SWT 3.2 only.  Code Ok -- Only called in SWT 3.2 mode
			int iColumnNo = event.index;

			if (item.getImage(iColumnNo) != null) {
				cellBounds.x += 18;
				cellBounds.width -= 18;
			}

			if (cellBounds.width <= 0 || cellBounds.height <= 0) {
				return;
			}

			if (bSkipFirstColumn) {
				if (iColumnNo == 0) {
					return;
				}
				iColumnNo--;
			}

			if (iColumnNo >= columnsOrdered.length) {
				System.out.println(iColumnNo + " >= " + columnsOrdered.length);
				return;
			}

			TableCellSWT cell = row.getTableCellSWT(columnsOrdered[iColumnNo].getName());

			if (!cell.isUpToDate()) {
				//System.out.println("R " + table.indexOf(item));
				cell.refresh(true, true);
				return;
			}

			//System.out.println("PS " + table.indexOf(item) + ";" + cellBounds);
			GCStringPrinter.printString(event.gc, cell.getText(), cellBounds, true,
					true,
					CoreTableColumn.getSWTAlign(columnsOrdered[iColumnNo].getAlignment()));

			if (cell.needsPainting()) {
				cell.doPaint(event.gc);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void runDefaultAction() {
		// plugin may have cancelled the default action

		if (lCancelSelectionTriggeredOn > 0
				&& System.currentTimeMillis() - lCancelSelectionTriggeredOn < 200) {
			lCancelSelectionTriggeredOn = -1;
		} else {
			TableRowCore[] selectedRows = getSelectedRows();
			triggerDefaultSelectedListeners(selectedRows);
		}
	}
	
	private void updateColumnVisibilities() {
		TableColumn[] columns = table.getColumns();
		int topIdx = table.getTopIndex();
		if (topIdx < 0 || table.getItemCount() < 1)
			return;
		columnVisibilitiesChanged = false;
		TableItem topRow = table.getItem(topIdx);
		Rectangle tableArea = table.getClientArea();
		for (int i = 0; i < columns.length; i++)
		{
			TableColumnCore tc = (TableColumnCore) columns[i].getData("TableColumnCore");
			if (tc == null)
				continue;
			Rectangle size = topRow.getBounds(i);
			size.intersect(tableArea);
			columnsVisible[tc.getPosition()] = !size.isEmpty();
		}
	}
	
	public boolean isColumnVisible(org.gudy.azureus2.plugins.ui.tables.TableColumn column)
	{
		return columnsVisible[column.getPosition()];
	}

	protected void initializeTableColumns(final Table table) {
		TableColumn[] oldColumns = table.getColumns();

		if (SWT.getVersion() >= 3100)
			for (int i = 0; i < oldColumns.length; i++)
				oldColumns[i].removeListener(SWT.Move, columnMoveListener);

		for (int i = oldColumns.length - 1; i >= 0; i--)
			oldColumns[i].dispose();

		columnPaddingAdjusted = false;

		// Pre 3.0RC1 SWT on OSX doesn't call this!! :(
		ControlListener resizeListener = new ControlAdapter() {
			// Bug: getClientArea() eventually calls back to controlResized,
			//      creating a loop until a stack overflow
			private boolean bInFunction = false;

			public void controlResized(ControlEvent e) {
				TableColumn column = (TableColumn) e.widget;
				if (column == null || column.isDisposed() || bInFunction)
					return;

				try {
					bInFunction = true;
					
					TableColumnCore tc = (TableColumnCore) column.getData("TableColumnCore");
					if (tc != null) {
						Long lPadding = (Long) column.getData("widthOffset");
						int padding = (lPadding == null) ? 0 : lPadding.intValue();
						tc.setWidth(column.getWidth() - padding);
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
		bSkipFirstColumn = bSkipFirstColumn && !Constants.isOSX;

		if (bSkipFirstColumn) {
			TableColumn tc = new TableColumn(table, SWT.NULL);
			tc.setWidth(0);
			tc.setResizable(false);
		}

		TableColumnCore[] tmpColumnsOrdered = new TableColumnCore[tableColumns.length];
		//Create all columns
		int columnOrderPos = 0;
		Arrays.sort(tableColumns, TableColumnManager.getTableColumnOrderComparator());
		for (int i = 0; i < tableColumns.length; i++) {
			int position = tableColumns[i].getPosition();
			if (position != -1 && tableColumns[i].isVisible()) {
				new TableColumn(table, SWT.NULL);
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
			int position = tableColumns[i].getPosition();
			if (position == -1 || !tableColumns[i].isVisible()) {
				continue;
			}

			String sName = tableColumns[i].getName();
			// +1 for Eclipse Bug 43910 (see above)
			// user has reported a problem here with index-out-of-bounds - not sure why
			// but putting in a preventative check so that hopefully the view still opens
			// so they can fix it

			if (swtColumnPos >= numSWTColumns) {
				Debug.out("Incorrect table column setup, skipping column '" + sName
						+ "', position=" + swtColumnPos + ";numCols=" + numSWTColumns);
				continue;
			}

			TableColumn column = table.getColumn(swtColumnPos);
			try {
				column.setMoveable(true);
			} catch (NoSuchMethodError e) {
				// Ignore < SWT 3.1
			}
			column.setAlignment(CoreTableColumn.getSWTAlign(tableColumns[i].getAlignment()));
			Messages.setLanguageText(column, tableColumns[i].getTitleLanguageKey());
			column.setWidth(tableColumns[i].getWidth());
			if (tableColumns[i].getMinWidth() == tableColumns[i].getMaxWidth()
					&& tableColumns[i].getMinWidth() > 0) {
				column.setResizable(false);
			}
			column.setData("TableColumnCore", tableColumns[i]);
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
		if (sSortColumn == null) {
			sSortColumn = sDefaultSortOn;
		}

		TableColumnCore tc = tcManager.getTableColumnCore(sTableID, sSortColumn);
		if (tc == null) {
			tc = tableColumns[0];
		}
		sortColumn = tc;
		changeColumnIndicator();

		// Add move listener at the very end, so we don't get a bazillion useless 
		// move triggers
		if (SWT.getVersion() >= 3100) {
			Listener columnResizeListener = (!COLUMN_CLICK_DELAY) ? null
					: new Listener() {
						public void handleEvent(Event event) {
							lLastColumnResizeOn = System.currentTimeMillis();
						}
					};

			TableColumn[] columns = table.getColumns();
			for (int i = 0; i < columns.length; i++) {
				TableColumn column = columns[i];
				column.addListener(SWT.Move, columnMoveListener);
				if (COLUMN_CLICK_DELAY)
					column.addListener(SWT.Resize, columnResizeListener);
			}
		}
		
		columnVisibilitiesChanged = true;
	}

	/** Creates the Context Menu.
	 *
	 * @return a new Menu object
	 */
	public Menu createMenu() {
		final Menu menu = new Menu(tableComposite.getShell(), SWT.POP_UP);
		MenuBuildUtils.addMaintenanceListenerForMenu(menu,
				new MenuBuildUtils.MenuBuilder() {
					public void buildMenu(Menu menu) {
						fillMenu(menu);
						addThisColumnSubMenu(getColumnNo(iMouseX));
					}
				});

		return menu;
	}

	/** Fill the Context Menu with items.  Called when menu is about to be shown.
	 *
	 * By default, a "Edit Columns" menu and a Column specific menu is set up.
	 *
	 * @param menu Menu to fill
	 */
	public void fillMenu(Menu menu) {
		Object[] listeners = listenersMenuFill.toArray();
		for (int i = 0; i < listeners.length; i++) {
			TableViewSWTMenuFillListener l = (TableViewSWTMenuFillListener) listeners[i];
			l.fillMenu(menu);
		}

		final MenuItem itemChangeTable = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemChangeTable,
				"MyTorrentsView.menu.editTableColumns");
		Utils.setMenuItemImage(itemChangeTable, "columns");

		itemChangeTable.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				new TableColumnEditorWindow(table.getShell(), sTableID, tableColumns,
						getFocusedRow(),
						TableStructureEventDispatcher.getInstance(sTableID));
			}
		});

		menuThisColumn = new Menu(tableComposite.getShell(), SWT.DROP_DOWN);
		final MenuItem itemThisColumn = new MenuItem(menu, SWT.CASCADE);
		itemThisColumn.setMenu(menuThisColumn);

		// Add Plugin Context menus..
		boolean enable_items = table != null && table.getSelection().length > 0;

		TableContextMenuItem[] items = TableContextMenuManager.getInstance().getAllAsArray(
				sTableID);
		
		// We'll add download-context specific menu items - if the table is download specific.
		// We need a better way to determine this...
		org.gudy.azureus2.plugins.ui.menus.MenuItem[] menu_items = null;
		if ("MySeeders".equals(sTableID) || "MyTorrents".equals(sTableID)) {
			menu_items = MenuItemManager.getInstance().getAllAsArray("download_context");
		}
		else {
			menu_items = MenuItemManager.getInstance().getAllAsArray((String)null);
		}
		if (items.length > 0 || menu_items.length > 0) {
			new org.eclipse.swt.widgets.MenuItem(menu, SWT.SEPARATOR);
			MenuBuildUtils.addPluginMenuItems(getComposite(), items, menu, true,
					enable_items, new MenuBuildUtils.PluginMenuController() {
						public Listener makeSelectionListener(
								final org.gudy.azureus2.plugins.ui.menus.MenuItem plugin_menu_item) {
							return new TableSelectedRowsListener(TableViewSWTImpl.this) {
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
			
			// Add download context menu items.
			if (menu_items != null) {
				// getSelectedDataSources(false) returns us plugin items.
    			MenuBuildUtils.addPluginMenuItems(getComposite(), menu_items, menu, true, true,
						new MenuBuildUtils.MenuItemPluginMenuControllerImpl(getSelectedDataSources(false))
    			);				
			}
		}
	}

	/**
	 * SubMenu for column specific tasks. 
	 *
	 * @param iColumn Column # that tasks apply to.
	 */
	private void addThisColumnSubMenu(int iColumn) {
		MenuItem item;

		if (menuThisColumn == null || menuThisColumn.isDisposed())
			return;

		// Dispose of the old items
		MenuItem[] oldItems = menuThisColumn.getItems();
		for (int i = 0; i < oldItems.length; i++) {
			oldItems[i].dispose();
		}

		item = menuThisColumn.getParentItem();
		if (iColumn == -1) {
			item.setEnabled(false);
			item.setText(MessageText.getString("GenericText.column"));
			return;
		}

		item.setEnabled(true);

		final TableColumn tcColumn = table.getColumn(iColumn);
		item.setText("'" + tcColumn.getText() + "' "
				+ MessageText.getString("GenericText.column"));

		menu.setData("ColumnNo", new Long(iColumn));
		menu.setData("column", tcColumn);
		
		String sColumnName = (String) tcColumn.getData("Name");
		if (sColumnName != null) {
			Object[] listeners = listenersMenuFill.toArray();
			for (int i = 0; i < listeners.length; i++) {
				TableViewSWTMenuFillListener l = (TableViewSWTMenuFillListener) listeners[i];
				l.addThisColumnSubMenu(sColumnName, menuThisColumn);
			}
		}

		if (menuThisColumn.getItemCount() > 0) {
			new MenuItem(menuThisColumn, SWT.SEPARATOR);
		}

		item = new MenuItem(menuThisColumn, SWT.PUSH);
		Messages.setLanguageText(item, "MyTorrentsView.menu.thisColumn.sort");
		item.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				int iColumn = ((Long) menu.getData("ColumnNo")).intValue();
				table.getColumn(iColumn).notifyListeners(SWT.Selection, new Event());
			}
		});
		
		final MenuItem at_item = new MenuItem(menuThisColumn, SWT.CHECK);
		Messages.setLanguageText(at_item, "MyTorrentsView.menu.thisColumn.autoTooltip");
		at_item.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				TableColumn tc = (TableColumn)menu.getData("column");
				TableColumnCore tcc = (TableColumnCore)tc.getData("TableColumnCore");
				tcc.setAutoTooltip(at_item.getSelection());
				tcc.invalidateCells();
			}			
		});
		at_item.setSelection(((TableColumnCore)tcColumn.getData("TableColumnCore")).doesAutoTooltip());
		
		item = new MenuItem(menuThisColumn, SWT.PUSH);
		Messages.setLanguageText(item, "MyTorrentsView.menu.thisColumn.remove");
		item.setEnabled(false);

		item = new MenuItem(menuThisColumn, SWT.PUSH);
		Messages.setLanguageText(item, "MyTorrentsView.menu.thisColumn.toClipboard");
		item.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				String sToClipboard = "";
				int iColumn = ((Long) menu.getData("ColumnNo")).intValue();
				TableItem[] tis = table.getSelection();
				for (int i = 0; i < tis.length; i++) {
					if (i != 0)
						sToClipboard += "\n";
					sToClipboard += tis[i].getText(iColumn);
				}
				new Clipboard(mainComposite.getDisplay()).setContents(new Object[] {
					sToClipboard
				}, new Transfer[] {
					TextTransfer.getInstance()
				});
			}
		});

		// Add Plugin Context menus..
		TableColumnCore tc = (TableColumnCore) tcColumn.getData("TableColumnCore");
		TableContextMenuItem[] items = tc.getContextMenuItems();
		if (items.length > 0) {
			new MenuItem(menuThisColumn, SWT.SEPARATOR);

			for (int i = 0; i < items.length; i++) {
				final TableContextMenuItemImpl contextMenuItem = (TableContextMenuItemImpl) items[i];
				final MenuItem menuItem = new MenuItem(menuThisColumn, SWT.PUSH);

				Messages.setLanguageText(menuItem, contextMenuItem.getResourceKey());
				menuItem.addListener(SWT.Selection,
						new TableSelectedRowsListener(this) {
							public boolean run(TableRowCore[] rows) {
								contextMenuItem.invokeListenersMulti(rows);
								return true;
							}
						});
			}
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

	public IView getActiveSubView() {
		if (!bEnableTabViews || tabFolder == null || tabFolder.isDisposed()
				|| tabFolder.getMinimized())
			return null;

		CTabItem item = tabFolder.getSelection();
		if (item != null) {
			return (IView) item.getData("IView");
		}

		return null;
	}

	public void refreshSelectedSubView() {
		IView view = getActiveSubView();
		if (view != null)
			view.refresh();
	}

	// see common.TableView
	public void refreshTable(final boolean bForceSort) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				_refreshTable(bForceSort);

				if (bEnableTabViews && tabFolder != null && !tabFolder.isDisposed()
						&& !tabFolder.getMinimized())
					refreshSelectedSubView();
			}
		});

		triggerTableRefreshListeners();
	}

	private void _refreshTable(boolean bForceSort) {
		// don't refresh while there's no table
		if (table == null)
			return;

		// XXX Try/Finally used to be there for monitor.enter/exit, however
		//     this doesn't stop re-entry from the same thread while already in
		//     process.. need a bAlreadyRefreshing variable instead
		try {
			if (getComposite() == null || getComposite().isDisposed())
				return;

			if (checkColumnWidthsEvery != 0
					&& (loopFactor % checkColumnWidthsEvery) == 0) {
				TableColumn[] tableColumnsSWT = table.getColumns();
				for (int i = 0; i < tableColumnsSWT.length; i++) {
					TableColumnCore tc = (TableColumnCore) tableColumnsSWT[i].getData("TableColumnCore");
					if (tc != null && tc.getWidth() != tableColumnsSWT[i].getWidth()) {
						tc.setWidth(tableColumnsSWT[i].getWidth());

						int columnNumber = table.indexOf(tableColumnsSWT[i]);
						locationChanged(columnNumber);
					}
				}
			}
			
			if(columnVisibilitiesChanged == true)
				updateColumnVisibilities();

			final boolean bDoGraphics = (loopFactor % graphicsUpdate) == 0;
			final boolean bWillSort = bForceSort || (reOrderDelay != 0)
					&& ((loopFactor % reOrderDelay) == 0);
			//System.out.println("Refresh.. WillSort? " + bWillSort);

			if (bWillSort) {
				if (bForceSort) {
					sortColumn.setLastSortValueChange(SystemTime.getCurrentTime());
				}
				sortColumn(true);
			}

			long lTimeStart = SystemTime.getMonotonousTime();

			//Refresh all visible items in table...
			runForAllRows(new TableGroupRowVisibilityRunner() {
				public void run(TableRowCore row, boolean bVisible) {
					row.refresh(bDoGraphics, bVisible);
				}
			});

			if (DEBUGADDREMOVE) {
				long lTimeDiff = (SystemTime.getMonotonousTime() - lTimeStart);
				if (lTimeDiff > 500)
					debug(lTimeDiff + "ms to refresh rows");
			}

			loopFactor++;
		} finally {
		}
	}

	private void refreshVisibleRows() {
		if (getComposite() == null || getComposite().isDisposed())
			return;

		runForVisibleRows(new TableGroupRowRunner() {
			public void run(TableRowCore row) {
				row.setAlternatingBGColor(true);
				row.refresh(false, true);
			}
		});
	}

	// see common.TableView
	public void processDataSourceQueue() {
		Object[] dataSourcesAdd = null;
		Object[] dataSourcesRemove = null;

		try {
			dataSourceToRow_mon.enter();
			if (dataSourcesToAdd.size() > 0) {
				dataSourcesAdd = dataSourcesToAdd.toArray();
				dataSourcesToAdd.clear();

				// remove the ones we are going to add then delete
				if (dataSourcesToRemove.size() > 0) {
					for (int i = 0; i < dataSourcesAdd.length; i++)
						if (dataSourcesToRemove.contains(dataSourcesAdd[i])) {
							dataSourcesToRemove.remove(dataSourcesAdd[i]);
							dataSourcesAdd[i] = null;
							if (DEBUGADDREMOVE)
								debug("Saved time by not adding a row that was removed");
						}
				}
			}

			if (dataSourcesToRemove.size() > 0) {
				dataSourcesRemove = dataSourcesToRemove.toArray();
				if (DEBUGADDREMOVE && dataSourcesRemove.length > 1)
					debug("Streamlining removing " + dataSourcesRemove.length + " rows");
				dataSourcesToRemove.clear();
			}
		} finally {
			dataSourceToRow_mon.exit();
		}

		if (dataSourcesAdd != null && dataSourcesAdd.length > 0) {
			reallyAddDataSources(dataSourcesAdd);
			if (DEBUGADDREMOVE && dataSourcesAdd.length > 1)
				debug("Streamlined adding " + dataSourcesAdd.length + " rows");
		}

		if (dataSourcesRemove != null && dataSourcesRemove.length > 0) {
			reallyRemoveDataSources(dataSourcesRemove);
		}
	}

	private void locationChanged(final int iStartColumn) {
		if (getComposite() == null || getComposite().isDisposed())
			return;
		
		columnVisibilitiesChanged = true;
		
		runForAllRows(new TableGroupRowRunner() {
			public void run(TableRowCore row) {
				row.locationChanged(iStartColumn);
			}
		});
	}

	private void doPaint(final GC gc) {
		if (getComposite() == null || getComposite().isDisposed())
			return;

		runForVisibleRows(new TableGroupRowRunner() {
			public void run(TableRowCore row) {
				((TableRowSWT) row).doPaint(gc, true);
			}
		});
	}

	/** IView.delete: This method is called when the view is destroyed.
	 * Each color instanciated, images and such things should be disposed.
	 * The caller is the GUI thread.
	 */
	public void delete() {
		triggerLifeCycleListener(TableLifeCycleListener.EVENT_DESTROYED);

		if (tabViews != null && tabViews.size() > 0) {
			for (int i = 0; i < tabViews.size(); i++) {
				IView view = (IView) tabViews.get(i);
				if (view != null)
					view.delete();
			}
		}

		TableStructureEventDispatcher.getInstance(sTableID).removeListener(this);
		TableColumnManager tcManager = TableColumnManager.getInstance();
		if (tcManager != null) {
			tcManager.saveTableColumns(sTableID);
		}

		if (table != null && !table.isDisposed())
			table.dispose();
		removeAllTableRows();
		configMan.removeParameterListener("ReOrder Delay", this);
		configMan.removeParameterListener("Graphics Update", this);
		Colors.getInstance().removeColorsChangedListener(this);

		if (timerProcessDataSources != null) {
			timerProcessDataSources.destroy();
			timerProcessDataSources = null;
		}

		//oldSelectedItems =  null;
		Composite comp = getComposite();
		if (comp != null && !comp.isDisposed())
			comp.dispose();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.views.AbstractIView#updateLanguage()
	 */
	public void updateLanguage() {
		if (tabViews != null && tabViews.size() > 0) {
			for (int i = 0; i < tabViews.size(); i++) {
				IView view = (IView) tabViews.get(i);
				if (view != null)
					view.updateLanguage();
			}
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#addDataSource(java.lang.Object, boolean)
	public void addDataSource(Object datasource, boolean immediate) {
		addDataSource(datasource);
	}

	// see common.TableView
	public void addDataSource(Object dataSource) {
		addDataSources(new Object[] {
			dataSource
		});
	}

	// see common.TableView
	public void addDataSources(final Object dataSources[]) {

		if (dataSources == null)
			return;

		if (IMMEDIATE_ADDREMOVE_DELAY == 0) {
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
				if (dataSources[i] != null
						&& !dataSourcesToAdd.contains(dataSources[i])) {
					count++;
					dataSourcesToAdd.add(dataSources[i]);
				}
			}

			if (DEBUGADDREMOVE)
				debug("Queued " + count + " of " + dataSources.length
						+ " dataSources to add.  Total Queued: " + dataSourcesToAdd.size());

		} finally {

			dataSourceToRow_mon.exit();
		}

		refreshenProcessDataSourcesTimer();
	}

	private void refreshenProcessDataSourcesTimer() {
		if (bReallyAddingDataSources || timerProcessDataSources == null) {
			// when timerProcessDataSources is null, we are disposing
			return;
		}
		
		if(cellEditNotifier != null)
			cellEditNotifier.sourcesChanged();

		synchronized (timerProcessDataSources) {
			if (timerEventProcessDS != null && !timerEventProcessDS.hasRun()) {
				// Push timer forward, unless we've pushed it forward for over x seconds
				long now = SystemTime.getCurrentTime();
				if (now - timerEventProcessDS.getCreatedTime() < IMMEDIATE_ADDREMOVE_MAXDELAY) {
					long lNextTime = now + IMMEDIATE_ADDREMOVE_DELAY;
					timerProcessDataSources.adjustAllBy(lNextTime
							- timerEventProcessDS.getWhen());
				} else {
					timerEventProcessDS.cancel();
					timerEventProcessDS = null;
					if (DEBUGADDREMOVE) {
						debug("Over immediate delay limit, processing queue now");
					}

					processDataSourceQueue();
				}
			} else {
				timerEventProcessDS = timerProcessDataSources.addEvent(
						SystemTime.getCurrentTime() + IMMEDIATE_ADDREMOVE_DELAY,
						new TimerEventPerformer() {
							public void perform(TimerEvent event) {
								if (DEBUGADDREMOVE && timerEventProcessDS != null) {
									debug("processDataSourceQueue after "
											+ (SystemTime.getCurrentTime() - timerEventProcessDS.getCreatedTime())
											+ "ms");
								}

								timerEventProcessDS = null;

								processDataSourceQueue();
							}
						});
			}
		}
	}

	private void reallyAddDataSources(final Object dataSources[]) {

		if (mainComposite == null || table == null || mainComposite.isDisposed()
				|| table.isDisposed())
			return;

		bReallyAddingDataSources = true;
		if (DEBUGADDREMOVE)
			debug(">>" + " Add " + dataSources.length + " rows;");

		Object[] remainingDataSources = null;
		Object[] doneDataSources = dataSources;

		// Create row, and add to map immediately
		try {
			dataSourceToRow_mon.enter();

			long lStartTime = SystemTime.getCurrentTime();

			for (int i = 0; i < dataSources.length; i++) {
				if (dataSources[i] == null)
					continue;

				// Break off and add the rows to the UI if we've taken too long to
				// create them
				if (SystemTime.getCurrentTime() - lStartTime > BREAKOFF_ADDTOMAP) {
					int iNewSize = dataSources.length - i;
					if (DEBUGADDREMOVE) {
						debug("Breaking off adding datasources to map after " + i
								+ " took " + (SystemTime.getCurrentTime() - lStartTime)
								+ "ms; # remaining: " + iNewSize);
					}
					remainingDataSources = new Object[iNewSize];
					doneDataSources = new Object[i];
					System.arraycopy(dataSources, i, remainingDataSources, 0, iNewSize);
					System.arraycopy(dataSources, 0, doneDataSources, 0, i);
					break;
				}

				if (mapDataSourceToRow.containsKey(dataSources[i])) {
					dataSources[i] = null;
				} else {
					TableRowImpl row = new TableRowImpl(this, table, sTableID,
							columnsOrdered, dataSources[i], bSkipFirstColumn);
					mapDataSourceToRow.put(dataSources[i], row);
				}
			}
		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "Error while added row to Table "
					+ sTableID, e));
		} finally {
			dataSourceToRow_mon.exit();
		}

		if (DEBUGADDREMOVE)
			debug("--" + " Add " + doneDataSources.length + " rows;");

		if (remainingDataSources == null) {
			addDataSourcesToSWT(doneDataSources, true);
		} else {
			final Object[] fDoneDataSources = doneDataSources;
			final Object[] fRemainingDataSources = remainingDataSources;
			// wrap both calls in a SWT thread so that continuation of adding 
			// remaining datasources will be on SWT thread.  OSX has horrible handling
			// of switching to SWT thread.
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					addDataSourcesToSWT(fDoneDataSources, false);
					reallyAddDataSources(fRemainingDataSources);
				}
			}, false);
		}
	}

	private void addDataSourcesToSWT(final Object dataSources[], boolean async) {
		try {
			if (DEBUGADDREMOVE)
				debug("--" + " Add " + dataSources.length + " rows to SWT "
						+ (async ? " async " : " NOW"));

			if (async) {
				table.getDisplay().asyncExec(new AERunnable() {
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

		mainComposite.getParent().setCursor(table.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));

		TableRowCore[] selectedRows = getSelectedRows();

		boolean bBrokeEarly = false;
		boolean bReplacedVisible = false;
		boolean bWas0Rows = table.getItemCount() == 0;
		try {
			dataSourceToRow_mon.enter();
			sortedRows_mon.enter();

			if (DEBUGADDREMOVE)
				debug("--" + " Add " + dataSources.length + " rows to SWT");

			// purposefully not included in time check 
			table.setItemCount(sortedRows.size() + dataSources.length);

			long lStartTime = SystemTime.getCurrentTime();

			int iTopIndex = table.getTopIndex();
			int iBottomIndex = Utils.getTableBottomIndex(table, iTopIndex);

			// add to sortedRows list in best position.  
			// We need to be in the SWT thread because the rowSorter may end up
			// calling SWT objects.
			for (int i = 0; i < dataSources.length; i++) {
				Object dataSource = dataSources[i];
				if (dataSource == null)
					continue;

				// If we've been processing on the SWT thread for too long,
				// break off and allow SWT a breather to update.
				if (SystemTime.getCurrentTime() - lStartTime > BREAKOFF_ADDROWSTOSWT) {
					int iNewSize = dataSources.length - i;
					if (DEBUGADDREMOVE) {
						debug("Breaking off adding datasources to SWT after " + i
								+ " took " + (SystemTime.getCurrentTime() - lStartTime)
								+ "ms; # remaining: " + iNewSize);
					}
					Object[] remainingDataSources = new Object[iNewSize];
					System.arraycopy(dataSources, i, remainingDataSources, 0, iNewSize);
					addDataSourcesToSWT(remainingDataSources, true);
					bBrokeEarly = true;
					break;
				}

				TableRowImpl row = (TableRowImpl) mapDataSourceToRow.get(dataSource);
				if (row == null || row.getIndex() >= 0)
					continue;
				if (sortColumn != null) {
					TableCellSWT cell = row.getTableCellSWT(sortColumn.getName());
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

				try {
					int index = 0;
					if (sortedRows.size() > 0) {
						// If we are >= to the last item, then just add it to the end
						// instead of relying on binarySearch, which may return an item
						// in the middle that also is equal.
						TableRowSWT lastRow = (TableRowSWT) sortedRows.get(sortedRows.size() - 1);
						if (sortColumn.compare(row, lastRow) >= 0) {
							index = sortedRows.size();
							sortedRows.add(row);
							if (DEBUGADDREMOVE)
								debug("Adding new row to bottom");
						} else {
							index = Collections.binarySearch(sortedRows, row, sortColumn);
							if (index < 0)
								index = -1 * index - 1; // best guess

							if (index > sortedRows.size())
								index = sortedRows.size();

							if (DEBUGADDREMOVE)
								debug("Adding new row at position " + index + " of "
										+ (sortedRows.size() - 1));
							sortedRows.add(index, row);
						}
					} else {
						if (DEBUGADDREMOVE)
							debug("Adding new row to bottom (1st Entry)");
						index = sortedRows.size();
						sortedRows.add(row);
					}

					triggerListenerRowAdded(row);

					if (!bReplacedVisible && index >= iTopIndex && index <= iBottomIndex) {
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
						if (!sortedRows.contains(row))
							sortedRows.add(row);
					} catch (Exception e2) {
						Debug.out(e2);
					}
				}
			} // for dataSources
			
			if (DEBUGADDREMOVE) {
				debug("Adding took "+ (SystemTime.getCurrentTime() - lStartTime) + "ms");
			}

			// Sanity Check: Make sure # of rows in table and in array match
			if (table.getItemCount() > sortedRows.size() && !bBrokeEarly) {
				// This could happen if one of the datasources was null, or
				// an error occured
				table.setItemCount(sortedRows.size());
			}

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "Error while adding row to Table "
					+ sTableID, e));
		} finally {
			sortedRows_mon.exit();
			dataSourceToRow_mon.exit();

			if (!bBrokeEarly) {
				bReallyAddingDataSources = false;
				refreshenProcessDataSourcesTimer();
			}
		}

		if (!bBrokeEarly || bReplacedVisible) {
			fillRowGaps(false);
			if (bReplacedVisible) {
				lastTopIndex = 0;
				lastBottomIndex = -1;
				visibleRowsChanged();
			}
		}

		if (!columnPaddingAdjusted && table.getItemCount() > 0 && bWas0Rows) {
			TableColumn[] tableColumnsSWT = table.getColumns();
			TableItem item = table.getItem(0);
			// on *nix, the last column expands to fill remaining space.. let's just not touch it
			int len = Constants.isUnix ? tableColumnsSWT.length - 1
					: tableColumnsSWT.length;
			for (int i = 0; i < len; i++) {
				TableColumnCore tc = (TableColumnCore) tableColumnsSWT[i].getData("TableColumnCore");
				if (tc != null) {
					Rectangle bounds = item.getBounds(i);
					int tcWidth = tc.getWidth();
					if (tcWidth != 0 && bounds.width != 0) {
  					int ofs = tc.getWidth() - bounds.width;
  					if (ofs > 0) {
  						tableColumnsSWT[i].setResizable(true);
  						tableColumnsSWT[i].setData("widthOffset", new Long(ofs));
  						tc.triggerColumnSizeChange();
  					}
					}
				}
			}
			columnPaddingAdjusted = true;
		}

		setSelectedRows(selectedRows);
		if (DEBUGADDREMOVE)
			debug("<< " + sortedRows.size());

		mainComposite.getParent().setCursor(null);
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#removeDataSource(java.lang.Object, boolean)
	public void removeDataSource(Object dataSource, boolean immediate) {
		removeDataSources(new Object[] {
			dataSource
		});
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#removeDataSource(java.lang.Object)
	public void removeDataSource(final Object dataSource) {
		removeDataSources(new Object[] {
			dataSource
		});
	}

	/** Remove the specified dataSource from the table.
	 *
	 * @param dataSources data sources to be removed
	 * @param bImmediate Remove immediately, or queue and remove at next refresh
	 */
	public void removeDataSources(final Object[] dataSources) {
		if (dataSources == null) {
			return;
		}

		if (IMMEDIATE_ADDREMOVE_DELAY == 0) {
			reallyRemoveDataSources(dataSources);
			return;
		}

		try {
			dataSourceToRow_mon.enter();

			for (int i = 0; i < dataSources.length; i++)
				dataSourcesToRemove.add(dataSources[i]);

			if (DEBUGADDREMOVE)
				debug("Queued " + dataSources.length
						+ " dataSources to remove.  Total Queued: "
						+ dataSourcesToRemove.size());
		} finally {
			dataSourceToRow_mon.exit();
		}

		refreshenProcessDataSourcesTimer();
	}

	private void reallyRemoveDataSources(final Object[] dataSources) {

		if (DEBUGADDREMOVE)
			debug(">> Remove rows");

		final long lStart = SystemTime.getCurrentTime();

		boolean ok = Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (table == null || table.isDisposed()) {
					return;
				}
				
				mainComposite.getParent().setCursor(table.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));

				StringBuffer sbWillRemove = null;
				if (DEBUGADDREMOVE) {
					debug(">>> Remove rows.  Start w/" + mapDataSourceToRow.size()
							+ "ds; tc=" + table.getItemCount() + ";"
							+ (SystemTime.getCurrentTime() - lStart) + "ms wait");

					sbWillRemove = new StringBuffer("Will soon remove row #");
				}

				ArrayList itemsToRemove = new ArrayList();
				ArrayList swtItemsToRemove = new ArrayList();
				int iTopIndex = table.getTopIndex();
				int iBottomIndex = Utils.getTableBottomIndex(table, iTopIndex);
				boolean bRefresh = false;

				// pass one: get the SWT indexes of the items we are going to remove
				//           This will re-link them if they lost their link
				for (int i = 0; i < dataSources.length; i++) {
					if (dataSources[i] == null)
						continue;

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

				// pass 2: remove from map and list, add removed to seperate list
				for (int i = 0; i < dataSources.length; i++) {
					if (dataSources[i] == null)
						continue;

					// Must remove from map before deleted from gui
					TableRowSWT item = (TableRowSWT) mapDataSourceToRow.remove(dataSources[i]);
					if (item != null) {
						itemsToRemove.add(item);
						sortedRows.remove(item);
						triggerListenerRowRemoved(item);
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
				}

				if (DEBUGADDREMOVE) {
					debug("-- Removed from SWT");
				}

				// Finally, delete the rows
				for (Iterator iter = itemsToRemove.iterator(); iter.hasNext();) {
					TableRowCore row = (TableRowCore) iter.next();
					row.delete();
				}

				if (bRefresh) {
					fillRowGaps(false);
					refreshVisibleRows();
				}

				if (DEBUGADDREMOVE)
					debug("<< Remove " + itemsToRemove.size() + " rows. now "
							+ mapDataSourceToRow.size() + "ds; tc=" + table.getItemCount());
				mainComposite.getParent().setCursor(null);
			}
		});

		if (!ok) {
			// execRunnable will only fail if we are closing
			for (int i = 0; i < dataSources.length; i++) {
				if (dataSources[i] == null)
					continue;

				TableRowSWT item = (TableRowSWT) mapDataSourceToRow.get(dataSources[i]);
				mapDataSourceToRow.remove(dataSources[i]);
				if (item != null) {
					sortedRows.remove(item);
					item.delete();
				}
			}

			fillRowGaps(false);
			if (DEBUGADDREMOVE)
				debug("<< Remove 1 row, noswt");
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

			if (DEBUGADDREMOVE)
				debug("removeAll");

		} finally {

			sortedRows_mon.exit();
			dataSourceToRow_mon.exit();
		}

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (table != null && !table.isDisposed())
					table.removeAll();

				// Image Disposal handled by each cell

				for (int i = 0; i < rows.length; i++)
					rows[i].delete();
			}
		});

		if (DEBUGADDREMOVE) {
			long lTimeDiff = (System.currentTimeMillis() - lTimeStart);
			if (lTimeDiff > 10)
				debug("RemovaAll took " + lTimeDiff + "ms");
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
			return;
		}
		if (parameterName == null || parameterName.equals("ReOrder Delay")) {
			reOrderDelay = configMan.getIntParameter("ReOrder Delay");
			return;
		}
		if (parameterName == null || parameterName.startsWith("Color")) {
			tableInvalidate();
		}
	}

	// ITableStructureModificationListener
	public void tableStructureChanged() {
		triggerLifeCycleListener(TableLifeCycleListener.EVENT_DESTROYED);

		removeAllTableRows();
		initializeTableColumns(table);
		refreshTable(false);

		triggerLifeCycleListener(TableLifeCycleListener.EVENT_INITIALIZED);
	}

	// ITableStructureModificationListener
	public void columnOrderChanged(int[] positions) {
		try {
			table.setColumnOrder(positions);
		} catch (NoSuchMethodError e) {
			// Pre SWT 3.1
			// This shouldn't really happen, since this function only gets triggered
			// from SWT >= 3.1
			tableStructureChanged();
		}
	}

	/** 
	 * The Columns width changed
	 */
	// ITableStructureModificationListener
	public void columnSizeChanged(TableColumnCore tableColumn) {
		int newWidth = tableColumn.getWidth();
		if (table == null || table.isDisposed())
			return;

		TableColumn column = null;
		TableColumn[] tableColumnsSWT = table.getColumns();
		for (int i = 0; i < tableColumnsSWT.length; i++) {
			if (tableColumnsSWT[i].getData("TableColumnCore") == tableColumn) {
				column = tableColumnsSWT[i];
				break;
			}
		}
		Long lOfs = (Long) column.getData("widthOffset");
		if (lOfs != null) {
			newWidth += lOfs.intValue();
		}
		if (column == null || column.isDisposed()
				|| (column.getWidth() == newWidth))
			return;

		if (Constants.isUnix) {
			final int fNewWidth = newWidth;
			final TableColumn fTableColumn = column;
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
	
	public void cellInvalidate(TableColumnCore tableColumn, Object data_source) {
		cellInvalidate(tableColumn, data_source, true);
	}

	public void columnRefresh(TableColumnCore tableColumn) {
		final String sColumnName = tableColumn.getName();
		runForAllRows(new TableGroupRowVisibilityRunner() {
			public void run(TableRowCore row, boolean bVisible) {
				TableCellSWT cell = ((TableRowSWT) row).getTableCellSWT(sColumnName);
				if (cell != null)
					cell.refresh(true, bVisible);
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
		if (tc != null)
			columnInvalidate(tc, tc.getType() == TableColumnCore.TYPE_TEXT_ONLY);
	}

	public void columnInvalidate(TableColumnCore tableColumn,
			final boolean bMustRefresh) {
		final String sColumnName = tableColumn.getName();

		runForAllRows(new TableGroupRowRunner() {
			public void run(TableRowCore row) {
				TableCellSWT cell = ((TableRowSWT) row).getTableCellSWT(sColumnName);
				if (cell != null)
					cell.invalidate(bMustRefresh);
			}
		});
	}
	
	public void cellInvalidate(TableColumnCore tableColumn,
			final Object data_source, final boolean bMustRefresh) {
		final String sColumnName = tableColumn.getName();

		runForAllRows(new TableGroupRowRunner() {
			public void run(TableRowCore row) {
				TableCellSWT cell = ((TableRowSWT) row).getTableCellSWT(sColumnName);
				if (cell != null && cell.getDataSource() != null && cell.getDataSource().equals(data_source)) {
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
			for (Iterator iter = sortedRows.iterator(); iter.hasNext();) {
				TableRowCore row = (TableRowCore) iter.next();
				cells[i++] = row.getTableCellCore(sColumnName);
			}

		} finally {
			sortedRows_mon.exit();
		}

		return cells;
	}

	// see common.TableView
	public TableRowCore[] getRows() {
		try {
			sortedRows_mon.enter();

			return (TableRowCore[]) sortedRows.toArray(new TableRowCore[0]);

		} finally {
			sortedRows_mon.exit();
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getRow(java.lang.Object)
	public TableRowCore getRow(Object dataSource) {
		return (TableRowCore) mapDataSourceToRow.get(dataSource);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableViewSWT#getRowSWT(java.lang.Object)
	public TableRowSWT getRowSWT(Object dataSource) {
		return (TableRowSWT) mapDataSourceToRow.get(dataSource);
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getRow(int)
	public TableRowCore getRow(int iPos) {
		try {
			sortedRows_mon.enter();

			if (iPos >= 0 && iPos < sortedRows.size()) {
				TableRowCore row = (TableRowCore) sortedRows.get(iPos);

				if (row.getIndex() != iPos) {
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
			return (TableRowCore) sortedRows.get(iPos);
		} catch (Exception e) {
			return null;
		}
	}

	public int indexOf(TableRowCore row) {
		int i = ((TableRowImpl) row).getRealIndex();
		if (i == -1) {
			i = sortedRows.indexOf(row);
			if (i >= 0) {
				row.setTableItem(i);
			}
		}
		return i;
	}

	private TableRowCore getRow(TableItem item) {
		try {
			Object o = item.getData("TableRow");
			if (o instanceof TableRowCore) {
				return (TableRowCore) o;
			} else {
				int iPos = table.indexOf(item);
				//System.out.println(iPos + " has no table row.. associating. " + Debug.getCompressedStackTrace(4));
				if (iPos >= 0 && iPos < sortedRows.size()) {
					TableRowSWT row = (TableRowSWT) sortedRows.get(iPos);
					//System.out.print(".. associating to " + row);
					if (row != null) {
						row.setTableItem(iPos);
					}
					//System.out.println(", now " + row);
					return row;
				}
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

	public Object[] getDataSources() {
		return mapDataSourceToRow.keySet().toArray();
	}

	/* various selected rows functions */
	/***********************************/

	public List getSelectedDataSourcesList() {
		return getSelectedDataSourcesList(true);
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
	public List getSelectedDataSourcesList(final boolean bCoreDataSource) {
		final ArrayList l = new ArrayList();
		if (table == null || table.isDisposed()) {
			return l;
		}
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (table.isDisposed()) {
					return;
				}
				TableItem[] tis = table.getSelection();
				for (int i = 0; i < tis.length; i++) {
					TableRowSWT row = (TableRowSWT) getRow(tis[i]);
					if (row == null) {
						fillRowGaps(false);

						// Try again
						row = (TableRowSWT) getRow(tis[i]);
						if (row == null)
							System.out.println("XXX Boo, row still null "
									+ table.indexOf(tis[i]) + ";sd=" + tis[i].getData("SD") + ";"
									+ Debug.getCompressedStackTrace());
					}
					if (row != null && row.getDataSource(true) != null)
						l.add(row.getDataSource(bCoreDataSource));
				}
			}
		}, false);
		return l;
	}

	/** Returns an array of all selected Data Sources.  Null data sources are
	 * ommitted.
	 *
	 * @param a the array into which the selected data sources are to be stored, 
	 *          if the size is the big enough; otherwise, a new array of the same 
	 *          runtime type is allocated for this purpose.
	 *
	 * @return an array containing the selected data sources
	 */
	public Object[] getSelectedDataSources(Object[] a) {
		return getSelectedDataSourcesList().toArray(a);
	}

	// see common.TableView
	public Object[] getSelectedDataSources() {
		return getSelectedDataSourcesList().toArray();
	}

	// see common.TableView
	public Object[] getSelectedDataSources(boolean bCoreDataSource) {
		return getSelectedDataSourcesList(bCoreDataSource).toArray();
	}

	/** @see com.aelitis.azureus.ui.common.table.TableView#getSelectedRows() */
	public TableRowCore[] getSelectedRows() {
		return (TableRowCore[]) getSelectedRowsList().toArray(new TableRowCore[0]);
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getSelectedRowsSize()
	public int getSelectedRowsSize() {
		if (table != null && !table.isDisposed()) {
			return table.getSelectionCount();
		}
		return 0;
	}

	public TableRowSWT[] getSelectedRowsSWT() {
		return (TableRowSWT[]) getSelectedRowsList().toArray(new TableRowSWT[0]);
	}

	/** Returns an list of all selected TableRowSWT objects.  Null data sources are
	 * ommitted.
	 *
	 * @return an list containing the selected TableRowSWT objects
	 */
	public List getSelectedRowsList() {
		ArrayList l = new ArrayList();
		if (table != null && !table.isDisposed()) {
			TableItem[] tis = table.getSelection();
			for (int i = 0; i < tis.length; i++) {
				TableRowSWT row = (TableRowSWT) getRow(tis[i]);
				if (row != null && row.getDataSource(true) != null)
					l.add(row);
			}
		}
		return l;
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getFocusedRow()
	public TableRowCore getFocusedRow() {
		TableRowSWT[] selectedRows = getSelectedRowsSWT();
		if (selectedRows.length == 0) {
			return null;
		}
		return selectedRows[0];
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getFirstSelectedDataSource()
	public Object getFirstSelectedDataSource() {
		return getFirstSelectedDataSource(true);
	}

	public TableRowSWT[] getVisibleRows() {
		if (table == null || table.isDisposed())
			return new TableRowSWT[0];

		int iTopIndex = table.getTopIndex();
		int iBottomIndex = Utils.getTableBottomIndex(table, iTopIndex);

		int size = iBottomIndex - iTopIndex + 1;
		if (size <= 0)
			return new TableRowSWT[0];

		TableRowSWT[] rows = new TableRowSWT[size];
		int pos = 0;
		for (int i = iTopIndex; i <= iBottomIndex; i++) {
			TableItem item = table.getItem(i);
			if (item != null && !item.isDisposed()) {
				TableRowSWT row = (TableRowSWT) getRow(item);
				if (row != null) {
					rows[pos++] = row;
				}
			}
		}

		if (pos <= rows.length) {
			// Some were null, shrink array
			TableRowSWT[] temp = new TableRowSWT[pos];
			System.arraycopy(rows, 0, temp, 0, pos);
			return temp;
		}

		return rows;
	}

	/** Returns the first selected data sources.
	 *
	 * @return the first selected data source, or null if no data source is 
	 *         selected
	 */
	public Object getFirstSelectedDataSource(boolean bCoreObject) {
		if (table == null || table.isDisposed() || table.getSelectionCount() == 0)
			return null;

		TableRowCore row = getRow(table.getSelection()[0]);
		if (row == null)
			return null;
		return row.getDataSource(bCoreObject);
	}

	/** For each row source that the user has selected, run the code
	 * provided by the specified parameter.
	 *
	 * @param runner Code to run for each selected row/datasource
	 */
	public void runForSelectedRows(TableGroupRowRunner runner) {
		if (table == null || table.isDisposed())
			return;

		TableItem[] tis = table.getSelection();
		List rows_to_use = null;
		if (runner.supportsMultipleRows()) {
			rows_to_use = new ArrayList(tis.length);
		}
		for (int i = 0; i < tis.length; i++) {
			TableRowSWT row = (TableRowSWT) getRow(tis[i]);
			if (row != null)
				if (rows_to_use != null) {
					rows_to_use.add(row);
				} else {
					runner.run(row);
				}
		}
		if (rows_to_use != null) {
			runner.run((TableRowSWT[]) rows_to_use.toArray(new TableRowSWT[rows_to_use.size()]));
		}
	}

	/** For each visible row source, run the code provided by the specified 
	 * parameter.
	 *
	 * @param runner Code to run for each selected row/datasource
	 */
	public void runForVisibleRows(TableGroupRowRunner runner) {
		TableRowSWT[] rows = getVisibleRows();
		if (runner.run(rows)) {
			return;
		}

		for (int i = 0; i < rows.length; i++)
			runner.run(rows[i]);
	}

	// see common.tableview
	public void runForAllRows(TableGroupRowVisibilityRunner runner) {
		if (table == null || table.isDisposed()) {
			return;
		}

		// put to array instead of synchronised iterator, so that runner can remove
		TableRowCore[] rows = getRows();
		int iTopIndex = table.getTopIndex();
		int iBottomIndex = Utils.getTableBottomIndex(table, iTopIndex);

		for (int i = 0; i < rows.length; i++) {
			runner.run(rows[i], i >= iTopIndex && i <= iBottomIndex);
		}
	}

	/**
	 * Runs a specified task for a list of table items that the table contains
	 * @param items A list of TableItems that are part of the table view
	 * @param runner A task
	 */
	public void runForTableItems(List items, TableGroupRowRunner runner) {
		if (table == null || table.isDisposed())
			return;

		final Iterator iter = items.iterator();
		List rows_to_use = null;
		if (runner.supportsMultipleRows()) {
			rows_to_use = new ArrayList(items.size());
		}
		while (iter.hasNext()) {
			TableItem tableItem = (TableItem) iter.next();
			if (tableItem.isDisposed())
				continue;

			TableRowSWT row = (TableRowSWT) getRow(tableItem);
			if (row != null) {
				if (rows_to_use != null) {
					rows_to_use.add(row);
				} else {
					runner.run(row);
				}
			}
		}
		if (rows_to_use != null) {
			runner.run((TableRowSWT[]) rows_to_use.toArray(new TableRowSWT[rows_to_use.size()]));
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#clipboardSelected()
	public void clipboardSelected() {
		String sToClipboard = "";
		for (int j = 0; j < table.getColumnCount(); j++) {
			if (j != 0)
				sToClipboard += "\t";
			sToClipboard += table.getColumn(j).getText();
		}

		TableItem[] tis = table.getSelection();
		for (int i = 0; i < tis.length; i++) {
			sToClipboard += "\n";
			for (int j = 0; j < table.getColumnCount(); j++) {
				if (j != 0)
					sToClipboard += "\t";
				sToClipboard += tis[i].getText(j);
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
			if (COLUMN_CLICK_DELAY) {
				// temporary for OSX.. resizing column triggers selection, so cancel
				// if a resize was recent. 
				final Timer timer = new Timer("Column Selection Wait");
				timer.addEvent(System.currentTimeMillis() + 85,
						new TimerEventPerformer() {
							public void perform(TimerEvent timerEvent) {
								Utils.execSWTThread(new AERunnable() {
									public void runSupport() {
										if (lLastColumnResizeOn == -1
												|| System.currentTimeMillis() - lLastColumnResizeOn > 220)
											reallyHandleEvent(event);
									}
								});
								timer.destroy();
							}
						});
			} else {
				reallyHandleEvent(event);
			}
		}

		private void reallyHandleEvent(Event event) {
			TableColumn column = (TableColumn) event.widget;
			if (column == null)
				return;
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
			TableColumn column = (TableColumn) event.widget;
			if (column == null)
				return;

			TableColumnCore tableColumnCore = (TableColumnCore) column.getData("TableColumnCore");
			if (tableColumnCore == null)
				return;

			Table table = column.getParent();

			// Get the 'added position' of column
			// It would have been easier if event (.start, .end) contained the old
			// and new position..
			TableColumn[] tableColumns = table.getColumns();
			int iAddedPosition;
			for (iAddedPosition = 0; iAddedPosition < tableColumns.length; iAddedPosition++) {
				if (column == tableColumns[iAddedPosition])
					break;
			}
			if (iAddedPosition >= tableColumns.length)
				return;

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
			if (topIndex >= itemCount) {
				topIndex = itemCount - 1;
			}
			TableItem ti = table.getItem(topIndex);
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
		if (iColumn < 0)
			return null;

		TableItem item = table.getItem(new Point(2, y));
		return (TableRowCore) getRow(item);
	}

	public TableCellSWT getTableCell(int x, int y) {
		int iColumn = getColumnNo(x);
		if (iColumn < 0)
			return null;

		TableItem item = table.getItem(new Point(2, y));
		if (item == null)
			return null;
		TableRowSWT row = (TableRowSWT) getRow(item);

		if (row == null) {
			return null;
		}

		TableColumn tcColumn = table.getColumn(iColumn);
		String sCellName = (String) tcColumn.getData("Name");
		if (sCellName == null)
			return null;

		return row.getTableCellSWT(sCellName);
	}

	public TableRowSWT getTableRow(int x, int y) {
		TableItem item = table.getItem(new Point(2, y));
		if (item == null)
			return null;
		return (TableRowSWT) getRow(item);
	}

	private TableColumnCore getTableColumnByOffset(int x) {
		int iColumn = getColumnNo(x);
		if (iColumn < 0)
			return null;

		TableColumn column = table.getColumn(iColumn);
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
			Iterator it = mapDataSourceToRow.keySet().iterator();

			while (it.hasNext()) {

				Object key = it.next();

				writer.println("  " + key + " -> " + mapDataSourceToRow.get(key));
			}

			writer.println("# of SubViews: " + tabViews.size());
			writer.indent();
			try {
				for (Iterator iter = tabViews.iterator(); iter.hasNext();) {
					IView view = (IView) iter.next();
					view.generateDiagnostics(writer);
				}
			} finally {
				writer.exdent();
			}
			
			writer.println("Columns:");
			writer.indent();
			try {
  			TableColumn[] tableColumnsSWT = table.getColumns();
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
		if (ptIconSize == null)
			ptIconSize = new Point(1, iHeight);
		else
			ptIconSize.y = iHeight;
		bSkipFirstColumn = true;
	}

	public int getRowDefaultHeight() {
		if (ptIconSize == null)
			return 0;
		return ptIconSize.y;
	}

	// from common.TableView
	public void setRowDefaultIconSize(Point size) {
		ptIconSize = size;
		bSkipFirstColumn = true;
	}

	// TabViews Functions
	public void addTabView(IView view) {
		if (view == null || tabFolder == null)
			return;

		CTabItem item = new CTabItem(tabFolder, SWT.NULL);
		item.setData("IView", view);
		Messages.setLanguageText(item, view.getData());
		view.initialize(tabFolder);
		item.setControl(view.getComposite());
		tabViews.add(view);
	}

	private void fillRowGaps(boolean bForceDataRefresh) {
		_sortColumn(bForceDataRefresh, true, true);
	}

	private void sortColumn(boolean bForceDataRefresh) {
		_sortColumn(bForceDataRefresh, false, true);
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

			// This actually gets the focus, assuming the focus is selected
			int iFocusIndex = table.getSelectionIndex();
			TableRowCore focusedRow = (iFocusIndex == -1) ? null
					: getRow(iFocusIndex);

			int iTopIndex = table.getTopIndex();
			int iBottomIndex = Utils.getTableBottomIndex(table, iTopIndex);
			boolean allSelectedRowsVisible = true;

			int[] selectedRowIndices = table.getSelectionIndices();
			TableRowCore[] selectedRows = new TableRowCore[selectedRowIndices.length];
			for (int i = 0; i < selectedRowIndices.length; i++) {
				int index = selectedRowIndices[i];
				selectedRows[i] = getRow(table.getItem(index));
				if (allSelectedRowsVisible
						&& (index < iTopIndex || index > iBottomIndex)) {
					allSelectedRowsVisible = false;
				}
				//System.out.println("Selected: " + selectedRowIndices[i] + ";" + selectedRows[i].getDataSource(true));
			}

			try {
				sortedRows_mon.enter();

				if (bForceDataRefresh && sortColumn != null) {
					String sColumnID = sortColumn.getName();
					for (Iterator iter = sortedRows.iterator(); iter.hasNext();) {
						TableRowSWT row = (TableRowSWT) iter.next();
						TableCellSWT cell = row.getTableCellSWT(sColumnID);
						if (cell != null) {
							cell.refresh(true);
						}
					}
				}

				if (!bFillGapsOnly) {
					if (sortColumn != null
							&& sortColumn.getLastSortValueChange() > lLastSortedOn) {
						lLastSortedOn = SystemTime.getCurrentTime();
						Collections.sort(sortedRows, sortColumn);
						if (DEBUG_SORTER) {
							long lTimeDiff = (System.currentTimeMillis() - lTimeStart);
							if (lTimeDiff > 150)
								System.out.println("--- Build & Sort took " + lTimeDiff + "ms");
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
				if (bTableVirtual && allSelectedRowsVisible) {
					for (int i = 0; i < sortedRows.size(); i++) {
						TableRowSWT row = (TableRowSWT) sortedRows.get(i);
						boolean visible = i >= iTopIndex && i <= iBottomIndex;
						if (visible) {
  						if (row.setTableItem(i)) {
  							iNumMoves++;
  						}
						} else {
							if (row instanceof TableRowImpl) {
								((TableRowImpl)row).setShown(visible, false);
							}
						}
					}

					// visibleRowsChanged() will setTableItem for the rest
				} else {
					for (int i = 0; i < sortedRows.size(); i++) {
						boolean visible = i >= iTopIndex && i <= iBottomIndex;
						TableRowSWT row = (TableRowSWT) sortedRows.get(i);
						if (row.setTableItem(i)) {
							iNumMoves++;
						} else {
							if (row instanceof TableRowImpl) {
								((TableRowImpl)row).setShown(visible, false);
							}
						}
					}
				}
			} finally {
				sortedRows_mon.exit();
			}

			// move cursor to selected row
			/** SWT/Windows Bug:
			 * When we set selection, the first index is the focus row.
			 * This works visually, however, if you press shift-up or shift-down,
			 * it uses an older selection index.
			 * 
			 * ie. User selects row #10
			 *     Programmically change selection to Row #15 only
			 *     Shift-down
			 *     Rows 10 through 26 will be selected
			 *     
			 * This is Eclipse bug #77106, and is marked WONTFIX 
			 */
			if (focusedRow != null) {
				int pos = 1;
				int numSame = 0;
				int[] newSelectedRowIndices = new int[selectedRows.length];
				Arrays.sort(selectedRowIndices);
				for (int i = 0; i < selectedRows.length; i++) {
					if (selectedRows[i] == null) {
						continue;
					}
					int index = selectedRows[i].getIndex();
					int iNewPos = (selectedRows[i] == focusedRow) ? 0 : pos++;
					//System.out.println("new selected, index=" + index + ";row=" + selectedRows[i].getDataSource(true));
					newSelectedRowIndices[iNewPos] = index;
					if (Arrays.binarySearch(selectedRowIndices, index) >= 0) {
						numSame++;
					}
				}

				if (numSame < selectedRows.length) {
					// XXX setSelection calls showSelection().  We don't want the table
					//     to jump all over.  Quick fix is to reset topIndex, but
					//     there might be a better way
					iTopIndex = 0;
					if (!bFollowSelected) {
						table.setRedraw(false);
						iTopIndex = table.getTopIndex();
					}
					table.setSelection(newSelectedRowIndices);
					if (!bFollowSelected) {
						table.setTopIndex(iTopIndex);
						table.setRedraw(true);
					}
				}
			}

			if (DEBUG_SORTER) {
				long lTimeDiff = (System.currentTimeMillis() - lTimeStart);
				if (lTimeDiff >= 500)
					System.out.println("<<< Sort & Assign took " + lTimeDiff + "ms with "
							+ iNumMoves + " rows (of " + sortedRows.size() + ") moved. "
							+ focusedRow + ";" + Debug.getCompressedStackTrace());
			}
		} finally {
			sortColumn_mon.exit();
		}
	}

	public void sortColumnReverse(TableColumnCore sorter) {
		boolean bSameColumn = sortColumn.equals(sorter);
		if (!bSameColumn) {
			sortColumn = sorter;
			int iSortDirection = configMan.getIntParameter(CFG_SORTDIRECTION);
			if (iSortDirection == 0)
				sortColumn.setSortAscending(true);
			else if (iSortDirection == 1)
				sortColumn.setSortAscending(false);
			else
				sortColumn.setSortAscending(!sortColumn.isSortAscending());

			TableColumnManager.getInstance().setDefaultSortColumnName(sTableID,
					sortColumn.getName());
		} else {
			sortColumn.setSortAscending(!sortColumn.isSortAscending());
		}

		changeColumnIndicator();
		sortColumn(!bSameColumn);
	}

	private void changeColumnIndicator() {
		if (table == null || table.isDisposed())
			return;

		try {
			// can't use TableColumnCore.getPosition, because user may have moved
			// columns around, messing up the SWT column indexes.  
			// We can either use search columnsOrdered, or search table.getColumns()
			TableColumn[] tcs = table.getColumns();
			for (int i = 0; i < tcs.length; i++) {
				String sName = (String) tcs[i].getData("Name");
				if (sName != null && sName.equals(sortColumn.getName())) {
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
		int i = row.getIndex();
		return i >= lastTopIndex && i <= lastBottomIndex;
	}

	private void visibleRowsChanged() {
		if (Utils.SWT32_TABLEPAINT) {
			return;
		}

		if (!table.isVisible()) {
			lastTopIndex = 0;
			lastBottomIndex = -1;
			return;
		}
		//debug("VRC " + Debug.getCompressedStackTrace());
		
		columnVisibilitiesChanged = true;

		boolean bTableUpdate = false;
		int iTopIndex = table.getTopIndex();
		int iBottomIndex = Utils.getTableBottomIndex(table, iTopIndex);

		if (lastTopIndex != iTopIndex) {
			int tmpIndex = lastTopIndex;
			lastTopIndex = iTopIndex;

			if (iTopIndex < tmpIndex) {
				if (tmpIndex > iBottomIndex + 1 && iBottomIndex >= 0)
					tmpIndex = iBottomIndex + 1;

				//debug("Refresh top rows " + iTopIndex + " to " + (tmpIndex - 1));
				try {
					sortedRows_mon.enter();
					for (int i = iTopIndex; i < tmpIndex && i < sortedRows.size(); i++) {
						TableRowSWT row = (TableRowSWT) getRow(i);
						if (row != null) {
							row.setAlternatingBGColor(true);
							row.refresh(true, true);
							if (row instanceof TableRowImpl) {
								((TableRowImpl)row).setShown(true, false);
							}
							if (Constants.isOSX) {
								bTableUpdate = true;
							}
						}
					}
				} finally {
					sortedRows_mon.exit();
				}

				// A refresh might have triggered a row height resize, so
				// bottom index needs updating
				iBottomIndex = Utils.getTableBottomIndex(table, iTopIndex);
			} else {
				//System.out.println("Made T.Invisible " + (tmpIndex) + " to " + (iTopIndex - 1));
				for (int i = tmpIndex; i < iTopIndex; i++) {
					TableRowSWT row = (TableRowSWT) getRow(i);
					if (row instanceof TableRowImpl) {
						((TableRowImpl)row).setShown(false, false);
					}
				}
			}
		}

		if (lastBottomIndex != iBottomIndex) {
			int tmpIndex = lastBottomIndex;
			lastBottomIndex = iBottomIndex;

			if (tmpIndex < iTopIndex - 1)
				tmpIndex = iTopIndex - 1;

			if (tmpIndex <= iBottomIndex) {
				//debug("Refresh bottom rows " + (tmpIndex + 1) + " to " + iBottomIndex);
				try {
					sortedRows_mon.enter();
					for (int i = tmpIndex + 1; i <= iBottomIndex && i < sortedRows.size(); i++) {
						TableRowSWT row = (TableRowSWT) getRow(i);
						if (row != null) {
							row.setAlternatingBGColor(true);
							row.refresh(true, true);
							if (row instanceof TableRowImpl) {
								((TableRowImpl)row).setShown(true, false);
							}

							if (Constants.isOSX) {
								bTableUpdate = true;
							}
						}
					}
				} finally {
					sortedRows_mon.exit();
				}
			} else {
				//System.out.println("Made B.Invisible " + (tmpIndex) + " to " + (iBottomIndex + 1));
				for (int i = tmpIndex; i <= iBottomIndex; i++) {
					TableRowSWT row = (TableRowSWT) getRow(i);
					if (row instanceof TableRowImpl) {
						((TableRowImpl)row).setShown(false, false);
					}
				}
			}
		}

		if (bTableUpdate) {
			table.update();
		}
	}

	public Image obfusticatedImage(final Image image, Point shellOffset) {
		if (table.getItemCount() == 0) {
			return image;
		}
		Rectangle tableArea = table.getClientArea();

		TableColumn[] tableColumnsSWT = table.getColumns();
		for (int i = 0; i < tableColumnsSWT.length; i++) {
			final TableColumnCore tc = (TableColumnCore) tableColumnsSWT[i].getData("TableColumnCore");

			if (tc != null && tc.isObfusticated()) {
				int iTopIndex = table.getTopIndex();
				int iBottomIndex = Utils.getTableBottomIndex(table, iTopIndex);

				int size = iBottomIndex - iTopIndex + 1;
				if (size <= 0)
					continue;

				for (int j = iTopIndex; j <= iBottomIndex; j++) {
					TableItem rowSWT = table.getItem(j);
					TableRowSWT row = (TableRowSWT) table.getItem(j).getData("TableRow");
					if (row != null) {
						TableCellSWT cell = row.getTableCellSWT(tc.getName());
						final Rectangle columnBounds = rowSWT.getBounds(i);
						if (columnBounds.y + columnBounds.height > tableArea.y
								+ tableArea.height) {
							columnBounds.height -= (columnBounds.y + columnBounds.height)
									- (tableArea.y + tableArea.height);
						}
						if (columnBounds.x + columnBounds.width > tableArea.x
								+ tableArea.width) {
							columnBounds.width -= (columnBounds.x + columnBounds.width)
									- (tableArea.x + tableArea.width);
						}

						final Point offset = table.toDisplay(columnBounds.x, columnBounds.y);

						columnBounds.x = offset.x - shellOffset.x;
						columnBounds.y = offset.y - shellOffset.y;

						String text = cell.getObfusticatedText();

						if (text != null) {
							UIDebugGenerator.obfusticateArea(table.getDisplay(), image,
									columnBounds, text);
						}
					}
				}

				//UIDebugGenerator.offusticateArea(image, columnBounds);
			}
		}

		IView view = getActiveSubView();
		if (view instanceof ObfusticateImage) {
			try {
				((ObfusticateImage) view).obfusticatedImage(image, shellOffset);
			} catch (Exception e) {
				Debug.out("Obfusticating " + view, e);
			}
		}
		return image;
	}

	void debug(String s) {
		AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger("table");
		diag_logger.log(s);

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

	// from common.TableView
	public IView[] getCoreTabViews() {
		return coreTabViews;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableViewSWT#setCoreTabViews(org.gudy.azureus2.ui.swt.views.IView[])
	public void setCoreTabViews(IView[] coreTabViews) {
		this.coreTabViews = coreTabViews;
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
	public int size(boolean bIncludeQueue) {
		int size = sortedRows.size();

		if (bIncludeQueue) {
			if (dataSourcesToAdd != null) {
				size += dataSourcesToAdd.size();
			}
			if (dataSourcesToRemove != null) {
				size += dataSourcesToRemove.size();
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
			ensureAllRowsHaveIndex();
			table.selectAll();
		}
	}

	/**
	 * 
	 *
	 * @since 3.0.0.7
	 */
	private void ensureAllRowsHaveIndex() {
		for (int i = 0; i < sortedRows.size(); i++) {
			TableRowSWT row = (TableRowSWT) sortedRows.get(i);
			row.setTableItem(i);
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#setSelectedRows(com.aelitis.azureus.ui.common.table.TableRowCore[])
	public void setSelectedRows(TableRowCore[] rows) {
		table.deselectAll();
		for (int i = 0; i < rows.length; i++) {
			TableRowCore row = rows[i];
			if (row.getIndex() == -1) {
				int j = sortedRows.indexOf(row);
				if (j == -1) {
					System.err.println("BOO");
				} else {
					row.setTableItem(j);
				}
			}
			row.setSelected(true);
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#isTableFocus()
	public boolean isTableFocus() {
		return table.isFocusControl();
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableViewSWT#createDragSource(int)
	public DragSource createDragSource(int style) {
		final DragSource dragSource = new DragSource(table, style);
		table.addDisposeListener(new DisposeListener() {
			// @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
			public void widgetDisposed(DisposeEvent e) {
				if (dragSource != null && !dragSource.isDisposed()) {
					dragSource.dispose();
				}
			}
		});
		return dragSource;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableViewSWT#createDropTarget(int)
	public DropTarget createDropTarget(int style) {
		final DropTarget dropTarget = new DropTarget(table, style);
		table.addDisposeListener(new DisposeListener() {
			// @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
			public void widgetDisposed(DisposeEvent e) {
				if (dropTarget != null && !dropTarget.isDisposed()) {
					dropTarget.dispose();
				}
			}
		});
		return dropTarget;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableViewSWT#indexOf(org.eclipse.swt.widgets.Widget)
	public TableRowCore getRow(DropTargetEvent event) {
		if (event.item instanceof TableItem) {
			TableItem ti = (TableItem) event.item;
			return (TableRowCore) ti.getData("TableRow");
		}
		return null;
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#dataSourceExists(java.lang.Object)
	public boolean dataSourceExists(Object dataSource) {
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
		return getTableRow(pt.x, pt.y);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableViewSWT#getTableCellMouseOffset()
	public Point getTableCellMouseOffset(TableCellSWT tableCell) {
		if (tableCell == null) {
			return null;
		}
		Point pt = table.getDisplay().getCursorLocation();
		pt = table.toControl(pt);
		
		Rectangle bounds = tableCell.getBounds();
		int x = pt.x - bounds.x + VerticalAligner.getTableAdjustHorizontallyBy(table);
		if (x < 0 || x > bounds.width) {
			return null;
		}
		int y = pt.y - bounds.y + VerticalAligner.getTableAdjustVerticalBy(table);
		if (y < 0 || y > bounds.height) {
			return null;
		}
		return new Point(x, y);
	}
}
