package dumbo.Ordering;

import dumbo.StringUtils;

import java.io.IOException;
import java.util.Arrays;

public abstract class OrderingBase {

    protected int pivotLength;
    protected int numMmers;
    protected int mask;

    protected StringUtils stringUtils;

    public OrderingBase(int pivotLength) {
        this.pivotLength = pivotLength;
        this.numMmers = (int) Math.pow(4, pivotLength);
        this.mask = numMmers - 1;
        this.stringUtils = new StringUtils();
    }


    public abstract int compareMmer(int x, int y);

    public int[] getRanks() {
        Integer[] ranks = new Integer[numMmers];
        for (int i = 0; i < ranks.length; i++) {
            ranks[i] = i;
        }

        Arrays.sort(ranks, this::compareMmer);

        int[] primitveRanks = new int[numMmers];
        for (int i = 0; i < ranks.length; i++) {
            primitveRanks[i] = ranks[i];
        }
        return primitveRanks;
    }

    public int findSmallest(char[] a, int from, int to) throws IOException {
        int min_pos = from;
        int minValue = stringUtils.getDecimal(a, min_pos, min_pos + pivotLength);
        int currentValue = minValue;
        for (int i = from + 1; i <= to - pivotLength; i++) {
            currentValue = ((currentValue << 2) + StringUtils.valTable[a[i + pivotLength - 1] - 'A']) & mask;
            if (compareMmer(minValue, currentValue) > 0) {
                min_pos = i;
                minValue = currentValue;
            }
        }

        return min_pos;
    }
}
