package com.github.dakusui.jcunit.theories;

import com.github.dakusui.jcunit.core.Checks;
import com.github.dakusui.jcunit.core.factor.Factor;
import com.github.dakusui.jcunit.core.factor.Factors;
import com.github.dakusui.jcunit.core.tuples.Tuple;
import com.github.dakusui.jcunit.exceptions.UndefinedSymbol;
import com.github.dakusui.jcunit.plugins.Plugin;
import com.github.dakusui.jcunit.plugins.constraintmanagers.ConstraintManager;
import com.github.dakusui.jcunit.plugins.constraintmanagers.ConstraintManagerBase;
import com.github.dakusui.jcunit.plugins.generators.IPO2TupleGenerator;
import com.github.dakusui.jcunit.plugins.generators.TupleGenerator;
import com.github.dakusui.jcunit.runners.standard.annotations.Constraint;
import com.github.dakusui.jcunit.runners.standard.annotations.Generator;
import com.github.dakusui.jcunit.runners.standard.annotations.Value;
import org.junit.Assert;
import org.junit.AssumptionViolatedException;
import org.junit.contrib.theories.PotentialAssignment;
import org.junit.contrib.theories.Theory;
import org.junit.contrib.theories.internal.Assignments;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static com.github.dakusui.jcunit.core.Checks.wrap;

public class Theories extends org.junit.contrib.theories.Theories {
  public Theories(Class<?> klass) throws InitializationError {
    super(klass);
  }

  @Override
  protected void validateTestMethods(List<Throwable> errors) {
    super.validateTestMethods(errors);
    for (FrameworkMethod method : computeTestMethods()) {
      Collection<String> names = new ArrayList<String>();
      for (Name each : getNameAnnotationsFromMethod(method, this.getTestClass())) {
        if (names.contains(each.value())) {
          errors.add(new Error("Parameter name '" + each.value() + "' is used more than once in " + method.getName()));
        }
        names.add(each.value());
      }
    }
  }

  private List<Name> getNameAnnotationsFromMethod(FrameworkMethod method, TestClass testClass) {
    List<Name> ret = new ArrayList<Name>();
    try {
      Assignments assignments = Assignments.allUnassigned(method.getMethod(), testClass);
      while (!assignments.isComplete()) {
        Name name = assignments.nextUnassigned().getAnnotation(Name.class);
        if (name != null) {
          ret.add(name);
        }
        assignments = assignments.assignNext(null);
      }
    } catch (Throwable throwable) {
      throw wrap(throwable);
    }
    return ret;
  }

  @Override
  public Statement methodBlock(final FrameworkMethod method) {
    Factors.Builder factorsBuilder = new Factors.Builder();
    final TestClass testClass = getTestClass();
    try {
      Assignments assignments = Assignments.allUnassigned(method.getMethod(), testClass);
      int i = 0;
      while (!assignments.isComplete()) {
        List<PotentialAssignment> potentials = assignments.potentialsForNextUnassigned();
        String prefix = String.format("param%03d", i);
        Name name = assignments.nextUnassigned().getAnnotation(Name.class);
        ////
        // Guarantee the factors names are generated in dictionary order.
        String factorName = String.format("%s:%s", prefix, name != null ? name.value() : prefix);
        Factor.Builder factorBuilder = new Factor.Builder(factorName);
        for (PotentialAssignment each : potentials) {
          factorBuilder.addLevel(each);
        }
        factorsBuilder.add(factorBuilder.build());
        assignments = assignments.assignNext(null);
        i++;
      }
    } catch (Throwable throwable) {
      throw wrap(throwable);
    }
    final TupleGenerator tg = createTupleGenerator(method.getMethod());
    tg.setFactors(factorsBuilder.build());
    tg.init();
    return new TheoryAnchor(method, testClass) {
      public List<Throwable> errors;
      PrintStream ps;
      int successes = 0;
      int index = 0;
      List<AssumptionViolatedException> fInvalidParameters = new ArrayList<AssumptionViolatedException>();

      @Override
      public void evaluate() throws Throwable {
        List<Integer> testIDsFailedLastTime = TheoriesUtils.readRecordFileOf(testClass, method);
        ps = createPrintOutputStreamForTestResult(testClass, method);
        try {
          this.errors = new LinkedList<Throwable>();
          if (testIDsFailedLastTime.isEmpty()) {
            for (index = 0; index < tg.size(); index++) {
              runWithCompleteAssignment(tuple2assignments(method.getMethod(), testClass, tg.get(index)));
            }
          } else {
            for (Integer i : testIDsFailedLastTime) {
              index = i;
              runWithCompleteAssignment(tuple2assignments(method.getMethod(), testClass, tg.get(index)));
            }
          }
          if (!this.errors.isEmpty()) {
            for (Throwable each : this.errors) {
              each.getMessage();
            }
            throw this.errors.get(0);
          }
        } finally {
          ps.close();
        }
        //if this test method is not annotated with Theory, then no successes is a valid case
        boolean hasTheoryAnnotation = method.getAnnotation(Theory.class) != null;
        if (successes == 0 && hasTheoryAnnotation) {
          Assert.fail("Never found parameters that satisfied method assumptions.  Violated assumptions: "
              + fInvalidParameters);
        }
      }

      @Override
      protected void runWithCompleteAssignment(final Assignments complete) throws Throwable {
        try {
          super.runWithCompleteAssignment(complete);
        } catch (Throwable t) {
          this.errors.add(t);
        }
      }


        @Override
      protected void reportParameterizedError(Throwable e, Object... params) throws Throwable {
        ps.println(this.index);
        super.reportParameterizedError(e, params);
      }

      @Override
      protected void handleAssumptionViolation(AssumptionViolatedException e) {
        fInvalidParameters.add(e);
      }

      @Override
      protected void handleDataPointSuccess() {
        successes++;
      }
    }

        ;
  }

