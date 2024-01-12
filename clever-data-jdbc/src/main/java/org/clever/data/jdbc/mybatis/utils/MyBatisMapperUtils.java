package org.clever.data.jdbc.mybatis.utils;

import org.apache.commons.lang3.StringUtils;
import org.clever.core.model.request.QueryByPage;
import org.clever.core.model.request.QueryBySort;
import org.clever.data.jdbc.mybatis.CreateObject;
import org.clever.data.jdbc.mybatis.MapperMethodInfo;
import org.clever.data.jdbc.mybatis.annotations.Param;
import org.clever.data.jdbc.support.BatchData;
import org.clever.data.jdbc.support.DbColumnMetaData;
import org.clever.data.jdbc.support.RowData;
import org.clever.util.Assert;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/12 17:09 <br/>
 */
public class MyBatisMapperUtils {
    /**
     * 判断 class 能否通过无参构造函数创建(clazz.newInstance())
     */
    private static boolean isNewInstance(Class<?> clazz) {
        // 接口 抽象类
        if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
            return false;
        }
        try {
            // 获取无参数的造函数
            Constructor<?> ctor = clazz.getDeclaredConstructor();
            // 如果不可访问，尝试设置为可访问
            if (!ctor.isAccessible()) {
                ctor.setAccessible(true);
            }
            ctor.newInstance();
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public static void fillMethodReturn(final Method method, final MapperMethodInfo.MapperMethodInfoBuilder builder) {
        String mapperClass = method.getDeclaringClass().getName();
        String methodName = method.getName();
        final String errMsgSuffix = "Method=(class=" + mapperClass + ", method=" + methodName + ")";
        // Method 返回值类型
        final Class<?> returnType = method.getReturnType();
        boolean returnVoid;
        boolean returnList = false;
        CreateObject<List<?>> newList = null;
        boolean returnSet = false;
        CreateObject<Set<?>> newSet = null;
        boolean returnArray = false;
        Class<?> returnItemType = null;
        boolean returnItemMap = false;
        CreateObject<Map<?, ?>> newItemMap = null;
        boolean returnMap = false;
        CreateObject<Map<?, ?>> newMap = null;
        boolean returnSimple = false;
        boolean queryMetaData = false;
        returnVoid = Void.TYPE.equals(returnType);
        if (!returnVoid) {
            if (Collection.class.isAssignableFrom(returnType)) {
                // 返回类型是集合(List、Set)
                returnSet = Set.class.isAssignableFrom(returnType);
                returnList = List.class.isAssignableFrom(returnType);
                Type type = method.getGenericReturnType();
                if (type instanceof ParameterizedType) {
                    Type[] types = ((ParameterizedType) type).getActualTypeArguments();
                    if (types != null && types.length == 1 && types[0] instanceof Class) {
                        returnItemType = (Class<?>) types[0];
                    }
                }
                if (returnList && !returnType.isAssignableFrom(ArrayList.class)) {
                    Assert.isTrue(isNewInstance(returnType), "返回值类型没有无参构造函数: " + returnType.getName() + ", " + errMsgSuffix);
                    newList = () -> (List<?>) returnType.newInstance();
                }
                if (returnSet && !returnType.isAssignableFrom(HashSet.class)) {
                    Assert.isTrue(isNewInstance(returnType), "返回值没有无参构造函数: " + returnType.getName() + ", " + errMsgSuffix);
                    newSet = () -> (Set<?>) returnType.newInstance();
                }
            } else if (returnType.isArray()) {
                // 返回类型是数组(Array)
                returnArray = true;
                returnItemType = returnType.getComponentType();
            } else if (Map.class.isAssignableFrom(returnType)) {
                // 返回类型是Map
                returnMap = true;
                if (!Objects.equals(Map.class, returnType)) {
                    Assert.isTrue(isNewInstance(returnType), "返回值没有无参构造函数: " + returnType.getName() + ", " + errMsgSuffix);
                    newMap = () -> (Map<?, ?>) returnType.newInstance();
                }
            } else {
                // 返回一个简单类型(基本类型或者实体类)
                returnSimple = true;
            }
            if (returnItemType != null) {
                // returnItemType 是否是 Map
                if (Map.class.isAssignableFrom(returnItemType)) {
                    returnItemMap = true;
                    if (!Objects.equals(Map.class, returnType)) {
                        Assert.isTrue(isNewInstance(returnType), "返回值没有无参构造函数: " + returnType.getName() + ", " + errMsgSuffix);
                        newItemMap = () -> (Map<?, ?>) returnType.newInstance();
                    }
                }
                // 是否是 queryMetaData
                if (DbColumnMetaData.class.isAssignableFrom(returnItemType)) {
                    queryMetaData = true;
                }
            }
            // 是否是 InsertResult???
        }
        // 设置返回值
        builder.returnVoid(returnVoid);
        builder.returnList(returnList);
        builder.newList(newList);
        builder.returnSet(returnSet);
        builder.newSet(newSet);
        builder.returnArray(returnArray);
        builder.returnItemType(returnItemType);
        builder.returnItemMap(returnItemMap);
        builder.newItemMap(newItemMap);
        builder.returnMap(returnMap);
        builder.newMap(newMap);
        builder.returnSimple(returnSimple);
        builder.queryMetaData(queryMetaData);
    }

    public static void fillMethodParams(final Method method, final MapperMethodInfo.MapperMethodInfoBuilder builder) {
        String mapperClass = method.getDeclaringClass().getName();
        String methodName = method.getName();
        final String errMsgSuffix = "Method=(class=" + mapperClass + ", method=" + methodName + ")";
        final Parameter[] parameters = method.getParameters();
        final Map<Integer, String> params = new HashMap<>();
        Integer sortParamIdx = null;
        Integer pageParamIdx = null;
        Integer cursorParamIdx = null;
        boolean cursorParamConsumer = false;
        boolean cursorUseBatch = false;
        for (int idx = 0; idx < parameters.length; idx++) {
            final Parameter parameter = parameters[idx];
            final Class<?> parameterClass = parameter.getType();
            ParameterizedType parameterType = null;
            if (parameter.getParameterizedType() instanceof ParameterizedType) {
                parameterType = (ParameterizedType) parameter.getParameterizedType();
            }
            final Type[] parameterGenerics = (parameterType == null) ? new Type[0] : parameterType.getActualTypeArguments();
            if (QueryByPage.class.isAssignableFrom(parameterClass)) {
                // 是否是 QueryByPage
                Assert.isNull(sortParamIdx, "QueryByPage 参数只能有一个, " + errMsgSuffix);
                pageParamIdx = idx;
            } else if (QueryBySort.class.isAssignableFrom(parameterClass)) {
                // 是否是 QueryBySort
                Assert.isNull(sortParamIdx, "QueryBySort 参数只能有一个, " + errMsgSuffix);
                sortParamIdx = idx;
            } else if ((Function.class.isAssignableFrom(parameterClass) && parameterGenerics.length == 2 && Objects.equals(parameterGenerics[1], Boolean.class))
                || (Consumer.class.isAssignableFrom(parameterClass) && parameterGenerics.length == 1)) {
                // Function/Consumer
                cursorParamConsumer = Consumer.class.isAssignableFrom(parameterClass);
                // 是否是 Function<BatchData/RowData, Boolean> | Consumer<BatchData/RowData>
                boolean isBatch = Objects.equals(parameterGenerics[0], BatchData.class);
                boolean isRow = Objects.equals(parameterGenerics[0], RowData.class);
                if (isBatch || isRow) {
                    Assert.isNull(cursorParamIdx, "Function/Consumer读取数据的回调参数只能有一个, " + errMsgSuffix);
                    cursorParamIdx = idx;
                    cursorUseBatch = isBatch;
                }
            }
            // 设置SQL参数
            final Param param = parameter.getAnnotation(Param.class);
            final String paramName = param != null && StringUtils.isNotBlank(param.value()) ? param.value() : parameter.getName();
            params.put(idx, StringUtils.trim(paramName));
        }
        // 设置返回值
        builder.params(params);
        builder.sortParamIdx(sortParamIdx);
        builder.pageParamIdx(pageParamIdx);
        builder.cursorParamIdx(cursorParamIdx);
        builder.cursorParamConsumer(cursorParamConsumer);
        builder.cursorUseBatch(cursorUseBatch);
    }

    /**
     * 获取 Method 签名字符串(严格的签名), 如: {@code java.lang.String myMethod(java.lang.String, int)}
     */
    public static String signature(Method method) {
        String methodName = method.getName();
        Class<?> returnType = method.getReturnType();
        String returnTypeStr = returnType.getTypeName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        StringBuilder paramsStr = new StringBuilder();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                paramsStr.append(", ");
            }
            paramsStr.append(parameterTypes[i].getTypeName());
        }
        return String.format("%s %s %s(%s)", method.getDeclaringClass().getName(), returnTypeStr, methodName, paramsStr);
    }

    /**
     * 判断两个 Method 是否是同一个 ClassLoader 加载的
     */
    public static boolean sameClassLoader(Method m1, Method m2) {
        return Objects.equals(m1.getDeclaringClass().getClassLoader(), m2.getDeclaringClass().getClassLoader());
    }

//    /**
//     * 判断
//     * @param returnType
//     * @return
//     */
//    public static boolean isSimpleType(Class<?> returnType) {
//        returnType.isPrimitive();
//    }
}
