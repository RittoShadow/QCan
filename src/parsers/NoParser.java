package parsers;

import org.apache.jena.ext.com.google.common.collect.HashMultiset;
import org.apache.jena.ext.com.google.common.collect.Multiset;
import org.apache.jena.ext.com.google.common.collect.TreeMultiset;
import org.apache.jena.query.QueryParseException;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class NoParser {

	BufferedReader bf;
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

	public NoParser(File f, int upTo) throws IOException{
		String s;
		int i = 0;
		try {
			bf = new BufferedReader(new FileReader(f));
			long t = System.currentTimeMillis();
			while ((s = bf.readLine())!=null){
				if (i == upTo){
					break;
				}
				try{
					canonQueries.add(s);
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
	
	public void getDistributionInfo(String filename) throws IOException{
		int i = 0;
		int max = 0;
		if (filename.contains("/")) {
			filename = filename.substring(filename.lastIndexOf("/"));
		}
		if (filename.contains(".")) {
			filename = filename.substring(0,filename.lastIndexOf("."));
		}
		File file = new File("resultFiles/default/" + filename + "_dist"+new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime())+".log");
		file.createNewFile();
		FileWriter fw = new FileWriter(file);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.append("Distribution of queries: \n");
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
		String filename = "clean_dbPediaQueries.txt";
		NoParser np = new NoParser(new File(filename), -1);
		np.getDistributionInfo(filename);
	}
}
