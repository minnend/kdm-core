package kdm.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import java.util.*;
import kdm.util.*;

/**
 * A component with a combo box that selects amongst different sub-panels
 */
public class ComboPanel extends JPanel implements ActionListener
{
   protected JComboBox cb;
   protected HashMap<String, Component> compMap;
   protected Component compCenter;
   protected ArrayList<ActionListener> listeners;

   public ComboPanel()
   {
      setLayout(new BorderLayout());
      compMap = new HashMap<String, Component>();
      cb = new JComboBox();
      cb.addActionListener(this);
      listeners = new ArrayList<ActionListener>();
      add(cb, BorderLayout.NORTH);
   }

   public ComboPanel(String sTitle)
   {
      this();
      JLabel lbTitle = new JLabel(sTitle);
      JPanel p = new JPanel();
      p.add(lbTitle);
      p.add(cb);
      add(p, BorderLayout.NORTH);
   }

   public JComboBox getComboBox()
   {
      return cb;
   }

   public void addPanel(String s, Component comp)
   {
      compMap.put(s, comp);
      cb.addItem(s);
      if (compMap.isEmpty() && comp != null){
         add(comp, BorderLayout.CENTER);
         compCenter = comp;
      }
   }

   public int getSelectedIndex()
   {
      return cb.getSelectedIndex();
   }

   public Object getSelectedItem()
   {
      return cb.getSelectedItem();
   }

   public Object getItemAt(int i)
   {
      return cb.getItemAt(i);
   }

   public void setSelectedIndex(int ix)
   {
      cb.setSelectedIndex(ix);
   }

   public void actionPerformed(ActionEvent e)
   {
      String s = (String)cb.getSelectedItem();
      Component comp = compMap.get(s);
      if (compCenter != null) remove(compCenter);
      if (comp != null) add(comp, BorderLayout.CENTER);
      Container parent = getParent();
      if (parent != null){
         if (parent instanceof JComponent) ((JComponent)parent).revalidate();
         else{
            parent.invalidate();
            parent.validate();            
         }
      }
      repaint();
      compCenter = comp;

      // forward (modified) event to listeners
      e.setSource(this);
      for(ActionListener listener : listeners)
         listener.actionPerformed(e);
   }

   public void addActionListener(ActionListener listener)
   {
      listeners.add(listener);
   }
}
