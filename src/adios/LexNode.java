
package adios;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public abstract class LexNode {
	public String name;

	public abstract ArrayList<LexNode> expand();

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

		public ArrayList<LexNode> expand() {
			ArrayList<LexNode> nodes = new ArrayList<LexNode>();

			Iterator<LexNode> temp = pieces.iterator();
			while (temp.hasNext())
				nodes.addAll(temp.next().expand());

			return nodes;
		}
		
		/**
		 * Equals method that will rely on the equals method of its contents.
		 * This will eventually filter down to relying on equality of LexNode.Leafs. 
		 */
		public boolean equals(Object o) {
			if (!(o instanceof LexNode.Equivalence))
				return false;
			LexNode.Equivalence temp = (LexNode.Equivalence) o;
			return temp.pieces.equals(pieces);
		}
	}

	public static class Pattern extends LexNode {
		private static int NPATT = 0; // number of equivalence ndoes so far; use for naming.
		public ArrayList<LexNode> pieces; // need all of the pieces together in order

		public Pattern(ArrayList<LexNode> a) {
			this.pieces = a;
			this.name = "P" + (++NPATT);
		}
		
		/**
		 * Necessary for CandidatePattern.
		 */
		public Pattern() {}
		
		/**
		 * Convert a CandidatePattern into a full Pattern.
		 * @param temp
		 */
		public Pattern(CandidatePattern temp) {
			this(temp.pieces);
		}

		public ArrayList<LexNode> expand() {
			ArrayList<SearchPath> expanded = SearchPath.expandAll(pieces);

			ArrayList<LexNode> patterns = new ArrayList<LexNode>();
			for (SearchPath p : expanded)
				patterns.add(new Pattern(p));

			return patterns;
		} 
		
		/**
		 * Equals method that will rely on the equals method of its contents.
		 * This will eventually filter down to relying on equality of LexNode.Leafs. 
		 */
		public boolean equals(Object o) {
			if (!(o instanceof LexNode.Pattern))
				return false;
			LexNode.Pattern temp = (LexNode.Pattern) o; 
			return pieces.equals(temp.pieces);
		}
		
		public static class CandidatePattern extends Pattern {
			public CandidatePattern(ArrayList<LexNode> a) {
				super();
				this.pieces = a;
			}
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
