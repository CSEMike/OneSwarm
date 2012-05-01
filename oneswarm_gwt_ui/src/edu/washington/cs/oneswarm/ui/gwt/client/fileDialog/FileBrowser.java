package edu.washington.cs.oneswarm.ui.gwt.client.fileDialog;

import java.util.LinkedList;
import java.util.Queue;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasTreeItems;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.ReportableErrorDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FileInfo;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmUIServiceAsync;

public class FileBrowser {
	private String session;
	private boolean directoryOk;
	private AsyncCallback<String> callback;
	protected static OSMessages msg;
	
	private OneSwarmUIServiceAsync fileSystem;
	private PopupPanel popup;

	private Queue<FileTreeItem> openItems;
	

	// Will be appended to directories for display
	static final String DIRECTORY_IDENTIFIER = " [...]";

	public FileBrowser(String session, boolean directoryOk, final AsyncCallback<String> callback) {
		this.session = session;
		this.callback = callback;
		this.directoryOk = directoryOk;
		msg = OneSwarmGWT.msg;
	}

	private void createPopup() {
		openItems = new LinkedList<FileTreeItem>();

		final Tree fileTree = new Tree();
		growTree(fileTree,"");

		fileTree.addOpenHandler(new OpenHandler<TreeItem>() {
			public void onOpen(OpenEvent<TreeItem> event) {
				FileTreeItem item = (FileTreeItem) event.getTarget();
				if (item.isDirectory()) {
					if (!item.hasBeenExpanded){
						growTree(item);
					}
				}
				closeItems(item.filePath());
				openItems.add(item);
			}

		});

		Button selectButton = new Button(msg.file_browser_button_select());
		selectButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				FileTreeItem item = (FileTreeItem) fileTree.getSelectedItem();
				if(item.fileStatus() == FileInfo.FileStatusFlag.NO_READ_PERMISSION)
					new ReportableErrorDialogBox(msg.file_browser_error_permission_denied(),false).show();
				else if(!directoryOk && item.isDirectory())
					new ReportableErrorDialogBox(msg.file_browser_error_directory_selected(),false).show();
				else{
					callback.onSuccess(item.filePath());
					popup.hide();
				}
			}
		});

		Button closeButton = new Button(msg.file_browser_button_cancel());
		closeButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				callback.onFailure(new Exception("No file Selected"));
				popup.hide();
			}
		});

		HorizontalPanel footer = new HorizontalPanel();
		footer.add(closeButton);
		footer.add(selectButton);

		ScrollPanel scrollArea = new ScrollPanel(fileTree);
		scrollArea.setHeight("400px");
		scrollArea.setWidth("450px");
		
		VerticalPanel contents = new VerticalPanel();
		contents.add(scrollArea);
		contents.add(footer);

		popup = new PopupPanel(false);
		popup.setStylePrimaryName("fileBrowserPopup");
		popup.setStyleName("gwt-DialogBox", true);
		popup.setStyleName("Top", true);
		popup.setGlassEnabled(true);
		popup.setTitle(msg.file_browser_title());
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
		item.hasBeenExpanded = true;
	}
	
	private void growTree(final HasTreeItems root, String filePath) {
		if (fileSystem == null) {
			fileSystem = OneSwarmRPCClient.getService();
		}
		
		root.removeItems();
		
		if(root instanceof FileTreeItem)
			if(((FileTreeItem) root).fileStatus() == FileInfo.FileStatusFlag.NO_READ_PERMISSION){
				root.addItem(new FileTreeItem(msg.file_browser_label_unreadable_directory()));
				((FileTreeItem) root).setState(true);
			}
		
		fileSystem.listFiles(session, filePath, new AsyncCallback<FileInfo[]>() {
			public void onFailure(Throwable caught) {
				callback.onFailure(caught);
			}

			public void onSuccess(FileInfo[] result) {
				if (result != null) {
					if(result.length == 0)
						root.addItem(new FileTreeItem(msg.file_browser_label_empty_directory()));
					else{
						for (int i = 0; i < result.length; i++) {
							FileTreeItem temp = new FileTreeItem(result[i]);
							if (temp.isDirectory())
								temp.addItem(new FileTreeItem(msg.file_browser_label_loading_directory()));
							root.addItem(temp);
							
						}
					}
					if(root instanceof FileTreeItem)
						((FileTreeItem) root).setState(true);
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
