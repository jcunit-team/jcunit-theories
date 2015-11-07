package com.github.dakusui.jcunit.theories;

import com.github.dakusui.jcunit.core.Checks;
import com.github.dakusui.jcunit.core.tuples.Tuple;
import com.github.dakusui.jcunit.exceptions.UndefinedSymbol;
import com.github.dakusui.jcunit.plugins.constraintmanagers.ConstraintManagerBase;
import com.github.dakusui.jcunit.plugins.generators.IPO2TupleGenerator;
import com.github.dakusui.jcunit.runners.standard.annotations.Constraint;
import com.github.dakusui.jcunit.runners.standard.annotations.Generator;
import com.github.dakusui.jcunit.runners.standard.annotations.Value;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class TheoriesExample2 {
  @DataPoints("posInt")
  public static int[] positiveIntegers() {
    return new int[] {
        1, 2, 3
    };
  }

  @DataPoints("negInt")
  public static int[] negativeIntegers() {
    return new int[] {
        -1, -2, -3
    };
  }

  @DataPoints("posLong")
  public static long[] posLongs() {
    return new long[] {
        100, 200, 300
    };
  }

  @DataPoints("negLong")
  public static long[] negLongs() {
    return new long[] {
        -100, -200, -300
    };
  }

  public static class CM extends ConstraintManagerBase {
    @Override
    public boolean check(Tuple tuple) throws UndefinedSymbol {
      Checks.checksymbols(tuple, "a", "b");
      return (Integer) tuple.get("a") + (Integer) tuple.get("b") == 0;
    }
  }

  @Theory
  @GenerateSuiteWith(
      generator = @Generator(value = IPO2TupleGenerator.class, params = { @Value("3") }),
      constraint = @Constraint(CM.class)
  )
  public void test1(
      @FromDataPoints("posInt") @Name("a") int a,
      @FromDataPoints("negInt") @Name("b") int b,
      @FromDataPoints("posLong") @Name("c") long c,
      @FromDataPoints("negLong") @Name("d") long d
  ) throws Exception {
    System.out.printf("a=%s, b=%s, c=%d, d=%d%n", a, b, c, d);
  }

  @Theory
  @GenerateSuiteWith(generator = @Generator(value = IPO2TupleGenerator.class, params = { @Value("2") }))
  public void test2(
      @FromDataPoints("posInt") int a,
      @FromDataPoints("negInt") int b
  ) throws Exception {
    System.out.printf("a=%s, b=%s%n", a, b);
  }
}
