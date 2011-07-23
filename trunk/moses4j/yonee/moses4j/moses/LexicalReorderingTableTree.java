package yonee.moses4j.moses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 
 * @author YONEE
 * @LATER
 */

public class LexicalReorderingTableTree extends LexicalReorderingTable {

	
	public LexicalReorderingTableTree(final String filePath, final int[] f_factors,
			final int[] e_factors, final int[] c_factors) {
		super(f_factors, e_factors, c_factors);
		m_UseCache = false;
		m_FilePath = filePath;
	}

	public boolean isCacheEnabled() {
		return m_UseCache;
	};

	public void enableCache() {
		m_UseCache = true;
	};

	public void disableCache() {
		m_UseCache = false;
	};

	public void clearCache() {
		if (m_UseCache) {
			m_Cache.clear();
		}
	};

//	public float[] getScore(final Phrase f, final Phrase e, final Phrase c) {
//		if ((m_FactorsF != null && 0 == f.getSize()) || (m_FactorsE != null && 0 == e.getSize())) {
//			// NOTE: no check for c as c might be empty, e.g. start of sentence
//			// not a proper key
//			// phi: commented out, since e may be empty (drop-unknown)
//			// std::cerr << "Not a proper key!\n";
//			return null;
//		}
//
//		// CacheType::iterator i;;
//		if (m_UseCache) {
//			Candidates r = m_Cache.put(makeCacheKey(f, e), new Candidates());
//			// std::pair<CacheType::iterator, bool> r =
//			// m_Cache.insert(std::make_pair(MakeCacheKey(f,e),Candidates()));
//			if (r == null) {
//				return auxFindScoreForContext(r, c);
//			}
//			// i = r.first;
//		} else if (!m_Cache.isEmpty()) {
//			// although we might not be caching now, cache might be none empty!
//			Candidates r = m_Cache.get(makeCacheKey(f, e));
//			if (r != null) {
//				return auxFindScoreForContext(r, c);
//			}
//		}
//		// not in cache go to file...
//		float[] score = null;
//		Candidates cands = new Candidates();
//		m_Table.getCandidates(makeTableKey(f, e), cands);
//		if (cands.isEmpty()) {
//			return null;
//		}
//
//		if (m_FactorsC == null) {
//			ASSERT.a(1 == cands.size());
//			return cands.get(0).getScore(0);
//		} else {
//			score = auxFindScoreForContext(cands, c);
//		}
//		// cache for future use
//		if (m_UseCache) {
//			m_Cache.put(makeCacheKey(f, e), cands);
//			// i.second = cands;
//		}
//		return score;
//	}

	public void initializeForInput(final InputType input) {
		clearCache();
		ConfusionNet cn = (ConfusionNet) input;
		Sentence s = (Sentence) input;
		if (cn != null) {
			cache(cn);
		} else if (s != null) {
			// Cache(*s); ... this just takes up too much memory, we cache elsewhere
			disableCache();
		}
		if (!m_Table.get()) {
			// load thread specific table.
			m_Table.reset(new PrefixTreeMap());
			m_Table.read(m_FilePath + ".binlexr");
		}
	}

	public void initializeForInputPhrase(final Phrase f) {
//		clearCache();
//		auxCacheForSrcPhrase(f);
	}

