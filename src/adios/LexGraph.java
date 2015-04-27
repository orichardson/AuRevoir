
package adios;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class LexGraph {

	HashMap<String, LexNode> nodes;

	ArrayList<SearchPath> paths;
	ArrayList<LexNode.Equivalence> eclasses;

	double eta = 0.65;
	double alpha = 0.001;
	int L = 3;
	double omega = 0.65;

	public LexGraph(String corpus) {
		nodes = new HashMap<String, LexNode>();

		// Take a string, split it up by periods (FIX LATER) and add to paths.
		ArrayList<SearchPath> paths = new ArrayList<SearchPath>();
		String[] sentences = corpus.split("\\.");

		for (String sentence : sentences)
			paths.add(SearchPath.fromSentence(this, sentence));

		this.paths = paths;
		// Extract all unique nodes out of the paths.
	}

	private int l(SearchPath sp, int i, int j) {
		int match = 0;

		for (SearchPath vertices : sp.expandAll()) {
			for (SearchPath p : paths) {
				int currentIndex = i;

				for (int k = 0; k < p.size(); k++) {
					// Will comparing addresses for equivalence classes and patterns be enough?
					if (p.get(k) == vertices.get(currentIndex)) {
						currentIndex++;
						if (currentIndex == j) {
							match++;
							currentIndex = i; //TODO: should this be removed? Currently, it will keep searching the SearchPath.
						}
					} else
						currentIndex = i;
				}
			}
		}

		return match;
	}

	public double P(SearchPath sp, int i, int j, boolean forward) {
		return forward ? l(sp, i, j) / l(sp, i, j - 1) : l(sp, i, j) / l(sp, i + 1, j);
	}

	public double D(SearchPath sp, int i, int j, boolean forward) {
		return forward ? P(sp, i, j, forward) / P(sp, i, j - 1, forward) : P(sp, i, j, !forward)
				/ P(sp, i + 1, j, !forward);
	}

	// Find the leading significant pattern.
	public LexNode.Pattern extractSignificantPattern(SearchPath sp) {
		double min = 1;
		LexNode.Pattern bestPattern = null;
		for (int i = 0; i < sp.size(); i++)
			for (int j = i + 1; j < i; j++) {//TODO: for some reason, j was set to start at i. I changed this to i + 1.
				//TODO: Create a candidate pattern. Maybe this is unnecessary, and one should just have
				//an index free significance method and an index dependent one?
				LexNode.Pattern candidatePattern = new LexNode.Pattern(sp.copy(i, j + 1));
				double sig = significance(sp, candidatePattern);
				
				//TODO: Worry: Shouldn't significance be GREATER than some threshold alpha?
				//TODO: Reply: No, the significance is measured by the p-value of the hypothesis test,
				//which, as normal, needs to be less than alpha.
				if (bestPattern == null || (sig < alpha && sig <= min)) {
					bestPattern = candidatePattern;
					min = sig;
				}
			}

		return bestPattern;
	}

	// Do we need a compute backwards (left) significance method?
	public double significance(SearchPath sp, LexNode.Pattern P) {
		double sum = 0;
		
		//TODO: created an index free version of this method.
		SearchPath subpath = new SearchPath(); 
		subpath.add(P);
		int i = sp.match(subpath);
		int j = i + subpath.size();
		
		for (int x = 0; x <= l(sp, i, j); x++) {
			sum += CombinatoricsUtils.binom(l(sp, i, j - 1), x, eta * P(sp, i, j - 1, true));
		}
		return Math.min(Math.max(sum, 0.0), 1.0);
	}

	public void rewire(LexNode.Pattern P, boolean a) {
		//create new vertex corresponding to pattern
		nodes.put(P.name,P);
		
		//TODO: I created a subpath out of P to be passed into SearchPath's match method.
		SearchPath subpath = new SearchPath();
		subpath.add(P);
		// Replace, in every path, the portion that matches with P
		for (SearchPath path : paths) {
			int ii = path.match(subpath);
			
			if (ii > 0 && (a || significance(path, P) < alpha))
				path.replace(P, ii, ii + subpath.size()); 
				//removal is exclusive on the right endpoint, hence ii + subpath.size() rather than
				//ii + subpath.size() - 1
		}
	}

	public void pattern_distillation(boolean a) {
		for (SearchPath p : paths) {
			LexNode.Pattern temp = extractSignificantPattern(p); // 2a
			rewire(temp, a); // 2b
		}
	}

	public void generalization_first(boolean a) {
		eclasses = new ArrayList<LexNode.Equivalence>();
		LexNode.Pattern mostSignificantPattern = null;
		LexNode.Equivalence mostSignificantEquivalence = null;
		for (SearchPath p : paths) {
			// these two loops handle 3a and 3b
			for (int i = 0; i < p.size() - L - 1; i++) {
				for (int j = i + 1; j <= i + L - 2; j++) {
					SearchPath pc = p.copy(); //Don't want to alter original SearchPath.
					LexNode.Equivalence e = equiv(p, i, j);
					pc.replace(e, j, j + 1); //Replacement is exclusive on the right endpoint. 
					
					LexNode.Pattern candidatePattern = extractSignificantPattern(pc); // candidate pattern from the SearchPath. 

					if (mostSignificantPattern == null || (significance(p, candidatePattern) > significance(p, mostSignificantPattern))) {
						mostSignificantPattern = candidatePattern;
						mostSignificantEquivalence = e; //Want to store the equivalence class associated with the pattern.
					}
				}
			}
			// handles 3c
			eclasses.add(mostSignificantEquivalence); 
			// handles 3d
			rewire(mostSignificantPattern, a);
			mostSignificantPattern = null; // reset mostSignificantPattern. TODO: This *probably* won't result in null pointers later on.
			mostSignificantEquivalence = null;
		}
	}
	
	//Returns an equivalence class at index j, where the path is known to start at i
	//and go to i + L - 1
	public LexNode.Equivalence equiv(SearchPath sp, int i, int j) {
		LexNode.Equivalence toReturn = new LexNode.Equivalence();
		toReturn.pieces.add(sp.get(j));
		for (SearchPath p : paths) {
			SearchPath left = sp.copy(i, j); //subpath from i (inclusive) to j - 1 (inclusive)
			SearchPath right = sp.copy(j + 1, i + L); //subpath from j + 1 (inclusive) to i + L - 1 (inclusive)
			int leftMatch = p.match(left); //this is stored so that the node in p can be retrieved. 
			if (leftMatch > 0 && p.match(right) > 0)
				toReturn.pieces.add(p.get(leftMatch + j - i - 1)); //TODO: the j - i - 1 brings us to the corresponding node in p
		}
		return toReturn;
	}

	public boolean generalization_bootstrap(boolean a) {
		for (SearchPath p : paths) {
			SearchPath pc = p.copy();
			LexNode.Pattern bestPattern = null;
			LexNode.Equivalence bestEquivalence = null;
			double bestOverlap = 0;
			for (int i = 0; i < p.size() - L - 1; i++) {
				for (int j = i + 1; j <= i + L - 2; j++) {
					//4a, i: Finds all subpaths of paths that match the subpath of p at the endpoints i and K - L - 2. Remember the paper indexes by 1. 
					ArrayList<SearchPath> matches = endpoint_match(p.copy(i, p.size() - L - 1)); //Remember right endpoint is excluded.
					//4a, ii: Selects the equivalence class with the largest overlap with encountered vertices at spot j. 
					LexNode.Equivalence e = compare(p, matches, i, j);
					pc.replace(e, j, j + 1); //Construct the generalized search path. 
				}

				for (int k = i + 1; k <= i + L - 2; k++) {
					for (int j = i + 1; j <= i + L - 2; j++) {
						if (j == k)
							continue;
						//Consider all paths going through all vertices in (vertex k (an equivalence class) intersect vertex j (an equivalence class))
						//TODO: intersect over all j or just one?
						LexNode.Equivalence vertex_k = (LexNode.Equivalence) pc.get(k); 
						LexNode.Equivalence vertex_j = (LexNode.Equivalence) pc.get(j);
						vertex_k.pieces.retainAll(vertex_j.pieces); 
						//Perform MEX on this generalized path
						LexNode.Pattern candidatePattern = extractSignificantPattern(pc);
						
						//Extract the leading pattern P, as well as equivalence class associated with P, and the overlap of that class.
						if (bestPattern == null || (significance(pc, candidatePattern) < significance(pc, bestPattern))) { //Less than because significance is measured in p-values.
							bestPattern = candidatePattern;
							bestEquivalence = vertex_k;
							bestOverlap = vertex_k.pieces.size()/vertex_j.pieces.size();
						}

					}
				}
			}
			
			//If the overlap is < 1, define a new equivalence class consisting of only "those members that did appear in the set"
			//I take this to mean the members in the intersection of vertex_k and vertex_j above, since it would be redundant
			//to add the equivalence class at vertex_j.
			if (bestOverlap < 1)
				eclasses.add(bestEquivalence);
			
			//4d
			rewire(bestPattern, a);
			bestPattern = null; bestEquivalence = null; bestOverlap = 0;
		}
		
		return false; 
		//TODO: How to repeat this method until no further significant patterns are found?
		//Currently, we will be forever adding new patterns due to the bestPattern == null condition.
	}
	
	//TODO: usually, returns an equivalence class. Sometimes the equivalence class
	//may only consist of one LexNode, so the return is a LexNode. 
	//TODO: I don't see a way around mentioning the indices.
	public LexNode.Equivalence compare(SearchPath p, ArrayList<SearchPath> matches, int i, int j) {
		HashSet<LexNode> encountered = new HashSet<LexNode>();
		for (SearchPath path : matches) {
			encountered.add(path.get(j - i)); //Since matches consist of subpaths, we don't need the shift that the OPair lets us store.
		}

		double intersect = 0;
		LexNode.Equivalence best = null;

		for (LexNode.Equivalence e : eclasses) {
			HashSet<LexNode> eclass = new HashSet<LexNode>(e.pieces);
			double size = encountered.size();
			encountered.retainAll(eclass);
			if (encountered.size() / size > intersect && encountered.size() / size > omega)
				best = e;
		}
		
		LexNode.Equivalence toReturn = new LexNode.Equivalence();
		if (best == null) {
			toReturn.pieces.add(p.get(j));
			return toReturn;
		} else
			return best;
	}

	// Handles step 4, a, i
	public ArrayList<SearchPath> endpoint_match(SearchPath subpath) {
		ArrayList<SearchPath> toReturn = new ArrayList<SearchPath>();
		for (SearchPath p : paths) {
			for (int start = 0; start < p.size(); start++) {
				boolean match = false;
				int end = start + subpath.size() - 1;
				try {
					match = (p.get(start) == subpath.get(0))
							&& (p.get(end) == subpath.get(subpath.size() - 1));
				} catch (Exception e) { continue; }
				
				if (match)
					toReturn.add(p.copy(start, end + 1)); //end + 1 since copy is exclusive on the right endpoint.
			}
		}
		return toReturn;
	}
}
