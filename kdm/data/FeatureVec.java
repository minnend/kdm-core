package kdm.data;

import kdm.util.*;
import kdm.io.*;
import java.util.*;
import java.io.*;
import kdm.models.*;
import no.uib.cipr.matrix.*;

/** represents a vector in R^n */
public final class FeatureVec extends Datum
{
   public static final String KeyTimestamp = "FV.TimeStamp";
   /** true = compare data for hashCode and equality test; false = defer to Object's implementation (memory based) */
   public static boolean bDataComp = true;
   
   /** actual data */
   protected double vec[];

   /**
    * Create a feature vec from the double array
    * 
    * @param _vec data for feature vec
    */
   public FeatureVec(double _vec[])
   {
      vec = _vec.clone();
   }

   /**
    * Duplicate the given feature vec
    * 
    * @param fv feature vec to duplicate
    */
   public FeatureVec(FeatureVec fv)
   {
      copyFrom(fv);
   }

   /** Create a zero vector with nDims dimensions */
   public FeatureVec(int nDims)
   {
      vec = new double[nDims];
   }
   
   /**
    * Create a feature vector with 'nDims' using the supplied values; if too few are given, the last one is
    * repeated (or zero if no values are provided).
    */
   public FeatureVec(int nDims, double... x)
   {
      vec = new double[nDims];

      int N = x.length;
      int M = Math.min(nDims, N);
      for(int i = 0; i < M; i++)
         vec[i] = x[i];
      double rep = (N>0 ? x[N-1] : 0);
      for(int i = M; i < nDims; i++)
         vec[i] = rep;
   }

   /**
    * Get data as a MTJ vector
    * 
    * @param bCopy copy the data or just reference the array?
    * @return MTJ vector representation
    */
   public DenseVector getMTJVec(boolean bCopy)
   {
      return new DenseVector(get(), bCopy);
   }

   /** set the time stamp of this point (ms) */
   public void setTime(long ms)
   {
      setMeta(KeyTimestamp, ms);
   }

   /** remove the time stampe for this point */
   public void removeTime()
   {
      
      removeMeta(KeyTimestamp);
   }

   /** @return time stamp for this point (ms) */
   public long getTime()
   {
      return (Long)getMeta(KeyTimestamp);
   }

   /** @return true if the timestamp for this point is valid */
   public boolean hasTime()
   {
      return containsMeta(KeyTimestamp);
   }

   /** @return true if no dimensions are NaN */
   public boolean isValid()
   {
      if (vec == null) return false;
      for(int i = 0; i < vec.length; i++)
         if (Double.isNaN(vec[i])) return false;
      return true;
   }

   /** @return dimensionality of this feature vector */
   public int getNumDims()
   {
      return (vec == null ? 0 : vec.length);
   }

   /** @return vector data as a double array (not a copy!) */
   public double[] get()
   {
      return vec;
   }

   /** @return vector data as a double array (copy of internal data) */
   public double[] toArray()
   {
      return vec.clone();
   }

   /** @return value of d^{th} dimension */
   public double get(int d)
   {
      return vec[d];
   }

   /** set the value of the d^{th} dimension to v */
   public void set(int d, double v)
   {
      vec[d] = v;
   }

   /** set the value of all dimensions to x */
   public void fill(double x)
   {
      Arrays.fill(vec, x);
   }

   /**
    * Copies the values of the argument into this feature vector.
    */
   public void copyFrom(FeatureVec fv)
   {
      super.copyFrom(fv);
      int n = fv.getNumDims();
      if (getNumDims() != n) vec = new double[n];
      for(int i = 0; i < n; i++)
         vec[i] = fv.get(i);
      if (fv.hasTime()) setTime(fv.getTime());
   }

   /**
    * Returns a new feature vector with just those dimensions specified by the dims array (and in that order).
    */
   public FeatureVec selectDims(int dims[])
   {
      int nd = dims.length;
      FeatureVec ret = new FeatureVec(nd);
      for(int i = 0; i < nd; i++)
         ret.set(i, get(dims[i]));
      return ret;
   }
   
