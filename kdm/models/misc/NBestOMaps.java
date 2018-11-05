package kdm.models.misc;

import kdm.models.*;
import kdm.util.*;
import kdm.io.*;
import java.util.*;

public class NBestOMaps
{
    protected int nBest, nReal;
    protected OatesMapping[] list;

    public NBestOMaps()
    {
        nBest = 0;
        nReal = 0;
    }

    public NBestOMaps(int _nBest)
    {
        nBest = _nBest;
        list = new OatesMapping[nBest];
        nReal = 0;
    }

    public int size(){ return nReal; }
    public int getN(){ return nBest; }
    public boolean full(){ return (nReal==nBest); }
    public void clear(){ nReal = 0; }

    public boolean add(OatesMapping x)
    {
        // remove any overlapping window
        for(int i=0; i<nReal; i++)
        {
            // skip if no overlap
            if (x.iSeries != list[i].iSeries) continue;
            if (x.getLastDataIndex() < list[i].getFirstDataIndex()
                || x.getFirstDataIndex() > list[i].getLastDataIndex()) continue;

            if (x.score > list[i].score)
            {
                // we want to keep the new mapping
                for(int j=i+1; j<nReal; j++) list[j-1] = list[j];
                nReal--;
                break;
            }
            else return false; // keep the old mapping
        }

        // handle the empty list case
        if (nReal == 0)
        {
            list[0] = x;
            nReal = 1;
            return true;
        }

        // now handle the more general case
        int i;
        for(i=0; i<nReal; i++)
        {
            if (x.score > list[i].score)
            {
                for(int j=(int)Math.min(nReal, nBest-1); j>i; j--) list[j] = list[j-1];
                list[i] = x;
                if (nReal < nBest) nReal++;
                return true;
            }
        }

        // we can also add to the end of the real list if there's space
        if (nReal < nBest)
        {
            assert (i == nReal);
            list[i] = x;
            nReal++;
            return true;
        }

        return false;
    }
    
    public OatesMapping get(int i)
    {
        if (i<0 || i>=nReal) throw new ArrayIndexOutOfBoundsException(i);
        return list[i];
    }
}
