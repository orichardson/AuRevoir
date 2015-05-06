package adios;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Demo {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		LexGraph g = new LexGraph(makeSentences());
		g.adios(true);
//		System.out.println(g.l(g.paths.get(5), 1, 2));
//		System.out.println(g.l(g.paths.get(0), 0, 0));
//		System.out.println(g.P(g.paths.get(0), 0, 1, true));
//		System.out.println(g.P(g.paths.get(0), 0, 0, true));
//		System.out.println(g.l_spec());
//		ArrayList<LexNode> temp = new ArrayList<LexNode>();
//		temp.add(g.nodes.get("Cindy"));
//		temp.add(g.nodes.get("believes"));
//		System.out.println(g.computeLeftSignificance(g.paths.get(0), new Pattern(temp, true)));
//		System.out.println(Math.exp(CombinatoricsUtils.logFac(5)));
	}
	
	public static ArrayList<String> makeSentences() throws FileNotFoundException, IOException {
		ArrayList<String> toReturn = new ArrayList<String>();
		
		try (BufferedReader br = new BufferedReader(new FileReader("corpus.txt"))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		       toReturn.add(line.trim());
		    }
		}
		return toReturn;
	}

}
