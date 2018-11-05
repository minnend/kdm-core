package kdm.util;

/**
 * Iterator for a span list; iterators become (silently) invalid if the associated span list is modified
 * during iteration. So don't do that!
 */
public final class SpanIterator
{
   // TODO iterator should know when underlying span list is modified
   // TODO range iterator -- so only iterate over points in which a contiguous block is available
   protected SpanList span;
   protected int iPos, iRange;

   public SpanIterator(SpanList _span)
   {
      span = _span;
      iRange = 0;
      iPos = 0;
   }

   public SpanIterator(SpanIterator it)
   {
      span = it.span;
      iPos = it.iPos;
      iRange = it.iRange;
   }

   /** @return true if there are more indices in this iterator */
   public final boolean hasMore()
   {
      return (iRange + 1 < span.getNumSpans() || (iRange + 1 == span.getNumSpans() && iPos + 1 < span
            .getRange(iRange).b));
   }
   
   /** Same as hasMore(); */
   public final boolean hasNext(){ return hasMore(); }

   /** modify this iterator so that the next index is >= the given index */
   public final void jump(int imin)
   {
      if (!hasMore()) return;
      if (peek() >= imin) return;
      assert(span.isSorted());

      // find the right range
      int nRange = span.getNumSpans();
      while(iRange < nRange)
      {
         Range r = span.getRange(iRange);
         if (r.b >= imin) break;
         iRange++;
      }

      if (iRange < nRange)
      {
         Range r = span.getRange(iRange);

         // find the right spot within this range
         if (r.a >= imin) iPos = 0;
         else iPos = imin - r.a;
      }
   }

   /** @return the next index without increasing this iterator */
   public final int peek()
   {
      return span.getRange(iRange).a + iPos;
   }

   /** @return the next index */
   public final int next()
   {
      Range r = span.getRange(iRange);
      int ret = r.a + iPos;
      if (ret == r.b)
      {
         iPos = 0;
         iRange++;
      }
      else iPos++;
      return ret;
   }
}
