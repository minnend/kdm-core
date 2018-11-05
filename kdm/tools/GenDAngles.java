package kdm.tools;

import kdm.data.*;
import kdm.data.transform.*;
import kdm.io.*;
import kdm.io.DataLoader.*;
import kdm.io.DataSaver.*;
import kdm.io.Def.DataDefLoader;
import kdm.models.*;
import kdm.util.*;
import java.util.*;
import java.util.regex.Pattern;

import gnu.getopt.*;
import java.io.*;
import jnt.FFT.*;

/** generate dt angle data from a sequence or data def file */
public class GenDAngles
{
   public static void usage()
   {
      // TODO: -loader and -saver option for specifying a loader/saver class
      System.err.println("USAGE: java ~GenDAngles [options] <input file> <output file>");
      System.err.println();
      System.err.println(" You can format the output files using %d (eg. output_file = data%02d.txt)");
      System.err.println("  -quant <n>               generate quantized data as well");
      System.err.println();
   }

   /**
    * Load the data, transform it, and write it out to a new file
    * 
    * @param sFileIn input file name
    * @param sFileOut output file name
    * @return true if successful
    */
   public static boolean process(String sFileIn, String sFileOut, int nBins)
   {
      System.err.print("Loading data... ");
      TimerMS timer = new TimerMS();
      ArrayList<Sequence> tseries;
      if (sFileIn.endsWith(".def"))
      {
         tseries = DataDefLoader.loadSeqs(new File(sFileIn), null);
         if (tseries == null)
         {
            System.err.println("Error: failed to load data def file:\n " + sFileIn);
            return false;
         }
      }
      else
      {
         Sequence seq = new DLRaw().load(sFileIn);
         if (seq == null)
         {
            System.err.println("Error: failed to load data file:\n " + sFileIn);
            return false;
         }
         tseries = new ArrayList<Sequence>();
         tseries.add(seq);
      }
      System.err.printf("done (%dms).\n", timer.time());

      // concat all data
      Sequence seqAll = new Sequence(tseries.get(0));
      if (tseries.size() > 1)
      {
         System.err.print("Concatenating data... ");
         timer.reset();         
         for(int i = 1; i < tseries.size(); i++)
            seqAll.append(tseries.get(i), true, false);
         System.err.printf("done (%dms).\n", timer.time());
      }

      // calc dAngles
      System.err.print("Calculating optimal (max entrpoy) time scales... ");
      timer.reset();
      double dt[] = TransformDAngles.calcTimeScale(seqAll, Math.abs(nBins), TransformDAngles.TSMETHOD.MaxEntropy);      
      System.err.printf("done (%dms).\n", timer.time());
      System.err.print("Calculating optimal (average diff) time scales... ");
      timer.reset();
      double dt2[] = TransformDAngles.calcTimeScale(seqAll, Math.abs(nBins), TransformDAngles.TSMETHOD.Average);      
      System.err.printf("done (%dms).\n", timer.time());      
      System.err.print("MaxEnt: ");
      for(int i = 0; i < dt.length; i++)
         System.err.printf("%.3f ", dt[i]);
      System.err.println();
      System.err.print("  Mean: ");
      for(int i = 0; i < dt2.length; i++)
         System.err.printf("%.3f ", dt2[i]);
      System.err.println();

      // transform the data
      System.err.print("Transforming data... ");
      timer.reset();
      ArrayList<Sequence> seqAng = new ArrayList<Sequence>();
      TransformDAngles tran = new TransformDAngles(dt);
      for(Sequence seq : tseries)
      {
         Sequence seqTran = tran.transform(seq);
         seqAng.add(seqTran);
      }
      System.err.printf("done (%dms).\n", timer.time());

      // save the transformed sequences
      System.err.print("Saving data... ");
      timer.reset();
      DataSaver saver = new DSRaw();
      String sRex = "^.*\\%0?\\d*d.*$";
      boolean bFormat = Pattern.matches(sRex, sFileOut);
      for(int i = 0; i < seqAng.size(); i++)
      {
         String s = sFileOut;
         if (bFormat) s = String.format(sFileOut, i + 1);
         if (!saver.save(seqAng.get(i), s))
         {
            System.err.println("Error: failed to save data file:\n " + s);
            return false;
         }
      }
      System.err.printf("done (%dms).\n", timer.time());

      if (nBins > 0)
      {
         final double PiOver2 = Math.PI / 2;
         System.err.print("Generating quantized data...\n");
         timer.reset();
         double edges[] = Histogram.genEdges(-PiOver2, PiOver2, nBins);
         ArrayList<DiscreteSeq> qseqs = new ArrayList<DiscreteSeq>();
         HashSet<Integer> uniq = new HashSet<Integer>(); 
         for(Sequence seq : seqAng)
         {
            DiscreteSeq dseq = new DiscreteSeq("Discrete: "+seq.getName(), seq.getFreq(), nBins, seq.getStartMS());
            int T = seq.length();
            int D = seq.getNumDims();
            for(int t=0; t<T; t++)
            {
               FeatureVec fv = new FeatureVec(D);
               for(int d=0; d<D; d++)
               {
                  double x = seq.get(t,d);
                  assert(x>=-PiOver2 && x<=PiOver2) : String.format("x=%.4f", x);
                  int iBin = Histogram.bin(x, edges);
                  assert(iBin>=0 && iBin<nBins) : String.format("iBin=%d", iBin);
                  uniq.add(iBin);
                  fv.set(d, iBin);
               }
               dseq.add(fv);
            }
            qseqs.add(dseq);
         }
         System.err.printf("done (%d unique, %dms).\n", uniq.size(), timer.time());

         System.err.print("Saving quantized data...");
         timer.reset();
         for(int i = 0; i < qseqs.size(); i++)
         {
            String s = sFileOut;
            if (bFormat) s = String.format(sFileOut, i + 1);
            String sExt = Library.getExt(s);
            s = Library.getPath(s)+Library.getTitle(s)+"q"+(sExt.length()>0 ? "."+sExt : "");
            if (!saver.save(qseqs.get(i), s))
            {
               System.err.println("Error: failed to save data file:\n " + s);
               return false;
            }
         }
         System.err.printf("done (%dms).\n", timer.time());
      }
      return true;
   }

   public static void main(String args[])
   {
      int nBins = -7;

      int c, iw;
      LongOpt[] longopts = new LongOpt[] { new LongOpt("quant", LongOpt.REQUIRED_ARGUMENT, null, 1001), };

      Getopt g = new Getopt("Transform", args, "?", longopts, true);
      while((c = g.getopt()) != -1)
      {
         String sArg = g.getOptarg();
         switch(c){
         case '?':
            usage();
            System.exit(0);
            break;
         case 1001: // nbins
            nBins = Integer.parseInt(sArg);
            break;
         }
      }

      // add file patterns to param object
      if ((args.length - g.getOptind()) != 2)
      {
         usage();
         System.exit(1);
      }
      String sFileIn = args[g.getOptind()];
      String sFileOut = args[g.getOptind() + 1];

      process(sFileIn, sFileOut, nBins);
   }

}
