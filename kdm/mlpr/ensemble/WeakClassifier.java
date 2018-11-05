package kdm.mlpr.ensemble;

import kdm.data.FeatureVec;
import kdm.data.Sequence;
import kdm.mlpr.*;
import kdm.mlpr.classifier1D.DecisionStump;
import kdm.mlpr.classifier1D.DecisionStump.LearnMethod;

/**
 * Contains information for a weak classifier that can be used within an ensemble method.
 */
public abstract class WeakClassifier
{
   protected double alpha;

   public double getAlpha()
   {
      return alpha;
   }

   public void setAlpha(double alpha)
   {
      this.alpha = alpha;
   }

   public void copyFrom(WeakClassifier wc)
   {
      this.alpha = wc.alpha;
   }

   /**
    * learn parameters of this classifier
    * 
    * @param iClass index of positive class
    * @param examples list of examples to learn over
    * @param weights weights of the example data points
    * @param priorPos prior probability of positive class
    * @param meta extra information specific to this learner
    * @return error rate
    */
   public abstract double learn(int iClass, Sequence[] examples, double[] weights, double priorPos,
         Object meta);

   /** @return POS or NEG */
   public abstract int classify(FeatureVec fv);

   /**
    * @return deep copy of this weak classifier
    * @see fromText
    */
   public abstract WeakClassifier dup();

   /**
    * @return text representation of this weak learner; should end with "|\<class\>]" so we know which kind
    *         of weak learner to construct
    */
   public abstract String toText();

   /**
    * load params of this weak classifier from a textual representation
    * 
    * @see toText
    */
   public abstract boolean fromText(String s);
}
