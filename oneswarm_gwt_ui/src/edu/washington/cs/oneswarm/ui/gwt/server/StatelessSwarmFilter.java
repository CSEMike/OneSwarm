package edu.washington.cs.oneswarm.ui.gwt.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.impl.AzureusCoreImpl;

import edu.washington.cs.oneswarm.MetaInfoPruner;
import edu.washington.cs.oneswarm.f2f.FileCollection;
import edu.washington.cs.oneswarm.f2f.FileList;
import edu.washington.cs.oneswarm.f2f.Friend;
import edu.washington.cs.oneswarm.f2f.FriendConnectListener;
import edu.washington.cs.oneswarm.f2f.multisource.Sha1DownloadManager;
import edu.washington.cs.oneswarm.ui.gwt.CoreInterface;
import edu.washington.cs.oneswarm.ui.gwt.CoreTools;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.FileTypeFilter;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FileTree;

/**
 * Returning a FileTree is a bit of a hack, but since we already have a
 * serialized type that encapsulates tree, just reuse it here instead of cooking
 * up a new one.
 */
class TreeScratch {
    public TreeScratch(String k) {
        tagName = k;
    }

    public List<TreeScratch> kids = new LinkedList<TreeScratch>();
    public String tagName;
};

public class StatelessSwarmFilter {

    private static Logger logger = Logger.getLogger(StatelessSwarmFilter.class.getName());

    boolean shouldUpdateClient = false;
    CoreInterface mCore = null;

    enum SortMetric {
        Name(new Comparator<DownloadManager>() {
            public int compare(DownloadManager lhs, DownloadManager rhs) {
                return (new String(lhs.getTorrent().getName()).toLowerCase().compareTo((new String(
                        rhs.getTorrent().getName())).toLowerCase()));
            }
        }), Date(new Comparator<DownloadManager>() {
            public int compare(DownloadManager lhs, DownloadManager rhs) {
                /**
                 * Special case sorting one of our download managers when
                 * viewing a friend's files only
                 */
                long lhs_time = lhs.getData("friend-added-time") != null ? (Long) lhs
                        .getData("friend-added-time") : lhs.getDownloadState().getLongParameter(
                        DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME);
                long rhs_time = rhs.getData("friend-added-time") != null ? (Long) rhs
                        .getData("friend-added-time") : rhs.getDownloadState().getLongParameter(
                        DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME);
                long foo = -(lhs_time - rhs_time);
                if (foo < 0)
                    return -1;
                else if (foo > 0)
                    return 1;
                else
                    return 0;
            }
        }), Size(new Comparator<DownloadManager>() {
            public int compare(DownloadManager lhs, DownloadManager rhs) {
                long foo = (lhs.getTorrent().getSize() - rhs.getTorrent().getSize());
                // we flip the order here to have largest first
                if (foo < 0)
                    return 1;
                else if (foo > 0)
                    return -1;
                else
                    return 0;
            }
        });

        Comparator<DownloadManager> mComp;

        SortMetric(Comparator<DownloadManager> comp) {
            mComp = comp;
        }
    };

    public StatelessSwarmFilter(CoreInterface inCore) {

        mCore = inCore;

        AzureusCoreImpl.getSingleton().getGlobalManager().addListener(new GlobalManagerListener() {
            public void destroyInitiated() {
            }

            public void destroyed() {
            }

            public void downloadManagerAdded(DownloadManager dm) {
                logger.finer("dl manager added forces client refresh: " + dm.getDisplayName());
                shouldUpdateClient = true;
            }

            public void downloadManagerRemoved(DownloadManager dm) {
                logger.finer("dl manager removed forces client refresh: " + dm.getDisplayName());
                shouldUpdateClient = true;

                String base64Hash = null;
                try {
                    base64Hash = new String(Base64.encode(dm.getTorrent().getHash()));
                    filteredUntilDeleteBase64.remove(base64Hash);
                } catch (TOTorrentException e) {
                    e.printStackTrace();
                }
            }

            public void seedingStatusChanged(boolean seeding_only_mode) {
            }
        });

        mCore.getF2FInterface().registerForFriendConnectNotifications(new FriendConnectListener() {
            public void friendConnected(Friend f) {
                logger.finer("friend connected forces client refresh: " + f.getNick());
                shouldUpdateClient = true;
                /**
                 * Record our observing these so we don't prune them
                 */
                FileList flist = mCore.getF2FInterface().getOnlineFileLists().get(f);
                for (FileCollection coll : flist.getElements()) {
                    String hexHash = ByteFormatter.encodeString(Base64.decode(coll.getUniqueID()));
                    MetaInfoPruner.get().recordActiveHash(hexHash);
                }
            }

            public void friendDisconnected(Friend f) {
                logger.finer("friend disconnected forces client refresh: " + f.getNick());
                shouldUpdateClient = true;
            }
        });

    }

