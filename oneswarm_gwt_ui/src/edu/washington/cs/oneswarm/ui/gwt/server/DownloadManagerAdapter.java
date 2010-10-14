package edu.washington.cs.oneswarm.ui.gwt.server;

import java.io.File;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerActivationListener;
import org.gudy.azureus2.core3.download.DownloadManagerDiskListener;
import org.gudy.azureus2.core3.download.DownloadManagerException;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.download.DownloadManagerPeerListener;
import org.gudy.azureus2.core3.download.DownloadManagerPieceListener;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.download.DownloadManagerStateListener;
import org.gudy.azureus2.core3.download.DownloadManagerStats;
import org.gudy.azureus2.core3.download.DownloadManagerTrackerListener;
import org.gudy.azureus2.core3.download.ForceRecheckListener;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentAnnounceURLGroup;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.plugins.download.DownloadAnnounceResult;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;

import com.aelitis.azureus.core.networkmanager.LimitedRateGroup;
import com.aelitis.azureus.core.util.CaseSensitiveFileMap;

import edu.washington.cs.oneswarm.f2f.FileCollection;
import edu.washington.cs.oneswarm.f2f.FileListFile;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.Strings;

public class DownloadManagerAdapter implements DownloadManager {
	
	FileCollection mCollection = null;
	int mFriendID;
	String nick = null;
	
	public String getFriendNick() { 
		return nick;
	}
	
	public int getFriendID() {
		return mFriendID;
	}
	
	public FileCollection getCollection() {
		return mCollection;
	}
	
	class StateAdapter implements DownloadManagerState
	{
		public void addListener(DownloadManagerStateListener l) {
		}

		public void clearFileLinks() {
		}

		public void clearResumeData() {
		}

		public void clearTrackerResponseCache() {
		}

		public void delete() {
		}

		public void discardFluff() {
		}

		public void generateEvidence(IndentWriter writer) {
		}

		public String getAttribute(String name) {
			return null;
		}

		public boolean getBooleanAttribute(String name) {
			return false;
		}

		public boolean getBooleanParameter(String name) {
			return false;
		}

		public Category getCategory() {
			return null;
		}

		public String getDisplayName() {
			return mCollection.getName();
		}

		public DownloadManager getDownloadManager() {
			return null;
		}

		public File getFileLink(File link_source) {
			return null;
		}

		public CaseSensitiveFileMap getFileLinks() {
			return null;
		}

		public boolean getFlag(long flag) {
			return false;
		}

		public int getIntAttribute(String name) {
			return 0;
		}

		public int getIntParameter(String name) {
			return 0;
		}

		public String[] getListAttribute(String name) {
			List<String> outTags = new LinkedList<String>();
			if( name.equals(FileCollection.ONESWARM_TAGS_ATTRIBUTE) ) {
				if( mCollection.getDirectoryTags() == null ) {
					return null;
				}
				StringBuilder sb = null;
				for( List<String> tag : mCollection.getDirectoryTags() ) {
					sb = new StringBuilder();
					for( String entry : tag ) {
						sb.append(entry + "/");
					}
					outTags.add(sb.toString().substring(0, sb.length()-1)); // get rid of trailling '/'
				}
				if( outTags.size() > 0 ) {
					return outTags.toArray(new String[0]);
				}
			}
			
			return null;
		}

		public long getLongAttribute(String name) {
			return 0;
		}

		public long getLongParameter(String name) {
			if( name.equals(DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME) )
			{
				return mCollection.getAddedTimeUTC();
			}
			
			System.err.println("unsupported getLongParameter: " + name);
			return -1;
		}

		public Map getMapAttribute(String name) {
			return null;
		}

		public String[] getNetworks() {
			return null;
		}

		public String[] getPeerSources() {
			return null;
		}

		public String getPrimaryFile() {
			return null;
		}

		public String getRelativeSavePath() {
			return null;
		}

		public Map getResumeData() {
			return null;
		}

		public File getStateFile(String name) {
			return null;
		}

		public TOTorrent getTorrent() {
			return torrentAdapter;
		}

		public String getTrackerClientExtensions() {
			return null;
		}

		public Map getTrackerResponseCache() {
			return null;
		}

		public String getUserComment() {
			return null;
		}

		public boolean hasAttribute(String name) {
			return false;
		}

		public boolean isNetworkEnabled(String network) {
			return false;
		}

		public boolean isOurContent() {
			return false;
		}

