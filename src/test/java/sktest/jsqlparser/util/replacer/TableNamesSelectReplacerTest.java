package sktest.jsqlparser.util.replacer;

import com.google.common.collect.Maps;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.shaneking.aspectj.AspectJUnit4Runner;
import org.shaneking.jsqlparser.util.replacer.TableNamesStatementReplacerFactory;
import sktest.jsqlparser.SKUnit;

import java.util.Map;

@RunWith(AspectJUnit4Runner.class)
public class TableNamesSelectReplacerTest extends SKUnit {
  Map<String, String> tableMap = Maps.newHashMap();

  @Before
  public void setUp() throws Exception {
    tableMap.put("schema.table".toLowerCase(), "(SELECT * FROM schema.table)");
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testPlainSelect() throws Exception {
    StringBuilder sb = new StringBuilder();
    Statement statement = CCJSqlParserUtil.parse("select t.*--comments\n from schema.table t");
    statement.accept(TableNamesStatementReplacerFactory.create(sb, tableMap));
//    System.out.println(sb);
    Assert.assertEquals("SELECT t.* FROM (SELECT * FROM schema.table) t", sb.toString());
  }

}