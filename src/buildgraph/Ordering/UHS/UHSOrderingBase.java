package buildgraph.Ordering.UHS;

import buildgraph.Ordering.IOrdering;
import buildgraph.StringUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

public abstract class UHSOrderingBase implements IOrdering {

    protected byte[] uhsBits;
    protected StringUtils stringUtils;

    protected static final int BOTH_IN_UHS = 824;
    protected int pivotLen;

    protected int[] rankOfPmer;
    protected  boolean isRankInit;


    protected static HashMap<Integer, Integer> pivotLengthToHexRepresentation = new HashMap<Integer, Integer>(){{
        put(8, 0x0000ffff);
        put(10, 0x000fffff);
        put(12, 0x00ffffff);
    }

    };

    public UHSOrderingBase(int pivotLen) throws IOException{
        this.pivotLen = pivotLen;
        stringUtils = new StringUtils();
        uhsBits = uhsBitSet(pivotLen);
        rankOfPmer = new int[(int)Math.pow(4,pivotLen)];
        Arrays.fill(rankOfPmer, Integer.MAX_VALUE);
        isRankInit = false;
        initRank();
        isRankInit = true;
    }


    public boolean isInUHS(int pmerDecimal) {
        int pmerDecimalDiv8 = pmerDecimal >> 3;
        int pmerDecimalMod8 = pmerDecimal & 0b111;
        if (((this.uhsBits[pmerDecimalDiv8] >> (pmerDecimalMod8)) & 1) == 1) {
            return true;
        }
        return false;
    }

    public boolean isInUHS(char[] a, int from, int to) {
        return isInUHS(stringUtils.getDecimal(a, from, to));
    }

    protected int strcmpBase(int x, int y) {
        if (x == y)
            return 0;

        boolean xInUHS = isInUHS(x);
        boolean yInUHS = isInUHS(y);
        if (xInUHS && !yInUHS) {
            return -1;
        } else if (!xInUHS && yInUHS) {
            return 1;
        }
        return BOTH_IN_UHS;
    }

    private byte[] uhsBitSet(int pivotLen) throws IOException {
        int n = (int) Math.pow(4, pivotLen) / 8;
        int i = 0;
        byte[] bits = new byte[n];

        String DocksFile = "res_" + pivotLen + ".txt";
        FileReader frG = new FileReader(DocksFile);
        int count = 0;

        BufferedReader reader;
        try {
            reader = new BufferedReader(frG);
            String line;
            while ((line = reader.readLine()) != null) {
                i = stringUtils.getDecimal(line.toCharArray(), 0, pivotLen);
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

    private void initRank(){
        System.out.println("start init rank");
        HashSet<char[]> pmers = getPmersInUHS();
        char[][] pmersArr = new char[pmers.size()][pivotLen];
        pmers.toArray(pmersArr);
        Arrays.stream(pmersArr).sorted((o1, o2) -> {
            try {
                return strcmp(o1, o2, 0, 0, pivotLen);
            } catch (IOException e) {
                    e.printStackTrace();
            }
            return 0;
        });
        for(int i = 0; i< pmersArr.length; i++){
            rankOfPmer[stringUtils.getDecimal(pmersArr[i], 0, pivotLen)] = i;
        }
        System.out.println("finish init rank");
    }

    private HashSet<char[]> getPmersInUHS(){
        HashSet<char[]> pmers = new HashSet<>();
        StringBuilder sb = new StringBuilder(pivotLen);
        for(int i = 0; i<pivotLen; i++) sb.append('A');
        generate(pmers, sb, 0);
        return pmers;

    }

    private void generate(HashSet<char[]> pmers, StringBuilder sb, int n){
        char[] alphabet = {'A', 'C', 'G', 'T'};
        if (n == sb.capacity()) {
            char[] pmer = sb.toString().toCharArray();
            if(isInUHS(pmer, 0, pivotLen)) {
                pmers.add(pmer);
            }
            return;
        }
        for (char letter : alphabet) {
            sb.setCharAt(n, letter);
            generate(pmers, sb, n + 1);
        }
    }
}
