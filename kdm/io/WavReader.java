package kdm.io;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import javax.sound.sampled.*;
import java.awt.*;

import kdm.data.*;
import kdm.util.*;
import kdm.gui.*;

/**
 * Load a .wav file
 */
public class WavReader
{
   protected FileChannel fc;
   protected MappedByteBuffer bb;   
   protected PlayThread playThread;   
   protected int nChannels, sampleRate, byteRate, blockAlign, bps, nSamples, headerLen;

   protected WavReader()
   {}
   
   /** @return number of samples */
   public int length(){ return nSamples; }
   public int getNumChannels(){ return nChannels; }
   public int getBitsPerSec(){ return bps; }   

   public static WavReader construct(File file)
   {
      WavReader wav = new WavReader();
      if (!wav.load(file)) return null;
      return wav;
   }

   public int getFreq(){ return sampleRate; }
   
   public void close()
   {
      // stop playback
      if (playThread!=null)
      {
         playThread.stopAudio();
         playThread = null;
      }
      
      // Close the channel and the stream
      if (fc != null)
      {         
         try
         {
            
            fc.close();
         } catch (IOException e)
         {}
         fc = null;
         bb = null;
      }
   }

   protected boolean load(File file)
   {
      try
      {
         fc = new FileInputStream(file).getChannel();

         // Get the file's size and then map it into memory
         int szFile = (int)fc.size();
         // System.err.printf(" file size: %d\n", szFile);
         bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, szFile);
         bb.order(ByteOrder.LITTLE_ENDIAN);

         // read riff header
         byte[] riff = new byte[4];
         bb.get(riff);
         if (riff[0] != 'R' || riff[1] != 'I' || riff[2] != 'F' || riff[3] != 'F')
         {
            System.err.printf("Error: invalid RIFF header\n");
            return false;
         }

         int chunkSize = bb.getInt();

         byte[] wave = new byte[4];
         bb.get(wave);
         if (wave[0] != 'W' || wave[1] != 'A' || wave[2] != 'V' || wave[3] != 'E')
         {
            System.err.printf("Error: invalid WAVE header\n");
            return false;
         }

         // read fmt chunk
         byte[] fmt = new byte[4];
         bb.get(fmt);
         if (fmt[0] != 'f' || fmt[1] != 'm' || fmt[2] != 't' || fmt[3] != ' ')
         {
            System.err.printf("Error: invalid fmt header\n");
            return false;
         }

         int szChunk1 = bb.getInt();
         if (szChunk1 != 16)
         {
            System.err.printf("Error: Invalid chunk1 size for PCM: %d\n", szChunk1);
            return false;
         }

         short format = bb.getShort();
         if (format != 1)
         {
            System.err.printf("Error: Invalid format for PCM: %d\n", format);
            return false;
         }

         nChannels = bb.getShort();
         // System.err.printf(" nChannels: %d\n", nChannels);

         sampleRate = bb.getInt();
         // System.err.printf(" sample rate: %d\n", sampleRate);

         byteRate = bb.getInt();
         // System.err.printf(" byte rate: %d\n", byteRate);

         blockAlign = bb.getShort();
         // System.err.printf(" block align: %d\n", blockAlign);

         bps = bb.getShort();
         if (bps != 8 && bps != 16)
         {
            System.err.printf("Error: invalid bits per sample (only 8 & 16 are supported): %d\n", bps);
            return false;
         }
         // System.err.printf(" bits per sample: %d\n", bps);

         // read data chunk
         byte[] dat = new byte[4];
         bb.get(dat);
         if (dat[0] != 'd' || dat[1] != 'a' || dat[2] != 't' || dat[3] != 'a')
         {
            System.err.printf("Error: invalid data header\n");
            return false;
         }

         int szChunk2 = bb.getInt();
         nSamples = szChunk2 / (bps * nChannels / 8);
         // System.err.printf(" data size (bytes): %d (= %d samples)\n", szChunk2, nSamples);

         headerLen = bb.position();
         // System.err.printf(" headerLen: %d\n", headerLen);

         return true;
      } catch (Exception e)
      {
         e.printStackTrace();
         return false;
      }
   }

   public void stop()
   {      
      if (playThread!=null)
      {
         playThread.stopAudio();
         playThread = null;
      }
      else System.err.printf("no play thread to stop\n"); 
   }
   
   public short[] getData16(int iStartSample, int iStopSample)
   {
      assert(bps == 16);      
      AudioFormat audioFormat = getAudioFormat();
      int bytesPerSample = audioFormat.getFrameSize();
      int nPlaySamples = iStopSample - iStartSample;
      int nPlayBytes = nPlaySamples * bytesPerSample;
      int nFrameSize = audioFormat.getFrameSize();
      byte[] audioData = new byte[nPlayBytes];
      bb.position(headerLen + iStartSample*bytesPerSample);
      bb.get(audioData);
      ShortBuffer sb = bb.asShortBuffer();
      return sb.array();
   }
   
   public byte[] getData8(int iStartSample, int iStopSample)
   {
      assert(bps == 8);      
      AudioFormat audioFormat = getAudioFormat();
      int bytesPerSample = audioFormat.getFrameSize();
      int nPlaySamples = iStopSample - iStartSample;
      int nPlayBytes = nPlaySamples * bytesPerSample;
      int nFrameSize = audioFormat.getFrameSize();
      byte[] audioData = new byte[nPlayBytes];
      bb.position(headerLen + iStartSample*bytesPerSample);
      bb.get(audioData);
      return bb.array();
   }
   
   
   public boolean play(AudioGraph graph, int iStartSample, int iStopSample)
   {
      stop();
      
      try
      {
         assert (bps == 8 || bps == 16);
         
         AudioFormat audioFormat = getAudioFormat();
         int bytesPerSample = audioFormat.getFrameSize();
         int nPlaySamples = iStopSample - iStartSample;
         int nPlayBytes = nPlaySamples * bytesPerSample;
         System.err.printf("play %d -> %d (%d)\n", iStartSample, iStopSample, nPlaySamples);
         //System.err.printf(" #bytes=%d  bytesPerSample=%d\n", nPlayBytes, bytesPerSample);
         
         int nFrameSize = audioFormat.getFrameSize();
         byte[] audioData = new byte[nPlayBytes];
         bb.position(headerLen + iStartSample*bytesPerSample);
         bb.get(audioData);
         DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
         SourceDataLine sourceDataLine = (SourceDataLine)AudioSystem.getLine(dataLineInfo);
         playThread = new PlayThread(graph, audioFormat, iStartSample, audioData, sourceDataLine);
         playThread.start();
         return true;
      } catch (Exception e)
      {
         e.printStackTrace();
         return false;
      }
   }

   public AudioFormat getAudioFormat()
   {
      return new AudioFormat((float)sampleRate, bps, nChannels, bps==16, false);
   }

   public static void main(String args[])
   {
      WavReader wav = WavReader.construct(new File(args[0]));
      if (wav == null) return;
      System.err.println();
      // System.err.printf(" File: %s\n", args[0]);
      System.err.printf("     # Channels: %d\n", wav.nChannels);
      System.err.printf("  Sampling Rate: %d Hz\n", wav.sampleRate);
      System.err.printf("      Byte Rate: %d bps\n", wav.byteRate);
      System.err.printf("    Block Align: %d bytes\n", wav.blockAlign);
      System.err.printf("Bits Per Sample: %d bits\n", wav.bps);      
      System.err.printf("      # Samples: %d\n", wav.nSamples);
      System.err.printf("       Duration: %s\n", Library.formatDuration(1000*wav.nSamples/wav.sampleRate));
      System.err.println();
   }

   // ///////////////////////////////////////////////////////////////////

   class PlayThread extends Thread
   {
      protected AudioGraph graph;
      protected AudioFormat audioFormat;
      protected int offset;
      protected int nBufLen;
      protected boolean bPlay = true;
      protected byte[] audioData;
      protected SourceDataLine sourceDataLine;      
      
      public PlayThread(AudioGraph graph, AudioFormat audioFormat, int offset, byte[] _audioData, SourceDataLine sdl)
      {
         this.graph = graph;
         this.audioFormat = audioFormat;
         this.offset = offset;
         nBufLen = Math.max(1024, (int)(audioFormat.getFrameRate()/4));
         audioData = _audioData;
         sourceDataLine = sdl;
      }
      
      public void stopAudio(){ bPlay = false; System.err.printf("requested audio stop\n"); }

      public void run()
      {
         try
         {
            sourceDataLine.open(audioFormat);
            sourceDataLine.start();

            // TODO save current vert markers?
            
            long dms0 = Math.round(1000.0 * offset / audioFormat.getSampleRate()); 
            int i=0, n=audioData.length;
            while(i<n || sourceDataLine.isActive())
            {
               if (!bPlay)
               {
                  System.err.printf("Stopping Audio Playback\n");
                  break;
               }
               if (i<n){
                  int buflen = Math.min(nBufLen, n-i);
                  sourceDataLine.write(audioData, i, buflen);
                  i += buflen;
               }
               if (graph != null){
                  long dms = sourceDataLine.getMicrosecondPosition() / 1000;
                  long ms0 = graph.getStartTime();
                  graph.setVertLine(ms0+dms0+dms, Color.red, 1.0f);
                  Library.sleep(1);
               }
            }
            
            if (bPlay) sourceDataLine.drain();
            if (graph != null) graph.clearVertMarkers();
            sourceDataLine.stop();
            sourceDataLine.close();
         } catch (Exception e)
         {
            e.printStackTrace();
         }
      }
   }
}
