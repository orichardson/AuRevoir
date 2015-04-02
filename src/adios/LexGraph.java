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
	
	public LexNode.Pattern findBestPattern(SearchPath path) {
		return null;
	}

	public void generalize() { // ADIOS ALGORITHM

	}
}

