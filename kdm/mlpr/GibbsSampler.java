package kdm.mlpr;

import kdm.util.*;
import kdm.data.*;
import kdm.models.*;
import kdm.models.misc.*;
import kdm.models.ProbFVModel.*;

import java.util.*;
import org.apache.commons.math.stat.*;

public class GibbsSampler
{
    public static double[][] estimateDist(ArrayList<Sequence> data, int nMarkers, 
                                          ProbSeqModel modelClass, double prior,
                                          int nMinPatLen)
    {
        int nSeqs = data.size();
        int nTotalValues ;
        double[][] dist = new double[nSeqs][];

        // create the distribution structure
        nTotalValues = 0;
        for(int i=0; i<nSeqs; i++)
        {
            int len = data.get(i).length();
            dist[i] = new double[len];
            nTotalValues += len;
        }

        // build a mixture model to estimate the likelihood of each value in the data
        double[] vals = new double[nTotalValues];
        int k=0;
        for(int i=0; i<nSeqs; i++)
            for(int j=0; j<data.get(i).length(); j++)
                vals[k++] = data.get(i).get(j,0);
        double xMin = StatUtils.min(vals);
        double xMax = StatUtils.max(vals);
        double range = xMax - xMin;

        GMM1D mix = null;
        double bestv = Double.NEGATIVE_INFINITY;
        for(int i=0; i<4; i++) // try a few random inits to be safe
        {
            int nMix = 8;
            GMM1D mix2 = new GMM1D(nMix); // TODO: determine best mixture size?
            mix2.setReport(Report.loglik);
            mix2.learn(vals);
            double v = mix2.eval(vals);
            if (v > bestv)
            {
                bestv = v;
                mix = mix2;
            }
        }
        vals = null; // let GC reclaim this space
        //System.err.println("Gibbs init mix ("+bestv+"): "+mix);

        // make the initial estimate of the distribution
        for(int i=0; i<nSeqs; i++)
            for(int j=0; j<data.get(i).length(); j++)
            {
                if (j<nMinPatLen) dist[i][j] = 0.0;
                else{
                    double x = data.get(i).get(j,0);
                    double p = mix.eval(x);
                    dist[i][j] = (1.0 / p) + prior;
                }
            }
        Library.normalize(dist);

        // now we can start with the actual gibbs sampling
        int markers[][] = new int[nMarkers][];
        
        // pick initial marker locations
        for(int i=0; i<nMarkers; i++)
        {
            markers[i] = Library.sample(dist);
            assert (markers[i] != null);
        }

        TimerMS timer = new TimerMS();
        int iLastMarker = -1;
        for(int iter=0; iter<10; iter++)
        {
            // pick a marker to move
            int iMarker;
            do{
                iMarker = (int)(Library.random() * nMarkers);
            } while(iMarker == iLastMarker);
            iLastMarker = iMarker;
            
            // choose a base marker to init the model
            int iBaseMarker;
            do{
                iBaseMarker = (int)(Library.random() * nMarkers);
            } while(iBaseMarker == iMarker);
            int iBaseSeries = markers[iBaseMarker][0];
            int iBaseEnd = markers[iBaseMarker][1];
            assert(iBaseEnd >= nMinPatLen);

            Sequence base = data.get(iBaseSeries).subseq(iBaseEnd-nMinPatLen, iBaseEnd);
            ProbSeqModel baseModel = modelClass.build(base, null);

            // map the base model on to the other marker locations
            ArrayList<Sequence> dtrain = new ArrayList<Sequence>();
            dtrain.add(base);
            for(int i=0; i<nMarkers; i++)
            {
                if (i==iMarker) continue;
                int iSeries = markers[i][0];
                int iEnd = markers[i][1];
                MapStartScore mss = baseModel.findMapStart(data.get(iSeries), iEnd);
                Sequence seq = data.get(iSeries).subseq(mss.start, iEnd+1);
                dtrain.add(seq);
            }
            ProbSeqModel model = modelClass.build(dtrain, null);

            // first compute dist for the base series since it should have the max match
            model.buildPatternMap(data.get(iBaseSeries), 0, -1, -1);
            double mxll = model.getCost(0);
            int n = data.get(iBaseSeries).length();
            for(int i=1; i<n; i++)
            {
                double x = model.getCost(i);
                if (x > mxll) mxll = x;
            }

            // recompute the rest of the distribution
            for(int i=0; i<nSeqs; i++)
            {
                if (i == iBaseSeries) continue; // we've already compute this
                model.buildPatternMap(data.get(i), 0, -1, -1);
                
                int j;
                n = data.get(i).length();
                for(j=0; j<nMinPatLen; j++) dist[i][j] = 0.0;
                for(; j<n; j++) dist[i][j] = Math.exp(model.getCost(j)-mxll);
            }
            Library.normalize(dist);

            // resample the chosen marker
            markers[iMarker] = Library.sample(dist);
        }
        System.err.println("gibbs sampling: "+timer.time()+"ms");

        return dist;
    }

}
