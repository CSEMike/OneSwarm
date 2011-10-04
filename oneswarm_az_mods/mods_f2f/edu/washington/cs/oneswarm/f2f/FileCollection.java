package edu.washington.cs.oneswarm.f2f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.torrent.impl.TOTorrentImpl;

/**
 *	Reducing the memory footprint of this class would be great. 
 */

public class FileCollection
{

	private Logger							logger											 = Logger.getLogger(FileCollection.class.getName());

	private Map<String, String> optionalFields							 = new HashMap<String, String>();

	public static final byte		TYPE_BITTORRENT							= 0;

	private byte								type;

	private transient byte[]		uniqueIDBytes;

	private String							uniqueID										 = "";

	private String							name												 = "";

	private String							description									= "";

	private String							category										 = "";

	private int								 hash												 = -1;

	private List<FileListFile>	children;

	private List<List<String>>	directoryTags								= new LinkedList<List<String>>();

	private long								addedTimeUTC;

	public static final String	ONESWARM_ARTIST_ATTRIBUTE		= TOTorrentImpl.OS_ARTIST;

	public static final String	ONESWARM_ALBUM_ATTRIBUTE		 = TOTorrentImpl.OS_ALBUM;

	public static final String	ONESWARM_TAGS_ATTRIBUTE			= TOTorrentImpl.OS_TAGS;

	public static final String	ONESWARM_STREAM_ATTRIBUTE = TOTorrentImpl.OS_NO_STREAM;

	public long getAddedTimeUTC() {
		return addedTimeUTC;
	}

	public void setAddedTimeUTC(long addedTimeUTC) {
		this.addedTimeUTC = addedTimeUTC;
	}

	private void setOptionalFields(Map<String, String> optionalFields) {
		this.optionalFields = optionalFields;
	}

	public FileCollection(byte type, String uniqueID, String name,
			String description, String category, List<FileListFile> children,
			long addedTimeUTC) {
		this.type = type;
		this.uniqueID = uniqueID;
		this.uniqueIDBytes = Base64.decode(uniqueID);
		this.name = name;
		this.description = description;
		this.category = category;
		this.children = children;
		this.addedTimeUTC = addedTimeUTC;
		calcHash();
		// System.out.println("Created listelement: " + toString());

	}

	public String getCategory() {
		return category;
	}

	public String getDescription() {
		return description;
	}

	public void calcHash() {
		hash = Arrays.hashCode(uniqueIDBytes) + directoryTags.hashCode();
	}

	public String getName() {
		return name;
	}

	public byte getType() {
		return type;
	}

	public String getUniqueID() {
		return uniqueID;
	}

	public byte[] getUniqueIdBytes() {
		return uniqueIDBytes;
	}

