'use strict';
angular.module('app.examination')
    .service('Examination', ['$q', '$location', '$translate', 'examService', 'StudentExamRes',
        function ($q, $location, $translate, examService, StudentExamRes) {

            var self = this;

            self.saveTextualAnswer = function (esq, hash, autosave) {
                esq.questionStatus = $translate.instant('sitnet_answer_saved');
                var deferred = $q.defer();
                var params = {
                    hash: hash,
                    qid: esq.id
                };
                var type = esq.question.type;
                var answerObj = type === 'EssayQuestion' ? esq.essayAnswer : esq.clozeTestAnswer;
                var resource = type === 'EssayQuestion' ? StudentExamRes.essayAnswer.saveEssay : StudentExamRes.clozeTestAnswer.save;
                var msg = {
                    answer: answerObj.answer,
                    objectVersion: answerObj.objectVersion
                };
                resource(params, msg,
                    function (answer) {
                        if (autosave) {
                            esq.autosaved = new Date();
                        } else {
                            toastr.info($translate.instant('sitnet_answer_saved'));
                            self.setQuestionColors(esq);
                        }
                        answerObj.objectVersion = answer.objectVersion;
                        deferred.resolve();
                    }, function (error) {
                        toastr.error(error.data);
                        deferred.reject();
                    });
                return deferred.promise;
            };

            var isTextualAnswer = function (esq) {
                switch (esq.question.type) {
                    case 'EssayQuestion':
                        return esq.essayAnswer && esq.essayAnswer.answer.length > 0;
                    case 'ClozeTestQuestion':
                        return esq.clozeTestAnswer && !_.isEmpty(esq.clozeTestAnswer.answer);
                    default:
                        return false;
                }
            };

            self.saveAllTextualAnswersOfSection = function (section, hash, autosave, canFail) {
                var deferred = $q.defer();
                var promises = [];
                section.sectionQuestions.filter(function (esq) {
                    return isTextualAnswer(esq);
                }).forEach(function (esq) {
                    promises.push(self.saveTextualAnswer(esq, hash, autosave));
                });
                if (canFail) {
                    $q.allSettled(promises).then(function () {
                        deferred.resolve();
                    });
                } else {
                    $q.all(promises).then(function () {
                        deferred.resolve();
                    });
                }
                return deferred.promise;
            };

            var stripHtml = function (text) {
                if (text && text.indexOf('math-tex') === -1) {
                    return String(text).replace(/<[^>]+>/gm, '');
                }
                return text;
            };

            self.setQuestionColors = function (sectionQuestion) {
                var isAnswered;
                switch (sectionQuestion.question.type) {
                    case 'EssayQuestion':
                        var essayAnswer = sectionQuestion.essayAnswer;
                        isAnswered = essayAnswer && essayAnswer.answer &&
                            stripHtml(essayAnswer.answer).length > 0;
                        break;
                    case 'MultipleChoiceQuestion':
                        isAnswered = angular.isDefined(sectionQuestion.selectedOption);
                        break;
                    case 'WeightedMultipleChoiceQuestion':
                        isAnswered = sectionQuestion.options.filter(function (o) {
                                return o.answered;
                            }).length > 0;
                        break;
                    case 'ClozeTestQuestion':
                        var clozeTestAnswer = sectionQuestion.clozeTestAnswer;
                        isAnswered = clozeTestAnswer && !_.isEmpty(clozeTestAnswer.answer);
                        break;
                    default:
                        break;
                }
                if (isAnswered) {
                    sectionQuestion.answered = true;
                    sectionQuestion.questionStatus = $translate.instant('sitnet_question_answered');
                    sectionQuestion.selectedAnsweredState = 'question-answered-header';
                } else {
                    sectionQuestion.answered = false;
                    sectionQuestion.questionStatus = $translate.instant('sitnet_question_unanswered');
                    sectionQuestion.selectedAnsweredState = 'question-unanswered-header';
                }
            };

            self.saveOption = function (hash, sq, preview) {
                var ids;
                if (sq.question.type === 'WeightedMultipleChoiceQuestion') {
                    ids = sq.options.filter(function (o) {
                        return o.answered;
                    }).map(function (o) {
                        return o.id;
                    });
                } else {
                    ids = [sq.selectedOption];
                }
                if (!preview) {
                    StudentExamRes.multipleChoiceAnswer.saveMultipleChoice({
                            hash: hash,
                            qid: sq.id,
                            oids: ids
                        },
                        function () {
                            toastr.info($translate.instant('sitnet_answer_saved'));
                            sq.options.forEach(function (o) {
                                o.answered = ids.indexOf(o.id) > -1;
                            });
                            self.setQuestionColors(sq);
                        }, function (error) {

                        });
                } else {
                    self.setQuestionColors(sq);
                }

            };

            self.logout = function (msg, hash) {
                StudentExamRes.exams.update({hash: hash}, function () {
                    toastr.info($translate.instant(msg), {timeOut: 5000});
                    window.onbeforeunload = null;
                    $location.path('/student/logout/finished');
                }, function (error) {
                    toastr.error($translate.instant(error.data));
                });
            };

        }]);
