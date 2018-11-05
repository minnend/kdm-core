package kdm.models;

import kdm.data.*;
import org.apache.commons.math.stat.*;

/** Represents a 1D histogram */
public class Histogram
{
   protected int[] bins;
   protected int nData;

   /** Create a histogram for the first dimension of the given sequence */
   public Histogram(int _nBins, Sequence seq)
   {
      this(_nBins, seq, 0);
   }

   /** Create a histogram for the iDim dimension of the given sequence */
   public Histogram(int _nBins, Sequence seq, int iDim)
   {
      bins = new int[_nBins];
      nData = seq.length();
      FeatureVec fvMin = seq.getMin();
      FeatureVec fvMax = seq.getMax();
      double vmin = fvMin.get(iDim);
      double vmax = fvMax.get(iDim);
      double[] edges = genEdges(vmin, vmax, _nBins);

      for(int i = 0; i < nData; i++)
      {
         double x = seq.get(i, iDim);
         int iBin = bin(x, edges);
         assert(iBin >= 0); // empirical min/max, so no oob errors
         bins[iBin]++;
      }
   }

   /** Create a histogram for the given data */
   public Histogram(int _nBins, double[] data)
   {      
      double vmin = StatUtils.min(data);
      double vmax = StatUtils.max(data);
      double edges[] = genEdges(vmin, vmax, _nBins);
      construct(data, edges);
   }

   /**
    * Create a histogram using bins defined by the given edges; data outside of the given range is ignored
    * 
    * @param data data to histogram
    * @param edges edges of the bins to use; must be monotonically non-decreasing
    */
   public Histogram(double[] data, double[] edges)
   {
      construct(data, edges);
   }

   /**
    * Generate edges array for the given range
    * 
    * @param vmin minimum value
    * @param vmax maximum value
    * @param nBins number of bnis
    * @return array of edges of bins, including start of first bin and end of last bin (ie, nBins+1 entries)
    */
   public static double[] genEdges(double vmin, double vmax, int nBins)
   {
      double[] edges = new double[nBins + 1];
      edges[0] = vmin;
      edges[nBins] = vmax;
      double wbin = (vmax - vmin) / nBins;
      for(int i = 1; i < nBins; i++)
         edges[i] = vmin + i * wbin;
      return edges;
   }
   
   /** @return bin in which x belongs */
   public final static int bin(double x, double[] edges)
   {        
      if (x < edges[0]) return -1; // too small
      int nBins = edges.length - 1;
      if (x > edges[nBins]) return -2; // too large

      for(int i = 1; i < nBins; i++)
         if (x < edges[i]) return i-1;
      return nBins - 1;      
   }

   /**
    * Construct the histogram using the given data and edges
    * @param data data to bin
    * @param edges edges of bins (non-decreasing array)
    */
   protected void construct(double[] data, double[] edges)
   {
      bins = new int[edges.length - 1];
      nData = data.length;
      for(int i = 0; i < nData; i++)
      {
         int iBin = bin(data[i], edges);
         if (iBin < 0) continue;
         bins[iBin]++;
      }
   }

   public int getNumBins()
   {
      return bins.length;
   }

   public int getNumDataPoints()
   {
      return nData;
   }

   /** @return entropy of the histogram */
   public double entropy()
   {
      double h = 0;
      for(int i = 0; i < bins.length; i++)
      {
         double p = (double)bins[i] / nData;
         if (p > 0) h -= p * Math.log(p);
      }
      return h;
   }
}
