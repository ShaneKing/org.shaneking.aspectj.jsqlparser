/*
 * @(#)SensitiveStatementReplacerFactory.java		Created at 2018/9/23
 *
 * Copyright (c) ShaneKing All rights reserved.
 * ShaneKing PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package org.shaneking.aspectj.jsqlparser.util.replacer;

import net.sf.jsqlparser.util.deparser.StatementDeParser;
import org.shaneking.skava.sk.collect.Tuple;

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
