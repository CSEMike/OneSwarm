package edu.washington.cs.oneswarm.ui.gwt.rpc;

import com.google.gwt.user.client.rpc.IsSerializable;

public class PagedTorrentInfo implements IsSerializable {
    public TorrentInfo[] swarms = null;
    public String total_size = null;
    public int total_swarms = 0;
    public int filtered_count = 0;
    public int by_type_count = 0;

    public FileTree tags = null;
    public boolean truncated_tags = false;
}
