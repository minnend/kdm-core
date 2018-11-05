package kdm.io.DataLoader;

import kdm.data.*;
import kdm.util.*;
import java.util.*;
import java.io.*;
import java.text.*;

/**
 * Loads data from a raw file with one time step per line; each line has a timestamp in
 * either the first or last position, or the loader can auto-detect where it is (based on
 * failure to parse a double).
 */
public class DLRawTimestamp extends DataLoader
{
   public static enum TPos {
      start, end, auto
   }
   
   public static enum TimeFormat {
      ms, sec, str, auto
   }

   // TODO: specify index (neg => count from end) of time stamp
   // TODO: auto detection should consider sec/ms doubles
   
   protected String delims = " \t\r\n,";
   protected TPos tpos = TPos.auto;
   protected boolean bRemoveDups = false;
   protected boolean bForceUniform = false;
   protected long offset = 0;
   protected TimeFormat timeFormat = TimeFormat.auto; 
   protected String sDateFormat = "yyyyMMdd-HHmmss";
   
   public DLRawTimestamp()
   {}

   public DLRawTimestamp(TPos _tpos, TimeFormat _timeFormat, boolean _bRemoveDups, boolean _bForceUniform)
   {
      tpos = _tpos;
      timeFormat = _timeFormat;
      bRemoveDups = _bRemoveDups;
      bForceUniform = _bForceUniform;
   }

   public static void usage()
   {
      System.err.println();
      System.err.printf("Class: %s\n", new DLRawTimestamp().getClass());
      System.err.println("Configuration Options:");
      System.err.println(" delims         specify delimiter characters (def: \\ \\t\\r\\n)");
      System.err.println(" tpos           timestamp position: start, end, auto (def: auto)");
      System.err.println(" time-format    format of timestamp: ms, seconds, string (def: auto)");
      System.err.println(" offset-ms      ms to add to each timestamp (def: 0ms)");
      System.err.println(" remove-dups    average data with same timestamp?  (def: false)");
      System.err.println(" force-uniform  force uniform sampling according to first time and global fps (def: false)");
      System.err.println();
   }
   
   @Override
   public boolean config(ConfigHelper chelp, String sKey, String sVal)
   {
      if (Library.stricmp(sKey, "delims"))
      {
         delims = sVal;
      }
      else if (Library.stricmp(sKey, "tpos"))
      {
         if (Library.stricmp(sVal, "start") || Library.stricmp(sVal, "front")) tpos = TPos.start;
         else if (Library.stricmp(sVal, "end") || Library.stricmp(sVal, "back")) tpos = TPos.end;
         else if (Library.stricmp(sVal, "auto")) tpos = TPos.auto;
         else
         {
            System.err.printf("DLRawTimestamp Error: unknown timestamp position (\"%s\")\n", sVal);
            return false;
         }
      }
      else if (Library.stricmp(sKey, "time-format"))
      {
         if (Library.stricmp(sVal, "ms"))
         {
            timeFormat = TimeFormat.ms;
         }
         else if (Library.stricmp(sVal, "seconds"))
         {
            timeFormat = TimeFormat.sec;
         }
         else if (Library.stricmp(sVal, "auto"))
         {
            timeFormat = TimeFormat.auto;
         }
         else{
            timeFormat = TimeFormat.str;
            sDateFormat = sVal;
         }
      }
      else if (Library.stricmp(sKey, "offset-ms"))
      {
         offset = Long.parseLong(sVal);
      }
      else if (Library.stricmp(sKey, "removedups"))
      {
         bRemoveDups = ConfigHelper.isTrueString(sVal);
      }
      else if (Library.stricmp(sKey, "force-uniform"))
      {
         bForceUniform = ConfigHelper.isTrueString(sVal);
      }
      else super.config(chelp, sKey, sVal);
      return true;
   }

   /** @return ms since epoch of the given time stamp, -1 if error */
   public long parseTimestamp(String ts)
   {
      if (ts==null) return -1;
      
      if (timeFormat == TimeFormat.ms || timeFormat == TimeFormat.auto)
      {
         // could be ms
         try{
            long x = Long.parseLong(ts);
            return x + offset;
         }
         catch(NumberFormatException nfe){}
      }
      
      if (timeFormat == TimeFormat.sec || timeFormat == TimeFormat.auto)
      {
         // could be seconds
         try{
            double x = Double.parseDouble(ts) * 1000.0;
            return Math.round(x) + offset;
         }
         catch(NumberFormatException nfe){}
      }
      
      // or could be a string representation
      SimpleDateFormat sdf = Library.getSDF(sDateFormat);
      ParsePosition ppos = new ParsePosition(0);
      Date date = sdf.parse(ts, ppos);
      if (date==null) return -1;
      return date.getTime()+offset;
   }
   
   /**
    * @return the average of the given double arrays
    */
   protected double[] avgData(ArrayList<double[]> data)
   {
      int n = data.size();
      int nd = data.get(0).length;
      double a[] = Library.allocVectorDouble(nd, 0);
      for(int i = 0; i < n; i++)
      {
         double w[] = data.get(i);
         for(int d = 0; d < nd; d++)
            a[d] += w[d];
      }
      for(int d = 0; d < nd; d++)
         a[d] /= (double)n;
      return a;
   }

