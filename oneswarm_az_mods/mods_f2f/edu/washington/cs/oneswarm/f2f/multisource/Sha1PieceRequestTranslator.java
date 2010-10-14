package edu.washington.cs.oneswarm.f2f.multisource;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.disk.DiskManagerReadRequest;
import org.gudy.azureus2.core3.disk.DiskManagerReadRequestListener;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.peer.impl.PEPeerControl;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;
import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.peermanager.piecepicker.util.BitFlags;

import edu.washington.cs.oneswarm.f2f.FileListFile;

public class Sha1PieceRequestTranslator
{
	private final static Logger	 logger = Logger.getLogger(Sha1PieceRequestTranslator.class.getName());

	private final DownloadManager dstDownloadManager;

	private final PEPeerControl	 sourceManager;

	private final DownloadManager srcDownloadManager;

	private final Sha1Peer				virtualPeer;

	private final VirtualPiece[]	virtualPieces;

	public Sha1PieceRequestTranslator(Sha1Peer peer,
			DownloadManager sourceDownloadManager,
			DownloadManager destinationDownloadManager) throws TOTorrentException,
			PieceTranslationExcetion {
		logger.fine("created sha1piecetranslator: "
				+ sourceDownloadManager.getDisplayName() + "->"
				+ destinationDownloadManager.getDisplayName());
		this.virtualPeer = peer;
		this.srcDownloadManager = sourceDownloadManager;
		this.sourceManager = (PEPeerControl) sourceDownloadManager.getPeerManager();
		this.dstDownloadManager = destinationDownloadManager;

		if (logger.isLoggable(Level.FINER)) {
			printTorrentInfo();
		}
		this.virtualPieces = createVirtualPieces();
		logger.finer("created virtual pieces, " + getAvailable().nbSet + "/"
				+ virtualPieces.length + " pieces available");
	}

