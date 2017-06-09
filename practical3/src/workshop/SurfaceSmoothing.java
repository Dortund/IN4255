package workshop;

import java.util.ArrayList;

import jv.geom.PgElementSet;
import jv.project.PgGeometry;
import jv.vecmath.PdVector;
import jv.vecmath.PiVector;
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
	
	/** Initialisation */
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
		int numVertices = m_geom.getNumVertices();
		PiVector[] triangles = m_geom.getElements();
		ArrayList<Integer>[] vertex_neighbours = new ArrayList[numVertices];
		//Iterate over all triangles, building a list of neighbours per vertex
		for(int i = 0; i < triangles.length; i++){
			PiVector triangle = triangles[i];
			int[] vertices = triangle.getEntries();
			//Initialise ArrayList if necessary
			if (vertex_neighbours[vertices[0]] == null){
				vertex_neighbours[vertices[0]] = new ArrayList<Integer>();
			}
			if (vertex_neighbours[vertices[1]] == null){
				vertex_neighbours[vertices[1]] = new ArrayList<Integer>();
			}
			if (vertex_neighbours[vertices[2]] == null){
				vertex_neighbours[vertices[2]] = new ArrayList<Integer>();
			}
			
			//Add neighbours of 0 to neighbour list if they're new neighbours
			if (!vertex_neighbours[vertices[0]].contains(vertices[1])){
				vertex_neighbours[vertices[0]].add(vertices[1]);
			}
			if (!vertex_neighbours[vertices[0]].contains(vertices[2])){
				vertex_neighbours[vertices[0]].add(vertices[2]);
			}
			//Add neighbours of 1 to neighbour list if they're new neighbours
			if (!vertex_neighbours[vertices[1]].contains(vertices[0])){
				vertex_neighbours[vertices[1]].add(vertices[0]);
			}
			if (!vertex_neighbours[vertices[1]].contains(vertices[2])){
				vertex_neighbours[vertices[1]].add(vertices[2]);
			}
			//Add neighbours of 1 to neighbour list if they're new neighbours
			if (!vertex_neighbours[vertices[2]].contains(vertices[1])){
				vertex_neighbours[vertices[2]].add(vertices[1]);
			}
			if (!vertex_neighbours[vertices[2]].contains(vertices[0])){
				vertex_neighbours[vertices[2]].add(vertices[0]);
			}
		}
		//Calculate new vertex position for each vertex
		for(int i = 0; i < numVertices; i++){
			PdVector vertex = m_geom.getVertex(i);
			PdVector average = new PdVector(0, 0, 0);
			int numNeighbours = vertex_neighbours[i].size();
			for (int nb_id : vertex_neighbours[i]){
				PdVector nb_vertex = m_geom.getVertex(nb_id);
				average.add(nb_vertex);
			}
			average.multScalar(1/(float)numNeighbours);
			PdVector diff = new PdVector();
			diff.sub(average, vertex);
			diff.multScalar(stepsize);
			vertex.add(diff);
			m_geom.setVertex(i, vertex);
		}
		m_geom.update(m_geom);
	}

	public void explicit(double stepsize) {
		// TODO Auto-generated method stub
		
	}

	public void implicit(double stepsize) {
		// TODO Auto-generated method stub
		
	}
}
