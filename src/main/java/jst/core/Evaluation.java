package jst.core;

import jst.utils.Utils;

import java.util.List;

public class Evaluation {

    private static boolean check(double[] g, double[] p){
        return (g.length > 0 && p.length > 0 && g.length == p.length);
    }

    /**
     * calculate acc@1, acc@2, acc@3
     * acc@k means the label with the highest prob in p is ranking top k in g
     * @param g groundtruth distribution
     * @param p predicted distribution
     * @return an array [acc@1, acc@2, acc@3]
     */
    public static int[] acc(double[] g, double[] p){
        if(!check(g, p)){
            try{
                throw new Exception("check(g, p) when calculate acc@1, acc@2, acc@3");
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        int acc1 = 0, acc2 = 0, acc3 = 0;
        List<Integer> predict = Utils.argmax(p);
        for(int index:predict){
            int rank = Utils.getRank(g, index);
            if(rank > 3) continue;
            acc3 = 1;
            if(rank > 2) continue;
            acc2 = 1;
            if(rank > 1) continue;;
            acc1 = 1;
            break;
        }
        return new int[]{acc1, acc2, acc3};
    }

    /**
     * calculate pearson correlation coefficient
     * if each element in g or p is the same, there will be a 0/0 NaN
     * @param g groundtruth distribution
     * @param p predicted distribution
     * @return r
     */
    public static double pearson_coefficient(double[] g, double[] p) {
        if(!check(g, p)){
            try{
                throw new Exception("check(g, p) when calculate pearson_coefficient");
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        int n = g.length;
        double m = 1.0/n;
        double numerator = 0.0, denominator1 = 0.0, denominator2 = 0.0;
        for(int i = 0; i < n; i++){
            numerator += (g[i]-m)*(p[i]-m);
            denominator1 += (g[i]-m)*(g[i]-m);
            denominator2 += (p[i]-m)*(p[i]-m);
        }
        double r = numerator / (Math.sqrt(denominator1) * Math.sqrt(denominator2));

        if(Double.isNaN(r)){
            g[0] += 0.00000001;
            p[0] += 0.00000001;
            Utils.normalize(g);
            Utils.normalize(p);
            numerator = denominator1 = denominator2 = 0.0;
            for(int i = 0; i < n; i++){
                numerator += (g[i]-m)*(p[i]-m);
                denominator1 += (g[i]-m)*(g[i]-m);
                denominator2 += (p[i]-m)*(p[i]-m);
            }
            r = numerator / (Math.sqrt(denominator1) * Math.sqrt(denominator2));
        }

        return Double.isNaN(r)?r:Math.max(Math.min(r, 1.0), -1.0);
    }

}