	private VirtualPiece[] createVirtualPieces() throws TOTorrentException,
			PieceTranslationExcetion {
		VirtualPiece[] pieces = new VirtualPiece[dstDownloadManager.getNbPieces()];

		// for each file in the destination torrent, try to locate it in the
		// source torrent
		TOTorrent destTorrent = dstDownloadManager.getTorrent();
		TOTorrent sourceTorrent = srcDownloadManager.getTorrent();

		TOTorrentFile[] destFiles = destTorrent.getFiles();
		HashWrapper[] destFileSha1s = Sha1DownloadManager.getHashesFromDownload(
				dstDownloadManager, FileListFile.KEY_SHA1_HASH, false);
		HashWrapper[] srcFileSha1s = Sha1DownloadManager.getHashesFromDownload(
				srcDownloadManager, FileListFile.KEY_SHA1_HASH, false);
		TOTorrentFile[] sourceFiles = sourceTorrent.getFiles();
		for (int i = 0; i < destFiles.length; i++) {
			TOTorrentFile destFile = destFiles[i];
			HashWrapper destFileSha1 = destFileSha1s[i];

			for (int j = 0; j < sourceFiles.length; j++) {
				TOTorrentFile sourceFile = sourceFiles[j];
				HashWrapper sourceFileSha1 = srcFileSha1s[j];

				if (destFileSha1.equals(sourceFileSha1)) {
					logger.fine("found sha1 match: " + destFile.getRelativePath() + "=="
							+ sourceFile.getRelativePath());

					DiskManagerPiece[] dstPieces = dstDownloadManager.getDiskManager().getPieces();
					DiskManagerPiece[] srcPieces = srcDownloadManager.getDiskManager().getPieces();

					long dstFileStartPosInTorrent = getFileStartPosInTorrent(destFiles, i);
					long srcFileStartPosInTorrent = getFileStartPosInTorrent(sourceFiles,
							j);
					logger.finer("src file starts at: " + srcFileStartPosInTorrent);
					logger.finer("dst file starts at: " + dstFileStartPosInTorrent);
					/*
					 * ok, time for the tricky part
					 * 
					 * for each piece in the destination file we need to create
					 * a virtual piece and put in in the pieces array
					 */
					file_loop: for (int pieceNum = destFile.getFirstPieceNumber(); pieceNum <= destFile.getLastPieceNumber(); pieceNum++) {
						// for each virtual piece we need:
						// the start offset, that is the position in the source
						// piece where the destination piece starts
						// + the pieces in the source that corresponds to the
						// current piece in the destination

						// get the position in the destination file for the
						// current piece
						logger.finest("creating virtual piece: " + pieceNum);
						long dstPiecePosInTorrent = 0;

						for (int k = 0; k < pieceNum; k++) {
							dstPiecePosInTorrent += dstPieces[k].getLength();
						}
						logger.finest("dst piece starts at pos " + dstPiecePosInTorrent
								+ " in torrent");
						// this piece is the first piece in the file, the
						// beginning of
						// it will contain parts of other files... nothing we
						// can do, lets try the next one
						if (dstFileStartPosInTorrent > dstPiecePosInTorrent) {
							logger.finest("dst piece starts before file, skipping");
							continue file_loop;
						}

						// ok, we found the position in the file were we are,
						// next, find what piece+offset that corresponds to in
						// the source file
						long dstPiecePosInFile = dstPiecePosInTorrent
								- dstFileStartPosInTorrent;
						logger.finest("dst piece starts " + dstPiecePosInFile
								+ " bytes into the file");
						/*
						 * next, we need to find the pieces that contains the
						 * range
						 * [dstPiecePosInFile,dstPiecePosInFile+currentDstPieceSize
						 * ]
						 */

						// start by finding the first piece and the piece offset
						long dstPieceStartPosInSrcTorrent = srcFileStartPosInTorrent
								+ dstPiecePosInFile;
						logger.finest("destination piece starts at "
								+ dstPieceStartPosInSrcTorrent + " in src torrent");
						int firstSrcPieceNum = 0;
						long currentPieceStartPos = 0;
						int firstSrcPieceOffset = 0;
						for (int k = 0; k < srcPieces.length; k++) {
							long nextPos = currentPieceStartPos + srcPieces[k].getLength();
							if (nextPos > dstPieceStartPosInSrcTorrent) {
								// ok, the current piece contains the byte
								// get the offset (the number of bytes from the
								// piece start pos in the
								firstSrcPieceOffset = (int) (dstPieceStartPosInSrcTorrent - currentPieceStartPos);
								break;
							}
							firstSrcPieceNum++;
							currentPieceStartPos = nextPos;
						}
						logger.finest("first src piece include dst piece: "
								+ firstSrcPieceNum);
						logger.finest("first src piece offset: " + firstSrcPieceOffset);

						// great, next step, figure out if we need to add more
						// pieces
						int dstPieceLength = dstPieces[pieceNum].getLength();

						ArrayList<Integer> piecesForVirtualPiece = new ArrayList<Integer>();
						piecesForVirtualPiece.add(firstSrcPieceNum);
						long bytesCovered = srcPieces[firstSrcPieceNum].getLength()
								- firstSrcPieceOffset;
						int nextPieceNum = firstSrcPieceNum + 1;
						while (bytesCovered < dstPieceLength) {
							// deal with last piece issues
							if (nextPieceNum >= srcPieces.length) {
								logger.finest("reached last piece, covered=" + bytesCovered
										+ " needed=" + dstPieceLength + ", skipping");
								continue file_loop;
							}

							piecesForVirtualPiece.add(new Integer(nextPieceNum));
							bytesCovered += srcPieces[nextPieceNum].getLength();
							nextPieceNum++;

						}

						// excellent! create the piece
						int[] pieceMapping = new int[piecesForVirtualPiece.size()];
						for (int k = 0; k < pieceMapping.length; k++) {
							pieceMapping[k] = piecesForVirtualPiece.get(k).intValue();
						}
						pieces[pieceNum] = new VirtualPiece(pieceNum, pieceMapping,
								firstSrcPieceOffset);
						logger.finest("created virtual piece: "
								+ pieces[pieceNum].toString());
					}
				}
			}
		}

		return pieces;
	}

