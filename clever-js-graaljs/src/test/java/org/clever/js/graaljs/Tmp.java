package org.clever.js.graaljs;

import com.oracle.truffle.api.debug.Debugger;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.clever.js.graaljs.proxy.ArrayListProxy;
import org.clever.js.graaljs.utils.InteropScriptToJavaUtils;
import org.clever.js.graaljs.utils.ScriptEngineUtils;
import org.graalvm.options.OptionDescriptor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/25 14:18 <br/>
 */
@Slf4j
public class Tmp {
    @Test
    public void t01() {
        // Engine engine = Engine.create();
        // Context.getCurrent()
        Context context = Context.newBuilder(GraalConstant.Js_Language_Id).build();
        String jsCode = "(function() { var b = x; return function() { return 1 + b; };});";
        Value function = context.eval(GraalConstant.Js_Language_Id, jsCode);
        context.getBindings(GraalConstant.Js_Language_Id).putMember("x", 10);
        Value res = function.execute();
        log.info("### res -> {}", res.execute());
        context.getBindings(GraalConstant.Js_Language_Id).putMember("x", 100);
        context.close();
    }

    @Test
    public void t02() {
        Context context = Context.newBuilder(GraalConstant.Js_Language_Id).allowAllAccess(true).build();
        AtomicInteger atomicInteger = new AtomicInteger(0);
        context.getBindings(GraalConstant.Js_Language_Id).putMember("x", atomicInteger);
        String jsCode = "(function() { while (true) { x.incrementAndGet(); } });";
        Value function = context.eval(GraalConstant.Js_Language_Id, jsCode);
        new Thread(() -> {
            try {
                Thread.sleep(1_000);
                context.close(true);
                log.info("### context.close()");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        try {
            function.executeVoid();
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (int i = 0; i < 10; i++) {
            log.info("### -> {}", atomicInteger.get());
        }
        log.info("### end");
        context.close();
    }

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    @Test
    public void t03() {
        Logger logger = LoggerFactory.getLogger(StringUtils.EMPTY);
        // Object arr = new String[]{"111", "222", "333"};
        Object arr = new int[]{111, 222, 333};
        Collection<?> collection = Arrays.asList((int[]) arr);
        logger.info("### collection -> {}", collection.size());
        for (Object o : collection) {
            logger.info("### o -> {}", o);
        }
        logger.info("###111 -> {}", collection);
        logger.info("###111 -> {}", new Date().toString());
    }

    @Test
    public void t04() {
        Context context_1 = Context.newBuilder(GraalConstant.Js_Language_Id).build();
        Value value = context_1.eval(GraalConstant.Js_Language_Id, "new Date()");
        context_1.close();
        Context context_2 = Context.newBuilder(GraalConstant.Js_Language_Id).build();
        Value fuc = context_2.eval(GraalConstant.Js_Language_Id, "(function(obj) { return JSON.stringify(obj); });");
        try {
            Object object = fuc.execute(value);
            log.info("# -> {}", object);
        } catch (Exception e) {
            log.info("# 不能把一个Context中的对象传给另一个Context-> ", e);
        }
        value = context_2.eval(GraalConstant.Js_Language_Id, "new Date()");
        Object object = fuc.execute(value);
        log.info("# -> {}", object);
        context_2.close();
    }

    @Test
    public void t05() {
        Context context = Context.newBuilder(GraalConstant.Js_Language_Id).build();
        byte a = 1;
        short b = 2;
        int c = 3;
        long d = 4;
        double e = 5;
        boolean f = false;
        char g = 'a';
        String h = "abcd";
        Date i = new Date();
        BigDecimal j = new BigDecimal("1354741344987654323456765434567564564568989.564948989745189789454894894864");
        List<String> k = new ArrayList<String>() {{
            add("111");
            add("222");
            add("333");
        }};
        Set<String> l = new HashSet<String>() {{
            add("111");
            add("222");
            add("333");
        }};
        Map<String, Object> m = new HashMap<String, Object>() {{
            put("int", 1);
            put("boolean", false);
            put("str", "asdfghjkl");
        }};
        log.info("## -> {}", context.asValue(a));
        log.info("## -> {}", context.asValue(b));
        log.info("## -> {}", context.asValue(c));
        log.info("## -> {}", context.asValue(d));
        log.info("## -> {}", context.asValue(e));
        log.info("## -> {}", context.asValue(f));
        log.info("## -> {}", context.asValue(g));
        log.info("## -> {}", context.asValue(h));
        log.info("## -> {}", context.asValue(i));
        log.info("## -> {}", context.asValue(j));
        log.info("## -> {}", context.asValue(k));
        log.info("## -> {}", context.asValue(l));
        log.info("## -> {}", context.asValue(m));
        log.info("## -> {}", context.asValue(m).getMemberKeys().size()); // size=0
        context.close();
    }

    @Test
    public void t06() {
        Context context = Context.newBuilder(GraalConstant.Js_Language_Id).build();
        Value value = ScriptEngineUtils.newArray(context, "aaa", 111, false);
        log.info("## -> {}", value);

        Object object = new Object[]{"aaa", 111, false};
        value = ScriptEngineUtils.newArray(context, object);
        log.info("## -> {}", value);

        log.info("## -> {}", object);

//        Map<Integer, Boolean> map = new HashMap<>();
//        TypeVariable<? extends Class<?>>[] parameters = map.getClass().getTypeParameters();
//        log.info("## parameters -> {}", parameters);
    }

    @Test
    public void t07() {
        List<Object> list = new ArrayList<>();
        list.add("111");
        list.add("222");
        list.add("333");
        ProxyArray proxyArray = ProxyArray.fromList(list);
        log.info("## -> {}", InteropScriptToJavaUtils.unWrapProxyArray(proxyArray));

        proxyArray = ProxyArray.fromArray("444", "555", "666");
        log.info("## -> {}", InteropScriptToJavaUtils.unWrapProxyArray(proxyArray));

        proxyArray = new ArrayListProxy(list);
        log.info("## -> {}", InteropScriptToJavaUtils.unWrapProxyArray(proxyArray));

        log.info("## -> {}", proxyArray);
    }

    @Test
    public void t08() {
        Context context = Context.newBuilder(GraalConstant.Js_Language_Id).build();
        context.getBindings(GraalConstant.Js_Language_Id).putMember("a", 10);
        context.getBindings(GraalConstant.Js_Language_Id).putMember("b", 11);
        context.getBindings(GraalConstant.Js_Language_Id).putMember("c", 11 * 10);
        Value value = context.eval(GraalConstant.Js_Language_Id, "a * b >= c");
        log.info("# -> {}", value);
        context.close();
    }

    @SneakyThrows
    @Test
    public void t09() {
//        Instrument.getOptions();
//        Language.getOptions()
        Engine engine = Engine.newBuilder().build();
        for (OptionDescriptor option : engine.getOptions()) {
            log.info("{} -> {}", option.getName(), option.getHelp());
        }
        engine.getInstruments().forEach((s, instrument) -> {
            for (OptionDescriptor option : instrument.getOptions()) {
                log.info("[{}] {} -> {}", s, option.getName(), option.getHelp());
            }
        });
        engine.close();
        String js = "function add(a,b){console.log(\"@@@\");return a+b}add(3,5);add(2,5);add(1,5);add(34,5);add(355,5);add(36,5);function tmp(){return{test:function test(obj){console.log(\"###\",obj)}}}tmp();\n" +
            "//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJuYW1lcyI6WyJhZGQiLCJhIiwiYiIsImNvbnNvbGUiLCJsb2ciLCJ0bXAiLCJ0ZXN0Iiwib2JqIl0sInNvdXJjZXMiOlsiYXBwLnRzIl0sInNvdXJjZXNDb250ZW50IjpbIlxuZnVuY3Rpb24gYWRkKGE6IG51bWJlciwgYjogbnVtYmVyKTogbnVtYmVyIHtcbiAgY29uc29sZS5sb2coXCJAQEBcIilcbiAgcmV0dXJuIGEgKyBiO1xufVxuXG5hZGQoMywgNSk7XG5hZGQoMiwgNSk7XG5hZGQoMSwgNSk7XG5hZGQoMzQsIDUpO1xuYWRkKDM1NSwgNSk7XG5hZGQoMzYsIDUpO1xuXG5mdW5jdGlvbiB0bXAoKSB7XG4gIHJldHVybiB7XG4gICAgdGVzdDogZnVuY3Rpb24gKG9iajogYW55KSB7XG4gICAgICBjb25zb2xlLmxvZyhcIiMjI1wiLCBvYmopXG4gICAgfVxuICB9XG59XG5cbnRtcCgpXG4gICJdLCJtYXBwaW5ncyI6IkFBQ0EsUUFBUyxDQUFBQSxHQUFHQSxDQUFDQyxDQUFTLENBQUVDLENBQVMsQ0FBVSxDQUN6Q0MsT0FBTyxDQUFDQyxHQUFHLENBQUMsS0FBSyxDQUFDLENBQ2xCLE1BQU8sQ0FBQUgsQ0FBQyxDQUFHQyxDQUNiLENBRUFGLEdBQUcsQ0FBQyxDQUFDLENBQUUsQ0FBQyxDQUFDLENBQ1RBLEdBQUcsQ0FBQyxDQUFDLENBQUUsQ0FBQyxDQUFDLENBQ1RBLEdBQUcsQ0FBQyxDQUFDLENBQUUsQ0FBQyxDQUFDLENBQ1RBLEdBQUcsQ0FBQyxFQUFFLENBQUUsQ0FBQyxDQUFDLENBQ1ZBLEdBQUcsQ0FBQyxHQUFHLENBQUUsQ0FBQyxDQUFDLENBQ1hBLEdBQUcsQ0FBQyxFQUFFLENBQUUsQ0FBQyxDQUFDLENBRVYsUUFBUyxDQUFBSyxHQUFHQSxDQUFBLENBQUcsQ0FDYixNQUFPLENBQ0xDLElBQUksQ0FBRSxTQUFBQSxLQUFVQyxHQUFRLENBQUUsQ0FDeEJKLE9BQU8sQ0FBQ0MsR0FBRyxDQUFDLEtBQUssQ0FBRUcsR0FBRyxDQUN4QixDQUNGLENBQ0YsQ0FFQUYsR0FBRyxFQUFFIn0=";


        Source source = Source.newBuilder("js", "utf-8", "tmp.js").content(js).build();
        // 创建一个新的上下文对象
        String port = "4242";
        String path = "test";
        Context context = Context.newBuilder("js")
            .allowAllAccess(true)
            .option("js.ecmascript-version", "staging")
            .option("inspect", port)
            .option("inspect.Path", path)
            .option("inspect.Suspend", "true")
//                .option("inspect.WaitAttached", "true")
//                .option("inspect.SourcePath", "")
            .build();
        String hostAdress = "127.0.0.1";
        String url = String.format("devtools://devtools/bundled/js_app.html?ws=%s:%s/%s", hostAdress, port, path);
        // devtools://devtools/bundled/js_app.html
        // chrome-devtools://devtools/bundled/js_app.html
        // chrome-devtools://devtools/bundled/inspector.html
        log.info("### -> {}", url);
        Debugger debugger = context.getEngine().getInstruments().get("inspect").lookup(Debugger.class);
        TestBean bean = new TestBean();
        bean.setA(12);
        bean.setB("abc");
        bean.setC(true);
        bean.setD(5.3D);
        bean.setE(DateUtils.parseDate("2023-06-11 00:00:01", "yyyy-MM-dd HH:mm:ss"));
        context.getBindings("js").putMember("bean", bean);
        Map<String, Object> global = new HashMap<>();
        global.put("a1", 456);
        global.put("a2", "qwe");
        global.put("a3", bean);
        context.getBindings("js").putMember("global", global);
        Value result = context.parse(source);
        result = result.execute();
        result.getMember("test").execute(bean);
        log.info("result -> {}", result);
//        // 将源代码加载到引擎中
//        Source source = Source.newBuilder("js", "console.log('Hello, World!');").build();
//        // 对于每个值，我们都可以启用调试
//        Value result = context.eval(source);
//        result.getMetaObject().getMetaSimpleName(); // 获取类型名称
//
//        // 再次运行代码以查看调试输出
//        result = context.eval(source);
        context.close();
    }
}
