package kdm.gui;

import java.awt.*;
import javax.swing.*;
import java.util.*;
import kdm.util.*;

/**
 * A Layout manager that creates a more flexible grid. Each row/column can be set to have
 * a fixed size, take up a proportion of the remaining space, or have a size determined by
 * its contents.
 */
public class GridFlexLayout implements LayoutManager
{
   // TODO: implement minpref and propwh

   /**
    * fill = take proportion of available space
    * fixed = take fixed (pre-specified) space
    * pref = take preferred space (from component)
    * minpref = preferred space is minimum, but no max
    * propwh = take space proportional to width or height
    */
   public static enum Style { fill, fixed, pref }

   protected int hgap, vgap;
   protected ArrayList<Style> cols, rows;
   protected MyFloatList colInfo, rowInfo;
   protected MyIntList wcol, hrow;
   
   public GridFlexLayout(int nRows, int nCols)
   {
      this(nRows, nCols, 0, 0);
   }

   public GridFlexLayout(int nRows, int nCols, int _hgap, int _vgap)
   {
      assert (nRows >= 0 && nCols >= 0 && _hgap >= 0 && _vgap >= 0);

      hgap = _hgap;
      vgap = _vgap;

      rows = new ArrayList<Style>();
      Library.forceSize(rows, nRows);
      Library.fill(rows, Style.fill);
      rowInfo = new MyFloatList(nRows);
      rowInfo.fill(1.0f);
      hrow = new MyIntList(nRows);      

      cols = new ArrayList<Style>();
      Library.forceSize(cols, nCols);
      Library.fill(cols, Style.fill);
      colInfo = new MyFloatList(nCols);
      colInfo.fill(1.0f);
      wcol = new MyIntList(nCols);      
   }
   
   public int getNumRows(){ return rows.size(); }
   public int getNumCols(){ return cols.size(); }
   public int getHGap(){ return hgap; }
   public int getVGap(){ return vgap; }

   public void setRow(int iRow, Style style)
   {
      assert (style == Style.pref);
      rows.set(iRow, style);
   }

   public void setRow(int iRow, Style style, float height)
   {
      assert (style == Style.fixed || style == Style.fill);
      rows.set(iRow, style);
      rowInfo.set(iRow, height);
   }

   public void setRows(Style style)
   {
      assert (style == Style.pref || style==Style.fill);
      if (style==Style.fill) setRows(style, 1.0f);
      else for(int i=0; i<rows.size(); i++) rows.set(i, style);
   }
   
   public void setRows(Style style, float height)
   {
      assert (style == Style.fixed || style == Style.fill);
      for(int i=0; i<rows.size(); i++) rows.set(i, style);
      for(int i=0; i<rowInfo.size(); i++) rowInfo.set(i, height);      
   }
   
   public void setColumns(Style style)
   {
      assert (style == Style.pref || style==Style.fill);
      if (style==Style.fill) setColumns(style, 1.0f);
      else for(int i=0; i<cols.size(); i++) cols.set(i, style);
   }

   public void setColumns(Style style, float width)
   {
      assert (style == Style.fixed || style == Style.fill);
      for(int i=0; i<cols.size(); i++) cols.set(i, style);
      for(int i=0; i<colInfo.size(); i++) colInfo.set(i, width);
   }
   
   public void setColumn(int iCol, Style style)
   {
      assert (style == Style.pref);
      cols.set(iCol, style);      
   }

   public void setColumn(int iCol, Style style, float width)
   {
      assert (style == Style.fixed || style == Style.fill);
      cols.set(iCol, style);
      colInfo.set(iCol, width);
   }

   public void setNumCols(int nCols, Style style, float width)
   {      
      assert (style == Style.fixed || style == Style.fill);
      int m = getNumCols();
      Library.forceSize(cols, nCols);
      for(; m<nCols; m++) cols.set(m, style);
      colInfo.setSize(nCols, width);
      wcol.setSize(nCols);
   }
   
   public void addCol(Style style, float height)
   {
      setNumCols(getNumCols()+1, style, height);
   }
   
   public void setNumRows(int nRows, Style style, float height)
   {      
      assert (style == Style.fixed || style == Style.fill);
      int m = getNumRows();
      Library.forceSize(rows, nRows);
      for(; m<nRows; m++) rows.set(m, style);
      rowInfo.setSize(nRows, height);
      hrow.setSize(nRows);
   }
   
   public void addRow(Style style, float height)
   {
      setNumRows(getNumRows()+1, style, height);
   }
   
   public void addLayoutComponent(String name, Component comp)
   {}

   public void removeLayoutComponent(Component comp)
   {}

   public Dimension preferredLayoutSize(Container parent)
   {
      int nCols = getNumCols();
      int nRows = getNumRows();
      calcGridSizes(parent.getComponents(), 0, 0);
      Insets ins = parent.getInsets();
      int w = ins.left + ins.right + (nCols - 1) * hgap;
      int h = ins.top + ins.bottom + (nRows - 1) * vgap;
      for(int i = 0; i < nCols; i++)
         w += wcol.get(i);
      for(int i = 0; i < nRows; i++)
         h += hrow.get(i);
      return new Dimension(w, h);
   }

   public Dimension minimumLayoutSize(Container parent)
   {
      return preferredLayoutSize(parent); // TODO: should compute real minimum
   }

