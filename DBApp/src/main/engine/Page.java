package main.engine;

import java.util.Hashtable;
import java.util.Vector;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.io.FileInputStream;
//import java.io.FileNotFoundException;
import java.io.IOException;

@SuppressWarnings("serial")
public class Page implements Serializable {
	private Vector<Hashtable<String,Object>> data;
	private int pageNumber;
	private int maxSize;


	
	public Page(int pageNumber) throws DBAppException {
		this.data = new Vector<Hashtable<String,Object>>();
		this.pageNumber = pageNumber;
		
		try {
			FileInputStream in = new FileInputStream("src/main/resources/DBApp.properties");
			
			Properties prop = new Properties();
			prop.load(in);
			
			String sizeProp = prop.getProperty("MaximumRowsCountinTablePage");
			this.maxSize = Integer.parseInt(sizeProp);
//			System.out.println(maxSize);
		} catch (IOException e) {
			throw new DBAppException("error with config file");
		}
	}
	

	public int getMaxSize() {
		return maxSize;
	}


	public int getPageNumber() {
		return pageNumber;
	}

	public void setPageNumber(int pageNumber) {
		this.pageNumber = pageNumber;
	}

	public Vector<Hashtable<String, Object>> getData() {
		return data;
	}

	public void setData(Vector<Hashtable<String, Object>> data) {
		this.data = data;
	}
	
