package tests;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import builder.QueryBuilder;
import main.SingleQuery;
import org.apache.jena.ext.com.google.common.collect.HashMultiset;
import org.apache.jena.ext.com.google.common.collect.Multiset;
import org.apache.jena.query.QueryParseException;

public class QueryParserTest {
	
	String queryInfo = "";
	String distInfo = "";
	BufferedReader bf;
	FileWriter fw;
	BufferedWriter bw;
	Multiset<String> uQ = HashMultiset.create();
	public Map<String, ArrayList<Integer>> parts0 = new HashMap<>();
	public Map<String, ArrayList<Integer>> parts1 = new HashMap<>();
	public ArrayList<String> canonQueries = new ArrayList<String>();
	long totalTime = 0;
	int totalQueries = 0;
	int supportedQueries = 0;
	int unsupportedQueries = 0;
	int badSyntaxQueries = 0;
	int otherUnspecifiedExceptions = 0;
	int numberOfDuplicates = 0;
	boolean enableFilter = true;
	boolean enableOptional = true;
	boolean enableCanonical = true;
	boolean enableLeaning = true;
	protected boolean pathNormalisation = true;
	private boolean enableRewrite = true;
	
	public void parse(String s) throws Exception{
		try{
		SingleQuery q = new SingleQuery(s, enableCanonical, enableRewrite, enableLeaning, pathNormalisation, false );
		canonQueries.add(q.getQuery());
//		q.getOriginalGraph().print();
//		System.out.println("");
//		q.getCanonicalGraph().print();
		QueryBuilder qb = new QueryBuilder(q.getCanonicalGraph());
		System.out.println(qb.getQuery());
		}
		catch (Exception e){
			e.printStackTrace();
		}	
	}

	public void parseAndSort(String s) {
		try{
			SingleQuery q = new SingleQuery(s, enableCanonical,enableRewrite, false, pathNormalisation, false );
			String query = q.getQuery();
			if (parts0.containsKey(query)) {
				ArrayList<Integer> current = parts0.get(query);
				current.add(totalQueries);
				parts0.put(query,current);
			}
			else{
				ArrayList<Integer> current = new ArrayList<>();
				current.add(totalQueries);
				parts0.put(query, current);
			}
			q = new SingleQuery(s, enableCanonical, enableRewrite, enableCanonical, pathNormalisation, false);
			query = q.getQuery();
			if (parts1.containsKey(query)) {
				ArrayList<Integer> current = parts1.get(query);
				current.add(totalQueries);
				parts1.put(query,current);
			}
			else{
				ArrayList<Integer> current = new ArrayList<>();
				current.add(totalQueries);
				parts1.put(query, current);
			}
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}

	public void readAndSort(File f) throws IOException {
		String s;
		int i = 0;
		bf = new BufferedReader(new FileReader(f));
		while ((s = bf.readLine())!=null){
			try{
				parseAndSort(s);
			}
			catch (UnsupportedOperationException e){
				unsupportedQueries++;
				uQ.add(e.getMessage());
			}
			catch(QueryParseException e){
				badSyntaxQueries++;
				e.printStackTrace();
			}
			catch (Exception e) {
				otherUnspecifiedExceptions++;
				e.printStackTrace();
			}
			totalQueries++;
			if (totalQueries%100 == 0) {
				System.out.println(totalQueries);
			}
		}
		Collection<ArrayList<Integer>> entries0 = parts0.values();
		Collection<ArrayList<Integer>> entries1 = parts1.values();
		System.out.println("In 0:");
		for (List<Integer> entry0 : entries0) {
			if (!entries1.contains(entry0)) {
				System.out.println(entry0);
			}
		}
		System.out.println("In 1:");
		for (List<Integer> entry1 : entries1) {
			if (!entries0.contains(entry1)) {
				System.out.println(entry1);
			}
		}
	}

	public boolean compare(String s) throws Exception {
		SingleQuery q1 = new SingleQuery(s, enableCanonical, enableRewrite, false, pathNormalisation, false );
		SingleQuery q2 = new SingleQuery(s, enableCanonical, false, false, pathNormalisation, false);
		return q1.getQuery().equals(q2.getQuery());
	}
	
	public QueryParserTest(File f) throws IOException{
		this.enableLeaning(true);
		this.enableCanonicalisation(true);
		this.pathNormalisation = true;
		this.enableRewrite = true;
		try {
			read(f);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}		
	}

	public QueryParserTest(File f, boolean test) throws IOException {
		if (test) {
			this.enableLeaning = true;
			this.enableCanonical = true;
			this.enableRewrite = true;
			this.pathNormalisation = true;
			readAndCompare(f);
		}
		else {
			readAndSort(f);
		}
	}

	public void read(File f) throws IOException {
		String s;
		int i = 0;
		bf = new BufferedReader(new FileReader(f));
		long t = System.currentTimeMillis();
		while ((s = bf.readLine())!=null){
			try{
				System.out.println(i++);
				this.parse(s);
			}
			catch (UnsupportedOperationException e){
				unsupportedQueries++;
				uQ.add(e.getMessage());
			}
			catch(QueryParseException e){
				badSyntaxQueries++;
				e.printStackTrace();
			}
			catch (Exception e) {
				otherUnspecifiedExceptions++;
				e.printStackTrace();
			}
			totalQueries++;
		}
		this.totalTime = System.currentTimeMillis() - t;

	}

	public void readAndCompare(File f) throws IOException {
		String s;
		int i = 0;
		bf = new BufferedReader(new FileReader(f));
		long t = System.currentTimeMillis();
		while ((s = bf.readLine())!=null){
			try{
				if (!compare(s)) {
					System.out.println(i);
				}
				i++;
			}
			catch (UnsupportedOperationException e){
				unsupportedQueries++;
				uQ.add(e.getMessage());
			}
			catch(QueryParseException e){
				badSyntaxQueries++;
				e.printStackTrace();
			}
			catch (Exception e) {
				otherUnspecifiedExceptions++;
				e.printStackTrace();
			}
			totalQueries++;
		}
		this.totalTime = System.currentTimeMillis() - t;
	}
	
	public String unsupportedFeaturesToString(){
		String output = "";
		for (Multiset.Entry<String> f : this.uQ.entrySet()){
			output += (f.getElement() + " : " + f.getCount())+"\n";
		}
		return output;
	}
	
	public void enableCanonicalisation(boolean b){
		this.enableCanonical = b;
	}
	
	public void enableLeaning(boolean b){
		this.enableLeaning = b;
	}
	
	public void printUnsupportedFeatures(){
		System.out.println(this.unsupportedFeaturesToString());
	}
	
	public boolean equalQueries(int x, int y){
		return canonQueries.get(x).equals(canonQueries.get(y));
	}
	
	@SuppressWarnings("unused")
	public static void main(String[] args) throws IOException{
		QueryParserTest qp = new QueryParserTest(new File("clean_RKBExplorerQueries.txt"),false);
	}
}
