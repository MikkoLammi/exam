/*
 * Copyright (c) 2017 Exam Consortium
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed
 * on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */

import angular from 'angular';
import toast from 'toastr';

angular.module('app.review')
    .component('rEssayQuestion', {
        template: require('./essayQuestion.template.html'),
        bindings: {
            sectionQuestion: '<',
            onScore: '&'
        },
        require: {
            parentCtrl: '^^rExamSection'
        },
        controller: ['$sce', '$routeParams', '$translate', 'Assessment', 'Attachment',
            function ($sce, $routeParams, $translate, Assessment, Attachment) {

                const vm = this;

                vm.$onInit = function () {
                    vm.participation = vm.parentCtrl.participation;
                    vm.exam = vm.parentCtrl.exam;
                    vm.isScorable = vm.parentCtrl.isScorable;
                };

                vm.displayQuestionText = function () {
                    return $sce.trustAsHtml(vm.sectionQuestion.question.question);
                };

                vm.downloadQuestionAttachment = function () {
                    if (vm.parentCtrl.collaborative) {
                        const attachment = vm.sectionQuestion.question.attachment;
                        return Attachment.downloadCollaborativeAttachment(attachment.externalId, attachment.fileName);
                    }
                    return Attachment.downloadQuestionAttachment(vm.sectionQuestion.question);
                };

                vm.downloadQuestionAnswerAttachment = function () {
                    if (vm.parentCtrl.collaborative) {
                        const attachment = vm.sectionQuestion.essayAnswer.attachment;
                        return Attachment.downloadCollaborativeAttachment(attachment.externalId, attachment.fileName);
                    }
                    return Attachment.downloadQuestionAnswerAttachment(vm.sectionQuestion);
                };

                vm.insertEssayScore = function () {
                    Assessment.saveEssayScore(vm.sectionQuestion, $routeParams.id, $routeParams.ref, vm.participation._rev)
                        .then(function (resp) {
                            toast.info($translate.instant('sitnet_graded'));
                            vm.onScore({ revision: resp.data ? resp.data.rev : undefined });
                        }).catch(function (error) {
                            toast.error($translate.instant(error.data));
                        });
                };

                vm.getWordCount = function () {
                    if (!vm.sectionQuestion.essayAnswer) {
                        return 0;
                    }
                    return Assessment.countWords(vm.sectionQuestion.essayAnswer.answer);
                };

                vm.getCharacterCount = function () {
                    if (!vm.sectionQuestion.essayAnswer) {
                        return 0;
                    }
                    return Assessment.countCharacters(vm.sectionQuestion.essayAnswer.answer);
                };

            }
        ]
    });
