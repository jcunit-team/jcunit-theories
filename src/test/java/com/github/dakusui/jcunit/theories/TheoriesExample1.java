package com.github.dakusui.jcunit.theories;


import org.hamcrest.CoreMatchers;
import org.junit.contrib.theories.DataPoints;
import org.junit.contrib.theories.Theory;
import org.junit.runner.RunWith;

import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Theories.class)
public class TheoriesExample1 {
  @DataPoints("posInt")
  public static int[] positiveIntegers() {
    return new int[] {
        1, 2, 3
    };
  }

  @Theory
  public void test1(
      int a,
      int b,
      int c,
      int d
  ) throws Exception {
    System.out.printf("a=%s, b=%s, c=%d, d=%d%n", a, b, c, d);
    assertThat(a, CoreMatchers.is(1));
  }
}
