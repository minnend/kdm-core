package kdm.util;

public class TTResult
{
    public enum Tails { One, Two }

    public double t, p;
    protected Tails tails = Tails.Two;

    public TTResult(double _t, double _p, Tails _tails)
    {
        t = _t;
        p = _p;
        tails = _tails;
    }

    public boolean isTwoTailed(){ return (tails == Tails.Two); }
    public boolean isOneTailed(){ return (tails == Tails.One); }
}
