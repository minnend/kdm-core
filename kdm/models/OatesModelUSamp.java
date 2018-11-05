package kdm.models;

import kdm.data.*;
import kdm.util.*;
import kdm.io.*;
import kdm.models.misc.MapStartScore;

import java.util.*;
import java.io.*;
import java.text.*;

/**
 * This class represents the model proposed in (Oates 2002). Each <i>pattern element</i> is represented by a
 * Gaussian distribution (one for each dimension) and the expected time between pattern elements is also
 * represented as a Gaussian.
 */
public class OatesModelUSamp extends ProbSeqModel implements Configurable
{
   protected Gaussian1D model[][]; // indices: (element index, dimension)
   protected Multinomial dtime[]; // prob of
   protected int nDims; // num dimensions
   protected int nLength; // num time steps

   transient public double costm[][]; // dp cost matrix
   transient public int parm[][]; // dp parent index matrix

   // protected double dtPrior[] = new double[] { 1, 20, 5, 2 };
   // protected double dtPrior[] = new double[] { 1, 40, 20, 5 };
   //protected double dtPrior[] = new double[] { 1, 80, 40, 5 };
   protected double dtPrior[] = new double[] { 1, 200, 50, 5 }; // TODO what's best? can we estimate it?
   //protected double dtPrior[] = new double[] { .01, .4, .1, .01 };
   protected int DTimeBins = dtPrior.length;
   protected FeatureVec initValueVar; // we need these variables to allow configuration
   protected FeatureVec minValueVar;

   protected final static Gaussian1D.Report report = Gaussian1D.Report.error;
   protected final static double DefInitValueVar = 0.1;
   protected final static double DefMinValueVar = 1e-9;
   protected final static int MaxEMIters = 10;
   protected final static double PruneLogLikDiff = -28.0;

   /**
    * Return the length (number of pattern elements) in this model.
    */
   public int size()
   {
      return nLength;
   }

   /**
    * Return the length (number of pattern elements) in this model.
    */
   public int length()
   {
      return nLength;
   }

   public Gaussian1D getModel(int i)
   {
      return model[i][0];
   }

   public Gaussian1D getModel(int i, int j)
   {
      return model[i][j];
   }

   public Multinomial getDTime(int i)
   {
      return dtime[i];
   }

   /**
    * Build an empty model
    */
   public OatesModelUSamp()
   {
      nLength = 0;
      nDims = 0;
   }

   /**
    * Build an empty model with the given length, dimensionality, and default variance settings.
    */
   public OatesModelUSamp(int _nLength, int _nDims)
   {
      this(_nLength, _nDims, DefInitValueVar, DefMinValueVar);
   }

   /**
    * Build an empty model with the given length, dimensionality, and variance parameters.
    */
   public OatesModelUSamp(int _nLength, int _nDims, double initv, double minv)
   {
      this(_nLength, _nDims, new FeatureVec(_nDims, initv), new FeatureVec(_nDims, initv));
   }

   /**
    * Build an empty model with the given length, dimensionality, and variance parameters.
    */
   public OatesModelUSamp(int _nLength, int _nDims, FeatureVec initv, FeatureVec minv)
   {
      nLength = _nLength;
      nDims = _nDims;
      setVarConstants(initv, minv);
      model = new Gaussian1D[nLength][nDims];
      for(int i = 0; i < nLength; i++)
         for(int j = 0; j < nDims; j++){
            model[i][j] = new Gaussian1D(0.0, initValueVar.get(j), minValueVar.get(j));
            model[i][j].setReport(report);
         }
      dtime = new Multinomial[nLength - 1];
      for(int i = 0; i < nLength - 1; i++)
         dtime[i] = new Multinomial(dtPrior);
   }

   /**
    * Build an Oates model from the entire sequence
    */
   public OatesModelUSamp(Sequence seq, FeatureVec initv, FeatureVec minv)
   {
      this(seq, new WindowLocation(0, seq.length()), initv, minv);
   }

   /**
    * Build an Oates model from the given time range
    */
   public OatesModelUSamp(Sequence seq, WindowLocation win, FeatureVec initv, FeatureVec minv)
   {
      int ia = win.start();
      nLength = win.length();
      nDims = seq.getNumDims();
      setVarConstants(initv, minv);
      model = new Gaussian1D[nLength][nDims];
      for(int i = 0; i < nLength; i++)
         for(int j = 0; j < nDims; j++){
            model[i][j] = new Gaussian1D(seq.get(ia + i, j), initValueVar.get(j), minValueVar.get(j));
            model[i][j].setReport(report);
         }
      dtime = new Multinomial[nLength - 1];
      for(int i = 0; i < nLength - 1; i++)
         dtime[i] = new Multinomial(dtPrior);
   }

