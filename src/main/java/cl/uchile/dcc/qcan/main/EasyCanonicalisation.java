package cl.uchile.dcc.qcan.main;

import cl.uchile.dcc.blabel.label.GraphColouring;
import org.apache.commons.cli.*;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.GZIPOutputStream;

public class EasyCanonicalisation {

    public boolean rewrite = true;
    public boolean minimisation = true;
    public boolean canonicalisation = true;
    public boolean gZipped = false;

    public EasyCanonicalisation() {

    }

    public void canonicaliseQuery(String q) throws GraphColouring.HashCollisionException, InterruptedException {
        SingleQuery sq = new SingleQuery(q,canonicalisation,rewrite,minimisation,true,false);
        String out = sq.getQuery();
        System.out.println(out);
    }

    public void canonicaliseFile(File in, File out) throws GraphColouring.HashCollisionException, InterruptedException, IOException {
        canonicaliseFile(in,out,false);
    }
    
    public void canonicaliseFile(File in, File out, boolean distinct) throws IOException, GraphColouring.HashCollisionException, InterruptedException {
    	canonicaliseFile(in,out,distinct,false);
    }

    public void canonicaliseFile(File in, File out, boolean distinct, boolean keep) throws IOException, GraphColouring.HashCollisionException, InterruptedException {
        String s;
        BufferedReader br;
        BufferedWriter bw;
        if (gZipped) {
            FileInputStream fileInputStream = new FileInputStream(in);
            GzipCompressorInputStream gzipInputStream = new GzipCompressorInputStream(fileInputStream);
            TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(gzipInputStream);
            TarArchiveEntry tarArchiveEntry = null;
            if ((tarArchiveEntry = tarArchiveInputStream.getNextTarEntry()) != null) {
                br = new BufferedReader(new InputStreamReader(tarArchiveInputStream));
            }
            else {
                System.exit(-1);
            }
            br = new BufferedReader(new FileReader(tarArchiveEntry.getFile()));
            OutputStream os = Files.newOutputStream(out.toPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            GZIPOutputStream gzip = new GZIPOutputStream(os);
            OutputStreamWriter ow = new OutputStreamWriter(gzip, StandardCharsets.UTF_8);
            bw = new BufferedWriter(ow);
        }
        else {
            FileReader fr = new FileReader(in);
            br = new BufferedReader(fr);
            FileWriter fw = new FileWriter(out);
            bw = new BufferedWriter(fw);
        }


        Set<String> distinctQueries = new TreeSet<>();
        while ((s = br.readLine()) != null) {
            String q = "";
            try {
                SingleQuery sq = new SingleQuery(s, canonicalisation, rewrite, minimisation, true, false);
                q = sq.getQuery();
            }
            catch (Exception e) {
                e.printStackTrace();
                System.err.println("Failed at: \n" + s);
            }
            q = q.replace("\n"," ");
            if (!distinct || distinctQueries.add(q)) {
            	if(keep) {
            		bw.append(s);
            	} else {
            		bw.append(q);
            	}
            	bw.newLine();
            }
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
        Option option_D = new Option("d",false,"Set to avoid writing duplicate queries in output file.");
        Option option_G = new Option("g",false,"Set if input is gzip file. Results will also be zipped.");
        Option option_K = new Option("k",false,"Keep original query syntax");
        option_F.setArgName("filename");
        option_Q.setArgName("query");
        option_O.setArgName("output");
        Options options = new Options();
        options.addOption(option_F);
        options.addOption(option_Q);
        options.addOption(option_M);
        options.addOption(option_O);
        options.addOption(option_D);
        options.addOption(option_G);
        options.addOption(option_K);
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("easy", header, options, footer, true);
        try {
            commandLine = parser.parse(options,args);
            boolean minimisation = commandLine.hasOption("m");
            EasyCanonicalisation ec = new EasyCanonicalisation();
            ec.minimisation = minimisation;
            ec.gZipped = commandLine.hasOption("g");
            if (commandLine.hasOption("f")) {
                String filename = commandLine.getOptionValue("f");
                String output = "";
                boolean distinct = commandLine.hasOption("d");
                boolean keep = commandLine.hasOption("k");
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
                ec.canonicaliseFile(f,out,distinct,keep);
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
