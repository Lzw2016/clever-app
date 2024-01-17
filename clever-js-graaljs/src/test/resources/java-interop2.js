var JavaInterop = Java.type("org.clever.hinny.test.graaljs.JavaInterop").Instance;

var map = JavaInterop.getProxyMap2();

console.log("# -> getProxyMap2: | ", map);
console.log("# -> getProxyMap2: | ", map.string);
console.log("# -> getProxyMap2: | ", Interop.asJMap(map));

var list = JavaInterop.getProxyMap3();
console.log("# -> list: | ", list);
JavaInterop.setValue(list);
JavaInterop.setList2(list);

list = Interop.asJList(map, map, map, map);
console.log("# -> list: | ", list);
JavaInterop.setValue(list);


