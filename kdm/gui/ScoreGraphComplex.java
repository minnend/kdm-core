package kdm.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

import kdm.data.*;
import kdm.models.misc.*;
import kdm.util.*;

/**
 * Visualization of a list of scores via a line graph
 */
public class ScoreGraphComplex extends JPanel implements ItemListener, ChangeListener, ActionListener
{
   protected ArrayList<ScoreGraphListener> listeners;
   protected LineGraph lgraph;
   protected JComboBox cbGraph;
   protected JSlider slideDisp;
   protected JTextField tfDisp;
   protected JButton btDump, btSelect;
   protected HashMap<String,MyDoubleList> data = new HashMap<String, MyDoubleList>();

   public ScoreGraphComplex()
   {
      init();
   }

   public ScoreGraphComplex(String name, MyDoubleList curve)
   {      
      init();
      data.put(name, curve);
      updateGraph();
   }

   public void init()
   {
      setLayout(new BorderLayout());
      listeners = new ArrayList<ScoreGraphListener>();

      lgraph = new LineGraph();
      lgraph.setGraphName("Score Graph");
      lgraph.setForeground(new Color(255, 190, 40));
      lgraph.setShowAll(true);
      this.add(new GraphComplexLite(lgraph), BorderLayout.CENTER);

      JPanel bottomp = new JPanel(new BorderLayout());
      this.add(bottomp, BorderLayout.SOUTH);
      
      cbGraph = new JComboBox();
      cbGraph.addItem(" Score");
      cbGraph.addItem(" Score Ratio");
      cbGraph.addItem(" Score Diff");
      cbGraph.addItemListener(this);
      cbGraph.setEnabled(false);

      slideDisp = new JSlider(0, 0, 0);
      slideDisp.setPaintTicks(false);
      slideDisp.setPaintLabels(false);
      slideDisp.setEnabled(false);
      slideDisp.addChangeListener(this);

      tfDisp = new JTextField(4);
      tfDisp.setEditable(false);

      btSelect = new JButton("Select...");
      btSelect.addActionListener(this);
      btSelect.setEnabled(false);
      
      btDump = new JButton("Print Scores");
      btDump.addActionListener(this);
      btDump.setEnabled(false);

      JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
      p.add(btSelect);
      p.add(btDump);
      bottomp.add(p, BorderLayout.WEST);

      p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      // p.add(Box.createHorizontalStrut(16));
      p.add(new JLabel("Display: "));
      p.add(slideDisp);
      p.add(tfDisp);
      p.add(Box.createHorizontalStrut(16));
      p.add(cbGraph);
      bottomp.add(p, BorderLayout.EAST);
   }
   
   public void addCurve(String name, MyDoubleList curve)
   {
      data.put(name, curve);
      updateGraph();
   }

   protected int getMaxNumScores()
   {
      Iterator<String> it = data.keySet().iterator();
      int T = 0;
      while(it.hasNext()){
         MyDoubleList list = data.get(it.next());
         if (list.size() > T) T = list.size();
      }
      return T;
   }

   public void updateGraph()
   {
      int nCurves = data.size();
      int T = getMaxNumScores();      
      
      if (T == 0){
         lgraph.setData(null);
         cbGraph.setEnabled(false);
         slideDisp.setEnabled(false);
         slideDisp.setMaximum(0);
         slideDisp.setValue(0);
         tfDisp.setText("");
         btDump.setEnabled(false);
         btSelect.setEnabled(false);         
         return;
      }

      slideDisp.setMaximum(T-1);
      slideDisp.setValue(T-1);
      tfDisp.setText("" + slideDisp.getValue());
      btDump.setEnabled(true);
      btSelect.setEnabled(T>1);
      cbGraph.setEnabled(true);
      slideDisp.setEnabled(true);

      Sequence seq = new Sequence("Spot Info", 1.0);
      FeatureVec fvPrev = null;
      FeatureVec fvPrevScore = new FeatureVec(nCurves);
      fvPrevScore.fill(Double.NaN);
      for(int t = 0; t < T; t++){
         FeatureVec fv = new FeatureVec(nCurves);
         Iterator<String> it = data.keySet().iterator();
         for(int i = 0; i < nCurves; i++){
            String name = it.next();
            MyDoubleList curve = data.get(name);
            double score = (t < curve.size() ? curve.get(t) : Double.NaN);
            switch(cbGraph.getSelectedIndex()){
            case 0: // score
               fv.set(i, score);
               break;
            case 1: // score ratio
            {
               double x;
               double prev = fvPrevScore.get(i);
               if (Double.isNaN(prev)) x = 1.0;
               else if (prev == 0){
                  if (score > 0) x = Library.INF;
                  else if (score == 0) x = 0;
                  else x = Library.NEGINF;
               }
               else x = score / prev;
               fv.set(i, x);
            }
               break;
            case 2: // score diff
            {
               double prev = fvPrevScore.get(i);
               fv.set(i, Double.isNaN(prev) ? 0 : score - prev);
               break;
            }
            }
            fvPrevScore.set(i, score);
         }
         seq.add(fv);
         fvPrev = fv;
      }
      lgraph.setData(seq);
   }

   public void addScoreGraphListener(ScoreGraphListener wsvl)
   {
      if (!listeners.contains(wsvl)) listeners.add(wsvl);
   }

   public void removeScoreGraphListener(ScoreGraphListener wsvl)
   {
      listeners.remove(wsvl);
   }

   public Dimension getPreferredSize()
   {
      return new Dimension(1, 150); // TODO
   }

   protected void fireGraphVizChanged()
   {
      for(ScoreGraphListener wsvl : listeners)
         wsvl.vizChanged(this, slideDisp.getValue());
   }

   public void itemStateChanged(ItemEvent e)
   {
      ItemSelectable src = e.getItemSelectable();
      boolean bOn = (e.getStateChange() == ItemEvent.SELECTED);

      if (src == cbGraph) updateGraph();
   }

   public void stateChanged(ChangeEvent e)
   {
      Object src = e.getSource();

      if (src == slideDisp){
         int val = slideDisp.getValue();
         lgraph.highlight(new TimeMarker(TimeMarker.Units.Index, val));
         tfDisp.setText("" + val);
         fireGraphVizChanged();
      }

   }
   
   public int getNumCurves(){ return data.size(); }
   public int getDisplayIndex(){ return slideDisp.getValue(); }

   protected void dump()
   {
      int nCurves = data.size();
      int T = getMaxNumScores();
      for(int i = 0; i < T; i++){
         Iterator<String> it = data.keySet().iterator();
         while(it.hasNext()){
            String name = it.next();
            MyDoubleList list = data.get(name);
            if (i >= list.size()) System.err.printf("%12s ", " ");
            else System.err.printf("%12f ", list.get(i));
         }
         System.err.println();
      }
      System.err.flush();
   }

   public void actionPerformed(ActionEvent e)
   {
      Object src = e.getSource();

      if (src == btDump){
         dump();
      }
      else if (src == btSelect){         
         System.err.printf("not yet implemented\n"); // TODO
      }
   }

}
