package adios;

import java.util.ArrayList;

public class SearchPath extends ArrayList<LexNode> {
	private static final long serialVersionUID = -4507771466776250930L;

	public SearchPath(ArrayList<LexNode> a) {
		super(a);
	}

	public SearchPath(LexNode... nodes) {
		super(nodes.length);
		for (LexNode n : nodes)
			add(n);
	}

	public ArrayList<SearchPath> expandAll() {
		return expandAll(this);
	}

	public SearchPath copy() {
		return new SearchPath(this);
	}
	
	public int match(SearchPath other, int i, int j) {
		int currentIndex = i;
		for (int k = 0; k < other.size(); k++) {
			if (get(currentIndex) == other.get(k)) {
				currentIndex++;
				if (currentIndex == j)
					return k - (j - i);
			else
				currentIndex = i;
			}
		}
		return -1;
	}

	public boolean replace(LexNode.Pattern P, int i, int j) {
		for (int k = i; k <= j; k++) {
			remove(i); //since arraylist will shift, remove the same spot
		}
		add(P, i); //add a note at index i
	}



	// / ************************** STATIC METHODS *************************

	public static SearchPath fromSentence(LexGraph g, String str) {
		String[] pieces = str.split("\\s+");
		ArrayList<LexNode> nodes = new ArrayList<LexNode>();
		for (String s : pieces) {
			if (g.nodes.containsKey(s))
				nodes.add(g.nodes.get(s));
			else {
				LexNode l = new LexNode.Leaf(s);
				g.nodes.put(s, l);
				nodes.add(l);
			}
		}
		return new SearchPath(nodes);
	}

	public static ArrayList<SearchPath> expandAll(ArrayList<LexNode> list) {
		ArrayList<SearchPath> paths = null;

		for (int i = 0; i < list.size(); i++) {
			ArrayList<LexNode> nodes = list.get(i).expand();

			ArrayList<SearchPath> newPaths = new ArrayList<SearchPath>();

			if (paths == null) {
				for (LexNode n : nodes)
					newPaths.add(new SearchPath(n));
			} else {
				for (SearchPath p : paths) {
					for (LexNode node : nodes) {
						SearchPath asdf = p.copy();
						asdf.add(node);
						newPaths.add(asdf);
					}
				}
			}

			paths = newPaths;
		}

		return paths;
	}
}
