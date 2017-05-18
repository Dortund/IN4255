package workshop;

import Jama.SingularValueDecomposition;
import jv.geom.PgElementSet;
import jv.object.PsDialog;
import jv.object.PsUpdateIf;
import jv.objectGui.PsList;
import jv.project.PgGeometryIf;
import jv.project.PvGeometryIf;
import jv.vecmath.PdMatrix;
import jv.vecmath.PdVector;
import jv.viewer.PvDisplay;
import jvx.project.PjWorkshop_IP;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

/**
 * Info Panel of Workshop for surface registration
 *
 */
public class RigidTransformation_IP extends PjWorkshop_IP implements ActionListener{
	protected	List			m_listActive;
	protected	List			m_listPassive;
	protected	Vector			m_geomList;
	protected workshop.RigidTransformation m_registration;
	protected   Button			m_bSetSurfaces;

    final private double K = 3;
    final private int NR_VERTICES = 250;
    final private double MAX_ERROR = 0.020;
    final private int MAX_STEPS = 500;

    protected Button btnTestConfig;
	protected Button btnTransform;
    protected Button btnReset;
	protected Button btnRandomRotation;
    protected Button btnRandomTranslation;
	protected Label lbl;
    protected Label lblMedian;
    protected Label lblConfig;
    protected Label lblNewSize;
	protected Label lblError;
	protected Label lblStep;

	/** Constructor */
	public RigidTransformation_IP () {
		super();
		if (getClass() == RigidTransformation_IP.class)
			init();
	}

	/**
	 * Informational text on the usage of the dialog.
	 * This notice will be displayed if this info panel is shown in a dialog.
	 * The text is split at line breaks into individual lines on the dialog.
	 */
	public String getNotice() {
		return "Part 2 of assignment2: iterative closest point and rigid transformation";
	}
	
	/** Assign a parent object. */
	public void setParent(PsUpdateIf parent) {
		super.setParent(parent);
		m_registration = (RigidTransformation) parent;
		
		addSubTitle("Select Surfaces to be Registered");
		
		Panel pGeometries = new Panel();
		pGeometries.setLayout(new GridLayout(1, 2));

		Panel Passive = new Panel();
		Passive.setLayout(new BorderLayout());
		Panel Active = new Panel();
		Active.setLayout(new BorderLayout());
		Label ActiveLabel = new Label("Surface P");
		Active.add(ActiveLabel, BorderLayout.NORTH);
		m_listActive = new PsList(5, true);
		Active.add(m_listActive, BorderLayout.CENTER);
		pGeometries.add(Active);
		Label PassiveLabel = new Label("Surface Q");
		Passive.add(PassiveLabel, BorderLayout.NORTH);
		m_listPassive = new PsList(5, true);
		Passive.add(m_listPassive, BorderLayout.CENTER);
		pGeometries.add(Passive);
		add(pGeometries);
		
		Panel pSetSurfaces = new Panel(new BorderLayout());
		m_bSetSurfaces = new Button("Set selected surfaces");
		m_bSetSurfaces.addActionListener(this);
		pSetSurfaces.add(m_bSetSurfaces, BorderLayout.CENTER);
		add(pSetSurfaces);

		Panel panelBottom = new Panel(new GridLayout(12,1));
        btnTransform = new Button("Transform");
        btnTransform.addActionListener(this);
		btnRandomRotation = new Button("Random rotation of Q");
		btnRandomRotation.addActionListener(this);
        btnRandomTranslation = new Button("Random translation of Q");
        btnRandomTranslation.addActionListener(this);
        btnTestConfig = new Button("Test config");
        btnTestConfig.addActionListener(this);
        btnReset = new Button("Reset");
        btnReset.addActionListener(this);
        lbl = new Label();
        lblMedian = new Label();
        lblConfig = new Label();
        lblNewSize = new Label();
		lblError = new Label();
		lblStep = new Label();
        panelBottom.add(btnTransform);
		panelBottom.add(btnRandomRotation);
        panelBottom.add(btnRandomTranslation);
        panelBottom.add(btnTestConfig);
        panelBottom.add(btnReset);
        panelBottom.add(lbl);
        panelBottom.add(lblConfig);
        panelBottom.add(lblMedian);
        panelBottom.add(lblNewSize);
		panelBottom.add(lblError);
		panelBottom.add(lblStep);
        add(panelBottom);

		updateGeomList();
		validate();
	}

