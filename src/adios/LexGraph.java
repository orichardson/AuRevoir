package adios;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class LexGraph {

	HashMap<String, LexNode> nodes;
	HashMap<String, LexNode.Equivalence> eclasses;
	HashMap<String, LexNode.Pattern> pattern; 
	ArrayList<SearchPath> paths;

	double eta = 0.65;
	double alpha = 0.001;
	int L = 3;
	double omega = 0.65;

	/**
	* Actual ADIOS algorithm.
	*/
	public void adios(boolean a) {
		pattern_distillation(a);
		generalization_first(a);
		while (generalization_bootstrap(a)) {};
	}
	
	/**
	 * Constructor which handles step 1 of the ADIOS algorithm.
	 * @param corpus
	 */
	public LexGraph(ArrayList<String> sentences) {
		nodes = new HashMap<String, LexNode>();
		eclasses = new HashMap<String, LexNode.Equivalence>();
		// Take a string, split it up by periods (FIX LATER) and add to paths.
		ArrayList<SearchPath> paths = new ArrayList<SearchPath>();
		//String[] sentences = corpus.split("\\.");
		
		for (String sentence : sentences)
			paths.add(SearchPath.fromSentence(this, sentence));

		this.paths = paths;
		// Extract all unique nodes out of the paths.
	}
	
	/**
	 * Handles step 2 of the ADIOS algorithm, pattern distillation.
	 * @param a
	 */
	public void pattern_distillation(boolean a) {
		for (SearchPath p : paths) {
			LexNode.Pattern temp = extractSignificantPattern(p); //2a: find the leading significant pattern
			rewire(temp, a); //2b: rewire the graph
		}
	}
	
	/**
	 * Handles step 3 of the ADIOS algori)thm, generalization-first step.
	 * @param a
	 */
	public void generalization_first(boolean a) {
		//Need to find the mostSignificantPattern, and the mostSignificantEquivalence
		//associated with the mostSignificantPattern.
		LexNode.Pattern mostSignificantPattern = null;
		LexNode.Equivalence mostSignificantEquivalence = null;
		double mostSignificant = 1;
		for (SearchPath p : paths) {
			//These two loops handle 3a and 3b. 
			for (int i = 0; i < p.size() - L - 1; i++) {
				for (int j = i + 1; j <= i + L - 2; j++) {
					//i, j give the context window.
					SearchPath pc = p.copy(); // Don't want to alter original SearchPath.
					//3a: alter SearchPath pc so that it is the generalized search path
					//discussed in 3a, i-iii. Replaces p.get(j) with an equivalence class.
					LexNode.Equivalence e = equiv(p, i, j);
					pc.replace(e, j, j + 1); // Replacement is exclusive on the right endpoint.

					//3b: performs MEX on the generalized path, finding the most significant pattern
					//over the course of the loop (one for each SearchPath). 
					LexNode.Pattern candidatePattern = extractSignificantPattern(pc);
					double tempSig = 0.5 * (computeLeftSignificance(p, candidatePattern) + computeRightSignificance(p, candidatePattern));
					if (mostSignificantPattern == null || tempSig <= mostSignificant) {
						mostSignificantPattern = candidatePattern;
						mostSignificantEquivalence = e; 
						mostSignificant = tempSig;
					}
				}
			}
			//3c: We already created the equivalence class.
			//Add this to our list of equivalence classes.
			eclasses.put(mostSignificantEquivalence.name, mostSignificantEquivalence);
			//3d: rewire the graph, and reset the mostSignificant variables.
			rewire(mostSignificantPattern, a);
			mostSignificantPattern = null;
			mostSignificantEquivalence = null;
		}
	}
	
	/**
	 * Handles step 4 of the ADIOS algorithm, generalization-bootstrap.
	 * Returns a boolean so that we can perform step 5 (repeat step 4). 
	 * @param a
	 * @return
	 */
	public boolean generalization_bootstrap(boolean a) {
		//A boolean for if all patterns found are null. If they are, then we are done.
		boolean allNullPattern = true;

		for (SearchPath p : paths) {
			SearchPath pc = p.copy();
			//For each SearchPath p, we want to find the bestPattern and
			//equivalence class in that pattern. These variables are to assist
			// in that.
			LexNode.Pattern bestPattern = null;
			LexNode.Equivalence bestEquivalence = null;
			double mostSignificant = 1;
			double bestOverlap = 0;
			
			//i, j give the context windows described in step 4. 
			for (int i = 0; i < p.size() - L - 1; i++) {
				for (int j = i + 1; j <= i + L - 2; j++) {
					// 4a, i: Finds all subpaths of paths that match the subpath
					// of p at the endpoints i and K - L - 2 (TODO: and the slot?). Remember the paper
					// indexes by 1.
					ArrayList<SearchPath> matches = endpoint_match(p.copy(i, p.size() - L - 1), j-i); // Remember right endpoint is excluded.
					// 4a, ii: Selects/creates the equivalence class with the largest
					// overlap with encountered vertices at spot j.
					LexNode.Equivalence e = compare(p, matches, i, j);
					// Construct the generalized search path, replacing
					// pc.get(j) with the equivalence class e. 
					pc.replace(e, j, j + 1); 
				}

				for (int k = i + 1; k <= i + L - 2; k++) {
					for (int j = i + 1; j <= i + L - 2; j++) {
						if (j == k)
							continue;
						// Consider all paths going through all vertices in
						// (vertex k (an equivalence class) intersect vertex j
						// (an equivalence class)). This performs the intersection
						// and alters vertex k of pc. 
						LexNode.Equivalence vertex_k = (LexNode.Equivalence) pc.get(k);
						LexNode.Equivalence vertex_j = (LexNode.Equivalence) pc.get(j);
						vertex_k.pieces.retainAll(vertex_j.pieces);

						// 4b, ii: Perform MEX on this generalized path
						LexNode.Pattern candidatePattern = extractSignificantPattern(pc);
						if (candidatePattern != null)
							allNullPattern = false;
						// Extract the leading pattern P, as well as equivalence
						// class associated with P, and the overlap of that
						// class, over the course of the loop (one for each SearchPath). 
						double tempSig = 0.5 * (computeLeftSignificance(pc, candidatePattern) + computeRightSignificance(pc, candidatePattern));
						if (bestPattern == null	|| tempSig <= mostSignificant) {
							bestPattern = candidatePattern;
							bestEquivalence = vertex_k;
							bestOverlap = vertex_k.pieces.size()/ (double) vertex_j.pieces.size();
							mostSignificant = tempSig;
						}

					}
				}
			}

			// If the overlap is < 1, define a new equivalence class consisting
			// of only "those members that did appear in the set"
			// I take this to mean the members in the intersection of vertex_k
			// and vertex_j above, since it would be redundant
			// to add the equivalence class at vertex_j.
			if (bestOverlap < 1)
				eclasses.put(bestEquivalence.name, bestEquivalence);

			// 4d: rewire the graph with the best pattern found.
			rewire(bestPattern, a);
			bestPattern = null;
			bestEquivalence = null;
			bestOverlap = 0;
		}

		return allNullPattern;
	}
	
	// Find the leading significant pattern.
	public LexNode.Pattern extractSignificantPattern(SearchPath sp) {
		double min = 1;
		LexNode.Pattern bestPattern = null;
		for (int i = 0; i < sp.size(); i++)
			for (int j = i + 1; j < sp.size(); j++) {
				LexNode.Pattern candidatePattern = new LexNode.Pattern(sp.copy(i, j + 1)); //TODO: creating a LexNode here may cause problems later.
				
				//Compute the average of the left and right significances. This is, I think,
				//what is suggested in the paper.
				double sig = 0.5 * (computeLeftSignificance(sp, candidatePattern) + computeRightSignificance(sp, candidatePattern));

				if (bestPattern == null || (sig < alpha && sig <= min)) {
					bestPattern = candidatePattern;
					min = sig;
				}
			}

		return bestPattern;
	}
	
	public void rewire(LexNode.Pattern P, boolean a) {
		// create new vertex corresponding to pattern
		nodes.put(P.name, P);

		SearchPath subpath = new SearchPath();
		subpath.add(P);
		// Replace, in every path, the portion that matches with P
		for (SearchPath path : paths) {
			int ii = path.match(subpath);

			if (ii > 0 && (a || (computeLeftSignificance(path, P) < alpha) && computeRightSignificance(path, P) < alpha))
				path.replace(P, ii, ii + subpath.size());
				// removal is exclusive on the right endpoint, hence ii +
				// subpath.size() rather than ii + subpath.size() - 1
		}
	}

	/**
	 * Counts how many times we see sp[i, j] in the rest of the graph.
	 */
	private int l(SearchPath sp, int i, int j) {
		int match = 0;

		for (SearchPath vertices : sp.expandAll()) {
			for (SearchPath p : paths) {
				int currentIndex = i;

				for (int k = 0; k < p.size(); k++) {
					if (p.get(k) == vertices.get(currentIndex)) {
						currentIndex++;
						if (currentIndex == j) {
							match++;
							currentIndex = i; //Go back to searching the rest of the sentence.
						}
					} else
						currentIndex = i;
				}
			}
		}

		return match;
	}

	/**
	* Calculate the right or left probabilities (if forward is true or not). Needed
	* for the MEX criterion.
	*/
	public double P(SearchPath sp, int i, int j, boolean forward) {
		return forward ? l(sp, i, j) / l(sp, i, j - 1) : l(sp, i, j)
				/ l(sp, i + 1, j);
	}

	/**
	* Calculate the right and left decrease ratios (if forward is true or not). Needed
	* for the MEX criterion.
	*/
	public double D(SearchPath sp, int i, int j, boolean forward) {
		return forward ? P(sp, i, j, forward) / P(sp, i, j - 1, forward) : P(
				sp, i, j, forward) / P(sp, i + 1, j, forward);
	}

	/**
	* Calculate the right significance. Details are in the Supporting Text of
	* Solan's paper. Needed for the MEX criterion. 
	*/
	public double computeRightSignificance(SearchPath sp, LexNode.Pattern P) {
		double sum = 0;
		SearchPath subpath = new SearchPath();
		subpath.add(P);
		int i = sp.match(subpath);
		int j = i + subpath.size();

		for (int x = 0; x <= l(sp, i, j); x++)
			sum += CombinatoricsUtils.binom(l(sp, i, j - 1), x, eta * P(sp, i, j - 1, true));
		return Math.min(Math.max(sum, 0.0), 1.0);
	}
	
	/**
	* Calculate the left significance. Needed for the MEX criterion.
	*/
	public double computeLeftSignificance(SearchPath sp, LexNode.Pattern P) {
		double sum = 0;
		SearchPath subpath = new SearchPath();
		subpath.add(P);
		int i = sp.match(subpath);
		int j = i + subpath.size();

		for (int x = 0; x <= l(sp, i, j); x++)
			sum += CombinatoricsUtils.binom(l(sp, i, j - 1), x,	eta * P(sp, i, j + 1, false)); //TODO: are i, j + 1 the correct indices?
		return Math.min(Math.max(sum, 0.0), 1.0);
	}


	/** Returns an equivalence class at index j, where the path is known to start
	 * at i and go to i + L - 1. This is needed for 3a, ii.
	 * 
	 */
	public LexNode.Equivalence equiv(SearchPath sp, int i, int j) {
		LexNode.Equivalence toReturn = new LexNode.Equivalence();
		toReturn.pieces.add(sp.get(j)); 
		//TODO: does this add sp.get(j) twice? I feel like we need a way to compare equality of SearchPaths.		
		for (SearchPath p : paths) {
			SearchPath left = sp.copy(i, j); // subpath from i (inclusive) to j - 1 (inclusive)
			SearchPath right = sp.copy(j + 1, i + L); // subpath from j + 1 (inclusive) to i + L - 1 (inclusive)
			int leftMatch = p.match(left); // this is stored so that the node in p can be retrieved.
			//If we find a match, add the corresponding node of p to our Equivalence class.
			if (leftMatch > 0 && p.match(right) > 0)
				toReturn.pieces.add(p.get(leftMatch + j - i - 1));
		}
		nodes.put(toReturn.name, toReturn); //TODO: need a separate HashMap for equivalence class?
		return toReturn;
	}

	/**
	* Returns an equivalence class of LexNodes that match p. Required for step 4a, ii. 
	*/
	public LexNode.Equivalence compare(SearchPath p, ArrayList<SearchPath> matches, int i, int j) {
		HashSet<LexNode> encountered = new HashSet<LexNode>();
		for (SearchPath path : matches)
			encountered.add(path.get(j - i)); 
		//Get j-i since matches consists of _subpaths_ which matched

		//Variables used to find the best equivalence class (step 4a, ii). 
		double bestIntersect = 0;
		LexNode.Equivalence best = null;

		//At slot j, compare the set of encountered vertices to the list of existing equivalence classes
		//Selecting the one that has the largest overlap with the encountered vertices, and the overlap is larger than 0.65
		for (LexNode.Equivalence e : eclasses.values()) {
			HashSet<LexNode> eclass = new HashSet<LexNode>(e.pieces);
			double size = encountered.size();
			encountered.retainAll(eclass);
			if (encountered.size() / size > bestIntersect && encountered.size() / size > omega) {
				best = e;
				bestIntersect = encountered.size()/size;
			}
		}

		LexNode.Equivalence toReturn = new LexNode.Equivalence();
		//If the best is null, we didn't find an equivalence class satisfying the
		//desired conditions. Use p.get(j) as an equivalence class.
		if (best == null) {
			toReturn.pieces.add(p.get(j));
		} else
			toReturn = best;
		
		nodes.put(toReturn.name, toReturn);
		return toReturn;
	}

	/**
	* Returns a list of SearchPaths that match subpath at its endpoints. Needed in 
	* 4a, i. 
	*/
	public ArrayList<SearchPath> endpoint_match(SearchPath subpath, int relativeSlot) {
		ArrayList<SearchPath> toReturn = new ArrayList<SearchPath>();
		for (SearchPath p : paths) {
			for (int start = 0; start < p.size(); start++) {
				boolean match = false;
				int end = start + subpath.size() - 1;
				int actualSlot = start + relativeSlot;
				try {
					match = (p.get(start) == subpath.get(0)) && (p.get(end) == subpath.get(subpath.size() - 1))
								&& (p.get(actualSlot) == subpath.get(relativeSlot));
				} catch (Exception e) {
					continue;
				}

				if (match)
					toReturn.add(p.copy(start, end + 1));
			}
		}
		return toReturn;
	}
}
