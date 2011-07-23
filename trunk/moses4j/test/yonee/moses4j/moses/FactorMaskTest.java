package yonee.moses4j.moses;

import junit.framework.Assert;

import org.junit.Test;

import yonee.moses4j.moses.FactorMask;

public class FactorMaskTest {

	@Test
	public void testFactorMaskBitSet() {
		FactorMask a = new FactorMask(new int[] { 1, 2, 3 });
		FactorMask b = new FactorMask(a);
		Assert.assertEquals("{1, 2, 3}", b.toString());
	}
}
