package kdm.data;

import java.util.*;
import java.io.*;

/**
 * Represents a set of TimeMarkers
 */
public class MarkupSet
{
   /** list of time markers that make up the labels */
   protected ArrayList<TimeMarker> marks;
   
   /** name of this set (e.g., the label) */
   protected String name;
   
   /** file where labels are stored (optional) */
   protected File fInput;
   
   /** sequences for which these time markers apply */
   protected Sequence seq;
   
   /** multiplier from original value to current value */
   protected double scale = 1.0;
   
   public MarkupSet()
   {
      this(null, null);
   }

   public MarkupSet(String name)
   {
      this(name, null);
   }
   
   public MarkupSet(String name, Sequence seq)
   {      
      this.name = name;
      this.seq = seq;
      marks = new ArrayList<TimeMarker>();
   }

   public boolean hasFile(){ return fInput!=null; }
   public File getFile(){ return fInput; }
   public void setFile(File file)
   {
      assert(file.exists()) : String.format("file does not exist: %s", file.getAbsoluteFile());
      fInput = file;
   }
   
   public Sequence getSeq(){ return seq; }
   public void setSeq(Sequence seq){ this.seq = seq; }
   
   
   public ArrayList<TimeMarker> getList()
   {
      return marks;
   }

   public TimeMarker get(int i)
   {
      return marks.get(i);
   }

   public TimeMarker first()
   {
      return marks.get(0);
   }

   public TimeMarker last()
   {
      return marks.get(marks.size() - 1);
   }

   public int size()
   {
      return marks.size();
   }

   public boolean isEmpty()
   {
      return marks.isEmpty();
   }

   public boolean contains(TimeMarker mark)
   {
      return marks.contains(mark);
   }

   public void remove(int i)
   {
      marks.remove(i);
   }

   public void setName(String _name)
   {
      name = _name;
   }

   public String getName()
   {
      return name;
   }

   public void sort()
   {
      Collections.sort(marks);
   }

   public boolean isTimeBased()
   {
      return first().isTime();
   }

   /** @return this list of time markers in their original units */
   public MarkupSet getInOrigUnits()
   {
      MarkupSet ms = new MarkupSet(name, seq);
      for(TimeMarker tm : marks) ms.add(tm.getInOrigUnits(seq));
      ms.setFile(fInput);
      return ms;
   }
   
   /** convert the time markers in this markup set to be time-based */
   public void convertToTime()
   {      
      assert(seq!=null);
      for(TimeMarker tm : marks)
         tm.convertToTime(seq);
   }
   
   /** convert the time markers in this markup set to be index-based */
   public void convertToIndex()
   {
      assert(seq!=null);
      for(TimeMarker tm : marks)
         tm.convertToIndex(seq);
   }
   
   public void scaleMarkers()
   {
      for(TimeMarker tm : marks)
      {
         boolean bTime = tm.isTime();
         if (bTime) tm.convertToIndex(seq);
         tm.scale(scale);
         if (bTime) tm.convertToTime(seq);
      }
   }

   /**
    * Remove any mark with a tag that is in the given list
    * 
    * @param ignore list of tags to ignore
    */
   public void removeClasses(ArrayList<String> ignore)
   {
      int i = 0;
      while(i < marks.size())
      {
         TimeMarker tm = marks.get(i);
         if (ignore.contains(tm.getTag())) marks.remove(i);
         else i++;
      }
   }

   /**
    * @return the "sentence" represented by this state -- ie, the concatenation of all timemark tags
    */
   public String getSentence()
   {
      StringBuffer sb = new StringBuffer();
      for(int i = 0; i < marks.size(); i++)
      {
         if (i > 0) sb.append(' ');
         sb.append(marks.get(i).getTag().replace(' ', '_'));
      }
      return sb.toString();
   }

   public void add(TimeMarker mark)
   {
      for(int i = marks.size() - 1; i >= 0; i--)
      {
         if (mark.compareTo(get(i)) >= 0)
         {
            marks.add(i + 1, mark);
            return;
         }
      }
      marks.add(0, mark);
   }

   public double getScale()
   {
      return scale;
   }

   public void setScale(double scale)
   {
      this.scale = scale;
   }
}
