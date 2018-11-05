package kdm.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import javax.swing.*;
import kdm.data.*;
import kdm.util.*;
import kdm.mlpr.*;

public class SubseqTreeView extends JPanel implements MouseListener, MouseMotionListener
{   
   public final static Stroke stroke1 = new BasicStroke(1.0f);
   public final static int xmargin = 12;
   public final static int ymargin = 4;
   public final static int nodeDiam = 7;
   public final static float MaxW = 5.0f;
   public final static float MinW = 0.3f;
   
   public final static Color colorMoveUp = new Color(.8f, .8f, .9f);
   public final static Color colorLiveNode = Color.BLUE;
   public final static Color colorDeadNode = Color.RED;
 
   protected SubseqTree tree;
   protected SubseqNode vizRoot;
   protected boolean bMoveUp = false;   
   protected ArrayList<Pair<Point, SubseqNode>> nodes;
   protected Pair<Point, SubseqNode> nodeHighlight;
   protected JTextField tfStatus;
   protected boolean bVertical = false;
   protected ArrayList<SubseqTreeSelectListener> listeners;
   
   public SubseqTreeView(SubseqTree _tree)
   {      
      super(new BorderLayout());
      setTree(_tree);      
      setOpaque(true);
      addMouseListener(this);
      addMouseMotionListener(this);
      tfStatus = new JTextField();
      tfStatus.setEditable(false);      
      add(tfStatus, BorderLayout.SOUTH);
      listeners = new ArrayList<SubseqTreeSelectListener>();
   }   
   
   public void setTree(SubseqTree _tree)
   {
      tree = _tree;
      vizRoot = tree.getRoot();
      bMoveUp = false;
      nodeHighlight = null;
      repaint();
   }
   
   /**
    * Display the tree vertically (top->bottom) or horizontally (left->right)
    * @param b true for vertical, false for horizontal
    */
   public void setVertical(boolean b)
   {
      if (b != bVertical)
      {
         bVertical = b;
         repaint();
      }
   }
   
   public void paintComponent(Graphics g)
   {          
      Graphics2D g2 = (Graphics2D)g;
      g = null;
      Dimension dims = getSize();
      
      OrientationHelper ohelp = new OrientationHelper(g2, dims, 0, 0, 0, tfStatus.getHeight());
      if (bVertical) ohelp.tran(g2, dims);

      updateStatus();
      
      g2.setColor(Color.white);
      g2.fillRect(xmargin, 0, dims.width, dims.height);
      if (bMoveUp) g2.setColor(colorMoveUp);
      g2.fillRect(0, 0, xmargin, dims.height);
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      
      if (vizRoot != null)
      {
         // draw grid
         g2.setStroke(stroke1);
         g2.setColor(Color.lightGray);
         int theight = vizRoot.getHeight();
         if (theight > 0)
         {
            double xgap = Math.max((double)(dims.width-2*xmargin) / theight, 1);
            for(int x=xmargin, i=0; x<dims.width; i++)
            {
               g2.drawLine(x, 0, x, dims.height);
               x = xmargin + (int)Math.round(xgap*(i+1));
            }
         }

         // draw highlight
         if (nodeHighlight != null)
         {
            int w = nodeDiam + 8;
            Point pt = nodeHighlight.first;
            g2.setColor(Color.yellow);
            g2.fillOval(pt.x-w/2, pt.y-w/2, w,w);
            g2.setColor(Color.gray);
            g2.drawOval(pt.x-w/2, pt.y-w/2, w,w);
         }
         
         // draw the tree
         nodes = new ArrayList<Pair<Point,SubseqNode>>();
         renderTree(g2, vizRoot, new Rectangle(xmargin, ymargin, dims.width - 2 * xmargin,
               dims.height - 2 * ymargin));

         // draw tree name so far
         if (vizRoot != tree.getRoot())
         {
            String sPrefix = getPrefix(false);
            g2.setColor(Color.black);
            g2.setFont(new Font(null, Font.BOLD, 20));            
            FontMetrics fm = g2.getFontMetrics();
            int fh = fm.getHeight();
            if (bVertical)
            {
               ohelp.reset(g2, dims);
               g2.drawString(sPrefix, dims.width/2 + 12, fm.getAscent()-2);
               ohelp.tran(g2, dims);
            }
            else{
               int y = ymargin + dims.height/2 - sPrefix.length() * fh + 4;
               for(int i=0; i<sPrefix.length(); i++)
                  g2.drawString(""+sPrefix.charAt(i), 8, y+i*fh);
            }
         }
      }

      if (bVertical) ohelp.reset(g2, dims);
   }
   
