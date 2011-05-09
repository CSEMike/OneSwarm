package edu.washington.cs.oneswarm.ui.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.MouseListener;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;

/**
 * A form of popup that has a caption area at the top and can be dragged by the
 * user.
 * <p>
 * <img class='gallery' src='DialogBox.png'/>
 * </p>
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-DialogBox { the outside of the dialog }</li>
 * <li>.gwt-DialogBox .Caption { the caption }</li>
 * </ul>
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.DialogBoxExample}
 * </p>
 */
public class OneSwarmDialogBox extends PopupPanel implements HasHTML, ClickHandler, MouseListener {

    public static final String CSS_DIALOG_HEADER = "os-dialog_header";

    public final static String CLOSE_IMAGE_URL = GWT.getModuleBaseURL() + "images/close.png";
    protected static OSMessages msg = OneSwarmGWT.msg;

    private Image closeImage = null;

    private HTML caption = new HTML();

    private Widget child;

    private boolean dragging;

    private int dragStartX, dragStartY;

    private FlexTable panel = new FlexTable();

    /**
     * Creates an empty dialog box. It should not be shown until its child
     * widget has been added using {@link #add(Widget)} This is the same as
     * OneSwarmDialogBox(false,false,true) .
     */
    public OneSwarmDialogBox() {
        this(false, false, true);
    }

    /**
     * Creates an empty dialog box specifying its "auto-hide" property. It
     * should not be shown until its child widget has been added using
     * {@link #add(Widget)}.
     * 
     * @param autoHide
     *            <code>true</code> if the dialog should be automatically hidden
     *            when the user clicks outside of it
     */
    public OneSwarmDialogBox(boolean autoHide) {
        this(autoHide, false, true);
    }

    /**
     * Creates an empty dialog box specifying its "auto-hide" property. It
     * should not be shown until its child widget has been added using
     * {@link #add(Widget)}.
     * 
     * @param autoHide
     *            <code>true</code> if the dialog should be automatically hidden
     *            when the user clicks outside of it
     * @param modal
     *            <code>true</code> if keyboard and mouse events for widgets not
     *            contained by the dialog should be ignored
     * @param canClose
     *            <code>true</code> if this dialog box is close-able
     */
    public OneSwarmDialogBox(boolean autoHide, boolean modal, boolean canClose) {
        super(autoHide, modal);

        // create a new panel, so we can fit the close sign
        FlexTable captionPanel = new FlexTable();
        captionPanel.setWidget(0, 0, caption);
        captionPanel.setBorderWidth(0);
        captionPanel.setCellSpacing(0);
        captionPanel.setCellPadding(0);
        captionPanel.setWidth("100%");
        // captionPanel.setStyleName("Caption");
        // add the close cross
        if (canClose) {
            closeImage = new Image(CLOSE_IMAGE_URL);
            captionPanel.setWidget(0, 1, closeImage);
        }
        // panel.getCellFormatter().setHeight(0, 1, "10px");
        captionPanel.getCellFormatter().setWidth(0, 1, "20px");
        captionPanel.getCellFormatter().setAlignment(0, 1, HasHorizontalAlignment.ALIGN_CENTER,
                HasVerticalAlignment.ALIGN_MIDDLE);
        if (canClose) {
            closeImage.addClickHandler(this);
        }
        captionPanel.setStyleName("Top");
        // super.setStyleName("os-image-panel");

        panel.setWidget(0, 0, captionPanel);
        panel.setHeight("100%");
        panel.setBorderWidth(0);
        panel.setCellPadding(0);
        panel.setCellSpacing(0);
        panel.getCellFormatter().setHeight(1, 0, "100%");
        panel.getCellFormatter().setWidth(1, 0, "100%");
        panel.getCellFormatter().setAlignment(1, 0, HasHorizontalAlignment.ALIGN_CENTER,
                HasVerticalAlignment.ALIGN_MIDDLE);
        super.setWidget(panel);

        setStyleName("gwt-DialogBox");
        caption.setStyleName("Caption");
        caption.addMouseListener(this);
        // caption.addMouseUpHandler(this);
        // caption.addMouseMoveHandler(this);
    }

