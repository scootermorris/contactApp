package edu.ucsf.rbvi.contactApp.internal.model;

import java.util.ArrayList;
import java.util.List;

class Pathway {
	private final String pathwayName;
	private final List<Integer> residues;

	public Pathway (String name) {
		residues = new ArrayList<Integer>();
		pathwayName = name;
	}

	public List<Integer> getResidues() {
		return residues;
	}

	public void addResidue(int residueNumber) {
		residues.add(residueNumber);
	}

	public String getName() {
		return pathwayName;
	}
}
