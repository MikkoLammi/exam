(function () {
    'use strict';
    angular.module("sitnet.controllers")
        .controller('ExamFeedbackController', ['$scope', '$modal', 'sessionService', '$sce', '$routeParams', '$translate', '$http', '$location', 'SITNET_CONF', 'StudentExamRes', 'QuestionRes', 'UserRes', 'dateService',
            function ($scope, $modal, sessionService, $sce, $routeParams, $translate, $http, $location, SITNET_CONF, StudentExamRes, QuestionRes, UserRes, dateService) {

                $scope.dateService = dateService;
                $scope.session = sessionService;

                $scope.generalInfoPath = SITNET_CONF.TEMPLATES_PATH + "student/exam_feedback_general_info.html";

                $scope.user = $scope.session.user;
                if ($scope.user.isStudent) {
                    $scope.feedbackTemplate = SITNET_CONF.TEMPLATES_PATH + "student/exam_feedback.html";
                }
                else if ($scope.user.isTeacher) {
                    $scope.feedbackTemplate = SITNET_CONF.TEMPLATES_PATH + "student/exam_feedback.html";
                }
                else if ($scope.user.isAdmin) {
                    $scope.feedbackTemplate = SITNET_CONF.TEMPLATES_PATH + "student/exam_feedback.html";
                }

                if ($routeParams.id === undefined) {
                    // Todo: We should not come here ever, redirect to homepage if we still arrive
                }
                // Get the exam that was specified in the URL
                else {
                    StudentExamRes.feedback.get({eid: $routeParams.id},
                        function (exam) {
                            $scope.reviewedExam = exam;
                        },
                        function (error) {
                            toastr.error(error.data);
                        }
                    );
                }

                //Go to feedback template to show teacher's comments
                $scope.exitFeedback = function () {
                    $location.path("/home");
                }

            }]);
}());