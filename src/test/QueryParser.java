package test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import org.apache.jena.ext.com.google.common.collect.HashMultiset;
import org.apache.jena.ext.com.google.common.collect.Multiset;
import org.apache.jena.ext.com.google.common.collect.TreeMultiset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;

public class QueryParser {
	
	String queryInfo = "";
	String distInfo = "";
	BufferedReader bf;
	FileWriter fw;
	BufferedWriter bw;
	Multiset<String> uQ = HashMultiset.create();
	public Multiset<String> canonQueries = TreeMultiset.create();
	public ArrayList<String> unsupportedQueriesList = new ArrayList<String>();
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
		SingleQuery q = new SingleQuery(s, enableFilter, enableOptional, enableCanonical, enableLeaning);
		queryInfo = totalQueries + "\t" + q.getGraphCreationTime() + "\t";
		queryInfo += q.getCanonicalisationTime() + "\t";
		queryInfo += q.getInitialTriples() + "\t";
		queryInfo += q.triplePatternsIn() + "\t";
		queryInfo += q.getVarsIn() + "\t";
		queryInfo += q.triplePatternsOut() + "\t";
		queryInfo += q.getVarsOut() + "\t";
		queryInfo += q.graphSizeIn() + "\t";
		queryInfo += q.graphSizeOut() + "\t";
		queryInfo += q.isDistinct() + "\t";
		queryInfo += q.hasJoin() + "\t";
		queryInfo += q.hasUnion() + "\t";
		queryInfo += q.getContainsOptional() + "\t";
		queryInfo += q.getContainsFilter() + "\t";
		queryInfo += q.getContainsNamedGraphs() + "\t";
		queryInfo += q.getContainsSolutionMods() + "\t";
		canonQueries.add(q.getQuery());
		bw.append(queryInfo);
		bw.newLine();
		supportedQueries++;
	}
	
	public QueryParser(File f, File out, int upTo, boolean enableFilter, boolean enableOptional, boolean enableLeaning, boolean enableCanon) throws IOException{
		String s;
		int i = 0;
		this.enableFilter = enableFilter;
		this.enableOptional = enableOptional;
		this.enableLeaning(enableLeaning);
		this.enableCanonicalisation(enableCanon);
		try {
			bf = new BufferedReader(new FileReader(f));
			fw = new FileWriter(out);
			bw = new BufferedWriter(fw);
			long t = System.currentTimeMillis();
			while ((s = bf.readLine())!=null){
				if (i == upTo){
					break;
				}
				try{
					if (i == 0){
						@SuppressWarnings("unused")
						SingleQuery q = new SingleQuery(s, enableFilter, enableOptional, enableCanonical, enableLeaning);
					}
					this.parse(s);
				}
				catch (UnsupportedOperationException e){
					unsupportedQueries++;
					uQ.add(e.getMessage());
				}
				catch(QueryParseException e){
					badSyntaxQueries++;
					unsupportedQueriesList.add("Bad syntax: "+s);
				} 
				catch (Exception e) {
					otherUnspecifiedExceptions++;
					unsupportedQueriesList.add(s + ":" +e.getMessage());
				}
				totalQueries++;
				i++;
			}
			this.totalTime = System.currentTimeMillis() - t;
			bw.write(getQueryInfo());
			bw.close();
			this.outputUnsupportedQueries();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}		
	}
	
	public static void removeQueries(String path) throws IOException{
		String s;
		try {
			File f = new File(path);
			BufferedReader bf = new BufferedReader(new FileReader(f));
			File out = new File("clean_" + f.getPath());
			if (!out.exists()){
				out.createNewFile();
			}
			FileWriter fw = new FileWriter(out);
			BufferedWriter bw = new BufferedWriter(fw);
			while ((s = bf.readLine())!=null){
				try{
					String w = s.substring(s.indexOf('"')+1, s.lastIndexOf('"'));
					QueryFactory.create(w);
					bw.append(w);
					bw.newLine();
				}
				catch (Exception e){
					continue;
				}
			}
			bf.close();
			bw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
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
	
	public void enableCanonicalisation(boolean b){
		this.enableCanonical = b;
	}
	
	public void enableLeaning(boolean b){
		this.enableLeaning = b;
	}
	
	public void getDistributionInfo() throws IOException{
		int i = 0;
		int max = 0;
		File file = new File("resultFiles/dist"+new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime())+".log");
		FileWriter fw = new FileWriter(file);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.append("Distribution of canonicalised queries: \n");
		bw.newLine();
		for (Multiset.Entry<String> f : canonQueries.entrySet()){
			i++;
			bw.append((f.getElement() + " : " + f.getCount()) + "\n");
			bw.newLine();
			numberOfDuplicates = numberOfDuplicates + (f.getCount() - 1);
			if (f.getCount() > max){
				max = f.getCount();
			}
			if (i%1000 == 0){
				System.out.println(i+" queries added.");
			}
		}
		bw.append("Total number of duplicates detected: "+numberOfDuplicates);
		bw.append("Most duplicates found: "+max);
		bw.close();
	}
	
	public String getQueryInfo(){
		String output = "";
		output += "Total number of queries: " + this.totalQueries + "\n";
		output += "Number of canonicalised queries: " + this.supportedQueries + "\n";
		output += "Number of unique queries: " + this.canonQueries.entrySet().size() + "\n";
		output += "Number of queries with unsupported features: "+this.unsupportedQueries + "\n";
		output += "Number of queries with unspecified exceptions: "+this.otherUnspecifiedExceptions + "\n";
		output += "Number of queries with bad syntax: "+this.badSyntaxQueries + "\n";
		output += "Summary of unsupported features: \n" + unsupportedFeaturesToString() + "\n";	
		output += "Total elapsed time (in milliseconds) : " + this.totalTime;
		return output;
	}
	
	public void printUnsupportedFeatures(){
		System.out.println(this.unsupportedFeaturesToString());
	}
	
	public void outputUnsupportedQueries() throws IOException{
		if (!unsupportedQueriesList.isEmpty()){
			FileWriter fw = new FileWriter(new File("resultFiles/unsupported"+new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime())+".log"));
			BufferedWriter bw = new BufferedWriter(fw);
			for (String s : unsupportedQueriesList){
				bw.append(s);
				bw.newLine();
			}
			bw.close();
		}
	}
	
	public static void main(String[] args) throws IOException{
		@SuppressWarnings("unused")
		QueryParser qp = new QueryParser(new File("testFiles/unsupported20171201_224715.log"), new File("resultFiles/unsupported20171201_224715.log"),-1,true,true,true,true);
	}
}
