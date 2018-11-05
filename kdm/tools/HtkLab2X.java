package kdm.tools;

import java.io.*;
import java.util.*;

import kdm.data.*;
import kdm.util.*;
import kdm.io.*;
import gnu.getopt.*;

/**
 * Converts a directory of .lab files into a .mlf file or a sentence file
 */
public class HtkLab2X
{
   public static boolean bVerbose = false;

   /**
    * Usage info for this tool
    */
   public static void usage()
   {
      System.err.println("USAGE: java kdm.tools.HtkLab2Mlf [options] <input dir> <output mlf file>");
      System.err.println();
      System.err.println(" Options:");
      System.err.println("  -v                     Output verbose information");
      System.err.println("  -?/help                Print this message");
      System.err.println("  -mlf <output file>     Specify .mlf output file");
      System.err.println("  -sent <output file>    Specify sentence output file");
      System.err.println();
   }
   
   /**
    * Generate a .mlf file from the markup set
    */
   protected static boolean genMlf(MarkupSet[] msets, String sPath)
   {      
      try{
         PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(sPath)));
         out.println("#!MLF!#");
         for(MarkupSet marks : msets)
         {
            out.printf("\"%s\"\n", marks.getName());
            for(TimeMarker tm : marks.getList())
               out.printf("%d %d %s\n", tm.getStart(), tm.getStop(), tm.getTag());
            out.println(".");
         }
         out.close();
         return true;
      }
      catch(IOException e){
         System.err.printf("Error: unable to open output .mlf file for writing\n (%s)\n", sPath);
         return false;
      }   
   }

   /**
    * Generate a sentence file from the markup set
    */
   protected static boolean genSent(MarkupSet[] msets, String sPath)
   {
      try{
         PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(sPath)));
         for(MarkupSet marks : msets)
            out.println(marks.getSentence());
         out.close();
         return true;
      }
      catch(IOException e){
         System.err.printf("Error: unable to open output sentence file for writing\n (%s)\n", sPath);
         return false;
      }
   }
   
   /**
    * Main entry point of program
    */
   public static void main(String args[])
   {
      String sMlf = null;
      String sSent = null;
      int c, iw;
      LongOpt[] longopts = new LongOpt[] {
            new LongOpt("help", LongOpt.NO_ARGUMENT, null, 1001),
            new LongOpt("v", LongOpt.NO_ARGUMENT, null, 1002),
            new LongOpt("mlf", LongOpt.REQUIRED_ARGUMENT, null, 1003),
            new LongOpt("sent", LongOpt.REQUIRED_ARGUMENT, null, 1004) };

      Getopt g = new Getopt("HtkLab2Mlf", args, "?", longopts, true);
      while((c = g.getopt()) != -1)
      {
         switch(c){
         case '?':
         case 1001: // help
            usage();
            System.exit(0);
            break;
         case 1002: // verbose
            bVerbose = true;
            break;
         case 1003: // mlf
            sMlf = g.getOptarg();
            break;
         case 1004: // sent
            sSent = g.getOptarg();
            break;
         }
      }
      
      // make sure we have the necessary params
      if (args.length - g.getOptind() != 1)
      {
         System.err.println("Error: expecting <input dir> on command line");
         System.exit(1);
      }
      String sInDir = args[args.length-1];
      
      // make sure we have something to do
      if (sMlf==null && sSent==null)
      {
         System.err.println("Error: nothing to do (use -mlf and/or -sent options)");
         System.exit(1);
      }
      
      // read input dir 
      File fDir = new File(sInDir);
      if (!fDir.exists() || !fDir.isDirectory())
      {
         System.err.printf("Error: input directory does not exist (or isn't a diretory)\n (%s)\n", sInDir);
         System.exit(1);
      }
      File[] labs = fDir.listFiles(new WildFilenameFilter("*.lab"));
      if (labs==null || labs.length == 0)
      {
         System.err.printf("Error: no .lab files found in input directory\n (%s)\n", sInDir);
         System.exit(1);
      }
      if (bVerbose) System.err.printf("Found %d .lab files in input dir (%s)\n", labs.length, sInDir);
      
      // load the .lab files
      MLHtkLab labLoader = new MLHtkLab();
      MarkupSet[] msets = new MarkupSet[labs.length];
      for(int i=0; i<labs.length; i++)
      {
         msets[i] = labLoader.load(labs[i].getAbsolutePath());
         if (msets[i] == null)
         {
            System.err.printf("Error: unable to load .lab file (%s)\n", labs[i].getAbsolutePath());
            System.exit(1);
         }
      }
      
      if (sMlf!=null) genMlf(msets, sMlf);
      if (sSent!=null) genSent(msets, sSent);            
   }
}
