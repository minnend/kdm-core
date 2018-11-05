package kdm.metrics;

import kdm.data.FeatureVec;

/**
 * Computes the L_p norm distance between the feature vectors.
 */
public class LpNormFV extends MetricFV
{
    double P;
    boolean bRoot;

    public LpNormFV(double _norm){ this(_norm, true); }
    public LpNormFV(double _norm, boolean _bRoot){ P=_norm; bRoot=_bRoot; }

    public double dist(FeatureVec a, FeatureVec b)
    {
        double d = 0.0;
        int nd = a.getNumDims();
        assert(nd == b.getNumDims());
        for(int i=0; i<nd; i++) d += Math.pow(Math.abs(a.get(i)-b.get(i)),P);
        if (bRoot) return Math.pow(d,1.0/P);
        else return d;
    }
}
