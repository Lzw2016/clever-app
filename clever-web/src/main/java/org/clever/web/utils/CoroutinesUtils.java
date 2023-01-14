//package org.clever.web.utils;
//
//import kotlin.Unit;
//import kotlin.jvm.JvmClassMappingKt;
//import kotlin.reflect.KClassifier;
//import kotlin.reflect.KFunction;
//import kotlin.reflect.full.KCallables;
//import kotlin.reflect.jvm.KCallablesJvm;
//import kotlin.reflect.jvm.ReflectJvmMapping;
//import kotlinx.coroutines.*;
//import kotlinx.coroutines.flow.Flow;
//import kotlinx.coroutines.reactor.MonoKt;
//import kotlinx.coroutines.reactor.ReactorFlowKt;
//import org.reactivestreams.Publisher;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//import java.util.Objects;
//
///**
// * 与 Kotlin 协程一起工作的实用程序
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2023/01/14 12:27 <br/>
// */
//public abstract class CoroutinesUtils {
//    /**
//     * 将 {@link Deferred} 实例转换为 {@link Mono}
//     */
//    public static <T> Mono<T> deferredToMono(Deferred<T> source) {
//        return MonoKt.mono(Dispatchers.getUnconfined(), (scope, continuation) -> source.await(continuation));
//    }
//
//    /**
//     * 将 {@link Mono} 实例转换为 {@link Deferred}
//     */
//    public static <T> Deferred<T> monoToDeferred(Mono<T> source) {
//        return BuildersKt.async(
//                GlobalScope.INSTANCE, Dispatchers.getUnconfined(),
//                CoroutineStart.DEFAULT,
//                (scope, continuation) -> MonoKt.awaitSingleOrNull(source, continuation)
//        );
//    }
//
//    /**
//     * 调用挂起函数并将其转换为 {@link Mono} 或 {@link Flux}
//     */
//    public static Publisher<?> invokeSuspendingFunction(Method method, Object target, Object... args) {
//        KFunction<?> function = Objects.requireNonNull(ReflectJvmMapping.getKotlinFunction(method));
//        if (method.isAccessible() && !KCallablesJvm.isAccessible(function)) {
//            KCallablesJvm.setAccessible(function, true);
//        }
//        KClassifier classifier = function.getReturnType().getClassifier();
//        Mono<Object> mono = MonoKt.mono(
//                        Dispatchers.getUnconfined(),
//                        (scope, continuation) -> KCallables.callSuspend(function, getSuspendedFunctionArgs(target, args), continuation)
//                ).filter(result -> !Objects.equals(result, Unit.INSTANCE))
//                .onErrorMap(InvocationTargetException.class, InvocationTargetException::getTargetException);
//        if (classifier != null && classifier.equals(JvmClassMappingKt.getKotlinClass(Flow.class))) {
//            return mono.flatMapMany(CoroutinesUtils::asFlux);
//        }
//        return mono;
//    }
//
//    private static Object[] getSuspendedFunctionArgs(Object target, Object... args) {
//        Object[] functionArgs = new Object[args.length];
//        functionArgs[0] = target;
//        System.arraycopy(args, 0, functionArgs, 1, args.length - 1);
//        return functionArgs;
//    }
//
//    private static Flux<?> asFlux(Object flow) {
//        return ReactorFlowKt.asFlux(((Flow<?>) flow));
//    }
//}