		public boolean isPeerSourceEnabled(String peerSource) {
			return false;
		}

		public boolean isPeerSourcePermitted(String peerSource) {
			return false;
		}

		public boolean isResumeDataComplete() {
			return false;
		}

		public boolean parameterExists(String name) {
			return false;
		}

		public void removeListener(DownloadManagerStateListener l) {
		}

		public void save() {
		}

		public void setActive(boolean active) {
		}

		public void setAttribute(String name, String value) {
		}

		public void setBooleanAttribute(String name, boolean value) {
		}

		public void setBooleanParameter(String name, boolean value) {
		}

		public void setCategory(Category cat) {
		}

		public void setDisplayName(String name) {
		}

		public void setFileLink(File link_source, File link_destination) {
		}

		public void setFlag(long flag, boolean set) {
		}

		public void setIntAttribute(String name, int value) {
		}

		public void setIntParameter(String name, int value) {
		}

		public void setListAttribute(String name, String[] values) {
		}

		public void setLongAttribute(String name, long value) {
		}

		public void setLongParameter(String name, long value) {
		}

		public void setMapAttribute(String name, Map value) {
		}

		public void setNetworkEnabled(String network, boolean enabled) {
		}

		public void setNetworks(String[] networks) {
		}

		public void setParameterDefault(String name) {
		}

		public void setPeerSourceEnabled(String source, boolean enabled) {
		}

		public void setPeerSourcePermitted(String peerSource, boolean permitted) {
		}

		public void setPeerSources(String[] sources) {
		}

		public void setPrimaryFile(String fileFullPath) {
		}

		public void setRelativeSavePath(String path) {
		}

		public void setResumeData(Map data) {
		}

		public void setTrackerClientExtensions(String value) {
		}

		public void setTrackerResponseCache(Map value) {
		}

		public void setUserComment(String name) {
		}

		public void supressStateSave(boolean supress) {
		}
		
	}
	
	StateAdapter stateAdapter = null;
	TorrentAdapter torrentAdapter = null;
	
	class TorrentAdapter implements TOTorrent
	{
		long mSize = 0;
		
		public TorrentAdapter()
		{
			if( mCollection.getChildren() == null )
			{
				System.err.println("collection has no children...");
				mSize = 0;
			}
			else
			{
				for( FileListFile f : mCollection.getChildren() )
				{
					mSize += f.getLength();
				}
			}
		}
		
		public byte[] getAdditionalByteArrayProperty(String name) {
			return null;
		}

		public List getAdditionalListProperty(String name) {
			return null;
		}

		public Long getAdditionalLongProperty(String name) {
			return null;
		}

		public Map getAdditionalMapProperty(String name) {
			return null;
		}

		public Object getAdditionalProperty(String name) {
			return null;
		}

		public String getAdditionalStringProperty(String name) {
			return null;
		}

		public URL getAnnounceURL() {
			return null;
		}

		public TOTorrentAnnounceURLGroup getAnnounceURLGroup() {
			return null;
		}

		public byte[] getComment() {
			return null;
		}

		public byte[] getCreatedBy() {
			return null;
		}

		public long getCreationDate() {
			return 0;
		}
		
		class TOTorrentFileAdapter implements TOTorrentFile {
			public int getFirstPieceNumber() {
				return 0;
			}

			public int getLastPieceNumber() {
				return 0;
			}

			public long getLength() {
				return size;
			}

			public int getNumberOfPieces() {
				return 0;
			}

			public byte[][] getPathComponents() {
				return null;
			}

			public String getRelativePath() {
				return name;
			}

			public TOTorrent getTorrent() {
				return null;
			}
			
			public String name;
			public long size;
		};

		public TOTorrentFile[] getFiles() {
			TOTorrentFileAdapter [] out = new TOTorrentFileAdapter[mCollection.getFileNum()];
			List<FileListFile> kids = mCollection.getChildren();
			for( int i=0; i<out.length; i++ )
			{
				out[i] = new TOTorrentFileAdapter();
				out[i].name = kids.get(i).getFileName();
				out[i].size = kids.get(i).getLength();
			}
			return out;
		}

		public byte[] getHash() throws TOTorrentException {
			return Base64.decode(mCollection.getUniqueID());
		}

		public HashWrapper getHashWrapper() throws TOTorrentException {
			return null;
		}

		public AEMonitor getMonitor() {
			return null;
		}

		public byte[] getName() {
			return mCollection.getName().getBytes();
		}

