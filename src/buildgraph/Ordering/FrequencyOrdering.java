package buildgraph.Ordering;

import buildgraph.Ordering.UHS.UHSSignatureOrdering;
import buildgraph.StringUtils;

import java.io.*;
import java.net.Inet4Address;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

public class FrequencyOrdering implements IOrdering {
    private int pivotLength;
    private String inputFile;
    private int readLen;
    private int bufSize;
    private long[] pmerFrequency;
    private long[] statsFrequency;
    private int numSamples;
    private int numStats;
    private int k;
    private StringUtils stringUtils;
    private int mask;

    public FrequencyOrdering(int pivotLen, String infile, int readLen, int bufSize, int numSamples, int numStats, int k) {
        pivotLength = pivotLen;
        this.inputFile = infile;
        this.readLen = readLen;
        this.bufSize = bufSize;
        pmerFrequency = new long[(int)Math.pow(4, pivotLen)];
        this.numSamples = numSamples;
        this.numStats = numStats;
        this.k = k;
        stringUtils = new StringUtils();
        mask = (int)Math.pow(4, pivotLen) - 1;
    }

    public void initFrequency() throws IOException {
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
                for (int i = 0; i < lineCharArray.length-pivotLength; i++) {

                    int lineValue = stringUtils.getDecimal(lineCharArray, i, i+pivotLength);
                    pmerFrequency[lineValue] += 1;

                    int revValue = stringUtils.getDecimal(revCharArray, i, i+pivotLength);
                    pmerFrequency[revValue] += 1;

                    counter++;
                }
                if(counter > numSamples){
                    break;
                }
            }
        }

        normalize();
        initStats(bfrG);

        bfrG.close();
        frG.close();
    }

    private void initStats(BufferedReader bfrG) throws IOException {

        int numSampled = 0;
        boolean keepSample = true;

        statsFrequency = new long[(int) Math.pow(4, pivotLength)];

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

    public long[] getRawOrdering()
    {
        return pmerFrequency.clone();
    }


    private void normalize() {
        Integer[] temp = new Integer[pmerFrequency.length];
        for (int i = 0; i < temp.length; temp[i] = i, i++) ;

        Arrays.sort(temp, this::strcmp);
        for(int i = 0 ; i<temp.length; i++){
            pmerFrequency[i] = temp[i];
        }

        for(int i = 0 ; i<temp.length; i++){
            int rev = getReversed(i);
            long min = Math.max(pmerFrequency[i], pmerFrequency[rev]);
            pmerFrequency[i] = min;
            pmerFrequency[rev] = min;
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
    public int strcmp(char[] a, char[] b, int froma, int fromb, int len) throws IOException {
        int x = stringUtils.getDecimal(a, froma, froma + pivotLength);
        int y = stringUtils.getDecimal(b, fromb, fromb + pivotLength);

        return strcmp(x,y);
    }

    public int strcmp(int x, int y)
    {
        if (x == y) return 0;
        if (pmerFrequency[x] == pmerFrequency[y]) {
            if(x<y)
                return -1;
            return 1;
        }
        if(pmerFrequency[x] < pmerFrequency[y])
            return -1;
        return 1;
    }


    public void exportOrderingForCpp() {
        File file = new File("ranks.txt");

        BufferedWriter bf = null;

        try {
            bf = new BufferedWriter(new FileWriter(file));

            for (int i = 0; i < pmerFrequency.length; i++) {
                bf.write(Long.toString(pmerFrequency[i]));
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
