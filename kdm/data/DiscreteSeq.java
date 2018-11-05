package kdm.data;

import java.util.*;

/**
 * Represents a discrete (1D, integeger values in [0..n) ) sequence
 */
public class DiscreteSeq extends Sequence
{
   int nSymbols = -1;

   public DiscreteSeq(String _name, int _nSymbols)
   {
      super(_name);
      nSymbols = _nSymbols;
   }

   public DiscreteSeq(String _name, double _freq, int _nSymbols)
   {
      super(_name, _freq);
      nSymbols = _nSymbols;
   }
   
   public DiscreteSeq(String _name, double _freq, int _nSymbols, long _msStart)
   {
      super(_name, _freq, _msStart);
      nSymbols = _nSymbols;
   }

   public DiscreteSeq(Sequence seq, int _nSymbols)
   {
      super(seq);
      //assert (seq.getNumDims() == 1) : String.format("Error: expecting 1D, found %d dims", seq.getNumDims());
      nSymbols = _nSymbols;
   }

   public DiscreteSeq(String _name, Sequence seq, int _nSymbols)
   {
      super(_name, seq);
      //assert (seq.getNumDims() == 1) : String.format("Error: expecting 1D, found %d dims", seq.getNumDims());
      nSymbols = _nSymbols;
   }

   /**
    * Create a discrete sequence from the given array of doubles. Values are rounded
    * before added to the sequence.
    * 
    * @param _name sequence name
    * @param _data data in sequence
    */
   public DiscreteSeq(String _name, double[] _data, int _nSymbols)
   {
      super(_name);
      nSymbols = _nSymbols;
      for(int i = 0; i < _data.length; i++)
         add(new FeatureVec(1, Math.round(_data[i])));
   }

   public DiscreteSeq(String _name, int[] _data, int _nSymbols)
   {
      this(_name, _nSymbols);
      for(int i = 0; i < _data.length; i++)
         add(new FeatureVec(1, _data[i]));
   }

   /**
    * @return number of symbols in the alphabet
    */
   public int getNumSymbols()
   {
      return nSymbols;
   }

   /**
    * Set the size of the alphabet
    * 
    * @param n size of the alphabet
    */
   public void setNumSymbols(int n)
   {
      nSymbols = n;
   }

   /**
    * Return this discrete seq as a string
    * 
    * @param alphabet mapping of indices to letters
    * @return data represented as a string
    */
   public String getAsString(String alphabet)
   {
      StringBuffer sb = new StringBuffer();
      for(int t = 0; t < length(); t++)
         sb.append(alphabet.charAt(geti(t)));
      return sb.toString();
   }

   /**
    * Determine the alphabet size by inspecting the data. The alphabet size is the maximum
    * number plus 1, since we assume a range of [0..n).
    * 
    * @return number of symbols in the alphabet
    */
   public int recalcSymbols()
   {
      nSymbols = (int)getMax().get(0) + 1;
      return nSymbols;
   }

   public void remapSymbols(HashMap<Integer,Integer> map)
   {
      int T = length();
      for(int i=0; i<T; i++)
         set(i, 0, map.get(geti(i)));
      nSymbols = map.size();
   }
   
   /**
    * Remap the data to force the symbols in [0..n). That is, we could start with symbosl {
    * 3, 7, 12 } and this would be remapped to { 0, 1, 2 }.
    * 
    * @return number of symbols in the alphabet
    */
   public int remapSymbols()
   {
      TreeSet<Integer> set = new TreeSet<Integer>();
      int n = length();
      for(int i = 0; i < n; i++)
         set.add(new Integer(geti(i)));
      Iterator<Integer> it = set.iterator();
      int iSymbol = 0;
      while(it.hasNext())
      {
         int x = it.next().intValue();
         for(int i = 0; i < n; i++)
            if (geti(i) == x) set(i, 0, iSymbol);
         iSymbol++;
      }
      nSymbols = iSymbol;
      return nSymbols;
   }

   /**
    * @return the i-th symbol in the sequence.
    */
   public int geti(int i)
   {
      return (int)get(i, 0);
   }

   public DiscreteSeq subseq(int iStart, int iEnd)
   {
      return subseq(iStart, iEnd, -1);
   }
   
   public DiscreteSeq subseq(int iStart, int iEnd, int index)
   {
      return wrap(super.subseq(iStart, iEnd, index), nSymbols);
   }
   
   /**
    * Extract a subsequence and return it in an array.
    * 
    * @param iStart - first index in the subsequence
    * @param len - length of subsequence to extract
    */
   public int[] extract(int iStart, int len)
   {
      int[] ret = new int[len];
      for(int i = 0; i < len; i++)
         ret[i] = geti(i + iStart);
      return ret;
   }

   public int[] toArray()
   {
      return extract(0, data.size());
   }

   public static ArrayList<DiscreteSeq> convertFull(ArrayList<Sequence> data, int nSymbols)
   {
      ArrayList<DiscreteSeq> ret = new ArrayList<DiscreteSeq>();
      for(Sequence seq : data)
         ret.add(new DiscreteSeq(seq, nSymbols));
      return ret;
   }
   
   public static ArrayList<Sequence> convert(ArrayList<Sequence> data, int nSymbols)
   {
      ArrayList<Sequence> ret = new ArrayList<Sequence>();
      for(Sequence seq : data)
         ret.add(new DiscreteSeq(seq, nSymbols));
      return ret;
   }

   /**
    * Wrap the sequence in a DiscreteSeq. No data copies or cleanup is performed, so be
    * careful!
    * 
    * @param seq the sequence to wrap
    * @param nSymbols number of symbols for the discrete version
    * @return discrete sequence wrapper
    */
   public static DiscreteSeq wrap(Sequence seq, int nSymbols)
   {
      DiscreteSeq dseq = new DiscreteSeq("Wrap: " + seq.getName(), nSymbols);
      dseq.data = seq.data;
      dseq.copyMeta(seq);
      return dseq;
   }
}
