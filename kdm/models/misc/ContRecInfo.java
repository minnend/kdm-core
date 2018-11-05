package kdm.models.misc;

import java.util.*;
import java.io.*;
import java.util.regex.*;

import kdm.data.*;
import kdm.io.*;
import kdm.util.*;

/**
 * Holds info relevant to evaluating continuous recognition and word spotting
 */
public class ContRecInfo
{
   public static final int CORRECT = 0;
   public static final int INSERTION = 1;
   public static final int DELETION = 2;
   public static final int SUBSTITUTION = 3;
   public static final int NUMBER = 4;

   /** number of words found by models */
   public int nSpotWords = 0;

   /** number of frames labeled by models */
   public int nSpotFrames = 0;

   /** number of words in labeled data (from oracle) */
   public int nLabeledWords = 0;

   /** number of frames labeled in labeled data (from oracle) */
   public int nLabeledFrames = 0;

   /** per class stats [iClass][C/I/D/S] */
   public int classStats[][];

   public int nWordCorrect = 0;
   public int nWordInsertions = 0;
   public int nWordDeletions = 0;
   public int nWordSubstitutions = 0;

   public int nFrameCorrect = 0;
   public int nFrameInsertions = 0;
   public int nFrameDeletions = 0;
   public int nFrameSubstitutions = 0;

   /** words labeled by during recognition */
   protected ArrayList<WordSpot> spots = new ArrayList();

   /** protected constructor used by static load method */
   protected ContRecInfo()
   {}

   /** construct a CRI with the given number of true classes */
   public ContRecInfo(int nTrueClasses)
   {
      classStats = new int[nTrueClasses][4];
   }

   public int getNumTrueClasses()
   {
      return (classStats == null ? 0 : classStats.length);
   }

   protected void clearClassStats()
   {
      for(int i = 0; i < classStats.length; i++)
         Arrays.fill(classStats[i], 0);
   }

   public int[] getClassStats(int i)
   {
      return classStats[i];
   }

   /**
    * Add a spot to the list; this will also increase the number of spotted words and spotted frames.
    * 
    * @param spot the spot to add
    */
   public void add(WordSpot spot)
   {
      spots.add(spot);
      nSpotWords++;
      nSpotFrames += spot.length();
   }

   public WordSpot getSpot(int i)
   {
      return spots.get(i);
   }

   /** @return number of found spots */
   public int getNumSpots()
   {
      return spots.size();
   }

   /**
    * @return number of motifs (classes) found; calc'd by computing union of all spot classes
    */
   public int calcNumMotifs()
   {
      HashSet<Short> set = new HashSet<Short>();
      for(WordSpot spot : spots)
         if (spot.iClass >= 0) set.add(spot.iClass);
      return set.size();
   }

   /** @return total number of frames in all found spots */
   public int getNumFrames()
   {
      return nSpotFrames;
   }

   public ArrayList<WordSpot> getSpots()
   {
      return spots;
   }

   /**
    * Convert the spotted words into a list of markup sets
    * 
    * @param cnames name of each class
    * @return list of markup sets for each sequence (number implicit from word spots)
    */
   public ArrayList<MarkupSet> getLabels(String[] cnames)
   {
      ArrayList<MarkupSet> labels = new ArrayList<MarkupSet>();

      for(WordSpot spot : spots){
         TimeMarker tm = new TimeMarker(cnames[spot.iClass], TimeMarker.Units.Index, spot.getFirstIndex(),
               spot.getLastIndex() + 1);
         while(spot.iSeries >= labels.size())
            labels.add(new MarkupSet());
         labels.get(spot.iSeries).add(tm);
      }
      for(MarkupSet marks : labels)
         marks.sort();

      return labels;
   }

   public int getTotalFramesSpotted()
   {
      return nFrameCorrect + nFrameInsertions + nFrameSubstitutions;
   }

   public int[] getRawFrameCounts()
   {
      int[] ret = new int[5];
      ret[CORRECT] = nFrameCorrect;
      ret[INSERTION] = nFrameInsertions;
      ret[DELETION] = nFrameDeletions;
      ret[SUBSTITUTION] = nFrameSubstitutions;
      ret[NUMBER] = nLabeledFrames;
      return ret;
   }

   public int[] getRawWordCounts()
   {
      int[] ret = new int[5];
      ret[CORRECT] = nWordCorrect;
      ret[INSERTION] = nWordInsertions;
      ret[DELETION] = nWordDeletions;
      ret[SUBSTITUTION] = nWordSubstitutions;
      ret[NUMBER] = nLabeledWords;
      return ret;
   }

   public double getWordRecall()
   {
      return (double)nWordCorrect / nLabeledWords;
   }

   public double getWordAcc()
   {
      return (double)(nWordCorrect - nWordInsertions - nWordSubstitutions) / nLabeledWords;
   }

   public double getWordPrec()
   {
      return (double)nWordCorrect / (nWordCorrect + nWordInsertions + nWordSubstitutions);
   }

   public double getClassAcc(int iClass)
   {
      int[] a = classStats[iClass];
      int n = a[CORRECT] + a[DELETION] + a[SUBSTITUTION];
      return (double)(a[CORRECT] - (a[INSERTION] + a[DELETION] + a[SUBSTITUTION])) / n;
   }

