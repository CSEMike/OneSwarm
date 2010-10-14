package edu.washington.cs.oneswarm.ui.gwt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SHA1Simple;
import org.gudy.azureus2.core3.util.SystemProperties;

public class RemoteAccessConfig
{
	public final static File	 REMOTE_ACCESS_FILE;

	private final static File	REMOTE_ACCESS_FILE_DIR = new File(
																												SystemProperties.getUserPath()
																														+ File.separator
																														+ "keys");

	public final static String MD5_SALT							 = "58ff700ae41d2a2e0683e36db2aa1edbc15b1af1";

	static {
		if (!REMOTE_ACCESS_FILE_DIR.isDirectory()) {
			REMOTE_ACCESS_FILE_DIR.mkdirs();
		}
		REMOTE_ACCESS_FILE = new File(REMOTE_ACCESS_FILE_DIR,
				"remote_users.properties");
		if (!REMOTE_ACCESS_FILE.isFile()) {
			try {
				REMOTE_ACCESS_FILE.createNewFile();
			} catch (IOException e) {
				Debug.out("unable to create remote access credentials file", e);
			}
		}
	}

	public static boolean usesMD5Sha1Password() {
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(REMOTE_ACCESS_FILE));
			String line = in.readLine();
			if (line != null && line.contains("CRYPT:")) {
				return false;
			}
		} catch (IOException e) {
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException e) {
			}
		}
		return true;
	}

	public static String getRemoteAccessUserName() {
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(REMOTE_ACCESS_FILE));
			String line = in.readLine();
			if (line != null && line.contains(":")) {
				String[] split = line.split(":");
				return split[0];
			}
		} catch (IOException e) {
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException e) {
			}
		}
		return "username";
	}

	public static String getRemoteAccessCryptedPassword() {
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(REMOTE_ACCESS_FILE));
			String line = in.readLine();
			if (line != null && line.contains(":")) {
				int pos = line.indexOf(':');
				String pwString = line.substring(pos + 1);
				//logger.finest("pw full: " + pwString);
				if (pwString.contains(",")) {
					String[] s = pwString.split(",");
					//logger.finest("crypted password is: '" + s[0] + "'");
					return s[0];
				}
			}
		} catch (Throwable e) {
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException e) {
			}
		}
		return null;
	}

	public static void saveRemoteAccessCredentials(String username,
			String password) throws IOException {
		String cryptedPassword;
		if (password != null) {
			if (password.length() < 8) {
				throw new IOException("Invalid password, len < 8");
			}
			if (username == null || username.length() < 1) {
				throw new IOException("Invalid username, len < 1");
			}
			cryptedPassword = MD5.digest(getSaltedPassword(password));
		} else {
			cryptedPassword = getRemoteAccessCryptedPassword();
			if (cryptedPassword == null) {
				throw new IOException("Invalid password");
			}
		}
		BufferedWriter out = new BufferedWriter(new FileWriter(REMOTE_ACCESS_FILE));
		if (username != null && username.length() > 0) {
			out.write(username + ": " + cryptedPassword + ", remote_user\n");
		}
		out.close();

	}

	public static String getSaltedPassword(String password)
			throws UnsupportedEncodingException {
		SHA1Simple sha1Simple = new SHA1Simple();
		byte[] sha1 = sha1Simple.calculateHash((MD5_SALT + password).getBytes("UTF-8"));
		return Base32.encode(sha1);

	}

	/* ------------------------------------------------------------ */
	/** MD5 Credentials, from jetty
	 */
	public static class MD5
	{
		public static final String	 __TYPE		= "MD5:";

		public static final Object	 __md5Lock = new Object();

		private static MessageDigest __md;

		private byte[]							 _digest;

		public static final String	 __ISO_8859_1;
		static {
			String iso = System.getProperty("ISO_8859_1");
			if (iso == null) {
				try {
					new String(new byte[] {
						(byte) 20
					}, "ISO-8859-1");
					iso = "ISO-8859-1";
				} catch (java.io.UnsupportedEncodingException e) {
					iso = "ISO8859_1";
				}
			}
			__ISO_8859_1 = iso;
		}

		/* ------------------------------------------------------------ */
		public static String digest(String password) {
			try {
				byte[] digest;
				synchronized (__md5Lock) {
					if (__md == null) {
						try {
							__md = MessageDigest.getInstance("MD5");
						} catch (Exception e) {
							e.printStackTrace();
							return null;
						}
					}

					__md.reset();
					__md.update(password.getBytes(__ISO_8859_1));
					digest = __md.digest();
				}

				return __TYPE + toString(digest, 16);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		public static String toString(byte[] bytes, int base) {
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < bytes.length; i++) {
				int bi = 0xff & bytes[i];
				int c = '0' + (bi / base) % base;
				if (c > '9')
					c = 'a' + (c - '0' - 10);
				buf.append((char) c);
				c = '0' + bi % base;
				if (c > '9')
					c = 'a' + (c - '0' - 10);
				buf.append((char) c);
			}
			return buf.toString();
		}
	}

}
