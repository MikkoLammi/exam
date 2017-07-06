'use strict';

angular.module('app.exam.editor')
    .component('courseSelection', {
        templateUrl: '/assets/app/exam/editor/creation/courseSelection.template.html',
        controller: ['$translate', '$q', '$location', '$routeParams', 'ExamRes', 'examService',
            function ($translate, $q, $location, $routeParams, ExamRes, examService) {

                var vm = this;

                vm.$onInit = function () {
                    ExamRes.exams.get({id: $routeParams.id}, function (exam) {
                        vm.exam = exam;
                    });
                };

                vm.getExecutionTypeTranslation = function () {
                    return !vm.exam || examService.getExecutionTypeTranslation(vm.exam.executionType.type);
                };

                vm.updateExamName = function () {
                    examService.updateExam(vm.exam).then(function () {
                        toastr.info($translate.instant("sitnet_exam_saved"));
                    }, function (error) {
                        if (error.data) {
                            var msg = error.data.message || error.data;
                            toastr.error($translate.instant(msg));
                        }
                    });
                };

                vm.cancelNewExam = function () {
                    ExamRes.exams.remove({id: vm.exam.id}, function () {
                        toastr.success($translate.instant('sitnet_exam_removed'));
                        $location.path('/');
                    });
                };

                vm.continueToExam = function () {
                    $location.path("/exams/examTabs/" + vm.exam.id + "/1");
                };

            }
        ]
    });