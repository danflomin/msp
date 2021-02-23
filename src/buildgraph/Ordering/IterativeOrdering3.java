package buildgraph.Ordering;

import buildgraph.StringUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

public class IterativeOrdering3 implements IOrdering {
    private String inputFile;
    private int readLen;
    private int bufSize;
    private int pivotLength;
    private int k;
    private long[] currentOrdering;
    private StringUtils stringUtils;

    private int roundSamples = 100000;
    private int rounds = 10000;
    private int elementsToPush = 1;

    public IterativeOrdering3(int pivotLength, String infile, int readLen, int bufSize, int k, long[] initialOrdering) {
        this.inputFile = infile;
        this.readLen = readLen;
        this.bufSize = bufSize;
        this.pivotLength = pivotLength;
        this.k = k;
        this.currentOrdering = initialOrdering.clone();
        stringUtils = new StringUtils();
    }

    public IterativeOrdering3(int pivotLength, String infile, int readLen, int bufSize, int k) {
        this(pivotLength, infile, readLen, bufSize, k, new long[(int) Math.pow(4, pivotLength)]);
        for (int i = 0; i < (int) Math.pow(4, pivotLength); i++) {
            int canonical = Math.min(i, getReversed(i));
            currentOrdering[i] = canonical;
            currentOrdering[getReversed(i)] = canonical;
        }
    }


    public void initFrequency() throws IOException {
        boolean keepSample = true;
        int numSampled = 0;
        int roundNumber = 0;

        FileReader frG = new FileReader(inputFile);
        BufferedReader bfrG = new BufferedReader(frG, bufSize);

        long[] pmerFrequency = new long[(int) Math.pow(4, pivotLength)];

        String describeline;
        char[] lineCharArray = new char[readLen];

        int len = readLen;



        int min_pos = -1;
        int minValue;

        while (keepSample && (describeline = bfrG.readLine()) != null) {

            bfrG.read(lineCharArray, 0, readLen);
            bfrG.read();

            if (stringUtils.isReadLegal(lineCharArray)) {

                min_pos = findSmallest(lineCharArray, 0, k);
                minValue = stringUtils.getDecimal(lineCharArray, min_pos, min_pos+pivotLength);
                pmerFrequency[minValue]++;

                int bound = len - k + 1;
                for (int i = 1; i < bound; i++) {
                    numSampled++;

                    if (i > min_pos) {
                        min_pos = findSmallest(lineCharArray, i, i + k);
                        minValue = stringUtils.getDecimal(lineCharArray, min_pos, min_pos+pivotLength);
                    } else {
                        int lastIndexInWindow = k + i - pivotLength;
                        if (strcmp(lineCharArray, lineCharArray, lastIndexInWindow, min_pos, pivotLength) < 0) {
                            min_pos = lastIndexInWindow;
                            minValue = stringUtils.getDecimal(lineCharArray, min_pos, min_pos+pivotLength);
                        }
                    }
                    pmerFrequency[minValue]++;
                }
            }
            pmerFrequency[min_pos]++;

            if (numSampled >= roundSamples) {
                roundNumber++;
                if (roundNumber == rounds)
                    keepSample = false;
                else
                    numSampled = 0;
                adaptOrdering(pmerFrequency);
                pmerFrequency = new long[(int) Math.pow(4, pivotLength)]; // zero out elements
            }
        }
        bfrG.close();
        frG.close();
    }


    private void adaptOrdering(long[] pmerFrequency) {
        for (int i = 0; i < elementsToPush; i++) {
            long biggest = Arrays.stream(pmerFrequency).max().getAsLong();
            for (int j = 0; j < pmerFrequency.length; j++) {
                if (pmerFrequency[j] == biggest) {
                    long newRank = currentOrdering[j] + (int) Math.pow(4, pivotLength)/100;
                    currentOrdering[j] = newRank;
                    currentOrdering[getReversed(j)] = newRank;
                    pmerFrequency[j] = 0;
                    pmerFrequency[getReversed(j)] = 0;
                }
            }
        }
    }

    private int getReversed(int x) {
        int rev = 0;
        int immer = ~x;
        for (int i = 0; i < pivotLength; ++i) {
            rev <<= 2;
            rev |= immer & 0x3;
            immer >>= 2;
        }
        return rev;
    }


    @Override
    public int findSmallest(char[] a, int from, int to) throws IOException {
        int min_pos = from;
        for (int i = from + 1; i <= to - pivotLength; i++) {
            if (strcmp(a, a, min_pos, i, pivotLength) > 0)
                min_pos = i;
        }

        return min_pos;
    }

    @Override
    public int strcmp(char[] a, char[] b, int froma, int fromb, int len) {
        int x = stringUtils.getDecimal(a, froma, froma + pivotLength);
        int y = stringUtils.getDecimal(b, fromb, fromb + pivotLength);

        if (x == y) return 0;
        if (currentOrdering[x] < currentOrdering[y]) return -1;
        return 1;
    }
}