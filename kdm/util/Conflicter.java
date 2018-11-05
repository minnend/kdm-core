package kdm.util;

/** lets an object specify if it is in conflict with another object; the meaning of "conflict" is contextual */ 
public interface Conflicter
{
   public boolean hasConflict(Object o);
}
