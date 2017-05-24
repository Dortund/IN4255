package workshop;

import jv.geom.PgElementSet;
import jv.project.PgGeometry;
import jv.vecmath.PdMatrix;
import jv.vecmath.PdVector;
import jv.vecmath.PiVector;
import jvx.numeric.PnSparseMatrix;
import jvx.project.PjWorkshop;

public class ShapeDeformation extends PjWorkshop {
    PgElementSet m_geom;
    PgElementSet m_geomSave;

    public ShapeDeformation() {
        super("Shape deformation algorithm");
        init();
    }

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
     * Computes matrix G for a triangle mesh (task 1)
     * Where G maps a continuous linear polynomial over all triangles of a mesh to its gradient vectors
     */
    public PnSparseMatrix meshToGradient(PgElementSet mesh) {
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
                int column = 3 * triangleIdx + columnIdx;

                G.addEntry(column, triangle.getEntry(0), subGradient.getEntry(columnIdx, 0));
                G.addEntry(column, triangle.getEntry(1), subGradient.getEntry(columnIdx, 1));
                G.addEntry(column, triangle.getEntry(2), subGradient.getEntry(columnIdx, 2));
            }
        }

        return G;
    }

    /**
     * Computes a 3x3 gradient matrix that maps a linear polynomial over a triangle to its gradient vector
     */
    public PdMatrix triangleToGradient(PdVector[] vertices) {
        PdMatrix gradient = new PdMatrix(3, 3);

        PdVector p1 = vertices[0];
        PdVector p2 = vertices[1];
        PdVector p3 = vertices[2];

        PdVector V = PdVector.subNew(p2, p1);
        PdVector W = PdVector.subNew(p3, p1);

        // area = 0.5 * ||(p2 - p1) x (p3 - p1)||
        double area = PdVector.crossNew(V, W).length() * 0.5;

        // Calculate the normal
        double Nx = V.getEntry(1) * W.getEntry(2) - V.getEntry(2) * W.getEntry(1);
        double Ny = V.getEntry(2) * W.getEntry(0) - V.getEntry(0) * W.getEntry(2);
        double Nz = V.getEntry(0) * W.getEntry(1) - V.getEntry(1) * W.getEntry(0);

        PdVector normal = new PdVector(Nx, Ny, Nz);
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
