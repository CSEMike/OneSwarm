package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.widgetideas.client.ProgressBar;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard.FriendsImportWizard;
import edu.washington.cs.oneswarm.ui.gwt.rpc.BackendTask;
import edu.washington.cs.oneswarm.ui.gwt.rpc.SpeedTestResult;

public class SpeedTestPanel extends VerticalPanel {
	protected final static OSMessages msg = OneSwarmGWT.msg;

	private final Timer updateTimer;
	private BackendTask task;

	private double getRemoteEstimate() {
		if (task != null) {
			return getSpeedTestResult().getRemoteEstimate();
		} else {
			return 1000;
		}
	}

	private SpeedTestResult getSpeedTestResult() {
		if (task != null) {
			return ((SpeedTestResult) task.getResult());
		}
		return null;
	}

	public static final int WIDTH = 270;
	private final ListBox fractionSelection = new ListBox();
	private final TextBox uploadLimit = new TextBox();
	private final TextBox uploadRate = new TextBox();
	private final Label uploadRateLabel = new Label();
	private double currentUpload = 0;
	private int uploadLimitCalc = 0;

	private void updateUpload() {
		int rateInKb = (int) Math.round(currentUpload / 1000);
		uploadRate.setText(rateInKb + "");
		uploadRateLabel.setText(rateInKb + "");
		double frac = Double.parseDouble(fractionSelection.getValue(fractionSelection.getSelectedIndex()));
		uploadLimitCalc = (int) Math.round(currentUpload * frac);
		uploadLimit.setText((uploadLimitCalc / 1000) + "");
	}

	public SpeedTestPanel(final SpeedTestCallback cb) {

		super.setWidth(WIDTH + "px");

		Label selectLabel = new HTML(msg.speed_test_description());
		selectLabel.addStyleName(FriendsImportWizard.CSS_DIALOG_HEADER);
		selectLabel.setWidth(WIDTH + "px");
		super.add(selectLabel);
		super.setSpacing(3);

		// AbsolutePanel imagePanel = createSpeedometer();
		// super.add(imagePanel);
		// super.setCellHorizontalAlignment(imagePanel, ALIGN_CENTER);

		for (int i = 50; i <= 100; i += 10) {
			fractionSelection.addItem(i + "%", (i / 100.0) + "");
		}
		fractionSelection.setSelectedIndex(3);
		fractionSelection.addChangeHandler(new ChangeHandler() {
			public void onChange(ChangeEvent event) {
				updateUpload();
			}
		});

		Grid g = new Grid(3, 2);
		final String rightColWidth = "60px";

		g.setWidget(0, 0, new Label(msg.speed_test_measured_capacity()));
		g.setWidget(0, 1, uploadRate);
		g.getCellFormatter().setHorizontalAlignment(0, 1, ALIGN_CENTER);
		uploadRate.setWidth(rightColWidth);
		uploadRate.setReadOnly(true);
		uploadRate.addStyleName(OneSwarmCss.TEXT_BLACK);
		uploadRate.addStyleName(OneSwarmCss.TEXT_BOLD);

		g.setWidget(1, 0, new Label(msg.speed_test_fraction_used()));
		g.setWidget(1, 1, fractionSelection);
		g.getCellFormatter().setHorizontalAlignment(1, 1, ALIGN_CENTER);
		fractionSelection.setWidth(rightColWidth);

		g.setWidget(2, 0, new Label(msg.speed_test_calculated_limit()));
		g.setWidget(2, 1, uploadLimit);
		g.getCellFormatter().setHorizontalAlignment(2, 1, ALIGN_CENTER);
		uploadLimit.setWidth(rightColWidth);
		uploadLimit.setReadOnly(true);
		uploadLimit.addStyleName(OneSwarmCss.TEXT_BLACK);
		super.add(g);
		super.setCellHorizontalAlignment(g, ALIGN_CENTER);

		final ProgressBar progress = new ProgressBar(0, 1);
		progress.setProgress(0);
		progress.setWidth("100%");
		super.add(progress);
		super.setCellHeight(progress, "30px");
		super.setCellVerticalAlignment(progress, ALIGN_MIDDLE);

		HorizontalPanel buttonPanel = new HorizontalPanel();
		buttonPanel.setSpacing(3);
		buttonPanel.setWidth("100%");
		final Button okButton = new Button(msg.speed_test_button_set_limit());
		okButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				cb.speedTestCompleted(uploadLimitCalc);
			}
		});

		Button cancelButton = new Button(msg.button_cancel());
		cancelButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				cb.speedTestCanceled();
			}
		});
		buttonPanel.add(cancelButton);
		buttonPanel.setCellHorizontalAlignment(cancelButton, ALIGN_LEFT);

		buttonPanel.add(okButton);
		buttonPanel.setCellHorizontalAlignment(okButton, ALIGN_RIGHT);

		super.add(buttonPanel);
		updateTimer = new Timer() {
			@Override
			public void run() {
				OneSwarmRPCClient.getService().getBackendTask(OneSwarmRPCClient.getSessionID(), task.getTaskID(), new AsyncCallback<BackendTask>() {
					public void onSuccess(BackendTask result) {
						if (result == null) {
							return;
						}
						task = result;

						SpeedTestResult testResult = getSpeedTestResult();
						if (testResult == null) {
							return;
						}

						progress.setProgress(testResult.getProgress());
						currentUpload = testResult.getEstimatedUploadRate();
						updateUpload();

						/*
						 * schedule the next update
						 */
						if (testResult.getProgress() < 1) {
							updateTimer.schedule(200);
						}
					}

					public void onFailure(Throwable caught) {
						okButton.setText("error");
					}
				});

			}
		};

		OneSwarmRPCClient.getService().performSpeedCheck(OneSwarmRPCClient.getSessionID(), 0, new AsyncCallback<BackendTask>() {
			public void onSuccess(BackendTask result) {
				task = result;
				updateTimer.schedule(200);
			}

			public void onFailure(Throwable caught) {
				okButton.setText("error");
			}
		});

		updateUpload();

	}

	// private AbsolutePanel createSpeedometer() {
	// AbsolutePanel imagePanel = new AbsolutePanel();
	// int panelSize = 225;
	// int imageSize = 175;
	// imagePanel.setPixelSize(panelSize, panelSize);
	// Image speedImage = new Image("images/speedometer.png");
	// int imageOffset = (panelSize - imageSize) / 2;
	// imagePanel.add(speedImage, imageOffset, imageOffset);
	//
	// uploadRateLabel.setWidth(imageSize + "px");
	// uploadRateLabel.setHorizontalAlignment(ALIGN_CENTER);
	// uploadRateLabel.addStyleName("os-welcome_banner");
	// imagePanel.add(uploadRateLabel, imageOffset, imageOffset + (imageSize /
	// 2) + 25);
	// Label kbpsLabel = new Label("KB/s");
	// kbpsLabel.setHorizontalAlignment(ALIGN_CENTER);
	// kbpsLabel.addStyleName(OneSwarmCss.TEXT_BOLD);
	// imagePanel.add(kbpsLabel, imageOffset, imageOffset + (imageSize / 2) +
	// 50);
	//		
	// return imagePanel;
	// }

	public interface SpeedTestCallback {
		public void speedTestCanceled();

		public void speedTestCompleted(double rate);
	}
}
