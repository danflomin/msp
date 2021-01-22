package buildgraph.Ordering;

import java.util.HashMap;

public class SignatureUtils {

    private int len;
    protected Boolean[] isPmerAllowed;

    public SignatureUtils(int len){
        this.len = len;
        isPmerAllowed = new Boolean[(int)Math.pow(4, len)];
    }

    public boolean isAllowed(char[] a, int from, int aDecimal) {
        if(isPmerAllowed[aDecimal] != null){
            return isPmerAllowed[aDecimal];
        }

        int lastIndex = from + len - 1;
        if (a[from] == 'A' && a[from + 2] == 'A') {
            if (a[from + 1] <= 'C') { // C or A
                isPmerAllowed[aDecimal] = false;
                return false;
            }
        } else if (a[lastIndex] == 'T' && a[lastIndex - 2] == 'T') {
            if (a[lastIndex - 1] >='G') { // G or T
                isPmerAllowed[aDecimal] = false;
                return false;
            }
        }

        for (int i = from + 2; i < lastIndex; i++) {
            if (a[i] == 'A' && a[i + 1] == 'A') {
                isPmerAllowed[aDecimal] = false;
                return false;
            }
        }
        isPmerAllowed[aDecimal] = true;
        return true;
    }

}
