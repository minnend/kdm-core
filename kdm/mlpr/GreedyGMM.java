package kdm.mlpr;

import kdm.io.*;
import kdm.io.DataLoader.*;
import kdm.data.*;
import kdm.models.*;
import kdm.util.*;
import java.util.*;
import org.apache.commons.math.stat.*;

/**
 * learns a Gaussian mixture model (GMM) using a greedy (iterative) approach; note that the first mixture
 * component will be a "background" model. Based on: Vlassis and Likas. A Greedy EM Algorithm for Gaussian
 * Mixture Learning. 2002.
 */
public class GreedyGMM
{
   /**
    * Construct a new mixture model from an existing one plus a new component
    * @param gmmPrev existing mixture model
    * @param comp new component
    * @param alpha mixing component (weight on new component)
    * @return new mixture model
    */
   protected static GMM buildNextGMM(GMM gmmPrev, GaussianAbstract comp, double alpha)
   {
      GMM gmm = new GMM(gmmPrev.getNumDims(), gmmPrev.getNumMix() + 1);

      for(int i = 0; i < gmmPrev.getNumMix(); i++)
      {
         double w = (1 - alpha) * gmm.getWeight(i);
         gmm.setComp(i, w, gmmPrev.getComp(i));
      }
      gmm.setComp(gmmPrev.getNumMix(), alpha, comp);

      return gmm;
   }
   
   /**
    * Calculate log-likelihood of data given a mixture model and candidate component according to eq 16
    * @param data data points over which to calc log-lik
    * @param gmm existing mixture model (f_k)
    * @param phi candidate component
    * @return [0]=log-lik of data, [1]=a^{hat}
    */
   protected static double[] calcNextLogLik(Sequence data, GMM gmm, GaussianAbstract phi)
   {
      int N = data.length();
      double loglik = Library.LOG_ONE;

      // first term of eq 16
      for(int i=0; i<N; i++)
      {
         FeatureVec x = data.get(i);
         loglik += Library.logadd(gmm.eval(x), phi.eval(x));
      }
      loglik -= N * Library.LOG_TWO;
      
      // second term of eq 16 and eq 17      
      double a = 0, b=0;
      for(int i=0; i<N; i++)
      {
         FeatureVec x = data.get(i);
         double y = phi.eval(x);
         a += y;
         b += y*y;         
      }
      loglik += (a*a)/(2*b);
      double aHat = 0.5 - a/(2*b);
      
      return new double[]{ loglik, aHat };
   }
   
   /**
    * Find the best location for the next component mean by searching over all of the data points
    * @param data data points to search
    * @param sigma radius of cov matrix for new component
    * @param gmmPrev existing GMM
    * @return mean that maximizes the log-likelihood along with mixing weight
    */
   protected static PhiMix findNextMean(Sequence data, double sigma, GMM gmmPrev)
   {
      int nDims = data.getNumDims();
      int N = data.length();
      GaussianAbstract phiBest = null;
      double llBest = Library.NEGINF;
      double aHat = Double.NaN;
      
      for(int i=0; i<N; i++)
      {
         // build phi, the initial estimate for the next component  
         GaussianSpherical phi = new GaussianSpherical(nDims);
         phi.setMean(data.get(i));
         phi.setVar(sigma);
         
         // see if this mean is the best so far
         double[] ret = calcNextLogLik(data, gmmPrev, phi);
         
         if (ret[1]<=0 || ret[1]>=1)
         {
            if (gmmPrev.getNumMix()==1) ret[1] = 0.5;
            else{
               ret[1] = 2.0/(gmmPrev.getNumMix()+1);
            }
         }
         
         if (ret[0] > llBest)
         {
            llBest = ret[0];
            aHat = ret[1];
            phiBest = phi;
         }
      }
      
      return (phiBest==null ? null : new PhiMix(phiBest, aHat, llBest));
   }

   /**
    * Update component parameters using EM 
    * @param data data over which to calc loglik
    * @param gmm existing mixture model
    * @param mm initial estimate of model and mixing parameters
    * @return improved model parameters
    */
   public static PhiMix partialEM(Sequence data, GMM gmm, PhiMix mm)
   {
      int N = data.length();
      int nDims = data.getNumDims();
      
      // accumulators
      double pkxn = 0; // sum(pkx)
      FeatureVec px = FeatureVec.zeros(nDims); // sum(pkx * x)
      double[] pkx = new double[N];
      
      for(int i=0; i<N; i++)
      {
         FeatureVec x = data.get(i);
         
         // calc P(k+1|x)         
         double phi = Math.exp(mm.phi.eval(x));
         double fkx = Math.exp(gmm.eval(x));
         double ap = mm.alpha * phi;
         pkx[i] = ap / ((1-mm.alpha)*fkx + ap);
         
         // update accumulators
         pkxn += pkx[i];
         px._add(x.mul(pkx[i]));
      }
      
      // update params
      double alpha = pkxn / N;
      FeatureVec fvMean = px.div(pkxn);
      GaussianAbstract phi = (GaussianAbstract)mm.phi.construct(nDims);
      phi.setMean(fvMean);
      
      // we do the covariance separate since it depends on the mean
      // TODO incremental cov estimation for one pass algorithm
      FeatureVec covd = FeatureVec.zeros(nDims); // sum(pkx * (x-m)(x-m)')
      for(int i=0; i<N; i++)
      {
         FeatureVec x = data.get(i);
         FeatureVec dx = x.sub(fvMean);
         covd._add(dx._sqr()._mul(pkx[i]));
      }
      covd._div(pkxn);
      if (phi instanceof GaussianSpherical)
      {
         GaussianSpherical g = (GaussianSpherical)phi;
         g.setVar(covd.mean());
      }
      else if (phi instanceof GaussianDiagonal)
      {
         GaussianDiagonal g = (GaussianDiagonal)phi;
         g.setVar(covd);
      }
      else{
         System.err.printf("Error: no support for component model: %s\n", phi.getClass());
         return null;
      }
      
      return new PhiMix(phi, alpha, Double.NaN);
   }   
   
