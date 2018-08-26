

package jst.core;


import java.util.Vector;

public class Document {


    public int [] words;
    public int length;
	public String rawStr;
    public int [] votes;
    public double [] distribution;

	public Document() {
        this.words = null;
        this.rawStr = "";
        this.length = 0;
        this.votes = null;
        this.distribution = null;
	}

    public Document(int length, Vector<Integer> words, String rawStr, int[] votes, double[] distribution) {
        this.length = length;
        this.rawStr = rawStr;
        this.words = new int[length];

        for (int i = 0 ; i < length; ++i){
            this.words[i] = words.get(i);
        }
        this.votes = votes;
        this.distribution = distribution;
    }

}
