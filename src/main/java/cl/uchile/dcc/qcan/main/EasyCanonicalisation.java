package cl.uchile.dcc.qcan.main;

import cl.uchile.dcc.blabel.label.GraphColouring;
import org.apache.commons.cli.*;

import java.io.*;

public class EasyCanonicalisation {

    public boolean rewrite = true;
    public boolean minimisation = true;
    public boolean canonicalisation = true;

    public EasyCanonicalisation() {

    }

    public void canonicaliseQuery(String q) throws GraphColouring.HashCollisionException, InterruptedException {
        SingleQuery sq = new SingleQuery(q,canonicalisation,rewrite,minimisation,true,false);
        String out = sq.getQuery();
        System.out.println(out);
    }

    public void canonicaliseFile(File in, File out) throws IOException, GraphColouring.HashCollisionException, InterruptedException {
        String s;
        FileReader fr = new FileReader(in);
        BufferedReader br = new BufferedReader(fr);
        FileWriter fw = new FileWriter(out);
        BufferedWriter bw = new BufferedWriter(fw);
        while ((s = br.readLine()) != null) {
            SingleQuery sq = new SingleQuery(s, canonicalisation, rewrite, minimisation, true, false);
            String q = sq.getQuery();
            q = q.replace("\n"," ");
            bw.append(q);
            bw.newLine();
        }
        bw.close();
    }

    public static void main(String[] args) {
        CommandLine commandLine;
        String header = "";
        String footer = "";
        Option option_F = new Option("f", true, "Filename that contains the query/queries to canonicalise.");
        Option option_Q = new Option("q", true, "The query to canonicalise.");
        Option option_M = new Option("m", false, "Set to enable minimisation/leaning.");
        Option option_O = new Option("o",true,"Output file");
        option_F.setArgName("filename");
        option_Q.setArgName("query");
        option_O.setArgName("output");
        Options options = new Options();
        options.addOption(option_F);
        options.addOption(option_Q);
        options.addOption(option_M);
        options.addOption(option_O);
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("benchmark", header, options, footer, true);
        try {
            commandLine = parser.parse(options,args);
            boolean minimisation = commandLine.hasOption("m");
            EasyCanonicalisation ec = new EasyCanonicalisation();
            ec.minimisation = minimisation;
            if (commandLine.hasOption("f")) {
                String filename = commandLine.getOptionValue("f");
                String output = "";
                File f = new File(filename);
                if (commandLine.hasOption("o")) {
                    output = commandLine.getOptionValue("o");
                }
                else {
                    System.err.println("No output file specified.");
                    System.exit(-1);
                }
                File out = new File(output);
                if (!out.exists()) {
                    out.createNewFile();
                }
                ec.canonicaliseFile(f,out);
            }
            else if (commandLine.hasOption("q")) {
                ec.canonicaliseQuery(commandLine.getOptionValue("q"));
            }

        } catch (ParseException | IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (GraphColouring.HashCollisionException e) {
            e.printStackTrace();
        }
    }
}
