package adios;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class Tests {
	
	private void test() {}

	public static void main(String[] args) {
		String s1 = "The dog is cute.";
		String s1m = s1.substring(0, s1.length() - 1);
		String s2 = "The cat is cute.";
		String s3 = "I am cute.";
		LexGraph g = new LexGraph(s1 + s2 + s3);
		SearchPath sp1 = g.paths.get(0);
		for (int i = 0; i < sp1.size(); i++) {
			for (int j = i + 1; j < sp1.size(); j++) {
				System.out.printf("(%d, %d): %f\n", i, j, g.P(sp1, i, j, true));
			}
		}
		
		for (int i = 0; i < sp1.size(); i++) {
			for (int j = i + 1; j < sp1.size(); j++) {
				System.out.printf("(%d, %d): %f\n", i, j, g.D(sp1, i, j, true));
			}
		}
	}

}
