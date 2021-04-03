package dumbo.Ordering.UHS;

import dumbo.Ordering.Standard.SignatureUtils;

import java.io.IOException;

public class UHSSignatureOrdering extends UHSOrderingBase {
    private SignatureUtils signatureUtils;
    protected boolean useSignature;
    protected int xor;


    public UHSSignatureOrdering(int xor, int pivotLen, boolean useSignature) throws IOException {
        super(pivotLen);
        this.xor = xor;
        this.useSignature = useSignature;
        signatureUtils = new SignatureUtils(pivotLen);
    }

    public UHSSignatureOrdering(int pivotLen, boolean useSignature) throws IOException {
        this(0, pivotLen, useSignature);
    }

    @Override
    public void initRank() throws IOException {
        super.initRank();
        isRankInit = true;
    }




    protected int rawCompare(int x, int y) {
        int a = stringUtils.getNormalizedValue(x, pivotLength);
        int b = stringUtils.getNormalizedValue(y, pivotLength);

        if (a == b) return 0;

        boolean aAllowed = true, bAllowed = true;
        if (useSignature) {
            aAllowed = signatureUtils.isAllowed(a);
            bAllowed = signatureUtils.isAllowed(b);
        }

        return rawCompare(a, b, aAllowed, bAllowed);
    }


    protected int rawCompare(int xNormalized, int yNormalized, boolean xAllowed, boolean yAllowed) {
        int baseCompareValue = compareMmerBase(xNormalized, yNormalized);
        if (baseCompareValue != BOTH_IN_UHS && baseCompareValue != BOTH_NOT_IN_UHS) {
            return baseCompareValue;
        }
        // from down here - both in UHS
        if (useSignature) {
            if (!xAllowed && yAllowed) {
                return 1;
            } else if (!yAllowed && xAllowed) {
                return -1;
            }
        }
        // both allowed or both not allowed
        if ((xNormalized ^ xor) < (yNormalized ^ xor))
            return -1;
        else
            return 1;

    }
}
