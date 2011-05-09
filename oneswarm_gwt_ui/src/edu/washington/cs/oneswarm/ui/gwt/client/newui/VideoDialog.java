/**
 * 
 */
package edu.washington.cs.oneswarm.ui.gwt.client.newui;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.Parser;
import edu.washington.cs.oneswarm.ui.gwt.client.ReportableErrorDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmUIServiceAsync;

public class VideoDialog extends OneSwarmDialogBox {

    /**
	 * 
	 */
    // private final MediaWidget mediaWidget;
    // number of sec to tell the player to buffer before starting to play
    private static final int BUFFER_LENGTH_DOWNLOADING = 20;
    private static final int BUFFER_LENGTH_FINISHED = 3;

    private Map<String, String> parameters = new HashMap<String, String>();
    private EntireUIRoot mRoot = null;

    public VideoDialog(final String torrentID, boolean downloading, String fileInTorrent,
            int filesInPlaylist, EntireUIRoot inRoot) {
        super(false, false, true);
        mRoot = inRoot;
        setText("Media preview");

        parameters.put(OneSwarmConstants.WEB_PARAM_TORRENT_ID, torrentID);

        int totalHeight = OneSwarmConstants.DEFAULT_WEB_PLAYER_HEIGTH;
        int width = OneSwarmConstants.DEFAULT_WEB_PLAYER_WIDTH;
        int playlistHeight = 200;

        parameters.put(OneSwarmConstants.WEB_PARAM_FLV_PLAYER_WIDTH, "" + width);
        parameters.put(OneSwarmConstants.WEB_PARAM_FLV_PLAYER_HEIGHT, "" + totalHeight);
        parameters.put(OneSwarmConstants.WEB_PARAM_VIDEO_PATH, fileInTorrent);
        parameters.put(OneSwarmConstants.WEB_PARAM_FLV_PLAYER_AUTOSTART, "" + true);
        if (filesInPlaylist > 1) {
            totalHeight += playlistHeight;
            parameters.put(OneSwarmConstants.WEB_PARAM_FLV_PLAYER_HEIGHT, "" + totalHeight);
            parameters.put(OneSwarmConstants.WEB_PARAM_FLV_PLAYER_PLAYLIST, "bottom");
            parameters.put(OneSwarmConstants.WEB_PARAM_FLV_PLAYER_PLAYLIST_SIZE, ""
                    + playlistHeight);
            parameters.put(OneSwarmConstants.WEB_PARAM_FLV_PLAYER_REPEAT, "list");

        }
        if (!downloading) {
            parameters.put(OneSwarmConstants.WEB_PARAM_FLV_PLAYER_BUFFER_LENGTH, ""
                    + BUFFER_LENGTH_FINISHED);
        } else {
            parameters.put(OneSwarmConstants.WEB_PARAM_FLV_PLAYER_BUFFER_LENGTH, ""
                    + BUFFER_LENGTH_DOWNLOADING);
        }

        // StringBuffer iframePath = new StringBuffer();
        // iframePath.append("flash.html?");
        //
        // for (Iterator<String> iter = parameters.keySet().iterator(); iter
        // .hasNext();) {
        // String key = iter.next();
        // String value = parameters.get(key);
        // iframePath.append(key + "=" + value + "&");
        // }
        // if (iframePath.charAt(iframePath.length() - 1) == '&') {
        // iframePath.setLength(iframePath.length() - 1);
        // }
        // // Window.alert(iframePath.toString());
        // Frame iframe = new Frame(iframePath.toString());
        //
        // iframe.setWidth("" + (width + 10));
        // iframe.setHeight("" + (heigth + 32));
        //
        // this.setWidget(iframe);

        this.setWidth((width + 10) + "px");
        this.setHeight((totalHeight + 32) + "px");

        this.setWidget(new HTML("<p id='player'> </p>"));

        final OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();
        service.torrentExists(OneSwarmRPCClient.getSessionID(), torrentID,
                new AsyncCallback<Boolean>() {

                    public void onFailure(Throwable caught) {

                        System.out.println("********** calling torrent exists failure");

                        RootPanel.get().add(new Label("And error occured: " + caught.getMessage()));
                    }

                    public void onSuccess(Boolean result) {

                        System.out.println("********** calling torrent exists success");

                        Boolean exists = (Boolean) result;

                        if (exists.booleanValue()) {
                            OneSwarmGWT.log("swarm exists: " + torrentID);
                            writeOutPlayer();
                        } else {
                            OneSwarmGWT.log("swarm does not exist: " + torrentID);
                            OneSwarmGWT.log("trying metainfo download: " + torrentID);

                            // String path;
                            // // check if the path to the real .torrent file is
                            // // specified
                            // if
                            // (parameters.containsKey(OneSwarmConstants.WEB_PARAM_METAINFO_PATH))
                            // {
                            // path = (String)
                            // parameters.get(OneSwarmConstants.WEB_PARAM_METAINFO_PATH);
                            // } else {
                            // // else, try with magnet link
                            // path = torrentID;
                            // }
                            //
                            // System.out.println("calling download torrent");
                            //
                            // service.downloadTorrent(OneSwarmRPCClient.getSessionID(),
                            // path, new AsyncCallback<Integer>() {
                            //
                            // public void onFailure(Throwable caught) {
                            // RootPanel.get().add(new
                            // Label("And error occured: " +
                            // caught.getMessage()));
                            // }
                            //
                            // public void onSuccess(Integer result) {
                            //
                            // System.out.println("calling download torrent success");
                            //
                            // Integer downloadID = result;
                            // // ok, start the download
                            // StartDownloadDialog startDialog = new
                            // StartDownloadDialog(_this,
                            // downloadID.intValue());
                            // startDialog.center();
                            // startDialog.show();
                            //
                            // }
                            // });
                        }

                    }
                });

        System.out.println("got to decrease update rate");

        OneSwarmGWT.registerPlayerWindow(this);
    }

