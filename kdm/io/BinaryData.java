package kdm.io;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import kdm.data.*;
import kdm.io.DataLoader.*;
import kdm.io.DataSaver.*;
import kdm.io.Def.*;
import kdm.util.*;

/**
 * Utility class to save and load binary data
 */
public class BinaryData
{
   /**
    * Save the data by writing it to file (metadata is lost)
    * 
    * @param file file to write
    * @param tseries data to save
    * @return true if successful
    */
   public static boolean save(File file, Sequence tseries)
   {
      ArrayList<Sequence> list = new ArrayList<Sequence>();
      list.add(tseries);
      return save(file, list);
   }
   
   /**
    * Save the data by writing it to file (metadata is lost)
    * 
    * @param file file to write
    * @param tseries data to save
    * @return true if successful
    */
   public static boolean save(File file, ArrayList<Sequence> tseries)
   {
      try
      {
         DataOutputStream out = new DataOutputStream(new FileOutputStream(file));
         out.writeInt(tseries.size());
         for(Sequence seq : tseries)
         {
            int T = seq.length();
            int D = seq.getNumDims();
            MyLongList dates = seq.getDates(false);
            int nDates = dates==null ? 0 : dates.size();
            out.writeInt(T);
            out.writeInt(D);
            out.writeInt(nDates);
            out.writeDouble(seq.getFreq());
            out.writeLong(seq.getStartMS()==Library.AppStartTime ? 0 : seq.getStartMS());
            out.writeInt(seq.getName()==null ? 0 : seq.getName().length());
            if (seq.getName()!=null) out.writeChars(seq.getName());
            out.writeInt(seq.getOrigFile()==null ? 0 : seq.getOrigFile().length());
            if (seq.getOrigFile()!=null) out.writeChars(seq.getOrigFile());
            out.writeInt(seq.getParentIndex());
            out.writeInt(seq.getParentOffset());
            for(int t = 0; t < T; t++)
               for(int d = 0; d < D; d++)
                  out.writeDouble(seq.get(t, d));
            for(int i=0; i<nDates; i++)
               out.writeLong(dates.get(i));
         }
         out.close();
      } catch (Exception e)
      {
         e.printStackTrace();
         return false;
      }
      return true;
   }

   /**
    * Load the sequences from the given file
    * 
    * @param file source of data
    * @return list of sequences from file
    */
   public static ArrayList<Sequence> loadList(File file)
   {
      Sequence[] tseries = load(file);
      if (tseries == null) return null;
      ArrayList<Sequence> list = new ArrayList<Sequence>(tseries.length);
      for(int i=0; i<tseries.length; i++) list.add(tseries[i]);
      return list;
   }
   
   /**
    * Load the sequences from the given file
    * 
    * @param file source of data
    * @return array of sequences from file
    */
   public static Sequence[] load(File file)
   {
      Sequence[] tseries = null;
      try
      {
         FileChannel fc = new FileInputStream(file).getChannel();

         // Get the file's size and then map it into memory
         int sz = (int)fc.size();
         MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);

         int nSeqs = bb.getInt();
         tseries = new Sequence[nSeqs];
         for(int iSeq = 0; iSeq < nSeqs; iSeq++)
         {
            int T = bb.getInt();
            int D = bb.getInt();
            int nDates = bb.getInt();
            double freq = bb.getDouble();
            long msStart = bb.getLong();            
            if (msStart == 0) msStart = Library.AppStartTime;
            int lenName = bb.getInt();
            StringBuffer sb = new StringBuffer();
            for(int i=0; i<lenName; i++) sb.append(bb.getChar());
            String sName = lenName>0 ? sb.toString() : null;
            int lenOrig = bb.getInt();
            sb = new StringBuffer();
            for(int i=0; i<lenOrig; i++) sb.append(bb.getChar());
            String sOrig = lenOrig>0 ? sb.toString() : null;
            int parIndex = bb.getInt();
            int parOffset = bb.getInt();
            
            Sequence seq = new Sequence(sName, freq, msStart);
            //if (sOrig!=null) seq.setOrigFile(sOrig); // TODO param to choose embedded orig file?
            seq.setOrigFile(Library.getCanonical(file.getAbsolutePath()));
            for(int t = 0; t < T; t++)
            {
               FeatureVec fv = new FeatureVec(D);
               for(int d = 0; d < D; d++)
                  fv.set(d, bb.getDouble());
               seq.add(fv);               
            }
            
            for(int i=0; i<nDates; i++) seq.setDate(i, bb.getLong());
            
            tseries[iSeq] = seq;
         }

         // Close the channel and the stream
         fc.close();
      } catch (Exception e)
      {
         System.err.printf("Warning: Failed to load binary data\n (%s)\n", file.getAbsolutePath());
         return null;
      }
      return tseries;
   }

   
   public static void main(String args[]) throws Exception
   {
      if (args.length<1 || args.length>2)
      {
         System.err.println();
         System.err.println("USAGE:");
         System.err.println(" ~.BinaryData <data def> <binary save file>   (create bin file)");
         System.err.println(" ~.BinaryData <binary file>                   (dump info about bin file)");
         System.err.println();
         System.exit(1);
      }
      
      if (args.length == 1)
      {
         Sequence[] seqs = load(new File(args[0]));
         if (seqs==null){
            System.err.printf("Error: unable to load binary data\n (%s)\n", args[0]);
            System.exit(1);
         }
         System.err.printf("%s: %d series\n", args[0], seqs.length);
         for(int i=0; i<seqs.length; i++)
         {
            System.err.printf(" %d) %s  %dD, %d frames\n", i+1, seqs[i].getName(), seqs[i].getNumDims(), seqs[i].length());
            System.err.printf("     [%s] -> [%s]  (%s)\n", Library.formatTime(seqs[i].getStartMS()), Library.formatTime(seqs[i].getEndMS()), Library.formatDuration(seqs[i].getLengthMS()));
         }
      }
      else{
         // load the data def file
         TimerMS timer = new TimerMS();
         DataDefLoader ddl = new DataDefLoader();
         if (!ddl.load(new File(args[0])))
         {
            System.err.println("Error: failed to load data definition file");
            System.exit(1);
         }
         System.err.printf("time to load: %dms\n", timer.time());
         ArrayList<Sequence> tseries = ddl.collectData();

         // save the data
         System.err.print("Saving binary data... ");
         timer.reset();
         save(new File(args[1]), tseries);
         System.err.printf("done (%dms).\n", timer.time());
      }
   }
}
