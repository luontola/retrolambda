// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.defaultmethods;

import org.objectweb.asm.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * Created by arneball on 2014-08-12.
 */
public class InterfaceModifier extends ClassVisitor implements Opcodes {
    private final int targetByteCode;
    private String className;
    private boolean isInterface;
    private ClassWriter helperClassVisitor;
    private String[] interfaces;
    private List<MethodContainer> methodContainers = new ArrayList<>();

    public InterfaceModifier(ClassVisitor classWriter, int targetBytodeCode) {
        super(ASM5, classWriter);
        this.targetByteCode = targetBytodeCode;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        isInterface = (access & ACC_INTERFACE) != 0;
        className = name;
        this.interfaces = Stream.of(interfaces)
                .filter(Helpers::interfaceBelongsToUs)
                .toArray(String[]::new);
        // force load this class, if not, we are overwriting the interface and succeeding loads will see the purely abstract onoe
        Helpers.loadClass(name);
        System.out.println("Visiting interface " + name);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        boolean isConcrete = (access & ACC_ABSTRACT) == 0;
        boolean isStatic = (access & ACC_STATIC) != 0;
        if (isConcrete && isInterface && !isStatic) {
            super.visitMethod(access | ACC_ABSTRACT, name, desc, signature, exceptions);
            MethodVisitor tmp = getHelperClassVisitor().visitMethod(
                    access | ACC_STATIC,
                    name,
                    Helpers.addParam(desc, className),
                    signature,
                    exceptions);
            methodContainers.add(new MethodContainer(name, desc, null, signature, exceptions));
            return new InterfaceToHelperRewriter(new BodyMover(tmp));
        } else if (isStatic && isInterface) {
            return getHelperClassVisitor().visitMethod(access, name + "$static", desc, signature, exceptions);
        } else {
            return super.visitMethod(access, name, desc, signature, exceptions);
        }
    }

    @Override
    public void visitEnd() {
        Path newPath = Helpers.config.getOutputDir();
        ArrayList<Method> allMethods = Stream.of(interfaces)
                .map(Helpers::loadClass)
                .flatMap(c -> Stream.of(c.getMethods()))
                .collect(Collectors.toCollection(ArrayList::new));
        methodContainers.stream().forEach(m -> {
            allMethods.stream()
                    .filter(meth -> bridgeNeeded(meth, m))
                    .forEach(meth -> createBridge(meth, m));
        });
        getHelperClassVisitor().visitEnd();
        super.visitEnd();
        try {
            Files.createDirectories(newPath.getParent());
            Files.write(newPath.resolve(helperClassName() + ".class"), getHelperClassVisitor().toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createBridge(Method meth, MethodContainer m) {
        int access = ACC_PUBLIC | ACC_STATIC | ACC_BRIDGE;
        String desc = Helpers.addParam(m.methodDesc, className);
        String returnType = Type.getReturnType(meth).getInternalName();
        desc = Helpers.changeReturnType(desc, returnType);
        MethodVisitor tmp = getHelperClassVisitor().visitMethod(access, m.methodName, desc, m.signature, m.exceptions);
        tmp.visitVarInsn(ALOAD, 0);
        int i = 1;
        for (Type arg : Type.getArgumentTypes(m.methodDesc)) {
            tmp.visitVarInsn(arg.getOpcode(ILOAD), i++);
        }
        String mDesc = Helpers.addParam(m.methodDesc, className);
        tmp.visitMethodInsn(INVOKESTATIC, className + "$helper", m.methodName, mDesc, false);
        tmp.visitInsn(ARETURN);
        tmp.visitMaxs(0, 0);
        tmp.visitEnd();
    }

    private static boolean bridgeNeeded(Method method, MethodContainer methodContainer) {
        Type[] methodArgumentns = Type.getArgumentTypes(method);
        Type[] containerArguments = Type.getArgumentTypes(methodContainer.methodDesc);
        boolean argsEquals = Arrays.equals(methodArgumentns, containerArguments);
        boolean nameEquals = method.getName().equals(methodContainer.methodName);
        log("" + method + " should be equal to " + methodContainer);
        log("Args equal = " + argsEquals + ", nameEquals = " + nameEquals);
        if (!argsEquals || !nameEquals) {
            return false;
        }
        Type containerReturnType = Type.getReturnType(methodContainer.methodDesc);
        Type methodReturnType = Type.getReturnType(method);
        boolean containerRetPrimite = Helpers.isPrimitive(containerReturnType);
        boolean methodRetPrimitive = Helpers.isPrimitive(methodReturnType);
        log("Container ret primitive = " + containerRetPrimite + ", methodRetPrimitive = " + methodRetPrimitive);
        if (containerRetPrimite || methodRetPrimitive) {
            log("Either one primitive, no bridge needed");
            return false;
        }

        Class<?> returnType = method.getReturnType();
        log("returnType = " + returnType + ", isPrimitive = " + returnType.isPrimitive());
        return returnType.isAssignableFrom(Helpers.loadClass(containerReturnType.getClassName()));
    }

    private static void log(String s) {
        System.out.println("interfaceModifier ======= " + s);
    }

    private ClassWriter getHelperClassVisitor() {
        return helperClassVisitor == null ? helperClassVisitor = mkHelperClassVisitor() : helperClassVisitor;
    }

    private ClassWriter mkHelperClassVisitor() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(targetByteCode,
                ACC_PUBLIC + ACC_SUPER,
                helperClassName(),
                null,
                "java/lang/Object",
                null);
        MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, "<init>", "()V", null, null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        return cw;
    }

    private String helperClassName() {
        return className + "$helper";
    }

    private static class BodyMover extends MethodVisitor {
        BodyMover(MethodVisitor newMethod) {
            super(ASM5, newMethod);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (opcode == INVOKESPECIAL && itf) {
                super.visitMethodInsn(INVOKESTATIC, owner + "$helper", name, Helpers.addParam(desc, owner), false);
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }

    }
}
