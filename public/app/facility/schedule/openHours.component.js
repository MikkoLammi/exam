'use strict';
angular.module('app.facility.schedule')
    .component('openHours', {
        templateUrl: '/assets/app/facility/schedule/openHours.template.html',
        bindings: {
            week: '<',
            onSelect: '&'
        },
        controller: ['Room', 'DateTime', 'toast', '$translate', '$scope',
            function (Room, DateTime, toast, $translate, $scope) {

            var vm = this;

            vm.$onInit = function () {
                vm.weekdayNames = DateTime.getWeekdayNames();
                vm.times = Room.getTimes();
            };

            vm.timeRange = function () {
                return Array.apply(null, new Array(vm.times.length - 1)).map(function (x, i) {
                    return i;
                });
            };

            vm.getWeekdays = function () {
                return Object.keys(vm.week);
            };

            vm.getType = function (day, time) {
                return vm.week[day][time].type;
            };

            vm.calculateTime = function (index) {
                return (vm.times[index] || "0:00") + " - " + vm.times[index + 1];
            };

            vm.selectSlot = function (day, time) {
                var i = 0, status = vm.week[day][time].type;
                if (status === 'accepted') { // clear selection
                    vm.week[day][time].type = '';
                    return;
                }
                if (status === 'selected') { // mark everything hereafter as free until next block
                    for (i = 0; i < vm.week[day].length; ++i) {
                        if (i >= time) {
                            if (vm.week[day][i].type === 'selected') {
                                vm.week[day][i].type = '';
                            } else {
                                break;
                            }
                        }
                    }
                }
                else {
                    // check if something is accepted yet
                    var accepted;
                    for (i = 0; i < vm.week[day].length; ++i) {
                        if (vm.week[day][i].type === 'accepted') {
                            accepted = i;
                            break;
                        }
                    }
                    if (accepted >= 0) { // mark everything between accepted and this as selected
                        if (accepted < time) {
                            for (i = accepted; i <= time; ++i) {
                                vm.week[day][i].type = 'selected';
                            }
                        } else {
                            for (i = time; i <= accepted; ++i) {
                                vm.week[day][i].type = 'selected';
                            }
                        }
                    } else {
                        vm.week[day][time].type = 'accepted'; // mark beginning
                    }
                }

                vm.onSelect();
            };

            $scope.$on('$localeChangeSuccess', function () {
                vm.weekdayNames = DateTime.getWeekdayNames();
            });
        }]
    });