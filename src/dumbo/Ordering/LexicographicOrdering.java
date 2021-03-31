package dumbo.Ordering;

public class LexicographicOrdering implements IOrdering {

    protected int pivotLen;

    public LexicographicOrdering(int pivotLen) {

        this.pivotLen = pivotLen;
    }


    @Override
    public int findSmallest(char[] a, int from, int to) {

        int min_pos = from;

        for (int i = from + 1; i <= to - pivotLen; i++) {
            if (strcmp(a, a, min_pos, i, pivotLen) > 0)
                min_pos = i;
        }

        return min_pos;
    }

    @Override
    public int strcmp(char[] a, char[] b, int froma, int fromb, int len) {
        for (int i = 0; i < len; i++) {
            if (a[froma + i] < b[fromb + i])
                return -1;
            else if (a[froma + i] > b[fromb + i])
                return 1;
        }
        return 0;
    }
}
