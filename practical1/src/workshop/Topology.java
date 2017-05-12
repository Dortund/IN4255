package workshop;

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
		int x = 1 - ((v - e + f) / 2);
		return x;
	}
	
	public double calculateVolume() {
		if (m_geom.getMaxDimOfElements() == 3) {
			double total = 0;
			if (!m_geom.hasElementNormals())
				m_geom.makeElementNormals();
			for (int i = 0; i < m_geom.getNumElements(); i++) {
				PiVector element = m_geom.getElement(i);
				PdVector a = m_geom.getVertex(element.getEntry(0));
				PdVector b = m_geom.getVertex(element.getEntry(1));
				PdVector c = m_geom.getVertex(element.getEntry(2));
				double vol = PdVector.crossNew(b, c).dot(a);
				
				PdVector normal = m_geom.getElementNormal(i);
				PdVector faceMid = new PdVector(
						(a.getEntry(0) + b.getEntry(0) + b.getEntry(0)) / 3,
						(a.getEntry(1) + b.getEntry(1) + b.getEntry(1)) / 3,
						(a.getEntry(2) + b.getEntry(2) + b.getEntry(2)) / 3);
				faceMid.normalize();
				double angle = normal.dot(faceMid);
				if (angle > 0) {
					total += vol;
				} else {
					total -= vol;
				}
			}
			/*PdVector[] bounds = m_geom.getBounds();
			double temp = (bounds[1].getEntry(0) - bounds[0].getEntry(0))
					* (bounds[1].getEntry(1) - bounds[0].getEntry(1))
					* (bounds[1].getEntry(2) - bounds[0].getEntry(2));*/
			return total / 6;
		}
		return - m_geom.getMaxDimOfElements();
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
