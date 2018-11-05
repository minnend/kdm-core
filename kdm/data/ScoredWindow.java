package kdm.data;

import java.util.regex.*;
import java.io.*;
import kdm.util.*;
import kdm.io.*;

/**
 * A window location with a score
 */
public class ScoredWindow extends WindowLocation implements Comparable, TextRep
{
   public double score;

   public ScoredWindow()
   {
      score = Library.NEGINF;
   }

   public ScoredWindow(int iSeries, int iStart, int nLength, double score)
   {
      super(iSeries, iStart, nLength);
      this.score = score;
   }
   
   public ScoredWindow(ScoredWindow swin)
   {
      super(swin);
      score = swin.score;
   }

   @Override
   public boolean equals(Object o)
   {
      ScoredWindow sw = (ScoredWindow)o;
      return (score == sw.score);
   }

   @Override
   public int compareTo(Object o)
   {
      ScoredWindow sw = (ScoredWindow)o;
      if (score < sw.score) return -1;
      if (score > sw.score) return 1;
      return super.compareTo(o);
   }

   @Override
   public String toString()
   {
      return String.format("[%d.%d  %d  |%.4f]", iSeries + 1, iStart, nLength, score);
   }

   @Override
   public String toText()
   {
      return String.format("%s %f", super.toText(), score);
   }

   @Override
   public int fromText(String s)
   {
      int i = super.fromText(s);
      if (i < 0) return i;

      Pattern pat = Pattern.compile("^\\s*([-\\+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+))\\s*");
      Matcher m = pat.matcher(s.substring(i));
      if (!m.lookingAt())
      {
         System.err.println("Failed to find ScoredWindow data");
         return -1;
      }
      assert (m.groupCount() == 1);
      score = Double.parseDouble(m.group(1));
      return m.end()+i;
   }
   
   /**
    * Test the real number regex 
    */
   public static void main(String args[]) throws Exception
   {
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      Pattern pat = Pattern.compile("^\\s*([-\\+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+))\\s*");      
      while(true)
      {
         System.err.print(">");
         String line = in.readLine();
         Matcher m = pat.matcher(line);
         if (!m.matches()) System.err.println("Match Failed!");         
         else System.err.printf("Matched!   (%d: %s)\n", m.groupCount(), m.group(1));
      }
   }
}
