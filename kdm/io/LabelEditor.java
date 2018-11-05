package kdm.io;

/**
 * Provides a method that adjusts a label (a string) and returns the new label
 */
public interface LabelEditor
{
   /** modify a label */
   public String adjustLabel(String sLabel);
   
   /** @return true = keep the label; false = don't use it */ 
   public boolean keepLabel(String sLabel);
}
