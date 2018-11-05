package kdm.data;

import java.util.*;

import kdm.models.*;
import kdm.util.*;
import java.text.*;

/**
 * A Sequence represents a list of multidimentional feature vectors. Each sequence has a
 * specific frequency and the absolute time of every data point can be computed.
 */
public class Sequence extends Datum
{
   public static final String KeyFreq = "Seq.Freq";
   public static final String KeyName = "Seq.Name";
   public static final String KeyStartMS = "Seq.StartMS";
   public static final String KeyParentIndex = "Seq.ParIndex";
   public static final String KeyParentOffset = "Seq.ParOfs";
   public static final String KeyOrigFile = "Seq.OrigFile";
   
   // TODO should be able to iterate over frames

   /** actual data stored chronologically */
   protected ArrayList<FeatureVec> data;
   
   /**
    * Create an anonymous sequence at 1Hz
    */
   public Sequence()
   {
      init();
   }

   /**
    * Create a named sequence
    * 
    * @param name name of this sequence
    */
   public Sequence(String name)
   {
      setName(name);
      init();
   }

   /**
    * Create a named sequence with the given sampling frequency
    * 
    * @param name name of this sequence
    * @param freq sampling frequency
    */
   public Sequence(String name, double freq)
   {
      setName(name);
      setFreq(freq);
      init();
   }

   /**
    * Create a named sequence with the given sampling frequency
    * 
    * @param name name of this sequence
    * @param freq sampling frequency
    * @param msStart start time (in ms) of the sequence
    */
   public Sequence(String name, double freq, long msStart)
   {
      setName(name);
      setFreq(freq);
      setStartMS(msStart);
      init();
   }
   
   /** initialize this sequence (constructors call this function) */
   protected void init()
   {
      data = new ArrayList<FeatureVec>();
   }

   /**
    * Duplicate the given sequence
    * 
    * @param seq sequence to dup
    */
   public Sequence(Sequence seq)
   {
      this(seq.getName(), seq);
   }

   /**
    * Duplicate the given sequence, but give the dup a new name
    * 
    * @param name new name
    * @param seq sequence to duplicate
    */
   public Sequence(String name, Sequence seq)
   {                  
      copyMeta(seq);      
      setName(name);
      
      int n = seq.length();
      data = new ArrayList<FeatureVec>(n);
      for(int i = 0; i < n; i++)
         data.add(new FeatureVec(seq.get(i)));
   }

   /**
    * Creates a new 1D sequence with the given name that contains the specified data.
    * 
    * @param name - name of the new sequence
    * @param data - data for the sequence
    */
   public Sequence(String name, double data[])
   {
      this(name);
      for(int i = 0; i < data.length; i++)
         add(new FeatureVec(1, data[i]));
   }

   /**
    * Creates a new sequence with the given name that contains the specified data.
    * 
    * @param name - name of the new sequence
    * @param data - data for the sequence (NxD)
    */
   public Sequence(String name, double data[][])
   {
      this(name);
      setData(data);
   }
   
   /**
    * Creates a new sequence with the given name that contains the specified data.
    * 
    * @param name - name of the new sequence
    * @param data - data for the sequence
    */
   public Sequence(String name, ArrayList<FeatureVec> data)
   {
      this(name);
      setData(data);
   }
   
   /** @return data as a list of feature vectors (not a copy!) */
   public ArrayList<FeatureVec> getData(){ return data; }

   /**
    * replace all data in this sequence
    * 
    * @param data new data (NxD)
    */
   public void setData(double data[][])
   {
      this.data.clear();
      for(int i = 0; i < data.length; i++)
         add(new FeatureVec(data[i]));
   }
   
   /**
    * replace all data in this sequence
    * 
    * @param data new data
    */
   public void setData(ArrayList<FeatureVec> data)
   {
      this.data.clear();
      this.data.addAll(data);
   }

   /**
    * replace all data in this sequence (this method is much slower than setData() due to the way the sequence
    * data is stored)
    * 
    * @param data new data (DxT)
    */
   public void setDataByRow(double data[][])
   {
      this.data.clear();
      int nDims = data.length;
      for(int i = 0; i < data[0].length; i++){
         FeatureVec fv = new FeatureVec(nDims);
         for(int d = 0; d < nDims; d++)
            fv.set(d, data[d][i]);
         add(fv);
      }
   }

   public void setOrigFile(String sFile)
   {
      if (sFile != null) setMeta(KeyOrigFile, sFile);
      else removeMeta(KeyOrigFile);
   }

