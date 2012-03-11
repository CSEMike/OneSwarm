package org.gudy.azureus2.ui.swt.progress;

/**
 * A simple class for a message
 * @author knguyen
 *
 */
public class ProgressReportMessage
	implements IMessage, IProgressReportConstants
{

	private String value = "";

	private int type;

	/**
	 * Create a message for the given value and type; message type can by any one of:
	 * <ul>
	 * <li> <code>IProgressReportConstants.MSG_TYPE_ERROR</code> -- an error message</li>
	 * <li> <code>IProgressReportConstants.MSG_TYPE_INFO</code> -- a general informational message</li>
	 * <li> <code>IProgressReportConstants.MSG_TYPE_LOG</code> -- a log message; for messages that are more detailed and verbose</li>
	 * </ul>
	 * @param value
	 * @param type
	 */
	public ProgressReportMessage(String value, int type) {
		this.value = value;

		switch (type) {
			case MSG_TYPE_ERROR:
			case MSG_TYPE_INFO:
				this.type = type;
				break;
			default:
				this.type = MSG_TYPE_LOG;
		}
	}

	public String getValue() {
		return value;
	}

	public int getType() {
		return type;
	}

	public boolean isError() {
		return type == MSG_TYPE_ERROR;
	}

	public boolean isInfo() {
		return type == MSG_TYPE_INFO;
	}

	public boolean isLog() {
		return type == MSG_TYPE_LOG;
	}
}
