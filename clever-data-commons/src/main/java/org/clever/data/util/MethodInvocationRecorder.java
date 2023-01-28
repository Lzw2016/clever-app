//package org.clever.data.util;
//
//import org.clever.core.CollectionFactory;
//import org.clever.core.ResolvableType;
//import org.clever.util.Assert;
//import org.clever.util.ObjectUtils;
//import org.clever.util.ReflectionUtils;
//import org.clever.util.StringUtils;
//
//import java.lang.reflect.Array;
//import java.lang.reflect.Method;
//import java.lang.reflect.Modifier;
//import java.util.*;
//import java.util.function.Function;
//
///** TODO 删除
// * 通过代理上的方法引用记录方法调用的 API
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2023/01/27 13:22 <br/>
// */
//public class MethodInvocationRecorder {
//    public static PropertyNameDetectionStrategy DEFAULT = DefaultPropertyNameDetectionStrategy.INSTANCE;
//    private Optional<RecordingMethodInterceptor> interceptor;
//
//    /**
//     * 创建一个新的 {@link MethodInvocationRecorder}。对于临时实例化，更喜欢静态 {@link #forProxyOf(Class)}
//     */
//    private MethodInvocationRecorder() {
//        this(Optional.empty());
//    }
//
//    private MethodInvocationRecorder(Optional<RecordingMethodInterceptor> interceptor) {
//        this.interceptor = interceptor;
//    }
//
//    /**
//     * 为给定类型创建一个新的 {@link Recorded}
//     *
//     * @param type 不得为 {@literal null}
//     */
//    public static <T> Recorded<T> forProxyOf(Class<T> type) {
//        Assert.notNull(type, "Type must not be null");
//        Assert.isTrue(!Modifier.isFinal(type.getModifiers()), "Type to record invocations on must not be final");
//        return new MethodInvocationRecorder().create(type);
//    }
//
//    /**
//     * 根据当前的 {@link MethodInvocationRecorder} 设置为给定类型创建一个新的 {@link Recorded}
//     */
//    @SuppressWarnings("unchecked")
//    private <T> Recorded<T> create(Class<T> type) {
//        RecordingMethodInterceptor interceptor = new RecordingMethodInterceptor();
//        ProxyFactory proxyFactory = new ProxyFactory();
//        proxyFactory.addAdvice(interceptor);
//        if (!type.isInterface()) {
//            proxyFactory.setTargetClass(type);
//            proxyFactory.setProxyTargetClass(true);
//        } else {
//            proxyFactory.addInterface(type);
//        }
//        T proxy = (T) proxyFactory.getProxy(type.getClassLoader());
//        return new Recorded<T>(proxy, new MethodInvocationRecorder(Optional.ofNullable(interceptor)));
//    }
//
//    private Optional<String> getPropertyPath(List<PropertyNameDetectionStrategy> strategies) {
//        return interceptor.flatMap(it -> it.getPropertyPath(strategies));
//    }
//
//    private class RecordingMethodInterceptor implements org.aopalliance.intercept.MethodInterceptor {
//        private InvocationInformation information = InvocationInformation.NOT_INVOKED;
//
//        @Override
//        @SuppressWarnings("null")
//        public Object invoke(MethodInvocation invocation) throws Throwable {
//            Method method = invocation.getMethod();
//            Object[] arguments = invocation.getArguments();
//            if (ReflectionUtils.isObjectMethod(method)) {
//                return method.invoke(this, arguments);
//            }
//            ResolvableType type = ResolvableType.forMethodReturnType(method);
//            Class<?> rawType = type.resolve(Object.class);
//            if (Collection.class.isAssignableFrom(rawType)) {
//                Class<?> clazz = type.getGeneric(0).resolve(Object.class);
//                InvocationInformation information = registerInvocation(method, clazz);
//                Collection<Object> collection = CollectionFactory.createCollection(rawType, 1);
//                collection.add(information.getCurrentInstance());
//                return collection;
//            }
//            if (Map.class.isAssignableFrom(rawType)) {
//                Class<?> clazz = type.getGeneric(1).resolve(Object.class);
//                InvocationInformation information = registerInvocation(method, clazz);
//                Map<Object, Object> map = CollectionFactory.createMap(rawType, 1);
//                map.put("_key_", information.getCurrentInstance());
//                return map;
//            }
//            return registerInvocation(method, rawType).getCurrentInstance();
//        }
//
//        private Optional<String> getPropertyPath(List<PropertyNameDetectionStrategy> strategies) {
//            return this.information.getPropertyPath(strategies);
//        }
//
//        private InvocationInformation registerInvocation(Method method, Class<?> proxyType) {
//            Recorded<?> create = Modifier.isFinal(proxyType.getModifiers()) ? new Unrecorded(proxyType) : create(proxyType);
//            InvocationInformation information = new InvocationInformation(create, method);
//            return this.information = information;
//        }
//    }
//
//    private static final class InvocationInformation {
//        private static final InvocationInformation NOT_INVOKED = new InvocationInformation(new Unrecorded(null), null);
//
//        private final Recorded<?> recorded;
//        private final Method invokedMethod;
//
//        public InvocationInformation(Recorded<?> recorded, Method invokedMethod) {
//            Assert.notNull(recorded, "Recorded must not be null");
//            this.recorded = recorded;
//            this.invokedMethod = invokedMethod;
//        }
//
//        Object getCurrentInstance() {
//            return recorded.currentInstance;
//        }
//
//        Optional<String> getPropertyPath(List<PropertyNameDetectionStrategy> strategies) {
//            Method invokedMethod = this.invokedMethod;
//            if (invokedMethod == null) {
//                return Optional.empty();
//            }
//            String propertyName = getPropertyName(invokedMethod, strategies);
//            Optional<String> next = recorded.getPropertyPath(strategies);
//            return Optionals.firstNonEmpty(() -> next.map(it -> propertyName.concat(".").concat(it)), () -> Optional.of(propertyName));
//        }
//
//        private static String getPropertyName(Method invokedMethod, List<PropertyNameDetectionStrategy> strategies) {
//            return strategies.stream().map(it -> it.getPropertyName(invokedMethod)).findFirst()
//                    .orElseThrow(() -> new IllegalArgumentException(String.format("No property name found for method %s", invokedMethod)));
//        }
//
//        public Recorded<?> getRecorded() {
//            return this.recorded;
//        }
//
//        public Method getInvokedMethod() {
//            return this.invokedMethod;
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            if (this == o) {
//                return true;
//            }
//            if (!(o instanceof InvocationInformation)) {
//                return false;
//            }
//            InvocationInformation that = (InvocationInformation) o;
//            if (!ObjectUtils.nullSafeEquals(recorded, that.recorded)) {
//                return false;
//            }
//            return ObjectUtils.nullSafeEquals(invokedMethod, that.invokedMethod);
//        }
//
//        @Override
//        public int hashCode() {
//            int result = ObjectUtils.nullSafeHashCode(recorded);
//            result = (31 * result) + ObjectUtils.nullSafeHashCode(invokedMethod);
//            return result;
//        }
//
//        @Override
//        public String toString() {
//            return "MethodInvocationRecorder.InvocationInformation(recorded=" + this.getRecorded() + ", invokedMethod=" + this.getInvokedMethod() + ")";
//        }
//    }
//
//    public interface PropertyNameDetectionStrategy {
//        String getPropertyName(Method method);
//    }
//
//    private enum DefaultPropertyNameDetectionStrategy implements PropertyNameDetectionStrategy {
//        INSTANCE;
//
//        @Override
//        public String getPropertyName(Method method) {
//            return getPropertyName(method.getReturnType(), method.getName());
//        }
//
//        private static String getPropertyName(Class<?> type, String methodName) {
//            String pattern = getPatternFor(type);
//            String replaced = methodName.replaceFirst(pattern, "");
//            return StringUtils.uncapitalize(replaced);
//        }
//
//        private static String getPatternFor(Class<?> type) {
//            return type.equals(boolean.class) ? "^(is)" : "^(get|set)";
//        }
//    }
//
//    public static class Recorded<T> {
//        private final T currentInstance;
//        private final MethodInvocationRecorder recorder;
//
//        Recorded(T currentInstance, MethodInvocationRecorder recorder) {
//            this.currentInstance = currentInstance;
//            this.recorder = recorder;
//        }
//
//        public Optional<String> getPropertyPath() {
//            return getPropertyPath(MethodInvocationRecorder.DEFAULT);
//        }
//
//        public Optional<String> getPropertyPath(PropertyNameDetectionStrategy strategy) {
//            MethodInvocationRecorder recorder = this.recorder;
//            return recorder == null ? Optional.empty() : recorder.getPropertyPath(Collections.singletonList(strategy));
//        }
//
//        public Optional<String> getPropertyPath(List<PropertyNameDetectionStrategy> strategies) {
//            MethodInvocationRecorder recorder = this.recorder;
//            return recorder == null ? Optional.empty() : recorder.getPropertyPath(strategies);
//        }
//
//        /**
//         * 将给定的 Converter 应用于记录的值并记住访问的属性
//         *
//         * @param converter 不得为 {@literal null}
//         */
//        public <S> Recorded<S> record(Function<? super T, S> converter) {
//            Assert.notNull(converter, "Function must not be null");
//            return new Recorded<S>(converter.apply(currentInstance), recorder);
//        }
//
//        /**
//         * 记录遍历到集合属性的方法调用
//         *
//         * @param converter 不得为 {@literal null}
//         */
//        public <S> Recorded<S> record(ToCollectionConverter<T, S> converter) {
//            Assert.notNull(converter, "Converter must not be null");
//            return new Recorded<S>(converter.apply(currentInstance).iterator().next(), recorder);
//        }
//
//        /**
//         * 记录遍历到某个地图属性的方法调用
//         *
//         * @param converter 不得为 {@literal null}
//         */
//        public <S> Recorded<S> record(ToMapConverter<T, S> converter) {
//            Assert.notNull(converter, "Converter must not be null");
//            return new Recorded<S>(converter.apply(currentInstance).values().iterator().next(), recorder);
//        }
//
//        @Override
//        public String toString() {
//            return "MethodInvocationRecorder.Recorded(currentInstance=" + this.currentInstance + ", recorder=" + this.recorder + ")";
//        }
//
//        public interface ToCollectionConverter<T, S> extends Function<T, Collection<S>> {
//        }
//
//        public interface ToMapConverter<T, S> extends Function<T, Map<?, S>> {
//        }
//    }
//
//    static class Unrecorded extends Recorded<Object> {
//        private Unrecorded(Class<?> type) {
//            super(type == null ? null : type.isPrimitive() ? getDefaultValue(type) : null, null);
//        }
//
//        @Override
//        public Optional<String> getPropertyPath(List<PropertyNameDetectionStrategy> strategies) {
//            return Optional.empty();
//        }
//
//        private static Object getDefaultValue(Class<?> clazz) {
//            return Array.get(Array.newInstance(clazz, 1), 0);
//        }
//    }
//}
