package kdm.data;

/**
 * Represents a y-coordinate and its corresponding value
 */
public class ValueY
{
    public int y;        // y screen coordinate
    public double v;     // value at this point
    
    public ValueY(int _y, double _v)
    {
        y = _y;
        v = _v;
    }
}
