package main.engine;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import main.grammar.*;
import main.grammar.SQLiteParser.Column_constraintContext;
import main.grammar.SQLiteParser.Column_defContext;
import main.grammar.SQLiteParser.Column_nameContext;
import main.grammar.SQLiteParser.Create_index_stmtContext;
import main.grammar.SQLiteParser.Create_table_stmtContext;
import main.grammar.SQLiteParser.Delete_stmtContext;
import main.grammar.SQLiteParser.ExprContext;
import main.grammar.SQLiteParser.Indexed_columnContext;
import main.grammar.SQLiteParser.Insert_stmtContext;
import main.grammar.SQLiteParser.Select_coreContext;
import main.grammar.SQLiteParser.Select_stmtContext;
import main.grammar.SQLiteParser.Update_stmtContext;
import main.grammar.SQLiteParser.Value_rowContext;

public class DBAppSQLParser {
	SQLTerm[] arrTerms;
	String[] operators;
	String[] indexColumns;
	String indexTableName;
	String deleteTargetTable;
	Hashtable<String, Object> deleteConditions;
	ArrayList<String> deleteOperators;
	String updateTargetTable;
	Hashtable<String, Object> updateConditions;
	String updateTargetClustrKey;
	String insertTargetTable;
	Hashtable<String, Object> insertedRow;
	String createTableName;
	String createTableClstrKey;
	Hashtable<String, String> colNameType;
	Hashtable<String, String> colNameMin;
	Hashtable<String, String> colNameMax;
	
	public void validateCreateIndex(Create_index_stmtContext tree) throws DBAppException {
		if(tree.schema_name() != null || tree.expr() != null || tree.EXISTS_() != null || tree.UNIQUE_() != null) {
			throw new DBAppException("this SQL statement is invalid");
		}
	}
	
	public void validateInsertStatement(Insert_stmtContext tree) throws DBAppException {
		String statement = tree.getText().toLowerCase();
		if(!statement.startsWith("insertinto") || tree.schema_name() != null || tree.select_stmt() != null || tree.table_alias() != null
		|| tree.upsert_clause() != null || tree.returning_clause() != null) {
			throw new DBAppException("this SQL statement is invalid");
		}
	}
	
	public void validateUpdateStatement(Update_stmtContext tree) throws DBAppException {
		String statement = tree.getText().toLowerCase();
		if(!statement.startsWith("update"+tree.qualified_table_name().getText().toLowerCase()) || tree.returning_clause() != null
		|| tree.table_or_subquery() != null || tree.join_clause() != null || tree.expr().size() != 2
		|| (tree.expr().get(1).getChild(0).getChildCount() == 3 && tree.expr().get(1).getChild(2).getChildCount() == 3)) {
			throw new DBAppException("this SQL statement is invalid");
		}
	}
	
	public void validateDeleteStatement(Delete_stmtContext tree) throws DBAppException {
		String statement = tree.getText().toLowerCase();
		if(!statement.startsWith("deletefrom") || tree.returning_clause() != null) {
			throw new DBAppException("this SQL statement is invalid");
		}
	}
	
	public void validateSelectStatement(Select_coreContext tree) throws DBAppException {
		String statement = tree.getText().toLowerCase();
		if(!statement.startsWith("select*from"+tree.table_or_subquery().get(0).table_name().getText().toLowerCase()) || tree.join_clause() != null || tree.groupByExpr != null
		|| tree.GROUP_() != null || tree.BY_() != null || tree.havingExpr != null || tree.HAVING_() != null) {
			throw new DBAppException("this SQL statement is invalid");
		}
	}
	
	public void validateCreateTableStatement(Create_table_stmtContext tree) throws DBAppException {
		String statement = tree.getText().toLowerCase();
		if(!statement.startsWith("createtable"+tree.table_name().getText().toLowerCase()) || tree.AS_() != null || tree.select_stmt() != null) {
			throw new DBAppException("this SQL statement is invalid");
		}
	}
	
