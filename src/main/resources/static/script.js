// Application State
let currentPage = 'home';
let userType = null;
let userEmail = '';
let complaints = [];

// Department email mapping
const departmentEmails = {
    'facilities': 'facilities@university.edu',
    'food-services': 'dining@university.edu',
    'academic': 'academic@university.edu',
    'technology': 'it@university.edu',
    'transportation': 'transport@university.edu',
    'other': 'general@university.edu'
};

// API helper with session credentials
async function apiFetch(url, options = {}) {
    const { method = 'GET', headers = {}, body = undefined, form = false } = options;
    const opts = { method, headers: { ...headers }, credentials: 'include' };
    if (body !== undefined) {
        if (form) {
            const params = new URLSearchParams();
            Object.entries(body).forEach(([k, v]) => params.append(k, v == null ? '' : String(v)));
            opts.body = params.toString();
            opts.headers['Content-Type'] = 'application/x-www-form-urlencoded;charset=UTF-8';
        } else {
            opts.body = JSON.stringify(body);
            opts.headers['Content-Type'] = 'application/json';
        }
    }
    const resp = await fetch(url, opts);
    const text = await resp.text();
    let data = {};
    try { if (text) data = JSON.parse(text); } catch (e) { data = { raw: text }; }
    if (!resp.ok) {
        const msg = (data && (data.error || data.message)) || resp.statusText || 'Request failed';
        throw new Error(msg);
    }
    return data;
}

// Initialize the application
document.addEventListener('DOMContentLoaded', function() {
    // Initialize Lucide icons
    lucide.createIcons();
    
    // Set up navigation event listeners
    setupNavigation();
    
    // Set up form event listeners
    setupForms();
    
    // Show home page by default
    showPage('home');
    
    // Update header
    updateHeader();
});

// Navigation
function setupNavigation() {
    // Navigation buttons
    document.querySelectorAll('.nav-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const page = this.getAttribute('data-page');
            navigateTo(page);
        });
    });
}

function navigateTo(page) {
    currentPage = page;
    showPage(page);
    updateHeader();
    
    // Hide support button on non-home pages
    const supportBtn = document.getElementById('support-btn');
    if (supportBtn) {
        supportBtn.style.display = page === 'home' ? 'flex' : 'none';
    }
}

function showPage(page) {
    // Hide all pages
    document.querySelectorAll('.page').forEach(p => {
        p.style.display = 'none';
    });
    
    // Show current page
    const pageElement = document.getElementById(page + '-page');
    if (pageElement) {
        pageElement.style.display = 'block';
    }
    
    // Special handling for dashboard
    if (page === 'dashboard') {
        if (userType === 'student') {
            document.getElementById('student-dashboard').style.display = 'block';
            populateStudentDashboard();
        } else if (userType === 'admin') {
            document.getElementById('admin-dashboard').style.display = 'block';
            populateAdminDashboard();
        } else if (userType === 'department') {
            // Redirect departments to server-rendered dashboard
            window.location.href = '/dept/dashboard';
            return;
        } else {
            // Not logged in, redirect to login
            navigateTo('login');
            return;
        }
    }
}

function updateHeader() {
    // Update navigation active states
    document.querySelectorAll('.nav-btn').forEach(btn => {
        const page = btn.getAttribute('data-page');
        if (page === currentPage) {
            btn.classList.add('active');
        } else {
            btn.classList.remove('active');
        }
    });
    
    // Update user info display
    const userInfo = document.querySelector('.user-info');
    const loginBtn = document.querySelector('.login-btn');
    const dashboardBtn = document.querySelector('.dashboard-btn');
    const userTypeText = document.querySelector('.user-type-text');
    const userIcon = document.querySelector('.user-icon');
    
    if (userType) {
        userInfo.style.display = 'block';
        loginBtn.style.display = 'none';
        dashboardBtn.style.display = 'block';
        userTypeText.textContent = userType;
        
        // Update icon based on user type
        if (userIcon) {
            userIcon.setAttribute('data-lucide', userType === 'admin' ? 'shield' : 'user');
            lucide.createIcons();
        }
    } else {
        userInfo.style.display = 'none';
        loginBtn.style.display = 'block';
        dashboardBtn.style.display = 'none';
    }
}

