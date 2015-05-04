
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

	/**
	 * Find whether there is a portion of this SearchPath that matches subpath. 
	 * Returns the index of the first match. Returns -1 otherwise.
	 * 
	 * TODO: is this method correct?
	 */
	@Deprecated
	public ArrayList<Integer> match(SearchPath subpath) {
		ArrayList<Integer> matches = new ArrayList<Integer>();
		int currentIndex = 0;
		//TODO: this method was changed to index free input.
		for (int k = 0; k < size(); k++) {
			if (subpath.get(currentIndex).equals(get(k))) { // there may be some issues here later?
				if (currentIndex == subpath.size() - 1) {
					matches.add(k - subpath.size() + 1);
					currentIndex = 0;
				} else
					currentIndex++;
				

			} else
				currentIndex = 0;
		}

		return matches;
	}
	
	/**
	 * Find whether there is a portion of this SearchPath that matches subpath. 
	 * Returns the index of the first match. Returns -1 otherwise.
	 * 
	 * TODO: is this method correct?
	 */
	public int firstMatch(SearchPath subpath) {
		int currentIndex = 0;
		//TODO: this method was changed to index free input.
		for (int k = 0; k < size(); k++) {
			if (subpath.get(currentIndex).equals(get(k))) { // there may be some issues here later?
				if (currentIndex == subpath.size() - 1)
					return k - subpath.size() + 1;
				currentIndex++;
				

			} else
				currentIndex = 0;
		}

		return -1;
	}

	// Typically, P will be a pattern. But for generalization,
	// it will also be a equivalence class. Removal on the right
	// index is exclusive.
	public void replace(LexNode P, int i, int j) {
		//TODO: check the entire SearchPath for a match with P? Currently we are not doing this.
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
