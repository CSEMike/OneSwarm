package edu.washington.cs.oneswarm.ui.gwt.client.newui.permissions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.gen2.table.client.FixedWidthFlexTable;
import com.google.gwt.gen2.table.client.FixedWidthGrid;
import com.google.gwt.gen2.table.client.ScrollTable;
import com.google.gwt.gen2.table.client.AbstractScrollTable.ResizePolicy;
import com.google.gwt.gen2.table.client.SelectionGrid.SelectionPolicy;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.HelpButton;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;

public class MembershipList<T> extends SimplePanel {
	private static OSMessages msg = OneSwarmGWT.msg;

	class ObjectAwareLabel extends Label {
		public T original;

		public ObjectAwareLabel(String inString) {
			super(inString);
		}
	}

	ScrollTable mScrollTable = null;
	private FixedWidthGrid mObjectsData = null;

	List<T> mObjects = null;
	Set<T> mExcluded = new HashSet<T>();
	Set<T> mFiltered = new HashSet<T>();

	public static final String CSS_MEMBERSHIP_LIST_ROW = "os-membership_list";
	public static final String CSS_MEMBERSHIP_DISABLED = "os-membership_list_disabled";

	boolean mAdding = true;

	List<MembershipListListener<T>> listeners = new ArrayList<MembershipListListener<T>>();

	TextBox mFilterTextBox = new TextBox();

	public MembershipList(String inColumnName, boolean inAdd, List<T> objects, List<T> excluded) {
		this(inColumnName, inAdd, objects, excluded, true);
	}

	FixedWidthFlexTable header = new FixedWidthFlexTable();

	public MembershipList(String inColumnName, boolean inAdd, List<T> inObjects, List<T> inExcluded, boolean useFilter) {
		this(inColumnName, inAdd, inObjects, inExcluded, useFilter, new Comparator<T>() {
			public int compare(T o1, T o2) {
				return o1.toString().compareTo(o2.toString());
			}
		});
	}

	public MembershipList(String inColumnName, boolean inAdd, List<T> inObjects, List<T> inExcluded, boolean useFilter, Comparator<T> comp) {
		this.setWidth("100%");

		List<T> objects = new ArrayList<T>(inObjects.size());
		for (T o : inObjects)
			objects.add(o);

		Collections.sort(objects, comp);

		VerticalPanel vp = new VerticalPanel();

		mAdding = inAdd;

		mObjects = objects;
		for (T o : inExcluded) {
			mExcluded.add(o);
		}

		String[] dl_cols = new String[] { inColumnName };

		mObjectsData = new FixedWidthGrid(0, dl_cols.length);

		mScrollTable = new ScrollTable(mObjectsData, header);
		mScrollTable.setWidth("100%");
		mScrollTable.setHeight("300px");
		mScrollTable.setResizePolicy(ResizePolicy.FIXED_WIDTH);

		header.setColumnWidth(0, 270);
		mObjectsData.setColumnWidth(0, 270);

		// mScrollTable.setScrollPolicy(ScrollPolicy.DISABLED);

		int[] widths = new int[] { 300 };

		for (int i = 0; i < dl_cols.length; i++) {
			header.setText(0, i, dl_cols[i]);

			// header.setColumnWidth(i, widths[i]);
			// mObjectsData.setColumnWidth(i, widths[i]);
		}

		mObjectsData.setSelectionPolicy(SelectionPolicy.ONE_ROW);

		HorizontalPanel filterPanel = new HorizontalPanel();

		if (useFilter) {
			Label filterLabel = new Label("Filter:");
			filterPanel.add(filterLabel);
			filterPanel.add(mFilterTextBox);
			filterPanel.setSpacing(3);
			mFilterTextBox.addStyleName(OneSwarmCss.SMALL_BUTTON);
			mFilterTextBox.setHeight("13px");
			filterPanel.setCellVerticalAlignment(filterLabel, VerticalPanel.ALIGN_MIDDLE);
			mFilterTextBox.addKeyboardListener(new KeyboardListener() {
				public void onKeyDown(Widget sender, char keyCode, int modifiers) {
				}

				public void onKeyPress(Widget sender, char keyCode, int modifiers) {
				}

				public void onKeyUp(Widget sender, char keyCode, int modifiers) {
					refilter();
				}
			});
			vp.add(filterPanel);
		}

		vp.setWidth("100%");
		vp.add(mScrollTable);

		refilter();

		// Button clearButton = new Button("clear");
		// clearButton.addClickListener(new ClickListener(){
		// public void onClick(Widget sender) {
		// int count = mObjectsData.getRowCount();
		// for( int i=0; i<count; i++ )
		// {
		// mObjectsData.removeRow(0);
		// }
		// }});
		// vp.add(clearButton);

		this.setWidget(vp);
	}

	public ArrayList<T> getMembers() {
		ArrayList<T> outList = new ArrayList<T>();
		for (T o : mObjects) {
			if (!mExcluded.contains(o)) {
				outList.add(o);
			}
		}
		return outList;
	}

	public void addExcluded(T inObject) {
		mExcluded.add(inObject);
		refilter();
	}

