package dumbo.Ordering;

import dumbo.StringUtils;

public class LexicographicOrdering implements IOrderingPP {

    protected int pivotLength;
    protected StringUtils stringUtils;
    protected int mask;

    public LexicographicOrdering(int pivotLength) {
        this.pivotLength = pivotLength;
        this.stringUtils = new StringUtils();
        this.mask = (int) Math.pow(4, pivotLength) - 1;
    }


    @Override
    public int strcmp(int x, int y) {
        return Integer.compare(stringUtils.getNormalizedValue(x, pivotLength), stringUtils.getNormalizedValue(y, pivotLength));
    }

    @Override
    public int findSmallest(char[] a, int from, int to) {
        int min_pos = from;
        int minValue = stringUtils.getDecimal(a, min_pos, min_pos + pivotLength);
        int currentValue = minValue;
        for (int i = from + 1; i <= to - pivotLength; i++) {
            currentValue = ((currentValue << 2) + StringUtils.valTable[a[i + pivotLength - 1] - 'A']) & mask;
            if (strcmp(minValue, currentValue) > 0) {
                min_pos = i;
                minValue = currentValue;
            }
        }

        return min_pos;
    }

}
