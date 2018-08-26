package jst.core;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

public class Corpus {
	
	public Dictionary localDict; // local dictionary
	public Document[] docs; // a list of documents	
	public int D; // number of documents
	public int V; // number of words
	public double avg_doc_len = 0.0;  // average of document length

	// map word local id to global ones (null if the global dictionary is not set)
	public Map<Integer, Integer> lid2gid;
	
	// link to a global dictionary (used in test)
	public Dictionary globalDict;


	public Corpus() {
		this(0);
	}
	
	public Corpus(int D) {
	    this(D, null);
	}
	
	public Corpus(int D, Dictionary globalDict) {
		this.localDict = new Dictionary();
		this.V = 0;
        this.D = D;
		this.docs = new Document[D];
		this.lid2gid = new HashMap<Integer, Integer>();
		this.globalDict = globalDict;
	}

	/**
	 * Set the document at the index idx if idx is greater than 0 and less than D.
	 * @param doc document to be set
	 * @param idx index in the document array
	 */	
	public void setDoc(Document doc, int idx) {
		if (0 <= idx && idx < D) {
			docs[idx] = doc;
		}
	}
	
	/**
	 * Set the document at the index idx if idx is greater than 0 and less than D.
	 * @param str string contains doc
	 * @param idx index in the document array
	 */
	public void setDoc(String str, int idx) {
		if (0 <= idx && idx < D) {

            StringTokenizer tknr = new StringTokenizer(str, "#");
            assert(tknr.countTokens() == 2);

            String votesStr = tknr.nextToken();
            String sentsStr = tknr.nextToken();

            tknr = new StringTokenizer(votesStr, " ");
            int S = tknr.countTokens();
            int[] votes = new int[S];
            double[] distribution = new double[S];
            int votesSum = 0;

            for(int i = 0; i < S; i++){
                votes[i] = Integer.parseInt(tknr.nextToken());
                votesSum += votes[i];
            }
            for(int i = 0; i < S; i++){
                distribution[i] = votes[i] * 1.0 / votesSum;
            }

            tknr = new StringTokenizer(sentsStr, " |");
            int N = tknr.countTokens();
            String[] words = new String[N];
            for(int i = 0; i < N; i++){
                words[i] = tknr.nextToken();
            }

            Vector<Integer> ids = new Vector<Integer>();
		    for(String word: words){
		    	if (Lexicon.stopwords.contains(word)){
		    	    continue;
                }

                int _id = localDict.word2id.size();
                if (localDict.contains(word)) {
                    _id = localDict.getID(word);
                }
                // inference
                if (globalDict != null) {
                    // get the global id
                    Integer id = globalDict.getID(word);

                    if (id != null) {   // otherwise, the word will not be considered in the model
                        localDict.addWord(word);
                        lid2gid.put(_id, id);
                        ids.add(_id);
                    }
                } else {   // train
                    localDict.addWord(word);
                    ids.add(_id);
                }
            }

			Document doc = new Document(ids.size(), ids, str, votes, distribution);
			docs[idx] = doc;
			V = localDict.word2id.size();
		}
	}
	
	//---------------------------------------------------------------
	// I/O methods
	//---------------------------------------------------------------
	
	/**
	 * Read a dataset from a stream, create new dictionary.
	 * @return dataset if success and null otherwise
	 */
	public static Corpus readCorpus(String filename) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(filename), "UTF-8"));
			Corpus corpus = readCorpus(reader);
			reader.close();
			
			return corpus;
		} catch (Exception e) {
			System.out.println("Read Corpus Error: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Read a dataset from a file with a preknown vocabulary.
	 * @param filename file from which we read dataset
	 * @param globalDict the dictionary
	 * @return dataset if success and null otherwise
	 */
	public static Corpus readCorpus(String filename, Dictionary globalDict) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(filename), "UTF-8"));
			Corpus corpus = readCorpus(reader, globalDict);
			reader.close();

			return corpus;
		} catch (Exception e) {
			System.out.println("Read Corpus Error: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

    /**
     * Read a dataset from a stream, create new dictionary.
     * @return dataset if success and null otherwise
     */
	private static Corpus readCorpus(BufferedReader reader) {
        try {
            // read number of documents
            String line;
            line = reader.readLine();
            int D = Integer.parseInt(line);
            Corpus corpus = new Corpus(D);
            for (int d = 0; d < D; ++d) {
                line = reader.readLine();
                corpus.setDoc(line, d);
            }
			double num_words = 0;
            for (int d = 0; d < D; d++){
            	num_words += corpus.docs[d].length;
			}
			corpus.avg_doc_len = num_words / corpus.D;

            return corpus;
        } catch (Exception e) {
            System.out.println("Read Corpus Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Read a dataset from a stream with respect to a specified dictionary.
     * @param reader stream from which we read dataset
     * @param dict the dictionary
     * @return dataset if success and null otherwise
     */
    private static Corpus readCorpus(BufferedReader reader, Dictionary dict) {
        try {
            // read number of document
            String line;
            line = reader.readLine();
            int D = Integer.parseInt(line);

            Corpus corpus = new Corpus(D, dict);
            for (int d = 0; d < D; ++d) {
                line = reader.readLine();
                corpus.setDoc(line, d);
            }
			double num_words = 0;
			for (int d = 0; d < D; d++){
				num_words += corpus.docs[d].length;
			}
			corpus.avg_doc_len = num_words / corpus.D;
            return corpus;
        } catch (Exception e) {
            System.out.println("Read Corpus Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

}
