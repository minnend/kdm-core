package kdm.util;

import java.util.*;

/**
 * This class represents a binary array by explicity representing all of the ranges in the array that are
 * true. This is very efficient if large, continuous areas are true, but is quite inefficient if true and
 * false elements are interspersed (i.e., T,F,T,F,T,F,...). One benefit of this representation is that false
 * areas can be skipped in constant time, potentially leading to significant time savings if only small
 * "islands" of true values exist.
 * 
 * Each Range that represents a true area is made up of two indices 'a' and 'b' where 'a' and 'b' are
 * inclusive. Furthermore, this class automatically merges intersecting (or abutting) ranges to minimizes the
 * number of ranges that must be represented (i.e., [1 10] + [10 20] -> [1 20] automatically).
 */
public class SpanList
{
   protected ArrayList<Range> spans;
   protected int vmin, vmax;

   protected boolean itReady = true;
   protected int itRange = 0;
   protected int itVal = 0;
   protected int itBDirty = 0; // 0: all, 1: dirty only, 2: clean only
   protected Range rItCurRange = null;

   public SpanList()
   {
      this(Integer.MIN_VALUE, Integer.MAX_VALUE, false);
   }

   public SpanList(int _vmin, int _vmax, boolean bFill)
   {
      spans = new ArrayList<Range>();
      vmin = _vmin;
      vmax = _vmax;
      if (bFill) fill();
      itReset();
   }

   public SpanList(SpanList sl)
   {
      vmin = sl.vmin;
      vmax = sl.vmax;
      spans = new ArrayList<Range>();
      for(int i = 0; i < sl.getNumSpans(); i++)
         spans.add(new Range(sl.getRange(i)));
      itReset();
   }

   /**
    * Reset the iterator
    */
   public void itReset() // TODO: should get rid of these functions and use iterator()
   {
      itRange = 0;
      itVal = 0;
      itReady = (itRange < spans.size());
      itBDirty = 0;
   }

   /**
    * Reset iterator to give dirty or clean values
    * 
    * @param bDirty true for dirty values, false for clean
    */
   public void itReset(boolean bDirty)
   {
      itRange = 0;
      rItCurRange = null;
      itVal = 0;
      itBDirty = (bDirty ? 1 : 2);
      if (bDirty){
         while(!spans.get(itRange).isDirty()) // want a dirty range
         {
            itRange++;
            if (itRange >= spans.size()) break;
         }
      }
      else{
         while(spans.get(itRange).isDirty()) // want a clean range
         {
            itRange++;
            if (itRange >= spans.size()) break;
         }
      }
      itReady = (itRange < spans.size());
   }

   /** @return the range that holds the current (just returned) index */
   public Range getItRange()
   {
      return rItCurRange;
   }

   /**
    * @return the next value for the current iteration
    * @throws IllegalStateException
    */
   public int itNext() throws IllegalStateException
   {
      if (!itReady) throw new IllegalStateException("SpanList iterator not ready" + " (call itReset()).");
      assert (itRange < spans.size()) : String.format("itRange=%d  itReady=%b  spans=%d", itRange, itReady,
            spans.size());
      rItCurRange = spans.get(itRange);
      int v = rItCurRange.a + itVal;
      itVal++;
      if (itVal >= rItCurRange.length()){
         itVal = 0;
         itRange++;
         if (itRange >= spans.size()) itReady = false;
         else{
            if (itBDirty == 1) // dirty only
            {
               while(!spans.get(itRange).isDirty()){
                  itRange++;
                  if (itRange >= spans.size()){
                     itReady = false;
                     break;
                  }
               }
            }
            else if (itBDirty == 2) // clean only
            {
               while(spans.get(itRange).isDirty()){
                  itRange++;
                  if (itRange >= spans.size()){
                     itReady = false;
                     break;
                  }
               }
            }
         }
      }
      return v;
   }

   /**
    * @return true if there are more values to be iterated over
    */
   public boolean itMore()
   {
      return itReady;
   }

   /** @return length of the longest continuous segment (range) */
   public int longestSegment()
   {
      int x = 0;
      for(Range r : spans)
         if (r.length() > x) x = r.length();
      return x;
   }

   /** @return an iterator for the values in this span list */
   public SpanIterator iterator()
   {
      sort();
      return new SpanIterator(this);
   }

