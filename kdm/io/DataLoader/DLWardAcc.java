package kdm.io.DataLoader;

import kdm.data.*;
import static java.util.Calendar.*;
import java.util.*;
import java.io.*;

/**
 * Loads data from one of Jamie Ward's wood workshop activity
 * acclerometer files.  These data files were collected at ETH
 * and consist of four 3-axis accelerometer readings.  The
 * absolute time of the data is taken from the last-modified
 * time of the corresponding file.  The sampling rate is 100 Hz
 *
 * Acc_1 = columns 2,3,4
 * Acc_2 = columns 5,6,7
 * Acc_3 = columns 8,9,10
 * Acc_4 = columns 11,12,13
 */
public class DLWardAcc extends DataLoader
{
    public Sequence load(String path)
    {
        LineNumberReader in = null;
        double v[] = new double[12];
        try{
            File file = new File(path);
            in = new LineNumberReader(new FileReader(file));
            String line;
            Sequence data = null;
            StringTokenizer st;
            long ms = file.lastModified();
            
            System.err.println("Loading data file: "+path);

            while((line = in.readLine()) != null)
            {
                // skip empty lines
                if (line.length() == 0) continue;

                st = new StringTokenizer(line, " \t\r\n");
                st.nextToken(); // skip time index
                
                // read the 12 accelerometer values (4 accelerometers, 3 axes each)
                for(int i=0; i<12; i++) v[i] = Double.parseDouble(st.nextToken());

                // add to the dataset (and create it if necessary)
                if (data == null)
                {
                    // we know that this is 100Hz data
                    data = new Sequence("Ward Accel", 100.0);
                    data.add(new FeatureVec(v),ms);
                }
                else data.add(new FeatureVec(v));
            }

            return data;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
        finally{
            try{ if (in!=null) in.close(); } catch(IOException ioe){ ioe.printStackTrace(); }
        }
    }
} 
