package main.engine;



import java.util.ArrayList;
import java.util.Hashtable;
import java.io.IOException;
import java.io.Serializable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

@SuppressWarnings("serial")
public class Table implements Serializable {
	private String strTableName; 
	private String strClusteringKeyColumn; 
	private Hashtable<String,String> htblColNameType; 
	private Hashtable<String,String> htblColNameMin; 
	private Hashtable<String,String> htblColNameMax;
	private int pagesCount;
	private ArrayList<String> indicesNames;

	public Table(String strTableName, String strClusteringKeyColumn, Hashtable<String, String> htblColNameType,
			Hashtable<String, String> htblColNameMin, Hashtable<String, String> htblColNameMax) {
		super();
		this.strTableName = strTableName;
		this.strClusteringKeyColumn = strClusteringKeyColumn;
		this.htblColNameType = htblColNameType;
		this.htblColNameMin = htblColNameMin;
		this.htblColNameMax = htblColNameMax;
		this.pagesCount = 0;
		this.indicesNames = new ArrayList<>();
	}
	
	public Octree2 deSerIndex(String indexName) throws DBAppException {
		Octree2 index = null;
		try {
	       FileInputStream fileIn = new FileInputStream("src/main/resources/data/" + indexName + ".ser");
	       ObjectInputStream in = new ObjectInputStream(fileIn);
	       index = (Octree2) in.readObject();
	       in.close();
	       fileIn.close();
	    } catch (IOException i) {
	    	throw new DBAppException("error: "+i.getMessage());
	    } catch (ClassNotFoundException c) {
	    	throw new DBAppException("error: "+c.getMessage());
	    }
		
		return index;
	}
	
	public void serIndex(Octree2 index) throws DBAppException {
		try {
	       FileOutputStream fileOut = new FileOutputStream("src/main/resources/data/" +index.getIndexName() + ".ser");
	       ObjectOutputStream out = new ObjectOutputStream(fileOut);
	       out.writeObject(index);
	       out.close();
	       fileOut.close();
//		   System.out.println("table serialized");
	    } catch (IOException i) {
	    	throw new DBAppException("error: "+i.getMessage());
	    }
//		index = null;
//		System.gc();
	}
	
	public void serPage(Page p) {
		try {
	       FileOutputStream fileOut = new FileOutputStream("src/main/resources/data/" + this.strTableName + "_page_" + (p.getPageNumber()) + ".ser");
	       ObjectOutputStream out = new ObjectOutputStream(fileOut);
	       out.writeObject(p);
	       out.close();
	       fileOut.close();
//	       System.out.println("new page serialized");
	    } catch (IOException i) {
	       i.printStackTrace();
	    }
	}
	
	public Page deSerPage(int pageNumber) {
		Page p = null;
		try {
	         FileInputStream fileIn = new FileInputStream("src/main/resources/data/" + this.strTableName + "_page_" + (pageNumber) + ".ser");
	         ObjectInputStream in = new ObjectInputStream(fileIn);
	         p = (Page) in.readObject();
	         in.close();
	         fileIn.close();
	      } catch (IOException i) {
	         i.printStackTrace();
	         return p;
	      } catch (ClassNotFoundException c) {
	         c.printStackTrace();
	         return p;
	      }
		
		return p;
	}
	
	public void addPage() {
		this.pagesCount++;
		Page p;
		try {
			p = new Page(this.pagesCount);
			//serialize the page as .ser file
			this.serPage(p);
			p = null;
			System.gc();
		} catch (DBAppException e) {
			e.printStackTrace();
		}
		
	}
	
	public void deletePage(int pageNumber) {
		File f = new File("src/main/resources/data/" + this.strTableName + "_page_" + (pageNumber) + ".ser");
//		String name = f.getName();
		f.delete();
//		this.pagesCount--;
	}
	
	public void deleteFromPage(Hashtable<String, Object> tuple, int pageNumber) {
		Page p = null;
		
		try {
	         FileInputStream fileIn = new FileInputStream("src/main/resources/data/" + this.strTableName + "_page_" + (pageNumber) + ".ser");
	         ObjectInputStream in = new ObjectInputStream(fileIn);
	         p = (Page) in.readObject();
	         in.close();
	         fileIn.close();
	      } catch (IOException i) {
	         i.printStackTrace();
	         return;
	      } catch (ClassNotFoundException c) {
	         c.printStackTrace();
	         return;
	      }
	      
	      p.deleteFromPage(tuple);
	      
	      if(p.getSize() == 0) {
	    	  this.deletePage(p.getPageNumber());
	      }
	      else {
	    	  try {
	    		  FileOutputStream fileOut = new FileOutputStream("src/main/resources/data/" + this.strTableName + "_page_" + (this.pagesCount) + ".ser");
	    		  ObjectOutputStream out = new ObjectOutputStream(fileOut);
	    		  out.writeObject(p);
	    		  out.close();
	    		  fileOut.close();
//	    		  System.out.println("Serialized");
	    	  } catch (IOException i) {
	    		  i.printStackTrace();
	    	  }
	      }
	      
	}
	
	
	public String getStrTableName() {
		return strTableName;
	}

	public void setStrTableName(String strTableName) {
		this.strTableName = strTableName;
	}

	public String getStrClusteringKeyColumn() {
		return strClusteringKeyColumn;
	}

	public void setStrClusteringKeyColumn(String strClusteringKeyColumn) {
		this.strClusteringKeyColumn = strClusteringKeyColumn;
	}

	public Hashtable<String, String> getHtblColNameType() {
		return htblColNameType;
	}

	public void setHtblColNameType(Hashtable<String, String> htblColNameType) {
		this.htblColNameType = htblColNameType;
	}

	public Hashtable<String, String> getHtblColNameMin() {
		return htblColNameMin;
	}

	public void setHtblColNameMin(Hashtable<String, String> htblColNameMin) {
		this.htblColNameMin = htblColNameMin;
	}

	public Hashtable<String, String> getHtblColNameMax() {
		return htblColNameMax;
	}

	public void setHtblColNameMax(Hashtable<String, String> htblColNameMax) {
		this.htblColNameMax = htblColNameMax;
	}

	public int getPagesCount() {
		return pagesCount;
	}

	public void setPagesCount(int pagesCount) {
		this.pagesCount = pagesCount;
	}

	public ArrayList<String> getIndicesNames() {
		return indicesNames;
	}
	
	

	
}
