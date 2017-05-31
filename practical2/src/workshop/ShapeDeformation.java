package workshop;

import dev6.numeric.PnMumpsSolver;
import jv.geom.PgElementSet;
import jv.object.PsDebug;
import jv.object.PsObject;
import jv.project.PgGeometry;
import jv.vecmath.PdMatrix;
import jv.vecmath.PdVector;
import jv.vecmath.PiVector;
import jvx.numeric.PnSparseMatrix;
import jvx.project.PjWorkshop;

public class ShapeDeformation extends PjWorkshop {
    PgElementSet m_geom;
    PgElementSet m_geomSave;
    
    PnSparseMatrix matrixG = null;
    PnSparseMatrix matrixM = null;
    long pointerToFactorization;

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
        
        calculateMatrices();
    }
    
    private void calculateMatrices() {
    	matrixG = meshToGradient();
    	matrixM = getM();
    	PnSparseMatrix s1 = PnSparseMatrix.multMatrices(matrixM, matrixG, null);
    	PnSparseMatrix s2 = PnSparseMatrix.multMatrices(PnSparseMatrix.transposeNew(matrixG), matrixM, null);
    	
    	try {
    		pointerToFactorization = PnMumpsSolver.factor(s2, PnMumpsSolver.Type.GENERAL_SYMMETRIC);
		} catch (Exception e) {
			e.printStackTrace();
			PsDebug.message("Failed to factorize.\n" + e.toString());
		}
    }
    
    private PnSparseMatrix getM() {
    	PnSparseMatrix M = new PnSparseMatrix(m_geom.getNumElements() * 3, m_geom.getNumElements() * 3, 1);
    	PiVector[] triangles = m_geom.getElements();
    	for(int triangleIdx = 0; triangleIdx < triangles.length; triangleIdx++) {
            PiVector triangle = triangles[triangleIdx];
            
            PdVector[] vertices = new PdVector[]{
            		m_geom.getVertex(triangle.getEntry(0)),
            		m_geom.getVertex(triangle.getEntry(1)),
            		m_geom.getVertex(triangle.getEntry(2))};
            
            PdVector p1 = vertices[0];
            PdVector p2 = vertices[1];
            PdVector p3 = vertices[2];

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
    
    public void deformSelected(PdMatrix deformMatrix) {
    	PdVector x = new PdVector(m_geom.getNumVertices());
    	PdVector y = new PdVector(m_geom.getNumVertices());
    	PdVector z = new PdVector(m_geom.getNumVertices());
    	
    	PdVector[] gTildes = calcGtilde(deformMatrix);
    	
    	PnSparseMatrix gTranspose = PnSparseMatrix.transposeNew(matrixG);
    	
    	PnSparseMatrix rightMatrix = PnSparseMatrix.multMatrices(gTranspose, matrixM, null);
    	
    	PdVector rightX = PnSparseMatrix.rightMultVector(rightMatrix, gTildes[0], null);
    	PdVector rightY = PnSparseMatrix.rightMultVector(rightMatrix, gTildes[1], null);
    	PdVector rightZ = PnSparseMatrix.rightMultVector(rightMatrix, gTildes[2], null);
    	
    	try {
			PnMumpsSolver.solve(pointerToFactorization, x, rightX);
			PnMumpsSolver.solve(pointerToFactorization, y, rightY);
			PnMumpsSolver.solve(pointerToFactorization, z, rightZ);
		} catch (Exception e) {
			e.printStackTrace();
			PsDebug.message("Failed to solve.\n" + e.toString());
		}
    	
    	PdVector[] vertices = m_geom.getVertices();
    	for (int vIndex = 0; vIndex < m_geom.getNumVertices(); vIndex++) {
    		PdVector vertex = vertices[vIndex];
    		vertex.setEntry(0, x.getEntry(vIndex));
    		vertex.setEntry(1, y.getEntry(vIndex));
    		vertex.setEntry(2, z.getEntry(vIndex));
    	}
    	
    	
    }

    /**
     * Computes matrix G for a triangle mesh (task 1)
     * Where G maps a continuous linear polynomial over all triangles of a mesh to its gradient vectors
     */
    public PnSparseMatrix meshToGradient() {
    	if (m_geom == null)
    		return null;
    	
        // 3#T x #V matrix
        PnSparseMatrix G = new PnSparseMatrix(m_geom.getNumElements() * 3, m_geom.getNumVertices(), 3);
        PiVector[] triangles = m_geom.getElements();

        for(int triangleIdx = 0; triangleIdx < triangles.length; triangleIdx++) {
            PiVector triangle = triangles[triangleIdx];

            PdMatrix subGradient = triangleToGradient(new PdVector[]{
            		m_geom.getVertex(triangle.getEntry(0)),
            		m_geom.getVertex(triangle.getEntry(1)),
            		m_geom.getVertex(triangle.getEntry(2))});

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

    /**
     * Calculates the g tilde vectors for x, y and z.
     * @param deformMatrix
     * @return
     */
    public PdVector[] calcGtilde(PdMatrix deformMatrix) {
        PdVector x = new PdVector();
        PdVector y = new PdVector();
        PdVector z = new PdVector();
        for(int i = 0; i < m_geom.getNumElements(); i++) {
            x.setEntry(i, m_geom.getVertex(i).getEntry(0));
            y.setEntry(i, m_geom.getVertex(i).getEntry(1));
            z.setEntry(i, m_geom.getVertex(i).getEntry(2));
        }

        PnSparseMatrix gradientMatrix = PnSparseMatrix.copyNew(matrixG);

        PdVector xGradient = PnSparseMatrix.rightMultVector(gradientMatrix, x, null);
        PdVector yGradient = PnSparseMatrix.rightMultVector(gradientMatrix, y, null);
        PdVector zGradient = PnSparseMatrix.rightMultVector(gradientMatrix, z, null);

        // multiple with user selected matrix for each selected triangle
        PiVector[] triangles = m_geom.getElements();

        for(int triangleIdx = 0; triangleIdx < triangles.length; triangleIdx++) {
        	if (triangles[triangleIdx].hasTag(PsObject.IS_SELECTED)) {
            	addDeformationMatrix(deformMatrix, xGradient, triangleIdx);
            	addDeformationMatrix(deformMatrix, yGradient, triangleIdx);
            	addDeformationMatrix(deformMatrix, zGradient, triangleIdx);
            }
        }
        
        PdVector[] res = new PdVector[3];
        res[0] = xGradient;
        res[1] = yGradient;
        res[2] = zGradient;
        
        return res;
    }
    
    private void addDeformationMatrix(PdMatrix deform, PdVector vector, int index) {
    	PdVector temp = new PdVector(3);
    	temp.setEntry(0, vector.getEntry((index*3) + 0));
    	temp.setEntry(1, vector.getEntry((index*3) + 1));
    	temp.setEntry(2, vector.getEntry((index*3) + 2));
    	
    	temp.leftMultMatrix(deform);
    	
    	vector.setEntry((index*3) + 0, temp.getEntry(0));
    	vector.setEntry((index*3) + 1, temp.getEntry(1));
    	vector.setEntry((index*3) + 2, temp.getEntry(2));
    }
}
