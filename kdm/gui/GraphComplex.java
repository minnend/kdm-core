package kdm.gui;

import kdm.data.*;
import kdm.gui.*;
import kdm.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

/** Combines a graph with a value legend, time legend, and a (optional) event bar */
public class GraphComplex extends GraphWrapper
{
    protected JScrollBar hsb;
    protected TimeLegend timeLegend;
    protected ValueLegend valueLegend;
    protected EventBar eventBar;
    protected Border9Container b9c;

    // TODO: turn off event bar
    
    public GraphComplex(Graph _graph){ this(_graph, null); }

    public GraphComplex(Graph _graph, MarkupSet _marks)
    {
        super(_graph);

        setOpaque(true);
        setDoubleBuffered(false);
        
        // setup the graph complex
        b9c = new Border9Container();
        b9c.add(graph, Border9Container.Center);

        timeLegend = new TimeLegend(graph);
        b9c.add(timeLegend, Border9Container.Top);

        valueLegend = new ValueLegend(graph);
        b9c.add(valueLegend, Border9Container.Right);
                
        eventBar = new EventBar(graph, _marks);
        b9c.add(eventBar, Border9Container.Bottom);

        b9c.add(new ColoredComp(Color.lightGray), Border9Container.TopRight);
        b9c.add(new ColoredComp(Color.lightGray), Border9Container.BottomRight);
        
        // add the components to this container
        setLayout(new BorderLayout());
        add(b9c, BorderLayout.CENTER);
    }
    
    public void showEventBar(boolean bShow)
    {
       if (bShow)
       {
          if (b9c.getKid(Border9Container.Bottom)!=null) return;
          b9c.add(eventBar, Border9Container.Bottom);
          b9c.add(new ColoredComp(Color.lightGray), Border9Container.BottomRight);
       }
       else{
          if (b9c.getKid(Border9Container.Bottom)==null) return;
          b9c.remove(Border9Container.Bottom);
          b9c.remove(Border9Container.BottomRight);
       }
    }

    @Override
    public void paintComponent(Graphics2D g, int w, int h)
    {
        // don't do anything
    }

   public void graphMouseMoved(Graph graph, TimeX timex, ValueY valuey)
   {
      // event bar will notify the graph directly, but we must tell the time legend
      if (graph == null) timeLegend.graphMouseMoved(graph, timex, valuey);
   }
}
