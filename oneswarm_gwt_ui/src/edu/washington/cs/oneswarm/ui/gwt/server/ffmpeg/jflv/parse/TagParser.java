/**
 * This file is part of jFlvTool.
 *
 * jFlvTool is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * jFlvTool is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * file name  : TagParser.java
 * authors    : Jon Keys
 * created    : July 4, 2007, 5:22 PM
 * copyright  : Sony Digital Authoring Services
 *
 * modifications:
 * Date:        Name:           Description:
 * ----------   --------------- ----------------------------------------------
 * July 4, 2007    Jon Keys         Creation
 */

package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.parse;

import java.util.ArrayList;

import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io.BufferHelper;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io.FileReader;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io.IOHelper;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.metadata.FrameSequence;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.metadata.TagStor;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.tags.*;

/**
 * 
 * @author Jon Keys
 */
public class TagParser {

	private IOHelper ioh;
	private FileReader fh;
	private BufferHelper bh;

	private ArrayList<TagStor> tags;
	private FrameSequence frameseq;
	private ArrayList<Double> keyFrameTagTimes;
	private ArrayList<Double> keyFrameTagOffsets;
	private ArrayList<Object> cuePointTags;

	private long prevTagLen;
	private int tagType;

	private int lastAudioTag;
	private int firstAudioTag;
	private long totalAudioSize;
	private long audioDataSize;

	private int lastVideoTag;
	private int firstVideoTag;
	private long totalVideoSize;
	private long videoDataSize;

	private int onMetaTag;
	private long totalMetaSize;

	private int lastTag;
	private long totalByteOffset;
	private long lastFrameTime;

	/** Creates a new instance of TagParser */
	public TagParser(IOHelper ioh) {

		this.ioh = ioh;
		fh = ioh.getFileReader();
		fh.setDebug(ioh.isDebug());
		bh = ioh.getBufferHelper();

		tags = new ArrayList<TagStor>();
		frameseq = new FrameSequence();
		keyFrameTagTimes = new ArrayList<Double>();
		keyFrameTagOffsets = new ArrayList<Double>();
		cuePointTags = new ArrayList<Object>();

		prevTagLen = 0;
		tagType = 0;

		lastAudioTag = -1;
		firstAudioTag = -1;
		audioDataSize = 0;
		totalAudioSize = 0;

		lastVideoTag = -1;
		totalVideoSize = 0;
		firstVideoTag = -1;

		onMetaTag = -1;
		totalMetaSize = 0;

		lastTag = 0;
		totalByteOffset = 0;
		lastFrameTime = 0;

	}// TagParser()

	public void readTags() {

		byte[] mbb = null;

		try {
			while (true) {
				// System.err.println("reading new tag, pos=" + fh.getPos());
				mbb = fh.readByteArray(5);

				if (mbb == null) {
					// System.out.println("mbb null");
					break;
				}

				// bh.setBuffer(mbb);
				prevTagLen = bh.readUint(mbb, 0, 4);
				tagType = bh.readUint(mbb, 4, 1);
				mbb = null;

				parseTag();
				if (tagType == FlvTag.AUDIO) {
					if (firstAudioTag == -1) {
						firstAudioTag = lastTag;
					}
					lastAudioTag = lastTag;
				} else if (tagType == FlvTag.VIDEO) {
					if (firstVideoTag == -1) {
						firstVideoTag = lastTag;
					}
					lastVideoTag = lastTag;
				}
				lastTag++;

			}// while
		} catch (Throwable t) {
			// if (tagType == FlvTag.AUDIO) {
			// lastAudioTag--;
			// } else if (tagType == FlvTag.VIDEO) {
			// lastVideoTag--;
			// }
			if (!(t instanceof java.lang.NullPointerException)) {
				t.printStackTrace();
			}
		}

	}// readTags()

	private long lastTagReadAttemptPos = 0;

	public long getLastTabReadAttemptPos() {
		return lastTagReadAttemptPos;
	}

