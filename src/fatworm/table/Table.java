package fatworm.table;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;

import fatworm.expr.Expr;
import fatworm.io.Cursor;
import fatworm.util.Env;
import fatworm.util.Util;
import fatworm.type.INT;
import fatworm.type.NULL;
import fatworm.type.Type;

public abstract class Table implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2559424450446511184L;
	public Schema schema;
	public Integer firstPageID;
	public List<Index> tableIndex = new ArrayList<Index>();
	
	public int insert(Tree t){
		int ans = 0;
		Record r = new Record(schema);
		r.autoFill();
		for (int i = 0; i < t.getChildCount(); i++) {
			Tree c = t.getChild(i);
			String colName = schema.columnName.get(i);
			Column col = schema.getColumn(colName);
			Type val = Util.getField(col, c);
			r.setField(colName, val);
			if (col.isAutoInc() && val instanceof INT)
				col.ai = Math.max(col.ai, ((INT)val).value+1);
		}
		addRecord(r);
		ans++;
		return ans;
	}
	
	public int insert(CommonTree t, Tree v){
		int ret = 0;
		Record r = new Record(schema);
		r.autoFill();
		for (int i = 0; i < v.getChildCount(); i++){
			String colName = t.getChild(i+1).getText();
			r.setField(colName, Util.getField(schema.getColumn(colName), v.getChild(i)));
		}
		for (int i = 0; i  <r.cols.size(); i++){
			if (r.cols.get(i) != null) continue;
			Column c = r.schema.getColumn(i);
			if (c.notNull) {
				r.cols.set(i, new INT(c.getAutoInc()));
			} else {
				r.cols.set(i, NULL.getInstance());
			}

		}
		addRecord(r);
		ret++;
		return ret;
	}
	
	public int delete(Expr e) throws Throwable {
		int ans = 0;
		for (Cursor c = open(); c.hasThis();) {
			Record r = c.fetchRecord();
			Env env = new Env();
			env.appendFromRecord(r);
			if (e != null && !e.evalPred(env)) {
				c.next();
				continue;
			}
			c.delete();
			ans++;
		}
		return ans;
	}
	
	public Schema getSchema() {
		return schema;
	}
	
	public boolean hasIndexOn(String col){
		for (Index idx : tableIndex)
			if (idx.column == schema.getColumn(col))
				return true;
		return false;
	}
	
	public Index getIndexOn(String col){
		for(Index idx:tableIndex)
			if(idx.column==schema.getColumn(col))
				return idx;
		
		return null;
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof Table){
			Table x = (Table) o;
			return x.schema.equals(schema);
		}
		return false;
	}
	
	public abstract int update(List<String> colName, List<Expr> expr, Expr e);
	
	public abstract void addRecord(Record r);

	public abstract void deleteAll();
	
	public abstract Cursor open();

	public void announceNewRoot(Integer id, Integer x) {
		for (Index idx : tableIndex)
			if (idx.pid.equals(id))
				idx.pid = x;
	}
}
