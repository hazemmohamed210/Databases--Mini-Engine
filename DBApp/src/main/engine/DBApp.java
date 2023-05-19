package main.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;

import main.grammar.SQLiteLexer;
import main.grammar.SQLiteParser;
import main.grammar.SQLiteParser.Create_table_stmtContext;

public class DBApp{
	
//	-------------------------------------------------- INDICES -----------------------------------------------------------------------------------
	public Object[] parseRanges(Table t, String[] strarrColName) throws DBAppException {
		Object[] ranges = new Object[6];
		int i = 0;
		for(String col : strarrColName) {
			switch(t.getHtblColNameType().get(col)) {
		        case "java.lang.String":
		            ranges[i] = t.getHtblColNameMin().get(col);
		            ranges[i+1] = t.getHtblColNameMax().get(col);
//		            System.out.println("string");
		            break;
		        case "java.util.Date":
		        	try {
						ranges[i] = new SimpleDateFormat("yyyy-MM-dd").parse(t.getHtblColNameMin().get(col));
						ranges[i+1] = new SimpleDateFormat("yyyy-MM-dd").parse(t.getHtblColNameMax().get(col));
					} catch (ParseException e) {
						throw new DBAppException("error: "+e.getMessage());
					}
		            break;
		        case "java.lang.Double":
		        	ranges[i] = Double.parseDouble(t.getHtblColNameMin().get(col));
		            ranges[i+1] = Double.parseDouble(t.getHtblColNameMax().get(col));
		            break;
		        case "java.lang.Integer":
		        	ranges[i] = Integer.parseInt(t.getHtblColNameMin().get(col));
		            ranges[i+1] = Integer.parseInt(t.getHtblColNameMax().get(col));
//		            System.out.println("int");
		            break;
			}
			i+=2;
		}
		return ranges;
	}
	
	public Object[] isIndex(String tableName, String[] strarrColName) throws DBAppException {
		BufferedReader br;
		boolean col1Checker = false;
		boolean col2Checker = false;
		boolean col3Checker = false;
		String name1 = "";
		String name2 = "";
		String name3 = "";
		if(strarrColName.length != 3 || strarrColName[0].equals(strarrColName[1]) || strarrColName[0].equals(strarrColName[2]) || strarrColName[1].equals(strarrColName[2])) {
			Object[] r = new Object[2];
			r[0] = false;
			r[1] = "null";
			return r;
		}
		try {
			br = new BufferedReader(new FileReader("src/main/resources/metadata.csv"));
			String line = "";
			while ((line = br.readLine()) != null) {
				String[] cells = line.split(",");
				if(cells[0].equals(tableName) && cells[1].equals(strarrColName[0]) && !cells[4].equals("null") && !cells[5].equals("null")) {
					col1Checker = true;
					name1 = cells[4];
				}
				if(cells[0].equals(tableName) && cells[1].equals(strarrColName[1]) && !cells[4].equals("null") && !cells[5].equals("null")) {
					col2Checker = true;
					name2 = cells[4];
				}
				if(cells[0].equals(tableName) && cells[1].equals(strarrColName[2]) && !cells[4].equals("null") && !cells[5].equals("null")) {
					col3Checker = true;
					name3 = cells[4];
				}
			}
			br.close();
			Object[] res = {col1Checker && col2Checker && col3Checker && name1.equals(name2) && name1.equals(name3), name1};
			return res;
		} catch (IOException e) {
			throw new DBAppException("error: "+e.getMessage());
		}
	}
	
