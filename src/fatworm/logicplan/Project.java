package fatworm.logicplan;

import java.util.LinkedList;
import java.util.List;

import fatworm.expr.Expr;
import fatworm.table.Column;
import fatworm.table.Record;
import fatworm.table.Schema;
import fatworm.util.Env;
import fatworm.util.Util;

public class Project extends Plan {
	public Plan src;
	public List<Expr> expr;
	final List<String> names;
	Schema schema;
	Env env;
	boolean hasProjectAll=false;

	public Project(Plan src, List<Expr> expr, boolean hasProjectAll) {
		super();
		this.src = src;
		this.expr = expr;
		this.src.parent = this;
		this.myAggr.addAll(this.src.getAggr());
		
		this.schema = new Schema();
		if (hasProjectAll) {
			Schema scm = src.getSchema();
			this.schema.columnDef.putAll(scm.columnDef);
			this.schema.columnName.addAll(scm.columnName);
		}
		this.schema.fromList(expr, src.getSchema());
		this.names = this.schema.columnName;
		this.hasProjectAll = hasProjectAll;
	}

	@Override
	public void eval(Env env) {
		hasEval = true;
		src.eval(env);
		this.env = env;
		for (Column c:schema.columnDef.values())
			if (c.type == java.sql.Types.NULL)
				c.type = java.sql.Types.VARCHAR;
	}
	@Override
	public String toString(){
		return "Project (from="+src.toString()+", names="+Util.deepToString(names)+")";
	}

	@Override
	public boolean hasNext() {
		return src.hasNext();
	}

	@Override
	public Record next() {
		Env localenv = env.clone();
		Record tmpr = src.next();
		Record ans = new Record(schema);
		if (hasProjectAll)
			ans.cols.addAll(tmpr.cols);
		
		localenv.appendFromRecord(tmpr);
		ans.addColFromExpr(localenv, expr);
		
		return ans;
	}

	@Override
	public void reset() {
		src.reset();
	}

	@Override
	public Schema getSchema() {
		return schema;
	}

	@Override
	public void close() {
		src.close();
	}

	@Override
	public List<String> getColumns() {
		return new LinkedList<String>(schema.columnName);
	}

	@Override
	public List<String> getRequestedColumns() {
		List <String> ans = new LinkedList<String>(src.getRequestedColumns());
		Util.removeAllCol(ans, src.getColumns());
		return ans;
	}

	@Override
	public void rename(String oldName, String newName) {
		src.rename(oldName, newName);
		for (Expr e : expr)
			if (e.getType(src.getSchema()) == java.sql.Types.NULL)
				e.rename(oldName, newName);
	}

	public boolean isConst(){
		for (Expr e:expr)
			if (!e.isConst) return false;
		
		return true;
	}
}
