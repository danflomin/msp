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

        String infile = null;

        int k = 60, pivot_len = 8, bufferSize = 81920, numThreads = 20, hsmapCapacity = 10000000;
        int readLen = 124;
        int numBlocks = (int)Math.pow(4, pivot_len);//256; 1000;//
//        boolean readable = false;
        String orderingName = "uhs_sig_freq";
        int xor = 0; //11101101;
        int numRounds = 0, elementsToPush = 0, samplesPerRound = 0, statSamples = 0;
        double punishPercentage = 1;
        String version = "10";

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
            if (args[i].equals("-in"))
                infile = args[i + 1];
            else if (args[i].equals("-v"))
                version = args[i + 1];
            else if (args[i].equals("-k"))
                k = new Integer(args[i + 1]);
    		else if(args[i].equals("-NB"))
    			numBlocks = new Integer(args[i+1]);
//            else
//				if(args[i].equals("-o"))
//				orderingName = args[i+1];
            else if (args[i].equals("-p"))
                pivot_len = new Integer(args[i + 1]);
            else if (args[i].equals("-b"))
                bufferSize = new Integer(args[i + 1]);
            else if (args[i].equals("-L"))
                readLen = new Integer(args[i + 1]);
            else if (args[i].equals("-t"))
                numThreads = new Integer(args[i + 1]);
//    		else if(args[i].equals("-r"))
//    			readable = new Boolean(args[i+1]);
            else if (args[i].equals("-rounds"))
                numRounds = new Integer(args[i + 1]);
            else if (args[i].equals("-samples"))
                samplesPerRound = new Integer(args[i + 1]);
            else if (args[i].equals("-elementsToPush"))
                elementsToPush = new Integer(args[i + 1]);
            else if (args[i].equals("-statSamples"))
                statSamples = new Integer(args[i + 1]);
            else if (args[i].equals("-punishPercentage"))
                punishPercentage = new Double(args[i + 1]);
            else {
                System.out.println("Wrong with arguments. Abort!");
                System.out.println(args[i]);
                return;
            }
        }


        orderingName = "iterativeOrdering";
//        IterativeOrdering ordering = new IterativeOrdering(pivot_len, infile, readLen, bufferSize, k); /// this is the first version 100000, 10000, 1
//        ordering.initFrequency();
//        IterativeOrdering ordering = new IterativeOrdering(pivot_len, infile, readLen, bufferSize, k, 25000, 30000, 1, 10);
//        IterativeOrdering ordering = new IterativeOrdering(pivot_len, infile, readLen, bufferSize, k, 25000, 100000, 1, 10);
//        IterativeOrdering ordering = new IterativeOrdering(pivot_len, infile, readLen, bufferSize, k, 25000, 100000, 1, (int)Math.pow(4,pivot_len)/100);

//        IterativeOrdering3 ordering = new IterativeOrdering3(pivot_len, infile, readLen, bufferSize, k);

//        IterativeOrdering2 ordering = new IterativeOrdering2(pivot_len, infile, readLen, bufferSize, k, 100000, 10000, 5, (int)Math.pow(4,pivot_len)/100);

//        ordering.initFrequency();

//        UHSFrequencySignatureOrdering ordering = new UHSFrequencySignatureOrdering(pivot_len, infile, readLen, bufferSize, true);
//        ordering.initRank();

//        IterativeOrdering3 ordering = new IterativeOrdering3(pivot_len, infile, readLen, bufferSize, k, samplesPerRound, numRounds,elementsToPush, statSamples);
//        IterativeOrdering4 ordering = new IterativeOrdering4(pivot_len, infile, readLen, bufferSize, k, samplesPerRound, numRounds,elementsToPush, statSamples, maskRatio, punishPercentage);
//        IterativeOrdering6 ordering = new IterativeOrdering6(pivot_len, infile, readLen, bufferSize, k, samplesPerRound, numRounds,elementsToPush, statSamples, maskRatio, punishPercentage);
//        IterativeUHSOrdering8 ordering = new IterativeUHSOrdering8(pivot_len, infile, readLen, bufferSize, k, samplesPerRound, numRounds,elementsToPush, statSamples, maskRatio, punishPercentage);
//        IterativeOrdering8 ordering = new IterativeOrdering8(pivot_len, infile, readLen, bufferSize, k, samplesPerRound, numRounds, elementsToPush, statSamples, punishPercentage);

