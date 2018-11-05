package kdm.data;

import java.util.*;

/** abstract base class for all kdm data objects */
public abstract class Datum
{
   protected HashMap<String,Object> meta; // lazy creation to save space

   /** copy the contents of the given datum into this object */
   public void copyFrom(Datum datum)
   {
      copyMeta(datum);
   }
   
   /** copy meta data from the given datum, overwriting existing entries in this datum */
   public void copyMeta(Datum datum)
   {
      if (datum.meta == null) return;
      if (meta == null) meta = new HashMap<String, Object>();
      meta.putAll(datum.meta);
   }
   
   /** add a new object to this datum's metadata */
   public Object setMeta(String key, Object data)
   {
      if (meta==null) meta = new HashMap<String, Object>();
      return meta.put(key, data);
   }
   
   /** retrieve metadata by name */
   public Object getMeta(String key)
   {
      return meta.get(key);
   }
   
   /** @return requested metadata or default value if metadata unspecified */
   public String getMeta(String key, String def)
   {
      if (containsMeta(key)) return (String)meta.get(key);
      return def;
   }
   
   /** @return requested metadata or default value if metadata unspecified */
   public double getMeta(String key, double def)
   {
      if (containsMeta(key)) return (Double)meta.get(key);
      return def;
   }
   
   /** @return requested metadata or default value if metadata unspecified */
   public int getMeta(String key, int def)
   {
      if (containsMeta(key)) return (Integer)meta.get(key);
      return def;
   }
   
   /** @return requested metadata or default value if metadata unspecified */
   public long getMeta(String key, long def)
   {
      if (containsMeta(key)) return (Long)meta.get(key);
      return def;
   }   
   
   /** remove metadata by name */
   public void removeMeta(String key)
   {
      if (meta!=null) meta.remove(key);
   }
   
   /** @return true if the given metadata key exists */
   public boolean containsMeta(String key)
   {
      if (meta == null) return false;
      return meta.containsKey(key);
   }
}
