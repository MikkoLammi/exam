(function () {
    'use strict';
    angular.module('app.facility')
        .factory("ExamMachineResource", ['$resource', function ($resource) {
            return $resource(
                "/app/machines/:id",
                {
                    id: "@id"
                },
                {
                    "get": {method: "GET"},
                    "update": {method: "PUT"},
                    "insert": {method: "POST"},
                    "remove": {method: "DELETE"}
                }
            );
        }]);
}());
