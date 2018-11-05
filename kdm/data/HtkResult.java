package kdm.data;

import java.util.*;
import java.io.*;
import kdm.util.*;

/**
 * Represents a HTK results (mlf) file.
 */
public class HtkResult
{
   protected String name;
   protected HashMap<String, ArrayList<MarkupSet>> map;
   protected ArrayList<String> order;

   private HtkResult(String _name)
   {
      name = _name;
      map = new HashMap<String, ArrayList<MarkupSet>>();
      order = new ArrayList<String>();
   }

   /**
    * @param iTitleNum index built in to the title of the file name
    * @return index in the list of this title
    */
   public int getIndex(int iTitleNum)
   {
      for(int i = 0; i < order.size(); i++){
         int x = Integer.parseInt(Library.getTitle(order.get(i)));
         if (x == iTitleNum) return i;
      }
      return -1;
   }

   public Iterator<String> iterator()
   {
      return map.keySet().iterator();
   }

   public ArrayList<MarkupSet> get(String s)
   {
      return map.get(s);
   }

   public int size()
   {
      return map.size();
   }

   public HashMap<String, ArrayList<MarkupSet>> getMap()
   {
      return map;
   }

   public ArrayList<String> getOrder()
   {
      return order;
   }

   /**
    * Find the matching interpretation in this HTK result file for the given sentence
    * 
    * @param fname file name to match
    * @param labSent labeled sentence to match
    * @param bSkipFirstLast should we skip the first and last words in the HTK result file (because, for
    *           example, they are "silence" or "garbage")
    * @return markup set from continuous recognition for the given sentence
    */
   public MarkupSet findMatch(String fname, ArrayList<String> labSent, boolean bSkipFirstLast)
   {
      int nls = labSent.size();
      ArrayList<MarkupSet> amark = map.get(fname);
      for(MarkupSet mset : amark){
         int n = mset.size();
         if (bSkipFirstLast) // TODO: clean up semantics
         {
            if (n != nls + 2) continue;
            boolean bMatch = true;
            for(int i = 1; i < n - 1; i++)
               if (!mset.get(i).tag.equals(labSent.get(i - 1))){
                  bMatch = false;
                  break;
               }
            if (bMatch) return mset;
         }
         else{
            if (n != nls) continue;
            boolean bMatch = true;
            for(int i = 0; i < n; i++){
               if (!mset.get(i).tag.equals(labSent.get(i))){
                  bMatch = false;
                  break;
               }
            }
            if (bMatch) return mset;
         }
      }
      return null;
   }

   protected static boolean loadSentence(BufferedReader in, HashMap<String, ArrayList<MarkupSet>> map,
         ArrayList<String> order, int scale)
   {
      try{
         // read the file name
         String line = Library.readLine(in);
         line = line.trim();
         int n = line.length();
         if (n < 3){
            System.err.printf("Error: sentence file name too short (%d): %s\n", n, line);
            return false;
         }
         if (line.charAt(0) != '"' || line.charAt(n - 1) != '"'){
            System.err.printf("Error: sentence file name not quoted: %s\n", line);
            return false;
         }
         String fname = line.substring(1, n - 1);
         ArrayList<MarkupSet> marks = new ArrayList<MarkupSet>();
         map.put(fname, marks);
         order.add(fname);
         for(int iParse = 1;; iParse++){
            // read the info
            MarkupSet mset = new MarkupSet(String.format("%s.%02d", fname, iParse));
            marks.add(mset);
            while((line = Library.readLine(in)) != null){
               // do we have another possibility or are we done?
               line = line.trim();
               if (line.equals("///")) break;
               if (line.equals(".")) return true;

               StringTokenizer st = new StringTokenizer(line, " \t\r\n,;");
               int start = Integer.parseInt(st.nextToken()) / scale;
               int end = Integer.parseInt(st.nextToken()) / scale;
               String tag = st.nextToken();
               mset.add(new TimeMarker(tag, TimeMarker.Units.Time, start, end));
            }
         }
      } catch (IOException e){
         return false;
      } catch (NullPointerException e){
         return false;
      }
   }

   public static HtkResult load(String sFile, int scale)
   {
      HtkResult hres = new HtkResult(sFile);

      try{
         BufferedReader in = new BufferedReader(new FileReader(sFile));
         String line = Library.readLine(in);
         if (!line.equals("#!MLF!#")){
            System.err.printf("Error: not a HTK results (MLF) file -- header: %s\n %s\n", line, sFile);
            return null;
         }
         while(loadSentence(in, hres.getMap(), hres.getOrder(), scale)){}
         in.close();
      } catch (IOException e){
         System.err.println(e);
         return null;
      }

      return hres;
   }
}