   public String getOrigFile()
   {
      return getMeta(KeyOrigFile, null);
   }

   public String toString()
   {
      return String.format("[Seq: %d, %dD  par=%d.%d]", length(), getNumDims(), getParentIndex(), getParentOffset());
   }

   /** set the frequency parameter for this sequence */
   public void setFreq(double freq)
   {
      setMeta(KeyFreq, freq);
   }

   /**
    * Clean the sequence by filling in NaN values via a 5-tap kernel. This procedure will only fill in
    * singleton NaN values, and will fail if more data is missing.
    * 
    * @param bReport if true, each NaN value is reported via stderr
    * @return true if the missing data could be filled in
    */
   public boolean clean(boolean bReport)
   {
      final double f1 = 0.125;
      final double f2 = 0.375;
      int T = length();
      int nd = getNumDims();
      for(int t = 0; t < T; t++){
         FeatureVec fv = get(t);
         if (!fv.isValid()){
            if (bReport) System.err.printf("Invalid Data: %s   (%s, %d)\n", fv, getMeta(KeyName, "anon"), t);
            if (T < 5) return false; // can't clean if there isn't enough data
            for(int d = 0; d < nd; d++){
               if (!Double.isNaN(fv.get(d))) continue;

               double ca, cb, cc, cd;

               if (t == 0) ca = cb = get(t + 1, d);
               else if (t == 1) ca = cb = get(t - 1, d);
               else{
                  ca = get(t - 2, d);
                  cb = get(t - 1, d);
               }
               if (t == T - 1) cc = cd = get(t - 1, d);
               else if (t == T - 2) cc = cd = get(t + 1, d);
               else{
                  cc = get(t + 1, d);
                  cd = get(t + 2, d);
               }

               if (Double.isNaN(ca) || Double.isNaN(cb) || Double.isNaN(cb) || Double.isNaN(cb)){
                  if (bReport)
                     System.err.printf("Can't clean: %.4f  %.4f  -X-  %.4f  %.4f\n", ca, cb, cc, cd);
                  return false;
               }

               fv.set(d, f1 * (ca + cd) + f2 * (cb + cc));
            }
            if (bReport) System.err.printf("   -> %s\n", fv);
         }
      }
      return true;
   }

   /** @return minimum value in each dimension */
   public FeatureVec getMin()
   {
      return getMin(0, data.size());
   }

   /** @return minimum value in each dimension within the given range (inclusive, exclusive) */
   public FeatureVec getMin(int iStart, int iEnd)
   {
      if (iStart >= iEnd) return null;
      FeatureVec ret = new FeatureVec(get(iStart));
      for(int i = iStart + 1; i < iEnd; i++)
         ret._min(get(i));
      return ret;
   }

   // TODO: write a min & max function using the 3n/2 method

   /** @return maximum value in each dimension */
   public FeatureVec getMax()
   {
      return getMax(0, data.size());
   }

   /** @return maximum value in each dimension within the given range (inclusive, exclusive) */
   public FeatureVec getMax(int iStart, int iEnd)
   {
      if (iStart >= iEnd) return null;
      FeatureVec ret = new FeatureVec(get(iStart));
      for(int i = iStart + 1; i < iEnd; i++)
         ret._max(get(i));
      return ret;
   }

   /** @return vectors of min/max values in the given range */
   public FeatureVec[] getMinMax(int iStart, int n)
   {
      int nd = getNumDims();
      FeatureVec[] ret = new FeatureVec[2];
      ret[0] = new FeatureVec(get(iStart));
      ret[1] = new FeatureVec(get(iStart));
      double[] vmin = ret[0].get();
      double[] vmax = ret[1].get();
      for(int i = 1; i < n; i++){
         double[] v = data.get(iStart + i).get();
         for(int d=0; d<nd; d++){
            if (v[d] < vmin[d]) vmin[d] = v[d];
            else if (v[d] > vmax[d]) vmax[d] = v[d];
         }
      }
      return ret;
   }

   /** @return mean of each dimension */
   public FeatureVec getMean()
   {
      return getMean(0, data.size());
   }

   /** @return mean of each dimension within the given range (inclusive, exclusive) */
   public FeatureVec getMean(int iStart, int iEnd)
   {
      if (iStart >= iEnd) return null;
      FeatureVec ret = new FeatureVec(get(iStart));
      for(int i = iStart + 1; i < iEnd; i++)
         ret._add(get(i));
      ret._div(iEnd - iStart);
      return ret;
   }

   /** @return variance of each dimension */
   public FeatureVec getVar()
   {
      return getVar(0, data.size());
   }

