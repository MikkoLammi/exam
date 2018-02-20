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
import * as toastr from 'toastr';
import { SessionService, User } from '../session/session.service';
import { Link, NavigationService } from './navigation.service';

declare function require(name: string): any;

export const NavigationComponent: angular.IComponentOptions = {
    template: require('./navigation.template.html'),
    controller: class NavigationController implements angular.IComponentController {

        appVersion: string;
        links: Link[];
        minimizeNavigation: boolean;
        mobileMenuOpen: boolean;
        user: User;

        constructor(
            private $rootScope: angular.IRootScopeService,
            private $location: angular.ILocationService,
            private $window: angular.IWindowService,
            private Navigation: NavigationService,
            private Session: SessionService) {
            'ngInject';

            $rootScope.$on('userUpdated', () => {
                this.user = this.Session.getUser();
                this.links = Navigation.getLinks();
            });
            $rootScope.$on('upcomingExam', () => this.links = Navigation.getLinks());
            $rootScope.$on('wrongLocation', () => this.links = Navigation.getLinks());
        }

        $onInit() {
            this.links = this.Navigation.getLinks();
            this.user = this.Session.getUser();
            if (this.user && this.user.isAdmin) {
                this.Navigation.getAppVersion()
                    .then(resp => this.appVersion = resp.data.appVersion)
                    .catch(e => toastr.error(e.data));
            } else {
                this.minimizeNavigation = this.$window.matchMedia('(min-width: 600px)').matches;
            }
        }

        isActive(link: Link): boolean {
            return link.href === this.$location.path();
        }

        openMenu(): void {
            this.mobileMenuOpen = !this.mobileMenuOpen;
        }

        switchLanguage(key): void {
            this.Session.switchLanguage(key);
        }

    }
};