	//public static boolean create(BufferedReader inFile, final String outFileName) {
		// String line;
		// //TRACE_ERR("Entering Create...\n");
		// String
		// ofn = outFileName+".binlexr.srctree",
		// oft = outFileName+".binlexr.tgtdata",
		// ofi = outFileName+".binlexr.idx",
		// ofsv = outFileName+".binlexr.voc0",
		// oftv = outFileName+".binlexr.voc1";
		//		  
		//
		// RandomAccessFile os = new RandomAccessFile(ofn,"w");
		//		 
		//		 
		// RandomAccessFile ot = new RandomAccessFile(oft,"w");
		//
		// //TRACE_ERR("opend files....\n");
		//
		// //typedef PrefixTreeSA<LabelId,OFF_T> PSA;
		// PrefixTreeSA<Integer,Long> psa = new PrefixTreeSA<Integer,Long>();
		// PrefixTreeSA.setDefault(FileUtils.InvalidOffT);
		// LVoc<String>[] voc = new LVoc[3];
		//		    
		// int currFirstWord = LVoc.InvalidLabelId;
		// List<Integer> currKey;
		//
		// Candidates cands;
		// List<Long> vo = new ArrayList<Long>();
		// int lnc = 0;
		// int numTokens = 0;
		// int numKeyTokens = 0;
		// while((line = inFile.readLine() ) != null){
		// //TRACE_ERR(lnc<<":"<<line<<"\n");
		// ++lnc;
		// if(0 == lnc % 10000){
		// TRACE.err(".");
		// }
		// List<Integer> key = new ArrayList<Integer>();
		// List<Float> score = new ArrayList<Float>();
		//
		// String[] tokens = Util.tokenizeMultiCharSeparator(line, "|||");
		// String w;
		// if(1 == lnc){
		// //do some init stuff in the first line
		// numTokens = tokens.length;
		// if(tokens.length == 2){ //f ||| score
		// numKeyTokens = 1;
		// voc[0] = new LVoc<String>();
		// voc[1] = null;
		// } else if(3 == tokens.length || 4 == tokens.length){ //either f ||| e ||| score or f |||
		// e ||| c ||| score
		// numKeyTokens = 2;
		// voc[0] = new LVoc<String>(); //f voc
		// voc[1] = new LVoc<String>(); //e voc
		// voc[2] = voc[1]; //c & e share voc
		// }
		// } else {
		// //sanity check ALL lines must have same number of tokens
		// ASSERT.a(numTokens == tokens.length);
		// }
		// int phrase = 0;
		// for(; phrase < numKeyTokens; ++phrase){
		// //conditioned on more than just f... need |||
		// if(phrase >=1){
		// key.add(PrefixTreeMap.MagicWord);
		// }
		// StringReader is = new StringReader(tokens[phrase]);
		// while((w = is.getString()) != null) {
		// key.add(voc[phrase].add(w));
		// }
		// }
		// //collect all non key phrases, i.e. c
		// List<Integer> tgt_phrases[] = new ArrayList[numTokens - numKeyTokens - 1];
		// //CollectionUtils.fill(tgt_phrases, 0, numTokens - numKeyTokens - 1, ArrayList.class);
		//		    
		// for(int j = 0; j < tgt_phrases.length; ++j, ++phrase){
		// StringReader is = new StringReader(tokens[numKeyTokens + j]);
		// while((w = is.getString()) != null) {
		// tgt_phrases[j].add(voc[phrase].add(w));
		// }
		// }
		// //last token is score
		// StringReader is = new StringReader(tokens[numTokens-1]);
		// while((w = is.getString()) != null) {
		// score.add(Float.valueOf(w));
		// }
		//		    
		// //transform score now...
		//		    
		// Util.transformScore(score);
		// Util.floorScore(score);
		//		    
		// List< List<Float> > scores = new ArrayList< List<Float> >();
		// scores.add(score);
		//		    
		// if(key.isEmpty()) {
		// TRACE.err("WARNING: empty source phrase in line '"+line+"'\n");
		// continue;
		// }
		// //first time inits
		// if(currFirstWord == FileUtils.InvalidLabelId){
		// currFirstWord = key.get(0);
		// }
		// if(currKey != null){
		// currKey = key;
		// //insert key into tree
		// ASSERT.a(psa != null);
		// // PSA::Data d = psa.insert(key);
		//		      
		// if(d == FileUtils.InvalidOffT) {
		// d = ot.getFilePointer();
		// } else {
		// TRACE.err("ERROR: source phrase already inserted (A)!\nline(" + lnc + "): '" + line +
		// "\n");
		// return false;
		// }
		// }
		// if(currKey != key){
		// //ok new key
		// currKey = key;
		// //a) write cands for old key
		// cands.writeBin(ot);
		// cands.clear();
		// //b) check if we need to move on to new tree root
		// if(key[0] != currFirstWord){
		// // write key prefix tree to file and clear
		// PTF pf;
		// if(currFirstWord >= vo.size()){
		// vo.resize(currFirstWord+1,InvalidOffT);
		// }
		// vo[currFirstWord] = fTell(os);
		// pf.create(psa, os);
		// // clear
		// delete psa; psa = new PSA;
		// currFirstWord = key[0];
		// }
		// //c) insert key into tree
		// ASSERT.a(psa != null);
		// PSA::Data& d = psa.insert(key);
		// if(d == InvalidOffT) {
		// d = fTell(ot);
		// } else {
		// TRACE_ERR("ERROR: source phrase already inserted (A)!\nline(" << lnc << "): '" << line <<
		// "\n");
		// return false;
		// }
		// }
		// cands.add(GenericCandidate(tgt_phrases, scores));
		// }
		// //flush remainders
		// cands.writeBin(ot);
		// cands.clear();
		// //process last currFirstWord
		// PTF pf;
		// if(currFirstWord >= vo.size()) {
		// vo.resize(currFirstWord+1,InvalidOffT);
		// }
		// vo[currFirstWord] = fTell(os);
		// pf.create(psa,os);
		// delete psa;
		// psa=0;
		//		  
		// os.close();
		// ot.close();
		//		
		//		  
		// std::vector<int> inv;
		// for(int i = 0; i < vo.size(); ++i){
		// if(vo[i] == InvalidOffT){
		// inv.push_back(i);
		// }
		// }
		// if(inv.size()) {
		// TRACE_ERR("WARNING: there are src voc entries with no phrase "
		// "translation: count "<<inv.size()<<"\n"
		// "There exists phrase translations for "<<vo.size()-inv.size()
		// <<" entries\n");
		// }
		//		  
		// RandomAccessFile oi = new RandomAccessFile(ofi,"w");
		//		  
		// for(int i = 0; i < vo.size();i++){
		// oi.writeLong(vo.get(i));
		// }
		// //fWriteVector(oi,vo);
		// //fClose(oi);
		// oi.close();
		//		  
		// if(voc[0] != null){
		// voc[0].write(ofsv);
		// voc[0] = null;
		// }
		// if(voc[1] != null){
		// voc[1].write(oftv);
		// voc[1] = null;
		// }
		// return true;
//		return true;
//	}