//        GOOD

        IOrdering ordering = null;
        System.out.println(version);
        switch(version)
        {
            case "9": // good version
                IterativeOrdering9 ordering9 = new IterativeOrdering9(pivot_len, infile, readLen, bufferSize, k, samplesPerRound, numRounds, elementsToPush, statSamples, punishPercentage);
                ordering9.initFrequency();
                ordering9.exportOrderingForCpp();
                ordering9.exportBinningForCpp();
                ordering = ordering9;
                break;
            case "9-normalized": // good version
                IterativeOrdering9_WithCounterNormalized ordering9_withCounterNormalized = new IterativeOrdering9_WithCounterNormalized(pivot_len, infile, readLen, bufferSize, k, samplesPerRound, numRounds, elementsToPush, statSamples, punishPercentage);
                ordering9_withCounterNormalized.initFrequency();
                ordering9_withCounterNormalized.exportOrderingForCpp();
                ordering9_withCounterNormalized.exportBinningForCpp();
                ordering = ordering9_withCounterNormalized;
                break;
            case "9-normalized-signature": //
                IterativeOrdering9_WithCounterNormalized_AndSignature ordering9_withCounterNormalized_andSignature = new IterativeOrdering9_WithCounterNormalized_AndSignature(pivot_len, infile, readLen, bufferSize, k, samplesPerRound, numRounds, elementsToPush, statSamples, punishPercentage);
                ordering9_withCounterNormalized_andSignature.initFrequency();
                ordering9_withCounterNormalized_andSignature.exportOrderingForCpp();
                ordering9_withCounterNormalized_andSignature.exportBinningForCpp();
                ordering = ordering9_withCounterNormalized_andSignature;
                System.out.println("lolz asdasd");
                break;
            case "10":
                IterativeOrdering10_WithCounterNormalized ordering10 = new IterativeOrdering10_WithCounterNormalized(pivot_len, infile, readLen, bufferSize, k, samplesPerRound, numRounds, elementsToPush, statSamples, punishPercentage);
                ordering10.initFrequency();
                ordering10.exportOrderingForCpp();
                ordering10.exportBinningForCpp();
                ordering = ordering10;
                break;
            case "universal-frequency-signature":
                UHSFrequencySignatureOrdering universalFrequencySignature = new UHSFrequencySignatureOrdering(pivot_len, infile,readLen, bufferSize, true, k, statSamples);;
                universalFrequencySignature.initRank();
                universalFrequencySignature.exportOrderingForCpp();
                universalFrequencySignature.exportBinningForCpp();
                ordering = universalFrequencySignature;
                break;
            case "universal-frequency":
                UHSFrequencySignatureOrdering universalFrequency = new UHSFrequencySignatureOrdering(pivot_len, infile,readLen, bufferSize, false, k, statSamples);;
                universalFrequency.initRank();
                universalFrequency.exportOrderingForCpp();
                universalFrequency.exportBinningForCpp();
                ordering = universalFrequency;
                break;
            case "frequency": //   FREQUENCY SUCKS
                FrequencyOrdering frequencyOrdering = new FrequencyOrdering(pivot_len, infile, readLen, bufferSize, numRounds*samplesPerRound, statSamples, k);
                frequencyOrdering.initFrequency();
                ordering = frequencyOrdering;
                break;
            case "signature":
                LexicographicSignatureOrdering signatureOrdering = new LexicographicSignatureOrdering(pivot_len);
                ordering = signatureOrdering;
                break;
        }

        try {

            System.out.println("Program Configuration:");
            System.out.print("Input File: " + infile + "\n" +
                    "Kmer Length: " + k + "\n" +
                    "Read Length: " + readLen + "\n" +
                    "Pivot Length: " + pivot_len + "\n" +
                    "# Of Threads: " + numThreads + "\n" +
                    "R/W Buffer Size: " + bufferSize + "\n" +
                    "Ordering: " + orderingName + "\n");

//            Partition partition = new Partition(k, infile, numBlocks, pivot_len, bufferSize, readLen, ordering);
            PartitionTrunc partition = new PartitionTrunc(k, infile, numBlocks, pivot_len, bufferSize, readLen, ordering);
            Map map = new Map(k, (int)Math.pow(4, pivot_len), bufferSize, hsmapCapacity);
//            MapTrunc map = new MapTrunc(k, (int)Math.pow(4, pivot_len), bufferSize, hsmapCapacity);


            partition.Run();

            AbstractMap<Long, Long> distinctKmersPerPartition = map.Run(numThreads);
            BuildDeBruijnGraph.writeToFile(distinctKmersPerPartition, orderingName + pivot_len + "_" + "kmers");
            System.out.println("TOTAL NUMBER OF DISTINCT KMERS = " + distinctKmersPerPartition.values().stream().mapToLong(Long::longValue).sum());

            HashMap<Long, Long> bytesPerFile = BuildDeBruijnGraph.getBytesPerFile();
            BuildDeBruijnGraph.writeToFile(bytesPerFile, orderingName + pivot_len + "_" + "bytes");

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
