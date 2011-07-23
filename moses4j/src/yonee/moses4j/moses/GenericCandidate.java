package yonee.moses4j.moses;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class GenericCandidate {

	// typedef std::vector<IPhrase> PhraseList;
	// typedef std::vector< std::vector<float> > ScoreList;

	private List<int[]> m_PhraseList = new ArrayList<int[]>();
	private List<float[]> m_ScoreList = new ArrayList<float[]>();

	public GenericCandidate() {
	};

	public GenericCandidate(final GenericCandidate other) {
		m_PhraseList = other.m_PhraseList;
		m_ScoreList = other.m_ScoreList;
	};

	public GenericCandidate(final List<int[]> p, final List<float[]> s) {
		m_PhraseList = p;
		m_ScoreList = s;
	};

	public int numPhrases() {
		return m_PhraseList.size();
	};

	public int numScores() {
		return m_ScoreList.size();
	};

	public final int[] getPhrase(int i) {
		return m_PhraseList.get(i);
	}

	public final float[] getScore(int i) {
		return m_ScoreList.get(i);
	}

	public void writeBin(ObjectOutputStream f) throws IOException {
		f.writeObject(m_PhraseList);
		f.writeObject(m_ScoreList);
	}

	@SuppressWarnings("unchecked")
	public void readBin(ObjectInputStream f) throws IOException {
		m_PhraseList.clear();
		m_ScoreList.clear();
		try {
			m_PhraseList = (List<int[]>) f.readObject();

			m_ScoreList = (List<float[]>) f.readObject();
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}

		/*
		 * int num_phrases = f.readInt();
		 * for (int i = 0; i < num_phrases; ++i) {
		 * 
		 * int[] phrase = null;
		 * try {
		 * phrase = (int[]) f.readObject();
		 * } catch (ClassNotFoundException e) {
		 * throw new RuntimeException(e);
		 * }
		 * 
		 * m_PhraseList.add(phrase);
		 * }
		 * ;
		 * int num_scores = f.readInt();
		 * 
		 * for (int j = 0; j < num_scores; ++j) {
		 * float[] score = null;
		 * try {
		 * score = (float[]) f.readObject();
		 * } catch (ClassNotFoundException e) {
		 * throw new RuntimeException(e);
		 * }
		 * m_ScoreList.add(score);
		 * }
		 * ;
		 */

	}

}