	@Override
	public Dimension getDialogSize() {
		return new Dimension(600, 700);
	}
		
	/** Initialisation */
	public void init() {
		super.init();
		setTitle("Surface Registration");
		
	}

	/** Set the list of geometries in the lists to the current state of the display. */
	public void updateGeomList() {
		Vector displays = m_registration.getGeometry().getDisplayList();
		int numDisplays = displays.size();
		m_geomList = new Vector();
		for (int i=0; i<numDisplays; i++) {
			PvDisplay disp =((PvDisplay)displays.elementAt(i));
			PgGeometryIf[] geomList = disp.getGeometries();
			int numGeom = geomList.length;
			for (int j=0; j<numGeom; j++) {
				if (!m_geomList.contains(geomList[j])) {
					//Take just PgElementSets from the list.
					if (geomList[j].getType() == PvGeometryIf.GEOM_ELEMENT_SET)
						m_geomList.addElement(geomList[j]);
				}
			}
		}
		int nog = m_geomList.size();
		m_listActive.removeAll();
		m_listPassive.removeAll();
		for (int i=0; i<nog; i++) {
			String name = ((PgGeometryIf)m_geomList.elementAt(i)).getName();
			m_listPassive.add(name);
			m_listActive.add(name);
		}
	}
	/**
	 * Handle action events fired by buttons etc.
	 */
	public void actionPerformed(ActionEvent event) {
		Object source = event.getSource();

		if (source == m_bSetSurfaces) {
			m_registration.setGeometries((PgElementSet)m_geomList.elementAt(m_listActive.getSelectedIndex()),
			(PgElementSet)m_geomList.elementAt(m_listPassive.getSelectedIndex()));
		} else if(source == btnTransform) {
		    this.applyRigidTransformation(NR_VERTICES, K, MAX_STEPS, MAX_ERROR);
        } else if(source == btnRandomRotation) {
            m_registration.randomRotationQ();
            m_registration.m_surfQ.update(m_registration.m_surfQ);
        } else if(source == btnRandomTranslation) {
            m_registration.randomTranslationQ(10);
            m_registration.m_surfQ.update(m_registration.m_surfQ);
        } else if(source == btnTestConfig) {
            performKTest();
        } else if(source == btnReset) {
            System.out.println("reset");
            m_registration.reset();
        }
	}

	private void performKTest() {
	    int iterations = 25;

        testConfig(NR_VERTICES, 1, MAX_STEPS, MAX_ERROR, iterations);
        testConfig(NR_VERTICES, 3, MAX_STEPS, MAX_ERROR, iterations);
        testConfig(NR_VERTICES, 5, MAX_STEPS, MAX_ERROR, iterations);
    }

    private void performNTest() {
        int iterations = 25;

        testConfig(100, K, MAX_STEPS, MAX_ERROR, iterations);
        testConfig(200, K, MAX_STEPS, MAX_ERROR, iterations);
        testConfig(300, K, MAX_STEPS, MAX_ERROR, iterations);
    }

    private void performStepsTest() {
		int iterations = 25;

		testConfig(100, K, 1000, MAX_ERROR, iterations);
		testConfig(200, K, 1000, MAX_ERROR, iterations);
		testConfig(300, K, 1000, MAX_ERROR, iterations);
	}

