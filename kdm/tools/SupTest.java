package kdm.tools;

import kdm.data.*;
import kdm.data.transform.*;
import kdm.io.*;
import kdm.io.DataLoader.*;
import kdm.io.Def.*;
import kdm.util.*;
import kdm.models.*;
import kdm.models.misc.*;
import kdm.metrics.*;

import java.util.*;
import java.io.*;

import gnu.getopt.*;
import org.apache.commons.math.stat.*;

/** Tool for evaluating methods for supervised learning */
public class SupTest
{
   public static enum Op {
      conf, sim, map, cont, wspot, stats, label
   }
   public static enum Per {
      cls, ex
   }
   public static enum Model {
      oates, erp, hmm, dhmm, dtw, zip, euc, absdist
   }
   public static enum ZNorm {
      global, seq, subseq, none
   }
   public static enum HmmEval {
      viterbi, forward
   }
   public static enum HmmTrain {
      viterbi, bw
   }
   public static enum Output {
      human, computer
   }
   public static enum ScoreMethod {
      direct, sigVar
   }

   public static final String sErrorTypes[] = new String[] { "Correct", "Insertion", "Deletion",
         "Substitution" };
   public static final int ICORRECT = 0;
   public static final int IINSERT = 1;
   public static final int IDELETE = 2;
   public static final int ISUBS = 3;

   public static boolean bVerbose = false;
   public static boolean bNorm = false;
   public static boolean bShowErr = false;
   public static boolean bClean = true;
   public static int splitW = 0, splitG = 0, splitO = 0;
   public static double splitT = 1e-3;
   public static int nFolds = 5;
   public static Op op = Op.conf;
   public static Per per = Per.cls;
   public static Model model = Model.hmm;
   public static String sOutPath = "./";
   public static ArrayList<DataTransform> trans = new ArrayList<DataTransform>();
   public static FeatureVec initv, minv;
   public static double rBand = Double.NaN;
   public static MetricModel.Method mmTrain = MetricModel.Method.Centroid;
   public static MetricSeq.LengthPrep lenPrep = MetricSeq.LengthPrep.none;
   public static MetricFV fvm = new AbsoluteDistFV(); // new EuclideanFV();
   public static ZNorm znorm = ZNorm.none;
   public static int nDims = 0;
   public static int nHmmStates = 5;
   public static int nHmmSkip = 1;
   public static HmmEval hmmEval = HmmEval.viterbi;
   public static HmmTrain hmmTrain = HmmTrain.bw;
   public static ScoreMethod scoreMethod = ScoreMethod.direct;
   public static Output output = Output.human;
   public static int nSymbols = -1; // number of symbols for discrete sequences
   public static double spotThresh = 0.1;
   public static int nSpotsMin = -1; // no min
   public static int nSpotsMax = -1; // no max
   public static Range rWordLen = null; // bounds on word length for word spotting?

   public static double INITV_DIV = 9.0;
   public static double MINV_DIV = 1000.0;

   public static int nTotalLabeledWords = 0;
   public static int nTotalLabeledFrames = 0;
   public static int nTotalFrames = 0;
   public static int nLabeledWords[];
   public static int nLabeledFrames[];

   // //////////////////////////////////////////////////////////

   public static void usage()
   {
      System.err.println();
      System.err.println("USAGE: java kdm.tools.SupTest [options] <data def file>");
      System.err.println();
      System.err.println(" Options:");
      System.err.println("  -v                      enable verbose output");
      System.err.println("  -nfolds                 number of folds for cross-val (" + nFolds + ")");
      System.err.println("  -seed                   seed for rng (def from clock)");
      System.err.println("  -output <human|comp>    Generate output for human or computer? (human)");
      System.err.println("  -norm                   normalize scores by model length (false)");
      System.err.println("  -showerr                show individual errors (for op=conf)");
      System.err.println("  -per <class|ex>         build model per class or per example (class)");
      System.err.println("  -op <op>                perform a specific operation");
      System.err.println("                           conf - build confusion matrix (def)");
      System.err.println("                           sim - compute similarity matrix");
      System.err.println("                           map - compute model/inst mappings");
      System.err.println("                           cont - continuous recognition");
      System.err.println("                           wspot - word spotting");
      System.err.println("                           stats - calc stats on the data/metric");
      System.err.println("                           label - calc labels for unlabeled series");
      System.err.println("  -model <model>          use the specified model");
      System.err.println("                           oates - model from PERUSE");
      System.err.println("                           erp - ERP distance metric");
      System.err.println("                           dtw - DTW distance metric");
      System.err.println("                           hmm - hidden Markov model (def)");
      System.err.println("                           dhmm - discrete hidden Markov model");
      System.err.println("                           comp - compression-based similarity");
      System.err.println("                           euc - (scaled) euclidean distance");
      System.err.println("                           absdist - (scaled) absolute distance");
      System.err.println("  -out                    directory in which to store files");
      System.err.println("                           (op==map only)");
      System.err.println("  -vardiv <initv:minv>    data variance divisor for init, min");
      System.err.println("                           model variance (5,1000)");
      System.err.println("  -band <frac>            Sakoe-Chiba band size for DTW/ERP (no band)");
      System.err.println("  -znorm <type>           kind of z-normalization to perform (none)");
      System.err.println("                           type = {global, seq, subseq}");
      System.err.println("  -hmmeval <type>         use \"viterbi\" (def) or \"forward\" algorithm?");
      System.err.println("  -hmmtrain <type>        use \"bw\" (def) or \"viterbi\" algorithm?");
      System.err.printf("  -hmmstates <n>          number of states in each HMM (%d)\n", nHmmStates);
      System.err.printf("  -hmmskip <n>            number of skip states in HMMs (%d)\n", nHmmSkip);
      System.err.println("  -nsymbols <n>           number of symbols in discrete data");
      System.err.println("  -wspot_min <n>          min number of words to spot (no min)");
      System.err.println("  -wspot_max <n>          max number of words to spot (no max)");
      System.err.printf("  -wspot_thresh <t>       threshold to stop word spotting (none)\n");
      System.err.printf("  -word_len <min:max>     word length boundaries for word spotting (none)\n");
      System.err.printf("  -scoremeth <type>       Method to compare p(D1|L1) to p(D2|L2) (direct)\n");
      System.err.println("                           type = {direct, divmean, sigvar}");
      System.err.println("  -presplit <w:t:g:o>     split sequences around gaps of zero variance");
      System.err.println("                           w = length of window for computing variance");
      System.err.println("                           t = threshold for equiv to zero");
      System.err.println("                           g = min length of gap for split");
      System.err.println("                           o = max length of positive gap to overlook");
      System.err.println("  -noclean                don't clean the data sequences");
      System.err.println("  -mmtrain <method>       metric model training method (def: centroid)");
      System.err.println("                           method = {centroid, nn}");
      System.err.println("  -lenprep <method>       adjustment for sequences with diff lengths");
      System.err.println("                           method = {none, extend, shrink}");
      System.err.println("  -trans <DataTrans>      add a transformation to the data");
      System.err.println();
   }

   /** @return SupTest.Model type from index */
   public static SupTest.Model getModelFromIndex(int iModel)
   {
      switch(iModel){
      case 0:
         return Model.oates;
      case 1:
         return Model.hmm;
      case 2:
         return Model.dhmm;
      }
      assert false; // we should never get here
      return Model.hmm;
   }

   /** @return index from SupTest.Model */
   public static int getIndexFromModel(Model model)
   {
      if (model == Model.oates) return 0;
      if (model == Model.hmm) return 1;
      if (model == Model.dhmm) return 2;
      return -1;
   }

   /** @return score method from index */
   public static SupTest.ScoreMethod getScoreFromIndex(int iScore)
   {
      switch(iScore){
      case 0:
         return ScoreMethod.direct;
      case 1:
         return ScoreMethod.sigVar;
      }
      assert false; // we should never get here
      return ScoreMethod.sigVar;
   }

   /** @return index from score method */
   public static int getIndexFromScore(ScoreMethod scoreMethod)
   {
      if (scoreMethod == ScoreMethod.direct) return 0;
      if (scoreMethod == ScoreMethod.sigVar) return 1;
      return -1;
   }

   /**
    * @return short model description (e.g., "hmm8v" -> HMM, 8 states, viterbi for training)
    */
   public static String getShortModelDesc()
   {
      if (model == Model.oates) return "oates";
      if (model == Model.hmm)
         return String.format("hmm:%d%s", nHmmStates, hmmTrain == HmmTrain.bw ? "bw" : "v");
      if (model == Model.dhmm)
         return String.format("dhmm:%d%s", nHmmStates, hmmTrain == HmmTrain.bw ? "bw" : "v");
      return "Unknown";
   }

