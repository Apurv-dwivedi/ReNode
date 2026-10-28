// 1. Initialize Map
// NAYA: Default zoom control band kiya taaki usko bottom-right me shift kar sakein
const map = L.map('map', { zoomControl: false }).setView([29.8688, 77.8880], 9);

// NAYA: Zoom control ko bottom-right par set kiya (Mobile UI overlap rokne ke liye)
L.control.zoom({ position: 'bottomright' }).addTo(map);

L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap',
    maxZoom: 19,
    updateWhenIdle: true,
    updateWhenZooming: false,
    keepBuffer: 1
}).addTo(map);

// Map Layers
let markersGroup = L.layerGroup().addTo(map);
let matchLinesGroup = L.layerGroup().addTo(map);
let radiusCircle = null;

// LocalStorage se Active Company ID aur JWT Token uthao
let MY_COMPANY_ID = localStorage.getItem('companyId') ? parseInt(localStorage.getItem('companyId')) : null;
let JWT_TOKEN = localStorage.getItem('jwt_token');
const myLat = 29.8688;
const myLon = 77.8880;

// UI me Active Company Name Set Karo
document.addEventListener("DOMContentLoaded", () => {
    const activeCompanyName = localStorage.getItem('companyName');
    if (activeCompanyName && document.getElementById('activeCompanyName')) {
        document.getElementById('activeCompanyName').innerText = activeCompanyName;
        // Badge color green kar do active dikhane ke liye
        document.querySelector('#userProfileBadge div:last-child').style.background = '#10b981';
        document.querySelector('#userProfileBadge div:last-child').style.boxShadow = '0 0 10px #10b981';
    }
});

// Custom Neon Center Pin for Logged-in / Main Factory
const centerIcon = L.divIcon({
    className: 'custom-center-pin',
    html: '<div style="background: #f59e0b; width: 18px; height: 18px; border-radius: 50%; border: 3px solid #ffffff; box-shadow: 0 0 15px #f59e0b;"></div>',
    iconSize: [20, 20]
});

L.marker([myLat, myLon], { icon: centerIcon }).addTo(map).bindPopup("<b>Your Active Location</b>").openPopup();

// 2. Fetch Data (Nearby + Matches) - Updated with Auth Headers
async function fetchData(radius) {
    // Agar companyId ya token nahi hai, toh API call mat karo
    if (!MY_COMPANY_ID || !JWT_TOKEN) {
        document.getElementById('results').innerHTML = `<p style="color: #94a3b8; font-style: italic; padding: 10px;">Please login to view network matches.</p>`;
        return;
    }

    try {
        const nearbyRes = await fetch(`http://localhost:8080/api/companies/${MY_COMPANY_ID}/nearby?radius=${radius}`, {
            headers: { 'Authorization': `Bearer ${JWT_TOKEN}` }
        });

        const matchRes = await fetch(`http://localhost:8080/api/companies/${MY_COMPANY_ID}/matches?radius=${radius}`, {
            headers: { 'Authorization': `Bearer ${JWT_TOKEN}` }
        });

        // Agar token expire ho gaya hai (403 Forbidden)
        if (!nearbyRes.ok || !matchRes.ok) {
            console.error("Session expired or unauthorized.");
            document.getElementById('results').innerHTML = `<p style="color: #ef4444; padding: 10px;">Session expired. Please <a href="login.html" style="color: #38bdf8;">Login again</a>.</p>`;
            return; // Yahin ruk jao, JSON parse mat karo
        }

        const nearbyData = await nearbyRes.json();
        const matchData = await matchRes.json();

        updateUI(nearbyData, matchData, radius);
    } catch (error) {
        console.error("Error fetching data", error);
    }
}

