package buildgraph.Ordering;

import buildgraph.StringUtils;

import java.io.*;
import java.util.Arrays;

public class IterativeOrdering4 implements IOrdering {
    private String inputFile;
    private int readLen;
    private int bufSize;
    private int pivotLength;
    private int k;
    private long[] currentOrdering;
    private StringUtils stringUtils;
    private long[] frequency;

    private int statisticsSamples;
    private int roundSamples;
    private int rounds;
    private int elementsToPush;

    private double maskRatio;
    private double percentagePunishment;

    public IterativeOrdering4(int pivotLength, String infile, int readLen, int bufSize, int k, long[] initialOrdering) {
        this.inputFile = infile;
        this.readLen = readLen;
        this.bufSize = bufSize;
        this.pivotLength = pivotLength;
        this.k = k;
        this.currentOrdering = initialOrdering.clone();
        stringUtils = new StringUtils();
    }

    public IterativeOrdering4(int pivotLength, String infile, int readLen, int bufSize, int k) {
        this(pivotLength, infile, readLen, bufSize, k, new long[(int) Math.pow(4, pivotLength)]);
        for (int i = 0; i < (int) Math.pow(4, pivotLength); i++) {
            int canonical = Math.min(i, getReversed(i));
            currentOrdering[i] = canonical;
            currentOrdering[getReversed(i)] = canonical;
        }
        roundSamples = 100000;
        rounds = 10000;
        elementsToPush = 1;
    }

    public IterativeOrdering4(int pivotLength, String infile, int readLen, int bufSize, int k, int roundSamples, int rounds, int elementsToPush, int statisticsSamples, double maskRatio, double percentagePunishment) {
        this(pivotLength, infile, readLen, bufSize, k);
        this.roundSamples = roundSamples;
        this.rounds = rounds;
        this.elementsToPush = elementsToPush;
        this.statisticsSamples = statisticsSamples;
        this.maskRatio = maskRatio;
        this.percentagePunishment = percentagePunishment;
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
        int minValue, currentValue;

        while (keepSample && (describeline = bfrG.readLine()) != null) {

            bfrG.read(lineCharArray, 0, readLen);
            bfrG.read();

            if (stringUtils.isReadLegal(lineCharArray)) {

                min_pos = findSmallest(lineCharArray, 0, k);
                minValue = stringUtils.getDecimal(lineCharArray, min_pos, min_pos + pivotLength);
                currentValue = stringUtils.getDecimal(lineCharArray, k - pivotLength, k);
                ;
                pmerFrequency[minValue] += k;

                int bound = len - k + 1;
                for (int i = 1; i < bound; i++) {
                    numSampled++;
                    currentValue = ((currentValue << 2) + StringUtils.valTable[lineCharArray[i + k - 1] - 'A']) & 0x3fff;//0xffff;

                    if (i > min_pos) {
                        min_pos = findSmallest(lineCharArray, i, i + k);
                        minValue = stringUtils.getDecimal(lineCharArray, min_pos, min_pos + pivotLength);
                        pmerFrequency[minValue] += k;
                    } else {
                        int lastIndexInWindow = k + i - pivotLength;
                        if (strcmp(currentValue, minValue) < 0) {
                            min_pos = lastIndexInWindow;
                            minValue = currentValue;
                            pmerFrequency[minValue] += k;
                        }
                    }

                    pmerFrequency[minValue]++;
                }
            }

            if (numSampled >= roundSamples) {
                roundNumber++;
                if (roundNumber <= rounds) {
                    numSampled = 0;
                    adaptOrdering(pmerFrequency);
                    pmerFrequency = new long[(int) Math.pow(4, pivotLength)]; // zero out elements
                    if (roundNumber == rounds) {
                        System.out.println("Sampling for binning round");
                        roundSamples = statisticsSamples;
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
        boolean[] mask = new boolean[pmerFrequency.length];
        for(int i = 0 ; i<mask.length; i++) {
            if(Math.random() < 1-maskRatio)
                mask[i] = true;
        }
// TODO : if biggest is smaller than (samples / 4^(m-1))/5
        for (int i = 0; i < elementsToPush; i++) {
            long biggest = -1;
            int biggestIndex = -1;
            for(int k = 0; k < pmerFrequency.length; k++) {
                if(mask[k] && pmerFrequency[k] > biggest) {
                    biggest = pmerFrequency[k];
                    biggestIndex = k;
                }
            }
            long newRank = currentOrdering[biggestIndex] + (int)((int) Math.pow(4, pivotLength) * percentagePunishment);
            currentOrdering[biggestIndex] = newRank;
            currentOrdering[getReversed(biggestIndex)] = newRank;
            pmerFrequency[biggestIndex] = 0;
            pmerFrequency[getReversed(biggestIndex)] = 0;
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

    public int strcmp(int x, int y) {
        if (x == y) return 0;
        if (currentOrdering[x] < currentOrdering[y]) return -1;
        return 1;
    }

    public void exportOrderingForCpp() {
        File file = new File("ranks.txt");

        BufferedWriter bf = null;

        try {
            bf = new BufferedWriter(new FileWriter(file));

            for (int i = 0; i < currentOrdering.length; i++) {
                bf.write(Long.toString(currentOrdering[i]));
                bf.newLine();
            }
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

    public void exportBinningForCpp() {
        File file = new File("freq.txt");

        BufferedWriter bf = null;

        try {
            bf = new BufferedWriter(new FileWriter(file));

            for (int i = 0; i < frequency.length; i++) {
                bf.write(Long.toString(frequency[i]));
                bf.newLine();
            }
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
