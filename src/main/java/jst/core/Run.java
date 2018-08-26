package jst.core;


import java.io.File;

/**
 * This is a test run
 */
public class Run {

    public static void main(String args[]) {



        CmdOption option = new CmdOption();

        // chains of gibbs sampling
        option.nchains = 1;

        // training iterations
        option.train_iters = 1000;

        // testing iterations
        option.test_iters = 100;

        option.savestep = 1000;

        // number of topic words to print under each combination of sentiment and topic
        option.topwords = 20;
        // train set file name
        option.train_corpus = "train.txt";
        // test set file name
        option.test_corpus = "test.txt";

        // number of topic
        option.T = 10;
        // number of sentiment
        option.S = 6;

        // sentiment lexicon
        option.lexicon = "data\\lexicon.txt";
        // stopwords
        option.stopwords = "data\\stopwords.txt";
        // the directory of train set and test set
        option.dir = "data";
        // the directory of model file
        option.model_dir = "model";


        JST.checkDir(option);
        File file = new File(option.model_dir);
        if(!file.exists()){
            boolean success = file.mkdirs();
            if(!success) {
                System.out.println("creating model dir " + option.model_dir + " failure!");
                return;
            }
        }
        Lexicon.loadSentimentLexicon(option.lexicon);
        Lexicon.loadStopwords(option.stopwords);
        JST.pipeline(option);



    }
}
