package kdm.io;

import java.util.*;

/** Abstract parent class that ensure that an object can extract a Date from a string */
public interface DateExtracter
{
   public Date extractDate(String s);
}
