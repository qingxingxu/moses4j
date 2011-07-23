package yonee.moses4j.moses;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
/**
 * 
 * @author YONEE
 * @OK
 */
public class UserMessage {
	public final static int MAX_MSG_QUEUE = 5;

	protected static boolean toStderr = true, toQueue = false;
	protected static Queue<String> msgQueue = new LinkedBlockingQueue<String>();

	// ! whether messages to go to stderr, a queue to later display, or both
	public static void setOutput(boolean toStderr, boolean toQueue) {
		UserMessage.toStderr = toStderr;
		UserMessage.toQueue = toQueue;
	}

	// ! add a message to be displayed
	public static void add(String msg) {
		if (toStderr) {
			System.err.println("ERROR:" + msg);
		}
		if (toQueue) {
			if (msgQueue.size() >= MAX_MSG_QUEUE)
				msgQueue.poll();
			msgQueue.add(msg);
		}
	}

	// ! get all messages in queue. Each is on a separate line. Clear queue
	// afterwards
	public static String getQueue() {
		StringBuilder strme = new StringBuilder("");
		while (!msgQueue.isEmpty()) {
			strme.append(msgQueue.peek()).append('\n');
			msgQueue.poll();
		}
		return strme.toString();
	}
}