   /** @return variance of each dimension within the given range (inclusive, exclusive) */
   public FeatureVec getVar(int iStart, int iEnd)
   {      
      if (iStart >= iEnd) return null;
      int nd = getNumDims();
      
      GaussianDyn1D[] gm = new GaussianDyn1D[nd];      
      for(int d=0; d<nd; d++) gm[d] = new GaussianDyn1D();
      for(int i = iStart; i < iEnd; i++)         
         for(int d=0; d<nd; d++)
            gm[d].add(get(i,d), false);
      
      FeatureVec ret = new FeatureVec(nd);
      for(int d=0; d<nd; d++){
         gm[d].update();
         ret.set(d, gm[d].getVar());
      }
      return ret;
   }
   
   public String getName()
   {
      return getMeta(KeyName, "SeqAnon");
   }

   public void setName(String name)
   {
      if (name != null) setMeta(KeyName, name);
      else removeMeta(KeyName);
   }

   public double getFreq()
   {
      return getMeta(KeyFreq, 1.0);
   }

   public double getPeriod()
   {
      return 1.0 / getFreq();
   }

   public long getPeriodMS()
   {
      return Math.round(1000 * getPeriod());
   }

   public final int length()
   {
      return data.size();
   }

   public final void setStartMS(long ms)
   {
      setMeta(KeyStartMS, ms);
   }

   /**
    * @return start time (time of first sample in ms) of this sequence
    */
   public final long getStartMS()
   {
      return getMeta(KeyStartMS, Library.AppStartTime);
   }

   /**
    * @return end time (time of last sample in ms) of this sequence
    */
   public final long getEndMS()
   {
      int T = length();
      if (T == 0) return getStartMS();
      return getTimeMS(T - 1);
   }

   /**
    * @return length of this sequence in milliseconds
    */
   public final long getLengthMS()
   {
      return getEndMS() - getStartMS();
   }

   /** @return reference to i-th feature vec */
   public final FeatureVec get(int i)
   {
      return data.get(i);
   }

   public final double get(int i, int d)
   {
      return data.get(i).get(d);
   }

   public final void set(int i, FeatureVec fv)
   {
      data.set(i, fv);
   }

   public final void set(int i, int d, double x)
   {
      data.get(i).set(d, x);
   }

   public final FeatureVec getLast()
   {
      return data.get(data.size() - 1);
   }

   public final boolean hasParent()
   {
      return (getMeta(KeyParentIndex,-1) >= 0);
   }

   public final void setParent(int iParent, int ofsParent)
   {
      setParentIndex(iParent);
      setParentOffset(ofsParent);
   }

   public final void setParentIndex(int iParent)
   {
      setMeta(KeyParentIndex, iParent);
   }
   
   public final void setParentOffset(int ofsParent)
   {
      setMeta(KeyParentOffset, ofsParent);
   }

   /** @return index of the parent sequence */
   public final int getParentIndex()
   {
      return getMeta(KeyParentIndex, -1);
   }

   /** @return offset (starting index) of this sequence in its parent sequence */
   public final int getParentOffset()
   {
      return getMeta(KeyParentOffset, -1);
   }

   /**
    * Return the dimensionality of this sequence. We assume that the first feature vector has the correct
    * dimensionality. Return 0 if no data is loaded.
    */
   public final int getNumDims()
   {
      if (data.isEmpty()) return 0;
      return data.get(0).getNumDims();
   }

   /** @return time in ms of the given data frame */
   public final long getTimeMS(int i)
   {
      // TODO: should scan for most recent date
      FeatureVec fv = data.get(i);
      if (fv.hasTime()) return fv.getTime();
      else return getStartMS() + Math.round(i * getPeriod() * 1000L);
   }

   /** Clear all dates, effectively forcing this seq to appear uniformly sampled */
   public final void removeDates()
   {
      for(FeatureVec fv : data)
         fv.removeTime();
   }

   public void setDate(int ix, long ms)
   {
      data.get(ix).setTime(ms);
      if (ix == 0) setStartMS(ms);
   }

   /**
    * compute list of time stamps for this sequence
    * @param bForceList if true, a list is always returned, even if all timestamps are derived
    * @return list of all time stamps for this sequence, null if only start time is given (and list not forced)
    */
   public MyLongList getDates(boolean bForceList)
   {
      MyLongList list = new MyLongList();
      int nReal = 0;
      int T = data.size();
      for(int i=0; i<T; i++){
         FeatureVec fv = data.get(i);
         if (fv.hasTime()){
            nReal++;
            list.add(fv.getTime());
         }
         else list.add(getTimeMS(i));
      }
      if (!bForceList && nReal == 0) return null;
      else return list;
   }

