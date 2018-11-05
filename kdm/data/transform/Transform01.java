package kdm.data.transform;

import kdm.data.*;
import java.util.*;

public class Transform01 extends DataTransform
{
    public Sequence transform(Sequence _data)
    {
        Sequence data = new Sequence("ZeroOne: "+_data.getName(),
                                     _data.getFreq(), _data.getStartMS());
        int iDate = 0;
        int nDims = _data.getNumDims();
        for(int i=0; i<_data.length(); i++)
        {
            FeatureVec fv = _data.get(i);
            for(int j=0; j<nDims; j++)
            {
                if (fv.get(j) > 0) fv.set(j, 1.0);
                else if (fv.get(j) < 0) fv.set(j, -1.0);
                else fv.set(j, 0.0);
            }
            data.add(fv);
        }
        data.copyMeta(_data);
        return data;
    }
    
    public void dumpParams()
    {
       System.err.printf("%s: no params\n", getClass());
    }
}
