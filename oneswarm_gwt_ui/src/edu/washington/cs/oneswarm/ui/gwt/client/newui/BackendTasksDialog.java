package edu.washington.cs.oneswarm.ui.gwt.client.newui;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.rpc.BackendTask;

public class BackendTasksDialog extends OneSwarmDialogBox {
	
	public static final int WIDTH = 500;
	
	public BackendTasksDialog( BackendTask [] inTasks ) {
		super();
		
		setText("Tasks");
		
		VerticalPanel mainPanel = new VerticalPanel();
		
		Label selectLabel = new Label("Cancel running tasks.");
		selectLabel.addStyleName(CSS_DIALOG_HEADER);
		selectLabel.setWidth(WIDTH + "px");
		mainPanel.add(selectLabel);
		mainPanel.setCellVerticalAlignment(selectLabel, VerticalPanel.ALIGN_TOP);
		mainPanel.setWidth(WIDTH+"px");
		
		for( BackendTask t : inTasks )
		{
			mainPanel.add(taskPanel(t));
		}
		
		Button hideButton = new Button("Hide");
		HorizontalPanel hideButtonHP = new HorizontalPanel();
		hideButtonHP.add(hideButton);
		mainPanel.add(hideButtonHP);
		mainPanel.setCellHorizontalAlignment(hideButtonHP, HorizontalPanel.ALIGN_RIGHT);
		hideButton.addClickListener(new ClickListener(){
			public void onClick(Widget sender) {
				hide();
			}});
		
		mainPanel.setWidth("100%");
		
		this.setWidget(mainPanel);
	}
	
	private VerticalPanel taskPanel( final BackendTask inTask )
	{
		final VerticalPanel outPanel = new VerticalPanel();
		
		outPanel.setWidth("100%");
		
		outPanel.add(new Label(inTask.getShortname()));
		outPanel.add(new Label(inTask.getSummary()));
		
		Button cancelButton = new Button("Cancel");
		cancelButton.addClickListener(new ClickListener(){
			public void onClick(Widget sender) {
				OneSwarmRPCClient.getService().cancelBackendTask(OneSwarmRPCClient.getSessionID(), inTask.getTaskID(), new AsyncCallback<Void>(){
					public void onFailure(Throwable caught) {
						caught.printStackTrace();
					}

					public void onSuccess(Void result) {
						System.out.println("cancelled");
						outPanel.removeFromParent();
					}});
			}});
		
		outPanel.add(cancelButton);
		outPanel.setSpacing(3);
		outPanel.setCellHorizontalAlignment(cancelButton, HorizontalPanel.ALIGN_LEFT);
		
		return outPanel;
	}
}
