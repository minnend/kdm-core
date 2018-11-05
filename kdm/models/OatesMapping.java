package kdm.models;

import java.util.*;
import kdm.data.*;
import kdm.util.*;
import kdm.io.*;

/** stores information about a mapping from an oates model to a sequence */
public class OatesMapping
{
    public int iSeries;
    public double score;
    
    /** maps from pattern elements to data */
    public int[] imap; 

    public OatesMapping(){ iSeries = -1; }
    public OatesMapping(double _score, int _nPatLen){ this(_score, _nPatLen, -1); }
    public OatesMapping(double _score, int _nPatLen, int _iSeries)
    {
        score = _score;
        imap = new int[_nPatLen];
        for(int i=0; i<_nPatLen; i++) imap[i] = -1;
        iSeries = _iSeries;
    }

    public WindowLocation getLoc()
    {
        return new WindowLocation(iSeries, getFirstDataIndex(), getDataLength());
    }
    
    public ScoredWindow getScoredwindow()
    {
        return new ScoredWindow(iSeries, getFirstDataIndex(), getDataLength(), score);
    }

    public int getDataLength(){ return getLastDataIndex() - getFirstDataIndex() + 1; }
    public int getPatternLength(){ return imap.length; }
    public int getFirstDataIndex(){ return imap[0]; }
    public int getLastDataIndex(){ return imap[imap.length-1]; }

    public String toString()
    {
        return "[OCMap: "+(iSeries+1)+"."+getFirstDataIndex()+"  "+getDataLength()+" ("
            +getPatternLength()+") "+score+"]";
    }
    
    /** @return comparator for OatesMapping (either ascending or descending) */
    public Comparator<OatesMapping> getComparator(final boolean bReverse)
    {
       return new Comparator<OatesMapping>(){
          public int compare(OatesMapping a, OatesMapping b)
          {
             int ret = 0;
             if (a.score > b.score) ret = 1;
             else if (a.score < b.score) ret = -1;
             else if (a.iSeries < b.iSeries) ret = 1;
             else if (a.iSeries > b.iSeries) ret = -1;
             else if (a.getFirstDataIndex() < b.getFirstDataIndex()) ret = 1;
             else if (a.getFirstDataIndex() > b.getFirstDataIndex()) ret = -1;
             else if (a.getLastDataIndex() < b.getLastDataIndex()) ret = 1;
             else if (a.getLastDataIndex() > b.getLastDataIndex()) ret = -1;        

             return (bReverse ? -ret : ret);
          }
       };
    }
}
