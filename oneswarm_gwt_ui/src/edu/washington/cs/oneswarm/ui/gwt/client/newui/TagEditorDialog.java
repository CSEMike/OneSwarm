package edu.washington.cs.oneswarm.ui.gwt.client.newui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.allen_sauer.gwt.dnd.client.DragContext;
import com.allen_sauer.gwt.dnd.client.PickupDragController;
import com.allen_sauer.gwt.dnd.client.drop.SimpleDropController;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HorizontalSplitPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.ReportableErrorDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.SwarmsBrowser.TorrentContainingImage;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.settings.SettingsDialog;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FileTree;
import edu.washington.cs.oneswarm.ui.gwt.rpc.TorrentInfo;

public class TagEditorDialog extends OneSwarmDialogBox {

	public static final int WIDTH = 550;
	public static final int HEIGHT = 450;

	Button updateButton = new Button("Update");
	Button cancelButton = new Button("Cancel");
	private Button addSelectedButton;
	private Button moveButton;
	private Label selectLabel;
	private TorrentInfo[] mTorrents;
	private VerticalPanel leftVP;
	private Button addButton;
	private Button removeButton;
	private Button clearButton;
	private Tree mCurrentTagsTree;
	private Tree mAllTagsTree;
	private TreeItem mCurrentTagsRoot;
	private TreeItem mAllTagsRoot;
	private EntireUIRoot mUIRoot;

	public TagEditorDialog(EntireUIRoot entireUIRoot, TorrentInfo[] torrents) {
		super(false, false, true);

		mUIRoot = entireUIRoot;

		mTorrents = torrents;

		this.setText("Edit tags");

		VerticalPanel mainPanel = new VerticalPanel();

		selectLabel = new Label("Editing tags for " + torrents.length + " selected swarm" + (torrents.length > 1 ? "s" : ""));
		selectLabel.addStyleName(CSS_DIALOG_HEADER);
		selectLabel.setWidth(WIDTH + "px");
		mainPanel.add(selectLabel);

		HorizontalSplitPanel splitPanel = new HorizontalSplitPanel();
		splitPanel.setLeftWidget(createLeftPanel());
		splitPanel.setRightWidget(createRightPanel());
		mainPanel.add(splitPanel);

		splitPanel.setHeight("250px");

		HorizontalPanel buttons_hp = new HorizontalPanel();
		buttons_hp.add(cancelButton);
		buttons_hp.add(updateButton);
		buttons_hp.setSpacing(3);

		cancelButton.addClickHandler(this);
		updateButton.addClickHandler(this);

		com.google.gwt.user.client.ui.Widget hrule = new SimplePanel();
		hrule.addStyleName(SettingsDialog.CSS_HRULE);
		mainPanel.add(hrule);

		mainPanel.add(buttons_hp);
		mainPanel.setCellHorizontalAlignment(buttons_hp, HorizontalPanel.ALIGN_RIGHT);

		this.setWidget(mainPanel);
	}

	private Widget createRightPanel() {
		final VerticalPanel rightVP = new VerticalPanel();

		rightVP.add(new HTML("<b>Available tags:</b>"));
		rightVP.add(new Label("Loading..."));

		// getAllTags
		OneSwarmRPCClient.getService().getAllTags(OneSwarmRPCClient.getSessionID(), new AsyncCallback<FileTree>() {

			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}

			public void onSuccess(FileTree result) {
				rightVP.remove(1);

				if (result == null) {
					hide();
					new ReportableErrorDialogBox("Couldn't load tag list!", false);
				}

				mAllTagsTree = new Tree();
				mAllTagsRoot = mAllTagsTree.addItem("All tags");
				addChildren(mAllTagsRoot, result.getChildren(), null);

				expandAll(mAllTagsRoot);
				mAllTagsRoot.setSelected(true);
				mAllTagsTree.setSelectedItem(mAllTagsRoot, true);

				mAllTagsTree.addSelectionHandler(new SelectionHandler<TreeItem>() {
					public void onSelection(SelectionEvent<TreeItem> event) {
						boolean enabled = !event.getSelectedItem().equals(mAllTagsRoot);
						if (addSelectedButton != null) {
							addSelectedButton.setEnabled(enabled);
						}
					}
				});

				HorizontalPanel rightActions = new HorizontalPanel();
				addSelectedButton = new Button("Add");
				moveButton = new Button("Move");

				addSelectedButton.setEnabled(false);

				addSelectedButton.addStyleName(OneSwarmCss.SMALL_BUTTON);
				moveButton.addStyleName(OneSwarmCss.SMALL_BUTTON);

				addSelectedButton.addClickHandler(TagEditorDialog.this);
				moveButton.addClickHandler(TagEditorDialog.this);

				rightActions.add(moveButton);
				rightActions.add(addSelectedButton);
				rightActions.setSpacing(2);

				rightVP.add(rightActions);

				rightVP.add(mAllTagsTree);

			}
		});

