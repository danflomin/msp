package buildgraph.Ordering.UHS;

import buildgraph.StringUtils;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;

public class UHSFrequencySignatureOrdering extends UHSSignatureOrdering {
    private String inputFile;
    private int readLen;
    private int bufSize;
    private long[] pmerFrequency;
    private int k;
    private int numStats;
    private boolean isInit;

    private long[] statsFrequency;
    private int mask;

    public UHSFrequencySignatureOrdering(int pivotLen, String infile, int readLen, int bufSize, boolean useSignature, int k, int numStats) throws IOException {
        super(0, pivotLen, useSignature);
        this.inputFile = infile;
        this.readLen = readLen;
        this.bufSize = bufSize;
        pmerFrequency = new long[(int)Math.pow(4, pivotLen)];
        this.k = k;
        this.numStats = numStats;
        isInit = false;
        mask = (int)Math.pow(4, pivotLen) - 1;
    }

    @Override
    public void initRank() throws IOException {
        initFrequency();
        super.initRank();
        isRankInit = true;
    }

    protected int strcmpSignature(int x, int y, boolean xAllowed, boolean yAllowed) throws IOException {
        int baseCompareValue = strcmpBase(x, y);
        if (baseCompareValue != BOTH_IN_UHS && baseCompareValue != BOTH_NOT_IN_UHS) {
            return baseCompareValue;
        }

        // from down here - both in UHS

        if(useSignature){
            if (!xAllowed && yAllowed) {
                return 1;
            } else if (!yAllowed && xAllowed) {
                return -1;
            }
        }

        // both allowed or both not allowed
        if(pmerFrequency[x] == pmerFrequency[y]){
            if(x<y)
                return -1;
            else
                return 1;
        }
        else if(pmerFrequency[x] < pmerFrequency[y])
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
                char[] revCharArray = stringUtils.getReversedRead(lineCharArray);
                for (int i = 0; i < lineCharArray.length-pivotLen; i++) {

                    int lineValue = stringUtils.getDecimal(lineCharArray, i, i+pivotLen);
                    pmerFrequency[lineValue] += 1;

                    int revValue = stringUtils.getDecimal(revCharArray, i, i+pivotLen);
                    pmerFrequency[revValue] += 1;

                    counter++;
                }
                if(counter > 1000000){
                    break;
                }
            }
        }
        initStats(bfrG);
        bfrG.close();
        frG.close();
    }

    private void initStats(BufferedReader bfrG) throws IOException {

        int numSampled = 0;
        boolean keepSample = true;

        statsFrequency = new long[(int) Math.pow(4, pivotLen)];

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
                minValue = stringUtils.getDecimal(lineCharArray, min_pos, min_pos + pivotLen);
                currentValue = stringUtils.getDecimal(lineCharArray, k - pivotLen, k);

                statsFrequency[minValue]++;

                int bound = len - k + 1;
                for (int i = 1; i < bound; i++) {
                    numSampled++;
                    currentValue = ((currentValue << 2) + StringUtils.valTable[lineCharArray[i + k - 1] - 'A']) & mask;//0xffff;

                    if (i > min_pos) {
                        min_pos = findSmallest(lineCharArray, i, i + k);
                        minValue = stringUtils.getDecimal(lineCharArray, min_pos, min_pos + pivotLen);


                        statsFrequency[minValue]++;
                    } else {
                        int lastIndexInWindow = k + i - pivotLen;
                        if (strcmp(currentValue, minValue) < 0) {
                            min_pos = lastIndexInWindow;
                            minValue = currentValue;

                            statsFrequency[minValue]++;
                        }
                    }
                    statsFrequency[minValue]++;
                }
                if(numSampled > numStats) keepSample = false;
            }
        }
    }

//    public int[] getNormalizedForm()
//    {
//        int[] ret = rankOfPmer.clone();
//        return ret;
//    }

    public void exportOrderingForCpp() {
        File file = new File("ranks.txt");

        BufferedWriter bf = null;

        try {
            bf = new BufferedWriter(new FileWriter(file));

            for (int i = 0; i < rankOfPmer.length; i++) {
                bf.write(Long.toString(rankOfPmer[i]));
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

            for (int i = 0; i < statsFrequency.length; i++) {
                bf.write(Long.toString(statsFrequency[i]));
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
