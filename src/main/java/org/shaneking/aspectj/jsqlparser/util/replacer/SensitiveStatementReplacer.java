/*
 * @(#)SensitiveStatementReplacer.java		Created at 2018/9/23
 *
 * Copyright (c) ShaneKing All rights reserved.
 * ShaneKing PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package org.shaneking.aspectj.jsqlparser.util.replacer;

import lombok.Getter;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import org.shaneking.aspectj.jsqlparser.annotation.SensitiveExpressionReplacerPath;

public class SensitiveStatementReplacer extends StatementDeParser {

  @Getter
  private ExpressionDeParser expressionReplacer;

  public SensitiveStatementReplacer(ExpressionDeParser expressionDeParser, SelectDeParser selectDeParser, StringBuilder buffer) {
    super(expressionDeParser, selectDeParser, buffer);
    this.expressionReplacer = expressionDeParser;
  }

  @SensitiveExpressionReplacerPath
  @Override
  public void visit(Select select) {
    super.visit(select);
  }
}
