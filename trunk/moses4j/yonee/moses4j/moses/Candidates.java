package yonee.moses4j.moses;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import yonee.utils.CollectionUtils;

/**
 * 
 * @author YONEE
 * @OK
 * @RISK
 *       DataOutputStream 的性能问题
 */
public class Candidates extends ArrayList<GenericCandidate> {

	private static final long serialVersionUID = 1L;

	public Candidates() {
		super();
	}

	void writeBin(ObjectOutputStream f) throws IOException {
		int s = size();
		f.writeInt(s);

		for (int i = 0; i < s; ++i) {
			get(i).writeBin(f);
		}
	}

	void readBin(ObjectInputStream f) throws IOException {
		int s = f.readInt();
		CollectionUtils.resize(this, s, GenericCandidate.class);
		for (int i = 0; i < s; ++i) {
			get(i).readBin(f);
		}
	}
}
