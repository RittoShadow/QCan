package test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.jena.ext.com.google.common.collect.HashMultiset;
import org.apache.jena.ext.com.google.common.collect.Multiset;

import cl.uchile.dcc.blabel.label.GraphColouring.HashCollisionException;

public class Benchmark {
	public File f;
	public int queriesRead;
	public int queriesCanon;
	public int unsupportedQueries;
	public int badSyntaxQueries;
	public QueryParser pq;
	public JenaParser jp;
	public Multiset<String> canonQueries = HashMultiset.create();
	public boolean enableFilter = false;
	public boolean enableOptional = false;
	public boolean enableLeaning = false;
	public boolean enableCanonicalisation = false;
	public boolean enableTrimming = false;
	
	public Benchmark(String path) throws IOException{
		f = new File(path);
	}
	
	public Benchmark(String path, boolean enableTrimming){
		f = new File(path);
		this.enableTrimming = enableTrimming;
	}
	
	public Benchmark(String path, boolean enableFilter, boolean enableOptional, boolean enableLeaning, boolean enableCanonicalisation){
		f = new File(path);
		this.enableFilter = enableFilter;
		this.enableOptional = enableOptional;
		this.enableLeaning = enableLeaning;
		this.enableCanonicalisation = enableCanonicalisation;
	}
	
	public void execute(int upTo, boolean printDist) throws IOException, InterruptedException, HashCollisionException{
		File out;
		if (enableLeaning){
			out = new File("resultFiles/results"+new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime())+".log");
		}
		else{
			out = new File("resultFiles/labelOnly_results"+new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime())+".log");
		}
		if (!out.exists()){
			out.createNewFile();
		}
		if (enableTrimming){
			jp = new JenaParser(f, out, upTo, true, true);
			if (printDist){
				jp.getDistributionInfo();
			}
		}
		else{
			pq = new QueryParser(f, out, upTo, enableFilter, enableOptional, enableLeaning, enableCanonicalisation);
			if (printDist){
				pq.getDistributionInfo();
			}
		}
//		writeToFile("resultFiles/canon"+new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime())+".log", thirdOutput);
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
		CommandLine commandLine;
	    Option option_W = new Option("w", false, "Set to remove queries with bad syntax from file.");
	    Option option_C = new Option("c", false, "Set to enable canonicalisation.");
	    Option option_N = new Option("n", true, "Number of queries to parse. Default is 10000");
	    option_N.setArgName("number");
	    Option option_X = new Option("x", true, "Path to file containing queries.");
	    Option option_D = new Option("d", false, "Set to output distribution of queries.");
	    Option option_O = new Option("o", false, "Set to enable canonicalisation of OPTIONAL terms.");
	    Option option_F = new Option("f", false, "Set to enable canonicalisation of FILTER terms.");
	    Option option_L = new Option("l", false, "Set to enable leaning of graphs.");
	    Option option_J = new Option("j", false, "Set to enable the Jena parser. If set as well as -c, it overrides the option.");
	    option_F.setArgName("file");
	    Options options = new Options();
	    CommandLineParser parser = new DefaultParser();

	    options.addOption(option_C);
	    options.addOption(option_W);
	    options.addOption(option_N);
	    options.addOption(option_F);
	    options.addOption(option_D);
	    options.addOption(option_X);
	    options.addOption(option_O);
	    options.addOption(option_L);
	    options.addOption(option_J);

	    String header = "";
	    String footer = "";
	    boolean enableFilter = false;
	    boolean enableOptional = false;
	    boolean enableLeaning = false;
	    boolean enableCanonicalisation = false;
	    HelpFormatter formatter = new HelpFormatter();
	    formatter.printHelp("benchmark", header, options, footer, true);    
	    try{
	    	Benchmark b;
	        commandLine = parser.parse(options, args);
	        if (commandLine.hasOption("x")){
	        	int upTo = 10000;
	        	String file = commandLine.getOptionValue("x");
	        	if (commandLine.hasOption("n")){
	        		upTo = Integer.parseInt(commandLine.getOptionValue("n"));
	        	}
	        	if (commandLine.hasOption("w")){
	        		QueryParser.removeQueries(file);
	        		file = "clean" + file;
	        	}
	        	if (commandLine.hasOption("f")){
	        		enableFilter = true;
	        	}
	        	if (commandLine.hasOption("o")){
	        		enableOptional = true;
	        	}
	        	if (commandLine.hasOption("l")){
	        		enableLeaning = true;
	        	}
	        	if (commandLine.hasOption("c")){
	        		enableCanonicalisation = true;
	        	}
	        	if (commandLine.hasOption("j")){
	        		b = new Benchmark(file, true);
	        	}
	        	else{
	        		b = new Benchmark(file, enableFilter, enableOptional, enableLeaning, enableCanonicalisation);
	        	}
	    		b.execute(upTo, commandLine.hasOption("d"));
	        }
	    }
	    catch (ParseException exception){
	        System.out.print("Parse error: ");
	        System.out.println(exception.getMessage());
	    }

	}

}