   public double getClassPrec(int iClass)
   {
      int[] a = classStats[iClass];
      int n = a[CORRECT] + a[INSERTION] + a[SUBSTITUTION];
      return (double)a[CORRECT] / n;
   }

   public double getClassRecall(int iClass)
   {
      int[] a = classStats[iClass];
      int n = a[CORRECT] + a[DELETION] + a[SUBSTITUTION];
      return (double)a[CORRECT] / n;
   }

   public double getFrameRecall()
   {
      return (double)nFrameCorrect / nLabeledFrames;
   }

   public double getFrameAcc()
   {
      return (double)(nFrameCorrect - nFrameInsertions - nFrameSubstitutions) / nLabeledFrames;
   }

   public double getFramePrec()
   {
      return (double)nFrameCorrect / (nFrameCorrect + nFrameInsertions + nFrameSubstitutions);
   }

   protected static int scoreMapping(int[][] omap, short[] map)
   {
      int v = 0;
      for(int i = 0; i < map.length; i++)
         if (map[i] >= 0) v += omap[i][map[i]];
      return v;
   }

   protected static int findBestMapping(int[][] omap, int[] nFoundLabels, int[] nTrueLabels, boolean[] used,
         int iFound, short[] temp, int best, short[] map, boolean bOptAcc)
   {
      if (iFound == omap.length){
         int v = scoreMapping(omap, temp);
         if (v > best){
            best = v;
            Library.copy(temp, map);
         }
         return best;
      }

      // try mapping this motif to all known classes
      for(int i = 0; i < used.length; i++){
         if (used[i]) continue;
         int C = omap[iFound][i];
         if (C == 0) continue;
         if (bOptAcc && (C < (nFoundLabels[iFound]+1) / 2)) continue; // ensure acc doesn't go negative (C-I>=0)

         used[i] = true;
         temp[iFound] = (short)i;
         int v = findBestMapping(omap, nFoundLabels, nTrueLabels, used, iFound + 1, temp, best, map, bOptAcc);
         if (v > best) best = v;
         used[i] = false;
      }

      // maybe this motif isn't mapped to any known class
      temp[iFound] = -1;
      int v = findBestMapping(omap, nFoundLabels, nTrueLabels, used, iFound + 1, temp, best, map, bOptAcc);
      return Math.max(v, best);
   }

   /**
    * Compute the best mapping from found classes to true classes
    * @param omap overlap map [nFound][nTrue]
    * @param nFoundLabels number of found occurrences for each found class
    * @param nTrueLabels number of true occurrences of each true class
    * @param bOptAcc true to optimize accuracy, false to optimize recall
    * @return map from found class to true class (-1 for not found)
    */
   public static short[] calcBestMappingFromOMap(int[][] omap, int[] nFoundLabels, int[] nTrueLabels,
         boolean bOptAcc)
   {
      assert (nTrueLabels.length == omap[0].length);
      short[] map = new short[omap.length];
      Arrays.fill(map, (short)-1);
      short[] temp = new short[omap.length];

      boolean[] used = new boolean[omap[0].length];
      findBestMapping(omap, nFoundLabels, nTrueLabels, used, 0, temp, 0, map, bOptAcc);

      return map;
   }

   /** @return max number of overlaps for the given set of spots and labels */
   protected int findMaxOverlap(ArrayList<WordSpot> spots, ArrayList<WindowLocation> labels, int iSpot,
         int iLabel, int nOverlap)
   {
      int nSpots = spots.size();
      if (iSpot == nSpots) return nOverlap;

      // see if iSpot overlaps anything available
      int nMax = 0;
      int nLabels = labels.size();
      WordSpot spot = spots.get(iSpot);
      for(int i = iLabel; i < nLabels; i++){
         if (!spot.overlaps(labels.get(i))) continue;
         int n = findMaxOverlap(spots, labels, iSpot + 1, i + 1, nOverlap + 1);
         if (n > nMax) nMax = n;
      }
      
      // TODO optimization: keep last overlap label, pass max(iLabel, iMax-1)

      // see what happens if iSpot doesn't match anything
      int n = findMaxOverlap(spots, labels, iSpot + 1, iLabel, nOverlap);
      return Math.max(n, nMax);
   }

   protected int findMaxOverlap(ArrayList<WordSpot> spots, WindowLocation[] labels, int nSeqs)
   {
      int n = 0;
      for(int iSeq = 0; iSeq < nSeqs; iSeq++){
         // find all spots and labels for this sequence
         ArrayList<WordSpot> spotsI = new ArrayList<WordSpot>();
         for(WordSpot spot : spots)
            if (spot.iSeries == iSeq) spotsI.add(spot);

         ArrayList<WindowLocation> labelsI = new ArrayList<WindowLocation>();
         for(int i = 0; i < labels.length; i++)
            if (labels[i].iSeries == iSeq) labelsI.add(labels[i]);

         // make sure lists are sorted by time
         Collections.sort(spotsI, new Comparator<WordSpot>() {
            public int compare(WordSpot o1, WordSpot o2)
            {
               if (o1.iStart < o2.iStart) return -1;
               if (o1.iStart > o2.iStart) return 1;
               return 0;
            }

         });
         Collections.sort(labelsI);

         n += findMaxOverlap(spotsI, labelsI, 0, 0, 0);
      }
      return n;
   }

