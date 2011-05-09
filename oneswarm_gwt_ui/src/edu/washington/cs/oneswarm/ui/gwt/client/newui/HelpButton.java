/**
 * 
 */
package edu.washington.cs.oneswarm.ui.gwt.client.newui;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

public class HelpButton extends HorizontalPanel implements ClickListener {
    String helpHtml;
    final Image icon = new Image(ImageConstants.ICON_HELP_BUTTON);
    private HelpButton.HelpPanel helpPanel;

    public HelpButton(String text) {
        this.helpHtml = text;

        HTML html = new HTML(text);
        icon.setTitle("Help: " + html.getText());
        super.add(icon);
        super.setCellVerticalAlignment(icon, HorizontalPanel.ALIGN_MIDDLE);
        icon.addClickListener(this);
    }

    public void onClick(Widget sender) {
        /*
         * if this is a second click, and we already show the help, hide it
         */
        if (helpPanel != null) {
            helpPanel.hide();
            helpPanel = null;
        } else {
            System.out.println("creating help panel with: " + helpHtml);
            helpPanel = new HelpPanel(helpHtml, sender);
            helpPanel.show();
        }
    }

    public void setTitle(String text) {
        icon.setTitle(text);
        helpPanel = null;
    }

    public void setText(String text) {
        helpHtml = text;
        setTitle(text);
    }

    static class HelpPanel extends PopupPanel implements ClickListener {

        private final Widget parent;
        private final String parentText;
        private HTML mHTML;

        public HelpPanel(String helpHTML, Widget parent) {
            super(true, false);
            this.parentText = parent.getTitle();
            this.parent = parent;
            setPopupPosition(parent.getAbsoluteLeft(), Math.max(0, parent.getAbsoluteTop() - 30));
            addStyleName("os-HelpBox");

            mHTML = new HTML(helpHTML);
            mHTML.addClickListener(this);
            add(mHTML);
        }

        public void onBrowserEven(Event e) {
            hide();
        }

        public void show() {
            parent.setTitle("");
            super.show();
        }

        protected void onDetach() {
            super.onDetach();
            parent.setTitle(parentText);
        }

        public void onClick(Widget sender) {
            hide();
        }
    }
}