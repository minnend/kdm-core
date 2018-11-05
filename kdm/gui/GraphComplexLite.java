package kdm.gui;

import kdm.data.*;
import kdm.io.Def.DefComp;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.util.*;

/** Combines a graph with a value legend to the right */
public class GraphComplexLite extends GraphWrapper
{
    protected ValueLegend valueLegend;

    public GraphComplexLite(Graph graph)
    {
        super(graph);

        setOpaque(true);
        setDoubleBuffered(false);
        
        setLayout(new BorderLayout());
        add(graph, BorderLayout.CENTER);
        valueLegend = new ValueLegend(graph);
        add(valueLegend, BorderLayout.EAST);
    }
    
    @Override
    public void paintComponent(Graphics2D g, int w, int h)
    {
        // don't do anything
    }
}