   /**
    * "clean" an overlap map by removing any element in a row or column that is less than 'frac' times the
    * maximum element
    */
   public void cleanOMap(int[][] m, double frac)
   {
      class Entry implements Comparable{
         public int score, i ,j;
         public Entry(int i, int j, int score){
            this.i=i;this.j=j;this.score=score;
         }
         public int compareTo(Object o)
         {
            Entry x = (Entry)o;
            if (score < x.score) return 1;
            if (score > x.score) return -1;
            return 0;
         }
         
         public boolean equals(Object o){ return compareTo(o)==0; }
      }
      
      PriorityQueue<Entry> q = new PriorityQueue<Entry>();
      for(int i=0; i<m.length; i++)
         for(int j=0; j<m[i].length; j++)
            if (m[i][j]>0) q.add(new Entry(i,j,m[i][j]));
      
      while(!q.isEmpty()){
         Entry x = q.poll();
         if (m[x.i][x.j]==0) continue;
         double thresh = (int)(x.score*frac);
         
         //System.err.printf("Anchor: (%d, %d) %d\n", x.i, x.j, x.score);
         
         // scan column
         for(int i=0; i<m.length;i++)
            if (m[i][x.j]<=thresh) m[i][x.j] = 0;
         
         // scan row
         for(int i=0; i<m[x.i].length;i++)
            if (m[x.i][i]<=thresh) m[x.i][i] = 0;
      }
   }

   /**
    * Build an overlap map for the given labels and the spots stored in this object
    * 
    * @param labData ground truth labels
    * @return overlap map [nFound][nTrue]
    */
   public int[][] buildOverlapMap(TreeMap<String, ArrayList<Sequence>> labData, int nSeqs)
   {
      int nSpots = getNumSpots();
      if (nSpots == 0) return null;
      int nTrue = labData.size();

      // build window location list for each ground truth label
      WindowLocation[][] gtLocs = new WindowLocation[nTrue][];
      Iterator<String> it = labData.keySet().iterator();
      for(int i = 0; i < nTrue; i++){
         ArrayList<Sequence> labels = labData.get(it.next());
         gtLocs[i] = new WindowLocation[labels.size()];
         for(int j = 0; j < gtLocs[i].length; j++){
            Sequence seq = labels.get(j);
            gtLocs[i][j] = new WindowLocation(seq.getParentIndex(), seq.getParentOffset(), seq.length());
         }
      }

      // figure out how many classes were found
      int nFound = 0;
      for(WordSpot spot : spots)
         if (spot.iClass >= nFound) nFound = spot.iClass + 1;

      int[][] m = new int[nFound][nTrue];
      for(int iFound = 0; iFound < nFound; iFound++){
         // build set of all spots with this index
         ArrayList<WordSpot> spotsI = new ArrayList<WordSpot>();
         for(WordSpot spot : spots)
            if (spot.iClass == iFound) spotsI.add(spot);
         for(int iTrue = 0; iTrue < nTrue; iTrue++)
            m[iFound][iTrue] = findMaxOverlap(spotsI, gtLocs[iTrue], nSeqs);
      }

      return m;
   }