	public boolean isIndexed(String tableName, String colName) throws DBAppException {
		BufferedReader br;
		boolean indexChecker = false;
		try {
			br = new BufferedReader(new FileReader("src/main/resources/metadata.csv"));
			String line = "";
			while ((line = br.readLine()) != null) {
				String[] cells = line.split(",");
				if(cells[0].equals(tableName) && cells[1].equals(colName) && !cells[4].equals("null") && !cells[5].equals("null")) {
					indexChecker = true;
					break;
				}
			}
			br.close();
			return indexChecker;
		} catch (IOException e) {
			throw new DBAppException("error: "+e.getMessage());
		}
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
	
	public void updateMetaData(String tableName, String[] strarrColName) throws DBAppException {
		try {
			
			// Open the CSV file for reading
			BufferedReader reader = new BufferedReader(new FileReader("src/main/resources/metadata.csv"));
			
			// Create a temporary file for writing the updated data
			FileWriter writer = new FileWriter("src/main/resources/metadata_temp.csv");
			
			String line;
			
			// Loop through the lines in the file
			while ((line = reader.readLine()) != null) {
				// Split the line into cells
				String[] cells = line.split(",");
				
				// Check if this is the line we want to update
				if (cells[0].equals(tableName) && (cells[1].equals(strarrColName[0]) || cells[1].equals(strarrColName[1]) || cells[1].equals(strarrColName[2]))) {
					// Update the cell value
					String indexName = tableName+"_"+strarrColName[0]+strarrColName[1]+strarrColName[2];
					cells[4] = indexName;
					cells[5] = "Octree";
				}
				
				// Write the updated line to the temporary file
				writer.write(String.join(",", cells) + "\n");
			}
			
			// Close the reader and writer
			reader.close();
			writer.close();
			
			// Replace the original file with the updated data
			File originalFile = new File("src/main/resources/metadata.csv");
			originalFile.delete();
			File tempFile = new File("src/main/resources/metadata_temp.csv");
			tempFile.renameTo(originalFile);
		} catch (IOException e) {
			throw new DBAppException("error: "+e.getMessage());
		}
	  }
	
	public void createIndex(String strTableName, String[] strarrColName) throws DBAppException {
		// ------------------------------------- check if the columns indexed are less than 3 --------------------------------------------------
		if(strarrColName.length != 3) throw new DBAppException("an octree index can't be created using only "+strarrColName.length+" columns");
		
		// ------------------------------------- check if the index already exists ------------------------------------------------------------
		if((boolean) this.isIndex(strTableName, strarrColName)[0]) {
			throw new DBAppException("an index on the 3 input columns already exists");
		}
		Table t = this.deSerTable(strTableName);
		// ----------------------------------------- get the index columns ranges -------------------------------------------------------------
		Object[] ranges = this.parseRanges(t, strarrColName);
		// ----------------------------------------- add a reference to the index created to the table ----------------------------------------
		t.getIndicesNames().add(strTableName+"_"+strarrColName[0]+strarrColName[1]+strarrColName[2]);
		
		Octree2 index = new Octree2(ranges, strarrColName, strTableName, strTableName+"_"+strarrColName[0]+strarrColName[1]+strarrColName[2]);
		// ------------------------------------------ update the metadata file ----------------------------------------------------------------
		this.updateMetaData(strTableName, strarrColName);
		// ------------------------------------- insert existing entries in the table into the index ------------------------------------------
		for(int i = 1; i<=t.getPagesCount(); i++) {
			Page p = t.deSerPage(i);
			for(int j = 0; j<p.getSize(); j++) {
				Object[] tupl = new Object[3];
				tupl[0] = p.getData().get(j).get(index.getColNames()[0]);
				tupl[1] = p.getData().get(j).get(index.getColNames()[1]);
				tupl[2] = p.getData().get(j).get(index.getColNames()[2]);
				OcTreeEntry en = new OcTreeEntry(tupl, i, j);
				index.insert(en);
			}
		}
		// ------------------------------------------- serialize the index --------------------------------------------------------------------
		this.serIndex(index);
		index = null;
		this.serTable(t);
		t = null;
		System.gc();
	}
	
//	-------------------------------------------------- Milestone 1 -------------------------------------------------------------------------------
	public ArrayList<String> getTableHeaders(String strTableName) throws DBAppException {
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader("src/main/resources/metadata.csv"));
			String line = "";
			ArrayList<String> headers = new ArrayList<>();
//			System.out.println("in there");
			while ((line = br.readLine()) != null) {
				if(line.split(",")[0].equals(strTableName)) {
					headers.add(line.split(",")[1]);
				}
			}
			br.close();
			return headers;
		} catch (FileNotFoundException e) {
			throw new DBAppException("error: "+e.getMessage());
		} catch (IOException e) {
			throw new DBAppException("error: "+e.getMessage());
		}
	}
	
	public void init( ) {
		//in the project description: 20) Upon application startup; to avoid having to scan all tables to build existing indices, 
		//you should save the index itself to disk and load it when the application starts next time
		
		//-------------------------------------- create the metadata file -------------------------------------------------------------------
		File f = new File("src/main/resources/metadata.csv");
		if (!f.exists()) {
			try {
				FileWriter writer = new FileWriter("src/main/resources/metadata.csv");
				System.out.println("csv created");
				writer.append("Table Name");
				writer.append(',');
				writer.append("Column Name");
				writer.append(',');
				writer.append("Column Type");
				writer.append(',');
				writer.append("ClusteringKey");
				writer.append(',');
				writer.append("IndexName");
				writer.append(',');
				writer.append("IndexType");
				writer.append(',');
				writer.append("min");
				writer.append(',');
				writer.append("max");
				writer.append("\n");
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		//-----------------------------------------------------------------------------------------------------------------------------------
	}
	
	public boolean isTable(String strTableName) throws DBAppException {
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader("src/main/resources/metadata.csv"));
			boolean tableChecker = false;
			String line = "";
			while ((line = br.readLine()) != null) {
				List<String> cells =  Arrays.asList(line.split(","));
				if (cells.contains(strTableName)) tableChecker = true;
			}
			br.close();
			return tableChecker;
		} catch (IOException e) {
			throw new DBAppException("error: "+e.getMessage());
		}
		
	}
	
	public void checkEntryMinMax(Hashtable<String, Object> htblColNameValue, String strTableName) throws DBAppException {
		try {
			for(String key : htblColNameValue.keySet()) {
				BufferedReader br = new BufferedReader(new FileReader("src/main/resources/metadata.csv"));
				String line = "";
//				System.out.println("enter loop: "+key);
				while ((line = br.readLine()) != null) {
					List<String> row =  Arrays.asList(line.split(","));
					if(row.get(0).equals(strTableName) && row.get(1).equals(key)) {
//						System.out.println("enter switch: "+row.get(2));
						switch (row.get(2)) {
							case "java.lang.Integer": 
//								System.out.println("I'm here "+key);
								String max = (row.get(7));
								if((int)htblColNameValue.get(key) < Integer.parseInt(row.get(6)) || (int)htblColNameValue.get(key) > Integer.parseInt(max)) {
									throw new DBAppException("min or max constraint is violated for: "+key);
								} break;
							case "java.lang.String": 
//								System.out.println("I'm here "+key);
								if(((String)htblColNameValue.get(key)).compareTo(row.get(6)) < 0 || ((String)htblColNameValue.get(key)).compareTo(row.get(7)) > 0) {
//									System.out.println(((String)htblColNameValue.get(key)).compareTo(row.get(6)));
//									System.out.println(((String)htblColNameValue.get(key)).compareTo(row.get(7)));
									throw new DBAppException("min or max constraint is violated for: "+key);
								} break;
							case "java.lang.Double": 
								if((double)htblColNameValue.get(key) < Double.parseDouble(row.get(6)) || (double)htblColNameValue.get(key) > Double.parseDouble(row.get(7))) {
									throw new DBAppException("min or max constraint is violated for: "+key);
								} break;
							case "java.util.Date":
//								System.out.println("helloz");
								if(((Date) htblColNameValue.get(key)).compareTo(new SimpleDateFormat("yyyy-MM-dd").parse(row.get(6))) < 0 || ((Date)htblColNameValue.get(key)).compareTo(new SimpleDateFormat("yyyy-MM-dd").parse(row.get(7))) > 0) {
									throw new DBAppException("min or max constraint is violated for: "+key);
								} break;
//							default: System.out.println("I'm here");
						}
					}
				}
				br.close();
			}
		} catch (IOException | ParseException e) {
			throw new DBAppException("error: "+e.getMessage());
		}
	}
	
	public void serTable(Table t) throws DBAppException {
		try {
	       FileOutputStream fileOut = new FileOutputStream("src/main/resources/data/" + t.getStrTableName() + ".ser");
	       ObjectOutputStream out = new ObjectOutputStream(fileOut);
	       out.writeObject(t);
	       out.close();
	       fileOut.close();
//	       System.out.println("table serialized");
	    } catch (IOException i) {
	    	throw new DBAppException("error: "+i.getMessage());
	    }
		t = null;
		System.gc();
	}
	
	public Table deSerTable(String strTableName) throws DBAppException {
		Table t = null;
		try {
	       FileInputStream fileIn = new FileInputStream("src/main/resources/data/" + strTableName + ".ser");
	       ObjectInputStream in = new ObjectInputStream(fileIn);
	       t = (Table) in.readObject();
	       in.close();
	       fileIn.close();
	    } catch (IOException i) {
	    	throw new DBAppException("error: "+i.getMessage());
	    } catch (ClassNotFoundException c) {
	    	throw new DBAppException("error: "+c.getMessage());
	    }
		
		return t;
	}
	
	public void createTable(String strTableName, 
			 String strClusteringKeyColumn, 
			Hashtable<String,String> htblColNameType, 
			Hashtable<String,String> htblColNameMin, 
			Hashtable<String,String> htblColNameMax ) 
			 throws DBAppException {
		//------------------------ check if the table already exists and throw exception if it does -----------------------------------------
		if(this.isTable(strTableName)) {
			throw new DBAppException("the table " + strTableName + " already exists in the database");
		}
		
		if(!(htblColNameMax.size() == htblColNameMin.size() && htblColNameMax.size() == htblColNameType.size())) {
			throw new DBAppException("missing/extra information entered");
		}
		//---------------------- check if the clusteringKey is one of the table columns -----------------------------------------------------
		boolean keyChecker = false;
		for(String key : htblColNameType.keySet()) {
			if(key.equals(strClusteringKeyColumn)) keyChecker = true;
		}
		if(!keyChecker) {
			throw new DBAppException("the clustering key column is not one of the table columns");
		}
		//-------------- check if each column type is one of the 4 types listed in the project description -----------------------------------
		String[] typesArr = {"java.lang.Integer", "java.lang.Double", "java.lang.String", "java.util.Date"};
		List<String> types =  Arrays.asList(typesArr);
		String msg = "";
		boolean typeChecker = true;
		for(String key : htblColNameType.keySet()) {
			if(!types.contains(htblColNameType.get(key))) {
				typeChecker = false;
				msg = key + " has an invalid type: " + htblColNameType.get(key);
			}
		}
		if (!typeChecker) {
			throw new DBAppException("the column " + msg);
		}
		//------------------------------------------- update the metadata file ---------------------------------------------------------------
		File f = new File("src/main/resources/metadata.csv");
		if(f.exists()) {
			try {
				FileWriter writer = new FileWriter("src/main/resources/metadata.csv", true);
				
				for(String key : htblColNameType.keySet()) {
					writer.append(strTableName);
					writer.append(',');
					writer.append(key);
					writer.append(',');
					writer.append(htblColNameType.get(key));
					writer.append(',');
					if(key.equals(strClusteringKeyColumn)) {
						writer.append("True");
					} else {
						writer.append("False");
					}
					writer.append(',');
					writer.append("null");
					writer.append(',');
					writer.append("null");
					writer.append(',');
					writer.append(htblColNameMin.get(key));
					writer.append(',');
					writer.append(htblColNameMax.get(key));
					writer.append("\n");
				}
				
				writer.close();
				
			} catch (IOException e) {
				throw new DBAppException("error: "+e.getMessage());
			}
		}
		//------------------------------------------ create a new csv file for the created table ---------------------------------------------
		try {
			FileWriter tbl = new FileWriter("src/main/resources/data/"+strTableName+".csv");
			ArrayList<String> arr = new ArrayList<String>();
			for(String key : htblColNameType.keySet()) {
				arr.add(key);
			}
			for(String key : arr) {
				tbl.append(key);
				tbl.append(",");
			}
			tbl.append("\n");
			tbl.close();
		} catch (IOException e) {
			throw new DBAppException("error: "+e.getMessage());
		}
		//------------------------------------------- create the table instance and serialize it ---------------------------------------------
		Table t = new Table(strTableName, strClusteringKeyColumn, htblColNameType, htblColNameMin, htblColNameMax);
		this.serTable(t);
		t = null;
		System.gc();
	}
	
	public void appendRow(FileWriter f, Hashtable<String, Object> htblColNameValue, String strTableName) throws DBAppException {
		for(String header : this.getTableHeaders(strTableName)) {
			if(htblColNameValue.containsKey(header)) {
				try {
					if((htblColNameValue.get(header).getClass()+"").split(" ")[1].equals("java.util.Date")) {
						f.append((new SimpleDateFormat("yyyy-MM-dd")).format(htblColNameValue.get(header)));
					} else f.append(htblColNameValue.get(header)+"");
					f.append(",");
				} catch (IOException e) {
					throw new DBAppException("error: "+e.getMessage());
				}
			} else {
				try {
					f.append("null");
					f.append(",");
				} catch (IOException e) {
					throw new DBAppException("error: "+e.getMessage());
				}
			}
		}
	}
	
	public void updateTableFile(Table t) throws DBAppException {
		if(t.getPagesCount() == 0) {
			try {
				FileWriter f = new FileWriter("src/main/resources/data/"+ t.getStrTableName() + ".csv");
//				System.out.println(this.getTableHeaders(t.getStrTableName()));
				for(String key : this.getTableHeaders(t.getStrTableName())) {
					f.append(key);
					f.append(",");
				}
				f.append("\n");
				f.close();
				
			} catch (IOException e) {
				throw new DBAppException("error: "+e.getMessage());
			}
			return;
		}
		try {
			FileWriter f = new FileWriter("src/main/resources/data/"+ t.getStrTableName() + ".csv");
			for(int i = 1; i <= t.getPagesCount(); i++) {
				Page p = t.deSerPage(i);
				if(i == 1) {
					for(String key : this.getTableHeaders(t.getStrTableName())) {
						f.append(key);
						f.append(",");
					}
					f.append("\n");
				}
				for(int j = 0; j < p.getSize(); j++) {
					this.appendRow(f, p.getData().get(j), t.getStrTableName());
					f.append("\n");
				}
			}
			f.close();
		} catch (IOException e) {
			throw new DBAppException("error: "+e.getMessage());
		}
//		System.out.println("table file updated");
	}
	
	public int getTargetPageV2(String target, String type, Table t) throws DBAppException {
//		System.out.println(t);
		for(int i = 1; i < t.getPagesCount(); i++) {
			Page p = t.deSerPage(i);
			try {
				int x = p.compareKeys(target, type, p.getData().get(p.getSize()-1).get(t.getStrClusteringKeyColumn()));
				int y = p.compareKeys(target, type, (t.deSerPage(i+1)).getData().get(0).get(t.getStrClusteringKeyColumn()));
				if((x <= 0 && p.isFull()) || (x <= 0 && y >= 0)) {
					t.serPage(p);
					return i;
				}
				t.serPage(p);
				p = null;
				System.gc();
			} catch (ParseException e) {
				throw new DBAppException("error: "+e.getMessage());
			}
//			System.out.println(x+ " AND "+ y+ " "+ p.isFull()+", size = "+p.getSize());
		}
		return t.getPagesCount();
	}
	
	public void insertIntoTable(String strTableName, 
			Hashtable<String,Object> htblColNameValue) 
					throws DBAppException {
		ArrayList<String> colTypes = new ArrayList<String>();
		String pk = "";
		String type = "";
		//-------------------- throw an exception if the table where the record will be inserted in doesn't exist ----------------------------
		try {
			BufferedReader br = new BufferedReader(new FileReader("src/main/resources/metadata.csv"));
			boolean tableChecker = false;
			String line = "";
			while ((line = br.readLine()) != null) {
				List<String> cells =  Arrays.asList(line.split(","));
				if(cells.get(0).equals(strTableName)) {
					colTypes.add(cells.get(2));
				}
				if(cells.get(0).equals(strTableName) && cells.get(3).equals("True")) {
					type = cells.get(2);
					pk = cells.get(1);
				}
				if (cells.contains(strTableName)) tableChecker = true;
			}
			br.close();
			
			if(!tableChecker) {
				throw new DBAppException("the table " + strTableName + " does not exist in the database");
			}
		} catch (IOException e) {
			throw new DBAppException("error with the metadata file");
		}
		//	1) -------------- check if the number of columns inserted is the same as the schema of the table else throw an exception ---------
		if(htblColNameValue.size() > colTypes.size()) {
			throw new DBAppException("you're trying to insert a row with more columns than the table columns");
		} else if (htblColNameValue.size() < colTypes.size()) {
			boolean pkChecker = false;
			for(String key : htblColNameValue.keySet()){
				if(key.equals(pk)) {
					pkChecker = true;
				}
			}
			if(!pkChecker) throw new DBAppException("the primary key : "+pk+" can't be null");
		}
		// ---------------------------------- check if a value is null -----------------------------------------------------------------------
		for(String colName : this.deSerTable(strTableName).getHtblColNameType().keySet()) {
			if(this.isIndexed(strTableName, colName) && htblColNameValue.get(colName) == null) {
				throw new DBAppException("the column "+colName+" is indexed so it can't be null");
			}
		}
		//-------------------------------------- deserialize the table that the row will be inserted in --------------------------------------
		Table t = this.deSerTable(strTableName);
//		System.out.println(t);
		// ------------------------------------- check if column names and datatypes inserted are correct ------------------------------------
		for(String key: htblColNameValue.keySet()) {
			if(!t.getHtblColNameType().containsKey(key)) throw new DBAppException("wrong column name entered: "+key);
			else {
				if(! (htblColNameValue.get(key).getClass()+"").split(" ")[1].equals(t.getHtblColNameType().get(key))) {
					throw new DBAppException("wrong datatype for column: "+key);
				}
			}
		}
		//  ------------------------------------ check min and max constraints for columns inserted ------------------------------------------
		this.checkEntryMinMax(htblColNameValue, strTableName);
		
			
		// check if there are no pages created
		if(t.getPagesCount() == 0) {
			t.addPage();
			Page p = t.deSerPage(t.getPagesCount());
			p.addToPage(htblColNameValue, pk);
			// insert in the index
			for(String index : t.getIndicesNames()) {
				Octree2 o = this.deSerIndex(index);
				Object[] tupl = new Object[3];
				tupl[0] = htblColNameValue.get(o.getColNames()[0]);
				tupl[1] = htblColNameValue.get(o.getColNames()[1]);
				tupl[2] = htblColNameValue.get(o.getColNames()[2]);
				OcTreeEntry en = new OcTreeEntry(tupl, 1, 0);
				o.insert(en);
				this.serIndex(o);
				o = null;
				System.gc();
			}
			t.serPage(p);
			p = null;
			System.gc();
		} else {
//			System.out.println(t);
			// get the target page where the row will be inserted
			int targetPageNumber;
			try {
				String in = "";
				if((htblColNameValue.get(pk).getClass()+"").split(" ")[1].equals("java.util.Date")) {
					in = (new SimpleDateFormat("yyyy-MM-dd")).format(htblColNameValue.get(pk));
//					System.out.println(in);
				} else in = htblColNameValue.get(pk)+"";
//				System.out.println(t);
				targetPageNumber = this.getTargetPageV2(in, type, t);
				// add the the row to the target page
				Page p = t.deSerPage(targetPageNumber);
				//	--------------------- check if a record with the inserted clusteringKey exists and if so throw an exception --------------
				int rowNum = (p.binarySearch(in, pk, 0, p.getSize()-1) +1)* -1;
				if(p.binarySearch(in, pk, 0, p.getSize()-1) >= 0) {
					throw new DBAppException("a row with a pk = "+htblColNameValue.get(pk)+" already exists");
				} 
				p.addToPage(htblColNameValue, pk);
				// insert in the index
				for(String index : t.getIndicesNames()) {
					Octree2 o = this.deSerIndex(index);
					Object[] tupl = new Object[3];
					tupl[0] = htblColNameValue.get(o.getColNames()[0]);
					tupl[1] = htblColNameValue.get(o.getColNames()[1]);
					tupl[2] = htblColNameValue.get(o.getColNames()[2]);
					OcTreeEntry en = new OcTreeEntry(tupl, targetPageNumber, rowNum);
					o.insert(en);
					for(int i = rowNum+1; i<p.getSize(); i++) {
//						System.out.println("start is: "+rowNum+1+" "+p.getData());
						Object[] tupl2 = new Object[3];
						tupl2[0] = p.getData().get(i).get(o.getColNames()[0]);
						tupl2[1] = p.getData().get(i).get(o.getColNames()[1]);
						tupl2[2] = p.getData().get(i).get(o.getColNames()[2]);
						OcTreeEntry inOld = new OcTreeEntry(tupl2, p.getPageNumber(), i-1);
						OcTreeEntry inNew = new OcTreeEntry(tupl2, p.getPageNumber(), i);
//						System.out.println(in);
//						System.out.println(inNew);
						o.update(inOld, inNew);
					}
					this.serIndex(o);
					o = null;
					System.gc();
				}
				// check that the page is full then handle row shifts
				p.handleShifts(t, rowNum+1);
				p = null;
				System.gc();
			} catch (ParseException e) {
				throw new DBAppException("error : "+e.getMessage());
			}
		}
		
		//	4) ---------------- insert in the right page file and in the table csv file if no exceptions are thrown---------------------------
		this.updateTableFile(t);
		// ------------------------------------------------ serialize the table --------------------------------------------------------------
		this.serTable(t);
		t = null;
		System.gc();
	}

	public void updateTable(String strTableName, 
			String strClusteringKeyValue, 
			Hashtable<String,Object> htblColNameValue ) 
					throws DBAppException {
		//-------------------- throw an exception if the table where the record will be updated doesn't exist --------------------------------
		String type = "";
		String name = "";
		if(!this.isTable(strTableName)) {
			throw new DBAppException("the table " + strTableName + " does not exist in the database");
		}
		try {
			//---------------------------------------------------------------------------------------------------------------------------------
			BufferedReader br = new BufferedReader(new FileReader("src/main/resources/metadata.csv"));
			String line = "";
			while ((line = br.readLine()) != null) {
				List<String> cells =  Arrays.asList(line.split(","));
				if(cells.get(0).equals(strTableName) && cells.get(3).equals("True")) {
					type = cells.get(2);
					name = cells.get(1);
				}
			}
			br.close();
		} catch (IOException e) {
			throw new DBAppException("error with metadata file");
		}
		if(strClusteringKeyValue == null) throw new DBAppException("you need to specify the clustering key of the row to be updated");
		Table t = this.deSerTable(strTableName);
		if(t.getPagesCount() == 0) throw new DBAppException("you can't update a record in a an empty table");
		// --------------------------------------- check updated columns names, datatypes, min and max constraints ---------------------------
		for(String key: htblColNameValue.keySet()) {
			if(!t.getHtblColNameType().containsKey(key)) throw new DBAppException("wrong column name entered: "+key);
			else {
				if(! (htblColNameValue.get(key).getClass()+"").split(" ")[1].equals(t.getHtblColNameType().get(key))) {
					throw new DBAppException("wrong datatype for column: "+key);
				}
			}
		}
		this.checkEntryMinMax(htblColNameValue, strTableName);
		if(htblColNameValue.containsKey(name)) throw new DBAppException("you can't update the clustering key of the row");
		//------------------------------------------------ locate the target page and tuple index --------------------------------------------
		try {
			int targetPageNumber = this.getTargetPageV2(strClusteringKeyValue, type, t);
			Page p = t.deSerPage(targetPageNumber);
			int end = p.getSize()-1;
			int rowIndex = p.binarySearch(strClusteringKeyValue, name, 0, end);
			if(rowIndex < 0) throw new DBAppException("a row with the clustering key you entered doesn't exist");
			else {
				Vector<OcTreeEntry> v = new Vector<>(); // contains the old entries in the octree indices on the table
				int i = 0;
				for(String indexName : t.getIndicesNames()) {
					Octree2 index = this.deSerIndex(indexName);
					Object[] tupl = new Object[3];
					tupl[0] = p.getData().get(rowIndex).get(index.getColNames()[0]);
					tupl[1] = p.getData().get(rowIndex).get(index.getColNames()[1]);
					tupl[2] = p.getData().get(rowIndex).get(index.getColNames()[2]);
					OcTreeEntry en = new OcTreeEntry(tupl, targetPageNumber, rowIndex);
					v.add(en);
				}
//				System.out.println(v);
				for(String key : htblColNameValue.keySet()) {
					p.getData().get(rowIndex).put(key, htblColNameValue.get(key));
				}
				for(String indexName : t.getIndicesNames()) { // update the entry in the index corresponding to the row in the table
					Octree2 index = this.deSerIndex(indexName);
					Object[] tupl = new Object[3];
					tupl[0] = p.getData().get(rowIndex).get(index.getColNames()[0]);
					tupl[1] = p.getData().get(rowIndex).get(index.getColNames()[1]);
					tupl[2] = p.getData().get(rowIndex).get(index.getColNames()[2]);
					OcTreeEntry en = new OcTreeEntry(tupl, targetPageNumber, rowIndex);
//					System.out.println(en);
					index.remove(v.get(i));
					index.insert(en);
					i++;
					this.serIndex(index);
				}
			}
			t.serPage(p);
			p = null;
			System.gc();
		} catch (ParseException e) {
			throw new DBAppException("error: "+e.getMessage());
		}
		//-------------------------------------- update the csv file then serialize the table ------------------------------------------------
		this.updateTableFile(t);
		this.serTable(t);
		t = null;
		System.gc();
	}
	
	public boolean checkTuple(Hashtable<String,Object> htblColNameValue, Hashtable<String,Object> query) {
		boolean checker = true;
		for(String key : query.keySet()) {
//			System.out.println(htblColNameValue.get(key));
//			System.out.println(query.get(key));
			if(!(htblColNameValue.get(key) == null)){
				if(!(htblColNameValue.get(key).equals(query.get(key)))) {
					checker = false;
				}
			}
		}
		return checker;
	}
	
	public void refactorPageFiles(Table t, int start) {
		if(start > t.getPagesCount()) {
			t.setPagesCount(t.getPagesCount()-1);
			return;
		}
		for(int i = start; i <= t.getPagesCount(); i++) {
			Page p = t.deSerPage(i);
//			System.out.println("in for page: "+p.getPageNumber());
			p.setPageNumber(i-1);
			t.serPage(p);
			p = null;
			System.gc();
			File f = new File("src/main/resources/data/" + t.getStrTableName() + "_page_" + i + ".ser");
			if(f.renameTo(new File("src/main/resources/data/" + t.getStrTableName() + "_page_" + (i-1) + ".ser"))) {
				System.out.println("renamed successfully");
			} else {
				t.deletePage(i);
//				System.out.println("renamed bel3afya successfully");
			}
			
		}
		t.setPagesCount(t.getPagesCount()-1);
	}
	
	public void deleteFromTable(String strTableName, 
			Hashtable<String,Object> htblColNameValue) 
					throws DBAppException {
		//-------------------- throw an exception if the table where the record will be deleted doesn't exist --------------------------------
		if(!this.isTable(strTableName)) {
			throw new DBAppException("the table " + strTableName + " does not exist in the database");
		}
		//-------------------------------------- if the table has no pages do nothing --------------------------------------------------------
		Table t = this.deSerTable(strTableName);
		if(t.getPagesCount() == 0) {
			throw new DBAppException("you're trying to delete from a table with no pages");
		}
		//----------------------------------------------- check then delete ------------------------------------------------------------------
		for(String key: htblColNameValue.keySet()) {
			if(!t.getHtblColNameType().containsKey(key)) throw new DBAppException("wrong column name entered: "+key);
			else {
				if(! (htblColNameValue.get(key).getClass()+"").split(" ")[1].equals(t.getHtblColNameType().get(key))) {
					throw new DBAppException("wrong datatype for column: "+key);
				}
			}
		}
		this.checkEntryMinMax(htblColNameValue, strTableName);
		boolean exists = false;
		int pageShift = 0;
		// ----------------------------------------- use index to get rows to be deleted if possible -----------------------------------------
		boolean useIndex = htblColNameValue.size() == 3;
		String[] inCols = new String[htblColNameValue.size()];
		int c = 0;
		for(String key : htblColNameValue.keySet()) {
			inCols[c] = key;
			c++;
		}
//		System.out.println("isIndex => "+(boolean)this.isIndex(strTableName, inCols)[0]);
		useIndex = useIndex && (boolean)this.isIndex(strTableName, inCols)[0];
		String indxName = (String)this.isIndex(strTableName, inCols)[1];
//		System.out.println(useIndex+" <= useIndex");
		if(useIndex) {
//			System.out.println("using the index to delete");
			Octree2 indx = this.deSerIndex(indxName);
			Object[] query = new Object[6]; 
			int i = 0;
			for(String key : indx.getColNames()) {
				query[i] = "=";
				query[i+1] = htblColNameValue.get(key);
				i+=2;
			}
			exists = indx.search(query).size() > 0;
			for(OcTreeEntry oEn : indx.search(query)) {
//				exists = true;
				indx.remove(oEn);
				Page target = t.deSerPage(oEn.pageNum);
				target.getData().remove(oEn.rowNum);
				for(int j = oEn.rowNum; j<target.getSize(); j++) {
					Object[] tupl = new Object[3];
					tupl[0] = target.getData().get(j).get(indx.getColNames()[0]);
					tupl[1] = target.getData().get(j).get(indx.getColNames()[1]);
					tupl[2] = target.getData().get(j).get(indx.getColNames()[2]);
//					System.out.println(target.getData().get(j));
//					System.out.println(target.getData());
					OcTreeEntry old = new OcTreeEntry(tupl, oEn.pageNum, j+1);
					OcTreeEntry neu = new OcTreeEntry(tupl, oEn.pageNum, j);
//					System.out.println(old);
//					System.out.println(neu);
					indx.update(old, neu);
				}
				t.serPage(target);
				
			}
			this.serIndex(indx);
			if(!exists) throw new DBAppException("a row with the value you entered doesn't exist");
			this.updateTableFile(t);
			this.serTable(t);
			t = null;
			System.gc();
			return;
		}
//		System.out.println("linear sacnning ...");
		// -----------------------------------------------------------------------------------------------------------------------------------
		for(int i = 1; i <= t.getPagesCount(); i++) { // loop on all pages
			Page p = t.deSerPage(i);
			int rowShift = 0;
			for(int j = 0; j < p.getSize(); j++) { // loop on each rows of each page and delete if the conditions are met
				if(this.checkTuple(p.getData().get(j), htblColNameValue)) {
					exists = true;
					// remove from the indices on the table
					for(String indexName : t.getIndicesNames()) {
						Octree2 o = this.deSerIndex(indexName);
						Object[] tupl = new Object[3];
						tupl[0] = p.getData().get(j).get(o.getColNames()[0]);
						tupl[1] = p.getData().get(j).get(o.getColNames()[1]);
						tupl[2] = p.getData().get(j).get(o.getColNames()[2]);
						OcTreeEntry en = new OcTreeEntry(tupl, i+pageShift, j+rowShift);
//						System.out.println(en);
						o.remove(en);
						this.serIndex(o);
						o = null;
						System.gc();
					}
					p.getData().remove(j);
					rowShift++;
//					System.out.println("tuple removed");
					j--; 
				}
			}
			// check if the page is empty the delete and refactor next pages
			if(p.getSize() == 0) {
				int start = p.getPageNumber() + 1;
//				System.out.println(p.getPageNumber());
				t.deletePage(p.getPageNumber());
				this.refactorPageFiles(t, start);
//				System.out.println("refactored");
				i--;
				pageShift++;
			} else {
				t.serPage(p);
				p = null;
				System.gc();
			}
		}
		if(!exists) throw new DBAppException("a row with the value you entered doesn't exist");
		this.updateTableFile(t);
		this.serTable(t);
		t = null;
		System.gc();
	}

