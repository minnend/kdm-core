package kdm.util;

import java.io.*;

/**
 * marks an object as configurable, which means that it can take key/value strings and
 * setup internal parameters.
 */
public interface Configurable
{
   /** Called for each parameter */
   public boolean config(ConfigHelper chelp, String key, String val);

   /** called with the full config string, uses ConfigHelper to parse and forward params */
   public boolean config(File fBase, String s);
}