   public static void dumpDataSummary(TreeMap<String, ArrayList<Sequence>> data)
   {
      System.err.println();
      System.err.printf("Data Summary (%d classes, %d examples, frames(lab/tot): %d/%d):\n", data.size(),
            nTotalLabeledWords, nTotalLabeledFrames, nTotalFrames);
      System.err.println("------------------------------------------------------------");
      int i = 0;
      Set<String> labels = data.keySet();
      Iterator<String> it = labels.iterator();
      while(it.hasNext()){
         String label = it.next();
         ArrayList<Sequence> list = data.get(label);
         int avglen = 0;
         int minlen = Integer.MAX_VALUE;
         int maxlen = 0;
         for(int j = 0; j < list.size(); j++){
            int x = list.get(j).length();
            avglen += x;
            if (x < minlen) minlen = x;
            if (x > maxlen) maxlen = x;
         }
         avglen /= list.size();
         System.err.printf("%2d) %20s: %3d   (len: %3d,%3d,%3d)\n", i + 1, label, list.size(), minlen,
               avglen, maxlen);
         i++;
      }
      System.err.println();
   }

   /**
    * Dump the given confusion matrix to STDOUT
    * 
    * @param m confusin matrix [nFoundClasses][nTrueClasses]
    * @param title title to use in header of output
    * @param classes name of each true class
    * @param nClass number of true occurrences of each class
    * @param bColumnHeaders output column headers (text)?
    */
   public static void dumpConfMatrix(int[][] m, String title, String classes[], int nClass[],
         boolean bColumnHeaders)
   {
      if (m == null) return;
      System.err.flush();
      System.out.flush();

      // compute some stats
      int nFoundClasses = m.length;
      if (nFoundClasses < 1){
         System.err.printf("Error: no results to report.\n");
         return;
      }

      int nTrueClasses = m[0].length;

      if (nClass == null){
         nClass = new int[nTrueClasses];
         for(int i = 0; i < nTrueClasses && i < nFoundClasses; i++)
            for(int j = 0; j < m[i].length; j++)
               nClass[i] += m[i][j];
      }

      int nExamples = 0;
      int nCorrect = 0;
      int nInsert = 0;
      int nSubs1 = 0; // S = said other class, really this class
      int nSubs2 = 0; // S' = said this class, really other class
      for(int i = 0; i < nTrueClasses - 1; i++){
         nExamples += nClass[i];
         if (i < nFoundClasses){
            nCorrect += m[i][i];
            for(int j = 0; j < nTrueClasses - 1; j++){
               if (i == j) continue;
               nSubs2 += m[i][j];
            }
            nInsert += m[i][nTrueClasses - 1];
         }
      }
      for(int i = 0; i < nFoundClasses; i++)
         for(int j = 0; j < nTrueClasses - 1; j++){
            if (i == j) continue;
            nSubs1 += m[i][j];
         }
      
      // N=C+D+S => D=N-C-S
      int nDeletions = nExamples-nCorrect-nSubs1;
      System.err.printf("NCIDSS': %d  %d  %d  %d  %d  %d\n", nExamples, nCorrect, nInsert, nDeletions, nSubs1, nSubs2);
      System.err.printf("S=%d  S'=%d   N=%d  NIDS=%d CI=%d\n", nSubs1, nSubs2, nExamples,
            nExamples-nInsert-nDeletions-nSubs1, nCorrect-nInsert);

      int nLongName = 0;
      for(int i = 0; i < nTrueClasses - 1; i++)
         if (classes[i].length() > nLongName) nLongName = classes[i].length();
      if (nFoundClasses > nTrueClasses)        
         nLongName = Math.max(nLongName, 9); // room for "Class XXX"

      double acc = 100.0 * (nCorrect - nInsert - nSubs1) / nExamples;
      double recall = (double)nCorrect / nExamples;
      double prec = (double)nCorrect / (nCorrect + nInsert + nSubs2);
      double f1 = (200.0 * recall*prec)/(recall+prec);
      recall *= 100.0;
      prec *= 100.0;

      // for computer output, just print the percent correct
      if (output == Output.computer){
         System.out.printf("%.4f %.4f %.4f %.4f", f1, acc, prec, recall);
         return;
      }

      // dump info header
      System.err.printf("%s: %.2f  %.2f  %.2f  %.2f (aprf)\n", title, acc, prec, recall, f1);
      System.err.flush();
      System.out.printf("%s (%d): %.1f%%  %.1f%%  %.1f%%  %.1f%% (aprf)\n", title, nExamples, acc, prec, recall, f1);
      for(int i = 0; i < 28 + nTrueClasses * 4; i++)
         System.out.print("-");
      System.out.println();

      // dump column titles
      if (bColumnHeaders){
         for(int p = 0; p < nLongName; p++){
            for(int i = 0; i < 23 + nLongName - p; i++)
               System.out.print(" ");
            for(int i = 0; i < nTrueClasses; i++){
               char c = ' ';
               int n = classes[i].length();
               String s = (i < classes.length ? classes[i] : "Insertions");
               if (n >= (nLongName - p)) c = s.charAt(nLongName - p - 1);
               System.out.printf("%c ", c);
               for(int g = 0; g < 2; g++)
                  System.out.print(" ");
            }
            System.out.println();
         }
         System.out.println();
      }

      // now we can dump the confusion matrices
      String sRowFormat = String.format("%%%ds: ", nLongName+1);
      for(int i = 0; i < nFoundClasses; i++){
         int nWrong = 0;
         String labeli = (i < classes.length ? classes[i] : String.format("Class %d", i + 1));
         System.out.printf(sRowFormat, labeli);
         for(int j = 0; j < nTrueClasses; j++)
            System.out.printf("%3d ", m[i][j]);
         if (i < nTrueClasses - 1)
            System.out.printf("  %.1f%%", 100.0
                  * (nClass[i] - (nClass[i] - m[i][i]) - (Library.sum(m[i]) - m[i][i])) / nClass[i]);
         System.out.println();
      }
      System.out.flush();
   }

   public static void dumpSimMatrix(double[][] m, String classes[])
   {
      for(int i = 0; i < m.length; i++){
         for(int j = 0; j < m[i].length; j++){
            System.out.printf("%e ", m[i][j]);
         }
         System.out.println();
      }
   }

   /**
    * Detects "silence" in the sequence according to the splitX parameters.
    * 
    * @param seq the sequence to analyze
    * @param out if non-null, silence labels will be written here
    * @return SpanList containing valid portion of sequence after silence is removed
    */
   public static SpanList presplit(Sequence seq, PrintWriter out)
   {
      // slide window and calc variance
      int n = seq.length();
      int ndims = seq.getNumDims();
      int a = 0;
      int i = splitW / 2;
      SpanList span = new SpanList(0, n - 1, false);
      while(a + splitW <= n){
         // all dimensions must have zero variance
         boolean bZero = true;
         for(int j = 0; j < ndims; j++){
            double[] x = seq.extractDim(j, a, splitW);
            double v = StatUtils.variance(x);
            if (v > splitT) // variance too large?
            {
               bZero = false;
               break;
            }
         }
         if (bZero) span.add(i);

         // slide the window
         a++;
         i++;
      }

      // add beginning and end if appropriate
      if (span.contains(splitW / 2)) span.add(0, splitW / 2);
      if (span.contains(n - splitW)) span.add(n - splitW, n);

      // "close" holes (a morphological operation)
      if (splitO > 0){
         span.suffix(splitO);
         span.suffix(-splitO);
      }

      // now we can search for long zero spans
      SpanList ret = new SpanList(0, n - 1, true);
      int iStart = 0;
      n = span.getNumSpans();
      for(i = 0; i < n; i++){
         Range r = span.getRange(i);
         if (r.length() >= splitG){
            if (out != null) out.printf("\"silence\" %d %d\n", r.a, r.b);
            ret.sub(r);
            iStart = r.b + 1;
         }
      }

      return ret;
   }

   /**
    * Create a permutation of the indices of the examples in each class
    * 
    * @param data tree holding the data
    * @return permutation of indices per class
    */
   public static int[][] getShuffledDataIndices(TreeMap<String, ArrayList<Sequence>> data)
   {
      int indices[][] = new int[data.size()][];

      Set<String> labels = data.keySet();
      Iterator<String> it = labels.iterator();
      int i = 0;
      while(it.hasNext()){
         String label = it.next();
         ArrayList<Sequence> list = data.get(label);
         indices[i] = Library.permute(list.size());
         i++;
      }
      return indices;
   }

   // ////////////////////////////////////////////////////////////////////

