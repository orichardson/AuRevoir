package adios;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.inference.TTest;

public class LexGraph {
	ArrayList<SearchPath> paths;
	HashMap<String, LexNode> nodes;
	
	public double eta = 0.1;
	public double L = 3;
	

	public LexGraph(String corpus) {
		nodes = new HashMap<String, LexNode>();
		//Take a string, split it up by periods (FIX LATER) and add to paths.
		ArrayList<SearchPath> paths = new ArrayList<SearchPath>();
		String[] sentences = corpus.split("\\.");
		for (String sentence : sentences)
			paths.add(SearchPath.fromSentence(this, sentence));
		this.paths = paths;
		//Extract all unique nodes out of the paths. 
	}
	
	/*
	 * fraction of paths through all but one of the vertices that also
	 * go through the remaining vertex --
	 * 
	 * if end == true, then we're looking at the end
	 * if end == false, we're looking at the beginning
	 */
	public double P(SearchPath sp, int i, int j, boolean end) {
		sp.remove(".");
		int completeMatch = 0;
		int partialMatch = 0;

		int temp = Math.min(i, j);
		j = Math.max(i, j);
		i = temp;

		for (SearchPath vertices : sp.expandAll()) {
			for (SearchPath p : paths) {
				int currentIndex = end ? i : i + 1;
				// start at 1 if we might not care about the beginning node

				for (int k = 0; k < p.size(); k++) {
					if (p.get(k) == vertices.get(currentIndex)) { // another match
						currentIndex++;

						if (end) {
							if (currentIndex == j - 1)
								partialMatch++;
							if (currentIndex == j)
								completeMatch++;
						} else {
							if (currentIndex == j) {
								partialMatch++;

								if (p.get(k - j + i) == vertices.get(i))
									completeMatch++;
							}
						}
					}
					else {
						currentIndex = end ? i : i + 1;
					}
				}
			}
		}

		return partialMatch == 0 ? 0 : completeMatch / (double) partialMatch;
	}

	public double D(SearchPath vertices, int i, int j, boolean end) {
		int diff = end ? 1 : 0;
		return P(vertices, i - (diff - 1), j + diff, end)
				/ P(vertices, i, j, end);
	}
	
	/*
	 * 2a: find the leading significant pattern.
	 */
	public LexNode.Pattern findBestPattern(SearchPath path) {
		HashMap<OPair<Integer, Integer>, OPair<Double, Double>> sample = new HashMap<OPair<Integer, Integer>, OPair<Double, Double>>();
		int ii = 0; int jj = 0;
		for (int i = 0; i < path.size(); i++) {
			for (int j = i + 1; j < path.size(); j++) {
				sample.put(new OPair(i, j), new OPair(D(path, i, j, true), D(path, i, j, false)));
			}
		}
		
		return significance(sample, path);
	}
	
	/*
	 * handles the MEX Criterion
	 * 
	 * Given a D_R and D_L, we first check if both are less than eta. If so, 
	 * we calculate the t-scores: 
	 * 
	 * t = (avg_R - D_R)/stdev_R
	 * 
	 * and similarly for L, where the sample is taken to be all n choose 2 subpaths
	 * of the SearchPath p
	 * 
	 */
	public LexNode.Pattern significance(HashMap<OPair<Integer, Integer>, OPair<Double, Double>> sample, SearchPath path) {
		
		ArrayList<LexNode> subpath = new ArrayList<LexNode>();
		int ii = 0; int jj = 0;	double p_avg = 1;
		double rootn = Math.sqrt((path.size()*(path.size()-1))/2);
		TDistribution t = new TDistribution(rootn*rootn - 1);

		double avg_L = mean(sample, path, true);
		double avg_R = mean(sample, path, false);
		double stdev_L = stdev(sample, path, true, avg_L)/rootn;
		double stdev_R = stdev(sample, path, false, avg_R)/rootn;
	
		for (int i = 0; i < path.size(); i++) {
			for (int j = i + 1; j < path.size(); j++) {
				OPair<Double, Double> D = sample.get(new OPair<Integer, Integer>(i, j));
				if (D.l >= eta && D.r >= eta)
					return null;
				
				double temp = 1 - 0.5*(t.cumulativeProbability((avg_L - D.l)/stdev_L) + t.cumulativeProbability((avg_R - D.r)/stdev_R));
				if (temp < p_avg)
				{
					p_avg = temp; ii = i; jj = j;
				}
			}
		}
		
		for (int i = ii; i <= jj; i++) 
			subpath.add(path.get(i));
		
		return new LexNode.Pattern(subpath);
	}
	
	public double mean(HashMap<OPair<Integer, Integer>, OPair<Double, Double>> sample, SearchPath path, boolean left) {
		double x_bar = 0;
		for (int i = 0; i < path.size(); i++)
			for (int j = i + 1; j < path.size(); j++) {
				if (left)
					x_bar += sample.get(new OPair<Integer, Integer>(i, j)).l;
				else
					x_bar += sample.get(new OPair<Integer, Integer>(i, j)).r;
			}
		return x_bar/(0.5*path.size()*(path.size()-1) - 1);
	}
	
	public double stdev(HashMap<OPair<Integer, Integer>, OPair<Double, Double>> sample, SearchPath path, boolean left, double mean) {
		double dev = 0;
		for (int i = 0; i < path.size(); i++)
			for (int j = i + 1; j < path.size(); j++) {
				if (left)
					dev += (sample.get(new OPair<Integer, Integer>(i, j)).l - mean)*(sample.get(new OPair<Integer, Integer>(i, j)).l - mean);
				else
					dev += (sample.get(new OPair<Integer, Integer>(i, j)).r - mean)*(sample.get(new OPair<Integer, Integer>(i, j)).r - mean);
			}
		return dev*2/(path.size()*(path.size() - 1));
	}

	/*
	 * 2b: rewires the graph
	 * 
	 * contfree provides the option for Mode A (true) and Mode B (false)
	 */
	public void rewire(LexNode.Pattern P, SearchPath path, int i, int j, boolean contfree) {
		//TODO: implement the following
		//Mode A:
		//for each path p in paths
		//  if path[i, j] matches a portion of p,
		//	  remove that portion of p, inserting the LexNode.Pattern P instead
		//    TODO: add method that removes all nodes and inserts one 
		//		--call ArrayList's remove repeatedly, then the add (at index) method
		//
		//Mode B: 
		//for each path p in paths
		//  if path[i, j] matches a portion of p, and that portion is significant, 
		//  TODO: what is meant by significant? most significant?
		//	  remove that portion of P, inserting the LexNode.Pattern P instead
		//
		//What to do about the HashMap nodes? Nodes are no longer defined by just strings.
	}
	
	
	public void reduce() { // ADIOS ALGORITHM
		//Handle step 1 in separate method (constructor?)
		//for each path:
		//	2a is handled, call method findBestPattern
		//	2b is handled, call method rewire
		
		//for each path:
		//  for i = 0.. p.length - L - 1
		//    for j = i + 1 .. i + L - 2
		//		create generalized search path which is all paths that only differ at j
		// 
		//
		//
		//	3d is handled, call method rewire
	}
}

