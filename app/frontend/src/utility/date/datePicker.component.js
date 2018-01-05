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

angular.module('app.utility')
    .component('datePicker', {
        template: require('./datePicker.template.html'),
        bindings: {
            onUpdate: '&',
            initialDate: '<?',
            extra: '<?',
            onExtraAction: '&?',
            extraText: '@?',
            modelOptions: '<?',
            optional: '<?'
        },
        controller: [
            function () {

                const vm = this;

                vm.$onInit = function () {
                    if (angular.isUndefined(vm.modelOptions)) {
                        vm.modelOptions = {};
                    }
                    vm.date = angular.isUndefined(vm.initialDate) ? new Date() : vm.initialDate;
                    vm.showWeeks = true;
                    vm.dateOptions = {
                        startingDay: 1
                    };
                    vm.format = 'dd.MM.yyyy';
                };

                vm.openPicker = function ($event) {
                    $event.preventDefault();
                    $event.stopPropagation();
                    vm.opened = true;
                };

                vm.dateChanged = function () {
                    vm.onUpdate({date: vm.date});
                };

                vm.extraClicked = function () {
                    vm.onExtraAction({date: vm.date});
                };


            }]
    });


