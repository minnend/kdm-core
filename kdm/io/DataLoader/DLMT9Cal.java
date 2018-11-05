package kdm.io.DataLoader;

import kdm.data.*;
import kdm.util.*;
import java.util.*;
import java.io.*;

import static java.util.Calendar.*;

/**
 * Loads data from a raw file with one time step per line.  The
 * data is assumed to be sampled at 1HZ.
 */
public class DLMT9Cal extends DataLoader
{
    final static String delims = " \t\r\n";
    boolean dims[] = new boolean[11];

    public DLMT9Cal()
    {
        Arrays.fill(dims, false);
    }

    public boolean config(ConfigHelper chelp, String sKey, String sVal)
    {
        boolean b = ConfigHelper.isTrueString(sVal);

        if (sKey.compareToIgnoreCase("acc")==0)
        {
            dims[1] = dims[2] = dims[3] = b;
        }
        else if (sKey.compareToIgnoreCase("gyr")==0)
        {
            dims[4] = dims[5] = dims[6] = b;
        }
        else if (sKey.compareToIgnoreCase("mag")==0)
        {
            dims[7] = dims[8] = dims[9] = b;
        }
        else if (sKey.compareToIgnoreCase("temp")==0)
        {
            dims[10] = b;
        }
        else if (sKey.startsWith("dim"))
        {
            try{
                int i = Integer.parseInt(sKey.substring(3));
                dims[i] = b;
            }
            catch(NumberFormatException nfe)
            {
                nfe.printStackTrace();
                return false;
            }
        }
        else super.config(chelp, sKey, sVal);
        return true;
    }

    public DLMT9Cal(boolean bAcc, boolean bGyr, boolean bMag, boolean bTemp)
    {
        Arrays.fill(dims, false);
        if (bAcc) dims[1] = dims[2] = dims[3] = true;
        if (bGyr) dims[4] = dims[5] = dims[6] = true;
        if (bMag) dims[7] = dims[8] = dims[9] = true;
        if (bTemp) dims[10] = true;
    }

    public DLMT9Cal(boolean _dims[])
    {
        assert _dims.length <= dims.length;
        Arrays.fill(dims, false);
        for(int i=1; i<_dims.length; i++) dims[i] = _dims[i];
    }

    public DLMT9Cal(int _dims[])
    {
        Arrays.fill(dims, false);
        for(int i=0; i<_dims.length; i++)
        {
            assert _dims[i]>0 && _dims[i]<dims.length;
            dims[_dims[i]] = true;
        }
    }

    public Sequence load(String path)
    {
        LineNumberReader in = null;
        try{
            File file = new File(path);
            in = new LineNumberReader(new FileReader(file));
            String line;
            Sequence data = null;
            double v[] = null;
            StringTokenizer st;
            long ms = file.lastModified();
            int nDims = 0;

            // figure out how many dimensions we have
            for(int i=0; i<dims.length; i++)
            {
                if (dims[i]) nDims++;
            }
            System.err.println("MT9 Cal: "+nDims+" dims");
            v = new double[nDims];

            System.err.println("Loading data file: "+path);

            while((line = in.readLine()) != null)
            {
                // skip empty lines
                if (line.length() == 0) continue;
                
                // setup string tokenizer
                st = new StringTokenizer(line, delims);

                // parse this line's data
                int j = 0;
                for(int i=0; i<dims.length; i++)
                {
                    String s = st.nextToken();
                    if (dims[i])
                    {
                        v[j] = Double.parseDouble(s);
                        j++;
                    }
                }

                // add to the dataset (and create it if necessary)
                if (data == null)
                {
                    // we know that this is 100Hz data
                    data = new Sequence("MT9 Calib", 100.0);
                    data.add(new FeatureVec(v),ms);
                }
                else data.add(new FeatureVec(v));
            }

            System.err.println("MT9 Cal: "+in.getLineNumber()+" lines read.");
            in.close();
            return data;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
} 
