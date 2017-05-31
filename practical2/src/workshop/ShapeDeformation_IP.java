package workshop;

import java.awt.Button;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

import javax.swing.JFormattedTextField;

import jv.object.PsDebug;
import jv.object.PsDialog;
import jv.object.PsUpdateIf;
import jv.vecmath.PdMatrix;
import jv.vecmath.PdVector;
import jvx.project.PjWorkshop_IP;

public class ShapeDeformation_IP extends PjWorkshop_IP implements ActionListener {
    protected ShapeDeformation shapeDeformation;

    //protected Button btnCalculate;
    
    protected JFormattedTextField[][] matrixInputs;
    
    protected Button btnDeform;
    protected Button btnReset;

    public ShapeDeformation_IP () {
        super();
        if (getClass() == ShapeDeformation_IP.class)
            init();
    }

    public String getNotice() {
        return "Calculates the gradients linear polynomial and edits the mesh according";
    }

    public void setParent(PsUpdateIf parent) {
    	try {
        super.setParent(parent);

        shapeDeformation = (ShapeDeformation) parent;

        Panel panel = new Panel(new GridLayout(3, 1));
        Panel matrixGrid = new Panel(new GridLayout(3,3));
        NumberFormat format = NumberFormat.getNumberInstance();
        matrixInputs = new JFormattedTextField[3][3];
        for (int row = 0; row < matrixInputs.length; row++) {
        	for (int column = 0; column < matrixInputs[row].length; column++) {
            	matrixInputs[row][column] = new JFormattedTextField(format);
            	matrixInputs[row][column].setText(row == column ? 1+"" : 0+"");
            	matrixGrid.add(matrixInputs[row][column]);
            }
        }
        panel.add(matrixGrid);
        
        btnDeform = new Button("Deform");
        btnDeform.addActionListener(this);
        panel.add(btnDeform);
        
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

    public void init() {
        super.init();
        setTitle("Shape deformation");
    }

    public void actionPerformed(ActionEvent event) {
        Object source = event.getSource();
        if (source == btnDeform) {
        	deformSelected();
        } else if (source == btnReset) {
        	shapeDeformation.reset();
        }
    }
    
    private void deformSelected() {
    	PdMatrix deform = new PdMatrix(3, 3);
    	for (int row = 0; row < matrixInputs.length; row++) {
        	for (int column = 0; column < matrixInputs[row].length; column++) {
        		double val = Double.parseDouble(matrixInputs[row][column].getText());
        		deform.setEntry(row, column, val);
        	}
    	}
    	PsDebug.message("Deform: " + deform);
    	shapeDeformation.deformSelected(deform);
    }

    private void testTriangleToGradient() {
        PdVector[] triangle1 = new PdVector[]{new PdVector(0.0,0.0,0.0), new PdVector(1.0,1.0,0.0), new PdVector(0.0,1.0,1.0)};
        double[][] expected = {{-1.0, 2.0, -1.0}, {-2.0, 1.0, 1.0}, {-1.0, -1.0, 2.0}};
        PdMatrix expectedMatrix = new PdMatrix(expected);
        expectedMatrix.multScalar(1.0/3.0);

        PsDebug.message("Expect: " + expectedMatrix.toShortString());
        PsDebug.message("Got: " + shapeDeformation.triangleToGradient(triangle1));

        PdVector[] triangle2 = new PdVector[]{
                new PdVector(-0.523035,0.4749694,0.436263),
                new PdVector(0.528191,0.492968,0.448928),
                new PdVector(-0.714874,1.3084,-0.42234)};
        expected = new double[][]{
                {-0.947366, 0.950318, -0.00295215},
                {-0.70442, 0.120702, 0.583718},
                {-0.692358, -0.0951279, -0.59723}};
        expectedMatrix = new PdMatrix(expected);

        PsDebug.message("Expect: " + expectedMatrix.toShortString());
        PsDebug.message("Got: " + shapeDeformation.triangleToGradient(triangle2));
    }

    protected int getDialogButtons()		{
        return PsDialog.BUTTON_OK;
    }
}