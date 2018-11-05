package kdm.tools;

import java.io.*;
import java.util.*;

import kdm.data.*;
import kdm.util.*;
import kdm.io.*;
import gnu.getopt.*;

/**
 * Take a HTK result file and list of sentences and generate a set of label files.
 */
public class HtkGenLabels
{
   public static boolean bVerbose = false;

   /**
    * Usage info for this tool
    */
   public static void usage()
   {
      System.err.println("USAGE: java kdm.tools.HtkGenLabels [options] <results file> <sentences>");
      System.err.println();
      System.err.println(" Options:");
      System.err.println("  -out <dir>             Send output label files to this directory");
      System.err.println("  -scale <scale>         Integer value to divide HTK times by (def=2000)\n");
      System.err.println("  -v                     Output verbose information");
      System.err.println("  -?/help                Print this message");
      System.err.println();
   }
   
   /**
    * Load the sentences from a file
    */
   public static AList2<String> loadSentences(String sFile)
   {
      AList2 sent = new AList2();
      try{
         BufferedReader in = new BufferedReader(new FileReader(sFile));
         String line;
         while((line = Library.readLine(in))!=null)
         {
            StringTokenizer st = new StringTokenizer(line, " \t\r\n,;");
            ArrayList<String> a = new ArrayList<String>();
            while(st.hasMoreTokens()) a.add(st.nextToken());
            sent.add(a);
         }
         in.close();
      }
      catch(IOException e){ System.err.println(e); return null; }
      return sent;
   }
   
   /**
    * Main entry point of program
    */
   public static void main(String args[])
   {
      String sDirOut = null;
      int scale = 2000;

      int c, iw;
      LongOpt[] longopts = new LongOpt[] { new LongOpt("help", LongOpt.NO_ARGUMENT, null, 1001),
            new LongOpt("v", LongOpt.NO_ARGUMENT, null, 1002),
            new LongOpt("out", LongOpt.REQUIRED_ARGUMENT, null, 1003),
            new LongOpt("scale", LongOpt.REQUIRED_ARGUMENT, null, 1004) };

      Getopt g = new Getopt("GenLabelsHtk", args, "?", longopts, true);
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
         case 1003: // out
            sDirOut = g.getOptarg();
            break;
         case 1004: // scale
            scale = Integer.parseInt(g.getOptarg());
            break;
         }
      }
      
      // check for the input file
      int nArgs = args.length - g.getOptind();
      if (nArgs != 2)
      {
         System.err.println("\nError: expecting input files <results file> and <sentences>");
         System.exit(1);
      }
      
      // load the data
      HtkResult hres = HtkResult.load(args[args.length-2], scale);
      if (bVerbose) System.err.printf("Found %d sentences in result/MLF file.\n", hres.size());
      AList2<String> sent = loadSentences(args[args.length-1]);
      if (bVerbose) System.err.printf("Found %d sentences in sentence file.\n", sent.size());

      // setup output to a file if requested
      if (sDirOut != null)
      {
         File file = new File(sDirOut);
         if (!file.exists() || !file.isDirectory())
         {
            System.err.printf("Error: output directory does not exist (or isn't a directory):\n %s\n", sDirOut);
            System.exit(1);
         }
         sDirOut = Library.ensurePathSep(sDirOut);
      }
      else sDirOut = "./";
      
      // match labeled sentences to word boundaries
      MSGeneral msg = new MSGeneral();
      int nSaved = 0;
      Iterator<String> it = hres.iterator();
      while(it.hasNext())
      {
         String fname = it.next();
         String title = Library.getTitle(fname);
         int ix = hres.getIndex(Integer.parseInt(title));
         MarkupSet marks = hres.findMatch(fname, sent.get(ix), true);
         if (marks == null)
         {
            System.err.printf("No Match - %s - ", title);
            for(String s : sent.get(ix)) System.err.printf("%s ", s);
            System.err.println();
         }
         else{
            String sFile = String.format("%s%s.glb", sDirOut, title);
            if (!msg.save(marks, sFile))
            {
               System.err.printf("Error: failed to save label file (%s)\n", sFile);
            }
            else nSaved++;
         }
      }
      if (bVerbose) System.err.printf("Saved %d label files.\n", nSaved);

   }
   
}
