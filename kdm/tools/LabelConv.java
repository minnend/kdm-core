package kdm.tools;

import java.util.StringTokenizer;

import kdm.io.*;
import kdm.data.*;
import gnu.getopt.*;

/** convert label files between formats (time vs. index) */
public class LabelConv
{

   public static void usage()
   {
      System.err.println();
      System.err.println("USAGE: java ~.LabelConv [Options] <in file> <out file>");
      System.err.println(" -step     step size (ms) (def=1)");      
      System.err.println(" -start    start time (ms) (def=0)");      
      System.err.println();
   }

   public static void main(String args[])
   {
      long step=1,start=0;
      
      int c;
      LongOpt[] longopts = new LongOpt[] { new LongOpt("help", LongOpt.NO_ARGUMENT, null, 1001),
            new LongOpt("step", LongOpt.REQUIRED_ARGUMENT, null, 1002),
            new LongOpt("start", LongOpt.REQUIRED_ARGUMENT, null, 1003),
            };

      Getopt g = new Getopt("TSView", args, "?", longopts, true);
      while((c = g.getopt()) != -1){
         String sArg = g.getOptarg();
         switch(c){
         case '?':
         case 1001: // help
            usage();
            System.exit(0);
            break;
         case 1002: // step
            step = Long.parseLong(sArg);
            break;
         case 1003: // start
            start = Long.parseLong(sArg);
            break;            
         }
      }

      // make sure that a view file was specified
      if (g.getOptind()+2 != args.length){
         System.err.println("Error: expecting input and output file");
         System.err.println();
         System.exit(1);
      }
      
      String sInFile = args[g.getOptind()];
      String sOutFile = args[g.getOptind()+1];
      System.err.printf("Processing label file: %s -> %s\n", sInFile, sOutFile);
      MLGeneral loader = new MLGeneral();
      MarkupSet marks = loader.load(sInFile);
      if (marks == null){
         System.err.printf("Error: failed to load labels.\n");
         System.exit(1);
      }
      
      // do the conversion
      MarkupSet marks2 = new MarkupSet();
      int nMarks = marks.size();
      for(int i=0; i<nMarks; i++)
      {
         TimeMarker tm = marks.get(i);
         if (tm.getUnits() == TimeMarker.Units.Time){
            long a = tm.getStartTime();
            long b = tm.getStopTime();
            tm.setUnits(TimeMarker.Units.Index);
            a = (a-start) / step;
            b = (b-start) / step;            
            tm.set(a, b);
         }         
         marks2.add(tm);
      }
      
      MSGeneral saver = new MSGeneral();
      if (!saver.save(marks2, sOutFile)){
         System.err.printf("Error: failed to save adjusted labels.\n");
         System.exit(1);
      }
   }
}
