
package adios;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import adios.LexNode.Equivalence;
import adios.LexNode.Pattern;

public class LexGraph {
	HashMap<String, LexNode> nodes;
	HashMap<String, Equivalence> eclasses;
	HashMap<String, Pattern> patterns;
	ArrayList<SearchPath> paths;

	double eta = 0.65;
	double alpha = 0.15;
	int L = 4;
	double omega = 0.65;

	/**
	 * Actual ADIOS algorithm.
	 */
	public void adios(boolean a) {
		pattern_distillation(a);
		// generalization_first(a);
		//
		// while (!generalization_bootstrap(a)) {
		// System.out.println("\n\n@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@"
		// + "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
		// System.out.println("Bootstrapping.");
		// }

		for (SearchPath sentence : paths)
			System.out.println(sentence);

	}

	/**
	 * Constructor which handles step 1 of the ADIOS algorithm.
	 * 
	 * @param corpus
	 */
	public LexGraph(ArrayList<String> sentences) {
		nodes = new HashMap<String, LexNode>();
		eclasses = new HashMap<String, Equivalence>();
		patterns = new HashMap<String, Pattern>();
		paths = new ArrayList<SearchPath>();

		for (String sentence : sentences)
			paths.add(SearchPath.fromSentence(this, sentence));
	}

	/**
	 * Handles step 2 of the ADIOS algorithm, pattern distillation.
	 * 
	 * @param a
	 *            : mode A or mode B -- true: replace on everything (context free) false: replace
	 *            only on significant paths (context-dependent)
	 */
	public void pattern_distillation(boolean a) {
		for (SearchPath p : paths) {
			Pattern temp = extractSignificantPattern(p); // 2a: find the leading significant pattern
			rewire(temp, a);// 2b: rewire the graph
		}
		return;
	}

	/**
	 * Handles step 3 of the ADIOS algori)thm, generalization-first step.
	 * 
	 * @param a
	 */
	public void generalization_first(boolean a) {
		// Need to find the mostSignificantPattern, and the mostSignificantEquivalence
		// associated with the mostSignificantPattern.
		Pattern mostSignificantPattern = null;
		Equivalence mostSignificantEquivalence = null;
		double mostSignificant = 1;
		for (SearchPath p : paths) {
			// These two loops handle 3a and 3b.
			for (int i = 0; i < p.size() - L - 1; i++) {
				for (int j = i + 1; j <= i + L - 2; j++) {
					// i, j give the context window.
					SearchPath pc = p.copy(); // Don't want to alter original SearchPath.
					// 3a: alter SearchPath pc so that it is the generalized search path
					// discussed in 3a, i-iii. Replaces p.get(j) with an equivalence class.
					Equivalence e = equiv(p, i, j);
					pc.replace(e, j, j + 1); // Replacement is exclusive on the right endpoint.

					// 3b: performs MEX on the generalized path, finding the most significant
					// pattern
					// over the course of the loop (one for each SearchPath).
					Pattern candidatePattern = extractSignificantPattern(pc);
					double D_max = Math.max(D(p, i, j, true), D(p, i, j, false));
					if (D_max >= eta)
						continue;

					double tempSig = Math.max(computeLeftSignificance(p, candidatePattern),
							computeRightSignificance(p, candidatePattern));
					if (mostSignificantPattern == null || tempSig < mostSignificant) {
						mostSignificantPattern = candidatePattern;
						mostSignificantEquivalence = e;
						mostSignificant = tempSig;
					}
				}
			}
			// 3c: We already created the equivalence class.
			// Add this to our list of equivalence classes.
			if (mostSignificantEquivalence != null) {
				rewire(mostSignificantPattern, a);
			}
			// 3d: rewire the graph, and reset the mostSignificant variables.
			mostSignificantPattern = null;
			mostSignificantEquivalence = null;
		}
	}

