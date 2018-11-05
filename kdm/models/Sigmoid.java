package kdm.models;

import kdm.mlpr.optimize.Golden1D;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.UnivariateRealFunction;

/** represents a 1D sigmoid: f(x) = 1/(1+exp(A(x+B))) */
public class Sigmoid
{
   protected double A,B;
   
   public Sigmoid()
   {
      this(-1,0);      
   }
   
   public Sigmoid(double A, double B)
   {
      set(A,B);
   }
   
   public Sigmoid(Sigmoid sig)
   {
      this.copyFrom(sig);
   }
   
   public void copyFrom(Sigmoid sig)
   {
      this.A = sig.A;
      this.B = sig.B;
   }
   
   public void set(double A, double B)
   {
      this.A = A;
      this.B = B;      
   }
   
   public void setA(double A)
   {
      this.A = A;
   }
   
   public void setB(double B)
   {
      this.B = B;
   }
   
   public double eval(double x)
   {
      return 1.0 / (1 + Math.exp(A*(x+B)));
   }
   
   /** @return sum of squared errors */
   public double sumSqrErr(double[] x, double[] y)
   {
      double sumerr = 0;
      for(int i = 0; i < x.length; i++){
         double pred = eval(x[i]);
         double err = y[i] - pred;
         sumerr += err*err;
      }
      return sumerr;
   }
   
   public void learn(final double[] x, final double[] y, double minB, double maxB)
   {
      // TODO use a proper multidim optimization!
      double err = sumSqrErr(x, y);
      final Sigmoid sig = new Sigmoid(this);
      while(true){
         UnivariateRealFunction funcA = new UnivariateRealFunction() {
            public double value(double A) throws FunctionEvaluationException
            {         
               sig.setA(A);
               return sig.sumSqrErr(x, y);
            }
         };
         double A2, B2;
         try{
            A2 = Golden1D.minimize(-100, A, 0.001, funcA, 0.00001);
         } catch (FunctionEvaluationException e){
            System.err.println(e);
            break;
         }
         
         sig.setA(A2);
         UnivariateRealFunction funcB = new UnivariateRealFunction() {
            public double value(double B) throws FunctionEvaluationException
            {                 
               sig.setB(B);
               return sig.sumSqrErr(x, y);
            }
         };
         try{
            B2 = Golden1D.minimize(minB, B, maxB, funcB, 0.00001);
         } catch (FunctionEvaluationException e){
            System.err.println(e);
            break;
         }
         
         sig.setB(B2);
         double e2 = sig.sumSqrErr(x, y);
         if (e2 < err) copyFrom(sig);
         if (err - e2 < 0.0001) break;
         err = e2;
      }
   }
   
   public String toString()
   {
      return String.format("[A=%.3f B=%.3f]", A, B);
   }
}