   /**
    * Compute the number of C,I,D,S words given the labeled data and the spotted words
    * 
    * @return confusion matrix [nFound][nTrue+1] (extra for null)
    */
   public int[][] scoreWordSpotWordsFast(TreeMap<String, ArrayList<Sequence>> labData)
   {
      int nSpots = getNumSpots();
      if (nSpots == 0) return null;

      int nTrueClasses = labData.size();
      int nLabels = nLabeledWords;

      // figure out how many examples of each class we have
      int nExamples[] = new int[nTrueClasses];
      Iterator<String> it = labData.keySet().iterator();
      for(int i = 0; i < nTrueClasses; i++)
         nExamples[i] = labData.get(it.next()).size();

      // figure out how many classes were found
      int nFoundClasses = 0;
      for(WordSpot spot : spots)
         if (spot.iClass > nFoundClasses - 1) nFoundClasses = spot.iClass + 1;
      int[][] conf = new int[nFoundClasses][nTrueClasses + 1]; // extra true class is "null"

      nWordCorrect = 0;
      nWordInsertions = 0;
      nWordDeletions = 0;
      nWordSubstitutions = 0;
      clearClassStats();

      // info on which true and found spots are left
      SpanList[] spanTrue = new SpanList[nTrueClasses];
      SpanList spanFound = new SpanList(0, nSpots - 1, true);

      // match correct spots first
      Iterator<String> itClass = labData.keySet().iterator();
      for(int iClass = 0; iClass < nTrueClasses; iClass++){
         String sClass = itClass.next();
         spanTrue[iClass] = new SpanList(0, nExamples[iClass] - 1, true);

         // iterate over all subseqs in this class
         Iterator<Sequence> itExamples = labData.get(sClass).iterator();
         SpanList spotMatched = new SpanList(0, nSpots - 1, false);
         for(int iExample = 0; iExample < nExamples[iClass]; iExample++){
            Sequence seq = itExamples.next();
            Range rSeq = new Range(seq.getParentOffset(), seq.getParentOffset() + seq.length() - 1);

            // find the best (unmatched) spot that overlaps with this labeled subseq
            int iBestSpot = -1;
            int bestScore = Integer.MIN_VALUE;
            SpanIterator itFound = spanFound.iterator();
            while(itFound.hasNext()){
               int iSpot = itFound.next();
               WordSpot spot = getSpot(iSpot);
               if (spot.iSeries != seq.getParentIndex()) continue;
               if (spot.iClass != iClass) continue;

               Range rSpot = spot.getRange();
               if (!rSpot.intersects(rSeq)) continue;

               // score of a spot is # overlap - # not overlap
               // ties go to the spot closest in length to the labeled subseq
               // ties from this go to the first spot
               int score = rSpot.getNumOverlap(rSeq) - rSpot.getNumNotOverlap(rSeq);
               if (score > bestScore
                     || (score == bestScore && (Math.abs(rSpot.length() - rSeq.length()) < Math.abs(getSpot(
                           iBestSpot).length()
                           - rSeq.length())))){
                  bestScore = score;
                  iBestSpot = iSpot;
               }
            }

            if (iBestSpot >= 0){
               // we found a match, so record it
               nWordCorrect++;
               classStats[iClass][CORRECT]++;
               spanFound.sub(iBestSpot);
               spanTrue[iClass].sub(iExample);
               WordSpot spot = spots.get(iBestSpot);
               spot.errorType = WordSpot.ERROR_CORRECT;
               conf[iClass][iClass]++;
            }
         }
      }

      // now we can search for substitution errors
      SpanList spotMatched = new SpanList(0, nSpots - 1, false);
      SpanIterator itFound = spanFound.iterator();
      while(itFound.hasMore()){
         int iSpot = itFound.next();
         WordSpot spot = getSpot(iSpot);
         Range rSpot = spot.getRange();

         String sBestClass = null;
         Range rBestSeq = null;
         int iBestClass = -1;
         int iBestExample = -1;
         int bestScore = Integer.MIN_VALUE;

         // find the best match (if there is one) for this spot
         itClass = labData.keySet().iterator();
         for(int iClass = 0; iClass < nTrueClasses; iClass++){
            String sClass = itClass.next();
            if (iClass == spot.iClass) continue; // we've already found all the correct matches
            SpanIterator itTrue = spanTrue[iClass].iterator();
            while(itTrue.hasMore()){
               int iExample = itTrue.next();
               Sequence seq = labData.get(sClass).get(iExample);
               if (spot.iSeries != seq.getParentIndex()) continue;
               Range rSeq = new Range(seq.getParentOffset(), seq.getParentOffset() + seq.length() - 1);

               // score of a spot is # overlap - # not overlap
               // ties go to the spot closest in length to the labeled subseq
               // ties from this go to the first spot
               if (!rSeq.intersects(rSpot)) continue;
               int score = rSeq.getNumOverlap(rSpot) - rSeq.getNumNotOverlap(rSpot);
               if (score > bestScore
                     || (score == bestScore && (Math.abs(rSpot.length() - rSeq.length()) < Math.abs(rSpot
                           .length()
                           - rBestSeq.length())))){
                  rBestSeq = rSeq;
                  iBestClass = iClass;
                  iBestExample = iExample;
                  bestScore = score;
               }
            }
         }

         if (iBestClass >= 0){
            nWordSubstitutions++;
            classStats[iBestClass][SUBSTITUTION]++;
            if (spot.iClass >= 0){
               conf[spot.iClass][iBestClass]++;
               if (spot.iClass < nTrueClasses) classStats[spot.iClass][INSERTION]++;
            }
            spot.errorType = WordSpot.ERROR_SUBSTITUTION;
            spotMatched.add(iSpot);
            spanTrue[iBestClass].sub(iBestExample);
         }
      }
      spanFound.sub(spotMatched); // remove the subs errors from the spot list

      // we've matched all the spots, so go through and classify the leftover labels
      for(int iClass = 0; iClass < nTrueClasses; iClass++){
         int nDeletions = spanTrue[iClass].size();
         nWordDeletions += nDeletions;
         classStats[iClass][DELETION] += nDeletions;
      }

      // all remaining spots are insertion errors
      nWordInsertions += spanFound.size();
      itFound = spanFound.iterator();
      while(itFound.hasMore()){
         WordSpot spot = spots.get(itFound.next());
         int iClass = spot.iClass;
         spot.errorType = WordSpot.ERROR_INSERTION;
         if (iClass >= 0){
            if (iClass < nTrueClasses) classStats[iClass][INSERTION]++;
            conf[iClass][nTrueClasses]++;
         }
      }

      return conf;
   }