   /**
    * Compute the absolute distance between this and the given vector
    * @param fv compute absolute distance to this vector
    * @return absolute distance
    */
   public double absdist(FeatureVec fv)
   {
      return fv.sub(this)._abs().sum();
   }

   /**
    * Compute the squared distance between the vectors
    * 
    * @param fv compute squared distance to this vector
    * @return squared distance
    */
   public double dist2(FeatureVec fv)
   {
      return fv.sub(this).norm2();
   }

   /**
    * Compute the distance between the vectors
    * 
    * @param fv compute distance to this vector
    * @return distance
    */
   public double dist(FeatureVec fv)
   {
      return Math.sqrt(dist2(fv));
   }

   /**
    * Create a feature vec with random values -- uniform over [0..1) 
    * 
    * @param n dimensionality of vector
    * @return random vector in unit cube
    */
   public static FeatureVec rand(int n)
   {
      FeatureVec ret = new FeatureVec(n);
      for(int i = 0; i < n; i++)
         ret.set(i, Library.random());
      return ret;
   }

   /**
    * Create a feature vec with random values -- gaussian
    * 
    * @param n dimensionality of vector
    * @return random vector (gaussian)  
    */
   public static FeatureVec rand(int n, double mean, double var)
   {
      FeatureVec ret = new FeatureVec(n);
      Gaussian1D g = new Gaussian1D(mean, var); 
      for(int i = 0; i < n; i++)
         ret.set(i, g.sample1());
      return ret;
   }

   
   /**
    * Create a feature vec of zeros
    * 
    * @param n dimensionality
    * @return new vector
    */
   public static FeatureVec zeros(int n)
   {
      return new FeatureVec(n, 0);
   }

   /**
    * Create a feature vec of ones
    * 
    * @param n dimensionality
    * @return new vector
    */
   public static FeatureVec ones(int n)
   {
      return new FeatureVec(n, 1);
   }

   /**
    * @return sum of elements of this feature vec
    */
   public double sum()
   {
      double v = 0;
      for(int i = 0; i < vec.length; i++)
         v += vec[i];
      return v;
   }

   /** @return mean (average) of elements of this feature vec */
   public double mean()
   {
      return sum() / getNumDims();
   }

   /**
    * in-place addition
    * 
    * @param fv summand
    */
   public FeatureVec _add(FeatureVec fv)
   {
      int n = getNumDims();
      assert n == fv.getNumDims() : String.format("this.nDims=%d  fv.nDims=%d\n", n, fv.getNumDims());
      for(int i = 0; i < n; i++)
         vec[i] += fv.get(i);
      return this;
   }

   /**
    * in-place addition
    * 
    * @param x summand
    */
   public FeatureVec _add(double x)
   {
      int n = getNumDims();
      for(int i = 0; i < n; i++)
         vec[i] += x;
      return this;
   }

   /**
    * in-place subtraction
    * 
    * @param fv vector to subtract
    */
   public FeatureVec _sub(FeatureVec fv)
   {
      int n = getNumDims();
      for(int i = 0; i < n; i++)
         vec[i] -= fv.get(i);
      return this;
   }

   /**
    * in-place subtraction
    * 
    * @param x value to substract
    */
   public FeatureVec _sub(double x)
   {
      int n = getNumDims();
      for(int i = 0; i < n; i++)
         vec[i] -= x;
      return this;
   }

   /**
    * in-place multiplicatin
    * 
    * @param fv multiplier
    */
   public FeatureVec _mul(FeatureVec fv)
   {
      int n = getNumDims();
      assert n == fv.getNumDims();
      for(int i = 0; i < n; i++)
         vec[i] *= fv.get(i);
      return this;
   }

   /**
    * in-place multiplication
    * 
    * @param x multiplier
    */
   public FeatureVec _mul(double x)
   {
      int n = getNumDims();
      for(int i = 0; i < n; i++)
         vec[i] *= x;
      return this;
   }

   /**
    * in-place division
    * 
    * @param fv vector divisor
    */
   public FeatureVec _div(FeatureVec fv)
   {
      int n = getNumDims();
      for(int i = 0; i < n; i++)
         vec[i] /= fv.get(i);
      return this;
   }

