package kdm.gui;

import kdm.data.*;

import java.util.*;
import java.awt.*;
import java.awt.event.*;

public abstract class GraphWrapper extends Graph
{
    protected Graph graph;

    public GraphWrapper(Graph _graph)
    {
        graph = _graph;
        assert (graph!=null);
    }

    /** @return the base graph that is being wrapped */
    public Graph getGraph()
    {
       Graph g = graph;
       while(g != g.getGraph()) g = g.getGraph();
       return g;
    }

    @Override public void setData(Sequence _data){ getGraph().setData(_data); }
    @Override public TimeMarker.Units getUnits(){ return getGraph().getUnits(); }
    @Override public long getStart(){ return getGraph().getStart(); }
    @Override public long getEnd(){ return getGraph().getEnd(); }       
    @Override public Sequence getData(){ return getGraph().data; }
    @Override public void setTime(TimeX tx){ getGraph().setTime(tx); }
    @Override public void highlight(TimeMarker tm){ getGraph().highlight(tm); }
    @Override public void addHighlight(TimeMarker tm){ getGraph().addHighlight(tm); }
    @Override public void clearHighlights(){ getGraph().clearHighlights(); }
    @Override public long getNumVisibleMS(){ return getGraph().getNumVisibleMS(); }
    @Override public ArrayList<TimeX> getGridTimes(){ return getGraph().getGridTimes(); }
    @Override public ArrayList<ValueY> getGridValues(){ return getGraph().getGridValues(); }
    @Override public TimeX getTimeX(int x){ return getGraph().getTimeX(x); }
    @Override public int getXFromTime(long ms){ return getGraph().getXFromTime(ms); }
    @Override public int getXFromIndex(int ix){ return getGraph().getXFromTime(ix); }
    @Override public long getStartIndex(){ return getGraph().getStartIndex();  }
    @Override public long getStartTime(){ return getGraph().getStartTime(); } 
    @Override public long getTimeFromX(int x){ return getGraph().getTimeFromX(x); }
    @Override public int getIndexFromX(int x, boolean bClip){ return getGraph().getIndexFromX(x,bClip); }
    @Override public void setShowAll(boolean b){ getGraph().setShowAll(b); }
    @Override public void setOffset(long offset){ getGraph().setOffset(offset); }
    @Override public boolean setBounds(long msStart, long msEnd){ return getGraph().setBounds(msStart, msEnd); }
    @Override public boolean setVirtualWidth(int w){ return getGraph().setVirtualWidth(w); }
}
