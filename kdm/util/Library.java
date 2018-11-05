package kdm.util;

import org.apache.commons.math.special.*;
import edu.cornell.lassp.houle.RngPack.*;
import java.text.*;
import java.util.regex.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import no.uib.cipr.matrix.*;
import javax.imageio.*;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import kdm.data.FeatureVec;
import kdm.data.Sequence;

import org.apache.commons.math.*;
import org.apache.commons.math.util.*;
import org.apache.commons.math.analysis.*;
import org.apache.commons.math.stat.regression.*;
import org.apache.commons.math.stat.StatUtils;
import com.sun.image.codec.jpeg.*;

import static kdm.util.TTResult.*;
import static java.util.Calendar.*;

public final class Library
{
   public final static Font smallSansFont = new Font("SansSerif", Font.PLAIN, 11);
   public final static Font normalMonoFont = new Font("Monospaced", Font.PLAIN, 12);
   public final static double MINUS_LOG_THRESH = -39.14;
   public final static int MAXIT = 100;
   public final static double EPS = 3.0e-7;
   public final static long LNAN = Long.MIN_VALUE;
   public final static double FPMIN = Double.MIN_VALUE;
   public final static double INF = Double.POSITIVE_INFINITY;
   public final static double NEGINF = Double.NEGATIVE_INFINITY;

   /** log(0.0) = -infinity */
   public final static double LOG_ZERO = NEGINF;
   /** log(1.0) = 0.0 */
   public final static double LOG_ONE = 0.0;

   public final static double LOG_TWO = Math.log(2.0);
   public final static double MINV_ABS = 1.0e-9;
   public final static double TWO_PI = 2.0 * Math.PI;
   public final static double SQRT_2PI = Math.sqrt(TWO_PI);
   public final static double SQRT_2 = Math.sqrt(2.0);

   public final static TimeZone utc = TimeZone.getTimeZone("UTC");
   public final static SimpleDateFormat sdf = getSDF("yyyy MMM d HH:mm:ss");
   public final static DecimalFormat df = new DecimalFormat();
   public static Ranmar rng;
   protected static GraphicsEnvironment ge = null;
   protected static GraphicsDevice gs = null;
   protected static GraphicsConfiguration gc = null;
   public final static long AppStartTime = getTime();

   public static enum MatrixOrder {
      RowMajor, ColumnMajor
   }

   static{
      df.setMaximumFractionDigits(4);
      rng = new Ranmar(getTime());
   }

   public static JFrame popup(String title, Component comp, int width, int height)
   {
      JFrame frame = new JFrame(title);
      frame.setSize(width, height);
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.add(comp);
      frame.setVisible(true);
      return frame;
   }
   
   /** @return default graphics configuration */
   public static GraphicsConfiguration getGC()
   {
      if (gc == null){
         ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
         gs = ge.getDefaultScreenDevice();
         gc = gs.getDefaultConfiguration();
      }
      return gc;
   }

   /**
    * Return the current time in milliseconds. This function just forwards the request to
    * System.currentTimeMillis();
    * 
    * @return the current time in milliseconds (since midnight Jan 1, 1970, UTC)
    */
   public static long getTime()
   {
      return System.currentTimeMillis();
   }

   /**
    * @return current time (UTC) with first day of week set to sunday
    */
   public static Calendar now()
   {
      Calendar cal = Calendar.getInstance(utc);
      cal.setFirstDayOfWeek(SUNDAY);
      return cal;
   }

   /**
    * @return calendar object representing the given date
    */
   public static Calendar date2cal(Date date)
   {
      Calendar cal = now();
      cal.setTimeInMillis(date.getTime());
      return cal;
   }

   /**
    * @param day day of month
    * @param month month of year (January = 1)
    * @param year Gregorian year
    * @return milliseconds since the epoch for the specified date
    */
   public static long getTime(int day, int month, int year)
   {
      Calendar cal = now();
      cal.setLenient(true);
      cal.set(Calendar.DATE, day);
      cal.set(Calendar.MONTH, month - 1);
      cal.set(Calendar.YEAR, year);
      return cal.getTimeInMillis();
   }

   /** @return string representation of the given time */
   public static String formatTime(long ms)
   {
      return sdf.format(new Date(ms));
   }

   /** @return human readable string representing the given amount of time */
   public static String formatDuration(long ms)
   {
      return formatDuration(ms, 1);
   }

   /** @return human readable string representing the given amount of time */
   public static String formatDuration(long ms, int nSigDig)
   {
      boolean bNeg = (ms < 0);
      ms = Math.abs(ms);

      double second = 1000.0;
      double minute = 60 * second;
      double hour = 60 * minute;
      double day = 24 * hour;
      double week = 7 * day;
      double month = 30.4167 * day;
      double year = 365 * day;

      String sNum = String.format("%%.%df", nSigDig);
      String sRet;
      if (ms > year) sRet = String.format(sNum + " years", ms / year);
      else if (ms > month) sRet = String.format(sNum + " months", ms / month);
      else if (ms > week) sRet = String.format(sNum + " weeks", ms / week);
      else if (ms > day) sRet = String.format(sNum + " days", ms / day);
      else if (ms > hour) sRet = String.format(sNum + " hours", ms / hour);
      else if (ms > minute) sRet = String.format(sNum + " min", ms / minute);
      else if (ms > second) sRet = String.format(sNum + "s", ms / second);
      else sRet = String.format("%dms", ms);

      if (bNeg) sRet = "-" + sRet;

      return sRet;
   }

   /**
    * Convert a string to the time it represents yyyy MM dd hh mm ss[.s*] yyyy MM dd hh mm ss uuu
    * 
    * @return time represented by the given string
    */
   public static Calendar str2cal(String s)
   {
      String sRexSeconds = "(\\d{4}).+?(\\d{2}).+?(\\d{2}).+?(\\d{2}).+?(\\d{2}).+?(\\d{2}(?:\\.\\d+)?)";
      String sRexMS = "(\\d{4}).+?(\\d{2}).+?(\\d{2}).+?(\\d{2}).+?(\\d{2}).+?(\\d{2}).+?(\\d{3})";

      Pattern pat = Pattern.compile(sRexSeconds);
      Matcher m = pat.matcher(s);
      if (m.find()){
         int year = Integer.parseInt(m.group(1));
         int month = Integer.parseInt(m.group(2)) - 1;
         int day = Integer.parseInt(m.group(3));
         int hour = Integer.parseInt(m.group(4));
         int minute = Integer.parseInt(m.group(5));
         double seconds = Double.parseDouble(m.group(6));
         int ms = (int)Math.round((seconds - Math.floor(seconds)) * 1000);

         Calendar cal = now();
         cal.set(year, month, day, hour, minute, (int)seconds);
         cal.setTimeInMillis(cal.getTimeInMillis() + ms);
         return cal;
      }

      pat = Pattern.compile(sRexMS);
      m = pat.matcher(s);
      if (m.find()){
         int year = Integer.parseInt(m.group(1));
         int month = Integer.parseInt(m.group(2)) - 1;
         int day = Integer.parseInt(m.group(3));
         int hour = Integer.parseInt(m.group(4));
         int minute = Integer.parseInt(m.group(5));
         int seconds = Integer.parseInt(m.group(6));
         int ms = Integer.parseInt(m.group(7));

         Calendar cal = now();
         cal.set(year, month, day, hour, minute, seconds);
         cal.setTimeInMillis(cal.getTimeInMillis() + ms);
         return cal;
      }

      return null;
   }

   /** @return sdf for the given format set to UTC */
   public static SimpleDateFormat getSDF(String sFormat)
   {
      SimpleDateFormat sdf = new SimpleDateFormat(sFormat);
      sdf.setTimeZone(utc);
      return sdf;
   }

   /** @return same string with special characters converted for XML */
   public static String str2xml(String s)
   {
      StringBuffer sb = new StringBuffer();
      int n = s.length();
      for(int i = 0; i < n; i++){
         char c = s.charAt(i);
         switch(c){
         case '&':
            sb.append("&amp;");
            break;
         case '<':
            sb.append("&lt;");
            break;
         case '>':
            sb.append("&gt;");
            break;
         case '"':
            sb.append("&quot;");
            break;
         case '\'':
            sb.append("&apos;");
            break;
         default:
            sb.append(c);
         }
      }
      return sb.toString();
   }

