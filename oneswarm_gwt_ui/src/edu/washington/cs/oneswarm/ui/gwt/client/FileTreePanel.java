package edu.washington.cs.oneswarm.ui.gwt.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.TreeListener;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FileTree;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmUIServiceAsync;

public class FileTreePanel extends VerticalPanel {
	public Tree t;
	public final Label loadingLabel = new Label("Loading, please wait...");

	public FileTreePanel(String path) {
		t = new Tree();

		add(loadingLabel);
		OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();
		service.getFiles(OneSwarmRPCClient.getSessionID(), path, new AsyncCallback<FileTree>() {

			public void onFailure(Throwable caught) {
				System.out.print("failed:" + caught.getMessage());
				loadingLabel.setText("error: " + caught.getMessage());
			}

			public void onSuccess(FileTree result) {
				root = new CheckBoxTreeItem(result);

				update();
				t.addTreeListener(treeExpander);
			}

		});
	}

	private String filter = null;
	private CheckBoxTreeItem root;
	private HorizontalPanel bottomButtons;

	private TreeListener treeExpander = new TreeListener() {

		public void onTreeItemSelected(TreeItem item) {
		}

		public void onTreeItemStateChanged(TreeItem item) {
			if (item.getState() == true) {
				final CheckBoxTreeItem checkBoxTreeItem = (CheckBoxTreeItem) item;
				checkBoxTreeItem.addChildren(1, null);
				bottomButtons.setVisible(root.getTotalChildCount() > 15);
			}
		}
	};

	private void update() {
		this.clear();
		root.addChildren(1, filter);
		loadingLabel.setVisible(false);
		t.addItem(root);

		recursiveInitialCheck(root, filter);

		this.add(createButtons(root, true));
		this.add(t);

		bottomButtons = createButtons(root, false);
		bottomButtons.setVisible(false);
		this.add(bottomButtons);
	}

