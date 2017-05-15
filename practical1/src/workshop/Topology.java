package workshop;

import java.util.Stack;

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
	
	public int calculateLoops() {
		
		return -1;
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
