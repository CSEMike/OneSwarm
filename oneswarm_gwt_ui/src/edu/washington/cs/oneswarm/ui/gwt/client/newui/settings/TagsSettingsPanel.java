package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.HelpButton;

public class TagsSettingsPanel extends SettingsPanel {
    private final List<SettingsCheckBox> checkBoxes = new ArrayList<SettingsCheckBox>();

    private final TextBox mMaxVisibleTags = new TextBox();

    TagsSettingsPanel() {

        Grid g = new Grid(4, 1);

        final SettingsCheckBox showTagsCheckbox = new SettingsCheckBox(
                msg.settings_interface_tags_show(), "oneswarm.show.tags");
        checkBoxes.add(showTagsCheckbox);

        final SettingsCheckBox id3Checkbox = new SettingsCheckBox(
                msg.settings_interface_tags_auto_audio(), "oneswarm.add.id3.tags");
        checkBoxes.add(id3Checkbox);

        final SettingsCheckBox directoryCheckbox = new SettingsCheckBox(
                msg.settings_interface_tags_auto_path(), "oneswarm.directory.tags");
        checkBoxes.add(directoryCheckbox);

        for (int row = 0; row < checkBoxes.size(); row++) {
            g.setWidget(row, 0, checkBoxes.get(row));
        }

        OneSwarmRPCClient.getService().getIntegerParameterValue(OneSwarmRPCClient.getSessionID(),
                "oneswarm.max.ui.tags", new AsyncCallback<Integer>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    public void onSuccess(Integer result) {
                        mMaxVisibleTags.setText(result.toString());
                        mMaxVisibleTags.setEnabled(true);
                        loadNotify();

                    }
                });

        mMaxVisibleTags.setEnabled(false);

        mMaxVisibleTags.addKeyPressHandler(new KeyPressHandler() {
            public void onKeyPress(KeyPressEvent event) {
                char keyCode = event.getCharCode();
                if ((!Character.isDigit(keyCode)) && (keyCode != (char) KeyCodes.KEY_TAB)
                        && (keyCode != (char) KeyCodes.KEY_BACKSPACE)
                        && (keyCode != (char) KeyCodes.KEY_DELETE)
                        && (keyCode != (char) KeyCodes.KEY_ENTER)
                        && (keyCode != (char) KeyCodes.KEY_HOME)
                        && (keyCode != (char) KeyCodes.KEY_END)
                        && (keyCode != (char) KeyCodes.KEY_LEFT)
                        && (keyCode != (char) KeyCodes.KEY_UP)
                        && (keyCode != (char) KeyCodes.KEY_RIGHT)
                        && (keyCode != (char) KeyCodes.KEY_DOWN)) {
                    // TextBox.cancelKey() suppresses the current keyboard
                    // event.
                    mMaxVisibleTags.cancelKey();
                }
            }
        });

        HorizontalPanel hp = new HorizontalPanel();
        Label l = new Label(msg.settings_interface_tags_max_display());
        hp.add(l);
        hp.add(mMaxVisibleTags);
        HelpButton hb = new HelpButton(msg.settings_interface_tags_max_display_help());
        hp.add(hb);

        hp.setCellVerticalAlignment(l, VerticalPanel.ALIGN_MIDDLE);
        hp.setCellVerticalAlignment(mMaxVisibleTags, VerticalPanel.ALIGN_MIDDLE);
        hp.setCellVerticalAlignment(hb, VerticalPanel.ALIGN_MIDDLE);

        g.setWidget(3, 0, hp);

        super.add(g);
    }

    public void sync() {
        for (SettingsCheckBox c : checkBoxes) {
            c.save();
        }

        int maxTags = 300;
        try {
            maxTags = Integer.parseInt(mMaxVisibleTags.getText());
        } catch (Exception e) {
            e.printStackTrace();
        }

        OneSwarmRPCClient.getService().setIntegerParameterValue(OneSwarmRPCClient.getSessionID(),
                "oneswarm.max.ui.tags", maxTags, new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    public void onSuccess(Void result) {
                        ; // success
                    }
                });
    }

    public String validData() {
        return null; // checkboxes are always valid
    }
}
