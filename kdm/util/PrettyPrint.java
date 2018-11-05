package kdm.util;

import java.io.*;

/**
 * Allows pretty-printing of real values. Pretty printing will remove trailing zeroes and
 * the decimal point if they are unnecessary.
 */
public class PrettyPrint
{
   public static String printDouble(double d)
   {
      return printDouble("%.4f", d);
   }

   public static String printDouble(double d, int nFrac)
   {
      return printDouble(String.format("%%.%df", nFrac), d);
   }

   public static String printDouble(String format, double d)
   {
      StringBuffer s = new StringBuffer(String.format(format, d));
      int n = s.length();
      while(s.charAt(n - 1) == '0')
      {
         n--;
         s.setLength(n);
      }
      if (s.charAt(n - 1) == '.') s.setLength(n - 1);
      return s.toString();
   }

   public static String printSigDig(double d, int nSig)
   {
      return printSigDig(d, nSig, true);
   }
   
   public static String printSigDig(double d, int nSig, boolean bLeadingZero)
   {
      String s = printDouble("%.8f", d);
      int i = s.lastIndexOf('.');
      if (d < 0) nSig++;
      calc:
      {
         if (i < 0) break calc; // no decimal point, so we can't trim      
         if (i >= nSig){ s = s.substring(0, i); break calc; }// no more info after decimal point
         if (!bLeadingZero && s.startsWith("0")) s = s.substring(1);
         if (nSig < s.length()) s = s.substring(0, nSig + 1);
      }
      if (s.equals("-0")) s = "0";
      return s;
   }

   public static String printInt(String format, int x)
   {
      return String.format(format, x);
   }
}
