package jst.core;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

public class Lexicon {

    public static Map<String, double[]> lexicon = new HashMap<String, double[]>();

    public static Set<String> stopwords = new HashSet<String>();


    public static int S;

    public static String[] sentimentNames;

    public static boolean loadLexicon = false;

    public static void loadSentimentLexicon(String filename){
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
            String line = reader.readLine();
            StringTokenizer tknr = new StringTokenizer(line, " ");
            S = Integer.parseInt(tknr.nextToken());
            if(tknr.countTokens() != S){
                throw(new Exception("sentiment number S error"));
            }
            sentimentNames = new String[S];
            for(int i = 0; i < S; i++){
                sentimentNames[i] = tknr.nextToken();
            }

            String word = null;
            double[] distribution;

            while((line = reader.readLine())!=null){
                tknr = new StringTokenizer(line, " ");
                assert(tknr.countTokens() == S+1);
                if(tknr.countTokens() != S+1){
                    throw(new Exception("sentiment lexicon format error"));
                }
                distribution = new double[S];
                word = tknr.nextToken();
                for(int i = 0; i < S; i++){
                    distribution[i] = Double.parseDouble(tknr.nextToken());
                }
                lexicon.put(word, distribution);
            }

            reader.close();
            loadLexicon = true;

        } catch (Exception e) {
            System.out.println("fail load sentiment lexicon: " + e.getMessage());
            e.printStackTrace();
            loadLexicon = false;
        }
    }

    public static void loadStopwords(String filename){
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
            String line;
            while((line = reader.readLine())!=null){
                stopwords.add(line);
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();

        }
    }


}