    public boolean shouldClientSideUIRefresh() {
        return shouldUpdateClient;
    }

    public void willUpdate() {
        shouldUpdateClient = false;
    }

    private List<DownloadManager> getSingleFriendsFiles(int selectedFriendID) {
        List<DownloadManager> out = new ArrayList<DownloadManager>();

        Friend friend = null;
        for (Friend candidate : mCore.getF2FInterface().getOnlineFileLists().keySet()) {
            if (candidate.getConnectionId() == selectedFriendID) {
                friend = candidate;
                break;
            }
        }
        if (friend == null) {
            logger.warning("couldn't find supposedly online friend! id: " + selectedFriendID);
            return out;
        }

        FileList fileList = mCore.getF2FInterface().getOnlineFileLists().get(friend);

        System.out.println("single friend's flist: " + friend.getNick() + " / " + fileList);

        if (fileList == null) {
            logger.warning("null file list for friend: " + friend.getNick());
            return out;
        }

        for (FileCollection collection : fileList.getElements()) {
            /**
             * If we have this file ourselves, no need to show a 'download'
             * option, we'll just include the local copy
             */
            DownloadManager ours = AzureusCoreImpl.getSingleton().getGlobalManager()
                    .getDownloadManager(new HashWrapper(Base64.decode(collection.getUniqueID())));
            if (ours != null) {
                /**
                 * This allows fast deletes (regardless of how long Azureus
                 * takes to remove things on the back end)
                 */
                String base64Hash = null;
                try {
                    base64Hash = new String(Base64.encode(ours.getTorrent().getHash()));
                } catch (TOTorrentException e) {
                    e.printStackTrace();
                    continue;
                }
                if (filteredUntilDeleteBase64.contains(base64Hash)) {
                    DownloadManagerAdapter adapter = new DownloadManagerAdapter(collection,
                            friend.getConnectionId(), friend.getNick());
                    out.add(adapter);
                } else {
                    out.add(ours);
                    ours.setData("friend-added-time", new Long(collection.getAddedTimeUTC()));
                }
            } else {
                DownloadManagerAdapter adapter = new DownloadManagerAdapter(collection,
                        friend.getConnectionId(), friend.getNick());
                out.add(adapter);
            }
        }

        return out;
    }

    enum SpecialMatcher {
        NOT1("not:") {
            boolean match(DownloadManager dm, String text) {
                return !deepKeywordMatch(dm, text);
            }
        },
        NOT2("-") {
            boolean match(DownloadManager dm, String text) {
                return NOT1.match(dm, text);
            }
        };

        String mLabel;

        SpecialMatcher(String inLabel) {
            mLabel = inLabel;
        }

        abstract boolean match(DownloadManager candidate, String text);
    };

    private static boolean deepKeywordMatch(DownloadManager d, String keyword) {
        /**
         * If the display name or any interior file...
         */
        if (d.getDisplayName().toLowerCase().contains(keyword) == false) {
            /**
             * Metainfo -- artist/album attributes if they exist
             */
            String album = d.getDownloadState().getAttribute(
                    FileCollection.ONESWARM_ALBUM_ATTRIBUTE);
            if (album != null) {
                if (album.toLowerCase().contains(keyword)) {
                    return true;
                }
            }

            String artist = d.getDownloadState().getAttribute(
                    FileCollection.ONESWARM_ARTIST_ATTRIBUTE);
            if (artist != null) {
                if (artist.toLowerCase().contains(keyword)) {
                    return true;
                }
            }

            /**
             * 2) filenames
             */
            for (TOTorrentFile f : d.getTorrent().getFiles()) {
                if (f.getRelativePath().toLowerCase().contains(keyword)) {
                    return true;
                }
            }

            return false;

        }
        /**
         * When here, display name matches.
         */
        return true;
    }

