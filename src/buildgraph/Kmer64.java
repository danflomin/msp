package buildgraph;

public class Kmer64 extends Object {

    public long high;
    public long low;

    private final static char[] baseDic = {'A', 'C', 'G', 'T'};
    private final static int[] intDic = {0, -1, 1, -1, -1, -1, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 3};

    private final int base2int(char base) {
        return intDic[base - 'A'];
    }


    public Kmer64(char[] str, int start, int end, boolean rev) {

        this.high = this.low = 0;

        int len = end - start;

        if (!rev) {
            if (len <= 32) {
                for (int i = start; i <= end - 1; i++) {
                    this.low = (this.low << 2) + base2int(str[i]);
                }
            } else {
                for (int i = end - 32; i <= end - 1; i++) {
                    this.low = (this.low << 2) + base2int(str[i]);
                }

                for (int i = start; i <= end - 33; i++) {
                    this.high = (this.high << 2) + base2int(str[i]);
                }
            }
        } else {
            if (len <= 32) {
                for (int i = end - 1; i >= start; i--) {
                    this.low = (this.low << 2) + 3 ^ base2int(str[i]);
                }
            } else {
                for (int i = start + 31; i >= start; i--) {
                    this.low = (this.low << 2) + 3 ^ base2int(str[i]);
                }

                for (int i = end - 1; i >= start + 32; i--) {
                    this.high = (this.high << 2) + 3 ^ base2int(str[i]);
                }
            }
        }

    }

    public Kmer64(long low, long high) {
        this.low = low;
        this.high = high;
    }

    @Override
    public boolean equals(Object another) {
        Kmer64 k = (Kmer64) another;
        if (this.high == k.high && this.low == k.low)
            return true;
        else
            return false;
    }

    @Override
    public int hashCode() {
        return (int) ((low ^ (low >>> 32)) ^ (high ^ (high >>> 32)));
    }


    public String toString() {
        return this.high + "," + this.low;
    }
}
	
