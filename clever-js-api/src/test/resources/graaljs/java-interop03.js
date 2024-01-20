var JavaInterop = Java.type("org.clever.js.graaljs.JavaInterop").Instance;

var map = JavaInterop.getMap2();

console.log("# -> map: | ", map);
console.log("# -> map.nest_01: | ", map.nest_01);
console.log("# -> map.nest_01.string: | ", map.nest_01.string);
console.log("# -> map?.nest_01?.string: | ", map?.nest_01?.string);

for (const key in map) {
    console.log("# -> key: | ", key);
}

for (const item of map) {
    console.log("# -> item: | ", item);
    console.log("# -> item: | ", item, " | k=", item.getKey(), " | value=", item.getValue());
}

console.log("----------------------------------------------------------------------------------------")

var bean = JavaInterop.getBean();

console.log("# -> bean: | ", bean);
console.log("# -> bean: | ", bean.getA());
console.log("# -> bean: | ", bean.a);

var bean2 = JavaInterop.getBeanProxy();

console.log("# -> bean2: | ", bean2);
// console.log("# -> bean2: | ", bean2.setA()); // 不支持调用函数
console.log("# -> bean2: | ", bean2.a);
bean2.a = 456
console.log("# -> bean2: | ", bean2.a);

console.log("----------------------------------------------------------------------------------------")

var bean3 = JavaInterop.setBean({
    a: 999,
    b: "QAZ XSW",
    c: false,
    d: 1.2,
    e: new Date(),
    // e: "2024-01-20 18:30:00",
});
console.log("# -> bean3: | ", bean3);