	public BitFlags getAvailable() throws PieceTranslationExcetion {
		boolean[] available = new boolean[virtualPieces.length];
		for (int i = 0; i < virtualPieces.length; i++) {
			available[i] = isPieceCompleted(i);
		}

		BitFlags a = new BitFlags(available);
		return a;
	}

	private boolean isPieceCompleted(int pieceNumber)
			throws PieceTranslationExcetion {
		if (virtualPieces[pieceNumber] != null) {
			return virtualPieces[pieceNumber].isDone();
		}
		return false;
	}

	private void printTorrentInfo() {
		logger.finer("source torrent:");
		printTorrentInfo(srcDownloadManager.getTorrent());
		logger.finer("destination torrent:");
		printTorrentInfo(dstDownloadManager.getTorrent());
	}

	public void request(DiskManagerReadRequest request,
			DiskManagerReadRequestListener listener) {
		try {
			logger.finest("got read request: piece=" + request.getPieceNumber()
					+ " offset=" + request.getOffset() + " length=" + request.getLength());

			int pieceNumber = request.getPieceNumber();
			if (pieceNumber < 0 || pieceNumber >= virtualPieces.length) {
				listener.readFailed(request, new Exception("invalid piece number"));
			}
			VirtualPiece piece = virtualPieces[pieceNumber];
			if (!piece.isDone()) {
				listener.readFailed(request, new Exception(
						"requested virtual piece not completed"));
			}
			piece.enqueueReadRequest(request, listener);
		} catch (Throwable t) {
			listener.readFailed(request, t);
			t.printStackTrace();
		}
	}

	private static long getFileStartPosInTorrent(TOTorrentFile[] files, int index) {
		long fileStartPosInTorrent = 0;
		for (int i = 0; i < index; i++) {
			fileStartPosInTorrent += files[i].getLength();
		}
		return fileStartPosInTorrent;
	}

	//	@SuppressWarnings("unchecked")
	//	private static HashWrapper getSha1FromTorrent(TOTorrent t, Map torrentMap, int fileIndex) throws TOTorrentException {
	//
	//		if (t.isSimpleTorrent()) {
	//			return getSha1FromTorrentMap(torrentMap);
	//		} else {
	//			Object filePropertiesObj = torrentMap.get("files");
	//			if (filePropertiesObj == null || !(filePropertiesObj instanceof List)) {
	//				throw new TOTorrentException("no files map in non-simple torrent!", TOTorrentException.RT_UNSUPPORTED_ENCODING);
	//			}
	//
	//			List fileProperties = (List) filePropertiesObj;
	//			if (fileProperties.size() <= fileIndex) {
	//				throw new TOTorrentException("invalid index!", TOTorrentException.RT_UNSUPPORTED_ENCODING);
	//			}
	//			Object pObj = fileProperties.get(fileIndex);
	//			if (!(pObj instanceof Map)) {
	//				throw new TOTorrentException("invalid file properties map", TOTorrentException.RT_UNSUPPORTED_ENCODING);
	//			}
	//			Map pMap = (Map) pObj;
	//			return getSha1FromTorrentMap(pMap);
	//		}
	//	}
	//
	//	@SuppressWarnings("unchecked")
	//	private static HashWrapper getSha1FromTorrentMap(Map torrentMap) throws TOTorrentException {
	//		if (torrentMap.containsKey(FileListFile.KEY_SHA1_HASH)) {
	//			byte[] hash = (byte[]) torrentMap.get(FileListFile.KEY_SHA1_HASH);
	//			return new HashWrapper(hash);
	//		}
	//		throw new TOTorrentException("no sha1 in map", TOTorrentException.RT_UNSUPPORTED_ENCODING);
	//	}

