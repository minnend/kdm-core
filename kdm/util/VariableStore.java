package kdm.util;

import java.util.*;

/**
 * Stores variables and values
 */
public class VariableStore extends HashMap<String,Object>
{
   public int getAsInt(String sVar)
   {
      try{ int x = Integer.parseInt(sVar); return x;}
      catch(NumberFormatException e){}
      Object o = get(sVar);
      if (o == null) throw new IllegalArgumentException("Variable does not exist: "+sVar);
      if (o instanceof Integer) return (Integer)o;
      if (o instanceof String) return Integer.parseInt((String)o);      
      return Integer.parseInt(o.toString());
   }
   
   public String getAsString(String sVar)
   {
      Object o = get(sVar);
      if (o == null) throw new IllegalArgumentException("Variable does not exist: "+sVar);
      if (o instanceof String) return (String)o;
      return o.toString();
   }
   
   public String resolveVar(String sVar)
   {
      StringBuffer sb = new StringBuffer();
      int iStart = 0;
      int n = sVar.length();
      while (iStart < n)
      {
         int i = sVar.indexOf('[', iStart);
         if (i<0) break;
         int j = sVar.indexOf(']', i+1);
         if (j<0) break;
         if (i+1==j)
         {
            sb.append(sVar.substring(iStart, j));
            iStart = j+1;
            continue;
         }
         
         String sInner = sVar.substring(i+1,j);
         sInner = resolveVar(sInner);         
         if (containsKey(sInner)) sInner = getAsString(sInner);
         else System.err.printf("%s not a variable\n", sInner);
         sb.append(String.format("%s[%s]", sVar.substring(iStart, i), sInner));
         iStart = j+1;
      }
      if (iStart < n) sb.append(sVar.substring(iStart));
      return sb.toString();
   }
   
   public void dump()
   {
      System.err.printf("Variable Store\n");
      Iterator<String> it = keySet().iterator();
      while(it.hasNext())
      {
         String s = it.next();
         System.err.printf(" %s = %s\n", s, getAsString(s));
      }
   }
}