	/**
	 * Handles step 4 of the ADIOS algorithm, generalization-bootstrap. Returns a boolean so that we
	 * can perform step 5 (repeat step 4).
	 * 
	 * @param a
	 * @return
	 */
	public boolean generalization_bootstrap(boolean a) {
		// A boolean for if all patterns found are null. If true, all patterns/equivs are null,
		// so we are done. If false, at least one pattern/equiv is not null. So not done.
		boolean allNullPattern = true;

		for (SearchPath p : paths) {
			SearchPath pc = p.copy();
			// For each SearchPath p, we want to find the bestPattern and
			// equivalence class in that pattern. These variables are to assist
			// in that.
			Pattern bestPattern = null;
			Equivalence bestEquivalence = null;
			double mostSignificant = 1;
			double bestOverlap = 0;

			// i, j give the context windows described in step 4.
			for (int i = 0; i < p.size() - L - 1; i++) {
				for (int j = i + 1; j <= i + L - 2; j++) {
					// 4a, i: Finds all subpaths of paths that match the subpath
					// of p at the endpoints i and K - L - 2 (TODO: and the slot? Paper is
					// confusing). Remember the paper
					// indexes by 1.
					ArrayList<SearchPath> matches = endpoint_match(p.copy(i, p.size() - L - 1), j
							- i); // Remember right endpoint is excluded.
					// 4a, ii: Selects/creates the equivalence class with the largest
					// overlap with encountered vertices at spot j.
					Equivalence e = compare(p, matches, i, j);
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
						Equivalence vertex_k = (Equivalence) pc.get(k);
						Equivalence vertex_j = (Equivalence) pc.get(j);
						vertex_k.pieces.retainAll(vertex_j.pieces);

						// 4b, ii: Perform MEX on this generalized path
						Pattern candidatePattern = extractSignificantPattern(pc);
						if (candidatePattern != null)
							allNullPattern = false;
						// Extract the leading pattern P, as well as equivalence
						// class associated with P, and the overlap of that
						// class, over the course of the loop (one for each SearchPath).
						double D_max = Math.max(D(pc, i, j, true), D(pc, i, j, false));
						if (D_max >= eta)
							continue;

						double tempSig = Math.max(computeLeftSignificance(pc, candidatePattern),
								computeRightSignificance(pc, candidatePattern));
						if (bestPattern == null || tempSig < mostSignificant) {
							bestPattern = candidatePattern;
							bestEquivalence = vertex_k;
							bestOverlap = vertex_k.pieces.size() / (double) vertex_j.pieces.size();
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
			if (bestOverlap < 1 && bestEquivalence != null) {
				eclasses.put(bestEquivalence.name, bestEquivalence);
			}

			// 4d: rewire the graph with the best pattern found.
			rewire(bestPattern, a);
			bestPattern = null;
			bestEquivalence = null;
			bestOverlap = 0;
		}

		return allNullPattern;
	}

	// Find the leading significant pattern.
	public Pattern extractSignificantPattern(SearchPath sp) {
		double min = 0;
		Pattern bestPattern = null;

		for (int i = 0; i < sp.size(); i++)
			for (int j = i + 1; j < sp.size(); j++) {
				// Check if the pattern from the subpath i, j already exists. If so,
				// make use of it. If so, then candidatePattern will be an instance of
				// Pattern. Otherwise, it will be an instance of Pattern.
				// CandidatePattern.
				Pattern candidatePattern = patternExists(sp.copy(i, j + 1));
				// Compute the average of the left and right significances. This is, I think,
				// what is suggested in the paper.
				// TODO: Maybe compare maxima instead of average

				double D_max = Math.max(D(sp, i, j, true), D(sp, i, j, false));
				if (D_max >= eta)
					continue;

				double sig = Math.max(computeLeftSignificance(sp, candidatePattern),
						computeRightSignificance(sp, candidatePattern));

				System.out.println((float) sig + "\tCandidate pattern: " + candidatePattern.pieces
						+ " for the path: \n\t\t" + sp);
				if (sig < alpha && (bestPattern == null || sig < min)) {
					bestPattern = candidatePattern;
					min = sig;
				}

				// We learned something, maybe? --reasons why we can't use just
				// P_R or P_L, and similarly with D_R and D_L. Biasing prl towards
				// smaller lengths didn't seem to work either.
			}
		return bestPattern;
	}

	/**
	 * Checks if there is a Pattern P with P.pieces = (ArrayList) sp. If there is, P is returned.
	 * Otherwise, a pattern with P.pieces = (ArrayList) sp is created.
	 * 
	 * @param sp
	 * @return
	 */
	public Pattern patternExists(SearchPath sp) {
		for (Pattern p : patterns.values())
			if (p.pieces.equals(sp)) {
				return p;
			}
		return new Pattern(sp, false);
	}

	/**
	 * Create new vertex corresponding to Pattern.
	 * 
	 * @param P
	 * @param a
	 */
	public void rewire(Pattern P, boolean a) {
		if (P == null)
			return;
		P.actualize();
		nodes.put(P.name, P);
		System.out.println("********************************************Rewiring " + P.pieces);
		// Replace, in every path, the portion that matches with P
		for (SearchPath path : paths) {
			int index = P.matchFirst(path);
			while (index >= 0) {
				if (a
						|| (computeLeftSignificance(path, P) < alpha && computeRightSignificance(
								path, P) < alpha)) {
					System.out.println(path);
					path.replace(P, index, index + P.pieces.size());
				}
				// removal is exclusive on the right endpoint, hence ii +
				// subpath.size() rather than ii + subpath.size() - 1

				index = P.matchFirst(path, index + 1);
			}

		}

	}

	/**
	 * Counts how many times we see sp[i, j] in the rest of the graph, both endpoints included. This
	 * is inconsistent from some of the other subpath matching methods.
	 */
	double l(SearchPath sp, int i, int j) {
		double match = 0;
		// Pattern p = new Pattern(sp.copy(i,j+1),false);
		//
		// for(SearchPath path : paths)
		// {
		// int index = 0;
		// int rslt = p.matchFirst(path);
		//
		// while(rslt >= 0) {
		// index = rslt + 1;
		// rslt = p.matchFirst(path,index);
		//
		// match++;
		// }
		// }

		// if (sp.toString().equals("[Pam, thinks, that, Cindy, P1, please, is, easy]"))
		// System.out.println("EXPANDED: " + sp.expandAll());

		for (SearchPath possible : sp.expandAll()) {
			// SearchPath possible = sp;
			for (SearchPath p : paths) {
				int currentIndex = i;

				for (int k = 0; k < p.size(); k++) {
					if (p.get(k).equals(possible.get(currentIndex))) {
						if (currentIndex == j) {
							match++;
							currentIndex = i; // Go back to searching the rest of the sentence.
						} else
							currentIndex++;
					} else {
						if (currentIndex != i)
							k--;
						currentIndex = i;
					}
				}
			}
		}
		return match;
	}

	double l_spec() {
		double sum = 0;
		for (LexNode l : nodes.values())
			sum += l(new SearchPath(l), 0, 0);
		return sum;
	}

	/**
	 * Calculate the right or left probabilities (if forward is true or not). Needed for the MEX
	 * criterion.
	 */
	public double P(SearchPath sp, int i, int j, boolean forward) {
		double denom = (i == j) ? l_spec() : (forward ? l(sp, i, j - 1) : l(sp, i + 1, j));
		if (denom == 0)
			return 0;
		if (l(sp, i, j) == 0) {
			// System.err.println(sp);
			// System.err.println("i, j = " + i + "," + j);
			// System.err.println(sp.copy(i, j + 1));
		}
		return l(sp, i, j) / denom;
	}

	/**
	 * Calculate the right and left decrease ratios (if forward is true or not). Needed for the MEX
	 * criterion.
	 */
	public double D(SearchPath sp, int i, int j, boolean forward) {
		return forward ? P(sp, i, j, forward) / P(sp, i, j - 1, forward) : P(sp, i, j, forward)
				/ P(sp, i + 1, j, forward);
	}

	/**
	 * Calculate the right significance. Details are in the Supporting Text of Solan's paper. Needed
	 * for the MEX criterion.
	 */
	public double computeRightSignificance(SearchPath sp, Pattern P) {
		double sum = 0;
		int i = P.matchFirst(sp);
		if (i < 0)
			return 1;
		int j = i + P.pieces.size() - 1;
		if (sp.toString().equals("[Pam, thinks, that, Cindy, P1, please, is, easy]")
				&& P.pieces.toString().equals("[thinks, that, Cindy]")) {
			System.err.printf("(i, j) = (%d, %d)\n", i, j);
			System.err.println("x_max=" + l(sp, i, j));
			System.err.println("n=" + l(sp, i, j - 1));// incorrect
			System.err.println("p=" + P(sp, i, j - 1, true));

		}
		for (int x = 0; x <= l(sp, i, j); x++)
			sum += CombinatoricsUtils.binom((int) l(sp, i, j - 1), x, eta * P(sp, i, j - 1, true));
		return sum;
	}

	/**
	 * Calculate the left significance. Needed for the MEX criterion.
	 */
	public double computeLeftSignificance(SearchPath sp, Pattern P) {
		double sum = 0;

		int i = P.matchFirst(sp);

		if (i < 0)
			return 1;

		// TODO: really sketchy length calculation. Note that patterns can
		// (1) match multiple times,
		// (2) have multiple lengths, and,
		// (3) have multiple start indices. Hoping that order of
		// generalization takes care of these issues.
		int j = i + P.pieces.size() - 1;// because inclusive

		for (int x = 0; x <= l(sp, i, j); x++) {
			sum += CombinatoricsUtils.binom((int) l(sp, i + 1, j), x, eta * P(sp, i + 1, j, false));
		}
		// TODO: are i, j + 1 the correct indices?

		return sum;
	}

	/**
	 * Returns an equivalence class at index j, where the path is known to start at i and go to i +
	 * L - 1. This is needed for 3a, ii.
	 * 
	 */
	public Equivalence equiv(SearchPath sp, int i, int j) {
		HashSet<LexNode> pieces = new HashSet<LexNode>();
		pieces.add(sp.get(j));
		// TODO: does this add sp.get(j) twice? I feel like we need a way to compare equality of
		// SearchPaths.
		for (SearchPath p : paths) {
			SearchPath left = sp.copy(i, j); // subpath from i (inclusive) to j - 1 (inclusive)
			SearchPath right = sp.copy(j + 1, i + L); // subpath from j + 1 (inclusive) to i + L - 1
														// (inclusive)
			// int leftMatch = p.match(left); // this is stored so that the node in p can be
			// retrieved.
			int leftMatch = p.firstMatch(left);
			// If we find a match, add the corresponding node of p to our Equivalence class.
			if (leftMatch >= 0 && p.firstMatch(right) >= 0)
				pieces.add(p.get(leftMatch + j - i - 1));
		}
		return equivalenceClassExists(pieces);
	}

	/**
	 * If an equivalence class E with E.pieces.equals(pieces), then E is returned. Otherwise, an
	 * equivalence class is made with E.pieces = pieces.
	 * 
	 * @param pieces
	 * @return
	 */
	public Equivalence equivalenceClassExists(HashSet<LexNode> pieces) {
		for (Equivalence e : eclasses.values())
			if (e.pieces.equals(pieces))
				return e;
		Equivalence e = new Equivalence(pieces);
		eclasses.put(e.name, e);
		return e;
	}

	/**
	 * Returns an equivalence class of LexNodes that match p. Required for step 4a, ii.
	 */
	public Equivalence compare(SearchPath p, ArrayList<SearchPath> matches, int i, int j) {
		HashSet<LexNode> encountered = new HashSet<LexNode>();
		for (SearchPath path : matches)
			encountered.add(path.get(j - i));
		// Get j-i since matches consists of _subpaths_ which matched

		// Variables used to find the best equivalence class (step 4a, ii).
		double bestIntersect = 0;
		Equivalence best = null;

		// At slot j, compare the set of encountered vertices to the list of existing equivalence
		// classes
		// Selecting the one that has the largest overlap with the encountered vertices, and the
		// overlap is larger than 0.65
		for (Equivalence e : eclasses.values()) {
			double size = encountered.size();
			encountered.retainAll(e.pieces);
			if (encountered.size() / size > bestIntersect && encountered.size() / size > omega) {
				best = e;
				bestIntersect = encountered.size() / size;
			}
		}
		// If the best is null, we didn't find an equivalence class satisfying the
		// desired conditions. Use p.get(j) as an equivalence class.
		if (best == null) {
			HashSet<LexNode> temp = new HashSet<LexNode>();
			temp.add(p.get(j));
			return equivalenceClassExists(temp);
		} else
			return best;
	}

	/**
	 * Returns a list of SearchPaths that match subpath at its endpoints. Needed in 4a, i.
	 */
	public ArrayList<SearchPath> endpoint_match(SearchPath subpath, int relativeSlot) {
		ArrayList<SearchPath> toReturn = new ArrayList<SearchPath>();
		for (SearchPath p : paths) {
			for (int start = 0; start < p.size(); start++) {
				boolean match = false;
				int end = start + subpath.size() - 1;
				int actualSlot = start + relativeSlot;
				try {
					match = (p.get(start).equals(subpath.get(0)))
							&& (p.get(end).equals(subpath.get(subpath.size() - 1)))
							&& (p.get(actualSlot).equals(subpath.get(relativeSlot)));
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
