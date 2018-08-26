package jst.utils;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;


public class Utils {
	public static boolean createDir(String destDirName) {
		File dir = new File(destDirName);
		if (dir.exists()) {
			return false;
		}
		if (!destDirName.endsWith(File.separator)) {
			destDirName = destDirName + File.separator;
		}
		if (dir.mkdirs()) {
			System.out.println("create new directory successfully:" + destDirName);
			return true;
		} else {
			return false;
		}
	}
    public static void normalize(double d[]){
        double sum = 0.0;
        for(int i = 0; i < d.length; i++){
            sum += d[i];
        }
        for(int i = 0; i < d.length; i++){
            d[i] /= sum;
        }
    }

    /**
     *  return the index(s) whose value is(are) the maximum
     */
    public static List<Integer> argmax(double d[]){
        if(d.length<1){
            return null;
        }
        double max = d[0];
        List<Integer> res = new ArrayList<Integer>();
        res.add(0);
        for(int i = 1; i < d.length; i++){
            if(d[i] == max){
                res.add(i);
            }else if(d[i] > max){
                max = d[i];
                res = new ArrayList<Integer>();
                res.add(i);
            }
        }
        return res;
    }

    /**
     *  find the rank of the num in d with index
     */
    public static int getRank(double[] d, int index){
        if(index > d.length-1 || index < 0){
            return -1;
        }
        int numGreater = 0;
        for(int i = 0; i < d.length; i++){
            if(index == i) continue;
            if(d[i] > d[index]){
                numGreater += 1;
            }
        }
        return numGreater+1;
    }





	/**
	 * Pad an integer with zeroes to put it into the desired width.
	 * @param number the number to pad
	 * @param width the width of the padded number
	 * @return the string of the padded number
	 */
	public static String zeroPad(int number, int width) {
	      StringBuffer result = new StringBuffer("");
	      for (int i = 0; i < width - Integer.toString(number).length(); i++) {
	         result.append("0");
	      }
	      result.append(Integer.toString(number));
	     
	      return result.toString();
	}
	
	/**
	 * Compute the log of the sum of a array of doubles expressed in the log space.
	 * 
	 * @param y array of logs, such that y[i] = log(x[i])
	 * @return the double log(x[0] + ... + x[n-1]) (= yMax + log(exp(y[0] - yMax) + ... + exp(y[n-1] - yMax)))
	 */
	public static double logSum(double[] y) {
		double yMax = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < y.length; i++) {
			if (y[i] > yMax) {
				yMax = y[i];
			}
		}
		
		double logArg = 0;
		for (int i = 0; i < y.length; i++) {
			logArg += Math.exp(y[i] - yMax);
		}
		
		return yMax + Math.log(logArg);
	}

	public static double logSum(double[][] y) {
		double yMax = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < y.length; i++) {
			for (int j = 0; j < y[0].length; j++){
				if (y[i][j] > yMax) {
					yMax = y[i][j];
				}
			}
		}

		double logArg = 0;
		for (int i = 0; i < y.length; i++) {
			for (int j = 0; j < y[0].length; j++){
				logArg += Math.exp(y[i][j] - yMax);
			}
		}

		return yMax + Math.log(logArg);
	}
}
