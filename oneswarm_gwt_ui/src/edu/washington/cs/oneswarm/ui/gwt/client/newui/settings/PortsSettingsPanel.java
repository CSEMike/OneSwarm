package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;

import java.util.HashMap;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.Updateable;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;

class PortsSettingsPanel extends SettingsPanel implements Updateable {
	private static final String PORT_STATUS = msg.settings_net_tcp_nat_test_status();
	TextBox tcp_port_box = new TextBox();
	TextBox udp_port_box = new TextBox();
	Label natStatusLabel = new Label();
	final Button natCheck = new Button(msg.settings_net_tcp_nat_test_button());

	public PortsSettingsPanel() {
		super();

		tcp_port_box.setText("...");
		udp_port_box.setText("...");

		Grid g = new Grid(2, 5);

		Label l = new Label(msg.settings_net_tcp_port());
		g.setWidget(0, 0, l);
		g.setWidget(0, 1, tcp_port_box);

		Button randomize_tcp = new Button(msg.button_randomize());
		randomize_tcp.addStyleName(OneSwarmCss.SMALL_BUTTON);
		randomize_tcp.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				tcp_port_box.setText(Integer.toString(Random.nextInt(65534 - 1025) + 1025));
			}
		});
		g.setWidget(0, 2, randomize_tcp);

		g.setWidget(0, 4, natStatusLabel);

		g.setWidget(0, 3, natCheck);
		natCheck.addStyleName(OneSwarmCss.SMALL_BUTTON);
		natCheck.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				/*
				 * setting the tcp port first...
				 */
				natCheck.setEnabled(false);
				OneSwarmRPCClient.getService().setIntegerParameterValue(OneSwarmRPCClient.getSessionID(), "TCP.Listen.Port", Integer.parseInt(tcp_port_box.getText()), new AsyncCallback<Void>() {
					public void onFailure(Throwable caught) {
						caught.printStackTrace();
					}

					public void onSuccess(Void result) {
						/*
						 * trigger the check
						 */
						OneSwarmRPCClient.getService().triggerNatCheck(OneSwarmRPCClient.getSessionID(), new AsyncCallback<Void>() {
							public void onFailure(Throwable caught) {
								caught.printStackTrace();
							}

							public void onSuccess(Void result) {
								OneSwarmGWT.addToUpdateTask(PortsSettingsPanel.this);
							}
						});
					}
				});

			}
		});

		Button randomize_udp = new Button(msg.button_randomize());
		randomize_udp.addStyleName(OneSwarmCss.SMALL_BUTTON);
		randomize_udp.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				udp_port_box.setText(Integer.toString(Random.nextInt(65534 - 1025) + 1025));
			}
		});
		g.setWidget(1, 2, randomize_udp);

		tcp_port_box.setWidth("75px");

		if (OneSwarmGWT.isRemoteAccess()) {
			randomize_tcp.setEnabled(false);
			tcp_port_box.setEnabled(false);
		}

		l = new Label(msg.settings_net_udp_port());
		g.setWidget(1, 0, l);
		g.setWidget(1, 1, udp_port_box);

		udp_port_box.setWidth("75px");

		this.add(g);
		this.setWidth("100%");

		OneSwarmRPCClient.getService().getIntegerParameterValue(OneSwarmRPCClient.getSessionID(), "TCP.Listen.Port", new AsyncCallback<Integer>() {
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}

			public void onSuccess(Integer result) {
				tcp_port_box.setText(result.toString());
				if (udp_port_box.getText().equals("...") == false) {
					loadNotify();
				}
			}
		});

		OneSwarmRPCClient.getService().getIntegerParameterValue(OneSwarmRPCClient.getSessionID(), "UDP.Listen.Port", new AsyncCallback<Integer>() {
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}

			public void onSuccess(Integer result) {
				udp_port_box.setText(result.toString());
				if (tcp_port_box.getText().equals("...") == false) {
					loadNotify();
				}
			}
		});
		update(0);
	}

	public void sync() {
		System.out.println("trying to sync udp/tcp ports");
		OneSwarmRPCClient.getService().setIntegerParameterValue(OneSwarmRPCClient.getSessionID(), "UDP.Listen.Port", Integer.parseInt(udp_port_box.getText()), new AsyncCallback<Void>() {
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}

			public void onSuccess(Void result) {
				System.out.println("success for udp sync");
			}
		});
		OneSwarmRPCClient.getService().setIntegerParameterValue(OneSwarmRPCClient.getSessionID(), "TCP.Listen.Port", Integer.parseInt(tcp_port_box.getText()), new AsyncCallback<Void>() {
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}

			public void onSuccess(Void result) {
				System.out.println("success for tcp sync");
			}
		});

	}

	final String RANGE = msg.settings_net_port_range_error();

	String validData() {
		try {
			int tcp = Integer.parseInt(tcp_port_box.getText());
			int udp = Integer.parseInt(udp_port_box.getText());

			// if( tcp < 1025 || tcp > 65534 || udp < 1024 || udp > 65534 )
			if (tcp < 0 || tcp > 65534 || udp < 0 || udp > 65534) {
				throw new Exception(RANGE);
			}
		} catch (NumberFormatException e) {
			return RANGE;
		} catch (Exception e) {
			return e.toString();
		}
		return null;
	}

	protected void onDetach() {
		OneSwarmGWT.removeFromUpdateTask(this);
		super.onDetach();
	}

	public void update(int count) {
		OneSwarmRPCClient.getService().getNatCheckResult(OneSwarmRPCClient.getSessionID(), new AsyncCallback<HashMap<String, String>>() {
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}

			public void onSuccess(HashMap<String, String> result) {
				String status = result.get("status");

				if (status == null) {
					natCheck.setEnabled(true);
					natStatusLabel.setText("");
				} else if ("1".equals(status)) {
					natCheck.setEnabled(false);
					natStatusLabel.setText(PORT_STATUS + msg.settings_net_tcp_nat_test_status_ok());
					natStatusLabel.setTitle(msg.settings_net_tcp_nat_test_status_ok_title(result.get("ip") + ":" + result.get("port")));
					OneSwarmGWT.removeFromUpdateTask(PortsSettingsPanel.this);
				} else if ("-1".equals(status)) {
					natCheck.setEnabled(true);
					natStatusLabel.setText(PORT_STATUS + msg.settings_net_tcp_nat_test_status_failed());
					OneSwarmGWT.removeFromUpdateTask(PortsSettingsPanel.this);
				} else if ("0".equals(status)) {
					natCheck.setEnabled(false);
					natStatusLabel.setText(PORT_STATUS + msg.settings_net_tcp_nat_test_status_waiting());
				}
			}
		});
	}
}