	private String makeCacheKey(final Phrase f, final Phrase e) {
		StringBuilder key = new StringBuilder();
		if (m_FactorsF != null) {
			key.append(f.getStringRep(m_FactorsF).trim());
		}
		if (m_FactorsE != null) {
			if (key != null) {
				key.append("|||");
			}
			key.append(e.getStringRep(m_FactorsE).trim());
		}
		return key.toString();
	}

	private List<Integer> makeTableKey(final Phrase f, final Phrase e) {
		List<Integer> key = new ArrayList<Integer>();
		List<String> keyPart = new ArrayList<String>();
		if (m_FactorsF != null) {
			for (int i = 0; i < f.getSize(); ++i) {
				/*
				 * old code
				 * String s = f.GetWord(i).ToString(m_FactorsF);
				 * keyPart.push_back(s.substr(0,s.size()-1));
				 */
				keyPart.add(f.getWord(i).getString(m_FactorsF, false));
			}
			auxAppend(key, m_Table.convertPhrase(keyPart, SourceVocId));
			keyPart.clear();
		}
		if (m_FactorsE != null) {
			if (!key.isEmpty()) {
				key.add(PrefixTreeMap.MagicWord);
			}
			for (int i = 0; i < e.getSize(); ++i) {
				/*
				 * old code
				 * String s = e.GetWord(i).ToString(m_FactorsE);
				 * keyPart.push_back(s.substr(0,s.size()-1));
				 */
				keyPart.add(e.getWord(i).getString(m_FactorsE, false));
			}
			auxAppend(key, m_Table.convertPhrase(keyPart, TargetVocId));
			// keyPart.clear();
		}
		return key;
	}

