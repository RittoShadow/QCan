package cl.uchile.dcc.qcan.tools;

import org.apache.commons.cli.*;

import java.io.*;
import java.net.URLDecoder;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class URLtoUTFParser implements FileVisitor<Path> {
	
	Path startingFile;
	Queue<String> queue = new LinkedBlockingQueue<String>();
	Path destination;
	
	public URLtoUTFParser(Path p, Path f) throws IOException{
		this.startingFile = p;	
		this.destination = f;
	}
	
	public void start() throws IOException{
		Files.walkFileTree(this.startingFile, this);
	}
	
	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(final Path file, BasicFileAttributes attrs) throws IOException {
		System.out.println(file);
		String s;
		File out = new File(destination.toString()+"/utf8"+file.getFileName());
		out.createNewFile();
		BufferedReader br = new BufferedReader(new FileReader(file.toFile()));
		BufferedWriter bw = new BufferedWriter(new FileWriter(out));
		while ((s = br.readLine()) != null) {
			String query = s.split("\t")[0];
			query = URLDecoder.decode(query, "UTF-8").replaceAll("\n", " ");
			bw.write(query);
			bw.newLine();
		}
		br.close();
		bw.close();
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
	
	public static void main(String[] args) throws IOException{
		CommandLine commandLine;
		Option option_X = new Option("x", true, "Path to folder containing queries.");
		Option option_Y = new Option("y", true, "Path to store new files.");
	    Options options = new Options();
	    CommandLineParser parser = new DefaultParser();
	    options.addOption(option_X);
	    options.addOption(option_Y);
	    try{
		    commandLine = parser.parse(options, args);
			Path p = new File(commandLine.getOptionValue("x")).toPath();
			Path f = new File(commandLine.getOptionValue("y")).toPath();
			URLtoUTFParser mg = new URLtoUTFParser(p,f);
			mg.start();
	    }
	    catch (ParseException exception){
	        System.out.print("Parse error: ");
	        System.out.println(exception.getMessage());
	    }
	}

}
