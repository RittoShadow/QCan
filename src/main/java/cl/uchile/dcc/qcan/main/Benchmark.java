package cl.uchile.dcc.qcan.main;

import cl.uchile.dcc.qcan.parsers.JenaParser;
import cl.uchile.dcc.qcan.parsers.QueryParser;
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
	public boolean enableLeaning = false;
	public boolean enableCanonicalisation = false;
	public boolean enableRewrite = false;
	public boolean enableTrimming = false;
	public boolean pathNormalisation = false;
	public boolean gZipped = false;
	
	public Benchmark(String path) {
		f = new File(path);
	}
	
	public Benchmark(String path, boolean enableTrimming){
		f = new File(path);
		this.enableTrimming = enableTrimming;
	}
	
	public Benchmark(String path, boolean enableLeaning, boolean enableCanonicalisation, boolean enableRewrite){
		this(path,enableLeaning,enableCanonicalisation,enableRewrite,false);
	}

	
	public Benchmark(String path, boolean enableLeaning, boolean enableCanonicalisation, boolean enableRewrite, boolean pathNormalisation){
		f = new File(path);
		this.enableLeaning = enableLeaning;
		this.enableCanonicalisation = enableCanonicalisation;
		this.enableRewrite = enableRewrite;
		this.pathNormalisation = pathNormalisation;
	}
	
	public void execute(int upTo, int offset, boolean printDist) throws IOException {
		File out;
		String filename = f.getName();
		if (filename.contains(".")) {
			filename = filename.substring(0,filename.indexOf("."));
		}
		if (enableTrimming){
			out = new File("resultFiles/jena/" + filename + "_labelOnly_results"+new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime())+".log");
			if (!out.exists()) {
				out.createNewFile();
			}
			jp = new JenaParser();
			jp.read(f,out,upTo,offset);
			if (printDist){
				jp.getDistributionInfo(filename,gZipped);
			}
		}
		else{
			if (enableLeaning) {
				filename = "min_" + filename;
			}
			if (enableCanonicalisation) {
				filename = "label_" + filename;
			}
			if (enableRewrite) {
				filename = "rewrite_" + filename;
			}
			out = new File("resultFiles/" + filename + "_results"+new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime())+".log");
			if (!out.exists()) {
				out.createNewFile();
			}
			pq = new QueryParser();
			if (gZipped) {
				pq.readGZFile(f, out, upTo, offset, enableLeaning, enableCanonicalisation, enableRewrite, pathNormalisation);
			}
			else {
				pq.read(f, out, upTo, offset, enableLeaning, enableCanonicalisation, enableRewrite, pathNormalisation);
			}
			if (printDist){
				pq.getDistributionInfo(filename,gZipped);
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
	    Option option_OFF = new Option("o", true, "Start at this query. Default is 0.");
	    option_OFF.setArgName("offset");
	    Option option_X = new Option("x", true, "Path to file containing queries.");
	    Option option_D = new Option("d", false, "Set to output distribution of queries.");
	    Option option_L = new Option("l", false, "Set to enable leaning of graphs.");
	    Option option_J = new Option("j", false, "Set to enable the Jena parser. If set as well as -c, it overrides the option.");
	    Option option_P = new Option("p", false, "Set to enable path normalisation.");
	    Option option_R = new Option("r",false,"Set to rewrite queries.");
	    Option option_G = new Option("g",false,"Set if input is gzip file. Results will also be zipped.");
	    Options options = new Options();
	    CommandLineParser parser = new DefaultParser();

	    options.addOption(option_C);
	    options.addOption(option_W);
	    options.addOption(option_N);
	    options.addOption(option_D);
	    options.addOption(option_X);
	    options.addOption(option_L);
	    options.addOption(option_J);
	    options.addOption(option_P);
	    options.addOption(option_R);
	    options.addOption(option_OFF);
	    options.addOption(option_G);

	    String header = "";
	    String footer = "";
	    boolean enableLeaning = false;
	    boolean enableCanonicalisation = false;
	    boolean enableRewrite = false;
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
	        	if (commandLine.hasOption("o")){
	        		offset = Integer.parseInt(commandLine.getOptionValue("o"));
	        	}
	        	if (commandLine.hasOption("w")){
	        		QueryParser.removeQueries(file);
	        		file = "clean" + file;
	        		System.exit(0);
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
	        	if (commandLine.hasOption("r")) {
					enableRewrite = true;
				}
	        	if (commandLine.hasOption("j")){
	        		b = new Benchmark(file, true);
	        	}
	        	else{
	        		b = new Benchmark(file, enableLeaning, enableCanonicalisation, enableRewrite, pathNormalisation);
	        	}
	        	b.gZipped = commandLine.hasOption("g");
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
