package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentException;

import com.aelitis.azureus.core.impl.AzureusCoreImpl;

import edu.washington.cs.oneswarm.f2f.share.DownloadManagerStarter;
import edu.washington.cs.oneswarm.f2f.share.DownloadManagerStarter.DownloadManagerStartListener;
import edu.washington.cs.oneswarm.ui.gwt.CoreInterface;
import edu.washington.cs.oneswarm.ui.gwt.RequiresShutdown;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants.InOrderType;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.FFMpegException.ErrorType;
import edu.washington.cs.oneswarm.ui.gwt.server.handlers.FileHandler;
import edu.washington.cs.oneswarm.ui.gwt.server.handlers.SharedFileHandler;

public class FFMpegWrapper implements RequiresShutdown {

	private static FFMpegException lastFFMpegException = null;
	private final static int AUDIO_RATE_DEFAULT = 128 * 1000;

	private final static int AUDIO_RATE_LOWER = 64 * 1000;

	private final static int AUDIO_RATE_LOWEST = 32 * 1000;

	private final static int AUDIO_RATE_MAX = 192 * 1000;

	public final static int CHUNK_SIZE = 1024;

	public final static int ENCODE_BUFFER = 10000;

	private final static int FFMPEG_UNITS = 1;

	/**
	 * This class functions as a buffer between the input stream feed to FFMpeg,
	 * and a slower source (like disk or http). This avoids having ffmpeg wait
	 * for more data
	 * 
	 * @author isdal
	 * 
	 */
	private static Logger logger = Logger.getLogger(FFMpegWrapper.class.getName());

	public static boolean logToStdOut = true;

	private final static double STREAM_MAX_RATE = 1.15;
	/*
	 * don't set the video rate to the full upload capacity setting this
	 * parameter set the upload rate to be the full upload capacity, but the
	 * video rate will be slightly lower to make the movie buffer a bit
	 */
	private static final float REMOTE_ACCESS_RATE_MARGIN = 0.85f;
	/*
	 * leave a constant amount of head room as well, (updating the ui takes
	 * 1-3KB/s)
	 */
	private static final int STREAM_MAX_USE_UPLOAD_LEAVE_KBPS = 5;

	private final static float STREAM_RATE_ERROR_MARGIN = 1.25f;

	// private OsgwtuiMain parent;

	/*
	 * 128kbit/s audio is 16KB/s if upload limit is less than 4x that, decrease
	 * audio quality
	 * 
	 * if upload limit is less than 4x lower, decrease to lowest
	 */
	private final static int UPLOAD_CAP_TO_TRIGGER_LOWER_AUDIO_KBps = 64;
	private final static int UPLOAD_CAP_TO_TRIGGER_LOWEST_AUDIO_KBps = 32;
	private final static int VIDEO_RATE_DEFAULT = 1000 * 1000;
	private final static int VIDEO_RATE_MAX = 1500 * 1000;
	private final static int VIDEO_RATE_MIN = 64 * 8 * 1000;

	private final int audiorate;

	Process converter = null;

	private final CoreInterface coreInterface;

	private final Download download;

	private final DiskManagerFileInfo fileInfo;

	// private final File imageFile;

	private boolean quit = false;

	private final boolean remoteAccess;

	private final int streamByteRate;

	private final Torrent torrent;

	private final int videorate;
	private final double startAtSecond;

	public FFMpegWrapper(CoreInterface coreInterface, DiskManagerFileInfo sourceFile,
			Download download, boolean remote, double startAtByte) throws TorrentException {
		this.remoteAccess = remote;
		this.fileInfo = sourceFile;
		this.download = download;
		this.torrent = download.getTorrent();
		// this.imageFile = coreInterface.getImageFile(torrent);
		this.coreInterface = coreInterface;
		this.audiorate = getAudioBitRate();
		this.videorate = getVideoBitRate(audiorate);
		this.streamByteRate = (videorate + audiorate) / 8;
		this.startAtSecond = startAtByte / streamByteRate;

		coreInterface.addShutdownObject(this);
	}

