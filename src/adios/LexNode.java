
package adios;

import java.util.ArrayList;

public abstract class LexNode {
	public String name;

	public abstract ArrayList<LexNode> expand();

	public static class Equivalence extends LexNode {
		private static int NEQIV = 0; // number of equivalence ndoes so far; use for naming.
		ArrayList<LexNode> pieces; // choose any piece; they are equivalent

		public Equivalence() {
			pieces = new ArrayList<LexNode>();
			this.name = "E" + (++NEQIV);
		}

		public ArrayList<LexNode> expand() {
			ArrayList<LexNode> nodes = new ArrayList<LexNode>();

			for (LexNode n : pieces)
				nodes.addAll(n.expand());

			return nodes;
		}
	}

	public static class Pattern extends LexNode {
		private static int NPATT = 0; // number of equivalence ndoes so far; use for naming.
		ArrayList<LexNode> pieces; // need all of the pieces together in order

		public Pattern(ArrayList<LexNode> a) {
			this.pieces = a;
			this.name = "P" + (++NPATT);
		}

		public ArrayList<LexNode> expand() {
			ArrayList<SearchPath> expanded = SearchPath.expandAll(pieces);

			ArrayList<LexNode> patterns = new ArrayList<LexNode>();
			for (SearchPath p : expanded)
				patterns.add(new Pattern(p));

			return patterns;
		}
	}

	public static class Leaf extends LexNode {

		public Leaf(String s) {
			this.name = s;
		}

		public ArrayList<LexNode> expand() {
			ArrayList<LexNode> listForOne = new ArrayList<LexNode>();
			listForOne.add(this); // I'm so lonely
			return listForOne;
		}
	}

	public String toString() {
		return name;
	}
}