	/**
	 * Runs the applyRigidTransformation for #iterations.
	 * Prints out the configuration that was used, the average error, number of times the maxSteps was reached,
	 * average number of steps.
	 * @param nrVertices The sample size
	 * @param k threshold
	 * @param maxSteps The maximum number of steps while converging
	 * @param maxError The maximum error it should achieve
	 * @param iterations The number of experiments to run
	 */
    private void testConfig(int nrVertices, double k, int maxSteps, double maxError, int iterations) {
        int maxReached = 0;
        int averageSteps = 0;
        double averageError = 0;

        for(int i = 0; i < iterations; i++) {
            PdVector result = applyRigidTransformation(nrVertices, k, maxSteps, maxError);
            averageSteps += result.getEntry(0);
            averageError += result.getEntry(1);
            if(result.getEntry(0) >= maxSteps - 1) maxReached++;
            m_registration.randomRotationQ();
            m_registration.randomTranslationQ(10);
            m_registration.m_surfQ.update(m_registration.m_surfQ);
            m_registration.reset();
            System.out.println(i);
        }

        averageError /= iterations;
        averageSteps /= iterations;

        String formatted = String.format("nrVertices: %d, k:%f, maxSteps:%d, maxError:%f, iterations:%d, maxReached:%d, averageSteps:%d average error:%.4f",
                nrVertices, k, maxSteps, maxError, iterations, maxReached, averageSteps, averageError);

        System.out.println(formatted);
    }

	/**
	 * Tries the find the optimal rigid transformation within maximum steps or until the maximum error was achieved.
	 * @param nrVertices The sample size
	 * @param k threshold
	 * @param maxSteps The maximum number of steps while converging
	 * @param maxError The maximum error it should achieve
	 * @return A vector, first entry gives the number of steps it took, second entry the minimal error.
	 */
	private PdVector applyRigidTransformation(int nrVertices, double k, int maxSteps, double maxError) {
		lblConfig.setText("n:" + nrVertices + " k:" + k);

        double error;
        double lowestError = Double.MAX_VALUE;
        int steps = 0;

        for(; steps < maxSteps; steps++) {
			lblStep.setText("Step: " + steps);
			lbl.setText("Calculating random vertices");
			PdVector[] randomVertices = m_registration.getRandomVertices(nrVertices);

			lbl.setText("Calculating closest vertices");
			PdVector[] closestVertices = m_registration.getClosestVertices(randomVertices);

			lbl.setText("Calculating median");
			double[] distances = m_registration.getAllDistances(randomVertices, closestVertices);
			double median = m_registration.getMedian(distances);
			lblMedian.setText("Median: " + median + " threshold: " + (median * k));

			lbl.setText("Calculating remove list");
			boolean[] listToRemove = m_registration.getRemoveList(distances, median, k);

			lbl.setText("Creating new removed lists");
			PdVector[] pointsP = m_registration.removeVertices(randomVertices, listToRemove);
			PdVector[] pointsQ = m_registration.removeVertices(closestVertices, listToRemove);
			lblNewSize.setText("New size:(" + pointsP.length + "," + pointsQ.length + ")");

			error = m_registration.calculateError(pointsP, pointsQ);
            lowestError = (error < lowestError) ? error : lowestError;
			lblError.setText("Error: " + error);

			if(error < maxError) {
				break;
			}

			lbl.setText("Calculating matrix M");
			PdMatrix M = m_registration.computeM(pointsP, pointsQ);

			lbl.setText("Calculating SVD");
			SingularValueDecomposition svd = new SingularValueDecomposition(new Jama.Matrix(M.getEntries()));

			lbl.setText("Calculating optimal rotation");
			PdMatrix optimalRotation = m_registration.computeOptimalRotation(svd);

			lbl.setText("Calculating optimal translation");
			PdVector optimalTranslation = m_registration.computeOptimalTranslation(pointsP, pointsQ, optimalRotation);

			lbl.setText("Rotating");
			m_registration.rotateMesh(optimalRotation, m_registration.m_surfP);

			lbl.setText("Translating");
			m_registration.translateMesh(optimalTranslation, m_registration.m_surfP);

            lbl.setText("Updating P mesh");
            m_registration.m_surfP.update(m_registration.m_surfP);
		}
		lbl.setText("Done");
        return new PdVector(steps,lowestError);
	}

	/**
	 * Get information which bottom buttons a dialog should create
	 * when showing this info panel.
	 */
	protected int getDialogButtons()		{
		return PsDialog.BUTTON_OK;
	}
}
