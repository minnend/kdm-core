package kdm.data;

import java.util.*;
import kdm.util.*;

/**
 * Represents a point in time on a graph, including the x screen coordinate, the index of
 * the nearest data point, and the absolute time.
 */
public class TimeX
{
   /** x screen coordinate */
   public int x;

   /** index of nearets data point */
   public int index;

   /** time since start of component in ms */
   public long timeOffset;

   /** absolute time in ms */
   public long time;

   public TimeX(TimeX tx)
   {
      this(tx.x, tx.index, tx.timeOffset, tx.time);
   }

   /**
    * Create a TimeX object explicitly from the components
    * @param _x x screen coordinate of point
    * @param _index index of closest data point
    * @param _timeOffset time since start of component in ms
    * @param _time absolute time in ms
    */
   public TimeX(int _x, int _index, long _timeOffset, long _time)
   {
      x = _x;
      timeOffset = _timeOffset;
      index = _index;
      time = _time;
   }

   public Calendar getCal()
   {
      Calendar cal = Library.now();
      cal.setTimeInMillis(time);
      return cal;
   }
   
   public Date getDate()
   {
      return new Date(time);
   }

   public String toString()
   {
      return String.format("[TimeX: x=%d index=%d  offset=%d]", x, index, timeOffset);
   }
}
