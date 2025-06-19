package io.github.tanice.twItemManager.helper.asm;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import static io.github.tanice.twItemManager.util.Logger.logInfo;
import static io.github.tanice.twItemManager.util.Logger.logWarning;

public class ASMHelper {

    public static byte[] modifyClass(byte[] originalBytes) {
        ClassReader reader = new ClassReader(originalBytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new DamageLogicModifier(Opcodes.ASM9, writer);
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    public static void applyModification() {
        try {
            String className = "org.bukkit.event.entity.EntityDamageEvent";
            InputStream classStream = Bukkit.class.getClassLoader().getResourceAsStream(className.replace('.', '/') + ".class");

            if (classStream != null) {
                byte[] originalBytes = readAllBytes(classStream);
                byte[] modifiedBytes = modifyClass(originalBytes);

                Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
                defineClass.setAccessible(true);
                defineClass.invoke(ASMHelper.class.getClassLoader(),
                        className,
                        modifiedBytes,
                        0,
                        modifiedBytes.length);

                logInfo("成功重定义: " + className);
            } else {
                logWarning("Class not found: " + className);
            }
        } catch (Exception e) {
            logWarning( "[applyModification] failed: " + e.getMessage());
        }
    }

    private static byte @NotNull [] readAllBytes(@NotNull InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        return buffer.toByteArray();
    }
}
