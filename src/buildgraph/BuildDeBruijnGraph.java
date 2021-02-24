package buildgraph;

import buildgraph.Ordering.*;
import buildgraph.Ordering.UHS.UHSFrequencySignatureOrdering;
import buildgraph.Ordering.UHS.UHSSignatureOrdering;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.HashMap;

public class BuildDeBruijnGraph {

    public static void main(String[] args) throws IOException {

//    	String infile = "/home/gaga/data-scratch/yaelbenari/datas/chr14.fastq";
//		String infile = "/home/gaga/data-scratch/yaelbenari/datas/smalldata.fastq";
        String infile = "/home/gaga/data-scratch/yaelbenari/datas/breastCancer.fastq";
//		String infile = "/home/gaga/data-scratch/yaelbenari/datas/beeData.fastq";
//		String infile = "/home/gaga/data-scratch/yaelbenari/datas/workspace/72.fastq";

        int k = 60, pivot_len = 8, bufferSize = 8192, numThreads = 20, hsmapCapacity = 10000000;
//    	int readLen = 124;
//		int readLen = 101;
        int readLen = 100;
        int numBlocks = (int)Math.pow(4, pivot_len);//256; 1000;//
        boolean readable = false;
        String orderingName = "uhs_sig_freq";
        int xor = 0; //11101101;

        if (args.length > 0 && args[0].equals("-help")) {
            System.out.print("Usage: java -jar BuildDeBruijnGraph.jar -in InputPath -k k -L readLength[options]\n" +
                    "Options Available: \n" +
                    "[-NB numOfBlocks] : (Integer) Number Of Kmer Blocks. Default: 256" + "\n" +
                    "[-p pivotLength] : (Integer) Pivot Length. Default: 12" + "\n" +
                    "[-t numOfThreads] : (Integer) Number Of Threads. Default: 1" + "\n" +
                    "[-b bufferSize] : (Integer) Read/Writer Buffer Size. Default: 8192" + "\n" +
                    "[-o order] : lexico or sig or uhs or uhs_sig" + "\n" +
                    "[-r readable] : (Boolean) Output Format: true means readable text, false means binary. Default: false" + "\n");
            return;
        }

        for (int i = 0; i < args.length; i += 2) {
    		if(args[i].equals("-in"))
    			infile = args[i+1];
    		else if(args[i].equals("-k"))
    			k = new Integer(args[i+1]);
    		else if(args[i].equals("-NB"))
    			numBlocks = new Integer(args[i+1]);
//            else
//				if(args[i].equals("-o"))
//				orderingName = args[i+1];
    		else if(args[i].equals("-p"))
    			pivot_len = new Integer(args[i+1]);
    		else if(args[i].equals("-b"))
    			bufferSize = new Integer(args[i+1]);
    		else if(args[i].equals("-L"))
    			readLen = new Integer(args[i+1]);
    		else if(args[i].equals("-t"))
    			numThreads = new Integer(args[i+1]);
    		else if(args[i].equals("-r"))
    			readable = new Boolean(args[i+1]);
    		else{
                System.out.println("Wrong with arguments. Abort!");
                return;
            }
        }



        orderingName = "iterativeOrdering";
        IterativeOrdering ordering = new IterativeOrdering(pivot_len, infile, readLen, bufferSize, k); /// this is the first version 100000, 10000, 1
        ordering.initFrequency();
//        IterativeOrdering ordering = new IterativeOrdering(pivot_len, infile, readLen, bufferSize, k, 25000, 30000, 1, 10);
//        IterativeOrdering ordering = new IterativeOrdering(pivot_len, infile, readLen, bufferSize, k, 25000, 100000, 1, 10);
//        IterativeOrdering ordering = new IterativeOrdering(pivot_len, infile, readLen, bufferSize, k, 25000, 100000, 1, (int)Math.pow(4,pivot_len)/100);

//        IterativeOrdering3 ordering = new IterativeOrdering3(pivot_len, infile, readLen, bufferSize, k);

//        IterativeOrdering2 ordering = new IterativeOrdering2(pivot_len, infile, readLen, bufferSize, k, 100000, 10000, 5, (int)Math.pow(4,pivot_len)/100);

//        ordering.initFrequency();

//        UHSFrequencySignatureOrdering ordering = new UHSFrequencySignatureOrdering(pivot_len, infile, readLen, bufferSize, true);
//        ordering.initRank();

        ordering.exportOrderingForCpp();
        ordering.exportBinningForCpp();

//        try {
//
//            System.out.println("Program Configuration:");
//            System.out.print("Input File: " + infile + "\n" +
//                    "Kmer Length: " + k + "\n" +
//                    "Read Length: " + readLen + "\n" +
//                    "# Of Blocks: " + numBlocks + "\n" +
//                    "Pivot Length: " + pivot_len + "\n" +
//                    "# Of Threads: " + numThreads + "\n" +
//                    "R/W Buffer Size: " + bufferSize + "\n" +
//                    "Ordering: " + orderingName + "\n" +
//                    "x xor: " + xor + "\n" +
//                    "Output Format: " + (readable == true ? "Text" : "Binary") + "\n");
//
//            Partition partition = new Partition(k, infile, numBlocks, pivot_len, bufferSize, readLen, ordering);
//            Map map = new Map(k, numBlocks, bufferSize, hsmapCapacity);
//
//
//            long maxID = partition.Run();
//
//            AbstractMap<Long, Long> distinctKmersPerPartition = map.Run(numThreads);
//            BuildDeBruijnGraph.writeToFile(distinctKmersPerPartition, orderingName + pivot_len + "_" + "kmers");
//            System.out.println("TOTAL NUMBER OF DISTINCT KMERS = " + distinctKmersPerPartition.values().stream().mapToLong(Long::longValue).sum());
//
//            HashMap<Long, Long> bytesPerFile = BuildDeBruijnGraph.getBytesPerFile();
//            BuildDeBruijnGraph.writeToFile(bytesPerFile, orderingName + pivot_len + "_" + "bytes");
////
////
////            long time1 = 0;
////            long t1 = System.currentTimeMillis();
////            System.out.println("Merge IDReplaceTables Begin!");
////            String sortcmd = "sort -t $\'\t\' -o IDReplaceTable +0 -1 -n -m Maps/maps*";
////            Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", sortcmd}, null, null).waitFor();
////            long t2 = System.currentTimeMillis();
////            time1 = (t2 - t1) / 1000;
////            System.out.println("Time used for merging: " + time1 + " seconds!");
////
////            Replace replace = new Replace("IDReplaceTable", "OutGraph", k, bufferSize, readLen, maxID);
////            replace.Run(readable);
//
//
//        } catch (Exception E) {
//            System.out.println("Exception caught!");
//            E.printStackTrace();
//        }

    }

    public static HashMap<Long, Long> getBytesPerFile() {
        File folder = new File("./Nodes");
        File[] listOfFiles = folder.listFiles();

        HashMap<Long, Long> bytesPerFile = new HashMap<>();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile())
                bytesPerFile.put(Long.parseLong(listOfFiles[i].getName().replace("nodes", "")), listOfFiles[i].length());
        }
        return bytesPerFile;
    }

    public static void writeToFile(AbstractMap<Long, Long> data, String fileName) {
        File file = new File(fileName);

        BufferedWriter bf = null;
        ;

        try {
            bf = new BufferedWriter(new FileWriter(file));

            bf.write("x = {");
            bf.newLine();

            //iterate map entries
            for (java.util.Map.Entry<Long, Long> entry : data.entrySet()) {
                bf.write(entry.getKey() + ":" + entry.getValue() + ",");
                bf.newLine();
            }
            bf.write("}");
            bf.flush();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            try {
                //always close the writer
                bf.close();
            } catch (Exception e) {
            }
        }

    }

}
