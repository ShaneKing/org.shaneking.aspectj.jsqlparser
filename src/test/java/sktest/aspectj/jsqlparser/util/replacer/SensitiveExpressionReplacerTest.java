package sktest.aspectj.jsqlparser.util.replacer;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.shaneking.aspectj.jsqlparser.util.SensitiveItemsFinder;
import org.shaneking.aspectj.jsqlparser.util.replacer.SensitiveStatementReplacerFactory;
import org.shaneking.aspectj.test.SKAspectJUnit;
import org.shaneking.skava.lang.String0;
import org.shaneking.skava.persistence.Tuple;

import java.util.Map;
import java.util.Set;

public class SensitiveExpressionReplacerTest extends SKAspectJUnit {
  Map<String, Tuple.Triple<Set<String>, String, String>> itemMap = Maps.newHashMap();
  Map<String, String> tableMap = Maps.newHashMap();

  @Before
  public void setUp() {
    itemMap.put("t.a", Tuple.of(Sets.newHashSet(Joiner.on(String0.ARROW).join(SensitiveItemsFinder.PATH_OF_SELECT, SensitiveItemsFinder.PATH_OF_SELECT_EXPRESSION_ITEM)), "hash(", ")"));
    tableMap.put("schema.table", "(select * from schema.table)");
  }

  @Test
  public void testPlainSelect() throws Exception {
    StringBuilder sb = new StringBuilder();
    Statement statement = CCJSqlParserUtil.parse("select t.a as ta,t.* from schema.table t");
    statement.accept(SensitiveStatementReplacerFactory.create(itemMap, sb, tableMap));
//    System.out.println(sb);
    Assert.assertEquals("SELECT hash(t.a) AS ta, t.* FROM (select * from schema.table) t", sb.toString());
  }
}