	public int hashCode() {
		return hash;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public void setDescription(String decription) {
		this.description = decription;
	}

	public void setHash(int hash) {
		this.hash = hash;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setType(byte type) {
		this.type = type;
	}

	public void setUniqueID(String uniqueID) {
		this.uniqueID = uniqueID;
		this.uniqueIDBytes = Base64.decode(uniqueID);
	}

	public String toString() {
		return "t=" + type + " u=" + uniqueID + " n=" + name + " c=" + category
				+ " d=" + description + " h=" + hashCode();
	}

	public int getHash() {
		return hash;
	}

	public List<FileListFile> getChildren() {
		return children;
	}

	public void setChildren(List<FileListFile> children) {
		this.children = children;
	}

	public FileCollection clone() {
		FileCollection fileCollection = new FileCollection(type, uniqueID, name,
				description, category, new ArrayList<FileListFile>(children),
				addedTimeUTC);
		fileCollection.setOptionalFields(optionalFields);
		return fileCollection;
	}

	public void setOptionalField(String key, String value) {
		optionalFields.put(key, value);
	}

	/**
	 * Method for handling search, current policy
	 * If the id:xxx keyword is specified the collection must have the specified IDm otherwise null is returned
	 * If the search string matches the collection name, all files are returned
	 * Otherwise a subcollection containing only the files with matching names are returned
	 * If no files match, null is returned
	 * @param searchString
	 * @return
	 */
	public FileCollection searchMatches(String searchString) {
		searchString = removeWhiteSpaceAfteKeyChars(searchString);
		// handle the id: keyword, if id:xxx is specified, only collections with that id are valid
		/*
		 * filter out thing not matching the keywords
		 */
		FileCollection keyWordHits = checkKeyWordMatch(searchString);
		// null = not match on id
		if (keyWordHits == null) {
			return null;
		}
		// switch to lower case from now on
		boolean metaInfoMatch = keyWordHits.nameMatch(searchString);
		// if we have a match in the torrent name, return all files in torrent
		if (metaInfoMatch) {
			return keyWordHits.clone();
		}

		// else, return a new collection that only contains the files that matched
		return keyWordHits.fileMatches(searchString);
	}

	/**
	 * Method for getting a subcollection that only contains the files that matches the search string
	 * @param searchString
	 * @return 
	 */
	public FileCollection fileMatches(String searchString) {
		List<FileListFile> searchMatches = new ArrayList<FileListFile>();
		for (FileListFile file : children) {
			if (file.searchMatch(searchString)) {
				searchMatches.add(file);
			}
		}
		if (searchMatches.size() > 0) {
			FileCollection fileCollection = new FileCollection(type, uniqueID, name,
					description, category, searchMatches, addedTimeUTC);
			fileCollection.setOptionalFields(optionalFields);
			return fileCollection;
		} else {
			return null;
		}

	}

	/**
	 * Method for determining if the name of the collection matches the search string
	 * @param searchString space separated words
	 * @return true if all keywords are in the name, false otherwise
	 */
	public boolean nameMatch(String searchString) {
		searchString = searchString.toLowerCase();
		String n = name.toLowerCase();
		String[] searchTerms = quoteRespectingSplit(searchString);
		for (String term : searchTerms) {
			// ignore terms that contain ':'
			if (!containsKeyword(term)) {
				boolean negatedTerm = term.startsWith("-");
				if (negatedTerm) {
					term = term.substring(1);
					System.out.println("got negated term:" + term);
				}

				// check if the name contains the term
				if (negatedTerm == n.contains(term)) {
					// ok, last shot, does the term exist in any of the optional fields?
					if (negatedTerm == optionalFieldHit(term)) {
						// no match, this term does not match either name nor any optional field
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * returns true if one of the optional fields (like artist or album) contains the term
	 * @param searchString
	 * @return
	 */
	public boolean optionalFieldHit(String term) {
		for (String p : optionalFields.values()) {
			if (p.toLowerCase().contains(term)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Method for getting the largest file in the file collection that has a name that matches the search string
	 * @param searchString space separated words
	 * @return a new collection containing only the largest file that matches
	 */
	public FileCollection getLargestFile(String searchString) {
		searchString = searchString.toLowerCase();
		FileCollection matchingFiles = fileMatches(searchString);
		if (matchingFiles == null) {
			return null;
		} else {
			FileCollection largestMatchingFile = matchingFiles.getLargestFile();

			return largestMatchingFile;
		}
	}

	/**
	 * Method for getting the largest file in the collection
	 * @return a new collection containing only the largest file
	 */
	public FileCollection getLargestFile() {
		FileListFile largestFile = null;
		long largestSize = 0;

		for (FileListFile f : children) {
			if (f.getLength() > largestSize) {
				largestSize = f.getLength();
				largestFile = f;
			}
		}
		List<FileListFile> l = new ArrayList<FileListFile>();
		l.add(largestFile);
		FileCollection fileCollection = new FileCollection(type, uniqueID, name,
				description, category, l, addedTimeUTC);
		fileCollection.setOptionalFields(optionalFields);
		return fileCollection;
	}

	// versions less than 0.67 will respond with everything for : searches (bug...), allow both : and ; for now
	public static String[] KEYWORDENDINGS = new String[] {
		":",
		";"
																				};

	/**
	 * Filters a file connections based on keywords
	 * @param searchString
	 * @return the entire collection if the infohash matches, otherwise a collection with only the files that matched
	 */
	private FileCollection checkKeyWordMatch(String searchString) {
		logger.fine("checking for keyword match: " + searchString);
		//		System.err.println("checking for keyword match: " + searchString);
		String[] termSplit = quoteRespectingSplit(searchString);
		boolean containsKeyWordEnd = containsKeyword(searchString);
		// if the search term doesn't contain any special chars, return true
		if (!containsKeyWordEnd) {
			return this;
		}
		logger.finer(searchString + " contains keyword");
		//System.err.println(searchString + " contains keyword");

		/*
		 * if it matches the infohash, return the entire thing
		 */
		for (String s : termSplit) {
			for (String keyWordEnd : KEYWORDENDINGS) {
				if (s.contains("id" + keyWordEnd)) {
					logger.finest(s + " contains " + keyWordEnd);
					//System.err.println(s + " contains " + keyWordEnd);
					String[] idSplit = s.split(keyWordEnd);
					if (idSplit.length == 2) {
						String id = idSplit[1];
						if (id.equals(this.uniqueID)) {
							logger.fine("id match");
							//System.err.println("id match");
							return this;
						}
					}
					logger.finest("id does not match: " + this.uniqueID + " != " + s);
					//System.err.println("id does not match: " + this.uniqueID + " != " + s);
					return null;
				}
			}
		}
		Set<FileListFile> keywordMatches = new HashSet<FileListFile>();
		/*
		 * or match one of the files
		 */
		for (FileListFile f : children) {
			if (f.keyWordMatch(termSplit)) {
				keywordMatches.add(f);
			}
		}

		if (keywordMatches.size() == 0) {
			return null;
		}
		FileCollection hits = this.clone();
		hits.setChildren(new ArrayList<FileListFile>(keywordMatches));
		return hits;
	}

	public static boolean containsKeyword(String searchString) {
		boolean containsKeyWordEnd = false;
		for (String keyWordEnd : KEYWORDENDINGS) {
			if (searchString.contains(keyWordEnd)) {
				containsKeyWordEnd = true;
			}
		}
		return containsKeyWordEnd;
	}

	public int getFileNum() {
		return children.size();
	}

	public static String removeWhiteSpaceAfteKeyChars(String input) {
		for (String keyword : KEYWORDENDINGS) {
			input = removeWhiteSpaceAfterKeyword(input, keyword);
		}
		return input;
	}

	public static String removeWhiteSpaceAfterKeyword(String input, String keyword) {
		boolean hasColon = input.contains(keyword);
		if (!hasColon) {
			return input;
		}
		boolean afterColon = false;

		StringBuilder b = new StringBuilder();
		char[] chars = input.toCharArray();
		for (char c : chars) {
			if (c == ':') {
				afterColon = true;
				b.append(c);
			} else if (Character.isWhitespace(c)) {
				if (afterColon) {
					// remove...
					continue;
				} else {
					b.append(c);
				}
			} else {
				b.append(c);
				afterColon = false;
			}
		}
		return b.toString();
	}

	public static String[] quoteRespectingSplit(String input) {
		/*
		 * quick check to see if we need to split, could be a single word
		 */
		if (!input.contains(" ")) {
			return new String[] {
				input
			};
		}

		//boolean withinSingle = false;
		boolean withinDouble = false;

		char[] chars = input.toCharArray();
		StringBuilder currentTerm = new StringBuilder();
		LinkedList<String> terms = new LinkedList<String>();

		for (char c : chars) {
			if (c == ' ') {
				// space breaks the term, unless we are within quotes, 
				// then it gets added to the current term
				if (withinDouble) {
					currentTerm.append(c);
				} else {
					terms.add(currentTerm.toString());
					currentTerm = new StringBuilder();
				}
			} else if (c == '"' || c == '\'') {
				withinDouble = !withinDouble;
			} else {
				currentTerm.append(c);
			}
		}
		// check if we have any leftovers
		if (currentTerm.length() > 0) {
			terms.add(currentTerm.toString());
		}

		return terms.toArray(new String[terms.size()]);
	}

	public static void main(String[] args) {
		String[] split = quoteRespectingSplit(removeWhiteSpaceAfteKeyChars("this is a \"single unit\" test another \"second unit\" and one more 'test test' and id: \"this is a long id\""));
		for (String s : split) {
			System.out.println(s);
		}

		String colonFix = removeWhiteSpaceAfteKeyChars("id: bajs");
		System.out.println(colonFix);

	}

	public List<List<String>> getDirectoryTags() {
		return directoryTags;
	}

	public void setDirectoryTags(List<List<String>> directoryTags) {
		this.directoryTags = directoryTags;
		calcHash();
	}

	public boolean containsFile(FileListFile f) {
		for (FileListFile c : children) {
			if (c.equals(f)) {
				return true;
			}
		}
		return false;
	}

}
