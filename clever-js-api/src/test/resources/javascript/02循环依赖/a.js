print('a 开始');
exports.done = false;
var b = require('./b.js');
print('在 a 中，b.done=', b.done);
exports.done = true;
print('a 结束');