package kdm.gui;

import kdm.data.*;
import java.awt.*;

/** value-frame renderer that draws a line graph */
public class OverlayLine extends FrameGraphOverlay
{
   protected Color[] dimColors;

   public OverlayLine(FuncSeq func, ValueFrameGraph graph, Color color)
   {
      this(func, graph, new Color[] { color });
   }

   public OverlayLine(FuncSeq func, ValueFrameGraph graph, Color[] dimColors)
   {
      super(func, graph);
      this.dimColors = dimColors;
      for(int i=0; i<dimColors.length; i++)
         if (dimColors[i]!=null) addVizDim(i);
      updateSeqViz();
   }

   public void render(Graphics2D g, ValueFrameGraph renderGraph)
   {      
      for(int d = 0; d < dimColors.length; d++)
      {
         if (dimColors[d] == null) continue;
         g.setColor(dimColors[d]);
         FeatureVec fvPrev = seqViz.get(0);
         int wbar = dataGraph.hbar + dataGraph.hgap;
         int xPrev = dataGraph.hgap;
         int x = dataGraph.hgap + wbar;
         int a = renderGraph.getYFromValue(fvPrev.get(d));
         for(int i = 1; i < seqViz.length(); i++)
         {
            FeatureVec fvNow = seqViz.get(i);
            int b = renderGraph.getYFromValue(fvNow.get(d));
            g.drawLine(xPrev, a, x, b);

            a = b;
            fvPrev = fvNow;
            xPrev = x;
            x += wbar;
         }
      }
   }
}
