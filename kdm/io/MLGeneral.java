package kdm.io;

import kdm.data.*;
import kdm.util.*;
import java.util.*;
import java.io.*;

/**
 * Loads a general format markup file. Each line contains one label of the form: "<tag>" <type> <start frame>
 * <stop frame>
 */
public class MLGeneral extends MarkupLoader
{
   protected double scale = 1.0;
   protected LabelEditor led;

   public MLGeneral()
   {
      super(false);
   }

   public MLGeneral(LabelEditor _led)
   {
      super(false);
      led = _led;
   }

   public void setLabelEditor(LabelEditor _led)
   {
      led = _led;
   }

   /**
    * Converts a string representing a time in ms to actual ms. If the string has a fractional part, then the
    * value is rounded.
    * 
    * @param s string representation
    * @return ms represented by string
    */
   protected long readTime(String s)
   {
      try{
         return Long.parseLong(s);
      } catch (NumberFormatException e){
         return Math.round(Double.parseDouble(s));
         // don't catch the exception here
      }
   }

   public MarkupSet load(String path)
   {
      LineNumberReader in = null;
      MarkupSet marks = new MarkupSet();
      marks.setScale(scale);
      try{
         in = new LineNumberReader(new FileReader(path));
         StringTokenizer st;
         String line;

         while((line = in.readLine()) != null){
            if (line.trim().length() == 0) continue; // skip blank lines
            st = new StringTokenizer(line, "\"");
            String sTag = st.nextToken();
            if (led != null) sTag = led.adjustLabel(sTag);
            String rest = st.nextToken();
            st = new StringTokenizer(rest, " \r\n\t");
            String sType = st.nextToken();
            String sStart = null;
            TimeMarker.Units units;
            if (Library.stricmp(sType, TimeMarker.Units.Time.toString())) units = TimeMarker.Units.Time;
            else if (Library.stricmp(sType, TimeMarker.Units.Index.toString())) units = TimeMarker.Units.Index;
            else{
               // assume index units if no text is present
               units = TimeMarker.Units.Index;
               sStart = sType;
            }
            long iStart = readTime(sStart == null ? st.nextToken() : sStart);
            long iStop = readTime(st.nextToken());
            TimeMarker mark = new TimeMarker(sTag, units, iStart, iStop);
            if (scale != 1.0) mark.scale(scale);
            marks.add(mark);
         }
         in.close();
      } catch (Exception e){
         System.err.printf("Error: label load exception: %s\n", e);
         return null;
      }

      return marks;
   }

   public boolean config(ConfigHelper chelp, String sKey, String sVal)
   {
      if (Library.stricmp(sKey, "scale")){
         scale = Double.parseDouble(sVal);
      }
      else{
         System.err.println("MLGeneral: Error: unknown setup key (" + sKey + ")");
         return false;
      }
      return true;
   }
}
