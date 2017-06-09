package workshop;

import jv.geom.PgElementSet;
import jv.project.PgGeometry;
import jv.vecmath.PdVector;
import jvx.project.PjWorkshop;

public class SurfaceSmoothing extends PjWorkshop {
	PgElementSet m_geom;
    PgElementSet m_geomSave;

	/** Constructor */
	public SurfaceSmoothing() {
		super("Surface Smoothing");
		if (getClass() == SurfaceSmoothing.class) {
			init();
		}
	}
	
	/** Initialization */
	public void init() {
		super.init();
	}
	
    @Override
    public void setGeometry(PgGeometry geom) {
        super.setGeometry(geom);
        m_geom 		= (PgElementSet)super.m_geom;
        m_geomSave 	= (PgElementSet)super.m_geomSave;
    }
	
    /**
     * Reset the geometry to its standard shape
     */
    public void reset() {
    	m_geom.setVertices(m_geomSave.getVertices().clone());
    	m_geom.update(m_geom);
    }

	public void iterative(double stepsize) {
		// TODO Auto-generated method stub
		
	}

	public void explicit(double stepsize) {
		// TODO Auto-generated method stub
		
	}

	public void implicit(double stepsize) {
		// TODO Auto-generated method stub
		
	}
}
