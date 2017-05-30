package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.ext.com.google.common.collect.HashMultiset;
import org.apache.jena.ext.com.google.common.collect.Multiset;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpWalker;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;

public class ParseQuery {
	
	String qString = "";
	String[] queries;
	String aString;
	String[] aQueries;
	Query query;
	Model model;
	BufferedReader bf;
	Set<Triple> tripleSet = new HashSet<Triple>();
	Map<Integer,Set<Triple>> tripleMap = new HashMap<Integer,Set<Triple>>();
	Map<Integer,List<Var>> varMap = new HashMap<Integer,List<Var>>();
	CustomOpVisitor visitor = new CustomOpVisitor();
	List<ExpandedGraph> graphList = new ArrayList<ExpandedGraph>();
	Multiset<String> uQ = HashMultiset.create();
	int unsupportedQueries = 0;
	int badSyntaxQueries = 0;
	
	public void parse(){
		this.aQueries = new String[this.queries.length]; //This is very ugly, I know.
		for (int i = 0; i < this.queries.length; i++){
			try{
				parseQuery(this.queries[i]);
			}
			catch (UnsupportedOperationException e){
				unsupportedQueries++;
				uQ.add(e.getMessage());
			}
			catch(QueryParseException e){
				badSyntaxQueries++;
			}
			if (i % 100 == 0){
				System.out.println((i+1)+" queries read.");
			}
		}
	}
	
	public void parseQuery(String q) throws UnsupportedOperationException, QueryParseException{
		Query query = QueryFactory.create(q);
		Op op = Algebra.compile(query);
		visitor = new CustomOpVisitor(query.getProjectVars());
		OpWalker.walk(op, visitor);
		graphList.add(visitor.getResult());
	}
	
	public void parseTriple(int i){
		Query query = QueryFactory.create(this.queries[i]);
		Op op = Algebra.compile(query);
		aQueries[i] = op.toString();
		tripleMap.put(i, new HashSet<Triple>());
		final Set<Triple> t = tripleMap.get(i);
		varMap.put(i, query.getProjectVars());
		ElementWalker.walk(query.getQueryPattern(),
			// Thanks, StackOverflow...
		    // For each element...
		    new ElementVisitorBase() {
		        // ...when it's a block of triples...
		        public void visit(ElementPathBlock el) {
		            // ...go through all the triples...
		            Iterator<TriplePath> triples = el.patternElts();
		            while (triples.hasNext()) {
		                // ...and grab the subject // I want the triples though
		            	t.add(triples.next().asTriple());
		            }
		        }
		    }
		);
	}
	
	public ParseQuery(File f, String delim) throws IOException{
		String s, w = "";
		try {
			bf = new BufferedReader(new FileReader(f));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		while ((s = bf.readLine())!=null){
			w += s + " ";
		}
		String[] e = w.trim().split(delim);
		this.queries = Arrays.copyOfRange(e, 1, e.length);
		this.parse();
	}
	
	public ParseQuery(File f, int upTo) throws IOException{
		String s, w = "";
		int i = 0;
		try {
			bf = new BufferedReader(new FileReader(f));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		while ((s = bf.readLine())!=null){
			w += s.substring(1, s.length()-1) + "\n";
			if (i % 1000 == 0 && i > 0){
				System.out.println("Read "+i+" lines.");
			}
			if (i == upTo){
				break;
			}
			i++;
		}
		String[] e = w.trim().split("\n");
		this.queries = Arrays.copyOfRange(e, 1, e.length);
		System.out.println("About to parse");
		this.parse();
	}
	
	public void execQuery(Query query, Model model, boolean b){	
		try (QueryExecution qexec = QueryExecutionFactory.create(query,model)){
			ResultSet results = qexec.execSelect();
			while(results.hasNext()){
				QuerySolution soln = results.nextSolution();
				if (b){
					System.out.println(soln.toString());
				}
			}
			qexec.close();
		}
	}
	
	public List<ExpandedGraph> getGraphList(){
		return this.graphList;
	}
	
	public Query parseQueryFromFile(String file) throws IOException{
		String s, w = "";
		File f = new File(file);
		try {
			bf = new BufferedReader(new FileReader(f));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		while ((s = bf.readLine())!=null){
			w += s + " ";
		}
		return QueryFactory.create(w);
	}
	
	public String unsupportedFeaturesToString(){
		String output = "";
		for (Multiset.Entry<String> f : this.uQ.entrySet()){
			output += (f.getElement() + " : " + f.getCount())+"\n";
		}
		return output;
	}
	
	public void printUnsupportedFeatures(){
		System.out.println(this.unsupportedFeaturesToString());
	}
	
	public static void main(String[] args){
		File f = new File("testFiles/test09.txt");
		try {
			ParseQuery q = new ParseQuery(f, "\\s*=+\\s*");
			for (int i = 0; i < q.queries.length; i++){
//				for (Triple t : q.tripleMap.get(i)){
//					System.out.println(t);
//				}
				q.parseQuery(q.queries[i]);
				q.visitor.getResult().print();
				q.visitor.getResult().update();
				System.out.println("");
				q.visitor.getResult().print();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
