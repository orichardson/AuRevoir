package adios;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Demo {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		LexGraph g = new LexGraph(basic());
		g.adios(true);
	}
	
	public static ArrayList<String> basic() throws FileNotFoundException, IOException {
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
