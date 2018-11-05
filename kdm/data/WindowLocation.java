package kdm.data;

import java.util.*;
import java.util.regex.*;

import kdm.util.*;
import kdm.io.*;

/**
 * Represents a window (contiguous subsequence) in a sequence.
 */
public class WindowLocation implements Comparable, TextRep
{
   public int iSeries, iStart, nLength;

   /** create empty window location */
   public WindowLocation()
   {
      this(-1, -1, 0);
   }
   
   /** create window location with iSeries = -1 */
   public WindowLocation(int iStart, int nLength)
   {
      this(-1, iStart, nLength);
   }

   /** create fully specified window location */
   public WindowLocation(int iSeries, int iStart, int nLength)
   {
      this.iSeries = iSeries;
      this.iStart = iStart;
      this.nLength = nLength;
   }
   
   /** create a copy of the given window location */
   public WindowLocation(WindowLocation wloc)
   {
      this(wloc.iSeries, wloc.iStart, wloc.nLength);
   }
   
   public void set(int iSeries, int iStart, int nLength)
   {
      this.iSeries = iSeries;
      this.iStart = iStart;
      this.nLength = nLength;
   }
   
   /**
    * @return "rasterized" index taking in to account the given sequence lengths
    */
   public final int getLinearIndex(int[] seqLens)
   {
      return Library.getIndexFromArrayOffset(iSeries, iStart, seqLens);
   }

   /** @return a range that covers this window location */
   public final Range getRange()
   {
      return new Range(iStart, iStart + nLength - 1);
   }
   
   @Override
   public int hashCode()
   {
      return iSeries*1013 + iStart*4261 + nLength*7253;
   }

   public boolean equals(Object o)
   {
      WindowLocation loc = (WindowLocation)o;
      return (iSeries == loc.iSeries && iStart == loc.iStart && nLength == loc.nLength);
   }

   public int compareTo(Object o)
   {
      WindowLocation loc = (WindowLocation)o;
      if (iSeries < loc.iSeries) return -1;
      if (iSeries > loc.iSeries) return 1;
      if (iStart < loc.iStart) return -1;
      if (iStart > loc.iStart) return 1;
      if (nLength < loc.nLength) return -1;
      if (nLength > loc.nLength) return 1;
      return 0;
   }

   /** @return number of frames of overlap between this window and the given location */
   public final int getNumOverlap(WindowLocation loc)
   {
      if (!overlaps(loc)) return 0;
      int a = Math.max(iStart, loc.iStart);
      int b = Math.min(getLastIndex(), loc.getLastIndex());
      return b-a;
   }
   
   /** @return true if the this window location overlaps (not abuts) the given location */
   public final boolean overlaps(WindowLocation loc)
   {
      if (iSeries != loc.iSeries) return false;
      return (iStart <= loc.getLastIndex() && loc.iStart <= getLastIndex());
   }

   /** @return true if the given index is withing this window */
   public final boolean contains(int t)
   {
      return (t >= iStart && t < iStart + nLength);
   }

   public final int getFirstIndex()
   {
      return iStart;
   }

   /** @return last real index of this window */ 
   public final int getLastIndex()
   {
      return (iStart + nLength - 1);
   }

   /**
    * @return the first index of the window
    */
   public final int start()
   {
      return iStart;
   }

   /**
    * @return the end of the window (ie, one past the last index)
    * @see getLastIndex
    */
   public final int end()
   {
      return (iStart + nLength);
   }

   /**
    * @return the length of the window
    */
   public final int length()
   {
      return nLength;
   }
   
   /**
    * @return subsequence corresponding to this window location 
    */
   public final Sequence getSeq(ArrayList<Sequence> data)
   {
      return data.get(iSeries).subseq(iStart, iStart+nLength, iSeries);
   }

   public String toString()
   {
      return String.format("[%d.%d (%d)]", iSeries + 1, iStart, nLength);
   }

   public String toText()
   {
      return String.format("%d.%d (%d)", iSeries, iStart, nLength);
   }

   public static WindowLocation createFromText(String s)   
   {
      WindowLocation wloc = new WindowLocation();
      if (wloc.fromText(s) < 0) return null;
      return wloc;
   }
   
   public int fromText(String s)
   {
      Pattern pat = Pattern.compile("^\\s*(\\d+)\\.(\\d+)\\s+\\((\\d+)\\)\\s*");
      Matcher m = pat.matcher(s);
      if (!m.lookingAt())
      {
         System.err.println("Failed to find WindowLocation data");
         return -1;
      }      
      iSeries = (short)Integer.parseInt(m.group(1));
      iStart = (short)Integer.parseInt(m.group(2));
      nLength = (short)Integer.parseInt(m.group(3));
      return m.end();
   }
   
   public static ArrayList<Sequence> getExamples(ArrayList<WindowLocation> occs, ArrayList<Sequence> data)
   {
      ArrayList<Sequence> examples = new ArrayList<Sequence>();
      for(WindowLocation wloc : occs)
      {
         Sequence seq = data.get(wloc.iSeries);
         examples.add(seq.subseq(wloc.iStart, wloc.getLastIndex() + 1, wloc.iSeries));
      }
      return examples;
   }
   
   public static ArrayList<DiscreteSeq> getExamplesD(ArrayList<WindowLocation> occs, ArrayList<DiscreteSeq> data)
   {
      ArrayList<DiscreteSeq> examples = new ArrayList<DiscreteSeq>();
      for(WindowLocation wloc : occs)
      {
         DiscreteSeq dseq = data.get(wloc.iSeries);
         examples.add(dseq.subseq(wloc.iStart, wloc.getLastIndex() + 1, wloc.iSeries));
      }
      return examples;
   }
}