    long keyword_time = 0;

    private boolean matchKeywords(DownloadManager d, String[] inKeywords) {
        long start = System.currentTimeMillis();

        if (inKeywords == null)
            return true;

        if (inKeywords.length == 0)
            return true;

        boolean matchedSpecial;
        for (String keyword : inKeywords) {
            matchedSpecial = false;

            for (SpecialMatcher sm : SpecialMatcher.values()) {
                if (keyword.toLowerCase().startsWith(sm.mLabel)) {
                    if (sm.match(d, keyword.toLowerCase().substring(sm.mLabel.length()))) {
                        matchedSpecial = true;
                        break;
                    }
                }
            }

            if (matchedSpecial == false) {
                if (deepKeywordMatch(d, keyword.toLowerCase()) == false) {
                    keyword_time += System.currentTimeMillis() - start;
                    return false;
                }
            }
        }

        keyword_time += System.currentTimeMillis() - start;
        return true;
    }

    // public static String getLargestFileName(DownloadManager dm) {
    // if (dm.getTorrent() != null) {
    // TOTorrentFile[] files_orig = dm.getTorrent().getFiles();
    //
    // TOTorrentFile largest = files_orig[0];
    // for (TOTorrentFile f : files_orig) {
    // if (f.getLength() > largest.getLength()) {
    // largest = f;
    // }
    // }
    //
    // return largest.getRelativePath();
    // } else {
    // System.err.println("null torrent!: " + dm.getDisplayName());
    // return "";
    // }
    // }

    private boolean matchType(DownloadManager d, FileTypeFilter inFileType) {
        final TOTorrentFile biggestFile = CoreTools.getBiggestFile(d, false);
        String largestFileName = "";
        if (biggestFile != null) {
            largestFileName = biggestFile.getRelativePath();
        }

        if (inFileType.equals(FileTypeFilter.All)) {
            return true;
        } else if (FileTypeFilter.match(largestFileName).equals(inFileType)) {
            return true;
        }
        return false;
    }

    /**
     * This allows fast deletes (regardless of how long Azureus takes to remove
     * things on the back end)
     */
    Set<String> filteredUntilDeleteBase64 = new HashSet<String>();

    public void filterUntilDelete(byte[] inHash) {
        String toAdd = new String(Base64.encode(inHash));
        filteredUntilDeleteBase64.add(toAdd);
        shouldUpdateClient = true;
        System.out.println("filterUntilDelete, trying force update " + toAdd);
    }

    public class FilteredSwarmInfo {
        public List<DownloadManager> outSwarms = null;
        public int total_swarms_in_type = 0;
        public FileTree tags = null;
        public boolean truncated_tags = false;
    };