   /**
    * Add (append) a feature vector to this sequence
    * 
    * @param value fv to add
    * @return index where fv was placed
    */
   public final int add(FeatureVec value)
   {
      data.add(value);
      if (data.size() == 1 && value.hasTime()) setStartMS(value.getTime());
      return data.size() - 1;
   }
   
   /** @param add the value at the given index location */
   public final void add(int ix, FeatureVec x)
   {
      data.add(ix, x);
   }

   /** Add a time stamped feature vector to this sequence */
   public final int add(FeatureVec value, long ms)
   {
      int ix = add(value);
      setDate(ix, ms);
      return ix;
   }

   /**
    * Compute what amounts to a Frobenius norm.
    */
   public double dist(Sequence seq)
   {
      int n = seq.length();
      assert n == length();
      int nd = seq.getNumDims();
      assert nd == getNumDims();

      FeatureVec fv1 = new FeatureVec(nd);
      for(int i = 0; i < n; i++){
         FeatureVec fv2 = get(i).sub(seq.get(i));
         fv2._mul(fv2);
         fv1._add(fv2);
      }
      return Math.sqrt(fv1.sum());
   }

   /**
    * @return index of the data point closest to the given time
    */
   public final int getClosestIndex(long ms)
   {
      int n = length();
      int a = 0;
      long ta = getTimeMS(a);
      int b = n - 1;
      long tb = getTimeMS(b);
      if (ms <= ta) return a;
      if (ms >= tb) return b;
      while(a + 1 < b){
         int m = (a + b) / 2;
         long tm = getTimeMS(m);
         if (tm == ms) return m;
         if (ms < tm) b = m;
         else a = m;
      }

      long da = Math.abs(ms - getTimeMS(a));
      long dap1 = (a + 1 < n ? Math.abs(ms - getTimeMS(a + 1)) : Long.MAX_VALUE);

      if (da <= dap1) return a;
      else return a + 1;
   }

   /**
    * Sample from the signal and interpolate as necessary. If the requested time is outside of the range of
    * the sequence, then the first (or last) feature vector is returned. Currently, linear interpolation is
    * used.
    * 
    * @param ms requested time
    * @return feature vector at the requested time, interpolated as necessary
    */
   public FeatureVec sampleTime(double ms)
   {
      // TODO: should support other (better) interpolation methods
      int n = length();
      if (ms <= getStartMS()) return new FeatureVec(get(0));
      if (ms >= getEndMS()) return new FeatureVec(get(n - 1));
      int ix = getClosestIndex(Math.round(ms));
      long msi = getTimeMS(ix);
      FeatureVec a, b;
      double f;
      if (ms < msi){
         a = get(ix - 1);
         b = get(ix);
         long msa = getTimeMS(ix - 1);
         long dms = msi - msa;
         if (dms == 0) f = 0.5;
         else f = (ms - msa) / (msi - msa);
      }
      else{
         a = get(ix);
         b = get(ix + 1);
         long msb = getTimeMS(ix + 1);
         long dms = msb - msi;
         if (dms == 0) f = 0.5;
         else f = (ms - msi) / (msb - msi);
      }

      FeatureVec fv = a.mul(f).add(b.mul(1.0 - f));

      return fv;
   }

   /**
    * Sample from the signal and interpolate as necessary. If the requested index is outside of the range of
    * the sequence, then the first (or last) feature vector is returned. Currently, linear interpolation is
    * used.
    * 
    * @param index fractional index at which to sample
    * @return feature vector at the requested position, interpolated as necessary
    */
   public FeatureVec sampleIndex(double index)
   {
      // TODO: should support other (better) interpolation methods
      int n = length();
      if (index <= 0) return new FeatureVec(get(0));
      if (index >= n - 1) return new FeatureVec(get(n - 1));

      int nd = getNumDims();
      int j = (int)index;
      double b = index - (double)j;
      FeatureVec v1 = get(j);
      FeatureVec v2 = get(j + 1);
      double a = 1.0 - b;
      FeatureVec fv = new FeatureVec(nd);
      for(int d = 0; d < nd; d++)
         fv.set(d, a * v1.get(d) + b * v2.get(d));
      return fv;
   }

