package edu.washington.cs.oneswarm.ui.gwt.server.handlers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URLClassLoader;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.AbstractHandler;

public class FileHandler extends AbstractHandler {
	private static Logger logger = Logger.getLogger(FileHandler.class.getName());

	private final static String INDEX_FILE_NAME = "oneswarmgwt/OneSwarmGWT.html";

	private final static String EMBEDDED_FILE_NAME = "oneswarmgwt/OneSwarmEmbedded.html";

	public static String mServerRootPath = "/";

	private static long jarBuiltTime = System.currentTimeMillis();
	static {
		File jarFile = new File(SystemProperties.getApplicationPath() + File.separator + "OneSwarmAzMods.jar");
		logger.finest("using jar file mod time for last_modified: " + jarFile);
		if (jarFile.exists()) {
			jarBuiltTime = jarFile.lastModified();
			logger.finest("using jar timestamp for last_modified: " + new Date(jarBuiltTime));
		} else {
			logger.warning("Unable to get time stamp of jar file (not found), using system time instead.");
		}
	}

	private ConcurrentHashMap<String, Long> modifiedTimes = new ConcurrentHashMap<String, Long>();
	private ConcurrentHashMap<String, Integer> fileSizes = new ConcurrentHashMap<String, Integer>();

	/* The ClassLoader used to retrieve resources. */
	private ClassLoader classLoader = null;
	
	public FileHandler() {
		// If we can't find the resource in the current ClassLoader, we might be the SWT target
		// in debug mode. Eclipse won't have built the GWT targets, but if we built them
		// externally, retrieve from the gwt-dist directory.
		if (System.getProperty("debug.war") != null) {
			try {
				classLoader = new URLClassLoader(
						new java.net.URL[]{new File(System.getProperty("debug.war")).toURI().toURL()},
						getClass().getClassLoader());
				
				// remove the leading '/' in paths.
				mServerRootPath = "";
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		} else {
			classLoader = getClass().getClassLoader();
		}
	}
	
	public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {

		int lastSlash = target.lastIndexOf('/');

		// get the filename (we are ignoring directories, for now...)
		String filename = "/";
		if (lastSlash >= 0) {
			filename = target.substring(lastSlash + 1, target.length());
		}

		logger.finer("req: " + target + " / fname: " + filename);

		// check if default file
		if (target.equals("/")) {
			serve(INDEX_FILE_NAME, request, response);
		} else if (filename.startsWith(EMBEDDED_FILE_NAME)) {
			serve(EMBEDDED_FILE_NAME, request, response);
		} else {
			// ok, match, serv it
			if (!serve(target, request, response)) {
				Debug.out("file not found: " + target);
			}

		}
	}

	public static String getContentType(String filename) {
		int lastDot = filename.lastIndexOf('.');
		if (lastDot > 0) {
			String suffix = filename.substring(lastDot + 1, filename.length());
			// System.out.println("file suffix='" + suffix + "'");
			if (suffix.equals("html")) {
				return "text/html";
			} else if (suffix.equals("js")) {
				return "application/javascript";
			} else if (suffix.equals("swf")) {
				return "application/x-shockwave-flash";
			} else if (suffix.equals("css")) {
				return "text/css";
			} else if (suffix.equals("gif")) {
				return "image/gif";
			} else if (suffix.equals("jpg")) {
				return "image/jpeg";
			} else if (suffix.equals("flv")) {
				return "video/x-FLV";
			} else if (suffix.equals("png")) {
				return "image/png";
			} else if (suffix.equalsIgnoreCase("rpc")) {
				return "text/html";
			} else if (suffix.equalsIgnoreCase("ico")) {
				return "image/x-icon";
			} else {
				Debug.out("unknown file suffix: ." + suffix);
			}
		}

		// just return text by default
		return "application/unknown";

	}

	private boolean serve(String filename, HttpServletRequest request, HttpServletResponse response) throws IOException {
		logger.finer("got request for: " + filename);
		if (filename.startsWith("/")) {
			filename = filename.substring(1);
		}
		String fullPath = mServerRootPath + filename;

		/**
		 * Hack to deal with 1.6 upgrade hosted / real world mode hacks
		 */
		if (fullPath.startsWith("/images") || fullPath.equals("/favicon.ico") || fullPath.startsWith("/player/")) {
			fullPath = "/oneswarmgwt" + fullPath;
		}

		boolean useCache = true;
		/*
		 * don't use cache for the index file, it is small and we need to modify
		 * it when the language changes
		 */
		if (INDEX_FILE_NAME.equals(filename)) {
			useCache = false;
			logger.fine("got request for index file, skipping cache");
			response.setHeader(HttpHeaders.PRAGMA, "no-cache");
			response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, must-revalidate");
		}

		if (useCache) {
			long last_modified;
			if (jarBuiltTime > 0) {
				last_modified = jarBuiltTime;
				// logger.finest("using jar timestamp for last_modified");
			} else {
				if (!modifiedTimes.containsKey(filename)) {
					modifiedTimes.put(filename, System.currentTimeMillis());
				}
				last_modified = modifiedTimes.get(filename);
			}

			if (last_modified > 0) {
				long if_modified = request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE);
				if (if_modified > 0 && last_modified / 1000 == if_modified / 1000) {
					response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
					((Request) request).setHandled(true);
					logger.finest("not modified, " + last_modified);
					return true;
				}
				// ok, we have to serve the file, set the header
				response.setDateHeader(HttpHeaders.LAST_MODIFIED, last_modified);
				logger.finest("setting modified tag, " + last_modified + " (prev was: " + if_modified + ")");
			}

			// set the expire time
			if (filename.endsWith("nocache.js")) {
				// important, set no cache on these
				logger.finest("setting no-cache on: " + filename);
				response.setHeader(HttpHeaders.PRAGMA, "no-cache");
				response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, must-revalidate");
			} else if (filename.endsWith(".cache.png") || filename.endsWith(".cache.html")) {
				// these file are named with their md5sum, this means they will
				// NEVER change: set cache to a year
				logger.finest("setting extra long cache on: " + filename);
				int oneYearInSeconds = 365 * 24 * 60 * 60;
				response.setDateHeader(HttpHeaders.EXPIRES, System.currentTimeMillis() + (oneYearInSeconds * 1000));
				response.setHeader(HttpHeaders.CACHE_CONTROL, "max-age=" + oneYearInSeconds + ", public");
			} else if (filename.startsWith("/oneswarmgwt/images/") || filename.startsWith("gwt/standard")) {
				// tell the browser to cache images up to 24 hours to speed up
				// load
				int secondsToCache = 24 * 60 * 60;
				response.setDateHeader(HttpHeaders.EXPIRES, System.currentTimeMillis() + (secondsToCache * 1000));
				response.setHeader(HttpHeaders.CACHE_CONTROL, "max-age=" + secondsToCache + ", public");
				logger.finest("setting 24h cache on: " + filename);
			} else {
				logger.finest("not setting custom cache rule on: " + filename);
			}
		}

		InputStream inputstream = classLoader.getResourceAsStream(fullPath);
		
		if (inputstream == null) {
			// ok, file not found, return
			return false;
		}

		int contentLength;
		if (fileSizes.containsKey(filename)) {
			contentLength = fileSizes.get(filename);
			logger.finest("Cached size: File: " + filename + " content length: " + contentLength);
		} else {
			/*
			 * calc the content length
			 */
			contentLength = 0;
			while (inputstream.read() != -1) {
				contentLength++;
			}
			fileSizes.put(filename, contentLength);
			inputstream = classLoader.getResourceAsStream(fullPath);
			logger.finest("Calculated: File: " + filename + " content length: " + contentLength);

		}

		ServletOutputStream outputstream = response.getOutputStream();
		String contentType = getContentType(filename);
		response.setContentType(contentType);
		response.setStatus(HttpServletResponse.SC_OK);

		/*
		 * inject the language
		 */
		if (INDEX_FILE_NAME.equals(filename)) {
			injectLocaleMetaTag(request, inputstream, outputstream);
		} else {
			response.setContentLength(contentLength);
			copyStream(inputstream, outputstream);
		}

		outputstream.close();
		// response.getWriter().println("<h1>Hello</h1>");
		((Request) request).setHandled(true);
		logger.finest("served: " + contentType + " " + filename);
		return true;
	}

