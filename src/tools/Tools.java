package tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.util.iterator.ExtendedIterator;

public class Tools {

	public static void printGraph(Graph g){
		ExtendedIterator<Triple> e = GraphUtil.findAll(g);
		while (e.hasNext()){
			System.out.println(e.next() + " .");
		}
	}
	
	public static void extractQueries(File in, File out, String split, int n, String beginAt, String end) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(in));
		BufferedWriter bw = new BufferedWriter(new FileWriter(out));
		String s;
		while ((s = br.readLine()) != null) {
			try {
				String query = s.split(split)[n];
				if (!query.contains(beginAt)) {
					continue;
				}
				if (query.contains(end) && end.length() > 0) {
					query = query.substring(query.indexOf(beginAt) + beginAt.length(), query.indexOf(end));
				}
				else {
					query = query.substring(query.indexOf(beginAt) + beginAt.length());
				}
				query = URLDecoder.decode(query, StandardCharsets.UTF_8);
				query = query.replace("\n", " ").trim();
				query = query.replace("\r", " ").trim();
				Query q = QueryFactory.create(query);
				if (q != null) {
					bw.write(query);
					bw.newLine();
				}
			}
			catch (Exception e) {
			}
		}
		br.close();
		bw.close();
	}
	
	public static void main(String[] args) throws IOException {
		File out = new File("RKBExplorerQueries.txt");
		if (!out.exists()) {
			out.createNewFile();
		}
		Tools.extractQueries(new File("RKBExplorer.log"), out, "\t", 3, "", "");
	}
}
