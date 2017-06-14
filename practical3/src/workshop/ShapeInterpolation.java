package workshop;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import Jama.SingularValueDecomposition;
import jv.geom.PgElementSet;
import jv.object.PsDebug;
import jv.vecmath.PdMatrix;
import jv.vecmath.PdVector;
import jv.vecmath.PiVector;
import jvx.numeric.PnBiconjugateGradient;
import jvx.numeric.PnSparseMatrix;
import jvx.project.PjWorkshop;

public class ShapeInterpolation extends PjWorkshop {
	
	private static final long serialVersionUID = 6630686380694288051L;
	
	/** First surface to be registered. */	
	PgElementSet	meshOrigin;
    /** Keeps the a copy of the vertices of P when loaded
     * This can be used to reset P.
     */
    PdVector[] meshOriginBackup;
    PdVector[] meshOriginNormalsBackup;
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
    
    static PdMatrix identity;

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
		identity = new PdMatrix(3);
		identity.setEntry(0, 0, 1);
		identity.setEntry(1, 1, 1);
		identity.setEntry(2, 2, 1);
	}
	
	/**
	 * Calculates all the data which stays constant during all interpolations
	 */
	public void calcConstantInfo() {
		int numElements = meshOrigin.getNumElements();
		
		angles = new double[numElements];
		rotationAxes = new PdVector[numElements];
		scalings = new PdMatrix[numElements];
		translations = new PdVector[numElements];
		
		// Get transforms
		PdMatrix[] transforms = getTransforms();
		
		// Calculate all the constant data for every element
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
	        
	        Matrix temp = R.plus(R.transpose()).times(0.5);
	        EigenvalueDecomposition eig = temp.eig();
	        for (int v = 0; v < eig.getRealEigenvalues().length; v++){
	        	if (Math.abs(eig.getRealEigenvalues()[v] - 1.0) <= 0.00001 && Math.abs(eig.getImagEigenvalues()[v]) <= 0.00001) {
	        		double[] entries = eig.getV().getColumnPackedCopy();
	        		PdVector axis = new PdVector(3);
	        		axis.setEntry(0, entries[v*3 + 0]);
	        		axis.setEntry(1, entries[v*3 + 1]);
	        		axis.setEntry(2, entries[v*3 + 2]);
	        		rotationAxes[i] = axis;
	        		break;
	        	}
	        }
	        
	        // Check if the angle is correct, or if we need to rotate in the other direction
	        if (!testAngle(angles[i], rotationAxes[i], R)) {
	        	angles[i] = -angles[i];
	        }
	        
	        PdVector o = meshOrigin.getVertex(meshOrigin.getElement(i).getEntry(0));
	        PdVector t = meshTarget.getVertex(meshTarget.getElement(i).getEntry(0));
	        
	        translations[i] = PdVector.subNew(t, o);
		}
		PsDebug.warning("Calculated all constant info");
	}
	
	/**
	 * Use this function to test of we have the correct angle.
	 * @param angle
	 * @param axis
	 * @param target
	 * @return
	 */
	private boolean testAngle(double angle, PdVector axis, Matrix target) {
		PdMatrix temp = getRotationMatrix(angle, axis);
		
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				double a = temp.getEntry(row, col);
				double b = target.get(row, col);
				if (Math.signum(a) != Math.signum(b)) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Creates a 'loose' mesh of interpolated elements
	 * @param time
	 * @return
	 */
	public PgElementSet getInterpolatedset(double time) {
		int numElements = meshOrigin.getNumElements();
		
		// Initiate the new geometry
		PgElementSet newSet = new PgElementSet();
		newSet.setNumElements(numElements);
		newSet.setNumVertices(numElements*3);
		
		PdVector[] normals = new PdVector[numElements];
		
		// Calculate the interpolated position for every element
		for (int index = 0; index < numElements; index++) {
			PiVector faceOrigin = meshOrigin.getElement(index);
			PdVector p1 = meshOrigin.getVertex(faceOrigin.getEntry(0));
			PdVector p2 = meshOrigin.getVertex(faceOrigin.getEntry(1));
			PdVector p3 = meshOrigin.getVertex(faceOrigin.getEntry(2));
			PdVector nv = meshOrigin.getElementNormal(index);
			
			PdVector v1 = PdVector.subNew(p2, p1);
			PdVector v2 = PdVector.subNew(p3, p1);
			
			// Calculate the first scaling part of the interpolation
			PdMatrix scaler = new PdMatrix(3);
			scaler.multScalar(scalings[index], time);
			
			// Calculate the second part of the scaling of the interpolation
			PdMatrix id = new PdMatrix(3);
			id.multScalar(identity, 1-time);
			
			// Add parts
			id.add(scaler);
			
			// Calculate the rotation part of the interpolation
			PdMatrix rot = getRotationMatrix(time * angles[index], rotationAxes[index]);
			
			// Add parts
			rot.rightMult(id);
			
			// Interpolate elements
			v1.leftMultMatrix(rot);
			v2.leftMultMatrix(rot);
			nv.leftMultMatrix(rot);
			
			// Restore vectors to proper vertex positions
			v1.add(p1);
			v2.add(p1);
			
			// Get the translation part of the interpolation
			PdVector translation = new PdVector(translations[index].getEntries());
			translation.multScalar(time);
			
			// Add the translation part
			PdVector p1New = PdVector.addNew(p1, translation);
			PdVector p2New = PdVector.addNew(v1, translation);
			PdVector p3New = PdVector.addNew(v2, translation);
			
			// Set the new vertex positions
			newSet.setVertex(index*3 + 0, p1New);
			newSet.setVertex(index*3 + 1, p2New);
			newSet.setVertex(index*3 + 2, p3New);
			
			// Set the new Element indices
			newSet.setElement(index, new PiVector(index*3, index*3+1, index*3+2));
			
			// Add the normal
			nv.normalize();
			normals[index] = nv;
		}
		
		newSet.setElementNormals(normals);
		
		newSet.update(newSet);
		
		PsDebug.warning("Created Loose Mesh");
		
		return newSet;
	}
	
	/**
	 * Get a rotation matrix using a given angle and axis
	 * @param angle The angle
	 * @param axis The axis
	 * @return The rotation matrix
	 */
	private PdMatrix getRotationMatrix(double angle, PdVector axis) {
		double C = Math.cos(angle);
		double S = Math.sin(angle);
		
		double Cinv = 1.0 - C;
		
		double a1 = axis.getEntry(0);
		double a2 = axis.getEntry(1);
		double a3 = axis.getEntry(2);
		
		PdMatrix res = new PdMatrix(3);
		res.setEntry(0, 0, Math.pow(a1, 2) + C*(1-Math.pow(a1, 2)));
		res.setEntry(1, 0, a1*a2*Cinv + a3*S);
		res.setEntry(2, 0, a1*a3*Cinv - a2*S);
		
		res.setEntry(0, 1, a1*a2*Cinv - a3*S);
		res.setEntry(1, 1, Math.pow(a2, 2)+ C*(1-Math.pow(a2, 2)));
		res.setEntry(2, 1, a2*a3*Cinv + a1*S);
		
		res.setEntry(0, 2, a1*a3*Cinv + a2*S);
		res.setEntry(1, 2, a2*a3*Cinv - a1*S);
		res.setEntry(2, 2, Math.pow(a3, 2) + C*(1-Math.pow(a3, 2)));
		
		return res;
	}
	
	/**
	 * Get the H matrices for every element
	 * @return A matrix containing the matrix H on position i for an element i.
	 */
	private PdMatrix[] getTransforms() {
		meshOrigin.makeElementNormals();
		meshTarget.makeElementNormals();
		
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
	
	/**
	 * Get the matrix H for a single element
	 * @param mesh The geometry containing the element
	 * @param index The index of the element
	 * @return The matrix H
	 */
	private PdMatrix getTransform(PgElementSet mesh, int index) {
		PiVector faceOrigin = mesh.getElement(index);
		PdVector p1 = mesh.getVertex(faceOrigin.getEntry(0));
		PdVector p2 = mesh.getVertex(faceOrigin.getEntry(1));
		PdVector p3 = mesh.getVertex(faceOrigin.getEntry(2));
		PdVector nv = mesh.getElementNormal(index);
		
		PdVector v1 = PdVector.subNew(p2, p1);
		PdVector v2 = PdVector.subNew(p3, p1);
		
		PdMatrix V = new PdMatrix(3, 3);
		V.setColumn(0, v1);
		V.setColumn(1, v2);
		V.setColumn(2, nv);
		
		return V;
	}
	
	public PgElementSet getGradientInterpolated(PgElementSet looseMesh) {
		return interpolateSet(meshOrigin, looseMesh);
	}
	
	/**
	 * Creates a geometry which is closed and tries to be as close as the target geometry as possible
	 * @param origin
	 * @param intermediate
	 * @return
	 */
	private PgElementSet interpolateSet(PgElementSet origin, PgElementSet intermediate) {
		PgElementSet copy = (PgElementSet) origin.clone();
        
        PsDebug.warning("Calculating left hand");
        PnSparseMatrix matrixG = Util.meshToGradient(origin);
        PnSparseMatrix MatrixGTranspose = PnSparseMatrix.transposeNew(matrixG);
        PnSparseMatrix matrixM = Util.getM(origin);
    	PnSparseMatrix left = PnSparseMatrix.multMatrices(MatrixGTranspose, PnSparseMatrix.multMatrices(matrixM, matrixG, null), null);
    	//s1.add(PnSparseMatrix.multScalar(matrixM, 0.0001));
    	PnSparseMatrix leftHand = PnSparseMatrix.copyNew(left);
    	
    	PsDebug.warning("Creating variables");
    	PdVector x = new PdVector(origin.getNumVertices());
    	PdVector y = new PdVector(origin.getNumVertices());
    	PdVector z = new PdVector(origin.getNumVertices());
        
        PsDebug.warning("Calculating right hand");
        
        PdVector[] g = Util.meshToGradientVector(origin, intermediate);
        PnSparseMatrix right = PnSparseMatrix.multMatrices(MatrixGTranspose, matrixM, null);
        
        PdVector xGradient = PnSparseMatrix.rightMultVector(right, g[0], null);
        PdVector yGradient = PnSparseMatrix.rightMultVector(right, g[1], null);
        PdVector zGradient = PnSparseMatrix.rightMultVector(right, g[2], null);
        
        PsDebug.warning("Solving linear problems");
    	try {
    		PnBiconjugateGradient solver = new PnBiconjugateGradient();
    		
    		solver.solve(leftHand, x, xGradient);
    		solver.solve(leftHand, y, yGradient);
    		solver.solve(leftHand, z, zGradient);
		} catch (Exception e) {
			e.printStackTrace();
			PsDebug.message("Failed to solve.\n" + e.toString());
		}
    	PsDebug.warning("Linear problems solved");
    	
    	// Calculate the old and new mean
    	PsDebug.warning("Calculating difference in mean");
    	PdVector sumNew = new PdVector(3);
    	for (int vIndex = 0; vIndex < origin.getNumVertices(); vIndex++) {
    		sumNew.setEntry(0, sumNew.getEntry(0) + x.getEntry(vIndex));
    		sumNew.setEntry(1, sumNew.getEntry(1) + y.getEntry(vIndex));
    		sumNew.setEntry(2, sumNew.getEntry(2) + z.getEntry(vIndex));
    	}
    	sumNew.multScalar(1.0 / origin.getNumVertices());
    	PdVector sumOld = getMean(intermediate.getVertices());
    	
    	// Get the translation from the new mean to the old mean
    	PdVector translationMean = PdVector.subNew(sumOld, sumNew);
    	
    	PsDebug.warning("Setting new vertex coordinates");
    	for (int vIndex = 0; vIndex < copy.getNumVertices(); vIndex++) {
    		PdVector newV = new PdVector(3);
    		newV.setEntry(0, x.getEntry(vIndex));
    		newV.setEntry(1, y.getEntry(vIndex));
    		newV.setEntry(2, z.getEntry(vIndex));
    		
    		newV.add(translationMean);
    		
    		copy.setVertex(vIndex, newV);
    	}
    	
		return copy;
	}
	
	private PdVector getMean(PdVector[] vertices) {
		PdVector sum = new PdVector(3);
		for (PdVector v : vertices) {
			sum.add(v);
		}
		sum.multScalar(1.0 / vertices.length);
		return sum;
	}
	
	/** Set two Geometries. */
	public void setGeometries(PgElementSet surfP, PgElementSet surfQ) {
		meshOrigin = surfP;
        meshOriginBackup = PdVector.copyNew(surfP.getVertices());
		meshTarget = surfQ;
        meshTargetBackup = PdVector.copyNew(surfQ.getVertices());
        PsDebug.warning(String.format("Faces Origin:%d, Faces Target: %d", meshOrigin.getNumElements(), meshTarget.getNumElements()));
        
        calcConstantInfo();
        PsDebug.warning("Calculated static information");
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
