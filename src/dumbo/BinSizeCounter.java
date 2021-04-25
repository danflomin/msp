package dumbo;

import dumbo.Ordering.OrderingBase;
import net.agkn.hll.HLL;
import net.agkn.hll.HLLType;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class BinSizeCounter {
    private String inputFile;
    private int readLen;
    private int bufSize;
    private int k;

    private int[] frequencies;
    private OrderingBase ordering;

    private StringUtils stringUtils;

    private int pivotLength;
    private long statisticsSamples;

    private int mask;
    private int numMmers;


    public BinSizeCounter(
            int pivotLength, String infile, int readLen, int bufSize, int k, long statisticsSamples, OrderingBase ordering) {
        this.pivotLength = pivotLength;
        this.statisticsSamples = statisticsSamples;
        this.inputFile = infile;
        this.readLen = readLen;
        this.bufSize = bufSize;
        this.k = k;
        numMmers = (int) Math.pow(4, pivotLength);
        frequencies = new int[numMmers];
        this.ordering = ordering;
        stringUtils = new StringUtils();
        mask = numMmers - 1;
    }


    private void concurrentCounter(char[] lineCharArray) throws Exception {
        int min_pos, minValue, minValueNormalized, currentValue;

        if (stringUtils.isReadLegal(lineCharArray)) {

            min_pos = ordering.findSmallest(lineCharArray, 0, k);
            minValue = stringUtils.getDecimal(lineCharArray, min_pos, min_pos + pivotLength);
            minValueNormalized = stringUtils.getNormalizedValue(minValue, pivotLength);
            currentValue = stringUtils.getDecimal(lineCharArray, k - pivotLength, k);


            frequencies[minValueNormalized] += k;

            int bound = readLen - k + 1;
            for (int i = 1; i < bound; i++) {

                currentValue = ((currentValue << 2) + StringUtils.valTable[lineCharArray[i + k - 1] - 'A']) & mask;

                if (i > min_pos) {
                    min_pos = ordering.findSmallest(lineCharArray, i, i + k);
                    minValue = stringUtils.getDecimal(lineCharArray, min_pos, min_pos + pivotLength);
                    minValueNormalized = stringUtils.getNormalizedValue(minValue, pivotLength);
                    frequencies[minValueNormalized] += k;
                } else if (ordering.compareMmer(currentValue, minValue) < 0) {
                    int lastIndexInWindow = k + i - pivotLength;
                    min_pos = lastIndexInWindow;
                    minValue = currentValue;
                    minValueNormalized = stringUtils.getNormalizedValue(minValue, pivotLength);
                    frequencies[minValueNormalized] += k;
                }
                else
                    frequencies[minValueNormalized]++;
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

            concurrentCounter(lineCharArray);
            numSampled += readLen - k;
            if (numSampled > statisticsSamples)
                keepSample = false;

        }

        executor.shutdown();
        bfrG.close();
        frG.close();
    }


    public long[] getStatistics() {
        long[] stats = new long[numMmers];
        for (int i = 0; i < numMmers; i++) {
            stats[i] = frequencies[i];
        }
        return stats;
    }


}