   /**
    * perform the full (exponential) search for the best matching between found and true instances of a
    * particular class
    * 
    * @param iClass index of class that we're matching
    * @param nSeqs number of sequences
    * @param spots found spots
    * @param gtSeqs ground truth sequences
    * @param confRet row from a confusion matrix to fill in
    * @return true if processing is successful
    */
   protected boolean matchSpots(int iClass, int nSeqs, ArrayList<WordSpot> spots,
         ArrayList<Sequence> gtSeqs, int[] confRet)
   {
      Arrays.fill(confRet, 0);
      int bestScore = Integer.MIN_VALUE;
      int[] confSeq = new int[confRet.length];
      int[] curConfRow = new int[confRet.length];
      int[] bestConfRow = new int[confRet.length];

      int nGT = gtSeqs.size();
      boolean[] used = new boolean[nGT];
      WindowLocation[] gtLocs = new WindowLocation[nGT];
      for(int i = 0; i < nGT; i++)
         gtLocs[i] = gtSeqs.get(i).getWindowLoc();

      // handle each sequence separately
      for(int iSeq = 0; iSeq < nSeqs; iSeq++){

         ArrayList<WindowLocation> labelsI = new ArrayList<WindowLocation>();
         for(WindowLocation wloc : gtLocs)
            if (wloc.iSeries == iSeq) labelsI.add(wloc);
         ArrayList<WordSpot> spotsI = new ArrayList<WordSpot>();
         for(WordSpot spot : spots)
            if (spot.iSeries == iSeq) spotsI.add(spot);

         TemporalEventMap tem = new TemporalEventMap(labelsI, spotsI);
         int nGraphs = tem.getNumSubgraphs();
         Arrays.fill(confSeq, 0);

         // process each subgraph
         for(int ig = 0; ig < nGraphs; ig++){
            Arrays.fill(bestConfRow, 0);
            ArrayList<WordSpot> spotsG = tem.getGraphSpots(ig);
            ArrayList<WindowLocation> labelsG = tem.getGraphLabels(ig);
            if (!matchSpots(iClass, 0, spotsG, labelsG, curConfRow, bestConfRow, used)) return false;
            for(int i = 0; i < confRet.length; i++)
               confSeq[i] += bestConfRow[i];
            assert (spotsG.size() == Library.sum(bestConfRow));
         }

         // add all spots that weren't in a subgraph as insertions
         confSeq[confSeq.length - 1] += (spotsI.size() - Library.sum(confSeq));

         // copy results from this sequence (all graphs) into the return conf row
         for(int i = 0; i < confRet.length; i++)
            confRet[i] += confSeq[i];
      }
      return true;
   }

   protected boolean matchSpots(int iClass, int iSpot, ArrayList<WordSpot> spots,
         ArrayList<WindowLocation> gtLocs, int[] curConfRow, int[] bestConfRow, boolean[] used)
   {
      // are we done?
      if (iSpot >= spots.size()){
         if (curConfRow[iClass] > bestConfRow[iClass]) Library.copy(curConfRow, bestConfRow);
         return true;
      }

      // process this spot
      WordSpot spot = spots.get(iSpot);
      assert (spot.errorType == WordSpot.ERROR_UNKNOWN);
      assert (spot.iClass == iClass);

      // it could be correct
      spot.errorType = WordSpot.ERROR_CORRECT;
      int nGT = gtLocs.size();
      for(int iGT = 0; iGT < nGT; iGT++){
         if (used[iGT]) continue;
         if (!gtLocs.get(iGT).overlaps(spot)) continue;
         used[iGT] = true;
         curConfRow[iClass]++;
         matchSpots(iClass, iSpot + 1, spots, gtLocs, curConfRow, bestConfRow, used);
         curConfRow[iClass]--;
         used[iGT] = false;
      }

      // or it could be an insertion
      spot.errorType = WordSpot.ERROR_INSERTION;
      curConfRow[curConfRow.length - 1]++;
      matchSpots(iClass, iSpot + 1, spots, gtLocs, curConfRow, bestConfRow, used);
      curConfRow[curConfRow.length - 1]--;

      // backtrack
      spot.errorType = WordSpot.ERROR_UNKNOWN;
      return true;
   }

