(function () {
    'use strict';
    angular.module("exam.controllers")
        .controller('RoomCtrl', ['dialogs', '$scope', '$routeParams', 'sessionService', '$location', '$modal', '$http', 'SoftwareResource', 'RoomResource', 'ExamMachineResource', 'EXAM_CONF', 'dateService', '$translate', '$route',
            function (dialogs, $scope, $routeParams, sessionService, $location, $modal, $http, SoftwareResource, RoomResource, ExamMachineResource, EXAM_CONF, dateService, $translate, $route) {

                $scope.dateService = dateService;

                $scope.machineTemplate = EXAM_CONF.TEMPLATES_PATH + "facility/machine.html";
                $scope.addressTemplate = EXAM_CONF.TEMPLATES_PATH + "facility/address.html";
                $scope.hoursTemplate = EXAM_CONF.TEMPLATES_PATH + "facility/open_hours.html";
                $scope.user = sessionService.getUser();
                $scope.examStartingHours = Array.apply(null, new Array(24)).map(function (x, i) {
                    return {startingHour: i + ":00", selected: true};
                });


                $http.get('accessibility').success(function (data) {
                    $scope.accessibilities = data;
                });

                // initialize the timeslots
                var week = {
                    'MONDAY': Array.apply(null, new Array(48)).map(function (x, i) {
                        return {'index': i, type: ''};
                    }),
                    'TUESDAY': Array.apply(null, new Array(48)).map(function (x, i) {
                        return {'index': i, type: ''};
                    }),
                    'WEDNESDAY': Array.apply(null, new Array(48)).map(function (x, i) {
                        return {'index': i, type: ''};
                    }),
                    'THURSDAY': Array.apply(null, new Array(48)).map(function (x, i) {
                        return {'index': i, type: ''};
                    }),
                    'FRIDAY': Array.apply(null, new Array(48)).map(function (x, i) {
                        return {'index': i, type: ''};
                    }),
                    'SATURDAY': Array.apply(null, new Array(48)).map(function (x, i) {
                        return {'index': i, type: ''};
                    }),
                    'SUNDAY': Array.apply(null, new Array(48)).map(function (x, i) {
                        return {'index': i, type: ''};
                    })
                };

                var times = [""]; // This is a dummy value for setting something for the table header

                for (var i = 0; i <= 24; ++i) {
                    if (i > 0) {
                        times.push(i + ":00");
                    }
                    if (i < 24) {
                        times.push(i + ":30");
                    }
                }

                var isAnyExamMachines = function () {
                    return $scope.roomInstance.examMachines && $scope.roomInstance.examMachines.length > 0;
                };

                var isEmpty = function (day) {
                    for (var i = 0; i < week[day].length; ++i) {
                        if (week[day][i].type !== '') {
                            return false;
                        }
                    }
                    return true;
                };

                var isSomethingSelected = function () {
                    for (var day in week) {
                        if (week.hasOwnProperty(day)) {
                            if (!isEmpty(day)) {
                                return true;
                            }
                        }
                    }
                    return false;
                };

                var firstSelection = function (day) {
                    for (var i = 0; i < week[day].length; ++i) {
                        if (week[day][i].type) {
                            return i;
                        }
                    }
                };

                var lastSelection = function (day) {
                    for (var i = week[day].length - 1; i >= 0; --i) {
                        if (week[day][i].type) {
                            return i;
                        }
                    }
                };

                var blocksForDay = function (day) {
                    var blocks = [];
                    var tmp = [];
                    for (var i = 0; i < week[day].length; ++i) {
                        if (week[day][i].type) {
                            tmp.push(i);
                            if (i === week[day].length - 1) {
                                blocks.push(tmp);
                                tmp = [];
                            }
                        } else if (tmp.length > 0) {
                            blocks.push(tmp);
                            tmp = [];
                        }
                    }
                    return blocks;
                };

                $scope.select = function (day, time) {
                    var status = week[day][time].type;
                    if (status === 'accepted') { // clear selection
                        week[day][time].type = '';
                        return;
                    }
                    if (status === 'selected') { // mark everything hereafter as free until next block
                        for (var i = 0; i < week[day].length; ++i) {
                            if (i >= time) {
                                if (week[day][i].type === 'selected') {
                                    week[day][i].type = '';
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                    else {
                        // check if something is accepted yet
                        var accepted;
                        for (i = 0; i < week[day].length; ++i) {
                            if (week[day][i].type === 'accepted') {
                                accepted = i;
                                break;
                            }
                        }
                        if (accepted >= 0) { // mark everything between accepted and this as selected
                            if (accepted < time) {
                                for (i = accepted; i <= time; ++i) {
                                    week[day][i].type = 'selected';
                                }
                            } else {
                                for (i = time; i <= accepted; ++i) {
                                    week[day][i].type = 'selected';
                                }
                            }
                        } else {
                            week[day][time].type = 'accepted'; // mark beginning
                            return;
                        }
                    }
                    $scope.updateWorkingHours($scope.roomInstance, week);
                };

                $scope.calculateTime = function (index) {
                    return (times[index] || "0:00") + " - " + times[index + 1];
                };

                var setSelected = function (day, slots) {
                    for (var i = 0; i < slots.length; ++i) {
                        if (week[day][slots[i]]) {
                            week[day][slots[i]].type = 'selected';
                        }
                    }
                };

                var slotToTimes = function (slot) {
                    var arr = [];
                    var startKey = moment(slot.startTime).format("H:mm");
                    var endKey = moment(slot.endTime).format("H:mm");
                    var start = startKey === '0:00' ? 0 : times.indexOf(startKey);
                    for (var i = start; i < times.length; i++) {
                        if (times[i] === endKey) {
                            break;
                        }
                        arr.push(i);
                    }
                    return arr;
                };

                var formatExceptionEvent = function(event) {
                    var startDate = moment(event.startDate);
                    var endDate = moment(event.endDate);
                    var offset = moment().isDST() ? -1 : 0;
                    startDate.add(offset, 'hour');
                    endDate.add(offset, 'hour');
                    event.startDate = startDate.format();
                    event.endDate = endDate.format();
                };

                if ($scope.user.isAdmin) {
                    if (!$routeParams.id) {
                        RoomResource.rooms.query(function (rooms) {
                            $scope.rooms = rooms;
                            angular.forEach($scope.rooms, function (room) {
                                room.examMachines = room.examMachines.filter(function (machine) {
                                    return !machine.archived;
                                });
                            });
                        });
                    } else {
                        RoomResource.rooms.get({id: $routeParams.id},
                            function (room) {
                                $scope.times = times;
                                room.transitionTime = parseInt(room.transitionTime || 0); // TODO: tt should be int in db
                                room.defaultWorkingHours.forEach(function (daySlot) {
                                    var timeSlots = slotToTimes(daySlot);
                                    setSelected(daySlot.day, timeSlots);
                                });
                                $scope.roomInstance = room;
                                if (!isAnyExamMachines()) {
                                    toastr.warning($translate.instant('sitnet_room_has_no_machines_yet'));
                                }
                                if ($scope.roomInstance.examStartingHours.length > 0) {
                                    var startingHours = $scope.roomInstance.examStartingHours.map(function (hours) {
                                        return moment(hours.startingHour);
                                    });
                                    $scope.roomInstance.examStartingHourOffset = startingHours[0].minute();
                                    startingHours = startingHours.map(function(hours) {
                                       return hours.format("H:mm");
                                    });
                                    $scope.setStartingHourOffset();
                                    $scope.examStartingHours.forEach(function (hours) {
                                        hours.selected = startingHours.indexOf(hours.startingHour) !== -1;
                                    });
                                }
                                $scope.roomInstance.calendarExceptionEvents.forEach(function (event) {
                                    formatExceptionEvent(event);
                                });
                            },
                            function (error) {
                                toastr.error(error.data);
                            }
                        );
                    }
                }
                else {
                    $location.path("/");
                }

                $scope.timerange = function () {
                    return Array.apply(null, new Array(times.length - 1)).map(function (x, i) {
                        return i;
                    });
                };

                $scope.getWeekdays = function () {
                    return Object.keys(week);
                };

                $scope.getType = function (day, time) {
                    return week[day][time].type;
                };

                $scope.countMachineAlerts = function (room) {
                    if (!room) return 0;
                    return room.examMachines.filter(function (m) {
                        return m.outOfService;
                    }).length;
                };

                $scope.countMachineNotices = function (room) {
                    if (!room) return 0;
                    return room.examMachines.filter(function (m) {
                        return !m.outOfService && m.statusComment;
                    }).length;
                };

                // Called when create exam button is clicked
                $scope.createExamRoom = function () {
                    RoomResource.draft.get(
                        function (room) {
                            toastr.info($translate.instant("sitnet_room_draft_created"));
                            $location.path("/rooms/" + room.id);
                        }, function (error) {
                            toastr.error(error.data);
                        }
                    );
                };

                $scope.manageSoftwares = function () {
                    $location.path("/softwares");
                };

                $scope.modifyMachine = function (machine) {
                    $location.path("/machines/" + machine.id);
                };

                $scope.updateRoom = function (room) {
                    RoomResource.rooms.update(room,
                        function () {
                            toastr.info($translate.instant('sitnet_room_updated'));
                        },
                        function (error) {
                            toastr.error(error.data);
                        }
                    );
                };

                $scope.toggleAllExamStartingHours = function () {
                    var anySelected = $scope.examStartingHours.some(function (hours) {
                        return hours.selected;
                    });
                    $scope.examStartingHours.forEach(function (hours) {
                        hours.selected = !anySelected;
                    });
                };

                function zeropad(n) {
                    n += '';
                    return n.length > 1 ? n : '0' + n;
                }

                $scope.setStartingHourOffset = function () {
                    $scope.roomInstance.examStartingHourOffset = $scope.roomInstance.examStartingHourOffset || 0;
                    $scope.examStartingHours.forEach(function (hours) {
                        hours.startingHour = hours.startingHour.split(':')[0] + ':' + zeropad($scope.roomInstance.examStartingHourOffset);
                    });
                };

                $scope.anyStartingHoursSelected = function () {
                    return $scope.examStartingHours.some(function (hours) {
                        return hours.selected;
                    });
                };

                $scope.updateStartingHours = function () {
                    var selected = $scope.examStartingHours.filter(function (hours) {
                        return hours.selected;
                    }).map(function (hours) {
                        return formatTime(hours.startingHour);
                    });
                    var data = {hours: selected, offset: $scope.roomInstance.examStartingHourOffset}
                    RoomResource.examStartingHours.update({id: $scope.roomInstance.id}, data,
                        function () {
                            toastr.info($translate.instant('sitnet_exam_starting_hours_updated'));
                        },
                        function (error) {
                            toastr.error(error.data);
                        }
                    );
                };

                $scope.saveRoom = function (room) {

                    if (!isSomethingSelected()) {
                        toastr.error($translate.instant('sitnet_room_must_have_default_opening_hours'));
                        return;
                    }

                    if (!isAnyExamMachines())
                        toastr.error($translate.instant("sitnet_dont_forget_to_add_machines") + " " + $scope.roomInstance.name);

                    RoomResource.rooms.update(room,
                        function (updated_room) {
                            toastr.info($translate.instant("sitnet_room_saved"));
                            $location.path("/rooms/");
                        },
                        function (error) {
                            toastr.error(error.data);
                        }
                    );
                };

                $scope.updateAddress = function (address) {
//                    RoomResource.addresses.update({id: address}, address,
                    RoomResource.addresses.update(address,
                        function (updated_address) {
                            toastr.info($translate.instant("sitnet_room_address_updated"));
                        },
                        function (error) {
                            toastr.error(error.data);
                        }
                    );
                };

                $scope.updateMachine = function (machine) {
                    ExamMachineResource.update({id: machine.id}, machine,
                        function (updated_machine) {
                            machine = updated_machine;
                            toastr.info('<i class="fa fa-save"></i>');
                        },
                        function (error) {
                            toastr.error(error.data);
                        }
                    );
                };

                $scope.selectedSoftwares = function (machine) {
                    return machine.softwareInfo.map(function (software) {
                        return software.name;
                    }).join(", ");
                };


                $scope.selectedAccessibilities = function () {
                    return $scope.roomInstance.accessibility.map(function (software) {
                        return software.name;
                    }).join(", ");
                };

                $scope.updateAccessibility = function (room) {
                    var ids = room.accessibility.map(function (item) {
                        return item.id;
                    }).join(", ");

                    $http.post('room/' + room.id + '/accessibility', {ids: ids})
                        .success(function () {
                            toastr.info($translate.instant("sitnet_room_updated"));
                        });
                };

                $scope.softwares = SoftwareResource.softwares.query();

                $scope.addNewMachine = function (room) {
                    var newMachine = {
                        "name": $translate.instant("sitnet_write_computer_name")
                    };

                    ExamMachineResource.insert({id: room.id}, newMachine, function (machine) {
                        toastr.info($translate.instant("sitnet_machine_added"));
                        room.examMachines.push(machine);
                    }, function (error) {
                        toastr.error(error.data);
                    });
                };

                $scope.updateMachineSoftware = function (machine) {
                    $scope.updateMachine(machine);
                    $scope.selectedSoftwares(machine);
                };

                $scope.updateSoftware = function (software) {
                    SoftwareResource.update.update({id: software.id}, software,
                        function (updated_software) {
                            software = updated_software;
                            toastr.info($translate.instant('sitnet_software_updated'));
                        },
                        function (error) {
                            toastr.error(error.data);
                        }
                    );
                };

                $scope.addSoftware = function (name) {
                    SoftwareResource.add.insert({name: name}, function (software) {
                            toastr.info($translate.instant('sitnet_software_added'));
                            $scope.softwares.push(software);
                        },
                        function (error) {
                            toastr.error(error.data);
                        }
                    );
                };

                $scope.removeSoftware = function (software) {
                    SoftwareResource.software.remove({id: software.id},
                        function () {
                            toastr.info($translate.instant('sitnet_software_removed'));
                            if ($scope.softwares.indexOf(software) > -1) {
                                $scope.softwares.splice($scope.softwares.indexOf(software), 1);
                            }
                        },
                        function (error) {
                            toastr.error(error.data);
                        }
                    );
                };

                $scope.disableRoom = function (room) {
                    var dialog = dialogs.confirm($translate.instant('sitnet_confirm'), $translate.instant('sitnet_confirm_room_inactivation'));
                    dialog.result.then(function (btn) {
                        RoomResource.rooms.inactivate({id: room.id},
                            function (data) {
                                //room = data;
                                toastr.info($translate.instant('sitnet_room_inactivated'));
                                $route.reload();
                            },
                            function (error) {
                                toastr.error(error.data);
                            }
                        );
                    });
                };

                $scope.enableRoom = function (room) {
                    RoomResource.rooms.activate({id: room.id},
                        function (data) {
                            //room = data;
                            toastr.info($translate.instant('sitnet_room_activated'));
                            $route.reload();
                        },
                        function (error) {
                            toastr.error(error.data);
                        }
                    );

                };

                var formatTime = function (time) {
                    var hours = moment().isDST() ? 1 : 0;
                    return moment()
                        .set('hour', parseInt(time.split(':')[0]) + hours)
                        .set('minute', time.split(':')[1])
                        .format("DD.MM.YYYY HH:mmZZ");
                };

                $scope.updateWorkingHours = function (room, week) {
                    var workingHours = [];
                    for (var day in week) {
                        if (week.hasOwnProperty(day)) {
                            var blocks = blocksForDay(day);
                            var weekdayBlocks = {'weekday': day, 'blocks': []};
                            for (var i = 0; i < blocks.length; ++i) {
                                var block = blocks[i];
                                var start = formatTime(times[block[0]] || "0:00");
                                var end = formatTime(times[block[block.length - 1] + 1]);
                                weekdayBlocks.blocks.push({'start': start, 'end': end});
                            }
                            workingHours.push(weekdayBlocks);
                        }
                    }

                    RoomResource.workinghours.update({id: $scope.roomInstance.id}, workingHours,
                        function () {
                            toastr.info($translate.instant('sitnet_default_opening_hours_updated'));
                        },
                        function (error) {
                            toastr.error(error.data);
                        }
                    );
                };


                var remove = function (arr, item) {
                    var index = arr.indexOf(item);
                    arr.splice(index, 1);
                };


                $scope.deleteException = function (exception) {
                    RoomResource.exception.remove({id: exception.id},
                        function (saveException) {
                            toastr.info($translate.instant('sitnet_exception_time_removed'));
                            remove($scope.roomInstance.calendarExceptionEvents, exception);
                        },
                        function (error) {
                            toastr.error(error.data);
                        }
                    );
                };

                $scope.formatDate = function (exception) {
                    var fmt = 'DD.MM.YYYY HH:mm';
                    var start = moment(exception.startDate);
                    var end = moment(exception.endDate);
                    return start.format(fmt) + ' - ' + end.format(fmt);
                };

                $scope.addException = function () {


                    var modalInstance = $modal.open({
                        templateUrl: EXAM_CONF.TEMPLATES_PATH + 'facility/exception.html',
                        backdrop: 'static',
                        keyboard: true,
                        controller: function ($scope, $modalInstance) {
                            var now = new Date();
                            now.setMinutes(0);
                            now.setSeconds(0);
                            now.setMilliseconds(0);
                            $scope.exception = { startDate: now, endDate: angular.copy(now), outOfService: true };

                            $scope.ok = function () {

                                var hourOffset = moment().isDST() ? 1 : 0;
                                var start = moment($scope.exception.startDate).add(hourOffset, 'hour');
                                var end = moment($scope.exception.endDate).add(hourOffset, 'hour');
                                if (end <= start) {
                                    toastr.error($translate.instant('sitnet_endtime_before_starttime'))
                                } else {
                                    $modalInstance.close({
                                        "startDate": start,
                                        "endDate": end,
                                        "outOfService": $scope.exception.outOfService
                                    });
                                }
                            };

                            $scope.cancel = function () {
                                $modalInstance.dismiss('cancel');
                            };
                        }
                    });

                    modalInstance.result.then(function (exception) {

                        RoomResource.exception.update({id: $scope.roomInstance.id}, exception,
                            function (data) {
                                toastr.info($translate.instant('sitnet_exception_time_added'));
                                formatExceptionEvent(data);
                                $scope.roomInstance.calendarExceptionEvents.push(data);
                            },
                            function (error) {
                                toastr.error(error.data);
                            }
                        );
                    });
                };

                $scope.isArchived = function (machine) {
                    return machine.isArchived() === false;
                };

                $scope.displayAddress = function (address) {

                    if (!address || (!address.street && !address.city && !address.zip)) return "N/A";
                    var street = address.street ? address.street + ", " : "";
                    var city = (address.city || "").toUpperCase();
                    return street + address.zip + " " + city;
                };

            }]);
}());