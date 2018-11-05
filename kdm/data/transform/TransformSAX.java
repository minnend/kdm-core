package kdm.data.transform;

import kdm.data.*;
import kdm.util.*;

/**
 * Transform a 1D real-valued sequence according to the SAX method of Lin and Keogh.
 */
public class TransformSAX extends DataTransform
{
   protected int nSymbols, nPaa;   
   protected int iStart, nLen;
   protected double[] paa;
   protected double[] raw;
   protected Sequence prevData;
   
   /** Create a SAX transformation that converts entire sequences */
   public TransformSAX(int nPaa, int nSymbols)
   {
      this(nPaa, nSymbols, -1, -1);
   }
   
   /** Create a SAX transformation that converts a subsequence */
   public TransformSAX(int nPaa, int nSymbols, int iStart, int nLen)
   {      
      this.nPaa = nPaa;
      this.nSymbols = nSymbols;
      this.iStart = iStart;
      this.nLen = nLen;      
   }
   
   public void setStart(int iStart)
   {
      this.iStart = iStart;
   }
   
   public void setLength(int nLen)
   {
      this.nLen = nLen;
   }
   
   public void setWindow(int iStart, int nLen)
   {
      this.iStart = iStart;
      this.nLen = nLen;
   }

   public void dumpParams()
   {
      System.err.printf("%s: #paa=%d  #symbols=%d\n", getClass(),  nPaa, nSymbols);      
   }

   public Sequence transform(Sequence data)
   {     
      assert data.getNumDims()==1 : "Warning: can't apply SAX to a multidimensonal sequence.";
      if (data.getNumDims() != 1) return null;
      
      // create the PAA
      if (raw==null || prevData != data){
         raw = data.extractDim(0);
         prevData = data;
      }
      if (paa==null || paa.length != nPaa) paa = new double[nPaa];
      int iStart = this.iStart < 0 ? 0 : this.iStart;
      int nLen = this.nLen <= 0 ? raw.length : this.nLen;
      SAX.genPAA(raw, iStart, nLen, paa);
      
      // create the SAX sequence
      DiscreteSeq dseq = new DiscreteSeq("SAX: " + data.getName(), data.getFreq(), nSymbols, data.getStartMS());
      for(int i = 0; i < nPaa; i++)
      {
         int sym = SAX.raw2int(paa[i], nSymbols);
         dseq.add(new FeatureVec(1, sym));
      }
      dseq.copyMeta(data);
      
      return dseq;
   }

   
}
