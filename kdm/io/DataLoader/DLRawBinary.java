package kdm.io.DataLoader;

import kdm.util.*;
import kdm.io.*;
import kdm.data.*;
import java.io.*;

/** Loads a sequence from a raw binary file */
public class DLRawBinary extends DataLoader
{

   @Override
   public Sequence load(String path)
   {
      Sequence[] seqs = BinaryData.load(new File(path));
      if (seqs==null || seqs.length==0) return null;
      return seqs[0];
   }
   
   public static void main(String args[])
   {
      DLRawBinary loader = new DLRawBinary();
      System.err.print("Loading sequence...");
      TimerMS timer = new TimerMS();
      Sequence seq = loader.load(args[0]);
      System.err.printf(" done (%dms).\n", timer.time());
      System.err.printf("seq: %s\n", seq);
   }

}
