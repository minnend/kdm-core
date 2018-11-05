package kdm.io;

import kdm.data.*;

import java.util.*;
import java.io.*;

public class MLWard extends MarkupLoader
{
   public MLWard()
   {
      super(false);
   }

   public MarkupSet load(String path)
   {
      LineNumberReader in = null;
      MarkupSet marks = new MarkupSet();
      try{
         in = new LineNumberReader(new FileReader(path));
         StringTokenizer st;
         String line;

         while((line = in.readLine()) != null){
            st = new StringTokenizer(line, " \r\n\t");
            char label = (char)Math.round(Double.parseDouble(st.nextToken()));
            int iStart = (int)Math.round(Double.parseDouble(st.nextToken()));
            int iStop = (int)Math.round(Double.parseDouble(st.nextToken()));
            String sTag;
            switch(label){
            case ' ':
               sTag = "Nothing";
               break;
            case 'h':
               sTag = "Hammering";
               break;
            case 's':
               sTag = "Sawing";
               break;
            case 'f':
               sTag = "Filing";
               break;
            case 'r':
               sTag = "Drilling";
               break;
            case 'a':
               sTag = "Sanding";
               break;
            case 'g':
               sTag = "Grinding";
               break;
            case 'w':
               sTag = "Screwdriving";
               break;
            case 'v':
               sTag = "Vice";
               break;
            case 'd':
               sTag = "Drawer";
               break;
            default:
               System.err.println("Error: unexpected tag (" + label + ")");
               return null;
            }

            TimeMarker mark = new TimeMarker(sTag, TimeMarker.Units.Index, iStart, iStop);
            marks.add(mark);
         }
      } catch (Exception e){
         e.printStackTrace();
         return null;
      } finally{
         try{
            if (in != null) in.close();
         } catch (IOException ioe){
            ioe.printStackTrace();
         }
      }

      return marks;
   }
}
