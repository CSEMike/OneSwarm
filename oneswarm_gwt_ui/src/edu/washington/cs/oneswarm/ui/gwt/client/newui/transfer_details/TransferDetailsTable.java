package edu.washington.cs.oneswarm.ui.gwt.client.newui.transfer_details;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.gen2.table.client.FixedWidthFlexTable;
import com.google.gwt.gen2.table.client.FixedWidthGrid;
import com.google.gwt.gen2.table.client.ScrollTable;
import com.google.gwt.gen2.table.client.SelectionGrid.SelectionPolicy;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.widgetideas.client.ProgressBar;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.Updateable;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.Strings;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.SwarmsBrowser;
import edu.washington.cs.oneswarm.ui.gwt.rpc.TorrentInfo;
import edu.washington.cs.oneswarm.ui.gwt.rpc.TorrentList;

/**
 * We store the infohash in the name label as a key so that we can update
 * columns upon refresh.
 */
class SwarmIDLabel extends Label {
    String mID;

    public SwarmIDLabel(TorrentInfo inSwarm) {
        super(inSwarm.getName());
        mID = inSwarm.getTorrentID();
        setTitle(inSwarm.getName()); // tooltip
    }

    public String getID() {
        return mID;
    }
}

class ComparableProgress extends ProgressBar implements Comparable<ComparableProgress> {
    public static final String CSS_DETAILS_PROGRESS = "os-details_progress_bar";

    public ComparableProgress() {
        super();
        addStyleName(CSS_DETAILS_PROGRESS);
    }

    public ComparableProgress(double minProgress, double maxProgress, double curProgress,
            TextFormatter textFormatter) {
        super(minProgress, maxProgress, curProgress, textFormatter);
        addStyleName(CSS_DETAILS_PROGRESS);
    }

    public ComparableProgress(double minProgress, double maxProgress, double curProgress) {
        super(minProgress, maxProgress, curProgress);
        addStyleName(CSS_DETAILS_PROGRESS);
    }

    public ComparableProgress(double minProgress, double maxProgress) {
        super(minProgress, maxProgress);
        addStyleName(CSS_DETAILS_PROGRESS);
    }

    public ComparableProgress(double curProgress) {
        super(curProgress);
        addStyleName(CSS_DETAILS_PROGRESS);
    }

    public int compareTo(ComparableProgress o) {
        if (o instanceof ProgressBar) {
            return (int) Math.round(getProgress() - ((ProgressBar) o).getProgress());
        }
        return -1;
    }
}

public class TransferDetailsTable extends ScrollTable implements Updateable {

    FixedWidthGrid mData = null;
    FixedWidthFlexTable mHeader = null;

    List<Integer> mFilteredRows = new ArrayList<Integer>();
    // private String mFilterText = "";

    Map<String, TorrentInfo> mCurrentIdsToSwarms = new HashMap<String, TorrentInfo>();
    private long mNextUpdate;

    public enum Type {
        PUBLIC, F2F
    };

    private final Type type;

    /**
     * 
     * @param isPublic
     *            true if the table should show public torrents false if showing
     *            friend-to-friend
     */
    public TransferDetailsTable(Type type) {
        super(new FixedWidthGrid(0, Strings.TRANSFER_DETAILS_COLUMNS.length),
                new FixedWidthFlexTable());
        this.type = type;
        mData = getDataTable();
        mHeader = getHeaderTable();

        mData.setSelectionPolicy(SelectionPolicy.ONE_ROW);

        /**
         * not only for webpage-style scrolling but also to get this widget to
         * declare it's needed size
         */
        setScrollPolicy(ScrollPolicy.DISABLED);

        mData.setSelectionPolicy(SelectionPolicy.MULTI_ROW);

        /**
         * This seems to work for now, but if you resize down, there's no going
         * back TODO: fix this
         */
        setResizePolicy(ResizePolicy.FILL_WIDTH);
        // setResizePolicy(ResizePolicy.FLOW);

        setupHeader();

        mData.setColumnSorter(new TransferColumnSorter());

        mHeader.setWidth("100%");
        mData.setWidth("100%");

        refreshRPC();
    }

    public void onDetach() {
        super.onDetach();
        OneSwarmGWT.removeFromUpdateTask(this);
    }

    public void onAttach() {
        super.onAttach();
        OneSwarmGWT.addToUpdateTask(this);
        // fix column widths
        // int width = this.getOffsetWidth();
        // double[] fracs = new double[] { 0.44, 0.06, 0.06, 0.2, 0.09, 0.07,
        // 0.07 };
        // for (int fItr = 0; fItr < fracs.length; fItr++) {
        // mHeader.setColumnWidth(fItr, (int) (fracs[fItr] * (double) width));
        // mData.setColumnWidth(fItr, (int) (fracs[fItr] * (double) width));
        // }
    }

