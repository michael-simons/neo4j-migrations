/*
 * Copyright 2020-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {LitElement, html} from 'lit';
import {JsonRpc} from 'jsonrpc';
import { notifier } from 'notifier';

import '@vaadin/button';
import '@vaadin/confirm-dialog';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/horizontal-layout';
import '@vaadin/vertical-layout';

/**
 * This component renders build time data in a table
 */
export class QwcNeo4jMigrations extends LitElement {

    rpc = new JsonRpc(this);

    static properties = {
        _connectionDetails: {state: true},
        _elements: {state: true},
        _confirmOpened: {state: true}
    };

    connectedCallback() {
        super.connectedCallback();
        this._connectionDetails = {
            username: 'n/a',
            serverAddress: 'n/a',
            serverVersion: 'n/a'
        };
        this._elements = [];
        this._confirmOpened = false;
        this.rpc.getAllMigrations().then(jsonRpcResponse => {
            this._elements = jsonRpcResponse.result;
        });
        this.rpc.getConnectionDetails().then(jsonRpcResponse => {
            this._connectionDetails = jsonRpcResponse.result;
        });
    }

    render() {
        return html`
            <vaadin-confirm-dialog
                    header='Clean history of applied migrations'
                    cancel-button-visible
                    confirm-text="Clean"
                    confirm-theme="error primary"
                    .opened="${this._confirmOpened}"
                    @cancel="${() => {
                        this._confirmOpened = false;
                    }}"
                    @confirm="${() => {
                        this._clean();
                    }}"
            >
                This will delete all objects created by Neo4j-Migrations in the currently configured schema database and for the selected target database. 
                It won't roll back or revert any data created by your migrations.<br>
                Do you want to continue?
            </vaadin-confirm-dialog>
            
            <vaadin-details summary="Connection details">
                <vaadin-vertical-layout>
                    <vaadin-horizontal-layout>
                        <span><strong>Connection:</strong></span>&#160;
                        <span>${this._connectionDetails.username}@${this._connectionDetails.serverAddress} (${this._connectionDetails.serverVersion})</span>
                    </vaadin-horizontal-layout>
                    ${(() => {
                        if (this._connectionDetails.hasOwnProperty('database')) {
                            return html`
                                <vaadin-horizontal-layout>
                                    <span><strong>Database:</strong></span>&#160;
                                    <span>${this._connectionDetails.database}</span>
                                </vaadin-horizontal-layout>
                            `
                        } else {
                            return html``
                        }
                    })()}
                    ${(() => {
                        if (this._connectionDetails.hasOwnProperty('schemaDatabase')) {
                            return html`
                                <vaadin-horizontal-layout>
                                    <span><strong>Schema database:</strong></span>&#160;
                                    <span>${this._connectionDetails.schemaDatabase}</span>
                                </vaadin-horizontal-layout>
                            `
                        } else {
                            return html``
                        }
                    })()}
                </vaadin-vertical-layout>
            </vaadin-details>
            
            <vaadin-grid .items="${this._elements}" class="datatable" theme="no-border">
                <vaadin-grid-sort-column auto-width header="Version" path="version"></vaadin-grid-sort-column>
                <vaadin-grid-column auto-width header="Description" path="description"></vaadin-grid-column>
                <vaadin-grid-sort-column auto-width header="Type" path="type"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Installed on" path="installedOn"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="by" path="installedBy"></vaadin-grid-sort-column>
                <vaadin-grid-column auto-width header="Execution time" path="executionTime"></vaadin-grid-column>
                <vaadin-grid-sort-column auto-width header="State" path="state"></vaadin-grid-sort-column>
                <vaadin-grid-column auto-width header="Source" path="source"></vaadin-grid-column>
            </vaadin-grid>

            <vaadin-horizontal-layout>
                <vaadin-button theme="primary" @click="${() => this._migrate()}">Migrate</vaadin-button>&#160;
                <vaadin-button theme="error" @click="${() => this._confirmOpened = true}">Clean</vaadin-button>
            </vaadin-horizontal-layout>
        `;
    }


    _migrate() {
        this.rpc.migrate().then(response => {
            this._elements = response.result.elements;
            notifier.showPrimarySuccessMessage(response.result.message, null);
        });
    }

    _clean() {
        this._confirmOpened = false;
        this.rpc.clean().then(response => {
            this._elements = response.result.elements;
            notifier.showPrimarySuccessMessage(response.result.message, null);
        });
    }
}

customElements.define('qwc-neo4j-migrations', QwcNeo4jMigrations);