	private static void printTorrentInfo(TOTorrent torrent) {
		logger.finer(new String(torrent.getName()));
		logger.finer("length: " + torrent.getSize());
		logger.finer("pieces: " + torrent.getNumberOfPieces());
		logger.finer("piece length: " + torrent.getPieceLength());
		logger.finer("files: " + torrent.getFiles().length);
		TOTorrentFile[] files = torrent.getFiles();
		for (int i = 0; i < files.length; i++) {
			logger.finer(getFileStartPosInTorrent(files, i) + ": "
					+ files[i].getRelativePath());
		}

	}

	class PieceTranslationExcetion
		extends Exception
	{
		private static final long serialVersionUID = 1L;

		public PieceTranslationExcetion(String message) {
			super(message);
		}

		public PieceTranslationExcetion(String message, Throwable cause) {
			super(message, cause);
		}
	}

	private class VirtualPiece
	{
		private final int	 firstPieceOffset;

		private final int[] pieceMapping;

		private final int	 pieceNumber;

		public VirtualPiece(int pieceNumber, int[] pieceMapping,
				int firstPieceOffset) throws PieceTranslationExcetion {
			this.pieceMapping = pieceMapping;
			this.firstPieceOffset = firstPieceOffset;
			this.pieceNumber = pieceNumber;
		}

		public void enqueueReadRequest(
				final DiskManagerReadRequest originalRequest,
				final DiskManagerReadRequestListener originalListener)
				throws PieceTranslationExcetion {
			int requestOffset = originalRequest.getOffset();
			// find the piece and position in which this request starts in the
			// source torrent
			int requestStartPiece = 0;
			int numBytesIntoDstPieceThatSrcPieceEnds = getPiece(0).getLength()
					- firstPieceOffset;
			while (numBytesIntoDstPieceThatSrcPieceEnds < requestOffset) {
				requestStartPiece++;
				numBytesIntoDstPieceThatSrcPieceEnds += getPiece(requestStartPiece).getLength();
			}
			int overshoot = numBytesIntoDstPieceThatSrcPieceEnds - requestOffset;
			int requestStartPos = getPiece(requestStartPiece).getLength() - overshoot;
			int firstPiece = getPiece(requestStartPiece).getPieceNumber();
			logger.finest("converted to: piece=" + firstPiece + " offset="
					+ requestStartPos);

			// we can only read up to the piece boundary which means that we
			// might have to queue up some requests
			ArrayList<DiskManagerReadRequest> requests = new ArrayList<DiskManagerReadRequest>();
			int requestedSoFar = 0;
			int currentPiece = firstPiece;
			int pieceOffSet = requestStartPos;
			DiskManagerPiece[] srcPieces = srcDownloadManager.getDiskManager().getPieces();
			while (requestedSoFar < originalRequest.getLength()) {
				int remaining = originalRequest.getLength() - requestedSoFar;
				int bytesLeftInPiece = srcPieces[currentPiece].getLength()
						- pieceOffSet;

				int requestedBytes = Math.min(remaining, bytesLeftInPiece);
				if (requestedBytes > 0) {
					logger.finest("requesting partial: piece=" + currentPiece
							+ " offset=" + pieceOffSet + " length=" + requestedBytes);
					DiskManagerReadRequest request = sourceManager.createDiskManagerRequest(
							currentPiece, pieceOffSet, requestedBytes);
					requests.add(request);
				} else {
					// no bytes left in piece, jump to the next one
				}
				// prepare for the next iteration
				requestedSoFar += requestedBytes;
				currentPiece++;
				pieceOffSet = 0;
			}

			// ok, now wait for the requests to return
			final DirectByteBuffer[] responses = new DirectByteBuffer[requests.size()];

			for (int i = 0; i < requests.size(); i++) {
				final int index = i;
				DiskManagerReadRequest request = requests.get(index);
				sourceManager.getAdapter().enqueueReadRequest(virtualPeer, request,
						new DiskManagerReadRequestListener() {
							public int getPriority() {
								return originalListener.getPriority();
							}

							private boolean isCompleted() {
								for (DirectByteBuffer d : responses) {
									if (d == null) {
										return false;
									}
								}
								return true;
							}

							public void readCompleted(DiskManagerReadRequest _request,
									DirectByteBuffer data) {
								logger.finest("partial request completed: piece="
										+ _request.getPieceNumber() + " offset="
										+ _request.getOffset() + " length="
										+ data.remaining(DirectByteBuffer.SS_FILE));
								responses[index] = data;
								if (isCompleted()) {
									DirectByteBuffer target = DirectByteBufferPool.getBuffer(
											DirectByteBuffer.AL_FILE, originalRequest.getLength());
									putInBuffer(responses, 0, responses.length, target, true);
									target.flip(DirectByteBuffer.SS_FILE);
									logger.finest("original request completed: piece="
											+ originalRequest.getPieceNumber() + " offset="
											+ originalRequest.getOffset() + " length="
											+ target.remaining(DirectByteBuffer.SS_FILE));
									originalListener.readCompleted(originalRequest, target);
								}
							}

							public void readFailed(DiskManagerReadRequest request,
									Throwable cause) {
								logger.finest("request failed: piece="
										+ request.getPieceNumber() + " offset="
										+ request.getOffset() + " length=" + request.getLength()
										+ ": " + cause.getMessage());
								originalListener.readFailed(originalRequest, cause);
							}

							public void requestExecuted(long bytes) {
								originalListener.requestExecuted(bytes);
							}
						});
			}

		}

