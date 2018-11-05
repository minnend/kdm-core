package kdm.mlpr.dataTree;

import java.util.*;
import kdm.data.*;
import kdm.io.DataLoader.DLRaw;
import kdm.metrics.*;
import kdm.util.*;

import org.apache.commons.math.stat.*;

/** metric tree for efficient storage of vectors in a metric space */
public class MetricTree extends VectorTree
{
   public static final String KeyMean = "MT.mean";
   public static final String KeyRadius = "MT.radius";
   public static final String KeyNumData = "MT.numData";
   public static final String KeyParent = "MT.parent";
   public static final String KeyMaxNeighbor = "MT.maxNeighbor";

   protected MetricFV metric;

   /** @return k nearest neighbors for each data point in this tree */
   public HashMap<FeatureVec, ArrayList<FeatureVec>> calcAllPairsKNN(int k)
   {
      assert false : "not yet implemented"; // TODO
      return null;
   }

   /** @return nearest neighbor for each data point in this tree */
   public HashMap<FeatureVec, Pair<MutableDouble, FeatureVec>> calcAllPairsNN()
   {
      boolean bDataComp = FeatureVec.bDataComp;
      FeatureVec.bDataComp = false;
      final HashMap<FeatureVec, Pair<MutableDouble, FeatureVec>> map = new HashMap<FeatureVec, Pair<MutableDouble, FeatureVec>>();

      // calc all within-leaf distances
      root.apply(new DataTreeApply() {
         public void apply(DataTreeNode node, Object param)
         {
            VectorTreeNode v = (VectorTreeNode)node;
            if (v.hasKids()) return;

            // hack to get inside leaf plus nearest other leaf 
            VectorTreeNode w = ((VectorTreeNode)v.meta.get(KeyParent)).getOtherKid(v);
            FeatureVec mean = (FeatureVec)v.meta.get(KeyMean);
            while(w.hasKids()){                  
               double d1 = metric.dist(mean, (FeatureVec)w.kids[0].meta.get(KeyMean));
               double d2 = metric.dist(mean, (FeatureVec)w.kids[1].meta.get(KeyMean));
               if (d1 < d2) w = w.kids[0];
               else w = w.kids[1];
            }
            calcAllPairsNN(v.data, w.data, map);            
            calcAllPairsNN(v.data, map);
         }
      }, map);

      // TODO !! what's wrong with dual-tree implementation?
      //if (root.hasKids()) calcAllPairsNN(root.kids[0], root.kids[1], map);

      FeatureVec.bDataComp = bDataComp;
      return map;
   }

   protected void calcAllPairsNN(VectorTreeNode node1, VectorTreeNode node2,
         HashMap<FeatureVec, Pair<MutableDouble, FeatureVec>> map)
   {
      FeatureVec mean1 = (FeatureVec)node1.meta.get(KeyMean);
      double r1 = (Double)node1.meta.get(KeyRadius);
      int n1 = (Integer)node1.meta.get(KeyNumData);

      FeatureVec mean2 = (FeatureVec)node2.meta.get(KeyMean);
      double r2 = (Double)node2.meta.get(KeyRadius);
      int n2 = (Integer)node2.meta.get(KeyNumData);

      double dist = metric.dist(mean1, mean2);
      double mincross = dist - (r1 + r2);

      // do we need to compare across nodes?
      if (n1 > 1 && n2 > 1 && 2 * r1 < mincross && 2 * r2 < mincross){
         // no we don't!
         if (node1.hasKids()) calcAllPairsNN(node1.kids[0], node1.kids[1], map);
         if (node2.hasKids()) calcAllPairsNN(node2.kids[0], node2.kids[1], map);
      }
      else{
         // yes we do, so figure out which crosses are needed
         if (node1.isLeaf() && node2.isLeaf()){
            // two leaves = no recursion
            calcAllPairsNN(node1.data, node2.data, map);
         }
         else if (node1.isLeaf()){
            // node1 is a leaf
            calcAllPairsNN(node1, node2.kids[0], map);
            calcAllPairsNN(node1, node2.kids[1], map);
            calcAllPairsNN(node2.kids[0], node2.kids[1], map);            
         }
         else if (node2.isLeaf()){
            // node2 is a leaf
            calcAllPairsNN(node2, node1.kids[0], map);
            calcAllPairsNN(node2, node1.kids[1], map);
            calcAllPairsNN(node1.kids[0], node1.kids[1], map);
         }
         else{
            // two internal nodes
            calcAllPairsNN(node1.kids[0], node2.kids[0], map);
            calcAllPairsNN(node1.kids[0], node2.kids[1], map);            
            calcAllPairsNN(node1.kids[1], node2.kids[0], map);
            calcAllPairsNN(node1.kids[1], node2.kids[1], map);       

            calcAllPairsNN(node1.kids[0], node1.kids[1], map);
            calcAllPairsNN(node2.kids[0], node2.kids[1], map);
         }
      }
   }