   /**
    * Add the given data to the given sequence (creating it if necessary using the given
    * title and start time). The data is added according to the current global bRemoveDups
    * setting.
    * 
    * @param data data to add
    * @param seq sequence to add data to
    * @param title title of sequence if needs to be created
    * @param ms time of first sample if sequence needs to be created
    * @return the sequence
    */
   protected Sequence addData(ArrayList<double[]> data, Sequence seq, String title, long ms)
   {      
      if (seq == null) seq = new Sequence(title);
         
      if (bRemoveDups)
      {
         // add the average of the elements
         double a[] = avgData(data);
         seq.add(new FeatureVec(a), ms);
      }
      else
      {
         // add all of the elements individually
         for(int i = 0; i < data.size(); i++)
         {
            double a[] = data.get(i);
            seq.add(new FeatureVec(a), ms);
         }
      }
      return seq;
   }
   
   /** move data points with same time stamp according to global fps */
   protected void calcAvgFreq(Sequence data)
   {
      if (data.length()<2) return;
      
      long msLength = data.getEndMS() - data.getStartMS();
      int nFrames = data.length();
      double fps = (double)nFrames / (msLength/1000.0);
      data.setFreq(fps);
      //System.err.printf(" Global fps (%s): %dD  #frames=%d  seconds=%.1f  =>  %.1f fps\n",
      //      data.getName(), data.getNumDims(), nFrames, (double)msLength/1000.0, fps);
      
      if (bForceUniform) data.removeDates();
   }

   public Sequence load(String path)
   {      
      String sTitle = Library.getTitle(path);
      LineNumberReader in = null;
      boolean bAutoFound = false;
      String line=null;
      try
      {         
         File file = new File(path);
         in = new LineNumberReader(new FileReader(file));
         Sequence data = null;
         ArrayList<double[]> prevData = new ArrayList<double[]>();
         int nd = -1;
         double v[] = null;
         int di = 0;
         String ts = null;
         long prevms = Library.LNAN, ms = Library.AppStartTime;
         int nBadLines = 0;
         
         while((line = in.readLine()) != null)
         {            
            // skip empty lines
            line = line.trim();
            if (line.length() == 0) continue;

            // setup string tokenizer
            StringTokenizer st = new StringTokenizer(line, delims);

            // figure out the dimensionality of the data if this is the first line
            int nTokens = st.countTokens();
            if (nd < 0) nd = nTokens - 1;
            if (nTokens < nd+1)
            {
               nBadLines++;
               if (nBadLines > 1)
               {
                  System.err.printf("Error: too many incomplete lines (%d)\n (%s)\n", nBadLines, path);
                  return null;
               }
               else continue;            
            }
            v = new double[nd];
            
            // parse this line's data
            if (tpos == TPos.start) ts = st.nextToken();
            for(int i = 0; i < v.length; i++)
            {
               String sToken = st.nextToken();
               if (tpos == TPos.auto && !bAutoFound)
               {
                  try
                  {
                     double x = Double.parseDouble(sToken);
                     v[i] = x;
                  } catch (NumberFormatException e)
                  {
                     // this is the time stamp
                     ts = sToken;
                     bAutoFound = true;
                     di = -1;
                  }
               }
               else v[i + di] = Double.parseDouble(sToken);
            }
            if (tpos == TPos.end) ts = st.nextToken();
            
            if (ts == null)
            {
               System.err.printf("Error: unable to find timestamp on line %d\n", in.getLineNumber());
               System.err.printf(" line=\"%s\"\n", line);
               System.err.printf(" tpos=%s  format=%s\n", tpos, timeFormat);
               return null;
            }
            ms = parseTimestamp(ts);
            if (ms < 0)
            {
               System.err.printf("Error: invalid timestamp on line %d (%s)\n", in.getLineNumber(), ts);
               return null;
            }
            
            // do we have new data?
            if (prevms!=ms)
            {
               // write the prev data
               if (!prevData.isEmpty())
               {
                  assert prevms!=Library.LNAN;
                  data = addData(prevData, data, sTitle, prevms);
               }

               // reset and add current data
               prevData.clear();
               prevData.add(v);
               prevms = ms;
            }
            else prevData.add(v);
         }

         // make sure we write out the last bit of data
         if (!prevData.isEmpty())
         {
            assert prevms!=Library.LNAN;
            data = addData(prevData, data, sTitle, prevms);
         }

         // calc average freq and interpolate if requested
         calcAvgFreq(data);
         
         in.close();
         return data;
      } catch (Exception e)
      {
         e.printStackTrace();
         System.err.printf("Line: [%s] -- could be incomplete last line of file?\n", line);
         System.err.println("Error (DLRawTS): unable to open data file\n (" + path + ")");         
         return null;
      }
   }
   
   public static void main(String args[])
   {
      usage();
   }
}
