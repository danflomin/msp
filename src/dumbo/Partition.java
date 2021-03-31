package dumbo;

import dumbo.Ordering.IOrderingPP;

import java.io.*;
import java.util.HashSet;

public class Partition {

    private int k;
    private String inputfile;
    private int numOfBlocks;
    private int pivotLen;
    private int bufSize;

    private FileReader frG;
    private BufferedReader bfrG;
    private FileWriter[] fwG;
    private BufferedWriter[] bfwG;

    private int readLen;
    private IOrderingPP ordering;

    private StringUtils stringUtils;


    private HashSet<Integer> currentMinimizers;
    private byte[] finishedMinimizers;
    private int maxMinimizersPerPass;
    private boolean keepPassing;

    private final int mask;


    public Partition(int kk, String infile, int numberOfBlocks, int pivotLength, int bufferSize, int readLen, IOrderingPP ordering) {
        this.k = kk;
        this.inputfile = infile;
        this.numOfBlocks = numberOfBlocks;
        this.pivotLen = pivotLength;
        this.bufSize = bufferSize;
        this.readLen = readLen;
        this.ordering = ordering;
        this.stringUtils = new StringUtils();
        this.mask = (int) Math.pow(4, pivotLength) - 1;
        this.finishedMinimizers = new byte[numOfBlocks];
        this.currentMinimizers = new HashSet<>();
        this.maxMinimizersPerPass = 1000;
        this.keepPassing = true;
    }


    private long DistributeNodes() throws IOException {
        frG = new FileReader(inputfile);
        bfrG = new BufferedReader(frG, bufSize);
        fwG = new FileWriter[numOfBlocks];
        bfwG = new BufferedWriter[numOfBlocks];

        currentMinimizers.clear();

        String describeline;

        int numSuperKmers = 0;

        int minPos = -1;

        char[] lineCharArray = new char[readLen];
        int len = readLen;


        long cnt = 0, outcnt = 0;

        File dir = new File("Nodes");
        if (!dir.exists())
            dir.mkdir();

        int minValue, minValueNormalized, currentValue, start;
        while ((describeline = bfrG.readLine()) != null) {

            bfrG.read(lineCharArray, 0, readLen);
            bfrG.read();

            if (stringUtils.isReadLegal(lineCharArray)) {

                minPos = ordering.findSmallest(lineCharArray, 0, k);
                start = 0;
                minValue = stringUtils.getDecimal(lineCharArray, minPos, minPos + pivotLen);
                minValueNormalized = getNormalizedValue(minValue);
                currentValue = stringUtils.getDecimal(lineCharArray, k - pivotLen, k);

                int bound = len - k + 1;
                for (int i = 1; i < bound; i++) {
                    currentValue = ((currentValue << 2) + StringUtils.valTable[lineCharArray[i + k - 1] - 'A']) & mask;

                    if (i > minPos) {
                        writeToFile(minValueNormalized, start, minPos + k, lineCharArray);

                        minPos = ordering.findSmallest(lineCharArray, i, i + k);
                        start = i;
                        minValue = stringUtils.getDecimal(lineCharArray, minPos, minPos + pivotLen);
                        minValueNormalized = getNormalizedValue(minValue);


                    } else {
                        int lastIndexInWindow = k + i - pivotLen;
                        if (ordering.strcmp(currentValue, minValue) < 0) {
                            writeToFile(minValueNormalized, start, lastIndexInWindow + pivotLen - 1, lineCharArray);

                            start = lastIndexInWindow + pivotLen - k;
                            minPos = lastIndexInWindow;
                            minValue = currentValue;
                            minValueNormalized = getNormalizedValue(minValue);
                        }
                    }
                }
                writeToFile(minValueNormalized, start, len, lineCharArray);
            }
        }

        System.out.println("Num superkmers is = " + numSuperKmers);

        for (int i = 0; i < bfwG.length; i++) {
            if (bfwG[i] != null) {
                bfwG[i].close();
                fwG[i].close();
            }
        }
        for (Integer i : currentMinimizers) {
            finishedMinimizers[i] = 1;
        }
        if(currentMinimizers.size() < maxMinimizersPerPass)
            keepPassing = false;
        currentMinimizers.clear();

        bfrG.close();
        frG.close();

        return cnt;
    }

    private int getNormalizedValue(int minValue) {
        return stringUtils.getNormalizedValue(minValue, pivotLen) % numOfBlocks;
    }

    private void tryCreateWriterForPmer(int prepos) throws IOException {
        if (bfwG[prepos] == null) {
            fwG[prepos] = new FileWriter("Nodes/nodes" + prepos, true);
            bfwG[prepos] = new BufferedWriter(fwG[prepos], bufSize);
        }
    }

    private void writeToFile(int prepos, int substart, int subend, char[] lineCharArray) throws IOException {
        if(finishedMinimizers[prepos] == 0 && currentMinimizers.size() < maxMinimizersPerPass)
        {
            currentMinimizers.add(prepos);
        }

        if (currentMinimizers.contains(prepos)) {
            tryCreateWriterForPmer(prepos);

            BufferedWriter writer = bfwG[prepos];

            writer.write(lineCharArray, substart, subend - substart);
            writer.newLine();
        }
    }

    public void Run() throws Exception {
        long time1 = 0;
        long t1 = System.currentTimeMillis();
        System.out.println("Distribute Nodes Begin!");
        while (keepPassing){
            System.out.println("hi");
            DistributeNodes();
        }
        long t2 = System.currentTimeMillis();
        time1 = (t2 - t1) / 1000;
        System.out.println("Time used for distributing nodes: " + time1 + " seconds!");
    }

}