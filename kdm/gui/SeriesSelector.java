package kdm.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import kdm.util.*;
import javax.swing.*;
import kdm.data.*;
import kdm.data.TimeMarker.Units;

/** gui component that selects between different time series */
public class SeriesSelector extends Graph implements ItemListener
{
   protected JComboBox cb;
   protected JTextField tf;
   protected HashMap<String, String> map;
   protected GraphScrollPane gsp;
   protected ArrayList<Graph> graphs;
   protected ArrayList<Sequence> series;
   protected ArrayList<MarkupSet> labels;
   protected EventBar eb;
   protected String sLabelFormat;
   protected int nSeries;

   public SeriesSelector()
   {
      map = new HashMap<String, String>();
      cb = new JComboBox();
      cb.addItemListener(this);
      JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
      p.setBackground(Color.gray);
      setLayout(new GridLayout(1, 1));
      add(p);
      p.add(cb);
      tf = new JTextField(24);
      tf.setEditable(false);
      // p.add(tf); // TODO: need to display info
   }

   public void setGraphs(ArrayList<Graph> graphs)
   {
      this.graphs = graphs;
   }

   public void setData(ArrayList<Sequence> data)
   {
      this.series = data;
   }

   public void setLabels(ArrayList<MarkupSet> labels, EventBar eb)
   {
      this.labels = labels;
      this.eb = eb;
   }
   
   public void setOffset(long offset)
   {
      // nothing to do
   }

   public void setGraphScrollPane(GraphScrollPane gsp)
   {
      this.gsp = gsp;
   }
   
   public boolean config(ConfigHelper chelp, String sKey, String sVal)
   {
      if (Library.stricmp(sKey, "set"))
      {
         StringTokenizer st = new StringTokenizer(sVal, ", ");
         while(st.hasMoreTokens())
         {
            String pair = st.nextToken();
            StringTokenizer st2 = new StringTokenizer(pair, ":");
            String sComp = st2.nextToken();
            String sFormat = st2.nextToken();
            map.put(sComp, sFormat);
         }
      }
      else if (Library.stricmp(sKey, "label"))
      {
         sLabelFormat = sVal;
         cb.removeAllItems();
         for(int i = 0; i < nSeries; i++)
            cb.addItem(String.format(sLabelFormat == null ? "Series %d" : sLabelFormat, i + 1));
      }
      else if (Library.stricmp(sKey, "num"))
      {
         try
         {
            nSeries = Integer.parseInt(sVal);
            cb.removeAllItems();
            for(int i = 0; i < nSeries; i++)
               cb.addItem(String.format(sLabelFormat == null ? "Series %d" : sLabelFormat, i + 1));
         } catch (NumberFormatException nfe)
         {
            System.err.printf("Error: invalid value for number of time series (\"%s\")\n", sVal);
            return false;
         }
      }
      else
      {
         System.err.printf("Error: unknown config param for SeriesSelector (\"%s\")\n", sKey);
         return false;
      }
      return true;
   }

   @Override
   public int getIndexFromX(int x, boolean bClip)
   {
      assert false;
      return -1;
   }

   @Override
   public ArrayList<TimeX> getGridTimes()
   {
      assert false;
      return null;
   }

   @Override
   public ArrayList<ValueY> getGridValues()
   {
      assert false;
      return null;
   }

   @Override
   public long getNumVisibleMS()
   {
      assert false;
      return 0;
   }

   @Override
   public TimeX getTimeX(int x)
   {
      assert false;
      return null;
   }

   public void itemStateChanged(ItemEvent e)
   {
      Object src = e.getSource();
      int event = e.getStateChange();
      if (src == cb && event == ItemEvent.SELECTED)
      {
         int ix = cb.getSelectedIndex();
         Iterator<String> it = map.keySet().iterator();
         Sequence seq = null;
         while(it.hasNext())
         {
            String sComp = it.next();
            String sFormat = map.get(sComp);
            String sData = String.format(sFormat, ix + 1);
            int nFound = 0;
            if (series != null)
            {
               for(Sequence sq : series)
               {
                  if (sData.equals(sq.getName()))
                  {
                     seq = sq;
                     nFound++;
                     break;
                  }
               }
               if (nFound == 0) System.err.printf("Warning: no matching data (%s)\n", sData);
            }            
            nFound = 0;
            if (graphs != null)
            {
               for(Graph graph : graphs)
               {
                  if (sComp.equals(graph.getGraphName()))
                  {
                     graph.setData(seq);
                     nFound++;
                  }
               }
               if (nFound == 0) System.err.printf("Warning: no matching component (%s)\n", sComp);
            }            
         }
         if (seq != null && eb != null && sLabelFormat != null && labels != null)
         {
            String s = String.format(sLabelFormat, ix + 1);
            MarkupSet marks = null;
            for(MarkupSet ms : labels)
            {
               if (ms == null) continue;
               if (s.equals(ms.getName()))
               {
                  marks = ms;
                  break;
               }
            }
            eb.setMarks(marks);
         }
         if (gsp != null) gsp.updateTimes();
      }
   }

   @Override
   public long getEnd()
   {
      return -1;
   }

   @Override
   public long getStart()
   {
      return -1;
   }

   @Override
   public Units getUnits()
   {
      return null;
   }

   @Override
   public boolean setBounds(long start, long end)
   {      
      return false;
   }

   @Override
   public long getTimeFromX(int x)
   {
      assert(false);
      return 0;
   }

   @Override
   public int getXFromTime(long ms)
   {
      assert(false);
      return 0;
   }
   
   @Override
   public int getXFromIndex(int ix)
   {
      assert(false);
      return 0;
   }

   @Override
   public long getStartIndex()
   {
      assert(false);
      return 0;
   }

   @Override
   public long getStartTime()
   {
      assert(false);
      return 0;
   }

   @Override
   public void setTime(TimeX tx)
   {
      assert(false);
   }
   
   @Override
   public void paintComponent(Graphics2D g, int w, int h)
   {
       // don't do anything
   }
}
