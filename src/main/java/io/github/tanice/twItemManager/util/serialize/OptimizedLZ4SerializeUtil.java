//package io.github.tanice.twItemManager.util.serialize;
//
//import net.jpountz.lz4.LZ4Compressor;
//import net.jpountz.lz4.LZ4Factory;
//import net.jpountz.lz4.LZ4FastDecompressor;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.io.*;
//import java.util.concurrent.ConcurrentLinkedQueue;
//
//import static io.github.tanice.twItemManager.util.Logger.logWarning;
//
///**
// * 高性能 LZ4 序列化压缩工具
// * 给PDC使用
// */
//@Deprecated
//public class OptimizedLZ4SerializeUtil {
//    // LZ4 工厂实例（线程安全）
//    private static final LZ4Factory lz4Factory = LZ4Factory.fastestInstance();
//
//    // 内存池配置
//    private static final int INITIAL_POOL_SIZE = 5;
//    private static final int MAX_BUFFER_SIZE = 10 * 1024 * 1024; // 10MB
//
//    // 压缩器/解压器实例（线程安全）
//    private static final LZ4Compressor compressor = lz4Factory.fastCompressor();
//    private static final LZ4FastDecompressor decompressor = lz4Factory.fastDecompressor();
//
//    // 内存池：对象输出流包装器
//    private static final ConcurrentLinkedQueue<ObjectOutputStreamWrapper> oosPool =
//            new ConcurrentLinkedQueue<>();
//
//    // 内存池：解压缓冲区
//    private static final ConcurrentLinkedQueue<byte[]> bufferPool =
//            new ConcurrentLinkedQueue<>();
//
//    static {
//        for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
//            oosPool.add(createOosWrapper());
//            bufferPool.add(new byte[1024 * 32]); // 32KB 初始缓冲区
//        }
//    }
//
//    /**
//     * 序列化对象为 LZ4 压缩的字节数组
//     */
//    public static byte @Nullable [] serialize(Object o) {
//        ObjectOutputStreamWrapper wrapper = null;
//
//        try {
//            wrapper = acquireOosWrapper();
//            final ByteArrayOutputStream bos = wrapper.bos;
//            final ObjectOutputStream oos = wrapper.oos;
//
//            bos.reset();
//            oos.writeObject(o);
//            oos.flush();
//            byte[] uncompressed = bos.toByteArray();
//            final int uncompressedLength = uncompressed.length;
//            final int maxCompressedLength = compressor.maxCompressedLength(uncompressedLength);
//
//            byte[] compressionBuffer = getBuffer(maxCompressedLength + 4);
//
//            compressionBuffer[0] = (byte) (uncompressedLength & 0xFF);
//            compressionBuffer[1] = (byte) ((uncompressedLength >>> 8) & 0xFF);
//            compressionBuffer[2] = (byte) ((uncompressedLength >>> 16) & 0xFF);
//            compressionBuffer[3] = (byte) ((uncompressedLength >>> 24) & 0xFF);
//
//            final int compressedSize = compressor.compress(
//                    uncompressed, 0, uncompressedLength,
//                    compressionBuffer, 4, maxCompressedLength
//            );
//
//            // 7. 创建精确大小的结果数组
//            byte[] result = new byte[compressedSize + 4];
//            System.arraycopy(compressionBuffer, 0, result, 0, result.length);
//            return result;
//
//        } catch (Exception e) {
//            logWarning("PDC 序列化失败: " + e);
//            return null;
//        } finally {
//            // 8. 将包装器返回到池中
//            if (wrapper != null) {
//                releaseOosWrapper(wrapper);
//            }
//        }
//    }
//
//    /**
//     * 从 LZ4 压缩的字节数组反序列化对象
//     */
//    @SuppressWarnings("unchecked")
//    public static <T> @Nullable T deserialize(byte @NotNull [] compressedData, Class<T> type) {
//        if (compressedData.length < 4) {
//            logWarning("PDC 数据长度不足: " + compressedData.length);
//            return null;
//        }
//
//        ObjectInputStream ois = null;
//        byte[] buffer = null;
//        try {
//            int uncompressedLength = (compressedData[0] & 0xFF) | ((compressedData[1] & 0xFF) << 8) | ((compressedData[2] & 0xFF) << 16) | ((compressedData[3] & 0xFF) << 24);
//            buffer = getBuffer(uncompressedLength);
//
//            final int decompressedSize = decompressor.decompress(compressedData, 4, buffer, 0, uncompressedLength);
//            if (decompressedSize != uncompressedLength) {
//                logWarning("PDC 反序列化失败: 解压大小不匹配: " + uncompressedLength + " != " + decompressedSize);
//                return null;
//            }
//
//            try (ByteArrayInputStream bis = new ByteArrayInputStream(buffer, 0, decompressedSize)) {
//                ois = new ObjectInputStream(bis);
//                Object obj = ois.readObject();
//
//                if (type.isInstance(obj)) {
//                    return (T) obj;
//                } else {
//                    logWarning("类型不匹配: 期望 " + type.getName() + ", 实际 " + obj.getClass().getName());
//                    return null;
//                }
//            }
//        } catch (Exception e) {
//            logWarning("LZ4反序列化失败: " + e);
//            return null;
//
//        } finally {
//            closeQuietly(ois);
//            releaseBuffer(buffer);
//        }
//    }
//
//    private static ObjectOutputStreamWrapper createOosWrapper() {
//        try {
//            ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);
//            ObjectOutputStream oos = new ObjectOutputStream(bos);
//            return new ObjectOutputStreamWrapper(bos, oos);
//        } catch (IOException e) {
//            throw new RuntimeException("创建ObjectOutputStream失败", e);
//        }
//    }
//
//    private static @NotNull ObjectOutputStreamWrapper acquireOosWrapper() {
//        ObjectOutputStreamWrapper wrapper = oosPool.poll();
//        return wrapper != null ? wrapper : createOosWrapper();
//    }
//
//    private static void releaseOosWrapper(@NotNull ObjectOutputStreamWrapper wrapper) {
//        if (wrapper.bos.size() < MAX_BUFFER_SIZE) {
//            oosPool.offer(wrapper);
//        }
//    }
//
//    private static byte @NotNull [] getBuffer(int minSize) {
//        // 尝试从池中获取合适大小的缓冲区
//        byte[] buffer = bufferPool.poll();
//        if (buffer == null || buffer.length < minSize) {
//            // 计算新缓冲区大小 (按2的幂次增长)
//            int newSize = calculateNewSize(minSize);
//            return new byte[newSize];
//        }
//        return buffer;
//    }
//
//    private static void releaseBuffer(byte[] buffer) {
//        if (buffer != null && buffer.length <= MAX_BUFFER_SIZE) {
//            bufferPool.offer(buffer);
//        }
//    }
//
//    private static int calculateNewSize(int minSize) {
//        // 按2的幂次增长，最小为16KB
//        int size = 16 * 1024; // 16KB
//        while (size < minSize) {
//            size *= 2;
//            // 防止过大内存分配
//            if (size > MAX_BUFFER_SIZE) {
//                return minSize; // 直接返回所需大小
//            }
//        }
//        return size;
//    }
//
//    private record ObjectOutputStreamWrapper(ByteArrayOutputStream bos, ObjectOutputStream oos) {
//    }
//
//    private static void closeQuietly(Closeable closeable) {
//        if (closeable != null) {
//            try {
//                closeable.close();
//            } catch (IOException e) {
//                logWarning("PDC 序列化内部错误: " + e.getMessage());
//            }
//        }
//    }
//}