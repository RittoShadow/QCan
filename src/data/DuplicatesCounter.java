package data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.regex.Pattern;

public class DuplicatesCounter {
	HashSet<Integer> duplicates = new HashSet<Integer>();
	int total = 0;
	
	public DuplicatesCounter(File f) throws IOException{
		String s;
		FileReader fr = new FileReader(f);
		BufferedReader br = new BufferedReader(fr);
		while ((s = br.readLine()) != null){
			if (Pattern.matches("\\s:\\s\\d+", s)){
				s = s.substring(s.lastIndexOf(":")+1).trim();
				int i = new Integer(s);
				duplicates.add(i);
				total += (i-1);
			}
		}
		br.close();
	}
	
	public static void main(String[] args) throws IOException{
		DuplicatesCounter dc = new DuplicatesCounter(new File("resultFiles/dist20180615_123904.log"));
		for (int i : dc.duplicates){
			System.out.println(i);
		}
		System.out.println("Total is: "+dc.total);
	}
}
