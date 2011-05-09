package edu.washington.cs.oneswarm.ui.gwt.client.newui.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.DOM;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;

public class ResizablePanel extends VerticalPanel {
    private Element panelElement;
    private boolean moveIt = false;
    private List<ResizablePanelListener> listeners = new ArrayList<ResizablePanelListener>();
    private boolean dragAndDrop = false;

    public ResizablePanel() {
        super();

        DOM.sinkEvents(this.getElement(), Event.ONMOUSEDOWN | Event.ONMOUSEMOVE | Event.ONMOUSEUP
                | Event.ONMOUSEOVER);
    }

    @Override
    public void onBrowserEvent(Event event) {

        final int eventType = DOM.eventGetType(event);

        if (Event.ONMOUSEOVER == eventType) {

            if (isCursorResize(event)) {
                DOM.setStyleAttribute(this.getElement(), "cursor", "se-resize");
            } else if (isCursorMove(event)) {
                DOM.setStyleAttribute(this.getElement(), "cursor", "se-resize");
            } else {
                DOM.setStyleAttribute(this.getElement(), "cursor", "default");
            }
        }
        if (Event.ONMOUSEDOWN == eventType) {
            if (isCursorResize(event)) {
                if (dragAndDrop == false) {
                    dragAndDrop = true;

                    event.preventDefault();

                    DOM.setCapture(this.getElement());
                }
            } else if (isCursorMove(event)) {
                DOM.setCapture(this.getElement());
                moveIt = true;

                event.preventDefault();

            }
        } else if (Event.ONMOUSEMOVE == eventType) {

            if (!isCursorResize(event) && !isCursorMove(event)) {
                DOM.setStyleAttribute(this.getElement(), "cursor", "default");
            }

            if (dragAndDrop == true) {
                int absX = DOM.eventGetClientX(event);
                int absY = DOM.eventGetClientY(event);
                int originalX = DOM.getAbsoluteLeft(this.getElement());
                int originalY = DOM.getAbsoluteTop(this.getElement());

                if (absY > originalY && absX > originalX) {

                    Integer height = absY - originalY + 2;
                    this.setHeight(height + "px");

                    Integer width = absX - originalX + 2;
                    this.setWidth(width + "px");
                    notify(width, height);
                }
            } else if (moveIt == true) {
                RootPanel.get().setWidgetPosition(this, DOM.eventGetClientX(event),
                        DOM.eventGetClientY(event));
            }
        } else if (Event.ONMOUSEUP == eventType) {
            if (moveIt == true) {
                moveIt = false;
                DOM.releaseCapture(this.getElement());
            }
            if (dragAndDrop == true) {
                dragAndDrop = false;
                DOM.releaseCapture(this.getElement());
            }
        }
    }

    protected boolean isCursorResize(Event event) {
        int cursorY = DOM.eventGetClientY(event);
        int initialY = this.getAbsoluteTop();
        int height = this.getOffsetHeight();

        int cursorX = DOM.eventGetClientX(event);
        int initialX = this.getAbsoluteLeft();
        int width = this.getOffsetWidth();

        if (((initialX + width - 10) < cursorX && cursorX <= (initialX + width))
                && ((initialY + height - 10) < cursorY && cursorY <= (initialY + height)))
            return true;
        else
            return false;
    }

    public void setMovingPanelElement(Element movingPanelElement) {
        this.panelElement = movingPanelElement;
    }

    protected boolean isCursorMove(Event event) {
        if (panelElement != null) {
            int cursorY = DOM.eventGetClientY(event);
            int initialY = panelElement.getAbsoluteTop();
            int cursorX = DOM.eventGetClientX(event);
            int initialX = panelElement.getAbsoluteLeft();

            if (initialY <= cursorY && initialX <= cursorX)
                return true;
            else
                return false;
        } else
            return false;
    }

    public void addResizeListener(ResizablePanelListener listener) {
        listeners.add(listener);
    }

    private void notify(int width, int height) {

        for (ResizablePanelListener i : listeners) {
            i.resized(width, height);
        }
    }
}