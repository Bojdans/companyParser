let viewFilter = {
    rubric: ""
}
let logArea = document.getElementById("log-area")

function switchTab(tabId) {
    const tabs = document.querySelectorAll('.content');
    const buttons = document.querySelectorAll('.tab-bar button');

    tabs.forEach(tab => tab.classList.remove('active'));
    buttons.forEach(button => button.classList.remove('active'));

    document.getElementById(tabId).classList.add('active');
    buttons[[...tabs].findIndex(tab => tab.id === tabId)].classList.add('active');
}


function renderSideRubrics(rubricsSide) {
    const rubricTableBody = document.querySelector("#rubric-table-side tbody");
    rubricTableBody.innerHTML = ""
    rubricsSide.forEach(rubric => {
        const row = document.createElement("tr");
        row.innerHTML = `
                <td>${rubric.name}</td>
            `;
        rubricTableBody.appendChild(row);
    });
}


document.addEventListener("DOMContentLoaded", () => {
    const table = document.querySelector("#company-table");
    const headers = table.querySelectorAll("th");

    headers.forEach((header) => {
        
        const resizer = document.createElement("div");
        resizer.classList.add("resizer");
        header.appendChild(resizer);

        
        resizer.addEventListener("mousedown", (e) => {
            const startX = e.pageX;
            const startWidth = header.offsetWidth;

            const onMouseMove = (e) => {
                const newWidth = startWidth + (e.pageX - startX);
                header.style.width = newWidth + "px";
            };

            const onMouseUp = () => {
                document.removeEventListener("mousemove", onMouseMove);
                document.removeEventListener("mouseup", onMouseUp);
            };

            document.addEventListener("mousemove", onMouseMove);
            document.addEventListener("mouseup", onMouseUp);
        });
    });
});
let regions = null


function toggleRegionContainer() {
    const container = document.querySelector('.region-container');
    container.style.display = container.style.display === 'block' ? 'none' : 'block';
}


function toggleSubRegions(regionId) {
    const subRegions = document.getElementById(`region-${regionId}`);
    const isVisible = subRegions.style.display === 'block';
    subRegions.style.display = isVisible ? 'none' : 'block';
}


function selectAllCities(regionCheckbox, regionId) {
    const subRegions = document.getElementById(`region-${regionId}`);
    const checkboxes = subRegions.querySelectorAll('input[type="checkbox"]');
    checkboxes.forEach((checkbox) => {
        checkbox.checked = regionCheckbox.checked;
    });
}


function generateRegions() {
    const container = document.querySelector('.region-container');
    const regionsContainer = document.createElement('div');
    regionsContainer.classList.add('regions-list');
    container.appendChild(regionsContainer); 

    regions.forEach((region) => {
        const regionDiv = document.createElement('div');
        regionDiv.classList.add('region');

        
        if (region.daughters.length > 0) {
            const regionButton = document.createElement('button');
            regionButton.classList.add('expand-button');
            regionButton.textContent = '+';
            regionButton.onclick = () => toggleSubRegions(region.id);
            regionDiv.appendChild(regionButton);
        } else {
            const regionButton = document.createElement('button');
            regionButton.classList.add('expand-button');
            regionButton.textContent = ' ';
            regionButton.style.margin = "7px"
            regionButton.onclick = () => toggleSubRegions(region.id);
            regionDiv.appendChild(regionButton);
        }
        
        const regionLabel = document.createElement('label');
        const regionCheckbox = document.createElement('input');
        regionCheckbox.type = 'checkbox';
        regionCheckbox.onchange = () => selectAllCities(regionCheckbox, region.id);

        regionLabel.appendChild(regionCheckbox);
        regionLabel.appendChild(document.createTextNode(` ${region.name}`));

        
        const subRegionsDiv = document.createElement('div');
        subRegionsDiv.classList.add('sub-regions');
        subRegionsDiv.id = `region-${region.id}`;
        subRegionsDiv.style.display = 'none'; 

        
        region.daughters.forEach((city) => {
            const cityDiv = document.createElement('div');
            cityDiv.classList.add('city');

            const cityLabel = document.createElement('label');
            const cityCheckbox = document.createElement('input');
            cityCheckbox.type = 'checkbox';

            cityLabel.appendChild(cityCheckbox);
            cityLabel.appendChild(document.createTextNode(` ${city.name}`));

            cityDiv.appendChild(cityLabel);
            subRegionsDiv.appendChild(cityDiv);
        });

        
        regionDiv.appendChild(regionLabel);
        regionDiv.appendChild(subRegionsDiv);
        regionsContainer.appendChild(regionDiv);
    });
}


