package kdm.metrics;

import kdm.data.*;
import kdm.util.*;

import java.util.*;
import java.util.zip.*;
import java.io.*;

public class CompressDist extends MetricSeq
{
    public static enum Method { zip, gzip }

    protected Method method;

    public CompressDist(){ this(Method.gzip); }

    public CompressDist(Method _method)
    {
        method = _method;
    }

    public double calcDist(Sequence a, WindowLocation winA, Sequence b, WindowLocation winB)
    {
        int x = compress(a, winA);
        int y = compress(b, winB);
        int xy = compress(a, winA, b, winB);
        return 1.0 - (double)xy / (double)(x + y);
    }

    protected int compress(Sequence seq, WindowLocation win)
    {
        try{
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DeflaterOutputStream comp = null;
            ObjectOutputStream out = null;

            if (method == Method.zip)
            {
                comp = new ZipOutputStream(bout);
                ((ZipOutputStream)comp).putNextEntry(new ZipEntry(""));
            }
            else if (method == Method.gzip) comp = new GZIPOutputStream(bout);
            out = new ObjectOutputStream(comp);

            int a = win.start();
            int b = win.end();
            int nd = seq.get(a).getNumDims();
            for(int j=0; j<nd; j++)
                for(int i=a; i<b; i++)
                    out.writeDouble(seq.get(i,j));

            out.close();

            return bout.size();
        }catch(IOException ioe){ ioe.printStackTrace(); }
        
        assert false;
        return 0;
    }

    protected int compress(Sequence seqA, WindowLocation winA, Sequence seqB, WindowLocation winB)
    {
        try{
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DeflaterOutputStream comp = null;
            ObjectOutputStream out = null;

            if (method == Method.zip)
            {
                comp = new ZipOutputStream(bout);
                ((ZipOutputStream)comp).putNextEntry(new ZipEntry(""));
            }
            else if (method == Method.gzip) comp = new GZIPOutputStream(bout);
            out = new ObjectOutputStream(comp);

            int a = winA.start();
            int b = winA.end();
            int nd = seqA.get(a).getNumDims();
            for(int j=0; j<nd; j++)
                for(int i=a; i<b; i++)
                    out.writeDouble(seqA.get(i,j));

            a = winB.start();
            b = winB.end();
            for(int j=0; j<nd; j++)
                for(int i=a; i<b; i++)
                    out.writeDouble(seqB.get(i,j));

            out.close();

            return bout.size();
        }catch(IOException ioe){ ioe.printStackTrace(); }
        
        assert false;
        return 0;
    }

}
