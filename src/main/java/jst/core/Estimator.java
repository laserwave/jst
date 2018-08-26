package jst.core;


import jst.utils.Utils;

import java.io.File;

public class Estimator {
    public Model trModel;
    public CmdOption option;
    public RunTime runTime;

    public boolean init(CmdOption option) {
        this.option = option;
        runTime = new RunTime();
        trModel = new Model();

        if (!trModel.initNewModel(option)) {
            return false;
        }

        trModel.incorporatePriorToBeta();
        trModel.incorporatePriorToGamma();
        return true;
    }

    public void estimate() {
        runTime.setStartTime(System.currentTimeMillis());
        runTime.setIter(trModel.train_iters);
        System.out.println("Sampling " + trModel.train_iters + " training iterations!");

        for (int currentIter = 1; currentIter <= trModel.train_iters; currentIter++) {
            System.out.println("Iteration " + currentIter + "...");

            for (int d = 0; d < trModel.D; d++) {

                for (int n = 0; n < trModel.corpus.docs[d].length; n++){
                    HiddenVariable hv = szSampling(d, n);
                    trModel.sAssign[d][n] = hv.sentiment;
                    trModel.zAssign[d][n] = hv.topic;
                }
            } // end for each document

            if (option.savestep > 0) {
                if ((currentIter % option.savestep == 0) && (currentIter != trModel.train_iters)) {
                    System.out.println("Saving the model at iteration " + currentIter + "...");
                    computePi();
                    computeTheta();
                    computePhi();
                    trModel.computePerplexity();
                    trModel.evaluateSentiment();
                    trModel.saveModel(trModel.modelName + "-" + Utils.zeroPad(currentIter, 4), null);
                    trModel.corpus.localDict.writeWordMap(option.model_dir + File.separator + trModel.modelName + "-" + Utils.zeroPad(currentIter, 4) + Model.wordMapSuffix);

                }
            }
        } // end iterations
        runTime.setEndTime(System.currentTimeMillis());
        System.out.println("Saving the final model");
        computePi();
        computeTheta();
        computePhi();
        trModel.computePerplexity();
        trModel.evaluateSentiment();
        trModel.saveModel(trModel.modelName + "-final", runTime);
        trModel.corpus.localDict.writeWordMap(option.model_dir + File.separator + trModel.modelName + "-final" + Model.wordMapSuffix);

        System.out.println("Gibbs sampling completed!\n");
    }


    public HiddenVariable szSampling(int d, int n){
        int topic = trModel.zAssign[d][n];
        int sentiment = trModel.sAssign[d][n];
        int word = trModel.corpus.docs[d].words[n];
        trModel.nszw[sentiment][topic][word] -= 1;
        trModel.nsz[sentiment][topic] -= 1;
        trModel.ndsz[d][sentiment][topic] -= 1;
        trModel.nds[d][sentiment] -= 1;
        trModel.nd[d] -= 1;

        /**
         * do multinominal sampling via cumulative method
         * log probabilities are used in order to avoid that probabilities undergo underflow(approximated to 0)
         */
        double[][] logP = new double[trModel.S][trModel.T];
        // maxLogP will be used to normalize the probabilities
        double maxLogP = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < trModel.S; i++){
            for (int j = 0; j < trModel.T; j++){
                logP[i][j] = Math.log(trModel.nszw[i][j][word] + trModel.beta_szw[i][j][word]) - Math.log(trModel.nsz[i][j] + trModel.beta_szwsum[i][j]);
                logP[i][j] += Math.log(trModel.ndsz[d][i][j] + trModel.alpha) - Math.log(trModel.nds[d][i] + trModel.alpha * trModel.T);
//                logP[i][j] += Math.log(trModel.nds[d][i] + trModel.gamma_ds[d][i]) - Math.log(trModel.nd[d] + trModel.gamma_dssum[d]);
                /**
                 * Fix PI FIXME
                 */
                logP[i][j] += Math.log(trModel.gamma_ds[d][i]) - Math.log(trModel.gamma_dssum[d]);
                if (logP[i][j] > maxLogP) {
                    maxLogP = logP[i][j];
                }
            }
        }
        // normalize probabilities
        for (int i = 0; i < trModel.S; i++){
            for (int j = 0; j < trModel.T; j++){
                trModel.psz[i][j] = Math.exp(logP[i][j]-maxLogP);
            }
        }

        // cumulate multinomial parameters
        for (int i = 0; i < trModel.S; i++){
            for (int j = 0; j < trModel.T; j++){
                if (j == 0){
                    if (i == 0){
                        continue;
                    }
                    else{
                        trModel.psz[i][j] += trModel.psz[i-1][trModel.T-1];
                    }
                }else{
                    trModel.psz[i][j] += trModel.psz[i][j-1];
                }
            }
        }

        // scaled sample
        double u = Math.random() * trModel.psz[trModel.S-1][trModel.T-1];
        boolean loopBreak = false;

        for (sentiment = 0; sentiment < trModel.S; sentiment++) {
            for (topic = 0; topic < trModel.T; topic++) {
                if(trModel.psz[sentiment][topic] > u){
                    loopBreak = true;
                    break;
                }
            }
            if(loopBreak){
                break;
            }
        }
        sentiment = Math.min(sentiment, trModel.S-1);
        topic = Math.min(topic, trModel.T-1);

        trModel.nszw[sentiment][topic][word] += 1;
        trModel.nsz[sentiment][topic] += 1;
        trModel.ndsz[d][sentiment][topic] += 1;
        trModel.nds[d][sentiment] += 1;
        trModel.nd[d] += 1;

        return new HiddenVariable(sentiment, topic);
    }

    public void computeTheta() {

        for (int i = 0; i < trModel.D; i++){
            for (int j = 0; j < trModel.S; j++){
                for (int k = 0; k < trModel.T; k++){
                    trModel.theta[i][j][k] = (trModel.ndsz[i][j][k] + trModel.alpha) / (trModel.nds[i][j] + trModel.T * trModel.alpha);
                }
            }
        }
    }

    public void computePi() {

        for (int i = 0; i < trModel.D; i++) {
            for (int j = 0; j < trModel.S; j++) {
                trModel.pi[i][j] = (trModel.nds[i][j] + trModel.gamma_ds[i][j]) / (trModel.nd[i] + trModel.gamma_dssum[i]);
            }
        }
    }

    public void computePhi() {
        for (int i = 0; i < trModel.S; i++) {
            for (int j = 0; j < trModel.T; j++) {
                for (int k = 0; k < trModel.V; k++) {
                    trModel.phi[i][j][k] = (trModel.nszw[i][j][k] + trModel.beta_szw[i][j][k]) / (trModel.nsz[i][j] + trModel.beta_szwsum[i][j]);
                }
            }
        }
    }


}
