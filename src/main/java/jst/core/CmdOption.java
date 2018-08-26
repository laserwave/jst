package jst.core;

import org.kohsuke.args4j.Option;

public class CmdOption {

    @Option(name="-est", usage="do estimation only")
    public boolean est = false;

    @Option(name="-inf", usage="do inference only")
    public boolean inf = false;

    @Option(name="-pipe", usage="do estimation and inference")
    public boolean pipe = false;

    @Option(name="-dir", usage="root directory of train/test corpus")
    public String dir = "";

    @Option(name="-model_dir", usage="root directory of model")
    public String model_dir = "";

    @Option(name="-lexicon", usage="sentiment lexicon")
    public String lexicon = "";

    @Option(name="-stopwords", usage="stopwords")
    public String stopwords = "";


    @Option(name="-alpha", usage="Specify alpha")
    public double alpha = -1.0;

    @Option(name="-beta", usage="Specify beta")
    public double beta = -1.0;

    @Option(name="-gamma", usage="Specify gamma")
    public double gamma = -1.0;

    @Option(name="-S", usage="Specify the number of sentiments")
    public int S = 6;

    @Option(name="-T", usage="Specify the number of topics")
    public int T = 10;


    @Option(name="-trainiters", usage="training iterations")
    public int train_iters = 1000;

    @Option(name="-testiters", usage="testing iterations")
    public int test_iters = 50;

    /**
     * The options that are less frequently set
     */
    @Option(name="-nchains", usage="Specify the number of chains(pipe)")
    public int nchains = 1;

    @Option(name="-train", usage="Specify file name of train corpus")
    public String train_corpus = "train.txt";

    @Option(name="-test", usage="Specify file name of test corpus")
    public String test_corpus = "test.txt";

    @Option(name="-topwords", usage="most likely (top) words to be printed")
    public int topwords = 20;

    @Option(name="-savestep", usage="number of steps to save the model")
    public int savestep = 200;

    @Option(name="-name", usage="Specify the model name")
    public String model_name = "model-1";

}
