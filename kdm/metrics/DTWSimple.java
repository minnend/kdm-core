package kdm.metrics;

import java.awt.Point;
import java.util.*;
import kdm.data.*;
import kdm.util.*;

/**
 * a metric for sequences that uses dynamic time warping (DTW) to compute the distance; the vector distance
 * metric used is absolute difference; this class is less general than DTW, which allows it to be more
 * efficient. The Sakoe-Chiba band is a percent of total length that gives the range of warping (r = band*|T| =>
 * t -> t +/- r/2).
 */
public class DTWSimple extends MetricSeq
{
   protected final static int PAR_DIAG = 0;
   protected final static int PAR_UP = 1;
   protected final static int PAR_LEFT = 2;

   protected double rBand = Double.NaN;
   protected Point[] path;
   protected boolean bPar = false;
   protected double costm[][];
   protected int par[][];

   /** construct a DTW metric with a 10% band */
   public DTWSimple()
   {
      this(0.1);
   }

   /** construct a DTW metric with the given band size */
   public DTWSimple(double rBand)
   {
      this(rBand, LengthPrep.extend);
   }

   /** construct a DTW metric with the given band size and lenth equalization method */
   public DTWSimple(Double rBand, LengthPrep lprep)
   {
      super(lprep);
      this.rBand = rBand;
   }

   public void setCalcPath(boolean bParent)
   {
      bPar = bParent;
      if (!bPar) par = null;
   }

   /** stores information for computing LB_Keogh for bounded (Sakoe-Chiba) DTW */
   public class DtwLBI extends LBInfo
   {
      public Sequence minSeq, maxSeq, origSeq;

      public DtwLBI(Sequence seq, WindowLocation wloc, double rBand)
      {
         // store information needed by LB_Keogh
         origSeq = seq;
         minSeq = new Sequence();
         maxSeq = new Sequence();
         int band = Math.max(1, (int)(rBand * wloc.length() / 2.0));
         int iStart = wloc.start();
         int iEnd = wloc.end() - 1;
         minSeq.add(seq.get(iStart));
         maxSeq.add(seq.get(iStart));
         for(int i = iStart + 1; i < iEnd; i++){
            int a = Math.max(iStart, i - band);
            int b = Math.min(iEnd, i + band + 1);
            minSeq.add(seq.getMin(a, b)); // TODO could be more efficient (eg, priority queue)
            maxSeq.add(seq.getMax(a, b));
         }
         minSeq.add(seq.get(iEnd));
         maxSeq.add(seq.get(iEnd));
      }

      public int length()
      {
         return minSeq.length();
      }

      @Override
      public int getReqLength()
      {
         return length();
      }
   }

   @Override
   public LBInfo calcLBInfo(Sequence seq, WindowLocation wloc)
   {
      return new DtwLBI(seq, wloc, rBand);
   }

   public Point[] getPath()
   {
      return path;
   }

   @Override
   public double lowerBound(LBInfo lbinfo, Sequence seq, WindowLocation wloc)
   {
      // compute LB_Keogh
      DtwLBI lbi = (DtwLBI)lbinfo;
      int n = wloc.length();
      int nDims = seq.getNumDims();
      assert (n == lbi.length());
      assert (nDims == lbi.minSeq.getNumDims());

      double lb = 0;
      for(int i = 0; i < n; i++){
         FeatureVec fv = seq.get(wloc.iStart + i);
         for(int d = 0; d < nDims; d++){
            double x = fv.get(d);
            double max = lbi.maxSeq.get(i, d);

            if (x > max) lb += x - max;
            else{
               double min = lbi.minSeq.get(i, d);
               if (x < min) lb += min - x;
            }
         }
      }
      return lb;
   }

   @Override
   protected double calcLowerBound(Sequence a, WindowLocation winA, Sequence b, WindowLocation winB)
   {
      assert false : "to calc LB for DTW, precompute DtwLBI";
      return 0.0;

      // compute the LB of Yi and Kim and return max
      /*
       * assert (winA.length() == winB.length()); // weak lower-bound if we don't use a band FeatureVec amin =
       * a.getMin(winA.start(), winA.end()); FeatureVec amax = a.getMax(winA.start(), winA.end()); FeatureVec
       * bmin = b.getMin(winB.start(), winB.end()); FeatureVec bmax = b.getMax(winB.start(), winB.end()); //
       * TODO: could combine lb1 & lb2, but trivial sum is wrong if min/max happens at // first/last index
       * 
       * double lb1 = fvm.dist(amin, bmin) + fvm.dist(amax, bmax); double lb2 = fvm.dist(a.get(winA.start()),
       * b.get(winB.start())) + fvm.dist(a.get(winA.getLastIndex()), b.get(winB.getLastIndex()));
       * 
       * return Math.max(lb1, lb2);
       */
   }

