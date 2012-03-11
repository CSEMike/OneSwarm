/*
 * Created on May 30, 2006 7:32:56 PM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.core3.internat;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.*;

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.TorrentUtils;

/**
 * Locale functions specific to Torrents.

 * @note Moved from LocaleUtil to keep (cyclical) dependencies down.
 */
public class LocaleTorrentUtil
{
	private static List listeners = new ArrayList();

	/**
	 * Retrieves the encoding of the torrent if it can be determined.
	 * <br>
	 * Does not prompt the user with choices.
	 * 
	 * @param torrent Torrent to get encoding of
	 * @return Locale torrent is in
	 * 
	 * @throws TOTorrentException
	 * @throws UnsupportedEncodingException
	 */
	static public LocaleUtilDecoder getTorrentEncodingIfAvailable(
			TOTorrent torrent)

		throws TOTorrentException, UnsupportedEncodingException
	{
		String encoding = torrent.getAdditionalStringProperty("encoding");
		
		if (encoding == null) {
			return null;
		}
		if (TOTorrent.ENCODING_ACTUALLY_UTF8_KEYS.equals(encoding)) {
			encoding = "utf8";
		}

		// get canonical name

		String canonical_name;

		try {
			canonical_name = Charset.forName(encoding).name();

		} catch (Throwable e) {

			canonical_name = encoding;
		}

		LocaleUtilDecoder chosenDecoder = null;
		LocaleUtilDecoder[] all_decoders = LocaleUtil.getSingleton().getDecoders();

		for (int i = 0; i < all_decoders.length; i++) {
			if (all_decoders[i].getName().equals(canonical_name)) {
				chosenDecoder = all_decoders[i];
				break;
			}
		}
		
		return chosenDecoder;
	}

	/**
	 * Get the torrent's encoding, prompting the user to choose from a list
	 * if needed.
	 * 
	 * @param torrent Torrent to get encoding of
	 * @return LocaleUtilDecoder that the torrent is in
	 * 
	 * @throws TOTorrentException
	 * @throws UnsupportedEncodingException
	 * 
	 */
	// TODO: Use getTorrentEncodingIfAvailable instead of almost-similar code
	//        (merge anything cool from this function's dup code into 
	//         getTorrentEncodingIfAvailable)
	static public LocaleUtilDecoder getTorrentEncoding(TOTorrent torrent)

		throws TOTorrentException, UnsupportedEncodingException
	{
		return getTorrentEncoding(torrent, true);
	}
	
	static public LocaleUtilDecoder getTorrentEncoding(TOTorrent torrent, boolean saveToFileAllowed)

	throws TOTorrentException, UnsupportedEncodingException
	{
		String encoding = torrent.getAdditionalStringProperty("encoding");
		if (TOTorrent.ENCODING_ACTUALLY_UTF8_KEYS.equals(encoding)) {
			encoding = "utf8";
		}

		// we can only persist the torrent if it has a filename defined for it

		boolean bSaveToFile;

		try {
			TorrentUtils.getTorrentFileName(torrent);

			bSaveToFile = true;

		} catch (Throwable e) {

			bSaveToFile = false;
		}

		if (encoding != null) {

			// get canonical name

			try {
				LocaleUtilDecoder[] all_decoders = LocaleUtil.getSingleton().getDecoders();
				LocaleUtilDecoder fallback_decoder = LocaleUtil.getSingleton().getFallBackDecoder();

				String canonical_name = encoding.equals(fallback_decoder.getName())
						? encoding : Charset.forName(encoding).name();

				for (int i = 0; i < all_decoders.length; i++) {

					if (all_decoders[i].getName().equals(canonical_name)) {

						return (all_decoders[i]);
					}
				}
			} catch (Throwable e) {

				Debug.printStackTrace(e);
			}
		}

		// get the decoders valid for various localisable parts of torrent content
		// not in any particular order

		LocaleUtilDecoderCandidate[] candidates = getTorrentCandidates(torrent);

		boolean system_decoder_is_valid = false;

		LocaleUtil localeUtil = LocaleUtil.getSingleton();
		LocaleUtilDecoder system_decoder = localeUtil.getSystemDecoder();
		for (int i = 0; i < candidates.length; i++) {
			if (candidates[i].getDecoder() == system_decoder) {
				system_decoder_is_valid = true;
				break;
			}
		}

		LocaleUtilDecoder selected_decoder = null;

		for (int i = 0; i < listeners.size(); i++) {

			LocaleUtilDecoderCandidate candidate = null;
			try {
				candidate = ((LocaleUtilListener) listeners.get(i)).selectDecoder(
						localeUtil, torrent, candidates);
			} catch (LocaleUtilEncodingException e) {
			}

			if (candidate != null) {

				selected_decoder = candidate.getDecoder();

				break;
			}

			bSaveToFile = false;
		}

		if (selected_decoder == null) {

			// go for system decoder, if valid, fallback if not

			if (system_decoder_is_valid) {

				selected_decoder = localeUtil.getSystemDecoder();

			} else {

				selected_decoder = localeUtil.getFallBackDecoder();
			}
		}

		torrent.setAdditionalStringProperty("encoding", selected_decoder.getName());

		if (bSaveToFile && saveToFileAllowed) {
			TorrentUtils.writeToFile(torrent);
		}

		return (selected_decoder);
	}

