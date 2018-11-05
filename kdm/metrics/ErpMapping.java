package kdm.metrics;

import java.util.*;
import kdm.io.*;
import kdm.data.*;
import kdm.util.*;

public class ErpMapping
{
   public double score;
   public PairII[] imap; // maps from query to data
   public int iSeries;

   public ErpMapping()
   {
      iSeries = -1;
   }

   public ErpMapping(double _score)
   {
      this(_score, -1);
   }

   public ErpMapping(double _score, int _iSeries)
   {
      score = _score;
      iSeries = _iSeries;
   }

   // TODO: convert array list -> array (reverse?)

   public WindowLocation getLoc()
   {
      return new WindowLocation(iSeries, getFirstDataIndex(), getDataLength());
   }

   public int getQueryLength()
   {
      return imap[imap.length - 1].a;
   }

   public int getDataLength()
   {
      return getLastDataIndex() - getFirstDataIndex() + 1;
   }

   public int getFirstDataIndex()
   {
      return imap[0].b;
   }

   public int getLastDataIndex()
   {
      return imap[imap.length - 1].b;
   }

   public String toString()
   {
      return String.format("[ErpMap: %d.%d  %d (%d)  %.2f]", iSeries + 1, getFirstDataIndex(),
            getQueryLength(), getDataLength(), score);
   }
}
