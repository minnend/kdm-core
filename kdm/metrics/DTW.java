package kdm.metrics;

import java.awt.Point;
import java.util.*;
import kdm.data.*;
import kdm.util.*;

/** a metric for sequences that uses dynamic time warping (DTW) to compute the distance */
public class DTW extends MetricSeq
{
   protected final static int PAR_DIAG = 0;
   protected final static int PAR_UP = 1;
   protected final static int PAR_LEFT = 2;

   protected MetricFV fvm = null;
   protected boolean bBand = false;
   protected double rBand = Double.NaN;
   protected Point[] path;
   protected boolean bPar = false;
   protected double costm[][];

   public DTW(double _rBand)
   {
      this(new EuclideanFV(), _rBand, LengthPrep.extend);
   }

   public DTW(MetricFV _fvm, double _rBand, LengthPrep lprep)
   {
      super(lprep);
      fvm = _fvm;
      if (!Double.isNaN(_rBand)){
         bBand = true;
         rBand = _rBand;
      }
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
         int band = (int)(rBand * wloc.length() / 2.0);
         int iStart = wloc.start();
         int iEnd = wloc.end() - 1;
         minSeq.add(seq.get(iStart));
         maxSeq.add(seq.get(iStart));
         for(int i = iStart+1; i < iEnd; i++){
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
      DtwLBI lbi = (DtwLBI)lbinfo;
      int n = wloc.length();
      int nDims = seq.getNumDims();
      assert (n == lbi.length());
      assert (nDims == lbi.minSeq.getNumDims());
      
      double lb = 0;
      FeatureVec fvd = new FeatureVec(nDims);
      FeatureVec fvzero = FeatureVec.zeros(nDims);
      for(int i = 0; i < n; i++){
         FeatureVec fv = seq.get(wloc.iStart + i);
         for(int d = 0; d < nDims; d++){
            double x = fv.get(d);
            double min = lbi.minSeq.get(i, d);
            double max = lbi.maxSeq.get(i, d);

            if (x > max) fvd.set(d, x - max);
            else if (x < min) fvd.set(d, min - x);
            else fvd.set(d, 0);
         }         
         lb += fvm.dist(fvd, fvzero);
      }

      return lb;
   }

   @Override
   protected double calcLowerBound(Sequence a, WindowLocation winA, Sequence b, WindowLocation winB)
   {
      // weak lower-bound if we don't use a band
      FeatureVec amin = a.getMin(winA.start(), winA.end());
      FeatureVec amax = a.getMax(winA.start(), winA.end());
      FeatureVec bmin = b.getMin(winB.start(), winB.end());
      FeatureVec bmax = b.getMax(winB.start(), winB.end());

      // TODO: could combine lb1 & lb2, but trivial sum is wrong if min/max happens at
      // first/last index

      double lb1 = fvm.dist(amin, bmin) + fvm.dist(amax, bmax);
      double lb2 = fvm.dist(a.get(winA.start()), b.get(winB.start()))
            + fvm.dist(a.get(winA.getLastIndex()), b.get(winB.getLastIndex()));

      return Math.max(lb1, lb2);
   }

   protected Point[] calcPath(int[][] par)
   {
      int y = par.length - 1;
      int x = par[0].length - 1;
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
      assert (x == 0 && y == 0 && par[y][x] == -1) : String.format("(%d, %d) par=%d", x, y, par[y][x]);
      list.add(new Point(0, 0));

      // reverse the list to create the path
      Point[] path = new Point[list.size()];
      for(int i = 0; i < path.length; i++)
         path[i] = list.get(path.length - i - 1);
      return path;
   }

   /**
    * Calculate the DTW distance according to this object's parameters (band param, etc.).
    * 
    * @return DTW distance between (sub)sequence A and B
    */
   protected double calcDist(Sequence a, WindowLocation winA, Sequence b, WindowLocation winB)
   {
      // we want B to be longer than A
      if (winA.length() > winB.length()) return calcDist(b, winB, a, winA);

      if (bBand) return calcDistBand(a, winA, b, winB);
      else{
         int ia = winA.start();
         int ib = winB.start();
         int na = winA.length();
         int nb = winB.length();
         if (costm == null || costm.length != na || costm[0].length != nb)
            costm = Library.allocMatrixDouble(na, nb, Library.INF);
         int[][] par = bPar ? Library.allocMatrixInt(na, nb, -1) : null;

         // init the cost matrix
         costm[0][0] = fvm.dist(a.get(ia), b.get(ib));
         for(int i = 1; i < na; i++){
            costm[i][0] = costm[i - 1][0] + fvm.dist(a.get(i + ia), b.get(ib));
            if (bPar) par[i][0] = PAR_UP;
         }
         for(int i = 1; i < nb; i++){
            costm[0][i] = costm[0][i - 1] + fvm.dist(b.get(i + ib), a.get(ia));
            if (bPar) par[0][i] = PAR_LEFT;
         }

         // fill in the cost matrix
         for(int i = 1; i < na; i++)
            for(int j = 1; j < nb; j++){
               double d = fvm.dist(a.get(i + ia), b.get(j + ib));
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
         if (bPar) path = calcPath(par);
         return costm[na - 1][nb - 1];
      }
   }

   /**
    * Calculate the DTW distance using this classes band parameter.
    * 
    * @return DTW distance between (sub)sequence A and B
    */
   protected double calcDistBand(Sequence a, WindowLocation winA, Sequence b, WindowLocation winB)
   {
      // we want B to be longer than A
      if (winA.length() > winB.length()) return calcDistBand(b, winB, a, winA);

      int n;
      int ia = winA.start();
      int ib = winB.start();
      int na = winA.length();
      int nb = winB.length();
      int nDims = a.getNumDims();
      if (costm == null || costm.length != na || costm[0].length != nb)
         costm = Library.allocMatrixDouble(na, nb, Library.INF);
      int[][] par = bPar ? Library.allocMatrixInt(na, nb, -1) : null;
      double slope = (na > 1 ? (double)(nb - 1) / (double)(na - 1) : Math.max(nb - 1, 1));
      // # steps in either direction along seqB
      int band = (int)(rBand * nb / 2.0);

      // init the cost matrix
      costm[0][0] = fvm.dist(a.get(ia), b.get(ib));
      n = Library.min(band + 1, na);
      FeatureVec fv = b.get(ib);
      for(int i = 1; i < n; i++){
         costm[i][0] = costm[i - 1][0] + fvm.dist(a.get(i + ia), fv);
         if (bPar) par[i][0] = PAR_UP;
      }
      n = Library.min(band + 1, nb);
      fv = a.get(ia);
      for(int i = 1; i < n; i++){
         costm[0][i] = costm[0][i - 1] + fvm.dist(b.get(i + ib), fv);
         if (bPar) par[0][i] = PAR_LEFT;
      }

      // fill in the cost matrix
      int jMaxStart = 1; // always before here to ensure that a mapping exists
      for(int i = 1; i < na; i++){
         int k = (int)Math.round((double)i * slope);
         int ja = Library.max(1, Library.min(jMaxStart, k - band));
         int jb = Library.max(ja + 1, Library.min(nb, k + band + 1));
         for(int j = ja; j < jb; j++){
            double d = fvm.dist(a.get(i + ia), b.get(j + ib));
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
         jMaxStart = jb;
      }
      if (bPar) path = calcPath(par);
      return costm[na - 1][nb - 1];
   }

}