   /**
    * Return a resampled sequence. The starting time of the new sequence will be the same as this sequence,
    * but other points will be interpolated as necessary to generate uniformly sampled feature vectors. The
    * final length of the new sequence may be slightly different than this sequence since no extrapolation
    * will be performed. Metadata is not preserved by this function.
    * 
    * @param freq desired frequency of the new sequence
    * @param msGap gaps longer than this value will be preserved (-1 for no preserved gaps)
    * @return new sequence with uniform frequency
    */
   public Sequence resample(double freq, long msGap)
   {
      long msStart = getStartMS();
      long msEnd = getEndMS();
      double per = 1.0 / freq;
      Sequence seq = new Sequence(getName(), freq, msStart);
      for(double fms = msStart; fms <= msEnd; fms += per){
         long ms = Math.round(fms);
         FeatureVec fv = sampleTime(ms);
         seq.add(fv);
      }
      return seq;
   }

   /**
    * Returns a new sequences with the given length that holds an interpolated version of this sequence. The
    * returned sequence can be shorter or longer than the original. This function does not preserve dates or
    * metadata. Bilinear filtering is used to compute the new data points.
    */
   public Sequence resample(int len)
   {
      assert (len > 0);

      Sequence ret = null;
      double f = getFreq() * (double)len / length();
      long msStart = getStartMS();
      ret = new Sequence(getName(), f, msStart);

      // setup the parent index info
      if (hasParent()) ret.setParent(getParentIndex(), getParentOffset());
      else ret.setParent(-1, 0);
      
      // now compute and add the rest of the data points
      double dms = (double)getLengthMS() / (len-1);
      double ms = msStart;
      for(int i = 0; i < len; i++, ms += dms)
         ret.add(sampleTime(ms));
      
      return ret;
   }

   /**
    * Downsample this sequence by the given factor. This functions applies a smoothing kernel (Gaussian) for
    * interpolation. Dates and metadata are not preserved.
    * 
    * @param f downsample factor (2 => output = half the size of input)
    * @return output sequence, appropriately downsampled
    */
   protected Sequence downsample(int f)
   {
      int len = length();
      double[] k = Library.buildSmoothKernel(2 * f + 1);
      Sequence seq = new Sequence(getName() + " - down" + f, getFreq() / f, getStartMS());
      for(int i = f / 2 - 1; i < len; i += f){
         FeatureVec fv = applyKernel(i, k);
         seq.add(fv);
      }
      return seq;
   }

   /**
    * Add the given value to each element of each frame
    * 
    * @param x value to add
    * @return this sequence
    */
   public final Sequence _add(double x)
   {
      int n = data.size();
      for(int i = 0; i < n; i++)
         data.get(i)._add(x);
      return this;
   }

   /**
    * Multiply each element of each frame by the given value
    * 
    * @param x value to multiply by
    * @return this sequence
    */
   public final Sequence _mul(double x)
   {
      int n = data.size();
      for(int i = 0; i < n; i++)
         data.get(i)._mul(x);
      return this;
   }

   /**
    * Subtract the given value from each element of each frame
    * 
    * @param x value to subtract
    * @return this sequence
    */
   public final Sequence _sub(double x)
   {
      int n = data.size();
      for(int i = 0; i < n; i++)
         data.get(i)._sub(x);
      return this;
   }

   /**
    * subtract each frame of the given sequence from the corresponding frame in this sequence (in-place);
    * Length and dimensionality must match
    * 
    * @param seq sequence to subtract
    * @return this sequence
    */
   public final Sequence _sub(Sequence seq)
   {
      assert (length() == seq.length());
      assert (getNumDims() == seq.getNumDims());
      int T = length();
      for(int t = 0; t < T; t++)
         get(t)._sub(seq.get(t));
      return this;
   }

   /**
    * Subtract each frame of the given sequence from the corresponding frame in this sequence and store the
    * difference in a new sequence; Length and dimensionality must match
    * 
    * @param seq sequence to subtract
    * @return sequence of differences
    */
   public final Sequence sub(Sequence seq)
   {
      Sequence ret = new Sequence(this);
      return this._sub(seq);
   }

   /**
    * Divide each element of each frame by the given value
    * 
    * @param x value to divid by
    * @return this sequence
    */
   public final Sequence _div(double x)
   {
      int n = data.size();
      for(int i = 0; i < n; i++)
         data.get(i)._div(x);
      return this;
   }
   
   /** @return subsequence that starts and ends at the given times */
   public Sequence subseqMS(long msA, long msB)
   {
      int iStart = getClosestIndex(msA);
      int iEnd = getClosestIndex(msB);
      return subseq(iStart, iEnd);
   }

