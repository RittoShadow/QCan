package cl.uchile.dcc.qcan.parsers;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.op.OpNull;

import java.io.File;
import java.io.IOException;

public class JenaParser extends Parser {

	public void parse(String s) throws Exception{
		long t = System.nanoTime();
		Query query = QueryFactory.create(s);
		Op op = Algebra.compile(query);
		if (op instanceof OpNull){
			canonQueries.add(query.toString());
			return;
		}
		Query result = OpAsQuery.asQuery(op);
		if (query.isAskType()) {
			result.setQueryAskType();
		}
		else if (query.isConstructType()) {
			result.setConstructTemplate(query.getConstructTemplate());
		}
		else if (query.isDescribeType()) {
			result.setQueryDescribeType();
		}
		t = System.nanoTime() - t;
		String queryInfo = totalQueries + "\t" + t + "\t";
		queryInfo += query.getResultVars().size() + "\t";
		queryInfo += query.isDistinct() + "\t";
		canonQueries.add(result.toString());
		bw.append(queryInfo);
		bw.newLine();
		supportedQueries++;
	}
	
	public JenaParser() {
		super("resultFiles/jena/");
	}

	public void read(File f, File out, int upTo, int offset) throws IOException {
		read(f,out,upTo,offset,true,true,true,true);
	}
	
	public void printUnsupportedFeatures(){
		System.out.println(this.unsupportedFeaturesToString());
	}

}
