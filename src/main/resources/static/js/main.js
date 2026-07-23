/* PulseLink Client-Side Logic & Chart.js Integration */

document.addEventListener('DOMContentLoaded', function () {
    // Initialize common utilities
    initPasswordToggle();
    initCopyButtons();
    initNotificationRead();
    initCharts();
    initFaq();
});

// Password Visibility Toggle
function initPasswordToggle() {
    const toggles = document.querySelectorAll('.password-toggle');
    toggles.forEach(toggle => {
        toggle.addEventListener('click', function () {
            const input = this.previousElementSibling;
            if (input.type === 'password') {
                input.type = 'text';
                this.classList.remove('fa-eye');
                this.classList.add('fa-eye-slash');
            } else {
                input.type = 'password';
                this.classList.remove('fa-eye-slash');
                this.classList.add('fa-eye');
            }
        });
    });
}

// Copy Demo Credentials
function initCopyButtons() {
    const copyButtons = document.querySelectorAll('.copy-btn');
    copyButtons.forEach(btn => {
        btn.addEventListener('click', function () {
            const email = this.getAttribute('data-email');
            const password = this.getAttribute('data-password');
            const type = this.getAttribute('data-type');

            // Set clipboard
            const textToCopy = `Email: ${email}\nPassword: ${password}`;
            navigator.clipboard.writeText(textToCopy).then(() => {
                const originalText = this.innerHTML;
                this.innerHTML = '<i class="fa-solid fa-check"></i> Copied!';
                this.style.background = '#10B981';
                this.style.color = '#fff';
                
                setTimeout(() => {
                    this.innerHTML = originalText;
                    this.style.background = '';
                    this.style.color = '';
                }, 2000);
            });
        });
    });
}

// Notification AJAX Mark as Read
function initNotificationRead() {
    const notificationItems = document.querySelectorAll('.notification-item.unread');
    notificationItems.forEach(item => {
        const markBtn = item.querySelector('.mark-read-btn');
        if (markBtn) {
            markBtn.addEventListener('click', function (e) {
                e.stopPropagation();
                const id = this.getAttribute('data-id');
                
                fetch(`/api/notifications/mark-read/${id}`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    }
                })
                .then(res => res.json())
                .then(data => {
                    if (data.success) {
                        item.classList.remove('unread');
                        this.remove();
                        // Update badge counter
                        const badge = document.querySelector('.notification-badge');
                        if (badge) {
                            let count = parseInt(badge.textContent);
                            if (count > 1) {
                                badge.textContent = count - 1;
                            } else {
                                badge.remove();
                            }
                        }
                    }
                });
            });
        }
    });
}

// FAQ Accordion
function initFaq() {
    const faqQuestions = document.querySelectorAll('.faq-question');
    faqQuestions.forEach(q => {
        q.addEventListener('click', function() {
            const answer = this.nextElementSibling;
            const isOpen = answer.style.display === 'block';
            
            // Close all answers
            document.querySelectorAll('.faq-answer').forEach(a => a.style.display = 'none');
            
            // Open clicked
            answer.style.display = isOpen ? 'none' : 'block';
        });
    });
}

// Chart.js Configuration
function initCharts() {
    const bloodGroupCtx = document.getElementById('bloodGroupChart');
    const trendsCtx = document.getElementById('trendsChart');

    if (bloodGroupCtx) {
        fetch('/api/stats/blood-groups')
            .then(res => res.json())
            .then(data => {
                const labels = data.map(item => item.group);
                const values = data.map(item => item.units);

                new Chart(bloodGroupCtx, {
                    type: 'doughnut',
                    data: {
                        labels: labels,
                        datasets: [{
                            label: 'Blood Units Available',
                            data: values,
                            backgroundColor: [
                                '#EC4899', // A+
                                '#F472B6', // A-
                                '#F43F5E', // B+
                                '#FDA4AF', // B-
                                '#D946EF', // AB+
                                '#F5D0FE', // AB-
                                '#10B981', // O+
                                '#A7F3D0'  // O-
                            ],
                            borderWidth: 2,
                            borderColor: '#ffffff'
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: {
                                position: 'right',
                                labels: {
                                    font: { family: 'Poppins', size: 12 },
                                    color: document.body.classList.contains('dark') ? '#94A3B8' : '#1F2937'
                                }
                            }
                        }
                    }
                });
            });
    }

    if (trendsCtx) {
        fetch('/api/stats/trends')
            .then(res => res.json())
            .then(data => {
                new Chart(trendsCtx, {
                    type: 'line',
                    data: {
                        labels: data.labels,
                        datasets: [
                            {
                                label: 'Donations (Units)',
                                data: data.donations,
                                borderColor: '#EC4899',
                                backgroundColor: 'rgba(236, 72, 153, 0.1)',
                                fill: true,
                                tension: 0.4,
                                borderWidth: 3
                            },
                            {
                                label: 'Requests (Units)',
                                data: data.requests,
                                borderColor: '#3B82F6',
                                backgroundColor: 'rgba(59, 130, 246, 0.1)',
                                fill: true,
                                tension: 0.4,
                                borderWidth: 3
                            }
                        ]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: {
                                position: 'top',
                                labels: { font: { family: 'Poppins', size: 12 } }
                            }
                        },
                        scales: {
                            y: {
                                beginAtZero: true,
                                grid: { color: 'rgba(0, 0, 0, 0.05)' }
                            },
                            x: {
                                grid: { display: false }
                            }
                        }
                    }
                });
            });
    }
}
