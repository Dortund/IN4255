package workshop;

import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import jv.object.PsDialog;
import jv.object.PsUpdateIf;
import jvx.project.PjWorkshop_IP;

public class Topology_IP extends PjWorkshop_IP implements ActionListener {

	protected Button btnGenus;
	protected Button btnVolume;
	protected Button btnComponents;
	protected Button btnLoops;
	
	protected Label lblGenus;
	protected Label lblVolume;
	protected Label lblComponents;
	protected Label lblLoops;
	
	Topology m_ws;
	
	public Topology_IP() {
		super();
		if(getClass() == Topology_IP.class)
			init();
	}
	
	public void init() {
		super.init();
		setTitle("Topology Algorithms");
	}
	
	public void setParent(PsUpdateIf parent) {
		super.setParent(parent);
		m_ws = (Topology)parent;
	
		addSubTitle("Part 1");
		
		btnGenus = new Button("Calculate Genus");
		btnGenus.addActionListener(this);
		btnVolume = new Button("Calculate Enclosed Volume");
		btnVolume.addActionListener(this);
		btnComponents = new Button("Calculate Connected Components");
		btnComponents.addActionListener(this);
		btnLoops = new Button("Calculate Boundary Loops");
		btnLoops.addActionListener(this);
		
		lblGenus = new Label();
		lblVolume = new Label();
		lblComponents = new Label();
		lblLoops = new Label();
		
		Panel panel1 = new Panel(new GridLayout(4, 2));
		panel1.add(btnGenus);
		panel1.add(lblGenus);
		panel1.add(btnVolume);
		panel1.add(lblVolume);
		panel1.add(btnComponents);
		panel1.add(lblComponents);
		panel1.add(btnLoops);
		panel1.add(lblLoops);
		add(panel1);
		
		validate();
	}
	
	/**
	 * Handle action events fired by buttons etc.
	 */
	public void actionPerformed(ActionEvent event) {
		Object source = event.getSource();
		if (source == btnGenus) {
			lblGenus.setText("...");
			lblGenus.setText(m_ws.calculateGenus() + "");
			m_ws.m_geom.update(m_ws.m_geom);
			return;
		} else if (source == btnVolume) {
			lblVolume.setText("...");
			lblVolume.setText(m_ws.calculateVolume() + "");
			m_ws.m_geom.update(m_ws.m_geom);
			return;
		} else if (source == btnComponents) {
			lblComponents.setText("...");
			lblComponents.setText(m_ws.calculateComponents() + "");
			m_ws.m_geom.update(m_ws.m_geom);
			return;
		} else if (source == btnLoops) {
//			lblLoops.setText("pre");
			lblLoops.setText(m_ws.calculateLoops() + "");
//			lblLoops.setText("post");
			m_ws.m_geom.update(m_ws.m_geom);
			return;
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
