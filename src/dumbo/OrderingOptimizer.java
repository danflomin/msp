package dumbo;

import dumbo.Ordering.*;
import dumbo.Ordering.UHS.UHSFrequencySignatureOrdering;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;

public class OrderingOptimizer {

    public static void main(String[] args) throws IOException {

        String infile = null;

        int k = 60, pivot_len = 8, bufferSize = 81920;
        int readLen = 124;
        String orderingName = "uhs_sig_freq";
        int numRounds = 0, elementsToPush = 0, samplesPerRound = 0, statSamples = 0;
        double punishPercentage = 1;
        String version = "10";
        String kmerSetFile = null;

        if (args.length > 0 && args[0].equals("-help")) {
            System.out.print("Usage: java -jar BuildDeBruijnGraph.jar -in InputPath -k k -L readLength[options]\n" +
                    "Options Available: \n" +
                    "[-p pivotLength] : (Integer) Pivot Length. Default: 12" + "\n" +
                    "[-b bufferSize] : (Integer) Read/Writer Buffer Size. Default: 8192" + "\n" +
                    "[-o order] : lexico or sig or uhs or uhs_sig" + "\n");
            return;
        }

        for (int i = 0; i < args.length; i += 2) {
            if (args[i].equals("-in"))
                infile = args[i + 1];
            else if (args[i].equals("-v"))
                version = args[i + 1];
            else if (args[i].equals("-k"))
                k = new Integer(args[i + 1]);
            else if (args[i].equals("-kmers-file"))
                kmerSetFile = args[i + 1];
//            else
//				if(args[i].equals("-o"))
//				orderingName = args[i+1];
            else if (args[i].equals("-p"))
                pivot_len = new Integer(args[i + 1]);
            else if (args[i].equals("-b"))
                bufferSize = new Integer(args[i + 1]);
            else if (args[i].equals("-L"))
                readLen = new Integer(args[i + 1]);
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

        System.out.println("Optimizing an ordering:");
        System.out.print("Input File: " + kmerSetFile + "\n" +
                "Kmer Length: " + k + "\n" +
                "Pivot Length: " + pivot_len + "\n" +
                "R/W Buffer Size: " + bufferSize + "\n" +
                "Read length" + readLen + "\n" +
                "Ordering: " + orderingName + "\n");


        orderingName = "iterativeOrdering";


        IOrderingPP ordering = null;
        System.out.println(version);
        switch (version) {

            case "9-normalized": // good version
                IterativeOrdering9_WithCounterNormalized_AndSignature iterative = new IterativeOrdering9_WithCounterNormalized_AndSignature(pivot_len, infile, readLen, bufferSize, k, samplesPerRound, numRounds, elementsToPush, statSamples, punishPercentage, false);
                iterative.initFrequency();
//                ordering9_withCounterNormalized.exportOrderingForCpp();
//                ordering9_withCounterNormalized.exportBinningForCpp();
                ordering = iterative;
                break;
            case "9-normalized-signature": //
                IterativeOrdering9_WithCounterNormalized_AndSignature iterativeSignature = new IterativeOrdering9_WithCounterNormalized_AndSignature(pivot_len, infile, readLen, bufferSize, k, samplesPerRound, numRounds, elementsToPush, statSamples, punishPercentage, true);
                iterativeSignature.initFrequency();
//                ordering9_withCounterNormalized_andSignature.exportOrderingForCpp();
//                ordering9_withCounterNormalized_andSignature.exportBinningForCpp();
                ordering = iterativeSignature;
                System.out.println("lolz asdasd");
                break;
            case "10":
                IterativeOrdering10_WithCounterNormalized ordering10 = new IterativeOrdering10_WithCounterNormalized(pivot_len, infile, readLen, bufferSize, k, samplesPerRound, numRounds, elementsToPush, statSamples, punishPercentage);
                ordering10.initFrequency();
                ordering10.exportOrderingForCpp();
                ordering10.exportBinningForCpp();
//                ordering = ordering10;
                break;
            case "universal-frequency-signature":
                UHSFrequencySignatureOrdering universalFrequencySignature = new UHSFrequencySignatureOrdering(pivot_len, infile, readLen, bufferSize, true, k, statSamples);
                ;
                universalFrequencySignature.initRank();
                universalFrequencySignature.exportOrderingForCpp();
                universalFrequencySignature.exportBinningForCpp();
//                ordering = universalFrequencySignature;
                break;
            case "universal-frequency":
                UHSFrequencySignatureOrdering universalFrequency = new UHSFrequencySignatureOrdering(pivot_len, infile, readLen, bufferSize, false, k, statSamples);
                ;
                universalFrequency.initRank();
                universalFrequency.exportOrderingForCpp();
                universalFrequency.exportBinningForCpp();
//                ordering = universalFrequency;
                break;
            case "frequency": //   FREQUENCY SUCKS
                FrequencyOrdering frequencyOrdering = new FrequencyOrdering(pivot_len, infile, readLen, bufferSize, numRounds * samplesPerRound, statSamples, k);
                frequencyOrdering.initFrequency();
//                ordering = frequencyOrdering;
                break;
            case "signature":
                LexicographicSignatureOrdering signatureOrdering = new LexicographicSignatureOrdering(pivot_len);
//                ordering = signatureOrdering;
                break;
        }

        if (kmerSetFile != null) {
            try {
                ExportUtils exportUtils = new ExportUtils();
                System.out.println("Counting minimizer appearances:");
                System.out.print("Input File: " + kmerSetFile + "\n" +
                        "Kmer Length: " + k + "\n" +
                        "Pivot Length: " + pivot_len + "\n" +
                        "R/W Buffer Size: " + bufferSize + "\n" +
                        "Ordering: " + orderingName + "\n");

                MinimizerCounter minimizerCounter = new MinimizerCounter(k, kmerSetFile, pivot_len, bufferSize, ordering);
                long[] counters = minimizerCounter.Run();
                exportUtils.writeToFile(counters, orderingName + pivot_len + "_" + "kmers");
                System.out.println("TOTAL NUMBER OF DISTINCT KMERS = " + Arrays.stream(counters).sum());


            } catch (Exception E) {
                System.out.println("Exception caught!");
                E.printStackTrace();
            }
        }
    }


}
