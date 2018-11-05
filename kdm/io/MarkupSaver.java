package kdm.io;

import java.io.*;
import kdm.data.*;

public interface MarkupSaver
{
    public boolean save(MarkupSet marks, String path);
}
