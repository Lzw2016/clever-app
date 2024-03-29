//package org.clever.core.zxing;
//
//import com.google.zxing.*;
//import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
//import com.google.zxing.client.j2se.ImageReader;
//import com.google.zxing.common.HybridBinarizer;
//import lombok.SneakyThrows;
//
//import javax.imageio.ImageIO;
//import java.awt.image.BufferedImage;
//import java.io.ByteArrayInputStream;
//import java.io.File;
//import java.io.InputStream;
//import java.net.URI;
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * Zxing 实现的读取各种二维码条形码内容的工具<br/>
// * <p/>
// * 作者：LiZW <br/>
// * 创建时间：2016-5-7 0:11 <br/>
// */
//public class ZxingReaderImageUtils {
//    /**
//     * 二维码生成工具类
//     */
//    private static final MultiFormatReader MULTI_FORMAT_READER;
//
//    private static final Map<DecodeHintType, Object> HINTS;
//
//    static {
//        HINTS = new HashMap<>();
//        // 指定编码格式
//        HINTS.put(DecodeHintType.CHARACTER_SET, "UTF-8");
//        MULTI_FORMAT_READER = new MultiFormatReader();
//    }
//
//    /**
//     * 读取条形码、二维码图片里的数据
//     *
//     * @param image 条形码、二维码图片对象
//     * @return 返回读取结果
//     */
//    @SneakyThrows
//    private static Result readerImage(BinaryBitmap image) {
//        return MULTI_FORMAT_READER.decode(image, HINTS);
//    }
//
//    /**
//     * 读取条形码、二维码图片里的数据
//     *
//     * @param bufferedImage 条形码、二维码图片对象
//     * @return 返回读取结果
//     */
//    public static String readerImage(BufferedImage bufferedImage) {
//        String contents = null;
//        LuminanceSource luminanceSource = new BufferedImageLuminanceSource(bufferedImage);
//        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(luminanceSource));
//        Result result = readerImage(binaryBitmap);
//        if (result != null) {
//            contents = result.getText();
//        }
//        return contents;
//    }
//
//    /**
//     * 读取条形码、二维码图片里的数据
//     *
//     * @param inputStream 条形码、二维码图片输入流
//     * @return 返回读取结果
//     */
//    @SneakyThrows
//    public static String readerImage(InputStream inputStream) {
//        String contents;
//        BufferedImage bufferedImage = ImageIO.read(inputStream);
//        contents = readerImage(bufferedImage);
//        return contents;
//    }
//
//    /**
//     * 读取条形码、二维码图片里的数据
//     *
//     * @param filePath 条形码、二维码图片路径
//     * @return 返回读取结果
//     */
//    @SneakyThrows
//    public static String readerImage(String filePath) {
//        String contents;
//        BufferedImage bufferedImage = ImageIO.read(new File(filePath));
//        contents = readerImage(bufferedImage);
//        return contents;
//    }
//
//    /**
//     * 读取条形码、二维码图片里的数据
//     *
//     * @param imageData 条形码、二维码图片数据
//     * @return 返回读取结果
//     */
//    public static String readerImage(byte[] imageData) {
//        String contents;
//        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);
//        contents = readerImage(inputStream);
//        return contents;
//    }
//
//    /**
//     * 读取条形码、二维码图片里的数据
//     *
//     * @param uriAddress 条形码、二维码图片URI地址
//     * @return 返回读取结果
//     */
//    @SneakyThrows
//    public static String readerImageByUri(String uriAddress) {
//        String contents;
//        URI uri = new URI(uriAddress);
//        BufferedImage bufferedImage = ImageReader.readImage(uri);
//        contents = readerImage(bufferedImage);
//        return contents;
//    }
//}
