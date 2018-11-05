package kdm.data;

import kdm.util.*;
import java.util.*;

/**
 * Represents a marker in time, which can be either an event (single point in time) or an interval. The marker
 * can be in terms of absolute time (ms) or as indices relative to a particular data sequence.
 */
public class TimeMarker implements Comparable
{
   public enum Units {
      Time, Index
   }

   public enum Type {
      Event, Interval
   }

   /** type of marker: event or inteval */
   protected Type type;

   /** units of marker: time (ms since epoch) or index (offset from zero) */
   protected Units units, origUnits;

   /** time of event or start time of interval */
   protected long start;

   /** ending time of interval (undef for event) */
   protected long stop;

   /** label for this time marker */
   protected String tag;

   /** associated metadata */
   protected Object meta;

   /**
    * Create an event time marker at time x.
    */
   public TimeMarker(Units units, long x)
   {
      this(null, units, x, null);
   }

   /**
    * Create an event time marker at time x with the given name.
    */
   public TimeMarker(String tag, Units units, long x)
   {
      this(tag, units, x, null);
   }

   /**
    * Create an event time marker at time x with the given name.
    */
   public TimeMarker(String tag, Units units, long x, Object meta)
   {
      setTag(tag);
      origUnits = units;
      this.units = units;
      set(x);
      this.meta = meta;
   }

   /**
    * Create an interval time marker from a (inclsive) to b (exclusive).
    */
   public TimeMarker(Units units, long a, long b)
   {
      this(null, units, a, b, null);
   }

   /**
    * Create a named interval time marker from a (inclsive) to b (exclusive).
    */
   public TimeMarker(String tag, Units units, long a, long b)
   {
      this(tag, units, a, b, null);
   }

   /**
    * Create a named interval time marker from a (inclsive) to b (exclusive) with meta data.
    */
   public TimeMarker(String tag, Units units, long a, long b, Object meta)
   {
      origUnits = units;
      this.units = units;
      setTag(tag);
      set(a, b);
      this.meta = meta;
   }

   public TimeMarker(TimeMarker tm)
   {
      origUnits = tm.origUnits;
      units = tm.units;
      start = tm.start;
      stop = tm.stop;
      tag = tm.tag;
   }

   public void setMeta(Object meta)
   {
      this.meta = meta;
   }

   public Object getMeta()
   {
      return meta;
   }

   public void setUnits(Units units)
   {
      this.units = units;
   }

   public Units getUnits()
   {
      return units;
   }

   public Units getOrigUnits()
   {
      return origUnits;
   }

   public boolean isInOrigUnits()
   {
      return units == origUnits;
   }

   /**
    * Return the length of the time marker (always zero for events).
    */
   public long length()
   {
      if (isEvent()) return 0;
      else return (stop - start);
   }

   public String getTag()
   {
      return tag;
   }

   public void setTag(String _tag)
   {
      tag = _tag;
   }

   public boolean isEvent()
   {
      return type == Type.Event;
   }

   public boolean isInterval()
   {
      return type == Type.Interval;
   }

   public final boolean isTime()
   {
      return units == Units.Time;
   }

   public final boolean isIndex()
   {
      return units == Units.Index;
   }

   public void set(long x)
   {
      type = Type.Event;
      start = x;
      stop = x;
   }

   public void setStart(long ms)
   {
      start = ms;
      if (start == stop) type = Type.Event;
      else type = Type.Interval;
   }

   public void setStop(long ms)
   {
      stop = ms;
      if (start == stop) type = Type.Event;
      else type = Type.Interval;
   }

   public void set(long _msStart, long _msStop)
   {
      if (_msStart == _msStop) set(start);
      else{
         type = Type.Interval;
         if (_msStart > _msStop){
            long t = _msStart;
            _msStart = _msStop;
            _msStop = t;
         }
         start = _msStart;
         stop = _msStop;
      }
   }

   public void translate(long dms)
   {
      start += dms;
      stop += dms;
   }

   public void scale(double v)
   {
      start = (int)Math.round(start * v);
      stop = (int)Math.round(stop * v);
   }

   public int compareTo(Object o)
   {
      TimeMarker mark = (TimeMarker)o;

      if (mark.getUnits() != units){
         // arbitrarily set time to come before indices
         if (units == Units.Time) return -1;
         return 1;
      }

      if (getStart() > mark.getStart()) return 1;
      if (getStart() < mark.getStart()) return -1;

      if (isEvent()){
         if (mark.isEvent()) return 0;
         return -1;
      }
      else{
         // this is an interval
         if (mark.isEvent()) return 1;
         if (getStop() > mark.getStop()) return 1;
         if (getStop() < mark.getStop()) return -1;
         return 0;
      }
   }

   public boolean equals(Object o)
   {
      if (o instanceof TimeMarker){
         TimeMarker mark = (TimeMarker)o;
         if (mark.getType() != type) return false;
         if (isEvent()) return mark.getStart() == getStart();
         else return (mark.getStart() == start && mark.getStop() == stop);
      }
      return false;
   }

   public Type getType()
   {
      return type;
   }

   public long getStart()
   {
      return start;
   }

   public long getStop()
   {
      return stop;
   }

