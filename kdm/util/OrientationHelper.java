package kdm.util;

import java.awt.*;
import java.awt.geom.*;

import javax.swing.*;

/**
 * Helps swing components support vertical and horizontal rendering
 */
public class OrientationHelper
{
   AffineTransform atOrig, atTran;
   Dimension dim;
   
   public OrientationHelper(Graphics2D g, Dimension _dim, int _mLeft, int _mTop, int _mRight, int _mBottom)
   {
      dim = new Dimension(_dim);
      dim.width -= _mLeft+_mRight;
      dim.height -= _mTop+ _mBottom;
      
      atOrig = g.getTransform();
      g.rotate(Math.PI / 2);
      g.transform(new AffineTransform(1, 0, 0, -1, 0, 0));
      atTran = g.getTransform();
      
      reset(g, _dim);
   }
   
   public void reset(Graphics2D g, Dimension _dim)
   {
      g.setTransform(atOrig);
      if (_dim!=null) _dim.setSize(dim.width, dim.height);
   }
   
   public void tran(Graphics2D g, Dimension _dim)
   {
      g.setTransform(atTran);
      if (_dim!=null) _dim.setSize(dim.height, dim.width);
   }
}
