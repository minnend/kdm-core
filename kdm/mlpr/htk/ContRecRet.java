package kdm.mlpr.htk;

import java.io.*;
import java.util.*;
import kdm.data.*;
import kdm.util.*;

/** return object for continuous recognition via HTK */
public class ContRecRet
{
   public ArrayList<MarkupSet> markupSets;
   public String sErr, sOut;
   public double llTotal;
   public MyDoubleList llSeq;

   public ContRecRet(ArrayList<MarkupSet> markupSets, MyDoubleList llSeq, double llTotal, String sOut,
         String sErr)
   {
      this.markupSets = markupSets;
      this.llSeq = llSeq;
      this.llTotal = llTotal;
      this.sErr = sErr;
      this.sOut = sOut;
   }

   /**
    * @return map from motif name to total data log-likelihood for that motif given the markupSets in this
    *         object
    */
   public HashMap<String, MotifInfo> getMotifInfo()
   {
      HashMap<String,MotifInfo> map = new HashMap<String, MotifInfo>();
      int iSeq = 0;
      for(MarkupSet marks : markupSets){
         for(TimeMarker tm : marks.getList()){
            String tag = tm.getTag();
            WindowLocation wloc = new WindowLocation(iSeq, tm.getStartIndex(), (int)tm.length());
            MotifInfo mi = map.get(tag);
            if (mi == null){
               mi = new MotifInfo();
               map.put(tag, mi);
            }
            mi.add(wloc, (Double)tm.getMeta());
         }
         iSeq++;
      }
      return map;
   }
   
   public class MotifInfo{
      public double loglik;
      public ArrayList<WindowLocation> wlocs;
      
      public MotifInfo()
      {
         this.loglik = Library.LOG_ONE;
         wlocs = new ArrayList<WindowLocation>();
      }

      public int getNumLocs(){ return wlocs.size(); }
      
      public void add(WindowLocation wloc, double loglik)
      {
         this.loglik += loglik;
         wlocs.add(wloc);
      }
   }
}
