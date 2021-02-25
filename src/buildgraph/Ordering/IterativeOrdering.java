package buildgraph.Ordering;

import buildgraph.Ordering.UHS.UHSOrderingBase;
import buildgraph.Ordering.UHS.UHSSignatureOrdering;
import buildgraph.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;

public class IterativeOrdering implements IOrdering {
    private String inputFile;
    private int readLen;
    private int bufSize;
    private int pivotLength;
    private int k;
    private long[] currentOrdering;
    private StringUtils stringUtils;
    private long[] frequency;

    private int roundSamples;
    private int rounds;
    private int elementsToPush;
    private int pushBy;

    public IterativeOrdering(int pivotLength, String infile, int readLen, int bufSize, int k, long[] initialOrdering, int roundSamples, int rounds, int elementsToPush, int pushBy) {
        this.inputFile = infile;
        this.readLen = readLen;
        this.bufSize = bufSize;
        this.pivotLength = pivotLength;
        this.k = k;
        this.currentOrdering = initialOrdering.clone();
        this.roundSamples = roundSamples;
        this.rounds = rounds;
        this.elementsToPush = elementsToPush;
        this.pushBy = pushBy;
        stringUtils = new StringUtils();
    }

    public IterativeOrdering(int pivotLength, String infile, int readLen, int bufSize, int k) {
        this(pivotLength, infile, readLen, bufSize, k, new long[(int) Math.pow(4, pivotLength)], 100000, 10000, 1, (int) Math.pow(4, pivotLength));
        for (int i = 0; i < (int) Math.pow(4, pivotLength); i++) {
            int canonical = Math.min(i, getReversed(i));
            currentOrdering[i] = canonical;
            currentOrdering[getReversed(i)] = canonical;
        }
    }

    public IterativeOrdering(int pivotLength, String infile, int readLen, int bufSize, int k, int roundSamples, int rounds, int elementsToPush, int pushBy) {
        this(pivotLength, infile, readLen, bufSize, k, new long[(int) Math.pow(4, pivotLength)], roundSamples, rounds, elementsToPush, pushBy);
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
        char[] currentArray;


        int prepos, min_pos = -1;
        int[] flag = new int[1];

        while (keepSample && (describeline = bfrG.readLine()) != null) {

            bfrG.read(lineCharArray, 0, readLen);
            bfrG.read();

            if (stringUtils.isReadLegal(lineCharArray)) {
                int len = readLen;
                char[] revCharArray = stringUtils.getReversedRead(lineCharArray);

                min_pos = findPosOfMin(lineCharArray, revCharArray, 0, k, flag);
                //int initialMinPos = min_pos;

                int bound = len - k + 1;
                for (int i = 1; i < bound; i++) {
                    numSampled++;

                    if (i > (flag[0] == 0 ? min_pos : len - min_pos - pivotLength)) {
                        currentArray = flag[0] == 0 ? lineCharArray : revCharArray;
                        int temp = calPosNew(currentArray, min_pos, min_pos + pivotLength);

                        min_pos = findPosOfMin(lineCharArray, revCharArray, i, i + k, flag);
                        //initialMinPos = min_pos;

                        if (temp != (flag[0] == 0 ? calPosNew(lineCharArray, min_pos, min_pos + pivotLength) : calPosNew(revCharArray, min_pos, min_pos + pivotLength))) {
                            prepos = temp;
                            pmerFrequency[prepos]++;
                        }

                    } else {

                        if (strcmp(lineCharArray, revCharArray, k + i - pivotLength, len - i - k, pivotLength) < 0) {
                            if (strcmp(lineCharArray, flag[0] == 0 ? lineCharArray : revCharArray, k + i - pivotLength, min_pos, pivotLength) < 0) {

                                currentArray = flag[0] == 0 ? lineCharArray : revCharArray;
                                int temp = calPosNew(currentArray, min_pos, min_pos + pivotLength);

                                min_pos = k + i - pivotLength;
                                if (temp != calPosNew(lineCharArray, min_pos, min_pos + pivotLength)) {
                                    prepos = temp;
                                    pmerFrequency[prepos]++;
                                }

                                flag[0] = 0;

                            }
                        } else {
                            if (strcmp(revCharArray, flag[0] == 0 ? lineCharArray : revCharArray, len - i - k, min_pos, pivotLength) < 0) {

                                currentArray = flag[0] == 0 ? lineCharArray : revCharArray;
                                int temp = calPosNew(currentArray, min_pos, min_pos + pivotLength);

                                min_pos = -k - i + len;

                                if (temp != calPosNew(revCharArray, min_pos, min_pos + pivotLength)) {
                                    prepos = temp;
                                    pmerFrequency[prepos]++;
                                }
                                flag[0] = 1;
                            }
                        }
                    }
                }
                currentArray = flag[0] == 0 ? lineCharArray : revCharArray;
                prepos = calPosNew(currentArray, min_pos, min_pos + pivotLength);
                pmerFrequency[prepos]++;
            }

            if (numSampled >= roundSamples) {
                roundNumber++;
                if (roundNumber <= rounds) {
                    numSampled = 0;
                    adaptOrdering(pmerFrequency);
                    pmerFrequency = new long[(int) Math.pow(4, pivotLength)]; // zero out elements
                    if (roundNumber == rounds)
                    {
                        System.out.println("Sampling for binning round");
                        roundSamples = Integer.MAX_VALUE;//100*rounds*roundSamples;
                    }
                } else {
                    keepSample = false;
                }
            }
            frequency = pmerFrequency;


        }
        bfrG.close();
        frG.close();
    }

    private void adaptOrdering(long[] pmerFrequency) {
        for (int i = 0; i < elementsToPush; i++) {
            long biggest = Arrays.stream(pmerFrequency).max().getAsLong();
            for (int j = 0; j < pmerFrequency.length; j++) {
                if (pmerFrequency[j] == biggest) {
                    long newRank = currentOrdering[j] + pushBy;
                    currentOrdering[j] = newRank;
                    currentOrdering[getReversed(j)] = newRank;
                    pmerFrequency[j] = 0;
                    pmerFrequency[getReversed(j)] = 0;
                }
            }
        }
    }

    private int calPosNew(char[] a, int from, int to) {
        return stringUtils.getDecimal(a, from, to);
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

    private int findPosOfMin(char[] a, char[] b, int from, int to, int[] flag) throws IOException {
        int len = a.length;
        int pos1 = findSmallest(a, from, to);
        int pos2 = findSmallest(b, len - to, len - from);

        if (strcmp(a, b, pos1, pos2, pivotLength) < 0) {
            flag[0] = 0;
            return pos1;
        } else {
            flag[0] = 1;
            return pos2;
        }
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

    public void exportOrderingForCpp() {
        System.out.print("{");
        for (int i = 0; i < currentOrdering.length; i++) {
            System.out.print(currentOrdering[i] + ",");
        }
        System.out.print("}");
        System.out.println();
    }

    public void exportBinningForCpp() {
        System.out.print("{");
        for (int i = 0; i < frequency.length; i++) {
            System.out.print(frequency[i] + ",");
        }
        System.out.print("}");
        System.out.println();
    }


}

