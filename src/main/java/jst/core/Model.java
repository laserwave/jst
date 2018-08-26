
package jst.core;

import jst.utils.Pair;
import jst.utils.Utils;

import java.io.*;
import java.util.*;


public class Model {


	public static String assignSuffix; // suffix for sentiment-topic assignment file
	public static String thetaSuffix; // suffix for theta (sentiment-topic distribution) file
	public static String piSuffix; // suffix for pi (sentiment distribution) file
	public static String phiSuffix; // suffix for phi (topical word distribution) file
	public static String othersSuffix; // suffix for containing other parameters
	public static String stwordsSuffix; // suffix for file containing words-per-sentiment/topic
	public static String wordMapSuffix; // suffix for file containing word to id map


	public String dir; // path of the directory containing the corpus file
    public String mdir; // path of the model directory
	public String train_corpus; // file name of train corpus
	public String test_corpus;  // file name of test corpus
	public String modelName; // name of the model
	public Corpus corpus; // link to a corpus

	public int D; // corpus size (i.e., number of docs)
	public int V; // vocabulary size
	public int T; // number of topics
	public int S; // number of sentiments


	public double alpha, beta, gamma; // hyperparameters
    public double[][][] beta_szw;   // for train/test
    public double[][] beta_szwsum;  // for train/test
    public double[][] lambda_sw;  // for train/test
    public double[][] gamma_ds;   // only for train
    public double[] gamma_dssum;   // only for train

	public int train_iters; // number of Gibbs sampling iterations per chain in training
    public int test_iters; // number of Gibbs sampling iterations per chain in testing
	public int savestep; // saving period
	public int topwords; // print out top words

	// Estimated/inferred parameters
	public double[][][] theta; // theta: sentiment-specific distributions over topics, size D x S x T
	public double[][] pi; // pi: distribution over sentiments, size D * S
	public double[][][] phi; // phi: sentiment-topic-specific distributions over words, size S x T x V

	// Variables for sampling
	public int[][] zAssign; // topic assignments for all words
	public int[][] sAssign; // sentiment assignments for all words

	public double[][][] nszw; // nszw[i][j][k]: number of word k in the corpus associated with sentiment i and topic j, size S x T x V

    public double[][] nsz; // nsz[i][j]: number of total words in the corpus associated with sentiment i and topic j, size S x T

	public double[][][] ndsz; // ndsz[i][j][k]: number of words in document i associated with sentiment j and topic k, size D x S x T

	public double[][] nds; // nds[i][j]: number of words in document i associated with sentiment j, size D x S

	public double[] nd; // nd[i]: number of words in document i, size D

	public double[][] psz; // propability to perform multinominal sampling via cumulative method

    public double perplexity; // perplexity of the model on the corpus
	public double accAT1;
    public double accAT2;
    public double accAT3;
    public double ap;


	// Random number generators
	private Random topicRandomGenerator;
	private Random sentimentRandomGenerator;


	public Model() {
		setDefaultValues();
	}

	/**
	 * Set default values for variables.
	 */
	public void setDefaultValues() {
		assignSuffix = ".assign";
		thetaSuffix = ".theta";
		piSuffix = ".pi";
		phiSuffix = ".phi";
		othersSuffix = ".others";
		stwordsSuffix = ".stwords";
		wordMapSuffix = ".wordmap";

		dir = "";
		mdir = "";
        train_corpus = "";
        test_corpus = "";

		D = 0;
		V = 0;
		T = 10;
		S = 6;
		perplexity = 0;
		// default values of alpha, gamma are not set here
        alpha = -1;
        beta = 0.01;
        gamma = -1;
		train_iters = 1000;
		test_iters = 50;
		savestep = 200;
		topwords = 20;

		zAssign = null;
		sAssign = null;
		nszw = null;
		nsz = null;
		ndsz = null;
		nds = null;
		nd = null;

		theta = null;
		pi = null;
		phi = null;
		beta_szw = null;
		beta_szwsum = null;
		lambda_sw = null;
		gamma_ds = null;
		gamma_dssum = null;

		topicRandomGenerator = null;
		sentimentRandomGenerator = null;
	}


