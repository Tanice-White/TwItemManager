package io.github.tanice.twItemManager.util.serialize;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;

import static io.github.tanice.twItemManager.util.Logger.logWarning;

public class OriSerializationUtil {
    public static byte @NotNull [] serialize(@NotNull Object obj){
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj); // 写入对象
            return baos.toByteArray(); // 获取字节数组
        } catch (IOException e) {
            logWarning("PDC serialize failed: " + e);
        }
        return new byte[0];
    }

    public static @Nullable Object deserialize(byte[] bytes){
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            logWarning("PDC deserialize failed: " + e);
        }
        return null;
    }
}