function filterRegionsAndCities() {
    const query = document.getElementById('search-input').value.toLowerCase();
    const regionsList = document.querySelectorAll('.region');

    regionsList.forEach((regionElement) => {
        const regionLabel = regionElement.querySelector('label').textContent.toLowerCase();
        const subRegions = regionElement.querySelector('.sub-regions');
        const cities = subRegions.querySelectorAll('.city');
        let match = false;

        
        if (regionLabel.includes(query)) {
            match = true;
            subRegions.style.display = 'block'; 
        } else {
            subRegions.style.display = 'none'; 
        }

        
        let cityMatch = false; 
        cities.forEach((cityElement) => {
            const cityLabel = cityElement.querySelector('label').textContent.toLowerCase();
            if (cityLabel.includes(query)) {
                cityElement.style.display = 'block'; 
                match = true;
                cityMatch = true;
            } else {
                cityElement.style.display = 'none'; 
            }
        });

        
        if (!regionLabel.includes(query) && cityMatch) {
            subRegions.style.display = 'block';
        }

        
        regionElement.style.display = match ? 'block' : 'none';
    });
}





function loadSettingsFromServer() {
    fetch('/api/settings', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
        },
    })
        .then((response) => {
            if (!response.ok) {
                throw new Error('Failed to fetch settings');
            }
            return response.json();
        })
        .then((settings) => {
            
            document.getElementById('active-only').checked = settings.onlyInOperation;
            document.getElementById('tenders').checked = settings.partOfGovernmentProcurement;
            document.getElementById('depth').value = settings.pagesDeep;
            document.getElementById('pause').value = settings.parsingDelay;
            document.getElementById('export').checked = settings.autoExcelOpen;
            document.getElementById('okved').checked = settings.onlyMainOKVED;
            document.getElementById('remember').checked = settings.rememberParsingPosition;
            document.getElementById('anti-captcha-key').value = settings.anticaptchaKey || '';
            document.getElementById('rucaptcha-key').value = settings.rucaptchaKey || '';
            document.getElementById('yandex').checked = settings.yandexCaptcha;
            document.getElementById('google').checked = settings.googleCaptcha;
            document.getElementById('proxy').value = settings.proxy || '';
            document.getElementById('login-proxy').value = settings.proxyLogin || '';
            document.getElementById('password-proxy').value = settings.proxyPassword || '';

            
            updateRegionsAndCities(settings.cities, settings.regions);
        })
        .catch((error) => {


        });
}


function updateRegionsAndCities(cities, regions) {
    
    document.querySelectorAll('.city input[type="checkbox"]').forEach((checkbox) => {
        checkbox.checked = false;
    });
    document.querySelectorAll('.region input[type="checkbox"]').forEach((checkbox) => {
        checkbox.checked = false;
    });
    
    cities.forEach((cityName) => {
        document.querySelectorAll('.city label').forEach((label) => {
            if (label.textContent.trim().toLowerCase() === cityName.trim().toLowerCase()) {

                const checkbox = label.querySelector('input[type="checkbox"]');
                if (checkbox) checkbox.checked = true;
            }
        });
    });

    
    regions.forEach((regionName) => {
        document.querySelectorAll('.region label').forEach((label) => {
            if (label.textContent.trim().toLowerCase() === regionName.trim().toLowerCase()) {
                const checkbox = label.querySelector('input[type="checkbox"]');
                if (checkbox) checkbox.checked = true;
            }
        });
    });
}


document.addEventListener('DOMContentLoaded', () => {
    fetch("http://localhost:8081/api/getRegions").then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    })
        .then(data => {
            regions = data
            startPollingStatus();
            generateRegions();
        }).then(data => {
        loadSettingsFromServer();
    })
});


function refreshSettings() {
    const settings = {
        onlyInOperation: document.getElementById('active-only').checked,
        partOfGovernmentProcurement: document.getElementById('tenders').checked,
        pagesDeep: parseInt(document.getElementById('depth').value, 10),
        parsingDelay: parseFloat(!document.getElementById('pause').value ? 0 : document.getElementById('pause').value),
        autoExcelOpen: document.getElementById('export').checked,
        onlyMainOKVED: document.getElementById('okved').checked,
        rememberParsingPosition: document.getElementById('remember').checked,
        yandexCaptcha: document.getElementById('yandex').checked,
        googleCaptcha: document.getElementById('google').checked,
        anticaptchaKey: document.getElementById('anti-captcha-key').value,
        rucaptchaKey: document.getElementById('rucaptcha-key').value,
        proxy: document.getElementById('proxy').value,
        proxyLogin: document.getElementById('login-proxy').value,
        proxyPassword: document.getElementById('password-proxy').value,
        cities: getSelectedCities(), 
        regions: getSelectedRegions(),
    };
    saveRubrics()
    
    fetch('/api/settings', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(settings)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to save settings');
            }
            window.location.reload(true);
        })
        .catch(error => {

        });
    loadRubricsFromServer()
}


