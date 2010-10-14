package edu.washington.cs.oneswarm.ui.gwt.client.newui.permissions.swarms;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.rpc.StringTools;
import edu.washington.cs.oneswarm.ui.gwt.rpc.TorrentInfo;

public class TorrentInfoHeaderPanel extends HorizontalPanel {

	public static final String CSS_TORRENT_HEADER_NAME = "os-torrent_header_name";
	public static final String CSS_TORRENT_HEADER_SUB = "os-torrent_header_sub";
	
	public TorrentInfoHeaderPanel(TorrentInfo inTorrent) {
		
		Image img = new Image(GWT.getModuleBaseURL() + "image?infohash=" + inTorrent.getTorrentID());
		img.setWidth("64px");
		img.setHeight("64px");
		this.add(img);
		this.setCellWidth(img, "64px");
		this.setCellHorizontalAlignment(img, ALIGN_LEFT);
		this.setSpacing(3);
		VerticalPanel vp = new VerticalPanel();
		Label l = new Label(StringTools.truncate(inTorrent.getName(), 58, true));
		l.setTitle(inTorrent.getName());
		l.addStyleName(CSS_TORRENT_HEADER_NAME);
		vp.add(l);
		String word = "files";
		if( inTorrent.getNumFiles() == 1 )
		{
			word = "file";
		}
		l = new Label(StringTools.formatRate(inTorrent.getTotalSize()) + " (" + inTorrent.getNumFiles() + " " + word + ")");
		l.addStyleName(CSS_TORRENT_HEADER_SUB);
		vp.add(l);
		l = new Label("Added: " + (new Date(inTorrent.getAddedDate())).toString());
		l.addStyleName(CSS_TORRENT_HEADER_SUB);
		vp.add(l);
		this.add(vp);
		setCellHorizontalAlignment(vp, ALIGN_LEFT);
	}
	
}
