package kdm.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;
import java.util.*;
import kdm.util.*;

/**
 * A container that has multiple views; only one view is visible at a time, and the view is selected by
 * clicking on the corresponding title
 */
public class VertChooseContainer extends JMyComponent implements MouseListener
{
   // TODO: animate switching from one view to another

   /** represents a clickable label and associated pane */
   class Pane
   {
      protected String title;
      protected Component comp;
      protected JLabel lbTitle;

      public Pane(String _title, Component _comp, VertChooseContainer parent)
      {
         title = _title;
         comp = _comp;
         lbTitle = new JLabel(title, JLabel.CENTER);
         lbTitle.setBorder(BorderFactory.createRaisedBevelBorder());
         lbTitle.addMouseListener(parent);
         parent.add(lbTitle);
         parent.add(comp);
      }

      public String getTitle()
      {
         return title;
      }

      public JLabel getLabel()
      {
         return lbTitle;
      }

      public Component getPanel()
      {
         return comp;
      }
   }

   protected ArrayList<Pane> kids;
   protected int iCurViz;
   protected Color cTab = new Color(200,200,200);

   public VertChooseContainer()
   {
      kids = new ArrayList<Pane>();
      iCurViz = 0;
      setLayout(null);
      setBackground(Color.gray);
   }

   public void addPane(String title, Component comp)
   {
      Pane pane = new Pane(title, comp, this);
      kids.add(pane);
   }

   public boolean show(String title)
   {
      for(int i = 0; i < kids.size(); i++){
         if (kids.get(i).getTitle().equals(title)){
            show(i);
            return true;
         }
      }
      return false;
   }

   public void show(int iPane)
   {
      iCurViz = iPane;
      revalidate();
   }

   public int getLabelHeight()
   {
      return 24; // TODO
   }

   @Override
   public void paintComponent(Graphics2D g, int w, int h)
   {
      int hlab = getLabelHeight();
      int nKids = kids.size();

      int yTop = (iCurViz + 1) * hlab;
      int yBot = Math.max(h - (nKids - iCurViz - 1) * hlab, yTop);

      g.setColor(cTab);
      g.fillRect(0, 0, w, yTop);
      g.fillRect(0, yBot, w, h - yBot);      
   }

   public void doLayout()
   {
      Insets ins = getInsets();
      int cx = ins.left;
      int cy = ins.top;
      int w = getWidth() - (ins.left + ins.right);
      int h = getHeight() - (ins.top + ins.bottom);
      int hlab = getLabelHeight();
      int nKids = kids.size();

      int yTop = (iCurViz + 1) * hlab;
      int yBot = Math.max(h - (nKids - iCurViz - 1) * hlab, yTop);

      int y = 0;
      for(int i = 0; i < nKids; i++){
         Pane pane = kids.get(i);
         if (i == iCurViz){
            pane.getLabel().setBounds(cx, cy + y, w, hlab);
            y += hlab;
            pane.getPanel().setBounds(cx, cy + y, w, yBot - y);
            pane.getPanel().setVisible(true);
            y = yBot;
         }
         else{
            pane.getLabel().setBounds(cx, cy + y, w, hlab);
            pane.getPanel().setVisible(false);
            y += hlab;
         }
      }
   }

   public Dimension getMinimumSize()
   {
      Dimension dim = new Dimension(0, 0);
      for(Pane pane : kids){
         Dimension kd = pane.getPanel().getMinimumSize();
         if (kd.width > dim.width) dim.width = kd.width;
         if (kd.height > dim.height) dim.height = kd.height;

         kd = pane.getLabel().getMinimumSize();
         if (kd.width > dim.width) dim.width = kd.width;
      }
      if (!kids.isEmpty()) dim.height += kids.size() * kids.get(0).getLabel().getMinimumSize().height;
      return dim;
   }

   public Dimension getPreferredSize()
   {
      Dimension dim = new Dimension(0, 0);
      for(Pane pane : kids){
         Dimension kd = pane.getPanel().getPreferredSize();
         if (kd.width > dim.width) dim.width = kd.width;
         if (kd.height > dim.height) dim.height = kd.height;

         kd = pane.getLabel().getPreferredSize();
         if (kd.width > dim.width) dim.width = kd.width;
      }
      if (!kids.isEmpty()) dim.height += kids.size() * kids.get(0).getLabel().getPreferredSize().height;
      return dim;
   }

   public static void main(String args[])
   {
      JFrame frame = new JFrame("VCC Tester");
      frame.setSize(300, 500);
      frame.setLocation(200, 100);
      VertChooseContainer vcc = new VertChooseContainer();
      vcc.addPane("View 1", new ColoredComp(Color.red));
      vcc.addPane("View 2", new ColoredComp(Color.blue));
      vcc.addPane("View 3", new ColoredComp(Color.green));
      vcc.addPane("View 4", new ColoredComp(Color.magenta));
      vcc.addPane("View 5", new ColoredComp(Color.orange));
      frame.add(vcc);
      frame.setVisible(true);
   }

   public void mouseClicked(MouseEvent e)
   {
   // TODO Auto-generated method stub

   }

   public void mousePressed(MouseEvent e)
   {
      Object src = e.getSource();
      int iPane = 0;
      for(; iPane < kids.size(); iPane++){
         Pane pane = kids.get(iPane);
         if (pane.getLabel() == src){
            if (iPane != iCurViz) show(iPane);
            break;
         }
      }
   }

   public void mouseReleased(MouseEvent e)
   {
   // TODO Auto-generated method stub

   }

   public void mouseEntered(MouseEvent e)
   {
   // TODO Auto-generated method stub

   }

   public void mouseExited(MouseEvent e)
   {
   // TODO Auto-generated method stub

   }
}
