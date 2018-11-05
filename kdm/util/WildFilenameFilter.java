package kdm.util;

import java.io.*;
import java.util.regex.*;

public class WildFilenameFilter implements FilenameFilter
{
    protected RegexFilenameFilter rff;

    public WildFilenameFilter(String _sWild)
    {        
        Matcher m;
        String sRegex = _sWild;

        // convert . to \.
        m = Pattern.compile("\\.").matcher(sRegex);
        sRegex = m.replaceAll("\\\\.");

        // convert * to .*
        m = Pattern.compile("\\*").matcher(sRegex);
        sRegex = m.replaceAll(".*");
        
        // convert ? to .
        m = Pattern.compile("\\?").matcher(sRegex);
        sRegex = m.replaceAll(".");

        //System.err.printf("input: %s\noutput: %s\n", _sWild, sRegex); // TODO
        rff = new RegexFilenameFilter(sRegex);
    }

    public boolean accept(File dir, String name){ return rff.accept(dir, name); }
}
