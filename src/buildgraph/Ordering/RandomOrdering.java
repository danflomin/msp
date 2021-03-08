package buildgraph.Ordering;

import buildgraph.StringUtils;

import java.io.IOException;

public class RandomOrdering implements IOrdering {

    protected StringUtils stringUtils;
    private int pivotLen;
    private int xor;

    public RandomOrdering(int pivotLen, int xor) {
        this.pivotLen = pivotLen;
        this.xor = xor;
        stringUtils = new StringUtils();
    }

    @Override
    public int findSmallest(char[] a, int from, int to) throws IOException {
        int min_pos = from;
        for (int i = from + 1; i <= to - pivotLen; i++) {
            if (strcmp(a, a, min_pos, i, pivotLen) > 0)
                min_pos = i;
        }
        return min_pos;
    }

    @Override
    public int strcmp(char[] a, char[] b, int froma, int fromb, int len) throws IOException {
        int x = stringUtils.getDecimal(a, froma, froma + pivotLen);
        int y = stringUtils.getDecimal(b, fromb, fromb + pivotLen);
        if ((x ^ xor) < (y ^ xor))
            return -1;
        else if ((x ^ xor) > (y ^ xor))
            return 1;
        return 0;
    }
}