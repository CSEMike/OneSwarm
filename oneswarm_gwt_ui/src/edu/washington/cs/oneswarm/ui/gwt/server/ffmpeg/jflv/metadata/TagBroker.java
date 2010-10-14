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
 * file name  : TagReader.java
 * authors    : Jon Keys
 * created    : June 28, 2007, 3:52 PM
 * copyright  : Sony Digital Authoring Services
 *
 * modifications:
 * Date:        Name:           Description:
 * ----------   --------------- ----------------------------------------------
 * June 28, 2007    Jon Keys         Creation
 */

package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.metadata;

import java.util.ArrayList;
import java.util.HashMap;

import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io.FileWriter;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io.IOHelper;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.parse.TagParser;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.tags.*;

/**
 * 
 * @author Jon Keys
 */
public class TagBroker {

	private double frameRate;
	private int startingByteOffset;
	private TagStor stor;
	private IOHelper ioh;
	private FlvHeader flvh;

	private ArrayList<TagStor> tags;
	private TagParser tp;

	/**
	 * Creates a new instance of TagBroker
	 */
	public TagBroker(IOHelper ioh, FlvHeader flvh) {
		frameRate = 0;
		stor = null;
		this.startingByteOffset = flvh.getDataOffset() + 4;
		tp = new TagParser(ioh);
		tp.setTotalByteOffset(startingByteOffset);
		tp.readTags();
		tags = tp.getTags();
		this.ioh = ioh;
		this.flvh = flvh;
	}

	public long getDuration() {

		long dur = 0;

		if (flvh.hasVideo()) {
			dur = tags.get(tp.getLastVideoTag()).getTimestamp();
		} else if (flvh.hasAudio()) {
			dur = tags.get(tp.getLastAudioTag()).getTimestamp();
		} else {
			dur = tags.get(tags.size() - 1).getTimestamp();
		}

		return dur;

	}// getDuration()

	public long getLastTabReadAttemptPos() {
		return tp.getLastTabReadAttemptPos();
	}

	public void writeTags() {
		FileWriter fw = ioh.getFileWriter(flvh);
		fw.setDebug(ioh.isDebug());
		fw.setTags(tags);
		fw.setInStream(ioh.getFileReader().getStream());
		flvh.setHasAudio((tp.getLastAudioTag() != -1));
		flvh.setHasVideo((tp.getLastVideoTag() != -1));
		fw.writeTags();
	}

	public void recalcKeyFrameTagOffsets() {

		ArrayList<Double> keyFrameTagOffsets = new ArrayList<Double>();
		long totalByteOffset = startingByteOffset;
		int totMetaBytes = 0;

		for (TagStor ts : tags) {

			if (ts.getType() == FlvTag.VIDEO) {

				if (((VideoTag) ts.getTag()).getFrameType() == VideoTag.KEYFRAME) {
					keyFrameTagOffsets.add(new Double(totalByteOffset));
				}

			} else if (ts.getType() == FlvTag.META) {

				if (ts.isNew()) {
					totMetaBytes += ts.getDataSize() + 15;
				} else {
					totMetaBytes += ts.getDataSize();
				}

			}// else

			totalByteOffset += ts.getDataSize() + 15;

		}// for

		tp.setKeyFrameTagOffsets(keyFrameTagOffsets);
		tp.setTotalByteOffset(totalByteOffset);
		tp.setTotalMetaSize(totMetaBytes);

	}// recalcTagOffsets()

	public void addTags(ArrayList<TagStor> tags2add, boolean keepFrameRate, boolean overwrite) {

		frameRate = tp.getFrameseq().getFrameRate();

		if (keepFrameRate && frameRate == 0) {

			System.out.println("Error adding new tags: current tag doesn't fit into existing framerate");
			return;

		} else if (keepFrameRate) {

			for (TagStor ts : tags2add) {

				if (ts.getTimestamp() % (1000 / frameRate) != 0) {
					System.out.println("Error adding new tags: current tag doesn't fit into existing framerate");
					return;
				}

			}// for

		}// else

		int pos = 0;

		for (TagStor ts : tags2add) {

			TagStor nextTag = findNextTag(ts.getTimestamp());

			if (nextTag == null) {
				tags.add(ts);
				// System.out.println("added new tag to end");
				continue;
			}

			pos = tags.indexOf(nextTag);

			if (ts.getTimestamp() == nextTag.getTimestamp() && ts.getType() == nextTag.getType()) {

				// IF type = meta AND (the new.meta.event != next.meta.event OR
				// can overwrite)
				if (ts.getType() == FlvTag.META && (!((MetaTag) ts.getTag()).getEvent().equals(((MetaTag) nextTag.getTag()).getEvent()) || !overwrite)) {

					tags.add(pos, ts);
					// System.out.println("inserted tag at : " + pos);
					tp.setTags(tags);
					pushTags(pos);

				} else {

					// System.out.println("replaced tag at : " + pos);
					tags.remove(pos);
					tags.add(pos, ts);
					tp.setTags(tags);

				}// else

			} else {

				tags.add(pos, ts);
				// System.out.println("inserted tag at : " + pos);
				tp.setTags(tags);
				pushTags(pos);

			}// else

		}// for

	}// addTags()

	private void pushTags(int pos) {

		if (pos <= tp.getLastAudioTag()) {
			tp.setLastAudioTag(tp.getLastAudioTag() + 1);
		}
		if (pos <= tp.getLastVideoTag()) {
			tp.setLastVideoTag(tp.getLastVideoTag() + 1);
		}
		if (pos <= tp.getFirstVideoTag()) {
			tp.setFirstVideoTag(tp.getFirstVideoTag() + 1);
		}
		if (pos <= tp.getFirstAudioTag()) {
			tp.setFirstAudioTag(tp.getFirstAudioTag() + 1);
		}
		if (pos <= tp.getOnMetaTag()) {
			tp.setOnMetaTag(tp.getOnMetaTag() + 1);
		}

	}// pushTags()

