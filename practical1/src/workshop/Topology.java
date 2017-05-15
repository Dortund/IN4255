package workshop;

import java.util.Stack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import jv.geom.PgEdgeStar;
import jv.geom.PgElementSet;
import jv.object.PsDebug;
import jv.project.PgGeometry;
import jv.vecmath.PdVector;
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
		// (V - E + F) = 2(1 - g)
		int x = (int) (1.0 - ((v - e + f) / 2.0));
		
		// Some debug output to verify the used numbers
		PsDebug.warning("Vertices: " + v + ", Edges: " + e + ", Faces: " + f);
		
		return x;
	}
	
	public double calculateVolume() {
		// Make sure we can calculate tetrahedrons
		if (m_geom.getMaxDimOfElements() == 3) {
			double total = 0;
			// Make sure we have face normals
			if (!m_geom.hasElementNormals())
				m_geom.makeElementNormals();
			
			// Calculate volume for each face
			for (int i = 0; i < m_geom.getNumElements(); i++) {
				// Calculate volume
				PiVector element = m_geom.getElement(i);
				PdVector a = m_geom.getVertex(element.getEntry(0));
				PdVector b = m_geom.getVertex(element.getEntry(1));
				PdVector c = m_geom.getVertex(element.getEntry(2));
				double vol = PdVector.crossNew(b, c).dot(a);
				
				// Find out if the face is facing towards or away from the origin
				PdVector normal = m_geom.getElementNormal(i);
				PdVector faceMid = new PdVector(
						(a.getEntry(0) + b.getEntry(0) + b.getEntry(0)) / 3,
						(a.getEntry(1) + b.getEntry(1) + b.getEntry(1)) / 3,
						(a.getEntry(2) + b.getEntry(2) + b.getEntry(2)) / 3);
				faceMid.normalize();
				double angle = normal.dot(faceMid);
				
				// If facing away, it is a positive volume, else it is a negative volume
				if (angle > 0) {
					total += vol;
				} else {
					total -= vol;
				}
			}
			// Some debug output to verify our own output
			PsDebug.warning(m_geom.getVolume() +  "");
			
			// We still need to divide the volume by 6 to get the correct tetrahedron volume calculation 
			return total / 6;
		}
		return - m_geom.getMaxDimOfElements();
	}
	
	public int calculateComponents() {
		/*PiVector[] neighbours = m_geom.getNeighbours();
		boolean[] used = new boolean[neighbours.length];
		
		int comp = 0;
		int next = -1;
		while ((next = getFalse(used)) != -1) {
			comp++;
			used = dfs(neighbours, used, next);
		}
		
		return comp;*/
		
		// Get neighbour information
		PiVector[] neighbours = m_geom.getNeighbours();
		
		// Track which faces we have visitied
		boolean[] used = new boolean[neighbours.length];
		
		int comp = 0;
		int next = -1;
		// As long as we haven't visited every face, find connected components
		while ((next = getFalse(used)) != -1) {
			comp++;
			Stack<Integer> todo = new Stack<Integer>(); 
			todo.push(next);
			
			// Depth First Search
			while (!todo.isEmpty()) {
				int x = todo.pop();
				if (!used[x]) {
					used[x] = true;
					for (int index : neighbours[x].getEntries()) {
						todo.push(index);
					}
				}
			}
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
	
	// No longer used. The non-recursive depth first search is faster.
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