    public void onAttach() {
        super.onAttach();

        if (mRoot != null) {
            mRoot.setPlayingVideo(true);
        }
    }

    public void onDetatch() {
        super.onDetach();

        if (mRoot != null) {
            mRoot.setPlayingVideo(false);
        }
    }

    private void writeOutPlayer() {
        System.out.println("writing out player");
        String evalString = generateSwfObjectVariables(parameters);
        System.out.println("load flash player");
        loadFlashPlayer(evalString);
    }

    /**
     * this function generates the javascript that sets the player properties
     * 
     * make sure that all key-value pairs in safeParameterMap are safe!
     */

    private static String generateSwfObjectVariables(Map<String, String> safeParameterMap) {
        StringBuffer buf = new StringBuffer();

        String movieWidth = safeParameterMap.get(OneSwarmConstants.WEB_PARAM_FLV_PLAYER_WIDTH);
        if (movieWidth == null) {
            movieWidth = "425";
            safeParameterMap.put(OneSwarmConstants.WEB_PARAM_FLV_PLAYER_WIDTH, movieWidth);

        }

        String movieHeigth = safeParameterMap.get(OneSwarmConstants.WEB_PARAM_FLV_PLAYER_HEIGHT);
        if (movieHeigth == null) {
            movieHeigth = "350";
            safeParameterMap.put(OneSwarmConstants.WEB_PARAM_FLV_PLAYER_HEIGHT, movieHeigth);
        }

        String torrentID = safeParameterMap.get(OneSwarmConstants.WEB_PARAM_TORRENT_ID);

        // String fileName = null;
        // if
        // (safeParameterMap.containsKey(OneSwarmConstants.WEB_PARAM_VIDEO_PATH))
        // {
        // fileName = OneSwarmConstants.videoPath + "?" +
        // OneSwarmConstants.WEB_PARAM_TORRENT_ID + "=" + torrentID + "&" +
        // OneSwarmConstants.WEB_PARAM_VIDEO_PATH + "=" +
        // safeParameterMap.get(OneSwarmConstants.WEB_PARAM_VIDEO_PATH);
        // } else {
        // fileName = OneSwarmConstants.videoPath + "?" +
        // OneSwarmConstants.WEB_PARAM_TORRENT_ID + "=" + torrentID;
        // }

        // create the javascript, make sure that all input strings are properly
        // escaped before this step
        buf.append("var so = new $wnd.SWFObject('" + GWT.getModuleBaseURL()
                + "flvplayer.swf','player', '" + movieWidth + "',	'" + movieHeigth + "', '7');\n");

        buf.append("so.addParam('allowfullscreen','true');\n");

        String filePath = "oneswarmgwt/flv_movie/" + torrentID.replaceAll("urn_btih_", "") + "/"
                + safeParameterMap.get(OneSwarmConstants.WEB_PARAM_VIDEO_PATH);
        // buf.append("so.addParam('flashvars','file=" + filePath + "');\n");
        // buf.append("so.addVariable('file',\"" + "file.flv" + "\");\n");
        buf.append("so.addVariable('file',\"" + filePath + "\");\n");
        // buf.append("so.addVariable('plugins',\"" + "metaviewer-1" +
        // "\");\n");
        // buf.append("so.addVariable('streamer','/oneswarmgwt/flv_movie');\n");
        // buf.append("so.addVariable('skin', '/playerskins/stylish.swf');\n");
        for (Iterator<String> iter = safeParameterMap.keySet().iterator(); iter.hasNext();) {
            String key = iter.next();
            // ignore the torrent id key
            if (!key.equals(OneSwarmConstants.WEB_PARAM_TORRENT_ID)) {
                String value = safeParameterMap.get(key);
                if (!Parser.isAcceptableString(value)) {
                    Window.alert("warning, '" + value + "' is not a safe string");
                } else {
                    buf.append("so.addVariable('" + key + "','" + value + "');\n");
                }
            }
        }
        buf.append("so.write('player');\n");
        // Window.alert(buf.toString());
        System.out.println("evalstring is: " + buf.toString());
        return buf.toString();

    }

