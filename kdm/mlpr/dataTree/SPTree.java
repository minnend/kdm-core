package kdm.mlpr.dataTree;

import kdm.util.*;
import kdm.data.*;

/**
 * Spill Tree
 */
public abstract class SPTree
{
    public double data[][]; // rows are points, cols are dimensions (i.e., NxD)
    public SPNode root;

    /**
     * Constructus a spill-tree for the given data using the specified
     * parameters tau and rho.
     *
     * @param data - [NxD] matrix of N data points with D dimensions
     * @param tau - distance from midpoint of overlap boundaries 
     * @param rho - max fraction of points in a spill child node before
     *               node is switched to metric style
     */
    public SPTree(Sequence data, double tau, double rho)
    {
        this(data.toFrameArray(), tau, rho, new SPNodeFactory());
    }
    
    /**
     * Constructus a spill-tree for the given data using the specified
     * parameters tau and rho.
     *
     * @param data - [NxD] matrix of N data points with D dimensions
     * @param tau - distance from midpoint of overlap boundaries 
     * @param rho - max fraction of points in a spill child node before
     *               node is switched to metric style
     */
    public SPTree(double data[][], double tau, double rho)
    {
        this(data, tau, rho, new SPNodeFactory());
    }

    /**
     * Constructus a spill-tree for the given data using the specified
     * parameters tau and rho.
     *
     * @param data - [NxD] matrix of N data points with D dimensions
     * @param tau - distance from midpoint of overlap boundaries 
     * @param rho - max fraction of points in a spill child node before
     *               node is switched to metric style
     * @param factory - node factory to use
     */
    public SPTree(double data[][], double tau, double rho, SPNodeFactory factory)
    {
        this.data = data;
        root = factory.create(data);
        root.split(tau*tau, rho, factory);
    }

    /**
     * @return i-th data point
     */
    public double[] getData(int i){ return data[i]; }
}
