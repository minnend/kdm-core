package kdm.data.generate;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

import kdm.gui.*;

import kdm.data.*;
import kdm.data.transform.*;
import kdm.util.*;

/** Generate random sequences */
public class SeqRand
{
   /**
    * Generate random data with known start and end points
    * 
    * @param vStart initial value
    * @param vEnd final value
    * @param scale scale of random value steps
    * @param smooth size of smoothing kernel (1 = no smoothing)
    * @param len length of data sequence to generate
    * @return random sequence
    */
   public static double[] generate(double vStart, double vEnd, double scale, int smooth, int len)
   {
      double[] x = new double[len];
      for(int i=0; i<len; i++)
         x[i] = (Library.random() - 0.5)*scale;
      
      if (smooth > 1)
      {
         Sequence seq = new Sequence("derivs", x);
         seq = new TransformSmooth(smooth).transform(seq);
         x = seq.toSeqArray()[0];
      }
      
      double diff = vEnd - vStart;
      x[0] = 0; // starting value is fixed
      double w = Library.sum(x);
      double dx = (diff - w) / (x.length-1);
      x[0] = vStart;
      for(int i=1; i<len; i++) x[i] += x[i-1] + dx;
      
      return x;
   }
   
   /** 
    * Generate random walk data
    * 
    * @param vStart initial value
    * @param vStep absolute change in value at each step 
    * @param smooth size of smoothing kernel
    * @param len number of frames to generate
    * @return random sequence
    */
   public static double[] generate(double vStart, double vStep, int smooth, int len)
   {
      Sequence seq = new Sequence();
      
      double v = vStart;
      seq.add(new FeatureVec(1, vStart));
      for(int t=1; t<len; t++)
      {
         if (Library.random() < 0.5) v -= vStep;
         else v += vStep;
         seq.add(new FeatureVec(1, v));
      }
      
      if (smooth > 1)
         seq = new TransformSmooth(smooth).transform(seq);         
      
      return seq.toSeqArray()[0];
   }
   
   public static void main(String args[])
   {
      JFrame frame = new JFrame("Random Sequence");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setSize(600, 400);
      Library.centerWin(frame, null);
      
      JPanel p = new JPanel(new BorderLayout());
      final LineGraph graph = new LineGraph();
      GraphComplexLite gcl = new GraphComplexLite(graph);
      p.add(gcl, BorderLayout.CENTER);
      
      JPanel q = new JPanel();
      final JButton btWalk = new JButton("Random Walk");
      q.add(btWalk);
      final JButton btDeriv = new JButton("Random Derivs");
      q.add(btDeriv);
      p.add(q, BorderLayout.SOUTH);
      frame.setContentPane(p);
      
      ActionListener listener = new ActionListener()
      {
         public void actionPerformed(ActionEvent e)
         {
            Object src = e.getSource();
            if (src == btWalk)
            {
               double[] a = generate(0, 1, 5, 1000);
               Sequence seq = new Sequence("Random Walk", a);
               graph.setData(seq);
            }
            else if (src == btDeriv)
            {
               double[] a = generate(0, 0, 1, 5, 1000);
               Sequence seq = new Sequence("Random Derivs", a);
               graph.setData(seq);
            }
         }         
      };
      
      btWalk.addActionListener(listener);
      btDeriv.addActionListener(listener);
      
      frame.setVisible(true);
   }
   
}
