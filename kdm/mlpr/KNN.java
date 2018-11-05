package kdm.mlpr;

import kdm.util.*;

public class KNN extends SPTree
{
    /**
     * Constructus a KNN spill-tree for the given data using the
     * specified parameters tau and rho.
     *
     * @param data - [NxD] matrix of N data points with D dimensions
     * @param tau - distance from midpoint of overlap boundaries 
     * @param rho - max fraction of points in a spill child node before
     *               node is switched to metric style
     */
    public KNN(double data[][], double tau, double rho)
    { super(data, tau, rho, new KnnNodeFactory()); }

    /**
     * Locates the k-nearest-neighbors to the specified data point.
     */
    public double[][] find(double x[], int k)
    {
        int iknn[] = new int[k];
        ((KnnNode)root).find(x, iknn);

        double vnn[][] = new double[k][];
        for(int i=0; i<k; i++) vnn[i] = data[iknn[i]];
        return vnn;
    }

    /**
     * Locates the k-nearest-neighbors to the specified data point and
     * returns their indices.
     */
    public int[] findi(double x[], int k)
    {
        int iknn[] = new int[k];
        ((KnnNode)root).find(x, iknn);
        return iknn;
    }

    /**
     * Locates the k-nearest-neighbors to ix^th data point.
     */
    public int[] find(int ix, int k)
    {
        int iknn[] = new int[k];
        ((KnnNode)root).find(data[ix], iknn);
        return iknn;
    }

    public static void main(String args[]) // for debugging / testing
    {
        TimerMS timer = new TimerMS();
        long tm;

        double x[][] = Library.read(args[1], Library.MatrixOrder.RowMajor);
        System.err.println("data points: "+x.length+"  dims: "+x[0].length);
        double y[][] = Library.read(args[0], Library.MatrixOrder.RowMajor);
        System.err.println("query points: "+y.length+"  dims: "+y[0].length);

        // build the tree
        timer.reset();
        KNN knn = new KNN(x, 0, .7);
        tm = timer.time();
        System.err.println("build time: "+tm);
        System.err.println("Spill: "+knn.root.numSpill()+" / "+knn.root.numNodes());

        // find knn for all query points
        timer.reset();
        for(int i=0; i<y.length; i++)
        {
            //System.err.printf("query %d: (%.3f, %.3f)\n", i, y[i][0], y[i][1]);
            double[][] z = knn.find(y[i], 4);
        }
        tm = timer.time();
        System.err.println("query time: "+tm);

        // find max dist to knn for all points
        timer.reset();
        for(int i=0; i<x.length; i++)
        {
            double[][] z = knn.find(x[i], 4);
        }
        tm = timer.time();
        System.err.println("query time: "+tm);

        /*for(int i=0; i<y.length; i++)
        {
            System.err.printf("query point %d: (%.0f, %.0f)\n", i, y[i][0], y[i][1]);
            double[][] z = knn.find(y[i], 4);
            for(int j=0; j<z.length; j++)
                System.err.printf("   (%.0f, %.0f)", z[j][0], z[j][1]);
            System.err.println();
            }*/
    }
}


//////////////////////////////////////////////////////////////////////

class KnnNodeFactory extends SPNodeFactory
{
    public SPNode create(double data[][])
    { return new KnnNode(data); }

    public SPNode create(double data[][], SpanList span)
    { return new KnnNode(data, span); }
}

//////////////////////////////////////////////////////////////////////

class KnnNode extends SPNode
{
    public KnnNode(double _data[][]){ super(_data); }

    public KnnNode(double _data[][], SpanList _points){ super(_data, _points); }

    public boolean find(double x[], int nn[])
    {
        int k = nn.length;
        AugKNN aug = new AugKNN(k);

        find(x, aug);

        for(int i=0; i<k; i++) nn[i] = aug.index[i];
        return (aug.getMaxDist() < Double.POSITIVE_INFINITY);
    }

    protected boolean find(double x[], AugKNN aug)
    {
        // if this is a leaf node, check all points
        if (left==null && right==null)
        {
            points.itReset();
            while(points.itMore())
            {
                int i = points.itNext();
                aug.update(i, x, data);
            }
            return true;
        }

        // not a leaf node, so see if we can prune
        double maxd = aug.getMaxDist();  // dist to kth nearest neighbor
        double d = Library.dist2(c, x);  // dist to node center
        double d2 = r + maxd;            // maxd + radius must be > dist to centroid
        if (d <= d2*d2)
        {
            if (type == Type.metric)
            {
                // this is the easy case, just recurse for both children
                ((KnnNode)left).find(x, aug);
                ((KnnNode)right).find(x, aug);
            }
            else{
                // only recurse for one child based on projection
                if (z.length != x.length) z = new double[x.length];
                Library.sub(z, x, c);
                if (Library.dot(z,v) >= 0)
                {
                    ((KnnNode)right).find(x, aug);
                    if (aug.nFound < aug.k) ((KnnNode)left).find(x, aug);
                }
                else{
                    ((KnnNode)left).find(x, aug);
                    if (aug.nFound < aug.k) ((KnnNode)right).find(x, aug);
                }
            }
        }
        //else System.err.println("Prune: "+nPoints);

        return false;
    }
}

//////////////////////////////////////////////////////////////////////

class AugKNN
{
    public int k;
    public int index[];
    public double dist[];
    public double maxd;
    public int nFound;

    public AugKNN(int _k)
    {
        k = _k;
        nFound = 0;
        index = new int[k];
        dist = new double[k];
        for(int i=0; i<k; i++)
        {
            index[i] = -1;
            dist[i] = Library.INF;
        }
        maxd = Library.INF;
    }

    public final double getMaxDist(){ return maxd; }

    public final boolean update(int ix, double x[], double data[][])
    {
        int i, j;

        // TODO: this is a linear implementation
        //      we could use binary search
        //      and a linked list to improve efficiency

        // ensure no dup insertions
        for(i=0; i<k; i++) if (index[i] == ix) return false;

        // determine appropriate position in list
        double d = Library.dist2(x, data[ix]);
        i = 0;
        while(i<k && d>dist[i]) i++;
        if (i >= k) return false; // off the list

        // move everything down one slot
        for(j=k-1; j>i; j--)
        {
            index[j] = index[j-1];
            dist[j] = dist[j-1];
        }

        // fill in the new point
        index[i] = ix;
        dist[i] = d;
        nFound++;

        // update max distance
        maxd = Math.sqrt(dist[k-1]);
        return true;
    }
}
