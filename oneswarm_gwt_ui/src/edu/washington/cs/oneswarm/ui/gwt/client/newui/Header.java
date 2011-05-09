package edu.washington.cs.oneswarm.ui.gwt.client.newui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;

public class Header extends HorizontalPanel {
    private boolean showDebug = false;
    private NavigationFilterBar navBar;

    public Header(boolean useDebug, boolean focusSearchText) {
        setWidth("100%");

        this.setVerticalAlignment(VerticalPanel.ALIGN_BOTTOM);
        this.setHorizontalAlignment(HorizontalPanel.ALIGN_LEFT);

        final Image headerImage = new Image(ImageConstants.HEADER_LOGO);
        if (useDebug) {
            headerImage.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    showDebug = !showDebug;
                    OneSwarmGWT.showDebug(showDebug);
                }
            });
        }

        add(headerImage);
        this.setCellVerticalAlignment(headerImage, VerticalPanel.ALIGN_BOTTOM);

        this.setCellWidth(headerImage, "200px");
        this.setCellHeight(headerImage, "45px");

        navBar = new NavigationFilterBar(focusSearchText);
        add(navBar);
        this.setCellVerticalAlignment(navBar, VerticalPanel.ALIGN_BOTTOM);
    }

    public NavigationFilterBar getFilterBar() {
        return navBar;
    }

    public void focusSearch() {
        navBar.focusSearch();
    }
}