   /**
    * Build an Oates model from an existing Oates model.
    */
   public OatesModelUSamp(OatesModelUSamp om)
   {
      copyFrom(om);
   }

   /**
    * Build an Oates model from a list of known patterns
    * 
    * @param pats known patterns
    * @param nMinModelLen minimum model length
    */
   public OatesModelUSamp(ArrayList<Sequence> pats, int nMinModelLen)
   {
      int n = pats.size();
      int nd = pats.get(0).getNumDims();
      FeatureVec initv = new FeatureVec(nd);
      FeatureVec minv = new FeatureVec(nd);
      GaussianDyn1D[] gm = new GaussianDyn1D[nd];
      for(int i = 0; i < nd; i++)
         gm[i] = new GaussianDyn1D();
      for(int i = 0; i < n; i++){
         Sequence seq = pats.get(i);
         int len = seq.length();
         for(int t = 0; t < len; t++){
            FeatureVec fv = seq.get(t);
            for(int d = 0; d < nd; d++)
               gm[d].add(fv.get(d), false);
         }
         for(int d = 0; d < nd; d++)
            gm[d].update();
      }
      for(int i = 0; i < nd; i++){
         initv.set(i, Math.max(gm[i].getVar() / 5.0, DefMinValueVar));
         minv.set(i, Math.max(gm[i].getVar() / 1000.0, DefMinValueVar));
      }

      init(pats, nMinModelLen, initv, minv);
   }

   /**
    * Build an Oates model from a list of known patterns
    * 
    * @param pats known patterns
    * @param nMinModelLen minimum model length
    * @param initv initial variance for each pat-el
    * @param minv minimum variance for each pat-el
    */
   public OatesModelUSamp(ArrayList<Sequence> pats, int nMinModelLen, FeatureVec initv, FeatureVec minv)
   {
      init(pats, nMinModelLen, initv, minv);
   }

   /**
    * initialize the parameters of this model using the given patterns (each sequence is a single instance of
    * the pattern)
    */
   protected void init(ArrayList<Sequence> pats, int nMinModelLen, FeatureVec initv, FeatureVec minv)
   {
      // TimerMS timer = new TimerMS();
      int nPats = pats.size();
      setVarConstants(initv, minv);
      assert (nPats > 0) : "can't initialize OM model from zero patterns";

      // TODO need to verify and time this procedure
      // TODO should do multiple alignment directly

      // we need to choose a pattern from which to initialize the model.
      // There are 2 main constraints:
      // 1) model can't be too long to map to shortest pattern
      // 2) model can't be too short to map to longest pattern

      // find the min/max pattern length
      SeqListInfo sli = new SeqListInfo(pats);
      assert (nMinModelLen <= sli.maxLen);
      int nMaxJump = DTimeBins - 1; // the farthest step between pat-el mappings
      int nMinPat = (int)Math.ceil((double)(sli.maxLen - 1) / (double)nMaxJump) + 1;
      int nWantLen = Library.max(sli.getAvgLen(), nMinPat, nMinModelLen);

      // find the sequence that's closest in length to what we want
      Sequence seqInit = null;
      int dist = -1;
      for(int i = 0; i < nPats; i++){
         int len = pats.get(i).length();
         if (len < nMinPat) continue;
         int d = (int)Math.abs(len - nWantLen);
         if (dist < 0 || d < dist){
            seqInit = pats.get(i);
            dist = d;
         }
      }
      assert (seqInit != null);

      // build a model from the selected pattern
      OatesModelUSamp om = new OatesModelUSamp(seqInit, initv, minv);
      copyFrom(om);

      // align the base model with all of the patterns
      OatesMapping[] maps = align(pats);

      // now re-est model from the alignments
      int nDims = seqInit.getNumDims();
      double[] adata = new double[nPats];
      int[] atime = new int[nPats];
      for(int i = 0; i < om.nLength; i++){
         for(int j = 0; j < nDims; j++){
            for(int k = 0; k < nPats; k++){
               int iDat = maps[k].imap[i];
               adata[k] = pats.get(k).get(iDat, j);
               if (j == 0 && i < om.nLength - 1){
                  int iDat2 = maps[k].imap[i + 1];
                  atime[k] = iDat2 - iDat;
               }
            }
            om.model[i][j].learn(adata);
            if (j == 0 && i < om.nLength - 1) om.dtime[i].learn(atime);
         }
      }

      // now copy the contructed model into this model
      copyFrom(om);

      // System.err.println("time to init from examples: "+timer.time()+" ms");
   }

