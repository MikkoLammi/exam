'use strict';

angular.module('app').config(['$translateProvider', '$routeProvider', '$httpProvider', '$locationProvider',
    '$compileProvider', 'tmhDynamicLocaleProvider', 'EXAM_CONF',
    function ($translateProvider, $routeProvider, $httpProvider, $locationProvider, $compileProvider,
              tmhDynamicLocaleProvider, EXAM_CONF) {
        $compileProvider.debugInfoEnabled(false);
        $httpProvider.useApplyAsync(true);

        // IE caches each and every GET unless the following is applied:
        if (!$httpProvider.defaults.headers.get) {
            $httpProvider.defaults.headers.get = {};
        }
        $httpProvider.defaults.headers.get['Cache-Control'] = 'no-cache';
        $httpProvider.defaults.headers.get.Pragma = 'no-cache';

        var path = EXAM_CONF.LANGUAGES_PATH;
        $translateProvider.useStaticFilesLoader({
            prefix: path + 'locale-',
            suffix: '.json'
        });
        $translateProvider.useSanitizeValueStrategy(null);
        $translateProvider.preferredLanguage('en');

        tmhDynamicLocaleProvider.localeLocationPattern(
            '/webjars/angular-i18n/' + EXAM_CONF.NG_VERSION + '/angular-locale_{{locale}}.js');

        $locationProvider.html5Mode({enabled: true, requireBase: false});

        // ROUTING -->

        var tmpl = EXAM_CONF.TEMPLATES_PATH;

        /* index */
        $routeProvider.when('/', {template: '<dashboard></dashboard>'});

        // questions
        $routeProvider.when('/questions', {template: '<library></library>'});
        $routeProvider.when('/questions/:id', {template: '<question new-question="false"></question>'});
        $routeProvider.when('/questions/newQuestion/:create', {template: '<question new-question="true"></question>'});

        /* exams */
        $routeProvider.when('/exams/:id/:tab', {template: '<exam-tabs></exam-tabs>'});
        $routeProvider.when('/exams/new', {template: '<new-exam></new-exam>'});
        $routeProvider.when('/exams/:id/course', {template: '<course-selection></course-selection>'});
        $routeProvider.when('/exams/preview/:id', {template: '<examination is-preview="true"><examination>'});
        $routeProvider.when('/exams/preview/:id/:tab', {template: '<examination is-preview="true"><examination>'}); //TODO: Can these be combined with :tab? ?
        $routeProvider.when('/exams/printout/:id', {
            templateUrl: tmpl + 'exam/printout/printout.html',
            controller: 'PrintoutController',
            controllerAs: 'ctrl'
        });
        $routeProvider.when('/exams/printout/:id/:tab', {
            templateUrl: tmpl + 'exam/printout/printout.html',
            controller: 'PrintoutController',
            controllerAs: 'ctrl'
        });
        $routeProvider.when('/printouts', {
            templateUrl: tmpl + 'exam/printout/printouts.html',
            controller: 'PrintoutController',
            controllerAs: 'ctrl'
        });

        /* calendar */
        $routeProvider.when('/calendar/:id', {template: '<calendar is-external="false"></calendar>'});
        $routeProvider.when('/iop/calendar/:id', {template: '<calendar is-external="true"></calendar>'});

        /* logout */
        $routeProvider.when('/logout', {template: '<logout></logout>'});

        /* Student */
        $routeProvider.when('/student/exam/:hash', {template: '<examination is-preview="false"><examination>'});
        $routeProvider.when('/student/waitingroom', {
            templateUrl: tmpl + 'enrolment/waitingroom.html',
            controller: 'WaitingRoomCtrl'
        });
        $routeProvider.when('/student/wrongmachine', {
            templateUrl: tmpl + 'enrolment/wrong_machine.html',
            controller: 'WrongMachineCtrl'
        });
        $routeProvider.when('/student/exams', {template: '<exam-search></exam-search>'});
        $routeProvider.when('/student/participations', {template: '<exam-participations></exam-participations>'});
        $routeProvider.when('/student/logout/:reason?', {template: '<examination-logout></examination-logout>'});
        $routeProvider.when('/enroll/:code/exam/:id', {template: '<exam-enrolments></exam-enrolments>'});


        /* review */
        $routeProvider.when('/exams/review/:id', {template: '<assessment></assessment>'});
        $routeProvider.when('/exams/reviews/:id/speedreview', {template: '<speed-review></speed-review>'});
        $routeProvider.when('/print/exam/:id', {template: '<printed-assessment></printed-assessment>'});

        /* reservations */
        $routeProvider.when('/reservations', {template: '<reservations user-role="teacher"></reservations>'});
        $routeProvider.when('/reservations/:eid', {template: '<reservations user-role="teacher"></reservations>'});

        /* Admin */
        $routeProvider.when('/exams', {templateUrl: tmpl + 'exam/exams.html', controller: 'ExamListingController'});
        $routeProvider.when('/rooms', {templateUrl: tmpl + 'facility/rooms.html', controller: 'RoomCtrl'});
        $routeProvider.when('/rooms/:id', {templateUrl: tmpl + 'facility/room.html', controller: 'RoomCtrl'});
        $routeProvider.when('/rooms_edit/edit_multiple', {
            templateUrl: tmpl + 'facility/room.html',
            controller: 'RoomCtrl'
        });

        $routeProvider.when('/softwares', {templateUrl: tmpl + 'facility/software.html', controller: 'RoomCtrl'});
        $routeProvider.when('/accessibility', {template: '<accessibility></accessibility>'});
        $routeProvider.when('/machines/:id', {template: '<machine></machine>'});
        $routeProvider.when('/softwares/update/:id/:name', {
            templateUrl: tmpl + 'facility/software.html',
            controller: 'RoomCtrl'
        });
        $routeProvider.when('/softwares/:id', {templateUrl: tmpl + 'facility/software.html', controller: 'RoomCtrl'});
        $routeProvider.when('/softwares/add/:name', {
            templateUrl: tmpl + 'facility/software.html',
            controller: 'RoomCtrl'
        });


        $routeProvider.when('/reports', {template: '<reports></reports>'});
        $routeProvider.when('/statistics', {template: '<statistics></statistics>'});
        $routeProvider.when('/settings', {template: '<settings></settings>'});
        $routeProvider.when('/users', {template: '<users></users>'});

        /* Language inspectors */
        $routeProvider.when('/inspections', {template: '<language-inspections></language-inspections>'});
        $routeProvider.when('/inspections/reports', {template: '<maturity-reporting></maturity-reporting>'});

        $routeProvider.otherwise({redirectTo: '/'});


        // HTTP INTERCEPTOR
        $httpProvider.interceptors.push(['$q', '$cookies', 'Session', '$rootScope', '$location', '$translate',
            'wrongRoomService', 'waitingRoomService',
            function ($q, $cookies, Session, $rootScope, $location, $translate, wrongRoomService, waitingRoomService) {
                return {
                    'request': function (request) {
                        if (request.method !== 'GET') {
                            var csrfToken = $cookies.get('csrfToken');
                            request.url += '?csrfToken=' + csrfToken;
                        }
                        return request;
                    },
                    'response': function (response) {

                        var b64_to_utf8 = function (data) {
                            return decodeURIComponent(escape(atob(data)));
                        };

                        var unknownMachine = response.headers()['x-exam-unknown-machine'];
                        var wrongRoom = response.headers()['x-exam-wrong-room'];
                        var wrongMachine = response.headers()['x-exam-wrong-machine'];
                        var hash = response.headers()['x-exam-start-exam'];

                        var enrolmentId = response.headers()['x-exam-upcoming-exam'];
                        var parts;
                        if (unknownMachine) {
                            var location = b64_to_utf8(unknownMachine).split(':::');
                            wrongRoomService.display(location);
                        }
                        else if (wrongRoom) {
                            parts = b64_to_utf8(wrongRoom).split(':::');
                            waitingRoomService.setEnrolmentId(parts[0]);
                            waitingRoomService.setActualRoom(parts[1] + ' (' + parts[2] + ')');
                            waitingRoomService.setActualMachine(parts[3]);
                            $location.path('/student/wrongmachine');
                            $rootScope.$broadcast('wrongMachine');
                        }
                        else if (wrongMachine) {
                            parts = b64_to_utf8(wrongMachine).split(':::');
                            waitingRoomService.setEnrolmentId(parts[0]);
                            waitingRoomService.setActualMachine(parts[1]);
                            $location.path('/student/wrongmachine');
                            $rootScope.$broadcast('wrongMachine');
                        }
                        else if (hash) {
                            if (enrolmentId) {
                                waitingRoomService.setEnrolmentId(enrolmentId);
                                $location.path('/student/waitingroom');
                                $rootScope.$broadcast('upcomingExam');
                            } else {
                                $location.path('/student/exam/' + hash);
                                $rootScope.$broadcast('examStarted');
                            }
                        } else if (enrolmentId) {
                            // no exams for today
                            waitingRoomService.setEnrolmentId(null);
                            $location.path('/student/waitingroom');
                            $rootScope.$broadcast('upcomingExam');
                        }

                        return response;
                    },
                    'responseError': function (response) {
                        if (response.status === -1) {
                            // connection failure
                            toastr.error($translate.instant('sitnet_connection_refused'));
                        }
                        else if (typeof response.data === 'string' || response.data instanceof String) {
                            var deferred = $q.defer();
                            if (response.data.match(/^".*"$/g)) {
                                response.data = response.data.slice(1, response.data.length - 1);
                            }
                            var parts = response.data.split(' ');
                            $translate(parts).then(function (t) {
                                for (var i = 0; i < parts.length; i++) {
                                    if (parts[i].substring(0, 7) === 'sitnet_') {
                                        parts[i] = t[parts[i]];
                                    }
                                }
                                response.data = parts.join(' ');
                                return deferred.reject(response);
                            });
                            return deferred.promise;
                        }
                        return $q.reject(response);
                    }
                };
            }
        ]);

    }
]);