package buildgraph.Ordering;

import buildgraph.StringUtils;

import java.io.IOException;

public class UniversalHittingSetXorOrdering extends UniversalHittingSetOrderingBase{
    protected int xor;

    public UniversalHittingSetXorOrdering(int xor, int pivotLen) throws IOException {
        super(pivotLen);
        this.xor = xor;
    }

    @Override
    public int strcmp(char[] a, char[] b, int froma, int fromb, int len) {
        int x = stringUtils.getDecimal(a, froma, froma + pivotLen);
        int y = stringUtils.getDecimal(b, fromb, fromb + pivotLen);
        return strcmp(x, y);
    }

    @Override
    public int findSmallest(char[] a, int from, int to) {
        int min_pos = from;
        int j = stringUtils.getDecimal(a, min_pos, min_pos + pivotLen);
        int prev = j;
        for (int i = from + 1; i <= to - pivotLen; i++) {
            j = ((j * 4) ^ (StringUtils.valTable[a[i + pivotLen - 1] - 'A'])) & pivotLengthToHexRepresentation.get(pivotLen);
            if (((uhsBits[j >> 3] >> (j & 0b111)) & 1) == 1) {
                if (strcmp(prev, j) > 0) {
                    min_pos = i;
                    prev = j;
                }

            }
        }
        return min_pos;
    }

    private int strcmp(int x, int y) {
        int baseCompareValue = strcmpBase(x,y);
        if(baseCompareValue != BOTH_IN_UHS){
            return baseCompareValue;
        }
        else { // both in UHS
            if((x ^ xor) < (y ^ xor))
                return -1;
            else
                return 1;
        }

    }


}
