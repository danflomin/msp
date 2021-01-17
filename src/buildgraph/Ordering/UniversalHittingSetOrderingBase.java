package buildgraph.Ordering;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public abstract class UniversalHittingSetOrderingBase implements IOrdering{

    protected byte[] uhsBits;

    public UniversalHittingSetOrderingBase(int pivotLen){
        try {
            uhsBits = uhsBitSet(pivotLen);
        }
        catch (Exception e) {}
    }




    protected boolean isInUHS(int x){
        int xdiv8 = x >> 3; int xmod8 = x & 0b111;
        if(((this.uhsBits[xdiv8] >> (xmod8)) & 1) == 1){
            return true;
        }
        return false;
    }


    @Override
    public int strcmp(int x, int y){
        if(x == y)
            return 0;

        int xdiv8 = x >> 3; int xmod8 = x & 0b111;
        int ydiv8 = y >> 3; int ymod8 = y & 0b111;
        boolean xInUHS = isInUHS(x);
        boolean yInUHS = isInUHS(y);
        if(xInUHS && !yInUHS){
            return -1;
        }
        else if (!xInUHS && yInUHS){
            return 1;
        }
        else { // both in UHS

        }

        if ((((this.uhsBits[xdiv8] >> (xmod8)) & 1) ^ ((this.uhsBits[ydiv8] >> (ymod8)) & 1)) == 0) {
            if((x ^ xor) < (y ^ xor))
                return -1;
            else if((x ^ xor) > (y ^ xor))
                return 1;
        }


    }

    @Override
    public int strcmp(char[] a, char[] b, int froma, int fromb, int len){
        int x = GetDecimal(a, froma, froma+pivotLen);
        int y = GetDecimal(b, fromb, fromb+pivotLen);
        return strcmp(x,y);
    }

    private  byte[] uhsBitSet(int pivotLen) throws IOException {
        int n = (int) Math.pow(4, pivotLen) / 8;
        int i = 0;
        byte [] bits = new byte[n];

        String DocksFile = "res_" + pivotLen + ".txt";
        FileReader frG = new FileReader(DocksFile);
        int count=0;

        BufferedReader reader;
        try {
            reader = new BufferedReader(frG);
            String line;
            while ((line = reader.readLine()) != null) {
                i = GetDecimal(line.toCharArray(), 0, pivotLen);
                bits[i / 8] |= 1 << (i % 8);
                count++;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(count);
        frG.close();
        return bits;
    }
}
