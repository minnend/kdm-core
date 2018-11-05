package kdm.models;

import java.util.*;

import kdm.data.*;
import kdm.util.*;

/** abstract base class for left-right HMMs */
public abstract class AbstractHMMLR extends AbstractHMM
{
   /** tran[i][j] = transition from state i to state i+j */
   protected double[][] tran;
   protected int nSkip = 0;

   /**
    * Construct a default HMM with the specified number of hidden states and with obsevations of the given
    * dimensionality. The transition matrix is setup so that each state i can only transition to states i+j
    * (j>=0, j<=nSkip+1). The observation model is a multivariate Gaussian with independent dimensions (e.g.,
    * diagonal covariance matrix).
    */
   public AbstractHMMLR(int nStates, int nSkip, int nDims)
   {
      super(nDims);      
      
      piStart = new double[nStates];
      Arrays.fill(piStart, Library.LOG_ZERO);
      piStart[0] = Library.LOG_ONE;
      piEnd = new double[nStates];
      Arrays.fill(piEnd, Library.LOG_ZERO);
      piEnd[nStates-1] = Library.LOG_ONE;
      piLeave = new double[nStates];
      Arrays.fill(piLeave, Library.LOG_ZERO);
      piLeave[nStates - 1] = Math.log(0.5);
      initTran(0.9, nSkip);
   }
   
   public void initTran(double pSelf, int nSkip)
   {
      this.nSkip = nSkip;
      int nStates = piStart.length;
      
      // initialize the tran matrix to a valid default
      tran = new double[nStates][];
      for(int i = 0; i < nStates - 1; i++){
         int nLeft = Library.min(nStates - i - 1, nSkip + 1);
         tran[i] = new double[nLeft + 1];
         tran[i][0] = Math.log(pSelf);
         
         // spread remaining probability so longer skips are less likely         
         double ratio = 2.0;
         double factor = 1.0;
         double ratioPow = 1.0;
         for(int j=1; j<nLeft; j++){
            ratioPow *= ratio;
            factor += ratioPow;
         }
         double base = (1.0-pSelf) / factor;
         for(int j=nLeft; j>=1; j--){
            tran[i][j] = Math.log(base);
            base *= ratio;
         }
         
         // spread remaining probability evenly among skips
         //double pSkip = (1.0-pself) / nLeft;
         //for(int j = 1; j < tran[i].length; j++)
         //   tran[i][j] = Math.log(pSkip);
      }
      tran[nStates - 1] = new double[] { Library.LOG_ONE };
   }
   
   public double[][] saveTran()
   {
      double[][] dup = new double[tran.length][];
      for(int i=0; i<tran.length; i++){
         dup[i] = new double[tran[i].length];
         Library.copy(tran[i], dup[i]);
      }
      return dup;
   }
   
   public void blendTran(double[][] tran2, double blend)
   {
      double logA = Math.log(blend);
      double log1mA = Math.log(1.0-blend);
      
      for(int i=0; i<tran.length; i++)
         for(int j=0; j<tran[i].length; j++){
            tran[i][j] = Library.logadd(log1mA+tran[i][j], logA+tran2[i][j]);
         }
   }
   
   /** @return transition data in LR format (tran[i][j] = s_i to s_{i+j}) */
   public double[][] getTranLR()
   {
      return tran;
   }

   @Override
   public double[][] getFullTransMatrix()
   {
      int nStates = getNumStates();
      double tm[][] = new double[nStates][nStates];
      for(int i = 0; i < nStates; i++){
         Arrays.fill(tm[i], Library.LOG_ZERO);
         for(int j = 0; j < tran[i].length; j++)
            tm[i][j + i] = tran[i][j];
      }
      return tm;
   }

   @Override
   public double eval(Sequence seq)
   {
      calcAlpha(seq);
      return alpha[seq.length() - 1][getNumStates() - 1];
   }

   @Override
   protected void calcAlpha(Sequence seq)
   {
      int T = seq.length();
      int nStates = getNumStates();
      alpha = Library.allocMatrixDouble(T, nStates, Library.LOG_ZERO);
      calcB(seq);

      // we have to start in the first state
      alpha[0][0] = bmat[0][0];

      // fill in the alpha matrix
      for(int t = 1; t < T; t++)
         for(int i = 0; i < nStates; i++){ // to state
            double a = Library.LOG_ZERO;
            for(int j = 0; j <= i; j++){ // from state
               int k = i - j;
               if (k >= tran[j].length || tran[j][k] == Library.NEGINF || alpha[t - 1][j] == Library.NEGINF)
                  continue;
               a = Library.logadd(a, alpha[t - 1][j] + tran[j][k]);
               assert(!Double.isNaN(a)) : String.format("t-1=%d j=%d k=%d  alpha=%f  tran=%f",
                     t-1,j,k,alpha[t-1][j],tran[j][k]);
            }
            alpha[t][i] = a + bmat[i][t];
         }
   }

