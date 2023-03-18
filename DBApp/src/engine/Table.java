package engine;

import java.util.Hashtable;
import java.io.Serializable;
//import java.util.Vector;

@SuppressWarnings("serial")
public class Table implements Serializable {
	private String strTableName; 
	private String strClusteringKeyColumn; 
	private Hashtable<String,String> htblColNameType; 
	private Hashtable<String,String> htblColNameMin; 
	private Hashtable<String,String> htblColNameMax;
	//Vector<> for pages?
	//private int numberOfPages?
	// create a method to add a page?
	// create a method to delete a page?
	// create a separate class for a page?

	
}
