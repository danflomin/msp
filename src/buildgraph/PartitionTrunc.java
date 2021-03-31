package buildgraph;

import buildgraph.Ordering.IOrdering;
import buildgraph.Ordering.UHS.YaelUHSOrdering;

import java.io.*;

public class PartitionTrunc {

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
    private IOrdering ordering;

    private StringUtils stringUtils;

    private int numOpenFiles;
    private int minFile;
    private int maxFile;


    public PartitionTrunc(int kk, String infile, int numberOfBlocks, int pivotLength, int bufferSize, int readLen, IOrdering ordering) {
        this.k = kk;
        this.inputfile = infile;
        this.numOfBlocks = numberOfBlocks;
        this.pivotLen = pivotLength;
        this.bufSize = bufferSize;
        this.readLen = readLen;
        this.ordering = ordering;
        this.stringUtils = new StringUtils();
        this.numOpenFiles = 0;
    }


    private int findPosOfMin(char[] a, int from, int to) throws IOException {

        int len = a.length;
        int pos1 = ordering.findSmallest(a, from, to);
        return pos1;

    }

    private int calPosNew(char[] a, int from, int to) {
        return Math.min(stringUtils.getDecimal(a, from, to), stringUtils.getReversedMmer(stringUtils.getDecimal(a, from, to), pivotLen)) % numOfBlocks;
    }

    private long DistributeNodes() throws IOException {
        frG = new FileReader(inputfile);
        bfrG = new BufferedReader(frG, bufSize);
        fwG = new FileWriter[numOfBlocks];
        bfwG = new BufferedWriter[numOfBlocks];

        String describeline;

        int numSuperKmers = 0;

        int prepos, substart = 0, subend, min_pos = -1;

        char[] lineCharArray = new char[readLen];

        long cnt = 0, outcnt = 0;

        File dir = new File("Nodes");
        if (!dir.exists())
            dir.mkdir();


        while ((describeline = bfrG.readLine()) != null) {

            bfrG.read(lineCharArray, 0, readLen);
            bfrG.read();

            prepos = -1;
            if (stringUtils.isReadLegal(lineCharArray)) {

                outcnt = cnt;

                int len = readLen;

                cnt += 2;

                int bound = len - k + 1;

                for (int i = 0; i < bound; i++) {
                    min_pos = findPosOfMin(lineCharArray, i, i + k);
                    prepos = calPosNew(lineCharArray, min_pos, min_pos+pivotLen);
                    writeToFile(prepos, i, i+k, lineCharArray, outcnt);
                    numSuperKmers++;

                    outcnt = cnt;
                    cnt += 2;
                }
            }
        }

        System.out.println("Largest ID is " + cnt);
        System.out.println("Num superkmers is = " + numSuperKmers);

        for (int i = 0; i < bfwG.length; i++) {
            if (bfwG[i] != null) {
                bfwG[i].close();
                fwG[i].close();
            }
        }

        bfrG.close();
        frG.close();

        return cnt;
    }

    private void tryCreateWriterForPmer(int prepos) throws IOException {
        if (numOpenFiles == 16000) {
            for (int i = 0; i < bfwG.length; i++) {
                if (bfwG[i] != null) {
                    bfwG[i].close();
                    fwG[i].close();
                    bfwG[i] = null;
                    fwG[i] = null;
                }
            }
            numOpenFiles = 0;
        }

        if (bfwG[prepos] == null) {
            fwG[prepos] = new FileWriter("Nodes/nodes" + prepos, true);
            bfwG[prepos] = new BufferedWriter(fwG[prepos], bufSize);
            numOpenFiles += 1;
        }
    }

    private void writeToFile(int prepos, int substart, int subend, char[] lineCharArray, long outcnt) throws IOException {
        if (minFile <= prepos && prepos < maxFile) {
            tryCreateWriterForPmer(prepos);

            BufferedWriter writer = bfwG[prepos];

            writer.write(lineCharArray, substart, subend - substart);
            writer.write("\t" + outcnt);
            writer.newLine();
        }
    }

    public void Run() throws Exception {
        long time1 = 0;
        long t1 = System.currentTimeMillis();
        System.out.println("Distribute Nodes Begin!");
        for (minFile = 0, maxFile = 10000; minFile < numOfBlocks; minFile += 10000, maxFile += 10000) {
            System.out.println("hi");
            DistributeNodes();
        }
        long t2 = System.currentTimeMillis();
        time1 = (t2 - t1) / 1000;
        System.out.println("Time used for distributing nodes: " + time1 + " seconds!");
    }

}