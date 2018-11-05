package kdm.gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

/**
 * A compound component with a label above the slider. The label will report the current
 * value of the slider (use %d somewhere in the string).
 */
public class LabeledSlider extends JPanel implements ChangeListener
{
   protected String sLabel;
   protected JLabel label;
   protected JSlider slider;
   protected boolean bValueAtTicks = false;
   protected ArrayList<ChangeListener> listeners;

   public LabeledSlider(String _sLabel)
   {
      this(_sLabel, 1, 10);      
   }

   public LabeledSlider(String _sLabel, int min, int max)
   {
      sLabel = _sLabel;
      label = new JLabel(String.format(sLabel, min), JLabel.CENTER);      
      slider = new JSlider(JSlider.HORIZONTAL, min, max, min);
      slider.addChangeListener(this);
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      add(label);
      add(Box.createVerticalStrut(1));
      add(slider);
      listeners = new ArrayList<ChangeListener>();
   }
   
   @Override
   public void setEnabled(boolean bEnabled)
   {
      label.setEnabled(bEnabled);
      slider.setEnabled(bEnabled);
   }
   
   public void setText(String s)
   {
      sLabel = s;
      updateLabel();
   }
   
   public void setValueAtTicks(boolean b){ bValueAtTicks = b; }

   public JSlider getSlider()
   {
      return slider;
   }
   
   public int getValue(){ return slider.getValue(); }
   public void setValue(int val){ slider.setValue(val); }

   public void setValues(int val, int vmin, int vmax)
   {
      slider.setMinimum(vmin);
      slider.setMaximum(vmax);
      slider.setValue(val);
   }
   
   public int getMaximum(){ return slider.getMaximum(); }
   public int getMinimum(){ return slider.getMinimum(); }
   
   protected void updateLabel()
   {
      int x = slider.getValue();
      if (bValueAtTicks) // TODO: should this effect the actual value as well?
      {
         int tick = Math.max(1, slider.getMinorTickSpacing());      
         int xmin = slider.getMinimum();
         x = xmin + ((x + tick/2 - xmin)/tick)*tick;
      }
      label.setText(String.format(sLabel, x));
   }
   
   public void stateChanged(ChangeEvent e)
   {
      updateLabel();
      
      // forward modified event to listeners
      ChangeEvent e2 = new ChangeEvent(this);
      for(ChangeListener cl : listeners)
         cl.stateChanged(e2);
   }
   
   public void addChangeListener(ChangeListener l)
   {
      listeners.add(l);
   }   
}
