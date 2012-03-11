/*
 * TorrentDownloaderManagerImpl.java
 *
 * Created on 2. November 2003, 04:29
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package org.gudy.azureus2.core3.torrentdownloader.impl;

import java.util.ArrayList;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderCallBackInterface;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderFactory;

/**
 *
 * @author  Tobias Minich
 */
public class TorrentDownloaderManager implements TorrentDownloaderCallBackInterface {
    
    private static TorrentDownloaderManager man = null;
    
    private boolean logged = false;
    private boolean autostart = false;
    private GlobalManager gm = null;
    private String downloaddir;
    //private String error;
    private ArrayList running = new ArrayList();
    private ArrayList queued = new ArrayList();
    private ArrayList errors = new ArrayList();
    
    public TorrentDownloaderManager() {
        try {
            downloaddir = COConfigurationManager.getDirectoryParameter("Default save path");
        } catch (Exception e) {
            //this.error = e.getMessage();
            downloaddir = null;
        }
    }
    
    public static TorrentDownloaderManager getInstance() {
        if (man==null)
            man = new TorrentDownloaderManager();
        return man;
    }
    
    public void init(GlobalManager _gm, boolean _logged, boolean _autostart, String _downloaddir) {
        this.gm = _gm;
        this.logged = _logged;
        this.autostart = _autostart;
        if (_downloaddir != null)
            this.downloaddir = _downloaddir;
    }
    
    public TorrentDownloader add(TorrentDownloader dl) {
        if (dl.getDownloadState()==TorrentDownloader.STATE_ERROR)
            this.errors.add(dl);
        else if (this.running.contains(dl) || this.queued.contains(dl)) {
            ((TorrentDownloaderImpl) dl).setDownloadState(TorrentDownloader.STATE_DUPLICATE);
            ((TorrentDownloaderImpl) dl).notifyListener();
            this.errors.add(dl);
        } else if (this.autostart) {
            dl.start();
        } else
            this.queued.add(dl);
        return dl;
    }
    
    public TorrentDownloader download(String url, String fileordir, boolean logged) {
        return add(TorrentDownloaderFactory.create(this, url, null, fileordir, logged));
    }
    
    public TorrentDownloader download(String url, boolean logged) {
        return add(TorrentDownloaderFactory.create(this, url, null, null, logged));
    }
    
    public TorrentDownloader download(String url, String fileordir) {
        return add(TorrentDownloaderFactory.create(this, url, null, fileordir, this.logged));
    }
    
    public TorrentDownloader download(String url) {
        return add(TorrentDownloaderFactory.create(this, url, this.logged));
    }
    
    public void TorrentDownloaderEvent(int state, org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader inf) {
        switch(state) {
            case TorrentDownloader.STATE_START:
                if (this.queued.contains(inf))
                    this.queued.remove(inf);
                if (!this.running.contains(inf))
                    this.running.add(inf);
                break;
            case TorrentDownloader.STATE_FINISHED:
                remove(inf);
                if ((gm != null) && (downloaddir != null)) {
                    gm.addDownloadManager(inf.getFile().getAbsolutePath(), downloaddir);
                }
                break;
            case TorrentDownloader.STATE_ERROR:
                remove(inf);
                this.errors.add(inf);
                break;
        }
    }

	/**
	 * @param inf
	 */
	public void remove(TorrentDownloader inf) {
		if (this.running.contains(inf))
		    this.running.remove(inf);
		if (this.queued.contains(inf))
		    this.queued.remove(inf);
	}
    
}
