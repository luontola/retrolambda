package net.orfjackal.retrolambda;

import org.objectweb.asm.*;

public class ClassReader2 extends ClassReader {
  public ClassReader2(byte[] b) {
    super(b);
  }

  @Override
  protected Label readLabel(int offset, Label[] labels) {
    return super.readLabel(Math.min(offset, labels.length - 1), labels);
  }
}
