package kdm.models.misc;

import kdm.data.*;
import kdm.util.*;
import java.util.*;

/**
 * stores information on whether a found and ground truth label overlaps and provides subclusters across which
 * matching decisions have no effect
 */
public final class TemporalEventMap
{
   protected ArrayList<WindowLocation> labels;
   protected ArrayList<WordSpot> spots;   
   protected ArrayList<Graph> graphs;
   protected HashMap<WordSpot, Integer> f2i;
   protected HashMap<WindowLocation, Integer> t2i;

   public TemporalEventMap(ArrayList<WindowLocation> labels, ArrayList<WordSpot> spots)
   {
      this.labels = labels;
      this.spots = spots;
      graphs = new ArrayList<Graph>();

      int nFound = spots.size();
      int nTrue = labels.size();
      
      if (nFound == 0 || nTrue == 0) return;

      int iSeries = spots.get(0).iSeries;

      // important that we traverse spots/labels in order
      Collections.sort(labels);
      Collections.sort(spots, new Comparator(){

         public int compare(Object o1, Object o2)
         {
            WordSpot spot1 = (WordSpot)o1;
            WordSpot spot2 = (WordSpot)o2;
            
            if (spot1.iStart < spot2.iStart) return -1;
            return 1;
         }
         
      });      
      
      // create maps from spots to labels and vice-versa
      ArrayList<ArrayList<WordSpot>> t2f = new ArrayList<ArrayList<WordSpot>>();
      ArrayList<ArrayList<WindowLocation>> f2t = new ArrayList<ArrayList<WindowLocation>>();
      for(int i = 0; i < nTrue; i++)
         t2f.add(new ArrayList<WordSpot>());
      for(int i = 0; i < nFound; i++)
         f2t.add(new ArrayList<WindowLocation>());

      // build index maps
      f2i = new HashMap<WordSpot, Integer>();
      for(int i = 0; i < nFound; i++)
         f2i.put(spots.get(i), i);
      t2i = new HashMap<WindowLocation, Integer>();
      for(int i = 0; i < nTrue; i++)
         t2i.put(labels.get(i), i);

      // map from labels to spots
      int j = 0;
      for(int i = 0; i < nTrue; i++){
         WindowLocation wlTrue = labels.get(i);
         while(j < nFound){
            if (spots.get(j).getFirstIndex() > wlTrue.getLastIndex()) break;
            while(j < nFound)
               if (spots.get(j).getLastIndex() >= wlTrue.iStart) break;
               else j++;
            if (j >= nFound) break;
            WordSpot spot = spots.get(j);
            if (spot.iStart > wlTrue.getLastIndex()) break;
            t2f.get(i).add(spot);
            if (j + 1 >= nFound || spots.get(j + 1).getFirstIndex() > wlTrue.getLastIndex()) break;
            else j++;
         }
      }

      // map from spots to labels
      int nTrueMap = t2f.size();
      for(int iTrue = 0; iTrue < nTrueMap; iTrue++){
         WindowLocation tm = labels.get(iTrue);
         ArrayList<WordSpot> tspots = t2f.get(iTrue);
         for(WordSpot spot : tspots)
            f2t.get(f2i.get(spot)).add(tm);
      }

      int iLabel = 0;      
      Graph graph = new Graph();
      int iLastSpot = -1; 
      while(iLabel < nTrue){         
         ArrayList<WordSpot> tspots = t2f.get(iLabel);
         if (tspots.isEmpty()){           
            iLabel++;
            if (!graph.isEmpty()){
               graphs.add(graph);
               graph = new Graph();
            }
            iLastSpot = -1;
            continue;
         }
         
         int iFirstSpot = f2i.get(tspots.get(0));
         if (iLastSpot<0 || iLastSpot==iFirstSpot){
            WindowLocation tm = labels.get(iLabel);
            graph.labels.add(tm);
            int ntSpots = tspots.size();
            int iSpot = (iLastSpot<0 ? 0 : 1);
            for(; iSpot<ntSpots; iSpot++)
               graph.spots.add(tspots.get(iSpot));
            iLastSpot = f2i.get(tspots.get(tspots.size()-1));
            iLabel++;
         }       
         else{
            if (!graph.isEmpty()){
               graphs.add(graph);
               graph = new Graph();
            }
            iLastSpot = -1;            
         }         
      }
      if (!graph.isEmpty()) graphs.add(graph);      
      
      // remove unnecessary labels / spots
      for(Graph g : graphs){
         // remove unnecessary spots
         ArrayList<WordSpot> spots2 = new ArrayList<WordSpot>();
         boolean bFirstLabel = true;
         for(WindowLocation tm : g.labels){
            ArrayList<WordSpot> tspots = t2f.get(t2i.get(tm));
            int ntspots = tspots.size();
            int nFree = 0;
            boolean bFirstSpot = true;
            for(WordSpot spot : tspots){
               if (f2t.get(f2i.get(spot)).size() > 1){
                  if (bFirstLabel || !bFirstSpot) spots2.add(spot);
               }
               else{
                  if (nFree == 0) spots2.add(spot);
                  nFree++;
               }
               bFirstSpot = false;
            }
            bFirstLabel = false;
         }
         g.spots = spots2;
         
         // remove unnecessary labels
         ArrayList<WindowLocation> labels2 = new ArrayList<WindowLocation>();
         bFirstLabel = true;
         for(WordSpot spot : g.spots){
            ArrayList<WindowLocation> wlabels = f2t.get(f2i.get(spot));
            int nLabels = wlabels.size();
            int nFree = 0;
            boolean bFirstSpot = true;
            for(WindowLocation tm : wlabels){
               if (t2f.get(t2i.get(tm)).size() > 1){
                  if (bFirstLabel || !bFirstSpot) labels2.add(tm);
               }
               else{
                  if (nFree == 0) labels2.add(tm);
                  nFree++;
               }
               bFirstSpot = false;
            }
            bFirstLabel = false;
         }
         g.labels = labels2;
      }
   }
   