		public int getNumberOfPieces() {
			return 0;
		}

		public long getPieceLength() {
			return 0;
		}

		public byte[][] getPieces() throws TOTorrentException {
			return null;
		}

		public boolean getPrivate() {
			return false;
		}

		public long getSize() {
			return mSize;
		}

		public boolean hasSameHashAs(TOTorrent other) {
			return false;
		}

		public boolean isCreated() {
			return false;
		}

		public boolean isSimpleTorrent() {
			return false;
		}

		public void print() {
		}

		public void removeAdditionalProperties() {
		}

		public void removeAdditionalProperty(String name) {
		}

		public void serialiseToBEncodedFile(File file) throws TOTorrentException {
		}

		public Map serialiseToMap() throws TOTorrentException {
			return null;
		}

		public void serialiseToXMLFile(File file) throws TOTorrentException {
		}

		public void setAdditionalByteArrayProperty(String name, byte[] value) {
		}

		public void setAdditionalListProperty(String name, List value) {
		}

		public void setAdditionalLongProperty(String name, Long value) {
		}

		public void setAdditionalMapProperty(String name, Map value) {
		}

		public void setAdditionalProperty(String name, Object value) {
		}

		public void setAdditionalStringProperty(String name, String value) {
		}

		public boolean setAnnounceURL(URL url) {
			return false;
		}

		public void setComment(String comment) {
		}

		public void setCreationDate(long date) {
		}

		public void setPieces(byte[][] pieces) throws TOTorrentException {
		}

		public void setPrivate(boolean _private) throws TOTorrentException {
		}
		
	}
	
	public DownloadManagerAdapter( FileCollection inCollection, int inFriendID, String nick )
	{
		mCollection = inCollection;
		stateAdapter = new StateAdapter();
		torrentAdapter = new TorrentAdapter();
		mFriendID = inFriendID;
		this.nick = nick;
	}

	public void addActivationListener(DownloadManagerActivationListener listener) {
	}

	public void addDiskListener(DownloadManagerDiskListener listener) {
	}

	public void addListener(DownloadManagerListener listener) {
	}

	public void addPeerListener(DownloadManagerPeerListener listener) {
	}

	public void addPeerListener(DownloadManagerPeerListener listener, boolean dispatchForExisting) {
	}

	public void addPieceListener(DownloadManagerPieceListener listener) {
	}

	public void addPieceListener(DownloadManagerPieceListener listener, boolean dispatchForExisting) {
	}

	public void addRateLimiter(LimitedRateGroup group, boolean upload) {
	}

	public void addTrackerListener(DownloadManagerTrackerListener listener) {
	}

	public File[] calculateDefaultPaths(boolean for_moving) {
		return null;
	}

	public boolean canForceRecheck() {
		return false;
	}

	public void destroy(boolean is_duplicate) {
	}

	public boolean filesExist() {
		return true;
	}

	public void forceRecheck() {
	}

	public void forceRecheck(ForceRecheckListener l) {
	}

	public void generateEvidence(IndentWriter writer) {
	}

	public File getAbsoluteSaveLocation() {
		return null;
	}

	public int getActivationCount() {
		return 0;
	}

	public boolean getAssumedComplete() {
		return false;
	}

	public long getCreationTime() {
		return 0;
	}

	public int getCryptoLevel() {
		return 0;
	}

	public PEPeer[] getCurrentPeers() {
		return null;
	}

	public PEPiece[] getCurrentPieces() {
		return null;
	}

	public Object getData(String key) {
		return null;
	}

	public DiskManager getDiskManager() {
		return null;
	}

	public DiskManagerFileInfo[] getDiskManagerFileInfo() {
		return null;
	}

	public String getDisplayName() {
		return mCollection.getName();
	}

	public DownloadManagerState getDownloadState() {
		return stateAdapter;
	}

	public int getEffectiveMaxUploads() {
		return 0;
	}

	public int getEffectiveUploadRateLimitBytesPerSecond() {
		return 0;
	}

	public String getErrorDetails() {
		return null;
	}

	public GlobalManager getGlobalManager() {
		return null;
	}

	public int getHealthStatus() {
		return 0;
	}

	public String getInternalName() {
		return null;
	}

	public int getMaxUploads() {
		return 0;
	}

	public int getNATStatus() {
		return 0;
	}

	public int getNbPeers() {
		return 0;
	}

	public int getNbPieces() {
		return 0;
	}

