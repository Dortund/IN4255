package workshop;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import Jama.SingularValueDecomposition;
import jv.geom.PgElementSet;
import jv.vecmath.PdMatrix;
import jv.vecmath.PdVector;
import jv.vecmath.PiVector;
import jvx.project.PjWorkshop;

public class ShapeInterpolation extends PjWorkshop {
	
	/** First surface to be registered. */	
	PgElementSet	meshOrigin;
    /** Keeps the a copy of the vertices of P when loaded
     * This can be used to reset P.
     */
    PdVector[] meshOriginBackup;
	/** Second surface to be registered. */
	PgElementSet	meshTarget;
    /** Keeps the a copy of the vertices of Q when loaded
     * This can be used to reset Q.
     */
    PdVector[] meshTargetBackup;
    
    double[] angles;
    PdVector[] rotationAxes;
    PdMatrix[] scalings;
    PdVector[] translations;

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
	
	public void interpolate(double time) {
		int numElements = meshOrigin.getNumElements();
		
		angles = new double[numElements];
		rotationAxes = new PdVector[numElements];
		scalings = new PdMatrix[numElements];
		translations = new PdVector[numElements];
		
		PdMatrix[] transforms = getTransforms();
		
		for (int i = 0; i < numElements; i++) {
			SingularValueDecomposition svd = new SingularValueDecomposition(new Matrix(transforms[i].getEntries()));
			
	        Matrix V = svd.getV();
	        Matrix Vt = svd.getV().transpose();
	        Matrix U = svd.getU();
	        Matrix UVt = U.times(Vt);
	        
	        double det = UVt.det();
	        
	        Matrix Z = Matrix.identity(3, 3);
	        Z.set(2, 2, det);
	        
	        Matrix R = U.times(Z).times(Vt);
	        Matrix S = V.times(Z).times(svd.getS()).times(Vt);
	        
	        scalings[i] = new PdMatrix(S.getArray());
	        
	        angles[i] = Math.acos((R.trace()-1)/2.0);
	        
	        Matrix temp = R.times(R.transpose()).times(0.5);
	        EigenvalueDecomposition eig = temp.eig();
	        for (int v = 0; v < eig.getRealEigenvalues().length; v++){
	        	if (eig.getRealEigenvalues()[v] == 1 && eig.getImagEigenvalues()[v] == 0) {
	        		double[] entries = eig.getV().getColumnPackedCopy();
	        		PdVector axis = new PdVector(3);
	        		axis.setEntry(0, entries[v*3 + 0]);
	        		axis.setEntry(1, entries[v*3 + 1]);
	        		axis.setEntry(2, entries[v*3 + 2]);
	        		rotationAxes[i] = axis;
	        	}
	        }
	        
	        PdVector o = meshOrigin.getVertex(meshOrigin.getElement(i).getEntry(0));
	        PdVector t = meshTarget.getVertex(meshTarget.getElement(i).getEntry(0));
	        
	        translations[i] = PdVector.subNew(t, o);
		}
	}
	
	private PdMatrix[] getTransforms() {
		meshOrigin.makeElementNormals();
		meshOrigin.makeElementNormals();
		
		PdMatrix[] transforms = new PdMatrix[meshOrigin.getNumElements()];
		
		for (int i = 0; i < meshOrigin.getNumElements(); i++) {
			PdMatrix V = getTransform(meshOrigin, i);
			PdMatrix W = getTransform(meshTarget, i);
			
			V.invert();
			W.rightMult(V);
			
			transforms[i] = W;
		}
		
		return transforms;
	}
	
	private PdMatrix getTransform(PgElementSet mesh, int index) {
		PiVector faceOrigin = mesh.getElement(index);
		PdVector p1 = mesh.getVertex(faceOrigin.getEntry(0));
		PdVector p2 = mesh.getVertex(faceOrigin.getEntry(1));
		PdVector p3 = mesh.getVertex(faceOrigin.getEntry(2));
		PdVector nv = mesh.getElementNormal(index);
		
		PdVector v1 = PdVector.subNew(p3, p1);
		PdVector v2 = PdVector.subNew(p2, p1);
		
		PdMatrix V = new PdMatrix(3, 3);
		V.setColumn(0, v1);
		V.setColumn(1, v2);
		V.setColumn(2, nv);
		
		return V;
	}
	
	/** Set two Geometries. */
	public void setGeometries(PgElementSet surfP, PgElementSet surfQ) {
		meshOrigin = surfP;
        meshOriginBackup = PdVector.copyNew(surfP.getVertices());
		meshTarget = surfQ;
        meshTargetBackup = PdVector.copyNew(surfQ.getVertices());
        System.out.println(String.format("Vertices P:%d vertices Q: %d", surfP.getVertices().length, surfQ.getVertices().length));
	}
	
	/**
     * Resets the two meshes to their initial state when they were loaded in.
     * No need to call update
     */
	public void reset() {
	    meshOrigin.setVertices(meshOriginBackup.clone());
        meshOrigin.update(meshOrigin);

        meshTarget.setVertices(meshTargetBackup.clone());
        meshTarget.update(meshTarget);
    }
}
