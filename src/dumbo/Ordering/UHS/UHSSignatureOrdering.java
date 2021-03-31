package dumbo.Ordering.UHS;

import dumbo.Ordering.SignatureUtils;
import dumbo.StringUtils;

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



    @Override
    public int strcmp(char[] a, char[] b, int froma, int fromb, int len) throws IOException {
        if(!isRankInit) throw new IOException("rank not initialized yet");

        int x = stringUtils.getDecimal(a, froma, froma + pivotLen);
        int y = stringUtils.getDecimal(b, fromb, fromb + pivotLen);

        if (x == y) return 0;

        // isRankInit = true here
        if (rankOfPmer[x] < rankOfPmer[y]) {
            return -1;
        }
        return 1;
    }

    public int strcmp(int x, int y) throws IOException {
        if(!isRankInit) throw new IOException("rank not initialized yet");

        if (x == y) return 0;

        // isRankInit = true here
        if (rankOfPmer[x] < rankOfPmer[y]) {
            return -1;
        }
        return 1;
    }

    @Override
    public int findSmallest(char[] a, int from, int to) throws IOException {
        int min_pos = from;
        int j = stringUtils.getDecimal(a, min_pos, min_pos + pivotLen);
        int prev = j;
        int hexRepresentation = pivotLengthToHexRepresentation.get(pivotLen);
        for (int i = from + 1; i <= to - pivotLen; i++) {
            j = ((j * 4) ^ (StringUtils.valTable[a[i + pivotLen - 1] - 'A'])) & hexRepresentation;

            if (isInUHS(j)) {
                if(rankOfPmer[j] < rankOfPmer[prev]){
                    min_pos = i;
                    prev = j;
                }

            }
        }
        return min_pos;
    }

    protected int calculateStrcmp(char[] a, char[] b, int froma, int fromb, int len) throws IOException {
        int x = stringUtils.getDecimal(a, froma, froma + pivotLen);
        int y = stringUtils.getDecimal(b, fromb, fromb + pivotLen);

        if (x == y) return 0;

        boolean aAllowed = true, bAllowed = true;
        if (useSignature) {
            aAllowed = signatureUtils.isAllowed(a, froma, x);
            bAllowed = signatureUtils.isAllowed(b, fromb, y);
        }

        return strcmpSignature(x, y, aAllowed, bAllowed);
    }

    protected int calculateStrcmp(int x, int y) throws IOException {
        if (x == y) return 0;

        boolean aAllowed = true, bAllowed = true;
        if (useSignature) {
            aAllowed = signatureUtils.isAllowed(x);
            bAllowed = signatureUtils.isAllowed(y);
        }

        return strcmpSignature(x, y, aAllowed, bAllowed);
    }



    protected int strcmpSignature(int x, int y, boolean xAllowed, boolean yAllowed) throws IOException {
        int baseCompareValue = strcmpBase(x, y);
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
        if ((x ^ xor) < (y ^ xor))
            return -1;
        else
            return 1;

    }
}