// Authentication
function setupForms() {
    // Login form
    const loginForm = document.getElementById('login-form');
    if (loginForm) {
        loginForm.addEventListener('submit', handleLogin);
    }
    
    // Login type buttons
    document.querySelectorAll('.login-type-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const type = this.getAttribute('data-type');
            setLoginType(type);
        });
    });
    
    // Contact form
    const contactForm = document.getElementById('contact-form');
    if (contactForm) {
        contactForm.addEventListener('submit', handleContactForm);
    }
    
    // Support form
    const supportForm = document.getElementById('support-form');
    if (supportForm) {
        supportForm.addEventListener('submit', handleSupportForm);
    }
    
    // Complaint form
    const complaintForm = document.getElementById('complaint-form');
    if (complaintForm) {
        complaintForm.addEventListener('submit', handleComplaintSubmission);
    }
}

function setLoginType(type) {
  try {
    const buttons = document.querySelectorAll('.login-type-btn');
    buttons.forEach(btn => {
      const btnType = btn.getAttribute('data-type');
      if (btnType === type) {
        btn.classList.remove('btn-outline');
        btn.classList.add('active');
      } else {
        btn.classList.add('btn-outline');
        btn.classList.remove('active');
      }
    });

    const loginIcon = document.querySelector('.login-icon');
    const loginTitle = document.querySelector('.login-title');
    const loginDescription = document.querySelector('.login-description');
    const loginSubmitText = document.querySelector('.login-submit-text');
    const emailInput = document.getElementById('login-email');

    if (type === 'admin') {
      if (loginIcon) loginIcon.setAttribute('data-lucide', 'shield');
      if (loginTitle) loginTitle.textContent = 'Administrator Login';
      if (loginDescription) loginDescription.textContent = 'Access administrative controls and manage system settings.';
      if (loginSubmitText) loginSubmitText.textContent = 'Sign In as Admin';
      if (emailInput) emailInput.placeholder = 'admin@system.com';
    } else if (type === 'department') {
      if (loginIcon) loginIcon.setAttribute('data-lucide', 'building-2');
      if (loginTitle) loginTitle.textContent = 'Department Login';
      if (loginDescription) loginDescription.textContent = 'Manage and respond to student complaints for your department.';
      if (loginSubmitText) loginSubmitText.textContent = 'Sign In as Department';
      if (emailInput) emailInput.placeholder = 'it@university.edu';
    } else {
      if (loginIcon) loginIcon.setAttribute('data-lucide', 'user');
      if (loginTitle) loginTitle.textContent = 'Student Login';
      if (loginDescription) loginDescription.textContent = 'Sign in to file complaints and track their status.';
      if (loginSubmitText) loginSubmitText.textContent = 'Sign In as Student';
      if (emailInput) emailInput.placeholder = 'student@srmist.edu.in';
    }

    try { if (window.lucide && typeof window.lucide.createIcons === 'function') window.lucide.createIcons(); } catch (e) {}
  } catch (err) {
    console.error('setLoginType error:', err);
  }
}

async function handleLogin(e) {
  e.preventDefault();
  try {
    const activeBtn = document.querySelector('.login-type-btn.active');
    const selectedType = activeBtn ? (activeBtn.getAttribute('data-type') || 'student') : 'student';
    const email = document.getElementById('login-email')?.value?.trim() || '';
    const password = document.getElementById('login-password')?.value || '';

    const body = { username: email, email, password, role: selectedType.toUpperCase() };
    const res = await fetch('/auth/api/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(body)
    });
    const resp = await res.json();

    if (!res.ok || !resp || !resp.success) {
      alert(resp?.error || resp?.message || 'Login failed. Please check your credentials.');
      return;
    }

    const actualRole = (resp.user?.role || selectedType).toString().toUpperCase();
    window.userType = actualRole.toLowerCase();
    window.userEmail = resp.user?.email || email;

    // Department: go to server-rendered dashboard (no SPA fallback)
    if (actualRole === 'DEPARTMENT') {
      window.location.assign('/dept/dashboard');
      return;
    }

    // Admin: go to server-rendered dashboard (no SPA fallback)
    if (actualRole === 'ADMIN') {
      window.location.assign('/admin/dashboard');
      return;
    }

    // Student: SPA dashboard
    navigateTo('dashboard');
  } catch (err) {
    console.error('Login error:', err);
    alert('An unexpected error occurred during login.');
  }
}

async function logout() {
    try { await fetch('/auth/logout', { credentials: 'include' }); } catch {}
    userType = null;
    userEmail = '';
    const prev = document.referrer;
    if (prev && prev.startsWith(window.location.origin)) {
        window.location.href = prev;
    } else {
        window.location.href = '/index.html';
    }
}

