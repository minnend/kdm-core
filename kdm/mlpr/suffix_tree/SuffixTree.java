package kdm.mlpr.suffix_tree;

import java.util.*;

import kdm.util.*;

public final class SuffixTree
{
   /** the root of the tree */
   private InternalNode root;

   /** the strings in the tree */
   private ListOfTokens tokens;

   /** total number of nodes in the tree */
   private int nNodes;

   /**
    * Alphabet specifies what characters are allowed in the strings added to the suffix tree.
    */
   public SuffixTree()
   {
      this.root = new InternalNode();
      this.tokens = new ListOfTokens();
      this.nNodes = 1;
   }

   /**
    * adds a new string to the suffix tree
    */
   public final int addToken(String token)
   {
      return this.tokens.insertToken(token);
   }

   /**
    * returns the root of the tree
    */
   public final InternalNode getRoot()
   {
      return this.root;
   }

   /**
    * @return left character of the given position; if this is the start of a string, "(char)(iToken+50000)"
    *         is returned
    */
   public char getLeftChar(int pos)
   {
      int iToken = getTokenIndex(pos);
      int index = pos - getStartIndex(iToken);
      if (index == 0) return (char)(50000 + iToken);
      Token token = tokens.getToken(iToken);
      return token.getToken().charAt(index - 1);
   }

   /**
    * @return substring starting at leftIndex and having length length from the string collection
    */
   public final String getSubstring(int leftIndex, int length)
   {
      return this.tokens.getSubstring(leftIndex, length);
   }

   /**
    * @return character at leftIndex (a full position)
    */
   public final char getChar(int leftIndex)
   {
      return tokens.getChar(leftIndex);
   }

   /**
    * Returns the length of the Longest Common Extension (LCE), starting at position pos of the pattern, and
    * leftIndex from the string collection.
    * 
    * @return the length of the longest common extension from position i of the pattern and the starting
    *         position of the label of this node.
    */
   final int getLCE(String pattern, int pos, NodeInterface node)
   {
      return tokens.getLCE(pattern, pos, node.getLeftIndex(), node.getLength());
   }

   /**
    * returns the suffix starting at leftIndex
    */
   public final String getSubstring(int leftIndex)
   {
      return this.tokens.getSubstring(leftIndex);
   }

   /**
    * @return starting index of the token at indexToken
    */
   public final int getStartIndex(int indexToken)
   {
      return this.tokens.getStart(indexToken);
   }

   /**
    * @return index of the token that contains position.
    */
   public final int getTokenIndex(int position)
   {
      return this.tokens.getIndex(position);
   }
   
   /** @return offset in the proper token for the given position (iToken computed if param < 0) */
   public final int getTokenOffset(int iToken, int position)
   {      
      return tokens.getOffset(iToken, position);
   }

   /**
    * returns the total length of the strings in the tree
    */
   public final int getTotalLength()
   {
      return this.tokens.getTotalLength();
   }

   /**
    * returns the total no of nodes in the tree
    */
   public final int getNumNodes()
   {
      return this.nNodes;
   }

   /**
    * updates the total no of nodes in the tree, after a new string was inserted
    */
   final void updateNumNodes(int value)
   {
      this.nNodes += value;
   }

   public final int getNumTokens()
   {
      return tokens.getNumTokens();
   }

   /** Remove all info objects from this suffix tree */
   public void clearInfo()
   {
      clearInfo(root);
   }

   /** Remove all info objects from the substree rooted at the given node */
   protected void clearInfo(NodeInterface node)
   {
      if (node == null) return;

      node.addInfo(null); // clear info from this node

      // clear info from all child nodes
      if (node instanceof InternalNode)
      {
         NodeInterface kid = (NodeInterface)((InternalNode)node).getFirstChild();
         while(kid != null)
         {
            clearInfo(kid);
            kid = (NodeInterface)kid.getRightSibling();
         }
      }
   }

   /**
    * mark all nodes that represent maximal repeats with a MaxRepeatInfo object
    * 
    * @param bClearInfo remove all other info tags if true
    */
   public int findMaxRepeats(boolean bClearInfo)
   {
      if (bClearInfo) clearInfo();
      int nRep = findMaxRepeats(root);

      // don't count the root
      if (nRep > 0)
      {
         MaxRepeatInfo info = MaxRepeatInfo.find(root.getInfo());
         info.set(false, (char)-1);
         return nRep - 1;
      }
      else return 0;
   }

