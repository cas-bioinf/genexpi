package cz.cas.mbu.genexpi.compute;

import java.util.List;

public class GeneProfile<NUMBER_TYPE extends Number> {
	private String name; 
	private List<NUMBER_TYPE> profile;
	
	public GeneProfile(String name, List<NUMBER_TYPE> profile) {
		super();
		this.name = name;
		this.profile = profile;
	}

	public String getName() {
		return name;
	}

	public List<NUMBER_TYPE> getProfile() {
		return profile;
	}
	
	
}
