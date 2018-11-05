package kdm.data;

import java.util.*;

/**
 * Computes and stores information about a list of sequences. 
 */
public class SeqListInfo
{
   public int minLen, maxLen, iMaxLen, iMinLen, totalLen;
   public ArrayList<Sequence> data;
   
   public SeqListInfo(ArrayList<Sequence> _data)
   {
      data = _data;
      int nData = data.size();
      if (nData == 0)
      {
         minLen = maxLen = totalLen = 0;
         iMinLen = iMaxLen = -1;         
      }
      else{
         iMinLen = iMaxLen = 0;         
         maxLen = minLen = totalLen = data.get(0).length();
         for(int i=1; i<nData; i++)
         {
            int len = data.get(i).length();
            totalLen += len;
            if (len > maxLen) 
            {
               iMaxLen = i;
               maxLen = len;
            }
            else if (len < minLen) 
            {
               iMinLen = i;
               minLen = len;
            }
         }
      }
   }
   
   public int size(){ return data.size(); }
   public int getAvgLen(){ return (int)Math.round((double)totalLen / data.size()); }
}
