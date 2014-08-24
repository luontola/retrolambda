// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.defaultmethods;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Stream;

/**
 * Created by arneball on 2014-08-12.
 */
public class ClassModifier extends ClassVisitor implements Opcodes {
	private final int bytecodeVersion;
	private String[] interfaces;

	private Set<VisitedMethod> visitedMethods = new HashSet<>();
	private Set<MethodContainer> defaultMethods = new HashSet<>();

	public ClassModifier(int bytecodeVersion, ClassVisitor cv) {
		super(ASM5, cv);
		this.bytecodeVersion = bytecodeVersion;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		boolean isClass = (access & ACC_INTERFACE) == 0;
		if(isClass) {
			defaultMethods = getMethodsToImplement(interfaces, signature);
		}
		this.interfaces = interfaces;
		System.out.println("Class is " + name + ", non abstract " + isClass + ", Interfaces are " + Arrays.toString(interfaces) + "" +
				", default methods are " + defaultMethods);
		super.visit(bytecodeVersion, access, name, signature, superName, interfaces);
	}

	private static Set<MethodContainer> getMethodsToImplement(String[] interfaces, String sig) {
		Set<MethodContainer> tmp = new HashSet<>();
		for(String iff : interfaces) {
			tmp.addAll(getMethodsToImplement(iff, sig));
		}
		return tmp;
	}

	private static Set<MethodContainer> getMethodsToImplement(String interfac, String sig) {
		Class<?> ifClass = Helpers.loadClass(interfac);
		Method[] tmp = ifClass.getMethods();
		System.out.println("Interface: " + interfac + ", methods: " + Arrays.toString(tmp));
		Set<MethodContainer> toReturn = new HashSet<>();
		for(Method m : tmp) {
			if(!Modifier.isAbstract(m.getModifiers())) {
				System.out.println("NEED TO CREATE PROXY TO " + interfac + sig + "\n" + m.getDeclaringClass());
				MethodContainer e = new MethodContainer(m.getName(), Type.getMethodDescriptor(m), m.getDeclaringClass().getName().replace(".", "/"), sig, getExceptions(m));
				toReturn.add(e);
			}
		}
		return toReturn;
	}

	private static String[] getExceptions(Method m) {
		Class<?>[] exceptionTypes = m.getExceptionTypes();
		String[] tmp = new String[exceptionTypes.length];
		for(int i = 0; i < exceptionTypes.length; i++) {
			tmp[i] = exceptionTypes[i].getName().replace(".", "/");
		}
		return tmp;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		visitedMethods.add(new VisitedMethod(name, desc));
		return new InterfaceToHelperRewriter(super.visitMethod(access, name, desc, signature, exceptions));
	}

	@Override
	public void visitEnd() {
		for(MethodContainer m : defaultMethods) {
			if(visitedMethods.contains(new VisitedMethod(m.methodName, m.methodDesc))) {
				continue;
			}
			System.out.println("VISITEND, CREATING PROXY " + m);
			MethodVisitor tmp = super.visitMethod(ACC_PUBLIC, m.methodName, m.methodDesc, m.signature, m.exceptions);
			tmp.visitVarInsn(ALOAD, 0);
			int i = 1;
			for(Type arg : Type.getArgumentTypes(m.methodDesc)) {
				tmp.visitVarInsn(getVarIns(arg), i++);
			}
			String rightInterace = findRightInterace(m, interfaces);
			System.out.println("It thinks that the right interface is " + rightInterace);
			String mDesc = Helpers.addParam(m.methodDesc, rightInterace);
			tmp.visitMethodInsn(INVOKESTATIC, rightInterace + "$helper", m.methodName, mDesc, false);
			tmp.visitInsn(getReturnIns(Type.getReturnType(m.methodDesc)));
			tmp.visitMaxs(0, 0);
			tmp.visitEnd();
		}
		super.visitEnd();
	}

	public static final Comparator<Method> COMPARATOR = (o1, o2) -> {
		boolean o1iso2 = o1.getDeclaringClass().isAssignableFrom(o2.getDeclaringClass());
		boolean o2iso1 = o2.getDeclaringClass().isAssignableFrom(o1.getDeclaringClass());
		if(o1iso2 && o2iso1) {
			return 0;
		} else if(o2iso1)
			return -1;
		else return 1;
	};

	public static String findRightInterace(MethodContainer methodContainer, String[] interfaces) {
		System.out.println("Find right interfaces for " + methodContainer + " " + Arrays.toString(interfaces));
		return Stream.of(interfaces)
				.map(Helpers::loadClass)
				.flatMap(i -> flattenInterfaces(i).stream())
				.flatMap(i -> Stream.of(i.getMethods()))
				.filter(Method::isDefault)
				.filter(m -> Type.getMethodDescriptor(m).equals(methodContainer.methodDesc))
				.min(COMPARATOR)
				.map(Method::getDeclaringClass)
				.map(Class::getName)
				.map(s -> s.replace(".", "/"))
				.orElseThrow(NullPointerException::new);
	}

	private static List<Class<?>> flattenInterfaces(Class<?> iff) {
		List<Class<?>> tmp = new ArrayList<>();
		if(iff != null){
			tmp.add(iff);
			for(Class<?> stream : iff.getInterfaces()) {
				tmp.addAll(flattenInterfaces(stream));
			}
		}
		return tmp;
	}

	static int getReturnIns(Type arg) {
		if(arg == Type.INT_TYPE || arg == Type.BOOLEAN_TYPE || arg == Type.SHORT_TYPE) {
			return IRETURN;
		}
		else if(arg == Type.LONG_TYPE) {
			return LRETURN;
		}
		else if(arg == Type.DOUBLE_TYPE) {
			return DRETURN;
		}
		else if(arg == Type.FLOAT_TYPE) {
			return FRETURN;
		}
		else if(arg == Type.VOID_TYPE) {
			return RETURN;
		}
		else {
			return ARETURN;
		}
	}

	static int getVarIns(Type arg) {
		if(arg == Type.INT_TYPE || arg == Type.BOOLEAN_TYPE || arg == Type.SHORT_TYPE) {
			return ILOAD;
		}
		else if(arg == Type.DOUBLE_TYPE) {
			return DLOAD;
		}
		else if(arg == Type.FLOAT_TYPE) {
			return FLOAD;
		}
		else {
			return ALOAD;
		}
	}

}