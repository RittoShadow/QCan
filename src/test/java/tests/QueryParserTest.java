package tests;

import cl.uchile.dcc.builder.QueryBuilder;
import cl.uchile.dcc.main.SingleQuery;
import org.apache.jena.ext.com.google.common.collect.HashMultiset;
import org.apache.jena.ext.com.google.common.collect.Multiset;
import org.apache.jena.query.QueryParseException;

import java.io.*;
import java.util.ArrayList;

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
	protected boolean pathNormalisation = true;
	private boolean enableRewrite = true;
	
	public void parse(String s) throws Exception{
		try{
		SingleQuery q = new SingleQuery(s, enableCanonical, enableRewrite, enableLeaning, pathNormalisation, false );
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

	public boolean compare(String s) throws Exception {
		SingleQuery q1 = new SingleQuery(s, enableCanonical, enableRewrite, enableLeaning, pathNormalisation, false );
		SingleQuery q2 = new SingleQuery(s, enableCanonical, enableRewrite, false, pathNormalisation, false);
		boolean ans = q1.getQuery().equals(q2.getQuery());
		//ans = ans && (q1.getCanonicalGraph().graph.size() > q2.getCanonicalGraph().graph.size());
		return ans;
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
					System.out.println(s);
				}
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
			i++;
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
		QueryParserTest qp = new QueryParserTest(new File("clean_dbPediaQueries.txt"), true);
	}
}
