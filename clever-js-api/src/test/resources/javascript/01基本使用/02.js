var js01 = require("./01");

var fuc = function (a, b) {
    var sum = a + b + js01.fuc(a, b);
    print(a + "+" + b + "+fuc(a, b) = " + sum);
    return sum;
}

exports.a1 = "a111111";
exports.a2 = "a222222";
exports.fuc = fuc;
