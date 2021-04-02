package dumbo.Ordering;

import dumbo.Ordering.Standard.SignatureUtils;
import dumbo.StringUtils;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

public class IterativeOrdering extends OrderingBase {
    private String inputFile;
    private int readLen;
    private int bufSize;
    private int k;
    private int[] currentOrdering;
    private SignatureUtils signatureUtils;
    private HashMap<Integer, HashSet<String>> frequency;

    private int statisticsSamples;
    private int roundSamples;
    private int rounds;
    private int elementsToPush;

    private double percentagePunishment;

    private long[] statFrequency;

    private boolean useSignature;

    private boolean initialized;


    public IterativeOrdering(
            int pivotLength, String infile, int readLen, int bufSize, int k, int roundSamples, int rounds,
            int elementsToPush, int statisticsSamples, double percentagePunishment, boolean useSignature) {
        super(pivotLength);
        this.roundSamples = roundSamples;
        this.rounds = rounds;
        this.elementsToPush = elementsToPush;
        this.statisticsSamples = statisticsSamples;
        this.percentagePunishment = percentagePunishment;
        this.useSignature = useSignature;
        this.inputFile = infile;
        this.readLen = readLen;
        this.bufSize = bufSize;
        this.k = k;
        signatureUtils = new SignatureUtils(pivotLength);
        currentOrdering = new int[(int) Math.pow(4, pivotLength)];
        initialized = false;
    }

    public IterativeOrdering(
            int pivotLength, String infile, int readLen, int bufSize, int k, int roundSamples, int rounds,
            int elementsToPush, int statisticsSamples, double percentagePunishment, boolean useSignature, int[] initialOrdering) {
        this(pivotLength, infile, readLen, bufSize, k, roundSamples, rounds, elementsToPush, statisticsSamples, percentagePunishment, useSignature);
        currentOrdering = initialOrdering.clone();
        initialized = true;
        badArgumentsThrow();
    }

    public IterativeOrdering(
            int pivotLength, String infile, int readLen, int bufSize, int k, int roundSamples, int rounds,
            int elementsToPush, int statisticsSamples, double percentagePunishment, boolean useSignature, OrderingBase initialOrdering) throws IOException {
        this(pivotLength, infile, readLen, bufSize, k, roundSamples, rounds, elementsToPush, statisticsSamples, percentagePunishment, useSignature);
        currentOrdering = initialOrdering.getRanks().clone();
        initialized = true;
        badArgumentsThrow();
    }

    private void badArgumentsThrow() {
        if (currentOrdering.length != numMmers)
            throw new IllegalArgumentException("initialOrdering is not of correct size");
        if (useSignature)
            throw new IllegalArgumentException("Can't initialize ordering from outside with useSignature as true");
    }


    public void initFrequency() throws IOException {

        if (!initialized) {
            for (int i = 0; i < numMmers; i++) {
                int canonical = Math.min(i, stringUtils.getReversedMmer(i, pivotLength));
                currentOrdering[i] = canonical;
                currentOrdering[stringUtils.getReversedMmer(i, pivotLength)] = canonical;
            }
            if (useSignature) {
                for (int i = 0; i < numMmers; i++) {
                    if (!signatureUtils.isAllowed(i) && i < stringUtils.getReversedMmer(i, pivotLength)) {
                        currentOrdering[i] += numMmers;
                        currentOrdering[stringUtils.getReversedMmer(i, pivotLength)] += numMmers;
                    }
                }
            }
        }


        boolean keepSample = true;
        int numSampled = 0;
        int roundNumber = 0;

        FileReader frG = new FileReader(inputFile);
        BufferedReader bfrG = new BufferedReader(frG, bufSize);

        statFrequency = new long[numMmers];
        HashMap<Integer, HashSet<String>> pmerFrequency = new HashMap<>(numMmers);

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
                minValueNormalized = stringUtils.getNormalizedValue(minValue, pivotLength);
                currentValue = stringUtils.getDecimal(lineCharArray, k - pivotLength, k);

                updateStatistics(roundNumber, pmerFrequency, minValueNormalized, line, 0);

                int bound = len - k + 1;
                for (int i = 1; i < bound; i++) {
                    numSampled++;
                    currentValue = ((currentValue << 2) + StringUtils.valTable[lineCharArray[i + k - 1] - 'A']) & mask;

                    if (i > min_pos) {
                        min_pos = findSmallest(lineCharArray, i, i + k);
                        minValue = stringUtils.getDecimal(lineCharArray, min_pos, min_pos + pivotLength);
                        minValueNormalized = stringUtils.getNormalizedValue(minValue, pivotLength);

                        updateStatistics(roundNumber, pmerFrequency, minValueNormalized, line, i);
                    } else {
                        int lastIndexInWindow = k + i - pivotLength;
                        if (compareMmer(currentValue, minValue) < 0) {
                            min_pos = lastIndexInWindow;
                            minValue = currentValue;
                            minValueNormalized = stringUtils.getNormalizedValue(minValue, pivotLength);

                            updateStatistics(roundNumber, pmerFrequency, minValueNormalized, line, i);
                        }
                    }
                    updateStatistics(roundNumber, pmerFrequency, minValueNormalized, line, i);
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

    private void updateStatistics(int roundNumber, HashMap<Integer, HashSet<String>> pmerFrequency, int minValueNormalized, String line, int startPosition) {
        if (roundNumber == rounds)
            statFrequency[minValueNormalized]++;
        else {
            if (!pmerFrequency.containsKey(minValueNormalized))
                pmerFrequency.put(minValueNormalized, new HashSet<>());
            pmerFrequency.get(minValueNormalized).add(stringUtils.getCanonical(line.substring(startPosition, k + startPosition)));
        }
    }


    private void adaptOrdering(HashMap<Integer, HashSet<String>> pmerFrequency) {
        int[] frequencies = new int[numMmers];
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
//             TODO: might not be necessary to change both.
            int newRank = currentOrdering[biggestIndex] + (int) (numMmers * percentagePunishment);
            currentOrdering[biggestIndex] = newRank;
            currentOrdering[stringUtils.getReversedMmer(biggestIndex, pivotLength)] = newRank;
            frequencies[biggestIndex] = 0;
            frequencies[stringUtils.getReversedMmer(biggestIndex, pivotLength)] = 0;
        }
    }


    @Override
    public int compareMmer(int x, int y) {
        int a = stringUtils.getNormalizedValue(x, pivotLength);
        int b = stringUtils.getNormalizedValue(y, pivotLength);
        if (a == b) return 0;
        if (currentOrdering[a] < currentOrdering[b]) return -1;
        return 1;
    }

    @Override
    public int[] getRanks() {
        return currentOrdering.clone();
    }

    private void normalize() {
        Integer[] temp = new Integer[currentOrdering.length];
        for (int i = 0; i < temp.length; i++)
            temp[i] = i;

        Arrays.sort(temp, Comparator.comparingLong(a -> currentOrdering[a]));
        for (int i = 0; i < temp.length; i++) {
            currentOrdering[temp[i]] = i;
        }
    }


}