	public int getNbSeeds() {
		return 0;
	}

	public PEPeerManager getPeerManager() {
		return null;
	}

	public String getPieceLength() {
		return null;
	}

	public int getPosition() {
		return 0;
	}

	public File getSaveLocation() {
		return null;
	}

	public int getSeedingRank() {
		return 0;
	}

	public long getSize() {
		return torrentAdapter.getSize();
	}

	public int getState() {
		return DownloadManager.STATE_QUEUED;
	}

	public DownloadManagerStats getStats() {
		return null;
	}

	public int[] getStorageType(DiskManagerFileInfo[] infos) {
		return null;
	}

	public int getSubState() {
		return 0;
	}

	public TOTorrent getTorrent() {
		return torrentAdapter;
	}

	public String getTorrentComment() {
		return null;
	}

	public String getTorrentCreatedBy() {
		return null;
	}

	public long getTorrentCreationDate() {
		return 0;
	}

	public String getTorrentFileName() {
		return null;
	}

	public TRTrackerAnnouncer getTrackerClient() {
		return null;
	}

	public TRTrackerScraperResponse getTrackerScrapeResponse() {
		return null;
	}

	public String getTrackerStatus() {
		return null;
	}

	public int getTrackerTime() {
		return 0;
	}

	public void initialize() {
	}

	public boolean isDataAlreadyAllocated() {
		return false;
	}

	public boolean isDestroyed() {
		return false;
	}

	public boolean isDownloadComplete(boolean includingDND) {
		return false;
	}

	public boolean isExtendedMessagingEnabled() {
		return false;
	}

	public boolean isForceStart() {
		return false;
	}

	public boolean isInDefaultSaveDir() {
		return false;
	}

	public boolean isPaused() {
		return false;
	}

	public boolean isPersistent() {
		return false;
	}

	public void moveDataFiles(File new_parent_dir) throws DownloadManagerException {
	}

	public void moveDataFiles(File new_parent_dir, String new_name) throws DownloadManagerException {
	}

	public void moveTorrentFile(File new_parent_dir) throws DownloadManagerException {
	}

	public boolean pause() {
		return false;
	}

	public void recheckFile(DiskManagerFileInfo file) {
	}

	public void removeActivationListener(DownloadManagerActivationListener listener) {
	}

	public void removeDiskListener(DownloadManagerDiskListener listener) {
	}

	public void removeListener(DownloadManagerListener listener) {
	}

	public void removePeerListener(DownloadManagerPeerListener listener) {
	}

	public void removePieceListener(DownloadManagerPieceListener listener) {
	}

	public void removeRateLimiter(LimitedRateGroup group, boolean upload) {
	}

	public void removeTrackerListener(DownloadManagerTrackerListener listener) {
	}

	public void renameDownload(String new_name) throws DownloadManagerException {
	}

	public boolean requestAssumedCompleteMode() {
		return false;
	}

	public void requestTrackerAnnounce(boolean immediate) {
	}

	public void requestTrackerScrape(boolean immediate) {
	}

	public void resetFile(DiskManagerFileInfo file) {
	}

	public void resume() {
	}

	public void saveDownload() {
	}

	public void saveResumeData() {
	}

	public boolean seedPieceRecheck() {
		return false;
	}

	public void setAZMessagingEnabled(boolean enable) {
	}

	public void setAnnounceResult(DownloadAnnounceResult result) {
	}

	public void setCreationTime(long t) {
	}

	public void setCryptoLevel(int level) {
	}

	public void setData(String key, Object value) {
	}

	public void setDataAlreadyAllocated(boolean already_allocated) {
	}

	public void setForceStart(boolean forceStart) {
	}

	public void setMaxUploads(int max_slots) {
	}

	public void setPieceCheckingEnabled(boolean enabled) {
	}

	public void setPosition(int newPosition) {
	}

	public void setScrapeResult(DownloadScrapeResult result) {
	}

	public void setSeedingRank(int rank) {
	}

	public void setStateQueued() {
	}

	public void setStateWaiting() {
	}

	public void setTorrentFileName(String string) {
	}

	public void setTorrentSaveDir(String path) {
	}

	public void setTorrentSaveDir(String parent_dir, String dl_name) {
	}

	public void setTrackerScrapeResponse(TRTrackerScraperResponse response) {
	}

	public void startDownload() {
	}

	public void stopIt(int stateAfterStopping, boolean remove_torrent, boolean remove_data) {
	}
}