   public ProbSeqModel build(ArrayList<Sequence> examples, String sConfig)
   {
      config(sConfig); // this will set "this" model's values
      return new OatesModelUSamp(examples, 0, initValueVar, minValueVar);
   }

   public ProbSeqModel build(Sequence seq, WindowLocation win, String sConfig)
   {
      config(sConfig); // this will set "this" model's values
      return new OatesModelUSamp(seq, win, initValueVar, minValueVar);
   }

   /**
    * Copy the parameters from the given om into this model.
    */
   public void copyFrom(OatesModelUSamp om)
   {
      if (nLength != om.nLength || nDims != om.nDims){
         nLength = om.nLength;
         nDims = om.nDims;
         model = new Gaussian1D[nLength][nDims];
         for(int i = 0; i < nLength; i++)
            for(int j = 0; j < nDims; j++){
               model[i][j] = new Gaussian1D(om.model[i][j]);
               model[i][j].setReport(report);
            }
         dtime = new Multinomial[nLength - 1];
         for(int i = 0; i < nLength - 1; i++)
            dtime[i] = new Multinomial(om.dtime[i]);
      }
      else{
         for(int i = 0; i < nLength; i++)
            for(int j = 0; j < nDims; j++)
               model[i][j].copyFrom(om.model[i][j]);
         for(int i = 0; i < nLength - 1; i++)
            dtime[i].copyFrom(om.dtime[i]);
      }

      dtPrior = om.dtPrior.clone();
      DTimeBins = om.DTimeBins;
      initValueVar = new FeatureVec(om.initValueVar);
      minValueVar = new FeatureVec(om.minValueVar);
   }

   public boolean config(ConfigHelper chelp, String sKey, String sVal)
   {
      // TODO: currently, there aren't any configurable parameters
      System.err.println("Error: unrecognized OatesModelUSamp parameters: " + sKey);
      assert false;
      return false;
   }

   public boolean config(String s)
   {
      // grrr: no multiple inheritance in java
      ConfigHelper chelp = new ConfigHelper();
      return chelp.config(s, this);
   }

   public void setVarConstants(double initv, double minv)
   {
      initValueVar.fill(initv);
      minValueVar.fill(minv);
   }

   public void setVarConstants(FeatureVec initv, FeatureVec minv)
   {
      initValueVar = new FeatureVec(initv);
      minValueVar = new FeatureVec(minv);
   }

   public double eval(Sequence seq)
   {
      OatesMapping omap = align(seq);
      if (omap == null){
         // System.err.println("Warning: Can't align model to given sequence");
         // System.err.println("model: "+this);
         // System.err.println("sequence: "+seq);
         return Double.NEGATIVE_INFINITY;
      }
      return omap.score;
   }

   /**
    * Evaluate the probability of the feature vector at the specified time index in the model; return the
    * log-probability
    */
   public double eval(int i, FeatureVec fv)
   {
      double v = 0.0;
      for(int j = 0; j < nDims; j++)
         v += model[i][j].eval(fv.get(j));
      return v;
   }

   /**
    * Evaluate the error of the feature vector at the specified time index in the model
    */
   public double evalError(int i, FeatureVec fv)
   {
      double v = 0.0;
      for(int j = 0; j < nDims; j++)
         v += model[i][j].evalError(fv.get(j));
      return v;
   }

   public double[] getFinalCost()
   {
      return costm[nLength - 1];
   }

   public double getCost(int iEnd)
   {
      return costm[nLength - 1][iEnd];
   }

   /**
    * Fills in the cost and parent matrices mapping this pattern to each span in the given list.
    */
   public void buildPatternMap(Sequence seq, SpanList spans, boolean bCalcFull)
   {
      // alloc cost/par matrices to hold all spans
      int b = spans.getSpanMax();
      int n = b + 1;

      if (bCalcFull){
         costm = Library.allocMatrixDouble(costm, nLength, n, Double.NEGATIVE_INFINITY);
         parm = Library.allocMatrixInt(parm, nLength, n, -1);
      }

      n = spans.getNumSpans();
      for(int i = 0; i < n; i++){
         Range r = spans.getRange(i);
         if (bCalcFull || r.isDirty()){
            for(int j = 0; j < nLength; j++){
               Arrays.fill(costm[j], r.a, r.b + 1, Double.NEGATIVE_INFINITY);
               Arrays.fill(parm[j], r.a, r.b + 1, -1);
            }
            buildPatternMap(seq, r.a, -1, r.b, false);
         }
      }
   }

