package buildgraph.Ordering;

public class UniversalHittingSetXorOrdering extends UniversalHittingSetOrderingBase{
    protected int xor;

    public UniversalHittingSetXorOrdering(int xor, int pivotLen){
        super(pivotLen);
        this.xor = xor;
    }
    @Override
    public int strcmp(int x, int y) {
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
