package main;

import cl.uchile.dcc.blabel.label.GraphColouring;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Benchmark {
	public File f;
	public QueryParser pq;
	public JenaParser jp;
	public boolean enableFilter = false;
	public boolean enableOptional = false;
	public boolean enableLeaning = false;
	public boolean enableCanonicalisation = false;
	public boolean enableTrimming = false;
	public boolean pathNormalisation = false;
	
	public Benchmark(String path) throws IOException{
		f = new File(path);
	}
	
	public Benchmark(String path, boolean enableTrimming){
		f = new File(path);
		this.enableTrimming = enableTrimming;
	}
	
	public Benchmark(String path, boolean enableFilter, boolean enableOptional, boolean enableLeaning, boolean enableCanonicalisation){
		this(path,enableFilter,enableOptional,enableLeaning,enableCanonicalisation,false);
	}

	
	public Benchmark(String path, boolean enableFilter, boolean enableOptional, boolean enableLeaning, boolean enableCanonicalisation, boolean pathNormalisation){
		f = new File(path);
		this.enableFilter = enableFilter;
		this.enableOptional = enableOptional;
		this.enableLeaning = enableLeaning;
		this.enableCanonicalisation = enableCanonicalisation;
		this.pathNormalisation = pathNormalisation;
	}
	
	public void execute(int upTo, int offset, boolean printDist) throws IOException {
		File out;
		String filename = f.getName();
		if (filename.contains(".")) {
			filename = filename.substring(0,filename.lastIndexOf("."));
		}
		if (enableLeaning){
			out = new File("resultFiles/" + filename + "_results"+new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime())+".log");
		}
		else{
			if (enableTrimming) {
				out = new File("resultFiles/jena/" + filename + "_labelOnly_results"+new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime())+".log");
			}
			else {
				out = new File("resultFiles/" + filename + "_labelOnly_results"+new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime())+".log");
			}
		}
		if (!out.exists()){
			out.createNewFile();
		}
		if (enableTrimming){
			jp = new JenaParser(f, out, upTo, true, true);
			if (printDist){
				jp.getDistributionInfo(filename);
			}
		}
		else{
			if (pathNormalisation) {
				pq = new QueryParser(f, out, upTo, offset, pathNormalisation);
			}
			else {
				pq = new QueryParser(f, out, upTo, offset, enableFilter, enableOptional, enableLeaning, enableCanonicalisation);
			}
			if (printDist){
				pq.getDistributionInfo(filename);
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
	
	public static void main(String[] args) throws IOException {
		CommandLine commandLine;
	    Option option_W = new Option("w", false, "Set to remove queries with bad syntax from file.");
	    Option option_C = new Option("c", false, "Set to enable canonicalisation.");
	    Option option_N = new Option("n", true, "Number of queries to parse. Default is 10000");
	    option_N.setArgName("number");
	    Option option_OFF = new Option("y", true, "Start at this query. Default is 0.");
	    option_OFF.setArgName("offset");
	    Option option_X = new Option("x", true, "Path to file containing queries.");
	    Option option_D = new Option("d", false, "Set to output distribution of queries.");
	    Option option_O = new Option("o", false, "Set to enable canonicalisation of OPTIONAL terms.");
	    Option option_F = new Option("f", false, "Set to enable canonicalisation of FILTER terms.");
	    Option option_L = new Option("l", false, "Set to enable leaning of graphs.");
	    Option option_J = new Option("j", false, "Set to enable the Jena parser. If set as well as -c, it overrides the option.");
	    Option option_P = new Option("p", false, "Set to enable path normalisation.");
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
	    options.addOption(option_P);
	    options.addOption(option_OFF);

	    String header = "";
	    String footer = "";
	    boolean enableFilter = false;
	    boolean enableOptional = false;
	    boolean enableLeaning = false;
	    boolean enableCanonicalisation = false;
	    boolean pathNormalisation = false;
	    HelpFormatter formatter = new HelpFormatter();
	    formatter.printHelp("benchmark", header, options, footer, true);    
	    try{
	    	Benchmark b;
	        commandLine = parser.parse(options, args);
	        if (commandLine.hasOption("x")){
	        	int upTo = 10000;
	        	int offset = 0;
	        	String file = commandLine.getOptionValue("x");
	        	if (commandLine.hasOption("n")){
	        		upTo = Integer.parseInt(commandLine.getOptionValue("n"));
	        	}
	        	if (commandLine.hasOption("y")){
	        		offset = Integer.parseInt(commandLine.getOptionValue("y"));
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
	        	if (commandLine.hasOption("p")) {
	        		pathNormalisation = true;
	        	}
	        	if (commandLine.hasOption("j")){
	        		b = new Benchmark(file, true);
	        	}
	        	else{
	        		b = new Benchmark(file, enableFilter, enableOptional, enableLeaning, enableCanonicalisation, pathNormalisation);
	        	}
	    		b.execute(upTo, offset, commandLine.hasOption("d"));
	    		System.exit(0);
	        }
	    }
	    catch (ParseException exception){
	        System.out.print("Parse error: ");
	        System.out.println(exception.getMessage());
	    }

	}

}
