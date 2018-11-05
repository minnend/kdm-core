package kdm.models;

import kdm.data.*;
import kdm.util.*;

import java.util.*;

/**
 * A left-right hmm with discrete observations
 */
public class HmmLRD extends AbstractHMMLR
{
   protected int nSymbols = 0;

   protected int NMAX_ITER = 50;
   protected double CONVERGE_THRESH = 1e-3;
   protected double BW_SYMBOL_PRIOR = 0.01;

   /**
    * Construct a default HMM with the specified number of hidden states and with obsevations with the given
    * number of symbols.
    */
   public HmmLRD(int _nStates, int _nSymbols)
   {
      this(_nStates, 0, _nSymbols);
   }

   /**
    * Construct a default HMM with the specified number of hidden states and with obsevations with the given
    * number of symbols. The transition matrix is setup so that each state i can only transition to states i+j
    * (j>=0, j<=nSkip+1).
    */
   public HmmLRD(int nStates, int nSkip, int nSymbols)
   {
      super(nStates, nSkip, nSymbols);

      states = new Multinomial[nStates];
      for(int i = 0; i < nStates; i++)
         states[i] = new Multinomial(nSymbols);
      tran = new double[nStates][];
   }

   /**
    * Find the best matching subsequence in the given sequence.
    * 
    * @param seq data to analyze
    * @param span ranges of the sequence to analyze
    * @return the best window
    */
   public ScoredWindow findBestSubseq(DiscreteSeq seq, SpanList span)
   {
      // TODO: run viterbi, but assume that each frame can start in the first state for free
      assert false : "not yet implemented -- use findBestSubseqSlow()";
      return null;
   }

