var test = function (a, b) {
    var sum = 0;
    for (var i = 0; i < a; i++) {
        sum += i;
        for (var j = 0; j < b; j++) {
            sum += j;
        }
    }
    return sum;
};
