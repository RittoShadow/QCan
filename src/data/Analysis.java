package data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class Analysis {
	File file;
	BufferedReader br;
	ArrayList<ArrayList<Double>> data = new ArrayList<ArrayList<Double>>();
	ArrayList<Double> average = new ArrayList<Double>();
	ArrayList<Double> median = new ArrayList<Double>();
	int uniqueQueries = 0;
	ArrayList<Double> MAX = new ArrayList<Double>();
	ArrayList<Double> MIN = new ArrayList<Double>();
	ArrayList<Double> q25 = new ArrayList<Double>();
	ArrayList<Double> q75 = new ArrayList<Double>();
	ArrayList<Double> nVars = new ArrayList<Double>();
	ArrayList<Double> graphSize = new ArrayList<Double>();
	ArrayList<Double> triples = new ArrayList<Double>();
	public int distinct = 0;
	public int joins = 0;
	public int unions = 0;
	public int optional = 0;
	public int filter = 0;
	public int solutionMods = 0;
	public int namedGraphs = 0;

	public Analysis(String s) throws FileNotFoundException{
		this.file = new File(s);
		br = new BufferedReader(new FileReader(this.file));
	}
	
	public void read() throws IOException{
		String line;
		for (int i = 0; i < 6; i++){
			data.add(i, new ArrayList<Double>());
			average.add(i, 0.0);
			median.add(i,0.0);
			MAX.add(i,0.0);
			MIN.add(i,0.0);
			q25.add(i,0.0);
			q75.add(i,0.0);
		}
		while(true){
			line = br.readLine();
			if (line.startsWith("Total")){
				break;
			}
			else{
				String[] params = line.split("\t");
				for (int i = 0; i < 2; i++){
					data.get(i).add(new Double(params[i+1]));
				}
//				data.get(2).add(new Double(params[4]) - new Double(params[6]));
//				data.get(3).add(new Double(params[5]) - new Double(params[7]));
//				data.get(4).add(new Double(params[8]) - new Double(params[9]));
				
				data.get(2).add(new Double(params[1])+new Double(params[2]));
				
				data.get(3).add(new Double(params[6]));
				data.get(4).add(new Double(params[7]));
				data.get(5).add(new Double(params[9]));
				uniqueQueries++;
				if (params[10].equals("true")){
					distinct++;
				}
				if (params[11].equals("true")){
					joins++;
				}
				if (params[12].equals("true")){
					unions++;
				}
				if (params[13].equals("true")){
					optional++;
				}
				if (params[14].equals("true")){
					filter++;
				}
				if (params[15].equals("true")){
					namedGraphs++;
				}				
				if (params[16].equals("true")){
					solutionMods++;
				}
				if (uniqueQueries%10000 == 0){
					System.out.println(uniqueQueries + " queries read.");
				}
			}
		}
		for (int i = 0; i < data.size(); i++){
			Collections.sort(data.get(i));
		}
	}
	
	public void shortRead() throws IOException{
		String line;
		for (int i = 0; i < 2; i++){
			data.add(i, new ArrayList<Double>());
			average.add(i, 0.0);
			median.add(i,0.0);
			MAX.add(i,0.0);
			MIN.add(i,0.0);
			q25.add(i,0.0);
			q75.add(i,0.0);
		}
		while(true){
			line = br.readLine();
			if (line.startsWith("Total")){
				break;
			}
			else{
				String[] params = line.split("\t");

//				data.get(2).add(new Double(params[4]) - new Double(params[6]));
//				data.get(3).add(new Double(params[5]) - new Double(params[7]));
//				data.get(4).add(new Double(params[8]) - new Double(params[9]));
				
				data.get(0).add(new Double(params[1]));
				data.get(1).add(new Double(params[2]));
				uniqueQueries++;
				if (uniqueQueries%10000 == 0){
					System.out.println(uniqueQueries + " queries read.");
				}
			}
		}
		for (int i = 0; i < data.size(); i++){
			Collections.sort(data.get(i));
		}
	}
	
	public void getAverage(){
		for (int i = 0; i < data.size(); i++){
			for (int k = 0 ; k < data.get(i).size() ; k++){
				double d;
				if (average.isEmpty()){
					d = 0;
				}
				else{
					d = average.get(i);
				}
				average.set(i, d + data.get(i).get(k));
			}
		}	
		for (int i = 0; i < average.size(); i++){
			double d = average.get(i)/uniqueQueries;
			average.set(i, d);
		}
	}
	
	public void getStandardDeviation(){
		for (int i = 0; i < data.size(); i++){
			for (double d : data.get(i)){
				double s;
				if (median.isEmpty()){
					s = 0;
				}
				else{
					s = median.get(i);
				}
				median.set(i, s + Math.pow(d - average.get(i), 2));
			}
			double s = median.get(i);
			s = s/uniqueQueries;
			s = Math.sqrt(s);
			median.set(i, s);
		}
	}
	
	public void getMedian(){
		for (int i = 0; i < data.size(); i++){
			int n = data.get(i).size();
			double median;
			if (n % 2 == 0){
				median = (data.get(i).get(n/2-1) + data.get(i).get(n/2))/2;
			}
			else{
				median = data.get(i).get(n/2);
			}
			this.median.set(i, median);
			this.q25.set(i, data.get(i).get(n/4));
			this.q75.set(i,data.get(i).get(3*n/4));
		}
	}
	
	public void getMax(){
		for (int i = 0; i < data.size(); i++){
			double max = data.get(i).get(0);
			for (double d : data.get(i)){
				if (d > max){
					max = d;
				}
			}
			MAX.set(i, max);
		}
	}
	
	public void getMin(){
		for (int i = 0; i < data.size(); i++){
			double min = data.get(i).get(0);
			for (double d : data.get(i)){
				if (d < min){
					min = d;
				}
			}
			MIN.set(i, min);
		}
	}
	
	public void getNegativeValues(int param){
		for (int i = 0; i < data.get(param).size(); i++){
			if (data.get(param).get(i) < 0){
				System.out.println(i);
			}
		}
	}
	
	public void displayInfo() throws IOException{
		read();
		getAverage();
		getMedian();
		getMax();
		getMin();
		String s = "";
		for (int i = 0; i < data.size(); i++){
			
			s += average.get(i) + ",";
			s += median.get(i) + ",";
			s += q25.get(i) + ",";
			s += q75.get(i) + ",";
			s += MAX.get(i) + ",";
			s += MIN.get(i) + ",";		
		}
		System.out.println(s);
		System.out.println("DISTINCT: "+distinct);
		System.out.println("UNION: "+unions);
		System.out.println("JOIN: "+joins);
		System.out.println("FILTER: "+filter);
		System.out.println("OPTIONAL: "+optional);
		System.out.println("SOLUTION MODIFIERS: "+solutionMods);
		System.out.println("NAMED GRAPH: "+namedGraphs);
	}
	
	public void shortDisplayInfo() throws IOException{
		shortRead();
		getAverage();
		getMedian();
		getMax();
		getMin();
		String s = "";
		for (int i = 0; i < data.size(); i++){	
			s += average.get(i) + ",";
			s += median.get(i) + ",";
			s += q25.get(i) + ",";
			s += q75.get(i) + ",";
			s += MAX.get(i) + ",";
			s += MIN.get(i) + ",";		
		}
		System.out.println(s);
	}
	
	public static void main(String[] args) throws IOException{
		Analysis a = new Analysis("resultFiles/results20180404_182940.log");
		a.displayInfo();
	}

}
