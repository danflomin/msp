package buildgraph.Ordering;

public class SignatureUtils {

    public boolean isAllowed(char[] a, int from, int to) {
        if (a[from] == 'A' && a[from + 2] == 'A') {
            if (a[from + 1] == 'C' || a[from + 1] == 'A') {
                return false;
            }
        } else if (a[to - 1] == 'T' && a[to - 3] == 'T') {
            if (a[to - 2] == 'T' || a[to - 2] == 'G') {
                return false;
            }
        }
        for (int i = from + 2; i < to - 1; i++) {
            if (a[i] == 'A' && a[i + 1] == 'A') {
                return false;
            }
        }
        return true;
    }

}
