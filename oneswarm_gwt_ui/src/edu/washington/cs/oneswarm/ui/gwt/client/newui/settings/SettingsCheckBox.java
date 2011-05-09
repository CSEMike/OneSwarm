/**
 * 
 */
package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;

class SettingsCheckBox extends CheckBox {
    private final String[] azCoreSettingString;

    public SettingsCheckBox(String text, String[] azCoreSettingStrings) {
        super(text);
        super.setEnabled(false);
        this.azCoreSettingString = azCoreSettingStrings;
        if (azCoreSettingString == null) {
            throw new RuntimeException("null settings not allowed");
        }
        if (azCoreSettingString.length < 1) {
            throw new RuntimeException("at least 1 setting must be specified");
        }

        OneSwarmRPCClient.getService().getBooleanParameterValue(OneSwarmRPCClient.getSessionID(),
                azCoreSettingString[0], new AsyncCallback<Boolean>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    public void onSuccess(Boolean result) {
                        setEnabled(true);
                        setValue(result);
                    }
                });
    }

    public SettingsCheckBox(String text, String azCoreSettingString) {
        this(text, new String[] { azCoreSettingString });
    }

    public void save() {
        if (isEnabled()) {
            for (String setting : azCoreSettingString) {
                System.out.println("setting " + setting + " to " + getValue());
                OneSwarmRPCClient.getService().setBooleanParameterValue(
                        OneSwarmRPCClient.getSessionID(), setting, getValue(),
                        new AsyncCallback<Void>() {
                            public void onFailure(Throwable caught) {
                                caught.printStackTrace();
                            }

                            public void onSuccess(Void result) {

                            }
                        });
            }
        }
    }
}