// Student Dashboard
async function populateStudentDashboard() {
    const username = userEmail.split('@')[0];
    const usernameEl = document.getElementById('student-username');
    if (usernameEl) usernameEl.textContent = username || '';
    
    try {
        const data = await apiFetch('/student/api/complaints');
        complaints = Array.isArray(data) ? data : (Array.isArray(data.complaints) ? data.complaints : []);
    } catch (err) {
        if (/Not authenticated/i.test(err.message)) {
            navigateTo('login');
            return;
        }
        complaints = [];
    }
    
    const container = document.getElementById('student-complaints-container');
    const userComplaints = complaints;
    if (!userComplaints || userComplaints.length === 0) {
        container.innerHTML = `
            <div class="card">
                <div class="card-content flex flex-col items-center justify-center py-12">
                    <i data-lucide="file-text" class="h-12 w-12 text-muted-foreground mb-4"></i>
                    <h3>No complaints submitted yet</h3>
                    <p class="text-muted-foreground text-center mb-4">
                        Click "Submit New Complaint" to submit your first complaint.
                    </p>
                </div>
            </div>
        `;
    } else {
        container.innerHTML = `
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Previous Complaints</h3>
                    <p class="card-description">
                        Track the status of your submitted complaints. All complaints are read-only after submission.
                    </p>
                </div>
                <div class="card-content">
                    <table class="table">
                        <thead>
                            <tr>
                                <th>Title</th>
                                <th>Category</th>
                                <th>Description</th>
                                <th>Status</th>
                                <th>Created At</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${userComplaints.map(complaint => `
                                <tr>
                                    <td>
                                        <div class="font-medium">${complaint.title}</div>
                                    </td>
                                    <td>
                                        <span class="badge badge-outline capitalize">
                                            ${String(complaint.category || '').replace('-', ' ')}
                                        </span>
                                    </td>
                                    <td>
                                        <div class="max-w-xs">
                                            <p class="text-sm text-muted-foreground line-clamp-2">
                                                ${complaint.description || ''}
                                            </p>
                                        </div>
                                    </td>
                                    <td>
                                        <span class="badge ${getStatusClass(complaint.status)} flex items-center space-x-1">
                                            ${getStatusIcon(complaint.status)}
                                            <span>${getStatusText(complaint.status)}</span>
                                        </span>
                                    </td>
                                    <td>
                                        <div class="text-sm">
                                            ${complaint.submittedAt ? new Date(complaint.submittedAt).toLocaleDateString() : ''}
                                        </div>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                    
                    ${userComplaints.filter(c => c.response).length > 0 ? `
                        <div class="mt-6 space-y-4">
                            ${userComplaints.filter(c => c.response).map(complaint => `
                                <div class="bg-muted/50 p-4 rounded-lg">
                                    <div class="flex items-center space-x-2 mb-2">
                                        <i data-lucide="message-square" class="h-4 w-4 text-primary"></i>
                                        <span class="font-medium">Response for: ${complaint.title}</span>
                                    </div>
                                    <p class="text-sm">${complaint.response}</p>
                                </div>
                            `).join('')}
                        </div>
                    ` : ''}
                </div>
            </div>
        `;
    }
    
    lucide.createIcons();
}

function toggleComplaintForm() {
    const formCard = document.getElementById('complaint-form-card');
    const isHidden = formCard.style.display === 'none';
    formCard.style.display = isHidden ? 'block' : 'none';
    
    if (!isHidden) {
        // Reset form when hiding
        document.getElementById('complaint-form').reset();
    }
}

function handleComplaintSubmission(e) {
    e.preventDefault();
    const formData = new FormData(e.target);

    const payload = {
        title: formData.get('title'),
        category: formData.get('category') || 'other',
        description: formData.get('description')
    };

    (async () => {
        try {
            await apiFetch('/student/api/complaints', { method: 'POST', body: payload });
            // Hide form and refresh dashboard
            toggleComplaintForm();
            await populateStudentDashboard();
            alert('Complaint submitted successfully!');
            e.target.reset();
        } catch (err) {
            alert(`Failed to submit complaint: ${err.message}`);
        }
    })();
}

// Admin Dashboard
// Application State
// Charts instances (prevent ReferenceError)
let adminChart = null;
let adminStatusChart = null;
let adminResolutionChart = null;
// Duplicate global declarations removed to prevent redeclaration errors.

// Track which unattended complaints have been shown to admin (persisted in localStorage)
let adminUnattendedSeen = new Set(JSON.parse(localStorage.getItem('admin_unattended_seen') || '[]'));
function saveAdminUnattendedSeen() {
    localStorage.setItem('admin_unattended_seen', JSON.stringify(Array.from(adminUnattendedSeen)));
}

async function populateAdminDashboard() {
    const username = userEmail.split('@')[0];
    const adminUserEl = document.getElementById('admin-username');
    if (adminUserEl) adminUserEl.textContent = username || '';

    try {
        const data = await apiFetch('/admin/api/complaints');
        complaints = Array.isArray(data) ? data : (Array.isArray(data.complaints) ? data.complaints : []);
    } catch (err) {
        if (/Not authenticated/i.test(err.message)) {
            navigateTo('login');
            return;
        }
        complaints = [];
    }

    // Calculate stats
    // Update Admin stats to include tri-state 'in-progress'
    const stats = {
        total: complaints.length,
        pending: complaints.filter(c => c.status === 'pending').length,
        inProgress: complaints.filter(c => c.status === 'in-progress' || c.status === 'sent-to-dept' || c.status === 'dept-confirmed').length,
        resolved: complaints.filter(c => c.status === 'resolved').length
    };

    // Populate stats cards
    const statsContainer = document.getElementById('admin-stats');
    statsContainer.innerHTML = `
        <div class="stats-card">
            <div class="flex items-center space-x-4">
                <div class="stats-icon total">
                    <i data-lucide="file-text" class="h-6 w-6"></i>
                </div>
                <div>
                    <p class="text-sm text-muted-foreground">Total Complaints</p>
                    <p class="text-2xl font-bold">${stats.total}</p>
                </div>
            </div>
        </div>
        <div class="stats-card">
            <div class="flex items-center space-x-4">
                <div class="stats-icon pending">
                    <i data-lucide="clock" class="h-6 w-6"></i>
                </div>
                <div>
                    <p class="text-sm text-muted-foreground">Pending</p>
                    <p class="text-2xl font-bold">${stats.pending}</p>
                </div>
            </div>
        </div>
        <div class="stats-card">
            <div class="flex items-center space-x-4">
                <div class="stats-icon progress">
                    <i data-lucide="trending-up" class="h-6 w-6"></i>
                </div>
                <div>
                    <p class="text-sm text-muted-foreground">In Progress</p>
                    <p class="text-2xl font-bold">${stats.inProgress}</p>
                </div>
            </div>
        </div>
        <div class="stats-card">
            <div class="flex items-center space-x-4">
                <div class="stats-icon resolved">
                    <i data-lucide="check-circle" class="h-6 w-6"></i>
                </div>
                <div>
                    <p class="text-sm text-muted-foreground">Resolved</p>
                    <p class="text-2xl font-bold">${stats.resolved}</p>
                </div>
            </div>
        </div>
    `;

    // Detect unattended complaints (pending + departmentStatus Pending)
    const unattended = complaints.filter(c => c.status === 'pending' && ((c.departmentStatus || 'Pending') === 'Pending'));
    const unseen = unattended.filter(c => !adminUnattendedSeen.has(String(c.id)));
    if (unseen.length > 0) {
        openUnattendedPopup(unseen);
        unseen.forEach(c => adminUnattendedSeen.add(String(c.id)));
        saveAdminUnattendedSeen();
    }

    // Populate complaints list (read-only for admin)
    const complaintsContainer = document.getElementById('admin-complaints-container');
    complaintsContainer.innerHTML = `
        <div class="space-y-4">
            ${complaints.map(complaint => {
                const isUnattended = complaint.status === 'pending' && ((complaint.departmentStatus || 'Pending') === 'Pending');
                return `
                <div class="flex items-center justify-between p-4 border rounded-lg">
                    <div class="flex-1">
                        <div class="flex items-center space-x-4 mb-2">
                            <h4 class="font-medium">${complaint.title}</h4>
                            <span class="badge badge-outline capitalize">
                                ${String(complaint.category || '').replace('-', ' ')}
                            </span>
                            <span class="badge ${getStatusClass(complaint.status)} flex items-center space-x-1">
                                ${getStatusIcon(complaint.status)}
                                <span>${getStatusText(complaint.status)}</span>
                            </span>
                            ${isUnattended ? '<span class="badge status-pending">Unattended</span>' : ''}
                        </div>
                        <p class="text-sm text-muted-foreground mb-1">
                            From: ${complaint.studentEmail || ''} • Submitted: ${complaint.submittedAt ? new Date(complaint.submittedAt).toLocaleDateString() : ''}
                        </p>
                        <p class="text-sm text-muted-foreground line-clamp-2">${complaint.description || ''}</p>
                    </div>
                    <div class="flex items-center gap-2">
                        ${isUnattended ? `
                        <button class="btn btn-outline btn-sm" onclick="pushComplaint('${complaint.id}')">
                            <i data-lucide="send" class="h-4 w-4 mr-2"></i>
                            Push
                        </button>
                        ` : ''}
                        <button class="btn btn-outline btn-sm" onclick="openComplaintModal('${complaint.id}')">
                            <i data-lucide="eye" class="h-4 w-4 mr-2"></i>
                            View
                        </button>
                    </div>
                </div>`;
            }).join('')}
        </div>
    `;

    // Set up analytics chart controls and initial render
    setupAdminAnalyticsControls();
    // Render new charts
    renderAdminStatusChart();
    setupResolutionTrendControls();
    
    // Refresh icons
    lucide.createIcons();
}

// Status helpers
function getStatusClass(status) {
    const classes = {
        'pending': 'status-pending',
        'in-progress': 'status-in-progress',
        'sent-to-dept': 'status-sent-to-dept',
        'dept-confirmed': 'status-dept-confirmed',
        'resolved': 'status-resolved'
    };
    return classes[status] || 'status-pending';
}

function getStatusIcon(status) {
    const icons = {
        'pending': '<i data-lucide="clock" class="h-4 w-4"></i>',
        'in-progress': '<i data-lucide="trending-up" class="h-4 w-4"></i>',
        'sent-to-dept': '<i data-lucide="send" class="h-4 w-4"></i>',
        'dept-confirmed': '<i data-lucide="alert-circle" class="h-4 w-4"></i>',
        'resolved': '<i data-lucide="check-circle" class="h-4 w-4"></i>'
    };
    return icons[status] || '<i data-lucide="clock" class="h-4 w-4"></i>';
}

function getStatusText(status) {
    const texts = {
        'pending': 'Pending',
        'in-progress': 'In Progress',
        'sent-to-dept': 'Sent to Dept',
        'dept-confirmed': 'Department Confirmed',
        'resolved': 'Resolved'
    };
    return texts[status] || 'Unknown';
}

// Complaint management modal
function openComplaintModal(complaintId) {
    const complaint = complaints.find(c => String(c.id) === String(complaintId));
    if (!complaint) return;
    
    const modal = document.getElementById('complaint-modal');
    const title = document.getElementById('modal-complaint-title');
    const content = document.getElementById('modal-complaint-content');
    
    // Show modal immediately
    modal.style.display = 'flex';

    // Safe date formatting
    const d = complaint.submittedAt ? new Date(complaint.submittedAt) : null;
    const submittedStr = (d && !isNaN(d)) ? d.toLocaleDateString() : 'N/A';
    const isUnattended = complaint.status === 'pending' && ((complaint.departmentStatus || 'Pending') === 'Pending');
    
    title.textContent = complaint.title;
    content.innerHTML = `
        <div class="space-y-4">
            <div>
                <p class="text-sm text-muted-foreground mb-2">
                    Submitted by ${complaint.studentEmail || ''} on ${submittedStr}
                </p>
            </div>
            <div>
                <label class="block mb-2">Description</label>
                <p class="text-sm bg-muted/50 p-3 rounded-lg">${complaint.description}</p>
            </div>
            <div class="space-y-2">
                <label>Status</label>
                <p class="text-sm bg-muted/50 p-3 rounded-lg">${getStatusText(complaint.status)}</p>
            </div>
            <div class="space-y-2">
                <label>Department Status</label>
                <p class="text-sm bg-muted/50 p-3 rounded-lg">${complaint.departmentStatus || 'Pending'}</p>
            </div>
            <div class="space-y-2">
                <label>Department Remarks</label>
                <p class="text-sm bg-muted/50 p-3 rounded-lg">${complaint.departmentRemarks || ''}</p>
            </div>
            <div class="flex justify-end space-x-2">
                <button class="btn btn-outline" onclick="closeComplaintModal()">
                    Close
                </button>
                ${isUnattended ? `
                <button class="btn" onclick="pushComplaint('${complaint.id}')">
                    <i data-lucide="send" class="h-4 w-4 mr-2"></i>
                    Push to Department
                </button>
                ` : ''}
            </div>
        </div>
    `;
    
    modal.style.display = 'flex';
    
    // Refresh icons
    lucide.createIcons();
}

function closeComplaintModal() {
    document.getElementById('complaint-modal').style.display = 'none';
}

async function updateComplaint(complaintId) {
    // Admin is read-only; prevent updates
    alert('Admins cannot modify complaint status.');
    closeComplaintModal();
}

// Support popup
function openSupportPopup() {
    document.getElementById('support-popup').style.display = 'flex';
}

function closeSupportPopup() {
    document.getElementById('support-popup').style.display = 'none';
    document.getElementById('support-form').reset();
}

function handleSupportForm(e) {
    e.preventDefault();
    alert('Thank you for your message! Our support team will get back to you within 24 hours.');
    closeSupportPopup();
}

function handleContactForm(e) {
    e.preventDefault();
    alert('Thank you for your message! We will get back to you soon.');
    e.target.reset();
}

// Close modals when clicking outside
document.addEventListener('click', function(e) {
    if (e.target.classList.contains('modal')) {
        if (e.target.id === 'support-popup') {
            closeSupportPopup();
        } else if (e.target.id === 'complaint-modal') {
            closeComplaintModal();
        }
    }
});

// Escape key to close modals
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        closeSupportPopup();
        closeComplaintModal();
    }
});

// Admin Analytics (Category/Status Chart)
function setupAdminAnalyticsControls() {
    const typeEl = document.getElementById('admin-chart-type');
    const categoryEl = document.getElementById('admin-category-select');
    const canvas = document.getElementById('admin-category-chart');
    if (!typeEl || !categoryEl || !canvas) return;

    // Build category list from complaints
    const categories = Array.from(new Set(complaints.map(c => c.category || 'other'))).sort();

    // Preserve previously selected value if exists
    const prev = categoryEl.value;

    // Populate options (All + categories)
    categoryEl.innerHTML = '';
    const allOpt = document.createElement('option');
    allOpt.value = 'All';
    allOpt.textContent = 'All';
    categoryEl.appendChild(allOpt);
    categories.forEach(cat => {
        const opt = document.createElement('option');
        opt.value = cat;
        opt.textContent = formatCategoryLabel(cat);
        categoryEl.appendChild(opt);
    });
    // Restore previous selection if still valid, else default to All
    if (prev && (prev === 'All' || categories.includes(prev))) {
        categoryEl.value = prev;
    } else {
        categoryEl.value = 'All';
    }

    // Attach change listeners once per render
    typeEl.onchange = renderAdminCategoryChart;
    categoryEl.onchange = renderAdminCategoryChart;

    // Initial render
    renderAdminCategoryChart();
}

function renderAdminCategoryChart() {
    const typeEl = document.getElementById('admin-chart-type');
    const categoryEl = document.getElementById('admin-category-select');
    const canvas = document.getElementById('admin-category-chart');
    if (!typeEl || !categoryEl || !canvas) return;

    const chartType = typeEl.value || 'bar';
    const selectedCategory = categoryEl.value || 'All';

    // Destroy previous chart instance if any
    if (adminChart) {
        try { adminChart.destroy(); } catch (_) {}
        adminChart = null;
    }

    const ctx = canvas.getContext('2d');

    let labels = [];
    let data = [];

    if (selectedCategory === 'All') {
        // Category distribution across all complaints
        const counts = new Map();
        complaints.forEach(c => {
            const cat = c.category || 'other';
            counts.set(cat, (counts.get(cat) || 0) + 1);
        });
        labels = Array.from(counts.keys()).map(formatCategoryLabel);
        data = Array.from(counts.values());
    } else {
        // Tri-state status distribution within selected category
        const statuses = ['pending', 'in-progress', 'resolved'];
        const counts = { 'pending': 0, 'in-progress': 0, 'resolved': 0 };
        complaints.filter(c => (c.category || 'other') === selectedCategory).forEach(c => {
            const s = (c.status === 'sent-to-dept' || c.status === 'dept-confirmed') ? 'in-progress' : (c.status || 'pending');
            if (counts[s] === undefined) counts[s] = 0;
            counts[s] += 1;
        });
        labels = statuses.map(getStatusText);
        data = statuses.map(s => counts[s] || 0);
    }

    const colors = getChartColors(Math.max(data.length, 1));

    adminChart = new Chart(ctx, {
        type: chartType,
        data: {
            labels,
            datasets: [{
                label: selectedCategory === 'All' ? 'Complaints by Category' : `Status in ${formatCategoryLabel(selectedCategory)}`,
                data,
                backgroundColor: chartType === 'bar' ? colors.map(c => c + 'CC') : colors,
                borderColor: colors,
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: chartType !== 'bar' },
                tooltip: { enabled: true }
            },
            scales: chartType === 'bar' ? {
                y: { beginAtZero: true, ticks: { precision: 0 } },
                x: { ticks: { autoSkip: false } }
            } : {}
        }
    });
}

function getChartColors(n) {
    const style = getComputedStyle(document.documentElement);
    const palette = [
        style.getPropertyValue('--chart-1').trim() || '#7C3AED',
        style.getPropertyValue('--chart-2').trim() || '#22D3EE',
        style.getPropertyValue('--chart-3').trim() || '#10B981',
        style.getPropertyValue('--chart-4').trim() || '#F59E0B',
        style.getPropertyValue('--chart-5').trim() || '#EF4444'
    ];
    const colors = [];
    for (let i = 0; i < n; i++) colors.push(palette[i % palette.length]);
    return colors;
}

function formatCategoryLabel(cat) {
    return String(cat || 'other').replace('-', ' ').replace(/\b\w/g, c => c.toUpperCase());
}

function renderAdminStatusChart() {
    const canvas = document.getElementById('admin-status-chart');
    if (!canvas) return;

    // Destroy previous chart instance if any
    if (adminStatusChart) {
        try { adminStatusChart.destroy(); } catch (_) {}
        adminStatusChart = null;
    }

    const ctx = canvas.getContext('2d');
    const statuses = ['pending', 'in-progress', 'resolved'];
    const counts = { 'pending': 0, 'in-progress': 0, 'resolved': 0 };

    complaints.forEach(c => {
        const s = (c.status === 'sent-to-dept' || c.status === 'dept-confirmed') ? 'in-progress' : (c.status || 'pending');
        if (counts[s] === undefined) counts[s] = 0;
        counts[s] += 1;
    });

    const labels = statuses.map(getStatusText);
    const data = statuses.map(s => counts[s] || 0);
    const colors = getChartColors(Math.max(data.length, 1));

    adminStatusChart = new Chart(ctx, {
        type: 'pie',
        data: {
            labels,
            datasets: [{
                label: 'Complaint Status Distribution',
                data,
                backgroundColor: colors,
                borderColor: colors.map(c => c),
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: true },
                tooltip: { enabled: true }
            }
        }
    });
}

function setupResolutionTrendControls() {
    const periodEl = document.getElementById('admin-period-select');
    const canvas = document.getElementById('admin-resolution-chart');
    if (!periodEl || !canvas) return;

    // Attach change listener
    periodEl.onchange = renderAdminResolutionChart;

    // Initial render
    renderAdminResolutionChart();
}

function renderAdminResolutionChart() {
    const periodEl = document.getElementById('admin-period-select');
    const canvas = document.getElementById('admin-resolution-chart');
    if (!periodEl || !canvas) return;

    const days = parseInt(periodEl.value || '30', 10);

    // Destroy previous chart instance if any
    if (adminResolutionChart) {
        try { adminResolutionChart.destroy(); } catch (_) {}
        adminResolutionChart = null;
    }

    const end = new Date(); // today
    end.setHours(0,0,0,0);
    const start = new Date(end);
    start.setDate(end.getDate() - (days - 1));

    // Build date buckets
    const buckets = new Map();
    const labels = [];
    const fmt = (d) => {
        // Format as DD Mon
        const opts = { month: 'short', day: 'numeric' };
        return d.toLocaleDateString(undefined, opts);
    };
    const cursor = new Date(start);
    while (cursor <= end) {
        const key = cursor.toDateString();
        buckets.set(key, 0);
        labels.push(fmt(cursor));
        cursor.setDate(cursor.getDate() + 1);
    }

    // Count resolved complaints per day
    complaints.forEach(c => {
        const ra = c.resolvedAt ? new Date(c.resolvedAt) : null;
        if (!ra || isNaN(ra)) return;
        ra.setHours(0,0,0,0);
        if (ra < start || ra > end) return;
        const key = ra.toDateString();
        buckets.set(key, (buckets.get(key) || 0) + 1);
    });

    const data = labels.map((_, idx) => {
        const d = new Date(start);
        d.setDate(start.getDate() + idx);
        const key = d.toDateString();
        return buckets.get(key) || 0;
    });

    const ctx = canvas.getContext('2d');
    const colors = getChartColors(1);

    adminResolutionChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels,
            datasets: [{
                label: 'Resolved per Day',
                data,
                backgroundColor: colors[0] + '33',
                borderColor: colors[0],
                borderWidth: 2,
                tension: 0.2,
                fill: true,
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: true },
                tooltip: { enabled: true }
            },
            scales: {
                y: { beginAtZero: true, ticks: { precision: 0 } }
            }
        }
    });
}

function openUnattendedPopup(items) {
  const modal = document.getElementById('unattended-popup');
  const listEl = document.getElementById('unattended-list');
  if (!modal || !listEl) return;
  if (!Array.isArray(items)) items = [];
  listEl.innerHTML = items.length === 0
    ? '<p class="text-sm text-muted-foreground">No unattended complaints.</p>'
    : items.map(c => `
      <div class="flex items-center justify-between p-3 border rounded-lg mb-2">
        <div class="flex-1">
          <div class="flex items-center gap-3 mb-1">
            <h4 class="font-medium">${c.title || ''}</h4>
            <span class="badge badge-outline capitalize">${formatCategoryLabel(c.category || '')}</span>
            <span class="badge ${getStatusClass(c.status)} flex items-center gap-1">
              ${getStatusIcon(c.status)}
              <span>${getStatusText(c.status)}</span>
            </span>
            <span class="badge status-pending">Dept: ${(c.departmentStatus || 'Pending')}</span>
          </div>
          <p class="text-sm text-muted-foreground">From: ${c.studentEmail || ''}</p>
        </div>
        <div class="flex items-center gap-2">
          <button class="btn btn-outline btn-sm" onclick="openComplaintModal('${c.id}')">
            <i data-lucide="eye" class="h-4 w-4 mr-2"></i>
            View
          </button>
          <button class="btn btn-sm" onclick="pushComplaint('${c.id}')">
            <i data-lucide="send" class="h-4 w-4 mr-2"></i>
            Push
          </button>
        </div>
      </div>
    `).join('');
  modal.style.display = 'block';
  // refresh icons inside the popup
  if (typeof lucide !== 'undefined' && lucide.createIcons) {
    lucide.createIcons();
  }
}

function closeUnattendedPopup() {
  const modal = document.getElementById('unattended-popup');
  if (modal) modal.style.display = 'none';
}

async function pushComplaint(id) {
  try {
    await apiFetch(`/admin/api/push/${id}`, { method: 'POST' });
    // mark as seen and persist
    if (typeof adminUnattendedSeen !== 'undefined') {
      adminUnattendedSeen.add(String(id));
      saveAdminUnattendedSeen();
    }
    // provide feedback and refresh dashboard
    alert('Complaint pushed to department successfully');
    closeUnattendedPopup();
    // re-populate to reflect latest data
    if (typeof populateAdminDashboard === 'function') {
      await populateAdminDashboard();
    }
  } catch (err) {
    alert(`Failed to push complaint: ${err.message}`);
  }
}

// Add role-based filtering for complaints and UI rendering
function getCurrentUser() {
  return apiFetch('/auth/api/me').then(u => u).catch(()=>null);
}

async function renderAdminDashboardSPA() {
  const me = await getCurrentUser();
  if (!me || me.role !== 'ADMIN') {
    window.location.href = '/auth/login';
    return;
  }
  // Show admin dashboard page and set user
  if (typeof showPage === 'function') {
    showPage('admin-dashboard');
  }
  const nameEl = document.getElementById('admin-username');
  if (nameEl) {
    nameEl.textContent = me.email || me.name || 'Admin';
  }
  // Populate admin dashboard (stats, tables, charts)
  if (typeof populateAdminDashboard === 'function') {
    await populateAdminDashboard();
  }
  // Optional: attach refresh button if present
  const refreshBtn = document.getElementById('refresh-admin');
  if (refreshBtn) {
    refreshBtn.addEventListener('click', async () => {
      if (typeof populateAdminDashboard === 'function') {
        await populateAdminDashboard();
      }
    });
  }
}
