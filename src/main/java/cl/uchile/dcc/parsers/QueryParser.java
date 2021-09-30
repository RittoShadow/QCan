package cl.uchile.dcc.parsers;

import cl.uchile.dcc.blabel.label.GraphColouring.HashCollisionException;
import cl.uchile.dcc.data.FeatureCounter;
import cl.uchile.dcc.main.SingleQuery;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.OpWalker;

import java.io.*;

public class QueryParser extends Parser {

	public QueryParser() {
		super("resultFiles/");
	}

	public void parse(final String s) throws Exception{
		System.out.println("Begin parsing.");
		Thread slave = new Thread(new Runnable(){

			@Override
			public void run() {
				try {
					queryInfo = "";
					SingleQuery q = new SingleQuery(s, enableCanonical, enableRewrite, enableLeaning, pathNormalisation, verbose );
					queryInfo = totalQueries + "\t" + q.getGraphCreationTime() + "\t"; //1
					queryInfo += q.getRewriteTime() + "\t"; //2
					queryInfo += q.getLabelTime() + "\t"; //3
					queryInfo += q.getCanonicalisationTime() + "\t"; //4
					queryInfo += q.getInitialTriples() + "\t"; //5
					queryInfo += q.triplePatternsIn() + "\t"; //6
					queryInfo += q.getVarsIn() + "\t"; //7
					queryInfo += q.triplePatternsOut() + "\t"; //8
					queryInfo += q.getVarsOut() + "\t"; //9
					queryInfo += q.graphSizeIn() + "\t"; //10
					queryInfo += q.graphSizeOut() + "\t"; //11
					queryInfo += q.isDistinct() + "\t";
					queryInfo += q.hasJoin() + "\t";
					queryInfo += q.hasUnion() + "\t";
					queryInfo += q.getContainsOptional() + "\t";
					queryInfo += q.getContainsFilter() + "\t";
					queryInfo += q.getContainsNamedGraphs() + "\t";
					queryInfo += q.getContainsSolutionMods() + "\t";
					queryInfo += q.containsBind() + "\t";
					queryInfo += q.containsGroupBy() + "\t";
					queryInfo += q.containsMinus() +"\t";
					queryInfo += q.containsPaths() + "\t";
					queryInfo += q.containsTable();
					if (verbose) {
						System.out.println("Adding to set");
					}
					canonQueries.add(q.getQuery());
					if (verbose) {
						System.out.println("Added to set");
						System.out.println("Writing to file");
					}
				} catch (InterruptedException | HashCollisionException e) {
					interruptedExceptions++;
					unsupportedQueriesList.add(s);
					System.out.println("Timeout");
				}
				catch (UnsupportedOperationException | NullPointerException e){
					unsupportedQueries++;
					unsupportedQueriesList.add(e.getMessage() + ": " + s);
				}
				catch(QueryParseException e){
					badSyntaxQueries++;
					unsupportedQueriesList.add("Bad syntax: "+s);
				} 
				catch (Exception e) {
					otherUnspecifiedExceptions++;
					unsupportedQueriesList.add(s + ":" +e.getMessage());
				}				
			}
			
		});
		slave.start();
		try{
			slave.join(1000*1*60);
			if (!queryInfo.equals("")) {
				System.out.println("Parsing done");
				bw.append(queryInfo);
				bw.newLine();
				bw.flush();
				System.out.println("Flushed");
			}
			supportedQueries++;
		}
		catch(InterruptedException e){
			interruptedExceptions++;
			unsupportedQueriesList.add(s);
			System.out.println("Timeout");
			bw.newLine();
			bw.flush();
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
					Query q = QueryFactory.create(s);
					Op op = Algebra.compile(q);
					q = OpAsQuery.asQuery(op);
					bw.append(s);
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

	public static void removeSPARQL10Queries(String path) throws IOException{
		String s;
		try {
			File f = new File(path);
			BufferedReader bf = new BufferedReader(new FileReader(f));
			File out = new File("sparql11_" + f.getPath());
			if (!out.exists()){
				out.createNewFile();
			}
			FileWriter fw = new FileWriter(out);
			BufferedWriter bw = new BufferedWriter(fw);
			while ((s = bf.readLine())!=null){
				try{
					Query q = QueryFactory.create(s);
					Op op = Algebra.compile(q);
					FeatureCounter fc = new FeatureCounter(op);
					OpWalker.walk(op,fc);
					q = OpAsQuery.asQuery(op);
					boolean paths = fc.getContainsPaths();
					boolean minus = fc.getFeatures().contains("minus");
					boolean extend = fc.getFeatures().contains("extend");
					boolean group = (fc.getFeatures().contains("group"));
					boolean table = (fc.getFeatures().contains("table"));
					if (paths || minus || extend || group || table) {
						bw.append(s);
						bw.newLine();
					}
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
	
	public static void main(String[] args) throws IOException{

	}
}