   protected void calcAllPairsNN(ArrayList<FeatureVec> data,
         HashMap<FeatureVec, Pair<MutableDouble, FeatureVec>> map)
   {
      int N = data.size();
      for(int i = 0; i < N; i++){
         FeatureVec fvi = data.get(i);
         for(int j = i + 1; j < N; j++){
            FeatureVec fvj = data.get(j);
            double d = metric.dist(fvi, fvj);

            Pair<MutableDouble, FeatureVec> pair = map.get(fvi);
            if (pair == null){
               pair = new Pair<MutableDouble, FeatureVec>(new MutableDouble(d), fvj);
               map.put(fvi, pair);
            }
            else if (d < pair.first.getValue()){
               pair.first.set(d);
               pair.second = fvj;
            }

            pair = map.get(fvj);
            if (pair == null){
               pair = new Pair<MutableDouble, FeatureVec>(new MutableDouble(d), fvi);
               map.put(fvj, pair);
            }
            else if (d < pair.first.getValue()){
               pair.first.set(d);
               pair.second = fvi;
            }
         }
      }
   }

   protected void calcAllPairsNN(ArrayList<FeatureVec> data1, ArrayList<FeatureVec> data2,
         HashMap<FeatureVec, Pair<MutableDouble, FeatureVec>> map)
   {
      int N = data1.size();
      int M = data2.size();
      for(int i = 0; i < N; i++){
         FeatureVec fvi = data1.get(i);
         for(int j = 0; j < M; j++){
            FeatureVec fvj = data2.get(j);
            double d = metric.dist(fvi, fvj);

            Pair<MutableDouble, FeatureVec> pair = map.get(fvi);
            if (pair == null){
               pair = new Pair<MutableDouble, FeatureVec>(new MutableDouble(d), fvj);
               map.put(fvi, pair);
            }
            else if (d < pair.first.getValue()){
               pair.first.set(d);
               pair.second = fvj;
            }

            pair = map.get(fvj);
            if (pair == null){
               pair = new Pair<MutableDouble, FeatureVec>(new MutableDouble(d), fvi);
               map.put(fvj, pair);
            }
            else if (d < pair.first.getValue()){
               pair.first.set(d);
               pair.second = fvi;
            }
         }
      }
   }

   /** @return total volume of all nodes in this tree */
   public double getTotalVolume()
   {
      if (root == null) return 0;

      final int D = ((FeatureVec)root.meta.get(KeyMean)).getNumDims();
      MutableDouble md = new MutableDouble(0);
      root.apply(new DataTreeApply() {

         public void apply(DataTreeNode node, Object param)
         {
            MutableDouble md = (MutableDouble)param;
            double r = (Double)node.meta.get(KeyRadius);
            double v = Library.getVolumeSphere(r, D);
            md.add(v);
         }

      }, md);
      return md.getValue();
   }

