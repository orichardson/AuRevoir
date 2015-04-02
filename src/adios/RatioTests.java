package adios;

import static org.junit.Assert.*;

import org.junit.Test;

public class RatioTests {

	String s1 = "The dog is cute";
	String s2 = "The cat is cute";
	SearchPath sp1; SearchPath sp2;
	LexGraph g;
	public void setUp() {
		sp1 = SearchPath.fromSentence(s1);
		sp2 = SearchPath.fromSentence(s2);
		g = new LexGraph();
	}
	
	public void P_R() {
		for (int i = 0; i < sp1.size(); i++)
			for (int j = i; j < sp1.size(); j++)
				System.out.printf("(%d, $d): %f", i, j, g.P(sp1, i, j, true));
		assert(false);
	}
	
}
