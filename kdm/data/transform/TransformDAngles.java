package kdm.data.transform;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.UnivariateRealFunction;

import kdm.mlpr.optimize.*;
import kdm.data.*;
import kdm.models.*;
import kdm.util.*;

/**
 * Transform a data set by computing the angle from one point to the next for each dimension. The class can
 * compute the time scale (mapping of one time step to a value commensurate with the data) by searching for
 * value that maximizes the entropy of the transformed data, or you can supply your own value for each
 * dimension.
 */
public class TransformDAngles extends DataTransform
{
   public static enum TSMETHOD {
      MaxEntropy, Average
   }

   public static final int NUM_BINS = 7; // used to calc entropy via histogram
   public static final double PiOver2 = Math.PI / 2;

   protected boolean bCalcDT;
   protected double[] dt;

   /** Construct a transform for arbitrary data; the proper time scale will be calculated automatically */
   public TransformDAngles()
   {
      bCalcDT = true;
   }

   /** Construct a transform for 1D data using the given time scale */
   public TransformDAngles(double _dt)
   {
      dt = new double[] { _dt };
      bCalcDT = false;
   }

   /** Construct a transform for N-D data using the given time scale */
   public TransformDAngles(double[] _dt)
   {
      dt = _dt;
      bCalcDT = false;
   }

   public void dumpParams()
   {
      System.err.printf("%s: bCalcDT=%b\n", getClass(), bCalcDT);
   }
   
   /**
    * Compute the best (maximum entropy) time scale for each dimension
    * 
    * @param seq data to transform
    * @param nSymbols number of symbols to use for computing entropy (via histogram)
    * @return time scale for each dimension of input data
    */
   public static double[] calcTimeScale(Sequence seq, final int nSymbols, TSMETHOD tsmethod)
   {
      int nDims = seq.getNumDims();
      double[] ret = new double[nDims];
      final double[] edges = Histogram.genEdges(-PiOver2, PiOver2, NUM_BINS);
      final int T = seq.length();

      for(int d = 0; d < nDims; d++)
      {
         if (tsmethod == TSMETHOD.MaxEntropy)
         {
            final double data[] = seq.extractDim(d);
            UnivariateRealFunction objFunc = new UnivariateRealFunction() {
               public double value(double dt) throws FunctionEvaluationException
               {
                  double[] angles = TransformDAngles.transform(data, dt);
                  Histogram hist = new Histogram(angles, edges);
                  // return neg entropy for minimization = max of actual entropy
                  return -hist.entropy();
               }
            };
            try
            {
               // TODO: should bracket more intelligently and use better opt function (see NR c10.1, 10.2)
               ret[d] = Golden1D.minimize(0.0001, 5.0, 1000.0, objFunc, 0.005);
            } catch (FunctionEvaluationException e)
            {
               e.printStackTrace();
               return null;
            }
         }
         else
         {
            // average abs change to estimate dt
            double sum = 0; 
            for(int t=1; t<T; t++)
               sum += Math.abs(seq.get(t,d)-seq.get(t-1,d));
            ret[d] = sum / (T-1);
         }
      }

      return ret;
   }

   /**
    * Tranform the data by calculating the angle from one point to the next
    * 
    * @param data data to transform
    * @param dt length of one time step
    * @return transformed data (length is one less than original)
    */
   public static double[] transform(double[] data, double dt)
   {
      int T = data.length;
      double[] ret = new double[T - 1];
      for(int i = 1; i < T; i++)
      {
         double dx = data[i] - data[i - 1];
         ret[i - 1] = Math.atan2(dx, dt);
         assert (ret[i - 1] >= -PiOver2 && ret[i - 1] <= PiOver2) : String.format(
               "ang=%.4f  dx=%.4f  dt=%.4f", ret[i - 1], dx, dt);
      }
      return ret;
   }

   @Override
   public Sequence transform(Sequence data)
   {
      if (bCalcDT)
      {
         dt = calcTimeScale(data, NUM_BINS, TSMETHOD.MaxEntropy);
         System.err.print("Calculated time scales: ");
         for(int i = 0; i < dt.length; i++)
            System.err.printf("%d=%.3f ", i + 1, dt[i]);
         System.err.println();
      }
      int nDims = dt.length;
      assert (nDims == data.getNumDims());
      Sequence ret = new Sequence("dAng: " + data.getName(), data.getFreq(), data.getStartMS());
      double raw[][] = new double[nDims][];
      for(int d = 0; d < nDims; d++)
      {
         double[] dim = data.extractDim(d);
         raw[d] = transform(dim, dt[d]);
      }
      ret.setDataByRow(raw);
      ret.copyMeta(data);
      return ret;
   }

}
