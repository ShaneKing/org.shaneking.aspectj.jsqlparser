/*
 * @(#)LingSutes.java		Created at 2018/2/3
 *
 * Copyright (c) ShaneKing All rights reserved.
 * ShaneKing PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package sktest.jsqlparser;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import sktest.jsqlparser.util.SensitiveItemsFinderTest;
import sktest.jsqlparser.util.TableNamesFinderTest;
import sktest.jsqlparser.util.replacer.SensitiveExpressionReplacerTest;
import sktest.jsqlparser.util.replacer.TableNamesSelectReplacerTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({SensitiveItemsFinderTest.class, SensitiveExpressionReplacerTest.class, TableNamesFinderTest.class, TableNamesSelectReplacerTest.class})
public class JSqlParserSuites {
}