	public void createTableParse(Create_table_stmtContext tree) {
		this.colNameType = new Hashtable<>();
		this.colNameMin = new Hashtable<>();
		this.colNameMax = new Hashtable<>();
//		System.out.println(tree.getText());
		this.createTableName = tree.table_name().getText().toLowerCase();
		for(Column_defContext s : tree.column_def()) {
			String type = "";
			switch(s.type_name().getText().toLowerCase()) {
			case "int": type = "java.lang.Integer"; break;
			case "double": type = "java.lang.Double"; break;
			case "varchar": type = "java.lang.String"; break;
			case "date": type = "java.util.Date"; break;
			default: type = "";
			}
			this.colNameType.put(s.column_name().getText().toLowerCase(), type);
			for(Column_constraintContext t : s.column_constraint()) {
				if(t.PRIMARY_() != null) this.createTableClstrKey = s.column_name().getText().toLowerCase();
				if(t.expr() != null) {
					String min = t.expr().getChild(0).getChild(2).getText();
					if(type.equals("java.lang.String")) min = t.expr().getChild(0).getChild(2).getText().substring(1, t.expr().getChild(0).getChild(2).getText().length()-1);
					this.colNameMin.put(s.column_name().getText().toLowerCase(), min);
					String max = t.expr().getChild(2).getChild(2).getText();
					if(type.equals("java.lang.String")) max = t.expr().getChild(2).getChild(2).getText().substring(1, t.expr().getChild(2).getChild(2).getText().length()-1);
					this.colNameMax.put(s.column_name().getText().toLowerCase(), max);
				}
			}
		}
	}
	
	public void insertParse(Insert_stmtContext tree, DBApp db) throws DBAppException {
		this.insertTargetTable = tree.table_name().getText().toLowerCase();
		List<ExprContext> p = tree.values_clause().value_row().get(0).expr();
		Hashtable<String, Object> result = new Hashtable<>();
		for(int i = 0; i<tree.column_name().size(); i++) {
			if(p.size() != tree.column_name().size()) {
				System.out.println("incompatible columns and values");
				return ;
			}
			String key = tree.column_name().get(i).getText().toLowerCase();
			Table t = db.deSerTable(this.insertTargetTable);
			switch(t.getHtblColNameType().get(key)) {
				case "java.lang.Integer": result.put(key, Integer.parseInt(p.get(i).getText())); break;
				case "java.lang.Double": result.put(key, Double.parseDouble(p.get(i).getText())); break;
				case "java.util.Date": try {
					result.put(key, new SimpleDateFormat("yyyy-MM-dd").parse(p.get(i).getText()));
				} catch (ParseException e) {
					throw new DBAppException("error: "+e.getMessage());
				} break;
				case "java.lang.String": result.put(key, p.get(i).getText().toLowerCase().substring(1, p.get(i).getText().length()-1));
			}
			
		}
		if (tree.column_name().size() == 0) {
			System.out.println("normal insert");
			Table t = db.deSerTable(this.insertTargetTable);
			int j = 0;
			for(String key : t.getHtblColNameType().keySet()) {
				if (j < p.size()) {
					result.put(key, p.get(j).getText());
					j++;
				} else {
					break;
				}
			}
		}
		this.insertedRow = result;
	}
	
	public Hashtable<String, Object> updateParse(Update_stmtContext tree){
		Hashtable<String, Object> result = new Hashtable<>();
		this.updateTargetTable = tree.qualified_table_name().getText().toLowerCase();
		if(tree.expr().get(tree.expr().size()-1).getChildCount() == 3 && tree.expr().get(tree.expr().size()-1).getChild(0).getChildCount() != 3) {
			this.updateTargetClustrKey = tree.expr().get(tree.expr().size()-1).getChild(2).getText();
			ArrayList<String> keys = new ArrayList<>();
			ArrayList<Object> values = new ArrayList<>();
			for(Column_nameContext col : tree.column_name()) {
				keys.add(col.getText().toLowerCase());
			}
			for(ExprContext t : tree.expr()) {
				values.add(t.getText());
			}
			for(int i = 0; i<keys.size(); i++) {
				result.put(keys.get(i), values.get(i));
			}
		}
		return result;
	}
	