	private void copyStream(InputStream inputstream, OutputStream outputstream) throws IOException {
		byte[] buffer = new byte[1024];
		int len;
		while ((len = inputstream.read(buffer)) > 0) {
			outputstream.write(buffer, 0, len);
		}
		outputstream.flush();
	}

	private void injectLocaleMetaTag(HttpServletRequest request, InputStream in, OutputStream out) throws IOException {

		/*
		 * a list of injectors that replace some text in the html page with
		 * another text
		 */
		List<HTMLTagInjector> injectors = new LinkedList<HTMLTagInjector>();
		injectors.add(new LocaleInjector());
		injectors.add(new RightClickInjector());

		BufferedReader bin = new BufferedReader(new InputStreamReader(in));
		BufferedWriter bout = new BufferedWriter(new OutputStreamWriter(out));

		String line;

		lineloop: while ((line = bin.readLine()) != null) {
			for (Iterator<HTMLTagInjector> iterator = injectors.iterator(); iterator.hasNext();) {
				HTMLTagInjector injector = iterator.next();
				/*
				 * check if it matches
				 */
				if (injector.matchesLine(line)) {
					// write out the replacement line
					bout.write(injector.getReplacementLine(request) + "\r\n");
					// remove this injector
					iterator.remove();
					// and continue to the next line
					continue lineloop;
				}
			}

			bout.write(line + "\r\n");

		}
		bout.flush();

	}

	static interface HTMLTagInjector {
		public boolean matchesLine(String line);

		public String getReplacementLine(HttpServletRequest request);

	}

	static class LocaleInjector implements HTMLTagInjector {
		public boolean matchesLine(String line) {
			if (line.trim().equals("<head>")) {
				return true;
			} else {
				return false;
			}
		}

		public String getReplacementLine(HttpServletRequest request) {
			Locale currentLocale = MessageText.getCurrentLocale();
			String locale = "en_US";
			if (currentLocale != null) {
				locale = currentLocale.getLanguage() + "_" + currentLocale.getCountry();
			}
			logger.finer("injected language info, locale=" + locale);
			return "<head>\r\n<meta name='gwt:property' content='locale=" + locale + "'/>";
		}
	}

	static class RightClickInjector implements HTMLTagInjector {
		public boolean matchesLine(String line) {
			if (line.trim().equals("<body>")) {
				return true;
			} else {
				return false;
			}
		}

		public String getReplacementLine(HttpServletRequest request) {
			boolean rightClickEnabled = false;
			if (request != null && request.getCookies() != null) {
				for (Cookie c : request.getCookies()) {
					if (c.getName().equals("os-disable_right_click")) {
						if ("0".equals(c.getValue())) {
							rightClickEnabled = true;
						}
					}
				}
			}
			if (rightClickEnabled) {
				logger.finer("injected right click enabled");
				return "<body oncontextmenu='return false;'>";
			} else {
				return "<body>";
			}
		}
	}
}
