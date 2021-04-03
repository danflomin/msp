package dumbo.Ordering.Standard;

import dumbo.Ordering.OrderingBase;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;

public class FrequencyOrdering extends OrderingBase {
    private String inputFile;
    private int readLen;
    private int bufSize;
    private int[] pmerFrequency;
    private int numSamples;

    public FrequencyOrdering(int pivotLen, String infile, int readLen, int bufSize, int numSamples) {
        super(pivotLen);
        this.inputFile = infile;
        this.readLen = readLen;
        this.bufSize = bufSize;
        this.pmerFrequency = new int[numMmers];
        this.numSamples = numSamples;
    }

    protected void initFrequency() throws IOException {
        FileReader frG = new FileReader(inputFile);
        BufferedReader bfrG = new BufferedReader(frG, bufSize);

        int counter = 0;

        String describeline;

        char[] lineCharArray = new char[readLen];


        while ((describeline = bfrG.readLine()) != null) {

            bfrG.read(lineCharArray, 0, readLen);
            bfrG.read();

            if (stringUtils.isReadLegal(lineCharArray)) {
                for (int i = 0; i < lineCharArray.length - pivotLength; i++) {

                    int value = stringUtils.getNormalizedValue(stringUtils.getDecimal(lineCharArray, i, i + pivotLength), pivotLength);
                    pmerFrequency[value] += 1;

                    counter++;
                }
                if (counter > numSamples) {
                    break;
                }
            }
        }
        bfrG.close();
        frG.close();
    }


    @Override
    public void initializeRanks() throws Exception {
        initFrequency();
        Integer[] mmers = new Integer[numMmers];
        for (int i = 0; i < mmers.length; i++) {
            mmers[i] = i;
        }

        Arrays.sort(mmers, this::rawCompareMmer);
        for (int i = 0; i < mmers.length; i++) {
            mmerRanks[mmers[i]] = i;
        }
        isRankInitialized = true;
    }

    @Override
    public int rawCompareMmer(int x, int y) {
        int a = stringUtils.getNormalizedValue(x, pivotLength);
        int b = stringUtils.getNormalizedValue(y, pivotLength);
        if (a == b) return 0;

        if (pmerFrequency[a] == pmerFrequency[b]) {
            if (a < b) return -1;
            return 1;
        }
        if (pmerFrequency[a] < pmerFrequency[b])
            return -1;
        else
            return 1;
    }


}
