package edu.washington.cs.oneswarm.ui.gwt.client.newui;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;

public class LabelWithHelp extends HorizontalPanel {
    private final HTML label;
    private final HelpButton helpButton;

    public LabelWithHelp(String labelText, String helpText) {
        this(labelText, helpText, false);
    }

    public LabelWithHelp(String labelText, String helpText, boolean hasHTML) {
        if (hasHTML) {
            this.label = new HTML(labelText);
        } else {
            this.label = new HTML("");
            this.label.setText(labelText);
        }
        this.helpButton = new HelpButton(helpText);
        super.setSpacing(3);
        super.add(label);
        super.add(helpButton);
    }

    public void setText(String text) {
        this.label.setText(text);
    }

    public void setHTML(String html) {
        this.label.setHTML(html);
    }

}
