package main.engine;

import java.io.Serializable;
//import java.util.Hashtable;
import java.util.Objects;

@SuppressWarnings("serial")
public class OcTreeEntry implements Serializable {
	Object[] tuple;
	int pageNum;
	int rowNum;
	
	public OcTreeEntry(Object[] entry, int pageNum, int rowNum) {
		super();
		this.tuple = entry;
		this.pageNum = pageNum;
		this.rowNum = rowNum;
	}
	
	public static boolean isTuplesEqual(Object[] ol, Object[] ol2) {
		boolean arraysEqual = true;
		for (int i = 0; i < ol.length; i++) {
		    if (!Objects.equals(ol[i], ol2[i])) {
		        arraysEqual = false;
		        break;
		    }
		}
		return arraysEqual;
	}
	
	public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof OcTreeEntry)) {
            return false;
        }

        OcTreeEntry other = (OcTreeEntry) obj;

        // Check if all attributes are equal
        return isTuplesEqual(this.tuple, other.tuple)
                && pageNum == other.pageNum
                && rowNum == other.rowNum;
    }

	public Object[] getTuple() {
		return tuple;
	}

	public void setTuple(Object[] entry) {
		this.tuple = entry;
	}

	public int getPageNum() {
		return pageNum;
	}

	public void setPageNum(int pageNum) {
		this.pageNum = pageNum;
	}

	public int getRowNum() {
		return rowNum;
	}

	public void setRowNum(int rowNum) {
		this.rowNum = rowNum;
	}
	
	public String toString() {
		return "( ["+tuple[0]+", "+tuple[1]+", "+tuple[2]+ "] ; pageNum = "+pageNum+"; rowNum = "+rowNum+" )";
	}
	
	public static void main(String[] args) {

	}
	
}