   /**
    * Fills in the cost and parent matrices mapping this pattern to the given sequence.
    */
   public void buildPatternMap(Sequence seq, int iMinStart, int iForcedStart, int iMaxEnd)
   {
      buildPatternMap(seq, iMinStart, iForcedStart, iMaxEnd, true);
   }

   /**
    * Fills in the cost and parent matrices mapping this pattern to the given sequence; the mapping is to the
    * full sequence (ie, not a subsequence)
    */
   public void buildPatternMap(Sequence seq)
   {
      buildPatternMap(seq, 0, 0, seq.length() - 1, true);
   }

   /**
    * Fills in the cost and parent matrices mapping this pattern to the given sequence.
    */
   public void buildPatternMap(Sequence seq, int iMinStart, int iForcedStart, int iMaxEnd, boolean bAlloc)
   {
      assert ((iMinStart == -1 && iForcedStart >= 0) || (iMinStart >= 0 && iForcedStart == -1)
            || (iMinStart == -1 && iForcedStart == -1) || (iMinStart <= iForcedStart)) : "im: " + iMinStart
            + "  if: " + iForcedStart;
      assert (iMaxEnd < seq.length());

      int nSeq, i, j, k, n;

      // no need to compute past the forced end
      if (iMaxEnd >= 0) nSeq = iMaxEnd + 1;
      else nSeq = seq.length();

      // note: 'cost' is a misnomer -- we actually want to maximize the log-likelihood
      if (bAlloc){
         costm = Library.allocMatrixDouble(costm, nLength, nSeq, Double.NEGATIVE_INFINITY);
         parm = Library.allocMatrixInt(parm, nLength, nSeq, -1);
      }

      // maximum "jump" in time from one pat-el to the next
      int td = DTimeBins - 1;

      // fill in the first row
      if (iForcedStart >= 0) costm[0][iForcedStart] = eval(0, seq.get(iForcedStart));
      else{
         // can't skip any starting positions in non-forced-start mode
         if (iMinStart < 0) iMinStart = 0;
         for(j = iMinStart; j < nSeq; j++)
            costm[0][j] = eval(0, seq.get(j));
      }

      int iStart = (iForcedStart < 0 ? iMinStart : iForcedStart);
      double dtcache[] = new double[DTimeBins];
      double c, c1, c2, c3;

      // now fill in the rest of the matrix
      for(i = 1; i < nLength; i++) // loop for each pattern element
      {
         int iPar = i - 1;
         for(int x = 0; x < DTimeBins; x++)
            dtcache[x] = dtime[iPar].eval(x);
         for(j = iStart; j < nSeq; j++) // loop for each data frame
         {
            c1 = eval(i, seq.get(j));
            // loop over each possible parent
            for(k = (int)Math.max(iStart, j - td); k <= j; k++){
               c2 = dtcache[j - k];
               c3 = costm[iPar][k];
               c = c1 + c2 + c3;
               if (c > costm[i][j]){
                  costm[i][j] = c;
                  parm[i][j] = k;
               }
            }
         }
      }
   }

   public MapStartScore findMapStart(Sequence seq, int iEnd)
   {
      int iStart = (int)Math.max(0, iEnd - nLength * (DTimeBins - 1));
      buildPatternMap(seq, iStart, -1, iEnd);
      OatesMapping omap = findMappingWithEnd(iEnd);
      return new MapStartScore(omap.getFirstDataIndex(), omap.score);
   }

   /**
    * Uses the cost and parent matrices to determine the best mapping that ends at the given position.
    */
   public OatesMapping findMappingWithEnd(int iEnd)
   {
      int i = nLength - 1;
      int j = iEnd;
      if (costm[i][j] == Double.NEGATIVE_INFINITY){
         // System.err.printf("neginf total cost (i=%d, j=%d)\n", i, j); // TODO: debug
         return null;
      }
      OatesMapping omap = new OatesMapping(costm[i][j], nLength);

      while(i >= 0){
         assert (j >= 0);
         omap.imap[i] = j;
         j = parm[i][j];
         i--;
      }

      return omap;
   }

