package com.github.dakusui.jcunit.theories;

import com.github.dakusui.jcunit.runners.standard.JCUnit;
import org.junit.Test;

import java.util.Date;

/**
 * Created by hiroshi on 11/4/15.
 */
public class TheoriesUtilsTest {
  static class Inner {
  }

  public static void main(String... args) {

  }

  @Test
  public void test1() {
    System.out.println(new Date(TheoriesUtils.lastModifiedOf(TheoriesUtilsTest.class)));

  }

  @Test
  public void test2() {
    System.out.println(new Date(TheoriesUtils.lastModifiedOf(TheoriesUtilsTest.Inner.class)));

  }

  @Test
  public void test3() {
    System.out.println(new Date(TheoriesUtils.lastModifiedOf(new Runnable() {
      @Override
      public void run() {

      }
    }.getClass())));
  }

  @Test
  public void test4() {
    System.out.println(new Date(TheoriesUtils.lastModifiedOf(JCUnit.class)));
  }

}
