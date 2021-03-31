package dumbo.Ordering;

public interface IOrderingPP extends IOrdering {
    long getRank(int mmer);
    int strcmp(int x, int y);
}
