package kdm.io;

import java.io.*;

import kdm.data.*;

/**
 * Save Markup data in the general format (see toText() in the TimeMarker class)
 */
public class MSGeneral implements MarkupSaver
{
    public boolean save(MarkupSet marks, String path)
    {
        if (marks.size() == 0) return false;

        PrintWriter out = null;
        try{
            out = new PrintWriter(new BufferedWriter(new FileWriter(path)));
            for(TimeMarker mark : marks.getList()) out.println(mark.toText());
        }
        catch(IOException ioe){ return false; }
        if (out!=null) out.close();
        return true;
    }
}