   /**
    * Compute the confusion matrix for isolated recognition
    */
   public static void calcConfMatrix(TreeMap<String, ArrayList<Sequence>> data)
   {
      TimerMS tmtot = new TimerMS();

      // shuffle the data
      int[][] indices = getShuffledDataIndices(data);

      // now we can run the tests
      int nClasses = data.size();
      int confTrain[][] = Library.allocMatrixInt(nClasses, nClasses);
      int confTest[][] = Library.allocMatrixInt(nClasses, nClasses);

      OatesModelUSamp om[] = new OatesModelUSamp[nClasses];
      MetricModel metmod[] = new MetricModel[nClasses];
      HmmLR hmm[] = new HmmLR[nClasses];
      HmmLRD dhmm[] = new HmmLRD[nClasses];
      double scores[] = new double[nClasses];

      // extract the class names from the data tree
      TimerMS timer = new TimerMS();
      String classes[] = new String[nClasses];
      int[] nClass = new int[nClasses];
      Set<String> labels = data.keySet();
      Iterator<String> it = labels.iterator();
      for(int i = 0; i < nClasses; i++){
         classes[i] = it.next();
         nClass[i] = data.get(classes[i]).size();
      }

      for(int iFold = 0; iFold < nFolds; iFold++){
         // build the models
         System.err.print("Building models for fold " + (iFold + 1) + " / " + nFolds + "...\n[");
         for(int i = 0; i < nClasses; i++){
            if (i > 0) System.err.print(" " + (i + 1));
            else System.err.print(i + 1);
            ArrayList<Sequence> train = new ArrayList<Sequence>();
            ArrayList<Sequence> dtrain = null;
            ArrayList<Sequence> all = data.get(classes[i]);
            int n = indices[i].length;
            int a = iFold * n / nFolds;
            int b = (iFold + 1) * n / nFolds;
            for(int j = 0; j < n; j++){
               if (j == a) j = b; // skip [a,b)
               if (j == n) break;
               train.add(all.get(indices[i][j]));
            }
            timer.reset();
            if (model == Model.oates) om[i] = new OatesModelUSamp(train, 0, initv, minv);
            else if (model == Model.erp) metmod[i] = new MetricModel(new ERP(fvm, rBand, lenPrep), train,
                  mmTrain);
            else if (model == Model.dtw) metmod[i] = new MetricModel(new DTW(fvm, rBand, lenPrep), train,
                  mmTrain);
            else if (model == Model.euc) metmod[i] = new MetricModel(new SumSeqDist(new EuclideanFV(false),
                  false, lenPrep), train, mmTrain);
            else if (model == Model.absdist) metmod[i] = new MetricModel(new SumSeqDist(
                  new AbsoluteDistFV(), false, lenPrep), train, mmTrain);
            else if (model == Model.hmm){
               hmm[i] = new HmmLR(nHmmStates, nHmmSkip, nDims);
               hmm[i].init_segk(train);
               if (hmmTrain == HmmTrain.viterbi) hmm[i].train_viterbi(train);
               else hmm[i].train_bw(train);
            }
            else if (model == Model.dhmm){
               if (nSymbols < 2){
                  System.err.printf("\nError: invalid number of symbols (%d)\n", nSymbols);
                  return;
               }
               dhmm[i] = new HmmLRD(nHmmStates, nHmmSkip, nSymbols);
               if (dtrain == null) dtrain = DiscreteSeq.convert(train, nSymbols);
               dhmm[i].init_segk(dtrain);
               if (hmmTrain == HmmTrain.viterbi) dhmm[i].train_viterbi(dtrain);
               else dhmm[i].train_bw(dtrain);
            }
            else{
               System.err.printf("\n\nError: mode (%s) not supported for computing a confusion matrix.\n",
                     model);
               return;
            }
            System.err.print("(" + timer.time() + ")");
         }
         System.err.println("]");

         // now run the classifier
         System.err.print("Classifying for fold " + (iFold + 1) + "...\n[");

         // loop through all classes
         for(int i = 0; i < nClasses; i++){
            if (i > 0) System.err.print(" " + (i + 1));
            else System.err.print(i + 1);
            ArrayList<Sequence> all = data.get(classes[i]);
            int n = indices[i].length;
            int a = iFold * n / nFolds;
            int b = (iFold + 1) * n / nFolds;
            timer.reset();
            // loop through each example of this class
            for(int j = 0; j < n; j++){
               // classify this example
               Sequence seq = all.get(indices[i][j]);
               int iBest = 0;

               if (model == Model.oates){
                  double bestScore = om[0].eval(seq);
                  if (bNorm) bestScore = bestScore / om[0].size();
                  for(int k = 1; k < nClasses; k++){
                     double score = om[k].eval(seq);
                     if (bNorm) score = score / om[k].size();
                     if (score > bestScore){
                        iBest = k;
                        bestScore = score;
                     }
                  }
               }
               else if (model == Model.erp || model == Model.dtw || model == Model.euc
                     || model == Model.absdist){
                  double bestScore = metmod[0].eval(seq);
                  if (bNorm) bestScore = bestScore / Math.max(metmod[0].length(), seq.length());
                  for(int k = 1; k < nClasses; k++){
                     double score = metmod[k].eval(seq);
                     if (bNorm) score = score / Math.max(metmod[k].length(), seq.length());
                     if (score < bestScore){
                        iBest = k;
                        bestScore = score;
                     }
                  }
               }
               else if (model == Model.hmm){
                  double bestScore = (hmmEval == HmmEval.viterbi ? hmm[0].viterbi(seq) : hmm[0].eval(seq));
                  scores[0] = bestScore;

                  if (bNorm) bestScore /= seq.length();
                  for(int k = 1; k < nClasses; k++){
                     double score = (hmmEval == HmmEval.viterbi ? hmm[k].viterbi(seq) : hmm[k].eval(seq));
                     scores[k] = score;
                     if (bNorm) score /= seq.length();
                     if (score > bestScore){
                        iBest = k;
                        bestScore = score;
                     }
                  }
               }
               else if (model == Model.dhmm){
                  DiscreteSeq dseq = DiscreteSeq.wrap(seq, nSymbols);
                  double bestScore = (hmmEval == HmmEval.viterbi ? dhmm[0].viterbi(dseq) : dhmm[0]
                        .eval(dseq));
                  scores[0] = bestScore;

                  if (bNorm) bestScore /= seq.length();
                  for(int k = 1; k < nClasses; k++){
                     double score = (hmmEval == HmmEval.viterbi ? dhmm[k].viterbi(dseq) : dhmm[k].eval(dseq));
                     scores[k] = score;
                     if (bNorm) score /= seq.length();
                     if (score > bestScore){
                        iBest = k;
                        bestScore = score;
                     }
                  }
               }
               else{
                  System.err.printf("Unknown/unsupported model type: %s\n", model);
                  assert false;
                  System.exit(1);
               }

               // display error info if requested
               if (bShowErr && i != iBest){
                  int ip = seq.getParentIndex() + 1;
                  int x = seq.getParentOffset();
                  int y = x + seq.length();
                  System.err.printf("\n - Error: %s (%d.%d->%d |%s) as %s (%.4f vs. %.4f) - ", classes[i],
                        ip, x, y, seq.getName(), classes[iBest], scores[i], scores[iBest]);
               }

               // record this result in the test or train confusion matrix
               if (j >= a && j < b) confTest[i][iBest]++;
               else confTrain[i][iBest]++;
            }
            System.err.print("(" + n + ":" + timer.time() + ")");
         }
         System.err.println("]");
         System.err.println();
      }

      System.err.println("Total time: " + tmtot.time());

      // output the confusion matrices to stdout
      dumpConfMatrix(confTrain, "Training", classes, null, false);
      if (output == Output.human) System.out.println();
      else System.out.print("  ");
      dumpConfMatrix(confTest, "Test", classes, null, false);
      if (output == Output.computer) System.out.println();
   }

   // ////////////////////////////////////////////////////////////////////

