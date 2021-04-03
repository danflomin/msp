package dumbo.Ordering.Standard;

import dumbo.Ordering.OrderingBase;
import dumbo.StringUtils;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;

public class FrequencyOrdering extends OrderingBase {
    private String inputFile;
    private int readLen;
    private int bufSize;
    private int[] pmerFrequency;
    private int[] currentOrdering;
    private long[] statsFrequency;
    private int numSamples;
    private int numStats;
    private int k;

    public FrequencyOrdering(int pivotLen, String infile, int readLen, int bufSize, int numSamples, int numStats, int k) {
        super(pivotLen);
        this.inputFile = infile;
        this.readLen = readLen;
        this.bufSize = bufSize;
        pmerFrequency = new int[numMmers];
        currentOrdering = new int[numMmers];
        this.numSamples = numSamples;
        this.numStats = numStats;
        this.k = k;
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
                for (int i = 0; i < lineCharArray.length - pivotLength; i++) {

                    int value = stringUtils.getNormalizedValue(stringUtils.getDecimal(lineCharArray, i, i + pivotLength), pivotLength);
                    pmerFrequency[value] += 1;

                    counter++;
                }
                if (counter > numSamples) {
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
//        TODO: FIX

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


    protected void normalize() {
        Integer[] temp = new Integer[pmerFrequency.length];
        for (int i = 0; i < temp.length; i++)
            temp[i] = i;

        Arrays.sort(temp, Comparator.comparingInt(a -> pmerFrequency[a]));
        for (int i = 0; i < temp.length; i++) {
            currentOrdering[temp[i]] = i;
        }

    }

    @Override
    public int[] getRanks() {
        return currentOrdering.clone();
    }

    public int compareMmer(int x, int y) {
        int a = stringUtils.getNormalizedValue(x, pivotLength);
        int b = stringUtils.getNormalizedValue(y, pivotLength);
        if (a == b) return 0;

        if (pmerFrequency[a] < pmerFrequency[b])
            return -1;
        else
            return 1;
    }


}
