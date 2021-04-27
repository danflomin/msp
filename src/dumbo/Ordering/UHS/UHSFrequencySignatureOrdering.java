package dumbo.Ordering.UHS;

import dumbo.Ordering.Standard.SignatureUtils;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;

public class UHSFrequencySignatureOrdering extends UHSOrderingBase {
    private String inputFile;
    private int readLen;
    private int bufSize;
    private int[] mmerFrequency;
    private int numMmersToCount;

    private SignatureUtils signatureUtils;
    protected boolean useSignature;


    public UHSFrequencySignatureOrdering(int pivotLen, String infile, int readLen, int bufSize, boolean useSignature, int numMmersToCount) throws IOException {
        super(pivotLen);
        this.inputFile = infile;
        this.readLen = readLen;
        this.bufSize = bufSize;
        this.mmerFrequency = new int[numMmers];
        this.numMmersToCount = numMmersToCount;

        this.useSignature = useSignature;
        this.signatureUtils = new SignatureUtils(pivotLen);
    }

    @Override
    public void initializeRanks() throws Exception {
        System.out.println("start init rank");

        countFrequency();
        Arrays.fill(mmerRanks, Integer.MAX_VALUE);

        int idx = 0;

        HashSet<Integer> normalizedAllowedMmersUHS = new HashSet<>();
        for (int i = 0; i < numMmers; i++) {
            if (isInUHS(i) && (!useSignature || signatureUtils.isAllowed(i)))
                normalizedAllowedMmersUHS.add(i);
        }
        Integer[] allowedMmers = new Integer[normalizedAllowedMmersUHS.size()];
        normalizedAllowedMmersUHS.toArray(allowedMmers);
        Arrays.sort(allowedMmers, Comparator.comparingInt(a -> mmerFrequency[a]));
        for (int i = 0; i < allowedMmers.length; i++) {
            mmerRanks[allowedMmers[i]] = idx;
            idx++;
        }

        if (useSignature) {
            HashSet<Integer> normalizedNotAllowedMmersUHS = new HashSet<>();
            for (int i = 0; i < numMmers; i++) {
                if (isInUHS(i) && (!signatureUtils.isAllowed(i)))
                    normalizedNotAllowedMmersUHS.add(i);
            }
            Integer[] notAllowedMmers = new Integer[normalizedNotAllowedMmersUHS.size()];
            normalizedNotAllowedMmersUHS.toArray(notAllowedMmers);
            Arrays.sort(notAllowedMmers, Comparator.comparingInt(a -> mmerFrequency[a]));
            for (int i = 0; i < notAllowedMmers.length; i++) {
                mmerRanks[notAllowedMmers[i]] = idx;
                idx++;
            }
        }

        normalize();
        System.out.println("finish init rank");
        isRankInitialized = true;
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
