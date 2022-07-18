package cl.uchile.dcc.qcan.main;

import cl.uchile.dcc.blabel.label.GraphColouring;
import cl.uchile.dcc.qcan.data.Analysis;
import cl.uchile.dcc.qcan.generate.MultipleGenerator;
import cl.uchile.dcc.qcan.generate.UCQGeneratorTest;
import cl.uchile.dcc.qcan.generate.WellDesignedPatternGenerator;
import cl.uchile.dcc.qcan.parsers.BGPParser;
import cl.uchile.dcc.qcan.paths.RPQMinimiser;
import cl.uchile.dcc.qcan.tools.URLtoUTFParser;

import java.io.IOException;

public class Launcher {

    public static void main(String[] args) throws IOException, GraphColouring.HashCollisionException, InterruptedException {
        if (args.length > 0) {
            String exec = args[0];
            String[] args1 = new String[args.length - 1];
            if (args.length > 1) {
                System.arraycopy(args,1,args1,0,args.length-1);
            }
            if (exec.equals("benchmark")) {
                Benchmark.main(args1);
            }
            else if (exec.equals("easy")) {
                EasyCanonicalisation.main(args1);
            }
            else if (exec.equals("ucq")) {
                UCQGeneratorTest.main(args1);
            }
            else if (exec.equals("wellDesigned")) {
                WellDesignedPatternGenerator.main(args1);
            }
            else if (exec.equals("single")) {
                SingleQuery.main(args1);
            }
            else if (exec.equals("multi")) {
                MultipleGenerator.main(args1);
            }
            else if (exec.equals("analysis")) {
                Analysis.main(args1);
            }
            else if (exec.equals("rpq")) {
                RPQMinimiser.main(args1);
            }
            else if (exec.equals("utf8")) {
                URLtoUTFParser.main(args1);
            }
            else if (exec.equals("bgp")) {
                BGPParser.main(args1);
            }
        }
    }
}
