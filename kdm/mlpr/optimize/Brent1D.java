package kdm.mlpr.optimize;

import kdm.util.*;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.*;

/**
 * Implements Brent's parabolic minimization in 1D that doesn't require derivative estimates (based on NR
 * 10.2)
 */
public class Brent1D
{
   /** maximum allowed number of iterations */
   public static int ITMAX = 100;

   /** CGOLD is the golden ratio */
   public static final double CGOLD = 0.3819660;

   /**
    * small number that protects against trying to achieve fractional accuracy for a minimum that happens to
    * be exactly zero
    */
   public static final double ZEPS = 1.0e-10;

   /*
    * Minimize a 1D function using Brent's parabolic method
    * 
    * From NR: Given a function f, and given a bracketing triplet of abscissas ax, bx, cx (such that bx is
    * between ax and cx, and f(bx) is less than both f(ax) and f(cx)), this routine isolates the minimum to a
    * fractional precision of about tol using Brent’s method. The abscissa of the minimum is returned as xmin,
    * and the minimum function value is returned as brent, the returned function value.
    */
   public final static double minimize(double ax, double bx, double cx, UnivariateRealFunction func,
         double tol) throws FunctionEvaluationException
   {
      int iter;
      double a, b, d = 0, etemp, fu, fv, fw, fx, p, q, r, tol1, tol2, u, v, w, x, xm;
      double e = 0.0; // this will be the distance moved on the step before last.
      a = (ax < cx ? ax : cx); // a and b must be in ascending order,
      b = (ax > cx ? ax : cx); // but input abscissas need not be.
      x = w = v = bx; // Initializations...
      fw = fv = fx = func.value(x);
      for(iter = 1; iter <= ITMAX; iter++){ // Main program loop.
         xm = 0.5 * (a + b);
         tol2 = 2.0 * (tol1 = tol * Math.abs(x) + ZEPS);
         if (Math.abs(x - xm) <= (tol2 - 0.5 * (b - a))) return x; // Test for done here.
         if (Math.abs(e) > tol1){ // Construct a trial parabolic fit.
            r = (x - w) * (fx - fv);
            q = (x - v) * (fx - fw);
            p = (x - v) * q - (x - w) * r;
            q = 2.0 * (q - r);
            if (q > 0.0) p = -p;
            q = Math.abs(q);
            etemp = e;
            e = d;
            if (Math.abs(p) >= Math.abs(0.5 * q * etemp) || p <= q * (a - x) || p >= q * (b - x)) d = CGOLD
                  * (e = (x >= xm ? a - x : b - x));
            // The above conditions determine the acceptability of the parabolic fit. Here we
            // take the golden section step into the larger of the two segments.
            else{
               d = p / q; // Take the parabolic step.
               u = x + d;
               if (u - a < tol2 || b - u < tol2) d = Library.sign(tol1, xm - x);
            }
         }
         else{
            d = CGOLD * (e = (x >= xm ? a - x : b - x));
         }
         u = (Math.abs(d) >= tol1 ? x + d : x + Library.sign(tol1, d));
         fu = func.value(u);
         // This is the one function evaluation per iteration.
         if (fu <= fx){ // Now decide what to do with our function evaluation.
            if (u >= x) a = x;
            else b = x;
            v = w;
            w = x;
            x = u;
            fv = fw;
            fw = fx;
            fx = fu;
         }
         else{
            if (u < x) a = u;
            else b = u;
            if (fu <= fw || w == x){
               v = w;
               w = u;
               fv = fw;
               fw = fu;
            }
            else if (fu <= fv || v == x || v == w){
               v = u;
               fv = fu;
            }
         } // Done with housekeeping. Back for another iteration.
      }
      System.err.println("Warning: too many iterations in Brent 1D minimizer.");
      return x;
   }
   
   public static void main(String[] args) throws Exception
   {
      UnivariateRealFunction func = new UnivariateRealFunction(){

         public double value(double i) throws FunctionEvaluationException
         {
            return Math.sin(i/250)*Math.cos(i/100)+Math.sin(-i/100)*Math.cos(-i/200);
         }         
      };
      
      Bracket1D brack = new Bracket1D();
      brack.bracket(0, 100, func);
      System.err.printf("bracket: %.1f  %.1f  %.1f\n", brack.ax, brack.bx, brack.cx);
      
      TimerMS timer = new TimerMS();

      timer.reset();
      for(int i=0; i<1000; i++){
         //double x = Golden1D.minimize(0, 100, 600, func, .00001);
         double x = Golden1D.minimize(brack.ax, brack.bx, brack.cx, func, .00001);
         if (i==0) System.err.printf("golden: x=%.2f\n", x);
      }
      long a = timer.time();
      System.err.printf("golden: %dms\n", a);
      
      timer.reset();
      for(int i=0; i<1000; i++){         
         //double x = Brent1D.minimize(0, 100, 600, func, .00001);
         double x = Brent1D.minimize(brack.ax, brack.bx, brack.cx, func, .00001);
         if (i==0) System.err.printf("brent: x=%.2f\n", x);
      }
      a = timer.time();
      System.err.printf("brent: %dms\n", a);
      
   }
}
