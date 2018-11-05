package kdm.io;

import java.util.*;
import java.io.*;
import kdm.data.*;
import kdm.data.transform.*;
import kdm.io.DataLoader.*;
import kdm.io.Def.*;
import kdm.util.*;

/**
 * Load labeled data (subsequences) from a data def file
 */
public class LabeledDataLoader
{
   /** this list (and others) will be filled in during loading */
   public static ArrayList<Sequence> tseries;
   public static ArrayList<DiscreteSeq> qseries;
   public static ArrayList<String> sentences;
   public static ArrayList<MarkupSet> marks;

   /** set all static variables to null */
   public static void reset()
   {
      tseries = null;
      qseries = null;
      sentences = null;
      marks = null;
   }

   /**
    * Load all of the labeled subsequences found in the given data def file
    * 
    * @param file name of file to load
    * @return tree mapping class name to list of example sequences
    */
   public static TreeMap<String, ArrayList<Sequence>> load(File file)
   {
      return load(file, null);
   }

   /** load the labels from the data def file */
   public static ArrayList<MarkupSet> loadLabels(File file)
   {
      // load the data def file, but not the actual data
      DataDefLoader ddl = new DataDefLoader();
      ddl.setLoadData(false);
      if (!ddl.load(file, null)){ // TODO option for repmap
         System.err.println("Error: failed to load data definition file");
         return null;
      }
      return ddl.collectLabels(false);
   }

   /**
    * Load all of the labeled subsequences found in the given data def file
    * 
    * @param file name of file to load
    * @param repmap replacement map for file
    * @return tree mapping class name to list of example sequences
    */
   public static TreeMap<String, ArrayList<Sequence>> load(File file, AbstractMap<String, String> repmap)
   {
      reset();

      // load the data def file
      DataDefLoader ddl = new DataDefLoader();
      if (!ddl.load(file, repmap)){
         System.err.println("Error: failed to load data definition file");
         return null;
      }

      tseries = ddl.collectData();
      marks = ddl.collectLabels(true);
      if (marks == null || marks.isEmpty()){
         System.err.println("Warning: can't extract labeled data with empty label list");
         return null;
      }

      // extract sentences
      sentences = new ArrayList<String>();
      for(MarkupSet ms : marks){
         if (ms == null) continue;
         sentences.add(ms.getSentence());
      }

      // now we can extract the labeled occurrences
      TreeMap<String, ArrayList<Sequence>> data = new TreeMap<String, ArrayList<Sequence>>();
      for(int iSeq = 0; iSeq < tseries.size(); iSeq++){
         if (marks.size() <= iSeq) continue;
         Sequence seq = tseries.get(iSeq);
         MarkupSet mark = marks.get(iSeq);
         if (mark == null) continue;
         for(int iMark = 0; iMark < mark.size(); iMark++){
            TimeMarker tm = mark.get(iMark);
            int iStart, iStop;
            if (tm.isIndex()){
               iStart = tm.getStartIndex();
               iStop = tm.getStopIndex();
            }
            else{
               iStart = seq.getClosestIndex(tm.getStartTime());
               iStop = seq.getClosestIndex(tm.getStopTime());
            }

            if (iStart == iStop) // TODO
            {
               System.err.printf("iSeq: %d  iMark: %d\n", iSeq, iMark);
               System.err.printf("iStart: %d  iStop: %d\n", iStart, iStop);
               System.err.printf("tm: %s\n", tm);
            }

            Sequence occ = seq.subseq(iStart, iStop, iSeq);
            assert (occ != null);

            ArrayList<Sequence> list = data.get(tm.getTag());
            if (list == null){
               // no occurrences of this class yet, so create a new list
               list = new ArrayList<Sequence>();

               // and add it to the data tree
               data.put(tm.getTag(), list);
            }
            list.add(occ);
         }
      }

      return data;
   }

   /**
    * Load all of the labeled subsequences found in the given data def file
    * 
    * @param file name of file to load
    * @param labData labeled continuous series to use for locations if not in the file (null for none)
    * @return tree mapping class name to list of example (discrete) sequences
    */
   public static TreeMap<String, ArrayList<DiscreteSeq>> loadDiscrete(File file,
         TreeMap<String, ArrayList<Sequence>> labData)
   {
      return loadDiscrete(file, labData, null);
   }

