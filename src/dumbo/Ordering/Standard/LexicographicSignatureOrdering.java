package dumbo.Ordering.Standard;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

public class LexicographicSignatureOrdering extends LexicographicOrdering {
    protected SignatureUtils signatureUtils;

    public LexicographicSignatureOrdering(int pivotLen) throws IOException {
        super(pivotLen);
        signatureUtils = new SignatureUtils(pivotLen);
    }

    @Override
    public void initializeRanks() throws IOException {
        Arrays.fill(mmerRanks, Integer.MAX_VALUE);

        HashSet<Integer> normalizedAllowedMmers = new HashSet<>();
        for (int i = 0; i < numMmers; i++) {
            if (signatureUtils.isAllowed(stringUtils.getNormalizedValue(i, pivotLength)))
                normalizedAllowedMmers.add(stringUtils.getNormalizedValue(i, pivotLength));
        }

        Integer[] mmers = new Integer[normalizedAllowedMmers.size()];
        normalizedAllowedMmers.toArray(mmers);

        Arrays.sort(mmers);

        for (int i = 0; i < mmers.length; i++) {
            mmerRanks[mmers[i]] = i;
        }
        normalize();
        System.out.println("finish init rank");
        isRankInitialized = true;
    }

    @Override
    protected int rawCompareMmer(int x, int y) {
        int a = stringUtils.getNormalizedValue(x, pivotLength);
        int b = stringUtils.getNormalizedValue(y, pivotLength);

        boolean aAllowed = signatureUtils.isAllowed(a);
        boolean bAllowed = signatureUtils.isAllowed(b);

        if (!aAllowed && bAllowed) {
            return 1;
        } else if (!bAllowed && aAllowed) {
            return -1;
        }

        return Integer.compare(a, b);
    }
}
