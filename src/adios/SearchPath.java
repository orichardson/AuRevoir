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
	
	/// ************************** STATIC METHODS *************************
	
	public static SearchPath fromSentence(String str) {
		String[] pieces = str.split("\\s+");
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