   /**
    * Returns a new sequence comprised of the elements that exist between iStart (inclusive) and iEnd
    * (exclusive). The parent of the returned sequence is set to -1 unless this sequence already has a parent.
    */
   public Sequence subseq(int iStart, int iEnd)
   {
      return subseq(iStart, iEnd, -1);
   }

   /**
    * Returns a new sequence comprised of the elements that exist between iStart (inclusive) and iEnd
    * (exclusive). The parent of the returned sequence is set to 'index' unless this sequence already has a
    * parent. Note that references are copied so no new FeatureVec objects are created.
    */
   public Sequence subseq(int iStart, int iEnd, int index)
   {
      assert !(iStart < 0 || iEnd <= iStart || iEnd > data.size()) : String.format(
            "iStart: %d  iEnd: %d  seq.len: %d\n", iStart, iEnd, length());

      Sequence ret = new Sequence(getName(), getFreq(), getTimeMS(iStart));

      for(int i = iStart; i < iEnd; i++)
         ret.add(data.get(i));

      // setup the parent index info
      if (hasParent()) ret.setParent(getParentIndex(), getParentOffset() + iStart);
      else ret.setParent(index, iStart);

      return ret;
   }

   /**
    * @return the location of this sequence, assuming it was extracted from some other sequence
    */
   public final WindowLocation getWindowLoc()
   {
      return new WindowLocation(getParentIndex(), getParentOffset(), length());
   }

   /**
    * Returns a sequence equal to the subsequence specified by the given window location in this sequence.
    */
   public final Sequence subseq(WindowLocation loc)
   {
      Sequence ret = subseq(loc.iStart, loc.end());
      if (!ret.hasParent()) ret.setParentIndex(loc.iSeries);
      return ret;
   }

   /**
    * Returns a new dataset comprised of all time steps in this dataset but with different dimensions. The new
    * dataset will have one dimension corresponding to each (zero-based) element in dims.
    * 
    * Thus, to select the first and third dimensions from a 3D dataset: Sequence data2D =
    * data3D.selectDims(new int[]{0,2});
    */
   public final Sequence selectDims(int[] dims)
   {
      Sequence ret = new Sequence(getName(), getFreq(), getStartMS());

      // setup the parent index info
      if (hasParent()) ret.setParent(getParentIndex(), getParentOffset());
      else ret.setParent(-1, 0);

      // loop through remaining time steps
      int j = 0;
      int n = length();
      for(int i = 0; i < n; i++)
         ret.add(get(i).selectDims(dims));
      ret.copyMeta(this);
      return ret;
   }

   /**
    * Add (literally, algebraic addition) the frames of the given sequence to the corresponding frames of this
    * sequences; length and dimensionality must match
    * 
    * @param seq sequence to add
    * @return this sequence, after given frames are added
    */
   public Sequence addFrames(Sequence seq)
   {
      assert (length() == seq.length());
      assert (getNumDims() == seq.getNumDims());

      int n = length();
      for(int i = 0; i < n; i++)
         data.get(i)._add(seq.get(i));

      return this;
   }

   /**
    * Creates a new dataset from the concatenation of this sequence and the given dataset in different
    * dimensions (does not modify this sequence).
    */
   public Sequence addDims(Sequence seq)
   {
      assert (length() == seq.length()) : String.format("length mismatch: %d vs %d", length(), seq.length());

      int n = length();
      int da = getNumDims();
      int db = seq.getNumDims();

      double x[][] = new double[n][da + db];
      for(int i = 0; i < n; i++){
         for(int j = 0; j < da; j++)
            x[i][j] = get(i, j);
         for(int j = 0; j < db; j++)
            x[i][da + j] = seq.get(i, j);
      }

      return new Sequence("Concat Dims: " + getName() + " + " + seq.getName(), x);
   }

   /** convenience method for simple sequence append -- calls append(seq,false,true)*/
   public int append(Sequence seq){
      return append(seq, false, true);
   }
   
   /**
    * Add the given sequence to the end of this sequence
    * 
    * @param seq the new data to append
    * @param bDates if true, copy dates
    * @param bKeepAbsolute if true, the absolute times of <code>seq</code> is retained, otherwise, dates
    *           from <code>seq</code> are shifted
    * @return new length of data
    */
   public int append(Sequence seq, boolean bDates, boolean bKeepAbsolute)
   {
      removeMeta(KeyParentIndex); // these values no longer make sense
      removeMeta(KeyParentOffset);

      int n1 = length();
      int n2 = seq.length();

      long msBase = getEndMS() + Math.round(getPeriod() * 1000);
      long msOffset = msBase - seq.getStartMS();

      for(FeatureVec fv : seq.data){
         FeatureVec x = new FeatureVec(fv);
         if (bDates && !bKeepAbsolute) x.setTime(x.getTime() + msOffset);
         else if (!bDates) x.removeTime();
         data.add(x);
      }

      return n1 + n2;
   }

