package buildgraph;

import buildgraph.Ordering.IOrdering;
import buildgraph.Ordering.UHS.UHSOrderingBase;
import buildgraph.Ordering.UHS.YaelUHSOrdering;

import java.io.*;

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
    private IOrdering ordering;

    private StringUtils stringUtils;

    private int numOpenFiles;
    private int minFile;
    private int maxFile;


    public Partition(int kk, String infile, int numberOfBlocks, int pivotLength, int bufferSize, int readLen, IOrdering ordering) {
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


    private int findPosOfMin(char[] a, char[] b, int from, int to, int[] flag) throws IOException {

        int len = a.length;
        int pos1 = ordering.findSmallest(a, from, to);
        int pos2 = ordering.findSmallest(b, len - to, len - from);

        if (ordering.strcmp(a, b, pos1, pos2, pivotLen) < 0) {
            flag[0] = 0;
            return pos1;
        } else {
            flag[0] = 1;
            return pos2;
        }
    }

    private int calPosNew(char[] a, int from, int to) {
        return stringUtils.getDecimal(a, from, to) % numOfBlocks;
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

        int[] flag = new int[1];

        long cnt = 0, outcnt = 0;

        File dir = new File("Nodes");
        if (!dir.exists())
            dir.mkdir();


        while ((describeline = bfrG.readLine()) != null) {

            bfrG.read(lineCharArray, 0, readLen);
            bfrG.read();

            prepos = -1;
            if (stringUtils.isReadLegal(lineCharArray)) {

                substart = 0;

                outcnt = cnt;

                int len = readLen;

                char[] revCharArray = stringUtils.getReversedRead(lineCharArray);

                min_pos = findPosOfMin(lineCharArray, revCharArray, 0, k, flag);

                cnt += 2;

                int bound = len - k + 1;

                for (int i = 1; i < bound; i++) {

                    if (i > (flag[0] == 0 ? min_pos : len - min_pos - pivotLen)) {

                        int temp = (flag[0] == 0 ? calPosNew(lineCharArray, min_pos, min_pos + pivotLen) : calPosNew(revCharArray, min_pos, min_pos + pivotLen));

                        min_pos = findPosOfMin(lineCharArray, revCharArray, i, i + k, flag);

                        if (temp != (flag[0] == 0 ? calPosNew(lineCharArray, min_pos, min_pos + pivotLen) : calPosNew(revCharArray, min_pos, min_pos + pivotLen))) {
                            prepos = temp;
                            subend = i - 1 + k;


                            writeToFile(prepos, substart, subend, lineCharArray, outcnt);
                            numSuperKmers++;

                            substart = i;
                            outcnt = cnt;
                        }

                    } else {

                        if (ordering.strcmp(lineCharArray, revCharArray, k + i - pivotLen, len - i - k, pivotLen) < 0) {
                            if (ordering.strcmp(lineCharArray, flag[0] == 0 ? lineCharArray : revCharArray, k + i - pivotLen, min_pos, pivotLen) < 0) {
                                boolean enter = true;
                                if (ordering instanceof YaelUHSOrdering) {
                                    if (!((YaelUHSOrdering) ordering).isInUHS(lineCharArray, k + i - pivotLen, k + i)) {
                                        enter = false;
                                    }
                                }
                                if (enter) {
                                    int temp = (flag[0] == 0 ? calPosNew(lineCharArray, min_pos, min_pos + pivotLen) : calPosNew(revCharArray, min_pos, min_pos + pivotLen));

                                    min_pos = k + i - pivotLen;

                                    if (temp != calPosNew(lineCharArray, min_pos, min_pos + pivotLen)) {
                                        prepos = temp;
                                        subend = i - 1 + k;

                                        writeToFile(prepos, substart, subend, lineCharArray, outcnt);
                                        numSuperKmers++;


                                        substart = i;
                                        outcnt = cnt;
                                    }

                                    flag[0] = 0;
                                }
                            }
                        } else {
                            if (ordering.strcmp(revCharArray, flag[0] == 0 ? lineCharArray : revCharArray, len - i - k, min_pos, pivotLen) < 0) {
                                boolean enter = true;
                                if (ordering instanceof YaelUHSOrdering) {
                                    if (!((YaelUHSOrdering) ordering).isInUHS(revCharArray, len - i - k, len - i - k + pivotLen)) {
                                        enter = false;
                                    }
                                }
                                if (enter) {
                                    int temp = (flag[0] == 0 ? calPosNew(lineCharArray, min_pos, min_pos + pivotLen) : calPosNew(revCharArray, min_pos, min_pos + pivotLen));

                                    min_pos = -k - i + len;

                                    if (temp != calPosNew(revCharArray, min_pos, min_pos + pivotLen)) {
                                        prepos = temp;
                                        subend = i - 1 + k;

                                        writeToFile(prepos, substart, subend, lineCharArray, outcnt);
                                        numSuperKmers++;


                                        substart = i;
                                        outcnt = cnt;
                                    }
                                    flag[0] = 1;
                                }
                            }
                        }
                    }

                    cnt += 2;
                }
                subend = len;
                prepos = (flag[0] == 0 ? calPosNew(lineCharArray, min_pos, min_pos + pivotLen) : calPosNew(revCharArray, min_pos, min_pos + pivotLen));

                writeToFile(prepos, substart, subend, lineCharArray, outcnt);
                numSuperKmers++;

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
        if(minFile <= prepos && prepos < maxFile)
        {
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
        for(minFile = 0, maxFile=10000; minFile < (int)Math.pow(4, pivotLen); minFile+= 10000, maxFile += 10000)
        {
            System.out.println("hi");
            DistributeNodes();
        }
        long t2 = System.currentTimeMillis();
        time1 = (t2 - t1) / 1000;
        System.out.println("Time used for distributing nodes: " + time1 + " seconds!");
    }

}