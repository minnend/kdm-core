package kdm.io.DataSaver;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

import kdm.data.*;
import kdm.util.*;

/**
 * Saves data to a HTK .ext file
 */
public class DSHtkExt extends DataSaver
{
   protected int nDims, nSamples, sampSize, sz, sampPer;

   public int getNumDims()
   {
      return nDims;
   }

   public int getNumSamples()
   {
      return nSamples;
   }

   public int getSampSize()
   {
      return sampSize;
   }

   public int getSampPer()
   {
      return sampPer;
   }
   
   public boolean save(Sequence seq, String path)
   {
      try{
         File file = new File(path);
         FileChannel fc = new FileOutputStream(file).getChannel();

         // Setup the memory-mapped byte buffer
         nDims = seq.getNumDims();
         nSamples = seq.length();
         sampSize = nDims * 4;
         sz = 12 + nSamples * sampSize;
         sampPer = (int)Math.round(10000.0 / seq.getFreq());
         //ByteBuffer bb = ByteBuffer.allocateDirect(sz);
         ByteBuffer bb = ByteBuffer.allocate(sz);
         bb.order(ByteOrder.BIG_ENDIAN);

         // write the header
         bb.putInt(nSamples);
         bb.putInt(sampPer);
         bb.putShort((short)sampSize);
         bb.putShort((short)9);

         // write the data
         for(int i = 0; i < nSamples; i++){
            FeatureVec fv = seq.get(i);
            for(int d = 0; d < nDims; d++)
               bb.putFloat((float)fv.get(d));
         }

         // write the data to the channel
         bb.flip();
         fc.write(bb);

         // Close the channel and the stream
         fc.close();

         return true;
      } catch (Exception e){
         System.err.println(e);
         return false;
      }
   }

}
