/*
 * Copyright (c) 2018 Exam Consortium
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

import angular from'angular';
import moment from 'moment';

angular.module('app.maturity')
    .component('reviewedInspections', {
        template: require('./reviewedInspections.template.html'),
        bindings: {
            inspections: '<',
            onStartDateChange: '&',
            onEndDateChange: '&'
        },
        controller: ['$translate', 'LanguageInspections',
            function ($translate, LanguageInspections) {

                const vm = this;

                vm.$onInit = function () {
                    vm.sorting = {
                        predicate: 'exam.created',
                        reverse: true
                    };

                    vm.getInspectionAmounts = function () {
                        const amount = vm.inspections.length.toString();
                        const year = moment().format('YYYY');
                        return $translate.instant('sitnet_processed_language_inspections_detail').replace('{0}', amount)
                            .replace('{1}', year);
                    };
                };

                vm.startDateChanged = function (date) {
                    vm.onStartDateChange({date: date});
                };

                vm.endDateChanged = function (date) {
                    vm.onEndDateChange({date: date});
                };

                vm.showStatement = function (statement) {
                    LanguageInspections.showStatement(statement);
                };


            }
        ]
    });

