package kdm.mlpr;

import kdm.data.*;

/**
 * Interface declaring a function that computes the distance between two sequences.
 */
public interface SeqDist
{
   public double dist(Sequence a, Sequence b);
}
