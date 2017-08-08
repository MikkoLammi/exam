'use strict';

angular.module('app.examination')
    .component('examinationQuestion', {
        templateUrl: '/assets/app/examination/question/examinationQuestion.template.html',
        bindings: {
            examHash: '<',
            sq: '<',
            isPreview: '<'
        },
        controller: ['$sce', '$filter', 'Examination', 'Attachment',
            function ($sce, $filter, Examination, Attachment) {

                var vm = this;

                vm.$onInit = function () {
                    vm.sq.expanded = true;
                    var answerData = vm.sq.clozeTestAnswer;
                    if (answerData && typeof answerData.answer === 'string') {
                        answerData.answer = JSON.parse(answerData.answer);
                    }
                };

                vm.displayQuestionText = function (truncated) {
                    var text = truncated ? truncate(vm.sq.question.question, 240) : vm.sq.question.question;
                    return $sce.trustAsHtml(text);
                };

                vm.downloadQuestionAttachment = function () {
                    Attachment.downloadQuestionAttachment(vm.sq.question);
                };

                vm.isAnswered = function () {
                    return Examination.isAnswered(vm.sq);
                };

                var truncate = function (content, offset) {
                    return $filter('truncate')(content, offset);
                };


            }
        ]
    });
