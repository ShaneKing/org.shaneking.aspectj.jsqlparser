package sktest.aspectj.jsqlparser;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import sktest.aspectj.jsqlparser.util.SensitiveItemsFinderTest;
import sktest.aspectj.jsqlparser.util.TableNamesFinderTest;
import sktest.aspectj.jsqlparser.util.replacer.SensitiveExpressionReplacerTest;
import sktest.aspectj.jsqlparser.util.replacer.TableNamesSelectReplacerTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({SensitiveItemsFinderTest.class, SensitiveExpressionReplacerTest.class, TableNamesFinderTest.class, TableNamesSelectReplacerTest.class})
public class JSqlParserSuites {
}
