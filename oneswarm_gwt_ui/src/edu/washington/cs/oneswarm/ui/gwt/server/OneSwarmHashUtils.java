package edu.washington.cs.oneswarm.ui.gwt.server;

import org.gudy.azureus2.core3.util.Base32;

import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants;

public class OneSwarmHashUtils {

    public static String createOneSwarmHash(byte[] hash) {
        return OneSwarmConstants.BITTORRENT_MAGNET_PREFIX + new String(Base32.encode(hash));
    }

    public static byte[] bytesFromOneSwarmHash(String hash) {
        if (hash == null) {
            return null;
        }
        if (hash.startsWith(OneSwarmConstants.BITTORRENT_MAGNET_PREFIX)) {
            return Base32
                    .decode(hash.substring(OneSwarmConstants.BITTORRENT_MAGNET_PREFIX.length()));
        } else {
            return Base32.decode(hash);
        }
    }
}
