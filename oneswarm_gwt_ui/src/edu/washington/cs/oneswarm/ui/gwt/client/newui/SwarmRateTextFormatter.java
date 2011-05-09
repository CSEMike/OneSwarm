package edu.washington.cs.oneswarm.ui.gwt.client.newui;

import com.google.gwt.widgetideas.client.ProgressBar;
import com.google.gwt.widgetideas.client.ProgressBar.TextFormatter;

import edu.washington.cs.oneswarm.ui.gwt.rpc.StringTools;
import edu.washington.cs.oneswarm.ui.gwt.rpc.TorrentInfo;

public class SwarmRateTextFormatter extends TextFormatter {

    TorrentInfo swarm;

    public SwarmRateTextFormatter(TorrentInfo swarm) {
        this.swarm = swarm;
    }

    protected String getText(ProgressBar bar, double curProgress) {
        if (swarm.isStarted()) {
            // * 1024 since these are denominated in KBps
            String rate;
            if (swarm.getExtraSourceSpeed() > 0) {
                rate = " ("
                        + StringTools.formatRate((int) swarm.getDownloadRate() * 1024
                                + swarm.getExtraSourceSpeed()) + "/s)*";
            } else {
                rate = " (" + StringTools.formatRate((int) swarm.getDownloadRate() * 1024) + "/s"
                        + ")";
            }
            return (int) (100 * bar.getPercent()) + "%" + rate;
        } else {
            return (int) (100 * bar.getPercent()) + "%" + " (stopped)";
        }
    }
}