	private void parseTag() {
		lastTagReadAttemptPos = ioh.getFileReader().getPos();
		// System.err.println("reading tag, pos=" + lastTagReadAttemptPos);

		TagStor stor = new TagStor();
		String type = "";

		switch (tagType) {
		case FlvTag.AUDIO:
			type = "Audio";
			AudioTag at = new AudioTag(ioh);
			totalByteOffset += at.getDataSize();
			stor.setDataSize((at.getDataSize() - 15));
			totalAudioSize += at.getDataSize() - 4;
			audioDataSize += at.getDataSize() - 15;
			stor.setTimestamp(at.getTimestamp());
			stor.setType(tagType);
			stor.setTag(at);
			tags.add(stor);
			break;

		case FlvTag.VIDEO:
			type = "Video";
			VideoTag vt = new VideoTag(ioh);
			vt.setByteOffset(totalByteOffset);
			totalByteOffset += vt.getDataSize();
			frameseq.addSequence((int) (vt.getTimestamp() - lastFrameTime));
			lastFrameTime = vt.getTimestamp();
			stor.setDataSize((vt.getDataSize() - 15));
			totalVideoSize += vt.getDataSize() - 4;
			videoDataSize += vt.getDataSize() - 15;
			stor.setTimestamp(vt.getTimestamp());
			stor.setType(tagType);
			stor.setTag(vt);
			if (vt.getFrameType() == vt.KEYFRAME) {
				keyFrameTagTimes.add(new Double(vt.getTimestamp() / 1000d));
				keyFrameTagOffsets.add(new Double(vt.getByteOffset()));
			}
			tags.add(stor);
			break;

		case FlvTag.META:
			type = "Meta";
			MetaTag mt = new MetaTag(ioh);
			totalByteOffset += mt.getDataSize();
			stor.setDataSize((mt.getDataSize() - 15));
			totalMetaSize += mt.getDataSize() + 15;
			stor.setTimestamp(mt.getTimestamp());
			stor.setType(tagType);
			stor.setTag(mt);
			if (mt.getEvent().equals("onCuePoint")) {
				cuePointTags.add(mt.getMetaData());
			} else if (mt.getEvent().equals("onMetaData")) {
				onMetaTag = lastTag;
			}
			tags.add(stor);
			break;

		case FlvTag.UNDEFINED:
			type = "Undef";
			FlvTag ft = new FlvTag(ioh);
			totalByteOffset += ft.getDataSize();
			stor.setDataSize((ft.getDataSize() - 15));
			stor.setTimestamp(ft.getTimestamp());
			stor.setType(tagType);
			stor.setTag(ft);
			tags.add(stor);
			break;

		default:
			type = "Unkown";
			FlvTag flt = new FlvTag(ioh);
			totalByteOffset += flt.getDataSize();
			stor.setDataSize((flt.getDataSize() - 15));
			stor.setTimestamp(flt.getTimestamp());
			stor.setType(tagType);
			stor.setTag(flt);
			tags.add(stor);
			break;
		}

		// System.out.println("type " + type);

	}// parseTag()

	public ArrayList<TagStor> getTags() {
		return tags;
	}

	public void setTags(ArrayList<TagStor> tags) {
		this.tags = tags;
	}

	public FrameSequence getFrameseq() {
		return frameseq;
	}

	public void setFrameseq(FrameSequence frameseq) {
		this.frameseq = frameseq;
	}

	public ArrayList<Double> getKeyFrameTagTimes() {
		return keyFrameTagTimes;
	}

	public void setKeyFrameTagTimes(ArrayList<Double> keyFrameTagTimes) {
		this.keyFrameTagTimes = keyFrameTagTimes;
	}

	public ArrayList<Double> getKeyFrameTagOffsets() {
		return keyFrameTagOffsets;
	}

	public void setKeyFrameTagOffsets(ArrayList<Double> keyFrameTagOffsets) {
		this.keyFrameTagOffsets = keyFrameTagOffsets;
	}

	public ArrayList<Object> getCuePointTags() {
		return cuePointTags;
	}

	public void setCuePointTags(ArrayList<Object> cuePointTags) {
		this.cuePointTags = cuePointTags;
	}

	public long getPrevTagLen() {
		return prevTagLen;
	}

	public void setPrevTagLen(long prevTagLen) {
		this.prevTagLen = prevTagLen;
	}

	public int getTagType() {
		return tagType;
	}

	public void setTagType(int tagType) {
		this.tagType = tagType;
	}

	public int getLastAudioTag() {
		return lastAudioTag;
	}

	public void setLastAudioTag(int lastAudioTag) {
		this.lastAudioTag = lastAudioTag;
	}

	public long getTotalAudioSize() {
		return totalAudioSize;
	}

	public void setTotalAudioSize(long totalAudioSize) {
		this.totalAudioSize = totalAudioSize;
	}

	public int getLastVideoTag() {
		return lastVideoTag;
	}

	public void setLastVideoTag(int lastVideoTag) {
		this.lastVideoTag = lastVideoTag;
	}

	public long getTotalVideoSize() {
		return totalVideoSize;
	}

	public void setTotalVideoSize(long totalVideoSize) {
		this.totalVideoSize = totalVideoSize;
	}

	public int getFirstVideoTag() {
		return firstVideoTag;
	}

	public void setFirstVideoTag(int firstVideoTag) {
		this.firstVideoTag = firstVideoTag;
	}

	public long getTotalMetaSize() {
		return totalMetaSize;
	}

	public void setTotalMetaSize(long totalMetaSize) {
		this.totalMetaSize = totalMetaSize;
	}

	public int getLastTag() {
		return lastTag;
	}

	public void setLastTag(int lastTag) {
		this.lastTag = lastTag;
	}

	public long getTotalByteOffset() {
		return totalByteOffset;
	}

	public void setTotalByteOffset(long totalByteOffset) {
		this.totalByteOffset = totalByteOffset;
	}

	public long getLastFrameTime() {
		return lastFrameTime;
	}

	public void setLastFrameTime(long lastFrameTime) {
		this.lastFrameTime = lastFrameTime;
	}

	public int getFirstAudioTag() {
		return firstAudioTag;
	}

	public void setFirstAudioTag(int firstAudioTag) {
		this.firstAudioTag = firstAudioTag;
	}

	public int getOnMetaTag() {
		return onMetaTag;
	}

	public void setOnMetaTag(int onMetaTag) {
		this.onMetaTag = onMetaTag;
	}

	public long getAudioDataSize() {
		return audioDataSize;
	}

	public void setAudioDataSize(long audioDataSize) {
		this.audioDataSize = audioDataSize;
	}

	public long getVideoDataSize() {
		return videoDataSize;
	}

	public void setVideoDataSize(long videoDataSize) {
		this.videoDataSize = videoDataSize;
	}

}// TagParser