   /**
    * in-place division
    * 
    * @param x divisor
    */
   public FeatureVec _div(double x)
   {
      int n = getNumDims();
      for(int i = 0; i < n; i++)
         vec[i] /= x;
      return this;
   }

   /**
    * in-place absolute value
    */
   public FeatureVec _abs()
   {
      int n = getNumDims();
      for(int i = 0; i < n; i++)
         vec[i] = Math.abs(vec[i]);
      return this;
   }

   /**
    * in-place squaring of elements
    */
   public FeatureVec _sqr()
   {
      int n = getNumDims();
      for(int i = 0; i < n; i++)
         vec[i] = vec[i] * vec[i];
      return this;
   }

   /** @return new vector with values equal to the squared value in this vector */ 
   public FeatureVec sqr()
   {
      return new FeatureVec(this)._sqr();
   }

   /** @return new vector with values equal to the absolute value of this vector */
   public FeatureVec abs()
   {
      return new FeatureVec(this)._abs();
   }

   /** @return new vector representing sum of this vector and given vector */
   public FeatureVec add(FeatureVec fv)
   {
      return new FeatureVec(this)._add(fv);
   }

   /** @return new vector with x added to each value in this vector */
   public FeatureVec add(double x)
   {
      return new FeatureVec(this)._add(x);
   }

   public FeatureVec sub(FeatureVec fv)
   {
      return new FeatureVec(this)._sub(fv);
   }

   public FeatureVec sub(double x)
   {
      return new FeatureVec(this)._sub(x);
   }

   public FeatureVec mul(FeatureVec fv)
   {
      return new FeatureVec(this)._mul(fv);
   }

   public FeatureVec mul(double x)
   {
      return new FeatureVec(this)._mul(x);
   }

   public FeatureVec div(FeatureVec fv)
   {
      return new FeatureVec(this)._div(fv);
   }

   public FeatureVec div(double x)
   {
      return new FeatureVec(this)._div(x);
   }

   public double dot(FeatureVec fv)
   {
      int n = getNumDims();
      assert n == fv.getNumDims();
      double ret = 0.0;
      for(int i = 0; i < n; i++)
         ret += vec[i] * fv.get(i);
      return ret;
   }

   /**
    * @return projection of this vector onto the given vector
    */
   public FeatureVec projv(FeatureVec fv)
   {
      double a = this.dot(fv);
      double b = fv.dot(fv);
      return fv._mul(a / b);
   }

   /**
    * @return position along the given vector (after normalization) of the projection. So the projection
    *         can be recovered by a*v/|v| if 'a' is the return value and 'v' is the given vector.
    */
   public double projLen(FeatureVec fv)
   {
      return this.dot(fv) / fv.norm();
   }

   /** @return squared L2 norm of this vector */
   public double norm2()
   {
      int n = getNumDims();
      double ret = 0.0;
      for(int i = 0; i < n; i++)
         ret += vec[i]*vec[i];
      return ret;
   }

   /** @return L2 norm of this vector */
   public double norm()
   {
      return Math.sqrt(norm2());
   }
   
   /** in-place square root */
   public FeatureVec _sqrt()
   {
      for(int i=0; i<vec.length; i++)
         vec[i] = Math.sqrt(vec[i]);
      return this;
   }
   
   /** @return vector with each dimension equal to the square root of the value in this vector */
   public FeatureVec sqrt()
   {
      return new FeatureVec(this)._sqrt();
   }

   /** @return unit vector in the direction of this vector */
   public FeatureVec unit()
   {
      return div(norm2());      
   }

   /** @return vector with each dimension equal to the smaller of this vector and the given vector */
   public FeatureVec min(FeatureVec fv)
   {
      return new FeatureVec(this)._min(fv);
   }

   /** in-place minimum value selection */
   public FeatureVec _min(FeatureVec fv)
   {
      int d = getNumDims();
      assert (fv.getNumDims() == d);
      for(int i = 0; i < d; i++)
         vec[i] = Math.min(vec[i], fv.get(i));
      return this;
   }