   /** @return the i-th range */
   public Range getRange(int i)
   {
      return spans.get(i);
   }

   /** @return number of spans (ranges) in this span list */
   public int getNumSpans()
   {
      return spans.size();
   }

   /** @return smallest index representable */
   public int getRangeMin()
   {
      return vmin;
   }

   /** @return largest index representable */
   public int getRangeMax()
   {
      return vmax;
   }

   /** @return smallest index that is actually in the range (MAX_VALUE if none) */
   public int getSpanMin()
   {
      if (isEmpty()) return Integer.MAX_VALUE;
      return spans.get(0).a;
   }

   /** @return largest index that is actually in the range (MIN_VALUE if none) */
   public int getSpanMax()
   {
      if (isEmpty()) return Integer.MIN_VALUE;
      return spans.get(spans.size() - 1).b;
   }

   /** @return the ix'th index that is actually in the range (MIN_VALUE if none) */
   public int get(int ix)
   {
      for(int i = 0; i < spans.size(); i++){
         Range r = spans.get(i);
         int m = r.length();
         if (ix < m) return r.a + ix;
         ix -= m;
      }
      return Integer.MIN_VALUE;
   }

   /** @return true if there are user-supplied bounds on this range */
   public boolean isBound()
   {
      return (vmin != Integer.MIN_VALUE && vmax != Integer.MAX_VALUE);
   }

   /** clear the span list so that no indices are actually in the range (all false) */
   public void clear()
   {
      itReady = false;
      spans.clear();
   }

   /** place all indices in the range (all true) */
   public void fill()
   {
      assert (isBound());
      itReady = false;
      spans.clear();
      add(vmin, vmax);
   }

   /**
    * Set all ranges to clean
    */
   public void clean()
   {
      for(int i = 0; i < spans.size(); i++)
         spans.get(i).clearDirty();
   }

   /** add the given index to the span list */
   public void add(int a)
   {
      add(a, a);
   }

   /** add the given range (inclusive) to the span list */
   public void add(int a, int b)
   {
      add(new Range(a, b));
   }

   /** add the given range to the span list */
   public void add(Range r)
   {
      itReady = false;

      // keep range in bound
      if (isBound()){
         if (r.a > vmax || r.b < vmin) return;
         if (r.a < vmin){
            r.a = vmin;
            r.setDirty();
         }
         if (r.b > vmax){
            r.b = vmax;
            r.setDirty();
         }
      }

      // merge all intersects with the new range
      for(int i = 0; i < spans.size();){
         Range ri = spans.get(i);
         if (r.intersectAbuts(ri)){
            r.merge(ri);
            spans.remove(i);
         }
         else i++;
      }
      spans.add(r); // finally, we can add the new (possibly modified) range
   }

   /** add all indices in the given span list */
   public void add(final SpanList sl)
   {
      int n = sl.getNumSpans();
      for(int i = 0; i < n; i++)
         add(new Range(sl.getRange(i)));
   }

   /** remove the given index from the span list */
   public void sub(int a)
   {
      sub(a, a);
   }

   /** remove the given range (inclusive) from the span list */
   public void sub(int a, int b)
   {
      sub(new Range(a, b));
   }

   /** remove the given range from the span list */
   public void sub(Range r)
   {
      itReady = false;
      for(int i = 0; i < spans.size();){
         Range ri = spans.get(i);
         if (r.intersects(ri)){
            // remove prefix
            if (r.a <= ri.a && r.b < ri.b){
               ri.a = r.b + 1;
               ri.setDirty();
               i++;
            }

            // remove suffix
            else if (r.a > ri.a && r.b >= ri.b){
               ri.b = r.a - 1;
               ri.setDirty();
               i++;
            }

            // remove all
            else if (r.a <= ri.a && r.b >= ri.b) spans.remove(i);

            // remove middle (split)
            else{
               spans.remove(i);
               spans.add(new Range(ri.a, r.a - 1));
               spans.add(new Range(r.b + 1, ri.b));
               break;
            }
         }
         else i++;
      }
   }

   /** remove all indices in the given span list */
   public void sub(SpanList sl)
   {
      int n = sl.getNumSpans();
      for(int i = 0; i < n; i++)
         sub(new Range(sl.getRange(i)));
   }

