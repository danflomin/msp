package buildgraph.Ordering;

public abstract class UniversalHittingSetOrderingBase implements IOrdering{
    public UniversalHittingSetOrderingBase(){

    }


    protected boolean isInUHS(int x){
        int xdiv8 = x >> 3; int xmod8 = x & 0b111;
        if(((this.uhs_bits[xdiv8] >> (xmod8)) & 1)){

        }
    }


    @Override
    public int strcmp(int x, int y){
        int xdiv8 = x >> 3; int xmod8 = x & 0b111;
        int ydiv8 = y >> 3; int ymod8 = y & 0b111;
        if ((((this.uhs_bits[xdiv8] >> (xmod8)) & 1) ^ ((this.uhs_bits[ydiv8] >> (ymod8)) & 1)) == 0) {
            if((x ^ xor) < (y ^ xor))
                return -1;
            else if((x ^ xor) > (y ^ xor))
                return 1;
        }

        if (((this.uhs_bits[xdiv8] >> (xmod8)) & 1) > ((this.uhs_bits[ydiv8] >> (ymod8)) & 1))
            return -1;
        if (((this.uhs_bits[xdiv8] >> (xmod8)) & 1) < ((this.uhs_bits[ydiv8] >> (ymod8)) & 1))
            return 1;

        return 0;
    }

    @Override
    public int strcmp(char[] a, char[] b, int froma, int fromb, int len){
        int x = GetDecimal(a, froma, froma+pivotLen);
        int y = GetDecimal(b, fromb, fromb+pivotLen);
        int xdiv8 = x >> 3; int xmod8 = x & 0b111;
        int ydiv8 = y >> 3; int ymod8 = y & 0b111;
        if ((((this.uhs_bits[xdiv8] >> (xmod8)) & 1) ^ ((this.uhs_bits[ydiv8] >> (ymod8)) & 1)) == 0) {
            if((x ^ xor) < (y ^ xor))
                return -1;
            else //if((x ^ 11101101) > (y ^ 11101101))
                return 1;
        }

        if (((this.uhs_bits[xdiv8] >> (xmod8)) & 1) > ((this.uhs_bits[ydiv8] >> (ymod8)) & 1))
            return -1;
        if (((this.uhs_bits[xdiv8] >> (xmod8)) & 1) < ((this.uhs_bits[ydiv8] >> (ymod8)) & 1))
            return 1;

        return 0;
    }
}
