print('main 开始');
var a = require('./a.js');
var b = require('./b.js');
print('在 main 中，a.done=', a.done, '，b.done=', b.done);

exports.test = function () {
};