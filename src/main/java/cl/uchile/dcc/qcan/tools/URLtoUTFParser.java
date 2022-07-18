package cl.uchile.dcc.qcan.tools;

import org.apache.commons.cli.*;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.GZIPOutputStream;

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
		if (!out.exists()) {
			out.createNewFile();
		}
		BufferedReader br = null;
		BufferedWriter bw = null;
		if (file.getFileName().toString().endsWith(".tar.gz")) {
			FileInputStream fileInputStream = new FileInputStream(file.toFile());
			GzipCompressorInputStream gzipInputStream = new GzipCompressorInputStream(fileInputStream);
			TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(gzipInputStream);
			TarArchiveEntry tarArchiveEntry = null;
			if ((tarArchiveEntry = tarArchiveInputStream.getNextTarEntry()) != null) {
				br = new BufferedReader(new InputStreamReader(tarArchiveInputStream));
			}
			else {
				System.exit(-1);
			}
			OutputStream os = Files.newOutputStream(out.toPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
			GZIPOutputStream gzip = new GZIPOutputStream(os);
			OutputStreamWriter ow = new OutputStreamWriter(gzip, StandardCharsets.UTF_8);
			bw = new BufferedWriter(ow);
		}
		else if (file.getFileName().toString().endsWith(".gz")) {
			FileInputStream fileInputStream = new FileInputStream(file.toFile());
			GzipCompressorInputStream gzipInputStream = new GzipCompressorInputStream(fileInputStream);
			br = new BufferedReader(new InputStreamReader(gzipInputStream));
			OutputStream os = Files.newOutputStream(out.toPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
			GZIPOutputStream gzip = new GZIPOutputStream(os);
			OutputStreamWriter ow = new OutputStreamWriter(gzip, StandardCharsets.UTF_8);
			bw = new BufferedWriter(ow);
		}
		else {
			br = new BufferedReader(new FileReader(file.toFile()));
			bw = new BufferedWriter(new FileWriter(out));
		}
		while ((s = br.readLine()) != null) {
			String query = s;
			if (s.contains("\t")) {
				System.out.println(s);
				if (s.split("\t").length > 0) {
					query = s.split("\t")[0];
				}
			}
//			query = query.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
//			query = query.replaceAll("\\+", "%2B");
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