   @Override
   protected void calcBeta(Sequence seq)
   {
      int T = seq.length();
      int nStates = getNumStates();
      int iLast = nStates - 1;
      beta = Library.allocMatrixDouble(T, nStates, Library.LOG_ZERO);
      calcB(seq);

      // we have to end in the last state
      beta[T - 1][iLast] = Library.LOG_ONE;

      for(int t = T - 2; t >= 0; t--){
         for(int i = 0; i < nStates; i++){
            double a = Library.LOG_ZERO;
            for(int j = 0; j < tran[i].length; j++){
               if (beta[t + 1][i + j] == Library.NEGINF || tran[i][j] == Library.NEGINF) continue;
               a = Library.logadd(a, beta[t + 1][i + j] + tran[i][j] + bmat[i + j][t + 1]);
               assert(!Double.isNaN(a)) : String.format("t+1=%d i=%d j=%d  beta=%f  tran=%f  bmat=%f",
                     t+1,i,j,beta[t+1][i+j],tran[i][j],bmat[i+j][t+1]);
            }
            beta[t][i] = a;
         }
      }
   }

   /**
    * Compute the best subseq match within the given range
    * 
    * @param seq sequence to sarch
    * @param r range within which to search
    * @param bNorm if true, find best length normalized subseq
    * @return best window
    */
   public ScoredWindow findBestSubseq(Sequence seq, Range r, boolean bNorm)
   {
      if (r == null) r = new Range(0, seq.length() - 1);
      int ia = r.a;
      int ib = r.b;

      int nStates = getNumStates();
      int rlen = r.length();
      int iLast = nStates - 1;
      // TODO should do m[<state>][<frame#>] to be consistent
      double m[][] = Library.allocMatrixDouble(rlen, nStates, Library.LOG_ZERO);
      int par[][] = new int[rlen][nStates];
      calcB(seq);

      // must start in first state, but in any frame
      for(int t = 0; t < rlen; t++){
         m[t][0] = bmat[0][t];
         par[t][0] = -1;
      }

      // fill in the trellis
      for(int t = 1; t < rlen; t++){
         // handle middle states
         for(int i = 1; i < nStates; i++){
            int iBest = i;
            double vBest = m[t - 1][i] + tran[i][0];
            for(int j = 1; j <= i; j++){
               int dij = i - j;
               if (j >= tran[dij].length) continue; // no trans from state(i-j) -> state(i)
               double v = m[t - 1][dij] + tran[dij][j];
               if (v > vBest){
                  vBest = v;
                  iBest = dij;
               }
            }
            m[t][i] = vBest + bmat[i][t];
            par[t][i] = iBest;
         }
      }

      // now we can scan for the best subseq
      int iBest = -1;
      int jBest = -1;
      double vBest = Library.LOG_ZERO;

      if (bNorm){
         // scan end-state for max normalized score
         for(int j = 0; j < rlen; j++){
            if (m[j][iLast] == Library.LOG_ZERO) continue;

            // figure out starting position
            int pathLen = 0;
            int i = j;
            int jState = iLast;
            while(jState >= 0){
               pathLen++;
               jState = par[i--][jState];
            }
            double v = m[j][iLast] / pathLen;
            if (v > vBest){
               vBest = v;
               jBest = j;
               iBest = i + 1;
            }
         }
      }
      else{
         // scan end-state for max
         jBest = 0;
         for(int j = 1; j < rlen; j++)
            if (m[j][iLast] > m[jBest][iLast]) jBest = j;
         vBest = m[jBest][iLast];
      }

      // figure out starting position
      if (vBest > Library.LOG_ZERO){
         MyIntList pathList = new MyIntList();
         int j = jBest;
         int jState = iLast;
         while(jState >= 0){
            pathList.addFront(jState);
            jState = par[j--][jState];
         }
         iBest = j + 1;
         assert ((jBest - iBest + 1) == pathList.size()) : String.format(
               "path length doesn't match (%d vs %d)", jBest - iBest + 1, pathList.size());
         path = pathList.toArray();
         return new ScoredWindow(-1, r.a + iBest, path.length, vBest);
      }
      else{
         path = null;
         return null;
      }
   }

