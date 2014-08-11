/**
 * Created by arneball on 2014-07-29.
 */

import java.lang.reflect.{Method, Modifier}
import java.net.{URL, URLClassLoader}
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import org.objectweb.asm._
import collection.JavaConversions._

object AsmTest {
  lazy val (output: Path, cp: Array[URL], input: Path, bytecodeVersion: Int, ucl: URLClassLoader) = {
    val List(_input, _output, _cp, _bytecode) = List("inputDir", "outputDir", "classpath", "bytecodeVersion").map{ name =>
      System.getProperties.getProperty("retrometh." + name)
    }
    val byteCodeVersion = _bytecode match {
      case "1.7" | "7" | "51" => Opcodes.V1_7
      case "1.6" | "6" | "50" => Opcodes.V1_6
      case "1.5" | "5" | "49" => Opcodes.V1_5
      case _ => throw new Exception("BytecodeVersion must be 1.6, 6, 1.7 or 7")
    }
    val input = Paths.get(_input)
    val output = Option(_output).map{ Paths.get(_) }.getOrElse(input)

    val cp = _cp.split(System.getProperty("path.separator")).map{ p =>
      Paths.get(p).toUri.toURL
    } :+ input.toUri.toURL
    Implicits.println(s"Input: $input, Output: $output, classpath: ${cp.mkString}")
    (output, cp, input, byteCodeVersion, new URLClassLoader(cp))
  }

  def main(args: Array[String]): Unit = {
    Files.walkFileTree(input, new SimpleFileVisitor[Path] {
      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = file.toString.toLowerCase.endsWith("class") match {
        case true =>
          println("Found class file " + file)
          val bytecode = Files.readAllBytes(file)
          val wr = new ClassWriter(ClassWriter.COMPUTE_FRAMES)
          val stage1 = new InterfaceModifier(wr, bytecodeVersion)
          val stage2 = new ClassModifier(stage1, bytecodeVersion)
          val reader = new ClassReader(bytecode).accept(stage2, 0);
          val outputFile = output.resolve(input.relativize(file));
          Files.createDirectories(outputFile.getParent());
          Files.write(outputFile, wr.toByteArray);
          FileVisitResult.CONTINUE
        case that =>
          FileVisitResult.CONTINUE
      }
    })
  }
}

object Implicits {
  def println(str: String) = Predef.println("KALLE " + str)
  val Sig = raw"\((.*)\)(.*)".r
  implicit class StrWr(val str: String) extends AnyVal {
    def addParam(cl: String) = str match {
      case Sig(content, returntype) =>
        val tmp = s"(L${cl};$content)$returntype"
        println(s"Before $str, now $tmp")
        tmp
    }
    def getInternalClass = AsmTest.ucl.loadClass(str.replace("/", "."))
  }
}