   /**
    * Compute the number of C,I,D,S words given the labeled data and the spotted words
    * 
    * @return confusion matrix [nFound][nTrue]
    */
   public int[][] scoreWordSpotWordsFull(TreeMap<String, ArrayList<Sequence>> labData)
   {
      int nSpots = getNumSpots();
      if (nSpots == 0) return null;

      int nTrueClasses = labData.size();
      int nLabels = nLabeledWords;

      // figure out how many examples of each class we have and how many sequences there are
      int nSeqs = 0;
      int nExamples[] = new int[nTrueClasses];
      Iterator<String> it = labData.keySet().iterator();
      for(int i = 0; i < nTrueClasses; i++){
         ArrayList<Sequence> examples = labData.get(it.next());
         for(Sequence seq : examples)
            if (seq.getParentIndex() >= nSeqs) nSeqs = seq.getParentIndex() + 1;
         nExamples[i] = examples.size();
      }

      // figure out how many classes were found
      int nFoundClasses = 0;
      for(WordSpot spot : spots){
         if (spot.iClass >= nFoundClasses) nFoundClasses = spot.iClass + 1;
         if (spot.iSeries >= nSeqs) nSeqs = spot.iSeries + 1;
      }
      int[][] conf = new int[nFoundClasses][nTrueClasses + 1]; // extra true class is "null"

      nWordCorrect = 0;
      nWordInsertions = 0;
      nWordDeletions = 0;
      nWordSubstitutions = 0;
      clearClassStats();

      // handle each true class
      Iterator<String> itClass = labData.keySet().iterator();
      for(int iClass = 0; iClass < Math.min(nFoundClasses, nTrueClasses); iClass++){
         String sClass = itClass.next();

         // extract the spots for this class
         ArrayList<WordSpot> cspots = new ArrayList<WordSpot>();
         for(int i = 0; i < nSpots; i++){
            WordSpot spot = spots.get(i);
            if (spot.iClass != iClass) continue;
            cspots.add(spot);
            spot.errorType = WordSpot.ERROR_UNKNOWN;
         }

         matchSpots(iClass, nSeqs, cspots, labData.get(sClass), conf[iClass]);

         nWordCorrect += conf[iClass][iClass];
         classStats[iClass][CORRECT] += conf[iClass][iClass];
         for(int i = 0; i < nTrueClasses; i++){
            if (i == iClass) continue;
            classStats[iClass][SUBSTITUTION] += conf[iClass][i];
            nWordSubstitutions += conf[iClass][i];
         }
         classStats[iClass][INSERTION] = conf[iClass][nTrueClasses];
         nWordInsertions += conf[iClass][nTrueClasses];
         classStats[iClass][DELETION] = nExamples[iClass] - classStats[iClass][CORRECT]
               - classStats[iClass][SUBSTITUTION];
         nWordDeletions += classStats[iClass][DELETION];

      }

      // take care of true classes that have no match
      for(int iClass = nFoundClasses; iClass < nTrueClasses; iClass++){
         int dels = nExamples[iClass] - classStats[iClass][CORRECT] - classStats[iClass][SUBSTITUTION];
         classStats[iClass][DELETION] = dels;
         nWordDeletions += dels;
      }

      // take care of extra classes
      for(int iClass = nTrueClasses; iClass < nFoundClasses; iClass++){
         // extract the spots for this class
         int nSpotsI = 0;
         for(int i = 0; i < nSpots; i++){
            WordSpot spot = spots.get(i);
            if (spot.iClass != iClass) continue;
            spot.errorType = WordSpot.ERROR_INSERTION;
            nSpotsI++;
         }

         conf[iClass][nTrueClasses] = nSpotsI;
         nWordInsertions += nSpotsI;
      }

      return conf;
   }

   /**
    * Find the WordSpot that covers the given series and time index
    * 
    * @param iSeries index of the time series
    * @param t time index in iSeries
    * @return the relevant WordSpot or null if none accounts for the given position
    */
   public static WordSpot getWordSpot(int iSeries, int t, ArrayList<WordSpot> spots)
   {
      for(WordSpot spot : spots)
         if (spot.iSeries == iSeries && spot.contains(t)) return spot;
      return null;
   }

   /**
    * Find the class label for the given series and time index
    * 
    * @param iSeries index of the time series
    * @param t time index in iSeries
    * @param labels labeled data
    * @return the index of the class or -1 if the time is unlabeled
    */
   public static int getClassLabel(int iSeries, int t, TreeMap<String, ArrayList<Sequence>> labels)
   {
      int iClass = 0;
      Iterator<String> it = labels.keySet().iterator();
      while(it.hasNext()){
         for(Sequence seq : labels.get(it.next())){
            if (seq.getParentIndex() != iSeries) continue;
            if (t >= seq.getParentOffset() && t < seq.getParentOffset() + seq.length()) return iClass;
         }
         iClass++;
      }
      return -1;
   }

   /**
    * Compute the number of C,I,D,S frames in the data set and spotted words
    */
   public void scoreWordSpotFrames(ArrayList<Sequence> tseries, TreeMap<String, ArrayList<Sequence>> labels)
   {
      nFrameCorrect = 0;
      nFrameInsertions = 0;
      nFrameDeletions = 0;
      nFrameSubstitutions = 0;

      for(int i = 0; i < tseries.size(); i++){
         Sequence seq = tseries.get(i);
         for(int t = 0; t < seq.length(); t++){
            WordSpot spot = getWordSpot(i, t, spots);
            int iClass = getClassLabel(i, t, labels);

            if (iClass == -1){
               if (spot != null) nFrameInsertions++;
            }
            else{
               if (spot == null) nFrameDeletions++;
               else{
                  if (spot.iClass == iClass) nFrameCorrect++;
                  else nFrameSubstitutions++;
               }
            }
         }
      }
   }

   public String getDumpString()
   {
      StringBuffer sb = new StringBuffer();
      sb.append(String.format(" nSpotWords = %d\n", nSpotWords));
      sb.append(String.format(" nSpotFrames = %d\n", nSpotFrames));
      sb.append(String.format(" nLabeledWords = %d\n", nLabeledWords));
      sb.append(String.format(" nLabeledFrames = %d\n", nLabeledFrames));
      sb.append(String.format(" nWordCorrect = %d\n", nWordCorrect));
      sb.append(String.format(" nWordInsertions = %d\n", nWordInsertions));
      sb.append(String.format(" nWordDeletions = %d\n", nWordDeletions));
      sb.append(String.format(" nWordSubstitutions = %d\n", nWordSubstitutions));
      sb.append(String.format(" nFrameCorrect = %d\n", nFrameCorrect));
      sb.append(String.format(" nFrameInsertions = %d\n", nFrameInsertions));
      sb.append(String.format(" nFrameDeletions = %d\n", nFrameDeletions));
      sb.append(String.format(" nFrameSubstitutions = %d\n", nFrameSubstitutions));
      return sb.toString();
   }

