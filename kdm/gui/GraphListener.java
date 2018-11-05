package kdm.gui;

import kdm.data.*;
import java.util.*;

public interface GraphListener extends EventListener
{
    public void graphMouseMoved(Graph graph, TimeX timex, ValueY valuey);
    public void graphVizChanged(Graph graph);
    public void graphDataChanged(Graph graph);
}