   /**
    * Load all of the labeled subsequences found in the given data def file
    * 
    * @param file name of file to load
    * @param labData labeled continuous series to use for locations if not in the file (null for none)
    * @param repmap replacement map for file
    * @return tree mapping class name to list of example (discrete) sequences
    */
   public static TreeMap<String, ArrayList<DiscreteSeq>> loadDiscrete(File file,
         TreeMap<String, ArrayList<Sequence>> labData, AbstractMap<String, String> repmap)
   {
      // load the raw sequences labeled subsequences
      TreeMap<String, ArrayList<Sequence>> data = load(file, repmap);
      if (data == null) return null;

      // convert series from Seqs to DiscSeqs
      qseries = new ArrayList<DiscreteSeq>();
      for(int i = 0; i < tseries.size(); i++)
         qseries.add(new DiscreteSeq(tseries.get(i), -1));

      // figure out alphabet size
      int nQuant = -1;
      for(int i = 0; i < tseries.size(); i++){
         int nq = qseries.get(i).recalcSymbols();
         if (nq > nQuant) nQuant = nq;
      }

      // update alphabet size in each dseq
      for(int i = 0; i < tseries.size(); i++)
         qseries.get(i).setNumSymbols(nQuant);

      // can't create quant label subseqs without raw data
      if (data == null && labData == null){
         System.err.println("LabeledDataLoader) Error: both data and labdata are null");
         return null;
      }

      // convert Sequences to DiscreteSequences
      TreeMap<String, ArrayList<DiscreteSeq>> qdata = new TreeMap<String, ArrayList<DiscreteSeq>>();
      if (data != null){
         // we have labels from the data def file, so use them
         Iterator<String> it = data.keySet().iterator();
         while(it.hasNext()){
            String label = it.next();
            ArrayList<Sequence> list = data.get(label);
            assert (list != null);
            ArrayList<DiscreteSeq> dlist = new ArrayList<DiscreteSeq>();
            qdata.put(label, dlist);
            for(int i = 0; i < list.size(); i++){
               DiscreteSeq dseq = new DiscreteSeq(list.get(i), -1);
               dlist.add(dseq);
            }
         }

         // set the proper alphabet size for each sequence
         it = qdata.keySet().iterator();
         while(it.hasNext()){
            ArrayList<DiscreteSeq> list = qdata.get(it.next());
            for(int i = 0; i < list.size(); i++)
               list.get(i).setNumSymbols(nQuant);
         }
      }
      else{
         // no labels in the file, so extract them from the continuous data
         Iterator<String> it = labData.keySet().iterator();
         while(it.hasNext()){
            String label = it.next();
            ArrayList<Sequence> list = labData.get(label);
            assert (list != null);
            ArrayList<DiscreteSeq> dlist = new ArrayList<DiscreteSeq>();
            qdata.put(label, dlist);
            for(int i = 0; i < list.size(); i++){
               Sequence seq = list.get(i);
               WindowLocation wloc = seq.getWindowLoc();
               DiscreteSeq dseq = qseries.get(wloc.iSeries);
               dlist.add(dseq.subseq(wloc.iStart, wloc.end()));
            }
         }
      }

      return qdata;
   }

   /** @return dimensionality of the first sequence found */
   public static int getNumDims(TreeMap<String, ArrayList<Sequence>> data)
   {
      if (data.isEmpty()) return 0;
      Iterator<ArrayList<Sequence>> it = data.values().iterator();
      while(it.hasNext()){
         ArrayList<Sequence> list = it.next();
         if (list.isEmpty()) continue;
         return list.get(0).getNumDims();
      }
      return 0;
   }

   /**
    * @return total number of examples across all classes
    */
   public static int getNumEx(TreeMap<String, ArrayList<Sequence>> data)
   {
      int n = 0;
      Set<String> labels = data.keySet();
      Iterator<String> it = labels.iterator();
      while(it.hasNext())
         n += data.get(it.next()).size();
      return n;
   }

   /**
    * Dump some info about the given data
    */
   public static void dumpDataSummary(TreeMap<String, ArrayList<Sequence>> data)
   {
      if (data == null) return;
      System.err.println();
      System.err.printf("Data Summary (%d classes, %d examples, %dD):\n", data.size(), getNumEx(data),
            getNumDims(data));
      System.err.println("--------------------------------------------");
      if (data.size() == 0){
         System.err.println(" No Labeled Data!");
         System.err.println();
         return;
      }
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

   public static void main(String args[])
   {
      if (args.length != 1){
         System.err.println("USAGE: java kdm.io.LabeledDataLoader <data def file>");
         System.exit(1);
      }
      TreeMap<String, ArrayList<Sequence>> data = load(new File(args[0]));
      System.err.printf("# series: %d\n", tseries.size());
      long msTotal = 0;
      for(int i = 0; i < tseries.size(); i++){
         Sequence seq = tseries.get(i);
         System.err.printf("%02d) %d frames, %dD (%s)  %s\n", i + 1, seq.length(), seq.getNumDims(), Library
               .formatDuration(seq.getLengthMS()), seq.getName());
         msTotal += seq.getLengthMS();
      }
      System.err.printf("Data total length: %s\n", Library.formatDuration(msTotal));
      dumpDataSummary(data);
   }
}
