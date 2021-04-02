package dumbo.Ordering;

import dumbo.StringUtils;

import java.io.IOException;

public class LexicographicSignatureOrdering extends LexicographicOrdering {
    private SignatureUtils signatureUtils;

    public LexicographicSignatureOrdering(int pivotLen) throws IOException {
        super(pivotLen);
        signatureUtils = new SignatureUtils(pivotLen);
    }

    @Override
    public int strcmp(int x, int y) {
        boolean aAllowed = signatureUtils.isAllowed(x);
        boolean bAllowed = signatureUtils.isAllowed(y);

        if (!aAllowed && bAllowed) {
            return 1;
        } else if (!bAllowed && aAllowed) {
            return -1;
        }

        return Integer.compare(stringUtils.getNormalizedValue(x, pivotLength), stringUtils.getNormalizedValue(y, pivotLength));
    }
}
