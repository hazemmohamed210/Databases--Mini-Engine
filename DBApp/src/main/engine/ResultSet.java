package main.engine;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

public class ResultSet implements Iterator<Hashtable<String,Object>> {
	Vector<Hashtable<String,Object>> results;
	int iterationIndex;
	
	public ResultSet() {
		results = new Vector<>();
		this.iterationIndex = 0;
	}
	
	public void addToResults(OcTreeEntry in, Table t) {
//		System.out.println("hey");
		int row = in.rowNum;
		if (row < 0) row = (row + 1) * -1;
		this.results.add(t.deSerPage(in.pageNum).getData().get(row));
//		System.out.println(results.size());
	}
	
	public void addToResults(Hashtable<String,Object> in) {
		this.results.add(in);
	}
	
	public void addToResults(Vector<Hashtable<String,Object>> rows) {
		this.results.addAll(rows);
	}

	public boolean hasNext() {
		if(this.iterationIndex == results.size()) {
			this.iterationIndex = 0;
			return false;
		} 
			
		return true;
	}

	public Hashtable<String,Object> next() {
		Hashtable<String, Object> res = results.get(iterationIndex);
		this.iterationIndex++;
		return res;
	}

}
