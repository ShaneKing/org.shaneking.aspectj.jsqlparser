package sktest.aspectj.jsqlparser.util;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.OracleHint;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.replace.Replace;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.upsert.Upsert;
import org.junit.Test;
import org.shaneking.aspectj.jsqlparser.util.SensitiveItemsFinder;
import org.shaneking.aspectj.test.SKAspectJUnit;
import org.shaneking.skava.persistence.Tuple;

import java.io.StringReader;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SensitiveItemsFinderTest extends SKAspectJUnit {
  private static CCJSqlParserManager pm = new CCJSqlParserManager();

  @Test
  public void testSelectExpressionWith() throws Exception {
    String sql = "with a as (select c from t), b as (select * from a) select * from (with c as (select * from b), d as (select * from c) select * from d) t2";
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(CCJSqlParserUtil.parse(sql));
    assertEquals(1, itemMap.size());
    assertEquals("{t2=[[t],{c=[[t,c,[Select→WithItem→SelectExpressionItem],false]], *=[[t,c,[Select→WithItem→SelectExpressionItem, Select→SubSelect→WithItem, Select, Select→SubSelect, Select→WithItem],false]]}]}", itemMap.toString());
  }

  @Test
  public void testSelectExpressionAlias() throws Exception {
    String sql = "SELECT (select (select a.host1+b.host2+c.host3 as c3 from mysql.user c where c.user = a.user) as c2 from mysql.user b where b.user = a.user) as c1 FROM mysql.user a";
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(CCJSqlParserUtil.parse(sql));
    assertEquals(3, itemMap.size());
    assertEquals("{a=[[mysql.user],{c3=[[mysql.user,host1,[Select→SelectExpressionItem→SubSelect→SelectExpressionItem→SubSelect→SelectExpressionItem],true]], host1=[[mysql.user,host1,[Select→SelectExpressionItem→SubSelect→SelectExpressionItem→SubSelect→SelectExpressionItem],true]]}], b=[[mysql.user],{c3=[[mysql.user,host2,[Select→SelectExpressionItem→SubSelect→SelectExpressionItem→SubSelect→SelectExpressionItem],true]], host2=[[mysql.user,host2,[Select→SelectExpressionItem→SubSelect→SelectExpressionItem→SubSelect→SelectExpressionItem],true]]}], c=[[mysql.user],{c3=[[mysql.user,host3,[Select→SelectExpressionItem→SubSelect→SelectExpressionItem→SubSelect→SelectExpressionItem],true]], host3=[[mysql.user,host3,[Select→SelectExpressionItem→SubSelect→SelectExpressionItem→SubSelect→SelectExpressionItem],true]]}]}", itemMap.toString());
  }

  //TODO
  @Test
  public void testSetOperationList() throws Exception {
    String sql = "select sub(t.a,1,3) from (select tab1.a from tab1 union select tab2.m from tab2) t";
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(CCJSqlParserUtil.parse(sql));
    assertEquals(1, itemMap.size());
    assertEquals("{t=[[tab1, tab2],{a=[[tab1,a,[Select→SubSelect→SelectExpressionItem, Select→SelectExpressionItem],true]], m=[[tab2,m,[Select→SubSelect→SelectExpressionItem],false]]}]}", itemMap.toString());
  }

  @Test
  public void testGetItemMap() throws Exception {
    String sql = "SELECT mt1.* FROM MY_TABLE1 mt1, MY_TABLE2, (SELECT imt3.* FROM MY_TABLE3 imt3) LEFT OUTER JOIN MY_TABLE4 "
      + " WHERE ID = (SELECT MAX(ID) FROM MY_TABLE5) AND ID2 IN (SELECT * FROM MY_TABLE6)";
    Statement statement = pm.parse(new StringReader(sql));

    // now you should use a class that implements StatementVisitor to decide what to do
    // based on the kind of the statement, that is SELECT or INSERT etc. but here we are only
    // interested in SELECTS
    if (statement instanceof Select) {
      Select selectStatement = (Select) statement;
      SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
      Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(selectStatement);
      assertEquals(4, itemMap.size());
      assertEquals("{my_table4=[[my_table4],{}], *=[[my_table3],{*=[[my_table3,*,[Select→SubSelect],false]]}], mt1=[[my_table1],{*=[[my_table1,*,[Select],false]]}], my_table2=[[my_table2],{}]}", itemMap.toString());
    }
  }

  @Test
  public void testGetItemMapWithAlias() throws Exception {
    String sql = "SELECT ALIAS_TABLE1.* FROM MY_TABLE1 as ALIAS_TABLE1";
    Statement statement = pm.parse(new StringReader(sql));

    Select selectStatement = (Select) statement;
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(selectStatement);
    assertEquals(1, itemMap.size());
    assertEquals("{alias_table1=[[my_table1],{*=[[my_table1,*,[Select],false]]}]}", itemMap.toString());
  }

  @Test
  public void testGetItemMapWithStmt() throws Exception {
    String sql = "WITH TESTSTMT as (SELECT ALIAS_TABLE1.* FROM MY_TABLE1 as ALIAS_TABLE1) SELECT TESTSTMT.* FROM TESTSTMT";
    Statement statement = pm.parse(new StringReader(sql));

    Select selectStatement = (Select) statement;
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(selectStatement);
    assertEquals(1, itemMap.size());
    assertEquals("{teststmt=[[my_table1],{*=[[my_table1,*,[Select, Select→WithItem],false]]}]}", itemMap.toString());
  }

  @Test
  public void testGetItemMapWithLateral() throws Exception {
    String sql = "SELECT al.* FROM MY_TABLE1, LATERAL(select MY_TABLE2.a from MY_TABLE2) as AL";
    Statement statement = pm.parse(new StringReader(sql));

    Select selectStatement = (Select) statement;
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(selectStatement);
    assertEquals(2, itemMap.size());
    assertEquals("{al=[[my_table2],{a=[[my_table2,a,[Select→FromItem→SubSelect→SelectExpressionItem],false]], *=[[my_table2,a,[Select, Select→FromItem→SubSelect→SelectExpressionItem],false]]}], my_table1=[[my_table1],{}]}", itemMap.toString());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetItemMapFromDelete() throws Exception {
    String sql = "DELETE FROM MY_TABLE1 as AL WHERE a = (SELECT a from MY_TABLE2)";
    Statement statement = pm.parse(new StringReader(sql));

    Delete deleteStatement = (Delete) statement;
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(deleteStatement);
    System.out.println(itemMap);
    assertEquals(2, itemMap.size());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetItemMapFromDelete2() throws Exception {
    String sql = "DELETE FROM MY_TABLE1";
    Statement statement = pm.parse(new StringReader(sql));

    Delete deleteStatement = (Delete) statement;
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(deleteStatement);
    System.out.println(itemMap);
    assertEquals(1, itemMap.size());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetItemMapFromDeleteWithJoin() throws Exception {
    String sql = "DELETE t1, t2 FROM MY_TABLE1 t1 JOIN MY_TABLE2 t2 ON t1.id = t2.id";
    Statement statement = pm.parse(new StringReader(sql));

    Delete deleteStatement = (Delete) statement;
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(deleteStatement);
    System.out.println(itemMap);
    assertEquals(2, itemMap.size());
  }

  @Test
  public void testGetItemMapFromInsert() throws Exception {
    String sql = "INSERT INTO MY_TABLE1 (a) VALUES ((SELECT MY_TABLE2.a from MY_TABLE2 WHERE a = 1))";
    Statement statement = pm.parse(new StringReader(sql));

    Insert insertStatement = (Insert) statement;
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(insertStatement);
    assertEquals(1, itemMap.size());
    assertEquals("{*=[[my_table2],{a=[[my_table2,a,[Insert→SubSelect→SelectExpressionItem],false]]}]}", itemMap.toString());
  }

  @Test
  public void testGetItemMapFromInsertValues() throws Exception {
    String sql = "INSERT INTO MY_TABLE1 (a) VALUES (5)";
    Statement statement = pm.parse(new StringReader(sql));

    Insert insertStatement = (Insert) statement;
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(insertStatement);
    assertEquals(0, itemMap.size());
    assertEquals("{}", itemMap.toString());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetItemMapFromReplace() throws Exception {
    String sql = "REPLACE INTO MY_TABLE1 (a) VALUES ((SELECT a from MY_TABLE2 WHERE a = 1))";
    Statement statement = pm.parse(new StringReader(sql));

    Replace replaceStatement = (Replace) statement;
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(replaceStatement);
    System.out.println(itemMap);
    assertEquals(2, itemMap.size());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetItemMapFromUpdate() throws Exception {
    String sql = "UPDATE MY_TABLE1 SET a = (SELECT a from MY_TABLE2 WHERE a = 1)";
    Statement statement = pm.parse(new StringReader(sql));

    Update updateStatement = (Update) statement;
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(updateStatement);
    System.out.println(itemMap);
    assertEquals(2, itemMap.size());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetItemMapFromUpdate2() throws Exception {
    String sql = "UPDATE MY_TABLE1 SET a = 5 WHERE 0 < (SELECT COUNT(b) FROM MY_TABLE3)";
    Statement statement = pm.parse(new StringReader(sql));

    Update updateStatement = (Update) statement;
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(updateStatement);
    System.out.println(itemMap);
    assertEquals(2, itemMap.size());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetItemMapFromUpdate3() throws Exception {
    String sql = "UPDATE MY_TABLE1 SET a = 5 FROM MY_TABLE1 INNER JOIN MY_TABLE2 on MY_TABLE1.C = MY_TABLE2.D WHERE 0 < (SELECT COUNT(b) FROM MY_TABLE3)";
    Statement statement = pm.parse(new StringReader(sql));

    Update updateStatement = (Update) statement;
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(updateStatement);
    System.out.println(itemMap);
    assertEquals(3, itemMap.size());
  }

  @Test
  public void testCmplxSelectProblem() throws Exception {
    String sql = "SELECT tbl.cid, (SELECT tbl0.name FROM tbl0 WHERE tbl0.id = cid) AS name1, tbl.original_id AS bc_id FROM tbl WHERE crid = ? AND user_id is null START WITH ID = (SELECT original_id FROM tbl2 WHERE USER_ID = ?) CONNECT BY prior parent_id = id AND rownum = 1";
    Statement statement = pm.parse(new StringReader(sql));

    Select selectStatement = (Select) statement;
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(selectStatement);
    assertEquals(2, itemMap.size());
    assertEquals("{tbl0=[[tbl0],{name=[[tbl0,name,[Select→SelectExpressionItem→SubSelect→SelectExpressionItem],false]]}], tbl=[[tbl],{bc_id=[[tbl,original_id,[Select→SelectExpressionItem],false]], original_id=[[tbl,original_id,[Select→SelectExpressionItem],false]], cid=[[tbl,cid,[Select→SelectExpressionItem],false]]}]}", itemMap.toString());
  }

  @Test
  public void testInsertSelect() throws Exception {
    String sql = "INSERT INTO mytable (mycolumn) SELECT mytable2.mycolumn FROM mytable2";
    Statement statement = pm.parse(new StringReader(sql));

    Insert insertStatement = (Insert) statement;
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(insertStatement);
    assertEquals(1, itemMap.size());
    assertEquals("{mytable2=[[mytable2],{mycolumn=[[mytable2,mycolumn,[Insert→Select→SelectExpressionItem],false]]}]}", itemMap.toString());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testCreateSelect() throws Exception {
    String sql = "CREATE TABLE mytable AS SELECT mycolumn FROM mytable2";
    Statement statement = pm.parse(new StringReader(sql));

    CreateTable createTable = (CreateTable) statement;
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(createTable);
    System.out.println(itemMap);
    assertEquals(2, itemMap.size());
  }

  @Test
  public void testInsertSubSelect() throws JSQLParserException {
    String sql = "INSERT INTO Customers (CustomerName, Country) SELECT Suppliers.SupplierName, Suppliers.Country FROM Suppliers WHERE Country='Germany'";
    Insert insert = (Insert) pm.parse(new StringReader(sql));
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(insert);
    assertEquals(1, itemMap.size());
    assertEquals("{suppliers=[[suppliers],{country=[[suppliers,country,[Insert→Select→SelectExpressionItem],false]], suppliername=[[suppliers,suppliername,[Insert→Select→SelectExpressionItem],false]]}]}", itemMap.toString());
  }

  @Test
  public void testExpr() throws JSQLParserException {
    String sql = "table.mycol in (select mytable.col2 from mytable)";
    Expression expr = CCJSqlParserUtil.parseCondExpression(sql);
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(expr);
    assertEquals(2, itemMap.size());
    assertEquals("{*=[[mytable],{col2=[[mytable,col2,[SubSelect→SelectExpressionItem],false]]}], table=[[table],{mycol=[[table,mycol,[],false]]}]}", itemMap.toString());
  }

  @Test
  public void testOracleHint() throws JSQLParserException {
    String sql = "select --+ HINT\ncol2 from mytable";
    Select select = (Select) CCJSqlParserUtil.parse(sql);
    final OracleHint[] holder = new OracleHint[1];
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder() {
      @Override
      public void visit(OracleHint hint) {
        super.visit(hint);
        holder[0] = hint;
      }
    };
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(select);
    assertNull(holder[0]);
    assertEquals("{mytable=[[mytable],{col2=[[mytable,col2,[Select→SelectExpressionItem],false]]}]}", itemMap.toString());
  }

  @Test
  public void testGetItemMapIssue194() throws Exception {
    String sql = "SELECT 1";
    Statement statement = pm.parse(new StringReader(sql));

    Select selectStatement = (Select) statement;
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(selectStatement);
    assertEquals(0, itemMap.size());
    assertEquals("{}", itemMap.toString());
  }

  @Test
  public void testGetItemMapIssue284() throws Exception {
    String sql = "SELECT NVL( (SELECT 1 FROM DUAL), 1) AS A FROM TEST1";
    Select selectStatement = (Select) CCJSqlParserUtil.parse(sql);
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(selectStatement);
    assertEquals(2, itemMap.size());
    assertEquals("{dual=[[dual],{}], test1=[[test1],{}]}", itemMap.toString());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testUpdateGetItemMapIssue295() throws JSQLParserException {
    Update statement = (Update) CCJSqlParserUtil.
      parse("UPDATE component SET col = 0 WHERE (component_id,ver_num) IN (SELECT component_id,ver_num FROM component_temp)");
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(statement);
    System.out.println(itemMap);
    assertEquals(2, itemMap.size());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetItemMapForMerge() throws Exception {
    String sql = "MERGE INTO employees e  USING hr_records h  ON (e.id = h.emp_id) WHEN MATCHED THEN  UPDATE SET e.address = h.address  WHEN NOT MATCHED THEN    INSERT (id, address) VALUES (h.emp_id, h.address);";
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();

    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(CCJSqlParserUtil.parse(sql));
    System.out.println(itemMap);
    assertEquals(2, itemMap.size());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetItemMapForMergeUsingQuery() throws Exception {
    String sql = "MERGE INTO employees e USING (SELECT * FROM hr_records WHERE start_date > ADD_MONTHS(SYSDATE, -1)) h  ON (e.id = h.emp_id)  WHEN MATCHED THEN  UPDATE SET e.address = h.address WHEN NOT MATCHED THEN INSERT (id, address) VALUES (h.emp_id, h.address)";
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(CCJSqlParserUtil.parse(sql));
    System.out.println(itemMap);
    assertEquals(2, itemMap.size());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testUpsertValues() throws Exception {
    String sql = "UPSERT INTO MY_TABLE1 (a) VALUES (5)";
    Statement statement = pm.parse(new StringReader(sql));

    Upsert insertStatement = (Upsert) statement;
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(insertStatement);
    System.out.println(itemMap);
    assertEquals(1, itemMap.size());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testUpsertSelect() throws Exception {
    String sql = "UPSERT INTO mytable (mycolumn) SELECT mycolumn FROM mytable2";
    Statement statement = pm.parse(new StringReader(sql));

    Upsert insertStatement = (Upsert) statement;
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(insertStatement);
    System.out.println(itemMap);
    assertEquals(2, itemMap.size());
  }

  @Test
  public void testCaseWhenSubSelect() throws JSQLParserException {
    String sql = "select case (select count(*) from mytable2) when 1 then 0 else -1 end";
    Statement stmt = CCJSqlParserUtil.parse(sql);
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(stmt);
    assertEquals(1, itemMap.size());
    assertEquals("{mytable2=[[mytable2],{}]}", itemMap.toString());
  }

  @Test
  public void testCaseWhenSubSelect2() throws JSQLParserException {
    String sql = "select case when (select count(*) from mytable2) = 1 then 0 else -1 end";
    Statement stmt = CCJSqlParserUtil.parse(sql);
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(stmt);
    assertEquals(1, itemMap.size());
    assertEquals("{mytable2=[[mytable2],{}]}", itemMap.toString());
  }

  @Test
  public void testCaseWhenSubSelect3() throws JSQLParserException {
    String sql = "select case when 1 = 2 then 0 else (select count(*) from mytable2) end";
    Statement stmt = CCJSqlParserUtil.parse(sql);
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(stmt);
    assertEquals(1, itemMap.size());
    assertEquals("{mytable2=[[mytable2],{}]}", itemMap.toString());
  }

  @Test
  public void testExpressionIssue515() throws JSQLParserException {
    SensitiveItemsFinder finder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = finder.findItemMap(CCJSqlParserUtil.parseCondExpression("SOME_TABLE.COLUMN = 'A'"));
    assertEquals(1, itemMap.size());
    assertEquals("{some_table=[[some_table],{column=[[some_table,column,[],false]]}]}", itemMap.toString());
  }

  @Test
  public void testSelectHavingSubquery() throws Exception {
    String sql = "SELECT TABLE1.* FROM TABLE1 GROUP BY COL1 HAVING SUM(COL2) > (SELECT COUNT(*) FROM TABLE2)";
    Statement statement = pm.parse(new StringReader(sql));

    Select selectStmt = (Select) statement;
    SensitiveItemsFinder sensitiveItemsFinder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = sensitiveItemsFinder.findItemMap(selectStmt);
    assertEquals(1, itemMap.size());
    assertEquals("{table1=[[table1],{*=[[table1,*,[Select],false]]}]}", itemMap.toString());
  }

  @Test
  public void testMySQLValueListExpression() throws JSQLParserException {
    String sql = "SELECT * FROM TABLE1 WHERE (a, b) = (c, d)";
    SensitiveItemsFinder finder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = finder.findItemMap(CCJSqlParserUtil.parse(sql));
    assertEquals(1, itemMap.size());
    assertEquals("{table1=[[table1],{*=[[table1,*,[Select],false]]}]}", itemMap.toString());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testSkippedSchemaIssue600() throws JSQLParserException {
    String sql = "delete from schema.table where id = 1";
    SensitiveItemsFinder finder = new SensitiveItemsFinder();
    Map<String, Tuple.Pair<Set<String>, Map<String, Set<Tuple.Quadruple<String, String, Set<String>, Boolean>>>>> itemMap = finder.findItemMap(CCJSqlParserUtil.parse(sql));
    System.out.println(itemMap);
    assertEquals(1, itemMap.size());
  }
}