   /**
    * Returns the best mapping from this model to some subsequence in the given sequence.
    */
   public OatesMapping findBestMapping(Sequence seq)
   {
      buildPatternMap(seq, 0, -1, -1);
      int iBest = 0, n = seq.length();
      double last[] = costm[nLength - 1];
      for(int i = 1; i < n; i++)
         if (last[i] > last[iBest]) iBest = i;
      OatesMapping omap = findMappingWithEnd(iBest);
      if (seq.hasParent()) omap.iSeries = seq.getParentIndex();
      return omap;
   }

   /**
    * Returns the best mapping from this model to some set of sequences; the mapping can be to any subsequence
    * in any of the sequences.
    */
   public OatesMapping findBestMapping(ArrayList<Sequence> vdata)
   {
      OatesMapping omapBest = null;

      for(int i = 0; i < vdata.size(); i++){
         Sequence seq = vdata.get(i);
         OatesMapping omap = findBestMapping(seq);
         if (omap == null) continue;
         omap.iSeries = i;
         if (omapBest == null || omap.score > omapBest.score) omapBest = omap;
      }
      return omapBest;
   }

   /**
    * Returns the best mapping form this model to the entire given sequence (i.e., it aligns the model to the
    * sequence rather than searching for the best map to a subsequence).
    */
   public OatesMapping align(Sequence seq)
   {
      buildPatternMap(seq);
      return findMappingWithEnd(seq.length() - 1);
   }

   /**
    * Align each of the given sequences to this model.
    * 
    * @param data list of sequences
    * @return list of mappings
    */
   public OatesMapping[] align(ArrayList<Sequence> data)
   {
      int n = data.size();
      OatesMapping[] maps = new OatesMapping[n];
      for(int i = 0; i < n; i++)
         maps[i] = align(data.get(i));
      return maps;
   }

   /**
    * Re-estimates the model parameters using an EM algorithm. The sequences passed to this method may contain
    * patterns as subsequences and don't necessarily contain only a pattern instance.
    */
   public void trainEM(ArrayList<Sequence> vdata, boolean bUpdateVar)
   {
      int nSeq = vdata.size();
      int nTotalFrames = 0;

      // compute total number of data frames
      for(int i = 0; i < nSeq; i++)
         nTotalFrames += vdata.get(i).length();

      // allocate/create storage space
      double[][][] mdata = new double[nLength][nDims][nTotalFrames];
      double[] weights = new double[nTotalFrames];
      double wmax;
      int[][] mtime = new int[nLength - 1][nTotalFrames];
      SpanList consider = new SpanList(0, nTotalFrames - 1, true);

      // iterate for EM
      TimerMS timer = new TimerMS();
      int iSeries = -1;
      Sequence seq = null;
      for(int iter = 0; iter < MaxEMIters; iter++){
         Arrays.fill(weights, Double.NEGATIVE_INFINITY);
         wmax = Double.NEGATIVE_INFINITY;

         timer.reset();
         consider.itReset();
         while(consider.itMore()){
            int iNext = consider.itNext();
            int iEnd = -1;
            int iBase = 0;
            for(int i = 0; i < vdata.size(); i++){
               int n = vdata.get(i).length();
               if (iNext < iBase + n){
                  if (iSeries != i){
                     iSeries = i;
                     seq = vdata.get(iSeries);
                     buildPatternMap(seq, 0, -1, -1);
                  }
                  iEnd = iNext - iBase;
                  break;
               }
               else iBase += n;
            }

            // e-step: compute loglik of ending at this location
            OatesMapping omap = findMappingWithEnd(iEnd);
            if (omap == null) continue;
            omap.iSeries = iSeries;
            assert omap.score > Double.NEGATIVE_INFINITY : "omap: " + omap;

            // m-step: build a new model given the p(end) and
            // values (actually, we just collect all the data
            // here and do the re-estimation later)
            for(int iPat = 0; iPat < nLength; iPat++){
               int iDat = omap.imap[iPat];
               FeatureVec fv = seq.get(iDat);
               for(int d = 0; d < nDims; d++)
                  mdata[iPat][d][iNext] = fv.get(d);
               if (iPat < nLength - 1) mtime[iPat][iNext] = omap.imap[iPat + 1] - omap.imap[iPat];
            }
            weights[iNext] = omap.score; // don't forget to convert loglik -> prob
            if (omap.score > wmax) wmax = omap.score;
         }

         // now compute real weights
         SpanList con2 = new SpanList(consider);
         double wsum = 0.0;
         while(con2.itMore()){
            int j = con2.itNext();
            double diff = weights[j] - wmax;
            if (diff < PruneLogLikDiff) consider.sub(j);
            else{
               weights[j] = Math.exp(diff);
               wsum += weights[j];
            }
         }

         // now that we have the values and weights over all sequences
         // and positions, we can re-estimate the model parameters
         for(int i = 0; i < nLength; i++){
            for(int j = 0; j < nDims; j++){
               double prevVar = model[i][j].getVar();
               if (!model[i][j].learn(mdata[i][j], weights, consider)){
                  System.err.println("Error: failed to init pattern element (" + i + "," + j + ")");
                  return;
               }
               if (!bUpdateVar) model[i][j].setVar(prevVar);
            }
            if (i < nLength - 1) dtime[i].learn(mtime[i], weights, consider);
         }

         if (bUpdateVar)
         {
            // finally, we blend the patel vars to avoid var -> 0
            int n = consider.size();
            for(int i = 0; i < nLength; i++)
               for(int j = 0; j < nDims; j++){
                  double v = Library.blendVar(model[i][j].getVar(), (double)wsum, initValueVar.get(j),
                        minValueVar.get(j));
                  model[i][j].setVar(v);
               }         
         }

         // TODO need to test for stopping criteria
         //System.err.printf("EM training (%d / %d) wsum=%.4f   iter=%d  time=%dms\n", consider.size(), nTotalFrames, wsum, iter + 1, timer.time());
      }
   }