	public void setUpdateInputs(Hashtable<String, Object> conditions, DBApp db) throws DBAppException {
		Table t = db.deSerTable(this.updateTargetTable);
		this.deleteConditions = new Hashtable<>();
		if(t.getHtblColNameType().get(t.getStrClusteringKeyColumn()).equals("java.lang.String")) {
			this.updateTargetClustrKey = this.updateTargetClustrKey.substring(1, this.updateTargetClustrKey.length()-1);
		}
		for(String key : conditions.keySet()) {
			switch(t.getHtblColNameType().get(key)) {
				case "java.lang.Integer": conditions.put(key, Integer.parseInt(conditions.get(key).toString())); break;
				case "java.lang.Double": conditions.put(key, Double.parseDouble(conditions.get(key).toString())); break;
				case "java.util.Date": try {
					conditions.put(key, new SimpleDateFormat("yyyy-MM-dd").parse(conditions.get(key).toString()));
				} catch (ParseException e) {
					throw new DBAppException("error: "+e.getMessage());
				}
				case "java.lang.String": conditions.put(key, conditions.get(key).toString().substring(1, conditions.get(key).toString().length()-1));
			}
		}
		this.updateConditions = conditions;
	}
	
	public static Hashtable<String, Object> mergeHashtables(Hashtable<String, Object> dest, Hashtable<String, Object> src) {
	    for (String key : src.keySet()) {
	        dest.put(key, src.get(key));
	    }
	    return dest;
	}
	
	public Hashtable<String, Object> deleteParse(Delete_stmtContext tree, ParseTree p){
		this.deleteOperators = new ArrayList<>();
		this.deleteTargetTable = tree.qualified_table_name().getText().toLowerCase();
		Hashtable<String, Object> result = new Hashtable<>();
		if (p == null) p = tree.expr();
		for(int i = 0; tree.expr() != null && i<p.getChildCount(); i++) {
			if(p.getChildCount() == 3 && p.getChild(0).getChildCount() == 1 && p.getChild(2).getChildCount() == 1) {
    			result.put(p.getChild(0).getText().toLowerCase(), p.getChild(2).getText().toLowerCase());
    			this.deleteOperators.add(p.getChild(1).getText());
    			return result;
    		}
			if(p.getChild(i).getChild(0)!= null && p.getChild(i).getChild(0).getChildCount() == 3) {
				result = mergeHashtables(result, this.deleteParse(tree, p.getChild(0)));
			} else {
//				if (i == 1) this.deleteOperators.add(p.getChild(i).getText());
				if (i != 1) result.put(p.getChild(i).getChild(0).getText().toLowerCase(), p.getChild(i).getChild(2).getText().toLowerCase());
				else this.deleteOperators.add(p.getChild(i).getText());
//    			this.deleteOperators.add(p.getChild(i).getChild(1).getText());
			}
		}
		return result;
	}
	
	public void setDeleteInputs(Hashtable<String, Object> conditions, DBApp db) throws DBAppException {
		Table t = db.deSerTable(this.deleteTargetTable);
		this.deleteConditions = new Hashtable<>();
		for(String key : conditions.keySet()) {
			switch(t.getHtblColNameType().get(key)) {
				case "java.lang.Integer": conditions.put(key, Integer.parseInt(conditions.get(key).toString())); break;
				case "java.lang.Double": conditions.put(key, Double.parseDouble(conditions.get(key).toString())); break;
				case "java.util.Date": try {
					conditions.put(key, new SimpleDateFormat("yyyy-MM-dd").parse(conditions.get(key).toString()));
				} catch (ParseException e) {
					throw new DBAppException("error: "+e.getMessage());
				}
				case "java.lang.String": conditions.put(key, conditions.get(key).toString().substring(1, conditions.get(key).toString().length()-1));
			}
		}
		this.deleteConditions = conditions;
	}
	
