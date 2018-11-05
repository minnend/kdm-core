package kdm.util;

import java.util.*;

/**
 * Adapted from here:
 * http://www.javaworld.com/javaforums/showthreaded.php?Cat=&Board=javabeginner&Number=15478&page=3&view=collapsed&sb=9&o=&vc=1
 */
public class CombinationGenerator implements Enumeration
{
   private int N;
   private BitSet X;
   private Object[] items;
   private Object[] array;

   public CombinationGenerator(Object[] items, int k)
   {
      this.items = items;
      N = items.length;
      array = new Object[k];
      X = new BitSet(N + 1);
      for(int i = 0; i < k; ++i) X.set(i);
   }

   public boolean hasMoreElements()
   {
      return !X.get(N);
   }

   private int findOne(BitSet bs)
   {
      int len = bs.size();
      for(int i = 0; i <= N; ++i)
         if (bs.get(i)) return i;
      return -1;
   }

   private int incr(BitSet bs, int n)
   {
      int a = 0;
      while(true)
      {
         if (bs.get(n))
         {
            bs.clear(n);
            n++;
            a++;
         }
         else
         {
            bs.set(n);
            break;
         }
      }
      return a;
   }

   public Object nextElement()
   {
      int k = 0;
      for(int i = 0; i <= N; i++)
         if (X.get(i)) array[k++] = items[i];
      int u = incr(X, findOne(X)) - 1;
      for(int i = 0; i < u; ++i)
         X.set(i);
      return array;
   }

   public static void main(String args[])
   {
      Integer[] a = new Integer[]{ 1, 2, 3, 4, 5 };
      CombinationGenerator gen = new CombinationGenerator(a, 3);
      while(gen.hasMoreElements())
      {
         Object[] x = (Object[])gen.nextElement();
         for(int i=0; i<x.length; i++) System.err.printf("%d ", (Integer)x[i]);
         System.err.println();
      }
   }
}
