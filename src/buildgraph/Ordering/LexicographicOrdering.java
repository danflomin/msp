package buildgraph.Ordering;

public class LexicographicOrdering implements IOrdering{
    @Override
    public int strcmp(int x, int y) {
        if(x<y){
            return -1;
        }
        else if(x>y){
            return 1;
        }
        return 0;
    }

    @Override
    public int strcmp(char[] a, char[] b, int froma, int fromb, int len){
        for(int i = 0; i < len; i++){
            if(a[froma+i] < b[fromb+i])
                return -1;
            else if(a[froma+i] > b[fromb+i])
                return 1;
        }
        return 0;
    }
}