    @SuppressWarnings("unchecked")
    public FilteredSwarmInfo filterSwarms(String[] inKeywords, SortMetric inSortingMetric,
            FileTypeFilter inFileType, boolean includeF2F, int selectedFriendID, String inTagPath) {

        keyword_time = 0;
        long start = System.currentTimeMillis();

        Set<String> stillFilteredUntilDelete = new HashSet<String>();

        FilteredSwarmInfo outInfo = new FilteredSwarmInfo();
        outInfo.outSwarms = new ArrayList<DownloadManager>();

        /**
         * We start by generating the list of all swarm and then sorting
         * according to the metric
         */

        logger.finer("filtered contains: " + filteredUntilDeleteBase64.size());
        List<DownloadManager> filteredExceptByTag = new LinkedList<DownloadManager>();

        if (selectedFriendID != Integer.MIN_VALUE) {
            /**
             * If this isn't MIN_VALUE, we're looking at a single friend's files
             * only.
             */
            List<DownloadManager> singleFriendsFiles = getSingleFriendsFiles(selectedFriendID);

            for (DownloadManager d : singleFriendsFiles) {
                if (matchKeywords(d, inKeywords) == false) {
                    continue;
                }

                filteredExceptByTag.add(d);

                if (matchTags(d, inTagPath) == false) {
                    continue;
                }

                outInfo.outSwarms.add(d);
            }

            outInfo.tags = getTagsFromSwarms(filteredExceptByTag);
            outInfo.total_swarms_in_type = outInfo.outSwarms.size();
        } else {
            Set<String> ourHashes = new HashSet<String>();
            /**
             * First, add all our files
             */
            for (DownloadManager d : (List<DownloadManager>) AzureusCoreImpl.getSingleton()
                    .getGlobalManager().getDownloadManagers()) {
                if (d.getTorrent() == null) {
                    logger.warning("null torrent!: " + d.getDisplayName());
                    continue;
                }
                if (d.getDownloadState().getBooleanAttribute(
                        Sha1DownloadManager.ONESWARM_AUTO_ADDED)) {
                    continue;
                }
                // clear this if it exists to avoid sorting problems in the main
                // view.
                d.setData("friend-added-time", null);

                if (matchType(d, inFileType) == false) {
                    continue;
                }

                String base64Hash = null;
                try {
                    base64Hash = new String(Base64.encode(d.getTorrent().getHash()));
                } catch (TOTorrentException e) {
                    e.printStackTrace();
                    continue;
                }

                /**
                 * This allows fast deletes (regardless of how long Azureus
                 * takes to remove things on the back end)
                 */
                if (filteredUntilDeleteBase64.contains(base64Hash)) {
                    stillFilteredUntilDelete.add(base64Hash);
                    continue;
                }

                outInfo.total_swarms_in_type++;

                if (matchKeywords(d, inKeywords) == false) {
                    continue;
                }

                /**
                 * We need to maintain this list to preserve the list of tags
                 * (which we also generate here) even if a tag filter has been
                 * set.
                 */
                filteredExceptByTag.add(d);

                if (matchTags(d, inTagPath) == false) {
                    continue;
                }

                outInfo.outSwarms.add(d);
                ourHashes.add(base64Hash);

            }
            filteredUntilDeleteBase64 = stillFilteredUntilDelete;
            /**
             * Next, if selected, friend's files
             */
            if (includeF2F) {
                Set<String> friendHashes = new HashSet<String>();
                for (Friend friend : mCore.getF2FInterface().getOnlineFileLists().keySet()) {
                    FileList fileList = mCore.getF2FInterface().getOnlineFileLists().get(friend);
                    if (fileList == null) {
                        logger.warning("null filelist for friend!!: " + friend.getNick());
                        continue;
                    }

                    for (FileCollection collection : fileList.getElements()) {
                        /**
                         * Skip files that multiple friends have. We'll discover
                         * these if needed during download. Also skip files that
                         * we have locally
                         */
                        if (friendHashes.contains(collection.getUniqueID())
                                || ourHashes.contains(collection.getUniqueID())) {
                            continue;
                        }

                        DownloadManagerAdapter adapter = new DownloadManagerAdapter(collection,
                                friend.getConnectionId(), friend.getNick());

                        if (matchType(adapter, inFileType) == false)
                            continue;

                        outInfo.total_swarms_in_type++;

                        if (matchKeywords(adapter, inKeywords) == false)
                            continue;

                        filteredExceptByTag.add(adapter);

                        if (matchTags(adapter, inTagPath) == false) {
                            continue;
                        }

                        outInfo.outSwarms.add(adapter);

                        friendHashes.add(collection.getUniqueID());
                    }
                }
            }
        }

        outInfo.tags = getTagsFromSwarms(filteredExceptByTag);

        int maxTags = COConfigurationManager.getIntParameter("oneswarm.max.ui.tags");
        if (maxTags != 0) {
            MutableInt count = new MutableInt();
            count.v = 0;
            outInfo.truncated_tags = pruneTagsBasedOnLimit(outInfo.tags, count, maxTags);
        }

        /**
         * Finally, sort
         */
        Collections.sort(outInfo.outSwarms, inSortingMetric.mComp);

        logger.fine("keyword time: " + keyword_time + " total: "
                + (System.currentTimeMillis() - start));

        return outInfo;
    }

