package kdm.mlpr.dataTree;

import kdm.util.*;

/**
 * Factory class for creating SPNode objects
 */
public class SPNodeFactory
{
    public SPNode create(double data[][])
    { return new SPNode(data); }

    public SPNode create(double data[][], SpanList span)
    { return new SPNode(data, span); }
}
