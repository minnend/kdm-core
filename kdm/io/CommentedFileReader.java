package kdm.io;

import java.io.*;

public class CommentedFileReader extends Reader implements MyReader
{
    protected boolean bBuf = true;
    protected Reader reader = null;
    protected char commentChar = 0;

    public CommentedFileReader(Reader _reader, char _commentChar)
    {
        if (_reader instanceof MyReader)
        {
            bBuf = false;
            reader = _reader;
        }
        else if (_reader instanceof BufferedReader) reader = _reader;
        else reader = new BufferedReader(_reader);
        commentChar = _commentChar;
    }

    public String readFile() throws IOException
    {
        StringBuffer sb = new StringBuffer();
        String line;
        while((line = readLine()) != null)
        {
            sb.append(line);
            sb.append('\n');
        }
        return sb.toString();
    }

    public String readLine() throws IOException
    {
        while(true)
        {
            String line = null;
            if (bBuf) line = ((BufferedReader)reader).readLine();
            else line = ((MyReader)reader).readLine();
            if (line == null) return null;

            if (commentChar != 0)
            {
                int i = line.indexOf(commentChar);
                if (i>=0) line = line.substring(0, i).trim();
            }
            if (line.length() > 0) return line;
        }
    }

    public int read(char[] cbuf, int off, int len)
    {
        assert false : "not yet implemented";
        return 0;
    }

    public void close() throws IOException { reader.close(); }
}
