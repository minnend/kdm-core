package kdm.io;

import kdm.data.*;
import kdm.util.*;
import java.util.*;
import java.io.*;

/**
 * Loads a HTK .lab file
 */
public class MLHtkLab extends MarkupLoader
{
   protected int sampPer = 1;

   public MLHtkLab()
   {
      super(false);
   }

   public MLHtkLab(int _sampPer)
   {
      super(false);
      sampPer = _sampPer;
   }

   public MarkupSet load(String path)
   {
      LineNumberReader in = null;
      MarkupSet marks = new MarkupSet(path);
      try{
         in = new LineNumberReader(new FileReader(path));
         StringTokenizer st;
         String line;

         while((line = in.readLine()) != null){
            st = new StringTokenizer(line, " \t\r\n");

            int tStart = Integer.parseInt(st.nextToken());
            int tStop = Integer.parseInt(st.nextToken());
            int iStart = (int)Math.round((double)tStart / sampPer);
            int iStop = (int)Math.round((double)tStop / sampPer);
            String sTag = st.nextToken();

            // System.err.printf("sampPer=%d tStart: %d -> %d tStop: %d -> %d\n", sampPer, tStart, iStart,
            // tStop, iStop);

            TimeMarker mark = new TimeMarker(sTag, TimeMarker.Units.Index, iStart, iStop + 1);
            marks.add(mark);
         }
         in.close();
      } catch (Exception e){
         System.err.println(e);
         return null;
      }

      // the labels shouldn't have gaps, so if they're off-by-one, then adjust them
      for(int i = 0; i < marks.size() - 1; i++){
         TimeMarker a = marks.get(i);
         TimeMarker b = marks.get(i + 1);
         if (a.getStop() + 2 == b.getStart()) a.setStop(a.getStop() + 1);
      }

      return marks;
   }

   public boolean config(ConfigHelper chelp, String sKey, String sVal)
   {
      if (Library.stricmp(sKey, "sampPer")){
         sampPer = Integer.parseInt(sVal);
      }
      else{
         System.err.println("MLGeneral: Error: unknown setup key (" + sKey + ")");
         return false;
      }
      return true;
   }
}
