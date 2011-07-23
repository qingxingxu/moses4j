package yonee.moses4j.moses;

import org.junit.Assert;
import org.junit.Test;

import yonee.moses4j.moses.UserMessage;

public class UserMessageTest {

	@Test
	public void testSetOutput() {
		UserMessage.setOutput(true, false);
		UserMessage.add("test");
		Assert.assertEquals(UserMessage.getQueue(), "");
	}

	@Test
	public void testGetQueue() {
		UserMessage.setOutput(false, true);
		UserMessage.add("test");
		UserMessage.add("aaaa");
		Assert.assertEquals(UserMessage.getQueue(), "test\naaaa\n");
	}

}
