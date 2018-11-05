package kdm.util;

/**
 * Wraps an int in a mutable object (compare to Integer which is immutable)
 */
public class MutableInteger
{
   protected int value;
   
   public MutableInteger(int x){ value = x; }
   public MutableInteger(Integer x){ value = x.intValue(); }
   
   public void set(int x){ value = x; }
   public int getValue(){ return value; }
   public Integer getInteger(){ return new Integer(value); }
   
   public void inc(){ value++; }
   public void dec(){ value--; }
   public void add(int x){ value += x; }
   public void sub(int x){ value -= x; }
}