   /**
    * Render a subtree
    * 
    * @param g graphics context to use
    * @param node node in tree to render (plus chid nodes)
    * @param r rectangle in which to draw tree
    */
   protected void renderTree(Graphics2D g, SubseqNode node, Rectangle r)
   {
      int theight = node.getHeight();
      int nKids = node.getNumKids();      
      int h2 = r.height/2;            
      int nd2 = nodeDiam/2;
      
      double xgap, ygap;
      if (theight > 0 && r.width>0 && r.height>0)
      {
         xgap = (double)r.width / theight;
         ygap = (double)r.height / nKids;
      
         // compute the total number of sequences that pass this way
         int nSeqs = 0;
         for(int i = 0; i < nKids; i++)
         {
            SubseqNode kid = node.getKid(i);
            if (kid == null) continue;
            nSeqs += kid.getNumSeqs();
         }
         
         for(int i = 0; i < nKids; i++)
         {
            SubseqNode kid = node.getKid(i);
            if (kid == null) continue; // TODO: draw anyway?
            int x = r.x + (int)xgap;
            int y = r.y + (int)Math.round(ygap*i);
            int w = r.width - (int)xgap;
            int h = (int)ygap;         
            
            g.setColor(Color.black);
            if (nSeqs<=1) g.setStroke(stroke1);
            else{
               float frac = (float)kid.getNumSeqs() / nSeqs;
               g.setStroke(new BasicStroke((MaxW-MinW)*frac+MinW));
            }
            g.drawLine(r.x, r.y+h2, x, y+h/2);
            renderTree(g, kid, new Rectangle(x, y, w, h));
         }
      }
      
      // draw oval for this node
      g.setStroke(stroke1);
      if (node.getNumSeqs()>0 || !node.isVisible())
      {         
         g.setColor(node.isVisible() ? colorLiveNode : colorDeadNode);
         g.fillOval(r.x-nd2, r.y+h2-nd2, nodeDiam, nodeDiam);
      }
      g.setColor(Color.black);
      g.drawOval(r.x-nd2, r.y+h2-nd2, nodeDiam, nodeDiam);
      nodes.add(new Pair(new Point(r.x, r.y+h2), node));
   }

   /**
    * Find the closest node to the given point
    * @param pt query point
    * @return the coordinates of the node and the node itself
    */
   protected Pair findClosestNode(Point pt)
   {
      if (tree==null || nodes==null) return null;
      
      if (bVertical) Library.swap(pt);
      
      int iBest = -1;
      double dBest = Library.INF;
      for(int i=0; i<nodes.size(); i++)
      {
         Pair<Point,SubseqNode> pn = nodes.get(i);
         double d = pt.distanceSq(pn.first);
         if (d < dBest){ dBest = d; iBest = i; }
      }
      
      if (bVertical) Library.swap(pt);
      return (iBest>=0 ? nodes.get(iBest) : null);
   }
   
   protected String getPrefix(boolean bPad)
   {
      StringBuffer sb = new StringBuffer();      
      SubseqNode node = vizRoot;
      while(!node.isRoot())
      {
         sb.insert(0, (char)(node.getSymbol() + 'a'));
         node = node.getParent();         
      }      
      if (bPad)
      {
         int theight = tree.getHeight();
         while(sb.length() < theight) sb.append('-');
      }
      return sb.toString();
   }
   
   public void addListener(SubseqTreeSelectListener l)
   {
      listeners.add(l);
   }
   
   public void fireSelect(int[] seq)
   {
      for(SubseqTreeSelectListener l : listeners) l.select(seq);
   }
   
   /**
    * Update the status text based on the current state
    */
   protected void updateStatus()
   {
      if (tree == null) return;
      
      StringBuffer sb = new StringBuffer();
      SubseqNode node;
      int i;
      int theight = tree.getHeight();
      
      // tree string      
      sb.append(String.format("Tree: %s (%d)", getPrefix(true), vizRoot.getNumSeqs()));
      
      // node string            
      if (nodeHighlight != null)
      {
         char stree[] = new char[theight];
         Arrays.fill(stree, '-');
         node = nodeHighlight.second;
         i = theight - node.getHeight();         
         while(!node.isRoot())
         {
            stree[--i] = (char)(node.getSymbol() + 'a');
            node = node.getParent();         
         }
         sb.append(String.format("  Node: %s (%d)", new String(stree), nodeHighlight.second.getNumSeqs()));
      }
            
      tfStatus.setText(sb.toString());
   }
   
   public void mouseClicked(MouseEvent e)
   {      
   }

   public void mousePressed(MouseEvent e)
   {  
      if (e.getButton() == MouseEvent.BUTTON1)
      {
         if (bMoveUp)
         {
            vizRoot = vizRoot.getParent();
            nodeHighlight = null;
            bMoveUp = !vizRoot.isRoot();
            repaint();
         }
         else if (nodeHighlight != null)
         {         
            vizRoot = nodeHighlight.second;
            if (vizRoot.isLeaf()) vizRoot = vizRoot.getParent();
            nodeHighlight = null;
            repaint();
         }
      }
      else{
         if (nodeHighlight != null)
         {
            fireSelect(nodeHighlight.second.getSeq());       
         }
      }
   }
   
   public void mouseReleased(MouseEvent e)
   {  
   }

   public void mouseEntered(MouseEvent e)
   {  
   }

   public void mouseExited(MouseEvent e)
   {  
      bMoveUp = false;
      nodeHighlight = null;
      repaint();
   }

   public void mouseDragged(MouseEvent e)
   {  
   }   
   
   public void mouseMoved(MouseEvent e)
   {    
      Point pt = e.getPoint();
      
      boolean b;
      Pair<Point, SubseqNode> pn = findClosestNode(pt);
            
      // move up highlight?
      b = (((bVertical && pt.y < xmargin) || (!bVertical && pt.x<xmargin)) && vizRoot!=tree.getRoot()) || (pn!=null && pn.second==vizRoot);
      if (bMoveUp != b && !vizRoot.isRoot())
      {
         repaint();
         bMoveUp = b;
         nodeHighlight = null;                  
      }
      
      // highlight node      
      if (pn!=null && !pn.equals(nodeHighlight))
      {
         if (pn.second.isRoot()) nodeHighlight = null;
         else nodeHighlight = pn;
         repaint();
      }
   }
      
}
