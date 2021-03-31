package dumbo.Ordering;

import dumbo.StringUtils;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

public class IterativeOrdering9_WithCounterNormalized_AndSignature implements IOrderingPP {
    private String inputFile;
    private int readLen;
    private int bufSize;
    private int pivotLength;
    private int k;
    private long[] currentOrdering;
    private StringUtils stringUtils;
    private SignatureUtils signatureUtils;
    private HashMap<Integer, HashSet<String>> frequency;

    private int statisticsSamples;
    private int roundSamples;
    private int rounds;
    private int elementsToPush;

    private double percentagePunishment;

    private Integer[] temp = null;
    private int mask;
    private long[] statFrequency;

    public IterativeOrdering9_WithCounterNormalized_AndSignature(int pivotLength, String infile, int readLen, int bufSize, int k, long[] initialOrdering) {
        this.inputFile = infile;
        this.readLen = readLen;
        this.bufSize = bufSize;
        this.pivotLength = pivotLength;
        this.k = k;
        this.currentOrdering = initialOrdering.clone();
        stringUtils = new StringUtils();
        signatureUtils = new SignatureUtils(pivotLength);
    }

    public IterativeOrdering9_WithCounterNormalized_AndSignature(int pivotLength, String infile, int readLen, int bufSize, int k) {
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

    public IterativeOrdering9_WithCounterNormalized_AndSignature(int pivotLength, String infile, int readLen, int bufSize, int k, int roundSamples, int rounds, int elementsToPush, int statisticsSamples, double percentagePunishment) {
        this(pivotLength, infile, readLen, bufSize, k);
        this.roundSamples = roundSamples;
        this.rounds = rounds;
        this.elementsToPush = elementsToPush;
        this.statisticsSamples = statisticsSamples;
        this.percentagePunishment = percentagePunishment;
        this.mask = (int) Math.pow(4, pivotLength) - 1;
    }

    public String getCanon(String line) {
        String x = new String(stringUtils.getReversedRead(line.toCharArray()));
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) < x.charAt(i))
                return line;
            else if (line.charAt(i) > x.charAt(i))
                return x;
        }
        return x;
    }


    public void initFrequency() throws IOException {
        int numMmers = (int)Math.pow(4, pivotLength);
        for (int i = 0; i < numMmers; i++) {
            if(!signatureUtils.isAllowed(i) && i < getReversed(i))
            {
                currentOrdering[i] += numMmers;
                currentOrdering[getReversed(i)] += numMmers;
            }
        }

        boolean keepSample = true;
        int numSampled = 0;
        int roundNumber = 0;

        FileReader frG = new FileReader(inputFile);
        BufferedReader bfrG = new BufferedReader(frG, bufSize);

        statFrequency = new long[(int) Math.pow(4, pivotLength)];
        HashMap<Integer, HashSet<String>> pmerFrequency = new HashMap<>((int) Math.pow(4, pivotLength));

        String describeline;
        char[] lineCharArray = new char[readLen];

        int len = readLen;


        int min_pos = -1;
        int minValue, currentValue, minValueNormalized;

        while (keepSample && (describeline = bfrG.readLine()) != null) {

            bfrG.read(lineCharArray, 0, readLen);
            bfrG.read();
            String line = new String(lineCharArray);

            if (stringUtils.isReadLegal(lineCharArray)) {

                min_pos = findSmallest(lineCharArray, 0, k);
                minValue = stringUtils.getDecimal(lineCharArray, min_pos, min_pos + pivotLength);
                minValueNormalized = Math.min(minValue, getReversed(minValue));
                currentValue = stringUtils.getDecimal(lineCharArray, k - pivotLength, k);
                ;
                if (!pmerFrequency.containsKey(minValueNormalized))
                    pmerFrequency.put(minValueNormalized, new HashSet<>());
                pmerFrequency.get(minValueNormalized).add(getCanon(line.substring(0, k))); // += 1;

                if (roundNumber == rounds) statFrequency[minValueNormalized]++;

                int bound = len - k + 1;
                for (int i = 1; i < bound; i++) {
                    numSampled++;
                    currentValue = ((currentValue << 2) + StringUtils.valTable[lineCharArray[i + k - 1] - 'A']) & mask;//0xffff;

                    if (i > min_pos) {
                        min_pos = findSmallest(lineCharArray, i, i + k);
                        minValue = stringUtils.getDecimal(lineCharArray, min_pos, min_pos + pivotLength);
                        minValueNormalized = Math.min(minValue, getReversed(minValue));

                        if (!pmerFrequency.containsKey(minValueNormalized))
                            pmerFrequency.put(minValueNormalized, new HashSet<>());
                        pmerFrequency.get(minValueNormalized).add(getCanon(line.substring(i, k + i))); // += 1;
                        if (roundNumber == rounds) statFrequency[minValueNormalized]++;
                    } else {
                        int lastIndexInWindow = k + i - pivotLength;
                        if (strcmp(currentValue, minValue) < 0) {
                            min_pos = lastIndexInWindow;
                            minValue = currentValue;
                            minValueNormalized = Math.min(minValue, getReversed(minValue));

                            if (!pmerFrequency.containsKey(minValueNormalized))
                                pmerFrequency.put(minValueNormalized, new HashSet<>());
                            pmerFrequency.get(minValueNormalized).add(getCanon(line.substring(i, k + i))); // += 1;
                            if (roundNumber == rounds) statFrequency[minValueNormalized]++;
                        }
                    }

                    pmerFrequency.get(minValueNormalized).add(getCanon(line.substring(i, k + i))); // += 1;
                    if (roundNumber == rounds) statFrequency[minValueNormalized]++;
                }
            }

            if (numSampled >= roundSamples) {
                roundNumber++;
                if (roundNumber <= rounds) {  // TODO: SHOULD THIS BE < and not <=
                    numSampled = 0;
                    adaptOrdering(pmerFrequency);
                    pmerFrequency.clear();
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
        normalize();
        bfrG.close();
        frG.close();
    }


    private void adaptOrdering(HashMap<Integer, HashSet<String>> pmerFrequency) {
        int[] frequencies = new int[(int) Math.pow(4, pivotLength)];
        for (Integer i : pmerFrequency.keySet()) {
            frequencies[i] = pmerFrequency.get(i).size();
        }
        for (int i = 0; i < elementsToPush; i++) {
            long biggest = -1;
            int biggestIndex = -1;
            for (int k = 0; k < frequencies.length; k++) {
                if (frequencies[k] > biggest) {
                    biggest = frequencies[k];
                    biggestIndex = k;
                }
            }
            long newRank = currentOrdering[biggestIndex] + (int) ((int) Math.pow(4, pivotLength) * percentagePunishment);
            currentOrdering[biggestIndex] = newRank;
            currentOrdering[getReversed(biggestIndex)] = newRank;
            frequencies[biggestIndex] = 0;
            frequencies[getReversed(biggestIndex)] = 0;
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

        return strcmp(x,y);
    }

    @Override
    public int strcmp(int x, int y) {
//        if (x == y || y == getReversed(x)) return 0;
        if (x == y) return 0;
        if (currentOrdering[x] < currentOrdering[y]) return -1;
        return 1;
    }

    private void normalize() {
//        currentOrdering
        if (temp == null) {
            temp = new Integer[currentOrdering.length];
            for (int i = 0; i < temp.length; temp[i] = i, i++) ;
        }
        Arrays.sort(temp, Comparator.comparingLong(a ->  currentOrdering[a]));
        for (int i = 0; i < temp.length; i++) {
            currentOrdering[temp[i]] = i; // TODO: FIXED THIS
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

            for (int i = 0; i < statFrequency.length; i++) {
                bf.write(Long.toString(statFrequency[i]));
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

    @Override
    public long getRank(int mmer) {
        return currentOrdering[mmer];
    }
}
