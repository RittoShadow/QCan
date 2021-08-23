package cl.uchile.dcc.tools;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.*;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.util.iterator.ExtendedIterator;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Utils {

	public static void printGraph(Graph g){
		ExtendedIterator<Triple> e = GraphUtil.findAll(g);
		while (e.hasNext()){
			System.out.println(e.next() + " .");
		}
		System.out.println();
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
				query = URLDecoder.decode(query, String.valueOf(StandardCharsets.UTF_8));
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

	public static <T> Set<Set<T>> powerSet(Set<T> originalSet) {
		Set<Set<T>> sets = new HashSet<>();
		if (originalSet.isEmpty()) {
			sets.add(new HashSet<>());
			return sets;
		}
		List<T> list = new ArrayList<>(originalSet);
		T head = list.get(0);
		Set<T> rest = new HashSet<>(list.subList(1, list.size()));
		for (Set<T> set : powerSet(rest)) {
			Set<T> newSet = new HashSet<>();
			newSet.add(head);
			newSet.addAll(set);
			sets.add(newSet);
			sets.add(set);
		}
		return sets;
	}

	/**
	 * @param t A triple pattern.
	 * @return A triple where all variable nodes have been replaced with blank nodes.
	 */
	public static Triple getTripleWithVars(Triple t) {
		Node s = t.getSubject();
		if (s.isBlank()) {
			s = Var.alloc(s.getBlankNodeLabel());
		}
		Node p = t.getPredicate();
		if (p.isBlank()) {
			p = Var.alloc(p.getBlankNodeLabel());
		}
		Node o = t.getObject();
		if (o.isBlank()) {
			o = Var.alloc(o.getBlankNodeLabel());
		}
		return Triple.create(s, p, o);
	}

	/**
	 * @param s An RDF literal with a datatype.
	 * @return A node that represents a literal with a datatype. If no datatype is specified, it is assumed to be a string.
	 */
	public static Node createLiteralWithType(String s) {
		Node ans;
		s = s.replaceAll("\"", "");
		if (s.contains("^^")) {
			ans = NodeFactory.createLiteralByValue(s.substring(0, s.indexOf("^^")), NodeFactory.getType(s.substring(1 + s.lastIndexOf("^")).replaceAll("[<>]", "")));
		} else {
			ans = NodeFactory.createLiteralByValue(s, XSDDatatype.XSDstring);
		}
		return ans;
	}

	/**
	 * @param s A string that represents a SPARQL function.
	 * @return Returns true if the function is ordered (i.e (f expr1 expr2) != (f expr2 expr1)).
	 */
	public static boolean isOrderedFunction(String s) {
		switch (s) {
			case "<":
			case "concat":
			case ">":
			case "<=":
			case ">=":
			case "-":
			case "/":
			case "regex":
			case "if":
			case "in":
			case "notin":
			case "replace":
			case "strdt":
			case "strlang":
			case "strstarts":
			case "strends":
			case "contains":
			case "strbefore":
			case "strafter":
			case "substr":
				return true;
			default:
				return false;
		}
	}

	public static void main(String[] args) throws IOException {
		File out = new File("RKBExplorerQueries.txt");
		if (!out.exists()) {
			out.createNewFile();
		}
		Utils.extractQueries(new File("RKBExplorer.log"), out, "\t", 3, "", "");
	}
}