	/**
	 * for testing
	 * 
	 * @throws FFMpegException
	 */
	private FFMpegWrapper(File file) throws FFMpegException {

		this.remoteAccess = false;
		this.fileInfo = null;
		this.download = null;
		this.torrent = null;
		// this.imageFile = null;
		this.coreInterface = null;
		this.audiorate = getAudioBitRate();
		this.videorate = getVideoBitRate(audiorate);
		this.streamByteRate = (videorate + audiorate) / 8;
		this.startAtSecond = 0;

	}

	private void createNewFileConverter(File sourceFile, OutputStream destinationStream,
			MovieStreamInfo movieInfo) throws FFMpegException, InterruptedException {

		if (!sourceFile.exists()) {
			throw new FFMpegException(FFMpegException.ErrorType.FILE_NOT_FOUND,
					"File does not exist");
		}

		String sourceFileName;
		try {
			sourceFileName = sourceFile.getCanonicalPath();
		} catch (IOException e) {
			throw new FFMpegException(FFMpegException.ErrorType.FILE_NOT_FOUND,
					"Problem reading file", e);
		}
		logger.fine("using file: '" + sourceFileName + "'");

		String[] mpegExecArray = getFFMpegExecArray(sourceFileName, movieInfo);
		try {
			converter = new ProcessBuilder(mpegExecArray).start();
		} catch (IOException e) {
			FFMpegException mpegException = new FFMpegException(
					FFMpegException.ErrorType.FFMPEG_BIN_ERROR, "Unable to create FFMpeg process '"
							+ mpegExecArray + "'", e);
			mpegException.setFFMpegExecArray(mpegExecArray);
			throw mpegException;
		}

		// make sure to dump anything showing up on stderr
		boolean showError = false;
		int bytesToStore = 5000;
		final StreamDumper streamDumper = new StreamDumper(converter.getErrorStream(),
				bytesToStore, showError);

		InputStream ffmpegOutput = converter.getInputStream();

		FlvOutputBufferManager flvTool2ToWebBufferHandler = new FlvOutputBufferManager(
				ffmpegOutput, destinationStream, audiorate, videorate, false, movieInfo,
				startAtSecond);

		Thread t = new Thread(flvTool2ToWebBufferHandler);
		t.setName("BufferHandler");
		t.setDaemon(true);
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.fine("ffmpeg to web handler finished");

		if (converter != null) {
			converter.destroy();
			int converterExitVal = converter.waitFor();
			if (converterExitVal == 255) {
				logger.fine("ffmpeg got killed (exit 255)");
			} else if (converterExitVal != 0) {
				final FFMpegException mpegException = new FFMpegException(
						FFMpegException.ErrorType.FORMAT_ERROR, converterExitVal,
						streamDumper.getStoredOutput());
				mpegException.setDataWritten(flvTool2ToWebBufferHandler.getTotal());
				mpegException.setFFMpegExecArray(mpegExecArray);
				lastFFMpegException = mpegException;
				throw mpegException;
			}
		}
	}

