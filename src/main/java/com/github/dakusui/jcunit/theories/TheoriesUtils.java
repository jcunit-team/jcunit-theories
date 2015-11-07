package com.github.dakusui.jcunit.theories;

import com.github.dakusui.jcunit.core.Utils;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

import java.io.*;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.github.dakusui.jcunit.core.Checks.*;

public class TheoriesUtils {
  private static final File JCUNIT_THEORIES_DIR = new File(System.getProperty("user.dir"), ".jcunit-theories");

  private TheoriesUtils() {
  }

  public static long lastModifiedOf(Class<?> c) {
    URL url = checknotnull(c.getProtectionDomain().getCodeSource()).getLocation();
    checkcond("file".equals(url.getProtocol()));
    File localPath = new File(url.getPath());
    checkcond(
        localPath.exists() && (localPath.isFile() || localPath.isDirectory()),
        "Unsupported type of file (e.g., named pipe, device, etc) is returned as a class file.");
    long d;
    if (localPath.isFile()) {
      d = localPath.lastModified();
    } else {
      File classFile = new File(localPath, relativePathToClassFile(c));
      checkcond(classFile.exists() && classFile.isFile());
      d = classFile.lastModified();
    }
    return d;
  }

  private static String relativePathToClassFile(Class c) {
    return Utils.join("/", c.getName().split("\\.")) + ".class";
  }

  public static File getFailedestsFileOf(TestClass testClass, FrameworkMethod method) {
    return
        new File(
            JCUNIT_THEORIES_DIR,
            testClass.getJavaClass().getName() + "#" + method.getName() + "(" +
                Utils.join(
                    ",",
                    new Utils.Formatter<Class<?>>() {
                      @Override
                      public String format(Class<?> elem) {
                        return elem.getName();
                      }
                    },
                    method.getMethod().getParameterTypes()) + ").failed");
  }

  public static List<Integer> readRecordFileOf(TestClass testClass, FrameworkMethod method) {
    List<Integer> ret = new LinkedList<Integer>();
    File failedTestsFile = getFailedestsFileOf(testClass, method);
    if (failedTestsFile.exists()) {
      try {
        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(failedTestsFile)));
        String line;
        while ((line = r.readLine()) != null) {
          ret.add(Integer.parseInt(line));
        }
      } catch (FileNotFoundException e) {
        throw wrap(e);
      } catch (IOException e) {
        throw wrap(e);
      }
      return ret;
    } else {
      return Collections.emptyList();
    }
  }
}
