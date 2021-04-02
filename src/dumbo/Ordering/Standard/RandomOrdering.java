package dumbo.Ordering.Standard;

import dumbo.Ordering.OrderingBase;

public class RandomOrdering extends OrderingBase {
    private int xor;

    public RandomOrdering(int pivotLen, int xor) {
        super(pivotLen);
        this.xor = xor;
    }

    @Override
    public int compareMmer(int x, int y) {
        if ((x ^ xor) < (y ^ xor))
            return -1;
        else if ((x ^ xor) > (y ^ xor))
            return 1;
        return 0;
    }
}