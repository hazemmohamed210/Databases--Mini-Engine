This project is a small database engine with support for Octree indices.

#Features:

1- creating tables
2- creating Octree indices on the columns of a table
3- inserting a single row to the table
4- updating a single row in the table corresponding to a given primary key value
5- deleting one or more rows from the table satisfying given conditions
6- selecting rows satisfying a given query as inputs
7- parsing SQL statements corresponding to one of the above features and execute the statement

#Installation:

1- clone the repository in the IDE you are using
2- add the jar file `antlr-4.4-complete.jar` that you will find in the folder named lib to the project's classpath as an external jar file to be able to use the parseSQL function

#Usage:
1- create an instance of the DBApp class in a main method
2- use the following methods to interact with the database engine:
  - `init( )`: initializes the database and creates a metadata file for the created tables
  - `createTable(String strTableName, String strClusteringKeyColumn, Hashtable<String,String> htblColNameType, Hashtable<String,String> htblColNameMin, Hashtable<String,String> htblColNameMax )`: creates a new table in the database
  ```
  DBApp db = new DBApp();
	db.init();
  Hashtable htblColNameType = new Hashtable( ); 
  htblColNameType.put("speed", "java.lang.Integer"); 
  htblColNameType.put("model", "java.lang.String"); 
  htblColNameType.put("id", "java.lang.Integer");
  Hashtable htblColNameMin = new Hashtable( ); 
  htblColNameMin.put("speed", "0"); 
  htblColNameMin.put("model", "a"); 
  htblColNameMin.put("id", "0");
  Hashtable htblColNameMax = new Hashtable( ); 
  htblColNameMax.put("speed", "500"); 
  htblColNameMax.put("model", "z"); 
  htblColNameMax.put("id", "500"); 
  
  try {
    db.createTable("cars", "id", htblColNameType, htblColNameMin, htblColNameMax);
  } catch (DBAppException e) {
			e.printStackTrace();
  }
  ```
  - `createIndex(String strTableName, String[] strarrColName)`: creates an index on a given table name and array of columns
  ```
  String[] columns = {"id","speed","model"};
  db.createIndex("cars", arr);
  ```
  - `insertIntoTable(String strTableName, Hashtable<String,Object> htblColNameValue)`: inserts a new row to a given table name
  ```
  Hashtable htblColNameValue = new Hashtable( ); 
  htblColNameValue.put("id", 1);
  htblColNameValue.put("model", "a");
  htblColNameValue.put("speed", 120);
			
  try {
    db.insertIntoTable("cars", htblColNameValue);
  } catch (DBAppException e) {
      e.printStackTrace();
  }
  ```
  - `updateTable(String strTableName, String strClusteringKeyValue, Hashtable<String,Object> htblColNameValue)`: updates a row in a given table name
  ```
  Hashtable htblColNameValue = new Hashtable( ); 
  htblColNameValue.put("speed", 3);
  try {
    db.updateTable("cars3", "1", htblColNameValue);
  } catch (DBAppException e) {
    e.printStackTrace();
  }
  ```
  - `deleteFromTable(String strTableName, Hashtable<String,Object> htblColNameValue)`: deletes any row satisfying the given conditions
  ```
  Hashtable htblColNameValue = new Hashtable( ); 
  htblColNameValue.put("speed", 3);
  try {
    db.deleteFromTable("cars", htblColNameValue);
  } catch (DBAppException e) {
    e.printStackTrace();
  }
  ```
  - `selectFromTable(SQLTerm[] arrSQLTerms, String[] strarrOperators)`: returns the result set of the given query conditions
  ```
  SQLTerm s2 = new SQLTerm("cars2", "model", "<", "d");
  SQLTerm s1 = new SQLTerm("cars2", "speed", "=", 241);
  SQLTerm s3 = new SQLTerm("cars2", "model", "=", "g");
  SQLTerm s4 = new SQLTerm("cars2", "id", "!=", 50);
  SQLTerm[] sqlarr = {s1,s2,s3};
  String[] ops = {"AND","AND"};
  ResultSet r = (ResultSet) db.selectFromTable(sqlarr, ops);
  ```
  - `parseSQL(StringBuffer strbufSQL)`: parses the given SQL and executes one of the above methods accordingly
  ```
  StringBuffer str = new StringBuffer("SELECT * FROM CARS WHERE MODEL > 'a' AND SPEED = 20");
  db.parseSQL(str)
  ```