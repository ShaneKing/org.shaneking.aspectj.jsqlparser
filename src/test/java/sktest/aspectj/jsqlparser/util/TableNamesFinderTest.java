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
import org.shaneking.aspectj.jsqlparser.util.TableNamesFinder;
import org.shaneking.aspectj.test.SKAspectJUnit;
import org.shaneking.skava.persistence.Tuple;

import java.io.StringReader;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class TableNamesFinderTest extends SKAspectJUnit {
  private static CCJSqlParserManager pm = new CCJSqlParserManager();

  @Test(expected = JSQLParserException.class)
  public void testAnyLike() throws Exception {
    String sql = "SELECT * FROM MY_TABLE1 t where t.col like any (array['%foo%', '%bar%', '%baz%']) ";
    Statement statement = pm.parse(new StringReader(sql));
    System.out.println(statement);
  }

  @Test
  public void testGetTableList() throws Exception {
    String sql = "SELECT * FROM MY_TABLE1, MY_TABLE2, (SELECT * FROM MY_TABLE3) LEFT OUTER JOIN MY_TABLE4 "
      + " WHERE ID = (SELECT MAX(ID) FROM MY_TABLE5) AND ID2 IN (SELECT * FROM MY_TABLE6)";
    Statement statement = pm.parse(new StringReader(sql));

    // now you should use a class that implements StatementVisitor to decide what to do
    // based on the kind of the statement, that is SELECT or INSERT etc. but here we are only
    // interested in SELECTS
    if (statement instanceof Select) {
      Select selectStatement = (Select) statement;
      TableNamesFinder tableNamesFinder = new TableNamesFinder();
      Set<Tuple.Pair<String, String>> tableList = tableNamesFinder.findTableSet(selectStatement);
      assertEquals(6, tableList.size());
      for (int i = 1; i < tableList.size(); i++) {
        assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toSet()).contains("my_table" + i));
      }
    }
  }

  @Test
  public void testGetTableListWithAlias() throws Exception {
    String sql = "SELECT * FROM MY_TABLE1 as ALIAS_TABLE1";
    Statement statement = pm.parse(new StringReader(sql));

    Select selectStatement = (Select) statement;
    TableNamesFinder tableNamesFinder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = tableNamesFinder.findTableSet(selectStatement);
    assertEquals(1, tableList.size());
//    assertEquals("MY_TABLE1", Tuple.getFirst(tableList.<Tuple.Pair>toArray()[0]));
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("my_table1"));
  }

  @Test
  public void testGetTableListWithStmt() throws Exception {
    String sql = "WITH TESTSTMT as (SELECT * FROM MY_TABLE1 as ALIAS_TABLE1) SELECT * FROM TESTSTMT";
    Statement statement = pm.parse(new StringReader(sql));

    Select selectStatement = (Select) statement;
    TableNamesFinder tableNamesFinder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = tableNamesFinder.findTableSet(selectStatement);
    assertEquals(1, tableList.size());
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("my_table1"));
  }

  @Test
  public void testGetTableListWithLateral() throws Exception {
    String sql = "SELECT * FROM MY_TABLE1, LATERAL(select a from MY_TABLE2) as AL";
    Statement statement = pm.parse(new StringReader(sql));

    Select selectStatement = (Select) statement;
    TableNamesFinder tableNamesFinder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = tableNamesFinder.findTableSet(selectStatement);
    assertEquals(2, tableList.size());
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("my_table1"));
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("my_table2"));
  }

  @Test
  public void testGetTableListFromDelete() throws Exception {
    String sql = "DELETE FROM MY_TABLE1 as AL WHERE a = (SELECT a from MY_TABLE2)";
    Statement statement = pm.parse(new StringReader(sql));

    Delete deleteStatement = (Delete) statement;
    TableNamesFinder tableNamesFinder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = tableNamesFinder.findTableSet(deleteStatement);
    assertEquals(2, tableList.size());
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("my_table1"));
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("my_table2"));
  }

  @Test
  public void testGetTableListFromDelete2() throws Exception {
    String sql = "DELETE FROM MY_TABLE1";
    Statement statement = pm.parse(new StringReader(sql));

    Delete deleteStatement = (Delete) statement;
    TableNamesFinder tableNamesFinder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = tableNamesFinder.findTableSet(deleteStatement);
    assertEquals(1, tableList.size());
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("my_table1"));
  }

  @Test
  public void testGetTableListFromDeleteWithJoin() throws Exception {
    String sql = "DELETE t1, t2 FROM MY_TABLE1 t1 JOIN MY_TABLE2 t2 ON t1.id = t2.id";
    Statement statement = pm.parse(new StringReader(sql));

    Delete deleteStatement = (Delete) statement;
    TableNamesFinder tableNamesFinder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = tableNamesFinder.findTableSet(deleteStatement);
    assertEquals(2, tableList.size());
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("my_table1"));
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("my_table2"));
  }

  @Test
  public void testGetTableListFromInsert() throws Exception {
    String sql = "INSERT INTO MY_TABLE1 (a) VALUES ((SELECT a from MY_TABLE2 WHERE a = 1))";
    Statement statement = pm.parse(new StringReader(sql));

    Insert insertStatement = (Insert) statement;
    TableNamesFinder tableNamesFinder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = tableNamesFinder.findTableSet(insertStatement);
    assertEquals(2, tableList.size());
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("my_table1"));
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("my_table2"));
  }

  @Test
  public void testGetTableListFromInsertValues() throws Exception {
    String sql = "INSERT INTO MY_TABLE1 (a) VALUES (5)";
    Statement statement = pm.parse(new StringReader(sql));

    Insert insertStatement = (Insert) statement;
    TableNamesFinder tableNamesFinder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = tableNamesFinder.findTableSet(insertStatement);
    assertEquals(1, tableList.size());
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("my_table1"));
  }

  @Test
  public void testGetTableListFromReplace() throws Exception {
    String sql = "REPLACE INTO MY_TABLE1 (a) VALUES ((SELECT a from MY_TABLE2 WHERE a = 1))";
    Statement statement = pm.parse(new StringReader(sql));

    Replace replaceStatement = (Replace) statement;
    TableNamesFinder tableNamesFinder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = tableNamesFinder.findTableSet(replaceStatement);
    assertEquals(2, tableList.size());
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("my_table1"));
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("my_table2"));
  }

  @Test
  public void testGetTableListFromUpdate() throws Exception {
    String sql = "UPDATE MY_TABLE1 SET a = (SELECT a from MY_TABLE2 WHERE a = 1)";
    Statement statement = pm.parse(new StringReader(sql));

    Update updateStatement = (Update) statement;
    TableNamesFinder tableNamesFinder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = tableNamesFinder.findTableSet(updateStatement);
    assertEquals(2, tableList.size());
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("my_table1"));
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("my_table2"));
  }

  @Test
  public void testGetTableListFromUpdate2() throws Exception {
    String sql = "UPDATE MY_TABLE1 SET a = 5 WHERE 0 < (SELECT COUNT(b) FROM MY_TABLE3)";
    Statement statement = pm.parse(new StringReader(sql));

    Update updateStatement = (Update) statement;
    TableNamesFinder tableNamesFinder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = tableNamesFinder.findTableSet(updateStatement);
    assertEquals(2, tableList.size());
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("my_table1"));
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("my_table3"));
  }

  @Test
  public void testGetTableListFromUpdate3() throws Exception {
    String sql = "UPDATE MY_TABLE1 SET a = 5 FROM MY_TABLE1 INNER JOIN MY_TABLE2 on MY_TABLE1.C = MY_TABLE2.D WHERE 0 < (SELECT COUNT(b) FROM MY_TABLE3)";
    Statement statement = pm.parse(new StringReader(sql));

    Update updateStatement = (Update) statement;
    TableNamesFinder tableNamesFinder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = tableNamesFinder.findTableSet(updateStatement);
    assertEquals(3, tableList.size());
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("my_table1"));
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("my_table2"));
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("my_table3"));
  }

  @Test
  public void testCmplxSelectProblem() throws Exception {
    String sql = "SELECT cid, (SELECT name FROM tbl0 WHERE tbl0.id = cid) AS name, original_id AS bc_id FROM tbl WHERE crid = ? AND user_id is null START WITH ID = (SELECT original_id FROM tbl2 WHERE USER_ID = ?) CONNECT BY prior parent_id = id AND rownum = 1";
    Statement statement = pm.parse(new StringReader(sql));

    Select selectStatement = (Select) statement;
    TableNamesFinder tableNamesFinder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = tableNamesFinder.findTableSet(selectStatement);
    assertEquals(3, tableList.size());
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("tbl0"));
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("tbl"));
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("tbl2"));
  }

  @Test
  public void testInsertSelect() throws Exception {
    String sql = "INSERT INTO mytable (mycolumn) SELECT mycolumn FROM mytable2";
    Statement statement = pm.parse(new StringReader(sql));

    Insert insertStatement = (Insert) statement;
    TableNamesFinder tableNamesFinder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = tableNamesFinder.findTableSet(insertStatement);
    assertEquals(2, tableList.size());
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("mytable"));
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("mytable2"));
  }

  @Test
  public void testCreateSelect() throws Exception {
    String sql = "CREATE TABLE mytable AS SELECT mycolumn FROM mytable2";
    Statement statement = pm.parse(new StringReader(sql));

    CreateTable createTable = (CreateTable) statement;
    TableNamesFinder tableNamesFinder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = tableNamesFinder.findTableSet(createTable);
    assertEquals(2, tableList.size());
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("mytable"));
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("mytable2"));
  }

  @Test
  public void testInsertSubSelect() throws JSQLParserException {
    String sql = "INSERT INTO Customers (CustomerName, Country) SELECT SupplierName, Country FROM Suppliers WHERE Country='Germany'";
    Insert insert = (Insert) pm.parse(new StringReader(sql));
    TableNamesFinder tableNamesFinder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = tableNamesFinder.findTableSet(insert);
    assertEquals(2, tableList.size());
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("customers"));
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("suppliers"));
  }

  @Test
  public void testExpr() throws JSQLParserException {
    String sql = "mycol in (select col2 from mytable)";
    Expression expr = CCJSqlParserUtil.parseCondExpression(sql);
    TableNamesFinder tableNamesFinder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = tableNamesFinder.findTableSet(expr);
    assertEquals(1, tableList.size());
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("mytable"));
  }

  @Test
  public void testOracleHint() throws JSQLParserException {
    String sql = "select --+ HINT\ncol2 from mytable";
    Select select = (Select) CCJSqlParserUtil.parse(sql);
    final OracleHint[] holder = new OracleHint[1];
    TableNamesFinder tableNamesFinder = new TableNamesFinder() {
      @Override
      public void visit(OracleHint hint) {
        super.visit(hint);
        holder[0] = hint;
      }
    };
    tableNamesFinder.findTableSet(select);
    assertNull(holder[0]);
  }

  @Test
  public void testGetTableListIssue194() throws Exception {
    String sql = "SELECT 1";
    Statement statement = pm.parse(new StringReader(sql));

    Select selectStatement = (Select) statement;
    TableNamesFinder tableNamesFinder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = tableNamesFinder.findTableSet(selectStatement);
    assertEquals(0, tableList.size());
  }

  @Test
  public void testGetTableListIssue284() throws Exception {
    String sql = "SELECT NVL( (SELECT 1 FROM DUAL), 1) AS A FROM TEST1";
    Select selectStatement = (Select) CCJSqlParserUtil.parse(sql);
    TableNamesFinder tableNamesFinder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = tableNamesFinder.findTableSet(selectStatement);
    assertEquals(2, tableList.size());
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("dual"));
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("test1"));
  }

  @Test
  public void testUpdateGetTableListIssue295() throws JSQLParserException {
    Update statement = (Update) CCJSqlParserUtil.
      parse("UPDATE component SET col = 0 WHERE (component_id,ver_num) IN (SELECT component_id,ver_num FROM component_temp)");
    TableNamesFinder tableNamesFinder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = tableNamesFinder.findTableSet(statement);
    assertEquals(2, tableList.size());
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("component"));
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("component_temp"));
  }

  @Test
  public void testGetTableListForMerge() throws Exception {
    String sql = "MERGE INTO employees e  USING hr_records h  ON (e.id = h.emp_id) WHEN MATCHED THEN  UPDATE SET e.address = h.address  WHEN NOT MATCHED THEN    INSERT (id, address) VALUES (h.emp_id, h.address);";
    TableNamesFinder tableNamesFinder = new TableNamesFinder();

    Set<Tuple.Pair<String, String>> tableList = tableNamesFinder.findTableSet(CCJSqlParserUtil.parse(sql));
    assertEquals(2, tableList.size());
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("employees"));
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("hr_records"));
//    assertEquals("employees", Tuple.getFirst(tableList.get(0)));
//    assertEquals("hr_records", Tuple.getFirst(tableList.get(1)));
  }

  @Test
  public void testGetTableListForMergeUsingQuery() throws Exception {
    String sql = "MERGE INTO employees e USING (SELECT * FROM hr_records WHERE start_date > ADD_MONTHS(SYSDATE, -1)) h  ON (e.id = h.emp_id)  WHEN MATCHED THEN  UPDATE SET e.address = h.address WHEN NOT MATCHED THEN INSERT (id, address) VALUES (h.emp_id, h.address)";
    TableNamesFinder tableNamesFinder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = tableNamesFinder.findTableSet(CCJSqlParserUtil.parse(sql));
    assertEquals(2, tableList.size());
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("employees"));
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("hr_records"));
//    assertEquals("employees", Tuple.getFirst(tableList.get(0)));
//    assertEquals("hr_records", Tuple.getFirst(tableList.get(1)));
  }

  @Test
  public void testUpsertValues() throws Exception {
    String sql = "UPSERT INTO MY_TABLE1 (a) VALUES (5)";
    Statement statement = pm.parse(new StringReader(sql));

    Upsert insertStatement = (Upsert) statement;
    TableNamesFinder tableNamesFinder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = tableNamesFinder.findTableSet(insertStatement);
    assertEquals(1, tableList.size());
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("my_table1"));
  }

  @Test
  public void testUpsertSelect() throws Exception {
    String sql = "UPSERT INTO mytable (mycolumn) SELECT mycolumn FROM mytable2";
    Statement statement = pm.parse(new StringReader(sql));

    Upsert insertStatement = (Upsert) statement;
    TableNamesFinder tableNamesFinder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = tableNamesFinder.findTableSet(insertStatement);
    assertEquals(2, tableList.size());
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("mytable"));
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("mytable2"));
  }

  @Test
  public void testCaseWhenSubSelect() throws JSQLParserException {
    String sql = "select case (select count(*) from mytable2) when 1 then 0 else -1 end";
    Statement stmt = CCJSqlParserUtil.parse(sql);
    TableNamesFinder tableNamesFinder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = tableNamesFinder.findTableSet(stmt);
    assertEquals(1, tableList.size());
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("mytable2"));
  }

  @Test
  public void testCaseWhenSubSelect2() throws JSQLParserException {
    String sql = "select case when (select count(*) from mytable2) = 1 then 0 else -1 end";
    Statement stmt = CCJSqlParserUtil.parse(sql);
    TableNamesFinder tableNamesFinder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = tableNamesFinder.findTableSet(stmt);
    assertEquals(1, tableList.size());
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("mytable2"));
  }

  @Test
  public void testCaseWhenSubSelect3() throws JSQLParserException {
    String sql = "select case when 1 = 2 then 0 else (select count(*) from mytable2) end";
    Statement stmt = CCJSqlParserUtil.parse(sql);
    TableNamesFinder tableNamesFinder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = tableNamesFinder.findTableSet(stmt);
    assertEquals(1, tableList.size());
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("mytable2"));
  }

  @Test
  public void testExpressionIssue515() throws JSQLParserException {
    TableNamesFinder finder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = finder.findTableSet(CCJSqlParserUtil.parseCondExpression("SOME_TABLE.COLUMN = 'A'"));
    assertEquals(1, tableList.size());
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("some_table"));
  }

  @Test
  public void testSelectHavingSubquery() throws Exception {
    String sql = "SELECT * FROM TABLE1 GROUP BY COL1 HAVING SUM(COL2) > (SELECT COUNT(*) FROM TABLE2)";
    Statement statement = pm.parse(new StringReader(sql));

    Select selectStmt = (Select) statement;
    TableNamesFinder tableNamesFinder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = tableNamesFinder.findTableSet(selectStmt);
    assertEquals(2, tableList.size());
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("table1"));
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("table2"));
  }

  @Test
  public void testMySQLValueListExpression() throws JSQLParserException {
    String sql = "SELECT * FROM TABLE1 WHERE (a, b) = (c, d)";
    TableNamesFinder finder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = finder.findTableSet(CCJSqlParserUtil.parse(sql));
    assertEquals(1, tableList.size());
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("table1"));
  }

  @Test
  public void testSkippedSchemaIssue600() throws JSQLParserException {
    String sql = "delete from schema.table where id = 1";
    TableNamesFinder finder = new TableNamesFinder();
    Set<Tuple.Pair<String, String>> tableList = finder.findTableSet(CCJSqlParserUtil.parse(sql));
    assertEquals(1, tableList.size());
    assertTrue(tableList.stream().map(Tuple::getFirst).collect(Collectors.toList()).contains("schema.table"));
  }
}