		private int getFirstPieceNum() throws PieceTranslationExcetion {
			return getPiece(0).getPieceNumber();
		}

		private int getLastPieceNum() throws PieceTranslationExcetion {
			return getPiece(pieceMapping.length - 1).getPieceNumber();
		}

		private DiskManagerPiece getPiece(int virtualIndex)
				throws PieceTranslationExcetion {
			if (virtualIndex > pieceMapping.length || virtualIndex < 0) {
				throw new PieceTranslationExcetion("invalid virtual index: "
						+ virtualIndex + " len=" + pieceMapping.length);
			}
			DiskManager diskMan = srcDownloadManager.getDiskManager();
			if (diskMan == null) {
				throw new PieceTranslationExcetion("disk manager is null!");
			}
			DiskManagerPiece[] dmPieces = diskMan.getPieces();
			if (dmPieces == null) {
				throw new PieceTranslationExcetion("pieces[] is null");
			}
			int realIndex = pieceMapping[virtualIndex];
			if (realIndex >= dmPieces.length || realIndex < 0) {
				throw new PieceTranslationExcetion("invalid piece index: " + realIndex
						+ " len=" + dmPieces.length);
			}

			return dmPieces[realIndex];
		}

		public boolean isDone() throws PieceTranslationExcetion {
			for (int i = 0; i < pieceMapping.length; i++) {
				if (!getPiece(i).isDone()) {
					return false;
				}
			}
			return true;
		}

		private int putInBuffer(DirectByteBuffer sources[], int array_offset,
				int length, DirectByteBuffer target, boolean returnSourceToPool) {

			int copied = 0;
			ByteBuffer t = target.getBuffer(DirectByteBuffer.SS_FILE);

			for (int i = array_offset; i < array_offset + length; i++) {
				ByteBuffer source = sources[i].getBuffer(DirectByteBuffer.SS_FILE);
				if (t.remaining() == 0) {
					break;
				}
				if (source.remaining() == 0) {
					continue;
				}

				int numBytesToCopy = Math.min(t.remaining(), source.remaining());
				if (t.remaining() < source.remaining()) {
					// we need to set the limit to avoid buffer overflow
					int oldLimit = source.limit();
					source.limit(source.position() + t.remaining());
					t.put(source);
					source.limit(oldLimit);
				} else {
					t.put(source);
				}
				copied += numBytesToCopy;
				sources[i].returnToPool();
			}
			return copied;
		}

		public String toString() {
			try {
				return "VirtualPiece " + pieceNumber + ": src[" + getFirstPieceNum()
						+ "," + getLastPieceNum() + "] offset=" + firstPieceOffset
						+ " done=" + isDone();
			} catch (PieceTranslationExcetion e) {
				return "VirtualPiece " + pieceNumber;
			}
		}
	}
}
