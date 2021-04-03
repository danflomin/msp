package dumbo.Ordering.UHS;

import dumbo.Ordering.Standard.SignatureUtils;

import java.io.*;

public class UHSFrequencySignatureOrdering extends UHSOrderingBase {
    private String inputFile;
    private int readLen;
    private int bufSize;
    private long[] mmerFrequency;
    private int numMmersToCount;

    private SignatureUtils signatureUtils;
    protected boolean useSignature;


    public UHSFrequencySignatureOrdering(int pivotLen, String infile, int readLen, int bufSize, boolean useSignature, int numMmersToCount) throws IOException {
        super(pivotLen);
        this.inputFile = infile;
        this.readLen = readLen;
        this.bufSize = bufSize;
        this.mmerFrequency = new long[numMmers];
        this.numMmersToCount = numMmersToCount;

        this.useSignature = useSignature;
        this.signatureUtils = new SignatureUtils(pivotLen);
    }

    @Override
    public void initializeRanks() throws Exception {
        countFrequency();
        super.initializeRanks();
        isRankInitialized = true;
    }

    @Override
    protected int rawCompareMmer(int x, int y) {
        int a = stringUtils.getNormalizedValue(x, pivotLength);
        int b = stringUtils.getNormalizedValue(y, pivotLength);

        if (a == b) return 0;

        boolean aAllowed = true, bAllowed = true;
        if (useSignature) {
            aAllowed = signatureUtils.isAllowed(a);
            bAllowed = signatureUtils.isAllowed(b);
        }

        return rawCompareMmer(a, b, aAllowed, bAllowed);
    }

    protected int rawCompareMmer(int xNormalized, int yNormalized, boolean xAllowed, boolean yAllowed) {
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
        if (mmerFrequency[xNormalized] == mmerFrequency[yNormalized]) {
            if (xNormalized < yNormalized)
                return -1;
            else
                return 1;
        } else if (mmerFrequency[xNormalized] < mmerFrequency[yNormalized])
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
                    mmerFrequency[value] += 1;
                    counter++;
                }
                if (counter > numMmersToCount) {
                    break;
                }
            }
        }
        bfrG.close();
        frG.close();
    }


}
