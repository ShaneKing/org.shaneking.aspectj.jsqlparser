package org.shaneking.aspectj.jsqlparser.util.replacer;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.MySQLIndexHint;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import org.shaneking.aspectj.jsqlparser.annotation.SensitiveExpressionReplacerPath;

import java.util.Map;

public class TableNamesSelectReplacer extends SelectDeParser {
  //Map<schema.table, (select * from schema.table where xxx in (...))>
  private Map<String, String> tableMap = Maps.newHashMap();

  TableNamesSelectReplacer(Map<String, String> tableMap) {
    super();
    this.tableMap = tableMap;
  }

  TableNamesSelectReplacer(ExpressionVisitor expressionVisitor, StringBuilder buffer, Map<String, String> tableMap) {
    super(expressionVisitor, buffer);
    this.tableMap = tableMap;
  }

  @Override
  public void visit(Table tableName) {
    String fullTableName = tableName.getFullyQualifiedName();
    String replaceString = tableMap.get(fullTableName.toLowerCase());
    getBuffer().append(Strings.isNullOrEmpty(replaceString) ? fullTableName : replaceString);
//    buffer.append(tableName.getFullyQualifiedName());
    Alias alias = tableName.getAlias();
    if (alias != null) {
      getBuffer().append(alias);
    }
    Pivot pivot = tableName.getPivot();
    if (pivot != null) {
      pivot.accept(this);
    }
    MySQLIndexHint indexHint = tableName.getIndexHint();
    if (indexHint != null) {
      getBuffer().append(indexHint);
    }
//    super.visit(tableName);
  }

  @SensitiveExpressionReplacerPath
  @Override
  public void visit(SubSelect subSelect) {
    super.visit(subSelect);
  }

  @SensitiveExpressionReplacerPath
  @Override
  public void visit(WithItem withItem) {
    super.visit(withItem);
  }

  @SensitiveExpressionReplacerPath
  @Override
  public void visit(SelectExpressionItem selectExpressionItem) {
    super.visit(selectExpressionItem);
  }

  @SensitiveExpressionReplacerPath
  @Override
  public void visit(LateralSubSelect lateralSubSelect) {
    super.visit(lateralSubSelect);
  }

  @SensitiveExpressionReplacerPath
  @Override
  public void visit(ParenthesisFromItem parenthesis) {
    super.visit(parenthesis);
  }

  @SensitiveExpressionReplacerPath
  @Override
  public void visit(SubJoin subjoin) {
    super.visit(subjoin);
  }
}
