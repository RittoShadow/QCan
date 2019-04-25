package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.jena.ext.com.google.common.collect.HashMultiset;
import org.apache.jena.ext.com.google.common.collect.Multiset;
import org.apache.jena.ext.com.google.common.collect.TreeMultiset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpNull;

public class JenaParser {
	
	String queryInfo = "";
	String distInfo = "";
	BufferedReader bf;
	FileWriter fw;
	BufferedWriter bw;
	Multiset<String> uQ = HashMultiset.create();
	public Multiset<String> canonQueries = TreeMultiset.create();
	long totalTime = 0;
	int totalQueries = 0;
	int supportedQueries = 0;
	int unsupportedQueries = 0;
	int badSyntaxQueries = 0;
	int otherUnspecifiedExceptions = 0;
	boolean enableFilter = true;
	boolean enableOptional = true;
	int numberOfDuplicates = 0;
	
	public void parse(String s) throws Exception{
		long t = System.nanoTime();
		Query query = QueryFactory.create(s);
		Op op = Algebra.compile(query);
		t = System.nanoTime() - t;
		if (op instanceof OpNull){
			return;
		}
		queryInfo = totalQueries + "\t" + t + "\t";
		queryInfo += query.getResultVars().size() + "\t";
		queryInfo += query.isDistinct() + "\t";
		canonQueries.add(op.toString());
		bw.append(queryInfo);
		bw.newLine();
		supportedQueries++;
	}
	
	public JenaParser(File f, File out, int upTo, boolean enableFilter, boolean enableOptional) throws IOException{
		String s;
		int i = 0;
		this.enableFilter = enableFilter;
		this.enableOptional = enableOptional;
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
						Query query = QueryFactory.create(s);
						@SuppressWarnings("unused")
						Op op = Algebra.compile(query);
					}
					this.parse(s);
				}
				catch (UnsupportedOperationException e){
					unsupportedQueries++;
					uQ.add(e.getMessage());
				}
				catch(QueryParseException e){
					badSyntaxQueries++;
				} 
				catch (Exception e) {
					otherUnspecifiedExceptions++;
				}
				totalQueries++;
				i++;
			}
			this.totalTime = System.currentTimeMillis() - t;
			bw.write(getQueryInfo());
			bw.close();
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
		output += "Number of unique queries: " + this.canonQueries.size() + "\n";
		output += "Number of queries with unsupported features: "+this.unsupportedQueries + "\n";
		output += "Number of queries with unspecified exceptions: "+this.otherUnspecifiedExceptions + "\n";
		output += "Summary of unsupported features: \n" + unsupportedFeaturesToString() + "\n";	
		output += "Total elapsed time (in milliseconds) : " + this.totalTime;
		return output;
	}
	
	public void printUnsupportedFeatures(){
		System.out.println(this.unsupportedFeaturesToString());
	}
	
	public static void main(String[] args) throws IOException{
		JenaParser qp = new JenaParser(new File("testFiles/filterTest1"), new File("resultFiles/filterTest"), -1, true, false);
		qp.getDistributionInfo();
	}
}
