
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
	
	//TODO: copy returns a subpath of this SearchPath from i (inclusive) to j (exclusive)
	public SearchPath copy(int i, int j) {
		SearchPath toReturn = new SearchPath();
		for (int k = i; k < j; k++)
			toReturn.add(get(k));
		return toReturn;
	}

	/*
	 * Find whether there is a portion of this SearchPath that matches subpath. 
	 */
	public int match(SearchPath subpath) {
		int currentIndex = 0;

		for (int k = 0; k < size(); k++) {
			if (subpath.get(currentIndex) == get(k)) { // there may be some issues here.
				currentIndex++;
				
				if (currentIndex == subpath.size() - 1)
					return k - subpath.size(); //TODO: off by one error?
			} else
				currentIndex = 0;
		}

		return -1;
	}

	// Typically, P will be a pattern. But for generalization,
	// it will also be a equivalence class.
	public void replace(LexNode P, int i, int j) {
		removeRange(i, j); 
		add(i, P); // add a note at index i
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
