package kdm.mlpr.htk;

import java.io.*;
import java.util.*;
import kdm.data.*;
import kdm.models.*;

/** setup info for running HVite for continuous recognition */
public class HtkSetupInfo
{
   public HtkSetupInfo(ArrayList<AbstractHMM> hmms, ArrayList<Sequence> seqs)
   {
      this.hmms = hmms;
      this.seqs = seqs;         
   }
   
   public File fDict, fCommands, fGrammar, fLattice, fMlf;
   public ArrayList<File> fExt;
   public int sampPer;
   public ArrayList<AbstractHMM> hmms;
   public ArrayList<Sequence> seqs;
}
