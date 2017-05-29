package workshop;

import jv.object.PsDebug;
import jv.object.PsDialog;
import jv.object.PsUpdateIf;
import jv.vecmath.PdMatrix;
import jv.vecmath.PdVector;
import jvx.project.PjWorkshop_IP;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ShapeDeformation_IP extends PjWorkshop_IP implements ActionListener {
    protected ShapeDeformation shapeDeformation;

    protected Button btnCalculate;

    public ShapeDeformation_IP () {
        super();
        if (getClass() == ShapeDeformation_IP.class)
            init();
    }

    public String getNotice() {
        return "Calculates the gradients linear polynomial and edits the mesh according";
    }

    public void setParent(PsUpdateIf parent) {
        super.setParent(parent);

        shapeDeformation = (ShapeDeformation) parent;

        btnCalculate = new Button("Calculate");
        btnCalculate.addActionListener(this);

        Panel panel = new Panel(new GridLayout(5, 1));
        panel.add(btnCalculate);

        this.add(panel);

        validate();
    }

    public void init() {
        super.init();
        setTitle("Shape deformation");

    }

    public void actionPerformed(ActionEvent event) {
        Object source = event.getSource();
        if(source == btnCalculate) {
            testTriangleToGradient();
        }
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