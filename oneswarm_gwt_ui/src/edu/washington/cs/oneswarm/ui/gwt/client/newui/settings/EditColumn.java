package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;

import com.google.gwt.cell.client.AbstractEditableCell;
import com.google.gwt.cell.client.EditTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.Window;

abstract class EditColumn<T> extends Column<T, String> implements
        FieldUpdater<T, String> {

    public EditColumn(boolean numbers) {
        super(new EditTextCell());
        setFieldUpdater(this);
    }

    public abstract String getValue(T service);

    public abstract void update(int index, T service, String value);

    @SuppressWarnings("unchecked")
    public void onBrowserEvent(Context context, Element elem, final T object,
            NativeEvent event) {
        try {
            super.onBrowserEvent(context, elem, object, event);
        } catch (NumberFormatException e) {
            Window.alert(e.getMessage());
            Object key = context.getKey();
            ((AbstractEditableCell<T, String>) this.getCell()).clearViewData(key);
            super.onBrowserEvent(context, elem, object, event);
        }
    }

}