function getSelectedCities() {
    const selectedCities = [];
    document.querySelectorAll('.city input[type="checkbox"]:checked').forEach((checkbox) => {
        selectedCities.push(checkbox.parentNode.textContent.trim());
    });
    return selectedCities;
}


function getSelectedRegions() {
    const selectedRegions = [];
    document.querySelectorAll('.region > label > input[type="checkbox"]:checked').forEach((checkbox) => {
        let regionName = checkbox.parentNode.textContent.trim();
        selectedRegions.push(regionName.trim());
    });
    return selectedRegions;
}


document.addEventListener('DOMContentLoaded', () => {
    loadSettingsFromServer();
});


let rubrics = [];


function loadRubricsFromServer() {
    fetch("/api/getCategories", {
        method: "GET",
        headers: {"Content-Type": "application/json"}
    })
        .then((response) => {
            if (!response.ok) {
                throw new Error("Failed to fetch rubrics");
            }
            return response.json();
        })
        .then((data) => {
            
            rubrics = data;

            renderRubricTable(data);

            
            fetch("/api/getActiveCategories")
                .then((response) => {
                    if (!response.ok) {
                        throw new Error("Failed to fetch active rubrics");
                    }
                    return response.json();
                })
                .then((activeData) => {

                    renderSideRubrics(activeData);
                })
                .catch((err) => {
                    console.error("Ошибка при загрузке активных рубрик:", err);
                });
        })
        .catch((error) => {
            console.error("Ошибка при загрузке рубрик:", error);
        });
}


function renderRubricTable(rubricsData) {

    const tableBody = document.getElementById("rubric-table");
    tableBody.innerHTML = "";

    rubricsData.forEach((rubric) => {
        
        const row = document.createElement("tr");

        row.innerHTML = `
          <td>
            <input type="hidden" value="${rubric.id}" />
            <input 
              type="text"
              value="${rubric.name}"
              onchange="updateRubric(${rubric.id}, 'name', this.value)"
            />
          </td>
          <td class="checkbox-cell">
            <input 
              type="checkbox" 
              ${rubric.active ? "checked" : ""}
              onchange="updateRubric(${rubric.id}, 'active', this.checked)"
            />
          </td>
          <td>
            <input 
              type="number"
              min="1" 
              max="5"
              value="${rubric.level}"
              onchange="updateRubric(${rubric.id}, 'level', parseInt(this.value))"
            />
          </td>
        `;
        tableBody.appendChild(row);
    });
}

addEventListener("DOMContentLoaded", () => {
    loadRubricsFromServer()
})


function updateRubric(rubricId, field, newValue) {
    
    const index = rubrics.findIndex(r => r.id === rubricId);
    if (index === -1) {
        console.error(`Rubric with id=${rubricId} не найден`);
        return;
    }

    
    rubrics[index][field] = newValue;

}


function saveRubrics() {


    fetch("/api/updateCategories", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(rubrics),
    })
        .then((response) => {
            if (!response.ok) {
                throw new Error("Failed to update rubrics");
            }

            
            
        })
        .catch((error) => {
            console.error("Ошибка при обновлении рубрик:", error);
        });
}


function searchCategories() {
    const query = document
        .getElementById("rubric-search")
        .value.trim()
        .toLowerCase();

    if (rubrics && rubrics.length > 0) {
        const filtered = rubrics.filter(r =>
            r.name.toLowerCase().includes(query)
        );
        renderRubricTable(filtered);
    } else {
        
        
    }
}


document.getElementById("rubric-search").addEventListener("input", searchCategories);

function generateMenu() {
    logArea.textContent = "Сбор категорий...."
    fetch('api/startParsingCategories', {
        method: "POST"
    }).then(response => {
        if (!response.ok) {
            throw new Error('Failed to fetch categories');
        } else {
            logArea.textContent = "Категории собраны"
            loadRubricsFromServer()
        }
    })
}

function startExport() {
    fetch('api/exportCompaniesDB', {
        method: "POST"
    }).then(response => {
        logArea.textContent = "Экспорт завершён"
    })
}

