package kdm.tools;

import kdm.data.*;
import kdm.util.*;
import kdm.models.*;

/**
 * Stores parameters for training, which is useful for checking if re-training is
 * necessary
 */
public class TrainInfo
{
   public String sDataSrc;
   public SupTest.Model model;
   public SupTest.HmmTrain hmmTrain;
   public int nHmmStates, nHmmSkip;
   public boolean bNorm; // TODO: incorporate scoring into equals/hashcode?  how to compute if mismatch?
   public SupTest.ScoreMethod scoreMethod;
   public ProbSeqModel[] models;
   public GaussianDyn1D[] pScores;

   /**
    * Initialize this TI from SupTest with iScore=0
    */
   public TrainInfo(String _sDataSrc)
   {
      this(_sDataSrc, SupTest.scoreMethod);
   }
   
   /**
    * Initialize this TI from SupTest using the given value for iScore
    */
   public TrainInfo(String _sDataSrc, SupTest.ScoreMethod _scoreMethod)
   {
      sDataSrc = _sDataSrc;
      model = SupTest.model;
      hmmTrain = SupTest.hmmTrain;
      nHmmStates = SupTest.nHmmStates;
      nHmmSkip = SupTest.nHmmSkip;
      bNorm = SupTest.bNorm;
      scoreMethod = _scoreMethod;
   }
   
   /**
    * Inititalize this TI from the given parameters
    */
   public TrainInfo(String _sDataSrc, SupTest.Model _model, SupTest.HmmTrain _hmmTrain, int _nHmmStates, int
         _nHmmSkip, boolean _bNorm, SupTest.ScoreMethod _scoreMethod)
   {
      init(_sDataSrc, _model, _hmmTrain, _nHmmStates, _nHmmSkip, _bNorm, _scoreMethod);
   }
   
   void init(String _sDataSrc, SupTest.Model _model, SupTest.HmmTrain _hmmTrain,
         int _nHmmStates, int _nHmmSkip, boolean _bNorm, SupTest.ScoreMethod _scoreMethod)
   {
      sDataSrc = _sDataSrc;
      model = _model;
      hmmTrain = _hmmTrain;
      nHmmStates = _nHmmStates;
      nHmmSkip = _nHmmSkip;
      bNorm = _bNorm;
      scoreMethod = _scoreMethod;
   }

   /**
    * Copy settings from this object into the static fields of SupTest
    */
   public void updateSupTest()
   {
      SupTest.model = model;
      SupTest.hmmTrain = hmmTrain;
      SupTest.nHmmStates = nHmmStates;
      SupTest.nHmmSkip = nHmmSkip;
      SupTest.bNorm = bNorm;
      SupTest.scoreMethod = scoreMethod;
   }
   
   /**
    * Set the models and pScores values -- arguments will be cloned, not assigned directly
    */
   public void setDetails(ProbSeqModel[] _models, GaussianDyn1D[] _pScores)
   {
      models = _models.clone();
      pScores = _pScores.clone();
   }
   
   /** @return number of class models stored in this object */
   public int getNumClasses()
   {
      assert(models.length == pScores.length) : String.format("mismatch: %d vs %d", models.length, pScores.length);
      return models.length;
   }
   
   public int hashCode()
   {
      int hash = sDataSrc.hashCode();
      
      if (model == SupTest.Model.oates) hash += 0;
      else if (model == SupTest.Model.erp) hash += 10000;
      else if (model == SupTest.Model.hmm) hash += 20000;
      else if (model == SupTest.Model.dhmm) hash += 30000;
      else if (model == SupTest.Model.dtw) hash += 40000;
      else if (model == SupTest.Model.zip) hash += 50000;
      
      if (model == SupTest.Model.hmm || model == SupTest.Model.dhmm)
      {
         hash += (hmmTrain == SupTest.HmmTrain.bw ? 0 : 5000);
         hash += nHmmStates*100;
         hash += nHmmSkip;
      }
      
      return hash;
   }
   
   public boolean equals(Object o)
   {
      if (!(o instanceof TrainInfo)) return false;
      TrainInfo ti = (TrainInfo)o;

      if (sDataSrc!=null && ti.sDataSrc!=null && !sDataSrc.equals(ti.sDataSrc)) return false;
      if (model != ti.model) return false;
      if (model == SupTest.Model.hmm  || model==SupTest.Model.dhmm)
      {
         if (hmmTrain != ti.hmmTrain) return false;
         if (nHmmStates != ti.nHmmStates) return false;
         if (nHmmSkip != ti.nHmmSkip) return false;
      }
      
      return true;
   }

   public String toString()
   {
      return String.format("[src: %s  model: %s  hmmTrain: %s  #states: %d  #skip: %d  score: %s]",
            sDataSrc, model, hmmTrain, nHmmStates, nHmmSkip, scoreMethod);
   }
}
