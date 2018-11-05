package kdm.io.DataSaver;

import java.util.*;
import java.io.*;
import kdm.data.*;
import kdm.util.*;

/**
 * Abstract base class for other classes that save data to different file formats
 */
public abstract class DataSaver implements Configurable
{
    public abstract boolean save(Sequence seq, String path);

    public boolean config(ConfigHelper chelp, String sKey, String sVal){ return true; }

    public boolean config(File fBase, String s)
    {
        // grrr: no multiple inheritance in java
       ConfigHelper chelp = new ConfigHelper(fBase);
       return chelp.config(s, this);
    }
}
