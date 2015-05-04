
package adios;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public abstract class LexNode {
	public String name;

	public abstract ArrayList<LexNode> expand();

	// Left hand side (called object) is the more abstract construction;
	// right hand side (argument) is the less abstract one.
	// returns the number of indices it ate.
	public abstract int matches(List<LexNode> l, int index);

	public static class Equivalence extends LexNode {
		private static int NEQIV = 0; // number of equivalence ndoes so far; use for naming.
		public HashSet<LexNode> pieces; // choose any piece; they are equivalent

		public Equivalence() {
			pieces = new HashSet<LexNode>();
			this.name = "E" + (++NEQIV);
		}

		public Equivalence(HashSet<LexNode> pieces) {
			this.pieces = pieces;
			this.name = "E" + (++NEQIV);
		}

		public int matches(List<LexNode> list, int k) {
			for (LexNode p : pieces) {
				int m = p.matches(list, k);
				if (m >= 0)
					return m;
			}
			return -1;
		}

		public ArrayList<LexNode> expand() {
			ArrayList<LexNode> nodes = new ArrayList<LexNode>();

			Iterator<LexNode> temp = pieces.iterator();
			while (temp.hasNext())
				nodes.addAll(temp.next().expand());

			return nodes;
		}

		/**
		 * Equals method that will rely on the equals method of its contents. This will eventually
		 * filter down to relying on equality of LexNode.Leafs.
		 */
		public boolean equals(Object o) {
			if (! (o instanceof LexNode.Equivalence))
				return false;
			LexNode.Equivalence temp = (LexNode.Equivalence) o;
			return temp.pieces.equals(pieces);
		}
	}

	public static class Pattern extends LexNode {
		private static int NPATT = 0; // number of equivalence ndoes so far; use for naming.
		public ArrayList<LexNode> pieces; // need all of the pieces together in order

		public Pattern(ArrayList<LexNode> a, boolean increment) {
			this.pieces = a;
			this.name = "TEMPORARY";

			if (increment)
				actualize();
		}
		/**
		 * Necessary for CandidatePattern.
		 */

		public ArrayList<LexNode> expand() {
			ArrayList<SearchPath> expanded = SearchPath.expandAll(pieces);

			ArrayList<LexNode> patterns = new ArrayList<LexNode>();
			for (SearchPath p : expanded)
				patterns.add(new Pattern(p, false));

			return patterns;
		}

		public boolean matches(List<LexNode> list, int k) {
			for (int a = k; a < pieces.size() + k; a++) {
				if (a >= list.size())
					return false;

				if (!pieces.get(a - k).matches(list, a))
					return false;
			}
			return true;
		}

		public int matchFirst(SearchPath path) {
			int currentIndex = 0;
			// TODO: this method was changed to index free input.
			for (int k = 0; k < path.size(); k++) {
				if (pieces.get(currentIndex).matches(path, k)) {
					if (currentIndex == pieces.size() - 1)
						return k - pieces.size() + 1;
					currentIndex++;

				} else
					currentIndex = 0;
			}

			return -1;
		}

		public void actualize() {
			if (name.equals("TEMPORARY"))
				this.name = "P" + (++NPATT);
		}

		/**
		 * Equals method that will rely on the equals method of its contents. This will eventually
		 * filter down to relying on equality of LexNode.Leafs.
		 */
		public boolean equals(Object o) {
			if (! (o instanceof LexNode.Pattern))
				return false;
			LexNode.Pattern temp = (LexNode.Pattern) o;
			return pieces.equals(temp.pieces);
		}
	}

	public static class Leaf extends LexNode {

		public Leaf(String s) {
			this.name = s;
		}

		public boolean matches(List<LexNode> list, int k) {
			return list.get(k) == this;
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
