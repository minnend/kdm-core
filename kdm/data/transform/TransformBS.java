package kdm.data.transform;

import kdm.data.*;
import kdm.io.DataLoader.DLRaw;
import kdm.io.DataSaver.DSRaw;
import kdm.io.Def.DataDefLoader;

import java.io.File;
import java.util.*;

import no.uib.cipr.matrix.NotConvergedException;

import org.apache.commons.math.stat.*;

/**
 * Converts a 1D signal into "Behavior Symbols" according to the method in Tanka, Iwamoto, and Uehara 2005.
 * The method takes in 1D contiuous data (if the raw data is multidimensional, then PCA is used to get down to
 * 1D). SAX is used to do an initial discretization. In the original paper, 4 PAA segments were used. The
 * alphabet size was not specified; examples use 3 symbols, but the standard for the literature is 5. Finally,
 * the SAX strings are hashed and each one is assigned a unique BS.
 */
public class TransformBS extends DataTransform
{
   protected int nPaaSegments = 4;
   protected int nSaxSymbols = 5;  
   protected int nSaxWindow;
   protected int nextSymbol = 0;
   protected HashMap<String, Integer> hashmap;
   
   public TransformBS(int _nSaxWindow)
   {
      nSaxWindow = _nSaxWindow;
      hashmap = new HashMap<String, Integer>();
   }
   
   public TransformBS(int _nSaxWindow, int _nPaaSymbols, int _nSaxSymbols)
   {
      nSaxWindow = _nSaxWindow;
      nPaaSegments = _nPaaSymbols;
      nSaxSymbols = _nSaxSymbols;      
      hashmap = new HashMap<String, Integer>();
   }
   
   public void dumpParams()
   {
      System.err.printf("%s:\n", getClass());
      System.err.printf(" Window Length: %d\n", nSaxWindow);
      System.err.printf(" # PAA Segments: %d\n", nPaaSegments);
      System.err.printf(" # SAX Symbols: %d\n", nSaxSymbols);      
   }
   
   /** Reset the table that maps SAX strings to BS symbols */
   public void reset()
   {
      hashmap.clear();  
      nextSymbol = 0;
   }
   
   protected int hash(String s)
   {
      Integer x = hashmap.get(s);
      if (x == null)
      {
         hashmap.put(s, nextSymbol);
         return nextSymbol++;
      }
      return x;
   }
   
   /** @return number of unique symbols needed so far */
   public final int getNumSymbols(){ return hashmap.size(); }
   
   @Override
   public Sequence transform(Sequence data)
   {
      if (data.getNumDims() != 1)
      {
         System.err.println("Error: TransformBS requires 1D data (try using PCA to reduce)");
         return null;
      }
      
      DiscreteSeq seq = new DiscreteSeq("BS: " + data, data.getFreq(), 0, data.getStartMS());
      double[] raw = data.extractDim(0);
      double[] paa = new double[nPaaSegments];
      int T = data.length();
      for(int t=0; t+nSaxWindow<=T; t++)
      {
         // calc PAA
         SAX.genPAA(raw, t, nSaxWindow, paa); 
            
         // generate the string for this window
         StringBuffer sb = new StringBuffer();
         for(int i=0; i<nPaaSegments; i++)
         {
            char c = SAX.raw2sym(paa[i], nSaxSymbols);
            sb.append(c);
         }
         
         // get the bs symbol
         int bs = hash(sb.toString());
         seq.add(new FeatureVec(1, bs));
      }
      seq.setNumSymbols(getNumSymbols());
      return seq;
   }

   /**
    * Sample application for simple PCA tranformations
    */
   public static void main(String args[]) throws Exception
   {
      if (args.length!=5)
      {
         System.err.println("USAGE: java ~.TransformBS <data def file> <W> <P> <S> <save name>");         
         System.err.println("  W = SAX window length");
         System.err.println("  P = num PAA segments");
         System.err.println("  S = num SAX symbols");
         System.err.println("  save name should have a %d format to number the output files");
         System.exit(1);
      }
            
      String sFile = args[0];
      int W = Integer.parseInt(args[1]);
      int P = Integer.parseInt(args[2]);
      int S = Integer.parseInt(args[3]);
      String sOut = args[4];     
      TransformBS bs = new TransformBS(W, P, S);
            
      if (sFile.endsWith(".def"))
      {
         ArrayList<Sequence> seqs = DataDefLoader.loadSeqs(new File(sFile), null);
         DSRaw saver = new DSRaw();
         for(int iSeq=0; iSeq<seqs.size(); iSeq++)
         {
            Sequence seq = seqs.get(iSeq);
            Sequence seqbs = bs.transform(seq);
            String sFileOut = String.format(sOut, iSeq+1);
            System.err.printf("Saving file: %s\n", sFileOut);
            saver.save(seqbs, sFileOut);
         }
         System.err.printf("Total # of behavior symbols: %d\n", bs.getNumSymbols());
      }
      else{
         assert false : "support for direct loading not implemented yet";
      }
   }
}
