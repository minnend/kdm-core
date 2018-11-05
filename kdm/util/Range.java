package kdm.util;

import java.util.*;

/**
 * A range represents a discrete set: [a,b] (i.e., both endpoints are inclusive)
 */
public class Range implements Comparable
{
   public int a, b;
   public boolean bDirty;
   public Object payload;

   public Range()
   {
      a = 1;
      b = 0;
      bDirty = false;
      payload = null;
   }
   
   public Range(int _a, int _b)
   {
      this(_a,_b,null);
   }
   
   public Range(int _a, int _b, Object _payload)
   {
      assert (_a <= _b) : String.format("a=%d  b=%d", _a, _b);
      a = _a;
      b = _b;
      payload = _payload;
      bDirty = true;      
   }

   public Range(Range r)
   {
      a = r.a;
      b = r.b;
      bDirty = r.bDirty;
   }

   public int length()
   {
      return Math.max(b - a + 1, 0);
   }

   /**
    * Determines if this range intersects the given range.
    */
   public boolean intersects(Range r)
   {
      return (a <= r.b && b >= r.a);
   }

   
   /** @return true if the given range is entirely contained within this range */
   public boolean contains(Range r)
   {
      return (r.a>=a && r.b<=b);
   }
   
   /** @return true if the given index is contained in this range */
   public boolean contains(int i)
   {
      return (i >= a && i <= b);
   }
   
   /** @return this range after the times have been adjusted by dt */
   public Range offset(long dt)
   {
      a += dt;
      b += dt;
      return this;
   }

   /**
    * @return length of this range that overlaps the given range
    */
   public int getNumOverlap(Range r)
   {
      return intersect(r).length();
   }
   
   /**
    * @return length of this range that does NOT overlap the given range
    */
   public int getNumNotOverlap(Range r)
   {
      return length() - intersect(r).length();
   }
   
   /**
    * Determines if this range intersects or abuts the given range.
    */
   public boolean intersectAbuts(Range r)
   {
      return ((a - 1) <= r.b && (b + 1) >= r.a);
   }

   public void merge(Range r)
   {
      assert intersectAbuts(r);
      if (r.a < a)
      {
         a = r.a;
         bDirty = true;
      }
      if (r.b > b)
      {
         b = r.b;
         bDirty = true;
      }
      if (payload==null) payload = r.payload;
   }

   public Range mergeNew(Range r)
   {
      assert intersectAbuts(r);
      return new Range(Math.min(a, r.a), Math.max(b, r.b));
   }

   public Range _intersect(Range r)
   {
      if (!intersects(r)) makeEmpty();
      else
      {
         a = Math.max(a, r.a);
         b = Math.min(b, r.b);
      }
      return this;
   }
   
   public Range intersect(Range r)
   {
      return new Range(this)._intersect(r);
   }

   public boolean isEmpty()
   {
      return b < a;
   }

   public void makeEmpty()
   {
      b = 0;
      a = 1;
   }
   
   public void set(int _a, int _b)
   {
      a = _a;
      b = _b;
   }
   
   public void setLength(int len)
   {
      b = a + len - 1;
   }

   public boolean isDirty()
   {
      return bDirty;
   }

   public void setDirty()
   {
      bDirty = true;
   }

   public void clearDirty()
   {
      bDirty = false;
   }

   public void setDirty(boolean _bDirty)
   {
      bDirty = _bDirty;
   }

   public int compareTo(Object o)
   {
      Range r = (Range)o;
      if (a < r.a) return -1;
      if (a > r.a) return 1;
      if (b < r.b) return -1;
      if (b > r.b) return 1;
      return 0;
   }

   public boolean equals(Object o)
   {
      Range r = (Range)o;
      return (a == r.a && b == r.b);
   }

   public String toString()
   {
      return "[" + a + " " + b + "]";
   }
}
