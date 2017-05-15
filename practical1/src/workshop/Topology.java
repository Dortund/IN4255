package workshop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import jv.geom.PgEdgeStar;
import jv.geom.PgElementSet;
import jv.object.PsDebug;
import jv.project.PgGeometry;
import jv.vecmath.PiVector;
import jvx.project.PjWorkshop;

public class Topology extends PjWorkshop {

	PgElementSet m_geom;
	PgElementSet m_geomSave;
	
	public Topology() {
		super("Topology Algorithms");
		init();
	}
	
	@Override
	public void setGeometry(PgGeometry geom) {
		super.setGeometry(geom);
		m_geom 		= (PgElementSet)super.m_geom;
		m_geomSave 	= (PgElementSet)super.m_geomSave;
	}
	
	public void init() {
		super.init();
	}
	
	public int calculateGenus() {
		int v = m_geom.getNumVertices();
		int e = m_geom.getNumEdges();
		int f = m_geom.getNumElements();
		int x = 1 - ((v - e + f) / 2);
		return x;
	}
	
	public double calculateVolume() {
		
		return -1;
	}
	
	public int calculateComponents() {
		PiVector[] neighbours = m_geom.getNeighbours();
		boolean[] used = new boolean[neighbours.length];
		
		int comp = 0;
		int next = -1;
		while ((next = getFalse(used)) != -1) {
			comp++;
			used = dfs(neighbours, used, next);
		}
		
		return comp;
	}
	
	public String calculateLoops() {
		try {
			PgEdgeStar[] edge_list = m_geom.makeEdgeStars();
//			PsDebug.message("number of edges in total: " + edge_list.length);
			HashMap<Integer, PgEdgeStar> edge_map = getBoundaryEdges(edge_list);
//			PsDebug.message("number of boundary edges: " + edge_map.size());
			HashMap<Integer, ArrayList<Integer>> vertex_map = getBoundaryVertices(edge_map);
//			PsDebug.message("number of boundary vertices: " + vertex_map.size());
			int boundary_loop_count = 0;
			
			/*
			 * Algorithm idea:
			 * 1) Find all edges that are part of only 1 face
			 * 2) While we have such edges, pick one, increase boundary loop count
			 * 3) Traverse the boundary,finding the next edge via their shared vertex
			 */
			
			// Because edges with 1 neighbour still list a second neighbour with ID 0, 
			// we need to filter out the edges that ACTUALLY have face 0 as a neighbour
			try {
				PiVector dontcare = m_geom.getElement(0);
				boundary_loop_count --;
			} catch(NullPointerException E){}
			
			while(vertex_map.size() > 0){
				//If there is n entry, there is a yet unhandled boundary loop
				boundary_loop_count ++;
				Set<Integer> keys = vertex_map.keySet();
				int vertex_id  = keys.iterator().next();
				while(vertex_map.containsKey(vertex_id) && vertex_map.get(vertex_id).size() > 0){
					//Traverse the boundary, removing all vertices belonging to it
					int edge_id = vertex_map.get(vertex_id).get(0);
					PgEdgeStar edge = edge_map.get(edge_id);
					int other_vertex_id = getOtherVertex(edge, vertex_id);
					//remove edge from both vertices
					vertex_map.get(vertex_id).remove(vertex_map.get(vertex_id).indexOf(edge_id));
					vertex_map.get(other_vertex_id).remove(vertex_map.get(other_vertex_id).indexOf(edge_id));
					//remove vertex if edge list is empty
					if(vertex_map.get(vertex_id).size() == 0)
						vertex_map.remove(vertex_id);
					if(vertex_map.get(other_vertex_id).size() == 0)
						vertex_map.remove(other_vertex_id);
					vertex_id = other_vertex_id;
				}
			}
			return boundary_loop_count + " boundary loops";
		} catch(Exception E){
			StackTraceElement[] stacktrace = E.getStackTrace();
			for (StackTraceElement elem : stacktrace)
				PsDebug.message(elem.toString());
			PsDebug.warning(E.toString());
		}
		return "-1";
	}
	
	private int getOtherVertex(PgEdgeStar edge, int vertex_id){
		int v_id = edge.getVertexInd(0);
		if (v_id == vertex_id)
			v_id = edge.getVertexInd(1);
		return v_id;
	}
	
	private HashMap<Integer, PgEdgeStar> getBoundaryEdges(PgEdgeStar[] edge_list){
		HashMap<Integer, PgEdgeStar> edge_map = new HashMap<Integer, PgEdgeStar>();
		for (int i = 0; i < edge_list.length; i++){
			int[] elem_inds = edge_list[i].getElementInd();
			// Edges with only 1 neighbouring face will still have a neighbour list of size 2, but with one of the values set to 0
			if (elem_inds[0] == 0 || elem_inds[1] == 0) {
				edge_map.put(i, edge_list[i]);
			}
		}
		return edge_map;
	}
	
	private HashMap<Integer, ArrayList<Integer>> getBoundaryVertices(HashMap<Integer, PgEdgeStar> edge_map){
		HashMap<Integer, ArrayList<Integer>> vertex_map = new HashMap<Integer, ArrayList<Integer>>();
		Set<Integer> keys = edge_map.keySet();
		for (int key : keys){
			PgEdgeStar edge = edge_map.get(key);
			// Register edge to the first vertex
			if (vertex_map.containsKey(edge.getVertexInd(0))){
				assert(vertex_map.get(edge.getVertexInd(0)).size() == 1);
				vertex_map.get(edge.getVertexInd(0)).add(key);
			}
			else{
				ArrayList<Integer> edge_id_list = new ArrayList<Integer>();
				edge_id_list.add(key);
				vertex_map.put(edge.getVertexInd(0), edge_id_list);
			}
			// Register edge to the second vertex
			if (vertex_map.containsKey(edge.getVertexInd(1))){
				assert(vertex_map.get(edge.getVertexInd(1)).size() == 1);
				vertex_map.get(edge.getVertexInd(1)).add(key);
			}
			else{
				ArrayList<Integer> edge_id_list = new ArrayList<Integer>();
				edge_id_list.add(key);
				vertex_map.put(edge.getVertexInd(1), edge_id_list);
			}
		}
		return vertex_map;
	}
	
	private int getFalse(boolean[] input) {
		for (int i = 0; i< input.length; i++) {
			if (!input[i]) {
				return i;
			}
		}
		return -1;
	}
	
	private boolean[] dfs(PiVector[] neighbours, boolean[] used, int next) {
		used[next] = true;
		for (int i = 0; i < 3; i++) {
			if (neighbours[next].m_data[i] != -1 && !used[neighbours[next].m_data[i]]) {
				used = dfs(neighbours, used, neighbours[next].m_data[i]);
			}
		}
		
		return used;
	}
}
