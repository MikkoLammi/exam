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

angular.module('app.exam')
    .factory('ExamRes', ['$resource', function ($resource) {
        return {
            exams: $resource('/app/exams/:id',
                {
                    id: '@id'
                },
                {
                    'copy': { method: 'POST' },
                    'update': { method: 'PUT' },
                    'remove': { method: 'DELETE' }
                }),
            examsearch: $resource('/app/examsearch'),
            examowner: $resource('/app/exam/:eid/owner/:uid',
                {
                    eid: '@eid', uid: '@uid'
                },
                {
                    'insert': { method: 'POST' },
                    'remove': { method: 'DELETE' }
                }),

            questions: $resource('/app/exams/:eid/sections/:sid/question/:qid',
                {
                    eid: '@eid', sid: '@sid', qid: '@qid'
                },
                {
                    'remove': { method: 'DELETE', params: { eid: '@eid', sid: '@sid', qid: '@qid' } }
                }),
            sections: $resource('/app/exams/:eid/sections/:sid',
                {
                    eid: '@eid', sid: '@sid'
                },
                {
                    'insert': { method: 'POST', params: { eid: '@eid', sid: '@sid' } },
                    'remove': { method: 'DELETE', params: { eid: '@eid', sid: '@sid' } },
                    'update': { method: 'PUT', params: { eid: '@eid', sid: '@sid' } }

                }),
            questionDistribution: $resource('/app/exams/question/:id/distribution', { id: '@id' }),
            sectionquestions: $resource('/app/exams/:eid/sections/:sid/:seq/question/:qid',
                {
                    eid: '@eid', sid: '@sid', seq: '@seq', qid: '@qid'
                },
                {
                    'insert': { method: 'POST', params: { eid: '@eid', sid: '@sid', seq: '@seq', qid: '@qid' } }

                }),
            sectionquestionsmultiple: $resource('/app/exams/:eid/sections/:sid/questions',
                {
                    eid: '@eid', sid: '@sid', questions: '@questions'
                },
                {
                    'insert': {
                        method: 'POST',
                        params: { eid: '@eid', sid: '@sid', questions: '@questions' }
                    }

                }),
            questionOrder: $resource('/app/exams/:eid/sections/:sid/reorder',
                {
                    eid: '@eid', sid: '@sid'
                },
                {
                    'update': { method: 'PUT', params: { eid: '@eid', sid: '@sid' } }
                }),
            sectionOrder: $resource('/app/exams/:eid/reorder',
                {
                    eid: '@eid'
                },
                {
                    'update': { method: 'PUT', params: { eid: '@eid' } }
                }),

            clearsection: $resource('/app/exams/:eid/sections/:sid/clear',
                {
                    eid: '@eid', sid: '@sid'
                },
                {
                    'clear': { method: 'DELETE', params: { eid: '@eid', sid: '@sid' } }
                }),

            course: $resource('/app/exams/:eid/course/:cid',
                {
                    eid: '@eid', sid: '@cid'
                },
                {
                    'update': { method: 'PUT', params: { eid: '@eid', cid: '@cid' } },
                    'delete': { method: 'DELETE', params: { eid: '@eid' } }
                }),
            reviewerExams: $resource('/app/reviewerexams'),
            reviewerExam: $resource('/app/review/:eid', { eid: '@eid' }),
            draft: $resource('/app/exams', null, { 'create': { method: 'POST' } }),
            review: $resource('/app/review/:id', { id: '@id' }, { 'update': { method: 'PUT' } }),
            inspectionComment: $resource('/app/review/:id/inspection', { id: '@id' }, { 'create': { method: 'POST' } }),
            examReviews: $resource('/app/reviews/:eid', { eid: '@eid' },
                {
                    'get': { method: 'GET', params: { eid: '@eid' } }
                }),
            noShows: $resource('/app/noshows/:eid', { eid: '@eid' }),
            archive: $resource('/app/reviews/archive', {}, { 'update': { method: 'PUT' } }),
            comment: $resource('/app/review/:eid/comment/:cid',
                {
                    id: '@eid', cid: '@cid'
                },
                {
                    'insert': { method: 'POST', params: { eid: '@eid' } },
                    'update': { method: 'PUT', params: { eid: '@eid', sid: '@cid' } }
                }),
            inspections: $resource('/app/exam/:id/inspections',
                {
                    id: '@id'
                },
                {
                    'get': { method: 'GET', isArray: true, params: { id: '@id' } }
                }),

            owners: $resource('/app/exam/:id/owners',
                {
                    id: '@id'
                }),
            inspection: $resource('/app/exams/:eid/inspector/:uid',
                {
                    eid: '@eid', uid: '@uid'
                },
                {
                    'insert': { method: 'POST', params: { eid: '@eid', uid: '@uid' } }
                }),

            inspectionReady: $resource('/app/exams/inspection/:id',
                {
                    id: '@id'
                },
                {
                    'update': { method: 'PUT', params: { id: '@id' } }
                }),

            inspector: $resource('/app/exams/inspector/:id',
                {
                    id: '@id'
                },
                {
                    'remove': { method: 'DELETE', params: { id: '@id' } }
                }),

            examEnrolments: $resource('/app/examenrolments/:eid',
                {
                    eid: '@eid'
                },
                {
                    'get': { method: 'GET', params: { eid: '@eid' } }
                }),

            examParticipations: $resource('/app/examparticipations/:eid',
                {
                    eid: '@eid'
                },
                {
                    'get': { method: 'GET', params: { eid: '@eid' } }
                }),
            examParticipationsOfUser: $resource('/app/examparticipations/:eid/:uid',
                {
                    eid: '@eid',
                    uid: '@uid'
                },
                {
                    'get': { method: 'GET', params: { eid: '@eid', uid: '@uid' } }
                }),
            studentInfo: $resource('/app/review/info/:id',
                {
                    id: '@id'
                }),
            email: $resource('/app/email/inspection/:eid',
                {
                    eid: '@eid'
                },
                {
                    inspection: { method: 'POST', params: { eid: '@eid' } }
                }),

            saveRecord: $resource('/app/exam/record', null,
                {
                    'add': { method: 'POST' }
                }),
            register: $resource('/app/exam/register', null,
                {
                    'add': { method: 'POST' }
                }),
            record: $resource('/app/exam/record/export/:id',
                {
                    id: '@id'
                },
                {
                    'export': { method: 'GET', params: { id: '@id' } }
                }),
            language: $resource('/app/exam/:eid/language/:code',
                {
                    eid: '@eid',
                    code: '@code'
                },
                {
                    'update': { method: 'PUT' }
                }),
            languages: $resource('/app/exam/:eid/languages',
                {
                    eid: '@eid'
                },
                {
                    'reset': { method: 'DELETE' }
                }),
            examTypes: $resource('/app/examtypes'),
            executionTypes: $resource('/app/executiontypes'),
            gradeScales: $resource('/app/gradescales'),
            software: $resource('/app/exam/:eid/software/:sid',
                {
                    eid: '@eid',
                    sid: '@sid'
                },
                {
                    'update': { method: 'PUT' }
                }),
            reservation: $resource('/app/reservations/:id',
                {
                    id: '@id'
                },
                {
                    'update': { method: 'PUT' }
                }),
            reservationInfo: $resource('/app/exams/:eid/reservation', { eid: '@eid' }),
            examinationDate: $resource('/app/exam/:eid/examinationdate/:edid', { eid: '@eid', edid: '@edid' },
                {
                    'create': { method: 'POST', params: { eid: '@eid' } },
                    'delete': { method: 'DELETE' }
                })
        };
    }]);

