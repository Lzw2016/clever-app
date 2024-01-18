print('b 开始');
exports.done = false;
var a = require('./a.js');
print('在 b 中，a.done=', a.done);
exports.done = true;
print('b 结束');