	/**
	 * Checks the Torrent's text fields (path, comment, etc) against a list
	 * of locals, returning only those that can handle all the fields.
	 * 
	 * @param torrent
	 * @return
	 * @throws TOTorrentException
	 * @throws UnsupportedEncodingException
	 */
	static protected LocaleUtilDecoderCandidate[] getTorrentCandidates(
			TOTorrent torrent)

		throws TOTorrentException, UnsupportedEncodingException
	{
		long lMinCandidates;
		byte[] minCandidatesArray;

		Set cand_set = new HashSet();
		LocaleUtil localeUtil = LocaleUtil.getSingleton();

		List candidateDecoders = localeUtil.getCandidateDecoders(torrent.getName());
		lMinCandidates = candidateDecoders.size();
		minCandidatesArray = torrent.getName();

		cand_set.addAll(candidateDecoders);

		TOTorrentFile[] files = torrent.getFiles();

		for (int i = 0; i < files.length; i++) {

			TOTorrentFile file = files[i];

			byte[][] comps = file.getPathComponents();

			for (int j = 0; j < comps.length; j++) {
				candidateDecoders = localeUtil.getCandidateDecoders(comps[j]);
				if (candidateDecoders.size() < lMinCandidates) {
					lMinCandidates = candidateDecoders.size();
					minCandidatesArray = comps[j];
				}
				cand_set.retainAll(candidateDecoders);
			}
		}

		byte[] comment = torrent.getComment();

		if (comment != null) {
			candidateDecoders = localeUtil.getCandidateDecoders(comment);
			if (candidateDecoders.size() < lMinCandidates) {
				lMinCandidates = candidateDecoders.size();
				minCandidatesArray = comment;
			}
			cand_set.retainAll(candidateDecoders);
		}

		byte[] created = torrent.getCreatedBy();

		if (created != null) {
			candidateDecoders = localeUtil.getCandidateDecoders(created);
			if (candidateDecoders.size() < lMinCandidates) {
				lMinCandidates = candidateDecoders.size();
				minCandidatesArray = created;
			}
			cand_set.retainAll(candidateDecoders);
		}

		List candidatesList = localeUtil.getCandidatesAsList(minCandidatesArray);
		LocaleUtilDecoderCandidate[] candidates;
		candidates = new LocaleUtilDecoderCandidate[candidatesList.size()];
		candidatesList.toArray(candidates);

		Arrays.sort(candidates, new Comparator() {
			public int compare(Object o1, Object o2) {
				LocaleUtilDecoderCandidate luc1 = (LocaleUtilDecoderCandidate) o1;
				LocaleUtilDecoderCandidate luc2 = (LocaleUtilDecoderCandidate) o2;

				return (luc1.getDecoder().getIndex() - luc2.getDecoder().getIndex());
			}
		});

		return candidates;
	}

	static public void setTorrentEncoding(TOTorrent torrent, String encoding)

		throws LocaleUtilEncodingException
	{
		try {
			LocaleUtil localeUtil = LocaleUtil.getSingleton();
			LocaleUtilDecoderCandidate[] candidates = getTorrentCandidates(torrent);

			String canonical_requested_name;

			// "System" means use the system encoding

			if (encoding.equalsIgnoreCase("system")) {

				canonical_requested_name = localeUtil.getSystemEncoding();

			} else if (encoding.equalsIgnoreCase(LocaleUtilDecoderFallback.NAME)) {

				canonical_requested_name = LocaleUtilDecoderFallback.NAME;

			} else {

				CharsetDecoder requested_decoder = Charset.forName(encoding).newDecoder();

				canonical_requested_name = requested_decoder.charset().name();
			}

			boolean ok = false;

			for (int i = 0; i < candidates.length; i++) {

				if (candidates[i].getDecoder().getName().equals(
						canonical_requested_name)) {

					ok = true;

					break;
				}
			}

			if (!ok) {

				String[] charsets = new String[candidates.length];
				String[] names = new String[candidates.length];

				for (int i = 0; i < candidates.length; i++) {

					LocaleUtilDecoder decoder = candidates[i].getDecoder();

					charsets[i] = decoder.getName();
					names[i] = decoder.decodeString(torrent.getName());
				}

				throw (new LocaleUtilEncodingException(charsets, names));
			}

			torrent.setAdditionalStringProperty("encoding", canonical_requested_name);

		} catch (Throwable e) {

			if (e instanceof LocaleUtilEncodingException) {

				throw ((LocaleUtilEncodingException) e);
			}

			throw (new LocaleUtilEncodingException(e));
		}
	}

	static public void setDefaultTorrentEncoding(TOTorrent torrent)

		throws LocaleUtilEncodingException
	{
		setTorrentEncoding(torrent, Constants.DEFAULT_ENCODING);
	}

	static public String getCurrentTorrentEncoding(TOTorrent torrent) {
		return (torrent.getAdditionalStringProperty("encoding"));
	}

	public static void addListener(LocaleUtilListener l) {
		listeners.add(l);
	}

	public static void removeListener(LocaleUtilListener l) {
		listeners.remove(l);
	}

}
