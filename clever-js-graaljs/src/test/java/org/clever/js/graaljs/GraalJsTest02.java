package org.clever.js.graaljs;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/21 10:06 <br/>
 */
@Slf4j
public class GraalJsTest02 {

    @Test
    public void t01() {
        Context context = Context.newBuilder(GraalConstant.Js_Language_Id)
            .option("js.ecmascript-version", "11")
            .build();

        log.info("### --> {}", context.getEngine().getVersion());
        log.info("### --> {}", context.getEngine().getImplementationName());

        log.info("-----------------------------------------------------------------------------------------------------------------------------------");

        context.getEngine().getOptions().forEach(option -> {
            log.info("###1 {} | {} | {}", option.getStability(), option.getName(), option.getHelp());
        });
        context.getEngine().getLanguages().entrySet().forEach(entry -> {
            entry.getValue().getOptions().forEach(option -> {
                log.info("###2 {} | {} | {} | {}", option.getStability(), option.getName(), option.getHelp(), option.getCategory());
            });
        });
        context.getEngine().getInstruments().entrySet().forEach(entry -> {
            entry.getValue().getOptions().forEach(option -> {
                log.info("###3 {} | {} | {}", option.getStability(), option.getName(), option.getHelp());
            });
        });
        context.close();
    }

    @Test
    public void t02() {
        Context context = Context.newBuilder(GraalConstant.Js_Language_Id)
            .option("js.ecmascript-version", "11")
            .build();
        Value value = context.eval(GraalConstant.Js_Language_Id, "JSON");
        Value res1 = value.invokeMember("parse", "{\"str\": \"abc\", \"int\": 123}");

        value = context.eval(GraalConstant.Js_Language_Id, "Object");
        Value res2 = value.newInstance();

        value = context.eval(GraalConstant.Js_Language_Id, "function fuc(a, b) { return a+b; } fuc;");
        Value res3 = value.execute(3, 6);

        value = context.eval(GraalConstant.Js_Language_Id, "function fuc(a) { return a+6; } fuc;");
        Value res4 = value.execute(res3);

        context.close();
    }

    @Test
    public void t03() {
        Require<Object> require = id -> {
            log.info("######### require -> id ={}", id);
            return 123456;
        };

        Context context = Context.newBuilder(GraalConstant.Js_Language_Id)
            .option("js.ecmascript-version", "11")
            .allowAllAccess(true)
            .build();
        Value value = context.eval(GraalConstant.Js_Language_Id, "(function(exports, require, module, __filename, __dirname) {\n" +
            "console.log('exports -> ', exports); \n" +
            "console.log('require -> ', require('/001/002/003')); \n" +
            "console.log('module -> ', module); \n" +
            "console.log('__filename -> ', __filename); \n" +
            "console.log('__dirname -> ', __dirname); \n" +
            "return 123; \n" +
            "\n" +
            "\n});");
        Value res1 = value.execute("111", require, "222", "333", "444");

        context.close();
    }

    @FunctionalInterface
    public interface Require<T> {
        T require(final String id) throws Exception;
    }
}