function cleanCompanies() {
    fetch('api/cleanCompanies', {
        method: "POST"
    }).then(response => {
        
        fetchCompanies().then(response => {
            logArea.textContent = "Очищено"
            const settings = {
                onlyInOperation: document.getElementById('active-only').checked,
                partOfGovernmentProcurement: document.getElementById('tenders').checked,
                pagesDeep: parseInt(document.getElementById('depth').value, 10),
                parsingDelay: parseFloat(document.getElementById('pause').value),
                autoExcelOpen: document.getElementById('export').checked,
                onlyMainOKVED: document.getElementById('okved').checked,
                rememberParsingPosition: document.getElementById('remember').checked,
                anticaptchaKey: document.getElementById('anti-captcha-key').value,
                proxy: document.getElementById('proxy').value,
                proxyLogin: document.getElementById('login-proxy').value,
                proxyPassword: document.getElementById('password-proxy').value,
                cities: [],
                regions: [],
            };
            rubrics = rubrics.map(rubric => rubric = {
                id: rubric.id,
                name: rubric.name,
                active: false,
                level: rubric.level
            })
            saveRubrics()
            
            fetch('/api/settings', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(settings)
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Failed to save settings');
                    }
                    loadRubricsFromServer()
                    loadSettingsFromServer()
                    document.querySelector("#company-table tbody").innerHTML = ""
                    window.location.reload(true);
                })
                .catch(error => {
                });
        })
    })
}

let page = 0; 
const pageSize = 50; 
let isLoading = false; 
let hasMoreData = true; 

async function fetchCompanies() {
    if (isLoading || !hasMoreData) return;
    isLoading = true;

    try {
        const response = await fetch(`http://localhost:8081/api/getAllCompanies?page=${page}&pageSize=${pageSize}`);
        const data = await response.json();

        if (data.content.length === 0) {
            hasMoreData = false; 
            observer.disconnect(); 
            return;
        }

        const tableBody = document.querySelector('#company-table tbody');
        const fragment = document.createDocumentFragment();

        data.content.forEach(company => {
            const row = document.createElement('tr');
            const phoneOptions = company.phones
                ? company.phones.split(',').map(phone => `<option>${phone.trim()}</option>`).join('')
                : '';

            row.innerHTML = `
                <td>${company.rubric || ''}</td>
                <td>${company.organizationType || ''}</td>
                <td>${company.organizationName || ''}</td>
                <td>${company.founder || ''}</td>
                <td>${company.founderPosition || ''}</td>
                <td>${company.inn || ''}</td>
                <td>${company.ogrn || ''}</td>
                <td>${company.okatoCode || ''}</td>
                <td>${company.authorizedCapital || ''}</td>
                <td>${company.legalAddress || ''}</td>
                <td>${company.city || ''}</td>
                <td><select class="dropdown">${phoneOptions}</select></td>
                <td>${company.email || ''}</td>
                <td>${company.website || ''}</td>
                <td>${company.revenue || ''}</td>
                <td>${company.profit || ''}</td>
                <td>${company.capital || ''}</td>
                <td>${company.taxes || ''}</td>
                <td>${company.insuranceContributions || ''}</td>
                <td>${company.governmentPurchasesCustomer || ''}</td>
                <td>${company.governmentPurchasesSupplier || ''}</td>
                <td>${company.activeCompany ? 'Да' : 'Нет'}</td>
                <td>${company.registrationDate || ''}</td>
                <td>${company.numberOfEmployees}</td>
                <td>${company.okvedCode || ''}</td>
            `;

            fragment.appendChild(row);
        });

        tableBody.appendChild(fragment);
        page++; 
    } catch (error) {
        console.error("Ошибка загрузки данных:", error);
    } finally {
        isLoading = false;
    }
}


const observer = new IntersectionObserver(entries => {
    if (entries[0].isIntersecting) {
        fetchCompanies();
    }
}, {rootMargin: "100px"});

observer.observe(document.querySelector("#load-more"));

window.onload = fetchCompanies;

function startParsing() {
    startPollingStatus();
    
    fetch('api/startParsingCompanies', {
        method: "POST"
    })
        .then(response => {
        })
        .catch(err => {
            console.error("Ошибка при старте парсинга:", err);
        });
}

let pollingIntervalId = null;

function startPollingStatus() {

    
    stopPollingStatus();
    
    const intervalMs = 2000;
    pollingIntervalId = setInterval(() => {
        fetch('api/getLogStatus')
            .then(response => response.text())
            .then(textData => {
                
                
                
                
                logArea.textContent = textData;
            })
            .catch(err => {
                console.error("Ошибка при получении статуса:", err);
            });
    }, intervalMs);
}

function stopPollingStatus() {
    if (pollingIntervalId) {
        clearInterval(pollingIntervalId);
        pollingIntervalId = null;
    }
}

function stopParsing() {
    fetch('api/stopParsingCompanies', {
        method: "POST"
    }).then(response => {
    })
}

function shutdown() {
    fetch('api/shutdown', {
        method: "POST"
    }).then(response => {
        logArea.textContent = "Выход..."
        window.opener = null;
        window.open("", "_self");
        window.close();
    })
}
