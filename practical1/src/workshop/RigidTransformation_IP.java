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
	protected 	Button			btnTransform;

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

		Panel panelBottom = new Panel(new GridLayout(10,1));
        btnTransform = new Button("Transform");
        btnTransform.addActionListener(this);
        lbl = new Label();
        lblMedian = new Label();
        lblConfig = new Label();
        lblNewSize = new Label();
		lblError = new Label();
		lblStep = new Label();
        panelBottom.add(btnTransform);
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
		return new Dimension(500, 500);
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
			return;
		} else if(source == btnTransform) {
		    this.applyRigidTransformation();
        }
	}

	private void applyRigidTransformation() {
		int nrVertices = 100;
		int k = 2;
		int maxSteps = 500;
		double maxError = 0.01;

		lblConfig.setText("n:" + nrVertices + " k:" + k);

		for(int steps = 0; steps < maxSteps; steps++) {
			lblStep.setText("Step: " + steps);
			lbl.setText("Calculating random vertices");
			PdVector[] randomVertices = m_registration.getRandomVertices(nrVertices);

			lbl.setText("Calculating closest vertices");
			PdVector[] closestVertices = m_registration.getClosestVertices(randomVertices);

			lbl.setText("Calculating median");
			double[] distances = m_registration.getAllDistances(randomVertices, closestVertices);
			double median = m_registration.getMedian(distances);
			lblMedian.setText("Median: " + median + " threshold: " + (k * median));

			lbl.setText("Calculating remove list");
			boolean[] listToRemove = m_registration.getRemoveList(distances, median, k);

			lbl.setText("Creating new removed lists");
			PdVector[] pointsP = m_registration.removeVertices(randomVertices, listToRemove);
			PdVector[] pointsQ = m_registration.removeVertices(closestVertices, listToRemove);
			lblNewSize.setText("New size:(" + pointsP.length + "," + pointsQ.length + ")");

			double error = m_registration.calculateError(pointsP, pointsQ);
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
			m_registration.rotatePMesh(optimalRotation);

			lbl.setText("Translating");
			m_registration.translatePMesh(optimalTranslation);

			lbl.setText("Updating P mesh");
			m_registration.m_surfP.update(m_registration.m_surfP);
		}
		lbl.setText("Done");
	}

	/**
	 * Get information which bottom buttons a dialog should create
	 * when showing this info panel.
	 */
	protected int getDialogButtons()		{
		return PsDialog.BUTTON_OK;
	}
}
