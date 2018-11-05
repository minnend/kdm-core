package kdm.util;

import kdm.io.*;

/**
 * Stores a pair of ints. If it's a map, then only the first int ('a') matters for comparisions.
 */
public class PairII implements Comparable
{
   public int a, b;
   public boolean bMap;

   public PairII()
   {
      this(0, 0, false);
   }

   public PairII(int a, int b)
   {
      this(a, b, false);
   }

   public PairII(int a, int b, boolean bMap)
   {
      this.a = a;
      this.b = b;
      this.bMap = bMap;
   }

   public PairII(PairII p)
   {
      copyFrom(p);
   }

   public void copyFrom(PairII p)
   {
      a = p.a;
      b = p.b;
      bMap = p.bMap;
   }

   public boolean isMap()
   {
      return bMap;
   }

   @Override
   public boolean equals(Object o)
   {
      PairII p = (PairII)o;
      return (a == p.a && (bMap || b == p.b));
   }

   public int compareTo(Object o)
   {
      PairII p = (PairII)o;
      if (a < p.a) return -1;
      if (a > p.a) return 1;
      if (!bMap){
         if (b < p.b) return -1;
         if (b > p.b) return 1;
      }
      return 0;
   }

   @Override
   public int hashCode()
   {
      // TODO probably a better hashcode for real pair
      if (bMap) return a;
      else return (a * 1021) + b;
   }

   @Override
   public String toString()
   {
      return "[PairII: " + a + ", " + b + "]";
   }
}
