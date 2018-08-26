package jst.core;

import java.io.File;

public class Inference {

    public Model oldModel; // train model
    public Dictionary globalDict;
    private CmdOption option;
    private Model newModel; // test model
    public RunTime runTime;


    public boolean init(CmdOption option) {
        this.option = option;
        runTime = new RunTime();
        oldModel = new Model();

        if (!oldModel.initEstimatedModel(option))
            return false;

        globalDict = oldModel.corpus.localDict;
        newModel = new Model();
        if (!newModel.initNewModel(option, oldModel))
            return false;
        newModel.incorporatePriorToBeta();
        return true;
    }

    public boolean init(CmdOption option, Model oldModel) {
        this.option = option;
        runTime = new RunTime();
        this.oldModel = oldModel;
        globalDict = oldModel.corpus.localDict;
        newModel = new Model();
        if (!newModel.initNewModel(option, oldModel))
            return false;
        newModel.incorporatePriorToBeta();
        return true;
    }


    /**
     * Infer new model using data from a corpus specified in the option.
     */
    public void infer() {
        runTime.setStartTime(System.currentTimeMillis());
        runTime.setIter(newModel.test_iters);
        System.out.println("Sampling " + newModel.test_iters + " testing iterations!");

        for (int currentIter = 1; currentIter <= newModel.test_iters; currentIter++) {
            System.out.println("Iteration " + currentIter + "...");

            for (int d = 0; d < newModel.D; d++) {

                for (int n = 0; n < newModel.corpus.docs[d].length; n++){
                    HiddenVariable hv = infSZSampling(d, n);
                    newModel.sAssign[d][n] = hv.sentiment;
                    newModel.zAssign[d][n] = hv.topic;
                }
            } // end for each document
        } // end iterations
        runTime.setEndTime(System.currentTimeMillis());
        System.out.println("Gibbs sampling for inference completed!");
        System.out.println("Saving the inference outputs!");

        computeNewPi();
        computeNewTheta();
        computeNewPhi();
        newModel.computePerplexity();
        newModel.evaluateSentiment();
        newModel.saveModel(oldModel.modelName + "-inference", runTime);
        newModel.corpus.localDict.writeWordMap(option.model_dir + File.separator + oldModel.modelName + "-inference" + Model.wordMapSuffix);

    }


    public HiddenVariable infSZSampling(int d, int n){
        int topic = newModel.zAssign[d][n];
        int sentiment = newModel.sAssign[d][n];
        int w = newModel.corpus.docs[d].words[n]; // word's index in test model
        int _w = newModel.corpus.lid2gid.get(w);  // word's index in train model
        newModel.nszw[sentiment][topic][w] -= 1;
        newModel.nsz[sentiment][topic] -= 1;
        newModel.ndsz[d][sentiment][topic] -= 1;
        newModel.nds[d][sentiment] -= 1;
        newModel.nd[d] -= 1;

        double[][] logP = new double[newModel.S][newModel.T];
        double maxLogP = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < newModel.S; i++){
            for (int j = 0; j < newModel.T; j++){
                logP[i][j] = Math.log(oldModel.nszw[i][j][_w] + newModel.nszw[i][j][w] + newModel.beta_szw[i][j][w]) - Math.log(oldModel.nsz[i][j] + newModel.nsz[i][j] + newModel.beta_szwsum[i][j]);
                logP[i][j] += Math.log(newModel.ndsz[d][i][j] + newModel.alpha) - Math.log(newModel.nds[d][i] + newModel.alpha * newModel.T);
                logP[i][j] += Math.log(newModel.nds[d][i] + newModel.gamma) - Math.log(newModel.nd[d] + newModel.gamma * newModel.S);
                if (logP[i][j] > maxLogP) {
                    maxLogP = logP[i][j];
                }
            }
        }
        // normalize probabilities
        for (int i = 0; i < newModel.S; i++){
            for (int j = 0; j < newModel.T; j++){
                newModel.psz[i][j] = Math.exp(logP[i][j]-maxLogP);
            }
        }

        // cumulate multinomial parameters
        for (int i = 0; i < newModel.S; i++){
            for (int j = 0; j < newModel.T; j++){
                if (j == 0){
                    if (i == 0){
                        continue;
                    }
                    else{
                        newModel.psz[i][j] += newModel.psz[i-1][newModel.T-1];
                    }
                }else{
                    newModel.psz[i][j] += newModel.psz[i][j-1];
                }
            }
        }

        // scaled sample
        double u = Math.random() * newModel.psz[newModel.S-1][newModel.T-1];
        boolean loopBreak = false;

        for (sentiment = 0; sentiment < newModel.S; sentiment++) {
            for (topic = 0; topic < newModel.T; topic++) {
                if(newModel.psz[sentiment][topic] > u){
                    loopBreak = true;
                    break;
                }
            }
            if(loopBreak){
                break;
            }
        }
        sentiment = Math.min(sentiment, newModel.S-1);
        topic = Math.min(topic, newModel.T-1);

        newModel.nszw[sentiment][topic][w] += 1;
        newModel.nsz[sentiment][topic] += 1;
        newModel.ndsz[d][sentiment][topic] += 1;
        newModel.nds[d][sentiment] += 1;
        newModel.nd[d] += 1;

        return new HiddenVariable(sentiment, topic);
    }


    public void computeNewTheta() {

        for (int i = 0; i < newModel.D; i++){
            for (int j = 0; j < newModel.S; j++){
                for (int k = 0; k < newModel.T; k++){
                    newModel.theta[i][j][k] = (newModel.ndsz[i][j][k] + newModel.alpha) / (newModel.nds[i][j] + newModel.T * newModel.alpha);
                }
            }
        }
    }

    public void computeNewPi() {

        for (int i = 0; i < newModel.D; i++) {
            for (int j = 0; j < newModel.S; j++) {
                newModel.pi[i][j] = (newModel.nds[i][j] + newModel.gamma) / (newModel.nd[i] + newModel.S * newModel.gamma);   // infer时不使用gamma_ds非对称超参
            }
        }
    }

    public void computeNewPhi() {
        for (int i = 0; i < newModel.S; i++) {
            for (int j = 0; j < newModel.T; j++) {
                for (int k = 0; k < newModel.V; k++) {
                    int _k = newModel.corpus.lid2gid.get(k);
                    newModel.phi[i][j][k] = (oldModel.nszw[i][j][_k] + newModel.nszw[i][j][k] + newModel.beta_szw[i][j][k]) / (oldModel.nsz[i][j] + newModel.nsz[i][j] + newModel.beta_szwsum[i][j]);
                }
            }
        }
    }

}