    private void refreshRPC() {
        OneSwarmRPCClient.getService().getTransferringInfo(OneSwarmRPCClient.getSessionID(),
                new AsyncCallback<TorrentList>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    public void onSuccess(TorrentList result) {
                        refresh(result);
                        mNextUpdate = System.currentTimeMillis() + 2000;
                    }
                });
    }

    private void refresh(TorrentList inList) {
        // set-ify this for access speed and so we can prune the list as we
        // update.
        Map<String, TorrentInfo> idsToSwarms = new HashMap<String, TorrentInfo>();
        for (TorrentInfo info : inList.getTorrentInfos()) {
            idsToSwarms.put(info.getTorrentID(), info);
            mCurrentIdsToSwarms.put(info.getTorrentID(), info);
        }

        List<Integer> toRemove = new ArrayList<Integer>();

        /**
         * First pass -- update what's already there
         */
        for (int row = 0; row < mData.getRowCount(); row++) {
            String swarmID = ((SwarmIDLabel) mData.getWidget(row, 0)).getID();

            // Apparently this row finished since it's no longer in the update
            // set (or was removed), so remove.
            TorrentInfo info = idsToSwarms.get(swarmID);
            if (info == null) {
                toRemove.add(row);
                continue;
            }

            updateRow(row, info);
            idsToSwarms.remove(swarmID);
        }

        /**
         * Next remove anything that finished or was stopped.
         */
        Collections.sort(toRemove);
        Collections.reverse(toRemove);
        for (int r : toRemove)
            mData.removeRow(r);

        /**
         * Finally add any new swarms from this update
         */
        for (TorrentInfo neu : idsToSwarms.values()) {
            if (neu.getSharePublic() && type == Type.PUBLIC) {
                updateRow(mData.getRowCount(), neu);
            } else if (neu.getShareWithFriends() && type == Type.F2F) {
                updateRow(mData.getRowCount(), neu);
            } else {
                // this only happens if the swarm has no permissions, and then
                // it shouldn't show up anyway...
                System.err
                        .println("this happened: swarm has no permissions and so we're skipping the updateRow(), "
                                + neu.getName());
            }
        }
        redraw();
    }

    private void updateRow(int row, TorrentInfo swarm) {
        // System.out.println("update row: " + row + " / " + swarm.getName());
        for (int col_index = 0; col_index < Strings.TRANSFER_DETAILS_COLUMNS.length; col_index++) {
            String col_key;
            if (type == Type.PUBLIC) {
                col_key = Strings.TRANSFER_DETAILS_COLUMNS[col_index];
            } else {
                col_key = Strings.TRANSFER_F2F_DETAILS_COLUMNS[col_index];
            }
            Widget widget = null;
            if (row < mData.getRowCount()) {
                mData.getWidget(row, col_index);
            }
            boolean needs_set = false;

            if (col_key.equals(Strings.XFER_NAME)) {
                if (widget == null) {
                    widget = new SwarmIDLabel(swarm);
                    needs_set = true;
                } else {
                    ; // we never need to update this
                }
            } else if (col_key.equals(Strings.XFER_PEERS)) {
                String text = "" + Math.max(0, swarm.getLeechers() - swarm.getLeechersF2f());
                if (swarm.getTotalLeechers() != -1) {
                    text += " (" + swarm.getTotalLeechers() + ")";
                }
                if (widget == null) {
                    widget = new Label(text);
                    needs_set = true;
                } else {
                    ((Label) widget).setText(text);
                }
            } else if (col_key.equals(Strings.XFER_F2F_PEERS)) {
                String text = "" + swarm.getLeechersF2f();
                if (widget == null) {
                    widget = new Label(text);
                    needs_set = true;
                } else {
                    ((Label) widget).setText(text);
                }
            } else if (col_key.equals(Strings.XFER_SEEDS)) {
                String text = "" + Math.max(0, swarm.getSeeders() - swarm.getSeedersF2F());
                if (swarm.getTotalSeeders() != -1) {
                    text += " (" + swarm.getTotalSeeders() + ")";
                }
                if (widget == null) {
                    widget = new Label(text);
                    needs_set = true;
                } else {
                    ((Label) widget).setText(text);
                }
            } else if (col_key.equals(Strings.XFER_F2F_SEEDS)) {
                String text = "" + swarm.getSeedersF2F();
                if (widget == null) {
                    widget = new Label(text);
                    needs_set = true;
                } else {
                    ((Label) widget).setText(text);
                }
            } else if (col_key.equals(Strings.XFER_PROGRESS)) {
                // widget = new Label(swarm.getProgress()+"");
                double prog = ((double) swarm.getProgress() / 1000.0) * 100.0;
                if (widget == null) {
                    if (prog < 100.0)
                        widget = new ComparableProgress(prog);
                    else
                        widget = new Label(Strings.get(Strings.SEEDING_STATUS_LABEL));

                    needs_set = true;
                } else if (widget instanceof ComparableProgress) {
                    if (prog >= 100) {
                        widget = new Label(Strings.get(Strings.SEEDING_STATUS_LABEL));
                        needs_set = true;
                    } else {
                        ((ComparableProgress) widget).setProgress(prog);
                    }
                } else {
                    ; // never updated
                }
            } else if (col_key.equals(Strings.XFER_SIZE)) {
                if (widget == null) {
                    widget = new FormattedSize(swarm.getTotalSize(), "");
                    needs_set = true;
                } else {
                    ; // never updated
                }
            } else if (col_key.equals(Strings.XFER_UPLOAD_RATE)) {
                if (widget == null) {
                    widget = new FormattedSize((long) swarm.getUploadRate() * 1024, "ps");
                    needs_set = true;
                } else {
                    ((FormattedSize) widget).update((long) swarm.getUploadRate() * 1024);
                }
            } else if (col_key.equals(Strings.XFER_DOWNLOAD_RATE)) {
                if (widget == null) {
                    widget = new FormattedSize((long) swarm.getDownloadRate() * 1024, "ps");
                    needs_set = true;
                } else {
                    ((FormattedSize) widget).update((long) swarm.getDownloadRate() * 1024);
                }
            } else if (col_key.equals(Strings.XFER_REMAINING)) {
                if (widget == null) {
                    widget = new Label(swarm.getRemaining());
                    needs_set = true;
                } else {
                    ((Label) widget).setText(swarm.getRemaining());
                }
            } else {
                System.err.println("unknown column key: " + col_key);
            }

            if (needs_set) {
                if (mData.getRowCount() <= row) {
                    mData.resizeRows(row + 1);
                }
                mData.setWidget(row, col_index, widget);
            }
        }
    }

    private void setupHeader() {
        for (int i = 0; i < Strings.TRANSFER_DETAILS_COLUMNS.length; i++) {
            // mHeader.setWidget(0, i, new
            // Label(Strings.TRANSFER_DETAILS_COLUMNS[i]));
            if (type == Type.PUBLIC) {
                mHeader.setText(0, i, Strings.TRANSFER_DETAILS_COLUMNS[i]);
            } else if (type == Type.F2F) {
                mHeader.setText(0, i, Strings.TRANSFER_F2F_DETAILS_COLUMNS[i]);
            } else {
                OneSwarmGWT.log("ERROR, transfer window unknown type!!!");
            }
        }
    }

    public void update(int count) {
        if (mNextUpdate < System.currentTimeMillis()) {
            /**
             * Just in case this RPC fails...
             */
            mNextUpdate = System.currentTimeMillis() + 10 * 1000;
            refreshRPC();
        }
    }

    public void filterDisplayed(String filterPattern) {

        // mFilterText = filterPattern;

        // removing old filters
        for (Integer r : mFilteredRows.toArray(new Integer[0])) {
            SwarmIDLabel label = (SwarmIDLabel) mData.getWidget(r, 0);
            if (label.getText().toLowerCase().contains(filterPattern) == true) {
                mFilteredRows.remove(r);
                mData.getRowFormatter().setVisible(r, true);
            }
        }

        // filtering
        for (int rItr = 0; rItr < mData.getRowCount(); rItr++) {
            SwarmIDLabel label = (SwarmIDLabel) mData.getWidget(rItr, 0);
            if (label == null) {
                System.err.println("null row label: " + rItr);
            }
            if (filterPattern == null) {
                System.err.println("null filter pattern");
                (new Exception()).printStackTrace();
            }
            if (label.getText().toLowerCase().contains(filterPattern) == false) {
                // only remove this once...
                if (mData.getRowFormatter().isVisible(rItr) == true) {
                    mFilteredRows.add(rItr);
                    mData.getRowFormatter().setVisible(rItr, false);
                }
            } else {
                System.out.println("\tkeep: " + label.mID + " / " + label.getText() + " / "
                        + filterPattern + " "
                        + label.getText().toLowerCase().contains(filterPattern));
            }
        }

        // this is a hack to deal with the async nature of the rpc
        if (getBrowserParent(this) != null)
            getBrowserParent(this).updateFilteredCountFromTransferDetails(mFilteredRows.size());
        else {
            System.err.println("***** get browser parent null, shouldn't happen");
        }

    }

    public static SwarmsBrowser getBrowserParent(Widget inWidget) {
        Widget w = inWidget;
        while ((w instanceof SwarmsBrowser) == false && w != null)
            w = w.getParent();
        return (SwarmsBrowser) w;
    }

    protected TorrentInfo getSelectedSwarm() {
        Set<Integer> selected = mData.getSelectedRows();
        assert selected.size() <= 1 : "we aren't allowing multiple selections";
        if (selected.size() == 0)
            return null;
        int row = selected.iterator().next();
        SwarmIDLabel swarmID = (SwarmIDLabel) mData.getWidget(row, 0);
        return mCurrentIdsToSwarms.get(swarmID.mID);
    }
}