//	---------------------------------------------- MILESTONE 2 ----------------------------------------------------------------------------------
	public static boolean evaluateBoolExpression(ArrayList<Object> boolExpression) throws DBAppException {
		boolean result = (boolean) boolExpression.get(0);

	    for (int i = 1; i < boolExpression.size(); i += 2) {
	        String operator = ((String) boolExpression.get(i)).toLowerCase();
	        boolean operand = (boolean) boolExpression.get(i + 1);

	        switch (operator) {
	            case "and":
	                result = result && operand;
	                break;
	            case "or":
	                result = result || operand;
	                break;
	            case "xor":
	                result = result ^ operand;
	                break;
	            default:
	                throw new DBAppException("Invalid operator: " + operator);
	        }
	    }
	    return result;
	}
	
	
	public static boolean compareTupleAndQuery(Hashtable<String,Object> row , SQLTerm[] query, String[] strarrOperators) throws DBAppException {
    	// {>,10,and,<,8,or,=11}     {false,and,true,or,true}
		ArrayList<Object> boolExpression = new ArrayList<>();
		boolean res = true;
    	int j = 0;
    	for(SQLTerm term : query) {
    		if(term.get_strOperator().equals(">")) {
//    			System.out.println(col+" > ?  "+query[i]);
        		res = Octree2.CompareTo(row.get(term.get_strColumnName()), term.get_objValue()) > 0;
        	} else if (term.get_strOperator().equals("<")) {
//        		System.out.println(col+"  < ? "+query[i]);
        		res = Octree2.CompareTo(row.get(term.get_strColumnName()), term.get_objValue()) < 0;
        	} else if (term.get_strOperator().equals("<=")) {
//        		System.out.println(query[1]+" & "+range[0]+" ::= "+ (CompareTo(query[1], range[1]) <= 0));
        		
        		res = Octree2.CompareTo(row.get(term.get_strColumnName()), term.get_objValue()) <= 0;
        	} else if (term.get_strOperator().equals(">=")) {
//        		System.out.println(query[1]+" & "+range[0]+" ::= "+ (CompareTo(query[1], range[1]) <= 0 && CompareTo(query[1], range[0]) >= 0));
        		
        		res = Octree2.CompareTo(row.get(term.get_strColumnName()), term.get_objValue()) >= 0;
        	} else if (term.get_strOperator().equals("=")) {
//        		System.out.println(col+"  = ? "+query[i] + "   "+(CompareTo(col, query[i]) == 0));
        		res = Octree2.CompareTo(row.get(term.get_strColumnName()), term.get_objValue()) == 0;
        	} else {
        		res = Octree2.CompareTo(row.get(term.get_strColumnName()), term.get_objValue()) != 0;
        	}
    		boolExpression.add(res);
    		if(j < strarrOperators.length) boolExpression.add(strarrOperators[j]);
    		j++;
    	}
//    	for(Object ff : boolExpression) {
//    		System.out.print(ff+" ");
//    	}
    	return evaluateBoolExpression(boolExpression);
    	
    }
	
	
	public Vector<Hashtable<String,Object>> scanTable(Table t, SQLTerm[] query, String[] strarrOperators) throws DBAppException{
		Vector<Hashtable<String,Object>> result = new Vector<>();
		for(int i = 1; i<=t.getPagesCount(); i++) {
			Page p = t.deSerPage(i);
			for(Hashtable<String, Object> row : p.getData()) {
				if(compareTupleAndQuery(row, query, strarrOperators)) result.add(row);
			}
		}
		return result;
	}
	
	public Object[] prepareQuery(SQLTerm[] arrSQLTerms, String indexName) throws DBAppException {
		Octree2 o = this.deSerIndex(indexName);
		Object[] query = new Object[6];
		for(SQLTerm term : arrSQLTerms) {
			if(term.get_strColumnName().equals(o.getColNames()[0])) {
				query[0] = term.get_strOperator();
				query[1] = term.get_objValue();
			} else if(term.get_strColumnName().equals(o.getColNames()[1])) {
				query[2] = term.get_strOperator();
				query[3] = term.get_objValue();
			} else {
				query[4] = term.get_strOperator();
				query[5] = term.get_objValue();
			}
		}
		return query;
	}
	
	@SuppressWarnings("rawtypes")
	public Iterator selectFromTable(SQLTerm[] arrSQLTerms, 
			 String[] strarrOperators) 
			throws DBAppException {
		// ----------------------------------------- check if SQLTerm[] arrSQLTerms is empty -----------------------------------------------------
		if(arrSQLTerms.length == 0) throw new DBAppException("can't perform select with empty SQLTerms");
		// ----------------------------------- check that all SQLTerms have the same tableName ---------------------------------------------------
		String tableName = arrSQLTerms[0].get_strTableName();
		for(int i = 1; i<arrSQLTerms.length; i++) {
			if(!tableName.equals(arrSQLTerms[i].get_strTableName())) {
				throw new DBAppException("can't perform selection from multiple tables");
			}
		}
		// ----------------------------------- check that the table selected from exists ---------------------------------------------------------
		if(!this.isTable(tableName)) throw new DBAppException("the table "+tableName+" doesn't exist");
		String[] colNames = new String[arrSQLTerms.length];
		int i = 0;
		for(SQLTerm term : arrSQLTerms) {
			colNames[i] = term.get_strColumnName();
			i++;
		}
		// ----------------------------------- check that the columns of the query are valid -----------------------------------------------------
		ArrayList<String> tableCols = new ArrayList<>();
		tableCols.addAll(this.deSerTable(tableName).getHtblColNameMax().keySet());
		for(SQLTerm term : arrSQLTerms) {
			if(!tableCols.contains(term.get_strColumnName())) throw new DBAppException("invalid column name: "+term.get_strColumnName());
		}
		// ----------------------------------- check that all comparison operators are valid -----------------------------------------------------
		ArrayList<String> compareOps = new ArrayList<>();
		String[] validComOps = {">","<","=","!=",">=","<="};
		for(String op : validComOps) {
			compareOps.add(op);
		}
		for(SQLTerm term : arrSQLTerms) {
			if(!compareOps.contains(term.get_strOperator())) throw new DBAppException("invalid comparison operator: "+term.get_strOperator());
		}
		// ----------------------------------- check that SQLTerms columns datatypes are correct -------------------------------------------------
		for(SQLTerm term : arrSQLTerms) {
			String colType = (term.get_objValue().getClass()+"").split(" ")[1];
			if(!(colType).equals((this.deSerTable(tableName).getHtblColNameType().get(term.get_strColumnName())))) {
				throw new DBAppException("invalid datatype ("+colType+") for the column: "+term.get_strColumnName());
			}
		}
		// --------------------------- check that all logical operators are one of the following : and, or, xor ----------------------------------
		ArrayList<String> validOps = new ArrayList<>();
		validOps.add("and");
		validOps.add("or");
		validOps.add("xor");
		for(String operator : strarrOperators) {
			if(!validOps.contains(operator.toLowerCase())) throw new DBAppException("invalid logical operator : "+operator);
		}
		// --------------------------- check that the number of logical operators is correct compared to SQLTerms --------------------------------
		if(arrSQLTerms.length - strarrOperators.length != 1) throw new DBAppException("number of logical operators must be less than SQLTerms by 1");
		// ---------------------------------- check that all SQLterms are anded ------------------------------------------------------------------
		boolean operatorsChecker = (strarrOperators.length == 2);
		for(String op : strarrOperators) {
			if(!(op.toLowerCase()).equals("and")) {
				operatorsChecker = false;
			}
		}
		// ---------------------------------- check that all SQLterms are indexed and anded ------------------------------------------------------
		if(colNames.length == 3 && (boolean)this.isIndex(tableName, colNames)[0] && operatorsChecker) {
			String indexName = (String)this.isIndex(tableName, colNames)[1];
			Object[] query = this.prepareQuery(arrSQLTerms, indexName);
			// use the octree to answer the query
			ResultSet r = new ResultSet();
			// add to the result set and return it
			for(OcTreeEntry e : (this.deSerIndex(indexName).search(query))) {
				r.addToResults(e, this.deSerTable(tableName));
			}
			return r;
		} else { // use linear scanning to answer the query
			ResultSet r = new ResultSet();
			r.addToResults(this.scanTable(this.deSerTable(tableName), arrSQLTerms, strarrOperators));
			return r;
		}
	}
	
	
