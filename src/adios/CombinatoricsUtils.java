package adios;


public class CombinatoricsUtils {
	public static double binom(int n, int k, double p) {
		return Math.exp(fac(n) - fac(k) - fac(n - k) + k*Math.log(p) + (n-k)*Math.log(1-p));
	}
	
	public static double fac(int n) {
		double log = 0;
		for (int i = 1; i <= n; i++)
			log += Math.log(i);
		return Math.exp(log);
	}
}