   /** mark all nodes in the given subtree with MaxRepeatInfo objects */
   public int findMaxRepeats(NodeInterface node)
   {
      if (node == null) return 0;

      if (node instanceof InternalNode)
      {
         int n = 0;
         InternalNode inode = (InternalNode)node;
         NodeInterface kid = inode.getFirstChild();
         while(kid != null)
         {
            n += findMaxRepeats(kid);
            kid = kid.getRightSibling();
         }

         if (n > 0)
         {
            // we have some diverse leaves, so this node is diverse as well
            inode.addInfo(new MaxRepeatInfo());
            return n + 1;
         }
         else
         {
            // find common char or declare this node to be diverse
            boolean bLeftDiverse = false;
            char leftChar = 0;
            kid = inode.getFirstChild();
            while(kid != null)
            {
               MaxRepeatInfo info = MaxRepeatInfo.find(kid.getInfo());
               if (leftChar == 0) leftChar = info.getLeftChar();
               else if (leftChar != info.getLeftChar())
               {
                  bLeftDiverse = true;
                  break;
               }
               assert (leftChar != 0);
               kid = kid.getRightSibling();
            }
            assert (leftChar != 0);
            if (bLeftDiverse)
            {
               n++;
               inode.addInfo(new MaxRepeatInfo());
            }
            else inode.addInfo(new MaxRepeatInfo(leftChar));
         }
         return n;
      }
      else
      {
         // we have a leaf node
         LeafNode lnode = (LeafNode)node;
         SuffixCoordinates sc = lnode.getCoordinates();
         boolean bLeftDiverse = false;
         char leftChar = 0;
         while(sc != null)
         {
            int pos = sc.getPosition();
            char c = getLeftChar(pos);
            if (leftChar == 0) leftChar = c;
            else
            {
               if (leftChar != c)
               {
                  bLeftDiverse = true;
                  break;
               }
            }
            sc = sc.getNext();
         }
         node.addInfo(new MaxRepeatInfo(bLeftDiverse, leftChar));
         return bLeftDiverse ? 1 : 0;
      }
   }

   /**
    * Return a list of positions where the given string occurs
    * 
    * @param sNeedle string to search for
    * @return list of positions
    */
   public MyIntList getOccurrences(String sNeedle)
   {
      NodeInterface node = root;      
      String prefix = new String();
      String sRest = sNeedle;
      while(node != null)
      {
         if (!(node instanceof InternalNode)) return new MyIntList();
         InternalNode inode = (InternalNode)node;
         NodeInterface kid = inode.getFirstChild();
         while(kid != null)
         {
            String label = prefix+getSubstring(node.getLeftIndex(), node.getLength());
            
            // we could be done
            if (label.startsWith(sNeedle))
            {
               return node.getCoors(null);
            }
            else if (sNeedle.startsWith(label))
            {
               // we could match and need to continue
               prefix = label;               
               break;
            }            
            kid = kid.getRightSibling();
         }
         node = kid;
      }
      return new MyIntList();
   }

   /**
    * @return number of occurrences of the given node
    */
   public static int getNumOccs(NodeInterface node)
   {
      if (node instanceof InternalNode)
      {
         int n = 0;
         InternalNode inode = (InternalNode)node;
         NodeInterface kid = inode.getFirstChild();
         while(kid != null)
         {
            n += getNumOccs(kid);
            kid = kid.getRightSibling();
         }
         return n;
      }
      else
      {
         LeafNode lnode = (LeafNode)node;
         return lnode.getCoordinates().getNum();
      }
   }

   /**
    * When occurrences are found by the findOccs method, they are sent here for further processing
    * 
    * @param sQuery query string that led to the match
    * @param sMatch matching string
    * @param node node in the suffix tree in which the match terminated
    * @param nMatch index in node after last matching character
    * @param aDontCare list of "don't care" indices
    * @param hash hashtable in which to store the occurences
    */
   protected void handleFoundOcc(String sQuery, String sMatch, NodeInterface node, int nMatch,
         MyIntList aDontCare, HashMap<OccInfo, MyIntList> hash)
   {
      MyIntList coors = node.getCoors(null);      
      int nOccs = SuffixTree.getNumOccs(node);
      /*
       * if (!aDontCare.isEmpty()) // TODO: debug { System.err.printf("%s -> %s ", sQuery, sMatch); for(int
       * i=0; i<aDontCare.size(); i++) System.err.printf("%d ", aDontCare.get(i)); System.err.println(); }
       */
      OccInfo occi = new OccInfo(sMatch, nOccs, -1, aDontCare);
      hash.put(occi, coors);
   }