	private HorizontalPanel createButtons(final CheckBoxTreeItem root, boolean top) {
		/*
		 * buttons for easy sharing
		 */
		final ListBox level = new ListBox();
		HorizontalPanel buttonPanel = new HorizontalPanel();

		Button selectAllButton = new Button("Check level: ");
		selectAllButton.addStyleName(OneSwarmCss.SMALL_BUTTON);
		selectAllButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent sender) {
				uncheckAll();
				checkLevel(root, 0, level.getSelectedIndex());
			}
		});
		buttonPanel.add(selectAllButton);

		// level.addStyleName(SaveLocationPanel.CSS_SMALL_BUTTON);
		for (int i = 0; i < 6; i++) {
			level.addItem("" + i);
		}
		level.setSelectedIndex(1);
		buttonPanel.add(level);

		Button noneButton = new Button("Select none");
		noneButton.addStyleName(OneSwarmCss.SMALL_BUTTON);
		noneButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent sender) {
				uncheckAll();
			}
		});
		buttonPanel.add(noneButton);

		Button autoButton = new Button("Auto Grouping");
		autoButton.addStyleName(OneSwarmCss.SMALL_BUTTON);
		autoButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent sender) {
				recursiveInitialCheck(root, null);
			}
		});
		buttonPanel.add(autoButton);

		if (top && false) {
			final TextBox filterBox = new TextBox();
			buttonPanel.add(filterBox);
			Button filterButton = new Button("Filter");
			buttonPanel.add(filterButton);
			filterButton.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					filter = filterBox.getText();
					update();
				}
			});
			filterBox.addKeyUpHandler(new KeyUpHandler() {
				public void onKeyUp(KeyUpEvent event) {
					if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
						filter = filterBox.getText();
						update();
					}
				}
			});
		}

		return buttonPanel;
	}

	public void checkLevel(CheckBoxTreeItem item, int currentLevel, int levelToCheck) {
		if (item == null) {
			return;
		} else if (currentLevel > levelToCheck) {
			return;
		} else if (levelToCheck == currentLevel) {
			TreeItem parent = item.getParentItem();
			if (parent != null) {
				parent.setState(true);
			}
			item.setValue(true);
			item.updateChildren(true);
		} else {
			for (CheckBoxTreeItem kid : item.getChildList()) {
				kid.addChildren(1, null);
				checkLevel(kid, currentLevel + 1, levelToCheck);
			}
		}
	}

	public void uncheckAll() {
		((CheckBoxTreeItem) t.getItem(0)).unCheckedRecursive();
		((CheckBoxTreeItem) t.getItem(0)).setState(true);
	}

	public void recursiveInitialCheck(CheckBoxTreeItem curr, String filter) {
		if (curr.fileTree.getMagicCheck()) {
			curr.setState(false);
			curr.setValue(true);
			curr.updateChildren(true);
		} else if (curr.fileTree.getCheckedChild()) {
			curr.setState(true);
			curr.addChildren(1, filter);
			for (CheckBoxTreeItem kid : curr.getChildList())
				recursiveInitialCheck(kid, filter);
		} else {
			curr.setState(false);
			curr.setValue(false);
			curr.updateChildren(false);
		}
	}

	public ArrayList<String> getSelected() {
		ArrayList<String> sel = new ArrayList<String>();
		LinkedList<CheckBoxTreeItem> itemsToCheck = new LinkedList<CheckBoxTreeItem>();
		itemsToCheck.addFirst((CheckBoxTreeItem) t.getItem(0));

		while (itemsToCheck.size() > 0) {
			CheckBoxTreeItem currItem = itemsToCheck.removeFirst();
			if (currItem.isFirstSelected()) {
				System.out.println("adding " + currItem.getFilePath());
				sel.add(currItem.getFilePath());
			} else {
				itemsToCheck.addAll(0, currItem.getChildList());
			}
		}

		return sel;
	}

	class CheckBoxTreeItem extends TreeItem {
		private final FileTree fileTree;
		private final CheckBox checkBox;

		public boolean isFirstSelected() {
			return checkBox.getValue() && checkBox.isEnabled();
		}

		public CheckBoxTreeItem(FileTree fileTree) {
			checkBox = new CheckBox(fileTree.getName());
			this.setWidget(checkBox);
			this.fileTree = fileTree;

			checkBox.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					boolean checked = ((CheckBox) event.getSource()).getValue();
					updateChildren(checked);
				}
			});

			checkBox.setValue(fileTree.getMagicCheck());
			this.setState(fileTree.getCheckedChild());
		}

		public String getFilePath() {
			return fileTree.getFullpath();
		}

		public int getTotalChildCount() {
			int count = 0;
			final Collection<CheckBoxTreeItem> children = getChildList();
			for (CheckBoxTreeItem c : children) {
				count += c.getTotalChildCount();
			}
			return count;
		}

		public void setState(boolean state) {
			if (state == true) {
				super.setState(true);
				TreeItem parent = getParentItem();
				if (parent != null) {
					parent.setState(true);
				}
			} else {
				super.setState(false);
			}
		}

		public void unCheckedRecursive() {
			checkBox.setEnabled(true);
			checkBox.setValue(false);
			this.setState(false);
			int childNum = this.getChildCount();
			for (int i = 0; i < childNum; i++) {
				CheckBoxTreeItem child = this.getChild(i);
				child.unCheckedRecursive();
			}

		}

		public Collection<CheckBoxTreeItem> getChildList() {
			List<CheckBoxTreeItem> c = new LinkedList<CheckBoxTreeItem>();
			int childNum = this.getChildCount();
			for (int i = 0; i < childNum; i++) {
				CheckBoxTreeItem child = this.getChild(i);
				c.add(child);
			}
			return c;
		}

		public void setValue(boolean checked) {
			checkBox.setValue(checked);
		}

		public void setEnabled(boolean enabled) {
			checkBox.setEnabled(enabled);
		}

		public void updateChildren(boolean checked) {
			int cNum = CheckBoxTreeItem.this.getChildCount();
			for (int i = 0; i < cNum; i++) {
				CheckBoxTreeItem child = CheckBoxTreeItem.this.getChild(i);
				child.setValue(checked);
				child.setEnabled(!checked);
				child.updateChildren(checked);
			}
		}

		public void addChildren(int depth, String filter) {

			// first, check if we need to add the child items
			if (this.getChildCount() == 0) {
				if (fileTree.getChildren() != null) {
					for (FileTree child : fileTree.getChildren()) {
						if (child.matches(filter)) {
							CheckBoxTreeItem childItem = new CheckBoxTreeItem(child);
							childItem.setValue(checkBox.getValue());
							childItem.setEnabled(checkBox.isEnabled());
							this.addItem(childItem);
						}
					}

				}
			}
			// then add the grand kids
			int childNum = this.getChildCount();
			for (int i = 0; i < childNum; i++) {
				CheckBoxTreeItem childItem = this.getChild(i);
				if (depth > 0) {
					if (childItem.fileTree.matches(filter)) {
						childItem.addChildren(depth - 1, filter);
					}
				}
			}
		}

		public CheckBoxTreeItem getChild(int index) {
			return (CheckBoxTreeItem) super.getChild(index);
		}
	}
}