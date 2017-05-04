package pract1;

import jv.geom.PgElementSet;
import jv.project.PgGeometry;
import jvx.project.PjWorkshop;

public class TopologyAlgorithms extends PjWorkshop {

	PgElementSet m_geom;
	PgElementSet m_geomSave;
	
	public TopologyAlgorithms() {
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
}
