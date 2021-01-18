package buildgraph.Ordering;

import java.io.*;
import java.util.HashMap;

public class UniversalFrequencySignatureOrdering extends UniversalHittingSetSignatureOrdering {
    private SignatureUtils signatureUtils;
    private String inputFile;
    private int readLen;
    private int bufSize;
    private long[] pmerFrequency;
    private boolean useSignature;
    private boolean isInit;

    public UniversalFrequencySignatureOrdering(int pivotLen, String infile, int readLen, int bufSize, boolean useSignature) throws IOException {
        super(0, pivotLen);
        this.inputFile = infile;
        this.readLen = readLen;
        this.bufSize = bufSize;
        pmerFrequency = new long[(int)Math.pow(4, pivotLen)];
        this.useSignature = useSignature;
        isInit = false;

    }

    protected int strcmpSignature(int x, int y, boolean xAllowed, boolean yAllowed) throws IOException {
        int baseCompareValue = strcmpBase(x, y);
        if (baseCompareValue != BOTH_IN_UHS) {
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

        if(!isInit){
            initFrequency();
            isInit = true;
        }

        // both allowed or both not allowed
        if(pmerFrequency[x] < pmerFrequency[y])
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
        bfrG.close();
        frG.close();
    }




}
