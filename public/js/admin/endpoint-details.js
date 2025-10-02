// public/javascripts/endpoints.js
document.addEventListener("DOMContentLoaded", () => {
    const container = document.getElementById("endpoints-container");
    const addBtn = document.getElementById("addEndpoint");

    if (!container) return;

    // --- Load existing endpoints if any ---
    let existing = [];
    try {
        existing = JSON.parse(container.dataset.existing || "[]");
    } catch (e) {
        console.error("Invalid existing endpoint JSON", e);
    }

    existing.forEach((ep, i) => renderEndpoint(ep, i));

    addBtn.addEventListener("click", () => {
        renderEndpoint({}, container.children.length);
    });

    // --- Renderer ---
    function renderEndpoint(data, index) {
        const template = `
        <div class="card mb-3 shadow-sm p-3 endpoint-card">
            <div class="d-flex justify-content-between align-items-center">
                <h6>Endpoint #${index + 1}</h6>
                <button type="button" class="btn btn-sm btn-outline-danger remove-endpoint">
                    <i class="bi bi-trash"></i>
                </button>
            </div>
            <div class="row g-3 mt-1">
                <div class="col-md-6">
                    <label class="form-label">Operation Type</label>
                    <select class="form-select" name="endpoints[${index}].operationType">
                        <option value="UNIQUE_TRANSACTION_SEARCH">Unique Transaction</option>
                        <option value="BULK_TRANSACTION_SEARCH">Bulk Transaction</option>
                    </select>
                </div>
                <div class="col-md-6">
                    <label class="form-label">HTTP Method</label>
                    <select class="form-select" name="endpoints[${index}].method">
                        <option value="GET">GET</option>
                        <option value="POST">POST</option>
                        <option value="PUT">PUT</option>
                        <option value="DELETE">DELETE</option>
                    </select>
                </div>
                <div class="col-md-12">
                    <label class="form-label">URL</label>
                    <input type="text" class="form-control" name="endpoints[${index}].url" value="${data.url || ''}">
                </div>
            </div>

            <!-- Path Params -->
            <div class="mt-3">
                <h6>Path Params</h6>
                <div class="params-container" id="pathParams-${index}"></div>
                <button type="button" class="btn btn-outline-secondary btn-sm add-path" data-index="${index}">
                    Add Path Param
                </button>
            </div>

            <!-- Query Params -->
            <div class="mt-3">
                <h6>Query Params</h6>
                <div class="params-container" id="queryParams-${index}"></div>
                <button type="button" class="btn btn-outline-secondary btn-sm add-query" data-index="${index}">
                    Add Query Param
                </button>
            </div>

            <!-- Static Headers -->
            <div class="mt-3">
                <h6>Static Headers</h6>
                <div class="headers-container" id="staticHeaders-${index}"></div>
                <button type="button" class="btn btn-outline-secondary btn-sm add-static" data-index="${index}">
                    Add Static Header
                </button>
            </div>

            <!-- Dynamic Headers -->
            <div class="mt-3">
                <h6>Dynamic Headers</h6>
                <div class="headers-container" id="dynamicHeaders-${index}"></div>
                <button type="button" class="btn btn-outline-secondary btn-sm add-dynamic" data-index="${index}">
                    Add Dynamic Header
                </button>
            </div>

            <!-- Request Body -->
            <div class="mt-3">
                <label class="form-label">Request Body Template</label>
                <textarea class="form-control" name="endpoints[${index}].requestBodyTemplate" rows="3">${data.requestBodyTemplate || ''}</textarea>
            </div>
        </div>
        `;

        container.insertAdjacentHTML("beforeend", template);
        const card = container.lastElementChild;

        // hydrate operationType + method
        if (data.operationType) {
            card.querySelector(`select[name="endpoints[${index}].operationType"]`).value = data.operationType;
        }
        if (data.method) {
            card.querySelector(`select[name="endpoints[${index}].method"]`).value = data.method;
        }

        // hydrate arrays
        (data.pathParamKeys || []).forEach((p, i2) => addParam(index, "path", i2, p));
        (data.queryParamKeys || []).forEach((p, i2) => addParam(index, "query", i2, p));

        (data.headers || []).forEach((h, i2) => addStaticHeader(index, i2, h));
        (data.dynamicHeaders || []).forEach((h, i2) => addDynamicHeader(index, i2, h));

        // bind remove
        card.querySelector(".remove-endpoint").onclick = () => card.remove();

        // bind add buttons
        card.querySelector(".add-path").onclick = () =>
            addParam(index, "path", card.querySelectorAll(`#pathParams-${index} .input-group`).length);

        card.querySelector(".add-query").onclick = () =>
            addParam(index, "query", card.querySelectorAll(`#queryParams-${index} .input-group`).length);

        card.querySelector(".add-static").onclick = () =>
            addStaticHeader(index, card.querySelectorAll(`#staticHeaders-${index} .input-group`).length);

        card.querySelector(".add-dynamic").onclick = () =>
            addDynamicHeader(index, card.querySelectorAll(`#dynamicHeaders-${index} .input-group`).length);
    }

    // --- Helpers ---
    function addParam(epIndex, type, i, value = "") {
        const containerId = type === "path" ? `pathParams-${epIndex}` : `queryParams-${epIndex}`;
        const container = document.getElementById(containerId);
        const template = `
            <div class="input-group mb-2">
                <input type="text" class="form-control"
                    name="endpoints[${epIndex}].${type}ParamKeys[${i}]" value="${value}">
                <button class="btn btn-outline-danger remove" type="button"><i class="bi bi-x"></i></button>
            </div>`;
        container.insertAdjacentHTML("beforeend", template);
        container.lastElementChild.querySelector(".remove").onclick = e => e.target.closest(".input-group").remove();
    }

    function addStaticHeader(epIndex, i, header = {}) {
        const container = document.getElementById(`staticHeaders-${epIndex}`);
        const template = `
            <div class="input-group mb-2">
                <input type="text" class="form-control" placeholder="Name"
                    name="endpoints[${epIndex}].headers[${i}].name" value="${header.name || ''}">
                <input type="text" class="form-control" placeholder="Value"
                    name="endpoints[${epIndex}].headers[${i}].value" value="${header.value || ''}">
                <div class="input-group-text">
                    <input class="form-check-input mt-0" type="checkbox"
                        name="endpoints[${epIndex}].headers[${i}].secret" ${header.secret ? "checked" : ""}>
                    <small class="ms-1">Secret</small>
                </div>
                <button class="btn btn-outline-danger remove" type="button"><i class="bi bi-x"></i></button>
            </div>`;
        container.insertAdjacentHTML("beforeend", template);
        container.lastElementChild.querySelector(".remove").onclick = e => e.target.closest(".input-group").remove();
    }

    function addDynamicHeader(epIndex, i, header = {}) {
        const container = document.getElementById(`dynamicHeaders-${epIndex}`);
        const template = `
            <div class="input-group mb-2">
                <input type="text" class="form-control" placeholder="Name"
                    name="endpoints[${epIndex}].dynamicHeaders[${i}].name" value="${header.name || ''}">
                <input type="text" class="form-control" placeholder="Description"
                    name="endpoints[${epIndex}].dynamicHeaders[${i}].description" value="${header.description || ''}">
                <div class="input-group-text">
                    <input class="form-check-input mt-0" type="checkbox"
                        name="endpoints[${epIndex}].dynamicHeaders[${i}].required" ${header.required ? "checked" : ""}>
                    <small class="ms-1">Req</small>
                </div>
                <button class="btn btn-outline-danger remove" type="button"><i class="bi bi-x"></i></button>
            </div>`;
        container.insertAdjacentHTML("beforeend", template);
        container.lastElementChild.querySelector(".remove").onclick = e => e.target.closest(".input-group").remove();
    }
});
