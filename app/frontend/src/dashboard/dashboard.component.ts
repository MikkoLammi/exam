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
import { SessionService, User } from '../session/session.service';

export const DashboardComponent: angular.IComponentOptions = {
    template: require('./dashboard.template.html'),
    controller: class DashboardController implements angular.IComponentController {

        user: User;

        constructor(private Session: SessionService) { 'ngInject'; }

        $onInit() {
            this.user = this.Session.getUser();
            if (!this.user) {
                console.log('not logged in');
            }
        }
    }
};
