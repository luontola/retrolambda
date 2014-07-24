// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import com.google.common.io.ByteStreams;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.objectweb.asm.*;

import java.io.*;
import java.util.function.Function;

@Ignore
public class Java6MissingStackMapFrameFixerTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private byte[] originalBytecode;

    @Before
    public void setup() throws IOException {
        originalBytecode = getBytecode(GuineaPig.class);
    }

    public static class GuineaPig /*extends com.google.android.gms.maps.SupportMapFragment*/ {
    }

    @Test
    public void foo() throws Exception {
//        ASMifier.main(new String[]{SupportMapFragment.class.getName()});
    }

    @Test
    public void example_class_cannot_be_loaded_because_of_missing_stackmap_frame() throws Exception {
        byte[] bytecode = transform(
                originalBytecode,
                cv -> new StackMapFrameRemover(cv).setClassVersion(Opcodes.V1_6)
        );
        assertFailsVerification(bytecode);
    }

    @Test
    public void adds_stackmap_frames_to_Java_6_bytecode() throws Exception {
        byte[] bytecode = transform(
                originalBytecode,
                cv -> new StackMapFrameRemover(cv).setClassVersion(Opcodes.V1_6)
        );
        assertPassesVerification(bytecode);
    }

    @Test
    public void does_not_add_stackmap_frames_to_Java_7_or_higher_bytecode() throws Exception {
        byte[] bytecode = transform(
                originalBytecode,
                cv -> new StackMapFrameRemover(cv).setClassVersion(Opcodes.V1_7)
        );
        assertFailsVerification(bytecode);
    }


    // helpers

    private void assertFailsVerification(byte[] bytecode) throws Exception {
        thrown.expect(VerifyError.class);
        thrown.expectMessage("Expecting a stackmap frame at branch target");
        assertPassesVerification(bytecode);
    }

    private void assertPassesVerification(byte[] bytecode) throws Exception {
        Class<?> clazz = new TestingClassLoader().defineClass(bytecode);
        clazz.newInstance();
    }

    private static byte[] transform(byte[] originalBytecode, Function<ClassVisitor, ClassVisitor> fn) {
        ClassWriter cw = new ClassWriter(0);
        new ClassReader(originalBytecode).accept(fn.apply(cw), 0);
        return cw.toByteArray();
    }

    private static byte[] getBytecode(Class<?> clazz) throws IOException {
        String classFile = clazz.getName().replace(".", "/") + ".class";
        try (InputStream b = clazz.getClassLoader().getResourceAsStream(classFile)) {
            return ByteStreams.toByteArray(b);
        }
    }

    private static class TestingClassLoader extends ClassLoader {

        public Class<?> defineClass(byte[] bytecode) {
            ClassReader cr = new ClassReader(bytecode);
            String className = cr.getClassName().replace("/", ".");
            return this.defineClass(className, bytecode, 0, bytecode.length);
        }
    }

    private static class StackMapFrameRemover extends ClassVisitor {

        private int classVersion = 0;

        public StackMapFrameRemover(ClassVisitor cv) {
            super(Opcodes.ASM5, cv);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(classVersion, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            return new MethodVisitor(Opcodes.ASM5, super.visitMethod(access, name, desc, signature, exceptions)) {
                @Override
                public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
                    // remove stackmap frames
                }
            };
        }

        private StackMapFrameRemover setClassVersion(int classVersion) {
            this.classVersion = classVersion;
            return this;
        }
    }
}
