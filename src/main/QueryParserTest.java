package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

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
	
	public void parse(String s) throws Exception{
		try{
		SingleQuery q = new SingleQuery(s, enableCanonical, enableLeaning, true);
		canonQueries.add(q.getQuery());
		q.getOriginalGraph().print();
		System.out.println("");
		q.getCanonicalGraph().print();
		QueryBuilder qb = new QueryBuilder(q.getCanonicalGraph());
		System.out.println(qb.getQuery());
		}
		catch (Exception e){
			e.printStackTrace();
		}	
	}
	
	public QueryParserTest(File f) throws IOException{
		String s;
		int i = 0;
		this.enableFilter = true;
		this.enableOptional = true;
		this.enableLeaning(true);
		this.enableCanonicalisation(true);
		try {
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
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}		
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
		QueryParserTest qp = new QueryParserTest(new File("testFiles/filterTest5.txt"));
	}
}
