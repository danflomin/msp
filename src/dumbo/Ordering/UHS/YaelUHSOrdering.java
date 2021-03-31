package dumbo.Ordering.UHS;

import dumbo.Ordering.IOrdering;
import dumbo.StringUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class YaelUHSOrdering implements IOrdering {

    private final StringUtils stringUtils;
    private int pivotLength;
    private byte[] uhs_bits;
    private int xor;
    private int mask;

    public YaelUHSOrdering(int pivotLength, int xor) throws IOException {
        this.xor = xor;
        this.stringUtils = new StringUtils();
        this.pivotLength = pivotLength;
        uhs_bits = uhsBitSet(pivotLength);
        mask = pivotLengthToHexRepresentation.get(pivotLength);
        System.out.println("YAEL UHS");
    }

    @Override
    public int findSmallest(char[] a, int from, int to){
        int min_pos = from;
        int j = stringUtils.getDecimal(a, min_pos, min_pos+pivotLength);
        int prev = j;
        for(int i=from+1; i<=to-pivotLength; i++){
            j = ((j * 4) ^ (StringUtils.valTable[a[i+pivotLength-1] - 'A'])) & mask;
            if(((this.uhs_bits[j >> 3] >> (j & 0b111)) & 1) == 1) {
                if(strcmp(prev, j)>0) {
                    min_pos = i;
                    prev = j;
                }
            }
        }
        return min_pos;
    }

    @Override
    public int strcmp(char[] a, char[] b, int froma, int fromb, int len){

        int x = stringUtils.getDecimal(a, froma, froma+pivotLength);
        int y = stringUtils.getDecimal(b, fromb, fromb+pivotLength);
        int xdiv8 = x >> 3; int xmod8 = x & 0b111;
        int ydiv8 = y >> 3; int ymod8 = y & 0b111;
        if ((((this.uhs_bits[xdiv8] >> (xmod8)) & 1) ^ ((this.uhs_bits[ydiv8] >> (ymod8)) & 1)) == 0) {
            if((x ^ xor) < (y ^ xor))
                return -1;
            else //if((x ^ 11101101) > (y ^ 11101101))
                return 1;
        }

        if (((this.uhs_bits[xdiv8] >> (xmod8)) & 1) > ((this.uhs_bits[ydiv8] >> (ymod8)) & 1))
            return -1;
        if (((this.uhs_bits[xdiv8] >> (xmod8)) & 1) < ((this.uhs_bits[ydiv8] >> (ymod8)) & 1))
            return 1;

        return 0;
    }

    private int strcmp(int x, int y){
        int xdiv8 = x >> 3; int xmod8 = x & 0b111;
        int ydiv8 = y >> 3; int ymod8 = y & 0b111;
        if ((((this.uhs_bits[xdiv8] >> (xmod8)) & 1) ^ ((this.uhs_bits[ydiv8] >> (ymod8)) & 1)) == 0) {
            if((x ^ xor) < (y ^ xor))
                return -1;
            else if((x ^ xor) > (y ^ xor))
                return 1;
        }

        if (((this.uhs_bits[xdiv8] >> (xmod8)) & 1) > ((this.uhs_bits[ydiv8] >> (ymod8)) & 1))
            return -1;
        if (((this.uhs_bits[xdiv8] >> (xmod8)) & 1) < ((this.uhs_bits[ydiv8] >> (ymod8)) & 1))
            return 1;

        return 0;
    }


    private byte[] uhsBitSet(int pivotLen) throws IOException {
        int n = (int) Math.pow(4, pivotLen) / 8;
        int i = 0;
        byte[] bits = new byte[n];

        String DocksFile = "res_" + pivotLen + ".txt";
        FileReader frG = new FileReader(DocksFile);
        int count = 0;

        BufferedReader reader;
        try {
            reader = new BufferedReader(frG);
            String line;
            while ((line = reader.readLine()) != null) {
                i = stringUtils.getDecimal(line.toCharArray(), 0, pivotLen);
                bits[i / 8] |= 1 << (i % 8);
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

    protected static HashMap<Integer, Integer> pivotLengthToHexRepresentation = new HashMap<Integer, Integer>() {
        {
            put(5, 0x3ff);
            put(6, 0xfff);
            put(7, 0x3fff);
            put(8, 0xffff);
            put(10, 0xfffff);
            put(11, 0x3fffff);
            put(12, 0xffffff);
            put(13, 0x3ffffff);
            put(14, 0xfffffff);
        }

    };

    public boolean isInUHS(int pmerDecimal) {
        int pmerDecimalDiv8 = pmerDecimal >> 3;
        int pmerDecimalMod8 = pmerDecimal & 0b111;
        if (((this.uhs_bits[pmerDecimalDiv8] >> (pmerDecimalMod8)) & 1) == 1) {
            return true;
        }
        return false;
    }

    public boolean isInUHS(char[] a, int from, int to) {
        return isInUHS(stringUtils.getDecimal(a, from, to));
    }

}
