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
	
	public void calcConstantInfo() {
		int numElements = meshOrigin.getNumElements();
		
		angles = new double[numElements];
		rotationAxes = new PdVector[numElements];
		scalings = new PdMatrix[numElements];
		translations = new PdVector[numElements];
		
		PsDebug.warning("getting transforms");
		PdMatrix[] transforms = getTransforms();
		PsDebug.warning("got transforms");
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
	        	PsDebug.warning("eigen stuffs: " + eig.getRealEigenvalues()[v] + ", " + eig.getImagEigenvalues()[v]);
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
	        
	        PdVector o = meshOrigin.getVertex(meshOrigin.getElement(i).getEntry(0));
	        PdVector t = meshTarget.getVertex(meshTarget.getElement(i).getEntry(0));
	        
	        translations[i] = PdVector.subNew(t, o);
	        
	        PsDebug.warning("static translation: " + o + ", " + t);
		}
		PsDebug.warning("got all static info");
	}
	
	public PgElementSet getInterpolatedset(double time) {
		int numElements = meshOrigin.getNumElements();
		
		PgElementSet newSet = new PgElementSet();
		newSet.setNumElements(numElements);
		newSet.setNumVertices(numElements*3);
		
		//PdVector[] vertices = new PdVector[numElements*3];
		//PiVector[] faces = new PiVector[numElements];
		PdVector[] normals = new PdVector[numElements];
		
		PsDebug.warning("Setup data structures");
		
		for (int index = 0; index < numElements; index++) {
			PiVector faceOrigin = meshOrigin.getElement(index);
			PdVector p1 = meshOrigin.getVertex(faceOrigin.getEntry(0));
			PdVector p2 = meshOrigin.getVertex(faceOrigin.getEntry(1));
			PdVector p3 = meshOrigin.getVertex(faceOrigin.getEntry(2));
			PdVector nv = meshOrigin.getElementNormal(index);
			
			PdVector v1 = PdVector.subNew(p2, p1);
			PdVector v2 = PdVector.subNew(p3, p1);
			
			PsDebug.warning("Got direction vectors and normal");
			
			PdMatrix scaler = new PdMatrix(3);
			PsDebug.warning("scaler: " + scaler);
			scaler.multScalar(scalings[index], time);
			
			PsDebug.warning("Got scaler");
			
			PdMatrix id = new PdMatrix(3);
			id.multScalar(identity, 1-time);
			id.add(scaler);
			PsDebug.warning("thingie: " + id);
			
			PsDebug.warning("Got id");
			
			PdMatrix rot = getRotationMatrix(time * angles[index], rotationAxes[index]);
			PsDebug.warning("R: " + rot);
			rot.rightMult(id);
			
			PsDebug.warning("Got R");
			
			PsDebug.warning("resMatrix: " + rot);
			
			v1.leftMultMatrix(rot);
			v2.leftMultMatrix(rot);
			nv.leftMultMatrix(rot);
			
			v1.add(p1);
			v2.add(p1);
			
			PsDebug.warning("Got transformed");
			
			PsDebug.warning("translation: " + translations[index]);
			
			PdVector translation = new PdVector(translations[index].getEntries());
			translation.multScalar(time);
			
			PsDebug.warning("Got translation");
			
			PdVector p1New = PdVector.addNew(p1, translation);
			PdVector p2New = PdVector.addNew(v1, translation);
			PdVector p3New = PdVector.addNew(v2, translation);
			
			newSet.setVertex(index*3 + 0, p1New);
			newSet.setVertex(index*3 + 1, p2New);
			newSet.setVertex(index*3 + 2, p3New);
			//vertices[index*3 + 1] = p2New;
			//vertices[index*3 + 2] = p3New;
			
			//faces[index] = new PiVector(index*3, index*3+1, index*3+2);
			newSet.setElement(index, new PiVector(index*3, index*3+1, index*3+2));
			
			PsDebug.warning("added translation");
			
			nv.normalize();
			normals[index] = nv;
			
			PsDebug.warning("set normal");
		}
		
		//newSet.setVertices(vertices);
		//newSet.setElements(faces);
		newSet.setElementNormals(normals);
		
		newSet.update(newSet);
		
		PsDebug.warning("set data");
		
		return newSet;
	}
	
	private PdMatrix getRotationMatrix(double angle, PdVector axis) {
		try {
		double C = Math.cos(angle);
		double S = Math.sin(angle);
		
		double Cinv = 1.0 - C;
		
		//PsDebug.warning(axis + "");
		
		double a1 = axis.getEntry(0);
		double a2 = axis.getEntry(1);
		double a3 = axis.getEntry(2);
		
		//PsDebug.warning("got info");
		
		PdMatrix res = new PdMatrix(3);
		res.setEntry(0, 0, Math.pow(a1, 2) + C*(1-Math.pow(a1, 2)));
		res.setEntry(1, 0, a1*a2*Cinv + a3*S);
		res.setEntry(2, 0, a1*a3*Cinv - a2*S);
		
		//PsDebug.warning("row 0");
		
		res.setEntry(0, 1, a1*a2*Cinv - a3*S);
		res.setEntry(1, 1, Math.pow(a2, 2)+ C*(1-Math.pow(a2, 2)));
		res.setEntry(2, 1, a2*a3*Cinv + a1*S);
		
		//PsDebug.warning("row 1");
		
		res.setEntry(0, 2, a1*a3*Cinv + a2*S);
		res.setEntry(1, 2, a2*a3*Cinv - a1*S);
		res.setEntry(2, 2, Math.pow(a3, 2) + C*(1-Math.pow(a3, 2)));
		
		//PsDebug.warning("row 2");
		
		return res;
		} catch (Exception e) {
			PsDebug.warning(e.toString());
			return new PdMatrix(3);
		}
	}
	
	private PdMatrix[] getTransforms() {
		boolean r1 = meshOrigin.makeElementNormals();
		boolean r2 = meshTarget.makeElementNormals();
		
		PsDebug.warning("normals Origin: " + r1 + ", normals Target: " + r2);
		
		PdMatrix[] transforms = new PdMatrix[meshOrigin.getNumElements()];
		
		PsDebug.warning("Starting loop");
		for (int i = 0; i < meshOrigin.getNumElements(); i++) {
			PsDebug.warning("Getting transform for: " + i);
			PdMatrix V = getTransform(meshOrigin, i);
			PdMatrix W = getTransform(meshTarget, i);
			
			V.invert();
			W.rightMult(V);
			
			
			transforms[i] = W;
		}
		
		return transforms;
	}
	
	private PdMatrix getTransform(PgElementSet mesh, int index) {
		try {
		PiVector faceOrigin = mesh.getElement(index);
		PdVector p1 = mesh.getVertex(faceOrigin.getEntry(0));
		PdVector p2 = mesh.getVertex(faceOrigin.getEntry(1));
		PdVector p3 = mesh.getVertex(faceOrigin.getEntry(2));
		PsDebug.warning("got vertices");
		PsDebug.warning(mesh.getElementNormals().length + "");
		PdVector nv = mesh.getElementNormal(index);
		PsDebug.warning("Got normal");
		
		PdVector v1 = PdVector.subNew(p2, p1);
		PdVector v2 = PdVector.subNew(p3, p1);
		
		PsDebug.warning("Got directions");
		
		PdMatrix V = new PdMatrix(3, 3);
		V.setColumn(0, v1);
		V.setColumn(1, v2);
		V.setColumn(2, nv);
		PsDebug.warning("Got matrix");
		return V;
		} catch( Exception e) {
			PsDebug.warning(e.toString());
			return new PdMatrix(3);
		}
	}
	
	public PgElementSet getGradientInterpolated(PgElementSet looseMesh) {
		return interpolateSet(meshOrigin, looseMesh);
	}
	
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
       /* PnSparseMatrix matrixG_target = Util.meshToGradient(origin, intermediate);
        //PnSparseMatrix MatrixGTranspose_target = PnSparseMatrix.transposeNew(matrixG_target);
        //PnSparseMatrix matrixM_target = Util.getM(origin);
    	//PnSparseMatrix right = PnSparseMatrix.multMatrices(MatrixGTranspose_target, PnSparseMatrix.multMatrices(matrixM_target, matrixG_target, null), null);
        PnSparseMatrix right = PnSparseMatrix.multMatrices(MatrixGTranspose, PnSparseMatrix.multMatrices(matrixM, matrixG_target, null), null);
    	//s1.add(PnSparseMatrix.multScalar(matrixM, 0.0001));
    	PnSparseMatrix rightHand = PnSparseMatrix.copyNew(right);
    	
    	// Get the current x/y/z values
        PdVector x_target = new PdVector(origin.getNumVertices());
        PdVector y_target = new PdVector(origin.getNumVertices());
        PdVector z_target = new PdVector(origin.getNumVertices());
        for(int i = 0; i < origin.getNumVertices(); i++) {
            x_target.setEntry(i, origin.getVertex(i).getEntry(0));
            y_target.setEntry(i, origin.getVertex(i).getEntry(1));
            z_target.setEntry(i, origin.getVertex(i).getEntry(2));
        }
        
        PdVector xGradient = PnSparseMatrix.rightMultVector(rightHand, x_target, null);
        PdVector yGradient = PnSparseMatrix.rightMultVector(rightHand, y_target, null);
        PdVector zGradient = PnSparseMatrix.rightMultVector(rightHand, z_target, null);
        
        PsDebug.warning(xGradient + "");
        PsDebug.warning(yGradient + "");
        PsDebug.warning(zGradient + "");*/
        
        PdVector[] g = Util.meshToGradientVector(origin, intermediate);
        PnSparseMatrix right = PnSparseMatrix.multMatrices(MatrixGTranspose, matrixM, null);
        
        PdVector xGradient = PnSparseMatrix.rightMultVector(right, g[0], null);
        PdVector yGradient = PnSparseMatrix.rightMultVector(right, g[1], null);
        PdVector zGradient = PnSparseMatrix.rightMultVector(right, g[2], null);
        
        PsDebug.warning("Solving linear problems");
    	try {
    		/*
    		long pointerToFactorization = PnMumpsSolver.factor(s1, PnMumpsSolver.Type.GENERAL_SYMMETRIC);
			PnMumpsSolver.solve(pointerToFactorization, x, rightX);
			PnMumpsSolver.solve(pointerToFactorization, y, rightY);
			PnMumpsSolver.solve(pointerToFactorization, z, rightZ);*/
			
			//PnMumpsSolver.solve(leftHand, x, rightX, Type.GENERAL_SYMMETRIC);
			//PnMumpsSolver.solve(leftHand, y, rightY, Type.GENERAL_SYMMETRIC);
			//PnMumpsSolver.solve(leftHand, z, rightZ, Type.GENERAL_SYMMETRIC);
    		
    		PnBiconjugateGradient solver = new PnBiconjugateGradient();
    		
    		solver.solve(leftHand, x, xGradient);
    		solver.solve(leftHand, y, yGradient);
    		solver.solve(leftHand, z, zGradient);
		} catch (Exception e) {
			e.printStackTrace();
			PsDebug.message("Failed to solve.\n" + e.toString());
		}
    	PsDebug.warning("Linear problems solved");
    	
    	PdVector[] vertices = copy.getVertices();
    	
    	// Calculate the old and new mean
    	PsDebug.warning("Calculating difference in mean");
    	PdVector sumOld = new PdVector(3);
    	PdVector sumNew = new PdVector(3);
    	for (int vIndex = 0; vIndex < origin.getNumVertices(); vIndex++) {
    		PdVector vertexReal = vertices[vIndex];
    		sumOld.add(vertexReal);
    		
    		sumNew.setEntry(0, sumNew.getEntry(0) + x.getEntry(vIndex));
    		sumNew.setEntry(1, sumNew.getEntry(1) + y.getEntry(vIndex));
    		sumNew.setEntry(2, sumNew.getEntry(2) + z.getEntry(vIndex));
    	}
    	sumOld.multScalar(1.0 / origin.getNumVertices());
    	sumNew.multScalar(1.0 / origin.getNumVertices());
    	
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
    	
    	//copy.update(copy);
		
    	//PsDebug.warning(matrixG_target + "");
    	//PsDebug.warning(Util.meshToGradient(copy) + "");
    	
		return copy;
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