   public void dump()
   {
      System.err.println("Continuous Rec Info:");
      System.err.println(getDumpString());
   }

   /**
    * Test cri values to make sure they're sensible (e.g., N=C+I+S).
    * 
    * @param bSpew write error info to stderr?
    * @return true if sensible
    */
   public boolean isValid(boolean bSpew)
   {
      if (nSpotWords != nWordCorrect + nWordInsertions + nWordSubstitutions){
         if (bSpew)
            System.err.printf("Invalid Words: %d = %d + %d + %d (N = CIS)\n", nSpotWords, nWordCorrect,
                  nWordInsertions, nWordSubstitutions);
         return false;
      }
      if (nLabeledWords != nWordCorrect + nWordDeletions + nWordSubstitutions){
         if (bSpew)
            System.err.printf("Invalid Words: %d = %d + %d + %d (N = CDS)\n", nLabeledWords, nWordCorrect,
                  nWordDeletions, nWordSubstitutions);
         return false;
      }

      if (nSpotFrames != nFrameCorrect + nFrameInsertions + nFrameSubstitutions){
         if (bSpew)
            System.err.printf("Invalid Frames: %d = %d + %d + %d (N=CIS)\n", nSpotFrames, nFrameCorrect,
                  nFrameInsertions, nFrameSubstitutions);
         return false;
      }
      if (nLabeledFrames != nFrameCorrect + nFrameDeletions + nFrameSubstitutions){
         if (bSpew)
            System.err.printf("Invalid Frames: %d = %d + %d + %d (N = CDS)\n", nLabeledFrames,
                  nFrameCorrect, nFrameDeletions, nFrameSubstitutions);
         return false;
      }

      return true;
   }

   public String getResultsString(String[] classes)
   {
      assert (isValid(true));

      StringBuffer sb = new StringBuffer();
      sb.append(String.format(" # spotted: w=%d  f=%d\n", nSpotWords, nSpotFrames));
      sb.append(String.format(" # labeled: w=%d  f=%d\n", nLabeledWords, nLabeledFrames));
      sb.append(String.format(" Word Level Results (CIDS):\n  # %d  %d  %d  %d\n", nWordCorrect,
            nWordInsertions, nWordDeletions, nWordSubstitutions));
      sb.append(String.format("  %.1f  %.1f  %.1f (acc/rcl/prec)\n", getWordAcc() * 100.0,
            getWordRecall() * 100.0, getWordPrec() * 100.0));
      for(int iClass = 0; iClass < getNumTrueClasses(); iClass++){
         sb.append(String.format("   %d: %.1f  %.1f  %.1f  (%d  %d  %d  %d) %s\n", iClass + 1,
               getClassAcc(iClass) * 100.0, getClassRecall(iClass) * 100.0, getClassPrec(iClass) * 100.0,
               classStats[iClass][CORRECT], classStats[iClass][INSERTION], classStats[iClass][DELETION],
               classStats[iClass][SUBSTITUTION], classes == null ? "" : classes[iClass]));
      }
      sb.append(String.format(" Frame Level Results (CIDS):\n  # %d  %d  %d  %d\n", nFrameCorrect,
            nFrameInsertions, nFrameDeletions, nFrameSubstitutions));
      sb.append(String.format("  %.1f  %.1f  %.1f (rcl/acc/prec)\n", getFrameAcc() * 100.0,
            getFrameRecall() * 100.0, getFramePrec() * 100.0));
      return sb.toString();
   }

   /**
    * Write results info to stderr
    * 
    * @param sHeader print this line first to identify the info
    */
   public void showResults(String sHeader, String[] classes)
   {
      System.err.println(sHeader);
      System.err.println(getResultsString(classes));
   }

