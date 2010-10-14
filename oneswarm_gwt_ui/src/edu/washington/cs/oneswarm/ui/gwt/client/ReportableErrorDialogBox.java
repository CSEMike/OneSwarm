package edu.washington.cs.oneswarm.ui.gwt.client;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmException;
import edu.washington.cs.oneswarm.ui.gwt.rpc.ReportableException;

public class ReportableErrorDialogBox extends OneSwarmDialogBox
{
	static final int WIDTH = 400;
	static final int HEIGHT = 200;
	
	private static OSMessages msg = OneSwarmGWT.msg;
	
	final Button dismissButton = new Button(msg.button_dismiss());
	final TextArea errorText = new TextArea();
	final VerticalPanel mainPanel = new VerticalPanel();
	
	public ReportableErrorDialogBox( final OneSwarmException inError, boolean fatal ) {
		this( inError.getMessage(), fatal );
	}
	
	public ReportableErrorDialogBox( final ReportableException inError, boolean fatal ) {
		this( inError.toString(), fatal );
	}
	
	public ReportableErrorDialogBox( final String inErrorString, boolean fatal ) { 
		this(inErrorString, fatal, true);
	}
	
	public ReportableErrorDialogBox( final String inErrorString, boolean fatal, boolean show_reporting )
	{
		super(false, true, !fatal);
		
		if( fatal ) {
			setText(msg.error_fatal_error());
		} else {
			setText(msg.error_error());
		}
		if( show_reporting == false ) { 
			setText("  "); // not clear this is an error
		}
		
		String mailto = "mailto:oneswarm@cs.washington.edu?subject=Bug report&body=" + inErrorString;
		
		mainPanel.setWidth(WIDTH + "px");
		
		if( show_reporting ) {
			mainPanel.add(new HTML(
				msg.error_report(mailto)
				));
			mainPanel.setHeight(HEIGHT + "px");
		} else { 
			mainPanel.setHeight("125px");
		}
		
		errorText.setWidth(WIDTH + "px");
		errorText.setVisibleLines(7);
		
		mainPanel.add(errorText);
		
		errorText.setText(inErrorString);
		
		HorizontalPanel status_and_button = new HorizontalPanel();
		status_and_button.setWidth("100%");
		final Label statusLabel = new Label("");
		status_and_button.add(statusLabel);
		status_and_button.add(dismissButton);
		status_and_button.setSpacing(3);
		
		status_and_button.setCellHorizontalAlignment(dismissButton, HorizontalPanel.ALIGN_RIGHT);
		status_and_button.setCellHorizontalAlignment(statusLabel, HorizontalPanel.ALIGN_LEFT);
		
		mainPanel.add(status_and_button);
		dismissButton.addClickListener(new ClickListener() {
			public void onClick( Widget sender ) {
//				statusLabel.setText("Sending report...");
//				((Button)sender).setEnabled(false);
//				
//				OneSwarmRPCClient.getService().reportError(inError, new AsyncCallback() {
//					public void onFailure( Throwable caught ) 
//					{
//						statusLabel.setText("Error sending report!");
//					}
//
//					public void onSuccess( Object result ) 
//					{
//						if( result == null )
//							statusLabel.setText("Report received. Thank you!");
//						else
//							onFailure(null);
//					}
				hide();
//				}); // reportError RPC
			} // report button onClick()
		}); // add click listener
		
		setWidget(mainPanel);
		
		show();
		setVisible(false);
		center();
		setVisible(true);
	}
}
