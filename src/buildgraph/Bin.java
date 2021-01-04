package buildgraph;

import java.util.HashSet;
import java.util.LinkedList;

public class Bin extends Object {

    private long maxCapacity;
    private long capacityLeft;
    private HashSet<Long> pmersInBin;

    public Bin(long maxCapacity) {
        this.maxCapacity = maxCapacity;
        this.capacityLeft = this.maxCapacity;
        pmersInBin = new HashSet<>();
    }

    public boolean TryAccomodate(long pmerID, long size) {
        if (size <= capacityLeft) {
            capacityLeft -= size;
            pmersInBin.add(pmerID);
            return true;
        }
        return false;
    }

    public Long[] getPmersInBin(){
        Long[] pmersInBinArray = new Long[pmersInBin.size()];
        pmersInBin.toArray(pmersInBinArray);
        return pmersInBinArray;

    }

}