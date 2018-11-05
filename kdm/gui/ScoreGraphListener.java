package kdm.gui;

import java.util.*;

/** interface that allows notification of a change in a ScoreGraphComplex */
public interface ScoreGraphListener extends EventListener
{
   /**
    * Visualization of score graph has changed
    * 
    * @param src score graph that changed
    * @param nDisp current position of score display slider
    */
   public void vizChanged(ScoreGraphComplex src, int nDisp);
}
