package adios;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class Tests {

	public static void main(String[] args) {
		String s1 = "The dog is cute.";
		String s2 = "The cat is cute.";
		SearchPath sp1; SearchPath sp2;
		LexGraph g;
		
		sp1 = SearchPath.fromSentence(s1);
		sp2 = SearchPath.fromSentence(s2);
		g = new LexGraph(s1 + s2);
		for (int i = 0; i < sp1.size(); i++)
			for (int j = i; j < sp1.size(); j++)
				System.out.printf("(%d, %d): %f", i, j, g.P(sp1, i, j, true));
		assert(false);
	}

}
