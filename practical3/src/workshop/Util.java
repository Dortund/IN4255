package workshop;

import jv.geom.PgElementSet;
import jv.vecmath.PdMatrix;
import jv.vecmath.PdVector;
import jv.vecmath.PiVector;
import jvx.numeric.PnSparseMatrix;

public class Util {
	
	/**
	 * Get the M_v matrix for the given mesh
	 * @param mesh The mesh
	 * @return The matrix M_v
	 */
	public static PnSparseMatrix getM(PgElementSet mesh) {
    	PnSparseMatrix M = new PnSparseMatrix(mesh.getNumElements() * 3, mesh.getNumElements() * 3, 1);
    	PiVector[] triangles = mesh.getElements();
    	for(int triangleIdx = 0; triangleIdx < triangles.length; triangleIdx++) {
            PiVector triangle = triangles[triangleIdx];
            
            PdVector p1 = mesh.getVertex(triangle.getEntry(0));
            PdVector p2 = mesh.getVertex(triangle.getEntry(1));
            PdVector p3 = mesh.getVertex(triangle.getEntry(2));

            PdVector V = PdVector.subNew(p2, p1);
            PdVector W = PdVector.subNew(p3, p1);

            // area = 0.5 * ||(p2 - p1) x (p3 - p1)||
            double area = PdVector.crossNew(V, W).length() * 0.5;
            
            int pos = triangleIdx*3;
            
            M.setEntry(pos, pos, area);
            M.setEntry(pos+1, pos+1, area);
            M.setEntry(pos+2, pos+2, area);
    	}
    	return M;
    }
	
	/**
	 * Get a gradient matrix with the gradients of a target mesh with the same number of faces
	 * @param mesh Mesh to calculate gradients for
	 * @param gradientTarget The mesh that is used to get the actual gradient
	 * @return gradient matrix G
	 */
	public static PnSparseMatrix meshToGradient(PgElementSet mesh, PgElementSet gradientTarget) {
        // 3#T x #V matrix
        PnSparseMatrix G = new PnSparseMatrix(mesh.getNumElements() * 3, mesh.getNumVertices(), 3);
        PiVector[] trianglesMesh = mesh.getElements();
        PiVector[] trianglesTarget = gradientTarget.getElements();

        for(int triangleIdx = 0; triangleIdx < trianglesMesh.length; triangleIdx++) {
            PiVector triangleMesh = trianglesMesh[triangleIdx];
            PiVector triangleTarget = trianglesTarget[triangleIdx];

            PdMatrix subGradient = triangleToGradient(new PdVector[]{
            		gradientTarget.getVertex(triangleTarget.getEntry(0)),
            		gradientTarget.getVertex(triangleTarget.getEntry(1)),
            		gradientTarget.getVertex(triangleTarget.getEntry(2))});

            for(int columnIdx = 0; columnIdx < 3; columnIdx++) {
                int column = 3 * triangleIdx;

                G.addEntry(column, triangleMesh.getEntry(columnIdx), subGradient.getColumn(columnIdx).getEntry(0));
                G.addEntry(column + 1, triangleMesh.getEntry(columnIdx), subGradient.getColumn(columnIdx).getEntry(1));
                G.addEntry(column + 2, triangleMesh.getEntry(columnIdx), subGradient.getColumn(columnIdx).getEntry(2));
            }
        }

        return G;
    }
	
	/**
	 * Computes matrix G for a triangle mesh
	 * @param mesh The triangle mesh
	 * @return The matrix G
	 */
	public static PnSparseMatrix meshToGradient(PgElementSet mesh) {
        // 3#T x #V matrix
        PnSparseMatrix G = new PnSparseMatrix(mesh.getNumElements() * 3, mesh.getNumVertices(), 3);
        PiVector[] triangles = mesh.getElements();

        for(int triangleIdx = 0; triangleIdx < triangles.length; triangleIdx++) {
            PiVector triangle = triangles[triangleIdx];

            PdMatrix subGradient = triangleToGradient(new PdVector[]{
            		mesh.getVertex(triangle.getEntry(0)),
            		mesh.getVertex(triangle.getEntry(1)),
            		mesh.getVertex(triangle.getEntry(2))});

            for(int columnIdx = 0; columnIdx < 3; columnIdx++) {
                int column = 3 * triangleIdx;

                G.addEntry(column, triangle.getEntry(columnIdx), subGradient.getColumn(columnIdx).getEntry(0));
                G.addEntry(column + 1, triangle.getEntry(columnIdx), subGradient.getColumn(columnIdx).getEntry(1));
                G.addEntry(column + 2, triangle.getEntry(columnIdx), subGradient.getColumn(columnIdx).getEntry(2));
            }
        }

        return G;
    }

    /**
     * Computes a 3x3 gradient matrix that maps a linear polynomial over a triangle to its gradient vector
     */
    private static PdMatrix triangleToGradient(PdVector[] vertices) {
        PdMatrix gradient = new PdMatrix(3, 3);

        PdVector p1 = vertices[0];
        PdVector p2 = vertices[1];
        PdVector p3 = vertices[2];

        PdVector V = PdVector.subNew(p2, p1);
        PdVector W = PdVector.subNew(p3, p1);

        // area = 0.5 * ||(p2 - p1) x (p3 - p1)||
        double area = PdVector.crossNew(V, W).length() * 0.5;

        // Calculate the normal
        PdVector normal = PdVector.crossNew(V, W);
        normal.normalize();

        PdVector e1 = PdVector.subNew(p3, p2);
        PdVector e2 = PdVector.subNew(p1, p3);
        PdVector e3 = PdVector.subNew(p2, p1);

        // Setup the gradient matrix: 1/(2*area) (n * e1, n * e2, n * e3)
        gradient.setColumn(0, PdVector.crossNew(normal, e1));
        gradient.setColumn(1, PdVector.crossNew(normal, e2));
        gradient.setColumn(2, PdVector.crossNew(normal, e3));

        gradient.multScalar(1.0 / (area * 2));

        return gradient;
    }
}
