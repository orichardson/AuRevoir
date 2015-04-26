package adios;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.inference.TTest;

public class LexGraph {
	
	ArrayList<SearchPath> paths;
	ArrayList<LexNode.Equivalence> eclasses;

	double eta = 0.65;
	double alpha = 0.001;
	double L = 3;
	double omega = 0.65;

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

	public double l(SearchPath sp, int i, int j) {
		double match = 0;
		for (SearchPath vertices : sp.expandAll()) {
			for (SearchPath p : paths) {
				int currentIndex = i;
				for (int k = 0; k < p.size(); k++) {
					//Will comparing addresses for equivalence classes and patterns be enough?
					if (p.get(k).equals(sp.get(currentIndex))) { 
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
		return forward ? l(sp, i, j)/l(sp, i, j - 1) : l(sp, i, j)/l(sp, i+1, j);
	}

	public double D(SearchPath sp, int i, int j, boolean forward) {
		return forward ? P(sp, i, j, forward)/P(sp, i, j - 1, forward) : P(sp, i, j, !forward)/P(sp, i+1, j, !forward);
	}

	//Find the leading significant pattern. 
	public Object[] most_sgf(SearchPath sp) {
		double min = 1; int ii = 0; int jj = 0;
		for (int i = 0; i < sp.size(); i++)
			for (int j = i; j < i; j++)
			{
				double temp = significance(sp, i, j);
				if (temp < alpha && temp <= min) {
					min = temp; ii = i; jj = j;
				}
			}

		ArrayList<LexNode> subpath = new ArrayList<LexNode>();
		for (int i = ii; i <= jj; i++) 
			subpath.add(path.get(i));
			
		return new Object[] {LexNode.Pattern(subpath), ii, jj};
	}

	//Do we need a compute backwards (left) significance method?
	public double significance(SearchPath sp, int i, int j) {
		double sum = 0;
		for (int x = 0; x <= l(sp, i, j); x++) {
			sum += binom(l(sp, i, j - 1), x, eta*P(sp, i, j - 1, true));
		}
		return Math.min(Math.max(significance, 0.0), 1.0);
	}

	public double binom(n, k, p) {
		return CombinatoricsUtils.binomialCoefficient(n, k)*Math.pow(p, k)*Math.pow(1 - p, k);
	}

	public void rewire(LexNode.Pattern P, SearchPath path, int i, int j, boolean a) {
		//Replace, in every path, the portion that matches with P
		for (SearchPath sp : paths) {
			int ii = path.match(sp, i, j);
			if (ii > 0 && (a || significance(sp, ii, ii + j - i) < alpha))
				sp.replace(P, ii, ii + j - i);
		}
	}

	public void pattern_distillation(boolean a) {
		for (SearchPath p : paths) {
			Object[] temp = most_sgf(p); //2a
			rewire(temp[0], p, temp[1], temp[2], a); //2b
		}
	}

	public void generalization_first(boolean a) {
		eclasses = new ArrayList<LexNode.Equivalence>();
		Object[] most_sgf_pat = null;
		LexNode.Equivalence e = null;
		for (SearchPath p : paths) {
			//these two loops handle 3a and 3b
			for (int i = 0; i < p.size() - L - 1; i++) {
				for (int j = i + 1; j <= i + L - 2; j++) {
					SearchPath pc = p.copy();
					e = equiv(sp, i, j);
					pc.replace(e, i, i);
					Object[] can_pat = most_sgf(pc); //candidate pattern
					if (most_sgf_pat == null)
						most_sgf_pat = temp2;
					else if (significance(most_sgf_pat[0], most_sgf_pat[1], most_sgf_pat[2])
								> significance(can_pat[0], can_pat[1], can_pat[2]))
						most_sgf_pat = temp2;
				}
			}
			//handles 3c
			eclasses.add(e); //TODO: this isn't the correct e (the one corresponding to P)
			//handles 3d
			rewire(most_sgf_pat[0], p, most_sgf_pat[1], most_sgf_pat[2]);
			most_sgf_pat = null; //reset most_sgf_pat.
		}
	}

	public LexNode.Equivalence equiv(SearchPath sp, int i, int j) {
		LexNode.Equivalence toReturn = new LexNode.Equivalence();
		toReturn.pieces.add(sp.get(j));
		for (SearchPath p : paths) {
			if (sp.match(p, i, j - 1) > 0 && sp.match(p, j + 1, i + L - 1) > 0)
				toReturn.pieces.add(p.get(j)); //TODO: the match won't be at j
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

				for (k = i + 1; k <= i + L - 2; k++) {
					for (j = i + 1; j <= i + L - 2; j++) {
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

	public LexNode.Equivalence compare(SearchPath p, ArrayList<OPair<SearchPath, Integer>> matches, int i, int j) {
		HashSet<LexNode> encountered = new HashSet<LexNode>();
		for (OPair<SearchPath, Integer> pair : matches) {
			SearchPath p = pair.l;
			int k = pair.r;
			encountered.add(p.get(k + j - i));
		}

		double intersect = 0; LexNode.Equivalence best = null;
		for (LexNode.Equivalence e : eclasses) {
			HashSet<LexNode> eclass = new HashSet<LexNode>(e);
			double size = encountered.size();
			encountered.retainAll(eclass);
			if (encountered.size()/size > intersect && encountered.size()/size > 0.65)
				best = e;
		}

		return best == null ? p.get(j) : best;
	}

	//Handles step 4, a, i
	public ArrayList<OPair<SearchPath, Integer>> endpoint_match(SearchPath path, int i, int j) {
		ArrayList<OPair<SearchPath, Integer>> toReturn = new ArrayList<OPair<SearchPath, Integer>>();
		for (SearchPath p : paths) {
			for (int k = 0; k < p.size(); k++) {
				boolean match = false;
				try {
					boolean match = path.get(i).equals(p.get(k)) && path.get(j).equals(p.get(k + j - i));
				} catch (Exception e) {
					continue;
				}
				if (match)
					toReturn.add(new OPair(p, k));
			}
		}
	}
}

