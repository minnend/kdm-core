package kdm.gui;

import kdm.data.*;
import kdm.io.*;
import kdm.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.util.*;
import java.io.*;

import static kdm.data.TimeMarker.*;

/** GUI component that allows interaction with temporal labels */
public class EventBar extends JMyComponent implements GraphListener, MouseListener, MouseMotionListener,
      ActionListener
{
   // TODO detect changes and notify user if exit without save

   public enum MarkerState {
      None, Move, Adjust
   }
   
   protected Graph graph;
   protected Vector<Graph> glist;
   protected Vector<GraphListener> glisteners;
   protected transient TimeX mouseTime = null;
   protected MarkupSet marks;
   protected transient TimeMarker liveMarker = null;
   protected transient int liveBase;
   protected transient boolean bNewMarker = false;
   protected transient boolean bShiftAll = false;
   protected transient MarkerState mstate = MarkerState.None;
   protected Color colorEvent, colorInterval;
   protected GraphScrollPane scrollPane;
   protected int mousex;
   protected int eventHalfWidth = 8;
   protected int intervalEndWidth = 6;
   protected boolean bRenderHighlights = true;
   protected boolean bAntiAlias = false;

   protected static final String sRefreshLabels = "Refresh Labels";
   protected static final String sLoadLabels = "Load Labels";
   protected static final String sSaveLabels = "Save Labels";
   protected static final String sSaveLabelsAs = "Save Labels As";
   protected static final String sShiftAll = "Shift All Markers";
   protected static final String sRemoveEvent = "Remove Event";
   protected static final String sRemoveInterval = "Remove Interval";
   protected static final String sRenameEvent = "Rename Event";
   protected static final String sRenameInterval = "Rename Interval";
   protected static final String sRenderHighlights = "Draw Interval Highlights";

   public EventBar(Graph _graph)
   {
      this(_graph, new MarkupSet());
   }

   public EventBar(Graph _graph, MarkupSet _marks)
   {
      graph = _graph;
      glist = new Vector<Graph>();
      glisteners = new Vector<GraphListener>();
      marks = (_marks != null ? _marks : new MarkupSet());

      addMouseListener(this);
      addMouseMotionListener(this);

      setBackground(Color.lightGray);
      setForeground(Color.black);
      colorEvent = Color.red;
      colorInterval = Color.blue;
   }
   
   public void setScrollPane(GraphScrollPane _gsp)
   {
      scrollPane = _gsp;
      scrollPane.addEventBar(this);
   }

   public void addGraph(Graph graph)
   {
      graph.addGraphListener(this);
      if (!glist.contains(graph)) glist.add(graph);
   }

   public void addGraphListener(GraphListener gl)
   {
      if (!glisteners.contains(gl)) glisteners.add(gl);
   }

   public MarkupSet getMarks()
   {
      return marks;
   }

   public void setMarks(MarkupSet _marks)
   {
      marks = _marks;
      repaint();
   }

   /**
    * Draws a time marker representing either an event or an interval using the globally defined colors.
    */
   protected void renderMarker(Graphics g, TimeMarker marker, Color colorBorder)
   {
      int h = getHeight();
      int h2 = h / 2;

      if (marker.isEvent())
      {
         Polygon polyEvent = new Polygon();
         polyEvent.addPoint(0, 0);
         polyEvent.addPoint(eventHalfWidth, h);
         polyEvent.addPoint(-eventHalfWidth, h);

         int x = graph.getXFromTime(marker.getStart());
         polyEvent.translate(x, 0);
         g.setColor(colorEvent);
         g.fillPolygon(polyEvent);
         g.setColor(colorBorder);
         g.drawPolygon(polyEvent);
      }
      else
      { // render the interval
         Polygon polyStart = new Polygon();
         polyStart.addPoint(0, 0);
         polyStart.addPoint(intervalEndWidth, h2);
         polyStart.addPoint(0, h);

         Polygon polyStop = new Polygon();
         polyStop.addPoint(0, 0);
         polyStop.addPoint(-intervalEndWidth, h2);
         polyStop.addPoint(0, h);

         int a = graph.getXFromTime(marker.getStart());
         int b = graph.getXFromTime(marker.getStop());

         polyStart.translate(a, 0);
         polyStop.translate(b, 0);

         g.setColor(colorInterval);
         g.fillPolygon(polyStart);
         g.fillPolygon(polyStop);
         g.drawLine(a, h2, b, h2);

         // render the name of the interval
         if ((marker.getTag() != null && marker.getTag().length() > 0) || liveMarker != null)
         {
            Rectangle oldClip = g.getClipBounds();
            int x = (int)Math.max(oldClip.x, a + intervalEndWidth + 4);
            int w = (int)Math.min(oldClip.x + oldClip.width, b - intervalEndWidth - 4) - x;
            g.setClip(x, 0, w, h);
            FontMetrics fm = g.getFontMetrics();
            String sTitle = marker.getTag();
            if (sTitle == null) sTitle = "";
            if (liveMarker == marker)
            {
               if (marker.getTag() == null) sTitle = Library.formatDuration(liveMarker.length());
               else sTitle = String.format("%s: %s", sTitle, Library.formatDuration(liveMarker.length()));
            }
            int sw = fm.stringWidth(sTitle);
            x += (w - sw) / 2;
            int sh = fm.getAscent();
            int y = (h + sh) / 2;
            g.setColor(getBackground());
            g.fillRect(x - 1, y - sh - 1, sw + 2, sh + 2);
            g.setColor(colorInterval);
            g.drawString(sTitle, x, y);
            g.setClip(oldClip.x, oldClip.y, oldClip.width, oldClip.height);
         }

         // draw the black outline on the interval endpoints
         g.setColor(colorBorder);
         g.drawPolygon(polyStart);
         g.drawPolygon(polyStop);
      }
   }

   /**
    * Returns the index of the time marker located at the given x-coordinate (or very near it). Returns -1 if
    * no marker is found.
    */
   public int getMarker(int x)
   {
      if (marks == null) return -1;

      long ms = graph.getTimeFromX(x);
      for(int i = marks.size() - 1; i >= 0; i--)
      {
         TimeMarker mark = marks.get(i);
         if (mark.isEvent())
         {
            int mx = graph.getXFromTime(mark.getStartTime());
            if (Math.abs(x - mx) < eventHalfWidth) return i;
         }
         else
         {
            if (ms >= mark.getStartTime() && ms <= mark.getStopTime()) return i;
         }
      }
      return -1;
   }

   /**
    * Returns the index of all time markers that intersect the given x-coordinate (or is very near it).
    * Returns an empty array if no markers are found.
    */
   public int[] getMarkers(int x)
   {
      MyIntList list = new MyIntList();
      long ms = graph.getTimeFromX(x);
      for(int i = marks.size() - 1; i >= 0; i--)
      {
         TimeMarker mark = marks.get(i);
         if (mark.isEvent())
         {
            int mx = graph.getXFromTime(mark.getStartTime());
            if (Math.abs(x - mx) < eventHalfWidth) list.add(i);
         }
         else
         {
            if (ms >= mark.getStartTime() && ms <= mark.getStopTime()) list.add(i);
         }
      }
      return list.toArray();
   }

   /**
    * Notify all listening graphs of a change
    */
   protected void fireGraphEvent(TimeX tx)
   {
      for(Graph g : glist)
      {
         g.setTime(tx);
         if (bRenderHighlights)
         {
            if (bShiftAll)
            {
               g.clearHighlights();
               for(int i = 0; i < marks.size(); i++)
                  g.addHighlight(marks.get(i));
            }
            else
            {
               if (liveMarker != null) g.highlight(liveMarker);
               else g.clearHighlights();
            }
         }
      }
      for(GraphListener gl : glisteners)
         gl.graphMouseMoved(null, tx, null);
   }

   public void mouseClicked(MouseEvent e)
   {}

   public void mouseEntered(MouseEvent e)
   {}

   public void mouseExited(MouseEvent e)
   {
      mouseTime = null;
      fireGraphEvent(mouseTime);
      repaint();
   }

   public void mouseMoved(MouseEvent e)
   {
      mousex = e.getX();
      mouseTime = graph.getTimeX(mousex);
      
      if (bShiftAll)
      {
         // TODO
         /*
          * int dx = x - liveBase; TimeMarker first = marks.first(); dx = (int)Math.max(dx,
          * -first.getStart()); TimeMarker last = marks.last(); dx = (int)Math.min(dx,
          * graph.getData().length() - 1 - last.getStop()); liveBase = x; for(int i = 0; i < marks.size();
          * i++) { TimeMarker mark = marks.get(i); mark.translate(dx); }
          * 
          * fireGraphEvent(mouseTime); repaint();
          */
      }
      else
      {
         int i = getMarker(mousex);
         if (i < 0) setToolTipText(null);
         else setToolTipText(String.format("%s (%.1fs)", marks.get(i).getTag(),
               marks.get(i).length() / 1000.0));
         repaint();
         fireGraphEvent(mouseTime);
      }
   }

   /** construct and display the popup menu given the current state */
   protected void popup(MouseEvent e)
   {
      JPopupMenu popup = new JPopupMenu();
      JMenuItem item;
      boolean bEvent = false;
      boolean bInterval = false;
      int[] markers = getMarkers(mousex);

      for(int i : markers)
         if (marks.get(i).isEvent())
         {
            bEvent = true;
            break;
         }
      for(int i : markers)
         if (marks.get(i).isInterval())
         {
            bInterval = true;
            break;
         }

      item = new JMenuItem(sRefreshLabels);
      item.addActionListener(this);
      item.setEnabled(marks.getFile() != null);
      popup.add(item);

      item = new JMenuItem(sLoadLabels);
      item.addActionListener(this);
      popup.add(item);

      item = new JMenuItem(sSaveLabels);
      item.addActionListener(this);
      item.setEnabled(marks.size() > 0 && marks.getFile() != null);
      popup.add(item);

      item = new JMenuItem(sSaveLabelsAs);
      item.addActionListener(this);
      item.setEnabled(marks.size() > 0);
      popup.add(item);

      popup.addSeparator();

      item = new JMenuItem(sShiftAll);
      item.addActionListener(this);
      // item.setEnabled(marks.size() > 0);
      item.setEnabled(false);
      popup.add(item);

      popup.addSeparator();

      item = new JMenuItem(sRenameEvent);
      item.addActionListener(this);
      item.setEnabled(bEvent);
      popup.add(item);

      item = new JMenuItem(sRenameInterval);
      item.addActionListener(this);
      item.setEnabled(bInterval);
      popup.add(item);

      popup.addSeparator();

      item = new JMenuItem(sRemoveEvent);
      item.addActionListener(this);
      item.setEnabled(bEvent);
      popup.add(item);

      item = new JMenuItem(sRemoveInterval);
      item.addActionListener(this);
      item.setEnabled(bInterval);
      popup.add(item);

      popup.addSeparator();

      item = new JCheckBoxMenuItem(sRenderHighlights, bRenderHighlights);
      item.addActionListener(this);
      popup.add(item);

      popup.show(this, mousex, e.getY());
   }

   public void mousePressed(MouseEvent e)
   {
      mousex = e.getX();
      mouseTime = graph.getTimeX(mousex);

      if (e.isPopupTrigger()) popup(e);
      else
      {
         if (bShiftAll)
         {
            bShiftAll = false;
            fireGraphEvent(mouseTime);
            repaint();
            return;
         }
         int iMark = getMarker(mousex);
         if (SwingUtilities.isLeftMouseButton(e))
         {
            if (iMark < 0)
            {
               mstate = MarkerState.None;
               bNewMarker = true;
               liveBase = mouseTime.x;
               liveMarker = new TimeMarker(Units.Time, mouseTime.time);
            }
            else
            {
               bNewMarker = false;
               TimeMarker mark = marks.get(iMark);
               if (mark.isEvent())
               {
                  mstate = MarkerState.Move;
                  marks.remove(iMark);
                  liveBase = graph.getXFromTime(mark.getStartTime());
                  liveMarker = mark;
               }
               else
               {
                  int startx = graph.getXFromTime(mark.getStartTime());
                  int stopx = graph.getXFromTime(mark.getStop());
                  if (mousex >= startx && mousex - startx <= intervalEndWidth)
                  {
                     mstate = MarkerState.Adjust;
                     liveBase = graph.getXFromTime(mark.getStopTime());
                  }
                  else if (mousex <= stopx && stopx - mousex <= intervalEndWidth)
                  {
                     mstate = MarkerState.Adjust;
                     liveBase = graph.getXFromTime(mark.getStartTime());
                  }
                  else
                  {
                     mstate = MarkerState.Move;
                     liveBase = mouseTime.x;
                  }

                  marks.remove(iMark);
                  liveMarker = mark;
               }
            }
            fireGraphEvent(mouseTime);
         }
         repaint();
      }
   }

   public void mouseReleased(MouseEvent e)
   {
      mousex = e.getX();
      mouseTime = graph.getTimeX(mousex);
      if (e.isPopupTrigger()) popup(e);
      else
      {
         if (liveMarker != null)
         {
            if (bNewMarker)
            {
               String sType = liveMarker.isEvent() ? "event" : "interval";
               String sName = JOptionPane.showInputDialog(this, "Please enter a name for this " + sType
                     + ":");
               if (sName != null)
               {
                  liveMarker.setTag(sName);
                  if (marks == null) marks = new MarkupSet();
                  marks.add(liveMarker);
               }
               bNewMarker = false;
            }
            else
            {
               assert (marks != null);
               marks.add(liveMarker);
            }

            liveMarker = null;
            bNewMarker = false;
            repaint();
            fireGraphEvent(mouseTime);
         }
      }
   }

   public void mouseDragged(MouseEvent e)
   {
      mousex = e.getX();
      mouseTime = graph.getTimeX(mousex);

      if (liveMarker != null)
      {
         if (mstate == MarkerState.Move)
         {
            if (liveMarker.isEvent()) liveMarker.set(mouseTime.time);
            else
            {
               long dtime = mouseTime.time - graph.getTimeFromX(liveBase);
               liveMarker.translate(dtime);
               liveBase = mouseTime.x;
            }
         }
         else
         {
            if (mstate == MarkerState.None) mstate = MarkerState.Adjust;
            assert mstate == MarkerState.Adjust;
            liveMarker.set(mouseTime.time, graph.getTimeFromX(liveBase));
         }

         if (mousex > getWidth())
         {
            // TODO: should scroll to the right
         }
         else if (mousex < 0)
         {
            // TODO: should scroll to the left
         }

         fireGraphEvent(mouseTime);
         repaint();
      }
   }

   public Dimension getPreferredSize()
   {
      return new Dimension(9999, 24);
   }
   
   public void graphMouseMoved(Graph graph, TimeX timex, ValueY valuey)
   {
      mouseTime = timex;      
      repaint();
   }
   
   public void graphDataChanged(Graph graph)
   {
      // nothing to do
   }
   
   public void graphVizChanged(Graph graph)
   {
      repaint();
   }
   
   protected void renameMarker(String sType)
   {
      TimeMarker mark = null;
      int[] markers = getMarkers(mousex);
      if (sType.equals("event"))
      {
         for(int i : markers)
            if (marks.get(i).isEvent())
            {
               mark = marks.get(i);
               break;
            }
      }
      else for(int i : markers)
         if (marks.get(i).isInterval())
         {
            mark = marks.get(i);
            break;
         }
      assert mark != null;
      String sName = JOptionPane.showInputDialog(this, "Please enter a new " + sType + " name:", mark
            .getTag());
      if (sName != null)
      {
         mark.setTag(sName);
         repaint();
      }
   }

   public void actionPerformed(ActionEvent e)
   {
      String cmd = e.getActionCommand();
      if (cmd.equals(sRefreshLabels))
      {
         MLGeneral loader = new MLGeneral();
         MarkupSet newMarks = loader.load(marks.getFile().getAbsolutePath());
         if (newMarks == null)
         {
            JOptionPane.showMessageDialog(this, "Failed to refresh labels.");
            return;
         }
         newMarks.setSeq(marks.getSeq());
         newMarks.setScale(marks.getScale());
         newMarks.setFile(marks.getFile());
         newMarks.scaleMarkers();
         newMarks.convertToTime();
         marks = newMarks;
         repaint();
      }
      else if (cmd.equals(sLoadLabels))
      {
         JFileChooser chooser = new JFileChooser("./");
         int returnVal = chooser.showOpenDialog(this);
         if (returnVal == JFileChooser.APPROVE_OPTION)
         {
            String sPath = chooser.getSelectedFile().getAbsolutePath();
            File file = new File(sPath);
            MLGeneral loader = new MLGeneral();
            MarkupSet newMarks = loader.load(file.getAbsolutePath());
            if (newMarks == null)
            {
               JOptionPane.showMessageDialog(this, "Failed to load the new labels.");
               return;
            }
            newMarks.setSeq(marks.getSeq());
            newMarks.setScale(marks.getScale());
            newMarks.scaleMarkers();
            newMarks.convertToTime();
            newMarks.setFile(file);
            marks = newMarks;
            repaint();
         }
      }
      else if (cmd.equals(sSaveLabels))
      {
         assert (marks.hasFile());
         int ret = JOptionPane.showConfirmDialog(this, "File exists; Overwrite?\n ("
               + Library.getCanonical(marks.getFile()) + ")", "TSView: confirm file overwrite",
               JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null);
         if (ret != JOptionPane.YES_OPTION) return;
         MSGeneral saver = new MSGeneral();
         saver.save(marks.getInOrigUnits(), marks.getFile().getAbsolutePath());
      }
      else if (cmd.equals(sSaveLabelsAs))
      {
         JFileChooser chooser = new JFileChooser("./");
         int returnVal = chooser.showSaveDialog(this);
         if (returnVal == JFileChooser.APPROVE_OPTION)
         {
            String sPath = chooser.getSelectedFile().getAbsolutePath();
            File file = new File(sPath);
            if (file.exists())
            {
               int ret = JOptionPane.showConfirmDialog(this, "File exists; Overwrite?\n (" + sPath + ")",
                     "TSView: confirm file overwrite", JOptionPane.YES_NO_OPTION,
                     JOptionPane.WARNING_MESSAGE, null);
               if (ret != JOptionPane.YES_OPTION) return;
            }
            MSGeneral saver = new MSGeneral();
            saver.save(marks.getInOrigUnits(), sPath);
            marks.setFile(file);
         }
      }
      else if (cmd.equals(sShiftAll))
      {
         liveBase = mousex;
         bShiftAll = true;
         fireGraphEvent(mouseTime);
         repaint();
      }
      else if (cmd.equals(sRemoveEvent))
      {
         int[] markers = getMarkers(mousex);
         for(int i : markers)
            if (marks.get(i).isEvent())
            {
               marks.remove(i);
               break;
            }
         repaint();
      }
      else if (cmd.equals(sRemoveInterval))
      {
         int[] markers = getMarkers(mousex);
         for(int i : markers)
            if (marks.get(i).isInterval())
            {
               marks.remove(i);
               break;
            }
         repaint();
      }
      else if (cmd.equals(sRenameEvent)) renameMarker("event");
      else if (cmd.equals(sRenameInterval)) renameMarker("interval");
      else if (cmd.equals(sRenderHighlights))
      {
         bRenderHighlights = !bRenderHighlights;
         repaint();
      }
   }

   @Override
   public void paintComponent(Graphics2D g, int width, int height)
   {      
      // clear the background
      g.setColor(getBackground());
      g.fillRect(0, 0, width, height);

      if (bAntiAlias) Library.setAntiAlias(g, true);

      // render grid extension
      if (!glist.isEmpty())
      {
         ArrayList<TimeX> grid = glist.get(0).getGridTimes();
         if (grid != null)
         {
            g.setColor(Color.gray);
            for(TimeX tx : grid)
            {
               if (tx == null) continue;
               g.drawLine(tx.x, 0, tx.x, height);
            }
         }
      }

      // render markers
      if (marks != null)
      {
         for(int i = 0; i < marks.size(); i++)
            renderMarker(g, marks.get(i), Color.black);
      }

      // render mouse info
      if (mouseTime != null)
      {
         g.setColor(getForeground());
         g.drawLine(mouseTime.x, 0, mouseTime.x, height);
      }

      // render live marker
      if (liveMarker != null) renderMarker(g, liveMarker, Color.yellow);

      // return the clipping rectangle to normal
      g.setClip(0, 0, width, height);

      if (bAntiAlias) Library.setAntiAlias(g, false);
   }
}