   /**
    * Save the cri data in the given file as text.
    * 
    * @param f the file in which to save the data
    * @return true if successful
    */
   public boolean save(File f)
   {
      try{
         PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)));
         assert (nSpotWords == spots.size());
         out.printf("%d %d %d %d\n", nSpotWords, nSpotFrames, nLabeledWords, nLabeledFrames);
         out.printf("%d %d %d %d\n", nWordCorrect, nWordInsertions, nWordDeletions, nWordSubstitutions);
         out.printf("%d %d %d %d\n", nFrameCorrect, nFrameInsertions, nFrameDeletions, nFrameSubstitutions);
         for(WordSpot spot : spots)
            out.println(spot.toText());
         out.printf("%d\n", classStats.length);
         for(int i = 0; i < classStats.length; i++){
            for(int j = 0; j < 4; j++)
               out.printf("%d ", classStats[i][j]);
            out.println();
         }
         out.close();
         return true;
      } catch (IOException e){
         return false;
      }
   }

   /**
    * Remove spots that don't correspond to known classes
    * 
    * @param nTrueClasses number of known classes
    * @return list of removed spots
    */
   public ArrayList<WordSpot> removeUnknown(int nTrueClasses)
   {
      ArrayList<WordSpot> removed = new ArrayList<WordSpot>();
      for(int i = 0; i < spots.size();){
         WordSpot spot = spots.get(i);
         if (spot.iClass < 0 || spot.iClass >= nTrueClasses){
            removed.add(spot);
            spots.remove(i);
         }
         else i++;
      }
      return removed;
   }

   /**
    * Invert the given map
    * 
    * @param map map[i] = j
    * @return map[j] = i
    */
   public short[] invertMap(short map[])
   {
      // find largest value
      int N = 0;
      for(int i = 0; i < map.length; i++)
         if (map[i] > N) N = map[i];

      // calc the imap
      short[] imap = new short[N + 1];
      Arrays.fill(imap, (short)-1);
      for(int i = 0; i < map.length; i++){
         if (map[i] >= 0){
            assert (imap[map[i]] == -1 || imap[map[i]] == i) : String.format("i=%d map[i]=%d", i, map[i]);
            imap[map[i]] = (short)i;
         }
      }
      return imap;
   }

   /**
    * Chance the class label of each spot according to the given map
    * 
    * @param map maps found class to given class
    */
   public void remapSpots(short[] map, int nTrueClasses)
   {
      short nNextExtra = (short)nTrueClasses;
      HashMap<Short, Short> extra = new HashMap<Short, Short>();
      for(WordSpot spot : spots){
         short x = map[spot.iClass];
         if (x < 0){
            Short y = extra.get(spot.iClass);
            if (y == null){
               extra.put(spot.iClass, nNextExtra);
               spot.iClass = nNextExtra++;
            }
            else spot.iClass = y;
         }
         else spot.iClass = x;
      }
   }

   /**
    * Change the class label of each spot according to the given map
    * 
    * @param map maps true class to found class (ie, set spot.iClass = X : map[X]=spot.iClass
    */
   public void remapSpotsOld(short map[])
   {
      short[] imap = invertMap(map);
      short nNextExtra = (short)map.length;
      HashMap<Short, Short> extra = new HashMap<Short, Short>();
      for(WordSpot spot : spots){
         if (spot.iClass >= imap.length || imap[spot.iClass] < 0){
            Short x = extra.get(spot.iClass);
            if (x == null){
               extra.put(spot.iClass, nNextExtra);
               spot.iClass = nNextExtra;
               nNextExtra++;
            }
            else spot.iClass = x;
         }
         else spot.iClass = imap[spot.iClass];
      }
   }

   /**
    * Load cri data into this object.
    * 
    * @param f the file from which to read the data
    * @return true if successful
    */
   public static ContRecInfo load(File f)
   {
      try{
         ContRecInfo cri = new ContRecInfo();
         BufferedReader in = new BufferedReader(new FileReader(f));
         String line;
         Pattern pat = Pattern.compile("^\\s*(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s*$");
         Matcher m;

         m = pat.matcher(in.readLine());
         if (!m.matches()) return null;
         assert (m.groupCount() == 4);
         cri.nSpotWords = Integer.parseInt(m.group(1));
         cri.nSpotFrames = Integer.parseInt(m.group(2));
         cri.nLabeledWords = Integer.parseInt(m.group(3));
         cri.nLabeledFrames = Integer.parseInt(m.group(4));

         m = pat.matcher(in.readLine());
         if (!m.matches()) return null;
         assert (m.groupCount() == 4);
         cri.nWordCorrect = Integer.parseInt(m.group(1));
         cri.nWordInsertions = Integer.parseInt(m.group(2));
         cri.nWordDeletions = Integer.parseInt(m.group(3));
         cri.nWordSubstitutions = Integer.parseInt(m.group(4));

         m = pat.matcher(in.readLine());
         if (!m.matches()) return null;
         assert (m.groupCount() == 4);
         cri.nFrameCorrect = Integer.parseInt(m.group(1));
         cri.nFrameInsertions = Integer.parseInt(m.group(2));
         cri.nFrameDeletions = Integer.parseInt(m.group(3));
         cri.nFrameSubstitutions = Integer.parseInt(m.group(4));

         cri.spots = new ArrayList(cri.nSpotWords);
         for(int i = 0; i < cri.nSpotWords; i++){
            line = in.readLine();
            if (line == null){
               System.err.printf(
                     "Error: expecting %d word spots in file, but failed to read line for #%d.\n",
                     cri.nSpotWords, i + 1);
               return null;
            }
            WordSpot spot = new WordSpot();
            if (spot.fromText(line) < 0){
               System.err.printf("Error: failed to parse word spot %d / %d\n", i + 1, cri.nSpotWords);
               return null;
            }
            cri.spots.add(spot);
         }

         // try to read class stats section, but it may be missing
         line = in.readLine();
         try{
            int nClasses = Integer.parseInt(line);
            cri.classStats = new int[nClasses][4];
            for(int i = 0; i < nClasses; i++){
               line = in.readLine();
               StringTokenizer st = new StringTokenizer(line, " \t\r\n");
               for(int j = 0; j < 4; j++)
                  cri.classStats[i][j] = Integer.parseInt(st.nextToken());
            }
         } catch (Exception e){
            cri.classStats = null;
         }

         in.close();
         return cri;
      } catch (IOException e){
         return null;
      }
   }
}
