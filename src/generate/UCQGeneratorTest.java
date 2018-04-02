package generate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import cl.uchile.dcc.blabel.label.GraphColouring.HashCollisionException;
import test.ExpandedGraph;

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
		bw.append("filename\tconjunctions\tunions\tgraph time\tcanon time\ttriples in\ttriples out\t");
		bw.newLine();
	}
	
	public void setup(int a, int b) throws IOException, InterruptedException, HashCollisionException{
		int c = a+b;
		conjunctions = a;
		unions = b;
		generator = new UCQGenerator(new File("eval/k/k-"+c));
		generator.generateTriples();
		generator.selectTriples(a, b);
		
	}
	
	public void execute() throws IOException, InterruptedException, HashCollisionException{
		for (int i = 0; i < 5; i++){
			String line = "eval/k/k-"+(conjunctions+unions);
			line += "\t"+conjunctions;
			line += "\t"+unions;
			generator.selectTriples(conjunctions, unions);
			long t = System.nanoTime();
			ExpandedGraph e = generator.generateGraph(conjunctions, unions);
			line += "\t"+(System.nanoTime()-t);
			t = System.nanoTime();
			ExpandedGraph e1 = e.getCanonicalForm(false);
			line += "\t"+(System.nanoTime()-t);
			line += "\t"+e.getNumberOfTriples();
			line += "\t"+e1.getNumberOfTriples();
			bw.append(line);
			bw.newLine();
		}
	}
	
	public void testAll(int a, int b) throws IOException, InterruptedException, HashCollisionException{
		for (int x = 0; x < a; x++){
			for (int y = 0; y < b; y++){
				setup((int)Math.pow(2, x),(int)Math.pow(2, y));
				try{
					execute();
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
		Option option_X = new Option("c", true, "Number of different values for conjunctions. Starts with 1 and increases by powers of 2. Default is 3.");
		Option option_T = new Option("u", true, "Number of different values for unions. Starts with 1 and increases by powers of 2. Default is 3.");
		
	    Options options = new Options();
	    CommandLineParser parser = new DefaultParser();
	    options.addOption(option_X);
	    options.addOption(option_T);
	    try{
		    commandLine = parser.parse(options, args);
		    int conjunctions = 3;
		    int unions = 3;
		    if (commandLine.hasOption("c")){
				conjunctions = new Integer(commandLine.getOptionValue("c"));
			}
			if (commandLine.hasOption("u")){
				unions = new Integer(commandLine.getOptionValue("u"));
			}
			UCQGeneratorTest test = new UCQGeneratorTest();
			test.testAll(conjunctions,unions);
			test.close();
	    }
	    catch (ParseException exception){
	        System.out.print("Parse error: ");
	        System.out.println(exception.getMessage());
	    }
	}

}
