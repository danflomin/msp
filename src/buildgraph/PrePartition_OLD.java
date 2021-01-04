package buildgraph;

import java.io.*;
import java.util.*;

public class PrePartition_OLD {

    private int k;
    private String inputfile;
    private int numOfBlocks;
    private int pivotLen;
    private int bufSize;

    private FileReader frG;
    private BufferedReader bfrG;
    private FileWriter fwG;
    private BufferedWriter bfwG;

    private int readLen;

    private Hashtable<Integer, Integer> charsPerMSP;

    private static int[] valTable = new int[]{0, -1, 1, -1, -1, -1, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 3};
    private static char[] twinTable = new char[]{'T', '0', 'G', '0', '0', '0', 'C', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', 'A'};

    public PrePartition_OLD(int kk, String infile, int pivotLength, int bufferSize, int readLen) {
        this.k = kk;
        this.inputfile = infile;
        this.numOfBlocks = 1;
        this.pivotLen = pivotLength;
        this.bufSize = bufferSize;
        this.readLen = readLen;
        this.charsPerMSP = new Hashtable<Integer, Integer>();
    }

    private boolean isReadLegal(char[] line) {
        int Len = line.length;
        for (int i = 0; i < Len; i++) {
            if (line[i] != 'A' && line[i] != 'C' && line[i] != 'G' && line[i] != 'T')
                return false;
        }
        return true;
    }

    private int strcmp(char[] a, char[] b, int froma, int fromb, int len) {
        for (int i = 0; i < len; i++) {
            if (a[froma + i] < b[fromb + i])
                return -1;
            else if (a[froma + i] > b[fromb + i])
                return 1;
        }
        return 0;
    }

    private int findSmallest(char[] a, int from, int to) {

        int min_pos = from;

        for (int i = from + 1; i <= to - pivotLen; i++) {
            if (strcmp(a, a, min_pos, i, pivotLen) > 0)
                min_pos = i;
        }

        return min_pos;
    }

    private int findPosOfMin(char[] a, char[] b, int from, int to, int[] flag) {

        int len = a.length;
        int pos1 = findSmallest(a, from, to);
        int pos2 = findSmallest(b, len - to, len - from);

        if (strcmp(a, b, pos1, pos2, pivotLen) < 0) {
            flag[0] = 0;
            return pos1;
        } else {
            flag[0] = 1;
            return pos2;
        }
    }

    private int calRawPosNew(char[] a, int from, int to) {

        int val = 0;

        for (int i = from; i < to; i++) {
            val = val << 2;
            val += valTable[a[i] - 'A'];
        }

        return val;
    }

    private int calPosNew(char[] a, int from, int to) {
        return calRawPosNew(a, from, to);
    }

    private void updateCharsPerMSP(Integer pmerValue, Integer numChars){
        if(!charsPerMSP.containsKey(pmerValue)){
			charsPerMSP.put(pmerValue, 0);
		}
		charsPerMSP.put(pmerValue, charsPerMSP.get(pmerValue) + numChars);
    }


    private void writeToFile(char[] lineCharArray, int substart, int subend, long outcnt) throws IOException{
        // TODO: need to add what is this line's pmer value
        bfwG.write(lineCharArray, substart, subend - substart);
        bfwG.write("\t" + outcnt);
        bfwG.newLine();
    }

    private long DistributeNodes() throws IOException {
        frG = new FileReader(inputfile);
        bfrG = new BufferedReader(frG, bufSize);
        fwG = new FileWriter("Nodes/nodes");
        bfwG = new BufferedWriter(fwG, bufSize);

        String describeline;

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

            if (isReadLegal(lineCharArray)) {

                substart = 0;

                outcnt = cnt;

                char[] revCharArray = new char[readLen];

                for (int i = 0; i < readLen; i++) {
                    revCharArray[i] = twinTable[lineCharArray[readLen - 1 - i] - 'A'];
                }

                min_pos = findPosOfMin(lineCharArray, revCharArray, 0, k, flag);

                cnt += 2;

                int bound = readLen - k + 1;

                for (int i = 1; i < bound; i++) {
                    // the last min-p-sub is not in the current kmer scope
                    if (i > (flag[0] == 0 ? min_pos : readLen - min_pos - pivotLen)) {

                        int temp = (flag[0] == 0 ? calPosNew(lineCharArray, min_pos, min_pos + pivotLen) : calPosNew(revCharArray, min_pos, min_pos + pivotLen));

                        min_pos = findPosOfMin(lineCharArray, revCharArray, i, i + k, flag);

                        if (temp != (flag[0] == 0 ? calPosNew(lineCharArray, min_pos, min_pos + pivotLen) : calPosNew(revCharArray, min_pos, min_pos + pivotLen))) {
                            subend = i - 1 + k;

                            writeToFile(lineCharArray, substart, subend, outcnt);

                            substart = i;
                            outcnt = cnt;
                        }

                    }
                    else { // occurs as long as the last min-p-sub is in the scope of the current kmer
                        subend = i - 1 + k;
                        if (strcmp(lineCharArray, revCharArray, k + i - pivotLen, readLen - i - k, pivotLen) < 0) {
                            if (flag[0] == 1  && strcmp(lineCharArray, revCharArray, k + i - pivotLen, min_pos, pivotLen) < 0) {
                                // will enter this is only if flag == 1 -- working on reverse complement
                                int temp = calPosNew(revCharArray, min_pos, min_pos + pivotLen);

                                min_pos = k + i - pivotLen;

                                if (temp != calPosNew(lineCharArray, min_pos, min_pos + pivotLen)) {
                                    writeToFile(lineCharArray, substart, subend, outcnt);
                                }

                                flag[0] = 0;

                            }
                        }
                        else {
                            if (flag[0] == 0 && strcmp(revCharArray,lineCharArray , readLen - i - k, min_pos, pivotLen) < 0) {
                                // will enter this only if flag = 0
                                int temp = calPosNew(lineCharArray, min_pos, min_pos + pivotLen);

                                min_pos = -k - i + readLen;

                                if (temp != calPosNew(revCharArray, min_pos, min_pos + pivotLen)) {
                                    writeToFile(lineCharArray, substart, subend, outcnt);
                                }

                                flag[0] = 1;
                            }
                        }
                        substart = i;
                        outcnt = cnt;
                    }

                    cnt += 2;
                }

                subend = readLen;
                writeToFile(lineCharArray, substart, subend, outcnt);
            }
        }

        System.out.println("Largest ID is " + cnt);


        bfwG.close();
        fwG.close();

        bfrG.close();
        frG.close();

        return cnt;
    }

