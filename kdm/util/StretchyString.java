package kdm.util;

import java.util.*;

/**
 * A stretchy string is a like a normal string, but it is allowed to stretch during comparisons.
 */
public class StretchyString implements Comparable
{
   protected String sBase;
   protected int nMaxStretch;

   public StretchyString(String s)
   {
      this(s, -1);      
   }
   
   public StretchyString(String s, int _nMaxStretch)
   {
      sBase = s;
      nMaxStretch = _nMaxStretch;
   }
   
   public String getString(){ return sBase; }
   public int getStretch(){ return nMaxStretch; }   
   public String getSkeleton(){ return getSkeleton(sBase); }
   
   public int hashCode()
   {
      return getSkeleton().hashCode();
   }
   
   public int compareTo(Object o)
   {
      StretchyString ss = (StretchyString)o;
      String skel = getSkeleton();
      String skel2 = ss.getSkeleton();
      
      // if skeletons are different, then equal
      int ret = skel.compareTo(skel2);
      if (ret != 0) return ret;

      // if any stretch is allowed, then equal
      int nStretch = ss.getStretch();
      if (nMaxStretch<0 || nStretch<0) return 0;
      nStretch = Math.max(nStretch, nMaxStretch);
      
      // see if strings are within stretch distance
      int[] runs = getRuns(sBase);
      int[] runs2 = getRuns(ss.getString());
      for(int i=0; i<runs.length; i++)
      {
         int x = Math.min(runs[i], runs2[i]);
         int y = Math.max(runs[i], runs2[i]);
         if (x*nStretch < y) return (x==runs[i] ? -1 : 1);
      }
      return 0;
   }
   
   public boolean equals(Object o)
   {
      return compareTo(o)==0;
   }
   
   /**
    * Compute the number of runs in the given string (eg. aabcccdd = {2,1,3,2} => 4)
    * 
    * @param s string to scan
    * @return number of runs
    */
   public static int getNumRuns(String s)
   {
      int slen = s.length();
      int nRuns = 1;
      for(int i = 1; i < slen; i++)
         if (s.charAt(i - 1) != s.charAt(i)) nRuns++;
      return nRuns;
   }

   /**
    * Compute the length of each character run in the given string (eg. aabcccdd =
    * {2,1,3,2} )
    * 
    * @param s string to scan
    * @return length of each character run
    */
   public static int[] getRuns(String s)
   {
      int slen = s.length();
      int[] runs = new int[getNumRuns(s)];
      Arrays.fill(runs, 1);

      int ix = 0;
      for(int iLetter = 0; iLetter < runs.length; iLetter++)
      {
         while(ix + 1 < slen && s.charAt(ix) == s.charAt(ix + 1))
         {
            runs[iLetter]++;
            ix++;
         }
         ix++;
      }

      return runs;
   }
   
   /**
    * Determine the run length of the character starting at the given index
    * @param s string to test
    * @param ix starting index
    * @return length of run of ix'th char of s
    */
   public static int getRunLength(String s, int ix)
   {
      if (s==null) return 0;
      int n = s.length();
      if (n<=ix) return 0;
      char c = s.charAt(ix);
      for(int i=ix+1; i<n; i++) if (s.charAt(i)!=c) return i-ix;
      return n-ix;
   }
   
   /**
    * @return edit distance (Levenshtein distance) between the two strings
    */
   public static int calcEditDist(String s1, String s2)
   {
      int na = s1.length();
      int nb = s2.length();
      int[][] costm = Library.allocMatrixInt(na, nb, Integer.MAX_VALUE);
      int ia, ib;
      char c;
      
      // init first row, column
      costm[0][0] = (s1.charAt(0) == s2.charAt(0) ? 0 : 1);      
      c = s2.charAt(0);
      for(ia=1; ia<na; ia++) costm[ia][0] = costm[ia-1][0]+1;      
      c = s1.charAt(0);
      for(ib=1; ib<nb; ib++) costm[0][ib] = costm[0][ib-1]+1;
      
      // now fill in the rest of the cost matrix
      // TODO: could include a band for more efficient calculation
      for(ia=1; ia<na; ia++)
      {
         c = s1.charAt(ia);
         for(ib=1; ib<nb; ib++)
            costm[ia][ib] = Library.min(costm[ia-1][ib]+1, costm[ia-1][ib-1]+(c==s2.charAt(ib) ? 0 : 1), costm[ia][ib-1]+1);
      }
      
      return costm[na-1][nb-1];
   }

   /**
    * @return the skeleton of the given string; the skeleton is the same as the original
    * string, but all runs are reduced to length 1.
    */
   public static String getSkeleton(String s)
   {
      int nRuns = getNumRuns(s);
      int slen = s.length();
      char[] chars = new char[nRuns];
      int ix = 0;
      for(int i = 0; i < nRuns; i++)
      {
         chars[i] = s.charAt(ix);
         while(ix + 1 < slen && s.charAt(ix) == s.charAt(ix + 1))
            ix++;
         ix++;
      }
      return new String(chars);
   }
}
