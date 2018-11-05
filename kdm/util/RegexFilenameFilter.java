package kdm.util;

import java.io.*;
import java.util.regex.*;

public class RegexFilenameFilter implements FilenameFilter
{
   protected Pattern pattern;

   public RegexFilenameFilter(String _sReg)
   {
      pattern = Pattern.compile(_sReg);
   }

   public boolean accept(File dir, String name)
   {
      return pattern.matcher(name).matches();
   }
}
