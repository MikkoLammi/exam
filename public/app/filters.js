(function () {
    'use strict';
    angular.module('sitnet.filters')
        .filter('newlines', function (text) {
            return text.replace(/\n/g, '');
        })
        .filter('utc', function () {
            return function (val) {
                var date = new Date(val);
                return new Date(date.getUTCFullYear(),
                    date.getUTCMonth(),
                    date.getUTCDate(),
                    date.getUTCHours(),
                    date.getUTCMinutes(),
                    date.getUTCSeconds());
            };

        })
        .filter('truncate', function () {
            return function (text, after) {
                if (text && text.indexOf("math-tex") === -1) {
                    return text.substring(0, after) + "...";
                }
                return text;
            };
        })
        .filter('striphtml', function () {
            return function (html) {
                // test if mathjax formula
                if (html && html.indexOf("math-tex") === -1) {
                    var div = document.createElement("div");
                    div.innerHTML = html;
                    return div.textContent || div.innerText || "";
                }
                if (html === undefined) {
                    return "";
                }
                return html;
            };
        })
        .filter('charcount', function () {
            return function (text) {
                return text.replace(/(^"|"$|\\n)/g, "").replace(/\s+/g, ' ').trim().length;
            };
        })
        .filter('wordcount', function () {
            return function (text) {
                var words = text.replace(/(\S\.)(?!\s)/g, "$1 ").replace(/(^"|"$|\\n)/g, '').match(/\S+/g);
                return words ? words.length : 0;
            };
        })
        .filter('diffInMinutesTo', function () {
            var magicNumber = (1000 * 60);

            return function (fromDate, toDate) {
                if (toDate && fromDate) {
                    var diff = (new Date(toDate).getTime() - new Date(fromDate).getTime()) / magicNumber;
                    return Math.floor(diff);
                }
            };
        })
        .filter('diffInDaysToNow', function () {
            var magicNumber = (1000 * 60 * 60 * 24);

            return function (fromDate) {
                if (fromDate) {
                    var diff = (new Date(fromDate).getTime() - new Date().getTime()) / magicNumber;
                    if (diff < 0) {
                        return '<span class="sitnet-text-alarm">' + Math.floor(diff) + '</span>';
                    }
                    return '<span>' + Math.floor(diff) + '</span>';
                }
            };
        })
        .filter('offset', function () {
            return function(input, start) {
                if (!input) return [];
                start = parseInt(start);
                return input.slice(start);
            }
        });
}());