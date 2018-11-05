package kdm.util;

/**
 * Implements a simple millisecond timer.  The timer starts running
 * when it is instantiated, and can be reset (timer goes to zero) by
 * calling the reset() method.  The duration is retrieved via time()
 * and the timer can be stopped via mark().
 */
public class TimerMS
{
    protected long last, mark=-1;
    public TimerMS(){ mark=-1; reset();}
    public final long time(){ return (mark==-1 ? (getMS() - last) : (mark-last)); }
    public final long timeMS(){ return time(); }
    public final long timeS(){ return time()*1000; }
    public final void mark(){ mark = getMS(); }
    public final void reset(){ last = getMS(); mark=-1; }
    public final boolean isRunning(){ return (mark==-1); }
    protected final long getMS(){ return System.currentTimeMillis(); }
}