	/**
	 * Read others file to get parameters.
	 */
    private boolean readOthersFile(String otherFile) {
		// open file <model>.others to read
		try {
			BufferedReader reader = new BufferedReader(new FileReader(otherFile));
			String line;
			while ((line = reader.readLine()) != null) {
				StringTokenizer tknr = new StringTokenizer(line,"= \t\r\n");

				int count = tknr.countTokens();
				if (count != 2) {
					continue;
				}

				String optstr = tknr.nextToken();
				String optval = tknr.nextToken();

				if (optstr.equalsIgnoreCase("alpha")) {
					alpha = Double.parseDouble(optval);
				} else if (optstr.equalsIgnoreCase("beta")) {
					beta = Double.parseDouble(optval);
				} else if (optstr.equalsIgnoreCase("gamma")) {
                    gamma = Double.parseDouble(optval);
				} else if (optstr.equalsIgnoreCase("T")) {
					T = Integer.parseInt(optval);
				} else if (optstr.equalsIgnoreCase("S")) {
					S = Integer.parseInt(optval);
				} else if (optstr.equalsIgnoreCase("V")) {
					V = Integer.parseInt(optval);
				} else if (optstr.equalsIgnoreCase("D")) {
					D = Integer.parseInt(optval);
				}
			}

			reader.close();

		} catch (Exception e) {
			System.out.println("Error while reading others file: " + e.getMessage());
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 *  Read the file containing the variable assignments.
	 */
    private boolean readAssignFile(String assignFile) {
		// open file <model>.assign to read
		try {
			int d, i;
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(assignFile), "UTF-8"));

			try {
				String line;
				zAssign = new int[D][];
				sAssign = new int[D][];
				corpus = new Corpus(D);
                corpus.V = V;

				for (d = 0; d < D; d++) {
					// one document per line
					line = reader.readLine();
                    // each line contains blocks seperated by a blank, and each block is w:s:z
                    StringTokenizer tknr = new StringTokenizer(line, " ");
                    Vector<Integer> words = new Vector<Integer>();
					int N = tknr.countTokens();
                    zAssign[d] = new int[N];
                    sAssign[d] = new int[N];
                    for (i = 0; i < N; i++){
                        String block = tknr.nextToken();
                        String[] blockSplit = block.split("[:]");
                        if (blockSplit.length != 3) {
                            System.out.println("Invalid word assignment line\n");
                            return false;
                        }
                        int wordId = Integer.parseInt(blockSplit[0]);
                        words.add(wordId);
                        sAssign[d][i] = Integer.parseInt(blockSplit[1]);
                        zAssign[d][i] = Integer.parseInt(blockSplit[2]);
                    }

					// allocate and add new document to the corpus
					Document doc = new Document(N, words, "", null, null);
					corpus.setDoc(doc, d);
				}

			} finally {
				reader.close();
			}

		} catch (Exception e) {
			System.out.println("Error while loading model: " + e.getMessage());
			e.printStackTrace();
			return false;
		}

		return true;
	}



	/**
	 * Save sentiment and topic assignments for this model.
	 */
    private boolean saveModelAssign(String filename) {

		int d, n;

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
            // write docs with sentiment assignments and topic assignments for words
            for (d = 0; d < corpus.D; d++) {
               for (n = 0; n < corpus.docs[d].length; n++){
                   String space = (n == corpus.docs[d].length-1?"":" ");
                   writer.write(corpus.docs[d].words[n] + ":" + sAssign[d][n] + ":" + zAssign[d][n] + space);
               }
               writer.write("\n");
            }
			writer.close();

		} catch (Exception e) {
			System.out.println("Error while saving model assign: " + e.getMessage());
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Save theta (joint sentiment-topic distribution) for this model.
	 */
    private boolean saveModelTheta(String filename) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
            for (int i = 0; i < D; i++) {
                for (int j = 0; j < S; j++) {
                    for (int k = 0; k < T; k++) {
                        writer.write(theta[i][j][k] + " ");
                    }
                }
                writer.write("\n");
            }

			writer.close();

		} catch (Exception e) {
			System.out.println("Error while saving the joint sentiment-topic distribution file for this model: " + e.getMessage());
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Save pi (sentiment distribution) for this model.
	 */
    private boolean saveModelPi(String filename) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

			for (int i = 0; i < D; i++){
                for (int j = 0; j < S; j++) {
                    writer.write(pi[i][j] + " ");
                }
                writer.write("\n");
            }
			writer.close();

		} catch (Exception e) {
			System.out.println("Error while saving the sentiment distribution file for this model: " + e.getMessage());
			e.printStackTrace();
			return false;
		}

