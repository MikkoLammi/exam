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

import * as angular from 'angular';
import * as toast from 'toastr';
import * as moment from 'moment';
import { Room, Slot, CalendarService } from './calendar.service';
import { DateTimeService } from '../utility/date/date.service';
import { IHttpResponse } from 'angular';

interface ExamInfo {
    examActiveStartDate: number;
    examActiveEndDate: number;
}

interface AvailableSlot extends Slot {
    availableMachines: number;
}

interface Accessibility {
    id: number;
    name: string;
    filtered: boolean;
}

interface FilteredRoom extends Room {
    filtered: boolean;
}

export const CalendarComponent: angular.IComponentOptions = {
    template: require('./calendar.template.html'),
    bindings: {
        isExternal: '<',
        isCollaborative: '<'
    },
    controller: class CalendarController implements angular.IComponentController {

        isExternal: boolean;
        isCollaborative: boolean;

        accessibilities: Accessibility[] = [];
        isInteroperable: boolean;
        confirming = false;
        examInfo: ExamInfo = { examActiveStartDate: 0, examActiveEndDate: 0 };
        limitations = {};
        openingHours: any[];
        organisations: any[];
        reservation: { room: string, start: moment.Moment, end: moment.Moment, time: string };
        rooms: FilteredRoom[] = [];
        exceptionHours: any[];
        loader = {
            loading: false
        };
        minDate: moment.Moment;
        maxDate: moment.Moment;
        reservationWindowEndDate: moment.Moment;
        reservationWindowSize: number;
        selectedRoom: FilteredRoom | undefined;
        selectedOrganisation: { _id: string, name: string, filtered: boolean };

        constructor(
            private $http: angular.IHttpService,
            private $scope: angular.IScope,
            private $location: angular.ILocationService,
            private $translate: angular.translate.ITranslateService,
            private $routeParams: angular.route.IRouteParamsService,
            private DateTime: DateTimeService,
            private Calendar: CalendarService,
            private uiCalendarConfig: any
        ) {
            'ngInject';
        }

        $onInit() {
            this.$scope.$on('$localeChangeSuccess', () => {
                const optionalRoom = this.selectedRoom;
                if (optionalRoom !== undefined) {
                    this.openingHours = this.Calendar.processOpeningHours(optionalRoom);
                }
            });

            this.$http.get('/app/settings/iop')
                .then((resp: IHttpResponse<{ isInteroperable: boolean }>) => {
                    this.isInteroperable = resp.data.isInteroperable;
                    // TODO: allow making external reservations to collaborative exams in the future
                    if (this.isInteroperable && this.isExternal && !this.isCollaborative) {
                        this.$http.get('/integration/iop/organisations').then((resp: IHttpResponse<any[]>) => {
                            this.organisations = resp.data.filter(org => !org.homeOrg);
                        });
                    }
                }).catch(resp => angular.noop);
            const url = this.isCollaborative ?
                `/integration/iop/exams/${this.$routeParams.id}/info` :
                `/app/student/exam/${this.$routeParams.id}/info`;
            this.$http.get(url).then((resp: IHttpResponse<ExamInfo>) => {
                this.examInfo = resp.data;
                this.$http.get('/app/settings/reservationWindow').then((resp: IHttpResponse<{ value: number }>) => {
                    this.reservationWindowSize = resp.data.value;
                    this.reservationWindowEndDate = moment().add(resp.data.value, 'days');
                    this.minDate = moment.max(moment(),
                        moment(this.examInfo.examActiveStartDate));
                    this.maxDate = moment.min(this.reservationWindowEndDate,
                        moment(this.examInfo.examActiveEndDate));

                    this.$http.get('/app/accessibility').then((resp: IHttpResponse<Accessibility[]>) => {
                        this.accessibilities = resp.data;
                    });

                    this.$http.get('/app/rooms').then((resp: IHttpResponse<Room[]>) => {
                        this.rooms = resp.data.map(r => angular.extend(r, { filtered: false }));
                    });

                });
            });
        }

        showReservationWindowInfo(): boolean {
            return moment(this.examInfo.examActiveEndDate) > this.reservationWindowEndDate;
        }

        getReservationWindowDescription(): string {
            const text = this.$translate.instant('sitnet_description_reservation_window')
                .replace('{}', this.reservationWindowSize.toString());
            return `${text} ( ${this.reservationWindowEndDate.format('DD.MM.YYYY')} )`;
        }

        selectedAccessibilities() {
            return this.accessibilities.filter(a => a.filtered);
        }

        getRoomInstructions(): string | undefined {
            const optionalRoom = this.selectedRoom;
            if (optionalRoom == undefined) {
                return;
            }
            const room: Room = optionalRoom;
            let info;
            switch (this.$translate.use()) {
                case 'fi':
                    info = room.roomInstruction;
                    break;
                case 'sv':
                    info = room.roomInstructionSV;
                    break;
                case 'en':
                /* falls through */
                default:
                    info = room.roomInstructionEN;
                    break;
            }
            return info;
        }

        getRoomAccessibility(): string {
            const room = this.selectedRoom;
            return room ? room.accessibilities.map(a => a.name).join(', ') : '';
        }

        makeExternalReservation() {
            this.$location.path('/iop/calendar/' + this.$routeParams.id);
        }

        makeInternalReservation() {
            this.$location.path('/calendar/' + this.$routeParams.id);
        }

        private adjust(date, tz): string {
            let adjusted: moment.Moment = moment.tz(date, tz);
            const offset = adjusted.isDST() ? -1 : 0;
            return adjusted.add(offset, 'hour').format();
        }

        private getTitle(slot: AvailableSlot): string {
            if (slot.availableMachines > 0) {
                return `${this.$translate.instant('sitnet_slot_available')} (${slot.availableMachines})`;
            } else {
                return slot.conflictingExam ?
                    this.$translate.instant('sitnet_own_reservation') :
                    this.$translate.instant('sitnet_reserved');
            }
        }

        private getColor(slot: AvailableSlot) {
            if (slot.availableMachines < 0) {
                return '#266B99';
            } else if (slot.availableMachines > 0) {
                return '#A6E9B2';
            } else {
                return '#D8D8D8';
            }
        }

        private query(success: (_: IHttpResponse<any>) => void,
            error: (_: IHttpResponse<any>) => void, date: string, room: Room, accessibility) {

            if (this.isExternal) {
                this.$http.get(`/integration/iop/calendar/${this.$routeParams.id}/${room._id}`, {
                    params: {
                        org: this.selectedOrganisation._id,
                        date: date
                    }
                }).then(success).catch(error);
            } else {
                const url = this.isCollaborative ?
                    `/integration/iop/exams/${this.$routeParams.id}/calendar/${room.id}` :
                    `/app/calendar/${this.$routeParams.id}/${room.id}`;
                this.$http.get(url, {
                    params: {
                        day: date,
                        aids: accessibility
                    }
                }).then(success).catch(error);
            }
        }

        render() {
            $('#calendarBlock').css({ display: 'block' });
            $('#calendar').css({ position: 'relative', visibility: 'visible', display: 'block' });
            $('#calendar').fullCalendar('destroy');
            $('#calendar').fullCalendar('render');
        }

        refresh(start: moment.Moment, callback: (_: any[]) => void) {
            const room = this.selectedRoom;
            if (!room) {
                // callback([]);
                return;
            }
            const date = start.format();
            const accessibilities = this.accessibilities.filter(i => i.filtered).map(i => i.id);
            this.loader.loading = true;
            const tz = room.localTimezone;
            const successFn = (resp: IHttpResponse<AvailableSlot[]>) => {
                const events = resp.data.map(slot => {
                    return {
                        title: this.getTitle(slot),
                        color: this.getColor(slot),
                        start: this.adjust(slot.start, tz),
                        end: this.adjust(slot.end, tz),
                        availableMachines: slot.availableMachines
                    };
                });
                callback(events);
                this.loader.loading = false;
            };
            const errorFn = (resp: IHttpResponse<any>) => {
                this.loader.loading = false;
                if (resp.status === 404) {
                    toast.error(this.$translate.instant('sitnet_exam_not_active_now'));
                } else if (resp.data) {
                    toast.error(resp.data.message);
                } else {
                    toast.error(this.$translate.instant('sitnet_no_suitable_enrolment_found'));
                }
            };
            this.query(successFn, errorFn, date, room, accessibilities);
            this.exceptionHours = this.Calendar.getExceptionHours(room);
        }

        private listExternalRooms() {
            if (this.selectedOrganisation) {
                this.$http.get('/integration/iop/facilities', {
                    params: {
                        org: this.selectedOrganisation._id
                    }
                }).then((resp: IHttpResponse<FilteredRoom[]>) => this.rooms = resp.data)
                    .catch(angular.noop);
            }
        }

        createReservation(start: moment.Moment, end: moment.Moment) {
            const room = this.selectedRoom;
            if (room !== undefined) {
                this.reservation = {
                    room: room.name,
                    time: start.format('DD.MM.YYYY HH:mm') + ' - ' + end.format('HH:mm'),
                    start: start,
                    end: end
                };
            }
        }

        confirmReservation() {
            const room = this.selectedRoom;
            if (!room || !this.reservation || this.confirming) {
                return;
            }
            this.confirming = true;
            this.Calendar.reserve(
                this.reservation.start,
                this.reservation.end,
                room,
                this.accessibilities,
                { _id: this.selectedOrganisation ? this.selectedOrganisation._id : null },
                this.isCollaborative
            ).then(() => this.confirming = false);
        }

        setOrganisation(org: { _id: string, name: string, filtered: boolean }) {
            this.organisations.forEach(o => o.filtered = false);
            org.filtered = true;
            this.selectedOrganisation = org;
            this.selectedRoom = undefined;
            this.listExternalRooms();
        }

        selectAccessibility(accessibility) {
            accessibility.filtered = !accessibility.filtered;
            if (this.selectedRoom) {
                this.uiCalendarConfig.calendars.myCalendar.fullCalendar('refetchEvents');
            }
        }

        getDescription(room: Room): string {
            if (room.outOfService) {
                const status = room.statusComment ? ': ' + room.statusComment : '';
                return this.$translate.instant('sitnet_room_out_of_service') + status;
            }
            return room.name;
        }

        printExamDuration(exam) {
            return this.DateTime.printExamDuration(exam);
        }

        selectRoom(room: FilteredRoom) {
            if (!room.outOfService) {
                this.rooms.forEach(r => r.filtered = false);
                room.filtered = true;
                this.selectedRoom = room;
                this.openingHours = this.Calendar.processOpeningHours(room);
                this.render();
            }
        }
    }
};
