package kdm.data;

import kdm.util.*;

/** stores a string that has been run length encoded */
public class StringRLE
{
   protected String sOrig, sRle;
   protected int aOrig[], aRle[];

   /** map from RLE string to original string index */
   protected int map[];

   /** repetitions of symbol in RLE string at a given index */
   protected int count[];

   public StringRLE(String s, int base)
   {
      sOrig = s;
      int N = s.length();
      aOrig = new int[N];
      for(int i = 0; i < N; i++)
         aOrig[i] = (int)s.charAt(i) - base;
      buildRLE(base);
   }

   public StringRLE(int[] s, int base)
   {
      sOrig = ia2str(s, base);
      this.aOrig = s;
      buildRLE(base);
   }

   /** convert the int array to a string mapping the given base character to zero */
   public static String ia2str(int[] a, int base)
   {
      StringBuffer sb = new StringBuffer();
      for(int i = 0; i < a.length; i++)
         sb.append((char)(a[i] + base));
      return sb.toString();
   }

   public String getOrigString()
   {
      return sOrig;
   }

   public int[] getOrigIntArray()
   {
      return aOrig;
   }

   public String getString()
   {
      return sRle;
   }

   public int[] getIntArray()
   {
      return aRle;
   }

   public int getOrigIndex(int ix)
   {
      return map[ix];
   }

   /**
    * @return index in RLE string given index in orig; note this requires a scan, so it's O(T) not O(1)
    */
   public int getRleIndex(int ix)
   {
      int N = map.length - 1;
      for(int i = 0; i < N; i++)
         if (map[i] <= ix && map[i + 1] > ix) return i;
      return N;
   }

   public int getInt(int i)
   {
      return aRle[i];
   }

   public int getCount(int i)
   {
      return count[i];
   }

   public char getOrigChar(int i)
   {
      return sOrig.charAt(i);
   }

   public char getChar(int i)
   {
      return sRle.charAt(i);
   }

   public int getOrigLen()
   {
      return sOrig.length();
   }

   public int getRleLen()
   {
      return sRle.length();
   }

   public int length()
   {
      return sRle.length();
   }

   protected boolean buildRLE(int base)
   {
      int N = aOrig.length;
      MyIntList syms = new MyIntList();
      MyIntList lmap = new MyIntList();
      MyIntList lcount = new MyIntList();
      int i = 0;
      while(i < N){
         syms.add(aOrig[i]);
         int j = i + 1;
         while(j < N && aOrig[j] == aOrig[i])
            j++;
         lcount.add(j - i);
         lmap.add(i);
         i = j;
      }
      aRle = syms.toArray();
      count = lcount.toArray();
      map = lmap.toArray();
      sRle = ia2str(aRle, base);
      return true;
   }

   public static void main(String args[])
   {
      int base = 'a';
      StringRLE r = new StringRLE("aaaabcccdeeffffg", base);
      System.err.printf("%s -> %s\n", r.getOrigString(), r.getString());
      for(int i = 0; i < r.length(); i++)
         System.err.printf(" %c: %d @ %d\n", r.getChar(i), r.getCount(i), r.getOrigIndex(i));
   }
}
