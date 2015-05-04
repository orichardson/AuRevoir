package adios;


public class CombinatoricsUtils {
	public static double binom(int n, int k, double p) {
		return Math.exp(logFac(n) - logFac(k) - logFac(n - k) + k*Math.log(p) + (n-k)*Math.log(1-p));
	}
	
	public static double logFac(int n) {
		double log = 0;
		for (int i = 1; i <= n; i++)
			log += Math.log(i);
		return log;
	}
}
