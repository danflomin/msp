package dumbo.Ordering;

import dumbo.Ordering.Standard.SignatureUtils;
import dumbo.StringUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

public class IterativeOrderingV2 extends OrderingBase {
    private String inputFile;
    private int readLen;
    private int bufSize;
    private int k;
    private SignatureUtils signatureUtils;

    private int roundSamples;
    private int rounds;
    private int elementsToPush;

    private boolean useSignature;

    private boolean initialized;

    private boolean samplingMinimizerFrequency;
    private int[] statFrequency;
    private int frequencySampledMinimizer;


    public IterativeOrderingV2(
            int pivotLength, String infile, int readLen, int bufSize, int k, int roundSamples, int rounds,
            int elementsToPush, boolean useSignature) {
        super(pivotLength);
        this.roundSamples = roundSamples;
        this.rounds = rounds;
        this.elementsToPush = elementsToPush;
        this.useSignature = useSignature;
        this.inputFile = infile;
        this.readLen = readLen;
        this.bufSize = bufSize;
        this.k = k;
        signatureUtils = new SignatureUtils(pivotLength);
        initialized = false;
        samplingMinimizerFrequency = false;
    }

    public IterativeOrderingV2(
            int pivotLength, String infile, int readLen, int bufSize, int k, int roundSamples, int rounds,
            int elementsToPush, boolean useSignature, int[] initialOrdering) {
        this(pivotLength, infile, readLen, bufSize, k, roundSamples, rounds, elementsToPush, useSignature);
        mmerRanks = initialOrdering.clone();
        initialized = true;
        badArgumentsThrow();
    }

    public IterativeOrderingV2(
            int pivotLength, String infile, int readLen, int bufSize, int k, int roundSamples, int rounds,
            int elementsToPush, boolean useSignature, OrderingBase initialOrdering) throws IOException {
        this(pivotLength, infile, readLen, bufSize, k, roundSamples, rounds, elementsToPush, useSignature);
        mmerRanks = initialOrdering.getRanks().clone();
        initialized = true;
        badArgumentsThrow();
    }

    private void badArgumentsThrow() {
        if (mmerRanks.length != numMmers)
            throw new IllegalArgumentException("initialOrdering is not of correct size");
        if (useSignature)
            throw new IllegalArgumentException("Can't initialize ordering from outside with useSignature as true");
    }


    protected void initFrequency() throws Exception {

        if (!initialized) {
            for (int i = 0; i < numMmers; i++) {
                int canonical = Math.min(i, stringUtils.getReversedMmer(i, pivotLength));
                mmerRanks[i] = canonical;
                mmerRanks[stringUtils.getReversedMmer(i, pivotLength)] = canonical;
            }
            if (useSignature) {
                for (int i = 0; i < numMmers; i++) {
                    if (!signatureUtils.isAllowed(i) && i < stringUtils.getReversedMmer(i, pivotLength)) {
                        mmerRanks[i] += numMmers;
                        mmerRanks[stringUtils.getReversedMmer(i, pivotLength)] += numMmers;
                    }
                }
            }
        }


        boolean keepSample = true;
        int numSampled = 0;
        int roundNumber = 0;

        FileReader frG = new FileReader(inputFile);
        BufferedReader bfrG = new BufferedReader(frG, bufSize);

        statFrequency = new int[numMmers];
        HashMap<Integer, HashSet<String>> pmerFrequency = new HashMap<>(roundSamples * 2);

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

                boolean sampledWantedMinimizer = false;

                min_pos = findSmallest(lineCharArray, 0, k);
                minValue = stringUtils.getDecimal(lineCharArray, min_pos, min_pos + pivotLength);
                minValueNormalized = stringUtils.getNormalizedValue(minValue, pivotLength);
                currentValue = stringUtils.getDecimal(lineCharArray, k - pivotLength, k);

                sampledWantedMinimizer = updateStatistics(pmerFrequency, minValueNormalized, line, 0);

                if(sampledWantedMinimizer)
                    continue;

                int bound = len - k + 1;
                for (int i = 1; i < bound; i++) {
                    if(!samplingMinimizerFrequency) numSampled++;
                    currentValue = ((currentValue << 2) + StringUtils.valTable[lineCharArray[i + k - 1] - 'A']) & mask;

                    if (i > min_pos) {
                        min_pos = findSmallest(lineCharArray, i, i + k);
                        minValue = stringUtils.getDecimal(lineCharArray, min_pos, min_pos + pivotLength);
                        minValueNormalized = stringUtils.getNormalizedValue(minValue, pivotLength);

                    } else {
                        int lastIndexInWindow = k + i - pivotLength;
                        if (compareMmer(currentValue, minValue) < 0) {
                            min_pos = lastIndexInWindow;
                            minValue = currentValue;
                            minValueNormalized = stringUtils.getNormalizedValue(minValue, pivotLength);
                        }
                    }
                    sampledWantedMinimizer = updateStatistics(pmerFrequency, minValueNormalized, line, i);
                    if(sampledWantedMinimizer)
                    {
                        numSampled++;
                        break;
                    }
                }
            }