   public static void calcSimMatrix(TreeMap<String, ArrayList<Sequence>> data, int nEx)
   {
      TimerMS timer = new TimerMS();

      // now we can run the tests
      int nClasses = data.size();
      double sim[][] = Library.allocMatrixDouble(nEx, nEx);

      // extract the class names from the data tree
      String classes[] = new String[nClasses];
      Set<String> labels = data.keySet();
      Iterator<String> it = labels.iterator();
      for(int i = 0; i < nClasses; i++)
         classes[i] = it.next();

      OatesModelUSamp m1 = null, m2 = null;
      ERP erp = new ERP(rBand);
      DTW dtw = new DTW(rBand);
      CompressDist compd = new CompressDist();

      double vmin = Double.POSITIVE_INFINITY;
      double vmax = Double.NEGATIVE_INFINITY;
      if (per == Per.ex) // each model is initialized from a single example
      {
         int jsim, isim = 0;
         for(int ic1 = 0; ic1 < nClasses; ic1++){
            ArrayList<Sequence> exs1 = data.get(classes[ic1]);
            for(int ix1 = 0; ix1 < exs1.size(); ix1++){
               System.err.println((isim + 1) + " / " + nEx);
               Sequence seq1 = exs1.get(ix1);
               if (model == Model.oates) m1 = new OatesModelUSamp(seq1, initv, minv);
               jsim = 0;
               for(int ic2 = 0; ic2 < nClasses; ic2++){
                  ArrayList<Sequence> exs2 = data.get(classes[ic2]);
                  for(int ix2 = 0; ix2 < exs2.size(); ix2++){
                     Sequence seq2 = exs2.get(ix2);
                     double vsim = Double.NEGATIVE_INFINITY;

                     if (model == Model.oates){
                        double v1 = m1.eval(seq2);
                        double v2 = m2.eval(seq1);
                        if (bNorm){
                           v1 /= m1.length();
                           v2 /= m2.length();
                        }
                        vsim = v1 + v2;
                     }
                     else if (model == Model.erp){
                        vsim = erp.dist(seq1, seq2);
                        if (bNorm) vsim /= Math.max(seq1.length(), seq2.length());
                     }
                     else if (model == Model.dtw){
                        vsim = dtw.dist(seq1, seq2);
                        if (bNorm) vsim /= Math.max(seq1.length(), seq2.length());
                     }
                     else if (model == Model.zip){
                        vsim = compd.dist(seq1, seq2);
                     }
                     else{
                        System.err.println("Can't compute similarity matrix per example"
                              + "with the specified distance metric.");
                        assert false;
                        System.exit(0);
                     }

                     if (vsim == Double.NEGATIVE_INFINITY) vsim = -(1e99);
                     else{
                        if (vsim < vmin) vmin = vsim;
                        if (vsim > vmax) vmax = vsim;
                     }
                     sim[isim][jsim] = vsim;
                     jsim++;
                  }
               }
               isim++;
            }
         }
      }
      else if (per == Per.cls) // each model initialized from entire class
      {
         int jsim, isim = 0;
         MetricModel metmod[] = null;
         OatesModelUSamp om[] = null;

         // train all models
         if (model == Model.oates){
            om = new OatesModelUSamp[nClasses];
            for(int i = 0; i < nClasses; i++){
               System.err.printf("Training Oates model %d / %d\n", i + 1, nClasses);
               om[i] = new OatesModelUSamp(data.get(classes[i]), 0, initv, minv);
            }
         }
         else if (model == Model.erp){
            metmod = new MetricModel[nClasses];
            for(int i = 0; i < nClasses; i++){
               System.err.printf("Training ERP model %d / %d\n", i + 1, nClasses);
               metmod[i] = new MetricModel(new ERP(fvm, rBand, lenPrep), data.get(classes[i]), mmTrain);
            }
         }
         else if (model == Model.dtw){
            metmod = new MetricModel[nClasses];
            for(int i = 0; i < nClasses; i++){
               System.err.printf("Training DTW model %d / %d\n", i + 1, nClasses);
               metmod[i] = new MetricModel(new DTW(fvm, rBand, lenPrep), data.get(classes[i]), mmTrain);
            }
         }

         for(int ic1 = 0; ic1 < nClasses; ic1++){
            ArrayList<Sequence> exs1 = data.get(classes[ic1]);
            System.err.println((ic1 + 1) + " / " + nClasses);
            jsim = 0;
            for(int ic2 = 0; ic2 < nClasses; ic2++){
               ArrayList<Sequence> exs2 = data.get(classes[ic2]);
               for(int ix2 = 0; ix2 < exs2.size(); ix2++){
                  double vsim = Library.NEGINF;
                  Sequence seq2 = exs2.get(ix2);
                  if (model == Model.oates){
                     vsim = om[ic1].eval(seq2);
                     if (bNorm) vsim /= om[ic1].length();
                  }
                  else if (model == Model.erp || model == Model.dtw){
                     vsim = metmod[ic1].eval(seq2);
                     if (bNorm) vsim /= Math.max(metmod[ic1].length(), seq2.length());
                  }
                  else{
                     System.err.println("Can't compute similarity matrix per example"
                           + "with the specified distance metric.");
                     assert false;
                     System.exit(0);
                  }

                  if (vsim == Double.NEGATIVE_INFINITY) vsim = -(1e99);
                  else{
                     if (vsim < vmin) vmin = vsim;
                     if (vsim > vmax) vmax = vsim;
                  }

                  for(int i = 0; i < exs1.size(); i++)
                     sim[isim + i][jsim] = vsim;
                  jsim++;
               }
            }
            isim += exs1.size();
         }
      }

      System.err.println("Time to compute similarity matrix: " + timer.time() + "ms");
      System.err.printf("min/max: (%e, %e)\n", vmin, vmax);

      // adjust sim matrix in range [0..1]
      for(int i = 0; i < sim.length; i++){
         for(int j = 0; j < sim[i].length; j++){
            if (sim[i][j] < vmin) sim[i][j] = vmin;
            sim[i][j] = (sim[i][j] - vmin) / (vmax - vmin);
         }
      }

      // output the similarity matrix to stdout
      dumpSimMatrix(sim, classes);
   }

   // ////////////////////////////////////////////////////////////////////

   public static void calcMaps(TreeMap<String, ArrayList<Sequence>> data)
   {
      try{
         TimerMS timer = new TimerMS();
         PrintWriter out1 = new PrintWriter(new BufferedWriter(new FileWriter(sOutPath + "summary.txt")));

         Set<String> labels = data.keySet();
         Iterator<String> it = labels.iterator();
         // loop through each class
         while(it.hasNext()){
            String sClass = it.next();
            System.err.println("Processing class: " + sClass);
            ArrayList<Sequence> occs = data.get(sClass);
            PrintWriter out2 = new PrintWriter(
                  new BufferedWriter(new FileWriter(sOutPath + sClass + ".txt")));
            out1.println("Class: " + sClass + " (" + occs.size() + " instances)");
            timer.reset();

            // build model for this class using all data
            OatesModelUSamp m = new OatesModelUSamp(occs, 0, initv, minv);
            long time1 = timer.time();
            long time2 = 0;

            // loop through each occurrence
            for(int i = 0; i < occs.size(); i++){
               timer.reset();
               OatesMapping omap = m.align(occs.get(i));
               time2 += timer.time();
               for(int j = 0; j < omap.getPatternLength(); j++)
                  out2.print(omap.imap[j] + " ");
               out2.println();
            }
            out2.close();
            System.err.println(" timing: " + time1 + ", " + time2);
         }

         out1.close();
      } catch (IOException ioe1){
         ioe1.printStackTrace();
      }
   }

   // ////////////////////////////////////////////////////////////////////

   /**
    * Determine if the index is in the test set
    * 
    * @param ix index to test
    * @param indices shuffled indices
    * @param a start of test set (inclusive)
    * @param b end of test set (exclusive)
    * @return true if ix is in indices[ [a..b) ]
    */
   public static boolean isTest(int ix, int indices[], int a, int b)
   {
      for(int i = a; i < b; i++)
         if (ix == indices[i]) return true;
      return false;
   }

   // ////////////////////////////////////////////////////////////////////

   public static double adjustScore(double score, Gaussian1D gm)
   {
      if (scoreMethod == ScoreMethod.direct) return score;
      else if (scoreMethod == ScoreMethod.sigVar){
         double diff = score - gm.getMean();
         return 1.0 / (1.0 + Math.exp(-diff / gm.getSDev()));
      }
      assert false : "invalid score method: " + scoreMethod;
      return Double.NaN;
   }

   // ////////////////////////////////////////////////////////////////////

   public static int[][] runWordSpotDiscrete(ProbSeqModel[] models, Gaussian1D[] pScore,
         ArrayList<DiscreteSeq> qseries, TreeMap<String, ArrayList<DiscreteSeq>> labQData, ContRecInfo cri)
   {
      // this is silly, but Java is too dumb to figure out the casts on its own

      ArrayList<Sequence> tqseries = new ArrayList<Sequence>(qseries.size());
      for(DiscreteSeq dseq : qseries)
         tqseries.add(dseq);

      TreeMap<String, ArrayList<Sequence>> labData = new TreeMap<String, ArrayList<Sequence>>();
      Iterator<String> it = labQData.keySet().iterator();
      while(it.hasNext()){
         String sClass = it.next();
         ArrayList<Sequence> examples = new ArrayList<Sequence>();
         for(DiscreteSeq dseq : labQData.get(sClass))
            examples.add(dseq);
         labData.put(sClass, examples);
      }

      return runWordSpot(models, pScore, tqseries, labData, cri);
   }

   // ////////////////////////////////////////////////////////////////////

