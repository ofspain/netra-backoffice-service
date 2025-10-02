// --- Auth Configs dynamic fields ---

// Templates for each Auth Type
const AUTH_FIELDS = {
    ApiKeyAuth: (index, values = {}) => `
    <div class="row g-3">
      <div class="col-md-6">
        <label class="form-label">Key</label>
        <input type="text" class="form-control"
               name="security.authConfigs[${index}].config.key"
               value="${values.key || ''}" required>
      </div>
      <div class="col-md-6">
        <label class="form-label">Location</label>
        <select class="form-select"
                name="security.authConfigs[${index}].config.location">
          <option value="header" ${values.location === 'header' ? 'selected' : ''}>Header</option>
          <option value="query" ${values.location === 'query' ? 'selected' : ''}>Query</option>
        </select>
      </div>
    </div>`,

    BasicAuth: (index, values = {}) => `
    <div class="row g-3">
      <div class="col-md-6">
        <label class="form-label">Username</label>
        <input type="text" class="form-control"
               name="security.authConfigs[${index}].config.username"
               value="${values.username || ''}" required>
      </div>
      <div class="col-md-6">
        <label class="form-label">Password</label>
        <input type="password" class="form-control"
               name="security.authConfigs[${index}].config.password"
               value="${values.password || ''}" required>
      </div>
    </div>`,

    BearerTokenAuth: (index, values = {}) => `
    <div class="row g-3">
      <div class="col-md-12">
        <label class="form-label">Token</label>
        <input type="text" class="form-control"
               name="security.authConfigs[${index}].config.token"
               value="${values.token || ''}" required>
      </div>
    </div>`,

    MtlsAuth: (index, values = {}) => `
    <div class="row g-3">
      <div class="col-md-6">
        <label class="form-label">Cert Alias</label>
        <input type="text" class="form-control"
               name="security.authConfigs[${index}].config.certAlias"
               value="${values.certAlias || ''}" required>
      </div>
      <div class="col-md-6">
        <label class="form-label">Truststore Alias</label>
        <input type="text" class="form-control"
               name="security.authConfigs[${index}].config.truststoreAlias"
               value="${values.truststoreAlias || ''}">
      </div>
    </div>`,

    CustomSignatureAuth: (index, values = {}) => `
    <div class="row g-3">
      <div class="col-md-12">
        <label class="form-label">Algorithm</label>
        <input type="text" class="form-control"
               name="security.authConfigs[${index}].config.algorithm"
               value="${values.algorithm || ''}" required>
      </div>
      <div class="col-md-12">
        <label class="form-label">Secret</label>
        <input type="password" class="form-control"
               name="security.authConfigs[${index}].config.secret"
               value="${values.secret || ''}" required>
      </div>
      <div class="col-md-12">
        <label class="form-label">Parameters</label>
        <div class="kv-container" data-name="security.authConfigs[${index}].config.parameters">
          ${renderKeyValuePairs(`security.authConfigs[${index}].config.parameters`, values.parameters)}
          <button type="button" class="btn btn-sm btn-outline-primary add-kv mt-2">+ Add Param</button>
        </div>
      </div>
      <div class="col-md-12">
        <label class="form-label">Decorations</label>
        <div class="kv-container" data-name="security.authConfigs[${index}].config.decorations">
          ${renderKeyValuePairs(`security.authConfigs[${index}].config.decorations`, values.decorations)}
          <button type="button" class="btn btn-sm btn-outline-primary add-kv mt-2">+ Add Decoration</button>
        </div>
      </div>
    </div>`
};

// Helper: render key/value inputs for maps
function renderKeyValuePairs(baseName, obj = {}) {
    if (!obj) obj = {};
    return Object.entries(obj).map(([k, v], idx) => `
    <div class="input-group mb-2">
      <input type="text" class="form-control"
             name="${baseName}[${idx}].key" value="${k}" placeholder="Key">
      <input type="text" class="form-control"
             name="${baseName}[${idx}].value" value="${v}" placeholder="Value">
      <button type="button" class="btn btn-outline-danger remove-kv">&times;</button>
    </div>
  `).join('');
}

