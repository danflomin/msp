package dumbo.Ordering.UHS;

import dumbo.Ordering.OrderingBase;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;

public abstract class UHSOrderingBase extends OrderingBase {

    protected byte[] uhsBits;

    protected static final int BOTH_IN_UHS = 824;
    protected static final int BOTH_NOT_IN_UHS = 1001;



    public UHSOrderingBase(int pivotLen) throws IOException {
        super(pivotLen);
        uhsBits = uhsBitSet(pivotLen);
        Arrays.fill(mmerRanks, Integer.MAX_VALUE);

    }

    abstract int rawCompare(int x, int y);


    public boolean isInUHS(int pmerDecimal) {
        int pmerDecimalDiv8 = pmerDecimal >> 3;
        int pmerDecimalMod8 = pmerDecimal & 0b111;
        if (((this.uhsBits[pmerDecimalDiv8] >> (pmerDecimalMod8)) & 1) == 1) {
            return true;
        }
        return false;
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

    public void initializeRanks() throws IOException {
        System.out.println("start init rank");
        HashSet<Integer> normalizedMmersUHS = new HashSet<>();
        for (int i = 0; i < numMmers; i++) {
            if (isInUHS(i)) normalizedMmersUHS.add(i);
        }

        Integer[] pmersArr = new Integer[normalizedMmersUHS.size()];
        normalizedMmersUHS.toArray(pmersArr);
        Arrays.sort(pmersArr, this::rawCompare);
        for (int i = 0; i < pmersArr.length; i++) {
            mmerRanks[pmersArr[i]] = i;
        }
        normalize();
        System.out.println("finish init rank");
    }


}
