package edu.washington.cs.oneswarm.f2f.permissions;

import java.util.List;

public class SwarmBean
{
	String hexHash;
	List<String> permitted_groups;
	public String getHexHash() {
		return hexHash;
	}
	public void setHexHash(String hexHash) {
		this.hexHash = hexHash;
	}
	public List<String> getPermitted_groups() {
		return permitted_groups;
	}
	public void setPermitted_groups(List<String> permitted_groups) {
		this.permitted_groups = permitted_groups;
	}
}
