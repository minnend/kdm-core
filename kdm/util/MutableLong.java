package kdm.util;

/**
 * Wraps a long in a mutable object (compare to Long which is immutable)
 */
public class MutableLong
{
   protected long value;
   
   public MutableLong(int x){ value = x; }
   public MutableLong(Long x){ value = x.intValue(); }
   
   public void set(long x){ value = x; }
   public long getValue(){ return value; }
   public Long getLong(){ return new Long(value); }
   
   public void inc(){ value++; }
   public void dec(){ value--; }
   public void add(long x){ value += x; }
   public void sub(long x){ value -= x; }
}
