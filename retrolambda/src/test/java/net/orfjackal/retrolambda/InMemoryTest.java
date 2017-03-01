package net.orfjackal.retrolambda;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.orfjackal.retrolambda.files.ClassSaver;
import net.orfjackal.retrolambda.interfaces.ClassHierarchyAnalyzer;
import net.orfjackal.retrolambda.lambdas.LambdaClassDumper;
import net.orfjackal.retrolambda.lambdas.LambdaClassSaver;
import static org.junit.Assert.*;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Jaroslav Tulach
 */
public class InMemoryTest {
    @Test public void convertSingleClass() throws Exception {
        InputStream is = InMemoryTest.class.getResourceAsStream("UseLambda.class");
        byte[] arr = new byte[is.available()];
        int len = is.read(arr);
        assertEquals("Read just enough", arr.length, len);
        ClassLoader classes = InMemoryTest.class.getClassLoader();
        int bytecodeVersion = Opcodes.V1_7;

        Map<String,byte[]> classFiles = Retrolambda.reify(bytecodeVersion, classes, arr);

        ClassLoader l = new ClassLoader(UseLambda.class.getClassLoader().getParent()) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] arr = classFiles.get(name.replace('.', '/'));
                if (arr == null) {
                    return null;
                }
                return defineClass(name, arr, 0, arr.length);
            }
        };

        Class<?> useLambda = l.loadClass(UseLambda.class.getName());
        Object ret = useLambda.getMethod("fourtyTwo").invoke(null);
        assertEquals("Computed ok", 42, ret);
    }

}
