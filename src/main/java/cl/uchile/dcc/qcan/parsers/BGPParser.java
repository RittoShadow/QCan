package cl.uchile.dcc.qcan.parsers;

import cl.uchile.dcc.qcan.visitors.BGPExtractor;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class BGPParser extends Parser {

    public BGPParser() {
        super("bgp/");
    }

    @Override
    public void parse(String s) {
        System.out.println("Begin parsing.");
        try {
            queryInfo = "";
            Op op = Algebra.compile(QueryFactory.create(s));
            BGPExtractor bgpExtractor = new BGPExtractor(op);
            List<Op> bgpList = bgpExtractor.getBGPs();
            for (Op bgp : bgpList) {
                Query query = OpAsQuery.asQuery(bgp);
                bw.append(query.toString().replace("\n"," "));
                bw.newLine();
                bw.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        BGPParser bgpParser = new BGPParser();
        String filename = "clean_SWDF.txt";
        bgpParser.read(new File(filename),new File("bgp_" + filename),-1,true,true,true);
    }
}
