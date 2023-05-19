package main.engine;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;

@SuppressWarnings("serial")
public class Octree2 implements Serializable {
	
	private Vector<OcTreeEntry> data;
	private String tableName;
	private String indexName;
	private String[] colNames;
    private Octree2[] children;
    private Object[] ranges;
    private int maxEntriesPerNode;
    
    public Octree2(Object[] ranges, String[] colNames, String TableName, String indexName) throws DBAppException{
    	try {
    		FileInputStream in = new FileInputStream("src/main/resources/DBApp.properties");
    		
    		Properties prop = new Properties();
    		prop.load(in);
    		
    		String sizeProp = prop.getProperty("MaximumEntriesinOctreeNode");
    		this.maxEntriesPerNode = Integer.parseInt(sizeProp);
    	} catch (IOException e) {
    		throw new DBAppException("error with config file");
    	}
    	this.ranges = ranges;
    	this.colNames = colNames;
    	data = new Vector<>();
    	children = null;
    	this.tableName = TableName;
    	this.indexName = indexName;
    }
    
    public String getIndexName() {
		return indexName;
	}

	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}

	public String getTableName() {
		return tableName;
	}

	public int getMaxEntriesPerNode() {
		return maxEntriesPerNode;
	}

	private boolean compareRangeAndQuery(Object[] range, Object[] query) throws DBAppException {
    	//Object[] query = {">",5,">","a","<",250};
    	if(query[0].equals(">")) {
//    		System.out.println(query[1]+" & "+range[0]+"-"+range[1]+" : "+ (CompareTo(query[1], range[1]) < 0));
    		
    		return CompareTo(query[1], range[1]) < 0;
    	} else if (query[0].equals("<")) {
//    		System.out.println(query[1]+" & "+range[0]+"-"+range[1]+" :: "+ (CompareTo(query[1], range[1]) < 0 && CompareTo(query[1], range[0]) > 0 || CompareTo(query[1], range[1]) > 0));
    		
    		return CompareTo(query[1], range[1]) < 0 && CompareTo(query[1], range[0]) > 0 || CompareTo(query[1], range[1]) > 0;
    	} else if (query[0].equals("<=")) {
//    		System.out.println(query[1]+" & "+range[0]+" ::= "+ (CompareTo(query[1], range[1]) <= 0));
    		
    		return CompareTo(query[1], range[1]) <= 0;
    	} else if (query[0].equals(">=")) {
//    		System.out.println(query[1]+" & "+range[0]+" ::= "+ (CompareTo(query[1], range[1]) <= 0 && CompareTo(query[1], range[0]) >= 0));
    		
    		return CompareTo(query[1], range[1]) <= 0;
    	} else if (query[0].equals("=")) { 
//    		System.out.println(query[1]+" & "+range[0]+"-"+range[1]+" :: "+(CompareTo(query[1], range[1]) <= 0));
    		return CompareTo(query[1], range[1]) <= 0;
    	}
    	return true;
    }
    
    private boolean isRangeSatisfied(Object[] ranges, Object[] queryValues) throws DBAppException {
    	boolean res = true;
    	for(int i = 0; i<6; i+=2) {
    		res = res && this.compareRangeAndQuery(Arrays.copyOfRange(ranges, i, i+2), Arrays.copyOfRange(queryValues, i, i+2));
    	}
    	return res;
    } 
    
    public static Vector<OcTreeEntry> filterData(Vector<OcTreeEntry> data, Object[] query) throws DBAppException{
    	Vector<OcTreeEntry> result = new Vector<>();
    	for(OcTreeEntry e : data) {
    		if(compareTupleAndQuery(e.tuple, query)) {
//    			System.out.println("added");
    			result.add(e);
    		}
    	}
    	return result;
    }
    
    public static boolean compareTupleAndQuery(Object[] tuple, Object[] query) throws DBAppException {
    	//Object[] query = {">",5,">","a","<",250};
//    	for(Object kol : tuple) {
//    		System.out.println("- "+kol);
//    	}
    	boolean res = true;
    	int i = 1;
    	for(Object col : tuple) {
    		if(query[i-1].equals(">")) {
//        		System.out.println(query[1]+" & "+tuple[0]+"-"+tuple[1]+" : "+ (CompareTo(query[1], tuple[1]) < 0));
//    			System.out.println(col+" > ?  "+query[i]);
        		res = CompareTo(col, query[i]) > 0;
        		if(!res) return res;
        	} else if (query[i-1].equals("<")) {
//        		System.out.println(query[1]+" & "+tuple[0]+"-"+tuple[1]+" :: "+ (CompareTo(query[1], tuple[1]) < 0 && CompareTo(query[1], tuple[0]) > 0 || CompareTo(query[1], tuple[1]) > 0));
//        		System.out.println(col+"  < ? "+query[i]);
        		res = CompareTo(col, query[i]) < 0;
        		if(!res) return res;
        	} else if (query[i-1].equals("<=")) {
//        		System.out.println(query[1]+" & "+range[0]+" ::= "+ (CompareTo(query[1], range[1]) <= 0));
        		
        		res = CompareTo(col, query[i]) <= 0;
        		if(!res) return res;
        	} else if (query[i-1].equals(">=")) {
//        		System.out.println(query[1]+" & "+range[0]+" ::= "+ (CompareTo(query[1], range[1]) <= 0 && CompareTo(query[1], range[0]) >= 0));
        		
        		res = CompareTo(col, query[i]) >= 0;
        		if(!res) return res;
        	} else if (query[i-1].equals("=")) {
//        		System.out.println(col+"  = ? "+query[i] + "   "+(CompareTo(col, query[i]) == 0));
        		res = CompareTo(col, query[i]) == 0;
        		if(!res) return res;
        	} else {
        		res = CompareTo(col, query[i]) != 0;
        		if(!res) return res;
        	}
    		i+=2;
    	}
    	return true;
    	
    }
    
    public Vector<OcTreeEntry> search(Object[] queryValues) throws DBAppException{
    	Vector<OcTreeEntry> res = new Vector<>();
    	if(this.isLeaf()) {
    		if(this.isRangeSatisfied(this.ranges, queryValues)) {
//    			System.out.println("yes man");
    			res.addAll(filterData(this.data, queryValues));
    			return res;
    		} else {
    			return res;
    		}
    	} else {
    		for(int i = 0; i<8; i++) {
//    			System.out.println("in "+i);
    			res.addAll(filterData(this.children[i].search(queryValues), queryValues));
    		}
    		return res;
    	}
    }
    
    public String printOcTree(int n) {
    	String res = "";
    	String line = "--------------------------------------------------------------------";
    	if(this.isLeaf()) {
    		String d = "";
    		String rngs = "";
    		for(int i = 0; i<4; i+=2) {
    			rngs += "( "+this.ranges[i]+" => "+this.ranges[i+1]+" ) & ";
    		}
    		rngs += "( "+this.ranges[4]+" => "+this.ranges[5]+" )";
    		d+= tabs(n-1)+rngs+ "\n"+"\n";
    		for(OcTreeEntry e : this.data) {
    			d += tabs(n-1)+e + "\n";
    		}
    		res = "node: {"+"\n"+"\n"+d+"\n"+ tabs(n-1)+"}";
    		return res;
    	} else {
    		res = "node: {"+"\n"+"\n"+ tabs(n)+"0) "+ this.children[0].printOcTree(n+1)+"\n"+tabs(n)+line+"\n"+ tabs(n)+"1) "+this.children[1].printOcTree(n+1)+"\n"+tabs(n)+line+"\n"+tabs(n)+
    				"2) "+this.children[2].printOcTree(n+1)+"\n"+tabs(n)+line+"\n"+ tabs(n)+"3) "+this.children[3].printOcTree(n+1)
    				+"\n"+tabs(n)+line+"\n"+ tabs(n)+"4) "+this.children[4].printOcTree(n+1)+"\n"+tabs(n)+line+"\n"+ tabs(n)+"5) "+this.children[5].printOcTree(n+1)
    				+"\n"+tabs(n)+line+"\n"+ tabs(n)+"6) "+this.children[6].printOcTree(n+1)+"\n"+tabs(n)+line+"\n"+ tabs(n)+"7) "+this.children[7].printOcTree(n+1)+"\n"+tabs(n-1)+"}";
    	}
    	return res;
    }
    
    public static String tabs(int n) {
    	String res = "";
    	for(int i = 0; i<n; i++) {
    		res += "\t";
    	}
    	return res;
    }
    
    public boolean remove(OcTreeEntry in) throws DBAppException {
    	if(this.isLeaf()) {
    		if(this.dataContains(in)) {
//				System.out.println("going in 4 remove");
				this.data.remove(in);
				return true;
			} else {
//				System.out.println("going in false");
				return false;
			}
    	}
    	if(CompareTo(in.getTuple()[0], GetMid(ranges[0], ranges[1])) < 0) {
//    		System.out.println("going in 1");
			if(CompareTo(in.getTuple()[1], GetMid(ranges[2], ranges[3])) < 0) {
//				System.out.println("going in 2");
				if(CompareTo(in.getTuple()[2], GetMid(ranges[4], ranges[5])) < 0) {
//					System.out.println("going in 3");
	    			if(!this.children[0].isLeaf()) {
//	    				System.out.println("going in 4");
	    				children[0].remove(in);
	    			} else {
	    				if(this.children[0].dataContains(in)) {
//	    					System.out.println("going in 4 remove");
	    					this.children[0].data.remove(in);
	    					return true;
	    				} else {
//	    					System.out.println("going in false");
	    					return false;
	    				}
	    			}
	    		} else {
	    			if(!this.children[2].isLeaf()) {
	    				children[2].remove(in);
	    			} else {
	    				if(this.children[2].dataContains(in)) {
	    					this.children[2].data.remove(in);
	    					return true;
	    				} else {
	    					return false;
	    				}
	    			}
	    		}
    		} else {
    			if(CompareTo(in.getTuple()[2], GetMid(ranges[4], ranges[5])) < 0) {
    				if(!this.children[4].isLeaf()) {
	    				children[4].remove(in);
	    			} else {
	    				if(this.children[4].dataContains(in)) {
	    					this.children[4].data.remove(in);
	    					return true;
	    				} else {
	    					return false;
	    				}
	    			}
        		} else {
        			if(!this.children[6].isLeaf()) {
	    				children[6].remove(in);
	    			} else {
	    				if(this.children[6].dataContains(in)) {
	    					this.children[6].data.remove(in);
	    					return true;
	    				} else {
	    					return false;
	    				}
	    			}
        		}
    		}
		} else {
			if(CompareTo(in.getTuple()[1], GetMid(ranges[2], ranges[3])) < 0) {
				if(CompareTo(in.getTuple()[2], GetMid(ranges[4], ranges[5])) < 0) {
					if(!this.children[1].isLeaf()) {
	    				children[1].remove(in);
	    			} else {
	    				if(this.children[1].dataContains(in)) {
	    					this.children[1].data.remove(in);
	    					return true;
	    				} else {
	    					return false;
	    				}
	    			}
	    		} else {
	    			if(!this.children[3].isLeaf()) {
	    				children[3].remove(in);
	    			} else {
	    				if(this.children[3].dataContains(in)) {
	    					this.children[3].data.remove(in);
	    					return true;
	    				} else {
	    					return false;
	    				}
	    			}
	    		}
    		} else {
    			if(CompareTo(in.getTuple()[2], GetMid(ranges[4], ranges[5])) < 0) {
    				if(!this.children[5].isLeaf()) {
	    				children[5].remove(in);
	    			} else {
	    				if(this.children[5].dataContains(in)) {
	    					this.children[5].data.remove(in);
	    					return true;
	    				} else {
	    					return false;
	    				}
	    			}
        		} else {
        			if(!this.children[7].isLeaf()) {
	    				children[7].remove(in);
	    			} else {
	    				if(this.children[7].dataContains(in)) {
	    					this.children[7].data.remove(in);
	    					return true;
	    				} else {
	    					return false;
	    				}
	    			}
        		}
    		}
		}
    	return false;
    }
       
    public static int CompareTo(Object keyofTheInsert, Object colkeyValue) throws DBAppException {
	    switch(keyofTheInsert.getClass().getSimpleName()) {
	        case "String":
	            if(colkeyValue instanceof String) {
	                return ((String)keyofTheInsert).compareTo((String)colkeyValue);
	            }
	            break;
	        case "Date":
	            if(colkeyValue instanceof Date) {
	                return ((Date)keyofTheInsert).compareTo((Date)colkeyValue);
	            }
	            break;
	        case "Double":
	            if(colkeyValue instanceof Double) {
	                if ((double)keyofTheInsert > (double)colkeyValue) {
//	                	System.out.println("in here");
	                    return 1;
	                } else if((double)keyofTheInsert < (double)colkeyValue) {
	                    return -1;
	                } else {
	                    return 0;
	                }
	            }
	            break;
	        case "Integer":
	            if(colkeyValue instanceof Integer) {
	                if ((int)keyofTheInsert > (int)colkeyValue) {
//	                	System.out.println("in here");
	                    return 1;
	                } else if((int)keyofTheInsert < (int)colkeyValue) {
	                    return -1;
	                } else {
	                    return 0;
	                }
	            }
	            break;
	    }
	    System.out.println(keyofTheInsert.getClass().getSimpleName()+" "+keyofTheInsert);
	    System.out.println(colkeyValue.getClass().getSimpleName()+" "+colkeyValue);
	    throw new DBAppException("invalid arguments types");
	}
    
    public void insert(OcTreeEntry in) throws DBAppException {
    	if(this.isLeaf() && this.isFull()) {
    		this.divideRanges();
    		Vector<OcTreeEntry> tmp = this.data;
    		for(OcTreeEntry e : tmp) {
    			this.insert(e);
    		}
    		this.insert(in);
    	} else if (this.isLeaf() && !this.isFull()) {
    		this.data.add(in);
    	} else {
    		this.findAndPut(in);
    	}
    }
    
    private boolean findAndPut(OcTreeEntry in) throws DBAppException {
    	if(CompareTo(in.getTuple()[0], GetMid(ranges[0], ranges[1])) < 0) {
			if(CompareTo(in.getTuple()[1], GetMid(ranges[2], ranges[3])) < 0) {
				if(CompareTo(in.getTuple()[2], GetMid(ranges[4], ranges[5])) < 0) {
	    			if(!this.children[0].isLeaf()) {
	    				children[0].findAndPut(in);
	    			} else {
	    				if(this.children[0].data.contains(in)) {
	    					return true;
	    				} else {
	    					this.children[0].insert(in);
	    					return false;
	    				}
	    			}
	    		} else {
	    			if(!this.children[2].isLeaf()) {
	    				children[2].findAndPut(in);
	    			} else {
	    				if(this.children[2].data.contains(in)) {
	    					return true;
	    				} else {
	    					this.children[2].insert(in);
	    					return false;
	    				}
	    			}
	    		}
    		} else {
    			if(CompareTo(in.getTuple()[2], GetMid(ranges[4], ranges[5])) < 0) {
    				if(!this.children[4].isLeaf()) {
	    				children[4].findAndPut(in);
	    			} else {
	    				if(this.children[4].data.contains(in)) {
	    					return true;
	    				} else {
	    					this.children[4].insert(in);
	    					return false;
	    				}
	    			}
        		} else {
        			if(!this.children[6].isLeaf()) {
	    				children[6].findAndPut(in);
	    			} else {
	    				if(this.children[6].data.contains(in)) {
	    					return true;
	    				} else {
	    					this.children[6].insert(in);
	    					return false;
	    				}
	    			}
        		}
    		}
		} else {
			if(CompareTo(in.getTuple()[1], GetMid(ranges[2], ranges[3])) < 0) {
				if(CompareTo(in.getTuple()[2], GetMid(ranges[4], ranges[5])) < 0) {
					if(!this.children[1].isLeaf()) {
	    				children[1].findAndPut(in);
	    			} else {
	    				if(this.children[1].data.contains(in)) {
	    					return true;
	    				} else {
	    					this.children[1].insert(in);
	    					return false;
	    				}
	    			}
	    		} else {
	    			if(!this.children[3].isLeaf()) {
	    				children[3].findAndPut(in);
	    			} else {
	    				if(this.children[3].data.contains(in)) {
	    					return true;
	    				} else {
	    					this.children[3].insert(in);
	    					return false;
	    				}
	    			}
	    		}
    		} else {
    			if(CompareTo(in.getTuple()[2], GetMid(ranges[4], ranges[5])) < 0) {
    				if(!this.children[5].isLeaf()) {
	    				children[5].findAndPut(in);
	    			} else {
	    				if(this.children[5].data.contains(in)) {
	    					return true;
	    				} else {
	    					this.children[5].insert(in);
	    					return false;
	    				}
	    			}
        		} else {
        			if(!this.children[7].isLeaf()) {
	    				children[7].findAndPut(in);
	    			} else {
	    				if(this.children[7].data.contains(in)) {
	    					return true;
	    				} else {
	    					this.children[7].insert(in);
	    					return false;
	    				}
	    			}
        		}
    		}
		}
    	return false;
    }
    
	public Vector<OcTreeEntry> getData() {
		return data;
	}

	public void setData(Vector<OcTreeEntry> data) {
		this.data = data;
	}

	public String[] getColNames() {
		return colNames;
	}

	public void setColNames(String[] colNames) {
		this.colNames = colNames;
	}

	public Octree2[] getChildren() {
		return children;
	}

	public void setChildren(Octree2[] children) {
		this.children = children;
	}

	public Object[] getRanges() {
		return ranges;
	}

	public void setRanges(Object[] ranges) {
		this.ranges = ranges;
	}
	
	public boolean isLeaf() {
		return this.children == null;
	}
	
	public boolean isFull() {
		return this.data.size() == this.maxEntriesPerNode;
	}
	
	public boolean dataContains(OcTreeEntry in) {
		boolean res = false;
		for(OcTreeEntry e : this.data) {
			if (e.equals(in)) res = true;
		}
		return res;
	}
	
	public static String findLexicographicMiddle(String str1, String str2) {
	    int minLength = Math.min(str1.length(), str2.length());
	    int i = 0;
	    while (i < minLength && str1.charAt(i) == str2.charAt(i)) {
	        i++;
	    }
	    if (i == minLength) {
	        // The common prefix of the two strings is the same as the shorter string
	        return minLength == str1.length() ? str1 : str2;
	    }
	    char middleChar = (char) ((str1.charAt(i) + str2.charAt(i)) / 2);
	    StringBuilder middle = new StringBuilder(str1.substring(0, i));
	    middle.append(middleChar);
	    return middle.toString();
	}
	
    public static Date findAverageDate(Date date1, Date date2) {
        long timestamp1 = date1.getTime();
        long timestamp2 = date2.getTime();
        long averageTimestamp = (timestamp1 + timestamp2) / 2;
        return new Date(averageTimestamp);
    }
	
	public static Object GetMid(Object x, Object y) throws DBAppException{
    	if(x instanceof String && y instanceof String ) {
    		return (Object) findLexicographicMiddle((String) x,(String) y);
    	}
    	else if(x instanceof Date && y instanceof Date) {
    		return (Object) findAverageDate((Date) x, (Date) y);
    	}
    	else if(x instanceof Integer && y instanceof Integer) {
    		int x2 = (int) x;
    		int y2 = (int) y;
    		int res = (x2+y2) /2;
    		return (Object) res;
    	}
    	else if(x instanceof Double && y instanceof Double) {
    		double x2 = (double) x;
    		double y2 = (double) y;
    		double res = (x2+y2) /2;
    		return (Object) res;
    	}
    	else {
    		throw new DBAppException("there is invalid data type");
    	}
    }

	private void divideRanges() throws DBAppException {
		Object midX = GetMid(ranges[0], ranges[1]);
		Object midY = GetMid(ranges[2], ranges[3]);
		Object midZ = GetMid(ranges[4], ranges[5]);
		
		this.children = new Octree2[8];
		Object[] r1 = {ranges[0],midX,ranges[2],midY,ranges[4],midZ};
		this.children[0] = new Octree2(r1, this.colNames, this.tableName, this.indexName);
		Object[] r2 = {midX,ranges[1],ranges[2],midY,ranges[4],midZ};
		this.children[1] = new Octree2(r2, this.colNames, this.tableName, this.indexName);
		Object[] r3 = {ranges[0],midX,ranges[2],midY,midZ, ranges[5]};
		this.children[2] = new Octree2(r3, this.colNames, this.tableName, this.indexName);
		Object[] r4 = {midX,ranges[1],ranges[2],midY,midZ,ranges[5]};
		this.children[3] = new Octree2(r4, this.colNames, this.tableName, this.indexName);
		Object[] r5 = {ranges[0],midX,midY,ranges[3],ranges[4],midZ};
		this.children[4] = new Octree2(r5, this.colNames, this.tableName, this.indexName);
		Object[] r6 = {midX,ranges[1],midY,ranges[3],ranges[4],midZ};
		this.children[5] = new Octree2(r6, this.colNames, this.tableName, this.indexName);
		Object[] r7 = {ranges[0],midX,midY,ranges[3],midZ,ranges[5]};
		this.children[6] = new Octree2(r7, this.colNames, this.tableName, this.indexName);
		Object[] r8 = {midX,ranges[1],midY,ranges[3],midZ,ranges[5]};
		this.children[7] = new Octree2(r8, this.colNames, this.tableName, this.indexName);
	}
	
	public boolean update(OcTreeEntry in, OcTreeEntry inNew) throws DBAppException {
	if(this.isLeaf()) {
		if(this.dataContains(in)) {
//			System.out.println("going in 4 remove");
			int pos = this.data.indexOf(in);
//			System.out.println(pos+" <=");
			this.data.setElementAt(inNew, pos);
			return true;
		} else {
//			System.out.println("going in false");
			return false;
		}
	}
	if(CompareTo(in.getTuple()[0], GetMid(ranges[0], ranges[1])) < 0) {
//		System.out.println("going in 1");
		if(CompareTo(in.getTuple()[1], GetMid(ranges[2], ranges[3])) < 0) {
//			System.out.println("going in 2");
			if(CompareTo(in.getTuple()[2], GetMid(ranges[4], ranges[5])) < 0) {
//				System.out.println("going in 3");
    			if(!this.children[0].isLeaf()) {
//    				System.out.println("going in 4");
    				children[0].update(in, inNew);
    			} else {
    				if(this.children[0].dataContains(in)) {
//    					System.out.println("going in 4 remove");
    					int pos = this.children[0].data.indexOf(in);
    					this.children[0].data.setElementAt(inNew, pos);
    					return true;
    				} else {
//    					System.out.println("going in false");
    					return false;
    				}
    			}
    		} else {
    			if(!this.children[2].isLeaf()) {
    				children[2].update(in, inNew);
    			} else {
    				if(this.children[2].dataContains(in)) {
    					int pos = this.children[2].data.indexOf(in);
    					this.children[2].data.setElementAt(inNew, pos);
    					return true;
    				} else {
    					return false;
    				}
    			}
    		}
		} else {
			if(CompareTo(in.getTuple()[2], GetMid(ranges[4], ranges[5])) < 0) {
				if(!this.children[4].isLeaf()) {
    				children[4].update(in, inNew);
    			} else {
    				if(this.children[4].dataContains(in)) {
    					int pos = this.children[4].data.indexOf(in);
    					this.children[4].data.setElementAt(inNew, pos);
    					return true;
    				} else {
    					return false;
    				}
    			}
    		} else {
    			if(!this.children[6].isLeaf()) {
    				children[6].update(in, inNew);
    			} else {
    				if(this.children[6].dataContains(in)) {
    					int pos = this.children[6].data.indexOf(in);
    					this.children[6].data.setElementAt(inNew, pos);
    					return true;
    				} else {
    					return false;
    				}
    			}
    		}
		}
	} else {
		if(CompareTo(in.getTuple()[1], GetMid(ranges[2], ranges[3])) < 0) {
			if(CompareTo(in.getTuple()[2], GetMid(ranges[4], ranges[5])) < 0) {
				if(!this.children[1].isLeaf()) {
    				children[1].update(in, inNew);
    			} else {
    				if(this.children[1].dataContains(in)) {
    					int pos = this.children[1].data.indexOf(in);
    					this.children[1].data.setElementAt(inNew, pos);
    					return true;
    				} else {
    					return false;
    				}
    			}
    		} else {
    			if(!this.children[3].isLeaf()) {
    				children[3].update(in, inNew);
    			} else {
    				if(this.children[3].dataContains(in)) {
    					int pos = this.children[3].data.indexOf(in);
    					this.children[3].data.setElementAt(inNew, pos);
    					return true;
    				} else {
    					return false;
    				}
    			}
    		}
		} else {
			if(CompareTo(in.getTuple()[2], GetMid(ranges[4], ranges[5])) < 0) {
				if(!this.children[5].isLeaf()) {
    				children[5].update(in, inNew);
    			} else {
    				if(this.children[5].dataContains(in)) {
    					int pos = this.children[5].data.indexOf(in);
    					this.children[5].data.setElementAt(inNew, pos);
    					return true;
    				} else {
    					return false;
    				}
    			}
    		} else {
    			if(!this.children[7].isLeaf()) {
    				children[7].update(in, inNew);
    			} else {
    				if(this.children[7].dataContains(in)) {
    					int pos = this.children[7].data.indexOf(in);
    					this.children[7].data.setElementAt(inNew, pos);
    					return true;
    				} else {
    					return false;
    				}
    			}
    		}
		}
	}
	return false;
}
	
	public static void main(String[] args) throws DBAppException, ParseException {
		
	}


}
