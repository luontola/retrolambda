import java.nio.file.Files

import Implicits._
import org.objectweb.asm.Opcodes._
import org.objectweb.asm._

import scala.collection.{mutable => m}

class InterfaceModifier(classWriter: ClassWriter, targetByteCode: Int = Opcodes.V1_6) extends ClassVisitor(ASM5, classWriter) with Opcodes {
  private var cName: String = _
  private var isInterface = false
  private lazy val helperClassVisitor = mkHelperClass
  private lazy val helperClassName = cName + "helper"

  override def visitMethod(access: Int, name: String, desc: String, signature: String, exceptions: Array[String]): MethodVisitor = {
    val methConcrete = (ACC_ABSTRACT & access) == 0
    val isStatic = (ACC_STATIC & access) != 0
    (methConcrete, isInterface, isStatic) match {
      case (true, true, false) => // concrete interface method
        super.visitMethod(access | ACC_ABSTRACT, name, desc, signature, exceptions)
        val tmp = helperClassVisitor.visitMethod(access | ACC_STATIC, name, desc.addParam(cName), signature, exceptions)
        new BodyStripper(tmp)
      case (true, true, true) => // static interface method
        helperClassVisitor.visitMethod(access, name + "$static", desc, signature, exceptions)
      case _ =>
        super.visitMethod(access, name, desc, signature, exceptions)
    }
  }

  override def visitEnd() = {
    val newPath = AsmTest.output.resolve(helperClassName + ".class")
    println("CREATING HELPER AT " + newPath)
    helperClassVisitor.visitEnd()
    super.visitEnd()
    Files.createDirectories(newPath.getParent)
    Files.write(newPath, helperClassVisitor.toByteArray)
  }

  private def mkHelperClass = {
    val cw = new ClassWriter(ClassWriter.COMPUTE_MAXS)

    cw.visit(targetByteCode,
      ACC_PUBLIC + ACC_SUPER,
      helperClassName,
      null,
      "java/lang/Object",
      null)

    cw.visitSource(s"$cName.java", null)

    {
      val mv = cw.visitMethod(ACC_PRIVATE, "<init>", "()V", null, null)
      mv.visitVarInsn(ALOAD, 0)
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
      mv.visitInsn(RETURN)
      mv.visitMaxs(0, 0)
      mv.visitEnd()
    }
    cw
  }

  override def visit(version: Int, access: Int, name: String, signature: String, superName: String, interfaces: Array[String]) = {
    isInterface = (access & ACC_INTERFACE) != 0
    cName = name
    super.visit(targetByteCode, access, name, signature, superName, interfaces)
  }
}

// works, strips the body
class BodyStripper(newMethod: MethodVisitor) extends MethodVisitor(Opcodes.ASM5, newMethod) {
  override def visitEnd() = {
    newMethod.visitMaxs(0, 0)
    super.visitEnd()
  }
}
