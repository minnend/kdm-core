package kdm.util;

/**
 * Wraps a double in a mutable object (compare to Double which is immutable)
 */
public class MutableDouble
{
   protected double value;
   
   public MutableDouble(int x){ value = x; }
   public MutableDouble(Double x){ value = x.doubleValue(); }
   
   public void set(double x){ value = x; }
   public double getValue(){ return value; }
   public Double getDouble(){ return new Double(value); }
   
   public void inc(){ value++; }
   public void dec(){ value--; }
   public void add(double x){ value += x; }
   public void sub(double x){ value -= x; }
}