package kdm.metrics;

import kdm.data.*;
import kdm.metrics.MetricSeq.LengthPrep;
import kdm.util.*;

public class ERP extends MetricSeq
{
   MetricFV fvm = new AbsoluteDistFV();
   boolean bBand = false;
   double rBand = Double.NaN;

   public ERP(double _rBand)
   {
      this(new EuclideanFV(), _rBand, LengthPrep.extend);      
   }
   
   public ERP(MetricFV _fvm, double _rBand, LengthPrep lprep)
   {
      super(lprep);
      fvm = _fvm;
      if (!Double.isNaN(_rBand))
      {
         bBand = true;
         rBand = _rBand;
      }
   }

   public final static double calcLBValue(Sequence seq)
   {
      return seq.sum().sum();
   }

   public double calcDist(Sequence a, WindowLocation winA, Sequence b, WindowLocation winB)
   {
      if (bBand) return distBand(a, winA, b, winB);
      else
      {
         int ia = winA.start();
         int ib = winB.start();
         int na = winA.length();
         int nb = winB.length();
         int nDims = a.getNumDims();
         FeatureVec zero = FeatureVec.zeros(nDims);

         double[][] costm = Library.allocMatrixDouble(na, nb);

         // init the cost matrix
         costm[0][0] = fvm.dist(a.get(ia), b.get(ib));
         for(int i = 1; i < na; i++)
            costm[i + ia][0] = costm[i + ia - 1][0] + fvm.dist(a.get(i + ia), zero);
         for(int i = 1; i < nb; i++)
            costm[0][i + ib] = costm[0][i + ib - 1] + fvm.dist(b.get(i + ib), zero);

         // fill in the cost matrix
         for(int i = 1; i < na; i++)
         {
            for(int j = 1; j < nb; j++)
            {
               double v1 = costm[i + ia - 1][j] + fvm.dist(a.get(i + ia), zero);
               double v2 = costm[i][j + ib - 1] + fvm.dist(zero, b.get(j + ib));
               double v3 = costm[i + ia - 1][j + ib - 1] + fvm.dist(a.get(i + ia), b.get(j + ib));
               costm[i][j] = Library.min(v1, v2, v3);
            }
         }

         return costm[na - 1][nb - 1];
      }
   }

   public double distBand(Sequence a, WindowLocation winA, Sequence b, WindowLocation winB)
   {
      int ia = winA.start();
      int ib = winB.start();
      int na = winA.length();
      int nb = winB.length();
      int nDims = a.getNumDims();
      FeatureVec zero = new FeatureVec(nDims, 0);

      double[][] costm = Library.allocMatrixDouble(na, nb, Library.INF);

      int n, jmin;
      int r = (int)Math.ceil(rBand * nb / 2.0);
      double slope = (double)(nb - 1) / (double)(na - 1);

      // init the cost matrix
      costm[0][0] = fvm.dist(a.get(ia), b.get(ib));
      n = Library.min(r, na);
      for(int i = 1; i < n; i++)
         costm[i][0] = costm[i - 1][0] + fvm.dist(a.get(i + ia), zero);
      n = Library.min(r, nb);
      for(int i = 1; i < n; i++)
         costm[0][i] = costm[0][i - 1] + fvm.dist(b.get(i + ib), zero);

      // fill in the cost matrix
      n = 0;
      jmin = 1;
      for(int i = 1; i < na; i++)
      {
         int k = (int)Math.round((double)i * slope);
         int ja = Library.max(1, Library.min(jmin, k - r));
         int jb = Library.min(nb, k + r + 1);
         assert (i != na - 1 || (ja <= nb - 1 && jb == nb)) : String.format(
               "i=%d  k=%d  ja=%d  jb=%d  r=%d  na=%d  nb=%d  jmin=%d\n", i, k, ja, jb, r, na, nb, jmin);
         for(int j = ja; j < jb; j++)
         {
            double v1 = costm[i - 1][j] + fvm.dist(a.get(i + ia), zero);
            double v2 = costm[i][j - 1] + fvm.dist(zero, b.get(j + ib));
            double v3 = costm[i - 1][j - 1] + fvm.dist(a.get(i + ia), b.get(j + ib));
            costm[i][j] = Library.min(v1, v2, v3);
            assert (costm[i][j] != Library.INF) : String.format("i=%d  j=%d  r=%d  na=%d  nb=%d", i, j, r,
                  na, nb);
            n++;
         }
         jmin = jb;
      }

      // int m = na*nb - na - nb + 1;
      // System.err.printf("erp band: %d / %d (%.3f) dist=%.2f\n", n, m, (double)n / m,
      // costm[na-1][nb-1]);

      assert (costm[na - 1][nb - 1] != Library.INF) : String.format("r=%d  na=%d  nb=%d  slope=%.2f\n", r,
            na, nb, slope);
      return costm[na - 1][nb - 1];
   }
}