	private TagStor findNextTag(long timestamp) {

		TagStor nextTag = null;

		for (int i = 0; i < tags.size(); i++) {

			if (tags.get(i).getTimestamp() >= timestamp) {
				nextTag = tags.get(i);
				break;
			}

		}// for

		return nextTag;

	}// findNextTag()

	public int getVideoWidth() {

		int vwidth = 0;
		if (tp.getFirstVideoTag() != -1) {
			if (((VideoTag) tags.get(tp.getFirstVideoTag()).getTag()).getWidth() == 0 && tp.getOnMetaTag() != -1) {

				try {
					HashMap<String, Object> mdata = (HashMap<String, Object>) (((MetaTag) tags.get(tp.getOnMetaTag()).getTag()).getMetaData());
					vwidth = ((Double) mdata.get("width")).intValue();
				} catch (Exception e) {
					// do nothing
				}

			} else {

				vwidth = ((VideoTag) tags.get(tp.getFirstVideoTag()).getTag()).getWidth();

			}// else
		}
		return vwidth;

	}// getVideoWidth()

	public int getVideoHeight() {

		int vheight = 0;
		if (tp.getFirstVideoTag() != -1) {
			if (((VideoTag) tags.get(tp.getFirstVideoTag()).getTag()).getHeight() == 0 && tp.getOnMetaTag() != -1) {

				try {
					HashMap<String, Object> mdata = (HashMap<String, Object>) (((MetaTag) tags.get(tp.getOnMetaTag()).getTag()).getMetaData());
					vheight = ((Double) mdata.get("height")).intValue();
				} catch (Exception e) {
					// do nothing
				}

			} else {

				vheight = ((VideoTag) tags.get(tp.getFirstVideoTag()).getTag()).getHeight();

			}// else
		}

		return vheight;

	}// getVideoHeight()

	public long getTotalDataSize() {
		return (tp.getTotalVideoSize() + tp.getTotalAudioSize() + tp.getTotalMetaSize());
	}

	public float getVideoDataRate(float duration) {

		float videoDataRate = 0;

		if (tp.getTotalVideoSize() != 0) {
			videoDataRate = tp.getVideoDataSize() / duration * 8 / 1000;
		}

		return videoDataRate;

	}// getVideoDataRate()

	public float getAudioDataRate(float duration) {

		float audioDataRate = 0;

		if (tp.getTotalAudioSize() != 0) {
			audioDataRate = tp.getAudioDataSize() / duration * 8 / 1000;
		}

		return audioDataRate;

	}// getAudioDataRate()

	public double getLastKeyFrameTimeStamp() {

		double lkfts = 0;
		double lkfTime = (tp.getKeyFrameTagTimes().get(tp.getKeyFrameTagTimes().size() - 1)).doubleValue();

		if (lkfTime != 0) {
			lkfts = lkfTime;
		}

		return lkfts;

	}// getLastKeyFrameTimeStamp()

	public int getAudioCodecId() {
		return ((AudioTag) tags.get(tp.getFirstAudioTag()).getTag()).getSoundFormat();
	}

	public int getFirstVideoTag() {
		return tp.getFirstVideoTag();
	}

	public int getVideoCcodecId() {
		int firstVideoTag = tp.getFirstVideoTag();
		if (firstVideoTag > 0) {
			return ((VideoTag) tags.get(firstVideoTag).getTag()).getCodecId();
		} else {
			return 0;
		}
	}

	public long getAudioDelay() {

		long audioDelay = 0;
		if (tp.getFirstVideoTag() != -1) {
			audioDelay = tags.get(tp.getFirstVideoTag()).getTimestamp() / 1000;
		}

		return audioDelay;

	}// getAudioDelay()

	public boolean canSeekToEnd() {
		return (((VideoTag) tags.get(tp.getLastVideoTag()).getTag()).getFrameType() == VideoTag.KEYFRAME);
	}

	public boolean isStero() {
		return (((AudioTag) tags.get(tp.getFirstAudioTag()).getTag()).getSoundType() == AudioTag.STEREO);
	}

	public int getAudioSampleRate() {
		return ((AudioTag) tags.get(tp.getFirstAudioTag()).getTag()).getSoundRate();
	}

	public int getAudioSampleSize() {
		return ((AudioTag) tags.get(tp.getFirstAudioTag()).getTag()).getSoundSampleSize();
	}

	public ArrayList<TagStor> getTags() {
		return tags;
	}

	public double getFrameRate() {
		return this.frameRate;
	}

	public long getTotalAudioSize() {
		return tp.getTotalAudioSize();
	}

	public long getTotalVideoSize() {
		return tp.getTotalVideoSize();
	}

	public long getTotalMetaSize() {
		return tp.getTotalMetaSize();
	}

	public long getTotalByteOffset() {
		return tp.getTotalByteOffset();
	}

	public AMFObject getKeyFrameTags() {

		AMFObject keyFrameTags = new AMFObject();
		keyFrameTags.put("times", tp.getKeyFrameTagTimes());
		keyFrameTags.put("filepositions", tp.getKeyFrameTagOffsets());
		return keyFrameTags;

	}// getKeyFrameTags()

	public ArrayList<Object> getCuePointTags() {
		return tp.getCuePointTags();
	}

	public int getLastAudioTag() {
		return tp.getLastAudioTag();
	}

	public int getLastVideoTag() {
		return tp.getLastVideoTag();
	}

	public int getFirstAudioTag() {
		return tp.getFirstAudioTag();
	}

}// TagBroker