   /**
    * Find (warped) matches to the given query in this suffix tree
    * 
    * @param sQuery query string to search for
    * @param nMaxStretch max multiplier that a symbol can be stretched in DTW
    * @param nMaxDontCare max number of "don't care" spots
    */
   public HashMap<OccInfo, MyIntList> findWarpedOccs(String sQuery, int nMaxStretch, int nMaxDontCare)
   {
      HashMap<OccInfo, MyIntList> hash;
      // StretchyString ssQuery = new StretchyString(sQuery, nMaxStretch);

      // iterate to find the best time-warped string
      String sAverage = sQuery;
      while(true)
      {
         String sBase = sAverage;

         // walk through the suffix tree to find all time warped occurrences
         hash = new HashMap<OccInfo, MyIntList>();
         findWarpedOccs(0, 0, 0, nMaxStretch, new MyIntList(), nMaxDontCare, sBase, getRoot(), "", hash);

         // calc average string from all those found
         sAverage = OccInfo.calcAverageString(sBase, hash.keySet());
         if (sBase.equals(sAverage)) break;

         // TODO: uncomment below to prevent an iteration that won't map to the original query
         /*
          * StretchyString ssAve = new StretchyString(sAverage, nMaxStretch); if (!ssQuery.equals(ssAve))
          * break; }
          */
      }
      return hash;
   }

