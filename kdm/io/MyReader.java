package kdm.io;

import java.io.*;

public interface MyReader
{
    public abstract String readLine() throws IOException;
    public abstract String readFile() throws IOException;
    public abstract void close() throws IOException;
}
