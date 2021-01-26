package buildgraph.Ordering.UHS;

import buildgraph.Ordering.SignatureUtils;
import buildgraph.StringUtils;

import java.io.IOException;

public class UHSSignatureOrdering extends UHSOrderingBase {
    private SignatureUtils signatureUtils;
    protected boolean useSignature;
    private boolean useCache;
    protected int xor;



    public UHSSignatureOrdering(int xor, int pivotLen, boolean useSignature, boolean useCache) throws IOException {
        super(pivotLen);
        this.xor = xor;
        this.useSignature = useSignature;
        this.useCache = useCache;
        signatureUtils = new SignatureUtils(pivotLen);
    }

    public UHSSignatureOrdering(int pivotLen, boolean useSignature, boolean useCache) throws IOException {
        this(0, pivotLen, useSignature, useCache);
    }

    @Override
    public int strcmp(char[] a, char[] b, int froma, int fromb, int len) throws IOException {
        int x = stringUtils.getDecimal(a, froma, froma + pivotLen);
        int y = stringUtils.getDecimal(b, fromb, fromb + pivotLen);

        if(x==y) return 0;

        if(isRankInit){
            if (rankOfPmer[x] < rankOfPmer[y]){
                return -1;
            }
            return 1;
        }

        boolean aAllowed = true, bAllowed = true;
        if(useSignature){
            aAllowed = signatureUtils.isAllowed(a, froma, x);
            bAllowed = signatureUtils.isAllowed(b, fromb, y);
        }

        return strcmpSignature(x, y, aAllowed, bAllowed);

    }

    @Override
    public int findSmallest(char[] a, int from, int to) throws IOException {
        int min_pos = from;
        int j = stringUtils.getDecimal(a, min_pos, min_pos + pivotLen);
        int prev = j;
        boolean prevAllowed = signatureUtils.isAllowed(a, min_pos, prev), jAllowed = true;
        int hexRepresentation = pivotLengthToHexRepresentation.get(pivotLen);
        for (int i = from + 1; i <= to - pivotLen; i++) {
            j = ((j * 4) ^ (StringUtils.valTable[a[i + pivotLen - 1] - 'A'])) & hexRepresentation;

            if(useSignature)
                jAllowed = signatureUtils.isAllowed(a, i, j);

            if (isInUHS(j)) {
                if (strcmpSignature(prev, j, prevAllowed, jAllowed) > 0) {
                    min_pos = i;
                    prev = j;
                }

            }
            prevAllowed = jAllowed;
        }
        return min_pos;
    }

    protected int strcmpSignature(int x, int y, boolean xAllowed, boolean yAllowed) throws IOException {
        int baseCompareValue = strcmpBase(x, y);
        if (baseCompareValue != BOTH_IN_UHS) {
            return baseCompareValue;
        }

        // from down here - both in UHS
        if(useSignature){
            if (!xAllowed && yAllowed) {
                return 1;
            } else if (!yAllowed && xAllowed) {
                return -1;
            }
        }


        // both allowed or both not allowed
        if ((x ^ xor) < (y ^ xor))
            return -1;
        else
            return 1;

    }



}
