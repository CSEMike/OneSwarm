/**
 * 
 */
package edu.washington.cs.oneswarm.f2f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class FileList
{
	private int									listId = -1;

	private long								 created;

	private List<FileCollection> elements;

	public FileList() {
		this.created = System.currentTimeMillis();
		this.elements = new LinkedList<FileCollection>();
		calcListId();
	}

	public FileList(List<FileCollection> _elements) {
		this.created = System.currentTimeMillis();
		this.elements = _elements;
		calcListId();

	}

	private void calcListId() {

		for (FileCollection e : elements) {
			listId = listId ^ e.hashCode();
		}

	}

	public int getListId() {
		return listId;
	}

	public void setListId(int listId) {
		this.listId = listId;
	}

	public long getCreated() {
		return created;
	}

	public void setCreated(long created) {
		this.created = created;
	}

	public List<FileCollection> getElements() {
		return elements;
	}

	public void setElements(List<FileCollection> elements) {
		this.elements = elements;
		calcListId();
	}

	public FileList searchMatches(String searchString) {
		List<FileCollection> searchMatches = new ArrayList<FileCollection>();

		for (FileCollection fileCollection : elements) {
			FileCollection collectionMatch = fileCollection.searchMatches(searchString);
			if (collectionMatch != null) {
				searchMatches.add(collectionMatch);
			}
		}

		if (searchMatches.size() > 0) {
			return new FileList(searchMatches);
		}

		return new FileList(new ArrayList<FileCollection>());
	}

	public boolean contains(byte[] hash) {
		for (FileCollection c : elements) {
			if (Arrays.equals(c.getUniqueIdBytes(), hash)) {
				return true;
			}
		}
		return false;
	}

	public boolean contains(String base64Hash) {
		for (FileCollection c : elements) {
			if (c.getUniqueID().equals(base64Hash)) {
				return true;
			}
		}
		return false;
	}

	public long getFileNum() {
		long sum = 0;
		for (FileCollection fc : elements) {
			sum += fc.getFileNum();
		}

		return sum;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		for( int i=0; i<getElements().size(); i++ ) {
			FileCollection coll = getElements().get(i);
			sb.append(coll.getName() + " tags: " + coll.getDirectoryTags().size() );
			for( List<String> tag : coll.getDirectoryTags() ) {
				for( String entry : tag ) {
					sb.append(entry + "/");
				}
				sb.append( " " );
			}
			if( i < getFileNum()-1 ) {
				sb.append("\n");
			}
		}
		
		return sb.toString();
	}
}