	public void addToPage(Hashtable<String, Object> htblColNameValue, String strClusteringKeyColumn) {
		try {
			String in = "";
			if((htblColNameValue.get(strClusteringKeyColumn).getClass()+"").split(" ")[1].equals("java.util.Date")) {
				in = (new SimpleDateFormat("yyyy-MM-dd")).format(htblColNameValue.get(strClusteringKeyColumn));
//				System.out.println(strClusteringKeyColumn);
			} else in = htblColNameValue.get(strClusteringKeyColumn)+"";
			int insertionPoint = this.binarySearch(in, strClusteringKeyColumn, 0, this.getSize()-1);
			if(insertionPoint >= 0) {
				this.getData().add(insertionPoint, htblColNameValue);
			} else {
				this.getData().add((insertionPoint + 1) * -1, htblColNameValue);
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	public void deleteFromPage(Hashtable<String, Object> tuple) {
		this.data.remove(tuple);
	}
	
	public int getSize() {
		return this.data.size();
	}
	
	public boolean isFull() {
		if(this.getSize() == this.maxSize) return true;
		return false;
	}
	
	public void handleShifts(Table t, int start) throws DBAppException {
		
		if (this.getSize() > this.maxSize) {
//			System.out.println(this.getData());
			// check if the full page is the last page
			if(this.getPageNumber() == t.getPagesCount()) {
				t.addPage();
//				System.out.println("new page created");
				Page pNew = t.deSerPage(t.getPagesCount());
//				System.out.println(this.getData());
//				System.out.println("last is: "+this.getData().get(this.getSize() - 1));
				pNew.addToPage(this.getData().remove(this.getSize() - 1), t.getStrClusteringKeyColumn());
				t.serPage(pNew);
				for(String indexName : t.getIndicesNames()) {
					int rowNum = this.getSize() - 1;
					Octree2 o = t.deSerIndex(indexName);
					for(int i = start; i<this.getSize(); i++) {
						System.out.println("start is: "+start+" "+this.getData());
						Object[] tupl2 = new Object[3];
						tupl2[0] = this.getData().get(i).get(o.getColNames()[0]);
						tupl2[1] = this.getData().get(i).get(o.getColNames()[1]);
						tupl2[2] = this.getData().get(i).get(o.getColNames()[2]);
						OcTreeEntry in = new OcTreeEntry(tupl2, this.getPageNumber(), i-1);
						OcTreeEntry inNew = new OcTreeEntry(tupl2, this.getPageNumber(), i);
						System.out.println(in);
						System.out.println(inNew);
						o.update(in, inNew);
					}
					Object[] tupl = new Object[3];
					tupl[0] = pNew.getData().get(0).get(o.getColNames()[0]);
					tupl[1] = pNew.getData().get(0).get(o.getColNames()[1]);
					tupl[2] = pNew.getData().get(0).get(o.getColNames()[2]);
					OcTreeEntry en = new OcTreeEntry(tupl, this.getPageNumber(), rowNum);
//					System.out.println(en);
					OcTreeEntry enNew = new OcTreeEntry(tupl, this.getPageNumber()+1, 0);
//					System.out.println(o.printOcTree(1));
					o.update(en, enNew);
					t.serIndex(o);
					o = null;
					System.gc();
				}
				t.serPage(this);
				pNew = null;
				System.gc();
				return;
			} else { // the full page is not the last page
				Page pNext = t.deSerPage(this.getPageNumber() + 1);
				pNext.addToPage(this.getData().remove(this.getSize() - 1), t.getStrClusteringKeyColumn());
				for(String indexName : t.getIndicesNames()) {
					Octree2 o = t.deSerIndex(indexName);
					Object[] tupl = new Object[3];
					for(int i = start; i<this.getSize(); i++) {
						Object[] tupl2 = new Object[3];
						tupl2[0] = this.getData().get(i).get(o.getColNames()[0]);
						tupl2[1] = this.getData().get(i).get(o.getColNames()[1]);
						tupl2[2] = this.getData().get(i).get(o.getColNames()[2]);
						OcTreeEntry in = new OcTreeEntry(tupl2, this.getPageNumber(), i-1);
						OcTreeEntry inNew = new OcTreeEntry(tupl2, this.getPageNumber(), i);
						System.out.println(in);
						System.out.println(inNew);
						o.update(in, inNew);
					}
					int rowNum = this.getSize() - 1;
					tupl[0] = pNext.getData().get(0).get(o.getColNames()[0]);
					tupl[1] = pNext.getData().get(0).get(o.getColNames()[1]);
					tupl[2] = pNext.getData().get(0).get(o.getColNames()[2]);
					OcTreeEntry en = new OcTreeEntry(tupl, this.getPageNumber(), rowNum);
					OcTreeEntry enNew = new OcTreeEntry(tupl, this.getPageNumber()+1, 0);
					o.update(en, enNew);
					for(int i = 1; i<pNext.getSize(); i++) {
						tupl[0] = this.getData().get(i).get(o.getColNames()[0]);
						tupl[1] = this.getData().get(i).get(o.getColNames()[1]);
						tupl[2] = this.getData().get(i).get(o.getColNames()[2]);
						OcTreeEntry in = new OcTreeEntry(tupl, this.getPageNumber(), rowNum);
						OcTreeEntry inNew = new OcTreeEntry(tupl, this.getPageNumber(), i);
						o.update(in, inNew);
					}
					t.serIndex(o);
					o = null;
					System.gc();
				}
				t.serPage(this);
				pNext.handleShifts(t, 0);
//				System.out.println("shift handled");
//				t.serPage(pNext);
			}
		} else {
			t.serPage(this);
			return;
		}
	}
	
	public int compareKeys(String strClusteringKeyValue, String type, Object key) throws ParseException {
		
		if(type.equals("java.lang.Integer")) {
			return (Integer.compare(Integer.parseInt(strClusteringKeyValue), (int) key));
			
		} else if(type.equals("java.lang.String")) {
			return (strClusteringKeyValue.compareTo(((String) (key))));
			
		} else if(type.equals("java.lang.Double")) {
			return Double.compare(Double.parseDouble(strClusteringKeyValue), (double) key );
		} else {
//			System.out.println(strClusteringKeyValue+"   "+key);
			return (new SimpleDateFormat("yyyy-MM-dd").parse(strClusteringKeyValue)).compareTo((java.util.Date) key);
		}
	}
	
	public int binarySearch( String key, String pk, int low, int high ) throws ParseException{
		if(low > high) {
			return -low - 1;
		}
		int mid = (low + high) / 2;
		Object o = this.getData().get(mid).get(pk);
		String type = o.getClass().getTypeName();
		if (this.compareKeys(key, type, o)  == 0)
		    return mid;
		if(this.compareKeys(key, type, this.getData().get(mid).get(pk)) < 0) 
			return binarySearch(key, pk, low, mid-1);
		else 
			return binarySearch(key, pk, mid+1, high);
	}

	
	
}
