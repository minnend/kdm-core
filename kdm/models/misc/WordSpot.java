package kdm.models.misc;

import kdm.data.*;
import kdm.models.*;
import kdm.util.*;
import kdm.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WordSpot extends ScoredWindow implements TextRep
{
   public static short ERROR_UNKNOWN = 0;
   public static short ERROR_CORRECT = 1;
   public static short ERROR_INSERTION = 2;
   public static short ERROR_DELETION = 3;
   public static short ERROR_SUBSTITUTION = 4;

   public short iClass;
   public short errorType;

   public WordSpot()
   {
      this(-1, -1, 0, Double.NEGATIVE_INFINITY, -1);
   }
   
   public WordSpot(WindowLocation wloc, double score, int iClass)
   {
      this(wloc.iSeries, wloc.iStart, wloc.nLength, score, iClass);
   }

   public WordSpot(int iSeries, int iStart, int nLen, double score, int iClass)
   {
      super(iSeries, iStart, nLen, score);
      this.iClass = (short)iClass;
      errorType = ERROR_UNKNOWN;
   }

   public void set(int iSeries, int iStart, int nLen, double score, int iClass)
   {
      this.iSeries = iSeries;
      this.iStart = iStart;
      nLength = nLen;
      this.score = score;
      this.iClass = (short)iClass;
   }

   public boolean isCorrect()
   {
      return (errorType == ERROR_CORRECT);
   }

   public String toText()
   {
      return String.format("%s %d %d", super.toText(), iClass, errorType);
   }

   public int fromText(String s)
   {
      int i = super.fromText(s);
      if (i<0) return i;
      
      Pattern pat = Pattern.compile("^\\s*(\\d+)\\s+(\\d+)\\s*");
      Matcher m = pat.matcher(s.substring(i));
      if (!m.find())
      {
         System.err.println("Failed to find WordSpot data");
         return -1;
      }
      assert (m.groupCount()==2);
      iClass = (short)Integer.parseInt(m.group(1));
      errorType = (short)Integer.parseInt(m.group(2));
      return m.end()+i;
   }

   public String toString()
   {
      return String.format("[%d: %d.%d  %d  |%.4f]", iClass, iSeries + 1, iStart, nLength, score);
   }
}
