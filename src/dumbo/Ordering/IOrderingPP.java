package dumbo.Ordering;

import java.io.IOException;

public interface IOrderingPP {
    int strcmp(int x, int y);
    int findSmallest(char[] a, int from, int to) throws IOException;
}