   protected Point[] calcPath(int na, int nb)
   {
      if (!bPar) return null;

      int y = na - 1;
      int x = nb - 1;
      ArrayList<Point> list = new ArrayList<Point>();
      while(x > 0 || y > 0){
         list.add(new Point(x, y));
         switch(par[y][x]){
         case PAR_DIAG:
            x--;
            y--;
            break;
         case PAR_UP:
            y--;
            break;
         case PAR_LEFT:
            x--;
            break;
         default:
            assert false : String
                  .format("Error: Invalid parent matrix value (%d, %d)=%d\n", x, y, par[y][x]);
            break;
         }
      }
      assert (x == 0 && y == 0) : String.format("(%d, %d) par=%d", x, y, par[y][x]);
      list.add(new Point(0, 0));

      // reverse the list to create the path
      Point[] path = new Point[list.size()];
      for(int i = 0; i < path.length; i++)
         path[i] = list.get(path.length - i - 1);
      return path;
   }

   /**
    * Distance calculations use potentially large matrices and don't free them to avoid re-allocation.
    * Explicitly free them by calling this method.
    */
   protected void freeMatrices()
   {
      costm = null;
      par = null;
   }

   /**
    * Calculate the DTW distance according to this object's parameters (band param, etc.).
    * 
    * @return DTW distance between (sub)sequence A and B
    */
   protected double calcDist(Sequence a, WindowLocation winA, Sequence b, WindowLocation winB)
   {
      // TODO if we don't need the path, we can compute the DTW distance without a full matrix, just keep two
      // rows; this should be more efficient

      int n;
      int ia = winA.start();
      int ib = winB.start();
      int na = winA.length();
      int nb = winB.length();
      assert(na == nb);
      int nDims = a.getNumDims();
      if (costm == null || costm.length < na || costm[0].length < nb)
         costm = Library.allocMatrixDouble(na, nb, Library.INF);
      if (bPar && (par == null || par.length < na || par[0].length < nb))
         par = Library.allocMatrixInt(na, nb, -1);
      
      // # steps in either direction along seqB
      int band = Math.max(1, (int)(rBand * nb / 2.0));

      // init the cost matrix
      costm[0][0] = a.get(ia).absdist(b.get(ib));
      n = Library.min(band + 1, na);
      FeatureVec fv = b.get(ib);
      for(int i = 1; i < n; i++){
         costm[i][0] = costm[i - 1][0] + fv.absdist(a.get(i + ia));
         if (bPar) par[i][0] = PAR_UP;
      }      
      fv = a.get(ia);
      for(int i = 1; i < n; i++){
         costm[0][i] = costm[0][i - 1] + fv.absdist(b.get(i + ib));
         if (bPar) par[0][i] = PAR_LEFT;
      }

      // make sure values outside of band aren't used
      if (n < na) costm[n][0] = costm[0][n] = Library.INF;      

      // fill in the cost matrix
      for(int i = 1; i < na; i++){
         int ja = Library.max(1, i - band);
         costm[i][ja-1] = Library.INF;         
         int jb = Library.min(nb, i + band + 1);
         for(int j = ja; j < jb; j++){
            double d = a.get(i + ia).absdist(b.get(j + ib));
            double v1 = costm[i - 1][j - 1];
            double v2 = costm[i - 1][j];
            double v3 = costm[i][j - 1];
            if (v1 < v2 && v1 < v3){
               costm[i][j] = v1 + d;
               if (bPar) par[i][j] = PAR_DIAG;
            }
            else if (v2 < v3){
               costm[i][j] = v2 + d;
               if (bPar) par[i][j] = PAR_UP;
            }
            else{
               costm[i][j] = v3 + d;
               if (bPar) par[i][j] = PAR_LEFT;
            }
         }
         if (jb < nb) costm[i][jb] = Library.INF;
      }
      if (bPar) path = calcPath(na, nb);
      return costm[na - 1][nb - 1];
   }
}
