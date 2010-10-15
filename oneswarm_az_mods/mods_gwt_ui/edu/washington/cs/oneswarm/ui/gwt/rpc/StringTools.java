package edu.washington.cs.oneswarm.ui.gwt.rpc;

import java.util.Date;

import com.google.gwt.i18n.client.DateTimeFormat;

public class StringTools {

	public static String formatDateMonthDayYear(Date inDate) {
		return DateTimeFormat.getFormat("MMM d, yyyy").format(inDate);
	}

	public static String formatRate(String inRateStr) {
		return formatRate(Long.parseLong(inRateStr));
	}

	public static String formatRate(double inLongBytes) {
		return formatRate(inLongBytes, "B");
	}

	public static String formatRate(double inBytes, String unit) {

		if (inBytes < 1024)
			return trim(inBytes, 0) + " " + unit;

		inBytes /= 1024.0;

		if (inBytes < 1024)
			return trim(inBytes, 0) + " K" + unit;

		inBytes /= 1024.0;

		if (inBytes < 1024)
			return trim(inBytes, 2) + " M" + unit;

		inBytes /= 1024.0;

		if (inBytes < 1024)
			return trim(inBytes, 2) + " G" + unit;

		inBytes /= 1024.0;

		return trim(inBytes, 2) + " T" + unit;
	}

	public static String trim(double d, int places) {
		String out = Double.toString(d);
		if (out.indexOf('.') != -1) {
			return out.substring(0, Math.min(out.indexOf('.') + places, out.length()));
		}
		return out;
	}

	public static String truncate(String str, int max, boolean trimBack) {
		if (str.length() < max) {
			return str;
		} else {
			if (trimBack)
				return str.substring(0, max - 3) + "...";
			else
				// trim from front
				return "..." + str.substring(str.length() - (max - 3), str.length());
		}
	}

	public static String formatDateAppleLike(Date date, boolean useAgo) {

		if (date == null) {
			return "never";
		}
		boolean inTheFuture = false;
		int secAgo = (int) (((new Date()).getTime() - date.getTime()) / 1000);

		if (secAgo < 0) {
			inTheFuture = true;
			secAgo = -secAgo;
		}
		int minAgo = secAgo / 60;
		int hoursAgo = minAgo / 60;
		int daysAgo = hoursAgo / 24;
		int monthsAgo = daysAgo / 31;
		String ret = "";
		if (secAgo < 5) {
			return "now";
		} else if (secAgo < 60) {
			ret = "<1 minute";
		} else if (minAgo == 1) {
			ret = "1 minute";
		} else if (minAgo < 60) {
			ret = minAgo + " minutes";
		} else if (hoursAgo == 1) {
			ret = "1 hour";
		} else if (hoursAgo < 24) {
			ret = hoursAgo + " hours";
		} else if (daysAgo == 1) {
			if (inTheFuture) {
				return "tomorrow";
			} else {
				return "yesterday";
			}
		} else if (daysAgo < 62) {
			ret = daysAgo + " days";
		} else if (monthsAgo < 24) {
			ret = (monthsAgo) + " months";
		} else {
			return "a long time ago in a galaxy far far away";
		}

		if (useAgo) {
			if (inTheFuture) {
				return "in " + ret;
			} else {
				return ret + " ago";
			}
		} else {
			return ret;
		}
	}

	public static String formatDateAppleLike(Date lastDate) {
		return StringTools.formatDateAppleLike(lastDate, true);
	}

}
