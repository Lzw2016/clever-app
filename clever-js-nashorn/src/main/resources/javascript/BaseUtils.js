// noinspection JSUnresolvedVariable
// var InternalUtilsClass = Java.type("org.clever.js.nashorn.utils.InternalUtils");
// var InternalUtils = InternalUtilsClass.Instance;

// -------------------------------------------------------------------------------------------------------

function getDate(d) {
    var day = d.getDate() < 10 ? '0' + d.getDate() : d.getDate(),
        month = d.getMonth() < 9 ? '0' + (d.getMonth() + 1) : (d.getMonth() + 1),
        year = d.getFullYear(),
        hour = d.getHours() < 10 ? '0' + d.getHours() : d.getHours(),
        minute = d.getMinutes() < 10 ? '0' + d.getMinutes() : d.getMinutes(),
        second = d.getSeconds() < 10 ? '0' + d.getSeconds() : d.getSeconds(),
        millisecond = d.getMilliseconds() < 10 ? '0' + d.getMilliseconds() : d.getMilliseconds();
    if (millisecond < 100) {
        millisecond = '0' + millisecond;
    }
    return {
        time: (+d),
        year: year,
        month: month,
        day: day,
        hour: hour,
        minute: minute,
        second: second,
        millisecond: millisecond
    };
}

function isNumber(value) {
    return Object.prototype.toString.call(value) === '[object Number]';
}

function isString(value) {
    return Object.prototype.toString.call(value) === '[object String]';
}

function isArray(value) {
    return Object.prototype.toString.call(value) === '[object Array]';
}

function isBoolean(value) {
    return Object.prototype.toString.call(value) === '[object Boolean]';
}

function isDate(value) {
    return Object.prototype.toString.call(value) === '[object Date]';
}

function isUndefined(value) {
    return value === undefined;
}

function isNull(value) {
    return value === null;
}

function isSymbol(value) {
    return Object.prototype.toString.call(value) === '[object Symbol]';
}

function isObject(value) {
    return (
        Object.prototype.toString.call(value) === '[object Object]'
        ||
        // if it isn't a primitive value, then it is a common object
        (
            !isDate(value) &&
            !isNumber(value) &&
            !isString(value) &&
            !isBoolean(value) &&
            !isArray(value) &&
            !isNull(value) &&
            !isFunction(value) &&
            !isUndefined(value) &&
            !isSymbol(value)
        )
    );
}

function isFunction(value) {
    return Object.prototype.toString.call(value) === '[object Function]';
}

function indexOf(array, item) {
    var index = -1;
    if (!array || array.length <= 0) {
        return index;
    }
    for (var i = 0; i < array.length; i++) {
        var value = array[i];
        // noinspection JSUnresolvedFunction
        if (InternalUtils.same(item, value)) {
            index = i;
            break;
        }
    }
    return index;
}

function dateToJSON(date) {
    var tmp = getDate(date);
    // 2019-08-26 12:15:54
    return tmp.year + "-" + tmp.month + "-" + tmp.day + "T" + tmp.hour + ":" + tmp.minute + ":" + tmp.second + "." + tmp.millisecond + "Z";
}

// 这个修改不起作用
Date.prototype.toJSON = function () {
    return dateToJSON(this);
};

// 解决了循环依赖问题
function inspect(object) {
    if (isUndefined(object)) {
        return 'undefined';
    }
    if (isNull(object)) {
        return 'null';
    }
    if (isString(object)) {
        return '"' + object.toString() + '"';
    }
    if (isNumber(object) || isBoolean(object)) {
        return object.toString();
    }
    if (isDate(object)) {
        return dateToJSON(object);
    }
    var cache = [];
    var keyCache = [];
    return JSON.stringify(object, function (key, value) {
        if (isObject(value)) {
            var index = indexOf(cache, value);
            if (index !== -1) {
                return "[Circular " + keyCache[index] + "]";
            }
            cache.push(value);
            keyCache.push(key || "root");
        }
        if (isFunction(value)) {
            return value.toString();
        }
        if (isUndefined(value)) {
            return "undefined";
        }
        if (isString(value)) {
            // noinspection JSUnresolvedFunction
            return InternalUtils.formatDate(value);
        }
        return value;
    });
}

function stringify(object) {
    return JSON.stringify(object);
}

// 导出的工具方法
exports.inspect = inspect;
exports.stringify = stringify;
