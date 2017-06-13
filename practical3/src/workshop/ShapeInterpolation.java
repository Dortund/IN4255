package workshop;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import Jama.SingularValueDecomposition;
import jv.geom.PgElementSet;
import jv.object.PsDebug;
import jv.vecmath.PdMatrix;
import jv.vecmath.PdVector;
import jv.vecmath.PiVector;
import jvx.project.PjWorkshop;

public class ShapeInterpolation extends PjWorkshop {
	
	private static final long serialVersionUID = 6630686380694288051L;
	
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
