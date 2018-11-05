package kdm.io.DataSaver;

import java.io.*;
import java.util.*;
import kdm.io.*;
import kdm.data.*;

/** Save a sequence in a file using the raw binary format */
public class DSRawBinary extends DataSaver
{
   @Override
   public boolean save(Sequence seq, String path)
   {
      ArrayList<Sequence> seqs = new ArrayList<Sequence>();
      seqs.add(seq);
      return BinaryData.save(new File(path), seqs);
   }

}