   /** @return smallest value across all of the dimensions */
   public double min()
   {
      double x = vec[0];
      for(int i = 1; i < vec.length; i++)
         x = Math.min(x, vec[i]);
      return x;
   }

   /** @return true if all dimensions are smaller than the corresponding value in the given vector */ 
   public boolean lessThan(FeatureVec fv)
   {
      for(int d=0; d<vec.length; d++)
         if (vec[d] >= fv.get(d)) return false;
      return true;
   }
   
   /** @return true if all dimensions are smaller than or equal to the corresponding value in the given vector */ 
   public boolean leqThan(FeatureVec fv)
   {
      for(int d=0; d<vec.length; d++)
         if (vec[d] > fv.get(d)) return false;
      return true;
   }
   
   /** @return true if all dimensions are larger than or equal to the corresponding value in the given vector */ 
   public boolean geqThan(FeatureVec fv)
   {
      for(int d=0; d<vec.length; d++)
         if (vec[d] < fv.get(d)) return false;
      return true;
   }
   
   /** @return true if all dimensions are larger than the corresponding value in the given vector */ 
   public boolean greaterThan(FeatureVec fv)
   {
      for(int d=0; d<vec.length; d++)
         if (vec[d] <= fv.get(d)) return false;
      return true;
   }
   
   /** @return true if all dimensions are smaller than the given value */
   public boolean lessThan(double x)
   {
      for(int d=0; d<vec.length; d++)
         if (vec[d] >= x) return false;
      return true;
   }
   
   /** @return true if all dimensions are smaller than or equal to the given value */
   public boolean leqThan(double x)
   {
      for(int d=0; d<vec.length; d++)
         if (vec[d] > x) return false;
      return true;
   }
   
   /** @return true if all dimensions are larger than or equal to the given value */ 
   public boolean geqThan(double x)
   {
      for(int d=0; d<vec.length; d++)
         if (vec[d] < x) return false;
      return true;
   }
   
   /** @return true if all dimensions are larger than the given value */ 
   public boolean greaterThan(double x)
   {
      for(int d=0; d<vec.length; d++)
         if (vec[d] <= x) return false;
      return true;
   }
   
   /** @return largest value across all of the dimensions */
   public double max()
   {
      double x = vec[0];
      for(int i = 1; i < vec.length; i++)
         x = Math.max(x, vec[i]);
      return x;
   }

   public FeatureVec max(FeatureVec fv)
   {
      FeatureVec ret = new FeatureVec(this);
      return ret._max(fv);
   }

   public FeatureVec _max(FeatureVec fv)
   {
      int d = getNumDims();
      assert (fv.getNumDims() == d);
      for(int i = 0; i < d; i++)
         vec[i] = Math.max(vec[i], fv.get(i));
      return this;
   }

   public FeatureVec min(double x)
   {
      int d = getNumDims();
      FeatureVec ret = new FeatureVec(d);
      for(int i = 0; i < d; i++)
         ret.set(i, Math.min(vec[i], d));
      return ret;
   }

   public FeatureVec max(double x)
   {
      int d = getNumDims();
      FeatureVec ret = new FeatureVec(d);
      for(int i = 0; i < d; i++)
         ret.set(i, Math.max(vec[i], x));
      return ret;
   }

   @Override
   /** @return hashcode based on the data in this vector (not including the time stamp) */
   public int hashCode()
   {      
      if (bDataComp) return Arrays.hashCode(vec);
      return super.hashCode();
   }
   
   @Override
   public boolean equals(Object o)
   {
      if (bDataComp){
         FeatureVec fv = (FeatureVec)o;
         int D = getNumDims();
         if (D != fv.getNumDims()) return false;
         for(int d=0; d<D; d++)
            if (Math.abs(vec[d] - fv.get(d)) > 1e-12) return false;
         return true;
      }
      else return super.equals(o);
   }

   public String toString()
   {
      StringBuffer sb = new StringBuffer();
      sb.append("[FV:");
      for(int i = 0; i < vec.length; i++)
         sb.append(String.format(" %.3f", vec[i]));
      sb.append("]");
      return sb.toString();
   }
}