   /** find the prototype (mean) and radius of this node */
   protected void initNode(VectorTreeNode node)
   {
      ArrayList<FeatureVec> data = node.data;
      int N = data.size();

      // find centroid of data
      int D = data.get(0).getNumDims();
      FeatureVec mean = FeatureVec.zeros(D);
      for(int i = 0; i < N; i++)
         mean._add(data.get(i));
      mean._div(N);
      node.meta.put(KeyMean, mean);

      // now calc radius
      double radius = 0;
      for(int i = 0; i < N; i++){
         double r = metric.dist(mean, data.get(i));
         if (r > radius) radius = r;
      }
      node.meta.put(KeyRadius, radius);
      node.meta.put(KeyNumData, N);
   }

   // /////////////////////////////////////////////////////////////////////
   // Naive construction
   // /////////////////////////////////////////////////////////////////////

   /** build the metric tree using the naive top-down method (stored via this tree's root) */
   public void constructNaive(ArrayList<FeatureVec> data, MetricFV metric, int nMaxMembers)
   {
      this.metric = metric;
      root = buildNaive(data, nMaxMembers);
   }

   /** @return metric tree built using the naive top-down method */
   protected VectorTreeNode buildNaive(ArrayList<FeatureVec> data, int nMaxMembers)
   {
      VectorTreeNode lroot = new VectorTreeNode(data);
      splitNaive(lroot, nMaxMembers);
      return lroot;
   }

   /** split the given node using a simple, top-down procedure */
   protected void splitNaive(VectorTreeNode node, int nMaxMembers)
   {
      ArrayList<FeatureVec> data = node.data;

      // make sure this node has a pivot and radius
      if (!node.meta.containsKey(KeyMean)) initNode(node);

      // maybe node is small enough already
      if (data.size() <= nMaxMembers) return;

      // nope, so split: find point farthest from pivot
      int N = data.size();
      FeatureVec mean = (FeatureVec)node.meta.get(KeyMean);
      double maxDist = 0;
      int iMax = -1;
      for(int i = 0; i < N; i++){
         double d = metric.dist(mean, data.get(i));
         if (d > maxDist){
            maxDist = d;
            iMax = i;
         }
      }
      FeatureVec pivot1 = data.get(iMax);

      // now find point farthest from pivot1
      maxDist = 0;
      iMax = -1;
      for(int i = 0; i < N; i++){
         double d = metric.dist(pivot1, data.get(i));
         if (d > maxDist){
            maxDist = d;
            iMax = i;
         }
      }
      FeatureVec pivot2 = data.get(iMax);

      // now assign points to kids
      ArrayList<FeatureVec> data1 = new ArrayList<FeatureVec>();
      ArrayList<FeatureVec> data2 = new ArrayList<FeatureVec>();
      for(int i = 0; i < N; i++){
         FeatureVec x = data.get(i);
         double d1 = metric.dist(pivot1, x);
         double d2 = metric.dist(pivot2, x);
         if (d1 < d2) data1.add(x);
         else data2.add(x);
      }

      node.kids = new VectorTreeNode[2];
      node.kids[0] = new VectorTreeNode(data1);
      node.kids[0].meta.put(KeyParent, node);
      node.kids[1] = new VectorTreeNode(data2);
      node.kids[1].meta.put(KeyParent, node);
      node.data = null; // we don't need to store data here anymore
      splitNaive(node.kids[0], nMaxMembers);
      splitNaive(node.kids[1], nMaxMembers);
   }

   // /////////////////////////////////////////////////////////////////////
   // Middle-out construction via Anchors hierarchy
   // /////////////////////////////////////////////////////////////////////

   /** construct this tree using Moore's anchor hierarchy "middle-out" method */
   public void constructAnchor(ArrayList<FeatureVec> data, MetricFV metric, int nMaxMembers)
   {
      this.metric = metric;
      root = buildAnchor(data, nMaxMembers);
   }

