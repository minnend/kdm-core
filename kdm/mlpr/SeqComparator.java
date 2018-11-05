package kdm.mlpr;

import java.util.*;
import kdm.data.*;

/**
 * Interface declaring a function that computes the distance between two sets of sequences
 */
public interface SeqComparator // TODO: better name
{
   public double dist(ArrayList<Sequence> a, ArrayList<Sequence> b);
}