   /**
    * Compute the width of each column and height of each row; save the results in wcol
    * and hrow
    * 
    * @param w total width of the client area
    * @param h total height of the client area
    */
   protected void calcGridSizes(Component[] comps, int w, int h)
   {
      int nCols = getNumCols();
      int nRows = getNumRows();
      int wrem = w - (nCols - 1) * hgap;
      int hrem = h - (nRows - 1) * vgap;

      float fillCols = 0.0f;
      float fillRows = 0.0f;

      for(int i=0; i<wcol.size(); i++) wcol.set(i, 0);
      for(int i=0; i<hrow.size(); i++) hrow.set(i, 0);

      // run through the comps collecting info
      int iComp = 0;
      for(int iRow = 0; iRow < nRows; iRow++)
      {
         if (rows.get(iRow) == Style.fixed)
         {
            hrow.set(iRow, (int)rowInfo.get(iRow));
            hrem -= hrow.get(iRow);
         }
         else if (rows.get(iRow) == Style.fill) fillRows += rowInfo.get(iRow);

         for(int iCol = 0; iCol < nCols; iCol++)
         {
            if (iRow == 0)
            {
               if (cols.get(iCol) == Style.fixed)
               {
                  wcol.set(iCol, (int)colInfo.get(iCol));
                  wrem -= wcol.get(iCol);
               }
               else if (cols.get(iCol) == Style.fill) fillCols += colInfo.get(iCol);
            }

            if (iComp < comps.length)
            {
               Dimension d = comps[iComp].getPreferredSize();
               if (cols.get(iCol) == Style.pref && d.width > wcol.get(iCol)) wcol.set(iCol, d.width);
               if (rows.get(iRow) == Style.pref && d.height > hrow.get(iRow)) hrow.set(iRow, d.height);
            }

            iComp++;
         }
      }

      // note width/height used by PREF columns/rows;
      for(int iCol = 0; iCol < nCols; iCol++)
         if (cols.get(iCol) == Style.pref) wrem -= wcol.get(iCol);
      for(int iRow = 0; iRow < nRows; iRow++)
         if (rows.get(iRow) == Style.pref) hrem -= hrow.get(iRow);

      // use the info to set the column and row sizes
      int wrem2 = wrem;
      int iLastFillCol = -1;
      for(int iCol = 0; iCol < nCols; iCol++)
      {
         if (cols.get(iCol) == Style.fill)
         {
            wcol.set(iCol, (int)Math.max(Math.round(wrem * colInfo.get(iCol) / fillCols), 0));
            wrem2 -= wcol.get(iCol);
            iLastFillCol = iCol;
         }
      }
      if (wrem2 != 0 && iLastFillCol >= 0) wcol.set(iLastFillCol, Math.max(0, wcol.get(iLastFillCol) + wrem2));

      int hrem2 = hrem;
      int iLastFillRow = -1;
      for(int iRow = 0; iRow < nRows; iRow++)
      {
         if (rows.get(iRow) == Style.fill)
         {
            hrow.set(iRow, (int)Math.max(Math.round(hrem2 * rowInfo.get(iRow) / fillRows), 0));
            fillRows -= rowInfo.get(iRow);
            hrem2 -= hrow.get(iRow);
            iLastFillRow = iRow;
         }
      }
      if (hrem2 != 0 && iLastFillRow >= 0) hrow.set(iLastFillRow, Math.max(0, hrow.get(iLastFillRow) + hrem2));
   }

   public void layoutContainer(Container parent)
   {
      int nCols = getNumCols();
      int nRows = getNumRows();
      int w = parent.getWidth();
      int h = parent.getHeight();
      Insets ins = parent.getInsets();
      int cw = w - (ins.left + ins.right);
      int ch = h - (ins.top + ins.bottom);
      Component comps[] = parent.getComponents();

      calcGridSizes(comps, cw, ch);

      Point tl = new Point(ins.left, ins.top);
      int iComp = 0;
      for(int iRow = 0; iRow < nRows; iRow++)
      {
         tl.x = ins.left;
         for(int iCol = 0; iCol < nCols; iCol++)
         {
            if (iComp >= comps.length) return;
            comps[iComp++].setBounds(tl.x, tl.y, wcol.get(iCol), hrow.get(iRow));
            tl.x += wcol.get(iCol) + hgap;
         }
         tl.y += hrow.get(iRow) + vgap;
      }
   }

   public static void main(String args[])
   {
      JFrame frame = new JFrame("GridFlexLayout Test");
      frame.setLocation(120, 80);
      frame.setSize(800, 600);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      GridFlexLayout gfl = new GridFlexLayout(3, 4, 4, 4);
      gfl.setColumn(0, Style.fill, 3);
      gfl.setColumn(1, Style.pref);
      gfl.setRow(0, Style.fill, 3);
      gfl.setRow(1, Style.fixed, 20);
      JPanel p = new JPanel(gfl);
      p.setBackground(Color.blue);
      p.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.red));
      for(int i = 0; i < 3; i++)
         for(int j = 0; j < 4; j++)
         {
            if (j == 1)
            {
               String[] foo = { "hello world!", "this sucks", "bob" };
               JLabel lab = new JLabel(foo[i]);
               p.add(lab);
               System.err.printf("i==%d  w=%d (%s)\n", i, lab.getPreferredSize().width, foo[i]);
            }
            else
            {
               JPanel c = new JPanel();
               c.setBackground(new Color(i * 85 + 85, j * 30 + 25, 30 + (i + j) * 40));
               c.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.green));
               p.add(c);
            }
         }
      frame.setContentPane(p);
      frame.setVisible(true);
   }

}
