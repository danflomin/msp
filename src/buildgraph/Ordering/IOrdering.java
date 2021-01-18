package buildgraph.Ordering;

import java.io.IOException;

public interface IOrdering {

    int findSmallest(char[] a, int from, int to) throws IOException;
    int strcmp(char[] a, char[] b, int froma, int fromb, int len) throws IOException;
}
