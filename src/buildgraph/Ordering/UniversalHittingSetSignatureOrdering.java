package buildgraph.Ordering;

import buildgraph.StringUtils;

import java.io.IOException;

public class UniversalHittingSetSignatureOrdering extends UniversalHittingSetXorOrdering {
    private SignatureUtils signatureUtils;
    public UniversalHittingSetSignatureOrdering(int xor, int pivotLen) throws IOException {
        super(xor, pivotLen);
        signatureUtils = new SignatureUtils();
    }

    @Override
    public int strcmp(char[] a, char[] b, int froma, int fromb, int len) {
        boolean aAllowed = signatureUtils.isAllowed(a, froma, froma + len);
        boolean bAllowed = signatureUtils.isAllowed(b, fromb, fromb + len);

        int x = stringUtils.getDecimal(a, froma, froma + pivotLen);
        int y = stringUtils.getDecimal(b, fromb, fromb + pivotLen);
        return strcmpSignature(x, y, aAllowed, bAllowed);

    }

    @Override
    public int findSmallest(char[] a, int from, int to) {
        int min_pos = from;
        int j = stringUtils.getDecimal(a, min_pos, min_pos + pivotLen);
        int prev = j;
        boolean jAllowed, prevAllowed = signatureUtils.isAllowed(a, min_pos, min_pos+pivotLen);
        for (int i = from + 1; i <= to - pivotLen; i++) {
            j = ((j * 4) ^ (StringUtils.valTable[a[i + pivotLen - 1] - 'A'])) & pivotLengthToHexRepresentation.get(pivotLen);
            jAllowed = signatureUtils.isAllowed(a, i, i+pivotLen);
            if (((uhsBits[j >> 3] >> (j & 0b111)) & 1) == 1) {
                if (strcmpSignature(prev, j, prevAllowed, jAllowed) > 0) {
                    min_pos = i;
                    prev = j;
                }

            }
            prevAllowed = jAllowed;
        }
        return min_pos;
    }

    private int strcmpSignature(int x, int y, boolean xAllowed, boolean yAllowed) {
        int baseCompareValue = strcmpBase(x, y);
        if (baseCompareValue != BOTH_IN_UHS) {
            return baseCompareValue;
        }

        // from down here - both in UHS

        if (!xAllowed && yAllowed) {
            return 1;
        } else if (!yAllowed && xAllowed) {
            return -1;
        }

        // both allowed or both not allowed
        if ((x ^ xor) < (y ^ xor))
            return -1;
        else
            return 1;

    }



}
