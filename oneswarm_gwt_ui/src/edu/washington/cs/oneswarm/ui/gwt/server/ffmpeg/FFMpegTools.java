package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.InvalidPropertiesFormatException;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.plugins.torrent.TorrentException;

import sun.awt.AWTAutoShutdown;
import edu.washington.cs.oneswarm.ui.gwt.CoreInterface;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.FFMpegException.ErrorType;

public class FFMpegTools {

	private static final int PREVIEW_IMAGE_AT_TIME = 30;

	private static String linuxPath = null;

	private static Logger logger = Logger.getLogger(FFMpegTools.class.getName());

	public static String getFFMpegPath() throws FFMpegException {

		String os = System.getProperty("os.name");
		if (os.contains("Mac OS")) {

			try {
				File appDir = new File(SystemProperties.getApplicationPath());
				logger.fine("App dir='" + appDir.getCanonicalPath() + "'");
				File ffmpegBin = new File(appDir, "bin/ffmpeg");
				logger.fine("ffmpeg bin='" + ffmpegBin.getCanonicalPath() + "'");
				return ffmpegBin.getCanonicalPath();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Debug.out("problem locating ffmpeg, faling back to ffmpeg in path");
		} else if (os.contains("Windows")) {
			return "bin" + File.separator + "ffmpeg.exe";
		} else if (os.contains("Linux")) {
			return linuxGetFFMpegPath();
		} else {
			Debug.out("unknown os: '" + os + "' --ffmpeg must be in path");
		}
		return "ffmpeg";
	}

	/**
	 * 
	 *Blocking method for getting media information about a specific file
	 * 
	 * @param file
	 * @return
	 * @throws FFMpegException
	 */
	static MovieStreamInfo getMovieInfo(byte[] infohash, File file) throws FFMpegException {
		logger.finest("getting movie info for: " + file);
		/*
		 * start by checking if we already done this
		 */
		MovieStreamInfo cached = readCachedMovieInfo(infohash, file);
		if (cached != null) {
			return cached;
		} else {
			MovieStreamInfo m = createMovieInfo(infohash, file);
			return m;
		}
	}

	static MovieStreamInfo readCachedMovieInfo(byte[] infohash, File file) {
		String pathHash = Integer.toHexString(file.getPath().hashCode());
		File metainfoDir;
		try {
			metainfoDir = CoreInterface.getMetaInfoDir(infohash);
			File existingInfoFile = new File(metainfoDir, "movieInfo_" + pathHash + ".xml");
			if (existingInfoFile.exists()) {
				try {
					MovieStreamInfo i = new MovieStreamInfo(existingInfoFile);
					logger.fine("loaded mediainfo from existing file: " + i);
					return i;
				} catch (InvalidPropertiesFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (TorrentException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return null;
	}

	static MovieStreamInfo createMovieInfo(byte[] infohash, File file) throws FFMpegException {
		logger.finest("using ffmpeg to create movie info for: " + file);

		final Process ffmpeg;
		try {
			ffmpeg = createFFmpegProcess(new String[] { "-i", file.getCanonicalPath() }, 5 * 1000);
		} catch (IOException e1) {
			throw new FFMpegException(ErrorType.FILE_NOT_FOUND, "got error when getting file: " + file, e1);
		}
		BufferedReader stdErr = new BufferedReader(new InputStreamReader(ffmpeg.getErrorStream()));
		// dump anything showing up no stdout (shouldn't be anything
		new StreamDumper(ffmpeg.getInputStream(), 0, false);

		// and read from stderr at the same time
		StringBuffer ffmpegStdErr = new StringBuffer();
		try {
			String line;
			while ((line = stdErr.readLine()) != null) {
				ffmpegStdErr.append(line + "\n");
			}
			stdErr.close();
		} catch (IOException e) {
			throw new FFMpegException(FFMpegException.ErrorType.OTHER, "Got IO error while reading from ffmpeg", e);
		}

		try {
			ffmpeg.waitFor();
		} catch (InterruptedException e) {
			throw new FFMpegException(FFMpegException.ErrorType.INTERUPT, "Got interupted while waiting for ffmpeg to complete", e);
		}
		MovieStreamInfo m = new MovieStreamInfo(ffmpegStdErr.toString());
		logger.finest("movie info created successfully, saving to disk");
		/*
		 * try to write it down to disk
		 */
		try {
			String pathHash = Integer.toHexString(file.getPath().hashCode());
			File metainfoDir = CoreInterface.getMetaInfoDir(infohash);
			File existingInfoFile = new File(metainfoDir, "movieInfo_" + pathHash + ".xml");
			File parent = existingInfoFile.getParentFile();
			if (!parent.isDirectory()) {
				parent.mkdirs();
			}
			m.writeToFile(existingInfoFile);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (TorrentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return m;
	}

	static Process createFFmpegProcess(String[] parameters, final int killAfterSeconds) throws FFMpegException {
		try {
			String[] allParameters = new String[parameters.length + 1];
			allParameters[0] = FFMpegTools.getFFMpegPath();
			System.arraycopy(parameters, 0, allParameters, 1, parameters.length);
			StringBuilder ffmpegParameters = new StringBuilder();
			for (String string : allParameters) {
				ffmpegParameters.append("'" + string + "' ");
			}
			logger.finest("executing: " + ffmpegParameters.toString());
			final Process ffmpeg = Runtime.getRuntime().exec(allParameters);
			/*
			 * add a thread that kill ffmpeg if it runs for too long
			 */
			if (killAfterSeconds > 0) {
				Thread ffmpegTerminatorThread = new Thread(new Runnable() {
					public void run() {
						long totalSlept = 0;
						try {
							if (totalSlept >= killAfterSeconds) {
								if (ffmpeg != null) {
									logger.finest("ffmpeg not completed after " + totalSlept + "s, killing");
									ffmpeg.destroy();
									return;
								}
							}
							Thread.sleep(1000);
							totalSlept++;
							try {
								ffmpeg.exitValue();
								logger.finest("ffmpeg process completed, stopping ffmpeg kill thread");
								// check if the process terminated
								// if we get an exit value the process
								// terminated,
								// then kill this thread
								return;
							} catch (IllegalThreadStateException e) {
								// this is expected
							}

						} catch (InterruptedException e) {
							// interrupted, just kill the thread
						}
					}
				});
				ffmpegTerminatorThread.setName("FFMpeg terminator thread");
				ffmpegTerminatorThread.setDaemon(true);
				ffmpegTerminatorThread.start();
			}
			return ffmpeg;
		} catch (IOException e1) {
			throw new FFMpegException(ErrorType.OTHER, "unable to create ffmpeg process", e1);
		}
	}

	static void createPreviewImage(byte[] infohash, File mediaFile, File imageFile) throws FFMpegException {
		logger.finer("trying to create preview image, file=" + mediaFile + " destination=" + imageFile);
		// check if we have video in the file
		MovieStreamInfo fileInfo = getMovieInfo(infohash, mediaFile);

		if (!fileInfo.hasVideo()) {
			throw new FFMpegException(ErrorType.FORMAT_ERROR, "unable to create preview image, no video stream found");
		}

		/*
		 * calc seek to
		 */
		double duration = fileInfo.getDuration();
		int seekTo;
		if (duration < 1) {
			seekTo = 0;
		} else if (duration > 2 * PREVIEW_IMAGE_AT_TIME) {
			seekTo = PREVIEW_IMAGE_AT_TIME;
		} else {
			seekTo = (int) Math.floor(duration / 2);
		}

		/*
		 * create the ffmpeg process
		 */
		final Process ffmpeg;
		try {
			ffmpeg = createFFmpegProcess(new String[] { "-i", mediaFile.getCanonicalPath(), "-vcodec", "png", "-ss", seekTo + "", "-vframes", "1", "-f", "rawvideo", "-" }, 30);
		} catch (IOException e) {
			throw new FFMpegException(ErrorType.FILE_NOT_FOUND, "got error when getting file: " + mediaFile, e);
		}
		StreamReader ffmpegStdOutReader = new StreamReader(ffmpeg.getInputStream());
		BufferedReader stdErr = new BufferedReader(new InputStreamReader(ffmpeg.getErrorStream()));
		// and read from stderr at the same time
		StringBuffer ffmpegStdErr = new StringBuffer();
		byte[] ffmpegStdOut;

		/*
		 * ffmpeg is running, read the output
		 */
		int lineNum = 0;
		try {
			String line;
			while ((line = stdErr.readLine()) != null) {
				// ffmpeg can send out megabytes on stderr, only save first 100
				// lines
				if (lineNum++ < 100) {
					ffmpegStdErr.append(line + "\n");
				}
			}
			stdErr.close();
			ffmpegStdOut = ffmpegStdOutReader.read();
		} catch (IOException e) {
			throw new FFMpegException(FFMpegException.ErrorType.OTHER, "Got IO error while reading from ffmpeg", e);
		} catch (InterruptedException e) {
			throw new FFMpegException(FFMpegException.ErrorType.INTERUPT, "Got interupted while reading from ffmpeg", e);
		}

		/*
		 * wait for ffmpeg to complete
		 */

		try {
			int exitVal = ffmpeg.waitFor();
			if (exitVal != 0) {
				throw new FFMpegException(FFMpegException.ErrorType.OTHER, exitVal, ffmpegStdErr.toString());
			}
		} catch (InterruptedException e) {
			throw new FFMpegException(FFMpegException.ErrorType.INTERUPT, "Got interupted while waiting for ffmpeg to complete", e);
		}

		/*
		 * and write the file
		 */
		try {
			writeTransformedImage(new ByteArrayInputStream(ffmpegStdOut), imageFile, true);
		} catch (IOException e) {
			throw new FFMpegException(FFMpegException.ErrorType.OTHER, "Got IO error when writing preview image", e);
		}

	}

	private static String linuxGetFFMpegPath() throws FFMpegException {
		// check if we already did this
		if (linuxPath != null) {
			return linuxPath;
		}
		System.out.println("linux: trying to find the ffmpeg path");
		// first, try the libc2.7 ffmpeg
		File binDir = new File(SystemProperties.getApplicationPath() + File.separator + "bin");
		File ffmpeg27 = new File(binDir, "ffmpeg_glibc2.7");
		File ffmpeg26 = new File(binDir, "ffmpeg_glibc2.6");
		try {
			String[] paths = { ffmpeg27.getCanonicalPath(), ffmpeg26.getCanonicalPath(), "/usr/bin/ffmpeg", "ffmpeg" };
			for (String path : paths) {
				try {

					boolean works = testFFMpeg(path);
					if (works) {
						linuxPath = path;
						return path;
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		throw new FFMpegException(ErrorType.FFMPEG_BIN_ERROR, "Unable to find a working ffmpeg binary");
	}

	private final static HashSet<File> failedPreviews = new HashSet<File>();

	static void setPrevImageGenerationFailed(File imageFile) {
		failedPreviews.add(imageFile);
		try {
			File failFile = new File(imageFile.getCanonicalPath() + ".failed");
			failFile.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	static boolean checkPrevImageGenerationFailed(File imageFile) {
		if (failedPreviews.contains(imageFile)) {
			return true;
		}
		try {
			File failFile = new File(imageFile.getCanonicalPath() + ".failed");
			return failFile.isFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	private static boolean testFFMpeg(String path) throws IOException, InterruptedException {

		Process p = Runtime.getRuntime().exec(new String[] { path, "-h" });
		BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));

		logger.finer("testing ffmpeg: '" + path + "'");
		while (in.readLine() != null) {
		}
		int returnValue = p.waitFor();
		logger.finer("got return value: " + returnValue);
		if (returnValue == 0) {
			return true;
		}
		return false;
	}

	/**
	 * instead of resizing on the client we do it here
	 * 
	 * @param inputStream
	 * @param imgFile
	 * @throws IOException
	 */
	static void writeTransformedImage(InputStream inputStream, File imgFile, boolean fromVideo) throws IOException {
		logger.fine("writing transformed image to: " + imgFile.getAbsolutePath());

		FileOutputStream out = new FileOutputStream(imgFile);

		ImageIO.setUseCache(false);

		if (inputStream.available() <= 0) {
			throw new IOException("tried to transform a zero-size image.");
		}

		Image base = ImageIO.read(inputStream);
		if (base == null) {
			throw new IOException("unable to read image");
		}
		double resizeFactorWidth = base.getWidth(null) / (double) 128;
		double resizeFactorHeight = base.getHeight(null) / (double) 128;
		double resizeFactor = Math.max(resizeFactorWidth, resizeFactorHeight);

		// System.out.println("resize factor: " + resizeFactor);

		base = base.getScaledInstance((int) (base.getWidth(null) / resizeFactor), (int) (base.getHeight(null) / resizeFactor), Image.SCALE_SMOOTH);

		BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);

		Graphics2D graph = image.createGraphics();
		// first, fill the background with transparent white
		Color transparent = new Color(255, 255, 255, 0);
		graph.setColor(transparent);
		graph.fill(new Rectangle(0, 0, 128, 128));

		// then create a 4x3 rectangle with black to make all strange 16x9,
		// 16x10,1:2.09 formats don't look uneven
		// but only if the image is wider that it is high and we got the image
		// from video
		int width = base.getWidth(null);
		int height = base.getHeight(null);

		if (width > height && fromVideo) {
			BufferedImage padding = new BufferedImage(128, 96, BufferedImage.TYPE_INT_ARGB);
			Graphics2D paddGraph = padding.createGraphics();
			paddGraph.setColor(Color.black);
			paddGraph.fill(new Rectangle(0, 0, 128, 96));
			AffineTransform padTrans = new AffineTransform();
			padTrans.setToTranslation(0, (128 - 96) / 2);
			graph.drawImage(padding, padTrans, null);
			paddGraph.dispose();
			padding.flush();
		}

		int x = (128 - width) / 2;
		int y = (128 - height) / 2;

		logger.finer("x: " + x + " y: " + y + " width: " + width + " height: " + height);

		AffineTransform trans = new AffineTransform();
		trans.setToTranslation(x, y);

		graph.drawImage(base, trans, null);

		ImageIO.write(image, "png", out);

		out.close();
		graph.dispose();
		image.flush();
		base.flush();

		/**
		 * There seems to be a weird interaction with using ImageIO if we don't
		 * mark this thread as free. (The AWT-Shutdown thread will hang around
		 * forever when we try to quit until it is forceably shutdown). Calling
		 * this seems to prevent that, although I'm not sure if we've addressed
		 * the root problem.
		 */
		try {
			AWTAutoShutdown.notifyToolkitThreadFree();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
