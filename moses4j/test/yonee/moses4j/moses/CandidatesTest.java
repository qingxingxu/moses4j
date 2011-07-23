package yonee.moses4j.moses;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import yonee.moses4j.moses.Candidates;
import yonee.moses4j.moses.GenericCandidate;

public class CandidatesTest {

	@Test
	public void testWriteBin() {
		Candidates c = new Candidates(), result = new Candidates();
		List<int[]> a = new ArrayList<int[]>();
		a.add(new int[] { 1, 2, 3 });
		a.add(new int[] { 4, 5, 6 });
		List<float[]> b = new ArrayList<float[]>();
		b.add(new float[] { 1.1f, 2.1f, 3.1f });
		b.add(new float[] { 4.1f, 5.1f, 6.1f });
		GenericCandidate gc = new GenericCandidate(a, b);
		c.add(gc);
		OutputStream fos = null;
		ObjectOutputStream oos = null;
		InputStream fis = null;
		ObjectInputStream ois = null;
		try {
			fos = new FileOutputStream("e:/t.tmp");
			oos = new ObjectOutputStream(fos);
			c.writeBin(oos);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());

		} finally {
			try {
				if (oos != null)
					oos.close();
				if (fos != null)
					fos.close();

			} catch (IOException e) {
				
				e.printStackTrace();
			}
		}
		
		try {
			fis = new FileInputStream("e:/t.tmp");
			ois = new ObjectInputStream(fis);
			result.readBin(ois);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());

		} finally {
			try {

				if (ois != null)
					ois.close();
				if (fis != null)
					fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		Assert.assertArrayEquals((c.get(0).getPhrase(0)), (result.get(0).getPhrase(0)));
		// Assert.assertArrayEquals((c.get(0).getScore(0)), (result.get(0).getScore(0)));
	}

}