   /** test application */
   public static void main(String[] args)
   {
      ArrayList<WindowLocation> labels = new ArrayList<WindowLocation>();
      ArrayList<WordSpot> spots = new ArrayList<WordSpot>();

      labels.add(new WindowLocation(0, 4, 5));
      labels.add(new WindowLocation(0, 10, 6));
      labels.add(new WindowLocation(0, 17, 3));
      labels.add(new WindowLocation(0, 21, 5));
      labels.add(new WindowLocation(0, 33, 5));
      labels.add(new WindowLocation(0, 39, 5));
      labels.add(new WindowLocation(0, 45, 26));
      labels.add(new WindowLocation(0, 75, 16));

      spots.add(new WordSpot(0, 0, 6, 0, 0));
      spots.add(new WordSpot(0, 7, 5, 0, 0));
      spots.add(new WordSpot(0, 14, 10, 0, 0));
      spots.add(new WordSpot(0, 29, 3, 0, 0));
      spots.add(new WordSpot(0, 35, 7, 0, 0));

      spots.add(new WordSpot(0, 46, 3, 0, 0));
      spots.add(new WordSpot(0, 49, 3, 0, 0));
      spots.add(new WordSpot(0, 53, 3, 0, 0));
      spots.add(new WordSpot(0, 65, 14, 0, 0));
      spots.add(new WordSpot(0, 80, 6, 0, 0));

      TemporalEventMap tem = new TemporalEventMap(labels, spots);
      tem.dump();
   }

   public int getNumSubgraphs()
   {
      return graphs.size();
   }

   public ArrayList<WindowLocation> getGraphLabels(int iGraph)
   {
      return graphs.get(iGraph).labels;
   }

   public ArrayList<WordSpot> getGraphSpots(int iGraph)
   {
      return graphs.get(iGraph).spots;
   }

   public void dump()
   {
      System.err.printf("Found %d subgraphs:\n", graphs.size());
      int iGraph = 0;
      for(Graph g : graphs){
         System.err.printf("Subgraph %d:\n", ++iGraph);
         System.err.print(" Labels: ");
         for(WindowLocation tm: g.labels)
            System.err.printf(" %c", t2i.get(tm)+'A');
         System.err.println();
         
         System.err.print("  Spots:");
         for(WordSpot spot : g.spots)
            System.err.printf(" %d", f2i.get(spot)+1);
         System.err.println();
      }
   }
   
   /** Graph object that stores a subset spots/labels must be exhaustively searched for matches */
   class Graph{
      public ArrayList<WordSpot> spots;
      public ArrayList<WindowLocation> labels;
      
      public Graph()
      {
         spots = new ArrayList<WordSpot>();
         labels = new ArrayList<WindowLocation>();
      }
      
      public boolean isEmpty(){ return spots.isEmpty(); }
   }
}
