//package org.clever.js.javet;
//
//import com.eclipsesource.v8.*;
//import com.eclipsesource.v8.utils.MemoryManager;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.Test;
//
///**
// * 作者：lizw <br/>
// * 创建时间：2020/07/22 19:45 <br/>
// */
//@Slf4j
//public class J2V8Test01 {
//
//    @Test
//    public void t01() {
//        V8 v8 = V8.createV8Runtime();
//        log.info("--> {}", V8.getV8Version());
//        v8.release();
//    }
//
//    @Test
//    public void t02() {
//        V8 v8 = V8.createV8Runtime();
//        MemoryManager memoryManager = new MemoryManager(v8);
//
//        V8Object v8Object = v8.executeObjectScript("JSON");
//        V8Array parameters = new V8Array(v8);
//        parameters.push("{\"str\": \"abc\", \"int\": 123}");
//        V8Object res1 = v8Object.executeObjectFunction("parse", parameters);
//
//        parameters = new V8Array(v8);
//        V8Object res2 = v8.executeObjectFunction("Object", parameters);
//
//        memoryManager.release();
//        v8.release();
//    }
//
//
//    @Test
//    public void t03() {
//        V8 v8 = V8.createV8Runtime();
//        MemoryManager memoryManager = new MemoryManager(v8);
//
//        // 注册 console
//        ConsoleJ consoleJ = new ConsoleJ();
//        V8Object console = new V8Object(v8);
//        console.registerJavaMethod(consoleJ, "log", "log", new Class<?>[]{String.class, Object.class});
//        v8.add("console", console);
//
//        // 注册 require
//        V8Function require = new V8Function(v8, consoleJ);
//
//
//        V8Object value = v8.executeObjectScript("(function(exports, require, module, __filename, __dirname) {\n" +
//                "console.log('exports -> ', exports); \n" +
//                "console.log('require -> ', require('/001/002/003')); \n" +
//                "console.log('module -> ', module); \n" +
//                "console.log('__filename -> ', __filename); \n" +
//                "console.log('__dirname -> ', __dirname); \n" +
//                "return 123; \n" +
//                "\n" +
//                "\n});");
//
//        if (value instanceof V8Function) {
//            V8Function function = (V8Function) value;
//            V8Array parameters = new V8Array(v8);
//            // "111", require, "222", "333", "444"
//
//            parameters.push("111");
//            parameters.push(require);
//            parameters.push("222");
//            parameters.push("333");
//            parameters.push("444");
//            Object res = function.call(function, parameters);
//            log.info("res --> {}", res);
//        }
//        memoryManager.release();
//        v8.release();
//    }
//
//    public static class ConsoleJ implements JavaCallback {
//        public void log(String a1, Object a2) {
//            log.info(a1 + a2);
//        }
//
//        // require
//        @Override
//        public Object invoke(V8Object receiver, V8Array parameters) {
//            log.info("######### require -> id ={}", parameters.get(0));
//            return 123456;
//        }
//    }
//}
