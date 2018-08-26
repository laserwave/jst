package jst.core;

import jst.utils.Utils;
import org.kohsuke.args4j.*;

import java.io.File;


/**
 * Joint Sentiment-Topic Model(Incorporating lexicon prior)
 * This is the main class for command line
 */
public class JST {


    public static void main(String args[]) {

        CmdOption option = new CmdOption();
        CmdLineParser parser = new CmdLineParser(option);

        try {
            if (args.length == 0) {
                showHelp(parser);
                return;
            }
            parser.parseArgument(args);

            /**
             * check directory of corpus and model
             */
            checkDir(option);

            /**
             * load sentiment prior of words
             */
            Lexicon.loadSentimentLexicon(option.lexicon);
            Lexicon.loadStopwords(option.stopwords);
            if(option.est){
                Estimator estimator = new Estimator();
                estimator.init(option);
                estimator.estimate();
            }else if(option.inf){
                Inference inference = new Inference();
                inference.init(option);
                inference.infer();
            }else if(option.pipe){
                pipeline(option);
            }

        } catch (CmdLineException cle) {
            System.out.println("Command line error: " + cle.getMessage());
            showHelp(parser);
            return;
        } catch (Exception e) {
            System.out.println("Error in main: " + e.getMessage());
            e.printStackTrace();
            return;
        }
    }

    /**
     * train and test pipeline
     */
    public static void pipeline(CmdOption option){
        System.out.println("Run " + option.nchains + " gibbs sampling chains");
        for(int currentChain = 1; currentChain <= option.nchains; currentChain++){
            System.out.println("Current chain " + currentChain + "...");
            Estimator estimator = new Estimator();
            estimator.init(option);
            estimator.trModel.modelName = "model-" + Utils.zeroPad(currentChain, 2);
            estimator.estimate();
            Inference inference = new Inference();
            estimator.trModel.modelName += "-final";
            inference.init(option, estimator.trModel);
            inference.infer();
        }
    }

    /**
     * check directory of corpus and model
     */
    public static void checkDir(CmdOption option){
        if (option.dir.endsWith(File.separator)) {
            option.dir = option.dir.substring(0, option.dir.length() - 1);
        }
        if (option.model_dir.length()>0 && option.model_dir.endsWith(File.separator)) {
            option.model_dir = option.model_dir.substring(0, option.model_dir.length() - 1);
        }
        if (option.model_dir.equals("")){
            option.model_dir = option.dir;
        }

    }

    public static void showHelp(CmdLineParser parser) {
        System.out.println("JST [options...] [arguments...]");
        parser.printUsage(System.out);
    }
}
