package buildgraph.Ordering;

import buildgraph.StringUtils;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;

public class IterativeUHSOrdering8 implements IOrdering {
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

    Integer[] temp = null;

    byte[] UHSElements;
    private  int sizeOfUHS;

    private  int mask;

    public IterativeUHSOrdering8(int pivotLength, String infile, int readLen, int bufSize, int k, long[] initialOrdering) {
        this.inputFile = infile;
        this.readLen = readLen;
        this.bufSize = bufSize;
        this.pivotLength = pivotLength;
        this.k = k;
        this.currentOrdering = initialOrdering.clone();
        stringUtils = new StringUtils();
    }

    public IterativeUHSOrdering8(int pivotLength, String infile, int readLen, int bufSize, int k) {
        this(pivotLength, infile, readLen, bufSize, k, new long[(int) Math.pow(4, pivotLength)]);
        roundSamples = 100000;
        rounds = 10000;
        elementsToPush = 1;
    }

    public IterativeUHSOrdering8(int pivotLength, String infile, int readLen, int bufSize, int k, int roundSamples, int rounds, int elementsToPush, int statisticsSamples, double maskRatio, double percentagePunishment) throws IOException {
        this(pivotLength, infile, readLen, bufSize, k);
        this.roundSamples = roundSamples;
        this.rounds = rounds;
        this.elementsToPush = elementsToPush;
        this.statisticsSamples = statisticsSamples;
        this.maskRatio = maskRatio;
        this.percentagePunishment = percentagePunishment;
        this.UHSElements = uhsBitSet();
        this.mask = (int)Math.pow(4, pivotLength) - 1;
    }


    public void initFrequency() throws IOException {
        int rank = 1;
        for (int i = 0; i < (int) Math.pow(4, pivotLength); i++) {
            if(UHSElements[i] == 1 && currentOrdering[i] == 0)
            {
                currentOrdering[i] = rank;
                currentOrdering[getReversed(i)] = rank;
                rank++;
            }
            else
            {
                currentOrdering[i] = Long.MAX_VALUE-i;
            }
        }
        sizeOfUHS = rank;

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
                pmerFrequency[minValue] += 1;

                int bound = len - k + 1;
                for (int i = 1; i < bound; i++) {
                    numSampled++;
                    currentValue = ((currentValue << 2) + StringUtils.valTable[lineCharArray[i + k - 1] - 'A']) & mask;//0xffff;

                    if (i > min_pos) {
                        min_pos = findSmallest(lineCharArray, i, i + k);
                        minValue = stringUtils.getDecimal(lineCharArray, min_pos, min_pos + pivotLength);
                        pmerFrequency[minValue] += 1;
                    } else {
                        int lastIndexInWindow = k + i - pivotLength;
                        if (strcmp(currentValue, minValue) < 0) {
                            min_pos = lastIndexInWindow;
                            minValue = currentValue;
                            pmerFrequency[minValue] += 1;
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
                    if(roundNumber % 100 == 0)
                        percentagePunishment *= 0.996;
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
        for(int i = 0 ; i<UHSElements.length; UHSElements[i]=1, i++); normalize();
    }


    private void adaptOrdering(long[] pmerFrequency) {
        for (int i = 0; i < elementsToPush; i++) {
            long biggest = -1;
            int biggestIndex = -1;
            for (int k = 0; k < pmerFrequency.length; k++) {
                if (UHSElements[k] == 1 && pmerFrequency[k] > biggest) {
                    biggest = pmerFrequency[k];
                    biggestIndex = k;
                }
            }
            long newRank = currentOrdering[biggestIndex] + (int) (sizeOfUHS * percentagePunishment);
            currentOrdering[biggestIndex] = newRank;
            currentOrdering[getReversed(biggestIndex)] = newRank;
            pmerFrequency[biggestIndex] = 0;
            pmerFrequency[getReversed(biggestIndex)] = 0;
        }

        //normalize();
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

    private void normalize() {
//        currentOrdering
        if(temp == null)
        {
            temp = new Integer[currentOrdering.length];
            for (int i = 0; i < temp.length; temp[i] = i, i++) ;
        }
        Arrays.sort(temp, Comparator.comparingLong(a -> currentOrdering[a]));
        for(int i = 0 ; i<temp.length; i++){
            if(UHSElements[i] == 1)
                currentOrdering[i] = temp[i];
            // shouldnt be a case where a non UHS element update is meaningful
        }
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

    private byte[] uhsBitSet() throws IOException {
        int n = (int) Math.pow(4, pivotLength);
        int i = 0;
        byte[] bits = new byte[n];

        String DocksFile = "res_" + pivotLength + ".txt";
        FileReader frG = new FileReader(DocksFile);
        int count = 0;

        BufferedReader reader;
        try {
            reader = new BufferedReader(frG);
            String line;
            while ((line = reader.readLine()) != null) {
                i = stringUtils.getDecimal(line.toCharArray(), 0, pivotLength);
                bits[i] = 1;
                bits[getReversed(i)] = 1;
                count++;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(count);
        frG.close();

        return bits;
    }
}
