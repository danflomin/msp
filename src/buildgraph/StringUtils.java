package buildgraph;

public class StringUtils {

    private static int[] valTable = new int[]{0,-1,1,-1,-1,-1,2,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,3};
    private static char[] twinTable = new char[]{'T','0','G','0','0','0','C','0','0','0','0','0','0','0','0','0','0','0','0','A'};

    public int getDecimal(char[] a, int from, int to){

        int val=0;

        for(int i=from; i<to; i++){
            val = val<<2;
            val += valTable[a[i]-'A'];
        }

        return val;
    }

    public boolean isReadLegal(char[] line){
        int Len = line.length;
        for(int i=0; i<Len; i++){
            if(line[i]!='A' && line[i]!='C' && line[i]!='G' && line[i]!='T')
                return false;
        }
        return true;
    }

    public char[] getReversedRead(char[] lineCharArray){
        int len = lineCharArray.length;
        char[] revCharArray = new char[len];
        for(int i=0; i<len; i++){
            revCharArray[i] = twinTable[lineCharArray[len-1-i]-'A'];
        }
        return revCharArray;
    }
}
