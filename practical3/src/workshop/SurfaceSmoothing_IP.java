package workshop;

import java.awt.Button;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.List;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Vector;

import javax.swing.JFormattedTextField;

import jv.object.PsDebug;
import jv.object.PsDialog;
import jv.object.PsUpdateIf;
import jvx.project.PjWorkshop_IP;

public class SurfaceSmoothing_IP  extends PjWorkshop_IP implements ActionListener{
	protected	List			m_listActive;
	protected	List			m_listPassive;
	protected	Vector			m_geomList;
	protected workshop.SurfaceSmoothing surfaceSmoothing;

	protected JFormattedTextField fieldStepsize;
    protected Button btnIterative;
	protected Button btnExplicit;
	protected Button btnImplicit;
    protected Button btnReset;

	/** Constructor */
	public SurfaceSmoothing_IP () {
		super();
		if (getClass() == SurfaceSmoothing_IP.class)
			init();
	}

	/**
	 * Informational text on the usage of the dialog.
	 * This notice will be displayed if this info panel is shown in a dialog.
	 * The text is split at line breaks into individual lines on the dialog.
	 */
	public String getNotice() {
		return "Task 1 of assignment 3: Surface Smoothing";
	}
	
	/** Assign a parent object. */
	public void setParent(PsUpdateIf parent) {
		try {
			super.setParent(parent);
			surfaceSmoothing = (SurfaceSmoothing) parent;
			
			addSubTitle("Set stepsize for smoothing");
			
			Panel panel = new Panel(new GridLayout(5, 1));
			
			NumberFormat format = NumberFormat.getNumberInstance();
			fieldStepsize = new JFormattedTextField(format);
			panel.add(fieldStepsize);
			
			btnIterative = new Button("Iterative Smoothing");
			btnIterative.addActionListener(this);
			panel.add(btnIterative);
			
			btnExplicit = new Button("Explicit Mean Curvature Flow Integration");
			btnExplicit.addActionListener(this);
			panel.add(btnExplicit);
			
			btnImplicit = new Button("Implicit Mean Curvature Flow Integration");
			btnImplicit.addActionListener(this);
			panel.add(btnImplicit);
			
			btnReset = new Button("Reset");
	        btnReset.addActionListener(this);
	        panel.add(btnReset);
	        
	        this.add(panel);
			
			validate();
		} catch(Exception E){
			StackTraceElement[] stacktrace = E.getStackTrace();
			for (StackTraceElement elem : stacktrace)
				PsDebug.message(elem.toString());
			PsDebug.warning(E.toString());
		}
	}

	@Override
	public Dimension getDialogSize() {
		return new Dimension(600, 700);
	}
		
	/** Initialisation */
	public void init() {
		super.init();
		setTitle("Surface Smoothing");
		
	}

	/**
	 * Handle action events fired by buttons etc.
	 */
	public void actionPerformed(ActionEvent event) {
		Object source = event.getSource();
		if (source == btnReset){
			surfaceSmoothing.reset();
		} else {
			double stepsize = Double.parseDouble(fieldStepsize.getText());
			if(source == btnIterative){
				surfaceSmoothing.iterative(stepsize);
			} else if (source == btnExplicit){
				surfaceSmoothing.explicit(stepsize);
			} else if (source == btnImplicit){
				surfaceSmoothing.implicit(stepsize);
			}
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