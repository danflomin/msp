package dumbo;

import dumbo.Ordering.IOrderingPP;

import java.io.*;

public class MinimizerCounter {

    private int k;
    private String kmerSetFile;
    private int pivotLen;
    private int bufSize;

    private FileReader frG;
    private BufferedReader bfrG;

    private IOrderingPP ordering;

    private StringUtils stringUtils;

    private long[] minimizerCounters;


    public MinimizerCounter(int kk, String kmerSetFile, int pivotLength, int bufferSize, IOrderingPP ordering) {
        this.k = kk;
        this.kmerSetFile = kmerSetFile;
        this.pivotLen = pivotLength;
        this.bufSize = bufferSize;
        this.ordering = ordering;
        this.stringUtils = new StringUtils();
        minimizerCounters = new long[(int) Math.pow(4, pivotLength)];
    }


    private long[] getMinimizersCounters() throws IOException {
        frG = new FileReader(kmerSetFile);
        bfrG = new BufferedReader(frG, bufSize);

        String describeline;

        int minPos;
        char[] lineCharArray = new char[k];


        int minValue, minValueNormalized, currentValue, start;
        while ((describeline = bfrG.readLine()) != null) {

            bfrG.read(lineCharArray, 0, k);
            bfrG.read();

            if (stringUtils.isReadLegal(lineCharArray)) {
                minPos = ordering.findSmallest(lineCharArray, 0, k);
                minValue = stringUtils.getDecimal(lineCharArray, minPos, minPos + pivotLen);
                minValueNormalized = stringUtils.getNormalizedValue(minValue, pivotLen);
                minimizerCounters[minValueNormalized]++;
            }
        }

        bfrG.close();
        frG.close();

        return minimizerCounters.clone();
    }

    public long[] Run() throws Exception {
        long time1 = 0;
        long t1 = System.currentTimeMillis();
        System.out.println("Minimizers counting Begin!");
        System.out.println("hi");
        long[] counters = getMinimizersCounters();

        long t2 = System.currentTimeMillis();
        time1 = (t2 - t1) / 1000;
        System.out.println("Time used for counting minimizers appearances: " + time1 + " seconds!");
        return counters;
    }

}