    public long Run() throws Exception {

        long time1 = 0;

        long t1 = System.currentTimeMillis();
        System.out.println("Distribute Nodes Begin!");
        long maxID = DistributeNodes();
        long t2 = System.currentTimeMillis();
        time1 = (t2 - t1) / 1000;
        System.out.println("Time used for distributing nodes: " + time1 + " seconds!");
        return maxID;

    }


//    public static void main(String[] args) {
//
//        String infile = "E:\\test.txt";
//        int k = 15, numBlocks = 256, pivot_len = 12, bufferSize = 8192, readLen = 101;
//
//        if (args[0].equals("-help")) {
//            System.out.print("Usage: java -jar Partition.jar -in InputPath -k k -L readLength[options]\n" +
//                    "Options Available: \n" +
//                    "[-NB numOfBlocks] : (Integer) Number Of Kmer Blocks. Default: 256" + "\n" +
//                    "[-p pivotLength] : (Integer) Pivot Length. Default: 12" + "\n" +
//                    "[-b bufferSize] : (Integer) Read/Writer Buffer Size. Default: 8192" + "\n");
//            return;
//        }
//
//        for (int i = 0; i < args.length; i += 2) {
//            if (args[i].equals("-in"))
//                infile = args[i + 1];
//            else if (args[i].equals("-k"))
//                k = new Integer(args[i + 1]);
//            else if (args[i].equals("-NB"))
//                numBlocks = new Integer(args[i + 1]);
//            else if (args[i].equals("-p"))
//                pivot_len = new Integer(args[i + 1]);
//            else if (args[i].equals("-b"))
//                bufferSize = new Integer(args[i + 1]);
//            else if (args[i].equals("-L"))
//                readLen = new Integer(args[i + 1]);
//            else {
//                System.out.println("Wrong with arguments. Abort!");
//                return;
//            }
//        }
//
//
//        Partition bdgraph = new PrePartition_OLD(k, infile, numBlocks, pivot_len, bufferSize, readLen);
//
//        try {
//
//            System.out.println("Program Configuration:");
//            System.out.print("Input File: " + infile + "\n" +
//                    "Kmer Length: " + k + "\n" +
//                    "Read Length: " + readLen + "\n" +
//                    "# Of Blocks: " + numBlocks + "\n" +
//                    "Pivot Length: " + pivot_len + "\n" +
//                    "R/W Buffer Size: " + bufferSize + "\n");
//
//            bdgraph.Run();
//
//        } catch (Exception E) {
//            System.out.println("Exception caught!");
//            E.printStackTrace();
//        }
//
//    }
}
	
	