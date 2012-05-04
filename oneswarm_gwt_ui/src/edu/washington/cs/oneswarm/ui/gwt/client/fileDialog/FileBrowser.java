package edu.washington.cs.oneswarm.ui.gwt.client.fileDialog;

import java.util.LinkedList;
import java.util.Queue;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasTreeItems;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FileInfo;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmUIServiceAsync;

public class FileBrowser {
	private String session;
	private boolean directoryOk;
	private AsyncCallback<String> callback;
	
	private OneSwarmUIServiceAsync fileSystem;
	private PopupPanel popup;

	private Queue<FileTreeItem> openItems;
	

	// Will be appended to directories
	static final String DIRECTORY_IDENTIFIER = " [...]";

	public FileBrowser(String session, boolean directoryOk, final AsyncCallback<String> callback) {
		this.session = session;
		this.callback = callback;
		this.directoryOk = directoryOk;
	}

	private void createPopup() {
		openItems = new LinkedList<FileTreeItem>();

		final Tree fileTree = new Tree();
		growTree(fileTree,"");
		
		fileTree.addSelectionHandler(new SelectionHandler<TreeItem>() {
			public void onSelection(SelectionEvent<TreeItem> event) {
				FileTreeItem item = (FileTreeItem) event.getSelectedItem();
				if (item.isDirectory()) {
					if (item.getChildCount() == 0)
						growTree(item);
				}
			}
		});

		fileTree.addOpenHandler(new OpenHandler<TreeItem>() {
			public void onOpen(OpenEvent<TreeItem> event) {
				FileTreeItem item = (FileTreeItem) event.getTarget();
				closeItems(item.filePath());
				openItems.add(item);
			}

		});

		Button selectButton = new Button("Select");
		selectButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				FileTreeItem item = (FileTreeItem) fileTree.getSelectedItem();
				if(item.fileStatus() == FileInfo.FileStatusFlag.NO_READ_PERMISSION)
					Window.alert("The selected file could not be found or does not have read permission. Please choose another file.");
				else if(!directoryOk && item.isDirectory())
					Window.alert("You have selected a directory. Please choose a file.");
				else{
					callback.onSuccess(item.filePath());
					popup.hide();
				}
			}
		});

		Button closeButton = new Button("Cancel");
		closeButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				callback.onFailure(new Exception("No file Selected"));
				popup.hide();
			}
		});

		HorizontalPanel footer = new HorizontalPanel();
		footer.setStylePrimaryName("nickmart-file-browser-footer");
		footer.add(closeButton);
		footer.add(selectButton);

		ScrollPanel scrollArea = new ScrollPanel(fileTree);
		footer.setStylePrimaryName("nickmart-file-browser-scrollArea");
		scrollArea.setHeight("400px");
		scrollArea.setWidth("450px");
		
		VerticalPanel contents = new VerticalPanel();
		contents.setStylePrimaryName("nickmart-file-browser-contents");
		contents.add(scrollArea);
		contents.add(footer);

		popup = new PopupPanel(false);
		popup.setStylePrimaryName("nickmart-file-browser-popup");
		popup.setStyleName("gwt-DialogBox", true);
		popup.setStyleName("Top", true);
		popup.setGlassEnabled(true);
		popup.setTitle("File Browser");
		popup.setWidget(contents);
	}

	private void closeItems(String exceptThis) {
		int size = openItems.size();
		for (int i = 0; i < size; i++) {
			FileTreeItem current = openItems.remove();
			if (!exceptThis.startsWith(current.filePath()))
				current.setState(false);
			else
				openItems.add(current);
		}
	}

	private void growTree(final FileTreeItem item){
		growTree(item, item.filePath());
		item.setState(true);
	}
	
	private void growTree(final HasTreeItems root, String filePath) {
		if (fileSystem == null) {
			fileSystem = OneSwarmRPCClient.getService();
		}

		fileSystem.listFiles(session, filePath, new AsyncCallback<FileInfo[]>() {
			public void onFailure(Throwable caught) {
				callback.onFailure(caught);
			}

			public void onSuccess(FileInfo[] result) {
				if (result != null) {
					for (int i = 0; i < result.length; i++) {
						FileTreeItem temp = new FileTreeItem(result[i]);
						if (temp.isDirectory())
							temp.setText(temp.fileName() + DIRECTORY_IDENTIFIER);
						root.addItem(temp);
					}
				}
			}
		});
	}

	public void show() {
		if (popup == null)
			createPopup();
		popup.center();
	}
}
