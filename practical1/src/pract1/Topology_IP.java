package pract1;

import java.awt.event.ActionListener;

import jvx.project.PjWorkshop_IP;

public class Topology_IP extends PjWorkshop_IP implements ActionListener {

	public Topology_IP() {
		super();
		if(getClass() == Topology_IP.class)
			init();
	}
	
	public void init() {
		super.init();
		setTitle("Topology Algorithms");
	}
}
