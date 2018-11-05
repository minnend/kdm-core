package kdm.mlpr.dataTree;

import kdm.util.*;

/**
 * Node in a spill tree
 */
public class SPNode
{
    public static enum Type { metric, spill, error }

    public int nPoints;        // number of points
    public double r;           // radius of bounding hypersphere
    public double c[];         // centroid of points
    public double v[];         // vector for projection
    public double data[][];    // reference to data (n points x d dims)
    public SpanList points;    // list of points contained in the node
    public SPNode left, right; // ptr to left/right child nodes
    public Type type;          // what kind of node is this?

    protected static double z[] = new double[3];      // temp vector

    public SPNode(double _data[][])
    {
        data = _data;
        points = new SpanList(0, _data.length-1, true);
        type = Type.error;
    }

    public SPNode(double _data[][], SpanList _points)
    {
        data = _data;
        points = new SpanList(_points);
        type = Type.error;
    }

    public int height()
    {
        int a = left==null ? 0 : left.height();
        int b = right==null ? 0 : right.height();
        return 1 + Math.max(a,b);
    }

    public int numNodes()
    {
        int a = left==null ? 0 : left.numNodes();
        int b = right==null ? 0 : right.numNodes();
        return 1 + a + b;
    }

    public int numSpill()
    {
        int a = left==null ? 0 : left.numSpill();
        int b = right==null ? 0 : right.numSpill();
        return (type==Type.spill ? 1 : 0) + a + b;
    }

    protected int findFarthestPoint(double x[])
    {
        double maxd;
        int imax;

        maxd = -1.0;
        imax = -1;

        points.itReset();
        while(points.itMore())
        {
            int i = points.itNext();
            double d = Library.dist2(data[i],x);
            if (d > maxd)
            {
                maxd = d;
                imax = i;
            }
        }

        return imax;
    }

    /**
     * Split the data points in this node into left and right child
     * nodes; also, mark the node as spill or metric depending on
     * proportion of split.
     *
     * @param tau - distance from midpoint of overlap boundary
     * @param rho - fraction of points allowed in child; if more than rho,
     *              then this node becomes metric
     */
    public boolean split(double tau, double rho, SPNodeFactory factory)
    {
        // no need to split if there's only one point
        nPoints = points.size();
        assert(nPoints > 0);
        if (nPoints < 10) return false;

        int ndims = data[0].length;
        int i, lp, rp;
        double x[], y[];
        double d;

        if (z.length != ndims) z = new double[ndims];

        // pick a random point
        i = (int)(nPoints * Library.random());
        lp = points.get(i);

        // approx two farthest points
        rp = findFarthestPoint(data[lp]);
        y = data[rp];
        lp = findFarthestPoint(y);
        x = data[lp];

        // find the centroid
        c = new double[ndims];
        for(i=0; i<ndims; i++) c[i] = (x[i] + y[i]) / 2;

        // find radius
        i = findFarthestPoint(c);
        r = Library.dist(c,data[i]);

        // compute v = rp-lp and normalize v
        v = new double[ndims];
        Library.sub(v, y, c);
        Library.norm(v);

        // project and classify the points
        SpanList pl = new SpanList(0, data.length-1, false);
        SpanList plo = new SpanList(0, data.length-1, false);
        SpanList pr = new SpanList(0, data.length-1, false);
        SpanList pro = new SpanList(0, data.length-1, false);
        points.itReset();
        while(points.itMore())
        {
            int ix = points.itNext();
            Library.sub(z, data[ix], c);
            
            // project this point
            d = Library.dot(z,v);
            
            // compare point to various bounds and add to proper list
            if (d >= 0) // right or left of midpoint?
            {
                if (d >= tau) pr.add(ix);
                else pro.add(ix);
            }
            else{
                if (d <= -tau) pl.add(ix);
                else plo.add(ix);
            }
        }

        int nov = plo.size() + pro.size();
        int nlt = pl.size() + nov;
        int nrt = pr.size() + nov;
        int nmax = (int)(rho * nPoints);
        
        // metric or spill node?
        if (nlt > nmax || nrt > nmax)
        {
            type = Type.metric;
        }
        else{
            type = Type.spill;
            plo.add(pro);
            pro = plo;
        }

        // create child nodes
        pl.add(plo);
        pr.add(pro);
        left = factory.create(data, pl);
        right = factory.create(data, pr);

        // recursively split children
        left.split(tau, rho, factory);
        right.split(tau, rho, factory);

        return true;
    }
}
