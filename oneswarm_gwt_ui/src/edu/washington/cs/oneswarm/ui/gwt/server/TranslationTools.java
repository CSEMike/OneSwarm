package edu.washington.cs.oneswarm.ui.gwt.server;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemProperties;

public class TranslationTools {
	private static Logger logger = Logger.getLogger(TranslationTools.class.getName());

	static {
		COConfigurationManager.addParameterListener("locale", new ParameterListener() {
			public void parameterChanged(String parameterName) {
				String locale = COConfigurationManager.getStringParameter("locale");
				String[] split = locale.split("_");
				if (split.length == 2) {
					MessageText.changeLocale(new Locale(split[0], split[1]));
				}
			}
		});
	}

	public static List<Locale> getLocales() throws IOException {
		List<Locale> locales = new LinkedList<Locale>();
		locales.add(Locale.US);
		File file = new File(SystemProperties.getApplicationPath() + File.separator + "OneSwarmAzMods.jar");
		logger.fine("loading translations from: '" + file + "'");
		if(!file.isFile()){
			Debug.out("unable to find OneSwarmAzMods.jar, sending out EN_US only");
			return locales;
		}
		JarFile jarFile = new JarFile(file);
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			JarEntry e = entries.nextElement();
			String fileName = e.getName();
			logger.finest("looking at: " + fileName);
			int posOfOsMessage = fileName.indexOf("OSMessages");
			if (posOfOsMessage != -1 && fileName.endsWith(".properties")) {
				logger.finer("match: " + fileName);
				String fileOnly = fileName.substring(posOfOsMessage, fileName.length() - ".properties".length());
				if (fileOnly.contains("_")) {
					String[] split = fileOnly.split("_");
					String country = split[2];
					String language = split[1];
					Locale l = new Locale(language, country);
					logger.finer("detected country: " + l.getDisplayCountry(l) + " (" + l.getDisplayLanguage(l) + ") " + " code=" + l.toString());
					locales.add(l);
				}
			}
		}
		return locales;
	}

	public static void main(String args[]) {
		try {
			OneSwarmUIServiceImpl.loadLogger();
			logger.setLevel(Level.FINER);

			System.out.println(logger.getLevel());
			getLocales();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