   /**
    * Load the given class, if not found, try pre-pending the package
    */
   public static Class getClass(String sClass, String sPackage) throws ClassNotFoundException
   {
      try{
         return Class.forName(sClass);
      } catch (ClassNotFoundException cnfe){}

      StringTokenizer st = new StringTokenizer(sPackage, ", ");
      while(st.hasMoreTokens()){
         try{
            return Class.forName(Library.forceSuffix(st.nextToken(), ".") + sClass);
         } catch (ClassNotFoundException e){}
      }

      throw new ClassNotFoundException(String.format("class: %s  package: %s", sClass, sPackage));
   }

   /** Flip the bytes in the given int (big-endian to little-endian) */
   public static int flipBytes(int x)
   {
      int y = 0;
      y |= (x & 0xff) << 24;
      y |= ((x >> 8) & 0xff) << 16;
      y |= ((x >> 16) & 0xff) << 8;
      y |= (x >> 24) & 0xff;
      return y;
   }

   /** Flip the bytes in the given short (big-endian to little-endian) */
   public static short flipBytes(short x)
   {
      short y = 0;
      y |= (x & 0xff) << 8;
      y |= (x >> 8) & 0xff;
      return y;
   }

   /** @return two solutions to the given quadratic equation (null on error; img or inf) */
   public static double[] quadratic(double a, double b, double c)
   {
      if (a == 0) return null; // TODO return +/- inf as appropriate

      double x = b * b - 4 * a * c;
      if (x < 0) return null;

      double v = Math.sqrt(x);
      double twoA = 2.0 * a;
      double x1 = (-b + v) / twoA;
      double x2 = (-b - v) / twoA;
      return new double[] { x1, x2 };
   }

   /** @return volume of a 'dim'-dimensional hypersphere with given radius */
   public static double getVolumeSphere(double radius, double dim)
   {
      double a = Math.pow(Math.PI, dim/2.0)*Math.pow(radius, dim);
      double b = Math.exp(Gamma.logGamma(dim/2.0+1.0));
      return a / b;
   }
   
   /** @return 2D rotation matrix [0 1; 2 3] for rotation through theta (in radians) */
   public static double[] get2DRotation(double theta)
   {
      double s = Math.sin(theta);
      double c = Math.cos(theta);
      return new double[] { c, -s, s, c };
   }

