package dumbo.Ordering.UHS;

import dumbo.Ordering.OrderingBase;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public abstract class UHSOrderingBase extends OrderingBase {

    protected byte[] normalizedUHS;

    protected static final int BOTH_IN_UHS = 824;
    protected static final int BOTH_NOT_IN_UHS = 1001;


    public UHSOrderingBase(int pivotLen) throws IOException {
        super(pivotLen);
        normalizedUHS = uhsBitSet(pivotLen);

    }

    public boolean isInUHS(int pmerDecimal) {
        return normalizedUHS[pmerDecimal] == 1;
    }


    protected int compareMmerBase(int xNormalized, int yNormalized) {
        if (xNormalized == yNormalized)
            return 0;

        boolean xInUHS = isInUHS(xNormalized);
        boolean yInUHS = isInUHS(yNormalized);
        if (xInUHS) {
            if (!yInUHS) return -1;
            return BOTH_IN_UHS;
        }
        if (yInUHS)
            return 1;
        return BOTH_NOT_IN_UHS;
    }

    private byte[] uhsBitSet(int pivotLen) throws IOException {
        int i = 0;
        byte[] bits = new byte[numMmers];

        String DocksFile = "res_" + pivotLen + ".txt";
        FileReader frG = new FileReader(DocksFile);
        int count = 0;

        BufferedReader reader;
        try {
            reader = new BufferedReader(frG);
            String line;
            while ((line = reader.readLine()) != null) {
                i = stringUtils.getNormalizedValue(stringUtils.getDecimal(line.toCharArray(), 0, pivotLen), pivotLength);
                bits[i] = 1;
                count++;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(count);
        frG.close();

        return bits;
    }


}
