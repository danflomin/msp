package dumbo.Ordering.Standard;


import dumbo.Ordering.OrderingBase;

public class LexicographicOrdering extends OrderingBase {

    public LexicographicOrdering(int pivotLength) {
        super(pivotLength);
    }


    @Override
    public int compareMmer(int x, int y) {
        return Integer.compare(stringUtils.getNormalizedValue(x, pivotLength), stringUtils.getNormalizedValue(y, pivotLength));
    }


}
