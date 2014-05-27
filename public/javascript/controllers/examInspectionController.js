(function () {
    'use strict';
    angular.module("sitnet.controllers")
        .controller('ExamInspectionController', ['$scope', '$modalInstance', 'exam', 'sessionService', '$sce', '$routeParams', '$translate', '$http', '$location', 'SITNET_CONF','ExamRes', 'UserRes', 'dateService', 'limitToFilter',
            function ($scope, $modalInstance, exam, sessionService, $sce, $routeParams, $translate, $http, $location, SITNET_CONF, ExamRes, UserRes, dateService, limitToFilter) {

                $scope.dateService = dateService;
                $scope.session = sessionService;
                $scope.user = $scope.session.user;
                $scope.exam = exam;

                $scope.newInspection = {
                    "user": {
                        "id": null,
                        "name": null
                    },
                    "exam": {
                        "id": $scope.exam.id
                    },
                    "comment": {
                        "comment": ""
                    }
                }

                $scope.examInspectors = function (filter, criteria) {
                    return UserRes.filterUsersByExam.query({role: 'TEACHER', eid: $scope.newInspection.exam.id, q: criteria}).$promise.then(
                        function (names) {
                            return limitToFilter(names, 15);
                        },
                        function (error) {
                            toastr.error(error.data);
                        }
                    );
                };

                $scope.setExamInspector = function ($item, $model, $label) {
                    $scope.newInspection.user.id = $item.id;
                    $scope.newInspection.user.name = $item.name;
                };

                // Cancel button is pressed in the modal dialog
                $scope.cancel = function () {
                    $modalInstance.dismiss('Canceled');
                };

                // Ok button is pressed in the modal dialog
                $scope.ok = function (data) {
                    $modalInstance.close(data);
                };

                $scope.saveInspector = function () {
                    if($scope.newInspection.user.id && $scope.newInspection.user.id > 0 && $scope.newInspection.exam.id && $scope.newInspection.exam.id > 0) {
                        ExamRes.inspection.insert({eid: $scope.newInspection.exam.id, uid: $scope.newInspection.user.id}, $scope.newInspection, function (inspection) {
                            toastr.info("Tentti tallennettu.");
                            $scope.ok(inspection);
                        }, function (error) {
                            toastr.error(error.data);
                            $scope.cancel();
                        });
                    } else {
                        toastr.error("Opettajaa ei löytynyt");
                        $scope.cancel();
                    }
                }

            }]);
}());