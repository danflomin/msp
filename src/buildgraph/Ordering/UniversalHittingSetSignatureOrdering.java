package buildgraph.Ordering;

public class UniversalHittingSetSignatureOrdering extends UniversalHittingSetXorOrdering {

    private int minDecimalForNotStartingWithACA;
    private int maxDecimalForNotEndingWithTGT;

    public UniversalHittingSetSignatureOrdering(int xor, int pivotLen) {
        super(xor, pivotLen);
        setBoundaries();
    }


    @Override
    public int strcmp(int x, int y) {
        int baseCompareValue = strcmpBase(x, y);
        if (baseCompareValue != BOTH_IN_UHS) {
            return baseCompareValue;
        }

        // from down here - both in UHS

        if (x < minDecimalForNotStartingWithACA || x > maxDecimalForNotEndingWithTGT) {
            if (maxDecimalForNotEndingWithTGT >= y && y >= minDecimalForNotStartingWithACA) {
                return 1;
            }
        } else if (y < minDecimalForNotStartingWithACA || y > maxDecimalForNotEndingWithTGT) {
            if (maxDecimalForNotEndingWithTGT >= x && x >= minDecimalForNotStartingWithACA) {
                return -1;
            }
        }

        if ((x ^ xor) < (y ^ xor))
            return -1;
        else
            return 1;

    }

    private void setBoundaries() {
        char[] ACA = new char[pivotLen];
        ACA[0] = 'A';
        ACA[1] = 'C';
        ACA[2] = 'A';
        for (int i = 3; i < pivotLen; i++) {
            ACA[i] = 'T';
        }
        int maxValueOfACA = stringUtils.getDecimal(ACA, 0, pivotLen);
        minDecimalForNotStartingWithACA = maxValueOfACA + 1;
        maxDecimalForNotEndingWithTGT = (((int) Math.pow(4, pivotLen)) - 1) - maxValueOfACA - 1;
    }

}