    public void onClick(ClickEvent event) {
        hide();
    }

    public String getHTML() {
        return caption.getHTML();
    }

    public String getText() {
        return caption.getText();
    }

    public boolean onEventPreview(Event event) {
        // We need to preventDefault() on mouseDown events (outside of the
        // DialogBox content) to keep text from being selected when it
        // is dragged.
        if (DOM.eventGetType(event) == Event.ONMOUSEDOWN) {
            if (DOM.isOrHasChild(caption.getElement(), DOM.eventGetTarget(event))) {
                DOM.eventPreventDefault(event);
            }
        }

        return super.onEventPreview(event);
    }

    public void onMouseDown(Widget sender, int x, int y) {
        dragging = true;
        DOM.setCapture(caption.getElement());
        dragStartX = x;
        dragStartY = y;
    }

    public void onMouseEnter(Widget sender) {
    }

    public void onMouseLeave(Widget sender) {
    }

    public void onMouseMove(Widget sender, int x, int y) {
        if (dragging) {
            int absX = x + getAbsoluteLeft();
            int absY = y + getAbsoluteTop();
            setPopupPosition(absX - dragStartX, absY - dragStartY);
        }
    }

    public void onMouseUp(Widget sender, int x, int y) {
        dragging = false;
        DOM.releaseCapture(caption.getElement());
    }

    public boolean remove(Widget w) {
        if (child != w) {
            return false;
        }

        panel.remove(w);
        return true;
    }

    public void setHTML(String html) {
        caption.setHTML(html);
    }

    public void setText(String text) {
        caption.setText(text);
    }

    public void setWidget(Widget w) {
        // If there is already a widget, remove it.
        if (child != null) {
            panel.remove(child);
        }

        // Add the widget to the center of the cell.
        if (w != null) {
            panel.setWidget(1, 0, w);
        }

        child = w;

    }

    /**
     * Override, so that interior panel reflows to match parent's new width.
     * 
     * @Override
     */
    public void setWidth(String width) {
        super.setWidth(width);

        // note that you CANNOT call panel.setWidth("100%") until parent's width
        // has been explicitly set, b/c until then parent's width is
        // unconstrained
        // and setting panel's width to 100% will flow parent to 100% of browser
        // (i.e. can't do this in constructor)
        panel.setWidth("100%");
    }

    // public void onClick(Widget sender) {
    // // Window.alert("clicked");
    // this.hide();
    //
    // }

    // /**
    // * remove the location sanity checks, makes it possible for the popup to
    // * more outside the iframe it is in
    // *
    // * @Override
    // */
    public void setPopupPosition(int left, int top) {
        // Keep the popup within the browser's client area, so that they can't
        // get
        // 'lost' and become impossible to interact with. Note that we don't
        // attempt
        // to keep popups pegged to the bottom and right edges, as they will
        // then
        // cause scrollbars to appear, so the user can't lose them.
        if (left < 0) {
            left = 0;
        }
        if (top < 0) {
            top = 0;
        }

        // Set the popup's position manually, allowing setPopupPosition() to be
        // called before show() is called (so a popup can be positioned without
        // it
        // 'jumping' on the screen).
        Element elem = getElement();
        DOM.setStyleAttribute(elem, "left", left + "px");
        DOM.setStyleAttribute(elem, "top", top + "px");
    }

    public void show() {

        super.show();
        DOM.setStyleAttribute(getElement(), "zIndex", "2000");
        // DOM.setStyleAttribute(, "position", "absolute");
    }

    public void hide() {
        super.hide();

    }

    public boolean onKeyUpPreview(char keyCode, int modifiers) {
        if (keyCode == KeyboardListener.KEY_ESCAPE && closeImage != null) // a
                                                                          // proxy
                                                                          // for
                                                                          // canClose
                                                                          // bool
        {
            hide();
            return false;
        }
        return true; // true means don't suppress, false means suppress (weird,
                     // but true)
    }
}
