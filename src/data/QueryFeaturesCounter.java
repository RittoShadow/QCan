 package data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.jena.ext.com.google.common.collect.HashMultiset;
import org.apache.jena.ext.com.google.common.collect.Multiset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpWalker;

public class QueryFeaturesCounter {
	
	String queryInfo = "";
	String distInfo = "";
	BufferedReader bf;
	File file;
	FileWriter fw;
	BufferedWriter bw;
	Multiset<String> features = HashMultiset.create();
	public ArrayList<String> canonQueries = new ArrayList<String>();
	long totalTime = 0;
	int totalQueries = 0;
	int supportedQueries = 0;
	int unsupportedQueries = 0;
	int badSyntaxQueries = 0;
	int otherUnspecifiedExceptions = 0;
	int numberOfDuplicates = 0;
	
	public void parse(String s) throws Exception{
		Query q = QueryFactory.create(s);
		Op op = Algebra.compile(q);
		if (!q.getGraphURIs().isEmpty() || !q.getNamedGraphURIs().isEmpty()){
			this.features.add("named");
		}	
		FeatureCounter fc = new FeatureCounter(op);
		OpWalker.walk(op, fc);
		HashSet<String> features = fc.getFeatures();
		for (String f : features){
			this.features.add(f);
		}
		
		this.features.addAll(fc.getPathFeatures());
		boolean unions = features.contains("union");
		boolean joins = features.contains("join");
		boolean distinct = features.contains("distinct");
		boolean unsupported = features.contains("Unsupported");
		boolean paths = features.contains("path");
		
		features.remove("union");
		features.remove("join");
		features.remove("distinct");
		features.remove("bgp");
		features.remove("project");
		features.remove("Unsupported");
		
		boolean others = !features.isEmpty();
		String ans = "";
		
		if (unions && !unsupported){
			ans += "U";
			if (joins){
				ans += "J";
			}
			if (distinct){
				ans += "D";
			}
			if (paths) {
				ans += "P";
			}
			if (others){
				ans += "*";
			}
		}
		if (fc.isCQ()) {
			this.features.add("CQ");
		}
		if (fc.isUCQ()) {
			this.features.add("UCQ");
		}
		if (fc.isC2RPQ()) {
			this.features.add("C2RPQ");
		}
		if (fc.isUC2RPQ()) {
			this.features.add("UC2RPQ");
		}
		if (fc.isMonotone()) {
			this.features.add("monotone");
		}
		if (fc.isM2RPQ()) {
			this.features.add("M2RPQ");
		}
		this.features.add(ans);
	}
	
	public QueryFeaturesCounter(File f) throws IOException {
		new QueryFeaturesCounter(f, true);
	}
	
	public QueryFeaturesCounter(File f, boolean parse) throws IOException{
		String s;
		this.file = new File("resultFiles/features/result"+new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime())+".log");
		if (!this.file.exists()){
			this.file.createNewFile();
		}
		fw = new FileWriter(this.file, true);
		bw = new BufferedWriter(fw);
		try {
			bf = new BufferedReader(new FileReader(f));
			long t = System.currentTimeMillis();
			while ((s = bf.readLine())!=null){
				if (parse) {
					try{
						this.parse(s);
					}
					catch (UnsupportedOperationException e){
						unsupportedQueries++;
					}
					catch(QueryParseException e){
						badSyntaxQueries++;
					} 
					catch (Exception e) {
						otherUnspecifiedExceptions++;
					}
				}		
				totalQueries++;
			}
			this.totalTime = System.currentTimeMillis() - t;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}		
		for (String h : features.elementSet()){
			bw.append(h + ": "+features.count(h));
			bw.newLine();
		}
		bw.append("query parse exception: " + badSyntaxQueries);
		bw.newLine();
		bw.append("unspecified exceptions: " + otherUnspecifiedExceptions);
		bw.newLine();
		bw.append("total: " + totalQueries);
		bw.close();
	}
	
	public static void combineFiles(Path f) throws IOException {
		final Map<String,Integer> featureMap = new HashMap<String,Integer>();
		FileVisitor<Path> fv = new FileVisitor<Path>(){

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				String s;
				BufferedReader br = new BufferedReader(new FileReader(file.toFile()));
				while((s = br.readLine()) != null) {
					String feature = s.substring(0, s.indexOf(":"));
					if (feature.isEmpty()) {
						continue;
					}
					String number = s.substring(s.indexOf(":") + 2);
					int n = Integer.valueOf(number);
					if (featureMap.containsKey(feature)) {
						featureMap.put(feature, featureMap.get(feature) + n);
					}
					else {
						featureMap.put(feature, n);
					}
				}
				br.close();
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				return FileVisitResult.TERMINATE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}
			
		};
		Files.walkFileTree(f, fv);
		System.out.println(featureMap);
	}
	
	public boolean equalQueries(int x, int y){
		return canonQueries.get(x).equals(canonQueries.get(y));
	}
	
	@SuppressWarnings("unused")
	public static void main(String[] args) throws IOException{
//		for (int i = 100; i < 183; i++) {
//			String n = String.valueOf(i).substring(1);
//			QueryFeaturesCounter qp = new QueryFeaturesCounter(new File("testFiles/utf8WikiDataQueries/utfWikiData"+n), true);
//		}
		QueryFeaturesCounter.combineFiles(new File("resultFiles/features").toPath());
	}		
}
