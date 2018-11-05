package kdm.mlpr.suffix_tree;

import java.util.*;

import kdm.util.*;

/**
 * Stores information about an occurrence: the string itself, how many times it occurs, and its score.
 */
public class OccInfo implements Comparable
{
   public String s;
   public int n, score;
   public MyIntList aDontCare;
   
   public OccInfo()
   {
      this(null, 0, 0, null);
   }
   
   public OccInfo(OccInfo occi)
   {
      this(occi.s, occi.n, occi.score, occi.aDontCare);
   }
   
   public OccInfo(String _s, int _n, int _score, MyIntList _aDontCare)
   {
      s = _s;
      n = _n;
      score = _score;   
      aDontCare = _aDontCare;
   }
   
   public int hashCode(){ return s.hashCode(); }
   
   public int compareTo(Object o)
   {
      OccInfo b = (OccInfo)o;
      if (score < b.score) return -1;
      if (score > b.score) return 1;
      if (n > b.n) return -1;
      if (n < b.n) return 1;
      return s.compareTo(b.s);
   }
   
   public boolean equals(Object o)
   {
      return (compareTo(o)==0);      
   }
   
   public String toString()
   {
      return String.format("[%s #%d %d]", s, n, score);
   }

   /** @return average string based on all of the given time warped strings */ 
   public static String calcAverageString(String sBase, Collection<OccInfo> strings)
   {            
      if (strings.isEmpty()) return sBase;
      int nStrings = 0;
      int[] waccum = new int[StretchyString.getNumRuns(sBase)];
      String sSkel = StretchyString.getSkeleton(sBase);
      
      // accum stats over all strings
      Iterator<OccInfo> it = strings.iterator();
      while(it.hasNext())
      {
         OccInfo occi = it.next();         
         nStrings += occi.n;
         int[] runs = StretchyString.getRuns(occi.s);
         String sCurSkel = StretchyString.getSkeleton(occi.s);
         int iAccum = 0;
         //System.err.printf("base=%s (%s,%d)  cur=%s (%s,%d)\n", sBase, sSkel, waccum.length, occi.s, sCurSkel, runs.length);
         for(int i=0; i<runs.length; i++)
         {
            if (sCurSkel.charAt(i) != sSkel.charAt(iAccum)) continue;
            waccum[iAccum++] += runs[i] * occi.n;
         }
         assert(iAccum == waccum.length);
      }
      // TODO: could do something smarter to account for bias toward longer runs      
      for(int i=0; i<waccum.length; i++)
      {
         //System.err.printf("%d  %d  ->  %.3f\n", waccum[i], nStrings, (double)waccum[i] / nStrings);
         waccum[i] = (int)Math.round((double)waccum[i] / nStrings);
      }
      
      // build the string from the run data
      StringBuffer sb = new StringBuffer();
      for(int i=0; i<waccum.length; i++)
         for(int j=0; j<waccum[i]; j++)
            sb.append(sSkel.charAt(i));
      
      return sb.toString();
   }
}
