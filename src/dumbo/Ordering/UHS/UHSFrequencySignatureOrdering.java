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
    public void initializeRanks() throws IOException {
        countFrequency();
        super.initializeRanks();
        isRankInitialized = true;
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

    private void countFrequency() throws IOException {
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


}
