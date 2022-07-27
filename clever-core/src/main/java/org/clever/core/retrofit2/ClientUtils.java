//package org.clever.core.retrofit2;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang3.StringUtils;
//import retrofit2.Call;
//import retrofit2.Response;
//
//import java.io.IOException;
//
///**
// * 作者：lizw <br/>
// * 创建时间：2017/7/27 17:33 <br/>
// */
//public class ClientUtils {
//
//    /**
//     * 根据响应 Response 获取异常消息，没有异常返回null
//     *
//     * @param response 响应对象
//     * @return 返回异常消息，没有异常返回null
//     */
//    @SuppressWarnings("ConstantConditions")
//    public static ErrorMessage getErrorMessage(Response response) {
//        if (response == null) {
//            return null;
//        }
//        if (response.isSuccessful()) {
//            return null;
//        }
//        String json;
//        try {
//            json = response.errorBody().string();
//        } catch (IOException e) {
//            throw new RuntimeException(e.getMessage(), e);
//        }
//        return JacksonMapper.nonEmptyMapper().fromJson(json, ErrorMessage.class);
//    }
//
//    /**
//     * 根据响应 Response 判断请求是否成功，若请求失败抛出异常
//     *
//     * @param response 响应对象
//     */
//    public static void hasErrorThrowException(Response response) {
//        ErrorMessage errorMessage = getErrorMessage(response);
//        if (errorMessage == null) {
//            return;
//        }
//        log.error("服务调用失败 {}", errorMessage);
//        if (StringUtils.isBlank(errorMessage.getException())) {
//            if (StringUtils.isNotBlank(errorMessage.getMessage())) {
//                throw new BusinessException(ErrorCode.BUSINESS_ERROR, errorMessage);
//            }
//            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, errorMessage);
//        }
//        if (errorMessage.getException().contains("MethodArgumentNotValidException")
//                || errorMessage.getException().contains("BindException")
//                || errorMessage.getException().contains("ValidationException")) {
//            errorMessage.setError(errorMessage.getMessage());
//            throw new BusinessException(ErrorCode.INVALID_PARAMETER, errorMessage);
//        } else if (errorMessage.getException().contains("HttpMessageConversionException")
//                || errorMessage.getException().contains("HttpMessageNotReadableException")) {
//            throw new BusinessException(ErrorCode.PARSE_JSON_ERROR, errorMessage);
//        } else if (errorMessage.getException().contains("BusinessException")) {
//            errorMessage.setError(errorMessage.getMessage());
//            throw new BusinessException(ErrorCode.BUSINESS_ERROR, errorMessage);
//        } else {
//            throw new RuntimeException(errorMessage.getMessage());
//        }
//    }
//
//    /**
//     * 获取接口响应
//     *
//     * @param call 执行的接口
//     * @param <T>  返回报文
//     */
//    @SuppressWarnings("unchecked")
//    public static <T> T getResponse(Call call) {
//        try {
//            Response<T> response = call.execute();
//            ClientUtils.hasErrorThrowException(response);
//            return response.body();
//        } catch (IOException e) {
//            log.error("服务调用失败", e);
//            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, e);
//        }
//    }
//}
