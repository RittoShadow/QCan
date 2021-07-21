package cl.uchile.dcc.generate;

import cl.uchile.dcc.blabel.label.GraphColouring.HashCollisionException;
import cl.uchile.dcc.main.RGraph;
import org.apache.commons.cli.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class MultipleGenerator implements FileVisitor<Path> {
	
	Path startingFile;
	public File file;
	public FileWriter fw;
	public BufferedWriter bw;
	private long timeout = 60*10*1000;
	Queue<String> queue = new LinkedBlockingQueue<String>();
	
	public MultipleGenerator(Path p) throws IOException{
		this.startingFile = p;	
		this.file = new File("resultFiles/leaning/result"+new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime())+".log");
		if (!this.file.exists()){
			this.file.createNewFile();
		}
		fw = new FileWriter(this.file, true);
		bw = new BufferedWriter(fw);
	}
	
	public void start() throws IOException{
		Files.walkFileTree(this.startingFile, this);
	}
	
	public void setTimeout(long t){
		this.timeout = t;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(final Path file, BasicFileAttributes attrs) throws IOException {
		System.out.println(file);
		Thread slave = new Thread(new Runnable(){
			@Override
			public void run() {
				int initialNodes, finalNodes, initialVars, finalVars, initialTriples, finalTriples;
				BGPGenerator g;
				try {
					g = new BGPGenerator(file.toFile());
					g.generateTriples();
					RGraph e = g.generateGraph();
					String output = "";
					initialNodes = e.getNumberOfNodes();
					initialVars = e.getNumberOfVars();
					initialTriples = e.getNumberOfTriples();
					long t = System.nanoTime();
					RGraph a = e.getCanonicalForm(false);
					t = System.nanoTime() - t;
					finalNodes = a.getNumberOfNodes();
					finalVars = a.getNumberOfVars();
					finalTriples = a.getNumberOfTriples();
					output += file.getFileName() + "\t";
					output += t + "\t";
					output += initialNodes + "\t";
					output += finalNodes + "\t";
					output += initialVars + "\t";
					output += finalVars + "\t";
					output += initialTriples +"\t";
					output += finalTriples;
					queue.add(output);
				} catch (InterruptedException | HashCollisionException | IOException e2) {
					System.err.println(e2.getMessage());
				} catch (StackOverflowError e){
					return;
				}
			}
			
		});
		slave.start();
		try {
			slave.join(timeout);
		} catch (InterruptedException e) {
			System.err.println(e.getMessage());
		}
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
	
	public void done() throws IOException{
		for (String s : queue){
			bw.append(s);
			bw.newLine();
		}
		bw.close();
	}
	
	public static void main(String[] args) throws IOException{
		CommandLine commandLine;
		Option option_X = new Option("x", true, "Path to folder containing queries.");
		Option option_T = new Option("t", true, "Timeout in milliseconds. Default is 10 minutes.");
	    Options options = new Options();
	    CommandLineParser parser = new DefaultParser();
	    options.addOption(option_X);
	    options.addOption(option_T);
	    try{
		    commandLine = parser.parse(options, args);
			Path p = new File(commandLine.getOptionValue("x")).toPath();
			MultipleGenerator mg = new MultipleGenerator(p);
			if (commandLine.hasOption("t")){
				mg.setTimeout(Long.parseLong(commandLine.getOptionValue("t")));
			}
			mg.start();
			mg.done();
	    }
	    catch (ParseException exception){
	        System.out.print("Parse error: ");
	        System.out.println(exception.getMessage());
	    }
	}

}
