package kdm.models;

import kdm.data.*;
import kdm.util.*;
import kdm.io.*;
import kdm.models.misc.MapStartScore;

import java.io.*;
import java.util.*;

/**
 * Abstract base class for probabilisti models over sequences
 */
public abstract class ProbSeqModel implements Configurable
{
   /**
    * Returns the log-likelihood of the data sequence given the model
    */
   public abstract double eval(Sequence seq);

   /**
    * Returns a sample sequence taken from this distribution.
    */
   public abstract Sequence sample();

   /**
    * Returns the start index of the best mapping of this model to the given sequence.
    */
   public MapStartScore findMapStart(Sequence seq, int iEnd)
   {
      assert false;
      return null;
   }

   public double getCost(int iEnd)
   {
      assert false;
      return 0;
   }

   public void buildPatternMap(Sequence seq, int iMinStart, int iForcedStart, int iForcedEnd)
   {
      assert false;
   }

   /**
    * Builds (trains) a model from the given examples.
    */
   public abstract ProbSeqModel build(ArrayList<Sequence> examples, String sConfig);

   /**
    * Builds (initializes) a model from the given sequence and time range.
    */
   public abstract ProbSeqModel build(Sequence seq, WindowLocation win, String sConfig);

   /**
    * Builds (initializes) a model from the given sequence.
    */
   public ProbSeqModel build(Sequence seq, String sConfig)
   {
      return build(seq, new WindowLocation(-1, 0, seq.length()), sConfig);
   }
   
   public boolean config(ConfigHelper chelp, String sKey, String sVal)
   {
      // TODO: currently, there aren't any configurable parameters
      System.err.printf("Error: unrecognized %s parameters: %s\n", getClass(), sKey);
      assert false;
      return false;
   }

   public boolean config(File fBase, String s)
   {
      // grrr: no multiple inheritance in java
      ConfigHelper chelp = new ConfigHelper(fBase);
      return chelp.config(s, this);
   }
}