   /** @return overlap between this time marker and the given time marker */
   public long getOverlap(TimeMarker tm)
   {
      long a = Math.max(start, tm.start);
      long b = Math.min(stop, tm.stop);
      return Math.max(0, b - a);
   }

   /** @return overlap between this time marker and the given interval */
   public long getOverlap(long qstart, long qstop)
   {
      long a = Math.max(start, qstart);
      long b = Math.min(stop, qstop);
      return Math.max(0, b - a);
   }

   /** @return true if this time marker and the given marker overlap */
   public boolean overlaps(TimeMarker tm)
   {
      return (getOverlap(tm) > 0);
   }

   public long getStartTime()
   {
      assert (isTime());
      return start;
   }

   public long getStopTime()
   {
      assert (isTime());
      return stop;
   }

   public int getStartIndex()
   {
      assert (isIndex());
      return (int)start;
   }

   public int getStopIndex()
   {
      assert (isIndex());
      return (int)stop;
   }

   public void convertToTime(Sequence seq)
   {
      if (isTime()) return;
      units = Units.Time;
      start = seq.getTimeMS((int)start);
      stop = seq.getTimeMS((int)stop);
   }

   public void convertToIndex(Sequence seq)
   {
      if (isIndex()) return;
      units = Units.Index;
      start = seq.getClosestIndex(start);
      stop = seq.getClosestIndex(stop);
   }

   public void convertToOrigUnits(Sequence seq)
   {
      if (units == origUnits) return;
      if (origUnits == Units.Time) convertToTime(seq);
      else convertToIndex(seq);
   }

   /** @return this time marker that holds original units (copy only if necessary) */
   public TimeMarker getInOrigUnits(Sequence seq)
   {
      if (isInOrigUnits()) return this;
      TimeMarker tm = new TimeMarker(this);
      tm.convertToOrigUnits(seq);
      return tm;
   }

   /** @return this time marker that holds absolute time (copy only if necessary) */
   public TimeMarker getInTime(Sequence seq)
   {
      if (isTime()) return this;
      TimeMarker tm = new TimeMarker(this);
      tm.convertToTime(seq);
      return tm;
   }

   /** @return this time marker that holds indices (copy only if necessary) */
   public TimeMarker getInIndex(Sequence seq)
   {
      if (isIndex()) return this;
      TimeMarker tm = new TimeMarker(this);
      tm.convertToIndex(seq);
      return tm;
   }

   /**
    * Adjust ms boundaries to nearest second boundary
    */
   public void forceToNearestSecond()
   {
      assert (isTime());

      long a = start % 1000;
      if (a < 500) start -= a;
      else start += 1000 - a;

      long b = stop % 1000;
      if (b < 500) stop -= b;
      else stop += 1000 - b;
   }

   /**
    * Intersect this time marker with the given list and return the remaining ranges
    * 
    * @param tms list of time markers with which to compute intersection
    * @return list of resulting ranges (possibly empty)
    */
   public ArrayList<TimeMarker> intersect(MarkupSet tms)
   {
      ArrayList<TimeMarker> ret = new ArrayList<TimeMarker>();

      int nMarks = tms.size();
      for(int iMark = 0; iMark < nMarks; iMark++){
         TimeMarker tmo = intersect(tms.get(iMark));
         if (tmo != null) ret.add(tmo);
      }

      return ret;
   }

   /** @return new time marker that is the intersection of this marker and the given one (null if none) */
   public TimeMarker intersect(TimeMarker tm)
   {
      long a = Math.max(start, tm.start);
      long b = Math.min(stop, tm.stop);
      if (a >= b) return null;
      return new TimeMarker(units, a, b);
   }

   public String toString()
   {
      if (units == Units.Time) return String.format("[%s: [%s] -> [%s]]", tag, Library
            .formatTime(start), Library.formatTime(stop));
      else return String.format("[%s: %d -> %d]", tag, start, stop);
   }

   public String toText()
   {
      if (isEvent()) String.format("\"%s\" %s %d", tag, units, start);
      return String.format("\"%s\" %s %d %d", tag, units, start, stop);
   }

   /**
    * Load timer marker from our format ("name" start stop).
    * 
    * @param s text to parse
    * @return timer marker representation of the text
    */
   public static TimeMarker fromText(String s)
   {
      try{
         StringTokenizer st = new StringTokenizer(s, "\"");
         String tag = st.nextToken();
         st = new StringTokenizer(st.nextToken(), " ");
         Units _units = Units.Index; // assume indices as default
         long _start, _stop;
         String sToken = st.nextToken();
         try{
            _start = Long.parseLong(sToken);
         } catch (NumberFormatException nfe){
            // explicit units
            if (sToken.equals(Units.Time)) _units = Units.Time;
            else if (sToken.equals(Units.Index)) _units = Units.Index;
            else{
               System.err.printf("Error: invalid units value (%s)\n", sToken);
               return null;
            }
            _start = Long.parseLong(st.nextToken());
         }

         if (st.hasMoreTokens()){
            _stop = Long.parseLong(st.nextToken());
            return new TimeMarker(tag, _units, _start, _stop);
         }
         else return new TimeMarker(tag, _units, _start);
      } catch (NoSuchElementException e){
         return null;
      }
   }
}
