
package adios;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.math3.util.CombinatoricsUtils;

import adios.LexNode.Pattern;

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
					if (p.get(k).equals(vertices.get(currentIndex))) {
						currentIndex++;
						if (currentIndex == j) {
							match++;
							currentIndex = i;
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
		int ii = 0;
		int jj = 0;

		for (int i = 0; i < sp.size(); i++)
			for (int j = i + 1; j < i; j++) {
				LexNode.Pattern candidatePattern = new LexNode.Pattern(sp.copy(i, j + 1));
				double temp = significance(sp, candidatePattern);
				
				//TODO: Worry: Shouldn't significance be GREATER than some threshold alpha?
				//TODO: Reply: No, the significance is measured by the p-value of the hypothesis test,
				//which as normal needs to be less than alpha.
				if (temp < alpha && temp <= min) {
					min = temp;
					ii = i;
					jj = j;
				}
			}

		ArrayList<LexNode> subpath = new ArrayList<LexNode>();
		for (int i = ii; i <= jj; i++)
			subpath.add(sp.get(i));

		return new LexNode.Pattern(subpath);
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
			sum += binom(l(sp, i, j - 1), x, eta * P(sp, i, j - 1, true));
		}
		return Math.min(Math.max(sum, 0.0), 1.0);
	}

	public double binom(int n, int k, double p) {
		return CombinatoricsUtils.binomialCoefficient(n, k) * Math.pow(p, k) * Math.pow(1 - p, k);
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
		LexNode.Equivalence e = null;
		for (SearchPath p : paths) {
			// these two loops handle 3a and 3b
			for (int i = 0; i < p.size() - L - 1; i++) {
				for (int j = i + 1; j <= i + L - 2; j++) {
					SearchPath pc = p.copy();
					e = equiv(p, i, j);

					pc.replace(e, i, i);
					LexNode.Pattern candidatePattern = extractSignificantPattern(pc); // candidate pattern

					if (mostSignificantPattern == null || (significance(p, candidatePattern) > significance(p, mostSignificantPattern)))
						mostSignificantPattern = candidatePattern;
				}
			}
			// handles 3c
			eclasses.add(e); // TODO: this isn't the correct e (the one corresponding to P)
			// handles 3d
			rewire(mostSignificantPattern, a);
			mostSignificantPattern = null; // reset mostSignificantPattern
		}
	}

	public LexNode.Equivalence equiv(SearchPath sp, int i, int j) {
		LexNode.Equivalence toReturn = new LexNode.Equivalence();
		toReturn.pieces.add(sp.get(j));
		for (SearchPath p : paths) {
			SearchPath left = sp.copy(i, j); //subpath from i (inclusive) to j - 1 (inclusive)
			SearchPath right = sp.copy(j + 1, i + L); //subpath from j + 1 (inclusive) to i + L - 1 (inclusive)
			if (p.match(left) > 0 && p.match(right) > 0)
				toReturn.pieces.add(p.get(j)); // TODO: the match won't be at j
		}
		return toReturn;
	}

	public void generalization_bootstrap(boolean a) {
		for (SearchPath p : paths) {
			SearchPath pc = p.copy();
			Object[] bestPattern = null;	
			for (int i = 0; i < p.size() - L - 1; i++) {
				for (int j = i + 1; j <= i + L - 2; j++) {
					//handles 4a, i
					ArrayList<OPair<SearchPath, Integer>> matches = endpoint_match(p, i, p.size() - L - 2);
					//handles 4a, ii
					LexNode.Equivalence e = compare(p, matches, i, j);
					pc.replace(e, j, j);
				}

				for (int k = i + 1; k <= i + L - 2; k++) {
					for (int j = i + 1; j <= i + L - 2; j++) {
						if (j == k)
							continue;

						pc.get(k).retainAll(pc.get(j)); //TODO: intersect over all j or just one?

						Object[] temp = most_sgf(pc);
						if (bestPattern == null)
							bestPattern = temp;
						else if (significance(temp[0], temp[1], temp[2]) > significance(bestPattern[0], bestPattern[1], bestPattern[2]))
							bestPattern = temp;

					}
				}
			}
			rewire()
		}
	}
	public LexNode.Equivalence compare(SearchPath p, ArrayList<OPair<SearchPath, Integer>> matches,
			int i, int j) {
		HashSet<LexNode> encountered = new HashSet<LexNode>();
		for (OPair<SearchPath, Integer> pair : matches) {
			SearchPath pp = pair.l;
			int k = pair.r;
			encountered.add(pp.get(k + j - i));
		}

		double intersect = 0;
		LexNode.Equivalence best = null;

		for (LexNode.Equivalence e : eclasses) {
			HashSet<LexNode> eclass = new HashSet<LexNode>(e.pieces);
			double size = encountered.size();
			encountered.retainAll(eclass);
			if (encountered.size() / size > intersect && encountered.size() / size > 0.65)
				best = e;
		}

		return best == null ? p.get(j) : best;
	}

	// Handles step 4, a, i
	public ArrayList<OPair<SearchPath, Integer>> endpoint_match(SearchPath path, int i, int j) {
		ArrayList<OPair<SearchPath, Integer>> toReturn = new ArrayList<OPair<SearchPath, Integer>>();
		for (SearchPath p : paths) {
			for (int k = 0; k < p.size(); k++) {
				boolean match = false;
				try {
					boolean matches = path.get(i).equals(p.get(k))
							&& path.get(j).equals(p.get(k + j - i));
				} catch (Exception e) {
					continue;
				}
				if (match)
					toReturn.add(new OPair(p, k));
			}
		}
	}
}