// Reindex kv pairs when adding/removing
function reindexKeyValuePairs(container) {
    const baseName = container.dataset.name;
    container.querySelectorAll('.input-group').forEach((row, idx) => {
        row.querySelectorAll('input').forEach(inp => {
            if (inp.name.includes('.key'))
                inp.name = `${baseName}[${idx}].key`;
            else
                inp.name = `${baseName}[${idx}].value`;
        });
    });
}

// --- Add new Auth Config ---
document.getElementById('addAuth').addEventListener('click', () => {
    const container = document.getElementById('auth-container');
    const index = container.querySelectorAll('.auth-card').length;
    const template = `
    <div class="card mb-3 shadow-sm p-3 auth-card">
      <div class="d-flex justify-content-between">
        <h6>Auth Config #${index + 1}</h6>
        <button type="button" class="btn btn-sm btn-outline-danger remove-auth">
          <i class="bi bi-trash"></i>
        </button>
      </div>
      <div class="row g-3 mt-1">
        <div class="col-md-6">
          <label class="form-label">Auth Type</label>
          <select class="form-select auth-type" name="security.authConfigs[${index}].authType" data-index="${index}">
            <option value="">-- select --</option>
            <option value="ApiKeyAuth">API Key</option>
            <option value="BasicAuth">Basic</option>
            <option value="BearerTokenAuth">Bearer Token</option>
            <option value="MtlsAuth">mTLS</option>
            <option value="CustomSignatureAuth">Custom Signature</option>
          </select>
        </div>
      </div>
      <div class="auth-fields mt-3" data-index="${index}"></div>
    </div>`;
    container.insertAdjacentHTML('beforeend', template);
    bindAuthEvents();
});

// --- Bind dynamic events ---
function bindAuthEvents() {
    // Remove auth config
    document.querySelectorAll('.remove-auth').forEach(btn => {
        btn.onclick = () => btn.closest('.auth-card').remove();
    });

    // Auth type change
    document.querySelectorAll('.auth-type').forEach(sel => {
        sel.onchange = () => {
            const index = sel.dataset.index;
            const wrapper = document.querySelector(`.auth-fields[data-index="${index}"]`);
            wrapper.innerHTML = AUTH_FIELDS[sel.value]
                ? AUTH_FIELDS[sel.value](index, {})
                : '';
            bindKVEvents();
        };
    });
}

// --- KV add/remove events ---
function bindKVEvents() {
    document.querySelectorAll('.add-kv').forEach(btn => {
        btn.onclick = () => {
            const container = btn.closest('.kv-container');
            const baseName = container.dataset.name;
            const idx = container.querySelectorAll('.input-group').length;
            const row = `
        <div class="input-group mb-2">
          <input type="text" class="form-control"
                 name="${baseName}[${idx}].key" placeholder="Key">
          <input type="text" class="form-control"
                 name="${baseName}[${idx}].value" placeholder="Value">
          <button type="button" class="btn btn-outline-danger remove-kv">&times;</button>
        </div>`;
            btn.insertAdjacentHTML('beforebegin', row);
            bindKVEvents();
        };
    });

    document.querySelectorAll('.remove-kv').forEach(btn => {
        btn.onclick = () => {
            const container = btn.closest('.kv-container');
            btn.closest('.input-group').remove();
            reindexKeyValuePairs(container);
        };
    });
}

// --- Hydrate existing configs (from server) ---
function hydrateExisting() {
    document.querySelectorAll('.auth-type').forEach(sel => {
        const cfg = sel.dataset.config ? JSON.parse(sel.dataset.config) : null;
        if (cfg) {
            const index = sel.dataset.index;
            const wrapper = document.querySelector(`.auth-fields[data-index="${index}"]`);
            const values = cfg.config || {};
            wrapper.innerHTML = AUTH_FIELDS[cfg.authType]
                ? AUTH_FIELDS[cfg.authType](index, values)
                : '';
        }
    });
    bindKVEvents();
}

document.addEventListener('DOMContentLoaded', () => {
    bindAuthEvents();
    hydrateExisting();
});