   /**
    * @return confusion matrix [nFoundClasses][nTrueClasses]
    */
   public static int[][] runWordSpot(ProbSeqModel[] models, Gaussian1D[] pScore,
         ArrayList<Sequence> tseries, TreeMap<String, ArrayList<Sequence>> labData, ContRecInfo cri)
   {
      TimerMS timer = new TimerMS();
      int nSeries = tseries.size();
      int nClasses = labData.size();
      WordSpot bestMatch[] = new WordSpot[nSeries];

      System.err.printf("ws: %d classes, %d models\n", nClasses, models.length);

      // what's left to process in each series? for now: everything
      SpanList span[] = new SpanList[nSeries];
      for(int i = 0; i < nSeries; i++)
         span[i] = new SpanList(0, tseries.get(i).length() - 1, true);

      // iterate looking for next best match
      for(int iMatch = 0;; iMatch++){
         int iBestSeries = -1;
         double vBestSeries = Library.NEGINF;

         // search for matches over all time series
         for(int iSeries = 0; iSeries < nSeries; iSeries++){
            // do we need to (re-)compute the best match for this series?
            if (bestMatch[iSeries] == null && !span[iSeries].isEmpty()){
               Sequence seq = tseries.get(iSeries);
               ScoredWindow bestWin = new ScoredWindow(iSeries, -1, 0, Library.NEGINF);
               int iBestClass = -1;

               // compute mappings for available spots and
               // find the smallest (since we normalized by a neg num) value
               for(int iClass = 0; iClass < nClasses; iClass++){
                  if (model == Model.oates){
                     OatesModelUSamp oates = (OatesModelUSamp)models[iClass];

                     // TODO: when is it not a full recalc?
                     oates.buildPatternMap(seq, span[iSeries], true);

                     // compute prob of all of the valid ending points
                     span[iSeries].itReset();
                     while(span[iSeries].itMore()){
                        int k = span[iSeries].itNext();
                        OatesMapping omap = oates.findMappingWithEnd(k);
                        omap.iSeries = iSeries;
                        omap.score = adjustScore(omap.score / (bNorm ? omap.getDataLength() : 1.0),
                              pScore[iClass]);
                        if (bestWin.iStart < 0 || omap.score > bestWin.score){
                           bestWin = omap.getScoredwindow();
                           iBestClass = iClass;
                        }
                     }
                  }
                  else if (model == Model.hmm){
                     HmmLR hmm = (HmmLR)models[iClass];
                     // ScoredWindow swin = hmm.findBestSubseqSlow(seq, span[iSeries],
                     // bNorm, rWordLen, 1);
                     ScoredWindow swin = hmm.findBestSubseq(seq, span[iSeries], bNorm);
                     if (swin != null){
                        swin.score = adjustScore(swin.score, pScore[iClass]);
                        if (bestWin.iStart < 0 || swin.score > bestWin.score){
                           bestWin.score = swin.score;
                           bestWin.iStart = swin.iStart;
                           bestWin.nLength = swin.length();
                           iBestClass = iClass;
                        }
                     }
                  }
                  else if (model == Model.dhmm){
                     DiscreteSeq dseq = DiscreteSeq.wrap(seq, nSymbols);
                     HmmLRD hmm = (HmmLRD)models[iClass];
                     ScoredWindow swin = hmm.findBestSubseqSlow(dseq, span[iSeries], bNorm, rWordLen, 1);
                     if (swin != null){
                        swin.score = adjustScore(swin.score, pScore[iClass]);
                        if (bestWin.iStart < 0 || swin.score > bestWin.score){
                           bestWin.score = swin.score;
                           bestWin.iStart = swin.iStart;
                           bestWin.nLength = swin.length();
                           iBestClass = iClass;
                        }
                     }
                  }
                  else{
                     assert false : String.format("Error: unsupported model (%s)", model);
                     return null;
                  }
               }

               // maybe we can't find anything at all, so stop trying
               if (bestWin.iStart < 0) span[iSeries].clear();
               else{
                  assert (bestWin.iStart >= 0 && iBestClass >= 0) : String.format(
                        "bw.iStart=%d  best_class=%d", bestWin.iStart, iBestClass);

                  // update winner for next best match
                  bestMatch[iSeries] = new WordSpot(iSeries, bestWin.getFirstIndex(), bestWin.length(),
                        bestWin.score, iBestClass);

                  // remove the mapped range from the span
                  span[iSeries].sub(bestMatch[iSeries].getFirstIndex(), bestMatch[iSeries].getLastIndex());
               }
            }

            if (bestMatch[iSeries] != null){
               assert (iSeries == bestMatch[iSeries].iSeries);

               // see if this is the best match for this round
               if (bestMatch[iSeries].score > vBestSeries){
                  vBestSeries = bestMatch[iSeries].score;
                  iBestSeries = bestMatch[iSeries].iSeries;
               }
            }
         }

         // figure out if it's time to quit
         if (iBestSeries < 0){
            System.err.println("Stopping because we couldn't find any more matched");
            break;
         }
         if (nSpotsMax > 0 && iMatch >= nSpotsMax){
            System.err.printf("Stopping because we found enough matches (%d/%d)\n", iMatch, nSpotsMax);
            break;
         }

         if (nSpotsMin < 0 || iMatch >= nSpotsMin){
            if (!Double.isNaN(spotThresh) && vBestSeries < spotThresh){
               System.err.printf("Stopping because score is too low (%.4f < %.4f)\n", vBestSeries,
                     spotThresh);
               break;
            }

            // could be too large or too small
            if (rWordLen != null){
               int wordLen = bestMatch[iBestSeries].length();
               if (rWordLen.a > 0 && wordLen < rWordLen.a){
                  System.err.printf("Stopping because spot length is too short (%d < %d)\n", wordLen,
                        rWordLen.a);
                  break;
               }
               if (rWordLen.b > 0 && wordLen > rWordLen.b){
                  System.err.printf("Stopping because spot length is too long (%d > %d)\n", wordLen,
                        rWordLen.b);
                  break;
               }
            }
         }

         // TODO uncomment for info on word spotting
         System.err.printf("next best match (%d) %f: %s\n", iMatch + 1, vBestSeries, bestMatch[iBestSeries]);

         // add the word spot to the list
         cri.add(bestMatch[iBestSeries]);
         bestMatch[iBestSeries] = null;
      }

      // evaluate the results
      int[][] conf = cri.scoreWordSpotWordsFast(labData);
      cri.scoreWordSpotFrames(tseries, labData);

      System.err.printf("Word Spotting: %dms\n", timer.time());

      return conf;
   }

   // ////////////////////////////////////////////////////////////////////

   /**
    * Word spotting labels only those frames in the data that match a class. This is typically more difficult
    * that continuous recognition if a good garbage/background model can be estimated but may be required when
    * such a model is unavailable.
    */
   public static void calcWordSpot(Sequence[] tseries, TreeMap<String, ArrayList<Sequence>> trainData,
         TreeMap<String, ArrayList<Sequence>> labData)
   {
      int nSeries = tseries.length;
      int nClasses = trainData.size();
      int[] tindices = Library.permute(nSeries); // shuffle the time series

      for(int iFold = 0; iFold < nFolds; iFold++){
         System.err.printf("Word Spot Fold: %d / %d\n", iFold + 1, nFolds);

         ProbSeqModel models[];
         if (model == Model.oates) models = new OatesModelUSamp[nClasses];
         else if (model == Model.hmm) models = new HmmLR[nClasses];
         else{
            // TODO: other models!
            System.err.println("\n\nError: mode (" + model + ") not supported for word spotting.\n");
            return;
         }
         GaussianDyn1D pScore[] = new GaussianDyn1D[nClasses];

         // the test sequences are time series
         int a = iFold * nSeries / nFolds;
         int b = (iFold + 1) * nSeries / nFolds;

         // create the training/test sets
         ArrayList<Sequence> trainSet = new ArrayList<Sequence>();
         ArrayList<Sequence> testSet = new ArrayList<Sequence>();
         for(int iSeries = 0; iSeries < nSeries; iSeries++){
            if (iSeries < a || iSeries >= b) trainSet.add(tseries[iSeries]);
            else testSet.add(tseries[iSeries]);
         }

         // storage for results/info about run
         ContRecInfo criTrain = new ContRecInfo(nClasses);
         ContRecInfo criTest = new ContRecInfo(nClasses);

         // pull out the relevant examples for training
         System.err.print(" Training models... (");
         TimerMS timer = new TimerMS();
         Iterator<ArrayList<Sequence>> it = trainData.values().iterator();
         for(int iClass = 0; iClass < nClasses; iClass++){
            System.err.printf("%d%s", iClass + 1, iClass + 1 == nClasses ? "" : " ");
            ArrayList<Sequence> examples = new ArrayList<Sequence>();

            for(Sequence seq : it.next()){
               if (isTest(seq.getParentIndex(), tindices, a, b)){
                  criTest.nLabeledWords++;
                  criTest.nLabeledFrames += seq.length();
               }
               else{
                  examples.add(seq);
                  criTrain.nLabeledWords++;
                  criTrain.nLabeledFrames += seq.length();
               }
            }

            // train model
            if (model == Model.oates) models[iClass] = new OatesModelUSamp(examples, 0, initv, minv);
            else if (model == Model.hmm){
               HmmLR hmm = new HmmLR(nHmmStates, nHmmSkip, nDims);
               hmm.init_segk(examples);
               if (hmmTrain == HmmTrain.viterbi) hmm.train_viterbi(examples);
               else hmm.train_bw(examples);
               models[iClass] = hmm;
            }

            // build distribution over training data scores
            pScore[iClass] = new GaussianDyn1D();
            for(Sequence seq : examples)
               pScore[iClass].add(models[iClass].eval(seq) / (bNorm ? seq.length() : 1.0), false);
            pScore[iClass].update();
         }
         System.err.printf(") done (%dms).\n", timer.time());

         // now run the word spotting
         System.err.print(" Evaluating on training set... ");
         runWordSpot(models, pScore, trainSet, labData, criTrain);
         System.err.println("done.");
         criTrain.dump();
         criTrain.showResults("Training set results...", null); // TODO: pass class name array

         System.err.print(" Evaluating on test set... ");
         runWordSpot(models, pScore, testSet, labData, criTest);
         System.err.println("done.");
         criTest.dump();
         criTest.showResults("Test set results...", null); // TODO: pass class name array

      }
   }

