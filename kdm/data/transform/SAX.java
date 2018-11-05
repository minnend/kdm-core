package kdm.data.transform;

import org.apache.commons.math.stat.StatUtils;
import kdm.models.*;
import kdm.util.Library;

import java.util.*;

/**
 * Implements the symbolic aggregate approximation (SAX) algorithm for converting continuous data into
 * symbolic data
 */
public class SAX
{
   public static final double INF = Double.POSITIVE_INFINITY;
   public static final double NEGINF = Double.NEGATIVE_INFINITY;

   /** table of cut-off points taken from Lin & Keogh's code */
   public static final double cuts[][] = new double[][] { {}, { NEGINF, INF }, { NEGINF, 0, INF },
         { NEGINF, -0.43, 0.43, INF }, { NEGINF, -0.67, 0, 0.67, INF },
         { NEGINF, -0.84, -0.25, 0.25, 0.84, INF }, { NEGINF, -0.97, -0.43, 0, 0.43, 0.97, INF },
         { NEGINF, -1.07, -0.57, -0.18, 0.18, 0.57, 1.07, INF },
         { NEGINF, -1.15, -0.67, -0.32, 0, 0.32, 0.67, 1.15, INF },
         { NEGINF, -1.22, -0.76, -0.43, -0.14, 0.14, 0.43, 0.76, 1.22, INF },
         { NEGINF, -1.28, -0.84, -0.52, -0.25, 0, 0.25, 0.52, 0.84, 1.28, INF } };

   /**
    * Convert the given value to a symbol
    * 
    * @param x real value to convert (after z-normalization)
    * @param nSymbols total number of symbols in the quantization
    * @return symbol (starting with 'a')
    */
   public static char raw2sym(double x, int nSymbols)
   {
      assert (nSymbols >= 1 && nSymbols < cuts.length) : String.format("Invalid number of SAX symbols (%d)",
            nSymbols);
      if (nSymbols < 1 || nSymbols >= cuts.length) return 0;
      return (char)(Histogram.bin(x, cuts[nSymbols]) + 'a');
   }
   
   /**
    * Convert the given value to a symbol index
    * 
    * @param x real value to convert (after z-normalization)
    * @param nSymbols total number of symbols in the quantization
    * @return symbol (starting with 0)
    */
   public static int raw2int(double x, int nSymbols)
   {
      assert (nSymbols >= 1 && nSymbols < cuts.length) : String.format("Invalid number of SAX symbols (%d)",
            nSymbols);
      if (nSymbols < 1 || nSymbols >= cuts.length) return 0;
      return Histogram.bin(x, cuts[nSymbols]);
   }

   /**
    * Generate PAA segments from raw data for SAX; data will be transformed to have zero mean and unit
    * variance before the PAA calculation takes place
    * 
    * @param raw raw data
    * @param iStart starting index of window
    * @param len length of the window
    * @param paa storage for PAA values (implicitly declares number of segments)
    * @return true if successful
    */
   public static boolean genPAA(double[] raw, int iStart, int len, double[] paa)
   {
      if (paa == null || paa.length < 1 || paa.length > len) return false;

      double mean = StatUtils.mean(raw, iStart, len);
      double sdev = Math.sqrt(StatUtils.variance(raw, iStart, len));

      int a = 0;
      double fa = 0;
      double fda = (double)len / paa.length;
      for(int i = 0; i < paa.length; i++){
         fa += fda;
         int b = (int)Math.round(fa);
         int n = b - a;
         paa[i] = 0;
         for(int j = a; j < b; j++)
            paa[i] += (raw[iStart + j] - mean) / sdev;
         paa[i] /= n;
         a = b;
      }

      return true;
   }

   /**
    * Determine the n boundary locations for the given gaussian (note: common values are precomputed in
    * <i>cuts</i>)
    * 
    * @param n number of boundary locations (so n+1 symbols/gaps)
    * @param mean mean of gaussian
    * @param sdev standard deviation of gaussian
    * @return array containing the n boundary points
    */
   public static double[] findBoundaries(int n, double mean, double sdev)
   {
      double[] ret = new double[n];
      double x = Library.NEGINF;
      double frac = 1.0 / (n + 1);
      for(int i = 0; i < n - i - 1; i++){
         x = searchForBoundary(x, frac, sdev);
         ret[i] = mean + x;
         ret[n - i - 1] = mean - x;
      }
      return ret;
   }

   /**
    * Search for the next boundary point
    * 
    * @param x starting location (may be negative infinity or previously computed value)
    * @param frac fraction of probability mass (0..1/2) that each gap should contain
    * @param sdev standard deviation of gaussian
    * @return location of the next boundary point
    */
   protected static double searchForBoundary(double x, double frac, double sdev)
   {
      final double EPS = 0.0001;
      assert (frac > 0 && frac <= 0.5 && sdev > 0);

      double y;
      double add = Math.max(sdev, 0.5);
      if (x == Library.NEGINF){
         x = -add * 3;
         y = 0;
         while(Library.gaussCDF(x, 0, sdev) >= frac){
            y = x;
            x -= add;
         }
      }
      else y = x + sdev;
      double base = Library.gaussCDF(x, 0, sdev);

      // find a value that is too far to the right
      while(Library.gaussCDF(y, 0, sdev) - base < frac)
         y += add;

      // now binary search to find the right value
      while(x + EPS < y){
         double m = (x + y) / 2;
         double v = Library.gaussCDF(m, 0, sdev);
         if (v - base > frac) y = m;
         else x = m;
      }

      return (x + y) / 2;
   }
}