// 3. Draw on Map & Update Sidebar - Updated with Auth Headers for resources
async function updateUI(companies, matches, radiusInKm) {
    markersGroup.clearLayers();
    matchLinesGroup.clearLayers();
    if (radiusCircle) map.removeLayer(radiusCircle);

    const resultsContainer = document.getElementById('results');
    resultsContainer.innerHTML = '';

    radiusCircle = L.circle([myLat, myLon], {
        color: '#10b981', fillColor: '#10b981', fillOpacity: 0.1, radius: radiusInKm * 1000
    }).addTo(map);

    if (matches.length > 0) {
        let matchHTML = `<h4 style="color: #f59e0b; margin-top: 0; margin-bottom: 10px; text-transform: uppercase; letter-spacing: 1px;">⚡ Perfect Matches (${matches.length})</h4>`;
        matches.forEach(match => {
            matchHTML += `
                <div style="background: rgba(245, 158, 11, 0.15); border-left: 4px solid #f59e0b; padding: 12px; margin-bottom: 15px; border-radius: 5px;">
                    <b style="color: #f59e0b; font-size: 16px;">${match.supplierCompanyName}</b><br>
                    <span style="font-size: 13px; color: #e2e8f0;">Ready to supply <b>${match.availableQuantity} tons</b> of ${match.requirementCategory}</span>
                </div>
            `;
        });
        resultsContainer.innerHTML += matchHTML;
    }

    resultsContainer.innerHTML += `<h4 style="color: #10b981; margin-bottom: 10px; text-transform: uppercase; letter-spacing: 1px;">📍 Network</h4>`;

    for (const company of companies) {
        let lat = company.latitude;
        let lon = company.longitude;

        if (!lat || !lon) {
            if (company.companyName.includes("Roorkee")) { lat = 29.8700; lon = 77.8900; }
            else if (company.companyName.includes("Apex")) { lat = 29.8688; lon = 77.8880; }
            else if (company.companyName.includes("Delhi")) { lat = 28.6139; lon = 77.2090; }
            else {
                lat = myLat + (Math.random() * 0.1 - 0.05);
                lon = myLon + (Math.random() * 0.1 - 0.05);
            }
        }

        let isMatch = matches.find(m => m.supplierCompanyName === company.companyName);

        if (isMatch && lat && lon) {
            L.polyline([[myLat, myLon], [lat, lon]], {
                color: '#f59e0b',
                weight: 3,
                dashArray: '10, 10',
                opacity: 0.9
            }).addTo(matchLinesGroup);
        }

        let resourcesHTML = '<div style="margin-top: 10px; font-size: 12px; display: flex; flex-direction: column; gap: 4px;">';
        try {
            const resResponse = await fetch(`http://localhost:8080/api/companies/${company.id}/resources`, {
                headers: { 'Authorization': `Bearer ${JWT_TOKEN}` }
            });
            const resources = await resResponse.json();
            if(resources.length === 0) {
                resourcesHTML += '<span style="color: #64748b; font-style: italic;">No resources listed.</span>';
            } else {
                resources.forEach(res => {
                    const borderColor = res.listingType === 'WASTE' ? '#ef4444' : '#3b82f6';
                    const icon = res.listingType === 'WASTE' ? '🗑️' : '📦';
                    resourcesHTML += `
                        <div style="background: rgba(255,255,255,0.05); padding: 6px; border-radius: 4px; border-left: 2px solid ${borderColor};">
                            <strong style="color: ${borderColor};">${icon} ${res.listingType}:</strong> 
                            ${res.category} <span style="color: #94a3b8;">(${res.quantityInTons}t)</span>
                        </div>
                    `;
                });
            }
        } catch (err) { }
        resourcesHTML += '</div>';

        if(lat && lon && company.id !== MY_COMPANY_ID) {
            L.marker([lat, lon]).addTo(markersGroup)
                .bindPopup(`<b>${company.companyName}</b><br><span style="color: gray;">${company.industryType}</span>${resourcesHTML}`);
        }

        if (company.id !== MY_COMPANY_ID) {
            resultsContainer.innerHTML += `
                <div class="company-card" style="margin-bottom: 10px; ${isMatch ? 'border-left: 4px solid #f59e0b;' : ''}">
                    <div class="company-name">${company.companyName}</div>
                    <div class="company-industry">${company.industryType}</div>
                    ${resourcesHTML}
                </div>
            `;
        }
    }
}

// 4. Slider Event
const slider = document.getElementById('radius-slider');
const radiusText = document.getElementById('radius-value');

slider.addEventListener('input', (e) => {
    const currentRadius = e.target.value;
    radiusText.innerText = currentRadius;
    fetchData(currentRadius);
});

// Start initial load
fetchData(85);

// --- LOAD PLATFORM STATS ---
async function loadPlatformStats() {
    try {
        // Ab yeh public API hai, isme token bhejne ki zaroorat nahi
        const response = await fetch('http://localhost:8080/api/companies/stats');

        if (response.ok) {
            const stats = await response.json();
            document.getElementById('stat-factories').innerText = stats.totalFactories;
            document.getElementById('stat-resources').innerText = stats.totalResources;
            document.getElementById('stat-waste').innerText = stats.totalWasteTons + 't';
        }
    } catch (error) {
        console.error("Failed to load stats", error);
    }
}

loadPlatformStats();

// --- INTERACTIVE MAP CLICK FOR REGISTRATION ---
let tempMarker = null;

map.on('click', function(e) {
    const sidebar = document.getElementById('sidebar');
    if (sidebar && sidebar.classList.contains('open')) {
        sidebar.classList.remove('open');
    }

    const lat = e.latlng.lat.toFixed(4);
    const lon = e.latlng.lng.toFixed(4);

    if (tempMarker) map.removeLayer(tempMarker);

    tempMarker = L.marker([lat, lon]).addTo(map)
        .bindPopup(`
            <div style="text-align: center;">
                <b>Empty Plot</b><br>Lat: ${lat}, Lon: ${lon}<br><br>
                <button onclick="openRegisterModal(${lat}, ${lon})" style="background: #38bdf8; color: white; border: none; padding: 5px 10px; border-radius: 3px; cursor: pointer;">Build Factory Here</button>
            </div>
        `).openPopup();
});

window.openRegisterModal = function(lat, lon) {
    document.getElementById('regLat').value = lat;
    document.getElementById('regLon').value = lon;
    document.getElementById('registerModal').style.display = 'flex';
    if (tempMarker) map.removeLayer(tempMarker);
};

// --- SUBMIT REGISTRATION FORM & REDIRECT TO LOGIN ---
const registerForm = document.getElementById('registerFactoryForm');
if(registerForm) {
    registerForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const payload = {
            companyName: document.getElementById('regName').value,
            industryType: document.getElementById('regIndustry').value,
            contactEmail: document.getElementById('regEmail').value,
            password: document.getElementById('regPassword').value,
            latitude: document.getElementById('regLat').value,
            longitude: document.getElementById('regLon').value
        };

        try {
            const response = await fetch('http://localhost:8080/api/companies/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (response.ok) {
                document.getElementById('registerModal').style.display = 'none';
                document.getElementById('registerFactoryForm').reset();

                alert("Factory Registered Successfully! Please login to access your dashboard.");
                window.location.href = 'login.html';
            }
        } catch (error) {
            console.error("Error registering factory:", error);
        }
    });
}