   /** @return root node of a metric tree built using the anchor hierarchy method */
   protected VectorTreeNode buildAnchor(ArrayList<FeatureVec> data, int nMaxMembers)
   {
      int N = data.size();
      int D = data.get(0).getNumDims();

      // maybe we don't need a tree at all
      if (N <= nMaxMembers){
         VectorTreeNode lroot = new VectorTreeNode(data);
         initNode(lroot);
         return lroot;
      }

      // build the initial anchor list
      int nMaxAnchors = (int)Math.max(2, Math.ceil(Math.sqrt(N)));
      ArrayList<Anchor> anchors = buildAnchorList(data, nMaxAnchors);

      // make a node for each anchor
      List<VectorTreeNode> nodes = new LinkedList<VectorTreeNode>();
      for(Anchor anchor : anchors){
         ArrayList<FeatureVec> nodeData = new ArrayList<FeatureVec>();
         for(Owned owned : anchor.owned)
            nodeData.add(owned.x);
         VectorTreeNode node = new VectorTreeNode(nodeData);
         initNode(node);
         nodes.add(node);
      }

      // build down the tree
      int nNodes = nodes.size();
      for(int i = 0; i < nNodes; i++){
         VectorTreeNode node = nodes.get(i);
         VectorTreeNode sub = buildAnchor(node.data, nMaxMembers);
         if (sub.hasKids()){
            int nKids = sub.getNumKids();
            node.kids = new VectorTreeNode[nKids];
            for(int j = 0; j < nKids; j++)
               node.kids[j] = sub.kids[j];
         }
      }

      // build list of inter-node distances
      List<MyDoubleList> alist = new LinkedList<MyDoubleList>();
      for(int i = 0; i < nNodes; i++){
         MyDoubleList list = new MyDoubleList();
         // TODO could be 2x faster by using symmetry
         for(int j = 0; j < nNodes; j++){
            if (i == j) list.add(0);
            else list.add(metric.dist((FeatureVec)nodes.get(i).meta.get(KeyMean),
                  (FeatureVec)nodes.get(j).meta.get(KeyMean)));
         }
         alist.add(list);
      }

      return buildAnchorTree(nodes, alist, nMaxMembers);
   }

