package kdm.mlpr.suffix_tree;

class ListOfTokens
{

   /**
    * head of the list
    */
   private Token head;

   ListOfTokens()
   {
      this.head = null;
   }

   /**
    * returns token position inserts token as string if the token is not already there
    */
   final int insertToken(String string)
   {
      int position = 0;

      if (this.head == null)
      {
         // empty list
         head = new Token();
         head.setToken(string);
      }
      else
      {
         Token current = this.head;
         while(current != null)
         {
            // list not at end
            if (!(current.getToken()).equals(string))
            { // current element different from token
               position++;
               if (current.getNext() != null)
               { // there are more to check
                  current = current.getNext();
               }
               else
               { // no more to check, have to insert
                  Token temp = new Token();
                  temp.setToken(string);
                  current.setNext(temp);
                  break;
               }
            }
            else
            { // found it, do not insert
               break;
            }
         }
      }

      return position;
   }

   /** @return token at the given position */
   final Token getToken(int index)
   {
      Token token = head;
      while(index > 0)
      {
         token = token.getNext();
         if (token == null) throw new ArrayIndexOutOfBoundsException(index);
         index--;
      }
      return token;
   }

   /**
    * @return char at the position "position", where position is counted from the first symbol of the first
    *         token (cumulative position)
    */
   final char getChar(int leftIndex)
   {
      int count = 0;
      Token current = this.head;
      int len = 0;
      while(leftIndex >= count + (len = current.getLen()))
      {
         count += len;
         current = current.getNext();
         if (current == null) throw new ArrayIndexOutOfBoundsException(leftIndex);
      }
      return current.getToken().charAt(leftIndex - count);
   }

   /**
    * @return substring at the position "position", where position is counted from the first symbol of the
    *         first token (cumulative position)
    */
   final String getSubstring(int leftIndex, int length)
   {
      int count = 0;
      Token next = null;
      Token current = this.head;
      int len = 0;
      while(leftIndex >= count + (len = current.getLen()))
      {
         count += len;
         current = current.getNext();
         if (current == null) throw new ArrayIndexOutOfBoundsException(leftIndex);
      }
      return (current.getToken()).substring(leftIndex - count, leftIndex - count + length);
   }

   /**
    * @return substring at the position "position", to the end of the string, where position is counted from
    *         the first symbol of the first token (cumulative position)
    */
   final String getSubstring(int leftIndex)
   {
      int count = 0;
      Token next = null;
      Token current = this.head;
      int len = 0;
      while(leftIndex >= count + (len = current.getLen()))
      {
         count += len;
         current = current.getNext();
         if (current == null) throw new ArrayIndexOutOfBoundsException(leftIndex);
      }
      return (current.getToken()).substring(leftIndex - count);
   }

   /**
    * @return starting index of the string with the given index
    */
   final int getStart(int indexToken)
   {
      int pos = 0;
      Token cur = this.head;
      for(int i = 0; i < indexToken; i++)
      {
         pos += cur.getLen();
         cur = cur.getNext();
         if (cur == null) throw new ArrayIndexOutOfBoundsException(indexToken);
      }
      return pos;
   }

   /**
    * @return index of the string that contains the given position
    */
   final int getIndex(int position)
   {
      int len, current = 0, index = 0;
      Token p = head;
      while(current + (len = p.getLen()) <= position)
      {
         current += len;
         p = p.getNext();
         index++;
      }
      return index;
   }
   
   /** @return offset of position in the given token (computed if iToken<0) */
   final int getOffset(int iToken, int position)
   {
      if (iToken < 0) iToken = getIndex(position);
      return position - getStart(iToken);
   }

   /**
    * Returns the length of the Longest Common Extension (LCE), starting at position i of p, and j within the
    * list of tokens.
    */
   final int getLCE(String p, int i, int j, int length)
   {

      // Move to j

      Token current = head;
      int count = 0;

      while(j > count + current.getLen())
      {

         count += current.getLen();
         current = current.getNext();

         if (current == null) throw new IndexOutOfBoundsException();
      }

      String token = current.getToken();
      int tokenLen = token.length(), offset = j - count;
      int pLen = p.length(), match = 0;

      while((i < pLen) && (offset < tokenLen) && (match < length) && (p.charAt(i) == token.charAt(offset)))
      {
         i++;
         offset++;
         match++;
      }

      return match;
   }

   /**
    * returns the total length of the strings contained in this list
    */
   final int getTotalLength()
   {
      int len = 0;
      Token cur = this.head;
      while(cur != null)
      {
         len += cur.getLen();
         cur = cur.getNext();
      }
      return len;
   }

   /**
    * @return number of tokens in this list
    */
   public final int getNumTokens()
   {
      int n = 0;
      Token cur = head;
      while(cur != null)
      {
         n++;
         cur = cur.getNext();
      }
      return n;
   }

}