   /**
    * Find occurrences of the query string in this suffix tree allowing DTW
    * 
    * @param ix index in query string of current symbol
    * @param nPreMatch number of (current) symbols already matched
    * @param ixTree index in node label of current symbol
    * @param nMaxStretch max multiplier that a symbol can be stretched in DTW
    * @param aDontCare list of "don't care" spots so far
    * @param nMaxDontCare max number of "don't care" spots allowed
    * @param sQuery the query string
    * @param node current node in the suffix tree
    * @param sPrefix prefix in tree leading up to the current node
    * @param hash hashtable in which to store results
    */
   protected void findWarpedOccs(int ix, int nPreMatch, int ixTree, int nMaxStretch, MyIntList aDontCare,
         int nMaxDontCare, String sQuery, NodeInterface node, String sPrefix,
         HashMap<OccInfo, MyIntList> hash)
   {
      if (node == getRoot())
      {
         // no data in root node, so just recurse for all children
         NodeInterface kid = (NodeInterface)((InternalNode)node).getFirstChild();
         while(kid != null)
         {
            findWarpedOccs(ix, nPreMatch, ixTree, nMaxStretch, aDontCare, nMaxDontCare, sQuery, kid,
                  sPrefix, hash);
            kid = (NodeInterface)kid.getRightSibling();
         }
         return;
      }

      // we're past the root, so we have real data
      int nRunLen = StretchyString.getRunLength(sQuery, ix);
      int nMinRun = (int)Math.ceil((double)nRunLen / nMaxStretch);
      int nMaxRun = nRunLen * nMaxStretch;
      // System.err.printf("query = %s nRun=%d nStretch=%d warp = %d -> %d\n", sQuery.substring(ix), nRunLen,
      // nMaxStretch, nMinRun, nMaxRun);

      String label = getSubstring(node.getLeftIndex(), node.getLength());
      int nTreeRunLen = StretchyString.getRunLength(label, ixTree);
      int nTreeMinRun = (int)Math.ceil((double)nRunLen / nMaxStretch);
      int nTreeMaxRun = nRunLen * nMaxStretch;
      // System.err.printf("tree = %s nRun=%d (%d pre) nStretch=%d warp = %d -> %d\n",
      // label.substring(ixTree), nTreeRunLen, nPreMatch, nMaxStretch, nTreeMinRun, nTreeMaxRun);

      // what's the min/max number of chars we can match in this node?
      assert (nMaxRun - nPreMatch >= 1) : String.format(
            "nMaxRun=%d  nPreMatch=%d  nRunLen=%d (%s,%d)  nMaxStretch=%d", nMaxRun, nPreMatch, nRunLen,
            sQuery, ix, nMaxStretch);
      int nMinMatch = Math.max(1, nMinRun - nPreMatch);
      int nMaxMatch = Math.min(nTreeRunLen, nMaxRun - nPreMatch);

      // calc some useful state vars
      boolean bEndOfNode = (ixTree + nTreeRunLen == node.getLength());
      boolean bStartOfString = (ix == 0);
      boolean bEndOfString = (ix + nRunLen == sQuery.length());
      char curChar = sQuery.charAt(ix);
      char prevChar = '\0', nextChar = '\0';
      if (!bStartOfString) prevChar = sQuery.charAt(ix - 1);
      if (!bEndOfString) nextChar = sQuery.charAt(ix + nRunLen);
      char treeChar = label.charAt(ixTree);
      boolean bInternalNode = (node instanceof InternalNode);

      // could this be a don't care spot?
      // TODO: ABCD -> AXABCD -- logic for start of string is wrong
      // TODO: ABCD -> ABCDDDXD -- logic for end of string is wrong
      // TODO: ABCD -> ABAABCD -- logic for next char and is wrong
      // TODO: ABCD -> AABBACD -- logic for prev char is wrong
      // TODO: issue: first char of next char shouldn't be a DC
      if (!bStartOfString && !bEndOfString && curChar != treeChar && nextChar != treeChar
            && prevChar != treeChar && aDontCare.size() < nMaxDontCare)
      {
         aDontCare.add(sPrefix.length() + ixTree);

         // special handling if this is the last symbol in this node
         if (ixTree == node.getLength() - 1)
         {
            if (bInternalNode)
            {
               NodeInterface kid = ((InternalNode)node).getFirstChild();
               while(kid != null)
               {
                  findWarpedOccs(ix, nPreMatch, 0, nMaxStretch, aDontCare, nMaxDontCare, sQuery, kid,
                        sPrefix + label, hash);
                  kid = (NodeInterface)kid.getRightSibling();
               }
            }
         }
         else
         {
            findWarpedOccs(ix, nPreMatch, ixTree + 1, nMaxStretch, aDontCare, nMaxDontCare, sQuery, node,
                  sPrefix, hash);
         }

         aDontCare.removeElementAt(aDontCare.size() - 1);
      }

      // not a don't care spot, so we fail if no match
      if (curChar != treeChar) return;

      // are we done with this node?
      if (bEndOfNode)
      {
         // we can move on to child nodes
         if (bInternalNode)
         {
            // are we at the end of the query string?
            if (bEndOfString)
            {
               for(int i = nMinMatch; i <= nMaxMatch; i++)
               {
                  // System.err.printf("Found a full internal match: %s <-> %s+%s (%d)\n", sQuery, sPrefix,
                  // label, i);
                  handleFoundOcc(sQuery, sPrefix + label.substring(0, ixTree + i), node, ixTree + i,
                        aDontCare, hash);
               }

               // could be more (DTW) of the match in child nodes
               if (nPreMatch + nTreeRunLen < nMaxRun) // only recurse if more matches are allowed
               {
                  NodeInterface kid = ((InternalNode)node).getFirstChild();
                  while(kid != null)
                  {
                     findWarpedOccs(ix, nPreMatch + nTreeRunLen, 0, nMaxStretch, aDontCare, nMaxDontCare,
                           sQuery, kid, sPrefix + label, hash);
                     kid = (NodeInterface)kid.getRightSibling();
                  }
               }
            }
            else
            {
               // not at end of string, so look for the rest of the match in the child nodes
               NodeInterface kid = ((InternalNode)node).getFirstChild();
               while(kid != null)
               {
                  // this could be a partial match for this symbol
                  if (nPreMatch + nTreeRunLen < nMaxRun)
                     findWarpedOccs(ix, nPreMatch + nTreeRunLen, 0, nMaxStretch, aDontCare, nMaxDontCare,
                           sQuery, kid, sPrefix + label, hash);

                  // or the full match for this symbol
                  if (nPreMatch + nTreeRunLen >= nMinRun && nPreMatch + nTreeRunLen <= nMaxRun)
                     findWarpedOccs(ix + nRunLen, 0, 0, nMaxStretch, aDontCare, nMaxDontCare, sQuery, kid,
                           sPrefix + label, hash);

                  kid = (NodeInterface)kid.getRightSibling();
               }
            }
         }
         else
         {
            // leaf => no child nodes, so we better be finished with the query string
            if (ix + nRunLen == sQuery.length())
            {
               for(int i = nMinMatch; i <= nMaxMatch; i++)
               {
                  // System.err.printf("Found a full leaf match: %s <-> %s+%s (%d)\n", sQuery, sPrefix, label,
                  // i);
                  handleFoundOcc(sQuery, sPrefix + label.substring(0, ixTree + i), node, ixTree + i,
                        aDontCare, hash);
               }
            }
         }
      }

      // we're not done with the current node
      else
      {
         // are we at the end of the query string?
         if (bEndOfString)
         {
            for(int i = nMinMatch; i <= nMaxMatch; i++)
            {
               // System.err.printf("Found a mid-node match: %s <-> %s+%s (%d)\n", sQuery, sPrefix, label, i);
               handleFoundOcc(sQuery, sPrefix + label.substring(0, ixTree + i), node, ixTree + i, aDontCare,
                     hash);
            }
         }
         else
         {
            // look for the rest of the match in this node
            if (nPreMatch + nTreeRunLen >= nMinRun && nPreMatch + nTreeRunLen <= nMaxRun)
               findWarpedOccs(ix + nRunLen, 0, ixTree + nTreeRunLen, nMaxStretch, aDontCare, nMaxDontCare,
                     sQuery, node, sPrefix, hash);
         }
      }
   }

}
