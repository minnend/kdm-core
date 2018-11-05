package kdm.metrics;
import kdm.data.FeatureVec;

/**
 * Computes the absolute difference between the FVs.  This equals the
 * sum of the absolute differences between the components of
 * <code>a</code> and <code>b</code>.
 */
public class AbsoluteDistFV extends MetricFV
{
    public double dist(FeatureVec a, FeatureVec b)
    {
        return a.absdist(b);
    }
}
