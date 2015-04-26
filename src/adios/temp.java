ArrayList<SearchPath> paths;
ArrayList<LexNode.Equivalence> eclasses;

double eta = 0.65;
double alpha = 0.001;
double L = 3;

public double l(SearchPath sp, int i, int j) {
	double match = 0;
	for (SearchPath vertices : sp.expandAll()) {
		for (SearchPath p : paths) {
			int currentIndex = i;
			for (int k = 0; k < p.size(); k++) {
				if (p.get(k) == sp.get(currentIndex)) {
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

public double significance(SearchPath sp, int i, int j) {
	double sum = 0;
	for (int x = 0; x <= l(sp, i, j); x++) {
		sum += binom(l(sp, i, j - 1), x, eta*P(sp, i, j - 1, true));
	}
	return sum;
}

public double binom(n, k, p) {
	return CombinatoricsUtils.binomialCoefficient(n, k)*Math.pow(p, k)*Math.pow(1 - p, k);
}

public void rewire(LexNode.Pattern P, SearchPath path, int i, int j, boolean a) {
	//Replace, in every path, the portion that matches with P
	for (SearchPath sp : paths)
		int ii = path.match(sp, i, j);
		if (ii > 0 && a)
			sp.replace(P, ii, ii + j - i);
		else if (ii > 0 && !a && significance(sp, ii, ii + j - i) < alpha)
			sp.replace(P, ii, ii + j - i);
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
		eclasses.add(e);
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
			toReturn.pieces.add(p.get(j));
	}
	return toReturn;
}