   /**
    * Find the best matching subsequence in the given sequence.
    * 
    * @param seq data to analyze
    * @param span ranges of the sequence to analyze
    * @param bNorm if true, determine best subseq based on normalized (avg per-frame) score
    * @return the best window
    */
   public ScoredWindow findBestSubseq(Sequence seq, SpanList span, boolean bNorm)
   {
      if (span == null) return findBestSubseq(seq, new Range(0, seq.length() - 1), bNorm);

      // check each range
      ScoredWindow bestWin = null;
      for(int iRange = 0; iRange < span.getNumSpans(); iRange++){
         Range r = span.getRange(iRange);
         ScoredWindow swin = findBestSubseq(seq, r, bNorm);
         if (swin == null) continue;
         if (bestWin == null || swin.score > bestWin.score) bestWin = swin;
      }

      return bestWin;
   }

   public ScoredWindow findBestSubseq(Sequence seq, boolean bNorm)
   {
      return findBestSubseq(seq, (Range)null, bNorm);
   }

   /**
    * Find the best matching subsequence in the given sequence by trying every possible subsequence (in the
    * allowed range) => O(T^2).
    * 
    * @param seq data to analyze
    * @param span ranges of the sequence to analyze
    * @param bNorm normalize the subsequence scores by the length (divide by length)?
    * @param rSubLen range of subsequence lengths to test (null or -1 for no bounds)
    * @param nStartStep offset between start indices (i.e., delta_iStart)
    * @return the best window
    */
   public ScoredWindow findBestSubseqSlow2(Sequence seq, SpanList span, boolean bNorm, Range rSubLen,
         int nStartStep)
   {
      int T = seq.length();
      int nStates = getNumStates();
      int iLast = nStates - 1;
      double m[][] = Library.allocMatrixDouble(T, nStates, Library.LOG_ZERO);
      int par[][] = new int[T][nStates];

      int iBest = -1;
      int jBest = -1;
      double vBest = Library.NEGINF;

      int nMinFrames = (rSubLen != null && rSubLen.a > 0) ? Math.max(1, rSubLen.a) : 1;
      int nMaxFrames = (rSubLen != null && rSubLen.b > 0) ? Math.max(nMinFrames, rSubLen.b) : seq.length();

      if (span == null) span = new SpanList(0, T - 1, true);

      // check each range
      for(int iRange = 0; iRange < span.getNumSpans(); iRange++){
         Range r = span.getRange(iRange);
         int ia = r.a;
         int ib = r.b;

         // within the range, check each starting point
         for(int i = ia; i < ib; i += nStartStep){
            int iEnd = Library.min(ib, i + nMaxFrames + 1);
            Sequence sub = seq.subseq(i, iEnd);
            for(int j = 0; j < sub.length(); j++)
               Arrays.fill(m[j], Library.LOG_ZERO);
            viterbi(sub, m, par);

            // find the best end point
            for(int j = i + nMinFrames; j < iEnd; j++){
               int len = j - i;
               double v = m[len][iLast];
               if (bNorm) v /= (double)len;
               if (v > vBest){
                  vBest = v;
                  iBest = i;
                  jBest = j;
               }
            }
         }
      }

      if (iBest < 0) return null;
      else return new ScoredWindow(-1, iBest, jBest - iBest + 1, vBest);
   }

   @Override
   public double viterbi(Sequence seq, double m[][], int par[][])
   {
      int T = seq.length();
      int nStates = getNumStates();
      int iLast = nStates - 1;

      calcB(seq);

      // must start in first state
      m[0][0] = bmat[0][0];
      par[0][0] = -1;

      // fill in the trellis
      for(int t = 1; t < T; t++){
         // handle first state
         m[t][0] = m[t - 1][0] + tran[0][0] + bmat[0][t];
         par[t][0] = 0;

         // handle middle states
         for(int i = 1; i < nStates; i++){
            int iBest = i;
            double vBest = m[t - 1][i] + tran[i][0];
            for(int j = 1; j <= i; j++){
               int dij = i - j;
               if (j >= tran[dij].length) continue; // no trans from state(i-j) -> state(i)
               double v = m[t - 1][dij] + tran[dij][j];
               if (v > vBest){
                  vBest = v;
                  iBest = dij;
               }
            }
            m[t][i] = vBest + bmat[i][t];
            par[t][i] = iBest;
         }
      }

      // extract path
      path = new int[T];
      path[T - 1] = iLast;
      for(int t = T - 2; t >= 0; t--)
         path[t] = par[t + 1][path[t + 1]];

      // must end in last state
      return m[T - 1][iLast];
   }
}