  private PrintStream createPrintOutputStreamForTestResult(TestClass testClass, FrameworkMethod method) {
    try {
      File outfile =TheoriesUtils.getFailedestsFileOf(testClass, method);
      outfile.getParentFile().mkdirs();
      return new PrintStream(new FileOutputStream(outfile));
    } catch (FileNotFoundException e) {
      throw wrap(e);
    }
  }


  protected TupleGenerator createTupleGenerator(final Method method) {
    GenerateSuiteWith tgAnn = method.getAnnotation(GenerateSuiteWith.class);
    TupleGenerator tg;
    final ConstraintManager cm;
    if (tgAnn != null) {
      tg = createTupleGenerator(tgAnn.generator());
      cm = createConstraintManager(tgAnn.constraint());
    } else {
      tg = new IPO2TupleGenerator(2);
      cm = ConstraintManager.DEFAULT_CONSTRAINT_MANAGER;
    }
    tg.setConstraintManager(new ConstraintManagerBase() {
      ConstraintManager baseCM = cm;

      @Override
      public boolean check(Tuple tuple) throws UndefinedSymbol {
        return baseCM.check(convert(tuple));
      }

      private Tuple convert(Tuple tuple) {
        Tuple.Builder b = new Tuple.Builder();
        for (String each : tuple.keySet()) {
          try {
            b.put(each.substring(each.indexOf(':') + 1), ((PotentialAssignment) tuple.get(each)).getValue());
          } catch (PotentialAssignment.CouldNotGenerateValueException e) {
            throw wrap(e);
          }
        }
        return b.build();
      }
    });
    return tg;
  }

  protected ConstraintManager createConstraintManager(Constraint constraintAnnotation) {
    Value.Resolver resolver = new Value.Resolver();
    //noinspection unchecked
    return Checks.cast(
        ConstraintManager.class,
        new Plugin.Factory<ConstraintManager, Value>(
            (Class<ConstraintManager>) constraintAnnotation.value(),
            resolver)
            .create(constraintAnnotation.params()));
  }

  private TupleGenerator createTupleGenerator(final Generator generatorAnnotation) {
    Value.Resolver resolver = new Value.Resolver();
    //noinspection unchecked
    return Checks.cast(
        TupleGenerator.class,
        new Plugin.Factory<TupleGenerator, Value>(
            (Class<TupleGenerator>) generatorAnnotation.value(),
            resolver)
            .create(generatorAnnotation.params()
            ));
  }

  private static Assignments tuple2assignments(Method method, TestClass testClass, Tuple t) {
    // Tuple generator generates dictionary order guaranteed tuples.
    try {
      Assignments ret = Assignments.allUnassigned(method, testClass);
      for (Object each : t.values()) {
        ret = ret.assignNext(Checks.cast(PotentialAssignment.class, each));
      }
      return ret;
    } catch (Exception e) {
      throw wrap(e);
    }
  }
}
