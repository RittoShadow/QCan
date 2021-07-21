 package cl.uchile.dcc.data;

import org.apache.jena.ext.com.google.common.collect.HashMultiset;
import org.apache.jena.ext.com.google.common.collect.Multiset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpWalker;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;

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
	
	
	public String pathFeatures(String s) throws Exception {
		String ans = "{ ";
		Query q = QueryFactory.create(s);
		Op op = Algebra.compile(q);
		FeatureCounter fc = new FeatureCounter(op);
		OpWalker.walk(op, fc);
		if (fc.getContainsPaths()) {
			for (String f : fc.getPathStats()) {
				ans += f + "\n";
			}
			ans = ans.substring(0, ans.length() - 1) + "} : " + fc.nPaths;
		}
		else {
			return null;
		}
		return ans;
	}
	public String parsePaths(String s) throws Exception {
		Query q = QueryFactory.create(s);
		Op op = Algebra.compile(q);
		if (!q.getGraphURIs().isEmpty() || !q.getNamedGraphURIs().isEmpty()){
			this.features.add("named");
		}	
		FeatureCounter fc = new FeatureCounter(op);
		OpWalker.walk(op, fc);
		HashSet<String> features = fc.getFeatures();
		if (features.contains("path")) {
			return s;
		}
		else {
			return null;
		}
	}
	
	public void parse(String s) throws Exception{
		Query q = QueryFactory.create(s);
		Op op = Algebra.compile(q);
		if (!q.getGraphURIs().isEmpty() || !q.getNamedGraphURIs().isEmpty()){
			this.features.add("named");
		}
		if (q.getQueryType() == Query.QueryTypeAsk) {
			this.features.add("ask");
		}
		if (q.getQueryType() == Query.QueryTypeConstruct) {
			this.features.add("construct");
		}
		if (q.getQueryType() == Query.QueryTypeDescribe) {
			this.features.add("describe");
		}
		if (q.getQueryType() == Query.QueryTypeSelect) {
			this.features.add("select");
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
	
	public QueryFeaturesCounter(File f, boolean parse, boolean paths) throws IOException {
		if (paths) {
			String s;
			if (parse) {
				this.file = new File("resultFiles/features/paths.paths/wikiPaths"+new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime())+".log");
			}
			else {
				this.file = new File("resultFiles/features/paths.paths/pathFeatures"+new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime())+".log");
			}
			if (!this.file.exists()) {
				this.file.createNewFile();
			}
			fw = new FileWriter(this.file, true);
			bw = new BufferedWriter(fw);
			try {
				bf = new BufferedReader(new FileReader(f));
				while ((s = bf.readLine()) != null) {
					try {
						String ans;
						if (parse) {
							ans = parsePaths(s);
						}
						else {
							ans = pathFeatures(s);
						}
						if (ans != null) {
							bw.append(ans);
							bw.newLine();
						}
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
			}
			catch (FileNotFoundException e) {
				
			}
			bw.close();
		}
		else {
			new QueryFeaturesCounter(f,parse);
		}
	}
	
	public QueryFeaturesCounter(File f, boolean parse) throws IOException{
		String s;
		String filename = f.getName();
		if (filename.contains(".")) {
			filename = filename.substring(0, filename.lastIndexOf("."));
		}
		this.file = new File("resultFiles/features/"+ filename + new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime())+".log");
		if (!this.file.exists()){
			this.file.createNewFile();
		}
		fw = new FileWriter(this.file, true);
		bw = new BufferedWriter(fw);
		try {
			bf = new BufferedReader(new FileReader(f));
			long t = System.currentTimeMillis();
			if (parse){
				while ((s = bf.readLine())!=null) {
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
	
	public static void filterPaths(List<File> in, File out) throws Exception {
		String s;
		FileWriter fw = new FileWriter(out);
		BufferedWriter bw = new BufferedWriter(fw);
		int i = 0;
		int n = 0;
		for (File f : in) {
			BufferedReader br = new BufferedReader(new FileReader(f));
			i++;
			while ((s = br.readLine()) != null) {
				try {
					n++;
					if (n % 10000 == 0) {
						System.out.println(n + " lines read.");
					}
					Query q = QueryFactory.create(s);
					Op op = Algebra.compile(q);
					FeatureCounter fc = new FeatureCounter(op);
					OpWalker.walk(op, fc);
					HashSet<String> features = fc.getFeatures();
					if (features.contains("path")) {
						bw.append(s);
						bw.newLine();
					}
				} catch (Exception e) {
					
				}
			}
			br.close();
			System.out.println(i+ "files read.");
		}
		bw.close();
	}
	
	public static void pathFiles(File f) throws Exception {
		pathFiles(f, -1);
	}
	
	public static void pathFiles(File f, int k) throws Exception {
		String s;
		BufferedReader br = new BufferedReader(new FileReader(f));
		int i = 0;
		int nLines = 0;
		int minNPaths = 0;
		int maxNPaths = 0;
		int unsupported = 0;
		HashMap<String,Integer> instances = new HashMap<String,Integer>();
		HashMap<String,Integer> normalInstances = new HashMap<String,Integer>();
		HashMap<String,Integer> featuresInstances = new HashMap<String,Integer>();
		long lengths = 0;
		long normalLengths = 0;
		long totalNPaths = 0;
		long minTime = 0;
		long maxTime = 0;
		long totalTime = 0;
		int different = 0;
		int minLength = 0, minNormalLength = 0, maxLength = 0, maxNormalLength = 0;
		while ((s = br.readLine()) != null) {
			nLines++;
			if (i == k) {
				break;
			}
			if (s.startsWith("{")) {
				i++;
				s = s.substring(1).trim();
				if (s.contains("}")) {
					int nPaths = Integer.parseInt(s.substring(s.lastIndexOf(":") + 1).trim());
					try {
						if (s.contains("unsupported")) {
							s = s.substring(0, s.indexOf("}")).trim();
						}
						else {
							s = s.substring(0, s.indexOf("}") - 1).trim();
						}
					}
					catch (StringIndexOutOfBoundsException e) {
						continue;
					}
					totalNPaths += nPaths;
					if (minNPaths == 0) {
						minNPaths = nPaths;
					}
					else {
						minNPaths = Math.min(minNPaths, nPaths);
					}
					if (maxNPaths == 0) {
						maxNPaths = nPaths;
					}
					else {
						maxNPaths = Math.max(maxNPaths, nPaths);
					}
				}
				String[] params = s.split("\t");
				if (params.length == 5) {
					String features1 = params[0];
					int size1 = Integer.parseInt(params[1]);
					String features2 = params[2];
					int size2 = Integer.parseInt(params[3]);
					long time = Long.parseLong(params[4]);
					if (instances.containsKey(features1)) {
						instances.put(features1, instances.get(features1) + 1);
					}
					else {
						instances.put(features1, 1);
					}
					if (normalInstances.containsKey(features2)) {
						normalInstances.put(features2, normalInstances.get(features2) + 1);
					}
					else {
						normalInstances.put(features2, 1);
					}
					if (minLength == 0) {
						minLength = size1;
					}
					else {
						minLength = Math.min(minLength, size1);
					}
					if (maxLength == 0) {
						maxLength = size1;
					}
					else {
						maxLength = Math.max(maxLength, size1);
					}
					if (minNormalLength == 0) {
						minNormalLength = size2;
					}
					else {
						minNormalLength = Math.min(minNormalLength, size2);
					}
					if (maxNormalLength == 0) {
						maxNormalLength = size2;
					}
					else {
						maxNormalLength = Math.max(maxNormalLength, size2);
					}
					if (minTime == 0) {
						minTime = time;
					}
					else {
						minTime = Math.min(minTime, time);
					}
					if (maxTime == 0) {
						maxTime = time;
					}
					else {
						maxTime = Math.max(maxTime, time);
					}
					totalTime += time;
					lengths += size1;
					normalLengths += size2;
					if (size1 != size2) {
						different++;
					}
				}
				else if (params.length == 3) {
					unsupported++;
					String features1 = params[0];
					int size1 = Integer.parseInt(params[1]);
					String features2 = params[2];
					if (instances.containsKey(features1)) {
						instances.put(features1, instances.get(features1) + 1);
					}
					else {
						instances.put(features1, 1);
					}
					if (normalInstances.containsKey(features2)) {
						normalInstances.put(features2, normalInstances.get(features2) + 1);
					}
					else {
						normalInstances.put(features2, 1);
					}
					lengths += size1; 
				}
				else {
					System.out.println(s);
					System.out.println(params);
					continue;
				}
			}
			else {
				if (s.contains("}")) {
					int nPaths = Integer.parseInt(s.substring(s.lastIndexOf(":") + 1).trim());
					try {
						if (s.contains("unsupported")) {
							s = s.substring(0, s.indexOf("}")).trim();
						}
						else {
							s = s.substring(0, s.indexOf("}") - 1).trim();
						}
					}
					catch (StringIndexOutOfBoundsException e) {
						continue;
					}
					totalNPaths += nPaths;
					if (minNPaths == 0) {
						minNPaths = nPaths;
					}
					else {
						minNPaths = Math.min(minNPaths, nPaths);
					}
					if (maxNPaths == 0) {
						maxNPaths = nPaths;
					}
					else {
						maxNPaths = Math.max(maxNPaths, nPaths);
					}
				}
				String[] params = s.split("\t");
				if (params.length == 3) {
					unsupported++;
					String features1 = params[0];
					int size1 = Integer.parseInt(params[1]);
					String features2 = params[2];
					if (instances.containsKey(features1)) {
						instances.put(features1, instances.get(features1) + 1);
					}
					else {
						instances.put(features1, 1);
					}
					if (normalInstances.containsKey(features2)) {
						normalInstances.put(features2, normalInstances.get(features2) + 1);
					}
					else {
						normalInstances.put(features2, 1);
					}
					lengths += size1;
				}
				else if (params.length == 5){
					String features1 = params[0];
					int size1 = Integer.parseInt(params[1]);
					String features2 = params[2];
					int size2 = Integer.parseInt(params[3]);
					long time = Long.parseLong(params[4]);
					if (instances.containsKey(features1)) {
						instances.put(features1, instances.get(features1) + 1);
					}
					else {
						instances.put(features1, 1);
					}
					if (normalInstances.containsKey(features2)) {
						normalInstances.put(features2, normalInstances.get(features2) + 1);
					}
					else {
						normalInstances.put(features2, 1);
					}
					if (minLength == 0) {
						minLength = size1;
					}
					else {
						minLength = Math.min(minLength, size1);
					}
					if (maxLength == 0) {
						maxLength = size1;
					}
					else {
						maxLength = Math.max(maxLength, size1);
					}
					if (minNormalLength == 0) {
						minNormalLength = size2;
					}
					else {
						minNormalLength = Math.min(minNormalLength, size2);
					}
					if (maxNormalLength == 0) {
						maxNormalLength = size2;
					}
					else {
						maxNormalLength = Math.max(maxNormalLength, size2);
					}
					if (maxTime == 0) {
						maxTime = time;
					}
					else {
						maxTime = Math.max(maxTime, time);
					}
					totalTime += time;
					lengths += size1;
					normalLengths += size2;
					if (size1 != size2) {
						different++;
					}
				}	
			}
		}
		for (String instance : instances.keySet()) {
			String original = instance;
			instance = instance.replace("[", "");
			instance = instance.replace("]", "").trim();
			String[] features = instance.split(",");
			for (String feat : features) {
				String feature = feat.trim();
				if (featuresInstances.containsKey(feature)) {
					int prev = featuresInstances.get(feature) + instances.get(original);
					featuresInstances.put(feature, prev);
				}
				else {
					featuresInstances.put(feature, instances.get(original));
				}
			}
		}
		double avgTime = (double) totalTime/(nLines - unsupported);
		double avgNPaths = (double) totalNPaths/i;
		double avgLengths = (double) lengths/nLines;
		double avgNormalLengths = (double) normalLengths/(nLines - unsupported);
		System.out.println("Number of queries: " + i);
		System.out.println("Average time: " + avgTime + " ns");
		System.out.println("Average paths in each query: " + avgNPaths);
		System.out.println("Average length of paths: " + avgLengths);
		System.out.println("Average length of normalised paths: " + avgNormalLengths);
		System.out.println("Min time: " + minTime + " ns");
		System.out.println("Max time: " + maxTime + " ns");
		System.out.println("Min length of paths: " + minLength);
		System.out.println("Max length of paths: " + maxLength);
		System.out.println("Min length of normalised paths: " + minNormalLength);
		System.out.println("Max length of normalised paths: " + maxNormalLength);
		System.out.println("Number of different paths: " + different);
		for (String str : instances.keySet()) {
			System.out.println(str + ": " + instances.get(str));
		}
		System.out.println("");
		for (String str : normalInstances.keySet()) {
			System.out.println(str + ": " + normalInstances.get(str));
		}
		System.out.println("");
		for (String str : featuresInstances.keySet()) {
			System.out.println(str + ": " + featuresInstances.get(str));
		}
		br.close();
	}
	
	public boolean equalQueries(int x, int y){
		return canonQueries.get(x).equals(canonQueries.get(y));
	}
	
	@SuppressWarnings("unused")
	public static void main(String[] args) throws Exception{
		List<File> in = new ArrayList<File>();
//		for (int i = 100; i < 183; i++) {
//			in.add(new File("testFiles/utf8WikiDataQueries/utfWikiData" + String.valueOf(i).substring(1)));
//		}
//		File inFile = new File("cleantestCases.nt");
//		File out = new File("testFiles/lsqPaths");
//		if (!out.exists()){
//			out.createNewFile();
//		}
//		in.add(inFile);
		QueryFeaturesCounter qfc = new QueryFeaturesCounter(new File("clean_wikidata.tsv"));
//		QueryFeaturesCounter.filterPaths(in, out);
//		QueryFeaturesCounter qfc = new QueryFeaturesCounter(new File("testFiles/wikiDataPaths"), false, true);
//		QueryFeaturesCounter.pathFiles(new File("resultFiles/features/paths.paths/pathFeatures20200513_154057.log"));
	}		
}