	private void cache(final ConfusionNet input) {
		return;
	}

//	private void cache(final Sentence input) {
//		// only works with sentences...
//		int prev_cache_size = m_Cache.size();
//		int max_phrase_length = input.getSize();
//		for (int len = 0; len <= max_phrase_length; ++len) {
//			for (int start = 0; start + len <= input.getSize(); ++start) {
//				Phrase f = input.getSubString(new WordsRange(start, start + len));
//				auxCacheForSrcPhrase(f);
//			}
//		}
//		System.err.print("Cached " + (m_Cache.size() - prev_cache_size)
//				+ " new primary reordering table keys\n");
//	}

//	private void auxCacheForSrcPhrase(final Phrase f) {
//		if(m_FactorsE!=null){
//			//f is all of key...
//			Candidates cands;
//			m_Table.getCandidates(makeTableKey(f,new Phrase(FactorDirection.Output)),cands);
//			m_Cache.put(makeCacheKey(f,new Phrase(FactorDirection.Output)), cands);
//		  } else {
//			ObjectPool<PPimp>     pool;
//			PPimp pPos  = m_Table.getRoot();
//			//1) goto subtree for f
//			for(int i = 0; i < f.getSize() && null != pPos && pPos.isValid(); ++i){
//			  /* old code
//			  pPos = m_Table.Extend(pPos, auxClearString(f.GetWord(i).ToString(m_FactorsF)), SourceVocId);
//			  */
//			  pPos = m_Table.extend(pPos, f.getWord(i).getString(m_FactorsF, false), SourceVocId);
//			}
//			if(null != pPos && pPos.isValid()){
//			  pPos = m_Table.extend(pPos, PrefixTreeMap.MagicWord);
//			}
//			if(null == pPos || !pPos.isValid()){
//			  return;
//			}
//			//2) explore whole subtree depth first & cache
//			String cache_key = auxClearString(f.getStringRep(m_FactorsF)) + "|||";
//			
//			List<State> stack = new ArrayList<State>();
//			stack.push_back(State(pool.get(PPimp(pPos.ptr().getPtr(pPos.idx),0,0)),""));
//			Candidates cands;
//			while(!stack.isEmpty()){
//			  if(stack.back().pos.isValid()){
//				LabelId w = stack.back().pos.ptr().getKey(stack.back().pos.idx);
//				String next_path = stack.back().path + " " + m_Table.ConvertWord(w,TargetVocId);
//				//cache this 
//				m_Table.GetCandidates(*stack.back().pos,&cands);
//				if(!cands.empty()){ 
//				  m_Cache[cache_key + auxClearString(next_path)] = cands;
//				}
//				cands.clear();
//				PPimp* next_pos = pool.get(PPimp(stack.back().pos.ptr().getPtr(stack.back().pos.idx),0,0));
//				++stack.back().pos.idx;
//				stack.push_back(State(next_pos,next_path));
//			  } else {
//				stack.pop_back();
//			  }
//			}
//		  }
//	}

	private float[] auxFindScoreForContext(final Candidates cands, final Phrase contex) {
		if (m_FactorsC == null) {
			assert (cands.size() <= 1);
			return (1 == cands.size()) ? (cands.get(0).getScore(0)) : null;
		} else {
			List<String> cvec = new ArrayList<String>();
			for (int i = 0; i < contex.getSize(); ++i) {
				/*
				 * old code
				 * String s = context.GetWord(i).ToString(m_FactorsC);
				 * cvec.push_back(s.substr(0,s.size()-1));
				 */
				cvec.add(contex.getWord(i).getString(m_FactorsC, false));
			}
			int[] c = m_Table.convertPhrase(cvec, TargetVocId);
			int[] sub_c = new int[c.length];
			int start = 0;
			for (int j = 0; j <= contex.getSize(); ++j, ++start) {
				System.arraycopy(c, 0, sub_c, 0, c.length);
				// sub_c.assign(start, c.end());

				for (int cand = 0; cand < cands.size(); ++cand) {
					int[] p = cands.get(cand).getPhrase(0);
					if (cands.get(cand).getPhrase(0) == sub_c) {
						return cands.get(cand).getScore(0);
					}
				}
			}
			return null;
		}
	}

	// typedef LexicalReorderingCand CandType;
	// private typedef std::map< String, Candidates > CacheType;
	// #ifdef WITH_THREADS
	// typedef boost::thread_specific_ptr<PrefixTreeMap> TableType;
	// #else
	// typedef std::auto_ptr<PrefixTreeMap> TableType;
	// #endif

	private static final int SourceVocId = 0;
	private static final int TargetVocId = 1;

	private boolean m_UseCache;
	private String m_FilePath;
	private Map<String, Candidates> m_Cache = new HashMap<String, Candidates>();
	private PrefixTreeMap m_Table = new PrefixTreeMap();
	@Override
	public List<Float> getScore(Phrase f, Phrase e, Phrase c) {
		// TODO Auto-generated method stub
		return null;
	}

}
