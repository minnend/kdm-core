package kdm.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

import kdm.data.*;
import kdm.io.*;
import kdm.util.ConfigHelper;
import kdm.util.Library;

/**
 * A line graph that represents audio. This adds the functionality of being able to play
 * the audio.
 */
public class AudioGraph extends LineGraph implements ActionListener, KeyListener
{
   public static final String sPlayAudio = "Play Audio";
   public static final String sStopAudio = "Stop Audio";
   
   protected transient TimeX dragStart;
   protected WindowLocation wPlay;
   protected WavReader wav;
   
   // TODO better playback -- seems to clip end of file
   // TODO visual feedback during playback -- vertical line showing current location
   
   public AudioGraph()
   {
      this(null);
   }

   public AudioGraph(Sequence _data)
   {
      super(_data);        
      addKeyListener(this);
      setFocusable(true);
   }
   
   public void setData(Sequence _data)
   {
      super.setData(_data);

      if (data!=null && data.getOrigFile() != null)
      {
         File file = new File(data.getOrigFile());
         if (!file.exists())
         {
            System.err.printf("Error: original audio file does not exist\n (%s)\n", data.getOrigFile());
            System.exit(1);
         }
         wav = WavReader.construct(file);
      }
   }
   
   public JPopupMenu buildPopup(boolean bAppend)
   {
      JPopupMenu menu = super.buildPopup(false);
      JMenuItem item;      
      item = new JMenuItem(sPlayAudio);
      item.addActionListener(this);
      menu.add(item);
      item = new JMenuItem(sStopAudio);
      item.addActionListener(this);
      menu.add(item);      
      if (bAppend) appendPopup(menu);
      return menu;
   }
   
   public void mousePressed(MouseEvent e)
   {      
      super.mousePressed(e);
      if (e.isPopupTrigger() && wPlay!=null) showPopup(buildPopup(true), e);
      else{
         if (SwingUtilities.isLeftMouseButton(e))
         {
            wPlay = null;
            clearHighlights();
            if (mouseTime != null) dragStart = new TimeX(mouseTime);
            else dragStart = null;
         }
         requestFocusInWindow();
      }
   }
   
   public void mouseReleased(MouseEvent e)
   {
      super.mouseReleased(e);      
      if (e.isPopupTrigger() && wPlay!=null) showPopup(buildPopup(true), e);
   }
   
   public void mouseDragged(MouseEvent e)   
   {
      super.mouseDragged(e);
      if (SwingUtilities.isLeftMouseButton(e))
      {
         TimeMarker tm;
         if (dragStart.index < mouseTime.index)
         {
             wPlay = new WindowLocation(dragStart.index, mouseTime.index-dragStart.index);
             tm = new TimeMarker(TimeMarker.Units.Time, dragStart.time, mouseTime.time);
         }
         else if (dragStart.index > mouseTime.index)
         {
            wPlay = new WindowLocation(mouseTime.index, dragStart.index-mouseTime.index);
            tm = new TimeMarker(TimeMarker.Units.Time, mouseTime.time, dragStart.time);
         }
         else{
            wPlay = null;
            tm = null;
         }
         highlight(tm);
      }
   }

   public void actionPerformed(ActionEvent e)
   {
      String cmd = e.getActionCommand();
      
      if (cmd.equals(sPlayAudio)) play();
      else if (cmd.equals(sStopAudio))
      {
         if (wav!=null) wav.stop();
      }
   }
   
   protected void play()
   {
      if (wav!=null && wPlay!=null)
      {
         //System.err.printf("play: %s   %s\n", data.getOrigFile(), wPlay);
         double scale = wav.getFreq() / data.getFreq();
         int iStart = (int)(wPlay.start()*scale);
         int iStop = (int)(wPlay.end()*scale);
         wav.play(this, iStart, iStop);
      }
   }

   public void keyPressed(KeyEvent e)
   {
      if (e.getKeyCode() == KeyEvent.VK_SPACE) play();
   }

   public void keyReleased(KeyEvent e)
   {
   }

   public void keyTyped(KeyEvent e)
   {
   }
}
