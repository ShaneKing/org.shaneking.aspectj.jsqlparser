package org.shaneking.aspectj.jsqlparser.util.replacer;

import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;

import java.util.Map;

public class TableNamesStatementReplacerFactory {
  public static StatementDeParser create(StringBuilder stringBuilder, Map<String, String> tableMap) {
    ExpressionDeParser expressionDeParser = new ExpressionDeParser();
    TableNamesSelectReplacer tableNamesSelectReplacer = new TableNamesSelectReplacer(expressionDeParser, stringBuilder, tableMap);
    expressionDeParser.setSelectVisitor(tableNamesSelectReplacer);
    expressionDeParser.setBuffer(stringBuilder);
    return new StatementDeParser(expressionDeParser, tableNamesSelectReplacer, stringBuilder);
  }
}