   /**
    * Computes the union of this and the given span list
    */
   public SpanList union(SpanList sl)
   {
      SpanList nsl = new SpanList(this);
      int n = sl.getNumSpans();
      for(int i = 0; i < n; i++)
         nsl.add(sl.getRange(i));
      return sl;
   }

   /**
    * Computes the intersection of this span list and the given range.
    */
   public void intersect(Range r)
   {
      itReady = false;
      assert false : "not yet implemented"; // TODO
   }

   /**
    * Computes the intersection of this and the given span list and returns the intersection in a new span
    * list.
    */
   public SpanList intersect(SpanList sl)
   {      
      int a = Math.min(vmin, sl.getRangeMin());
      int b = Math.max(vmax, sl.getRangeMax());
      SpanList spanInt = new SpanList(a, b, false);
      int n = getNumSpans();
      int m = sl.getNumSpans();
      // TODO could be (much) more efficient
      for(int i=0; i<n; i++){
         Range ri = getRange(i);
         for(int j=0; j<m; j++){           
            Range rj = sl.getRange(j);
            if (!ri.intersects(rj)) continue;
            spanInt.add(ri.intersect(rj));
         }
      }
      return spanInt;
   }

   /**
    * Inverts the span list by making all ignored area covered and vice-versa (i.e., swap T/F regions). This
    * only works on bounded ranges.
    */
   public void invert()
   {
      assert (isBound());
      itReady = false;

      // copy this span list
      SpanList sl = new SpanList(this);

      // include the whole range
      fill();

      // subtract everything that was in the span list
      int n = sl.getNumSpans();
      for(int i = 0; i < n; i++)
         sub(sl.getRange(i));
   }

   public void prefix(int v)
   {
      itReady = false;
      SpanList sl = new SpanList(this);
      clear();
      int nSpans = sl.getNumSpans();
      for(int iSpan = 0; iSpan < nSpans; iSpan++){
         Range r = sl.getRange(iSpan);
         r.a -= v;
         add(r);
      }
   }

   public void suffix(int v)
   {
      if (v == 0) return;

      itReady = false;
      SpanList sl = new SpanList(this);
      clear();
      int nSpans = sl.getNumSpans();
      for(int iSpan = 0; iSpan < nSpans; iSpan++){
         Range r = sl.getRange(iSpan);
         r.b += v;
         add(r);
      }
   }

   public void sort()
   {
      itReady = false;
      Collections.sort(spans);
   }

   public boolean isSorted()
   {
      for(int i = 1; i < spans.size(); i++){
         Range r1 = spans.get(i - 1);
         Range r2 = spans.get(i);
         if (r1.b > r2.a) return false;
      }
      return true;
   }

   /** @return true if the given range is in (completely) included in this span list */
   public boolean contains(Range r)
   {
      for(int j = 0; j < spans.size(); j++)
         if (spans.get(j).contains(r)) return true;
      return false;
   }

   /** @return true if the given index is in included in this span list */
   public boolean contains(int i)
   {
      for(int j = 0; j < spans.size(); j++)
         if (spans.get(j).contains(i)) return true;
      return false;
   }

   /** @return true if there are no spots included in this span list */
   public boolean isEmpty()
   {
      return spans.isEmpty();
   }

   /** @return number of locations in this span list */
   public int size()
   {
      int n = 0;
      for(Range r : spans)
         n += r.length();
      return n;
   }

   /** @return maximum number of spots if all are included */
   public int getMaxSize()
   {
      assert (isBound());
      return vmax - vmin + 1;
   }

   /** @return number of potential locations that are no longer in this span list */
   public int getNumRemoved()
   {
      return getMaxSize() - size();

   }

   public String toString()
   {
      StringBuffer sb = new StringBuffer();
      sb.append("[");
      for(int i = 0; i < spans.size(); i++){
         sb.append(spans.get(i));
         if (i < spans.size() - 1) sb.append(", ");
      }
      sb.append("]");
      return sb.toString();
   }
   
   /** @return array of all indices contained in this list */ 
   public int[] toIndexArray()
   {
      int N = size();
      int[] a = new int[N];
      SpanIterator it = iterator();
      int i = 0;
      while(it.hasNext())
         a[i++] = it.next();
      return a;
   }
}
