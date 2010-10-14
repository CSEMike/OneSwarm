package edu.washington.cs.oneswarm.ui.gwt.server.community;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentAnnounceURLSet;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.torrent.Torrent;

import com.aelitis.azureus.core.impl.AzureusCoreImpl;

import edu.washington.cs.oneswarm.ui.gwt.CoreInterface;
import edu.washington.cs.oneswarm.ui.gwt.rpc.BackendTask;
import edu.washington.cs.oneswarm.ui.gwt.rpc.CommunityRecord;
import edu.washington.cs.oneswarm.ui.gwt.rpc.TorrentInfo;
import edu.washington.cs.oneswarm.ui.gwt.server.BackendErrorLog;
import edu.washington.cs.oneswarm.ui.gwt.server.BackendTaskManager;
import edu.washington.cs.oneswarm.ui.gwt.server.OneSwarmHashUtils;
import edu.washington.cs.oneswarm.ui.gwt.server.PreviewImageGenerator;

public class PublishSwarmsThread extends Thread {

	private static Logger logger = Logger.getLogger(PublishSwarmsThread.class.getName());
	
	private CommunityRecord toServer;
	private String[] previewPaths;
	private TorrentInfo[] infos;
	private boolean cancelled = false;
	private BackendTask task = null;
	private String[] comments;

	private CoreInterface coreInterface;

	private String[] categories;

	public PublishSwarmsThread(CoreInterface coreInterface, TorrentInfo[] infos, String[] previewPaths, String[] comments, String[] categories, CommunityRecord toServer) {
		this.coreInterface = coreInterface;
		this.infos = infos;
		this.previewPaths = previewPaths;
		this.toServer = toServer;
		this.comments = comments;
		this.categories = categories;
	}

	public void cancel() {
		cancelled = true;
	}

	static class ConversionException extends IOException {
		public ConversionException( String s ) { super(s); }
	}
	