	private void createNewStreamConverter(InputStream sourceStream,
			ServletOutputStream destinationStream, MovieStreamInfo movieInfo)
			throws FFMpegException, InterruptedException {
		String[] mpegExecArray = getFFMpegExecArray("-", movieInfo);
		try {
			converter = new ProcessBuilder(mpegExecArray).start();
		} catch (IOException e) {
			throw new FFMpegException(FFMpegException.ErrorType.FFMPEG_BIN_ERROR,
					"Unable to create FFMpeg process: '" + mpegExecArray + "'", e);
		}
		OutputStream ffmpegInput = converter.getOutputStream();

		// create a buffer for the input data, just to make sure that ffmpeg
		// never is waiting if it hasn't to
		DownloadBufferManager downloadStream = new DownloadBufferManager(sourceStream, fileInfo);

		// make sure to dump anything showing up on stderr
		boolean showError = false;
		int numBytesToSave = 5000;
		final StreamDumper streamDumper = new StreamDumper(converter.getErrorStream(),
				numBytesToSave, showError);

		// start the bufferhandler moving data from ffmpeg to the webserver

		InputStream ffmpegOutput = converter.getInputStream();
		FlvOutputBufferManager ffmpegToWebBufferHandler = new FlvOutputBufferManager(ffmpegOutput,
				destinationStream, audiorate, videorate, true, movieInfo, startAtSecond);

		int position = 0;

		byte[] buf = new byte[CHUNK_SIZE];

		int read = 0;

		// write as much as we can
		try {
			logger.fine("Starting to read");
			while ((read = downloadStream.read(buf)) != -1 && ffmpegToWebBufferHandler.isRunning()
					&& !quit) {
				ffmpegInput.write(buf, 0, read);
				position += read;
			}
		} catch (IOException e) {
			final FFMpegException mpegException = new FFMpegException(
					FFMpegException.ErrorType.FORMAT_ERROR_DOWNLOADING,
					"Error writing to ffmpeg (app closed??)", e);
			mpegException.setStdErr(streamDumper.getStoredOutput());
			mpegException.setFFMpegExecArray(mpegExecArray);
			throw mpegException;
		}

		logger.fine("Convertion Done");
		ffmpegToWebBufferHandler.quit();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {

		}

		try {
			ffmpegInput.close();
			ffmpegOutput.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// close the process in case of exceptions
		if (converter != null) {
			converter.destroy();
			int converterExitVal = converter.waitFor();
			if (converterExitVal == 255) {
				logger.fine("ffmpeg got killed (exit 255)");
			} else if (converterExitVal != 0) {
				final FFMpegException mpegException = new FFMpegException(
						FFMpegException.ErrorType.FORMAT_ERROR_DOWNLOADING, converterExitVal,
						streamDumper.getStoredOutput());
				mpegException.setDataWritten(ffmpegToWebBufferHandler.getTotal());
				lastFFMpegException = mpegException;
				mpegException.setFFMpegExecArray(mpegExecArray);
				throw mpegException;
			}
		}

	}

	private int getAudioBitRate() {
		int audioRate;
		if (remoteAccess) {
			/*
			 * we need to think a bit here, we want to scale the audio rate
			 * nicely so that audio won't use the entire stream
			 */
			int maxUpload = COConfigurationManager.getIntParameter("Max Upload Speed KBs");
			if (maxUpload <= 0) {
				return AUDIO_RATE_MAX;
			}
			if (maxUpload < UPLOAD_CAP_TO_TRIGGER_LOWEST_AUDIO_KBps) {
				audioRate = AUDIO_RATE_LOWEST;
			} else if (maxUpload < UPLOAD_CAP_TO_TRIGGER_LOWER_AUDIO_KBps) {
				audioRate = AUDIO_RATE_LOWER;
			} else {
				audioRate = AUDIO_RATE_DEFAULT;
			}
		} else {
			audioRate = AUDIO_RATE_DEFAULT;
		}
		logger.finer("calculated audio rate, returning: " + audioRate);
		return audioRate;
	}

	private String[] getFFMpegExecArray(String sourceFile, MovieStreamInfo movieInfo)
			throws FFMpegException {
		// set the maximum rate allowed
		int videoRate = (int) (videorate / FFMPEG_UNITS);
		int audioRate = (int) (audiorate / FFMPEG_UNITS);
		int maxRate = (int) (videorate * STREAM_MAX_RATE / FFMPEG_UNITS);
		// the buffer size to use to make sure that the video rate is below max
		// rate, 2 seconds should be enough
		int bufSize = videorate * 2 / FFMPEG_UNITS;

		// int cpuCores = Runtime.getRuntime().availableProcessors();
		// removing -re

		List<String> parameters = new LinkedList<String>();
		parameters.add(FFMpegTools.getFFMpegPath());

		// parameters.add("-threads");
		// parameters.add("" + Runtime.getRuntime().availableProcessors());

		if (startAtSecond > 0) {
			parameters.add("-ss");
			parameters.add("" + startAtSecond);
			logger.fine("seeking to " + startAtSecond + " s");
		}

		parameters.add("-i");
		parameters.add(sourceFile);

		parameters.add("-f");
		parameters.add("flv");

		/*
		 * add key frames every 50 frames
		 */
		parameters.add("-g");
		parameters.add("50");

		parameters.add("-b");
		parameters.add("" + videoRate);

		parameters.add("-maxrate");
		parameters.add("" + maxRate);

		parameters.add("-bufsize");
		parameters.add("" + bufSize);

		/*
		 * to make the conversion faster: scale down if res is larger than
		 * player width or player height
		 */
		if (movieInfo.getResolutionX() > OneSwarmConstants.DEFAULT_WEB_PLAYER_WIDTH
				|| movieInfo.getResolutionY() > OneSwarmConstants.DEFAULT_WEB_PLAYER_HEIGTH) {
			double resizeFactorWidth = movieInfo.getResolutionX()
					/ (double) OneSwarmConstants.DEFAULT_WEB_PLAYER_WIDTH;
			double resizeFactorHeight = movieInfo.getResolutionY()
					/ (double) OneSwarmConstants.DEFAULT_WEB_PLAYER_HEIGTH;
			double resizeFactor = Math.max(resizeFactorWidth, resizeFactorHeight);

			int newWidth = getResolution(movieInfo.getResolutionX(), resizeFactor);
			int newHeigth = getResolution(movieInfo.getResolutionY(), resizeFactor);
			parameters.add("-s");
			parameters.add(newWidth + "x" + newHeigth);
			logger.finer("reducing resolution to: " + newWidth + "x" + newHeigth);
		}

		parameters.add("-acodec");
		parameters.add("libmp3lame");

		parameters.add("-ac");
		parameters.add("2");

		parameters.add("-ar");
		parameters.add("44100");

		parameters.add("-ab");
		parameters.add("" + audioRate);

		if (movieInfo.isCropSet()
				&& (movieInfo.getCropTop() > 0 || movieInfo.getCropBottom() > 0
						|| movieInfo.getCropLeft() > 0 || movieInfo.getCropRight() > 0)) {
			parameters.add("-croptop");
			parameters.add("" + movieInfo.getCropTop());
			parameters.add("-cropbottom");
			parameters.add("" + movieInfo.getCropBottom());
			parameters.add("-cropleft");
			parameters.add("" + movieInfo.getCropLeft());
			parameters.add("-cropright");
			parameters.add("" + movieInfo.getCropRight());

		}

		parameters.add("-");
		StringBuilder cmd = new StringBuilder();
		for (String string : parameters) {
			cmd.append("'" + string + "' ");
		}
		logger.fine("starting ffmpeg, parameters: \"" + cmd.toString() + "\"");
		return parameters.toArray(new String[parameters.size()]);
	}

	private int getResolution(long oldRes, double resizeFactor) {
		double newRes = oldRes / resizeFactor;
		int newResInt = (int) Math.round(newRes);

		if (newResInt % 2 != 0) {
			/*
			 * many codec need res to be divisible by 2
			 */
			if (newResInt - newRes > 0) {
				newResInt--;
			} else {
				newResInt++;
			}
		}
		logger.finer("resizing video, new size=" + newResInt + " (exact=" + newRes + "0)");
		return newResInt;
	}

	private int getVideoBitRate(int aRate) {
		int videoRate;
		if (remoteAccess) {
			/*
			 * basically we want to use all available bandwidth, but not more
			 * than the MAX_RATE
			 */
			int maxUpload = COConfigurationManager.getIntParameter("Max Upload Speed KBs");
			if (maxUpload <= 0) {
				return VIDEO_RATE_MAX;
			}
			int maxUploadBps = maxUpload * 8 * 1024;

			int available = Math.round((maxUploadBps - aRate) * REMOTE_ACCESS_RATE_MARGIN)
					- (STREAM_MAX_USE_UPLOAD_LEAVE_KBPS * 1000 * 8);
			String msg = "video rate calc: maxupload=" + maxUpload + " maxUploadBps="
					+ maxUploadBps + " audio=" + aRate + " avilable=" + available;
			logger.finest(msg);
			System.err.println(msg);
			videoRate = Math.max(VIDEO_RATE_MIN, Math.min(VIDEO_RATE_MAX, available));
		} else {
			videoRate = COConfigurationManager.getIntParameter("OSGWT.flash bit rate",
					VIDEO_RATE_DEFAULT);
		}
		String msg = "calculated video rate, returning: " + videoRate;
		logger.finer(msg);
		System.err.println(msg);
		return videoRate;
	}

	private void handleFFMpegConvertFile(InOrderType type, HttpServletResponse response,
			HttpServletRequest request, MovieStreamInfo movieStreamInfo) throws IOException,
			FFMpegException, InterruptedException {

		response.setContentType(type.convertedMime);
		response.setStatus(HttpServletResponse.SC_OK);

		long contentLengthGuess = (long) Math.round(movieStreamInfo.getDuration() * streamByteRate
				* STREAM_RATE_ERROR_MARGIN);
		SharedFileHandler.setContentLength(response, contentLengthGuess);

		ServletOutputStream responseStream = response.getOutputStream();

		logger.fine("file download is complete, just passing path to FFMpeg");
		createNewFileConverter(fileInfo.getFile(), responseStream, movieStreamInfo);

	}

	private void handleFFMpegStream(InOrderType type, HttpServletResponse response,
			HttpServletRequest request, MovieStreamInfo movieStreamInfo, InputStream sourceStream)
			throws IOException, FFMpegException, InterruptedException {
		logger.fine("handleFFMpegStream");
		response.setContentType(type.convertedMime);
		response.setStatus(HttpServletResponse.SC_OK);
		final double length = movieStreamInfo.getDuration() * streamByteRate;
		SharedFileHandler.setContentLength(response, length);

		ServletOutputStream responseStream = response.getOutputStream();

		createNewStreamConverter(sourceStream, responseStream, movieStreamInfo);
	}

	public void process(HttpServletResponse response, HttpServletRequest request) {
		boolean downloadCompleted = false;
		try {
			downloadCompleted = fileInfo.getDownloaded() == fileInfo.getLength()
					&& fileInfo.getFile().exists();

			InOrderType type = InOrderType.getType(fileInfo.getFile().getName());
			if (type == null) {
				if (downloadCompleted) {
					throw new FFMpegException(ErrorType.FORMAT_ERROR, "Unknown media type");
				} else {
					throw new FFMpegException(ErrorType.FORMAT_ERROR_DOWNLOADING,
							"Unknown media type");
				}
			}

			if (!type.convertNeeded) {
				new SharedFileHandler(fileInfo, download).process(response, request);
			} else if (downloadCompleted) {
				/*
				 * we need to convert this, get the movie info so we can set the
				 * movie and http content length
				 */
				MovieStreamInfo movieStreamInfo = FFMpegTools.getMovieInfo(fileInfo.getDownload()
						.getTorrent().getHash(), fileInfo.getFile());
				if (movieStreamInfo.isFlashReady()) {
					new SharedFileHandler(fileInfo, download).process(response, request);
				} else {
					handleFFMpegConvertFile(type, response, request, movieStreamInfo);
				}
			} else {
				/*
				 * need to convert it and it is not yet downloaded
				 */
				MovieStreamInfo movieStreamInfo = waitForDownloadAndGetMovieInfo();
				InputStream sourceStream = download.getStats().getFileStream(fileInfo,
						movieStreamInfo.getBitRate());
				if (movieStreamInfo.isFlashReady()) {
					new SharedFileHandler(fileInfo, download).process(response, request);
				} else {
					handleFFMpegStream(type, response, request, movieStreamInfo, sourceStream);
				}
			}

		} catch (org.mortbay.jetty.EofException e) {
			logger.fine("connection closed");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FFMpegException e) {
			System.err.println("file='" + fileInfo.getFile() + "'");
			System.err.println(e.toString());
			try {
				if (e.getDataWritten() == 0) {
					System.err.println("sending error file instead");
					if (!downloadCompleted) {
						new FileHandler().handle(
								"/oneswarmgwt/images/format_error_downloading.flv", request,
								response, 0);
					} else {
						new FileHandler().handle("/oneswarmgwt/images/format_error.flv", request,
								response, 0);
					}
				} else {
					System.err.println("error even though some data written: " + e.toString()
							+ " / dataWritten: " + e.getDataWritten());
				}
				if (COConfigurationManager.getBooleanParameter("oneswarm.beta.updates")) {
					// BackendErrorLog.get().logString("\n" + e.toString());
				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (ServletException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DownloadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			shutdown();
			coreInterface.removeShutdownObject(this);
		}
	}

	public void shutdown() {
		if (converter != null) {
			converter.destroy();
		}
		this.quit = true;
	}

	private MovieStreamInfo waitForDownloadAndGetMovieInfo() throws InterruptedException,
			DownloadException, FFMpegException {
		// ok, file is not downloaded
		// check if the download is running
		final Semaphore s = new Semaphore(0);
		DownloadManagerStarter.startDownload(AzureusCoreImpl.getSingleton().getGlobalManager()
				.getDownloadManager(new HashWrapper(download.getTorrent().getHash())),
				new DownloadManagerStartListener() {
					public void downloadStarted() {
						s.release();
					}
				});
		s.acquire();
		logger.fine("file is downloading");

		while (!quit) {
			boolean done = download.getStats().isFirstLastMbDone(fileInfo, true);

			if (!done) {
				Thread.sleep(500);
				done = download.getStats().isFirstLastMbDone(fileInfo, false);
				System.out.println("waiting for download to complete enough to get video length");
			} else {
				break;
			}
		}

		MovieStreamInfo movieStreamInfo = FFMpegTools.getMovieInfo(fileInfo.getDownload()
				.getTorrent().getHash(), fileInfo.getFile());
		logger.fine("got movie info: " + movieStreamInfo.toString());

		return movieStreamInfo;
	}

	/**
	 * For testing
	 */
	// public static void main(String args[]) {
	// if (args.length != 1) {
	// System.err.println("Usage: FFMpegWrapper file");
	// System.exit(1);
	// }
	// File file = new File(args[0]);
	// if (!file.exists()) {
	// System.err.println("'" + file.getAbsolutePath() + "' not found");
	// System.exit(1);
	// }
	// if (file.isDirectory()) {
	// System.err.println("'" + file.getAbsolutePath() + "' is a folder");
	// System.exit(1);
	// }
	// try {
	// MovieStreamInfo movieStreamInfo = FFMpegTools.getMovieInfo(file);
	//
	// /*
	// * create the converted file
	// */
	// FFMpegWrapper mpegWrapper = new FFMpegWrapper(file);
	// File tempOutFile = new File("/tmp/ffmpegtest.flv");
	// BufferedOutputStream out = new BufferedOutputStream(new
	// FileOutputStream(tempOutFile));
	// mpegWrapper.createNewFileConverter(file, out, movieStreamInfo);
	//
	// /*
	// * and test
	// */
	// IOHelper ioh2 = new IOHelper(tempOutFile);
	// ioh2.setDebug(true);
	// FlvHeader flvh2 = new FlvHeader(ioh2);
	//
	// ParseMeta parm = new ParseMeta(ioh2);
	// parm.findMetaTag();
	// HashMap<String, Object> originalMetaData = parm.getMetaData();
	// System.out.println("\nBEGIN modified meta data:\n" +
	// EmbeddedData.prettyPrintData(originalMetaData) +
	// "\nEND modified meta data");
	//
	// // generate metadata
	// TagBroker tb = new TagBroker(ioh2, flvh2);
	// // MetaDataGen mdg = new MetaDataGen(tb, flvh2);
	// // mdg.buildOnLastSecond();
	// // mdg.buildOnMetaData();
	// // mdg.sealMetaData(true,);
	// // System.out.println(mdg.getMetaData().printMetaData());
	//
	// } catch (FFMpegException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (FileNotFoundException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (InterruptedException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	//
	// }

	class DownloadBufferManager implements Runnable {

		private final static int BYTE_STEAM_BUFFER_SIZE = 64 * CHUNK_SIZE;

		// decides how many chunks to read from disk at the same time, decreases
		// disk seeks
		private final static int CHUNK_BATCH_SIZE = 128;

		private ByteStreamBuffer buffer;

		private volatile boolean isReading = true;

		private InputStream source;

		public DownloadBufferManager(InputStream source, DiskManagerFileInfo fileInfo) {

			this.buffer = new ByteStreamBuffer(BYTE_STEAM_BUFFER_SIZE, CHUNK_SIZE);
			this.source = source;

			Thread t = new Thread(this);
			t.setName("DownloadBuffer");
			t.setDaemon(true);
			t.start();
		}

		public void close() {
			try {
				source.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		public int read(byte[] buf) {
			if (!isReading && buffer.isEmpty()) {
				return -1;
			}

			try {
				return buffer.read(buf);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return -1;
		}

		public void run() {
			int len = 0;
			int total = 0;
			byte[] buf = new byte[CHUNK_SIZE * CHUNK_BATCH_SIZE];

			try {
				System.out.println("DownloadBufferManager started");
				while ((len = source.read(buf, 0, buf.length)) != -1) {
					buffer.write(buf, len);
					// System.out.println("wrote " + len + " bytes");
					total += len;

				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				if (e.getMessage().contains("read position")) {
					// ok, it seems like the sequential reader quit
					long position = Long.parseLong(e.getMessage().split("=")[1]);
					logger.fine("got exception from the sequential stream reader");

					if (fileInfo.getLength() == fileInfo.getDownloaded()) {
						logger.fine("Download is complete, continuing from the file instead, starting at pos: "
								+ position);
						try {
							BufferedInputStream in = new BufferedInputStream(new FileInputStream(
									fileInfo.getFile()));
							in.skip(position);
							while ((len = in.read(buf, 0, buf.length)) != -1) {
								buffer.write(buf, len);
								// System.out.println("wrote " + len + "
								// bytes");
								total += len;

							}

						} catch (FileNotFoundException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (IOException e3) {
							// TODO Auto-generated catch block
							e3.printStackTrace();
						} catch (InterruptedException e4) {
							// TODO Auto-generated catch block
							e4.printStackTrace();
						}
					}

				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.isReading = false;
			System.out.println("DownloadBufferManager stopped");
		}

	}

	class StreamMover implements Runnable {
		private final OutputStream dst;

		private final InputStream src;

		public StreamMover(InputStream src, final OutputStream dst) {
			this.src = new BufferedInputStream(src);
			this.dst = new BufferedOutputStream(dst);
			Thread t = new Thread(this);
			t.setName("Stream mover");
			t.start();
		}

		public void run() {
			int len = 0;
			int total = 0;
			byte[] buffer = new byte[FFMpegWrapper.CHUNK_SIZE];
			try {
				while ((len = src.read(buffer)) != -1) {
					// System.out.println("trying to write " + len
					// + " to flvtool2 (total=" + total + ")");
					total += len;
					dst.write(buffer, 0, len);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Stream mover stopped");
		}

	}

	public static FFMpegException getLastException() {
		return lastFFMpegException;
	}

}
