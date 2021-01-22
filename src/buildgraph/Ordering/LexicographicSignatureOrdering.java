package buildgraph.Ordering;

import buildgraph.StringUtils;

import java.io.IOException;

public class LexicographicSignatureOrdering extends LexicographicOrdering {
    private SignatureUtils signatureUtils;
    private StringUtils stringUtils;
    public LexicographicSignatureOrdering(int pivotLen) throws IOException {
        super(pivotLen);
        signatureUtils = new SignatureUtils(pivotLen);
        stringUtils = new StringUtils();
    }

    @Override
    public int strcmp(char[] a, char[] b, int froma, int fromb, int len) {
//        boolean aAllowed = signatureUtils.isAllowed(a, froma, froma + len);
//        boolean bAllowed = signatureUtils.isAllowed(b, fromb, fromb + len);
        int x = stringUtils.getDecimal(a, froma, froma + pivotLen);
        int y = stringUtils.getDecimal(b, fromb, fromb + pivotLen);
        boolean aAllowed = signatureUtils.isAllowed(a, froma, x);
        boolean bAllowed = signatureUtils.isAllowed(b, fromb, y);

        if (!aAllowed && bAllowed) {
            return 1;
        } else if (!bAllowed && aAllowed) {
            return -1;
        }

        for (int i = 0; i < len; i++) {
            if (a[froma + i] < b[fromb + i])
                return -1;
            else if (a[froma + i] > b[fromb + i])
                return 1;
        }
        return 0;
    }
}
