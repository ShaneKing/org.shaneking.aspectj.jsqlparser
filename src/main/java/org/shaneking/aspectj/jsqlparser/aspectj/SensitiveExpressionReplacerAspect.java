package org.shaneking.aspectj.jsqlparser.aspectj;

import com.google.common.base.Strings;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.truncate.Truncate;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.shaneking.aspectj.jsqlparser.annotation.SensitiveExpressionReplacerPath;
import org.shaneking.aspectj.jsqlparser.util.SensitiveItemsFinder;
import org.shaneking.aspectj.jsqlparser.util.replacer.SensitiveExpressionReplacer;
import org.shaneking.aspectj.jsqlparser.util.replacer.SensitiveStatementReplacer;
import org.shaneking.aspectj.jsqlparser.util.replacer.TableNamesSelectReplacer;

import java.util.Stack;

@Aspect
public class SensitiveExpressionReplacerAspect {

  @Pointcut("execution(@org.shaneking.aspectj.jsqlparser.annotation.SensitiveExpressionReplacerPath * *..*.*(..))")
  private void pointcut() {
  }

  @Around("pointcut() && @annotation(path)")
  public Object aroundPath(ProceedingJoinPoint joinPoint, SensitiveExpressionReplacerPath path) throws Throwable {
    Stack<String> handlePathStack = null;
    Object originInstance = joinPoint.getThis();
    if (originInstance == null) {
      originInstance = joinPoint.getTarget();
    }
    if (originInstance instanceof SensitiveExpressionReplacer) {
      handlePathStack = ((SensitiveExpressionReplacer) originInstance).getPathStack();
    } else if (originInstance instanceof TableNamesSelectReplacer) {
      ExpressionVisitor expressionVisitor = ((TableNamesSelectReplacer) originInstance).getExpressionVisitor();
      if (expressionVisitor instanceof SensitiveExpressionReplacer) {
        handlePathStack = ((SensitiveExpressionReplacer) expressionVisitor).getPathStack();
      }
    } else if (originInstance instanceof SensitiveStatementReplacer) {
      ExpressionVisitor expressionVisitor = ((SensitiveStatementReplacer) originInstance).getExpressionReplacer();
      if (expressionVisitor instanceof SensitiveExpressionReplacer) {
        handlePathStack = ((SensitiveExpressionReplacer) expressionVisitor).getPathStack();
      }
    }
    Object arg0 = joinPoint.getArgs()[0];
    if (handlePathStack != null) {
      if (path == null || Strings.isNullOrEmpty(path.value())) {
        if (arg0 instanceof Select) {
          handlePathStack.push(SensitiveItemsFinder.PATH_OF_SELECT);
        } else if (arg0 instanceof SelectExpressionItem) {
          handlePathStack.push(SensitiveItemsFinder.PATH_OF_SELECT_EXPRESSION_ITEM);
        } else if (arg0 instanceof SubSelect) {
          handlePathStack.push(SensitiveItemsFinder.PATH_OF_SUB_SELECT);
        } else if (arg0 instanceof WithItem) {
          handlePathStack.push(SensitiveItemsFinder.PATH_OF_WITH_ITEM);
        } else if (arg0 instanceof SubJoin) {
          handlePathStack.push(SensitiveItemsFinder.PATH_OF_FROM_ITEM);
        } else if (arg0 instanceof LateralSubSelect) {
          handlePathStack.push(SensitiveItemsFinder.PATH_OF_FROM_ITEM);
        } else if (arg0 instanceof ParenthesisFromItem) {
          handlePathStack.push(SensitiveItemsFinder.PATH_OF_FROM_ITEM);
        } else if (arg0 instanceof Insert) {
          handlePathStack.push(SensitiveItemsFinder.PATH_OF_INSERT);
        } else if (arg0 instanceof Truncate) {
          handlePathStack.push(SensitiveItemsFinder.PATH_OF_TRUNCATE);
        }
      } else {
        handlePathStack.push(path.value());
      }
    }
    Object result = joinPoint.proceed();
    if (handlePathStack != null) {
      if (arg0 instanceof Select || arg0 instanceof SelectExpressionItem || arg0 instanceof SubSelect || arg0 instanceof WithItem || arg0 instanceof SubJoin || arg0 instanceof LateralSubSelect || arg0 instanceof ParenthesisFromItem || arg0 instanceof Insert || arg0 instanceof Truncate) {
        handlePathStack.pop();
      }
    }
    return result;
  }
}
