package tests;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.optimize.TransformPathFlattern;
import org.junit.Before;
import cl.uchile.dcc.paths.RPQMinimiser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RPQTests {
    @Before
    public void setUp() throws Exception {

    }

    public List<String> readQueries(String filepath) throws IOException {
        List<String> ans = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(new File(filepath)));
        String s;
        while ((s = br.readLine())!= null) {
            ans.add(s);
        }
        br.close();
        return ans;
    }

    @org.junit.Test
    public void tests() throws IOException {
        List<String> queries = readQueries("testFiles/rpqTest");
        RPQMinimiser rpqMinimiser = new RPQMinimiser();
        for (String string : queries) {
            Query query = QueryFactory.create(string);
            Op op = Algebra.compile(query);
            op = Transformer.transform(new TransformPathFlattern(), op);
            System.out.println(query);
            op = rpqMinimiser.visit(op);
            System.out.println(op);
        }
    }
}
