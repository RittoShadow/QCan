package generate;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public class MultipleGenerator implements FileVisitor<Path> {
	
	Path startingFile;
	
	public MultipleGenerator(Path p) throws IOException{
		this.startingFile = p;		
	}
	
	public void start() throws IOException{
		Files.walkFileTree(this.startingFile, this);
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		// TODO Auto-generated method stub
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		System.out.println(file.toFile());
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		// TODO Auto-generated method stub
		return FileVisitResult.TERMINATE;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		// TODO Auto-generated method stub
		return FileVisitResult.CONTINUE;
	}
	
	public static void main(String[] args) throws IOException{
		Path p = new File("src/eval").toPath();
		MultipleGenerator mg = new MultipleGenerator(p);
		mg.start();
	}

}