   /**
    * Compute the score of the given confusion matrix.
    * 
    * @param m confusion matrix [nFound][nTrue]
    * @return the "score" of the given (square) confusion matrix
    */
   protected static int scoreConfMatrix(int[][] m)
   {
      int N = m[0].length;
      short[] map = new short[N];
      for(int i = 0; i < N; i++)
         map[i] = (short)i;
      return scoreConfMatrix(m, map, null);
   }

   /**
    * Compute the score of the given confusion matrix.
    * 
    * @param m confusion matrix [nFound][nTrue]
    * @param map matrix column permutation (map[iTrue] = iFound)
    * @param rowScore if non-null, calculated score for each row
    * @return the "score" of the given confusion matrix
    */
   public static int scoreConfMatrix(int[][] m, short[] map, int[] rowScore)
   {
      int nFound = m.length;
      int nTrue = m[0].length - 1;
      int score = 0;
      for(int i = 0; i < nTrue; i++){
         if (map[i] < 0) continue;
         int rs = 2 * m[map[i]][i] - m[map[i]][nTrue];
         // int rs = 0;
         // for(int j = 0; j <= nTrue; j++)
         // if (i == j) rs += m[map[i]][i];
         // else rs -= m[map[i]][j];
         score += rs;
         if (rowScore != null) rowScore[i] = rs;
      }

      return score;
   }

   /**
    * Given a confusion matrix, calc the permutation that gives the best score
    * 
    * @param m confusion matrix -- m[nFound][nTrue]
    * @param bOptAcc if true, remove mappings that lead to acc less than -100%
    * @return mapping to get to the best permutation (-1 => no match); map goes from true class to cluster
    *         (ie, map[iTrue] = iCluster)
    */
   public static short[] calcBestMappingFromConfMatrix(int[][] m, boolean bOptAcc)
   {
      // TODO: should do something smarter... GA, MCMC, random restarts

      // for now, we just use a greedy algorithm that selects the best mapping
      // without any backtracking or swapping; we try each possible
      // starting position, and then a constant number of permutations of the rest. We
      // select the best one (score = trace - (sum of off diagonal elements) - (insertions)).

      if (m == null) return null;
      
      int nFoundClasses = m.length; // number of discovered classes
      int nTrueClasses = m[0].length - 1; // number of true classes
      final int NPERM = 10;
      short[] map = new short[nTrueClasses];
      short[] bestMap = new short[nTrueClasses];
      int bestScore = Integer.MIN_VALUE;
      int[] order = new int[nTrueClasses];

      Arrays.fill(bestMap, (short)-1);
      // start search with each possible true class
      for(int iFirst = 0; iFirst < nTrueClasses; iFirst++){
         // randomly permute search order of remaining classes
         for(int iPerm = 0; iPerm < NPERM; iPerm++){
            // generate a random order that has iFirst in the first position
            int[] perm = Library.permute(nTrueClasses - 1);
            order[0] = iFirst;
            for(int i = 0; i < perm.length; i++){
               int j = perm[i];
               order[i + 1] = (j < iFirst ? j : j + 1);
            }

            // greedily select the next best match
            SpanList span = new SpanList(0, nFoundClasses - 1, true);
            Arrays.fill(map, (short)-1);
            for(int iTrue = 0; iTrue < nTrueClasses; iTrue++){
               int jBest = -1;
               span.itReset();
               while(span.itMore()){
                  int j = span.itNext();

                  int s1 = m[j][order[iTrue]];
                  if (s1 == 0) continue;
                  if (bOptAcc){
                     s1 += s1 - m[j][nTrueClasses];// s1 -= (Library.sum(m[j]) - s1);
                     if (s1 < 0) continue;
                  }
                  if (jBest < 0) jBest = j;
                  else{
                     int s2 = m[jBest][order[iTrue]];
                     if (bOptAcc) s2 += s2 - m[jBest][nTrueClasses];// s2 -= (Library.sum(m[jBest]) - s2);
                     if (s1 > s2) jBest = j;
                  }
               }
               if (jBest >= 0){
                  span.sub(jBest);
                  map[order[iTrue]] = (short)jBest;
               }
               if (span.isEmpty()) break;
            }

            // score the matrix and remember it if it's the best so far
            int score = scoreConfMatrix(m, map, null);
            if (score > bestScore){
               bestScore = score;
               Library.copy(map, bestMap);
            }
         }
      }
      // System.err.print("best map: ");
      // for(int i = 0; i < bestMap.length; i++)
      // System.err.printf("%d ", bestMap[i]);
      // System.err.println();
      return bestMap;
   }

   /**
    * Continuous recognition labels every data point with one of the class labels. Typically, a garbage (i.e.,
    * silence/background) class is included to account for the non-interesting-class frames.
    */
   public static void calcContinuous(Sequence[] tseries, TreeMap<String, ArrayList<Sequence>> data)
   {
      System.err.println("Error: continuous recognition has not been implemented!");
      assert false : "continuous recognition has not been implemented!";
      System.exit(1);
   }

   // ////////////////////////////////////////////////////////////////////

   public static void calcStats(Sequence[] tseries, TreeMap<String, ArrayList<Sequence>> data)
   {
      int nClasses = data.size();
      ERP erp = new ERP(rBand);

      // extract the class names from the data tree
      String classes[] = new String[nClasses];
      Set<String> labels = data.keySet();
      Iterator<String> it = labels.iterator();

      System.err.println("Stats: class number, min/mean/max intra-dist, i/max dist centroid,");
      System.err.println("       min/max dist occ, min/mean/max length");

      for(int i = 0; i < nClasses; i++){
         classes[i] = it.next();
         ArrayList<Sequence> all = data.get(classes[i]);
         int nSeqs = all.size();
         MetricModel metmod = new MetricModel(new DTW(fvm, rBand, lenPrep), all, mmTrain);

         double dm[][] = Library.allocMatrixDouble(nSeqs, nSeqs);
         double dist[] = new double[] { Library.INF, 0, Library.NEGINF };
         int len[] = new int[] { Integer.MAX_VALUE, 0, Integer.MIN_VALUE };
         double vSumDist[] = new double[] { Library.INF, Library.NEGINF };
         int iSumDist[] = new int[] { -1, -1 };
         double vMaxDistMean = Library.NEGINF;
         int iMaxDistMean = -1;

         for(int j = 0; j < nSeqs; j++){
            Sequence seq = all.get(j);

            // update length stats
            int n = seq.length();
            if (n < len[0]) len[0] = n;
            if (n > len[2]) len[2] = n;
            len[1] += n;

            // update dist stats
            for(int k = j + 1; k < nSeqs; k++){
               if (model == Model.erp){
                  double d = erp.dist(seq, all.get(k));
                  if (d < dist[0]) dist[0] = d;
                  if (d > dist[2]) dist[2] = d;
                  dm[j][k] = dm[k][j] = d;
                  dist[1] += d;
               }
               else{
                  assert false : "Not yet implemented";
               }
            }

            double d = metmod.eval(seq);
            if (d > vMaxDistMean){
               vMaxDistMean = d;
               iMaxDistMean = j;
            }
         }

         // turn sums into averages
         len[1] = (int)Math.round((double)len[1] / (double)nSeqs);
         int nPairs = nSeqs * (nSeqs - 1) / 2;
         dist[1] = dist[1] / (double)nPairs;

         // compute the most distant point for this class
         for(int j = 0; j < nSeqs; j++){
            double d = 0;
            for(int k = 0; k < nSeqs; k++)
               d += dm[j][k];
            if (d < vSumDist[0]){
               vSumDist[0] = d;
               iSumDist[0] = j;
            }
            if (d > vSumDist[1]){
               vSumDist[1] = d;
               iSumDist[1] = j;
            }
         }

         // output results
         System.out.printf("%2d  %7.2f %7.2f %7.2f  %2d %7.2f  %2d %2d  %4d %4d %4d\n", i + 1, dist[0],
               dist[1], dist[2], iMaxDistMean, vMaxDistMean, iSumDist[0], iSumDist[1], len[0], len[1],
               len[2]);
      }
   }

   // ////////////////////////////////////////////////////////////////////

