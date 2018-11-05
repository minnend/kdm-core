package kdm.util;

/**
 * Represents an arbitrary pair of values. If you compare two Pairs, "A" must be comparable, while "B" only
 * has to be comparable if this is not a map (i.e., bMap = false)
 */
public class Pair<A, B> implements Comparable
{
   public A first;
   public B second;
   public boolean bMap;

   /** construct a real pair (i.e., not a map) */
   public Pair(A first, B second)
   {
      this(first, second, false);
   }

   /** construct a real pair (bMap=false) or a map (bMap = true) */
   public Pair(A first, B second, boolean bMap)
   {
      this.first = first;
      this.second = second;
      this.bMap = bMap;
   }

   @Override
   public String toString()
   {
      return "[Pair: " + first + ", " + second + "]";
   }

   public boolean isMap()
   {
      return bMap;
   }

   @Override
   public boolean equals(Object o)
   {
      Pair p = (Pair)o;
      return (first.equals(p.first) && (bMap || second.equals(p.second)));
   }

   public int compareTo(Object o)
   {
      Pair p = (Pair)o;
      int v = ((Comparable)first).compareTo(p.first);
      if (v != 0 || bMap) return v;
      return ((Comparable)second).compareTo(p.second);
   }
}
