package kdm.gui;

import java.awt.*;
import kdm.data.*;
import kdm.util.MyIntList;

/**
 * abstract base class of all frame graph overlays - these are classes that know how to render a certain kind
 * of data/graph on a ValueFrameGraph
 */
public abstract class FrameGraphOverlay implements GraphListener
{
   protected ValueFrameGraph dataGraph;
   protected FuncSeq func;   
   public Sequence data, seqViz;
   public MyIntList vizDims;
   
   public FrameGraphOverlay(FuncSeq func, ValueFrameGraph graph)
   {      
      this.func = func;
      this.dataGraph = graph;
      vizDims = new MyIntList();
      graph.addGraphListener(this);
      data = (func==null ? null : func.compute(graph.getData()));
   }   
   public abstract void render(Graphics2D g, ValueFrameGraph renderGraph);   
   
   public void setVizDim(int iDim)
   {
      vizDims.clear();
      vizDims.add(iDim);
   }
   
   public void addVizDim(int ... dims)
   {
      for(int i : dims) vizDims.add(i);
   }
   
   /** recalculate the visual data sequence for this overlay */
   protected void updateSeqViz()
   {  
      if (data == null) return;
      Sequence seqGraph = dataGraph.getSeqViz();      
      seqViz = new Sequence();
   
      int T = seqGraph.length();
      for(int t=0; t<T; t++)
      {
         Point p = (Point)seqGraph.get(t).getMeta(ValueFrameGraph.KeyFrameRange);
         FeatureVec fv = new FeatureVec(data.get(p.x));
         for(int i=p.x+1; i<p.y; i++)
            fv._add(data.get(i));
         int n = p.y-p.x;
         if (n > 1) fv._div(n);
         seqViz.add(fv);
      }
   }   
   
   public double[] getMinMax(int iStart, int nViz)
   {
      if (data == null) return new double[]{ -1, 1 };
      
      FeatureVec[] fv = data.getMinMax(iStart, nViz);
      if (vizDims.isEmpty()) return new double[] { fv[0].min(), fv[1].max() };
      else{
         double[] mnx = new double[2];
         mnx[0] = fv[0].get(vizDims.get(0));
         mnx[1] = fv[1].get(vizDims.get(0));
         int n = vizDims.size();
         for(int i=1; i<n; i++)
         {            
            int iDim = vizDims.get(i);
            mnx[0] = Math.min(mnx[0], fv[0].get(iDim));
            mnx[1] = Math.max(mnx[1], fv[1].get(iDim));
         }
         return mnx;
      }
   }
   
   public void graphMouseMoved(Graph graph, TimeX timex, ValueY valuey)
   {
      // nothing to do here
   }
   
   public void graphDataChanged(Graph graph)
   {
      data = (func==null ? null : func.compute(graph.getData()));
   }
   
   public void graphVizChanged(Graph graph)
   {
      if (graph == this.dataGraph) updateSeqViz();      
   }
}
