package dumbo.Ordering.UHS;

import dumbo.StringUtils;

import java.io.*;

public class UHSFrequencySignatureOrdering extends UHSSignatureOrdering {
    private String inputFile;
    private int readLen;
    private int bufSize;
    private long[] pmerFrequency;
    private int k;
    private int numStats;
    private boolean isInit;

    private long[] statsFrequency;

    public UHSFrequencySignatureOrdering(int pivotLen, String infile, int readLen, int bufSize, boolean useSignature, int k, int numStats) throws IOException {
        super(0, pivotLen, useSignature);
        this.inputFile = infile;
        this.readLen = readLen;
        this.bufSize = bufSize;
        pmerFrequency = new long[numMmers];
        this.k = k;
        this.numStats = numStats;
        isInit = false;
    }

    @Override
    public void initRank() throws IOException {
        initFrequency();
        super.initRank();
        isRankInit = true;
    }

    protected int rawCompare(int xNormalized, int yNormalized, boolean xAllowed, boolean yAllowed) {
        int baseCompareValue = compareMmerBase(xNormalized, yNormalized);
        if (baseCompareValue != BOTH_IN_UHS && baseCompareValue != BOTH_NOT_IN_UHS) {
            return baseCompareValue;
        }

        // from down here - both in UHS

        if (useSignature) {
            if (!xAllowed && yAllowed) {
                return 1;
            } else if (!yAllowed && xAllowed) {
                return -1;
            }
        }

        // both allowed or both not allowed
        if (pmerFrequency[xNormalized] == pmerFrequency[yNormalized]) {
            if (xNormalized < yNormalized)
                return -1;
            else
                return 1;
        } else if (pmerFrequency[xNormalized] < pmerFrequency[yNormalized])
            return -1;
        else
            return 1;

    }

    private void initFrequency() throws IOException {
        FileReader frG = new FileReader(inputFile);
        BufferedReader bfrG = new BufferedReader(frG, bufSize);

        int counter = 0;
        String describeline;
        char[] lineCharArray = new char[readLen];

        while ((describeline = bfrG.readLine()) != null) {

            bfrG.read(lineCharArray, 0, readLen);
            bfrG.read();

            if (stringUtils.isReadLegal(lineCharArray)) {
                for (int i = 0; i <= lineCharArray.length - pivotLength; i++) {

                    int value = stringUtils.getNormalizedValue(stringUtils.getDecimal(lineCharArray, i, i + pivotLength), pivotLength);
                    pmerFrequency[value] += 1;
                    counter++;
                }
                if (counter > 1000000) {
                    break;
                }
            }
        }
//        initStats(bfrG);
        bfrG.close();
        frG.close();
    }

    private void initStats(BufferedReader bfrG) throws IOException {

        int numSampled = 0;
        boolean keepSample = true;

        statsFrequency = new long[numMmers];

        String describeline;
        char[] lineCharArray = new char[readLen];

        int len = readLen;


        int min_pos = -1;
        int minValue, currentValue;

        while (keepSample && (describeline = bfrG.readLine()) != null) {

            bfrG.read(lineCharArray, 0, readLen);
            bfrG.read();
            String line = new String(lineCharArray);

            if (stringUtils.isReadLegal(lineCharArray)) {

                min_pos = findSmallest(lineCharArray, 0, k);
                minValue = stringUtils.getDecimal(lineCharArray, min_pos, min_pos + pivotLength);
                currentValue = stringUtils.getDecimal(lineCharArray, k - pivotLength, k);

                statsFrequency[minValue]++;

                int bound = len - k + 1;
                for (int i = 1; i < bound; i++) {
                    numSampled++;
                    currentValue = ((currentValue << 2) + StringUtils.valTable[lineCharArray[i + k - 1] - 'A']) & mask;//0xffff;

                    if (i > min_pos) {
                        min_pos = findSmallest(lineCharArray, i, i + k);
                        minValue = stringUtils.getDecimal(lineCharArray, min_pos, min_pos + pivotLength);


                        statsFrequency[minValue]++;
                    } else {
                        int lastIndexInWindow = k + i - pivotLength;
                        if (compareMmer(currentValue, minValue) < 0) {
                            min_pos = lastIndexInWindow;
                            minValue = currentValue;

                            statsFrequency[minValue]++;
                        }
                    }
                    statsFrequency[minValue]++;
                }
                if (numSampled > numStats) keepSample = false;
            }
        }
    }
}
