package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;

public class MagicTypeSelectionDialog extends OneSwarmDialogBox {

    private static final int WIDTH = 400;
    private static final int HEIGHT = 300;

    interface Callback {
        public void done(boolean cancelled, MagicWatchType which);
    };

    Button okButton = new Button("Save");
    Button cancelButton = new Button("Cancel");

    RadioButton magic = new RadioButton("magicType", "Grouped media files only");
    RadioButton everything = new RadioButton("magicType", "Everything");

    Callback callback = null;

    public MagicTypeSelectionDialog(MagicWatchType initial, Callback callback) {
        super(false, true, false);

        setText("Watch directory type");

        this.callback = callback;

        VerticalPanel panel = new VerticalPanel();

        panel.setWidth(WIDTH + "px");
        panel.setWidth(HEIGHT + "px");

        panel.add(everything);
        panel.add(magic);

        if (initial.equals(MagicWatchType.Everything)) {
            everything.setValue(true);
        } else {
            magic.setValue(true);
        }

        HorizontalPanel buttons = new HorizontalPanel();
        buttons.add(cancelButton);
        buttons.add(okButton);
        buttons.setSpacing(3);

        okButton.addClickHandler(this);
        cancelButton.addClickHandler(this);

        panel.add(buttons);

        panel.setCellHorizontalAlignment(buttons, HorizontalPanel.ALIGN_RIGHT);

        this.setWidget(panel);
    }

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(okButton)) {
            callback.done(false, everything.getValue() ? MagicWatchType.Everything
                    : MagicWatchType.Magic);
            hide();
        } else if (event.getSource().equals(cancelButton)) {
            callback.done(true, null);
            hide();
        } else {
            super.onClick(event);
        }
    }
}
