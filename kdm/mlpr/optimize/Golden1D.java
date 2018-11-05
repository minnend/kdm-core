package kdm.mlpr.optimize;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.*;

/** Implements the Golden Section method for 1D minimization (based on NR 10.1) */
public class Golden1D
{
   /**
    * Solve a 1D minimization problem using golden section search
    * 
    * From NR: Given a function f, and given a bracketing triplet of abscissas ax, bx, cx (such that bx is
    * between ax and cx, and f(bx) is less than both f(ax) and f(cx)), this routine performs a golden section
    * search for the minimum, isolating it to a fractional precision of about tol. The abscissa of the minimum
    * is returned as xmin, and the minimum function value is returned as golden, the returned function value.
    * 
    * @throws FunctionEvaluationException
    */
   public final static double minimize(double ax, double bx, double cx, UnivariateRealFunction func,
         double tol) throws FunctionEvaluationException
   {
      final double R = 0.61803399;
      final double C = 1.0 - R;

      double f1, f2, x0, x1, x2, x3;
      x0 = ax;
      x3 = cx;
      if (Math.abs(cx - bx) > Math.abs(bx - ax)){
         x1 = bx;
         x2 = bx + C * (cx - bx);
      }
      else{
         x2 = bx;
         x1 = bx - C * (bx - ax);
      }
      f1 = func.value(x1);
      f2 = func.value(x2);
      while(Math.abs(x3 - x0) > tol * (Math.abs(x1) + Math.abs(x2))){
         if (f2 < f1){
            x0 = x1;
            x1 = x2;
            x2 = R * x1 + C * x3;
            f1 = f2;
            f2 = func.value(x2);
         }
         else{
            x3 = x2;
            x2 = x1;
            x1 = R * x2 + C * x0;
            f2 = f1;
            f1 = func.value(x1);
         }
      }
      if (f1 < f2) return x1;
      else return x2;
   }

}