   /**
    * Create a new feature vector from the distribution represented by the <code>iPat</code> pattern element
    * (zero-based).
    */
   protected FeatureVec sample(int iPat)
   {
      FeatureVec fv = new FeatureVec(nDims);
      for(int d = 0; d < nDims; d++)
         fv.set(d, model[iPat][d].sample().get(0));
      return fv;
   }

   /**
    * Create a new sequence from the distribution represented by this model.
    */
   public Sequence sample()
   {
      Sequence seq = new Sequence("Oates Sample", 1);
      FeatureVec prevFV = sample(0);
      seq.add(prevFV, Library.AppStartTime);
      for(int iPat = 1; iPat < nLength; iPat++){
         FeatureVec fv = sample(iPat);

         // figure out when the next sample is
         int dt = (int)Math.max(1, dtime[iPat - 1].samplei());

         // fill in any gaps using linear interpolation
         for(int i = 1; i < dt; i++){
            double p = (double)i / (double)dt;
            FeatureVec v = new FeatureVec(nDims);
            for(int d = 0; d < nDims; d++)
               v.set(d, p * fv.get(d) + (1.0 - p) * prevFV.get(d));
            seq.add(v);
         }

         // now add the sample from this pattern element
         seq.add(fv);
         prevFV = fv; // cur FV becomes prev FV
      }
      return seq;
   }

   /**
    * Write the dtime parameters to stderr
    */
   public void dumpDT()
   {
      for(int i = 0; i < dtime.length; i++){
         System.err.printf("%d) ", i);
         for(int t = 0; t < dtime[i].getSize(); t++)
            System.err.printf("%.4f  ", dtime[i].get(t));
         System.err.println();
      }
   }

   public String toString()
   {
      StringBuffer sb = new StringBuffer();
      sb.append("Means:\n");
      for(int i = 0; i < nLength; i++){
         for(int j = 0; j < nDims; j++)
            sb.append(String.format("%.4f ", model[i][j].getMean()));
         sb.append("\n");
      }
      sb.append("Variance:\n");
      for(int i = 0; i < nLength; i++){
         for(int j = 0; j < nDims; j++)
            sb.append(String.format("%.6f ", model[i][j].getVar()));
         sb.append("\n");
      }
      return sb.toString();
   }

   /**
    * Returns a 2D arrays of sequences representing a lower and upper bound for each model dimension. The
    * bounds are 'dsig' standard deviations away from the model mean.
    * 
    * @return seq[0..(nDims-1)][0..1] (lower / upper bound)
    */
   public Sequence bounds(double dsig)
   {
      Sequence viz = new Sequence("bounds", 1);

      for(int i = 0; i < nLength; i++){
         FeatureVec fv = new FeatureVec(nDims * 2);
         for(int j = 0; j < nDims; j++){
            fv.set(j * 2, model[i][j].getMean() - dsig * model[i][j].getSDev());
            fv.set(j * 2 + 1, model[i][j].getMean() + dsig * model[i][j].getSDev());
         }
         if (i == 0) viz.add(fv, Library.AppStartTime);
         else viz.add(fv);
      }

      return viz;
   }
}
