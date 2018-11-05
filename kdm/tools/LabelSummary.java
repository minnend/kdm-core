package kdm.tools;

import kdm.io.*;
import kdm.util.*;
import kdm.data.*;
import java.util.*;
import java.io.*;

/** dumps label info given a list of label files */
public class LabelSummary
{
   public static PrintWriter out = new PrintWriter(System.out);

   public static String adjustLabel(String sLabel)
   {
      int ix = sLabel.lastIndexOf(':');
      if (ix < 0) return sLabel;
      return sLabel.substring(ix + 1);
   }
   
   public static boolean keepClass(String lab)
   {
      if (lab.endsWith("down") || lab.endsWith("up")) return false;
      if (lab.endsWith("_bad")) return false;
      return true;
   }
   
   public static void main(String args[])
   {           
      if (args.length == 0)
      {
         System.err.println();
         System.err.println("USAGE: java kdm.tools.LabelSummary <labels 1> ... <labels n>");
         System.err.println();
         return;
      }

      // load the labels
      MLGeneral mloader = new MLGeneral();
      TreeMap<String, ArrayList<TimeMarker>> data = new TreeMap<String, ArrayList<TimeMarker>>();
      int nMarkupFiles = 0;
      int nMarks = 0;
      for(int i = 0; i < args.length; i++)
      {
         String sFilePat = args[i];
         for(File file : Library.getFilesWild("./", sFilePat))
         {
            String sPath = null;
            try
            {
               sPath = file.getCanonicalPath();
            } catch (IOException ioe)
            {
               ioe.printStackTrace();
               return;
            }
            assert (sPath != null);
            MarkupSet marks = mloader.load(file.getPath());
            assert (marks != null);
            String sName = Library.getTitle(sPath);
            marks.setName(sName);
            for(int j = 0; j < marks.size(); j++)
            {
               TimeMarker mark = marks.get(j);
               String label = adjustLabel(mark.getTag());
               if (!keepClass(label)) continue;
               ArrayList<TimeMarker> list = data.get(label);
               if (list == null)
               {
                  list = new ArrayList<TimeMarker>();
                  data.put(label, list);
               }
               list.add(mark);
               nMarks++;
            }
            nMarkupFiles++;
         }
      }
      out.printf("Found %d markup files, %d labels, and %d marks.\n", nMarkupFiles, data.size(), nMarks);
      
      Iterator<String> it = data.keySet().iterator();
      while(it.hasNext())
      {
         String lab = it.next();
         ArrayList<TimeMarker> examples = data.get(lab);
         int n = examples.size();
         long lenmin=0,lenmax=0,lensum=0;
         double lenavg = 0;
         if (n > 0)
         {
            lenmax = lensum = lenmin = examples.get(0).length();
            for(int i=1; i<n; i++)
            {
               long len = examples.get(i).length();
               if (len<lenmin)lenmin = len;
               else if (len>lenmax)lenmax = len;
               lensum += len;
            }            
            lenavg = (double)lensum / n;
         }
         out.printf(" %20s) %2d examples  ms=[%4d, %5d, %5d]\n", lab, n, lenmin, Math.round(lenavg), lenmax);
      }
      
      out.flush();
   }
}

