// var _trim = require('lodash/trim');
var lodash = require('lodash');
var underscore = require('underscore');
// var Sugar = require('sugar');    // 不可用
// var Lazy = require('lazy.js');   // 不可用
// var R = require('ramda');        // 不可用
var Immutable = require('immutable');
// var numeral = require('numeral');
// var bigJs = require('big.js');   // 不可用
// var cryptoJs = require('crypto-js');
var dayjs = require('dayjs');
// var moment = require('moment');

var trim = function (str) {
    // print("require -> ", require.toString());
    // print("_trim -> ", _trim('  a123  '));
    print("lodash -> ", lodash.trim('  a123  '));
    print("underscore -> ", underscore.keys({one: 1, two: 2, three: 3}));
    // print("Sugar -> ", Sugar.Array.unique([1, 2, 2, 3]));
    // print("Lazy -> ", Lazy([6, 18, 2, 49, 34]).min());
    // print("ramda -> ", R.add(7)(10));
    print("Immutable -> ", Immutable.Map({a: 1, b: 2, c: 3}));
    // print("moment  -> ", moment("2020-07-19").toDate());
    print("dayjs   -> ", dayjs('2018-05-05').locale('zh-cn').format())
    return str.trim(str);
}

// exports._trim = _trim;
exports.trim = trim;