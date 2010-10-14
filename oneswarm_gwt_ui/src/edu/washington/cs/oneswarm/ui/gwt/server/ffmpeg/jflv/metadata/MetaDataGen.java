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
 * file name  : MetaDataGen.java
 * authors    : Jon Keys
 * created    : July 5, 2007, 8:44 AM
 * copyright  : Sony Digital Authoring Services
 *
 * modifications:
 * Date:        Name:           Description:
 * ----------   --------------- ----------------------------------------------
 * July 5, 2007    Jon Keys         Creation
 */

package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.tags.FlvTag;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.tags.MetaTag;

/**
 * 
 * @author Jon Keys
 */
public class MetaDataGen {

	private TagBroker tb;
	private FlvHeader flvh;
	private EmbeddedData emb;
	private MetaTag mt2;

	private long duration;
	private long lastTimeStamp;
	private static Logger logger = Logger.getLogger(MetaDataGen.class.getName());

	/** Creates a new instance of MetaDataGen */
	public MetaDataGen(TagBroker tb, FlvHeader flvh) {

		this.tb = tb;
		this.flvh = flvh;
		emb = null;

		duration = 0;
		lastTimeStamp = 0;

	}// MetaDataGen()

	public void buildOnLastSecond() {

		duration = tb.getDuration();
		lastTimeStamp = (duration - 1000);

		MetaTag mt = new MetaTag();
		mt.setEvent("onLastSecond");
		mt.setTimestamp(lastTimeStamp);
		mt.setMetaData(new HashMap<String, Object>());

		if (lastTimeStamp > 0) {
			ArrayList<TagStor> nts = new ArrayList<TagStor>();
			TagStor mtTagStor = new TagStor(FlvTag.META, (mt.getDataSizeFromBuffer() - 15), lastTimeStamp, mt);
			mtTagStor.setIsNew(true);
			nts.add(mtTagStor);
			tb.addTags(nts, false, true);
		}

	}// buildOnLastSecond()

	public void buildOnMetaData() {

		mt2 = new MetaTag();
		mt2.setEvent("onMetaData");

		emb = new EmbeddedData();

		// emb.addData("metadatacreator", "Sony DADC jFlvTool");
		emb.addData("metadatadate", new AMFTime());
		emb.addData("duration", new Double(((duration + tb.getFrameRate())) / 1000d));
		emb.addData("lasttimestamp", new Double((duration / 1000d)));
		emb.addData("audiosize", new Double(tb.getTotalAudioSize()));
		emb.addData("datasize", new Double(0));
		emb.addData("filesize", new Double(0));
		emb.addData("cuePoints", tb.getCuePointTags());
		emb.addData("keyframes", tb.getKeyFrameTags());
		emb.addData("hasMetadata", new Boolean(true));
		emb.addData("hasCuePoints", new Boolean((tb.getCuePointTags().size() > 0)));
		emb.addData("hasKeyframes", new Boolean((tb.getKeyFrameTags().size() > 0)));

		// has video?
		if ((tb.getLastVideoTag() != -1) && tb.getFirstVideoTag() != -1) {
			emb.addData("videocodecid", new Double(tb.getVideoCcodecId()));
			emb.addData("width", new Double(tb.getVideoWidth()));
			emb.addData("height", new Double(tb.getVideoHeight()));
			emb.addData("lastkeyframetimestamp", new Double(tb.getLastKeyFrameTimeStamp()));
			emb.addData("canSeekToEnd", new Boolean(tb.canSeekToEnd()));
			emb.addData("framerate", new Double(tb.getFrameRate()));
			emb.addData("videosize", new Double(tb.getTotalVideoSize()));
			emb.addData("videodatarate", new Double(tb.getVideoDataRate((duration / 1000f))));
			emb.addData("hasVideo", new Boolean(true));
		} else {
			// emb.addData("videocodecid", new Double(0));
			// emb.addData("width", new Double(0));
			// emb.addData("height", new Double(0));
			// emb.addData("lastkeyframetimestamp", new Double(0));
			// emb.addData("canSeekToEnd", new Boolean(true));
			// emb.addData("framerate", new Double(0));
			// emb.addData("videosize", new Double(0));
			// emb.addData("videodatarate", new Double(0));
			// emb.addData("hasVideo", new Boolean(false));
		}

		// has audio?
		if ((tb.getLastAudioTag() != -1) && (tb.getFirstAudioTag() != -1)) {
			emb.addData("hasAudio", new Boolean(true));
			emb.addData("audiodatarate", new Double(tb.getAudioDataRate((duration / 1000f))));
			emb.addData("audiosamplerate", new Double(tb.getAudioSampleRate()));
			emb.addData("audiosamplesize", new Double(tb.getAudioSampleSize()));
			emb.addData("audiocodecid", new Double(tb.getAudioCodecId()));
			emb.addData("audiodelay", new Double(tb.getAudioDelay()));
			emb.addData("stereo", new Boolean(tb.isStero()));
		} else {
			// emb.addData("hasAudio", new Boolean(false));
			// emb.addData("audiodatarate", new Double(0));
			// emb.addData("audiosamplerate", new Double(0));
			// emb.addData("audiosamplesize", new Double(0));
			// emb.addData("audiocodecid", new Double(0));
			// emb.addData("audiodelay", new Double(0));
			// emb.addData("stereo", new Boolean(false));
		}
		logger.finer("\n===BEGIN created meta data====\n" + emb.printMetaData() + "\n===END created meta data====");

	}// buildOnMetaData()

