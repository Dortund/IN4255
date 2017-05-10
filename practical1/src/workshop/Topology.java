package workshop;

import java.util.List;

import jv.geom.PgElementSet;
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
