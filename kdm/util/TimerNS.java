package kdm.util;

/**
 * Implements a simple nanosecond timer.  The timer starts running
 * when it is instantiated, and can be reset (timer goes to zero) by
 * calling the reset() method.  The duration is retrieved via time()
 * and the timer can be stopped via mark().
 */
public class TimerNS
{
    protected long last, mark=-1;
    public TimerNS(){ mark=-1; reset();}
    public final long time(){ return (mark==-1 ? (getNS() - last) : (mark-last)); }
    public final long timeNS(){ return time(); }
    public final double timeUS(){ return time()/1000.0; }
    public final double timeMS(){ return time()/1000000.0; }
    public final void mark(){ mark = getNS(); }
    public final void reset(){ last = getNS(); mark=-1; }
    public final boolean isRunning(){ return (mark==-1); }
    protected final long getNS(){ return System.nanoTime(); }
}
