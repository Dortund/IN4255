package workshop;

import jv.object.PsDebug;
import jv.object.PsDialog;
import jv.object.PsUpdateIf;
import jvx.project.PjWorkshop_IP;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Vector;

public class SurfaceSmoothing_IP  extends PjWorkshop_IP implements ActionListener{
	protected	List			m_listActive;
	protected	List			m_listPassive;
	protected	Vector			m_geomList;
	protected workshop.SurfaceSmoothing surfaceSmoothing;

	protected JFormattedTextField fieldStepsize;
	protected JFormattedTextField fieldNumSteps;
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
			
			Panel panel = new Panel(new GridLayout(8, 1));
			
			NumberFormat format = NumberFormat.getNumberInstance();
			
			Label labelStepsize = new Label("Set stepsize for smoothing");
			panel.add(labelStepsize);
			fieldStepsize = new JFormattedTextField(format);
			fieldStepsize.setValue(0.5);
			panel.add(fieldStepsize);
			
			Label labelNumSteps = new Label("Set number of steps");
			panel.add(labelNumSteps);
			fieldNumSteps = new JFormattedTextField(format);
			fieldNumSteps.setValue(1);
			panel.add(fieldNumSteps);
			
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
		return new Dimension(300, 350);
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
		try{
			Object source = event.getSource();
			if (source == btnReset){
				surfaceSmoothing.reset();
			} else {
				double stepsize = Double.parseDouble(fieldStepsize.getText());
				int numSteps = Integer.parseInt(fieldNumSteps.getText());
	//			PsDebug.message("Stepsize: " + stepsize);
				for (int i = 0; i < numSteps; i++){
					if(source == btnIterative){
						surfaceSmoothing.iterative(stepsize);
					} else if (source == btnExplicit){
						surfaceSmoothing.explicit(stepsize);
					} else if (source == btnImplicit){
						surfaceSmoothing.implicit(stepsize);
					}
				}
			}
		} catch(Exception E){
			StackTraceElement[] stacktrace = E.getStackTrace();
			for (StackTraceElement elem : stacktrace)
				PsDebug.message(elem.toString());
			PsDebug.warning(E.toString());
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