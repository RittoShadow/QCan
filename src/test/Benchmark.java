package test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.jena.ext.com.google.common.collect.HashMultiset;
import org.apache.jena.ext.com.google.common.collect.Multiset;

import cl.uchile.dcc.blabel.label.GraphColouring.HashCollisionException;

public class Benchmark {
	public File f;
	public int queriesRead;
	public int queriesCanon;
	public int unsupportedQueries;
	public int badSyntaxQueries;
	public ParseQuery pq;
	public Multiset<String> canonQueries = HashMultiset.create();
	
	public Benchmark(String path) throws IOException{
		f = new File(path);
	}
	
	public void execute(int upTo) throws IOException, InterruptedException, HashCollisionException{
		pq = new ParseQuery(f, upTo);
		String output = "";
		List<ExpandedGraph> graphs = pq.getGraphList();
		queriesCanon = graphs.size();
		unsupportedQueries = pq.unsupportedQueries;
		badSyntaxQueries = pq.badSyntaxQueries;
		output += "total queries = "+upTo+"\n";
		output += "queries converted = "+queriesCanon+"\n";
		output += "unsupported queries = "+(unsupportedQueries)+"\n";
		output += "queries with bad syntax = "+(badSyntaxQueries)+"\n";
		output += "unsupported queries summary: \n"+pq.unsupportedFeaturesToString();
		for (ExpandedGraph e : pq.getGraphList()){
			ExpandedGraph e1 = e.getCanonicalForm(false);
			try{
			canonQueries.add(e1.getQuery());
			}
			catch (NoSuchElementException n){

			}
		}
		output += "Canonicalised queries summary: \n";
		for (Multiset.Entry<String> f : canonQueries.entrySet()){
			output += (f.getElement() + " : " + f.getCount()) + "\n";
		}
		writeToFile("resultFiles/results"+new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime())+".log", output);
	}
	
	public void writeToFile(String path, String content){
		File file = new File(path);
		try (FileOutputStream fop = new FileOutputStream(file)) {

			// if file doesn't exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}

			// get the content in bytes
			byte[] contentInBytes = content.getBytes();

			fop.write(contentInBytes);
			fop.flush();
			fop.close();

			System.out.println("Done");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws IOException, InterruptedException, HashCollisionException{
		Benchmark b = new Benchmark(args[0]);
		int upTo = new Integer(args[1]);
		b.execute(upTo);
	}

}
