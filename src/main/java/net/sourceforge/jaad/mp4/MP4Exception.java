package net.sourceforge.jaad.mp4;

import java.io.IOException;

public class MP4Exception extends IOException {
	private static final long serialVersionUID = 3144853349684945218L;
	public MP4Exception(String message) {
		super(message);
	}
	public MP4Exception(String message, Throwable cause) {
		super(message, cause);
	}
	public MP4Exception(Throwable cause) {
		super(cause);
	}
	public MP4Exception() {
	}
}