    public static native void loadFlashPlayer(String evalString) /*-{
                                                                 $doc.getElementById('player').innerHTML = 
                                                                 "<a href='http://www.macromedia.com/go/getflashplayer' target='_blanc' >You need flash to see this movie</a>";
                                                                 eval(evalString);
                                                                 }-*/;

    // @Override
    public void onClick(ClickEvent event) {
        OneSwarmGWT.deRegisterPlayerWindow();
        super.onClick(event);
    }

    // class StartDownloadDialog extends OneSwarmDialogBox implements
    // ClickListener {
    //
    // final Button yesButton;
    //
    // final int downloadId;
    //
    // final Label progressLabel;
    //
    // final VideoDialog parent;
    // Timer progressUpdater;
    //
    // boolean evenUpdate = false;

    // public StartDownloadDialog(VideoDialog parent, int downloadID) {
    // super();
    // this.parent = parent;
    // this.downloadId = downloadID;
    //
    // setText("Start download?");
    // DockPanel panel = new DockPanel();
    //
    // VerticalPanel textPanel = new VerticalPanel();
    // Label text = new Label("Do you want to start this download?");
    // textPanel.add(text);
    //
    // HTML link = new
    // HTML("<a href='http://oneswarm.cs.washington.edu/' target='_blank'>Why we ask</a>");
    // textPanel.add(link);
    //
    // textPanel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
    // panel.add(textPanel, DockPanel.NORTH);
    // panel.setCellHorizontalAlignment(textPanel, DockPanel.ALIGN_CENTER);
    //
    // yesButton = new Button("Start Download!");
    // yesButton.addClickListener(this);
    // panel.add(yesButton, DockPanel.CENTER);
    // panel.setCellHorizontalAlignment(yesButton, DockPanel.ALIGN_CENTER);
    //
    // progressLabel = new Label("");
    // progressLabel.setVisible(false);
    //
    // panel.add(progressLabel, DockPanel.SOUTH);
    //
    // setWidget(panel);
    // }
    //
    // public void onClick(Widget sender) {
    // if (sender.equals(yesButton)) {
    // setText("Downloading...");
    // yesButton.setEnabled(false);
    //
    // progressLabel.setText("downloading metadata...");
    // progressLabel.setVisible(true);
    //
    // progressUpdater = createProgressUpdateTimer();
    //
    // progressUpdater.scheduleRepeating(1000);
    //
    // addDownload();
    // // hide();
    // }
    // }
    //
    // private Timer createProgressUpdateTimer() {
    // Timer t = new Timer() {
    // public void run() {
    // OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();
    //
    // service.getTorrentDownloadProgress(OneSwarmRPCClient.getSessionID(),
    // downloadId, new AsyncCallback<Integer>() {
    //
    // public void onFailure(Throwable caught) {
    // progressLabel.setText("Error while downloading metadata");
    // progressUpdater.cancel();
    // }
    //
    // public void onSuccess(Integer result) {
    // Integer progress = result;
    //
    // if (progress.intValue() == 100) {
    //
    // progressLabel.setText("starting download...");
    // progressUpdater.cancel();
    // } else if (progress.intValue() == 0) {
    // evenUpdate = !evenUpdate;
    // if (evenUpdate) {
    // progressLabel.setText("downloading metadata...");
    // } else {
    // progressLabel.setText("downloading metadata....");
    // }
    // } else if (progress.intValue() != -2) {
    //
    // progressLabel.setText("downloading metadata... (" + progress + ")");
    //
    // } else {
    // setWidget(new Label("An error occurred"));
    // progressUpdater.cancel();
    // }
    //
    // }
    // });
    // }
    // };
    // return t;
    // }

    // private void addDownload() {
    // OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();
    //
    //
    // service.addTorrent(OneSwarmRPCClient.getSessionID(), downloadId, null,
    // new ArrayList<PermissionsGroup>(), new AsyncCallback() {
    //
    // public void onFailure(Throwable caught) {
    // setWidget(new Label("An error occurred: " + caught.getMessage()));
    // progressUpdater.cancel();
    // }
    //
    // public void onSuccess(Object result) {
    // Boolean success = (Boolean) result;
    //
    // if (success.booleanValue()) {
    // parent.writeOutPlayer();
    // hide();
    // } else {
    // setWidget(new Label("An error occurred: "));
    // }
    // progressUpdater.cancel();
    // }
    // });
    // }
    //
    // }
}
