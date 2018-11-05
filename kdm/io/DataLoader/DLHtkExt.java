package kdm.io.DataLoader;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

import kdm.data.*;
import kdm.util.*;

/**
 * Loads data from a HTK .ext file. You can also use this as a utility class to dump info
 * about a .ext file to stdout.
 */
public class DLHtkExt extends DataLoader
{
   protected boolean bEnergy, bSuppAbsEnergy, bDelta, bAcc, bComp, bZeroMean, bCRC, bZeroCC;
   
   @Override
   public Sequence load(String path)
   {
      try
      {
         File file = new File(path);
         FileChannel fc = new FileInputStream(file).getChannel();

         // Get the file's size and then map it into memory
         int sz = (int)fc.size();
         MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);
         bb.order(ByteOrder.BIG_ENDIAN);

         // read the header
         int nSamples = bb.getInt(); // # of samples in file
         int sampPeriod = bb.getInt(); // sample period in 100ns units
         short sampSize = bb.getShort(); // # bytes per sample
         short parmFull = bb.getShort(); // code indicating sample kind (9=user)
         
         short parmKind = (short)(parmFull & 63); // higher bits are indicators
         
         if (parmKind == 6)
         {
            bEnergy = (parmFull & 64)!=0;
            bSuppAbsEnergy = (parmFull & 128)!=0;
            bDelta = (parmFull & 256)!=0;
            bAcc = (parmFull & 512)!=0;
            bComp = (parmFull & 1024)!=0;
            bZeroMean = (parmFull & 2048)!=0;
            bCRC = (parmFull & 4096)!=0;
            bZeroCC = (parmFull & 8192)!=0;
         }
         else{
            bEnergy=bSuppAbsEnergy=bDelta=bAcc=bComp=bZeroMean=bCRC=bZeroCC=false;            
         }
         
         // check for various kinds of errors or things we don't handle
         if (parmKind != 9 && parmKind!=6)
         {
            System.err.printf("Error: only HTK .ext files with MFCC or user-defined data are supported (parmKind=%d).\n", parmKind);
            return null;
         }

         if ((sampSize % 4) != 0)
         {
            System.err.printf("Error: sample size not divisble by 4 => not a 32-bit real feature vector.\n");
            return null;
         }

         if (sz != (12 + sampSize * nSamples))
         {
            System.err.printf("Error: file size (%d) != header info (%d)\n", sz, 12 + sampSize * nSamples);
            return null;
         }

         // create the sequence and load the data
         int nd = sampSize / 4;
         double freq;
         if (parmKind==9) freq = 10000.0 / sampPeriod; // should be 10,000,000, but we treat ns as us
         else freq = sampPeriod * 1e-7;
         Sequence seq = new Sequence(Library.getFileName(path), freq);

         for(int i = 0; i < nSamples; i++)
         {
            FeatureVec fv = new FeatureVec(nd);
            for(int d = 0; d < nd; d++)
               fv.set(d, bb.getFloat());
            seq.add(fv);
         }

         //System.err.printf("nSamples: %d (%d)  length: %dms\n", nSamples, seq.length(), seq.getLengthMS());

         // Close the channel and the stream
         fc.close();
         return seq;
      } catch (Exception e)
      {
         System.err.println(e);
         return null;
      }
   }

   public static void main(String args[])
   {
      if (args.length != 1)
      {
         System.err.println("USAGE: java kdm.io.DLHtkExt <ext file>");
         return;
      }
      DLHtkExt htk = new DLHtkExt();
      System.err.printf("Loading file: %s\n", args[0]);
      Sequence seq = htk.load(args[0]);
      if (seq == null){
         System.err.println("Error: failed to load HTK data file.");
         return;
      }
      System.err.printf(" length=%d  #dims=%d  freq=%.4fHz (per=%.2f)\n", seq.length(), seq.getNumDims(),
            seq.getFreq(), seq.getPeriod());
      int nd = seq.getNumDims();
      for(int i = 0; i < seq.length(); i++)
         System.err.printf("%03d) %s\n", i, seq.get(i));
   }

}
