package kdm.io.DataLoader;

import java.io.*;

import kdm.data.*;
import kdm.util.*;

/** load a wav (audio) file */
public class DLWav extends DataLoader
{  
   @Override
   public Sequence load(String path)
   {
      // TODO should use WavReader class
      try{
         DataInputStream in = new DataInputStream(new FileInputStream(path));         
         Sequence seq = new Sequence(Library.getFileName(path));
         
         //System.err.printf("Loading WAV: %s\n", Library.getFileName(path));
         
         // read riff header
         byte[] riff = new byte[4];
         in.read(riff);
         if (riff[0]!='R' || riff[1]!='I' || riff[2]!='F' || riff[3]!='F')
         {
            System.err.printf("Error: invalid RIFF header\n");
            return null;
         }
         
         int chunkSize = Library.flipBytes(in.readInt());
         
         byte[] wave = new byte[4];
         in.read(wave);
         if (wave[0]!='W' || wave[1]!='A' || wave[2]!='V' || wave[3]!='E')
         {
            System.err.printf("Error: invalid WAVE header\n");
            return null;
         }
         
         // read fmt chunk
         byte[] fmt = new byte[4];
         in.read(fmt);
         if (fmt[0]!='f' || fmt[1]!='m' || fmt[2]!='t' || fmt[3]!=' ')
         {
            System.err.printf("Error: invalid fmt header\n");
            return null;
         }
         
         int szChunk1 = Library.flipBytes(in.readInt());
         if (szChunk1 != 16)
         {
            System.err.printf("Error: Invalid chunk1 size for PCM: %d\n", szChunk1);
            return null;
         }
         
         short format = Library.flipBytes(in.readShort());
         if (format != 1)
         {
            System.err.printf("Error: Invalid format for PCM: %d\n", format);
            return null;
         }
         
         short nChannels = Library.flipBytes(in.readShort());         
         //System.err.printf(" nChannels: %d\n", nChannels);
         
         int sampleRate = Library.flipBytes(in.readInt());
         seq.setFreq((double)sampleRate);
         //System.err.printf(" sample rate: %d\n", sampleRate);
         
         int byteRate = Library.flipBytes(in.readInt());
         //System.err.printf(" byte rate: %d\n", byteRate);
         
         short blockAlign = Library.flipBytes(in.readShort());         
         //System.err.printf(" block align: %d\n", blockAlign);
         
         short bps = Library.flipBytes(in.readShort());
         if (bps!=8 && bps!=16)
         {
            System.err.printf("Error: invalid bits per sample (only 8 & 16 are supported): %d\n", bps);
            return null;
         }
         //System.err.printf(" bits per sample: %d\n", bps);
                 
         // read data chunk
         byte[] dat = new byte[4];
         in.read(dat);
         if (dat[0]!='d' || dat[1]!='a' || dat[2]!='t' || dat[3]!='a')
         {
            System.err.printf("Error: invalid data header\n");
            return null;
         }
         
         int szChunk2 = Library.flipBytes(in.readInt());
         int nSamples = szChunk2 / (bps*nChannels/8);
         //System.err.printf(" data size (bytes): %d  (= %d samples)\n", szChunk2, nSamples);
         
         if (bps == 8)
         {
            byte[] data = new byte[szChunk2];
            if (in.read(data) != szChunk2)
            {
               System.err.printf("Error: failed to read full wave data section\n");
               return null;
            }
            
            int ix = 0;
            for(int i=0; i<nSamples; i++)
            {
               FeatureVec fv = new FeatureVec(nChannels);
               for(int d=0; d<nChannels; d++)
               {
                  int x = data[ix++];
                  if (x < 0) x += 256;
                  fv.set(d, (double)x);
               }
               if (i==0) seq.add(fv, calStart.getTimeInMillis());
               else seq.add(fv);
            }
         }
         else{
            byte[] data = new byte[szChunk2*2];
            if (in.read(data) != szChunk2)
            {
               System.err.printf("Error: failed to read full wave data section\n");
               return null;
            }
            
            int ix = 0;
            for(int i=0; i<nSamples; i++)
            {
               FeatureVec fv = new FeatureVec(nChannels);
               for(int d=0; d<nChannels; d++)
               {
                  short x = (short)((short)data[ix] | (short)(data[ix+1] << 8));
                  fv.set(d, (double)x);
                  ix += 2;
               }
               if (i==0) seq.add(fv, calStart.getTimeInMillis());
               else seq.add(fv);
            }
         }
         
         in.close();
         return seq;
      }
      catch(IOException e)
      {
         System.err.println("Error: unable to open data file\n ("+path+")");
         return null;
      }      
   }

}
