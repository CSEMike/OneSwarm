package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg;

public class FFMpegException extends Exception {

	public static enum ErrorType {
		FFMPEG_BIN_ERROR, FORMAT_ERROR_DOWNLOADING, FORMAT_ERROR, FILE_NOT_FOUND, INTERUPT, OTHER
	};

	private static final long serialVersionUID = -444771194088684321L;

	private int exitCode = -1;
	private String stdErr = "";
	private long dataWritten = 0;
	private final ErrorType type;

	public int getExitCode() {
		return exitCode;
	}

	public void setExitCode(int exitCode) {
		this.exitCode = exitCode;
	}

	public long getDataWritten() {
		return dataWritten;
	}

	public void setDataWritten(long dataWritten) {
		this.dataWritten = dataWritten;
	}

	public FFMpegException(ErrorType type, String message) {
		super(message);
		this.type = type;
	}

	public FFMpegException(ErrorType type, String message, Throwable cause) {
		super(message, cause);
		this.type = type;
	}

	public FFMpegException(ErrorType type, int exitCode, String stderr) {
		super("FFmpeg exited with exit code " + exitCode);
		this.exitCode = exitCode;
		this.type = type;
		this.stdErr = stderr;
	}

	public void setStdErr(String stdErr) {
		this.stdErr = stdErr;
	}

	public String getStdErr() {
		return stdErr;
	}

	public void setFFMpegExecArray(String[] parameters) {
		ffmpegParameters = parameters;
	}

	private String[] ffmpegParameters;

	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("got ffmpeg exception: " + this.getMessage() + "\n");
		b.append("type=" + this.type.name() + "\n");
		b.append("exit code=" + this.getExitCode() + "\n");
		b.append("written=" + this.getDataWritten() + "\n");
		if (ffmpegParameters != null) {
			for (String p : ffmpegParameters) {
				b.append(p + " ");
			}
			b.append("\n");
		}
		b.append("=========ffmpeg stderr===========\n");
		b.append(this.getStdErr() + "\n");
		b.append("=================================\n");
		return b.toString();
	}
}