	private void refilter() {
		mFiltered.clear();
		for (T o : mObjects) {
			if (!o.toString().toLowerCase().contains(mFilterTextBox.getText().toLowerCase())) {
				mFiltered.add(o);
			}
		}

		int count = mObjectsData.getRowCount();
		for (int i = 0; i < count; i++) {
			mObjectsData.removeRow(0);
		}

		for (int i = 0; i < mObjects.size(); i++) {
			if (mExcluded.contains(mObjects.get(i))) {
				continue;
			}

			if (mFiltered.contains(mObjects.get(i))) {
				continue;
			}

			addRow(mObjects.get(i));
		}
	}

	private int findPos(T inObject) {
		if (mExcluded.contains(inObject)) {
			System.err.println("trying to find position for excluded object -- shouldn't happen!");
			return -1;
		}

		int pos, itr;
		for (pos = 0, itr = 0; itr < mObjects.size(); itr++) {
			if (mExcluded.contains(mObjects.get(itr))) {
				// logger.finer("excluded contains:" + mObjects.get(itr));
				continue;
			}
			if (mFiltered.contains(mObjects.get(itr))) {
				// logger.finer("mfiltered contains: " +
				// mObjects.get(itr).toString());
				continue;
			}
			if (mObjects.get(itr).equals(inObject))
				break;
			pos++;
		}

		return pos;
	}

	private void addRow(final T inObject) {
		final int row = findPos(inObject);
		mObjectsData.insertRow(row);

		final HorizontalPanel hp = new HorizontalPanel();
		hp.addStyleName(CSS_MEMBERSHIP_LIST_ROW);

		ObjectAwareLabel l = new ObjectAwareLabel(inObject.toString());
		l.original = inObject;

		hp.setCellHorizontalAlignment(l, HorizontalPanel.ALIGN_LEFT);
		HelpButton help = null;
		// if
		// (inObject.toString().equals(msg.visibility_group_public_internet()))
		// {
		if (inObject.toString().equals("Public Internet")) {
			help = new HelpButton(msg.visibility_group_public_internet_help_HTML());
			l.setText(msg.visibility_group_public_internet());
			// } else if
			// (inObject.toString().equals(msg.visibility_group_all_friends()))
			// {
		} else if (inObject.toString().equals("All friends")) {
			help = new HelpButton(msg.visibility_group_all_friends_help_HTML());
			l.setText(msg.visibility_group_all_friends());
		}
		if (help != null) {
			HorizontalPanel labelPlusHelp = new HorizontalPanel();
			labelPlusHelp.add(l);
			labelPlusHelp.add(help);
			labelPlusHelp.setCellVerticalAlignment(l, HorizontalPanel.ALIGN_MIDDLE);
			labelPlusHelp.setCellVerticalAlignment(help, HorizontalPanel.ALIGN_MIDDLE);
			hp.add(labelPlusHelp);
		} else {
			hp.add(l);
		}

		l.setWordWrap(true);

		Hyperlink addButton = mAdding ? new Hyperlink("(Add)", "Add") : new Hyperlink("(Remove)", "Remove");
//		addButton.addStyleName(OneSwarmCss.SMALL_BUTTON);
		addButton.addClickListener(new ClickListener() {
			public void onClick(Widget sender) {

				if (hp.getStyleName().contains(CSS_MEMBERSHIP_DISABLED)) {
					Window.alert("You cannot add or remove specific groups because more general groups already apply to this swarm.");
				} else {
					mExcluded.add(inObject);
					refilter();

					for (MembershipListListener<T> l : listeners) {
						l.objectEvent(MembershipList.this, inObject);
					}
				}
			}
		});

		hp.setWidth("100%");

		hp.add(addButton);
		hp.setCellHorizontalAlignment(addButton, HorizontalPanel.ALIGN_RIGHT);
		hp.setCellVerticalAlignment(addButton, VerticalPanel.ALIGN_MIDDLE);

		mObjectsData.setWidget(row, 0, hp);
	}

	public void disableAllExcept(Set<T> inEnabled) {
		for (int i = 0; i < mObjectsData.getRowCount(); i++) {
			if (inEnabled != null) {
				if (inEnabled.contains(((ObjectAwareLabel) mObjectsData.getWidget(i, 0)).original)) {
					continue;
				}
			}
			HorizontalPanel hp = (HorizontalPanel) mObjectsData.getWidget(i, 0);
			hp.addStyleName(CSS_MEMBERSHIP_DISABLED);
		}
	}

	public void enableAllExcept(Set<T> inDisabled) {
		for (int i = 0; i < mObjectsData.getRowCount(); i++) {
			if (inDisabled != null) {
				if (inDisabled.contains(((ObjectAwareLabel) mObjectsData.getWidget(i, 0)).original)) {
					continue;
				}
			}
			HorizontalPanel hp = (HorizontalPanel) mObjectsData.getWidget(i, 0);
			hp.removeStyleName(CSS_MEMBERSHIP_DISABLED);
		}
	}

	public void addListener(MembershipListListener<T> inListener) {
		listeners.add(inListener);
	}

	public void restoreExcluded(T excluded) {
		if (mExcluded.remove(excluded) == false) {
			return;
		} else {
			// logger.finer("restored excluded: " + excluded.toString());
		}
		refilter();
	}
}