   /**
    * learn a mixture model for the given data
    * 
    * @param nComps number of components to learn (including background component)
    * @param type spherical or diagonal Gaussians?
    * @param data data to learn over
    * @return mixture model
    */
   public static GMM learn(int nComps, GaussianAbstract factory, Sequence data)
   {
      int N = data.length();
      int nDims = data.getNumDims();
      //System.err.printf("Data: %dD, %d points\n", nDims, N);

      // first, learn a background model
      GaussianAbstract bg = (GaussianAbstract)factory.construct(nDims);
      bg.learn(data);
      //System.err.printf("Background model (%s): mean=%s\n", bg.getClass(), bg.getMean());

      // extract est global sdev from bg
      FeatureVec fvVar = bg.getCovDiag();
      double[] var = fvVar.get();

      // build the first GMM with just the bg component
      GMM gmmPrev = new GMM(nDims, 1);
      gmmPrev.setComp(0, 1.0, bg);
      
      // calc initial loglik given just the bg model
      double loglik = gmmPrev.eval(data);
      //System.err.printf("Loglik of bg model: %f\n", loglik);

      // calc the generic sigma for new components
      double beta = StatUtils.max(var) / 2.0;
      double a = 4.0 / ((nDims + 2) * data.length());
      double b = 1.0 / (nDims + 4.0);
      double sigma = beta * Math.pow(a, b);
      //System.err.printf("sigma = %f\n", sigma);

      // now iteratively add real components
      for(int iComp = 1; iComp < nComps; iComp++)
      {
         //System.err.printf("Searching for component %d...\n", iComp);         
         PhiMix mm = findNextMean(data, sigma, gmmPrev);
         if (mm == null)
         {
            System.err.printf("Error: failed to find a new mean\n");
            break;
         }
         
         if (factory instanceof GaussianDiagonal)
            mm.phi = new GaussianDiagonal((GaussianSpherical)mm.phi);
         
         //System.err.printf(" next mean: phi=%s  alpha=%f  loglik=%f\n", mm.phi.getMean(), mm.alpha, mm.loglik);
         mm = partialEM(data, gmmPrev, mm);
         if (mm.loglik <= loglik)
         {
            System.err.printf("Warning: loglik decreased %f -> %f\n", loglik, mm.loglik);
            break;
         }
         GMM gmmNext = buildNextGMM(gmmPrev, mm.phi, mm.alpha);
         double loglik2 = gmmNext.eval(data);
         //System.err.printf(" new loglik (%d): %f (%f)\n", iComp, loglik2, loglik);
         if (iComp==1 || loglik2 > loglik)
         {
            loglik = loglik2;
            gmmPrev = gmmNext;
         }
         else break;
      }
      
      // remove the background model
      GMM gmmNoBg = gmmPrev.getNumMix()>1 ? gmmPrev.removeComponent(0) : gmmPrev;
      GMM gmm = new GMM(gmmNoBg);
      gmm.learn(data);
      //System.err.printf("llBG=%f  llNoBG=%f llEM=%f\n", gmmPrev.eval(data), gmmNoBg.eval(data), gmm.eval(data));
      
      return gmm;
   }

   public static void main(String args[])
   {
      if (args.length == 0 || args[0].equals("-?"))
      {
         System.err.printf("\nUSAGE: java ~.GreedyGM <#comps> <data file>\n\n");
         System.exit(1);
      }
      int nComps = Integer.parseInt(args[0]);
      Sequence data = new DLRaw().load(args[1]);      
      if (data == null)
      {
         System.err.printf("Error: failed to load data: %s\n", args[1]);
         System.exit(1);
      }
      FeatureVec mean = data.getMean();
      int nDims = data.getNumDims();
      GaussianAbstract factory = new GaussianSpherical(0);
      
      // greedy learning
      TimerMS timer = new TimerMS();
      GMM gmm;
      /*GMM gmm = learn(nComps+1, factory, data);
      System.err.printf("Time to learn (greedy): %dms\n", timer.time());
      //gmm.dump();
      System.err.printf("loglik = %f\n\n", gmm.eval(data));
      */
      
      // EM with random element for init
      timer.reset();
      gmm = new GMM(nDims, nComps, factory);
      for(int i=0; i<nComps; i++)
         ((GaussianAbstract)gmm.getComp(i)).setMean(data.get(Library.random(data.length())));
      gmm.learn(data);
      System.err.printf("Time to learn (EM-init-x): %dms\n", timer.time());
      //gmm.dump();
      System.err.printf("loglik = %f\n\n", gmm.eval(data));
      
      // EM with centers around global mean for init
      timer.reset();
      gmm = new GMM(nDims, nComps, factory);
      for(int i=0; i<nComps; i++)
      {
         FeatureVec v = new FeatureVec(mean);
         v._add(FeatureVec.rand(nDims)._sub(-0.5)._mul(0.1));
         ((GaussianAbstract)gmm.getComp(i)).setMean(v);
      }
      gmm.learn(data);
      System.err.printf("Time to learn (EM-init-mean): %dms\n", timer.time());
      gmm.dump();
      System.err.printf("loglik = %f\n\n", gmm.eval(data));
   }
}

///////////////////////////////////////////////////////////

class PhiMix
{
   public GaussianAbstract phi;
   public double alpha;
   public double loglik;
   
   public PhiMix(GaussianAbstract phi, double alpha, double loglik)
   {
      this.phi = phi;
      this.alpha = alpha;
      this.loglik = loglik;
   }
}