		return rightVP;
	}

	class MutableInt {
		public int v;
	};

	private Widget createLeftPanel() {
		leftVP = new VerticalPanel();

		leftVP.add(new HTML("<b>Current tags:</b>"));
		leftVP.add(new Label("Loading..."));

		mCurrentTagsTree = new Tree();
		mCurrentTagsRoot = mCurrentTagsTree.addItem("All tags");
		mCurrentTagsTree.setSelectedItem(mCurrentTagsRoot, true);

		mCurrentTagsTree.addSelectionHandler(new SelectionHandler<TreeItem>() {

			public void onSelection(SelectionEvent<TreeItem> event) {
				boolean enabled = !event.getSelectedItem().equals(mCurrentTagsRoot);
				if (removeButton != null) {
					removeButton.setEnabled(enabled);
				}
			}
		});

		final MutableInt done = new MutableInt();

		for (TorrentInfo t : mTorrents) {
			OneSwarmRPCClient.getService().getTags(OneSwarmRPCClient.getSessionID(), t.getTorrentID(), new AsyncCallback<FileTree>() {
				public void onFailure(Throwable caught) {
					caught.printStackTrace();
				}

				public void onSuccess(FileTree result) {
					addChildren(mCurrentTagsRoot, result.getChildren(), null);

					if ((++done.v) == mTorrents.length) {
						leftLoadDone(mCurrentTagsTree);
						expandAll(mCurrentTagsRoot);
					}
				}
			});
		}

		return leftVP;
	}

	private void leftLoadDone(Tree finishedTree) {
		leftVP.remove(1);

		HorizontalPanel buttonsHP = new HorizontalPanel();
		addButton = new Button("Add");
		removeButton = new Button("Remove");
		removeButton.setEnabled(false);
		clearButton = new Button("Clear");
		buttonsHP.add(addButton);
		buttonsHP.add(removeButton);
		buttonsHP.add(clearButton);

		addButton.addClickHandler(this);
		removeButton.addClickHandler(this);
		clearButton.addClickHandler(this);

		addButton.addStyleName(OneSwarmCss.SMALL_BUTTON);
		removeButton.addStyleName(OneSwarmCss.SMALL_BUTTON);
		clearButton.addStyleName(OneSwarmCss.SMALL_BUTTON);

		buttonsHP.setSpacing(2);

		leftVP.add(buttonsHP);

		leftVP.add(finishedTree);
	}

	private void commit() {
		System.out.println("should save here");
		List<String> tags = new ArrayList<String>();
		for (int kItr = 0; kItr < mCurrentTagsRoot.getChildCount(); kItr++) {
			for (String tag : getPaths(mCurrentTagsRoot.getChild(kItr))) {
				tags.add(tag);
			}
		}

		for (String s : tags) {
			System.out.println("client sending tag: " + s);
		}

		String[] rpcArr = null;
		if (tags.size() > 0) {
			rpcArr = tags.toArray(new String[0]);
		}
		for (TorrentInfo torrent : mTorrents) {
			OneSwarmRPCClient.getService().setTags(OneSwarmRPCClient.getSessionID(), torrent.getTorrentID(), rpcArr, new AsyncCallback<Void>() {
				public void onFailure(Throwable caught) {
					caught.printStackTrace();
				}

				public void onSuccess(Void result) {
					mUIRoot.refreshSwarms();
				}
			});
		}
	}

	private static List<String> getPaths(TreeItem curr) {
		if (curr.getChildCount() == 0) {
			return Arrays.asList(new String[] { curr.getText() });
		} else {
			List<String> out = new ArrayList<String>();
			for (int kItr = 0; kItr < curr.getChildCount(); kItr++) {
				for (String path : getPaths(curr.getChild(kItr))) {
					out.add(curr.getText() + "/" + path);
				}
			}
			return out;
		}
	}

	public static void addChildren(TreeItem parent, FileTree[] kids, PickupDragController controller) {
		if (kids == null) {
			return;
		}
		boolean found = false;
		for (FileTree kid : kids) {
			found = false;
			TreeItem kti = null;
			String name = kid.getName();
			for (int kItr = 0; kItr < parent.getChildCount(); kItr++) {
				if (parent.getChild(kItr).getText().equals(name)) {
					found = true;
					kti = parent.getChild(kItr);
					break;
				}
			}
			if (!found) {
				final Label l = new Label(name);
				kti = new TreeItem(l) {
					public String getText() {
						return l.getText();
					}

					public void setSelected(boolean selected) {
						if (selected) {
							l.addStyleDependentName("selected");
						} else {
							l.removeStyleDependentName("selected");
						}
						super.setSelected(selected);
					}
				};

				// kti = parent.addItem(name);
				parent.addItem(kti);
				if (controller != null) {
					controller.registerDropController(new TreeItemDropController(l, kti));
				}

				l.setStylePrimaryName("gwt-TreeItem");
				l.setStyleName("gwt-TreeItem");
			}
			addChildren(kti, kid.getChildren(), controller);
		}
	}

	final static class TreeItemDropController extends SimpleDropController {
		private static final String SELECTED = "selected";
		private final TreeItem item;

		private TreeItemDropController(Label dropTarget, TreeItem item) {
			super(dropTarget);
			this.item = item;
		}

		public void onDrop(DragContext context) {
			List<Widget> selectedWidgets = context.selectedWidgets;
			// Window.alert("got drag and drop");
			for (Widget widget : selectedWidgets) {
				if (widget instanceof TorrentContainingImage) {
					TorrentInfo torrent = ((TorrentContainingImage) widget).getTorrent();
					String path = item.getText();
					TreeItem current = item;
					TreeItem parent;
					while ((parent = current.getParentItem()) != null && parent.getParentItem() != null) {
						path = parent.getText() + "/" + path;
						current = parent;
					}
					if (Window.confirm("Set tag for '" + torrent.getName() + "' to '" + path + "'?")) {
						OneSwarmRPCClient.getService().setTags(OneSwarmRPCClient.getSessionID(), torrent.getTorrentID(), new String[] { path }, new AsyncCallback<Void>() {
							public void onFailure(Throwable caught) {
								caught.printStackTrace();
							}

							public void onSuccess(Void result) {
								EntireUIRoot.getRoot(item.getWidget()).refreshSwarms();
							}
						});
					}
				}
			}
		}

		public void onEnter(DragContext context) {

			if (!item.isSelected()) {
				item.addStyleDependentName(SELECTED);
				if (item.getWidget() != null) {
					item.getWidget().addStyleDependentName(SELECTED);
				}
			}
		}

		public void onLeave(DragContext context) {
			if (!item.isSelected()) {
				item.removeStyleDependentName(SELECTED);
				if (item.getWidget() != null) {
					item.getWidget().removeStyleDependentName(SELECTED);
				}

			}
		}
	}

	public static void expandAll(TreeItem curr) {
		curr.setState(true);
		for (int kItr = 0; kItr < curr.getChildCount(); kItr++) {
			expandAll(curr.getChild(kItr));
		}
	}

	public void onClick(ClickEvent event) {
		boolean moveIt = false;
		if (event.getSource().equals(moveButton)) {
			moveIt = true; // this does the next two actions in order, which is
			// a move
		}
		if (event.getSource().equals(clearButton) || moveIt) {
			this.mCurrentTagsTree.clear();
			mCurrentTagsRoot = mCurrentTagsTree.addItem("All tags");
		}
		if (event.getSource().equals(addSelectedButton) || moveIt) {
			TreeItem selected = mAllTagsTree.getSelectedItem();
			TreeItem added = selected;
			List<String> toAdd = new ArrayList<String>();
			while (added.equals(mAllTagsRoot) == false) {
				toAdd.add(added.getText());
				added = added.getParentItem();
			}
			Collections.reverse(toAdd);
			TreeItem curr = mCurrentTagsRoot;
			boolean found = false;
			for (String s : toAdd) {
				found = false;
				for (int kid = 0; kid < curr.getChildCount(); kid++) {
					if (curr.getChild(kid).getText().equals(s)) {
						found = true;
						curr = curr.getChild(kid);
						break;
					}
				}
				if (!found) {
					curr = curr.addItem(s);
					curr.getParentItem().setState(true);
				}
				System.out.println("add selected: " + s);
			}
		} else if (event.getSource().equals(addButton)) {
			String name = Window.prompt("Tag name:", "");
			if (name == null) {
				return;
			}
			if (name.length() == 0) {
				return;
			}
			name = name.replaceAll("/", "-");
			TreeItem selected = mCurrentTagsTree.getSelectedItem();
			boolean found = false;
			// check for duplicates
			for (int kid = 0; kid < selected.getChildCount(); kid++) {
				if (selected.getChild(kid).getText().equals(name)) {
					found = true;
				}
			}
			if (!found) {
				selected.addItem(name);
				selected.setState(true);
			}
		} else if (event.getSource().equals(removeButton)) {
			TreeItem selected = mCurrentTagsTree.getSelectedItem();
			if (selected.equals(mCurrentTagsRoot)) {
				System.err.println("Cannot remove root");
			}
			mCurrentTagsTree.setSelectedItem(selected.getParentItem(), true);
			selected.getParentItem().removeItem(selected);
			selected.remove();
		} else if (event.getSource().equals(clearButton)) {
			this.mCurrentTagsTree.clear();
			mCurrentTagsRoot = mCurrentTagsTree.addItem("All tags");
		} else if (event.getSource().equals(updateButton)) {
			commit();
			hide();
		} else if (event.getSource().equals(cancelButton)) {
			hide();
		} else {
			super.onClick(event);
		}
	}
}
