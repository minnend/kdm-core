package kdm.io;

/**
 * Marks a class as having a text-based represetation.
 */
public interface TextRep
{
   public String toText();
   public int fromText(String s);
}
