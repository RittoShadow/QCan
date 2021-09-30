package cl.uchile.dcc.generate;

import cl.uchile.dcc.blabel.label.GraphColouring.HashCollisionException;
import cl.uchile.dcc.main.RGraph;
import org.apache.commons.cli.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class UCQGeneratorTest {
	
	public final String[] testFiles = {
			"eval/k/k-4",
			"eval/k/k-8",
			"eval/k/k-16",
			"eval/k/k-32"
	};
	
	private UCQGenerator generator;
	private int conjunctions;
	private int unions;
	private int numberOfPredicates;
	private File file;
	private FileWriter fw;
	private BufferedWriter bw;
	
	public UCQGeneratorTest() throws IOException{
		this.file = new File("resultFiles/ucq/result"+new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime())+".log");
		if (!this.file.exists()){
			this.file.createNewFile();
		}
		fw = new FileWriter(this.file, true);
		bw = new BufferedWriter(fw);
		bw.append("filename\tconjunctions\tunions\tgraph time\trewrite time\tleaning time\tcanon time\ttriples in\ttriples out\t");
		bw.newLine();
	}
	
	public void setup(int a, int b, int n) throws IOException, InterruptedException, HashCollisionException{
		int c = a + b;
		conjunctions = a;
		unions = b;
		numberOfPredicates = n;
		generator = new UCQGenerator(new File("eval/k/k-"+c),a,b,n);
		generator.generateTriples();
		
	}
	
	public void execute() throws IOException, InterruptedException, HashCollisionException{
		for (int i = 0; i < 5; i++){
			String line = "eval/k/k-"+(conjunctions+unions);
			line += "\t"+conjunctions;
			line += "\t"+unions;
			generator.selectTriples(conjunctions,unions);
			RGraph e = generator.generateGraph();
			int triplesIn = e.getNumberOfTriples();
			line += "\t"+generator.graphTime;
			line += "\t"+generator.rewriteTime;
			RGraph e1 = e.getCanonicalForm(false);
			line += "\t"+e1.getMinimisationTime();
			line += "\t"+e1.getLabelTime();
			line += "\t"+triplesIn;
			line += "\t"+e1.getNumberOfTriples();
			bw.append(line);
			bw.newLine();
			bw.flush();
		}
	}
	
	public void testAll(int a, int b, int n) throws IOException, InterruptedException, HashCollisionException{
		for (int x = 1; x <= a; x++){
			for (int y = 1; y <= b; y++){
				setup(x,y,n);
				try{
					execute();
				}
				catch (StackOverflowError e) {
					System.err.println("Stopped at: c = "+a+" and u = "+b);
					break;
				}
				catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	}
	
	public void close() throws IOException{
		bw.close();
	}
	
	public static void main(String[] args) throws IOException, InterruptedException, HashCollisionException{
		CommandLine commandLine;
		Option option_X = new Option("c", true, "Number of conjunctions. Starts with 1. Default is 3.");
		Option option_T = new Option("u", true, "Number of triples per union patterns. Starts with 1. Default is 3.");
		Option option_N = new Option("n", true, "Number of different predicates. Default is 2.");


	    Options options = new Options();
	    CommandLineParser parser = new DefaultParser();
	    options.addOption(option_X);
	    options.addOption(option_T);
	    options.addOption(option_N);
	    try{
		    commandLine = parser.parse(options, args);
		    int conjunctions = 4;
		    int unions = 16;
		    int numberOfPredicates = 16;
		    if (commandLine.hasOption("c")){
				conjunctions = Integer.valueOf(commandLine.getOptionValue("c"));
			}
			if (commandLine.hasOption("u")){
				unions = Integer.valueOf(commandLine.getOptionValue("u"));
			}
			if (commandLine.hasOption("n")) {
				numberOfPredicates = Integer.valueOf(commandLine.getOptionValue("n"));
			}
			UCQGeneratorTest test = new UCQGeneratorTest();
			test.testAll(conjunctions,unions,numberOfPredicates);
			test.close();
			System.exit(0);
	    }
	    catch (ParseException exception){
	        System.out.print("Parse error: ");
	        System.out.println(exception.getMessage());
	    }
	}

}
