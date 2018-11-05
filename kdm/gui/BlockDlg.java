package kdm.gui;

import java.awt.*;

import javax.swing.*;

import kdm.util.*;

/**
 * A BlockDlg is a dialog box that is displayed while a worker thread executes. During this time, the owning
 * JFrame is disabled to preclude additional user interaction.
 * 
 * @author David Minnen
 */
public class BlockDlg extends JFrame
{
   public final static int DEF_WIDTH_LABEL = 240;

   public final static int DEF_HEIGHT_LABEL = 120;

   public final static int DEF_WIDTH_MULTI = 480;

   public final static int DEF_HEIGHT_MULTI = 240;

   protected JFrame owner;

   protected JPanel mainp;

   protected JTextArea ta;

   protected Thread thread;

   protected boolean bShow;

   protected Component oldGlassPane;

   /**
    * Construct a block dialog
    * 
    * @param _owner the owning frame that will be disabled while the dialog's worker thread executes
    * @param _bShow should the dlg box be displayed?
    */
   public BlockDlg(JFrame _owner, boolean _bShow)
   {
      // setup and construct the dialog frame
      owner = _owner;
      bShow = _bShow;
      setUndecorated(true);

      // create the main panel
      mainp = new JPanel(new BorderLayout());
      mainp.setBorder(BorderFactory.createRaisedBevelBorder());
      mainp.setOpaque(true);
      setContentPane(mainp);

      // create the text area
      ta = new JTextArea();
      ta.setEditable(false);
      ta.setBorder(BorderFactory.createLoweredBevelBorder());

      setAlwaysOnTop(true);
   }

   /**
    * Position the dialog box so that it is centered over its owner frame
    * 
    * @param w the desired width of the dialog box
    * @param h the desired height of the dialog box
    * @param _bShow display the dialog box?
    */
   protected void positionWin(int w, int h, boolean _bShow)
   {
      Rectangle bounds = owner.getBounds();
      int x = bounds.x + (bounds.width - w) / 2;
      int y = bounds.y + (bounds.height - h) / 2;
      setLocation(x, y);
      setSize(w, h);
      setVisible(_bShow);
   }

   /**
    * Display a multi-line dialog; use print and println to set the message text
    * 
    * @param _thread the worker thread to execute
    * @param _bShow display the dlg box (overrides member variable)?
    */
   public void display(Thread _thread, boolean _bShow)
   {
      // disable the parent frame
      owner.setEnabled(false); // TODO should only disable content

      // create the content of the dialg
      mainp.removeAll();
      ta.setText("");
      JScrollPane sp = new JScrollPane(ta);
      sp.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
      mainp.add(sp, BorderLayout.CENTER);

      // center and display the dlg
      positionWin(DEF_WIDTH_MULTI, DEF_HEIGHT_MULTI, _bShow);

      // start the worker thread
      thread = _thread;
      try{
         SwingUtilities.invokeAndWait(thread);
         // thread.start();
      } catch (Exception e){
         e.printStackTrace();
      }
   }

   /**
    * Display a multi-line dialog
    * 
    * @param _thread the worker thread to execute
    */
   public void display(Thread _thread)
   {
      display(_thread, bShow);
   }

   /**
    * Append a line of text to a multi-line dialog and add a trailing newline.
    * 
    * @param line the text to add
    */
   public void println(String line)
   {
      ta.append(line + "\n");
   }

   /**
    * Append a line of text to a multi-line dialog without adding a trailing newline.
    * 
    * @param line the text to add
    */
   public void print(String line)
   {
      ta.append(line);
   }

   /**
    * Display a single line message and start the worker thread
    * 
    * @param msg the message to display
    * @param _thread worker thread (should call BlockDlg.close() at end)
    */
   public void display(String msg, Thread _thread)
   {
      display(msg, _thread, bShow);
   }

   /**
    * Display a single line message and start the worker thread
    * 
    * @param msg the message to display
    * @param _thread worker thread (should call BlockDlg.close() at the end)
    * @param _bShow display the dlg box (overrides member variable)
    */
   public void display(final String msg, final Thread _thread, final boolean _bShow)
   {
      // disable the parent frame
      try{
         SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
               owner.setEnabled(false);
               oldGlassPane = owner.getGlassPane();
               ColoredComp cc = new ColoredComp(new Color(0, 0, 0, 0.25f));
               owner.setGlassPane(cc);
               cc.setVisible(true);

               // create the content of the dialg
               mainp.removeAll();
               JLabel label = new JLabel(msg, JLabel.CENTER);
               mainp.add(label, BorderLayout.CENTER);

               // center and display the dlg
               int w = label.getMinimumSize().width + 24;
               positionWin(w, DEF_HEIGHT_LABEL, _bShow);

               // start the worker thread
               thread = _thread;
               thread.start();
            }
         });
      } catch (Exception e){}
   }

   /**
    * Wait for the worker thread to end
    */
   public void waitForMe()
   {
      while(thread != null && thread.isAlive())
         Library.sleep(50);
   }

   /**
    * Close the block dialog box by hiding it and re-enabling the parent frame
    */
   public void close()
   {      
      owner.getGlassPane().setVisible(false);
      owner.setGlassPane(oldGlassPane);
      owner.setEnabled(true);
      setAlwaysOnTop(false);
      if (isVisible()) setVisible(false);
   }
}