            if (numSampled >= roundSamples) {
                numSampled = 0;
                if (samplingMinimizerFrequency) { // 2 iterations is 1 round
                    adaptOrdering(pmerFrequency);
                    pmerFrequency.clear();
                    samplingMinimizerFrequency = false;
                    roundNumber++;
                    roundSamples *= 100;
                    if (roundNumber == rounds)   // TODO: SHOULD THIS BE < and not <=
                        keepSample = false;
                } else {
                    // find biggest and put it as freq minimizer
                    HashMap<Integer, Integer> x = new HashMap<>();
                    for (Integer i : pmerFrequency.keySet()) {
                        x.put(i, pmerFrequency.get(i).size());
                    }
                    int biggest = -1, idx = -1;
                    for (Integer i : x.keySet()) {
                        if (x.get(i) > biggest) {
                            biggest = x.get(i);
                            idx = i;
                        }
                    }
                    roundSamples /= 100;
                    frequencySampledMinimizer = idx;
                    samplingMinimizerFrequency = true;
                }
            }

        }
        normalize();
        bfrG.close();
        frG.close();
    }

    private boolean updateStatistics(HashMap<Integer, HashSet<String>> pmerFrequency, int minValueNormalized, String line, int startPosition) {
        String canonical = stringUtils.getCanonical(line.substring(startPosition, k + startPosition));
        if (samplingMinimizerFrequency) {
            if (minValueNormalized == frequencySampledMinimizer) {
                for (int i = 0; i <= canonical.length() - pivotLength; i++) {
                    int value = stringUtils.getNormalizedValue(stringUtils.getDecimal(canonical.toCharArray(), i, i + pivotLength), pivotLength);
                    statFrequency[value] += 1;
                }
                return true;
            }
        } else {
            if (!pmerFrequency.containsKey(minValueNormalized))
                pmerFrequency.put(minValueNormalized, new HashSet<>());
            pmerFrequency.get(minValueNormalized).add(canonical);
        }
        return false;
    }




    private void adaptOrdering(HashMap<Integer, HashSet<String>> pmerFrequency) {
        for (int i = 0; i < elementsToPush; i++) {
            int biggest = 0;
            int biggestIndex = -1;
            for (int k = 0; k < statFrequency.length; k++) {
                if (k != frequencySampledMinimizer && statFrequency[k] > biggest && mmerRanks[k] < mmerRanks[frequencySampledMinimizer] + 1000) { // TODO: add k is normalized
                    if ((!pmerFrequency.containsKey(k)) || pmerFrequency.get(k).size() < 0.1 * pmerFrequency.get(frequencySampledMinimizer).size()) {
                        biggest = statFrequency[k];
                        biggestIndex = k;
                    }
                }
            }
//             TODO: might not be necessary to change both.
            int newRank = mmerRanks[frequencySampledMinimizer] - 1;
            mmerRanks[biggestIndex] = newRank;
            mmerRanks[stringUtils.getReversedMmer(biggestIndex, pivotLength)] = newRank;
            statFrequency[biggestIndex] = 0;
            statFrequency[stringUtils.getReversedMmer(biggestIndex, pivotLength)] = 0;
        }
    }


    @Override
    public void initializeRanks() throws Exception {
        isRankInitialized = true;
        initFrequency();
    }

}
