package dumbo;

import dumbo.Ordering.*;
import dumbo.Ordering.Standard.FrequencyOrdering;
import dumbo.Ordering.Standard.LexicographicOrdering;
import dumbo.Ordering.Standard.LexicographicSignatureOrdering;
import dumbo.Ordering.UHS.UHSFrequencySignatureOrdering;

import java.io.IOException;
import java.util.Arrays;

public class OrderingOptimizer {

    public static void main(String[] args) throws Exception {

        String infile = null;

        int k = 60, pivot_len = 8, bufferSize = 81920;
        int readLen = 124;
        String orderingName = "iterativeOrdering";
        int numRounds = 0, elementsToPush = 0, samplesPerRound = 0;
        long statSamples = 0;
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
                statSamples = new Long(args[i + 1]);
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



        OrderingBase ordering = null;
        System.out.println(version);
        switch (version) {

            case "9-normalized": // good version
                IterativeOrdering iterative = new IterativeOrdering(pivot_len, infile, readLen, bufferSize, k,
                        samplesPerRound, numRounds, elementsToPush, 0, punishPercentage, false);
                iterative.initializeRanks();
                ordering = iterative;
                break;
            case "9-frequency":
                FrequencyOrdering _frequencyOrdering = new FrequencyOrdering(pivot_len, infile, readLen, bufferSize, 100000000);
                _frequencyOrdering.initializeRanks();

                IterativeOrdering iterativeFrequency = new IterativeOrdering(pivot_len, infile, readLen, bufferSize, k,
                        samplesPerRound, numRounds, elementsToPush, 0, punishPercentage, false, _frequencyOrdering);
                iterativeFrequency.initializeRanks();
                ordering = iterativeFrequency;
                break;
            case "split-frequency":
                FrequencyOrdering _frequencyOrdering2 = new FrequencyOrdering(pivot_len, infile, readLen, bufferSize, 1000000000);
                _frequencyOrdering2.initializeRanks();

                IterativeOrderingV2 iterative2Frequency = new IterativeOrderingV2(pivot_len, infile, readLen, bufferSize, k,
                        samplesPerRound, numRounds, elementsToPush, false, _frequencyOrdering2);
                iterative2Frequency.initializeRanks();
                ordering = iterative2Frequency;
                break;
            case "9-normalized-signature":
                IterativeOrdering iterativeSignature = new IterativeOrdering(pivot_len, infile, readLen, bufferSize, k,
                        samplesPerRound, numRounds, elementsToPush, 0, punishPercentage, true);
                iterativeSignature.initializeRanks();
                ordering = iterativeSignature;
                System.out.println("lolz asdasd");
                break;
            case "universal-frequency-signature":
                UHSFrequencySignatureOrdering universalFrequencySignature = new UHSFrequencySignatureOrdering(pivot_len, infile, readLen, bufferSize, true, 100000000);
                universalFrequencySignature.initializeRanks();
                ordering = universalFrequencySignature;
                break;
            case "universal-frequency":
                UHSFrequencySignatureOrdering universalFrequency = new UHSFrequencySignatureOrdering(pivot_len, infile, readLen, bufferSize, false, 100000000);
                universalFrequency.initializeRanks();
                ordering = universalFrequency;
                break;
            case "frequency": //   FREQUENCY SUCKS
                FrequencyOrdering frequencyOrdering = new FrequencyOrdering(pivot_len, infile, readLen, bufferSize, numRounds * samplesPerRound);
                frequencyOrdering.initializeRanks();
                ordering = frequencyOrdering;
                break;
            case "signature":
                ordering = new LexicographicSignatureOrdering(pivot_len);
                ordering.initializeRanks();
                break;
            case "lexicographic":
                ordering = new LexicographicOrdering(pivot_len);
                ordering.initializeRanks();
                break;
        }

        ExportUtils exportUtils = new ExportUtils();

        int[] ranks = ordering.getRanks();
        long[] longRanks = new long[ranks.length];
        for (int i = 0; i < longRanks.length; longRanks[i]=ranks[i], i++) ;

        exportUtils.exportOrderingForCpp(longRanks);

        if (kmerSetFile != null) {
            try {

                System.out.println("Counting minimizer appearances:");
                System.out.print("Input File: " + kmerSetFile + "\n" +
                        "Kmer Length: " + k + "\n" +
                        "Pivot Length: " + pivot_len + "\n" +
                        "R/W Buffer Size: " + bufferSize + "\n" +
                        "Ordering: " + orderingName + "\n");

//                MinimizerCounter minimizerCounter = new MinimizerCounter(k, kmerSetFile, pivot_len, bufferSize, ordering);
//                long[] counters = minimizerCounter.Run();

//                LoadCounter counter = new LoadCounter(pivot_len, infile, readLen, bufferSize, k, statSamples, ordering);
//                counter.initFrequency();

                BinSizeCounter counter = new BinSizeCounter(pivot_len, infile, readLen, bufferSize, k, statSamples, ordering);
                counter.initFrequency();

                long[] counters = counter.getStatistics();

                exportUtils.writeToFile(counters, orderingName + pivot_len + "_" + "kmers");
                System.out.println("TOTAL NUMBER OF DISTINCT KMERS = " + Arrays.stream(counters).sum());
                exportUtils.exportBinningForCpp(counters);


            } catch (Exception E) {
                System.out.println("Exception caught!");
                E.printStackTrace();
            }
        }
    }


}
