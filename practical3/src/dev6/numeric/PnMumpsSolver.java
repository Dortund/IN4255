package dev6.numeric;

import jv.object.PsDebug;
import jv.vecmath.PdVector;
import jv.vecmath.PiVector;
import jvx.numeric.PnSparseMatrix;

/**
 * 
 * @author		Christoph von Tycowicz
 * @version		21.10.2010, 1.00 created (cvt)
 */
public class PnMumpsSolver {

	public static long NULL = 0;
	
	//is MUMPS library available
	private static boolean m_available = true;
	public static boolean isAvailable() { return m_available; }
	//load library
	static {
		try{
			if(System.getProperty("os.name").startsWith("Win") && System.getProperty("os.arch").endsWith("64"))
			{	
				System.loadLibrary("w64gcc_s_sjlj-1");
				System.loadLibrary("libstdc++-6");	
				System.loadLibrary("libgfortran-3");
			}
			System.loadLibrary("MumpsJNI");
		}
		catch(UnsatisfiedLinkError e){
			m_available = false;
			if(PsDebug.WARNING){
				PsDebug.warning("Cannot load MUMPS library: "+e.getMessage());
			}
		}
	}
	
	/**
	 * 
	 * @author		Christoph von Tycowicz
	 * @version		Oct 22, 2010, 1.00 created (cvt)
	 */
	public enum Type{
		UNSYMMETRIC(0),
		GENERAL_SYMMETRIC(2),
		SYMMETRIC_POSITIVE_DEFINITE(1);
		
		protected int mumps_id;
		private Type(int id) { mumps_id = id; }
	}
	
	/**
	 * @param sindx
	 * @param snoe
	 * @param stiff
	 * @param x
	 * @param b
	 * @param sym			0 = unsymmetric, 1 = symmetric positive-definite, 2 = general symmetric
	 * @throws Exception
	 */
	private static native void solve(PiVector[] sindx, PiVector snoe, PdVector[] stiff, PdVector x, PdVector b, int sym) throws Exception;

	/**
	 * @param matrix		Must be valid -> {@link PnSparseMatrix#validate()}.
	 * @param x
	 * @param b
	 * @param sym
	 * @throws Exception
	 */
	public static void solve(PnSparseMatrix matrix, PdVector x, PdVector b, Type sym) throws Exception{
		solve(matrix.getColIndices(), matrix.getNumEntries(), matrix.getEntries(), x, b, sym.mumps_id);
	}
	
	/**
	 * Low-level method that analyzes sparsety pattern of matrix.
	 * 
	 * @param sindx		{@link PnSparseMatrix#getColIndices() Column indices of non-zero entries}.
	 * @param snoe		{@link PnSparseMatrix#getNumEntries() Number of non-zeros per row}.
	 * @param sym
	 * @return Pointer to factorization.
	 * @throws Exception
	 */
	public static long analyze(PiVector[] sindx, PiVector snoe, Type sym) throws Exception{
		return analyze(sindx, snoe, sym.mumps_id);
	}
	
	/**
	 * Low-level method that analyzes sparsety pattern of matrix.
	 * 
	 * @param sindx		{@link PnSparseMatrix#getColIndices() Column indices of non-zero entries}.
	 * @param snoe		{@link PnSparseMatrix#getNumEntries() Number of non-zeros per row}.
	 * @param sym
	 * @return Pointer to factorization.
	 * @throws Exception
	 */
	private static native long analyze(PiVector[] sindx, PiVector snoe, int sym) throws Exception;
	
	/**
	 * Low-level method constructong a factorization based on preprocessing from {@link #analyze(PiVector[], PiVector, int)}.
	 * 
	 * @param factorization		Pointer to preprocessed/analyzed matrix.
	 * @param stiff				{@link PnSparseMatrix#getEntries() Non-zero entries} of matrix.
	 * @return Pointer to factorization.
	 * @throws Exception
	 */
	public static native long factor(long factorization, PdVector[] stiff) throws Exception;
	
	/**
	 * @param matrix		Must be valid -> {@link PnSparseMatrix#validate()}.
	 * @param sym
	 * @return Pointer to factorization.
	 * @throws Exception
	 */
	public static long factor(PnSparseMatrix matrix, Type sym) throws Exception{
		long factorization = analyze(matrix.getColIndices(), matrix.getNumEntries(), sym.mumps_id);
		return factor(factorization, matrix.getEntries());
	}
	
	/**
	 * @param factorization
	 * @param x
	 * @param y
	 * @throws Exception
	 */
	public static native void solve(long factorization, PdVector x, PdVector y) throws Exception;
	
	/**
	 * @param factorization
	 * @throws Exception
	 */
	public static native void free(long factorization) throws Exception;
}
