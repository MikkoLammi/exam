'use strict';
angular.module('app.reservation')
    .controller('ExternalCalendarCtrl', ['$scope', '$http', '$location', '$translate', '$routeParams', 'dateService',
        '$locale', 'StudentExamRes', 'reservationService', 'dialogs', 'SettingsResource', 'CalendarRes',
        'uiCalendarConfig', 'InteroperabilityResource',
        function ($scope, $http, $location, $translate, $routeParams, dateService, $locale, StudentExamRes,
                  reservationService, dialogs, SettingsResource, CalendarRes, uiCalendarConfig, InteroperabilityRes) {

            $scope.limitations = {};
            $scope.rooms = [];
            $scope.openingHours = [];
            $scope.exceptionHours = [];
            $scope.loader = {
                loading: false
            };
            $scope.eventSources = [];
            $scope.selectedSpace = "ei valittu";
            $scope.selectedTime = "ei valittu";
            $scope.validSelections = false;
            $scope.changeBorder = "notactive";
            $scope.saveActivated = "link-button";
            $scope.start = null;
            $scope.end = null;

            StudentExamRes.examInfo.get({eid: $routeParams.id}, function (info) {
                $scope.examInfo = info;
                uiCalendarConfig.calendars.externalCalendar.fullCalendar('gotoDate', moment.max(moment(),
                    moment($scope.examInfo.examActiveStartDate)));
            });
            SettingsResource.reservationWindow.get(function (setting) {
                $scope.reservationWindowSize = setting.value;
                $scope.reservationWindowEndDate = moment().add(setting.value, 'days');
            });

            $scope.showReservationWindowInfo = function () {
                if ($scope.examInfo && $scope.reservationWindowEndDate) {
                    return moment($scope.examInfo.examActiveEndDate) > $scope.reservationWindowEndDate;
                }
                return false;
            };

            $scope.getReservationWindowDescription = function () {
                return $translate.instant('sitnet_description_reservation_window')
                        .replace('{}', $scope.reservationWindowSize) + ' (' +
                    $scope.reservationWindowEndDate.format('DD.MM.YYYY') + ')';
            };

            $scope.selectedRoom = function () {
                var room = null;
                $scope.rooms.some(function (r) {
                    if (r.filtered) {
                        room = r;
                        return true;
                    }
                });
                return room;
            };

            $scope.makeInternalReservation = function () {
                $location.path('/calendar/' + $routeParams.id);
            };

            var adjust = function (date, tz) {
                date = moment.tz(date, tz);
                var offset = date.isDST() ? -1 : 0;
                return date.add(offset, 'hour').format();
            };

            var adjustBack = function (date, tz) {
                date = moment.tz(date, tz);
                var offset = date.isDST() ? 1 : 0;
                return moment.utc(date.add(offset, 'hour')).format();
            };

            var getTitle = function (slot) {
                if (slot.availableMachines > 0) {
                    return $translate.instant('sitnet_slot_available') + ' (' + slot.availableMachines + ')';
                }
                if (slot.availableMachines < 0) {
                    return slot.conflictingExam || $translate.instant('sitnet_own_reservation');
                }
                return $translate.instant('sitnet_reserved');
            };

            var getColor = function (slot) {
                if (slot.availableMachines < 0) {
                    return '#266B99';
                }
                if (slot.availableMachines > 0) {
                    return '#A6E9B2';
                }
                return '#D8D8D8';
            };

            var refresh = function (start, callback) {
                var date = start.format();
                var room = $scope.selectedRoom();
                if (room) {
                    $scope.loader.loading = true;
                    InteroperabilityRes.slots.query({
                            examId: $routeParams.id,
                            roomRef: room._id,
                            org: $scope.selectedOrganisation._id,
                            date: date
                        },
                        function (slots) {
                            var tz = room.localTimezone;
                            var events = slots.map(function (slot) {
                                return {
                                    title: getTitle(slot),
                                    color: getColor(slot),
                                    start: adjust(slot.start, tz),
                                    end: adjust(slot.end, tz),
                                    availableMachines: slot.availableMachines
                                };
                            });
                            callback(events);
                            $scope.loader.loading = false;
                        }, function (error) {
                            $scope.loader.loading = false;
                            if (error && error.status === 404) {
                                toastr.error($translate.instant('sitnet_exam_not_active_now'));
                            } else if (error) {
                                toastr.error(error.data.message);
                            }
                            else {
                                toastr.error($translate.instant('sitnet_no_suitable_enrolment_found'));
                            }
                        });
                    $scope.exceptionHours = reservationService.getExceptionHours();
                }
            };

            var listExternalRooms = function () {
                if ($scope.selectedOrganisation) {
                    InteroperabilityRes.facilities.query({org: $scope.selectedOrganisation._id}, function (data) {
                        $scope.rooms = data;
                    });
                }
            };

            InteroperabilityRes.organisations.query(function (data) {
                $scope.organisations = data.filter(function (org) {
                    return !org.homeOrg;
                });
            });

            $scope.setOrganisation = function (org) {
                $scope.selectedOrganisation = org;
                $scope.rooms.forEach(function (r) {
                    delete r.filtered;
                });
                //uiCalendarConfig.calendars.externalCalendar.fullCalendar('refetchEvents');
                listExternalRooms();
            };

            $scope.$on('$localeChangeSuccess', function () {
                $scope.calendarConfig.buttonText.today = $translate.instant('sitnet_today');
                $scope.openingHours = reservationService.processOpeningHours($scope.selectedRoom());
            });

            var reserve = function (start, end) {
                var tz = $scope.selectedRoom().localTimezone;
                var slot = {};
                slot.start = adjustBack(start, tz);
                slot.end = adjustBack(end, tz);
                slot.orgId = $scope.selectedOrganisation._id;
                slot.examId = $routeParams.id;
                slot.roomId = $scope.selectedRoom()._id;
                InteroperabilityRes.reservations.create(slot, function () {
                    $location.path('/');
                }, function (error) {
                    toastr.error(error.data);
                });
            };

            $scope.createReservation = function (start, end) {

                $scope.selectedSpace = $scope.selectedRoom().name;
                $scope.selectedTime = start.format("DD.MM.YYYY HH:mm") + " - " + end.format("HH:mm");
                $scope.validSelections = true;
                $scope.changeBorder = "";
                $scope.saveActivated = "calendar-button-save-activated";
                $scope.start = start;
                $scope.end = end;
            };

            $scope.confirmReservation = function () {

                if ($scope.start && $scope.end) {
                    //console.log('confirming reservation to: ' + $scope.start + ', ' +$scope.end);
                    reserve($scope.start, $scope.end);
                }
            };

            $scope.getDescription = function (room) {
                if (room.outOfService) {
                    var status = room.statusComment ? ": " + room.statusComment : "";
                    return $translate.instant("sitnet_room_out_of_service") + status;
                }
                return room.name;
            };

            $scope.getRoomAccessibility = function () {
                if (!$scope.selectedRoom()) {
                    return;
                }
                return $scope.selectedRoom().accessibility.map(function (a) {
                    return a.name;
                }).join(', ');
            };

            $scope.getRoomInstructions = function () {
                if (!$scope.selectedRoom()) {
                    return;
                }
                var info;
                switch ($translate.use()) {
                    case "fi":
                        info = $scope.selectedRoom().roomInstruction;
                        break;
                    case "sv":
                        info = $scope.selectedRoom().roomInstructionSV;
                        break;
                    case "en":
                    /* falls through */
                    default:
                        info = $scope.selectedRoom().roomInstructionEN;
                        break;
                }
                return info;
            };

            $scope.printExamDuration = function (exam) {
                return dateService.printExamDuration(exam);
            };

            $scope.selectRoom = function (room) {
                if (!room.outOfService) {
                    $scope.rooms.forEach(function (room) {
                        delete room.filtered;
                    });
                    room.filtered = true;
                    $scope.openingHours = reservationService.processOpeningHours(room);
                    var minTime = reservationService.getEarliestOpening(room);
                    var maxTime = reservationService.getLatestClosing(room);
                    var hiddenDays = reservationService.getClosedWeekdays(room);
                    $("#calendar").fullCalendar(
                        $.extend($scope.calendarConfig, {
                            timezone: room.localTimezone,
                            minTime: minTime,
                            maxTime: maxTime,
                            scrollTime: minTime,
                            hiddenDays: hiddenDays
                        })
                    );
                }
            };

            $scope.calendarConfig = {
                editable: false,
                selectable: false,
                selectHelper: false,
                defaultView: 'agendaWeek',
                allDaySlot: false,
                weekNumbers: false,
                firstDay: 1,
                timeFormat: 'H:mm',
                columnFormat: 'dddd D.M',
                titleFormat: 'D.M.YYYY',
                slotLabelFormat: 'H:mm',
                slotEventOverlap: false,
                buttonText: {
                    today: $translate.instant('sitnet_today')
                },
                header: {
                    left: 'myCustomButton',
                    center: 'prev title next',
                    right: 'today'
                },
                customButtons: {
                    myCustomButton: {
                        text: moment().format('MMMM YYYY'),
                        click: function () {

                        }
                    }
                },
                events: function (start, end, timezone, callback) {
                    reservationService.renderCalendarTitle();
                    refresh(start, callback);
                },
                eventClick: function (event) {
                    $scope.validSelections = false;
                    if (event.availableMachines > 0) {
                        $scope.createReservation(event.start, event.end);
                    }
                },
                eventMouseover: function (event, jsEvent, view) {
                    if (event.availableMachines > 0) {
                        $(this).css('background-color', '#3CA34F');
                        $(this).css('border-color', '#358F45');
                        $(this).css('color', '#2c2c2c');
                        $(this).css('cursor', 'pointer');
                    }
                },
                eventMouseout: function (event, jsEvent, view) {
                    if (event.availableMachines > 0) {
                        $(this).css('color', 'white');
                        $(this).css('border-color', '#979797');
                        $(this).css('background-color', '#A6E9B2');
                    }
                },
                eventRender: function (event, element, view) {
                    if (event.availableMachines > 0) {
                        element.attr('title', $translate.instant('sitnet_new_reservation') + " " +
                            event.start.format("HH:mm") + " - " + event.end.format("HH:mm"));
                    }
                },
                eventAfterAllRender: function (view) {
                    // Disable next/prev buttons if date range is off limits
                    var prevButton = $(".fc-prev-button");
                    var nextButton = $(".fc-next-button");
                    var todayButton = $(".fc-today-button");

                    var minDate = !$scope.examInfo ? moment() : moment.max(moment(),
                        moment($scope.examInfo.examActiveStartDate));
                    var maxDate = !$scope.examInfo ? moment() : moment.min($scope.reservationWindowEndDate,
                        moment($scope.examInfo.examActiveEndDate));
                    var today = moment();

                    if (minDate >= view.start && minDate <= view.end) {
                        prevButton.prop('disabled', true);
                        prevButton.addClass('fc-state-disabled');
                    }
                    else {
                        prevButton.removeClass('fc-state-disabled');
                        prevButton.prop('disabled', false);
                    }
                    if (maxDate >= view.start && maxDate <= view.end) {
                        nextButton.prop('disabled', true);
                        nextButton.addClass('fc-state-disabled');
                    } else {
                        nextButton.removeClass('fc-state-disabled');
                        nextButton.prop('disabled', false);
                    }
                    if (today < minDate) {
                        todayButton.prop('disabled', true);
                        todayButton.addClass('fc-state-disabled');
                    } else {
                        todayButton.removeClass('fc-state-disabled');
                        todayButton.prop('disabled', false);
                    }
                }
            };
        }
    ]);

