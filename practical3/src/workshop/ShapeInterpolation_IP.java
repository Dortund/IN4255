package workshop;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jv.geom.PgElementSet;
import jv.object.PsDebug;
import jv.object.PsDialog;
import jv.object.PsUpdateIf;
import jv.objectGui.PsList;
import jv.project.PgGeometryIf;
import jv.project.PvGeometryIf;
import jv.viewer.PvDisplay;
import jvx.project.PjWorkshop_IP;

public class ShapeInterpolation_IP  extends PjWorkshop_IP implements ActionListener{
	protected	List			m_listActive;
	protected	List			m_listPassive;
	protected	Vector			m_geomList;
	protected workshop.ShapeInterpolation m_interpolation;
	protected   Button			m_bSetSurfaces;

    protected JSlider slider;
    
    protected double time = 0;
    protected PgElementSet looseMesh;
    protected PgElementSet gradientMesh;
    
    protected static final int SLIDER_MAX = 20;
    

	/** Constructor */
	public ShapeInterpolation_IP () {
		super();
		if (getClass() == ShapeInterpolation_IP.class)
			init();
	}

	/**
	 * Informational text on the usage of the dialog.
	 * This notice will be displayed if this info panel is shown in a dialog.
	 * The text is split at line breaks into individual lines on the dialog.
	 */
	public String getNotice() {
		return "Task 2 of assignment 3: Shape Interpolation";
	}
	
	/** Assign a parent object. */
	public void setParent(PsUpdateIf parent) {
		super.setParent(parent);
		m_interpolation = (ShapeInterpolation) parent;
		
		addSubTitle("Select meshes to be interpolated");
		
		Panel pGeometries = new Panel();
		pGeometries.setLayout(new GridLayout(1, 2));

		Panel Passive = new Panel();
		Passive.setLayout(new BorderLayout());
		Panel Active = new Panel();
		Active.setLayout(new BorderLayout());
		Label ActiveLabel = new Label("Origin Mesh");
		Active.add(ActiveLabel, BorderLayout.NORTH);
		m_listActive = new PsList(5, true);
		Active.add(m_listActive, BorderLayout.CENTER);
		pGeometries.add(Active);
		Label PassiveLabel = new Label("Target Mesh");
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

		Panel panelBottom = new Panel(new GridLayout(3,1));
        
        slider = new JSlider(JSlider.HORIZONTAL, 0, SLIDER_MAX, 0);
        slider.setMajorTickSpacing(10);
        slider.setMinorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
        labelTable.put( new Integer( 0 ), new JLabel("0.0") );
        labelTable.put( new Integer( 10 ), new JLabel("0.5") );
        labelTable.put( new Integer( 20 ), new JLabel("1.0") );
        slider.setLabelTable( labelTable );
        slider.addChangeListener(new ChangeListener() {
        	public void stateChanged(ChangeEvent e) {
        		JSlider source = (JSlider)e.getSource();
                if (!source.getValueIsAdjusting()) {
                    double t = (int)source.getValue() / new Double(SLIDER_MAX);
                    time = t;
                    getMeshes();
                } 
        	}
        });
        panelBottom.add(slider);
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
		setTitle("Mesh Interpolation");
		
	}

	/** Set the list of geometries in the lists to the current state of the display. */
	public void updateGeomList() {
		Vector displays = m_interpolation.getGeometry().getDisplayList();
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
			m_interpolation.setGeometries((PgElementSet)m_geomList.elementAt(m_listActive.getSelectedIndex()),
			(PgElementSet)m_geomList.elementAt(m_listPassive.getSelectedIndex()));
			getMeshes();
		}
	}
	
	/**
	 * Get the loose and gradient mesh.
	 * Removes the old meshes and adds the new
	 */
	private void getMeshes() {
		if (looseMesh != null) {
			removeMesh(looseMesh);
		}
		if (gradientMesh != null) {
			removeMesh(gradientMesh);
		}
		looseMesh = m_interpolation.getInterpolatedset(time);
		looseMesh.setVisible(false);
		looseMesh.setName("LooseMesh");
        addMesh(looseMesh);
		gradientMesh = m_interpolation.getGradientInterpolated(looseMesh);
		gradientMesh.setVisible(true);
		gradientMesh.setName("GradientMesh");
		addMesh(gradientMesh);
	}
	
	/**
	 * Removes a geometry from all displays
	 * @param mesh The geometry to be removed
	 */
	private void removeMesh(PgElementSet mesh) {
		Vector displays = m_interpolation.getGeometry().getDisplayList();
		int numDisplays = displays.size();
		for (int i=0; i<numDisplays; i++) {
			PvDisplay disp =((PvDisplay)displays.elementAt(i));
			disp.removeGeometry(mesh);
			mesh.update(mesh);
		}
	}
	
	/**
	 * Adds a geometry to all displays
	 * @param mesh The geometry to be added
	 */
	private void addMesh(PgElementSet mesh) {
		Vector displays = m_interpolation.getGeometry().getDisplayList();
		int numDisplays = displays.size();
		for (int i=0; i<numDisplays; i++) {
			PvDisplay disp =((PvDisplay)displays.elementAt(i));
			disp.addGeometry(mesh);
			mesh.update(mesh);
		}
	}

	/**
	 * Get information which bottom buttons a dialog should create
	 * when showing this info panel.
	 */
	protected int getDialogButtons()		{
		return PsDialog.BUTTON_OK;
	}
}