package kdm.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/** simple tabbed container where the "tabs" are buttons */
public class TabButtonPane extends JComponent implements ActionListener
{
   protected JPanel pPane, pButtons;
   protected JToggleButton btCur;
   protected HashMap<String,JComponent> map;

   public TabButtonPane()
   {
      setLayout(new BorderLayout());
      pPane = new JPanel(new GridLayout(1,1));
      pButtons = new JPanel();
      pButtons.setLayout(new BoxLayout(pButtons, BoxLayout.X_AXIS));
      pButtons.setBorder(BorderFactory.createEmptyBorder(2,4,2,4));
      add(pPane, BorderLayout.CENTER);
      add(pButtons, BorderLayout.SOUTH);
      map = new HashMap<String, JComponent>();
   }
   
   public void add(String title, JComponent comp)
   {
      map.put(title, comp);
      JToggleButton bt = new JToggleButton(title);    
      bt.addActionListener(this);
      pButtons.add(bt);      
      pButtons.add(Box.createHorizontalStrut(4));
      if (getButtons().size()==1){
         bt.setSelected(true);
         pPane.removeAll();
         pPane.add(comp);
         btCur = bt;
      }
   }
   
   public boolean select(int ix)
   {
      Vector<JToggleButton> vec = getButtons();
      select(vec.get(ix));
      return true;
   }
   
   public boolean select(String s)
   {
      for(JToggleButton bt : getButtons())
      {
         if (bt.getText().equals(s)){
            select(bt);
            return true;
         }
      }
      return false;
   }
   
   protected Vector<JToggleButton> getButtons()
   {
      Component[] comps = pButtons.getComponents();
      Vector<JToggleButton> vec = new Vector<JToggleButton>();
      for(Component comp : comps){
         if (comp instanceof JToggleButton) vec.add((JToggleButton)comp);
      }
      return vec;
   }
   
   protected void select(JToggleButton bt)
   {
      // turn off old selected button      
      btCur.setSelected(false);
      btCur = bt;
      
      // update the view
      pPane.removeAll();
      pPane.add(map.get(bt.getText()));
      pPane.revalidate();
      pPane.repaint();
   }

   public void actionPerformed(ActionEvent e)
   {      
      Object src = e.getSource();
      
      // if click on selected button, do nothing
      if (src == btCur){
         btCur.setSelected(true);
         return;
      }

      select((JToggleButton)src);
   }
}
