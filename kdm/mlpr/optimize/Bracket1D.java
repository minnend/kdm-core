package kdm.mlpr.optimize;

import kdm.util.*;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.*;

/** find valid bracket triplet for a 1D function; from NR 10.1 */
public class Bracket1D
{
   /** default ratio by which successive intervals are magnified */
   public static final double GOLD = 1.618034;

   /** maximum magnification allowed for a parabolic-fit step */

   public static final double GLIMIT = 100.0;
   public static final double TINY = 1.0e-20;

   public double ax, bx, cx;

   /**
    * Find a valid bracket triplet for the given 1D function
    * 
    * From NR: Given a function func, and given distinct initial points ax and bx, this routine searches in
    * the downhill direction (defined by the function as evaluated at the initial points) and returns new
    * points ax, bx, cx that bracket a minimum of the function. Also returned are the function values at the
    * three points, fa, fb, and fc.
    */
   public void bracket(double ax0, double bx0, UnivariateRealFunction func)
         throws FunctionEvaluationException
   {
      ax = ax0;
      bx = bx0;
      double ulim, u, r, q, fu, dum;
      double fa = func.value(ax);
      double fb = func.value(bx);
      if (fb > fa){ // Switch roles of a and b so that we can go downhill in the direction from a to b.
         dum = ax;
         ax = bx;
         bx = dum;
         dum = fb;
         fb = fa;
         fa = dum;
      }
      cx = (bx) + GOLD * (bx - ax); // First guess for c.
      double fc = func.value(cx);
      while(fb > fc){ // Keep returning here until we bracket.
         r = (bx - ax) * (fb - fc); // Compute u by parabolic extrapolation from a, b, c. TINY is used to
                                    // prevent any possible division by zero.
         q = (bx - cx) * (fb - fa);
         u = (bx) - ((bx - cx) * q - (bx - ax) * r)
               / (2.0 * Library.sign(Math.max(Math.abs(q - r), TINY), q - r));
         ulim = bx + GLIMIT * (cx - bx); // We won’t go farther than this. Test various possibilities:
         if ((bx - u) * (u - cx) > 0.0){ // Parabolic u is between b and c: try it.
            fu = func.value(u);
            if (fu < fc){ // Got a minimum between b and c.
               ax = bx;
               bx = u;
               fa = fb;
               fb = fu;
               return;
            }
            else if (fu > fb){ // Got a minimum between between a and u.
               cx = u;
               fc = fu;
               return;
            }
            u = cx + GOLD * (cx - bx); // Parabolic fit was no use. Use default magnification.
            fu = func.value(u);
         }
         else if ((cx - u) * (u - ulim) > 0.0){ // Parabolic fit is between c and its allowed limit.
            fu = func.value(u);
            if (fu < fc){
               bx = cx;
               cx = u;
               u = cx + GOLD * (cx - bx);
               fb = fc;
               fc = fu;
               fu = func.value(u);
            }
         }
         else if ((u - ulim) * (ulim - cx) >= 0.0){ // Limit parabolic u to maximum allowed value.
            u = ulim;
            fu = func.value(u);
         }
         else{ // Reject parabolic u, use default magnification.
            u = cx + GOLD * (cx - bx);
            fu = func.value(u);
         }
         ax = bx;
         bx = cx;
         cx = u;
         fa = fb;
         fb = fc;
         fc = fu;
      }
   }
}