   /**
    * Given a set of sequences and a set of window locations, return a new set of sequences containing just
    * the specified windows.
    */
   public static ArrayList<Sequence> extractPats(ArrayList<Sequence> vdata, ArrayList<WindowLocation> locs)
   {
      ArrayList<Sequence> vPats = new ArrayList<Sequence>();
      for(WindowLocation loc : locs){
         Sequence seq = vdata.get(loc.iSeries);
         vPats.add(seq.subseq(loc));
      }
      return vPats;
   }

   /**
    * Modify the given set of sequences by removing the specified window location.
    */
   public static void chopInPlace(ArrayList<Sequence> vdata, WindowLocation loc, int minSeqWidth)
   {
      Sequence par = vdata.get(loc.iSeries);
      Sequence prefix = null;
      Sequence suffix = null;

      // are there enough samples at the start of this sequences to warrant a chop?
      if (loc.iStart >= minSeqWidth){
         prefix = par.subseq(0, loc.iStart);
         if (!prefix.hasParent()) prefix.setParentIndex(loc.iSeries);
      }

      // are there enough samples at the end of this sequences to warrant a chop?
      int ofs = loc.iStart + loc.length();
      if (ofs <= par.length() - minSeqWidth){
         suffix = par.subseq(ofs, par.length());
         if (!suffix.hasParent()) suffix.setParentIndex(loc.iSeries);
      }

      // update the sequence list
      vdata.remove(loc.iSeries);
      if (prefix != null) vdata.add(prefix);
      if (suffix != null) vdata.add(suffix);
   }

   /**
    * Given a set of sequences and a set of window locations, create a new set of sequences by removing the
    * specified windows.
    */
   public static ArrayList<Sequence> chop(ArrayList<Sequence> pars, WindowLocation loc, int minSeqWidth)
   {
      if (pars.isEmpty()) return pars;

      // duplicate the parent list
      ArrayList<Sequence> ret = (ArrayList<Sequence>)pars.clone();
      chopInPlace(ret, loc, minSeqWidth);
      return ret;
   }

   /**
    * Given a set of sequences and a set of window locations, remove the window locations from the sequences
    * and create a new set. Each sequence that contains a window will be broken up into zero, one, or two
    * pieces depending on whether the pieces are larger or smaller than the specified minimum sequence width.
    */
   public static ArrayList<Sequence> chop(ArrayList<Sequence> vdata, ArrayList<WindowLocation> vLocs,
         int minSeqWidth)
   {
      if (vdata.isEmpty()) return vdata;

      ArrayList<Sequence> vChop = new ArrayList<Sequence>();
      Sequence par;
      WindowLocation seg;

      // create a sorted list of window locations
      WindowLocation[] aLocs = new WindowLocation[vLocs.size()];
      vLocs.toArray(aLocs);
      Arrays.sort(aLocs);

      // loop through all of the sequences
      int iSeries = -1;
      int iPos = -1;
      for(int iLoc = 0; iLoc < aLocs.length; iLoc++){
         WindowLocation loc = aLocs[iLoc];
         if (loc.iSeries != iSeries){
            // add entire sequence if skipped (ie, no windows in the sequence)
            for(int iSeq = iSeries + 1; iSeq < loc.iSeries; iSeq++)
               vChop.add(vdata.get(iSeq));

            // add end of previous sequence
            if (iSeries >= 0 && iPos >= 0){
               par = vdata.get(iSeries);
               seg = new WindowLocation(iSeries, iPos, par.length() - iPos);
               if (seg.length() >= minSeqWidth) vChop.add(par.subseq(seg));
            }
            iSeries = loc.iSeries;
            iPos = 0;
         }

         // add everything up to this window
         par = vdata.get(iSeries);
         seg = new WindowLocation(iSeries, iPos, loc.iStart - iPos);
         if (seg.length() >= minSeqWidth) vChop.add(par.subseq(seg));

         // and finally update the position marker
         iPos = loc.end();
      }

      // finally, we have to handle everything past the last window location
      if (iSeries >= 0 && iPos >= 0){
         par = vdata.get(iSeries);
         seg = new WindowLocation(iSeries, iPos, par.length() - iPos);
         if (seg.length() >= minSeqWidth) vChop.add(par.subseq(seg));
      }
      for(int iSeq = iSeries + 1; iSeq < vdata.size(); iSeq++)
         vChop.add(vdata.get(iSeq));

      return vChop;
   }

