package kdm.mlpr;

import kdm.util.*;

public class KDE extends SPTree
{
    /**
     * Constructus a KDE spill-tree for the given data using the
     * specified parameters tau and rho.
     */
    public KDE(double _data[][])
    { super(_data, 0, 0, new KdeNodeFactory()); }

    /**
     * Computes the KDE at the specified data point.
     */
    public double calcKDE(double x[], double kw, double threshDiff)
    {
        KdeNode.setup(kw);
        return ((KdeNode)root).calcKDE(x, threshDiff);
    }

    public double[] calcAllKDE(double kw, double threshDiff)
    {
        KdeNode.setup(kw);
        double[] kde = new double[data.length];
        assert false : "not yet implemented"; // TODO
        //((KdeNode)root).calcAllKDE(kde, threshDiff);
        return kde;
    }

    public double calcSlow(double y[], double kw)
    {
        double x[][] = root.data;
        double kde = 0.0;
        for(int i=0; i<x.length; i++)
        {
            double d = 0.0;
            for(int j=0; j<y.length; j++)
            {
                double c = y[j] - x[i][j];
                d += c*c;
            }
            d = Math.sqrt(d);
            kde += Library.gaussV(d, 0, kw);
        }
        return kde;
    }

    public static void main(String args[])
    {
        TimerMS timer = new TimerMS();
        long tm;

        double x[][] = Library.read(args[1], Library.MatrixOrder.RowMajor);
        System.err.println("data points: "+x.length+"  dims: "+x[0].length);
        double y[][] = Library.read(args[0], Library.MatrixOrder.RowMajor);
        System.err.println("query points: "+y.length+"  dims: "+y[0].length);

        double kw = 0.002;
        double res[] = new double[y.length];
        double thresh = 1e-15;

        // build the tree
        timer.reset();
        KDE kde = new KDE(x);
        tm = timer.time();
        System.err.println("build time: "+tm);

        timer.reset();
        for(int i=0; i<y.length; i++)
        {
            double kde1 = kde.calcKDE(y[i], kw, thresh);
            res[i] = kde1;
        }
        tm = timer.time();
        System.err.println("time1: "+tm);
        
        double avg = 0.0;
        timer.reset();
        for(int i=0; i<y.length; i++)
        {
            double kde2 = kde.calcSlow(y[i], kw);
            res[i] -= kde2;
            avg += Math.abs(res[i]) / kde2;
            //assert(Math.abs(res[i]) < 1e-6) : "i="+i+"   "+"kde="+kde2+"    error="+res[i];
        }
        tm = timer.time();
        System.err.println("time2: "+tm);
        avg /= y.length;
        System.err.println("avg percent error: "+avg);
    }
}

//////////////////////////////////////////////////////////////////////

class KdeNodeFactory extends SPNodeFactory
{
    public SPNode create(double data[][])
    { return new KdeNode(data); }

    public SPNode create(double data[][], SpanList span)
    { return new KdeNode(data, span); }
}

//////////////////////////////////////////////////////////////////////

class KdeNode extends SPNode
{
    // params for Gaussian kernel that are precomputed
    protected static double KA = Library.INF;
    protected static double KB = Library.INF;

    public KdeNode(double _data[][]){ super(_data); }

    public KdeNode(double _data[][], SpanList _points){ super(_data, _points); }

    /**
     * Perform preprocessing based on the given kernel width.
     */
    public static void setup(double kw)
    {
        KA = 1.0 / (Library.SQRT_2PI * Math.sqrt(kw));
        KB = 1.0 / (-2.0 * kw);
    }

    /**
     * Computes the KDE for the given distance.
     */
    protected static double calcKDE(double d)
    {
        return KA * Math.exp(d * KB);
    }

    /**
     * Computes the KDE between the given arbitray point (x) and the
     * given data point (index = i).
     */
    protected double calcKDE(double x[], int i)
    {
        double d = 0.0;
        for(int j=0; j<x.length; j++)
        {
            double y = x[j] - data[i][j]; 
            d += y*y;
        }
        return calcKDE(d);
    }

    /**
     * Computes the KDE between the given arbitrary point (x) and all
     * points in this node.
     *
     * @param x - data point for which the KDE is computed
     * @param threshdiff - if the diff between the min and max KDE in
     *        this node is less than this threshold, then the node is
     *        approximated by the average KDE.
     */
    public double calcKDE(double x[], double threshDiff)
    {
        assert (KA!=Library.INF) : "Invalid KA; you probably forgot to setup() the KdeNode";

        double kde = 0.0;

        // if this is a leaf node, add contribution of each point
        if (left==null && right==null)
        {            
            points.itReset();
            while(points.itMore())
            {                
                int i = points.itNext();
                kde += calcKDE(x, i);
            }
        }
        else{
            // see if we can prune this node
            double d = Library.dist(c, x);
            if (d < r) // nope
            {
                kde += ((KdeNode)left).calcKDE(x, threshDiff);
                kde += ((KdeNode)right).calcKDE(x, threshDiff);
            }
            else{
                double minKDE = calcKDE(d + r);
                double maxKDE = calcKDE(d - r);
                assert maxKDE>minKDE;
                if (maxKDE - minKDE < threshDiff)
                {
                    // approx node by <average KDE> * <number of points>
                    kde += calcKDE(d) * nPoints;
                }
                else{
                    kde += ((KdeNode)left).calcKDE(x, threshDiff);
                    kde += ((KdeNode)right).calcKDE(x, threshDiff);
                }
            }
        }
        return kde;
    }
}