	public String[] createIndexParse(Create_index_stmtContext tree) {
		String[] inputs = new String[4];
		inputs[0] = tree.table_name().getText();
		int i = 1;
		for(Indexed_columnContext s : tree.indexed_column()) {
			if(i < 4) inputs[i] = s.getText();
			i++;
		}
		return inputs;
	}
	
	public void setCreateIndexInputs(String[] inputs) {
		this.indexTableName = inputs[0].toLowerCase();
		this.indexColumns = new String[3];
		for(int i = 1; i<4; i++) {
			this.indexColumns[i-1] = inputs[i].toLowerCase();
		}
	}
	
	public ArrayList<String> selectParse(List<Select_coreContext> t, ParseTree p){
		ArrayList<String> cols = new ArrayList<>();
        String tableName = "";
        for(Select_coreContext node : t) {
        	if(p == null) {
        		p = node.whereExpr;
            	tableName = node.table_or_subquery().get(0).table_name().getText();
            	cols.add(tableName);
        	} 
        	for(int i = 0; i<p.getChildCount(); i++) {
//        		System.out.println("i: "+i+", "+p.getText());
//        		System.out.println(p.getChildCount());
        		if(p.getChildCount() == 3 && p.getChild(0).getChildCount() == 1 && p.getChild(2).getChildCount() == 1) {
        			cols.add(p.getChild(0).getText());
    				cols.add(p.getChild(1).getText());
    				cols.add(p.getChild(2).getText());
    				return cols;
        		}
        		if(p.getChild(i).getChild(0)!= null && p.getChild(i).getChild(0).getChildCount() == 3) {
//        			System.out.println("helloz");
        			cols.addAll(selectParse(t, p.getChild(i)));
        		} else {
        			if(i == 1) cols.add(p.getChild(i).getText());
        			else {
//        				System.out.println(p.getChild(i).getChild(2).getText());
        				cols.add(p.getChild(i).getChild(0).getText());
        				cols.add(p.getChild(i).getChild(1).getText());
        				cols.add(p.getChild(i).getChild(2).getText());
        			} 
        		}
        	}

        }
        return cols;
	}
	
	public void setSelectInputs(ArrayList<String> cols, DBApp db) throws DBAppException {
		ArrayList<String> ops2 = new ArrayList<>();
		int termsSize = 0;
		for(int i = 1; i<cols.size(); i+=4) {
			termsSize++;
		}
    	this.arrTerms = new SQLTerm[termsSize];
    	for(int i = 1; i<cols.size(); i+=4) {
    		Table t = db.deSerTable(cols.get(0).toLowerCase());
    		SQLTerm s = null;
    		String kol = t.getHtblColNameType().get(cols.get(i).toLowerCase());
    		switch(kol) {
    			case "java.lang.String":  s = new SQLTerm(cols.get(0).toLowerCase(), cols.get(i).toLowerCase(), cols.get(i+1), cols.get(i+2).substring(1, cols.get(i+2).length()-1)); break;
    			case "java.lang.Integer": s = new SQLTerm(cols.get(0).toLowerCase(), cols.get(i).toLowerCase(), cols.get(i+1), Integer.parseInt(cols.get(i+2))); break;
    			case "java.lang.Double": s = new SQLTerm(cols.get(0).toLowerCase(), cols.get(i).toLowerCase(), cols.get(i+1), Double.parseDouble(cols.get(i+2))); break;
    			default: try {
					s = new SQLTerm(cols.get(0).toLowerCase(), cols.get(i).toLowerCase(), cols.get(i+1), new SimpleDateFormat("yyyy-MM-dd").parse(cols.get(i+2)));
				} catch (ParseException e) {
					throw new DBAppException("error: "+e.getMessage());
				}
    		}
        	if (i+3 < cols.size()) ops2.add(cols.get(i+3));
        	arrTerms[(i-1)/4] = s;
        }
    	this.operators = new String[ops2.size()];
        String[] ops = new String[ops2.size()];
        for(int i = 0; i<ops.length; i++) {
        	this.operators[i] = ops2.get(i);
        }
	}

	public static void main(String[] args) {
	

	}

}