   /**
    * Construct a metric tree using anchor hierarchy method
    * 
    * @param nodes list of nodes from anchor list
    * @param dnode distance between every pair of node centers
    * @param metric vector metric to use
    * @param nMaxMembers max number of points per leaf node
    * @return root node of a metric tree built using the anchor hierarchy method
    */
   protected VectorTreeNode buildAnchorTree(List<VectorTreeNode> nodes, List<MyDoubleList> dnode,
         int nMaxMembers)
   {
      DataTreeApply calcRad = new DataTreeApply() {
         public void apply(DataTreeNode node, Object param)
         {
            Pair<FeatureVec, MutableDouble> pair = (Pair<FeatureVec, MutableDouble>)param;
            FeatureVec mean = pair.first;
            MutableDouble radius = pair.second;
            VectorTreeNode v = (VectorTreeNode)node;
            if (v.data != null){
               for(FeatureVec fv : v.data){
                  double r = metric.dist(mean, fv);
                  if (r > radius.getValue()) radius.set(r);
               }
            }
         }
      };

      while(nodes.size() > 1){
         int N = nodes.size();

         // find closest nodes
         int iBest = -1, jBest = -1;
         VectorTreeNode nodeI = null, nodeJ = null;
         double rmin = Library.INF;
         Iterator<VectorTreeNode> itI = nodes.iterator();
         Iterator<MyDoubleList> itListI = dnode.iterator();
         for(int i = 0; i < N; i++){
            VectorTreeNode inode = itI.next();
            double ri = (Double)inode.meta.get(KeyRadius);
            MyDoubleList listI = itListI.next();
            Iterator<VectorTreeNode> itJ = nodes.iterator();
            for(int j = 0; j <= i; j++)
               itJ.next();
            for(int j = i + 1; j < N; j++){
               VectorTreeNode jnode = itJ.next();
               double rj = (Double)jnode.meta.get(KeyRadius);
               double r = ri + rj + listI.get(j);
               if (r < rmin){
                  rmin = r;
                  iBest = i;
                  jBest = j;
                  nodeI = inode;
                  nodeJ = jnode;
               }
            }
         }

         // merge the best pair
         VectorTreeNode node = new VectorTreeNode(null);
         node.kids = new VectorTreeNode[2];
         node.kids[0] = nodeI;
         node.kids[0].meta.put(KeyParent, node);
         node.kids[1] = nodeJ;
         node.kids[1].meta.put(KeyParent, node);

         // calc mean of parent node from cached stats
         int ni = (Integer)nodeI.meta.get(KeyNumData);
         int nj = (Integer)nodeJ.meta.get(KeyNumData);
         FeatureVec meanI = (FeatureVec)nodeI.meta.get(KeyMean);
         FeatureVec meanJ = (FeatureVec)nodeJ.meta.get(KeyMean);
         FeatureVec mean = meanI.mul(ni)._add(meanJ.mul(nj));
         mean._div(ni + nj);
         node.meta.put(KeyMean, mean);
         node.meta.put(KeyNumData, ni + nj);

         // calc radius of parent node
         MutableDouble rad = new MutableDouble(0);
         Pair<FeatureVec, MutableDouble> param = new Pair<FeatureVec, MutableDouble>(mean, rad);
         nodeI.apply(calcRad, param);
         nodeJ.apply(calcRad, param);
         node.meta.put(KeyRadius, rad.getValue());

         // remove nodes I and J
         assert (jBest > iBest); // needed to ensure consistency during removal (remove jBest first!)
         nodes.remove(jBest); // remove node from list
         nodes.remove(iBest);
         dnode.remove(jBest); // remove rows from node distance list
         dnode.remove(iBest);
         for(MyDoubleList list : dnode){
            list.removeElementAt(jBest);
            list.removeElementAt(iBest);
         }

         // add new parent node and update distance matrix
         int nNodes = nodes.size();
         nodes.add(node);
         MyDoubleList list = new MyDoubleList();
         itI = nodes.iterator();
         itListI = dnode.iterator();
         for(int i = 0; i < nNodes; i++){
            double dist = metric.dist(mean, (FeatureVec)itI.next().meta.get(KeyMean));
            list.add(dist);
            itListI.next().add(dist);
         }
         list.add(0); // diagonal value
         dnode.add(list);
      }

      return nodes.get(0);
   }

   /** @return anchor list for the given data */
   public ArrayList<Anchor> buildAnchorList(ArrayList<FeatureVec> data, int nMaxAnchors)
   {
      ArrayList<Anchor> anchors = new ArrayList<Anchor>();

      // build the first anchor
      int N = data.size();
      int D = data.get(0).getNumDims();
      FeatureVec mean = FeatureVec.zeros(D);
      for(int i = 0; i < N; i++)
         mean._add(data.get(i));
      mean._div(N);
      Anchor anchor = new Anchor(mean, false);

      for(int i = 0; i < N; i++){
         FeatureVec x = data.get(i);
         anchor.owned.add(new Owned(x, metric.dist(mean, x)));
      }
      assert (anchor.owned.size() == data.size()) : String.format("#owned=%d  #data=%d\n", anchor.owned
            .size(), data.size());
      Collections.sort(anchor.owned);
      anchors.add(anchor);
      int nAnchors = 1;

      // keep explicit map of inter-anchor distance (pivot-to-pivot)
      double[][] adist = new double[nMaxAnchors][nMaxAnchors];

      // now we can add additional anchors
      while(nAnchors < nMaxAnchors){
         // find anchor with max radius
         double maxrad = 0;
         int iMax = -1;
         for(int i = 0; i < nAnchors; i++){
            double r = anchors.get(i).getRadius();
            if (r > maxrad){
               maxrad = r;
               iMax = i;
            }
         }
         if (iMax < 0) break;

         // create a new anchor
         Anchor loser = anchors.get(iMax);
         anchor = new Anchor(loser.removeLast().x, true);

         // calc dist from new anchor to all existing anchors
         int iNew = nAnchors;
         for(int i = 0; i < nAnchors; i++)
            adist[iNew][i] = adist[i][iNew] = metric.dist(anchor.pivot, anchors.get(i).pivot);

         // now let the new anchor steal points from all of the others
         for(int i = 0; i < nAnchors; i++){
            loser = anchors.get(i);
            for(int j = loser.getLastIndex(); j > 0; j--){
               Owned owned = loser.owned.get(j);
               if (owned.dist <= adist[iNew][i] / 2.0) break;
               loser.removeLast();
               owned.dist = metric.dist(anchor.pivot, owned.x);
               anchor.owned.add(owned);
            }
         }
         Collections.sort(anchor.owned);

         // add the new anchor to the list
         anchors.add(anchor);
         nAnchors++;
         assert (nAnchors == anchors.size());
      }

      return anchors;
   }

