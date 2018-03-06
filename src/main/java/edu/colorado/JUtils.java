package edu.colorado;

import soot.tagkit.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by s on 10/17/16.
 */
public class JUtils {
    public static List<String> getClasses(String pathTojar)throws FileNotFoundException, IOException{
        List<String> classNames = new ArrayList<String>();
        ZipInputStream zip = new ZipInputStream(new FileInputStream(pathTojar));
        for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
            if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                // This ZipEntry represents a class. Now, what class does it represent?
                String className = entry.getName().replace('/', '.'); // including ".class"
                classNames.add(className.substring(0, className.length() - ".class".length()));
            }
        }
        return classNames;
    }
      /** Returns the line number of the code element host
   * or 0 if the line number tag does not exists
   *
   * @param
   * @return the line number of host if it exsits, 0 otherwise
   */
  public static int getLineNumber(Host code)
  {
    int lineNumber = 0;

//      for(code.getTags()
    /* solution that should works both on bytecode and on sources */
    Tag lineNumberTag = code.getTag("SourceLnPosTag");
    if (null != lineNumberTag && lineNumberTag instanceof SourceLnPosTag) {
      lineNumber = ((SourceLnPosTag) lineNumberTag).startLn();
    }
    else {
      lineNumberTag = code.getTag("LineNumberTag");
      if (null != lineNumberTag && lineNumberTag instanceof LineNumberTag) {
        lineNumber = ((LineNumberTag) lineNumberTag).getLineNumber();
      }
    }

    return lineNumber;
  }
}
