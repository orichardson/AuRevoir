package adios;

import java.util.ArrayList;
import java.util.HashMap;

public class LexGraph {
	ArrayList<SearchPath> paths;
	HashMap<String, LexNode> nodes;
	
	public double eta = 0.1;
	

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
		ArrayList<LexNode> subpath = new ArrayList<LexNode>();
		double sigSoFar = 0;
		int ii = 0; int jj = 0;
		for (int i = 0; i < path.size(); i++) {
			for (int j = i + 1; j < path.size(); j++) {
				double D_L = D(path, i, j, true);
				double D_R = D(path, i, j, false);
				double temp = significance(D_L, D_R, path);
				if (sigSoFar <= temp) { //TODO: May want this to be ">", see comments in significance(...) method.
					sigSoFar = temp;
					ii = i; jj = j;
				}	
			}
		}
		
		//Maintains a reference to the original LexNode. Otherwise,
		//I remember that issues occur.
		for (int i = ii; i <= jj; i++) 
			subpath.add(path.get(i));
		
		return new LexNode.Pattern(subpath);
	}
	
	/*
	 * handles the MEX Criterion
	 * 
	 * TODO: Determine what the sample is from. The wording in the original paper
	 * strongly suggests that the sample is from all possible subpaths of the search path:
	 * 
	 * "choose _out of all segments_ the leading significant pattern P for the search path."
	 * 
	 * and in the description of Mode B for rewiring a graph:
	 * 
	 * "replace the string of vertices comprising P with the new vertex P only on
	 * those paths on which P is significant according to the MEX criterion."
	 * 
	 * TODO: fill in. We need to calculate p-values associated to the hypotheses: 
	 * D_R < eta, D_L < eta. The p-values are required to be, on average, 
	 * smaller than a threshold alpha << 1. 
	 * 
	 * Does this mean: return average of the two p-values, and define significance as the one
	 * having the lowest p-value?
	 */
	public double significance(double D_R, double D_L, SearchPath p) {
		return 0;
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
		//
		//
		//
		//	3d is handled, call method rewire
	}
}