    static class MutableInt {
        public int v;
    }

    private boolean pruneTagsBasedOnLimit(FileTree tags, MutableInt count, int max) {
        /**
         * Visit this node.
         */
        count.v++;

        /**
         * Visit children in order
         */
        int removed = 0;
        for (int i = 0; i < tags.getChildren().length; i++) {
            if (count.v > max) {
                tags.getChildren()[i] = null;
                removed++;
            } else {
                count.v++;
            }
        }

        /**
         * Compact if we removed anything
         */
        if (removed > 0) {
            FileTree[] neu = new FileTree[tags.getChildren().length - removed];
            int where = 0;
            for (FileTree old : tags.getChildren()) {
                if (old != null) {
                    neu[where++] = old;
                }
            }
            tags.setChildren(neu);
        }

        boolean truncatedKid = false;
        for (int i = 0; i < tags.getChildren().length; i++) {
            truncatedKid = truncatedKid || pruneTagsBasedOnLimit(tags.getChildren()[i], count, max);
        }
        return removed > 0 || truncatedKid;
    }

    private boolean matchTags(DownloadManager d, String inTagPath) {

        if (inTagPath == null) {
            return true;
        }

        if (inTagPath.length() == 0) {
            return true;
        }

        String[] tags = d.getDownloadState().getListAttribute(
                FileCollection.ONESWARM_TAGS_ATTRIBUTE);

        if (tags == null) {
            return false;
        }

        for (String tag : tags) {
            if (tag.startsWith(inTagPath)) {
                return true;
            }
        }

        return false;
    }

    public static FileTree getTagsFromSwarms(List<DownloadManager> outSwarms) {
        try {

            TreeScratch root = new TreeScratch("");

            if (COConfigurationManager.getBooleanParameter("oneswarm.show.tags") == false) {
                return convertScratchToFileTree(root); // no tags
            }

            for (org.gudy.azureus2.core3.download.DownloadManager dm : outSwarms) {
                String[] tags_raw = dm.getDownloadState().getListAttribute(
                        FileCollection.ONESWARM_TAGS_ATTRIBUTE);
                if (tags_raw == null) {
                    continue;
                }
                // convert this into a set of paths
                List<List<String>> tags = new LinkedList<List<String>>();
                for (String tag : tags_raw) {
                    LinkedList<String> ll = new LinkedList<String>();
                    for (String s : tag.split("/")) {
                        ll.add(s);
                    }
                    tags.add(ll);
                }

                for (List<String> path : tags) {
                    findTreeMatch(root, path);
                }
            }

            // now convert the TreeScratch into a FileTree to deliver to client
            return convertScratchToFileTree(root);
        } catch (Exception e) {
            System.err.println(e.toString());
        }
        return null;
    }

    private static FileTree convertScratchToFileTree(TreeScratch node) {
        FileTree ftree = new FileTree();
        ftree.setName(node.tagName);
        if (node.kids.size() == 0) {
            return ftree;
        } else {
            FileTree[] convertedKids = new FileTree[node.kids.size()];
            int i = 0;
            for (TreeScratch k : node.kids) {
                convertedKids[i++] = convertScratchToFileTree(k);
            }

            Arrays.sort(convertedKids, new Comparator<FileTree>() {
                public int compare(FileTree o1, FileTree o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });

            ftree.setChildren(convertedKids);
            return ftree;
        }
    }

    private static void findTreeMatch(TreeScratch node, List<String> remainingMatch) {
        if (remainingMatch.size() == 0) {
            // duplicate
            return;
        }

        for (TreeScratch kid : node.kids) {
            if (remainingMatch.get(0).equals(kid.tagName)) {
                remainingMatch.remove(0);
                findTreeMatch(kid, remainingMatch);
                return;
            }
        }
        // need to add the rest of these as kids in the unified tree
        TreeScratch curr = node;
        for (String tag : remainingMatch) {
            TreeScratch neu = new TreeScratch(tag);
            curr.kids.add(neu);
            curr = neu;
        }
    }
}
