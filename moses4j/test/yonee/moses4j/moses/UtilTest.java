package yonee.moses4j.moses;



import junit.framework.Assert;

import org.junit.Test;

import yonee.moses4j.moses.Util;

public class UtilTest {

	@Test
	public void testTokenizeStringString() {
		String ss[] = Util.tokenize("Not yet implemented", " ");
		Assert.assertEquals("Not", ss[0]);
		Assert.assertEquals("yet", ss[1]);
		Assert.assertEquals("implemented", ss[2]);
	
	}
	
	@Test
	public void testTokenizeMultiCharSeparator() {
		String ss[] = Util.tokenizeMultiCharSeparator("the ||| ет ||| 0.3 ||| |||", "|||");
		Assert.assertEquals("the ", ss[0]);
		Assert.assertEquals(" ет ", ss[1]);
		Assert.assertEquals(" 0.3 ", ss[2]);

	}

}
