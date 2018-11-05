package kdm.models;

import java.io.*;
import java.util.ArrayList;

import kdm.data.DiscreteSeq;
import kdm.data.FeatureVec;
import kdm.data.Sequence;
import kdm.util.*;

/**
 * Abstract base class for hidden Markov models
 */
public abstract class AbstractHMM extends ProbSeqModel
{
   protected String name = "anon";
   protected int nDims = 0;

   protected ProbFVModel[] states;

   /** alpha[t][iState] */
   protected transient double alpha[][];
   /** beta[t][iState] */
   protected transient double beta[][];
   /** p(obs)[iState][t] */
   protected transient double bmat[][];
   protected transient int[] path;
   protected transient Sequence seqLastCalcB = null;

   /** loglik of starting in a particular state */
   protected double[] piStart;

   /** loglik of ending in a given state */
   protected double[] piEnd;

   /** loglik of leaving each state assuming we're in that state and it's an end state (used for HTK) */
   protected double[] piLeave;

   protected int NMAX_ITER = 50;
   protected double CONVERGE_THRESH = 1e-3;

   public AbstractHMM(int nDims)
   {
      this.nDims = nDims;
   }

   public String getName()
   {
      return name;
   }

   public void setName(String name)
   {
      this.name = name;
   }

   public int getNumStates()
   {
      return states.length;
   }

   public double[] getPiStart()
   {
      return piStart;
   }

   public double getPiStart(int i)
   {
      return piStart[i];
   }

   public void setPiStart(int i, double loglik)
   {
      piStart[i] = loglik;
   }

   public double[] getPiEnd()
   {
      return piEnd;
   }

   public double getPiEnd(int i)
   {
      return piEnd[i];
   }

   public void setPiEnd(int i, double loglik)
   {
      piEnd[i] = loglik;
   }

   public double[] getPiLeave()
   {
      return piLeave;
   }

   public void setPiLeave(int i, double loglik)
   {
      piLeave[i] = loglik;
   }

   public double getPiLeave(int i)
   {
      return piLeave[i];
   }

   // TODO should have a setFullTransMatrix since you can't assume that the instantiation will return a reference
   
   /** @return full transition matrix m[<src state>][<dest state>] = log(transition) */
   public abstract double[][] getFullTransMatrix();

   /**
    * Return the path as compute by viterbi alignment.
    */
   public final int[] getPath()
   {
      return path;
   }
   
   /** specify the maximum number of EM iterations for learning */
   public void setMaxIters(int n)
   {
      NMAX_ITER = n;
   }

   /** @return dimensionality of the observations */
   public int getNumDims()
   {
      return nDims;
   }

   /** set the given state's observation distribution */
   public void setState(int iState, ProbFVModel model)
   {
      states[iState] = model;
   }

   /** @return observation distribution for each state */
   public ProbFVModel[] getStates()
   {
      return states;
   }

   /** @return observation distribution for the given state */
   public ProbFVModel getState(int iState)
   {
      return states[iState];
   }

   public boolean isStartState(int iState)
   {
      return (piStart[iState] > Library.LOG_ZERO);
   }

   public boolean isEndState(int iState)
   {
      return (piEnd[iState] > Library.LOG_ZERO);
   }

   /**
    * Computes a matrix that contains p(O_i|S_j) where O_i is the ith data point in the sequence and S_j is
    * the jth states of the HMM.
    */
   public void calcB(Sequence seq)
   {
      if (seq == seqLastCalcB) return;
      int nStates = getNumStates();

      int T = seq.length();
      bmat = new double[nStates][T];
      for(int t = 0; t < T; t++){
         FeatureVec fv = seq.get(t);
         for(int i = 0; i < nStates; i++){
            bmat[i][t] = states[i].eval(fv);
            assert(!Double.isNaN(bmat[i][t])) : String.format("bmat=NaN! iState=%d t=%d\n fv=%s\n states=%s",i,t, fv, states[i]);
         }
      }
      seqLastCalcB = seq;
   }

   /** reset bmat and ensure it's recalculated on next request */
   public void resetCalcB()
   {
      seqLastCalcB = null;
      bmat = null;
   }

   /**
    * Compute the forward (alpha) matrix for the given sequence.
    */
   protected abstract void calcAlpha(Sequence seq);

   /**
    * Compute the backward (beta) matrix for the given sequence.
    */
   protected abstract void calcBeta(Sequence seq);

   /** @return log likelihood of the sequence given this model */
   public abstract double eval(Sequence seq);

   /**
    * Compute the probability of the set of sequences given this model. The probability is computed via the
    * forward algorithm for each sequence.
    */
   public double eval(ArrayList<? extends Sequence> v)
   {
      double ret = Library.LOG_ONE;
      for(Sequence seq : v)
         ret += eval(seq);
      return ret;
   }

   /**
    * Align this model to the given sequence via the Viterbi algorithm.
    * 
    * @return loglik of the optimal path
    */
   public double viterbi(Sequence seq)
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
   public abstract double viterbi(Sequence seq, double m[][], int par[][]);

   /**
    * convenience method for segmental k-means initialization from a single example
    * 
    * @see train_init_segk(ArrayList<? extends Sequence>)
    */
   public void init_segk(Sequence vtrain)
   {
      ArrayList<Sequence> a = new ArrayList<Sequence>();
      a.add(vtrain);
      init_segk(a);
   }
   
   /**
    * convenience method for segmental k-means initialization from a single example with overlapping segments
    * 
    * @see train_init_segk(ArrayList<? extends Sequence>)
    */
   public void init_segk_overlap(Sequence vtrain, double percentOverlap)
   {
      ArrayList<Sequence> a = new ArrayList<Sequence>();
      a.add(vtrain);
      init_segk_overlap(a, percentOverlap);
   }

   /**
    * Initialize this model using the segmental k-means algorithm and the given data set.
    */
   public abstract void init_segk(ArrayList<? extends Sequence> vtrain);

   /**
    * Initialize this model using the segmental k-means algorithm and the given data set.
    */
   public abstract boolean init_segk_overlap(ArrayList<? extends Sequence> vtrain, double percentOverlap);
   
   /**
    * convenience method for viterbi training on a single example
    * 
    * @return number of iterations
    * @see train_viterbi(ArrayList<? extends Sequence>)
    */
   public int train_viterbi(Sequence vtrain)
   {
      ArrayList<Sequence> a = new ArrayList<Sequence>();
      a.add(vtrain);
      return train_viterbi(a);
   }

   /**
    * Reestimate the parameters of this model using Viterbi alignment.
    * 
    * @return number of iterations
    */
   public abstract int train_viterbi(ArrayList<? extends Sequence> vtrain);

   /**
    * convenience method for baulm-welch training on a single example
    * 
    * @return number of iterations
    * @see train_bw(ArrayList<? extends Sequence>)
    */
   public int train_bw(Sequence vtrain)
   {
      ArrayList<Sequence> a = new ArrayList<Sequence>();
      a.add(vtrain);
      return train_bw(a);
   }

   /**
    * Reestimate the parameters of this model using the Baum-Welch algorithm and the given data set.
    * Typically, you will initialize the HMM manually or through init_segk before calling this method.
    * 
    * @return number of iterations
    * @see init_segk(ArrayList<? extends Sequence>)
    */
   public abstract int train_bw(ArrayList<? extends Sequence> vtrain);
}