   /**
    * Turn the anti-aliasing hint on or to default value for the given graphics context
    */
   public static void setAntiAlias(Graphics2D g, boolean bOn)
   {
      if (bOn) g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      else g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);      
   }

   /**
    * Create a file chooser that accepts the given extensions
    * 
    * @param ext comma separated list of extensions
    * @param sDesc description for this filter
    * @return contructed file chooser
    */
   public static JFileChooser buildFileChooser(String ext, String sDesc)
   {
      return buildFileChooser(new String[] { ext, sDesc });
   }

   /**
    * Create a file chooser using the given extension filters
    * 
    * @param sInfo even elements are extension lists (eg, "jpg" or "jpg,gif,png") and odd elements are
    *           corresponding descriptions (eg, "JPEG images" or "All Images")
    * @return file chooser with given extension filters
    */
   public static JFileChooser buildFileChooser(String sInfo[])
   {
      JFileChooser fc = new JFileChooser();
      for(int i = 0; i < sInfo.length; i += 2){
         final String sExts = sInfo[i];
         final String sDesc = sInfo[i + 1];
         fc.addChoosableFileFilter(new FileFilter() {
            public boolean accept(File f)
            {
               if (f.isDirectory()) return true;
               String ext = Library.getExt(f.getName());
               if (ext == null) return false;
               String exts[] = sExts.split("\\,");
               for(int i = 0; i < exts.length; i++)
                  if (Library.stricmp(ext, exts[i])) return true;
               return false;
            }

            public String getDescription()
            {
               return sDesc;
            }
         });
      }
      return fc;
   }

   /**
    * Ensure the given array list has the given size
    * 
    * @param a array list to adjust
    * @param n desired size
    */
   public static void forceSize(ArrayList a, int n)
   {
      if (n < 1) a.clear();
      else{
         int m = a.size();
         while(n > m){
            a.add(null);
            m++;
         }
         while(n < m)
            a.remove(--m);
         a.trimToSize();
      }
   }

   /**
    * Fill the given array list with the given object
    * 
    * @param a array list to fill
    * @param o filler object
    */
   public static void fill(ArrayList a, Object o)
   {
      int n = a.size();
      for(int i = 0; i < n; i++)
         a.set(i, o);
   }

   /**
    * @return size of the default screen
    */
   public static Dimension getScreenSize()
   {
      return Toolkit.getDefaultToolkit().getScreenSize();
   }

   /** @return (pow^y) : y is an integer and (pow^y) <= x */
   public static double calcSmallerPow(double x, double pow) {
      double y = Math.log(x) / Math.log(pow);
      return Math.pow(pow, Math.floor(y));
   }
   
   /** @return "pretty number" for the given base that is less than or equal to x */
   public static double getPrettyNumberSmaller(double x, double range, double base)
   {
      base = calcSmallerPow(range, base);
      return base * Math.floor(x / base);
   }
   
   /** @return "pretty number" for the given base that is greater than or equal to x */
   public static double getPrettyNumberLarger(double x, double range, double base)
   {
      base = calcSmallerPow(range, base);
      return base * Math.ceil(x / base);
   }
   
   /**
    * Center the given component and change its size to be the given proportion of the parent
    * 
    * @param comp component to resize and center
    * @param parent parent container
    * @param percent fraction of width/height of parent to consume (eg: 0.5)
    */
   public static void centerSizeWin(Component comp, Component parent, double percent)
   {
      Dimension pardim = (parent == null ? getScreenSize() : parent.getSize());
      comp.setSize((int)Math.round(pardim.width * percent), (int)Math.round(pardim.height * percent));
      Dimension dim = comp.getSize();
      comp.setLocation((pardim.width - dim.width) / 2, (pardim.height - dim.height) / 2);
   }

   /**
    * Center the given component in the given parent
    */
   public static void centerWin(Component comp, Component parent)
   {
      Dimension dim = comp.getSize();
      Point parloc = (parent == null ? new Point(0,0) : parent.getLocationOnScreen());
      Dimension pardim = (parent == null ? getScreenSize() : parent.getSize());
      comp.setLocation(parloc.x + (pardim.width - dim.width) / 2, parloc.y + (pardim.height - dim.height) / 2);
   }

   /** @return the factorial of the given integer */
   public static long fact(int x)
   {
      return MathUtils.factorial(x);
   }

   /** @return number of combinations -- n choose k */
   public static long choose(int n, int k)
   {
      return MathUtils.binomialCoefficient(n, k);
   }

   /** @return number of permutations */
   public static long perm(int a, int b)
   {
      assert (a >= b);
      long x = 1;
      for(int i = (a - b) + 1; i <= a; i++)
         x *= i;
      return x;
   }

   /**
    * Compute the value of a 1D Gaussian
    * 
    * @param x location for computation
    * @param u mean of Gaussian
    * @param var variance of Gaussian
    * @return value of distribution at 'x'
    */
   public static double gaussV(double x, double u, double var)
   {
      double a = 1.0 / (SQRT_2PI * Math.sqrt(var));
      double y = x - u;
      double b = -0.5 * y * y / var;
      return a * Math.exp(b);
   }

   /**
    * Compute the value of a 1D Gaussian
    * 
    * @param x location for computation
    * @param u mean of Gaussian
    * @param sdev stadard deviation of Gaussian
    * @return value of distribution at 'x'
    */
   public static double gaussSD(double x, double u, double sdev)
   {
      double a = 1.0 / (SQRT_2PI * sdev);
      double y = x - u;
      double ys = y / sdev;
      double b = -0.5 * ys * ys;
      return a * Math.exp(b);
   }

   /**
    * Compute the log of a 1D Gaussian
    * 
    * @param x location for computation
    * @param u mean of Gaussian
    * @param var variance of Gaussian
    * @return log of distribution at 'x'
    */
   public static double logGaussV(double x, double u, double var)
   {
      double a = -Math.log(SQRT_2PI * Math.sqrt(var));
      double y = x - u;
      double b = -0.5 * y * y / var;
      return a + b;
   }

   /**
    * Compute the log of a 1D Gaussian
    * 
    * @param x location for computation
    * @param u mean of Gaussian
    * @param sdev standard deviation of Gaussian
    * @return log of distribution at 'x'
    */
   public static double logGaussSD(double x, double u, double sdev)
   {
      double a = -Math.log(SQRT_2PI * sdev);
      double y = x - u;
      double ys = y / sdev;
      double b = -0.5 * ys * ys;
      return a + b;
   }

   public static double gaussCDF(double x, double u, double sdev)
   {
      return 0.5 * (1.0 + erf((x - u) / (sdev * SQRT_2)));
   }

   /**
    * Create a smooth (Gaussian) kernel
    * 
    * @param w width of kernel (total, so w/2 per side)
    * @return the values of the (sampled) kernel
    */
   public static double[] buildSmoothKernel(int w)
   {
      double k[] = new double[w];
      double sdev = (w - 1) / (2.0 * 2.5); // 2.5 stdevs, 2 sides
      int n = w - 1;
      double mean = n / 2.0;
      for(int i = 0; i <= n - i; i++)
         k[i] = k[n - i] = gaussSD(i, mean, sdev);
      Library.normalize(k);
      return k;
   }

   /** @return estimate optimal bandwidth for gaussian kernel (from Silverman 1986) */
   public static double bandwidth(double sdev, int n)
   {
      return 1.06 * sdev * Math.pow(n, -0.2);
   }

   /**
    * Compute the weighted mean of the given data
    * 
    * @param w weights, w[i]>=0
    * @param x data
    * @return weighted mean
    */
   public static double mean(double[] w, double[] x)
   {
      assert (w.length == x.length);
      double wsum = 0;
      double xsum = 0;
      for(int i = 0; i < w.length; i++){
         assert (w[i] >= 0);
         xsum += w[i] * x[i];
         wsum += w[i];
      }
      if (wsum <= 0){
         System.err.printf("Warning: can't compute weighted mean with zero/negative weight sum (%f)!\n",
               wsum);
         return Double.NaN;
      }
      return xsum / wsum;
   }

   /**
    * Compute the variance of the weighted data given the mean.
    * 
    * @param w weight for each data point, w[i]>=0
    * @param x data points
    * @param mean mean of data points
    * @return variance of <i>x</i> around <i>mean</i>
    */
   public static double var(double[] w, double[] x, double mean)
   {
      assert (w.length == x.length);

      // calc variance using the "corrected two-pass algorithm" adapted from nr (eq 14.1.8)
      double a = 0;
      double b = 0;
      double wsum = 0;
      for(int i = 0; i < w.length; i++){
         assert (w[i] >= 0);
         double dx = x[i] - mean;
         double y = w[i] * dx;
         wsum += w[i];
         a += y * dx;
         b += y;
      }
      return (a - b * b / wsum) / (wsum - wsum / w.length);
   }

   /**
    * Compute erf(x) quickly (fractional error less than 1.2 * 10 ^ -7).
    */
   public static double erf(double z)
   {
      double t = 1.0 / (1.0 + 0.5 * Math.abs(z));

      // use Horner's method
      double ans = 1
            - t
            * Math
                  .exp(-z
                        * z
                        - 1.26551223
                        + t
                        * (1.00002368 + t
                              * (0.37409196 + t
                                    * (0.09678418 + t
                                          * (-0.18628806 + t
                                                * (0.27886807 + t
                                                      * (-1.13520398 + t
                                                            * (1.48851587 + t
                                                                  * (-0.82215223 + t * (0.17087277))))))))));
      if (z >= 0) return ans;
      else return -ans;
   }

   /**
    * Uses the jakarta commons command. This is slower but (probably) more accurate than Library.erf().
    * 
    * @param x value at which to compute erf
    * @return erf(x) or NaN if convergence fails
    * @throws MathException
    */
   public static double erf2(double x)
   {
      try{
         return org.apache.commons.math.special.Erf.erf(x);
      } catch (MathException e){
         System.err.println("** Erf failed ***\n");
         return Double.NaN;
      }
   }

   /**
    * Calculate the closest integer to <code>val</code> that is >= <code>min</code> and a multiple of
    * <code>div</code>.
    * 
    * @param val the value to approximate
    * @param min minimum return value
    * @param div return must be a multiple of this
    * @return closest valid int to <code>val</code>
    */
   public static int closestNum(int val, int min, int div)
   {
      double f = (double)val / div;
      return Math.max(min, (int)Math.round(f) * div);
   }

   /**
    * Swap the width and height fields
    */
   public static void swap(Dimension dims)
   {
      int t = dims.width;
      dims.width = dims.height;
      dims.height = t;
   }

   /**
    * Swap the x and y fields
    */
   public static void swap(Point p)
   {
      int t = p.x;
      p.x = p.y;
      p.y = t;
   }

   /**
    * Sleep for the specified number of ms. This method simply calls Thread.sleep() and catches the
    * Interrupted Exception.
    * 
    * @param millis number of milliseconds to sleep
    * @return true only if the sleep was not interrupted
    */
   public static boolean sleep(long millis)
   {
      try{
         Thread.sleep(millis);
      } catch (InterruptedException ie){
         return false;
      }
      return true;
   }

   /** @return gray color with the given luminence */
   public static Color makeGray(int v)
   {
      return new Color(v, v, v);
   }

   /**
    * Generate n random colors that are distinguishable
    * 
    * @param n number of colors to generate
    * @return array of n distinguishable colors
    */
   public static Color[] generateColors(int n)
   {
      int nBaseMax = 24; // from generateColors(int,float)
      Color[] colors = new Color[n];
      if (n <= nBaseMax){
         if (n <= 12) return generateColors(n, 1.0f);
         Color[] a = generateColors(12, 1.0f);
         Library.copy(a, colors);
         a = generateColors(n - 12, 0.3f);
         Library.copy(a, colors, 0, 12, n - 12);
      }
      else{
         int nColors = 0;
         while(nColors < n){
            Color[] a = generateColors(Math.min(n - nColors, nBaseMax), 1.0f);
            copy(a, colors, 0, nColors, a.length);
            nColors += a.length;
            if (nColors < n){
               int na = Math.min(n - nColors, nBaseMax);
               a = generateColors(na, 0.2f);
               copy(a, colors, 0, nColors, a.length);
            }
         }
      }
      return colors;
   }

   /**
    * Generate n random colors that are distinguishable using the given saturation
    * 
    * @param n number of colors to generate
    * @param vSat saturation to use
    * @return array of n distinguishable colors
    */
   protected static Color[] generateColors(int n, float vSat)
   {
      assert (n <= 24) : "sorry: a max of 24 colors is supported"; // TODO
      Color[] colors = new Color[n];
      int i = 0;
      for(; i < n && i < 3; i++)
         colors[i] = Color.getHSBColor(i / 3.0f, vSat, 1.0f);
      for(; i < n && i < 6; i++)
         colors[i] = Color.getHSBColor(i / 3.0f + 1.0f / 6.0f, vSat, 1.0f);
      for(; i < n && i < 9; i++)
         colors[i] = Color.getHSBColor(i / 3.0f, vSat, 0.4f);
      for(; i < n && i < 12; i++)
         colors[i] = Color.getHSBColor(i / 3.0f + 1.0f / 6.0f, vSat, 0.4f);
      for(; i < n && i < 18; i++)
         colors[i] = Color.getHSBColor(i / 6.0f + 1.0f / 12.0f, vSat, 1.0f);
      for(; i < n && i < 24; i++)
         colors[i] = Color.getHSBColor(i / 6.0f + 1.0f / 12.0f, vSat, 0.4f);
      return colors;
   }

   /**
    * Ensure that the path has a trailing separator (local OS dependent)
    * 
    * @param sPath the path that may or may not have a trailing separator
    * @return the same path with a trailing separator
    */
   public static String ensurePathSep(String sPath)
   {
      return forceSuffix(sPath, File.separator);
   }

   /**
    * Enusre that the path uses unix style seperators ("/" instead of "\")
    * 
    * @param sPath the original path
    * @return same as sPath, but guaranteed to only use unix style directory separators
    */
   public static String ensureUnixPath(String sPath)
   {
      return sPath.replace('\\', '/');
   }

   /**
    * Enusre that the path uses windows style seperators ("\" instead of "\")
    * 
    * @param sPath the original path
    * @return same as sPath, but guaranteed to only use windows style directory separators
    */
   public static String ensureWindowsPath(String sPath)
   {
      return sPath.replace('/', '\\');
   }

   /**
    * Return the given string after ensuring that it starts with the given prefix. (e.g., (foo, bar) -> barfoo |
    * (foobar, foo) -> foobar | (oobar, foo) -> foooobar note from the last example that this method is not
    * smart enough to detect partial matches.
    * 
    * @param s the base string
    * @param pre the prefix
    * @return the string w/ the prefix added - if appropriate
    */
   public static String forcePrefix(String s, String pre)
   {
      if (s.startsWith(pre)) return s;
      else return (pre + s);
   }

   /**
    * Forces the string to end with the given suffix.<br>
    * (e.g., (foo, bar) -> foobar<br>
    * (foobar, bar) -> foobar<br>
    * (fooba, bar) -> foobabar<br>
    * 
    * note from the last example that this method does not attempt to detect partial matches.
    * 
    * @param s the base string
    * @param suf the suffix
    * @return the string w/ the given suffix appended (if it did not end w/ it already)
    */
   public static String forceSuffix(String s, String suf)
   {
      if (s.endsWith(suf)) return s;
      else return (s + suf);
   }

   /**
    * Return the given string wrapped in quotes. The string will not be double quoted. (e.g., (foo -> "foo") |
    * ("foo -> "foo") | (" -> ");
    * 
    * @param s the base string
    * @return s with added quote marks if necessary
    */
   public static String quoteAsNeeded(String s)
   {
      return forcePrefix(forceSuffix(s, "\""), "\"");
   }

   /**
    * Return the given string wrapped in quotes. The string will be double quoted. (e.g., (foo -> "foo") |
    * ("foo -> ""foo") | (" -> """);
    * 
    * @param s the base string
    * @return s with surrounding quote marks
    */
   public static String quote(String s)
   {
      return "\"" + s + "\"";
   }

   /**
    * Return the canonical version of this file
    * 
    * @param f the file of interest
    * @return the canonical version or null if there is an error
    */
   public static File getCanonical(File f)
   {
      if (f == null) return null;
      try{
         return f.getCanonicalFile();
      } catch (IOException e){
         return null;
      }
   }

   /**
    * Return the canonical version of this path
    * 
    * @param path the path of interest
    * @return the canonical version or null if there is an error
    */
   public static String getCanonical(String path)
   {
      if (path == null) return null;
      try{
         return new File(path).getCanonicalPath();
      } catch (IOException e){
         return null;
      }
   }

   /**
    * Determines if 'a' and 'b' are equal by checking if compareTo() returns zero.
    */
   public static boolean eq(Comparable a, Comparable b)
   {
      return (a.compareTo(b) == 0);
   }

   /**
    * Case-insensitive string comparison
    * 
    * @return true if the strings are the same (regardless of case)
    */
   public static boolean stricmp(String a, String b)
   {
      return (a.compareToIgnoreCase(b) == 0);
   }

   /**
    * Generate a permutation of [0..n-1]. This is an implementation of the Fisher-Yates shuffling algorithm.
    * 
    * @param n upper bound (exclusive) on indices
    * @return array containing permuted indices
    */
   public static int[] permute(int n)
   {
      int a[] = new int[n];
      for(int i = 0; i < n; i++)
         a[i] = i;
      for(int i = 0; i < n; i++){
         // swap i with a random element in [i..n)
         int j = i + random(n - i);
         int t = a[i];
         a[i] = a[j];
         a[j] = t;
      }
      return a;
   }

   /**
    * Internal insertion sort routine for subarrays that is used by quicksort.
    * 
    * @param a an array of Comparable items.
    * @param low the left-most index of the subarray.
    * @param n the number of items to sort.
    */
   private static void insertionSort(double[] a, int[] ii, int low, int high)
   {
      for(int p = low + 1; p <= high; p++){
         double tmp = a[p];
         int j;
         for(j = p; j > low && tmp < a[j - 1]; j--){
            a[j] = a[j - 1];
            ii[j] = ii[j - 1];
         }
         a[j] = tmp;
         ii[j] = p;
      }
   }

   /**
    * Sort the given array and return the resulting indices
    * 
    * @param a data to sort
    * @return index of original location of data
    */
   public static int[] sort(double[] a)
   {
      int[] ii = new int[a.length];
      for(int i = 0; i < ii.length; i++)
         ii[i] = i;
      sort(a, ii, 0, a.length - 1);
      return ii;
   }

   /**
    * Internal quicksort method that makes recursive calls. Uses median-of-three partitioning and a cutoff of
    * 10.
    * 
    * @param a an array of Comparable items.
    * @param low the left-most index of the subarray.
    * @param high the right-most index of the subarray.
    */
   private static void sort(double[] a, int[] ii, int low, int high)
   {
      if (low + 10 > high) insertionSort(a, ii, low, high);
      else{
         // Sort low, middle, high
         int middle = (low + high) / 2;
         if (a[middle] < a[low]) swap(a, ii, low, middle);
         if (a[high] < a[low]) swap(a, ii, low, high);
         if (a[high] < a[middle]) swap(a, ii, middle, high);

         // Place pivot at position high - 1
         swap(a, ii, middle, high - 1);
         double pivot = a[high - 1];

         // Begin partitioning
         int i, j;
         for(i = low, j = high - 1;;){
            while(a[++i] < pivot){}
            while(pivot < a[--j]){}

            if (i >= j) break;
            swap(a, ii, i, j);
         }

         // Restore pivot
         swap(a, ii, i, high - 1);

         sort(a, ii, low, i - 1); // Sort small elements
         sort(a, ii, i + 1, high); // Sort large elements
      }
   }

   /**
    * Method to swap to elements in an array.
    * 
    * @param a an array of objects.
    * @param index1 the index of the first object.
    * @param index2 the index of the second object.
    */
   public static void swap(double[] a, int[] ii, int index1, int index2)
   {
      double tmp = a[index1];
      a[index1] = a[index2];
      a[index2] = tmp;

      int itmp = ii[index1];
      ii[index1] = ii[index2];
      ii[index2] = itmp;
   }

   /**
    * Select a random subset of (unique) indices
    * 
    * @param nNeeded size of subset
    * @param nAvail total range of indices to select from ([0..nAvail-1])
    * @return list of selected indices
    */
   public static int[] selectRandomIndices(int nNeeded, int nAvail)
   {
      if (nNeeded > nAvail) return null;

      // create and initialize the indices
      int j, ii[] = new int[nNeeded];

      if (nNeeded > nAvail / 2) // better to select missing indices
      {
         for(int i = 0; i < ii.length; i++)
            ii[i] = i;
         int missing[] = selectRandomIndices(nAvail - nNeeded, nAvail);
         int m = nNeeded;
         for(int i = 0; i < ii.length; i++){
            while(contains(missing, ii[i]))
               ii[i] = m++;
         }
         shuffle(ii);
      }
      else{ // better to select chosen indices
         Arrays.fill(ii, -1);
         for(int i = 0; i < ii.length; i++){
            do{
               j = random(nAvail);
            } while(contains(ii, j));
            ii[i] = j;
         }
      }

      return ii;
   }

   /**
    * Determine if 'x' is in 'a'
    * 
    * @param a array of ints
    * @param x int to seach for
    * @return true if x is in a
    */
   public static boolean contains(int a[], int x)
   {
      for(int i = 0; i < a.length; i++)
         if (a[i] == x) return true;
      return false;
   }

   /**
    * Randomly permute the given array
    * 
    * @return permutation indices (maps original index to new index)
    */
   public static int[] shuffle(int a[])
   {
      int ii[] = permute(a.length);
      shuffle(a, ii);
      return ii;
   }

   /**
    * Randomly permute the given array
    * 
    * @return permutation indices (maps original index to new index)
    */
   public static int[] shuffle(double a[])
   {
      int ii[] = permute(a.length);
      shuffle(a, ii);
      return ii;
   }

   /**
    * Randomly permute the given array
    * 
    * @return permutation indices (maps original index to new index)
    */
   public static int[] shuffle(Object a[])
   {
      int ii[] = permute(a.length);
      shuffle(a, ii);
      return ii;
   }

   /**
    * Re-order the elements of 'a' by the indices in 'ii'
    * 
    * @param ii permutation of [0..a.length-1]
    */
   public static void shuffle(int a[], int ii[])
   {
      int n = a.length;
      int b[] = a.clone();
      for(int i = 0; i < n; i++)
         a[i] = b[ii[i]];
   }

   /**
    * Re-order the elements of 'a' by the indices in 'ii'
    * 
    * @param ii a permutation of [0..a.length-1]
    */
   public static void shuffle(double a[], int ii[])
   {
      int n = a.length;
      double b[] = a.clone();
      for(int i = 0; i < n; i++)
         a[i] = b[ii[i]];
   }

   /**
    * Re-order the elements of 'a' by the indices in 'ii'
    * 
    * @param ii a permutation of [0..a.length-1]
    */
   public static void shuffle(Object a[], int ii[])
   {
      int n = a.length;
      Object b[] = a.clone();
      for(int i = 0; i < n; i++)
         a[i] = b[ii[i]];
   }

   /**
    * Return a random sample from the multinomial represented by 'a'.
    * 
    * @param a array of discrete probabilities for each bin. The elements of 'a' should sum to one.
    * @return index of a bin in 'a', selected proportional to its weight
    */
   public static int sample(double a[])
   {
      double v = random();
      double sum = 0.0;
      int n = a.length;
      for(int i = 0; i < n; i++){
         sum += a[i];
         if (v <= sum) return i;
      }
      return -1;
   }

   /**
    * Return a random sample from the multinomial represented by a.
    * 
    * @param a array of discrete probabilities for each bin. The 2D array is treated as if it were one long
    *           multinomial. All elements of 'a' should sum to one.
    * @return indices (i,j) of a bin in a, selected proportional to its weight
    */
   public static int[] sample(double a[][])
   {
      double v = random();
      double sum = 0.0;
      for(int i = 0; i < a.length; i++){
         for(int j = 0; j < a[i].length; j++){
            sum += a[i][j];
            if (v <= sum) return new int[] { i, j };
         }
      }
      assert false : "error drawing sample: sum=" + sum;
      return null;
   }

   /**
    * Reseed the random number generator
    */
   public static void reseed(long seed)
   {
      rng = new Ranmar(seed);
   }

   /**
    * @return random number in [0..1)
    */
   public static double random()
   {
      return rng.raw();
   }

   /**
    * Pick a random integer in [0..n-1]
    * 
    * @param n upper bound of uniform distribution (exclusive)
    * @return random itneger in [0..n-1]
    */
   public static int random(int n)
   {
      return (int)(random() * n);
   }

   /**
    * Computes the variance of a model based on the estimated variance, full variance (based on all of the
    * data), and the minimum variance. The blend is computed according to a sigmoid function that weights the
    * full variance more when n is small.
    * 
    * @param estv - estimated variance
    * @param n - number of data points used to compute estv
    * @param fullv - variance based on some other method (typically larger than estv)
    * @param minv - the minimum allowed variance (typically quite small)
    * @return blended variance based on sigmoid and n
    */
   public static double blendVar(double estv, double n, double fullv, double minv)
   {
      double a = 1.0 / 3.0;
      double b = 0.5;
      double nmax = 30.0; // by then, the model can take care of itself
      double alpha = 1.0 / (1.0 + Math.exp(a * (n - b * nmax)));
      double blendv = fullv * alpha + (1.0 - alpha) * estv;
      return Math.max(blendv, minv);
   }

   /**
    * @return L_2 (Euclidean) distance between the two vectors
    */
   public static double dist(double[] x, double[] y)
   {
      return Math.sqrt(dist2(x, y));
   }

   /**
    * @return squared L_2 (Euclidean) distance between the two vectors
    */
   public static double dist2(double[] x, double[] y)
   {
      double d = 0.0;
      for(int i = 0; i < x.length; i++){
         double v = x[i] - y[i];
         d += v * v;
      }
      return d;
   }

   /** @return index of the largest element in given array, -1 on error */
   public static int maxi(int[] a)
   {
      if (a == null || a.length == 0) return -1;
      int iBest = 0;
      for(int i = 1; i < a.length; i++)
         if (a[i] > a[iBest]) iBest = i;
      return iBest;
   }

   /** @return index of the largest element in given array, -1 on error */
   public static int maxi(double[] a)
   {
      if (a == null || a.length == 0) return -1;
      int iBest = 0;
      for(int i = 1; i < a.length; i++)
         if (a[i] > a[iBest]) iBest = i;
      return iBest;
   }

   public static int max(int a, int b)
   {
      return (a > b ? a : b);
   }

   public static int max(int a, int b, int c)
   {
      if (a > b) return (a > c ? a : c);
      else return (b > c ? b : c);
   }

   public static int min(int a, int b)
   {
      return (a < b ? a : b);
   }

   public static int min(int a, int b, int c)
   {
      if (a < b) return (a < c ? a : c);
      else return (b < c ? b : c);
   }

   public static double min(double a, double b, double c)
   {
      if (a < b) return (a < c ? a : c);
      else return (b < c ? b : c);
   }

   public static double max(double a, double b, double c)
   {
      if (a > b) return (a > c ? a : c);
      else return (b > c ? b : c);
   }

   /**
    * Computes the number of valid windows fow a sliding window setup.
    * 
    * @param seqlen length of sequence
    * @param wlen length of window
    * @param step number of sites to increment for each window (1 => every position)
    * @return number of sliding window sites
    */
   public static int getNumSlidingWindowSites(int seqlen, int wlen, int step)
   {
      return (int)Math.max(Math.ceil((double)(seqlen - wlen + 1) / step), 0);
   }

   /** @return log_2(x) */
   public static double log2(double x)
   {
      return Math.log(x) / LOG_TWO;
   }

   /** @return log(exp(x)+exp(y)) */
   public static double logadd(double x, double y)
   {
      if (Double.isNaN(x) || Double.isNaN(y)) return Double.NaN;
      if (x==Library.NEGINF && y==Library.NEGINF) return Library.NEGINF;
      if (x < y){
         double t = x;
         x = y;
         y = t;
      }
      double z = y - x;      
      if (z < MINUS_LOG_THRESH) return x;
      else return x + Math.log1p(Math.exp(z));
   }

   /** @return log(exp(x)-exp(y)) */
   public static double logsub(double x, double y)
   {
      if (Double.isNaN(x) || Double.isNaN(y)) return Double.NaN;            
      if (x == y) return LOG_ZERO;
      double z = y - x;
      if (z < MINUS_LOG_THRESH) return x;
      return x + Math.log1p(-Math.exp(z));
   }

   public static void mul(double[] x, double v)
   {
      for(int i = 0; i < x.length; i++)
         x[i] *= v;
   }

   public static void add(double[] x, double v)
   {
      for(int i = 0; i < x.length; i++)
         x[i] += v;
   }

   public static void norm(double[] x)
   {
      mul(x, 1.0 / Math.sqrt(length2(x)));
   }

   public static double length2(double[] x)
   {
      return dot(x, x);
   }

   public static double dot(double[] x, double[] y)
   {
      double d = 0.0;
      for(int i = 0; i < x.length; i++)
         d += x[i] * y[i];
      return d;
   }

   public static void sub(double v[], double[] x, double[] y)
   {
      for(int i = 0; i < x.length; i++)
         v[i] = x[i] - y[i];
   }

   public static void add(double v[], double[] x, double[] y)
   {
      for(int i = 0; i < x.length; i++)
         v[i] = x[i] + y[i];
   }

   public static void copy(int from[], int[] to)
   {
      assert (from.length <= to.length);
      for(int i = 0; i < from.length; i++)
         to[i] = from[i];
   }

   public static void copy(long from[], long[] to)
   {
      assert (from.length <= to.length);
      for(int i = 0; i < from.length; i++)
         to[i] = from[i];
   }

   public static void copy(long from[], long[] to, int iStartFrom, int iStartTo, int len)
   {
      for(int i = 0; i < len; i++)
         to[i + iStartTo] = from[i + iStartFrom];
   }

   public static void copy(short from[], short[] to)
   {
      assert (from.length <= to.length);
      for(int i = 0; i < from.length; i++)
         to[i] = from[i];
   }

   public static void copy(double from[], double[] to)
   {
      int n = Math.min(from.length, to.length);
      for(int i = 0; i < n; i++)
         to[i] = from[i];
   }

   public static void copy(double from[], double[] to, int iStartFrom, int iStartTo, int len)
   {
      for(int i = 0; i < len; i++)
         to[i + iStartTo] = from[i + iStartFrom];
   }

   public static void copy(Object from[], Object[] to)
   {
      assert (from.length <= to.length);
      for(int i = 0; i < from.length; i++)
         to[i] = from[i];
   }

   public static void copy(Object from[], Object[] to, int iStartFrom, int iStartTo, int len)
   {
      for(int i = 0; i < len; i++)
         to[i + iStartTo] = from[i + iStartFrom];
   }

   public static double[] extract(double[] a, int iStart, int iEnd)
   {
      double[] ret = new double[iEnd - iStart];
      for(int i = iStart; i < iEnd; i++)
         ret[i - iStart] = a[i];
      return ret;
   }

   /**
    * Adds the new dimensions to the existing data structure.
    * 
    * @param base existing data ([iTime][iDim])
    * @param newDims new data to add ([iTime][iDim])
    */
   public static double[][] appendDims(double[][] base, double[][] newDims)
   {
      int na = (base == null ? 0 : base[0].length);
      int nb = newDims[0].length;
      int len = newDims.length;

      double[][] ret = new double[len][na + nb];

      if (base != null){
         for(int i = 0; i < len; i++)
            for(int j = 0; j < na; j++)
               ret[i][j] = base[i][j];
      }

      for(int i = 0; i < len; i++){
         for(int j = 0; j < nb; j++)
            ret[i][na + j] = newDims[i][j];
      }

      return ret;
   }

   public static double sum(double[] a)
   {
      double sum = 0.0;
      for(int i = 0; i < a.length; i++)
         sum += a[i];
      return sum;
   }

   public static int sum(int[] a)
   {
      int sum = 0;
      for(int i = 0; i < a.length; i++)
         sum += a[i];
      return sum;
   }

   public static int[] findMax(double[][] a)
   {
      int i, j, p, q;
      p = q = 0;
      double score = a[p][q];
      for(i = 0; i < a.length; i++){
         for(j = 0; j < a[i].length; j++){
            if (a[i][j] > score){
               p = i;
               q = j;
               score = a[p][q];
            }
         }
      }
      return new int[] { p, q };
   }

   /**
    * Returns the qualified path by prefixing the base path to the given path if necessary.
    * 
    * examples (base path = /home/david):<br>
    * ./foo/bar.html -> /home/david/foo/bar.html<br>
    * foo/bar.html -> /home/david/foo/bar.html<br>
    * /foo/bar.html -> /foo/bar.html (no change)<br>
    */
   public static String qualifyPath(String sPath, File fBase)
   {
      File f = new File(fBase, sPath);
      try{
         return f.getCanonicalPath();
      } catch (IOException e){
         return f.getAbsolutePath();
      }
   }

   /**
    * Returns the qualified path by prefixing the base path to the given path if necessary.
    * 
    * examples (base path = /home/david):<br>
    * ./foo/bar.html -> /home/david/foo/bar.html<br>
    * foo/bar.html -> /home/david/foo/bar.html<br>
    * /foo/bar.html -> /foo/bar.html (no change)<br>
    */
   public static String qualifyPath(String sPath, String sBase)
   {
      if (sBase == null) return sPath;

      char c1 = sPath.charAt(0);

      if (c1 == '.' && !sPath.startsWith("..")){
         // just a dot -> base path
         if (sPath.length() == 1) return sBase;

         // ./ or .\ -> replace with base path
         char c2 = sPath.charAt(1);
         if (c2 == '/' || c2 == '\\') return sBase + sPath.substring(2);

         // .X -> file, so just return sPath
         return sPath;
      }
      else if (c1 == '/' || c1 == '\\' || (Character.isLetter(c1) && sPath.substring(1, 3).equals(":\\"))){
         return sPath;
      }
      else return sBase + sPath;
   }

   /**
    * Returns the path portion of a fully specified file.
    * 
    * example: /foo/bar/jon/test.html -> /foo/bar/jon/
    */
   public static String getPath(String sPath)
   {
      int i = (int)Math.max(sPath.lastIndexOf('\\'), sPath.lastIndexOf('/'));
      if (i < 0) return "";
      else return sPath.substring(0, i + 1);
   }

   /**
    * Returns the file portion of a fully specified file.
    * 
    * example: /foo/bar/jon/test.html -> test.html
    */
   public static String getFileName(String sPath)
   {
      int i = (int)Math.max(sPath.lastIndexOf('\\'), sPath.lastIndexOf('/'));
      if (i < 0) return sPath;
      else return sPath.substring(i + 1);
   }

   /**
    * Returns the file title (file name w/o extension) of a fully specified file.
    * 
    * for example: /foo/bar/test.html -> test; /foo/bar/.test.html -> .test
    */
   public static String getTitle(String sPath)
   {
      String s = getFileName(sPath);
      int i = s.lastIndexOf('.'); // TODO should this be lastIndexOf? maybe a param?
      if (i < 0) return s;
      if (i == 0){
         int j = s.indexOf('.', 1);
         if (j <= 1) return s;
         else return s.substring(0, j);
      }
      else return s.substring(0, i);
   }

   /**
    * Returns the suffix (everything after the title) of a fully specified file.
    * 
    * for example: /foo/bar/test.html -> html; /foo/bar/test.data.html -> data.html
    * 
    * @return suffix of file name, empty string if none
    */
   public static String getSuffix(String sPath)
   {
      String title = getTitle(sPath);
      int i = sPath.indexOf(title) + title.length();
      String s = sPath.substring(i);
      if (s.charAt(0) == '.') s = s.substring(1);
      return s;
   }

   /**
    * Returns the extension of a fully specified file.
    * 
    * for example: /foo/bar/test.html -> html /foo/bar/test.data.html -> html
    */
   public static String getExt(String sPath)
   {
      int i = sPath.lastIndexOf('.');
      if (i < 0) return "";
      return sPath.substring(i + 1);
   }

   public static File[] getFilesWild(String sPath, String sFilePat)
   {
      try{
         sPath = ensurePathSep(sPath) + getPath(sFilePat);
         // System.err.println("orig spath: "+sPath);
         sPath = ensurePathSep(new File(sPath).getCanonicalPath());
         // System.err.println("canon spath: "+sPath);
         sFilePat = getFileName(sFilePat);
         sPath = getPath(sPath);
         // System.err.println("spath: "+sPath);
         // System.err.println("sFilePat: "+sFilePat);
         File[] files = new File(sPath).listFiles(new WildFilenameFilter(sFilePat));
         // System.err.println("Found: "+files.length+" files");
         if (files != null) Arrays.sort(files);
         return files;
      } catch (IOException ioe){
         ioe.printStackTrace();
      }
      return new File[0];
   }

   /**
    * @return next non-empty line or null if EOF
    */
   public static String readLine(BufferedReader in) throws IOException
   {
      String line;
      while((line = in.readLine()) != null){
         line = line.trim();
         if (line.length() == 0) continue;
         return line;
      }
      return null;
   }

   /**
    * Read data from a text file with one reading per line
    * 
    * @param sPath location of file
    * @return double array containing data
    */
   public static double[] read1D(String sPath)
   {
      try{
         BufferedReader in = new BufferedReader(new FileReader(sPath));
         MyDoubleList data = new MyDoubleList();
         String line;
         while((line = in.readLine()) != null)
            data.add(Double.parseDouble(line));
         in.close();
         return data.toArray();
      } catch (IOException ioe){
         ioe.printStackTrace();
         return null;
      }
   }

   /**
    * Read data from a space or comma delimited text file
    * 
    * @param sPath location of file
    * @param mo RowMajor or ColumnMajor order
    * @return 2D double array containing data
    */
   public static double[][] read(String sPath, MatrixOrder mo)
   {
      try{
         BufferedReader in = new BufferedReader(new FileReader(sPath));
         ArrayList<double[]> data = new ArrayList<double[]>();
         String line;
         int n = 0;
         while((line = in.readLine()) != null){
            StringTokenizer st = new StringTokenizer(line, ",\t ");
            if (n == 0) n = st.countTokens();
            double y[] = new double[n];
            for(int i = 0; i < n; i++)
               y[i] = Double.parseDouble(st.nextToken());
            data.add(y);
         }
         in.close();

         int m = data.size(); // number of rows
         double x[][];
         if (mo == MatrixOrder.RowMajor){
            x = new double[m][n];
            for(int i = 0; i < m; i++)
               x[i] = data.get(i);
         }
         else{ // column major order
            x = new double[n][m];
            for(int i = 0; i < n; i++){
               x[i] = new double[m];
               for(int j = 0; j < m; j++)
                  x[i][j] = data.get(j)[i];
            }
         }
         return x;
      } catch (IOException ioe){
         ioe.printStackTrace();
         return null;
      }
   }

   /** @return x with dimensions switched (i.e., transposed) */
   public static double[][] transpose(double[][] x)
   {
      double[][] y = allocMatrixDouble(x[0].length, x.length);
      for(int i = 0; i < x.length; i++)
         for(int j = 0; j < y.length; j++)
            y[j][i] = x[i][j];
      return y;
   }

   public static double[] allocVectorDouble(int n, double init)
   {
      double[] v = new double[n];
      Arrays.fill(v, init);
      return v;
   }

   public static int[] allocVectorInt(int n, int init)
   {
      int[] v = new int[n];
      Arrays.fill(v, init);
      return v;
   }

   public static double[][] allocMatrixDouble(int nRows, int nCols)
   {
      return allocMatrixDouble(nRows, nCols, 0.0);
   }

   public static double[][] allocMatrixDouble(double[][] a, int nRows, int nCols)
   {
      return allocMatrixDouble(a, nRows, nCols, 0.0);
   }

   public static double[][] allocMatrixDouble(double[][] a, int nRows, int nCols, double init)
   {
      if (a != null && a.length >= nRows && a[0].length >= nCols){
         for(int i = 0; i < nRows; i++)
            Arrays.fill(a[i], init);
         return a;
      }
      return allocMatrixDouble(nRows, nCols, init);
   }

   public static double[][] allocMatrixDouble(int nRows, int nCols, double init)
   {
      double a[][] = new double[nRows][nCols];
      for(int i = 0; i < nRows; i++)
         Arrays.fill(a[i], init);
      return a;
   }

   public static int[][] allocMatrixInt(int nRows, int nCols)
   {
      return allocMatrixInt(nRows, nCols, 0);
   }

   public static int[][] allocMatrixInt(int[][] a, int nRows, int nCols)
   {
      return allocMatrixInt(a, nRows, nCols, 0);
   }

   public static int[][] allocMatrixInt(int[][] a, int nRows, int nCols, int init)
   {
      if (a != null && a.length >= nRows && a[0].length >= nCols){
         for(int i = 0; i < nRows; i++)
            Arrays.fill(a[i], init);
         return a;
      }
      return allocMatrixInt(nRows, nCols, init);
   }

   public static int[][] allocMatrixInt(int nRows, int nCols, int init)
   {
      int a[][] = new int[nRows][nCols];
      for(int i = 0; i < nRows; i++)
         Arrays.fill(a[i], init);
      return a;
   }

   public static byte[][] allocMatrixByte(int nRows, int nCols)
   {
      return allocMatrixByte(nRows, nCols, (byte)0);
   }

   public static byte[][] allocMatrixByte(byte[][] a, int nRows, int nCols)
   {
      return allocMatrixByte(a, nRows, nCols, (byte)0);
   }

   public static byte[][] allocMatrixByte(byte[][] a, int nRows, int nCols, byte init)
   {
      if (a != null && a.length >= nRows && a[0].length >= nCols){
         for(int i = 0; i < nRows; i++)
            Arrays.fill(a[i], init);
         return a;
      }
      return allocMatrixByte(nRows, nCols, init);
   }

   public static byte[][] allocMatrixByte(int nRows, int nCols, byte init)
   {
      byte a[][] = new byte[nRows][nCols];
      for(int i = 0; i < nRows; i++)
         Arrays.fill(a[i], init);
      return a;
   }

   public static short[][] allocMatrixShort(int nRows, int nCols)
   {
      return allocMatrixShort(nRows, nCols, (short)0);
   }

   public static short[][] allocMatrixShort(int nRows, int nCols, short init)
   {
      short a[][] = new short[nRows][];
      for(int i = 0; i < nRows; i++){
         a[i] = new short[nCols];
         Arrays.fill(a[i], init);
      }
      return a;
   }

   /** @return sum of the sequence m, m+1, ... n-1, n */
   public static long sumseq(int m, int n)
   {
      assert (m <= n);
      return (long)(n - m + 1) * (n + m) / 2;
   }

   /** @return true if sum of data is 1.0 */
   public static boolean isNormalized(double data[])
   {
      double sum = 0.0;
      for(int i = 0; i < data.length; i++)
         sum += data[i];
      return (Math.abs(sum - 1) < 0.000001);
   }

   /**
    * Normalize array of data sets so that the overall sum is 1.
    */
   public static void normalize(double data[][])
   {
      double sum = 0.0;
      for(int i = 0; i < data.length; i++)
         for(int j = 0; j < data[i].length; j++)
            sum += data[i][j];
      assert (sum > 0.0);

      for(int i = 0; i < data.length; i++)
         for(int j = 0; j < data[i].length; j++)
            data[i][j] /= sum;
   }

   /**
    * Normalize array of data sets so that each row sums to 1.
    */
   public static void normalizeRows(double data[][])
   {
      for(int i = 0; i < data.length; i++)
         normalize(data[i]);
   }

   /**
    * Normalize a data set so that the sum is 1.
    */
   public static boolean normalize(double data[])
   {
      double sum = 0.0;
      for(int i = 0; i < data.length; i++)
         sum += data[i];
      if (Double.isNaN(sum) || sum <= 0){
         Arrays.fill(data, 0);
         return false;
      }
      for(int i = 0; i < data.length; i++)
         data[i] /= sum;
      return true;
   }

   /** @return square of the input value */
   public static double sqr(double x)
   {
      return x * x;
   }

   /** @return value of 'a' with sign equivalent to 'b' */
   public static double sign(double a, double b)
   {
      return (b >= 0) ? Math.abs(a) : -Math.abs(a);
   }

   /**
    * convert a string to an int array
    * 
    * @param s string to convert
    * @return int array representation (s.charAt(i) - 'a')
    */
   public static int[] str2IntArr(String s)
   {
      int[] a = new int[s.length()];
      for(int i = 0; i < a.length; i++)
         a[i] = (int)(s.charAt(i) - 'a');
      return a;
   }

   /**
    * convert an int array to a string
    * 
    * @param a the int array to convert
    * @return string representation of the int array ('a' + array[i])
    */
   public static String intArr2Str(int[] a)
   {
      StringBuffer sb = new StringBuffer();
      for(int i = 0; i < a.length; i++)
         sb.append((char)('a' + a[i]));
      return sb.toString();
   }

   /**
    * Find the array index and offset given a linear offset and a list of array lengths.
    * 
    * @param ix linear offset
    * @param lens list of array lengths
    * @param ret use this for return value (2 ints), allocate if null
    * @return [array index, offset], null if none
    * @see getIndexFromArrayOffset inverse operation
    */
   public static int[] getArrayOffset(int ix, int[] lens, int[] ret)
   {
      if (ret == null) ret = new int[2];
      for(int i = 0; i < lens.length; i++){
         if (ix < lens[i]){
            ret[0] = i;
            ret[1] = ix;
            return ret;
         }
         ix -= lens[i];
      }
      ret[0] = ret[1] = -1;
      return null;
   }

   /**
    * Compute the "flat index" from an array index and local offset
    * 
    * @param iArray index of current array
    * @param iOffset offset in the current array
    * @param lens list of array lengths
    * @return corresponding index
    * @see getArrayOffset inverse operation
    */
   public static int getIndexFromArrayOffset(int iArray, int iOffset, int[] lens)
   {
      int index = 0;
      for(int i = 0; i < iArray; i++)
         index += lens[i];
      return index + iOffset;
   }

   /** @return x/2 rounded to nearest odd value */
   public static int getOddHalf(int x)
   {
      int y = x / 2;
      if (x % 2 == 0) return y;
      if (y % 2 == 0) return y + 1;
      return y;
   }

   /**
    * Computes the student's t value for two data sets with (assumed to be) equal true variances. Code ported
    * from NR in C, Section 14.2.
    */
   public static TTResult ttest(double data1[], double data2[], Tails tails)
   {
      // TODO: use jakarta math implementation instead
      double n1 = (double)data1.length;
      double n2 = (double)data2.length;
      double ave1 = StatUtils.mean(data1);
      double ave2 = StatUtils.mean(data2);
      double var1 = StatUtils.variance(data1);
      double var2 = StatUtils.variance(data2);
      return ttest(ave1, ave2, var1, var2, n1, n2, tails);
   }

   /**
    * Computes the student's t value for two data sets with (assumed to be) equal true variances, given their
    * mean, variance, and sample size. Code ported from NR in C, Section 14.2.
    */
   public static TTResult ttest(double ave1, double ave2, double var1, double var2, double n1,
         double n2, Tails tails)
   {
      double df = n1 + n2 - 2;
      double svar = ((n1 - 1) * var1 + (n2 - 1) * var2) / df;
      double t = (ave1 - ave2) / Math.sqrt(svar * (1.0 / n1 + 1.0 / n2));
      double p = betai(0.5 * df, 0.5, df / (df + t * t));
      if (tails == Tails.One) p *= 0.5;
      return new TTResult(t, p, tails);
   }

   /**
    * Computes the student's t value for two data sets with (assumed to be) unequal true variances. Code
    * ported from NR in C, Section 14.2.
    */
   public static TTResult tutest(double data1[], double data2[], Tails tails)
   {
      double n1 = (double)data1.length;
      double n2 = (double)data2.length;
      double ave1 = StatUtils.mean(data1);
      double ave2 = StatUtils.mean(data2);
      double var1 = StatUtils.variance(data1);
      double var2 = StatUtils.variance(data2);

      return tutest(ave1, ave2, var1, var2, n1, n2, tails);
   }

   /**
    * Computes the student's t value for two data sets with (assumed to be) unequal true variances, given
    * their mean, variance, and sample size. Code ported from NR in C, Section 14.2.
    */
   public static TTResult tutest(double ave1, double ave2, double var1, double var2, double n1,
         double n2, Tails tails)
   {
      double t = (ave1 - ave2) / Math.sqrt(var1 / n1 + var2 / n2);
      double df = sqr(var1 / n1 + var2 / n2) / (sqr(var1 / n2) / (n1 - 1) + sqr(var2 / n2) / (n2 - 1));
      double p = betai(0.5 * df, 0.5, df / (df + sqr(t)));
      if (tails == Tails.One) p *= 0.5;
      return new TTResult(t, p, tails);
   }

   /**
    * Computes the incomplete beta function. Code ported from NR in C, Section 6.4.
    */
   public static double betai(double a, double b, double x)
   {
      assert x >= 0.0 && x <= 1.0 : "x=" + x + " -- should be in [0,1]";

      double bt;
      if (x == 0.0 || x == 1.0) bt = 0.0;
      else bt = Math.exp(Gamma.logGamma(a + b) - Gamma.logGamma(a) - Gamma.logGamma(b) + a * Math.log(x) + b
            * Math.log(1.0 - x));
      if (x < (a + 1.0) / (a + b + 2.0)) return bt * betacf(a, b, x) / a;
      else return 1.0 - bt * betacf(b, a, 1.0 - x) / b;
   }

   /**
    * Computes the continued fraction for the incomplete beta function by modified Lentz's method. Code ported
    * from NR in C, Section 6.4.
    */
   public static double betacf(double a, double b, double x)
   {
      int m, m2;
      double aa, c, d, del, h, qab, qam, qap;

      qab = a + b;
      qap = a + 1.0;
      qam = a - 1.0;
      c = 1.0;
      d = 1.0 - qab * x / qap;
      if (Math.abs(d) < FPMIN) d = FPMIN;
      d = 1.0 / d;
      h = d;
      for(m = 1; m <= MAXIT; m++){
         m2 = 2 * m;
         aa = m * (b - m) * x / ((qam + m2) * (a + m2));
         d = 1.0 + aa * d;
         if (Math.abs(d) < FPMIN) d = FPMIN;
         c = 1.0 + aa / c;
         if (Math.abs(c) < FPMIN) c = FPMIN;
         d = 1.0 / d;
         h *= d * c;
         aa = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2));
         d = 1.0 + aa * d;
         if (Math.abs(d) < FPMIN) d = FPMIN;
         c = 1.0 + aa / c;
         if (Math.abs(c) < FPMIN) c = FPMIN;
         d = 1.0 / d;
         del = d * c;
         h *= del;
         if (Math.abs(del - 1.0) < EPS) break;
      }
      assert (m <= MAXIT) : "a or b too big, or MAXIT too small in betacf";
      return h;
   }

   /**
    * Read an image from a file
    * 
    * @param path the location of the image file
    * @return null on failure
    */
   public static BufferedImage readImage(String path)
   {
      try{
         return readImage(new FileInputStream(path), getExt(path));
      } catch (FileNotFoundException e){
         return null;
      }
   }

   /**
    * Read an image from an input stream
    * 
    * @param in the input stream for the image resource
    * @param sExt the extension of the file (jpg, gif, png, etc.)
    * @return the image or null if there was an error
    */
   public static BufferedImage readImage(InputStream in, String sExt)
   {
      if (sExt.equalsIgnoreCase("jpg") || sExt.equalsIgnoreCase("jpeg")) return readJpeg(in);

      try{
         Iterator it = ImageIO.getImageReadersBySuffix(sExt);
         ImageReader reader = (ImageReader)it.next();
         reader.setInput(ImageIO.createImageInputStream(in), true);
         return reader.read(0);
      } catch (Exception e){
         System.err.printf("Load exception: %s\n", e);
         return null;
      }
   }

   /**
    * Read a jpeg from an input stream
    * 
    * @param in the input stream for the jpeg
    * @return the image or null if there was an error
    */
   public static BufferedImage readJpeg(InputStream in)
   {
      try{
         JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(in);
         return decoder.decodeAsBufferedImage();
      } catch (Exception e){
         return null;
      }
   }

   /** @return the power of 2 at least as large as the input */
   public static int getNextPow2(int x)
   {
      double y = log2(x);
      int yi = (int)Math.ceil(y);
      return (1 << yi);
   }

   /** @return m^{-1}, that is, the inverse of the matrix m */
   public static Matrix invert(DenseMatrix m)
   {
      int n = m.numRows();
      assert (n == m.numColumns());
      DenseMatrix I = Matrices.identity(n);
      DenseMatrix AI = I.copy();
      return m.solve(I, AI);
   }

   /** Compute the correlation between the input data */
   public static double corr(double[] x, double[] y)
   {
      int n = x.length;
      assert (n == y.length);
      SimpleRegression reg = new SimpleRegression();
      for(int i = 0; i < n; i++)
         reg.addData(x[i], y[i]);
      return reg.getR();
   }

   /**
    * Find the i'th smallest value in a[p..r]; random_select method from CLR c10.2
    * 
    * @param a array of data
    * @param p start of search range (inclusive)
    * @param r end of search range (inclusive)
    * @param i find the i'th smallest value (1, 2, 3, ... n)
    * @return i'th smallest value in a[p..r]
    */
   public static int select(double a[], int p, int r, int i)
   {
      if (p == r) return p;
      int q = randomPartition(a, p, r);
      int k = q - p + 1;
      if (i == k) return q;
      else if (i < k) return select(a, p, q - 1, i);
      else return select(a, q + 1, r, i - k);
   }

   /** helper function for select() */
   protected static int randomPartition(double[] a, int p, int r)
   {
      int rand = p + random(r - p + 1);
      double temp = a[rand];
      a[rand] = a[r];
      a[r] = temp;
      double x = a[r]; // choose pivot value
      int i = p - 1;
      for(int j = p; j <= r - 1; j++){
         if (a[j] <= x){
            i++;
            temp = a[i];
            a[i] = a[j];
            a[j] = temp;
         }
      }
      temp = a[i + 1];
      a[i + 1] = a[r];
      a[r] = temp;
      return i + 1;
   }

   /** @return the text at the given url */
   public static String getWebPage(String sUrl)
   {
      try{
         URL url = new URL(sUrl);
         BufferedInputStream in = new BufferedInputStream(url.openStream());
         StringBuffer sb = new StringBuffer();
         while(true){
            int data = in.read();
            if (data == -1) break;
            else sb.append((char)data);
         }
         return sb.toString();
      } catch (Exception e){
         return null;
      }
   }

   /**
    * Find the "knee in the curve" using the method at http://www.minnen.org/david/research/knee/ Note that
    * this method will sort the given data.
    * 
    * @param a data in which to find the knee
    * @return expected value of knee (fractional index)
    */
   public static double findKnee(double[] a, boolean bSort)
   {
      int n = a.length;
      if (bSort) Arrays.sort(a);
      double[] da = new double[n - 1];
      for(int i = 1; i < n; i++)
         da[i - 1] = Math.abs(a[i] - a[i - 1]); // TODO abs?
      return findMeanWeightedIndex(da);
   }

   /** @return mean weighted index => treat elements of 'a' as weights and find E(i)=sum(a[i]*i)/sum(i) */
   public static double findMeanWeightedIndex(double[] a)
   {
      double wsum = 0, sum = Math.abs(a[0]);
      for(int i = 1; i < a.length; i++){
         sum += a[i];
         wsum += a[i] * i;
      }
      return wsum / sum;
   }

   /** for library debugging */
   public static void main(String args[]) throws Exception
   {
      double[] a = new double[] { 5, 4, 3, 2, 1 };
      int i = select(a, 0, a.length - 1, 1);
      System.err.printf("i=%d  a[i]=%.1f\n", i, a[i]);
   }
}
