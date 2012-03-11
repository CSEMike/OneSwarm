/*
 * Created on 2 juil. 2003
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
package org.gudy.azureus2.ui.swt.views;

import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.List;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.util.Timer;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;
import org.gudy.azureus2.pluginsimpl.local.ui.config.ConfigSectionRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.*;
import org.gudy.azureus2.ui.swt.views.configsections.*;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

@SuppressWarnings("deprecation")
public class ConfigView implements UISWTViewEventListener {
	private static final LogIDs LOGID = LogIDs.GUI;
  public static final String sSectionPrefix = "ConfigView.section.";
  
  Map<TreeItem, ConfigSection> sections = new HashMap<TreeItem, ConfigSection>();
  // Only access on SWT Thread
  java.util.List<ConfigSection> sectionsCreated = new ArrayList<ConfigSection>(1);
  Composite cConfig;
  Composite cConfigSection;
  StackLayout layoutConfigSection;
  Label lHeader;
  Label usermodeHint;
  Font headerFont;
  Font filterFoundFont;
  Tree tree;
  ArrayList<ConfigSection> pluginSections;

	private Timer filterDelayTimer;
	private String filterText = "";
	private Label lblX;
	private Listener scResizeListener;
	
	private Image imgSmallX;
	private Image imgSmallXGray;
	private String startSection;
	private UISWTView swtView;

	public ConfigView() {
  }
  
  private void initialize(final Composite composite) {
  	// need to initalize composite now, since getComposite can
  	// be called at any time
    cConfig = new Composite(composite, SWT.NONE);

    GridLayout configLayout = new GridLayout();
    configLayout.marginHeight = 0;
    configLayout.marginWidth = 0;
    cConfig.setLayout(configLayout);
    GridData gridData = new GridData(GridData.FILL_BOTH);
    cConfig.setLayoutData(gridData);

    final Label label = new Label(cConfig, SWT.CENTER);
    Messages.setLanguageText(label, "view.waiting.core");
    gridData = new GridData(GridData.FILL_BOTH);
    label.setLayoutData(gridData);
    
    // Need to delay initialation until core is done so we can guarantee
    // all config sections are loaded (ie. plugin ones).
    // TODO: Maybe add them on the fly?
    AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						_initialize(composite);
						label.dispose();
						composite.layout(true, true);
					}
				});
			}
		});
  }
  	
  private void _initialize(Composite composite) {
  	
    GridData gridData;
    /*
    /--cConfig-----------------------------------------------------------\
    | ###SashForm#form################################################## |
    | # /--cLeftSide-\ /--cRightSide---------------------------------\ # |
    | # | ##tree#### | | ***cHeader********************************* | # |
    | # | #        # | | * lHeader                    usermodeHint * | # |
    | # | #        # | | ******************************************* | # |
    | # | #        # | | ###Composite cConfigSection################ | # |
    | # | #        # | | #                                         # | # |
    | # | #        # | | #                                         # | # |
    | # | #        # | | #                                         # | # |
    | # | ########## | | #                                         # | # |
    | # |txtFilter X | | ########################################### | # |
    | # \------------/ \---------------------------------------------/ # |
    | ################################################################## |
    |                                                          [Buttons] |
    \--------------------------------------------------------------------/
    */
    try {
      Display d = composite.getDisplay();
      GridLayout configLayout;
  
      SashForm form = new SashForm(cConfig,SWT.HORIZONTAL);
      gridData = new GridData(GridData.FILL_BOTH);
      form.setLayoutData(gridData);
      
      Composite cLeftSide = new Composite(form, SWT.BORDER);
      gridData = new GridData(GridData.FILL_BOTH);
      cLeftSide.setLayoutData(gridData);
      
      FormLayout layout = new FormLayout();
      cLeftSide.setLayout(layout);
      
      Composite cFilterArea = new Composite(cLeftSide, SWT.NONE);
      cFilterArea.setLayout(new FormLayout());
      
      final Text txtFilter = new Text(cFilterArea, SWT.BORDER);
      final String sFilterText = MessageText.getString("ConfigView.filter");
      txtFilter.setText(sFilterText);
      txtFilter.selectAll();
      txtFilter.addModifyListener(new ModifyListener() {
      	public void modifyText(ModifyEvent e) {
      		filterTree(txtFilter.getText());
      	}
      });
      txtFilter.addMouseListener(new MouseAdapter() {
				public void mouseDown(MouseEvent e) {
					if (txtFilter.getText().equals(sFilterText)) {
						txtFilter.selectAll();
					}
				}
			});
      txtFilter.setFocus();
      
      ImageLoader imageLoader = ImageLoader.getInstance();
      imgSmallXGray = imageLoader.getImage("smallx-gray");
      imgSmallX = imageLoader.getImage("smallx");

  		lblX = new Label(cFilterArea, SWT.WRAP);
      Messages.setLanguageTooltip(lblX, "MyTorrentsView.clearFilter.tooltip");
      lblX.setImage(imgSmallXGray);
      lblX.addMouseListener(new MouseAdapter() {
      	public void mouseUp(MouseEvent e) {
      		txtFilter.setText("");
      	}
      });
      
      lblX.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent arg0) {
			    ImageLoader imageLoader = ImageLoader.getInstance();
			    imageLoader.releaseImage("smallx-gray");
			    imageLoader.releaseImage("smallx");
				}
			});
      
      Label lblSearch = new Label(cFilterArea, SWT.NONE);
      imageLoader.setLabelImage(lblSearch, "search");
  
      tree = new Tree(cLeftSide, SWT.NONE);
      FontData[] fontData = tree.getFont().getFontData();
      fontData[0].setStyle(SWT.BOLD);
      filterFoundFont = new Font(d, fontData);
      
      FormData formData;

      formData = new FormData();
      formData.bottom = new FormAttachment(100, -5);
      formData.left = new FormAttachment(0, 0);
      formData.right = new FormAttachment(100, 0);
      cFilterArea.setLayoutData(formData);
      
      formData = new FormData();
      formData.top = new FormAttachment(0,5);
      formData.left = new FormAttachment(0, 5);
      lblSearch.setLayoutData(formData);

      formData = new FormData();
      formData.top = new FormAttachment(0,5);
      formData.left = new FormAttachment(lblSearch,5);
      formData.right = new FormAttachment(lblX, -3);
      txtFilter.setLayoutData(formData);

      formData = new FormData();
      formData.top = new FormAttachment(0,5);
      formData.right = new FormAttachment(100,-5);
      lblX.setLayoutData(formData);

      formData = new FormData();
      formData.top = new FormAttachment(0, 0);
      formData.left = new FormAttachment(0,0);
      formData.right = new FormAttachment(100,0);
      formData.bottom = new FormAttachment(cFilterArea,-1);
      tree.setLayoutData(formData);
  
      Composite cRightSide = new Composite(form, SWT.NULL);
      configLayout = new GridLayout();
      configLayout.marginHeight = 3;
      configLayout.marginWidth = 0;
      cRightSide.setLayout(configLayout);
  
      // Header
      Composite cHeader = new Composite(cRightSide, SWT.BORDER);
      configLayout = new GridLayout();
      configLayout.marginHeight = 3;
      configLayout.marginWidth = 0;
      configLayout.numColumns = 2;
      configLayout.marginRight = 5;
      cHeader.setLayout(configLayout);
      gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
      cHeader.setLayoutData(gridData);
  
      cHeader.setBackground(d.getSystemColor(SWT.COLOR_LIST_SELECTION));
      cHeader.setForeground(d.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
  
      lHeader = new Label(cHeader, SWT.NULL);
      lHeader.setBackground(d.getSystemColor(SWT.COLOR_LIST_SELECTION));
      lHeader.setForeground(d.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
      fontData = lHeader.getFont().getFontData();
      fontData[0].setStyle(SWT.BOLD);
      int fontHeight = (int)(fontData[0].getHeight() * 1.2);
      fontData[0].setHeight(fontHeight);
      headerFont = new Font(d, fontData);
      lHeader.setFont(headerFont);
      gridData = new GridData(GridData.VERTICAL_ALIGN_CENTER | GridData.HORIZONTAL_ALIGN_BEGINNING);
      lHeader.setLayoutData(gridData);
      
      
      usermodeHint = new Label(cHeader, SWT.NULL);
      usermodeHint.setBackground(d.getSystemColor(SWT.COLOR_LIST_SELECTION));
      usermodeHint.setForeground(d.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
      gridData = new GridData(GridData.VERTICAL_ALIGN_CENTER | GridData.HORIZONTAL_ALIGN_END | GridData.GRAB_HORIZONTAL);
      usermodeHint.setLayoutData(gridData);
  
      // Config Section
      cConfigSection = new Composite(cRightSide, SWT.NULL);
      layoutConfigSection = new StackLayout();
      cConfigSection.setLayout(layoutConfigSection);
      gridData = new GridData(GridData.FILL_BOTH);
      gridData.horizontalIndent = 2;
      cConfigSection.setLayoutData(gridData);
  
  
      form.setWeights(new int[] {20,80});
  
      tree.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          Tree tree = (Tree)e.getSource();
          //Check that at least an item is selected
          //OSX lets you select nothing in the tree for example when a child is selected
          //and you close its parent.
          if(tree.getSelection().length > 0)
    	      showSection(tree.getSelection()[0]);
           }
      });
      // Double click = expand/contract branch
      tree.addListener(SWT.DefaultSelection, new Listener() {
        public void handleEvent(Event e) {
            TreeItem item = (TreeItem)e.item;
            if (item != null)
              item.setExpanded(!item.getExpanded());
        }
      });
    } catch (Exception e) {
    	Logger.log(new LogEvent(LOGID, "Error initializing ConfigView", e));
    }

    scResizeListener = new Listener() {
			public void handleEvent(Event event) {
				setupSC((ScrolledComposite)event.widget);
			}
		};


    // Add sections
    /** How to add a new section
     * 1) Create a new implementation of ConfigSectionSWT in a new file
     *    (Use the ConfigSectionTMP.java as a template if it's still around)
     * 2) import it into here
     * 3) add it to the internal sections list
     */
    pluginSections = ConfigSectionRepository.getInstance().getList();

    ConfigSection[] internalSections = { 
                                         new ConfigSectionMode(),
                                         new ConfigSectionStartShutdown(),
                                         new ConfigSectionConnection(),
                                         new ConfigSectionConnectionProxy(),
                                         new ConfigSectionConnectionAdvanced(),
                                         new ConfigSectionConnectionEncryption(),
                                         new ConfigSectionTransfer(),
                                         new ConfigSectionTransferAutoSpeedSelect(),
                                         new ConfigSectionTransferAutoSpeed(),
                                         new ConfigSectionTransferAutoSpeedBeta(),
                                         new ConfigSectionTransferLAN(),
                                         new ConfigSectionFile(), 
                                         new ConfigSectionFileMove(),
                                         new ConfigSectionFileTorrents(),
                                         new ConfigSectionFileTorrentsDecoding(),
                                         new ConfigSectionFilePerformance(),
                                         new ConfigSectionInterface(),
                                         new ConfigSectionInterfaceLanguage(),
                                         new ConfigSectionInterfaceStart(),
                                         new ConfigSectionInterfaceDisplay(),
                                         new ConfigSectionInterfaceTables(),
                                         new ConfigSectionInterfaceColor(),
                                         new ConfigSectionInterfaceAlerts(),
                                         new ConfigSectionInterfacePassword(),
                                         new ConfigSectionInterfaceLegacy(),
                                         new ConfigSectionIPFilter(),
                                         new ConfigSectionPlugins(this),
                                         new ConfigSectionStats(),
                                         new ConfigSectionTracker(),
                                         new ConfigSectionTrackerClient(),
                                         new ConfigSectionTrackerServer(),
                                         new ConfigSectionSecurity(),
                                         new ConfigSectionSharing(),
                                         new ConfigSectionLogging()
                                        };
    
    pluginSections.addAll(0, Arrays.asList(internalSections));

    for (int i = 0; i < pluginSections.size(); i++) {
   
    	// slip the non-standard "plugins" initialisation inbetween the internal ones
    	// and the plugin ones so plugin ones can be children of it
    	
      boolean	plugin_section = i >= internalSections.length;
      
      ConfigSection section = pluginSections.get(i);
      
      if (section instanceof ConfigSectionSWT || section instanceof UISWTConfigSection ) {
        String name;
        try {
          name = section.configSectionGetName();
         } catch (Exception e) {
        	 Logger.log(new LogEvent(LOGID, "A ConfigSection plugin caused an "
							+ "error while trying to call its "
							+ "configSectionGetName function", e));
          name = "Bad Plugin";
        }

         String	section_key = name;
         
         if ( plugin_section ){
         		// if resource exists without prefix then use it as plugins don't
         		// need to start with the prefix
         	
         	if ( !MessageText.keyExists(section_key)){
         		
         		section_key = sSectionPrefix + name;
         	}
         	
         }else{
         	
         	section_key = sSectionPrefix + name;
         }
         
         String	section_name = MessageText.getString( section_key );
         
         try {
          TreeItem treeItem;
          String location = section.configSectionGetParentSection();
  
          if ( location.length() == 0 || location.equalsIgnoreCase(ConfigSection.SECTION_ROOT)){
        	  //int position = findInsertPointFor(section_name, tree);
        	  //if ( position == -1 ){
        		  treeItem = new TreeItem(tree, SWT.NULL);
        	  // }else{
        	  //	  treeItem = new TreeItem(tree, SWT.NULL, position); 
        	  //}
         }else{
        	  TreeItem treeItemFound = findTreeItem(tree, location);
        	  if (treeItemFound != null){
        		  if (location.equalsIgnoreCase(ConfigSection.SECTION_PLUGINS)) {
        			  // Force ordering by name here.
        			  int position = findInsertPointFor(section_name, treeItemFound);
        			  if (position == -1) {
        				  treeItem = new TreeItem(treeItemFound, SWT.NULL);
        			  }
        			  else {
        				  treeItem = new TreeItem(treeItemFound, SWT.NULL, position);
        			  }
        		  }
        		  else {
        			  treeItem = new TreeItem(treeItemFound, SWT.NULL);
        		  }
        	  }else{
        		  treeItem = new TreeItem(tree, SWT.NULL);
        	  }
          }
  
          ScrolledComposite sc = new ScrolledComposite(cConfigSection, SWT.H_SCROLL | SWT.V_SCROLL);
          sc.setExpandHorizontal(true);
          sc.setExpandVertical(true);
          sc.setLayoutData(new GridData(GridData.FILL_BOTH));
      		sc.getVerticalBar().setIncrement(16);
      		sc.addListener(SWT.Resize, scResizeListener);
          
          if(i == 0) {
            Composite c;
            if ( section instanceof ConfigSectionSWT ){
          	  
          	  c = ((ConfigSectionSWT)section).configSectionCreate(sc);
          	  
            }else{
   
            	  c = ((UISWTConfigSection)section).configSectionCreate(sc);
            }
            sectionsCreated.add(section);
            sc.setContent(c);
          }
          
          Messages.setLanguageText(treeItem, section_key);
          treeItem.setData("Panel", sc);
          treeItem.setData("ID", name);
          treeItem.setData("ConfigSectionSWT", section);
          
          sections.put(treeItem, section);
          
          // ConfigSectionPlugins is special because it has to handle the
          // PluginConfigModel config pages
          if (section instanceof ConfigSectionPlugins)
          	((ConfigSectionPlugins)section).initPluginSubSections();
        } catch (Exception e) {
        	Logger.log(new LogEvent(LOGID, "ConfigSection plugin '" + name
							+ "' caused an error", e));
        }
      }
    }
    
 


    if (composite instanceof Shell) {
    	initApplyCloseButton();
    } else {
    	initSaveButton();
    }
    
    if (startSection != null) {
    	if (selectSection(startSection)) {
    		return;
    	}
    }

    TreeItem[] items = { tree.getItems()[0] };
    tree.setSelection(items);
    // setSelection doesn't trigger a SelectionListener, so..
    showSection(items[0]);
  }

	private void setupSC(ScrolledComposite sc) {
		Composite c = (Composite) sc.getContent();
		if (c != null) {
			Point size1 = c.computeSize(sc.getClientArea().width, SWT.DEFAULT);
			Point size = c.computeSize(SWT.DEFAULT, size1.y);
			sc.setMinSize(size);
		}
		sc.getVerticalBar().setPageIncrement(sc.getSize().y);
	}
	

  /**
	 * @param text
	 */
	protected void filterTree(String text) {
		filterText = text;
		if (filterDelayTimer != null) {
			filterDelayTimer.destroy();
		}
		
		if (lblX != null && !lblX.isDisposed()) {
			Image img = filterText.length() > 0 ? imgSmallX : imgSmallXGray;

			lblX.setImage(img);
		}


		filterDelayTimer = new Timer("Filter");
		filterDelayTimer.addEvent(SystemTime.getCurrentTime() + 300,
				new TimerEventPerformer() {
					public void perform(TimerEvent event) {
						filterDelayTimer.destroy();
						filterDelayTimer = null;

						Utils.execSWTThread(new AERunnable() {
							public void runSupport() {
								if (filterDelayTimer != null) {
									return;
								}
								Shell shell = tree.getShell();
								if (shell != null) {
									shell.setCursor(shell.getDisplay().getSystemCursor(
											SWT.CURSOR_WAIT));
								}
								try {
									ArrayList<TreeItem> foundItems = new ArrayList<TreeItem>();
									TreeItem[] items = tree.getItems();
									try {
										tree.setRedraw(false);
										for (int i = 0; i < items.length; i++) {
											items[i].setExpanded(false);
										}

										filterTree(items, filterText, foundItems);
									} finally {
										tree.setRedraw(true);
									}
								} finally {
									if (shell != null) {
										shell.setCursor(null);
									}
									TreeItem[] selection = tree.getSelection();
									if (selection != null && selection.length > 0) {
										showSection(selection[0]);
									}
								}
							}
						});
					}
				});
	}

	protected void filterTree(TreeItem[] items, String text,
			ArrayList<TreeItem> foundItems) {
		text = text.toLowerCase();
		for (int i = 0; i < items.length; i++) {
			ensureSectionBuilt(items[i], false);
			ScrolledComposite composite = (ScrolledComposite) items[i].getData("Panel");

			if (text.length() > 0
					&& (items[i].getText().toLowerCase().indexOf(text) >= 0 || compositeHasText(
							composite, text))) {
				foundItems.add(items[i]);

				ensureExpandedTo(items[i]);
				items[i].setFont(filterFoundFont);
			} else {
				items[i].setFont(null);
			}
			filterTree(items[i].getItems(), text, foundItems);
		}
	}
	
	private void ensureExpandedTo(TreeItem item) {
    TreeItem itemParent = item.getParentItem();
  	if (itemParent != null) {
  		itemParent.setExpanded(true);
  		ensureExpandedTo(itemParent);
  	}
	}

	/**
	 * @param composite 
	 * @param text
	 * @return
	 */
	private boolean compositeHasText(Composite composite, String text) {
		Control[] children = composite.getChildren();
		
		for (int i = 0; i < children.length; i++) {
			Control child = children[i];
			if (child instanceof Label) {
				if (((Label)child).getText().toLowerCase().indexOf(text) >= 0) {
					return true;
				}
			} else if (child instanceof Group) {
				if (((Group)child).getText().toLowerCase().indexOf(text) >= 0) {
					return true;
				}
			} else if (child instanceof Button) {
				if (((Button)child).getText().toLowerCase().indexOf(text) >= 0) {
					return true;
				}
			} else if (child instanceof List) {
				String[] items = ((List)child).getItems();
				for (String item : items) {
					if (item.toLowerCase().indexOf(text) >= 0) {
						return true;
					}
				}
			} else if (child instanceof Combo) {
				String[] items = ((Combo)child).getItems();
				for (String item : items) {
					if (item.toLowerCase().indexOf(text) >= 0) {
						return true;
					}
				}
			}

			if (child instanceof Composite) {
				if (compositeHasText((Composite) child, text)) {
					return true;
				}
			}
		}
		
		return false;
	}

	private void showSection(TreeItem section) {
    ScrolledComposite item = (ScrolledComposite)section.getData("Panel");

    if (item != null) {
    	
    	ensureSectionBuilt(section, true);
    	
      layoutConfigSection.topControl = item;
      
      setupSC(item);

      if (filterText != null && filterText.length() > 0) {
      	hilightText(item, filterText);
        item.layout(true, true);
      }

      cConfigSection.layout();
      
      updateHeader(section);
    }
  }
	
	private void hilightText(Composite c, String text) {
		Control[] children = c.getChildren();
		for (Control child : children) {
			if (child instanceof Composite) {
				hilightText((Composite) child, text);
			}

			if (child instanceof Label) {
				if (((Label)child).getText().toLowerCase().indexOf(text) >= 0) {
					hilightControl(child);
				}
			} else if (child instanceof Group) {
				if (((Group)child).getText().toLowerCase().indexOf(text) >= 0) {
					hilightControl(child);
				}
			} else if (child instanceof Button) {
				if (((Button)child).getText().toLowerCase().indexOf(text) >= 0) {
					hilightControl(child);
				}
			} else if (child instanceof List) {
				String[] items = ((List)child).getItems();
				for (String item : items) {
					if (item.toLowerCase().indexOf(text) >= 0) {
						hilightControl(child);
						break;
					}
				}
			} else if (child instanceof Combo) {
				String[] items = ((Combo)child).getItems();
				for (String item : items) {
					if (item.toLowerCase().indexOf(text) >= 0) {
						hilightControl(child);
						break;
					}
				}
			}

		}
	}

	/**
	 * @param child
	 *
	 * @since 4.5.1.1
	 */
	private void hilightControl(Control child) {
		child.setFont(headerFont);
		child.setBackground(child.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		child.setForeground(child.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
	}

	private void ensureSectionBuilt(TreeItem treeSection, boolean recreateIfAlreadyThere) {
    ScrolledComposite item = (ScrolledComposite)treeSection.getData("Panel");

    if (item != null) {
    	
      ConfigSection configSection = (ConfigSection)treeSection.getData("ConfigSectionSWT");
      
      if (configSection != null) {
    	  
        Control previous = item.getContent();
        if (previous instanceof Composite) {
        	if (!recreateIfAlreadyThere) {
        		return;
        	}
        	configSection.configSectionDelete();
          sectionsCreated.remove(configSection);    	
          Utils.disposeComposite((Composite)previous,true);
        }
        
        Composite c;
        
        if ( configSection instanceof ConfigSectionSWT ){
      	  
      	  c = ((ConfigSectionSWT)configSection).configSectionCreate(item);
      	  
        }else{

          c = ((UISWTConfigSection)configSection).configSectionCreate(item);
        }
        sectionsCreated.add(configSection);
        
        item.setContent(c);
      }
    }
	}

  private void updateHeader(TreeItem section) {
		if (section == null)
			return;
		
		int userMode = COConfigurationManager.getIntParameter("User Mode");
		int maxUsermode = 0;
		try
		{
			ConfigSection sect = sections.get(section);
			if (sect instanceof UISWTConfigSection)
			{
				maxUsermode = ((UISWTConfigSection) sect).maxUserMode();
			}
		} catch (Error e)
		{
			//Debug.printStackTrace(e);
		}
		
		if (userMode < maxUsermode)
			Messages.setLanguageText(usermodeHint, "ConfigView.higher.mode.available");
		else
			usermodeHint.setText("");
		
		String sHeader = section.getText();
		section = section.getParentItem();
		while (section != null)
		{
			sHeader = section.getText() + " : " + sHeader;
			section = section.getParentItem();
		}
		lHeader.setText(" " + sHeader.replaceAll("&", "&&"));
		lHeader.getParent().layout(true, true);
	}


  public Composite createConfigSection(TreeItem treeItemParent, 
                                        String sNameID, 
                                        int position, 
                                        boolean bPrefix) {
    ScrolledComposite sc = new ScrolledComposite(cConfigSection, SWT.H_SCROLL | SWT.V_SCROLL);
    sc.setExpandHorizontal(true);
    sc.setExpandVertical(true);
    sc.setLayoutData(new GridData(GridData.FILL_BOTH));
		sc.getVerticalBar().setIncrement(16);
		sc.addListener(SWT.Resize, scResizeListener);

    Composite cConfigSection = new Composite(sc, SWT.NULL);
    
    String section_key = ((bPrefix) ? sSectionPrefix : "") + sNameID;
    
    if (position == -2) { // Means "auto-order".
    	position = findInsertPointFor(MessageText.getString(section_key), (treeItemParent == null) ? (Object)tree : (Object)treeItemParent);
    }

    TreeItem treeItem;
    if (treeItemParent == null) {
      if (position >= 0)
        treeItem = new TreeItem(tree, SWT.NULL, position);
      else
        treeItem = new TreeItem(tree, SWT.NULL);
    } else {
      if (position >= 0)
        treeItem = new TreeItem(treeItemParent, SWT.NULL, position);
      else
        treeItem = new TreeItem(treeItemParent, SWT.NULL);
    }
    Messages.setLanguageText(treeItem, section_key);
    treeItem.setData("Panel", sc);
    treeItem.setData("ID", sNameID);

    sc.setContent(cConfigSection);
    return cConfigSection;
  }
  
  private static Comparator<Object> insert_point_comparator = new Comparator<Object>() {
	  
	  private String asString(Object o) {
		  if (o instanceof String) {
			  return (String)o;
		  }
		  else if (o instanceof TreeItem) {
			  return ((TreeItem)o).getText();
		  }
		  else {
			  throw new ClassCastException("object is not String or TreeItem: " + o.getClass().getName());
		  }
	  }
	  
	  public int compare(Object o1, Object o2) {
		  int result = String.CASE_INSENSITIVE_ORDER.compare(asString(o1), asString(o2));
		  return result;
	  }
  };
  
  private static int findInsertPointFor(String name, Object structure) {
	  TreeItem[] children = null;
	  if (structure instanceof Tree) {
	      children = ((Tree)structure).getItems();
	  }
	  else {
		  children = ((TreeItem)structure).getItems();
	  }
	  if (children.length == 0) {return -1;}
	  int result =  Arrays.binarySearch(children, name, insert_point_comparator);
	  if (result > 0) {return result;}
	  result = -(result+1);
	  if (result == children.length) {
		  result = -1;
	  }
	  return result;
  }

  public TreeItem findTreeItem(String ID) {
  	return findTreeItem((Tree)null, ID);
  }

  private TreeItem findTreeItem(Tree tree, String ID) {
  	if (tree == null) {
  		tree = this.tree;
  	}
  	if (tree == null) {
  		return null;
  	}
    TreeItem[] items = tree.getItems();
    for (int i = 0; i < items.length; i++) {
      String itemID = (String)items[i].getData("ID");
      if (itemID != null && itemID.equalsIgnoreCase(ID)) {
        return items[i];
      }
      TreeItem itemFound = findTreeItem(items[i], ID);
      if (itemFound != null)
        return itemFound;
    }
	 return null;
  }

  private TreeItem findTreeItem(TreeItem item, String ID) {
    TreeItem[] subItems = item.getItems();
    for (int i = 0; i < subItems.length; i++) {
      String itemID = (String)subItems[i].getData("ID");
      if (itemID != null && itemID.equalsIgnoreCase(ID)) {
        return subItems[i];
      }

      TreeItem itemFound = findTreeItem(subItems[i], ID);
      if (itemFound != null)
        return itemFound;
    }
    return null;
  }

  private void initSaveButton() {
    GridData gridData;
    final Button save = new Button(cConfig, SWT.PUSH);
    Messages.setLanguageText(save, "ConfigView.button.save"); //$NON-NLS-1$
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
    gridData.horizontalSpan = 2;
    gridData.widthHint = 80;
    save.setLayoutData(gridData);

    save.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent event) {
				// force focusout on osx
				save.setFocus();
				save();
			}
		});
  }

  private void initApplyCloseButton() {
  	Composite cButtons = new Composite(cConfig, SWT.NONE);
  	GridLayout gridLayout = new GridLayout();
		gridLayout.horizontalSpacing = gridLayout.verticalSpacing = gridLayout.marginHeight = gridLayout.marginWidth = 0;
		gridLayout.numColumns = 2;
		cButtons.setLayout(gridLayout);
		cButtons.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
  	
    GridData gridData;
    final Button apply = new Button(cButtons, SWT.PUSH);
    Messages.setLanguageText(apply, "Button.apply");
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
    gridData.widthHint = 80;
    apply.setLayoutData(gridData);

    apply.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent event) {
				// force focusout on osx
				apply.setFocus();
				save();
			}
		});
    
    final Button close = new Button(cButtons, SWT.PUSH);
    Messages.setLanguageText(close, "Button.close");
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
    gridData.widthHint = 80;
    close.setLayoutData(gridData);

    close.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent event) {
				// force focusout on osx
				apply.setFocus();
				save();
				apply.getShell().dispose();
			}
		});
  }
  
  private Composite getComposite() {
    return cConfig;
  }

  private void updateLanguage() {
    updateHeader(tree.getSelection()[0]);
    if (swtView != null) {
    	swtView.setTitle(getFullTitle());
    }
//    cConfig.setSize(cConfig.computeSize(SWT.DEFAULT, SWT.DEFAULT));
  }

  private void delete() {
  	for (ConfigSection section : sectionsCreated) {
    	try {
    		section.configSectionDelete();
    	} catch (Exception e) {
    		Debug.out("Error while deleting config section", e);
    	}
    }
  	sectionsCreated.clear();
  	if ( pluginSections != null ){
  		pluginSections.clear();
  	}
  	if ( tree != null ){
	    if(! tree.isDisposed()) {
		    TreeItem[] items = tree.getItems();
		    for (int i = 0; i < items.length; i++) {
		      Composite c = (Composite)items[i].getData("Panel");
		      Utils.disposeComposite(c);
		      items[i].setData("Panel", null);
		
		      items[i].setData("ConfigSectionSWT", null);
		    }
	    }
  	}
    Utils.disposeComposite(cConfig);

  	Utils.disposeSWTObjects(new Object[] { headerFont, filterFoundFont });
		headerFont = null;
		filterFoundFont = null;
  }

  private String getFullTitle() {
  	/*
  	 * Using resolveLocalizationKey because there are different version for Classic vs. Vuze
  	 */
    return MessageText.getString(MessageText.resolveLocalizationKey("ConfigView.title.full")); //$NON-NLS-1$
  }

  public boolean selectSection(String id) {
		TreeItem ti = findTreeItem(id);
		if (ti == null)
			return false;
		tree.setSelection(new TreeItem[] { ti });
		showSection(ti);
		return true;
	}

  public void
  selectSection(
  	Class<?>	config_section_class )
  {
	  TreeItem[]	items = tree.getItems();
	  
	  for (int i=0;i<items.length;i++){
		  
		  TreeItem	item = items[i];
		  	    	
		  ConfigSection section = (ConfigSection)item.getData("ConfigSectionSWT");
			  
		  if ( section != null && section.getClass() == config_section_class ){
				  
			  tree.setSelection( new TreeItem[]{ item });
			  
			  showSection( item );
			  
			  break;
		  }
	  }
  }
  
  public void save() {
		COConfigurationManager.setParameter("updated", 1);
		COConfigurationManager.save();

		if (null != pluginSections) {
			for (int i = 0; i < pluginSections.size(); i++) {
				pluginSections.get(i).configSectionSave();
			}
		}
	}
  
  private void dataSourceChanged(Object newDataSource) {
  	
  	if (newDataSource instanceof String) {
			String id = (String) newDataSource;
	  	startSection = id;
	  	Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					selectSection(startSection);
				}
			});
		}
  }

	public boolean eventOccurred(UISWTViewEvent event) {
    switch (event.getType()) {
      case UISWTViewEvent.TYPE_CREATE:
      	swtView = (UISWTView)event.getData();
      	swtView.setTitle(getFullTitle());
        break;

      case UISWTViewEvent.TYPE_DESTROY:
        delete();
        break;

      case UISWTViewEvent.TYPE_INITIALIZE:
        initialize((Composite)event.getData());
        break;

      case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
      	Messages.updateLanguageForControl(getComposite());
      	updateLanguage();
        break;

      case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:
      	dataSourceChanged(event.getData());
        break;
        
      case UISWTViewEvent.TYPE_FOCUSGAINED:
      	break;
        
      case UISWTViewEvent.TYPE_REFRESH:
        break;
    }

    return true;
  }

}