		return true;
	}


	/**
	 * Save phi (joint sentiment-topic word distribution) for this model.
	 */
    private boolean saveModelPhi(String filename) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

			for (int i = 0; i < S; i++) {
				for (int j = 0; j < T; j++) {
					for (int k = 0; k < V; k++) {
						writer.write(phi[i][j][k] + " ");
					}
					writer.write("\n");
				}
				writer.write("\n");
			}

			writer.close();

		} catch (Exception e) {
			System.out.println("Error while saving the joint sentiment-topic word distribution file for this model: " + e.getMessage());
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Save other information of this model.
	 */
    private boolean saveModelOthers(String filename, RunTime runTime) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

			writer.write("alpha=" + alpha + "\n");
			writer.write("beta=" + beta + "\n");
			writer.write("gamma=" + gamma + "\n");
			writer.write("T=" + T + "\n");
			writer.write("S=" + S + "\n");
			writer.write("D=" + D + "\n");
			writer.write("V=" + V + "\n");
			writer.write("acc@1=" + accAT1 + "\n");
			writer.write("acc@2=" + accAT2 + "\n");
            writer.write("acc@3=" + accAT3 + "\n");
            writer.write("ap=" + ap + "\n");
            writer.write("perplexity" + perplexity + "\n");
            writer.write("train_iters=" + train_iters + "\n");
            writer.write("test_iters=" + test_iters + "\n");
			if(runTime != null){
				writer.write("所有迭代总运行时间(ms)=" + runTime.totalTimeSeconds() + "\n");
				writer.write("单次迭代运行时间(ms)=" + runTime.timeEachGibbsSampling() + "\n");
			}
			writer.close();

		} catch(Exception e) {
			System.out.println("Error while saving model others:" + e.getMessage());
			e.printStackTrace();
			return false;
		}

		return true;
	}


	/**
	 * Save the most likely joint sentiment-topic words for each sentiment and each topic.
	 */
    private boolean saveModelSTWords(String filename) {
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(filename), "UTF-8"));

			if (topwords > V) {
				topwords = V;
			}

			for (int i = 0; i < S; i++) {
				for (int j = 0; j < T; j++) {
					List<Pair> wordsProbsList = new ArrayList<Pair>();

					for (int k = 0; k < V; k++) {
						Pair p = new Pair(k, phi[i][j][k], false);
						wordsProbsList.add(p);
					} // end for each word

					// print sentiment and topic
					writer.write(Lexicon.sentimentNames[i] + ", Topic " + j + ":\n");
					Collections.sort(wordsProbsList);

					for (int t = 0; t < topwords; t++) {
						if (corpus.localDict.contains((Integer)wordsProbsList.get(t).first)) {
							String word = corpus.localDict.getWord((Integer)wordsProbsList.get(t).first);
							writer.write("\t" + word + " " + wordsProbsList.get(t).second + "\n");
						}
					}
				} // end for each topic
			} // end for each sentiment

			writer.close();

		} catch(Exception e) {
			System.out.println("Error while saving model vtwords: " + e.getMessage());
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Save the model.
	 */
	public boolean saveModel(String modelName, RunTime runTime) {
		if (!saveModelAssign(mdir + File.separator + modelName + assignSuffix)) {
			return false;
		}

		if (!saveModelOthers(mdir + File.separator + modelName + othersSuffix, runTime)) {
			return false;
		}

		if (!saveModelTheta(mdir + File.separator + modelName + thetaSuffix)) {
			return false;
		}

		if (!saveModelPi(mdir + File.separator + modelName + piSuffix)) {
			return false;
		}

		if (!saveModelPhi(mdir + File.separator + modelName + phiSuffix)) {
			return false;
		}

		if (topwords > 0) {
			if (!saveModelSTWords(mdir + File.separator + modelName + stwordsSuffix)) {
				return false;
			}
		}



		return true;
	}


	/**
	 * Initialize the hyperparameter from option.
	 */
	private boolean init(CmdOption option) {
		if (option == null) {
			return false;
		}

		dir = option.dir;
		mdir = option.model_dir;

        train_corpus = option.train_corpus;
        test_corpus = option.test_corpus;

		if (!option.model_name.equals("")){
            modelName = option.model_name;
        }

		T = option.T;
		S = option.S;

		if(option.alpha > 0){
		    alpha = option.alpha;
        }

		if (option.beta > 0) {
			beta = option.beta;
		}

        if (option.gamma > 0) {
            gamma = option.gamma;
        }
        train_iters = option.train_iters;
		test_iters = option.test_iters;
		topwords = option.topwords;
		savestep = option.savestep;
		return true;
	}

    /**
     * Initialize the counts to all zero.
     */
	private boolean initCountsZero(){
        int i, j, k;

        nszw = new double[S][T][V];
        for (i = 0; i < S; i++) {
            for (j = 0; j < T; j++) {
                for(k = 0; k < V; k++){
                    nszw[i][j][k] = 0;
                }
            }
        }

        nsz = new double[S][T];
        for (i = 0; i < S; i++) {
            for (j = 0; j < T; j++) {
                nsz[i][j] = 0;
            }
        }

        ndsz = new double[D][S][T];
        for (i = 0; i < D; i++) {
            for (j = 0; j < S; j++) {
                for (k = 0; k < T; k++) {
                    ndsz[i][j][k] = 0;
                }
            }
        }

        nds = new double[D][S];
        for (i = 0; i < D; i++) {
            for (j = 0; j < S; j++) {
                nds[i][j] = 0;
            }
        }

        nd = new double[D];
        for (i = 0; i < D; i++){
            nd[i] = 0;
        }
	    return true;
    }

	/**
	 * Initialize the sentiment and topic assigns (incorporate sentiment prior) and the counts.
	 */
	private boolean initCountsAndAssignsPrior(){

		initCountsZero();

		int d, n;

		topicRandomGenerator = new Random();
		sentimentRandomGenerator = new Random();

		zAssign = new int[D][];
		sAssign = new int[D][];

		// initialize zAssign, sAssign for each word
		for (d = 0; d < corpus.D; d++){
			int N = corpus.docs[d].length; // number of words in document d
			zAssign[d] = new int[N];
			sAssign[d] = new int[N];

			for (n = 0; n < N; n++){


				int word = corpus.docs[d].words[n];
                int topic = topicRandomGenerator.nextInt(T);
                zAssign[d][n] = topic;
                int sentiment = -1;
				if(Lexicon.lexicon.containsKey(word)){
				    double[] dist = Lexicon.lexicon.get(word);
                    List<Integer> res = Utils.argmax(dist);

                    if(res.size() == 1){
                        sentiment = res.get(0);
                    }else{
                        while(!res.contains(sentiment)){
                            sentiment = sentimentRandomGenerator.nextInt(S);
                        }
                    }
                }else{
                    sentiment = sentimentRandomGenerator.nextInt(S);
                }

                sAssign[d][n] = sentiment;

				nszw[sentiment][topic][word] += 1;
				nsz[sentiment][topic] += 1;
				ndsz[d][sentiment][topic] += 1;
				nds[d][sentiment] += 1;
				nd[d] += 1;
			}
		}

		return true;
	}

    /**
     * Initialize the sentiment and topic assigns (randomly) and the counts.
     */
	private boolean initCountsAndAssignsRandomly(){

	    initCountsZero();

        int d, n;

        topicRandomGenerator = new Random();
        sentimentRandomGenerator = new Random();

        zAssign = new int[D][];
        sAssign = new int[D][];

        // initialize zAssign, sAssign for each word
        for (d = 0; d < corpus.D; d++){
            int N = corpus.docs[d].length; // number of words in document d
            zAssign[d] = new int[N];
            sAssign[d] = new int[N];
            for (n = 0; n < N; n++){
                int topic = topicRandomGenerator.nextInt(T);
                zAssign[d][n] = topic;
                int sentiment = sentimentRandomGenerator.nextInt(S);
                sAssign[d][n] = sentiment;

                int word = corpus.docs[d].words[n];
                nszw[sentiment][topic][word] += 1;
                nsz[sentiment][topic] += 1;
                ndsz[d][sentiment][topic] += 1;
                nds[d][sentiment] += 1;
                nd[d] += 1;
            }
        }

        return true;
    }

    /**
     * Initialize the model hyparameters and parameters.
     */
	private boolean initModelParameters(){
	    beta_szw = new double[S][T][V];
	    beta_szwsum = new double[S][T];
	    lambda_sw = new double[S][V];
        gamma_ds = new double[D][S];
        gamma_dssum = new double[D];

        theta = new double[D][S][T];
        pi = new double[D][S];
        phi = new double[S][T][V];
        return true;
    }

    /**
     * incorporate prior to beta
     */
    public void incorporatePriorToBeta(){

	    if(Lexicon.loadLexicon == false || Lexicon.S != S){
            System.out.println("Sentiment Lexicon not used");
	        for(int i = 0; i < S; i++){
	            for(int j = 0; j < T; j++){
	                for(int k = 0; k < V; k++){
	                    beta_szw[i][j][k] = beta;
                    }
                    beta_szwsum[i][j] = V * beta;
                }
            }
        }else{
	        for(int i = 0; i < S; i++){
	            for(int j = 0; j < V; j++){
                    lambda_sw[i][j] = 1.0;
                }
            }

            for(String word: Lexicon.lexicon.keySet()){
	            if(corpus.localDict.word2id.containsKey(word)){
                    int w = corpus.localDict.word2id.get(word);
                    for(int i = 0; i < S; i++){
                        lambda_sw[i][w] = Lexicon.lexicon.get(word)[i];
                    }
                }
            }

            for(int i = 0; i < S; i++){
                for(int j = 0; j < T; j++){
                    for(int k = 0; k < V; k++){
                        beta_szw[i][j][k] = beta * lambda_sw[i][k];
                        beta_szwsum[i][j] += beta_szw[i][j][k];
                    }
                }
            }

        }
    }

    /**
     * incorporate prior to gamma
     */
    public void incorporatePriorToGamma(){
        for(int i = 0; i < D; i++){
            if(corpus.docs[i].distribution == null){
                for(int j = 0; j < S; j++){
                    gamma_ds[i][j] = gamma;
                }
                gamma_dssum[i] = S * gamma;
            }else{
                for(int j = 0; j < S; j++){
                    gamma_ds[i][j] = gamma * S * corpus.docs[i].distribution[j];
                    gamma_dssum[i] += gamma_ds[i][j];
                }
            }
        }
    }


	/**
	 * Initial a train model in -est process
	 */
	public boolean initNewModel(CmdOption option){
		if (!init(option)) {
			return false;
		}
		psz = new double[S][T];

		corpus = Corpus.readCorpus(dir + File.separator + train_corpus);
        System.out.println("read corpus, D = " + corpus.D);
		if (corpus == null) {
			System.out.println("Fail to read training data!\n");
			return false;
		}

		if(alpha < 0){
		    alpha = corpus.avg_doc_len * 0.05 / (S * T);
        }
        if(gamma < 0){
		    gamma = corpus.avg_doc_len * 0.05 / S;
        }

		// assign values for variables
		D = corpus.D;
		V = corpus.V;

//		initCountsAndAssignsRandomly();
        initCountsAndAssignsPrior();
		initModelParameters();



		return true;
	}

	/**
	 * Initial a test model in -inf process
	 */
	public boolean initNewModel(CmdOption option, Model trModel) {
		if (!init(option))
			return false;

		Corpus newCorpus = Corpus.readCorpus(dir + File.separator + test_corpus, trModel.corpus.localDict);
		if (newCorpus == null) {
			System.out.println("Fail to read corpus!\n");
			return false;
		}
        T = trModel.T;
        S = trModel.S;
        alpha = trModel.alpha;
        beta = trModel.beta;
        gamma = trModel.gamma;

        psz = new double[S][T];
        System.out.println("T:" + T);
        System.out.println("S:" + S);

        corpus = newCorpus;

        // assign values for variables
        D = corpus.D;
        V = corpus.V;
        System.out.println("D:" + D);
        System.out.println("V:" + V);

//        initCountsAndAssignsRandomly();
        initCountsAndAssignsPrior();
        initModelParameters();
        return true;
	}


	/**
	 * Load a trained model in -inf process
	 */
	public boolean initEstimatedModel(CmdOption option) {
		if (!init(option))
			return false;

        psz = new double[S][T];

		if (!loadModel()) {
			System.out.println("Fail to load the assignment file of the model!\n");
			return false;
		}

		System.out.println("Model loaded:");
		System.out.println("\talpha:" + alpha);
		System.out.println("\tbeta:" + beta);
		System.out.println("\tgamma:" + gamma);
		System.out.println("\tD:" + D);
		System.out.println("\tV:" + V);

        initCountsZero();

        int d, n;
        // initialize count variables according to assigns
        for (d = 0; d < corpus.D; d++){
            int N = corpus.docs[d].length; // number of words in document d
            for (n = 0; n < N; n++){
                int topic = zAssign[d][n];
                int sentiment = sAssign[d][n];
                int word = corpus.docs[d].words[n];
                nszw[sentiment][topic][word] += 1;
                nsz[sentiment][topic] += 1;
                ndsz[d][sentiment][topic] += 1;
                nds[d][sentiment] += 1;
                nd[d] += 1;
            }
        }
        initModelParameters();
		return true;
	}

    private boolean loadModel() {
        if (!readOthersFile(mdir + File.separator + modelName + othersSuffix)) {
            return false;
        }

        if (!readAssignFile(mdir + File.separator + modelName + assignSuffix)) {
            return false;
        }

        // read dictionary
        Dictionary dict = new Dictionary();
        if (!dict.readWordMap(mdir + File.separator + modelName + wordMapSuffix)) {
            return false;
        }

        corpus.localDict = dict;

        return true;
    }

    /**
     * Compute the perplexity of the model
     */
    public void computePerplexity() {
        double logP = 0;
        double N = 0;
        for (int d = 0; d < D; d++) {
            Document document = corpus.docs[d];
            N += document.length;
            for (int n = 0; n < document.length; n++) {
                int word = document.words[n];
                double[][] logPsz = new double[S][T];
                for (int i = 0; i < S; i++){
                    for (int j = 0; j < T; j++){
                        logPsz[i][j] += Math.log(phi[i][j][word]);
                        logPsz[i][j] += Math.log(theta[d][i][j]);
                        logPsz[i][j] += Math.log(pi[d][i]);
                    }
                }
                logP += Utils.logSum(logPsz);
            }
        }

        perplexity = Math.exp(-logP/N);
    }


	/**
	 * Compute classification evulation metrics
	 */
	public void evaluateSentiment(){
	    accAT1 = 0;
	    accAT2 = 0;
	    accAT3 = 0;
	    ap = 0;
	    int D2 = 0;
        for(int d = 0; d < D; d++){
            double[] g = corpus.docs[d].distribution;
            double[] p = pi[d];
            int[] acc = Evaluation.acc(g, p);
            accAT1 += acc[0];
            accAT2 += acc[1];
            accAT3 += acc[2];
            double r = Evaluation.pearson_coefficient(g, p);
            if(!Double.isNaN(r)){
                ap += r;
                D2 += 1;
            }
        }
        accAT1 = accAT1 / D;
        accAT2 = accAT2 / D;
        accAT3 = accAT3 / D;
        ap = ap / D2;

        System.out.println("Evaluation:");
        System.out.println("\tacc@1:" + accAT1);
        System.out.println("\tacc@2:" + accAT2);
        System.out.println("\tacc@3:" + accAT3);
        System.out.println("\tap:" + ap);

    }

}