   public static void calcLabels(Sequence[] tseries, TreeMap<String, ArrayList<Sequence>> data,
         MarkupSet[] marks)
   {
      assert (tseries.length == marks.length);
      int nClasses = data.size();

      // extract the class names from the data tree
      String classes[] = new String[nClasses];
      Set<String> labels = data.keySet();
      Iterator<String> it = labels.iterator();
      for(int i = 0; i < nClasses; i++)
         classes[i] = it.next();

      // train models
      assert model == Model.hmm : "only hmms are currently supported";
      HmmLR hmm[] = new HmmLR[nClasses];
      System.err.print("Training models... ");
      for(int i = 0; i < nClasses; i++){
         ArrayList<Sequence> train = data.get(classes[i]);
         hmm[i] = new HmmLR(nHmmStates, nHmmSkip, nDims);
         // hmm[i].setUpdateVar(false);
         // hmm[i].setVar(new FeatureVec(new double[]{ 4, 4, 4 }));
         // hmm[i].setVar(new FeatureVec(new double[] { .5, .5, .5 }));
         hmm[i].init_segk(train);
         if (hmmTrain == HmmTrain.viterbi) hmm[i].train_viterbi(train);
         else hmm[i].train_bw(train);
         // if (hmm[i].getUpdateVar()) hmm[i].mulVar(2);
      }
      System.err.println("done.");

      for(int i = 0; i < tseries.length; i++){
         MarkupSet mark = marks[i];
         if (mark != null && !mark.isEmpty()) continue;
         Sequence seq = tseries[i];
         System.err.printf("Labeling Series %d (%s)...\n", i + 1, seq.getName());

         try{
            String sFile = String.format("%slabels%02d%s.txt", sOutPath, i + 1, tseries[i].getName());
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(sFile)));
            SpanList span;

            // remove silence or include whole sequence?
            if (splitW > 0) span = presplit(seq, null);// out);
            else span = new SpanList(0, seq.length() - 1, true);

            // start searching for occurrences
            TimerMS timer = new TimerMS();
            ScoredWindow sw[] = new ScoredWindow[nClasses];
            double vPrevScore = Library.LOG_ZERO;
            for(int k = 0; k < 10; k++) // TODO: hard max on num occs
            {
               int iBest = -1;
               for(int j = 0; j < nClasses; j++){
                  if (sw[j] == null){
                     // sw[j] = hmm[j].findBestSubseqSlow(seq, span, bNorm, rWordLen, nStep);
                     sw[j] = hmm[j].findBestSubseq(seq, span, bNorm);
                     if (sw[j] != null) sw[j].iSeries = j;
                  }
                  if (iBest < 0 || (sw[j] != null && sw[j].score > sw[iBest].score)) iBest = j;
               }
               if (iBest < 0 || sw[iBest] == null) break;
               ScoredWindow swBest = sw[iBest];
               sw[iBest] = null; // we always have to recompute the winner

               // who else needs to be recomputed?
               int x = swBest.start();
               int y = swBest.end();
               for(int j = 0; j < nClasses; j++){
                  if (j == iBest) continue;
                  int a = sw[j].start();
                  int b = sw[j].end();
                  if (a < y && b > x) sw[j] = null;
               }

               // are we done?
               if (k > 1) // TODO: hard min number of occurrences
               {
                  double r = swBest.score / vPrevScore;
                  System.err.printf("k=%d  ratio=%.4f\n", k, r);
                  if (r > 1.2) break; // TODO
               }
               vPrevScore = swBest.score;

               span.sub(x, y - 1); // remove this occurrence

               System.err.println("swBest: " + swBest + "  (" + timer.time() + "ms)");
               // TODO !! hack -- modify values as appropriate
               out.printf("\"%s\" %d %d\n", classes[swBest.iSeries], swBest.start() * 10, swBest.end() * 10);
            }
            out.close();
         } catch (IOException e){
            e.printStackTrace();
         }

