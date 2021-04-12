package dumbo;

import dumbo.Ordering.OrderingBase;
import dumbo.Ordering.Standard.SignatureUtils;
import dumbo.StringUtils;
import net.agkn.hll.HLL;
import net.agkn.hll.HLLType;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import java.security.MessageDigest;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class LoadCounter {
    private String inputFile;
    private int readLen;
    private int bufSize;
    private int k;
    private HashMap<Integer, HLL> frequency;
    private Object[] frequencyLocks;
    private int[] frequencies;
    private OrderingBase ordering;

    private StringUtils stringUtils;

    private int pivotLength;
    private long statisticsSamples;

    private int mask;
    private int numMmers;




    public LoadCounter(
            int pivotLength, String infile, int readLen, int bufSize, int k, long statisticsSamples, OrderingBase ordering) {
        this.pivotLength = pivotLength;
        this.statisticsSamples = statisticsSamples;
        this.inputFile = infile;
        this.readLen = readLen;
        this.bufSize = bufSize;
        this.k = k;
        numMmers = (int) Math.pow(4, pivotLength);
        frequency = new HashMap<>(numMmers);
        frequencies = new int[numMmers];
        this.ordering = ordering;
        stringUtils = new StringUtils();
        mask = numMmers - 1;
        frequencyLocks = new Object[numMmers + 1];
        for (int i = 0; i < frequencyLocks.length - 1; i++) {
            frequencyLocks[i] = new Object();
        }
    }


    private void concurrentCounter(char[] lineCharArray) throws Exception {
        int min_pos, minValue, minValueNormalized, currentValue, numSampled = 0;

        String line = new String(lineCharArray);

        if (stringUtils.isReadLegal(lineCharArray)) {

            min_pos = ordering.findSmallest(lineCharArray, 0, k);
            minValue = stringUtils.getDecimal(lineCharArray, min_pos, min_pos + pivotLength);
            minValueNormalized = stringUtils.getNormalizedValue(minValue, pivotLength);
            currentValue = stringUtils.getDecimal(lineCharArray, k - pivotLength, k);

            updateStatistics(minValueNormalized, line, 0);

            int bound = readLen - k + 1;
            for (int i = 1; i < bound; i++) {
                numSampled++;
                currentValue = ((currentValue << 2) + StringUtils.valTable[lineCharArray[i + k - 1] - 'A']) & mask;

                if (i > min_pos) {
                    min_pos = ordering.findSmallest(lineCharArray, i, i + k);
                    minValue = stringUtils.getDecimal(lineCharArray, min_pos, min_pos + pivotLength);
                    minValueNormalized = stringUtils.getNormalizedValue(minValue, pivotLength);
                } else {
                    int lastIndexInWindow = k + i - pivotLength;
                    if (ordering.compareMmer(currentValue, minValue) < 0) {
                        min_pos = lastIndexInWindow;
                        minValue = currentValue;
                        minValueNormalized = stringUtils.getNormalizedValue(minValue, pivotLength);
                    }
                }
                updateStatistics(minValueNormalized, line, i);
            }
        }
    }


    protected void initFrequency() throws Exception {


        boolean keepSample = true;
        long numSampled = 0;
        int roundNumber = 0;

        FileReader frG = new FileReader(inputFile);
        BufferedReader bfrG = new BufferedReader(frG, bufSize);


        String describeline;
        char[] lineCharArray = new char[readLen];

        ThreadPoolExecutor executor =
                (ThreadPoolExecutor) Executors.newFixedThreadPool(1);


        while (keepSample && (describeline = bfrG.readLine()) != null) {

            bfrG.read(lineCharArray, 0, readLen);
            bfrG.read();

//            char[] localLineCharArray = lineCharArray.clone();
//            executor.submit(() -> {
//                concurrentCounter(localLineCharArray);
//                return null;
//            });

            concurrentCounter(lineCharArray);
            numSampled += readLen - k;
            if (numSampled > statisticsSamples)
                keepSample = false;

        }

        executor.shutdown();
        bfrG.close();
        frG.close();
    }

    private void updateStatistics(int minValueNormalized, String line, int startPosition) {
//        synchronized (frequencyLocks[minValueNormalized])
//        {
        if (!frequency.containsKey(minValueNormalized))
//                frequency.put(minValueNormalized, new HLL(11, 5)); /// about 3gb of ram before going to sparse
            frequency.put(minValueNormalized, new HLL(11, 5, 0, true, HLLType.FULL));
        frequency.get(minValueNormalized).addRaw(hashString(stringUtils.getCanonical(line.substring(startPosition, k + startPosition))));
//        }
        //synchronized (frequencyLocks[numMmers])
        frequencies[minValueNormalized]++;


    }

    private long hashString(String s) {
        long h = 1125899906842597L; // prime
        int len = s.length();
        for (int i = 0; i < len; i++) {
            h = 31 * h + s.charAt(i);
        }
        return h;
    }


    public long[] getStatistics() {
        long[] stats = new long[numMmers];
        for (int i = 0; i < numMmers; i++) {
            if (frequency.containsKey(i)) {
                stats[i] = frequency.get(i).cardinality();
            }
//            if (i < stringUtils.getReversedMmer(i, pivotLength)) {
//                stats[i] += 1000;
//            }
        }



        // pure counters
//        System.out.println("x = [");
//        for (int i = 0; i < stats.length; i++) {
//            System.out.print(stats[i]+ ", ");
//        }
//        System.out.println("]");


        // all ratios
        System.out.println("x = [");
        for (int j = 0; j < stats.length; j++) {
            if(frequencies[j] != 0)
                System.out.print((float) stats[j] / frequencies[j] + ", ");
            else
                System.out.print("0, ");
        }
        System.out.println("]");
//        ConcurrentLinkedQueue<Integer>x = new ConcurrentLinkedQueue<>();
//        x.remove()

//        long max = Arrays.stream(stats).max().getAsLong();
//        for (int i = 0; i < numMmers; i++) {
//            if (stats[i] > 0 && stats[i] * 1.1 > max) {
//                stats[i] *= 1.1;
//            }
//        }
        return stats;
    }


}
