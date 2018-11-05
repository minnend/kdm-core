package kdm.gui;

import java.awt.*;

import kdm.data.*;

/** renders a histogram graph (vertical bars from zero) */
public class OverlayHist extends FrameGraphOverlay
{
   protected Color cHist = new Color(140, 150, 170);
   protected Color cHistBorder = cHist.darker();
   protected int iDim;

   public OverlayHist(FuncSeq func, ValueFrameGraph graph, int iDim)
   {
      super(func, graph);
      this.iDim = iDim;
      setVizDim(iDim);
      updateSeqViz();
   }

   @Override
   public void render(Graphics2D g, ValueFrameGraph renderGraph)
   {      
      int yBase = renderGraph.getYFromValue(0);
      int wbar = dataGraph.hbar + dataGraph.hgap;
      int xPrev = dataGraph.hgap;
      int x = dataGraph.hgap + wbar;      
      for(int i = 0; i < seqViz.length(); i++)
      {
         double v = seqViz.get(i).get(iDim);
         int y = renderGraph.getYFromValue(v); // TODO scale?
         int top = Math.min(y, yBase);
         int h = Math.abs(y-yBase);
         g.setColor(cHist);
         g.fillRect(xPrev, top, wbar, h);
         g.setColor(cHistBorder);
         g.drawRect(xPrev, top, wbar, h);
         xPrev = x;
         x += wbar;
      }      
   }

}
