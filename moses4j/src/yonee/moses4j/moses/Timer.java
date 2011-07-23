package yonee.moses4j.moses;

import java.io.IOException;

import java.io.Writer;

import yonee.utils.TRACE;

/**
 * 
 * @author YONEE
 * @OK<<
 */
public class Timer {

	// friend std::ostream& operator<<(std::ostream& os, Timer& t);

	private boolean running;
	private long startTime;

	// in seconds
	private double elapsedTime() {
		long now = System.currentTimeMillis();
		return now - startTime;
	}

	/***
	 * 'running' is initially false. A timer needs to be explicitly started
	 * using 'start' or 'restart'
	 */
	public Timer() {
		running = false;
		startTime = System.currentTimeMillis();
	}

	public void start() {
		start(null);
	}

	public void start(final String msg) {
		// Print an optional message, something like "Starting timer t";
		if (msg != null)
			TRACE.err(msg + "\n");

		// Return immediately if the timer is already running
		if (running)
			return;

		// Change timer status to running
		running = true;

		// Set the start time;
		startTime = System.currentTimeMillis();
	}

	public void check() {
		check(null);
	}

	public void check(final String msg) {
		// Print an optional message, something like "Checking timer t";
		if (msg != null)
			TRACE.err(msg + " : ");
		TRACE.err("[" + (running ? elapsedTime()/ TypeDef.CLOCKS_PER_SEC : 0) + "] seconds\n");
	}

	public double getElapsedTime() {
		return elapsedTime();
	}

	public Writer append(Writer os) {

		try {
			os.append((this.running ? this.elapsedTime() : 0) + "");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return os;
	}
}
