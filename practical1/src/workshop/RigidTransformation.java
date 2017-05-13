package workshop;

import jv.geom.PgElementSet;
import jv.vecmath.PdMatrix;
import jv.vecmath.PdVector;
import jvx.project.PjWorkshop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *  Workshop for surface registration
 */

public class RigidTransformation extends PjWorkshop {
	
	/** First surface to be registered. */	
	PgElementSet	m_surfP;	
	/** Second surface to be registered. */
	PgElementSet	m_surfQ;	
	
	
	/** Constructor */
	public RigidTransformation() {
		super("Surface Registration");
		if (getClass() == RigidTransformation.class) {
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
		m_surfQ = surfQ;
	}

	public PdVector[] getRandomVertices(int nrVertices) {
		PdVector[] allVertices = m_surfP.getVertices();
		if (allVertices.length < nrVertices) {
		    return allVertices;
        }
        // Due references, we have to clone first
        List<PdVector> listVertices = Arrays.asList(allVertices.clone());
        // Use the java8 shuffle
        Collections.shuffle(listVertices);
        // Get the sublist
        return listVertices.subList(0, nrVertices).toArray(new PdVector[nrVertices]);
	}

    /**
     * For every entry in the given vertices finds the closest vertice in set Q for it.
     */
    public PdVector[] getClosestVertices(PdVector[] vertices) {
        PdVector[] closestVertices = new PdVector[vertices.length];
        for(int i = 0; i < vertices.length; i++) {
            closestVertices[i] = findClosestVertices(vertices[i]);
        }
        return closestVertices;
    }

    /**
     * Finds the closest vertice in the set Q from the given vertice
     */
    private PdVector findClosestVertices(PdVector vertice) {
        PdVector[] vertices = m_surfQ.getVertices();
        PdVector current = vertices[0];
        double smallest = vertice.dist(current);
        for(int i = 1; i < vertices.length -1; i++) {
            double dist = vertice.dist(vertices[i]);
            if(dist < smallest) {
                current = vertices[i];
                smallest = dist;
            }
        }
        return current;
    }

    public double getMedian(double[] allDistances) {
        double[] clonedDistances = allDistances.clone();
        Arrays.sort(clonedDistances);
        int middle = clonedDistances.length / 2;
        if (clonedDistances.length % 2 == 0) {
            return (clonedDistances[middle] + clonedDistances[middle - 1]) / 2.0;
        }
        return clonedDistances[middle];
    }

    public double[] getAllDistances(PdVector[] left, PdVector[] right) {
        double[] distances = new double[left.length];
        for(int i = 0; i < left.length; i++) {
            distances[i] = left[i].dist(right[i]);
        }
        return distances;
    }

    public boolean[] getRemoveList(double[] distances, double median, int k) {
        double treshold = median * k;
        boolean[] removeList = new boolean[distances.length];
        for(int i = 0; i < distances.length; i++) {
            removeList[i] = treshold < distances[i];
        }
        return removeList;
    }

    public PdVector[] removeVertices(PdVector[] vertices, boolean[] listToRemove) {
        List<PdVector> reducedList = new ArrayList<>();
        for(int i = 0; i < vertices.length; i++) {
            if(!listToRemove[i]) {
                reducedList.add(vertices[i]);
            }
        }
        return reducedList.toArray(new PdVector[reducedList.size()]);
    }
}
