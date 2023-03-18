package engine;
import java.util.Hashtable;
import java.util.Iterator;
import java.io.Serializable;
//import java.util.Vector;
//import java.util.Properties;

@SuppressWarnings("serial")
public class DBApp implements Serializable {
	
	public void init( ) {
		//create the metadata file if it does not exist?
		
		//in the project description: 20) Upon application startup; to avoid having to scan all tables to build existing indices, 
		//you should save the index itself to disk and load it when the application starts next time
	}
	
	public void createTable(String strTableName, 
			 String strClusteringKeyColumn, 
			Hashtable<String,String> htblColNameType, 
			Hashtable<String,String> htblColNameMin, 
			Hashtable<String,String> htblColNameMax ) 
			 throws DBAppException {
		//check if the table already exists and throw exception if it does
		//check if the clusteringKey is one of the table columns
		//check if each column type is one of the 4 types listed in the project description
		//update the metadata file
	}
	
	public void insertIntoTable(String strTableName, 
			Hashtable<String,Object> htblColNameValue) 
					throws DBAppException {
		//throw an exception if the table where the record will be inserted in doesn't exist
		//check if a record with the inserted clusteringKey exists and if so throw an exception 
	}
	
	public void updateTable(String strTableName, 
			String strClusteringKeyValue, 
			Hashtable<String,Object> htblColNameValue ) 
					throws DBAppException {
		//update the metadata file
	}
	
	public void deleteFromTable(String strTableName, 
			Hashtable<String,Object> htblColNameValue) 
					throws DBAppException {
		//check if the record does not exist?
	}

//	MILESTONE 2 ---------------------------------------------------------------------------------------------------------------------------------------
	
	public void createIndex(String strTableName, 
			String[] strarrColName) throws DBAppException {
		
	}
	
	
	@SuppressWarnings("rawtypes")
	public Iterator selectFromTable(SQLTerm[] arrSQLTerms, 
			 String[] strarrOperators) 
			throws DBAppException {
				return null;
		
	}
	
	
// 	BONUS METHOD -----------------------------------------------------------------------------------------------------------------------------------
	
	@SuppressWarnings("rawtypes")
	public Iterator parseSQL( StringBuffer strbufSQL ) throws
	DBAppException {
		return null;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
//		String strTableName = "Student"; 
//		DBApp dbApp = new DBApp( ); 
//		Hashtable htblColNameType = new Hashtable( ); 
//		htblColNameType.put("id", "java.lang.Integer"); 
//		htblColNameType.put("name", "java.lang.String"); 
//		htblColNameType.put("gpa", "java.lang.double"); 
//		/* dbApp.createTable( strTableName, "id", htblColNameType in addition to min 
//		max hashtables” ); */
//		dbApp.createIndex( strTableName, new String[] {"gpa"} ); 
//		Hashtable htblColNameValue = new Hashtable( ); 
//		htblColNameValue.put("id", new Integer( 2343432 )); 
//		htblColNameValue.put("name", new String("Ahmed Noor" ) ); 
//		htblColNameValue.put("gpa", new Double( 0.95 ) ); 
//		dbApp.insertIntoTable( strTableName , htblColNameValue ); 
//		htblColNameValue.clear( ); 
//		htblColNameValue.put("id", new Integer( 453455 )); 
//		htblColNameValue.put("name", new String("Ahmed Noor" ) ); 
//		htblColNameValue.put("gpa", new Double( 0.95 ) ); 
//		dbApp.insertIntoTable( strTableName , htblColNameValue ); 
//		htblColNameValue.clear( ); 
//		htblColNameValue.put("id", new Integer( 5674567 )); 
//		htblColNameValue.put("name", new String("Dalia Noor" ) ); 
//		htblColNameValue.put("gpa", new Double( 1.25 ) ); 
//		dbApp.insertIntoTable( strTableName , htblColNameValue ); 
//		htblColNameValue.clear( ); 
//		htblColNameValue.put("id", new Integer( 23498 )); 
//		htblColNameValue.put("name", new String("John Noor" ) ); 
//		htblColNameValue.put("gpa", new Double( 1.5 ) ); 
//		dbApp.insertIntoTable( strTableName , htblColNameValue ); 
//		htblColNameValue.clear( ); 
//		htblColNameValue.put("id", new Integer( 78452 )); 
//		htblColNameValue.put("name", new String("Zaky Noor" ) ); 
//		htblColNameValue.put("gpa", new Double( 0.88 ) ); 
//		dbApp.insertIntoTable( strTableName , htblColNameValue ); 
//		SQLTerm[] arrSQLTerms; 
//		arrSQLTerms = new SQLTerm[2]; 
//		arrSQLTerms[0]._strTableName = "Student"; 
//		arrSQLTerms[0]._strColumnName= "name"; 
//		arrSQLTerms[0]._strOperator = "="; 
//		arrSQLTerms[0]._objValue = "John Noor"; 
//		arrSQLTerms[1]._strTableName = "Student"; 
//		arrSQLTerms[1]._strColumnName= "gpa"; 
//		arrSQLTerms[1]._strOperator = "="; 
//		arrSQLTerms[1]._objValue = new Double( 1.5 ); 
//		String[]strarrOperators = new String[1]; 
//		strarrOperators[0] = "OR"; 
//		// select * from Student where name = “John Noor” or gpa = 1.5; 
//		Iterator resultSet = dbApp.selectFromTable(arrSQLTerms , strarrOperators);
	}

}
