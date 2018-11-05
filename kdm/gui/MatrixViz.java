package kdm.gui;

import java.awt.*;

import javax.swing.*;

import kdm.models.*;
import kdm.util.*;

/**
 * Displays a matrix with optional labels and color span (default: black -> white)
 */
public class MatrixViz extends JPanel
{
   protected GridFlexLayout gfl;
   protected Color c0, c1;
   
   /** create a grayscale matrix view */
   public MatrixViz(double m[][])
   {
      this(m, Color.black, Color.white);
   }
   
   /** create a matrix view using the given colors */
   public MatrixViz(double m[][], Color _c0, Color _c1)
   {
      c0 = _c0;
      c1 = _c1;
      setTransMatrix(m);
   }
   
   public void setTransMatrix(double[][] m)
   {
      removeAll();
      
      int n = m.length;      

      gfl = new GridFlexLayout(n + 1, n + 1, 1, 1);
      gfl.setColumn(0, GridFlexLayout.Style.pref);
      gfl.setRow(0, GridFlexLayout.Style.pref);
      setLayout(gfl);
      add(new EmptyComponent());
      float[] f0 = c0.getRGBColorComponents(null);
      float[] f1 = c1.getRGBColorComponents(null);
      float[] fr = new float[]{ f1[0]-f0[0], f1[1]-f0[1], f1[2]-f0[2] };
      for(int i = 0; i < n; i++)
         add(new JLabel(String.format("%d", i + 1), JLabel.CENTER));
      for(int i = 0; i < n; i++)
      {
         add(new JLabel(String.format("%d", i + 1)));
         for(int j = 0; j < n; j++)
         {            
            ColoredComp comp;
            if (m[i][j] > Library.LOG_ZERO)
            {
               double prob = Math.exp(m[i][j]);
               float f = (float)(prob * 0.8 + 0.2);
               comp = new ColoredComp(new Color(f0[0]+f*fr[0], f0[1]+f*fr[1], f0[2]+f*fr[2]));
               comp.setToolTipText(String.format("%.1f%%", 100.0*prob));
            }
            else comp = new ColoredComp(Color.black);
            add(comp);
         }
      }
      revalidate();
   }
   
   /** show or hide the matrix labels */
   public void showLabels(boolean b)
   {
      if (b)
      {
         gfl.setColumn(0, GridFlexLayout.Style.pref);
         gfl.setRow(0, GridFlexLayout.Style.pref);
      }
      else{
         gfl.setColumn(0, GridFlexLayout.Style.fixed, 0);
         gfl.setRow(0, GridFlexLayout.Style.fixed, 0);
      }
      revalidate();
   }
}