         break; // TODO just label one at a time
      }
   }

   /**
    * @return Gaussian model per dimension over all of the given data
    */
   public static Gaussian1D[] calcGauss(Sequence[] tseries)
   {
      // compute mean and variance of each dimension
      GaussianDyn1D[] gm = new GaussianDyn1D[tseries[0].getNumDims()];
      for(int i = 0; i < gm.length; i++)
         gm[i] = new GaussianDyn1D();
      for(int i = 0; i < tseries.length; i++)
         for(int t = 0; t < tseries[i].length(); t++)
            for(int d = 0; d < gm.length; d++)
               gm[d].add(tseries[i].get(t, d), false);
      for(int i = 0; i < gm.length; i++)
         gm[i].update();

      // now that we have the var for each dim, setup initv/minv
      initv = new FeatureVec(gm.length);
      minv = new FeatureVec(gm.length);
      for(int i = 0; i < gm.length; i++){
         initv.set(i, gm[i].getVar() / INITV_DIV);
         minv.set(i, gm[i].getVar() / MINV_DIV);
      }
      return gm;
   }

   // ////////////////////////////////////////////////////////////////////

   public static void main(String args[])
   {
      int c;
      LongOpt[] longopts = new LongOpt[] { new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
            new LongOpt("v", LongOpt.NO_ARGUMENT, null, 'v'),
            new LongOpt("nfolds", LongOpt.REQUIRED_ARGUMENT, null, 'f'),
            new LongOpt("seed", LongOpt.REQUIRED_ARGUMENT, null, 'd'),
            new LongOpt("norm", LongOpt.NO_ARGUMENT, null, 'n'),
            new LongOpt("showerr", LongOpt.NO_ARGUMENT, null, 'e'),
            new LongOpt("per", LongOpt.REQUIRED_ARGUMENT, null, 'p'),
            new LongOpt("op", LongOpt.REQUIRED_ARGUMENT, null, 'o'),
            new LongOpt("out", LongOpt.REQUIRED_ARGUMENT, null, 'u'),
            new LongOpt("vardiv", LongOpt.REQUIRED_ARGUMENT, null, 1000),
            new LongOpt("model", LongOpt.REQUIRED_ARGUMENT, null, 'm'),
            new LongOpt("band", LongOpt.REQUIRED_ARGUMENT, null, 'b'),
            new LongOpt("znorm", LongOpt.REQUIRED_ARGUMENT, null, 'z'),
            new LongOpt("hmmeval", LongOpt.REQUIRED_ARGUMENT, null, 1001),
            new LongOpt("hmmstates", LongOpt.REQUIRED_ARGUMENT, null, 1002),
            new LongOpt("hmmskip", LongOpt.REQUIRED_ARGUMENT, null, 1003),
            new LongOpt("hmmtrain", LongOpt.REQUIRED_ARGUMENT, null, 1004),
            new LongOpt("output", LongOpt.REQUIRED_ARGUMENT, null, 1005),
            new LongOpt("presplit", LongOpt.REQUIRED_ARGUMENT, null, 1006),
            new LongOpt("nsymbols", LongOpt.REQUIRED_ARGUMENT, null, 1008),
            new LongOpt("wspot_min", LongOpt.REQUIRED_ARGUMENT, null, 1009),
            new LongOpt("wspot_max", LongOpt.REQUIRED_ARGUMENT, null, 1010),
            new LongOpt("wspot_thresh", LongOpt.REQUIRED_ARGUMENT, null, 1011),
            new LongOpt("word_len", LongOpt.REQUIRED_ARGUMENT, null, 1012),
            new LongOpt("scoremeth", LongOpt.REQUIRED_ARGUMENT, null, 1013),
            new LongOpt("noclean", LongOpt.NO_ARGUMENT, null, 1014),
            new LongOpt("mmtrain", LongOpt.REQUIRED_ARGUMENT, null, 1015),
            new LongOpt("lenprep", LongOpt.REQUIRED_ARGUMENT, null, 1016),
            new LongOpt("trans", LongOpt.REQUIRED_ARGUMENT, null, 1017) };

      Getopt g = new Getopt("SupTest", args, "?", longopts, true);
      while((c = g.getopt()) != -1){
         String sArg = g.getOptarg();
         switch(c){
         case '?':
         case 'h': // help
            usage();
            System.exit(0);
            break;
         case 'v': // v
            bVerbose = true;
            break;
         case 'f': // nfolds
            nFolds = Integer.parseInt(sArg);
            break;
         case 'd': // seed
            Library.reseed(Long.parseLong(sArg));
            break;
         case 'n': // norm
            bNorm = true;
            break;
         case 'e': // showerr
            bShowErr = true;
            break;
         case 'p': // per
            if (Library.stricmp(sArg, "class")) per = Per.cls;
            else if (Library.stricmp(sArg, "ex")) per = Per.ex;
            else{
               System.err.println("Error: unrecognized \"per\" option: " + sArg);
               System.exit(1);
            }
            break;
         case 'o': // op
            if (Library.stricmp(sArg, "conf")) op = Op.conf;
            else if (Library.stricmp(sArg, "sim")) op = Op.sim;
            else if (Library.stricmp(sArg, "map")) op = Op.map;
            else if (Library.stricmp(sArg, "cont")) op = Op.cont;
            else if (Library.stricmp(sArg, "wspot")) op = Op.wspot;
            else if (Library.stricmp(sArg, "stats")) op = Op.stats;
            else if (Library.stricmp(sArg, "label")) op = Op.label;
            else{
               System.err.println("Error: unrecognized \"op\" option: " + sArg);
               System.exit(1);
            }
            break;
         case 'm': // model
            if (Library.stricmp(sArg, "oates")) model = Model.oates;
            else if (Library.stricmp(sArg, "erp")) model = Model.erp;
            else if (Library.stricmp(sArg, "dtw")) model = Model.dtw;
            else if (Library.stricmp(sArg, "hmm")) model = Model.hmm;
            else if (Library.stricmp(sArg, "dhmm")) model = Model.dhmm;
            else if (Library.stricmp(sArg, "comp")) model = Model.zip;
            else if (Library.stricmp(sArg, "euc")) model = Model.euc;
            else if (Library.stricmp(sArg, "absdist")) model = Model.absdist;
            else{
               System.err.println("Error: unrecognized \"model\" option: " + sArg);
               System.exit(1);
            }
            break;
         case 'u': // out
            sOutPath = Library.ensurePathSep(sArg);
            break;
         case 1000: // vardiv
         {
            StringTokenizer st = new StringTokenizer(sArg, ":");
            INITV_DIV = Double.parseDouble(st.nextToken());
            MINV_DIV = Double.parseDouble(st.nextToken());
         }
            break;
         case 'b': // band
            rBand = Double.parseDouble(sArg);
            break;
         case 'z': // znorm
            if (Library.stricmp(sArg, "global")) znorm = ZNorm.global;
            else if (Library.stricmp(sArg, "seq")) znorm = ZNorm.seq;
            else if (Library.stricmp(sArg, "subseq")) znorm = ZNorm.subseq;
            else{
               System.err.println("Error: unrecognized \"znorm\" option: " + sArg);
               System.exit(1);
            }
            break;
         case 1001: // hmmeval
            if (Library.stricmp(sArg, "viterbi")) hmmEval = HmmEval.viterbi;
            else if (Library.stricmp(sArg, "forward")) hmmEval = HmmEval.forward;
            else{
               System.err.println("Error: unrecognized \"hmmeval\" option: " + sArg);
               System.exit(1);
            }
            break;
         case 1002: // hmmstates
            nHmmStates = Integer.parseInt(sArg);
            break;
         case 1003: // hmmskip
            nHmmSkip = Integer.parseInt(sArg);
            break;
         case 1004: // hmmtrain
            if (Library.stricmp(sArg, "viterbi")) hmmTrain = HmmTrain.viterbi;
            else if (Library.stricmp(sArg, "bw")) hmmTrain = HmmTrain.bw;
            else{
               System.err.println("Error: unrecognized \"hmmtrain\" option: " + sArg);
               System.exit(1);
            }
            break;
         case 1005: // output
            if (Library.stricmp(sArg, "human")) output = Output.human;
            else if (Library.stricmp(sArg, "comp") || Library.stricmp(sArg, "computer")) output = Output.computer;
            else{
               System.err.println("Error: unrecognized \"output\" option: " + sArg);
               System.exit(1);
            }
            break;
         case 1006: // presplit
         {
            StringTokenizer st = new StringTokenizer(sArg, ":");
            assert (st.countTokens() == 4) : "-presplit parameter should have form: w:t:g:o";
            splitW = Integer.parseInt(st.nextToken());
            splitT = Double.parseDouble(st.nextToken());
            splitG = Integer.parseInt(st.nextToken());
            splitO = Integer.parseInt(st.nextToken());
            assert splitW > 0;
         }
            break;
         case 1008: // nsymbols
            nSymbols = Integer.parseInt(sArg);
            break;
         case 1009: // wspot_min
            nSpotsMin = Integer.parseInt(sArg);
            break;
         case 1010: // wspot_max
            nSpotsMax = Integer.parseInt(sArg);
            break;
         case 1011: // wspot_thresh
            spotThresh = Double.parseDouble(sArg);
            break;
         case 1012: // word_len
         {
            StringTokenizer st = new StringTokenizer(sArg, ":");
            assert (st.countTokens() == 2) : "-word_len parameter should ahve form: min:max";
            rWordLen = new Range(Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken()));
         }
            break;
         case 1013: // scoremeth
            if (Library.stricmp(sArg, "direct")) scoreMethod = ScoreMethod.direct;
            else if (Library.stricmp(sArg, "divmean")) scoreMethod = ScoreMethod.direct;
            else if (Library.stricmp(sArg, "sigvar")) scoreMethod = ScoreMethod.direct;
            else{
               System.err.println("Error: unrecognized -scoremeth option: " + sArg);
               System.exit(1);
            }
            break;
         case 1014: // noclean
            bClean = false;
            break;
         case 1015: // mmtrain
            if (Library.stricmp(sArg, "centroid")) mmTrain = MetricModel.Method.Centroid;
            else if (Library.stricmp(sArg, "nn")) mmTrain = MetricModel.Method.NN;
            break;
         case 1016: // lenprep
            if (Library.stricmp(sArg, "none")) lenPrep = MetricSeq.LengthPrep.none;
            else if (Library.stricmp(sArg, "extend")) lenPrep = MetricSeq.LengthPrep.extend;
            else if (Library.stricmp(sArg, "shrink")) lenPrep = MetricSeq.LengthPrep.shrink;
            break;
         case 1017: // trans
            try{
               Class cls = Library.getClass(sArg, "kdm.data.transform");
               DataTransform dtran = (DataTransform)cls.newInstance();
               trans.add(dtran);
               break;
            } catch (Exception e){
               System.err.printf("Error: failed to instantiated DataTransform (%s)\n\n", sArg);
               e.printStackTrace();
               System.exit(1);
            }
         default:
            System.err.println("unrecognized command line option: " + c);
            System.exit(1);
            break;
         }
      }

      // make sure that a data def file was specified
      if (g.getOptind() >= args.length){
         System.err.println("Error: no data definition file specified!");
         System.err.println();
         usage();
         return;
      }

      if (bVerbose){
         System.err.println();
         System.err.println("Settings");
         System.err.println("-----------------------------------");
         System.err.printf(" Model: %s\n", model);
         System.err.printf(" Length Prep: %s\n", lenPrep);
         System.err.printf(" DTW rBand: %.2f\n", rBand);
         System.err.printf(" Metric Method: %s\n", mmTrain);
         System.err.printf(" Vector Metric: %s\n", fvm.getClass());
         System.err.println();
      }

      // load the data def file
      System.err.print("Loading data def file... ");
      TreeMap<String, ArrayList<Sequence>> data = LabeledDataLoader.load(new File(args[g.getOptind()]));
      if (data == null){
         System.err.println("Error: failed to load data definition file");
         System.exit(1);
      }
      Sequence[] tseries = LabeledDataLoader.tseries.toArray(new Sequence[0]);
      MarkupSet[] marks = LabeledDataLoader.marks.toArray(new MarkupSet[0]);
      int nSeqs = tseries.length;
      System.err.printf("done (%d).\n", nSeqs);

      if (trans.size() > 0){
         System.err.print("Transforming data... ");
         TimerMS timer = new TimerMS();
         for(int i = 0; i < tseries.length; i++)
            for(DataTransform dtran : trans)
               tseries[i] = dtran.transform(tseries[i]);
         System.err.printf("done (%dms).\n", timer.time());
      }

      nLabeledFrames = new int[nSeqs];
      nLabeledWords = new int[nSeqs];
      nDims = tseries[0].getNumDims();
      for(Sequence seq : tseries)
         nTotalFrames += seq.length();

      // calc global gms
      Gaussian1D gm[] = calcGauss(tseries);

      // handle global normalization if requested
      if (znorm == ZNorm.global){
         for(int i = 0; i < tseries.length; i++)
            tseries[i].znorm(gm);
         for(int i = 0; i < gm.length; i++)
            gm[i].set(0, 1);
      }
      else if (znorm == ZNorm.seq){
         for(int i = 0; i < tseries.length; i++)
            tseries[i].znorm();
      }

      // now that we have the var for each dim, setup initv/minv
      initv = new FeatureVec(gm.length);
      minv = new FeatureVec(gm.length);
      for(int i = 0; i < gm.length; i++){
         initv.set(i, gm[i].getVar() / INITV_DIV);
         minv.set(i, gm[i].getVar() / MINV_DIV);
      }

      // now we can extract the labeled occurrences
      for(int iSeq = 0; iSeq < tseries.length; iSeq++){
         Sequence seq = tseries[iSeq];
         MarkupSet mark = marks[iSeq];
         if (mark == null) continue;
         nLabeledWords[iSeq] = mark.size();
         nTotalLabeledWords += mark.size();

         for(int iMark = 0; iMark < mark.size(); iMark++){
            TimeMarker tm = mark.get(iMark);
            assert (tm.isIndex());
            nLabeledFrames[iSeq] += tm.length();
            nTotalLabeledFrames += tm.length();
         }
      }

      // dump a summary of the data
      dumpDataSummary(data);

      if (op == Op.conf) calcConfMatrix(data);
      else if (op == Op.sim) calcSimMatrix(data, nTotalLabeledWords);
      else if (op == Op.map) calcMaps(data);
      else if (op == Op.cont) calcContinuous(tseries, data);
      else if (op == Op.wspot) calcWordSpot(tseries, data, data);
      else if (op == Op.stats) calcStats(tseries, data);
      else if (op == Op.label) calcLabels(tseries, data, marks);
   }
}
