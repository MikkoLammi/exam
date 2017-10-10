'use strict';
angular.module('app.enrolment')
    .component('examParticipations', {
        templateUrl: '/assets/app/enrolment/finished/examParticipations.template.html',
        controller: ['$scope', '$translate', 'StudentExamRes', 'Exam', 'EXAM_CONF',
            function ($scope, $translate, StudentExamRes, Exam, EXAM_CONF) {

                var vm = this;

                vm.$onInit = function () {
                    vm.filter = {ordering: '-ended', text: null};
                    vm.pageSize = 10;
                    vm.search();
                };

                vm.search = function () {
                    StudentExamRes.finishedExams.query({filter: vm.filter.text},
                        function (data) {
                            vm.participations = data;
                        },
                        function (error) {
                            toastr.error(error.data);
                        });
                };

            }
        ]
    });



