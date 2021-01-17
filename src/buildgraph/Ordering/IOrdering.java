package buildgraph.Ordering;

public interface IOrdering {
    int strcmp(int x, int y);
    int strcmp(char[] a, char[] b, int froma, int fromb, int len);
}
