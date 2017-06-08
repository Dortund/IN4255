package workshop;

import jv.geom.PgElementSet;
import jv.vecmath.PdVector;
import jvx.project.PjWorkshop;

public class ShapeInterpolation extends PjWorkshop {
	
	/** First surface to be registered. */	
	PgElementSet	m_surfP;
    /** Keeps the a copy of the vertices of P when loaded
     * This can be used to reset P.
     */
    PdVector[] m_surfP_original;
	/** Second surface to be registered. */
	PgElementSet	m_surfQ;
    /** Keeps the a copy of the vertices of Q when loaded
     * This can be used to reset Q.
     */
    PdVector[] m_surfQ_original;

	/** Constructor */
	public ShapeInterpolation() {
		super("Mesh Interpolation");
		if (getClass() == ShapeInterpolation.class) {
			init();
		}
	}
	
	/** Initialization */
	public void init() {
		super.init();
	}
	
	/** Set two Geometries. */
	public void setGeometries(PgElementSet surfP, PgElementSet surfQ) {
		m_surfP = surfP;
        m_surfP_original = PdVector.copyNew(surfP.getVertices());
		m_surfQ = surfQ;
        m_surfQ_original = PdVector.copyNew(surfQ.getVertices());
        System.out.println(String.format("Vertices P:%d vertices Q: %d", surfP.getVertices().length, surfQ.getVertices().length));
	}
	
	/**
     * Resets the two meshes to their initial state when they were loaded in.
     * No need to call update
     */
	public void reset() {
	    m_surfP.setVertices(m_surfP_original.clone());
        m_surfP.update(m_surfP);

        m_surfQ.setVertices(m_surfQ_original.clone());
        m_surfQ.update(m_surfQ);
    }
}