   /**
    * Find the best matching subsequence in the given sequence by trying every possible subsequence (in the
    * allowed range).
    * 
    * @param seq data to analyze
    * @param span ranges of the sequence to analyze
    * @param bNorm normalize the subsequence scores by the length (divide by length)?
    * @param rSubLen range of subsequence lengths to test
    * @param nStartStep offset between start indices (i.e., delta_iStart)
    * @return the best window
    */
   public ScoredWindow findBestSubseqSlow(DiscreteSeq seq, SpanList span, boolean bNorm, Range rSubLen,
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

      int nMinFrames = Math.max(1, rSubLen.a);
      int nMaxFrames = Math.max(nMinFrames, rSubLen.b);

      if (span == null) span = new SpanList(0, T - 1, true);

      for(int iRange = 0; iRange < span.getNumSpans(); iRange++){
         Range r = span.getRange(iRange);
         int ia = r.a;
         int ib = r.b;

         for(int i = ia; i < ib; i += nStartStep){
            int iEnd = Library.min(ib, i + nMaxFrames + 1);
            DiscreteSeq sub = DiscreteSeq.wrap(seq.subseq(i, iEnd), seq.getNumSymbols());
            for(int j = 0; j < sub.length(); j++)
               Arrays.fill(m[j], Library.LOG_ZERO);
            viterbi(sub, m, par);
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
      else return new ScoredWindow(0, iBest, jBest - iBest, vBest);
   }

   /**
    * Align this model to the given sequence via the Viterbi algorithm.
    * 
    * @return loglik of the optimal path
    */
   public double viterbi(DiscreteSeq seq)
   {
      int T = seq.length();
      int nStates = getNumStates();
      double m[][] = Library.allocMatrixDouble(T, nStates, Library.LOG_ZERO);
      int par[][] = new int[T][nStates];
      return viterbi(seq, m, par);
   }

   /**
    * Align this model to the given sequence via the Viterbi algorithm. Use the given trellis and parent
    * matrices.
    * 
    * @param seq data to fit
    * @param m trellis matrix
    * @param par parent matrix for computing path
    * @return loglik of the optimal path
    */
   public double viterbi(DiscreteSeq seq, double m[][], int par[][])
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
            for(int j = 1; j <= i && j < tran[i - j].length; j++){
               double v = m[t - 1][i - j] + tran[i - j][j];
               if (v > vBest){
                  vBest = v;
                  iBest = i - j;
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

   /**
    * Initialize this model using bands. The data will be broken into five equal probability bands and a new
    * state is added for each band change. This function may change the number of states in this model.
    */
   public void init_bands(ArrayList<DiscreteSeq> vtrain)
   {
      assert false : "not yet implemented"; // TODO
   }

   @Override
   public void init_segk(ArrayList<? extends Sequence> vtrain)
   {
      assert (((DiscreteSeq)vtrain.get(0)).getNumSymbols() == nSymbols);
      int nStates = getNumStates();

      // divide each training sequence into nStates blocks and calc stats
      for(int i = 0; i < nStates; i++)
         ((Multinomial)states[i]).reset(1.0);
      for(Sequence seq : vtrain){
         int n = seq.length();
         for(int i = 0; i < nStates; i++){
            int a = i * n / nStates;
            int b = (i + 1) * n / nStates;
            int nObs = b - a + 1;
            for(int j = a; j < b; j++)
               ((Multinomial)states[i]).addProb(((DiscreteSeq)seq).geti(j), 1.0);
         }
      }
      for(int i = 0; i < nStates; i++)
         ((Multinomial)states[i]).normalize();

      // initialize the tran matrix to a valid default
      for(int i = 0; i < nStates - 1; i++){
         int nLeft = Library.min(nStates - i - 1, nSkip + 1);
         tran[i][0] = Math.log(0.95);
         double pSkip = 0.05 / nLeft;
         for(int j = 1; j < tran[i].length; j++)
            tran[i][j] = Math.log(pSkip);
      }
      tran[nStates - 1] = new double[] { Library.LOG_ONE };
   }

   @Override
   public boolean init_segk_overlap(ArrayList<? extends Sequence> vtrain, double percentOverlap)
   {
      // TODO Auto-generated method stub
      assert false : "nyi";
      return false;
   }

   @Override
   public int train_viterbi(ArrayList<? extends Sequence> vtrain)
   {
      assert (((DiscreteSeq)vtrain.get(0)).getNumSymbols() == nSymbols);
      int nSeq = vtrain.size();
      int nStates = getNumStates();
      int oldpath[][] = new int[nSeq][];
      Multinomial[] m = new Multinomial[nStates];
      int nObsPerState[] = new int[nStates];
      int nTran[][] = new int[nStates][];
      boolean bConverge = false;
      int iter = 0;

      // create the new multinomial and nTran data structures
      for(int i = 0; i < nStates; i++){
         nTran[i] = new int[tran[i].length];
         m[i] = new Multinomial(nSymbols);
      }

      for(; iter < NMAX_ITER && !bConverge; iter++){
         bConverge = true; // hope for the best

         // create / reset stat accumulators
         Arrays.fill(nObsPerState, 0);
         for(int i = 0; i < nStates; i++){
            Arrays.fill(nTran[i], 0);
            m[i].reset(1.0);
         }

         // calc viterbi paths, accum stats
         for(int iSeries = 0; iSeries < nSeq; iSeries++){
            DiscreteSeq seq = (DiscreteSeq)vtrain.get(iSeries);
            int T = seq.length();
            calcB(seq);
            viterbi(seq);

            if (oldpath[iSeries] == null){
               oldpath[iSeries] = path.clone();
               bConverge = false;
            }
            else{
               if (bConverge) // check for (lack of) convergence
               {
                  for(int i = 0; i < T; i++){
                     if (path[i] != oldpath[iSeries][i]){
                        bConverge = false;
                        break;
                     }
                  }
               }
               oldpath[iSeries] = path.clone();
            }

            // accum sates based on viterbi path
            for(int i = 0; i < T; i++){
               int iState = path[i];
               nObsPerState[iState]++;
               if (i < T - 1) nTran[iState][path[i + 1] - iState]++;
               m[iState].addProb(seq.geti(i), 1.0);
            }
         }

         for(int i = 0; i < nStates; i++){
            // reest tran matrix
            for(int j = 0; j < tran[i].length; j++)
               tran[i][j] = Math.log((double)nTran[i][j] / nObsPerState[i]);

            // reest obs pdf
            m[i].normalize();
         }
      }
      return iter;
   }

   @Override
   public int train_bw(ArrayList<? extends Sequence> vtrain)
   {
      assert (((DiscreteSeq)vtrain.get(0)).getNumSymbols() == nSymbols);
      int nSeq = vtrain.size();
      int nStates = getNumStates();
      boolean bConverge = false;
      double prevLogProb = Library.NEGINF;
      int iter = 0;

      for(; iter < NMAX_ITER && !bConverge; iter++){
         bConverge = true;

         double logprob = 0.0;
         double[][] tran_acc = new double[nStates][];
         // gamma[i][t][j] = prob(in state j at time t for seq i)
         double[][][] gamma = new double[nSeq][][];
         double[] wsum = new double[nStates];

         for(int i = 0; i < nStates; i++)
            tran_acc[i] = new double[tran[i].length];

         for(int iSeq = 0; iSeq < nSeq; iSeq++){
            DiscreteSeq seq = (DiscreteSeq)vtrain.get(iSeq);
            int T = seq.length();
            int iLast = T - 1;
            int iLastState = nStates - 1;

            calcB(seq);

            // create gamma and xi storage
            gamma[iSeq] = new double[T][nStates];
            double[][][] xi = new double[iLast][nStates][];
            for(int t = 0; t < iLast; t++)
               for(int j = 0; j < nStates; j++)
                  xi[t][j] = new double[tran[j].length];

            // compute alpha and beta matrices
            calcAlpha(seq);
            calcBeta(seq);

            double logprob_i = alpha[iLast][iLastState];
            logprob += logprob_i;

            // compute gamma and xi from alpha and beta
            for(int t = 0; t < T; t++)
               for(int i = 0; i < nStates; i++){
                  gamma[iSeq][t][i] = alpha[t][i] + beta[t][i] - logprob_i;
                  if (t < iLast){
                     for(int j = 0; j < tran[i].length; j++)
                        xi[t][i][j] = alpha[t][i] + beta[t + 1][i + j] + tran[i][j] + bmat[i + j][t + 1]
                              - logprob_i;
                  }
               }

            for(int i = 0; i < nStates; i++){
               for(int t = 0; t < T; t++){
                  gamma[iSeq][t][i] = Math.exp(gamma[iSeq][t][i]);
                  wsum[i] += gamma[iSeq][t][i]; // don't count gamma[iSeq][iLast][i]
                  // for p(exit state i)
                  if (t < iLast){
                     for(int j = 0; j < tran[i].length; j++)
                        tran_acc[i][j] += Math.exp(xi[t][i][j]);
                  }
               }
            }
         }

         // check for convergence / error
         if (logprob < prevLogProb){
            // System.err.printf("\n*** Warning:\niter=%d) ll=%.8f prev=%.8f (%.8f)\n\n",
            // iter, logprob, prevLogProb, logprob-prevLogProb);
            break; // don't update params with worse estimates
         }
         if (logprob - prevLogProb > CONVERGE_THRESH) bConverge = false;
         prevLogProb = logprob;

         // normalize the weights
         for(int i = 0; i < nSeq; i++){
            int T = vtrain.get(i).length();
            for(int t = 0; t < T; t++)
               for(int j = 0; j < nStates; j++)
                  gamma[i][t][j] /= wsum[j];
         }

         // update the tran matrix and obs pdf
         for(int i = 0; i < nStates; i++){
            // update the transition matrix
            double sum = 0.0;
            for(int j = 0; j < tran[i].length; j++)
               sum += tran_acc[i][j];
            sum = Math.log(sum);
            for(int j = 0; j < tran[i].length; j++)
               tran[i][j] = Math.log(tran_acc[i][j]) - sum;

            // compute the new mean for this state
            ((Multinomial)states[i]).reset();
            for(int j = 0; j < nSeq; j++){
               DiscreteSeq seq = (DiscreteSeq)vtrain.get(j);
               int T = seq.length();
               for(int t = 0; t < T; t++)
                  ((Multinomial)states[i]).addProb(seq.geti(t), gamma[j][t][i]);
            }
            ((Multinomial)states[i]).normalize();
            ((Multinomial)states[i]).addPrior(Library.allocVectorDouble(nSymbols, BW_SYMBOL_PRIOR));
         }
      }
      return iter;
   }

   @Override
   public String toString()
   {
      StringBuffer sb = new StringBuffer();
      int nStates = getNumStates();
      sb.append(String.format("|DHMM %d states, %d symbols\n", nStates, nSymbols));
      for(int i = 0; i < nStates; i++){
         sb.append(String.format("| State %d:", i + 1));
         for(int j = 0; j < nSymbols; j++)
            sb.append(String.format(" (%.2f)", ((Multinomial)states[i]).get(j)));
         sb.append(String.format(" [%.2f", Math.exp(tran[i][0])));
         for(int j = 1; j < tran[i].length; j++)
            sb.append(String.format(" %.2f", Math.exp(tran[i][j])));
         sb.append("]");
         sb.append("\n");
      }
      return sb.toString();
   }

   public Sequence sample()
   {
      assert false : "not yet implemented"; // TODO
      return null;
   }

   /**
    * Builds (trains) a model from the given examples.
    */
   public ProbSeqModel build(ArrayList<Sequence> examples, String sConfig)
   {
      assert false : "not yet implemented"; // TODO
      return null;
   }

   /**
    * Builds (initializes) a model from the given sequence and time range.
    */
   public ProbSeqModel build(Sequence seq, WindowLocation win, String sConfig)
   {
      assert false : "not yet implemented"; // TODO
      return null;
   }
}