   /** represents an anchor in Moore's anchor hierarchy */
   public class Anchor
   {
      public FeatureVec pivot;
      public ArrayList<Owned> owned;

      public int getLastIndex()
      {
         return owned.size() - 1;
      }

      public double getRadius()
      {
         return owned.get(owned.size() - 1).dist;
      }

      public Owned removeLast()
      {
         return owned.remove(owned.size() - 1);
      }

      public Anchor(FeatureVec pivot, boolean bOwnPivot)
      {
         this.pivot = pivot;
         owned = new ArrayList<Owned>();
         if (bOwnPivot) owned.add(new Owned(pivot, 0));
      }
   }

   /** represents a point owned by an anchor */
   class Owned implements Comparable<Owned>
   {
      public FeatureVec x;
      public double dist;

      public Owned(FeatureVec x, double dist)
      {
         this.x = x;
         this.dist = dist;
      }

      public int compareTo(Owned o)
      {
         // sort in ascending order
         if (dist < o.dist) return -1;
         if (dist > o.dist) return 1;
         return 0;
      }

      @Override
      public boolean equals(Object o)
      {
         return dist == ((Owned)o).dist;
      }
   }

   public static void main(String[] args)
   {
      FeatureVec.bDataComp = false;
      Sequence data = new DLRaw().load(args[0]);
      int nData = data.length();
      MetricFV metric = new EuclideanFV(false);

      System.err.printf("Building tree (%d)... ", data.length());
      TimerMS timer = new TimerMS();
      int nMaxMembers = 5;
      MetricTree mtree = new MetricTree();
      mtree.constructNaive(data.getData(), metric, nMaxMembers);
      System.err.printf("done (%dms).\n", timer.time());
      System.err.printf("Calc nearest-neighbors (tree)... ");
      timer.reset();
      HashMap<FeatureVec, Pair<MutableDouble, FeatureVec>> map = mtree.calcAllPairsNN();
      System.err.printf("done (%dms).\n", timer.time());

      System.err.printf("Calc nearest-neighbors (direct)... ");
      timer.reset();
      HashMap<FeatureVec, Pair<MutableDouble, FeatureVec>> map2 = new HashMap<FeatureVec, Pair<MutableDouble, FeatureVec>>();
      mtree.calcAllPairsNN(data.getData(), map2);
      System.err.printf("done (%dms).\n", timer.time());

      // compare tree vs. direct method
      double[] ratio = new double[nData];
      int nMissing = 0;
      int nErrs = 0;
      for(int i = 0; i < nData; i++){
         FeatureVec fv = data.get(i);
         Pair<MutableDouble, FeatureVec> pairTree = map.get(fv);
         Pair<MutableDouble, FeatureVec> pairDirect = map.get(fv);
         if (pairTree == null){
            nMissing++;
            ratio[i] = 0.0;
         }
         else{
            ratio[i] = pairTree.first.getValue() / pairDirect.first.getValue();
            if (pairTree.second != pairDirect.second) nErrs++;
         }
      }

      System.err.printf("#errors: %d   #missing: %d\n", nErrs, nMissing);
      System.err.printf(" mean=%f  max=%f\n", StatUtils.mean(ratio), StatUtils.max(ratio));
   }
}
