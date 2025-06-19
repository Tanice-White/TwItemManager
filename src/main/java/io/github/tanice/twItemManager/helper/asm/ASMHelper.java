package io.github.tanice.twItemManager.helper.asm;

import org.jetbrains.annotations.NotNull;

import org.objectweb.asm.*;
import org.bukkit.Bukkit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ASMHelper {

    public static void applyModification() {
        try {
            String className = "org.bukkit.event.entity.EntityDamageEvent";
            ClassLoader bukkitClassLoader = Bukkit.class.getClassLoader();
            InputStream classStream = bukkitClassLoader.getResourceAsStream(className.replace('.', '/') + ".class");
            if (classStream == null) {
                Bukkit.getLogger().warning("Class not found: " + className);
                return;
            }
            byte[] originalBytes = readAllBytes(classStream);
            classStream.close();
            byte[] modifiedBytes = modifyClass(originalBytes);
            redefineClass(className, modifiedBytes, bukkitClassLoader);

            Bukkit.getLogger().info("[ASM] 成功修改 EntityDamageEvent");
        } catch (Exception e) {
            Bukkit.getLogger().warning("[ASM] 修改失败: 如果是服务器reload导致的, 请忽略");
        }
    }

    private static byte[] modifyClass(byte[] originalBytes) {
        ClassReader reader = new ClassReader(originalBytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if ("getFinalDamage".equals(name) && "()D".equals(descriptor)) {
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override
                        public void visitCode() {
                            super.visitCode();
                            visitVarInsn(Opcodes.ALOAD, 0);
                            visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                                    "org/bukkit/event/entity/EntityDamageEvent",
                                    "getDamage",
                                    "()D",
                                    false);
                            visitInsn(Opcodes.DRETURN);
                            visitMaxs(0, 0);
                        }
                    };
                }
                return mv;
            }
        };
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private static void redefineClass(String className, byte[] modifiedBytes, ClassLoader loader) throws Exception {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Object unsafe = theUnsafe.get(null);

            Method defineClass = unsafeClass.getMethod("defineClass", String.class, byte[].class, int.class, int.class, ClassLoader.class);
            defineClass.invoke(unsafe, className, modifiedBytes, 0, modifiedBytes.length, loader);
        } catch (Exception e) {
            Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
            defineClass.setAccessible(true);
            defineClass.invoke(loader, className, modifiedBytes, 0, modifiedBytes.length);
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
