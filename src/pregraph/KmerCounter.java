package pregraph;

import buildgraph.Ordering.IOrdering;
import buildgraph.Ordering.UniversalFrequencySignatureOrdering;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;

public class KmerCounter {

    public static void main(String[] args) throws IOException {

//    	String infile = "/home/gaga/data-scratch/yaelbenari/datas/chr14.fastq";
//		String infile = "/home/gaga/data-scratch/yaelbenari/datas/smalldata.fastq";
//		String infile = "/home/gaga/data-scratch/yaelbenari/datas/breastCancer.fastq";
//		String infile = "/home/gaga/data-scratch/yaelbenari/datas/beeData.fastq";
        String infile = "/home/gaga/data-scratch/yaelbenari/datas/workspace/72.fastq";

        int k = 55, pivot_len = 8, bufferSize = 8192, numThreads = 30, hsmapCapacity = 10000000;
        int readLen = 100;
        int numBlocks = 4000;//(int)Math.pow(4, pivot_len);//256; 1000;//
        boolean readable = false;
        String orderingName = "uhs_freq_sig";
        int xor = 0; //11101101;


        for (int i = 0; i < args.length; i += 2) {
//    		if(args[i].equals("-in"))
//    			infile = args[i+1];
//    		else if(args[i].equals("-k"))
//    			k = new Integer(args[i+1]);
//    		else if(args[i].equals("-NB"))
//    			numBlocks = new Integer(args[i+1]);
            //else
//				if(args[i].equals("-o"))
//				orderingName = args[i+1];
//    		else if(args[i].equals("-p"))
//    			pivot_len = new Integer(args[i+1]);
//    		else if(args[i].equals("-b"))
//    			bufferSize = new Integer(args[i+1]);
//    		else if(args[i].equals("-L"))
//    			readLen = new Integer(args[i+1]);
//    		else if(args[i].equals("-t"))
//    			numThreads = new Integer(args[i+1]);
//    		else if(args[i].equals("-r"))
//    			readable = new Boolean(args[i+1]);
//    		else{
//                System.out.println("Wrong with arguments. Abort!");
//                return;
//            }
        }


        IOrdering ordering = new UniversalFrequencySignatureOrdering(pivot_len, infile, readLen, bufferSize, true, true);
        Partition partition = new Partition(k, infile, numBlocks, pivot_len, bufferSize, readLen, ordering);
        Count64 map = new Count64(k, numBlocks, bufferSize, hsmapCapacity);

        try {

            System.out.println("Program Configuration:");
            System.out.print("Input File: " + infile + "\n" +
                    "Kmer Length: " + k + "\n" +
                    "Read Length: " + readLen + "\n" +
                    "# Of Blocks: " + numBlocks + "\n" +
                    "Pivot Length: " + pivot_len + "\n" +
                    "# Of Threads: " + numThreads + "\n" +
                    "R/W Buffer Size: " + bufferSize + "\n" +
                    "Ordering: " + orderingName + "\n" +
                    "x xor: " + xor + "\n" +
                    "Output Format: " + (readable == true ? "Text" : "Binary") + "\n");


            partition.Run(numThreads);

//            AbstractMap<Long, Long> distinctKmersPerPartition =
            map.Run(numThreads);
//            buildgraph.BuildDeBruijnGraph.writeToFile(distinctKmersPerPartition, orderingName + pivot_len + "_" + "kmers");

//            HashMap<Long, Long> bytesPerFile = buildgraph.BuildDeBruijnGraph.getBytesPerFile();
//            buildgraph.BuildDeBruijnGraph.writeToFile(bytesPerFile, orderingName + pivot_len + "_" + "bytes");


        } catch (Exception E) {
            System.out.println("Exception caught!");
            E.printStackTrace();
        }

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