	public void sealMetaData(boolean updateKeyFrames, boolean keepFrameRate) {

		mt2.setMetaData(emb.getData());

		ArrayList<TagStor> nts2 = new ArrayList<TagStor>();
		TagStor mt2TagStor = new TagStor(FlvTag.META, (mt2.getDataSizeFromBuffer() - 15), 0, mt2);
		mt2TagStor.setIsNew(true);
		nts2.add(mt2TagStor);

		tb.addTags(nts2, keepFrameRate, true);

		// re-calc values affected by adding the actual metatag
		tb.recalcKeyFrameTagOffsets();

		if (updateKeyFrames) {
			emb.removeData("keyframes");
			emb.addData("keyframes", tb.getKeyFrameTags());

			emb.removeData("datasize");
			emb.addData("datasize", new Double(tb.getTotalDataSize()));

			emb.removeData("filesize");
			emb.addData("filesize", new Double((flvh.getDataOffset() + tb.getTotalDataSize() + ((tb.getTags().size() + 1) * 4))));
		}
	}// addMetaData()

	@SuppressWarnings("unchecked")
	public void spoofMetaData(double startOffset, double realDurationInSeconds, double audioRateKBitPerS, double videoRateKbitPerS) {
		/*
		 * update duration field
		 */
		emb.removeData("duration");
		emb.addData("duration", new Double(realDurationInSeconds));
		/*
		 * lasttimestamp
		 */
		emb.removeData("lasttimestamp");
		emb.addData("lasttimestamp", new Double(realDurationInSeconds));

		/*
		 * audiosize, videosize, datasize, filesize
		 */
		// has audio?
		double audioSize = 0;
		if ((tb.getLastAudioTag() != -1)) {
			emb.removeData("audiosize");
			audioSize = audioRateKBitPerS * 1024 * realDurationInSeconds / 8.0;
			emb.addData("audiosize", new Double(audioSize));
		}

		double videoSize = 0;
		if ((tb.getLastVideoTag() != -1)) {
			emb.removeData("videosize");
			videoSize = videoRateKbitPerS * 1024 * realDurationInSeconds / 8.0;
			emb.addData("videosize", new Double(videoSize));
		}

		emb.removeData("filesize");
		emb.addData("filesize", new Double(audioSize + videoSize));

		emb.removeData("datasize");
		emb.addData("datasize", new Double(audioSize + videoSize));

		/*
		 * keyframes, lastkeyframetimestamp
		 */
		try {
			if ((tb.getKeyFrameTags().size() > 1) && tb.getLastVideoTag() != -1 && tb.getFirstVideoTag() != -1) {

				AMFObject keyFrameTags = tb.getKeyFrameTags();
				ArrayList<Double> times = (ArrayList<Double>) keyFrameTags.get("times");
				ArrayList<Double> filepositions = (ArrayList<Double>) keyFrameTags.get("filepositions");

				// double timeBetweenKeyFrames = getAverageDiff(times);
				double timeBetweenKeyFrames = 2.0; // just setting it to 2 sec
				double bytesBetweenKeyFrames = 2 * videoRateKbitPerS;// getAverageDiff(filepositions);
				// System.out.println("average diff: time=" +
				// timeBetweenKeyFrames + " bytes=" + bytesBetweenKeyFrames);
				double currTime = startOffset;
				double currByte = filepositions.get(0);

				times.clear();
				filepositions.clear();

				while (currTime < realDurationInSeconds) {
					times.add(currTime);
					currTime += timeBetweenKeyFrames;

					filepositions.add(currByte);
					currByte += bytesBetweenKeyFrames;
				}

				emb.removeData("lastkeyframetimestamp");
				emb.addData("lastkeyframetimestamp", currTime - timeBetweenKeyFrames);
			} else {
				emb.removeData("hasKeyframes");
				emb.removeData("keyframes");
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
		if (logger.isLoggable(Level.FINER)) {
			logger.finer("\n===BEGIN spoofed meta data====\n" + emb.printMetaData() + "\n===END spoofed meta data====");
		}
	}

	// private static double getAverageDiff(ArrayList<Double> list) throws
	// Exception {
	// double num = list.size() - 1;
	// double totalDiff = 0;
	// for (int i = 1; i < list.size(); i++) {
	// totalDiff += list.get(i) - list.get(i - 1);
	// }
	// if (num == 0 || totalDiff == 0) {
	// throw new Exception("num=" + num + " totaldiff=" + totalDiff);
	// }
	// return totalDiff / num;
	// }

	public EmbeddedData getMetaData() {
		return emb;
	}

}// MetaDataGen
