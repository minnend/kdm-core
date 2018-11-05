package kdm.gui;

import java.awt.*;
import java.awt.event.*;

import kdm.data.*;
import kdm.mlpr.*;
import kdm.mlpr.classifier1D.*;
import kdm.mlpr.ensemble.*;
import kdm.util.*;
import java.util.*;
import javax.swing.*;

/**
 * Component to visualize the learning of a 2D boosted decision stump
 */
public class BDSViewer extends JMyComponent implements MouseListener
{
   Sequence[] data;
   double[] mind, maxd, range;
   double[][] weight;
   int nTotal;
   boolean bClick = false;
   ArrayList<WeakClassifier> wca;

   public BDSViewer(Sequence[] _data)
   {
      data = _data;
      
      assert data[0].getNumDims()==2;
      mind = new double[2];
      maxd = new double[2];
      mind[0] = mind[1] = Double.MAX_VALUE;
      maxd[0] = maxd[1] = Double.MIN_VALUE;
      for(int i=0; i<data.length; i++)
      {
         FeatureVec fvmin = data[i].getMin();
         FeatureVec fvmax = data[i].getMax();
         for(int j=0; j<2; j++)
         {
            if (fvmin.get(j) < mind[j]) mind[j] = fvmin.get(j);
            if (fvmax.get(j) > maxd[j]) maxd[j] = fvmax.get(j);
         }
      }
      
      range = new double[2];
      range[0] = maxd[0] - mind[0];
      range[1] = maxd[1] - mind[1];
      
      mind[0] -= range[0]/20.0;
      mind[1] -= range[1]/20.0;
      maxd[0] += range[0]/20.0;
      maxd[1] += range[1]/20.0;
      
      range[0] = maxd[0] - mind[0];
      range[1] = maxd[1] - mind[1];
      
      nTotal = 0;
      for(int i=0; i<data.length; i++) nTotal += data[i].length();
         
      weight = new double[data.length][];
      for(int i=0; i<data.length; i++){
         weight[i] = new double[data[i].length()];
         Arrays.fill(weight[i], 1.0 / nTotal);
      }
      wca = new ArrayList<WeakClassifier>();
      
      addMouseListener(this);
      
      JFrame frame = new JFrame("BDS Viewer");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setSize(600,600);
      frame.setContentPane(this);
      frame.setVisible(true);
   }
   
   public void waitForClick()
   {
      bClick = false;
      while(!bClick) Library.sleep(100);   
   }
   
   public void add(DecisionStump ds)
   {
      wca.add(new DecisionStump(ds));
      double x = ds.getDecisionBoundary();
      double r[] = new double[]{range[0]/20,range[1]/20};
      int i = ds.getFeature();      
      if (x < mind[i]+r[i]){
         mind[i] = x;
         range[i] = maxd[i] - mind[i] + r[i];
         mind[i] -= range[i]/20.0;
      }
      else if (x > maxd[i]-r[i]){
         maxd[i] = x;
         range[i] = maxd[i] - mind[i] + r[i];
         maxd[i] += range[i]/20.0;
      }
      range[i] = maxd[i] - mind[i];
      repaint();
   }   
   
   public void setWeights(double[] w)
   {
      int iw = 0;      
      for(int i=0; i<weight.length; i++)
         for(int j=0; j<weight[i].length; j++)
            weight[i][j] = w[iw++];
      repaint();
   }

   @Override
   public void paintComponent(Graphics2D g, int w, int h)
   {
      g.setColor(Color.white);
      g.fillRect(0,0,w,h);
      
      int nClasses = data.length;
      
      g.setColor(Color.darkGray);
      for(WeakClassifier wc : wca)
      {
         DecisionStump ds = (DecisionStump)wc;
         if (ds.getFeature() == 0)
         {
            double fx = ds.getDecisionBoundary();
            int sign = ds.getSign();
            int x = (int)Math.round(w * (fx-mind[0])/range[0]);
            g.drawLine(x,0,x,h);
         }
         else{
            double fy = ds.getDecisionBoundary();
            int sign = ds.getSign();
            int y = h - (int)Math.round(h * (fy-mind[1])/range[1]);
            g.drawLine(0,y,w,y);
         }
      }
      
      Color[] cc = Library.generateColors(nClasses);
      for(int i=0; i<nClasses; i++)
      {
         for(int j=0; j<data[i].length(); j++)
         {
            int x = (int)Math.round(w * (data[i].get(j,0)-mind[0])/range[0]);
            int y = h - (int)Math.round(h * (data[i].get(j,1)-mind[1])/range[1]);
            g.setColor(cc[i]);
            int rad = (int)Math.max(3, Math.round(weight[i][j]*nTotal * 8));
            g.fillOval(x-rad/2, y-rad/2, rad, rad);
         }
      }
   }

   public void mouseClicked(MouseEvent e)
   {
      // TODO Auto-generated method stub
      
   }

   public void mousePressed(MouseEvent e)
   {
      bClick = true;
      
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