// 	BONUS METHOD -----------------------------------------------------------------------------------------------------------------------------------
	
	
	
	@SuppressWarnings("rawtypes")
	public Iterator parseSQL( StringBuffer strbufSQL ) throws
	DBAppException {
		// -------------------------------- parser creation steps -----------------------------------------------------------------
		String sqlStatement = strbufSQL.toString().toUpperCase();
		CharStream stream = new ANTLRInputStream(sqlStatement);
		SQLiteLexer l = new SQLiteLexer(stream);
		CommonTokenStream tokens = new CommonTokenStream(l);
		SQLiteParser parser = new SQLiteParser(tokens);
		// ----------------------------------- DBAppParser ------------------------------------------------------------------------
		DBAppSQLParser par = new DBAppSQLParser();
		if(parser.sql_stmt().create_table_stmt() != null) {
//			System.out.println("this is a create table statement");
			tokens.seek(0);
			par.validateCreateTableStatement(parser.create_table_stmt());
			tokens.seek(0);
			par.createTableParse(parser.create_table_stmt());
			this.createTable(par.createTableName, par.createTableClstrKey, par.colNameType, par.colNameMin, par.colNameMax);
//			System.out.println("created the table successfully");
			return null;	
		} 
		tokens.seek(0);
		if (parser.sql_stmt().create_index_stmt() != null) {
//			System.out.println("this is a create index statement");
			tokens.seek(0);
			par.validateCreateIndex(parser.create_index_stmt());
			tokens.seek(0);
			par.setCreateIndexInputs(par.createIndexParse(parser.create_index_stmt()));
			this.createIndex(par.indexTableName, par.indexColumns);
//			System.out.println("created the index successfully");
			return null;	
		} 
		tokens.seek(0);
		if (parser.sql_stmt().insert_stmt() != null) {
//			System.out.println("this is an insert statement");
			tokens.seek(0);
			par.validateInsertStatement(parser.insert_stmt());
			tokens.seek(0);
			par.insertParse(parser.insert_stmt(), this);
			this.insertIntoTable(par.insertTargetTable, par.insertedRow);
//			System.out.println("inserted the row successfully");
			return null;		
		} 
		tokens.seek(0);
		if (parser.sql_stmt().update_stmt() != null) {
//			System.out.println("this is an update statement");
			tokens.seek(0);
			par.validateUpdateStatement(parser.update_stmt());
			tokens.seek(0);
			par.setUpdateInputs(par.updateParse(parser.update_stmt()), this);
			this.updateTable(par.updateTargetTable, par.updateTargetClustrKey, par.updateConditions);
//			System.out.println("updated the row successfully");
			return null;		
		} 
		tokens.seek(0);
		if (parser.sql_stmt().delete_stmt() != null) {
//			System.out.println("this is a delete statement");
			tokens.seek(0);
			par.validateDeleteStatement(parser.delete_stmt());
			tokens.seek(0);
			par.setDeleteInputs(par.deleteParse(parser.delete_stmt(), null), this);
			this.deleteFromTable(par.deleteTargetTable, par.deleteConditions);
//			System.out.println("deleted the row successfully");
			return null;			
		} 
		tokens.seek(0);
		if (parser.sql_stmt().select_stmt() != null) {
//			System.out.println("this is a select statement");
			tokens.seek(0);
			par.validateSelectStatement(parser.select_stmt().select_core().get(0));
			tokens.seek(0);
			par.setSelectInputs(par.selectParse(parser.select_stmt().select_core(), null), this);
//			System.out.println("select executed successfully");
			return this.selectFromTable(par.arrTerms, par.operators);
		}
		throw new DBAppException("this statement can't be parsed by this DB engine");
	}

	public static void main(String[] args) {
				
	}

}
