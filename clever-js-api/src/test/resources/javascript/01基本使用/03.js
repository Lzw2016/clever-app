// exports, require, module, __filename, __dirname
print("require -> ", require);
print("module -> ", JSON.stringify(module));
print("__filename -> ", __filename);
print("__dirname -> ", __dirname);

print("exports -> ", JSON.stringify(exports));
print("module.exports -> ", JSON.stringify(module.exports));

exports.a = "aaa";
module.exports.b = "bbb";

print("exports -> ", JSON.stringify(exports));
print("module.exports -> ", JSON.stringify(module.exports));