	public void run() {

		if (task != null) {
			task.setSummary("Preparing...");
		}

		try {
			
			/**
			 * First convert all the preview images to max 800x800 (also makes sure
			 * they are all image data!)
			 */
			List<File> converted = new ArrayList<File>();
			for (int pathItr=0; pathItr<previewPaths.length; pathItr++ ) {
				String path = previewPaths[pathItr];
				
				if (cancelled) {
					return;
				}
				File scratchFile = null;
				FileOutputStream scratch = null;
				
				try {
					if( path != null && path.length() > 0 ) {
						scratchFile = File.createTempFile("osul", "png");
						scratchFile.deleteOnExit();
						scratch = new FileOutputStream(scratchFile);
						PreviewImageGenerator.previewImageForArbitraryPath(path, 800, scratch);
						scratch.flush();
						scratch.close();
					} else { 
						Download download = coreInterface.getDownload(infos[pathItr].getTorrentID());
						if (download == null) {
							scratchFile = null;
							throw new ConversionException("null download");
						}
	
						try {
							
							scratchFile = coreInterface.getImageFile(download);
						} catch( Exception e ) {
							logger.warning("Error during preview grab during publish: " + e.toString());
							throw new ConversionException("Error during preview grab: " + e.toString());
						}
						
						if( scratchFile == null ) { 
							throw new ConversionException("null scratchFile");
						}
						
						if( scratchFile.exists() == false ) {
							scratchFile = null;
							throw new ConversionException("scratchFile doesn't exist");
						}
					}
				} catch( ConversionException e ) { 
					logger.warning("Conversion exception: " + e.toString());
				}
				
				converted.add(scratchFile);
			}
			
			/**
			 * Now submit form requests for each
			 */
			for( int swarmItr=0; swarmItr<infos.length; swarmItr++ ) { 
				if (cancelled) {
					return;
				}
				
				TorrentInfo swarm = infos[swarmItr];
				File previewFile = converted.get(swarmItr);
				String commentStr = comments[swarmItr];
				String categoryStr = categories[swarmItr];
				
				/**
				 * Next, assemble the form post, comprised of the torrents, previews,
				 * and comments
				 */
//				DefaultHttpClient httpclient = new DefaultHttpClient();
//				
//				httpclient.getParams().setParameter(HttpProtocolParams.USER_AGENT, Constants.AZUREUS_NAME + "/" + Constants.AZUREUS_VERSION);
				
//				if( toServer.isAuth_required() ) { 
//					final Credentials creds = new UsernamePasswordCredentials(toServer.getUsername(), toServer.getPw());
//					httpclient.setCredentialsProvider(new CredentialsProvider(){
//						public void clear() {}
//						public Credentials getCredentials(AuthScope arg0) {
//							return creds;
//						}
//						public void setCredentials(AuthScope arg0, Credentials arg1) {}});
//				}
				
				String theURLString = CommunityServerRequest.getCommunityBase(new URL(toServer.getUrl())) + "/" + toServer.getSupports_publish();
				HttpPost httppost = new HttpPost(theURLString);
				MultipartEntity reqEntity = new MultipartEntity();
				
				if( commentStr != null ) {
					StringBody comment = new StringBody(commentStr);
					reqEntity.addPart("commentstr", comment);
				}
				
				if( categoryStr != null ) { 
					StringBody category = new StringBody(categoryStr);
					reqEntity.addPart("categorystr", category);
				}
				
				DownloadManager dm = AzureusCoreImpl.getSingleton().getGlobalManager().getDownloadManager(new HashWrapper(
						OneSwarmHashUtils.bytesFromOneSwarmHash(swarm.getTorrentID())));
				
				File scratchFile = File.createTempFile("osul", ".torrent");
				FileOutputStream scratch = new FileOutputStream(scratchFile);
				serializeForPublishing(dm.getTorrent(), scratch);
				
				logger.finer("Torrent path: " + scratchFile.getAbsolutePath());
				
				FileBody bin = new FileBody(new File(scratchFile.getAbsolutePath()));
				reqEntity.addPart("torrentbin", bin);
				
				if( previewFile != null ) { 
					bin = new FileBody(previewFile);
					reqEntity.addPart("previewpng", bin);
				}
				
				httppost.setEntity(reqEntity);
				
				logger.fine("executing request " + httppost.getRequestLine());
				if( task != null ) { 
					task.setProgress((swarmItr+1) + "/" + infos.length);
					task.setSummary("Uploading... ");
				}
				
//				HttpResponse response = httpclient.execute(httppost);
//				HttpEntity resEntity = response.getEntity();
				
				/**
				 * We need to use HttpURLConnection instead of HttpClient to ensure the use of our SSL
				 * security handlers, which are adapted to accept community-server style certificates. 
				 */
				
				URL url = new URL(theURLString);
				
				Map<String, String> extraHeaders = new HashMap<String, String>();
				/**
				 * Transmoprh this connection to line up with the post.
				 */
				extraHeaders.put("Content-Type", httppost.getEntity().getContentType().getValue());
				extraHeaders.put("Content-Length", httppost.getEntity().getContentLength()+"");
				
				HttpURLConnection conn = CommunityServerRequest.getConnection(url, "POST", toServer.isAuth_required(), toServer, extraHeaders);
				
				
				/**
				 * Copy everything
				 */
				httppost.getEntity().writeTo(conn.getOutputStream());
				
				if( conn.getResponseCode() == HttpServletResponse.SC_UNAUTHORIZED ) {
					StringBuilder err = new StringBuilder();
					err.append("Server does not permit publishing from this account.");
					if( toServer.isAuth_required() == false ) { 
						err.append(" Many community servers require an account to publish. If you have an account, enter your username and password in the settings. ");
					} else { 
						err.append(" Check that you have a valid account with the server and that you have entered your username and password correctly. ");
					}
					err.append("\r\n\r\n (" + (toServer.getServer_name() == null ? toServer.getUrl() : toServer.getServer_name()) + ")");
					throw new IOException(err.toString());
				}
				
				if( conn.getResponseCode() != HttpServletResponse.SC_OK ) { 
					throw new IOException("Server responded with bad status code: " + conn.getResponseCode() + 
							(conn.getResponseCode() == HttpServletResponse.SC_CONFLICT ? 
									" Duplicate entry -- this file has already been published at this community server" : "") );
				}
			}
			
			task.setSummary("Publish complete. " + infos.length);
			task.setShortname("Publish complete.");
			logger.finer("Publish complete: " + infos.length);
			Thread.sleep(7*1000);
			BackendTaskManager.get().removeTask(task.getTaskID());
			logger.finest("Removed task");
			
			BackendErrorLog.get().logString("Publish succeeded. Note that your swarm will not appear immediately on community servers that require moderation.", false);
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.warning(e.toString());
			if (task != null) {
				task.setGood(false);
				task.setProgress("100");
				task.setSummary("An error occurred: " + e.toString());
				try {
					Thread.sleep(7 * 1000);
				} catch (Exception e2) {
				}
				BackendTaskManager.get().removeTask(task.getTaskID());
				
				BackendErrorLog.get().logString("Publishing failed: " + e.getMessage(), false);
			}
		}
	}

	private static void serializeForPublishing(TOTorrent torrent, OutputStream scratch) throws IOException, TOTorrentException {
		TOTorrent clone = TOTorrentFactory.deserialiseFromMap(torrent.serialiseToMap());
		if (clone.getPrivate()) {
			String replaceWithUrl = "http://tracker.removed.invalid/announce";
			clone.setAnnounceURL(new URL(replaceWithUrl));
			clone.getAnnounceURLGroup().setAnnounceURLSets(new TOTorrentAnnounceURLSet[0]);
		}
		clone.removeAdditionalProperties();
		Map root = clone.serialiseToMap();
		scratch.write(BEncoder.encode(root));
	}

	public void associateTask(BackendTask task) {
		this.task = task;
	}
}
