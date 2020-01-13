package org.shaneking.aspectj.jsqlparser.util.replacer;

import net.sf.jsqlparser.util.deparser.StatementDeParser;
import org.shaneking.skava.persistence.Tuple;

import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class SensitiveStatementReplacerFactory {

  public static StatementDeParser create(Map<String, Tuple.Triple<Set<String>, String, String>> itemMap, StringBuilder stringBuilder, Map<String, String> tableMap) {
    SensitiveExpressionReplacer expressionDeParser = new SensitiveExpressionReplacer(new Stack<String>(), itemMap);
    TableNamesSelectReplacer tableNamesSelectReplacer = new TableNamesSelectReplacer(expressionDeParser, stringBuilder, tableMap);
    expressionDeParser.setSelectVisitor(tableNamesSelectReplacer);
    expressionDeParser.setBuffer(stringBuilder);
    return new SensitiveStatementReplacer(expressionDeParser, tableNamesSelectReplacer, stringBuilder);
  }
}
