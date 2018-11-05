package kdm.io;

import java.io.*;
import java.util.*;

import kdm.data.*;
import kdm.util.*;

/** loads an HTK master label file (mlf) */
public class MLHtkMlf extends MarkupLoader
{
   protected int sampPer = 1;
   protected MyDoubleList llSeq;
   protected double llTotal;

   public MLHtkMlf()
   {
      super(true);
   }

   public MLHtkMlf(int _sampPer)
   {
      super(true);
      sampPer = _sampPer;
   }

   public MyDoubleList getSeqLogLiks(){ return llSeq; }
   public double getSeqLogLik(int i){ return llSeq.get(i); }
   public double getTotalLogLik(){ return llTotal; }
   
   @Override
   public MarkupSet load(String path)
   {
      LineNumberReader in = null;

      try{
         in = new LineNumberReader(new FileReader(path));
         StringTokenizer st;
         String line;

         // read the #!MLF!# header
         line = Library.readLine(in);
         if (line == null || !line.equals("#!MLF!#")){
            System.err.printf("Error: missing MLF header (%s)\n", line);
            return null;
         }

         // we may have multiple sequences
         markupSets = new ArrayList<MarkupSet>();
         llSeq = new MyDoubleList();
         while(true){
            // parse the next sequence
            MarkupSet marks = new MarkupSet();
            line = Library.readLine(in);
            if (line == null) break; // no more label files
            if (line.length() < 2 || line.charAt(0) != '"' || !line.endsWith("\"")){
               System.err.printf("Error: missing label set name (line %d: %s)\n", in.getLineNumber(), line);
               return null;
            }
            marks.setName(line.substring(1, line.length() - 1));

            // now read the labels
            double llsum = 0;
            sentence: while((line = in.readLine()) != null){
               st = new StringTokenizer(line, " \t\r\n");

               // detect the final period
               String sFirst = st.nextToken();
               if (sFirst.equals(".")) break;

               // see if we have n-best
               if (sFirst.equals("///")){
                  // we do, so skip to period
                  while(true){
                     line = in.readLine();
                     if (line == null){
                        System.err.println("Error: missing final period in n-best list");
                        return null;
                     }
                     if (line.equals(".")) break sentence;
                  }
               }

               // no period, so parse another time marker
               int tStart = Integer.parseInt(sFirst);
               int tStop = Integer.parseInt(st.nextToken());
               String name = st.nextToken();
               double loglik = Double.parseDouble(st.nextToken());
               llsum += loglik;

               int iStart = (int)Math.round((double)tStart / sampPer);
               int iStop = (int)Math.round((double)tStop / sampPer);

               TimeMarker mark = new TimeMarker(name, TimeMarker.Units.Index, iStart, iStop, loglik);               
               marks.add(mark);               
            }            
            llTotal += llsum;
            llSeq.add(llsum);
            markupSets.add(marks);
         }
         in.close();
      } catch (Exception e){
         System.err.println(e);
         return null;
      }

      if (markupSets.isEmpty()){
         System.err.printf("Warning: no labels found!\n");
         return null;
      }
      return markupSets.get(0);
   }

}