   /**
    * Return a FV containing the sum of each dimension of this sequence.
    */
   public final FeatureVec sum()
   {
      int n = length();
      FeatureVec fv = new FeatureVec(getNumDims());
      for(int i = 0; i < n; i++)
         fv._add(get(i));
      return fv;
   }

   /**
    * Modifies this sequence to have zero mean and unit variance.
    */
   public final void znorm()
   {
      int n = length();
      int nDims = getNumDims();
      GaussianDyn1D g[] = new GaussianDyn1D[nDims];
      for(int j = 0; j < nDims; j++)
         g[j] = new GaussianDyn1D();

      for(int i = 0; i < n; i++){
         FeatureVec fv = get(i);
         for(int j = 0; j < nDims; j++)
            g[j].add(fv.get(j), false);
      }
      for(int j = 0; j < nDims; j++)
         g[j].update();
      znorm(g);
   }

   /**
    * Modifies this sequence according to the given mean/variance.
    */
   public final void znorm(Gaussian1D[] g)
   {
      int n = length();
      int nDims = getNumDims();

      for(int i = 0; i < n; i++){
         FeatureVec fv = get(i);
         for(int j = 0; j < nDims; j++)
            fv.set(j, (fv.get(j) - g[j].getMean()) / g[j].getSDev());
      }
   }

   /**
    * Extract a subsequence from the given dimension and return it in an array.
    * 
    * @param iDim - index of the dimension from which to extract
    * @param iStart - first index in the subsequence
    * @param len - length of subsequence to extract
    * @return double array representing the subseq
    */
   public double[] extractDim(int iDim, int iStart, int len)
   {
      double[] ret = new double[len];
      for(int i = 0; i < len; i++)
         ret[i] = get(i + iStart, iDim);
      return ret;
   }

   /**
    * Extract the given dimension and return it in an array.
    * 
    * @param iDim - index of the dimension to retrieve
    * @return the given dimension as a double array
    */
   public final double[] extractDim(int iDim)
   {
      return extractDim(iDim, 0, length());
   }

   /**
    * Create a new sequence with just the dimensions specified.
    * 
    * @param dims dimensions to keep (and their order in the new sequence)
    * @return new sequence with just the specified dimensions
    */
   public final Sequence extractDims(int[] dims)
   {
      int nd = dims.length;
      assert (nd > 0 && nd <= getNumDims()) : String.format("invalid #dims: %d (%d)", nd, getNumDims());

      Sequence seq = new Sequence(getName() + "- exdims", getFreq(), getStartMS());
      int T = length();
      for(int t = 0; t < T; t++){
         FeatureVec fv = data.get(t).selectDims(dims);
         seq.add(fv);
      }
      seq.copyMeta(this);
      return seq;
   }

   /**
    * Convert the data in this Sequence to a 2D array (DxT). Rows are 1D sequences.
    */
   public final double[][] toSeqArray()
   {
      int n = length();
      int nDims = getNumDims();
      double[][] x = new double[nDims][n];
      for(int i = 0; i < n; i++)
         for(int j = 0; j < nDims; j++)
            x[j][i] = data.get(i).get(j);
      return x;
   }

   /**
    * Convert the data in this Sequence into a 2D array (TxD). Rows are feature vectors.
    */
   public final double[][] toFrameArray()
   {
      int n = length();
      int nDims = getNumDims();
      double[][] x = new double[n][];
      for(int i = 0; i < n; i++)
         x[i] = data.get(i).toArray();
      return x;
   }

   /**
    * Apply the given kernel to this sequence at the given position. If the kernel extends outside of the
    * sequence, repeat the first/last data point.
    * 
    * @param iPos position at which to apply the
    * @param k the kernel to apply
    * @return new feature vector representing the convolution of the sequence with the kernel at the given pos
    */
   public FeatureVec applyKernel(int iPos, double[] k)
   {
      int nk = k.length;
      int nSeq = length();
      int iCenter = (nk - 1) / 2;
      FeatureVec fvRet = new FeatureVec(getNumDims());
      for(int ik = 0; ik < nk; ik++){
         int ix = iPos + ik - iCenter;
         FeatureVec fv;
         if (ix < 0) fv = get(0);
         else if (ix >= nSeq) fv = get(nSeq - 1);
         else fv = get(ix);
         fvRet._add(fv.mul(k[ik]));
      }
      return fvRet;
   }
}
