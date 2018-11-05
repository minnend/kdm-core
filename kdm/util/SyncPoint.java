package kdm.util;

import java.util.*;

public class SyncPoint
{
    public int iData1, iVideo1;
    public int iData2, iVideo2;
    public double vidPerData; 
    
    public SyncPoint(Map.Entry<Integer,Integer> a,
                     Map.Entry<Integer,Integer> b)
    {
        assert (a!=null && b!=null);
        iData1 = a.getKey();
        iVideo1 = a.getValue();
        iData2 = b.getKey();
        iVideo2 = b.getValue();
        vidPerData = (double)(iVideo2-iVideo1+1) / (double)(iData2-iData1+1);
    }

    public SyncPoint(Map.Entry<Integer,Integer> a, double _vidPerData)
    {
        assert (a!=null);
        iData1 = a.getKey();
        iVideo1 = a.getValue();
        iData2 = iVideo2 = -1;
        vidPerData = _vidPerData;
    }

    public int getVideo(int iData)
    {
        if (iData2<0)
        {
            return (int)Math.round(iVideo1 + (iData-iData1) * vidPerData);
        }
        else{
            assert(iData <= iData2);
            int dataRange = iData2-iData1+1;
            int videoRange = iVideo2-iVideo1+1;
            double f = (double)(iData-iData1) / (double)dataRange;
            return (int)Math.round(iVideo1 + f * videoRange);
        }
    }

    public int getData(int iVideo)
    {
        assert(iVideo >= iVideo1);
        if (iVideo2<0)
        {
            return (int)Math.round(iData1 + (iVideo-iVideo1) / vidPerData);
        }
        else{
            assert(iVideo <= iVideo2);
            int dataRange = iData2-iData1+1;
            int videoRange = iVideo2-iVideo1+1;
            double f = (double)(iVideo-iVideo1) / (double)videoRange;
            return (int)Math.round(iData1 + f * dataRange);
        }
    }


    /**
     * Returns the sync entry that maps to the given index.  If an
     * exact match can't be found, the next smallest entry is
     * returned.
     */
    public static SyncPoint getSyncData(int index, TreeMap<Integer,Integer> syncs, double _vidPerData)
    {
        assert syncs.size() > 0;

        Set<Map.Entry<Integer,Integer>> entries = syncs.entrySet();
        Object[] v = entries.toArray();

        // handle illegal request by returning first sync point
        if (index < 0) return new SyncPoint((Map.Entry<Integer,Integer>)v[0], _vidPerData);

        int a = -1;
        int b = v.length;
        while(b-a > 1)
        {
            int m = (a+b)/2;
            if (index <= ((Map.Entry<Integer,Integer>)v[m]).getKey()) b = m;
            else a = m;
        }
        if (b<v.length && index == ((Map.Entry<Integer,Integer>)v[b]).getKey())
        {
            if (b+1 >= v.length) return new SyncPoint((Map.Entry<Integer,Integer>)v[b], _vidPerData);
            else return new SyncPoint((Map.Entry<Integer,Integer>)v[b],
                                      (Map.Entry<Integer,Integer>)v[b+1]);
        }
        else{
            if (a+1 >= v.length) return new SyncPoint((Map.Entry<Integer,Integer>)v[a], _vidPerData);
            else if (a<0) return new SyncPoint((Map.Entry<Integer,Integer>)v[0],
                                               (Map.Entry<Integer,Integer>)v[1]);
            else return new SyncPoint((Map.Entry<Integer,Integer>)v[a],
                                     (Map.Entry<Integer,Integer>)v[a+1]);
        }
    }

    /**
     * Returns the sync entry that maps to the given index frame.
     * If an exact match can't be found, the next smallest entry is
     * returned.
     */
    public static SyncPoint getSyncVideo(int index, TreeMap<Integer,Integer> syncs, double _vidPerData)
    {
        assert syncs.size() > 0;

        Set<Map.Entry<Integer,Integer>> entries = syncs.entrySet();
        Object[] v = entries.toArray();

        // handle illegal request by returning first sync point
        if (index < 0) return new SyncPoint((Map.Entry<Integer,Integer>)v[0], _vidPerData);

        int a = -1;
        int b = v.length;
        while(b-a > 1)
        {
            int m = (a+b)/2;
            if (index <= ((Map.Entry<Integer,Integer>)v[m]).getValue()) b = m;
            else a = m;
        }
        if (b<v.length && index == ((Map.Entry<Integer,Integer>)v[b]).getValue())
        {
            if (b+1 >= v.length) return new SyncPoint((Map.Entry<Integer,Integer>)v[b], _vidPerData);
            else return new SyncPoint((Map.Entry<Integer,Integer>)v[b],
                                      (Map.Entry<Integer,Integer>)v[b+1]);
        }
        else{
            if (a+1 >= v.length) return new SyncPoint((Map.Entry<Integer,Integer>)v[a], _vidPerData);
            else if (a<0) return new SyncPoint((Map.Entry<Integer,Integer>)v[0],
                                      (Map.Entry<Integer,Integer>)v[1]);
            else return new SyncPoint((Map.Entry<Integer,Integer>)v[a],
                                      (Map.Entry<Integer,Integer>)v[a+1]);
        }
    }

    public String toString()
    {
        return String.format("[Sync1: %d -> %d   Sync2: %d -> %d  (%.3f)]",
                             iData1, iVideo1, iData2, iVideo2, vidPerData);
    }
}
