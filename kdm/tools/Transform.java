package kdm.tools;

import gnu.getopt.*;
import java.util.*;
import java.io.*;

import kdm.data.*;
import kdm.data.transform.*;
import kdm.io.DataLoader.*;
import kdm.io.DataSaver.*;
import kdm.io.Def.DataDefLoader;
import kdm.util.*;

/** Apply transformations to a data sequence */
public class Transform
{
   protected static boolean bVerbose = false;

   /**
    * Usage info for this tool
    */
   public static void usage()
   {
      // TODO: specify loader and saver
      System.err.println("USAGE: java kdm.tools.Transform [options] <input file>");
      System.err.println();
      System.err.println(" Options:");
      System.err.println("  -tran <class>          transformation to apply");
      System.err.println("  -params <params>       list of params for prev transform (key=\"val\" ...)");
      System.err.println("  -v                     output verbose information");
      System.err.println();
      System.err.println(" Note: output files will have \".tran\" extension");
      System.err.println();
   }

   /**
    * Main entry point of program
    */
   public static void main(String args[]) throws Exception
   {
      String sInput;
      ArrayList<DataTransform> trans = new ArrayList<DataTransform>();

      int c;
      LongOpt[] longopts = new LongOpt[] { new LongOpt("help", LongOpt.NO_ARGUMENT, null, 1001),
            new LongOpt("v", LongOpt.NO_ARGUMENT, null, 1002),
            new LongOpt("tran", LongOpt.REQUIRED_ARGUMENT, null, 1003),
            new LongOpt("params", LongOpt.REQUIRED_ARGUMENT, null, 1004) };

      Getopt g = new Getopt("Transform", args, "?", longopts, true);
      while((c = g.getopt()) != -1){
         String sArg = g.getOptarg();
         switch(c){
         case '?':
         case 1001: // help
            usage();
            System.exit(0);
            break;
         case 1002: // verbose
            bVerbose = true;
            break;
         case 1003: // tran
         {
            try{
               Class cls = null;
               try{
                  cls = Library.getClass(sArg, "kdm.data.transform");
               } catch (ClassNotFoundException e){
                  cls = Class.forName("kdm.data.transform.Transform" + sArg);
               }
               DataTransform tran = (DataTransform)cls.newInstance();
               trans.add(tran);
            } catch (Exception e){
               System.err.printf("Error: unable to load transformation \"%s\"\n (%s)\n", sArg, e);
               System.exit(1);
            }
         }
            break;
         case 1004: // params
         {
            if (trans.isEmpty()){
               System.err.printf("Error: parameters specified before any transformations\n");
               System.exit(1);
            }
            DataTransform tran = trans.get(trans.size() - 1);
            tran.config(new File("."), sArg);
         }
            break;
         }
      }

      // make sure we have a transformation
      if (trans.isEmpty()){
         System.err.printf("Error: no transformations (use -tran option)\n");
         System.exit(1);
      }

      // check for the input file
      int iArg = g.getOptind();
      int nArgs = args.length - iArg;
      if (nArgs < 1){
         System.err.printf("Error: no input file\n");
         System.exit(1);
      }

      ArrayList<Sequence> tseries = new ArrayList<Sequence>();
      while(iArg < args.length){
         sInput = args[iArg++];
         boolean bDefFile = false;
         String sExt = Library.getExt(sInput);
         if (sExt.equals("def")){
            ArrayList<Sequence> seqs = DataDefLoader.loadSeqs(new File(sInput), null);
            if (seqs == null){
               System.err.printf("Error: failed to load data def file\n (%s)\n", sInput);
               System.exit(1);
            }
            tseries.addAll(seqs);
         }
         else{
            DataLoader loader = new DLRaw();
            Sequence seqin = loader.load(sInput);            
            if (seqin == null){
               System.err.printf("Error: failed to load data\n (%s)\n", sInput);
               System.exit(1);
            }
         }
      }
      if (tseries.isEmpty()){
         System.err.println("Error: no sequences to transform.");
         System.exit(1);
      }
      
      if (bVerbose){
         if (tseries.size()==1) System.err.printf("Input: %s\n", tseries.get(0).getOrigFile());
         else System.err.printf("Input: %d sequences\n", tseries.size());
         System.err.printf("Transformations:\n");
         for(DataTransform tran : trans)
            tran.dumpParams();
      }

      // transform the data
      for(Sequence seqin : tseries){
         String sOutput = seqin.getOrigFile()+".tran";
         Sequence seqout = seqin;
         for(DataTransform tran : trans)
            seqout = tran.transform(seqout);
         if (bVerbose){
            System.err.printf("Input data: %d frames, %dD\n", seqin.length(), seqin.getNumDims());
            System.err.printf(" Tran data: %d frames, %dD\n", seqout.length(), seqout.getNumDims());
         }

         // save the data
         DataSaver saver = new DSRaw();
         if (!saver.save(seqout, sOutput)){
            System.err.printf("Error: failed to save tranformed data\n (%s)\n", sOutput);
            System.exit(1);
         }
      }
   }
}
