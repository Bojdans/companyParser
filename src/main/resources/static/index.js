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

// Генерация рубрик в таблице
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

//Изменение размеров таблицы
document.addEventListener("DOMContentLoaded", () => {
    const table = document.querySelector("#company-table");
    const headers = table.querySelectorAll("th");

    headers.forEach((header) => {
        // Создаем элемент для изменения ширины
        const resizer = document.createElement("div");
        resizer.classList.add("resizer");
        header.appendChild(resizer);

        // Обработка событий для изменения ширины
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

// Функция для скрытия/показа всего контейнера регионов
function toggleRegionContainer() {
    const container = document.querySelector('.region-container');
    container.style.display = container.style.display === 'block' ? 'none' : 'block';
}

// Функция для скрытия/показа дочерних городов
function toggleSubRegions(regionId) {
    const subRegions = document.getElementById(`region-${regionId}`);
    const isVisible = subRegions.style.display === 'block';
    subRegions.style.display = isVisible ? 'none' : 'block';
}

// Функция для выбора/снятия выбора всех городов региона
function selectAllCities(regionCheckbox, regionId) {
    const subRegions = document.getElementById(`region-${regionId}`);
    const checkboxes = subRegions.querySelectorAll('input[type="checkbox"]');
    checkboxes.forEach((checkbox) => {
        checkbox.checked = regionCheckbox.checked;
    });
}

// Функция генерации списка регионов и городов
function generateRegions() {
    const container = document.querySelector('.region-container');
    const regionsContainer = document.createElement('div');
    regionsContainer.classList.add('regions-list');
    container.appendChild(regionsContainer); // Контейнер для регионов

    regions.forEach((region) => {
        const regionDiv = document.createElement('div');
        regionDiv.classList.add('region');

        // Кнопка для раскрытия дочерних городов
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
        // Лейбл и чекбокс для региона
        const regionLabel = document.createElement('label');
        const regionCheckbox = document.createElement('input');
        regionCheckbox.type = 'checkbox';
        regionCheckbox.onchange = () => selectAllCities(regionCheckbox, region.id);

        regionLabel.appendChild(regionCheckbox);
        regionLabel.appendChild(document.createTextNode(` ${region.name}`));

        // Контейнер для дочерних городов
        const subRegionsDiv = document.createElement('div');
        subRegionsDiv.classList.add('sub-regions');
        subRegionsDiv.id = `region-${region.id}`;
        subRegionsDiv.style.display = 'none'; // Скрываем по умолчанию

        // Генерация дочерних городов
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

        // Добавление региона и его городов в контейнер
        regionDiv.appendChild(regionLabel);
        regionDiv.appendChild(subRegionsDiv);
        regionsContainer.appendChild(regionDiv);
    });
}

//Поиск городов и регионов
function filterRegionsAndCities() {
    const query = document.getElementById('search-input').value.toLowerCase();
    const regionsList = document.querySelectorAll('.region');

    regionsList.forEach((regionElement) => {
        const regionLabel = regionElement.querySelector('label').textContent.toLowerCase();
        const subRegions = regionElement.querySelector('.sub-regions');
        const cities = subRegions.querySelectorAll('.city');
        let match = false;

        // Проверяем совпадение с названием региона
        if (regionLabel.includes(query)) {
            match = true;
            subRegions.style.display = 'block'; // Разворачиваем регион
        } else {
            subRegions.style.display = 'none'; // Скрываем города, если регион не совпадает
        }

        // Проверяем совпадение с названиями городов
        let cityMatch = false; // Отслеживаем наличие подходящих городов
        cities.forEach((cityElement) => {
            const cityLabel = cityElement.querySelector('label').textContent.toLowerCase();
            if (cityLabel.includes(query)) {
                cityElement.style.display = 'block'; // Показываем подходящий город
                match = true;
                cityMatch = true;
            } else {
                cityElement.style.display = 'none'; // Скрываем город, если он не совпадает
            }
        });

        // Если регион сам не подходит, но есть совпадающие города, разворачиваем его
        if (!regionLabel.includes(query) && cityMatch) {
            subRegions.style.display = 'block';
        }

        // Показываем или скрываем регион в зависимости от совпадений
        regionElement.style.display = match ? 'block' : 'none';
    });
}

// Инициализация при загрузке страницы

//настройки парсинга
// Функция для загрузки настроек с сервера
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
            // Устанавливаем значения в элементы формы
            document.getElementById('active-only').checked = settings.onlyInOperation;
            document.getElementById('tenders').checked = settings.partOfGovernmentProcurement;
            document.getElementById('depth').value = settings.pagesDeep;
            document.getElementById('pause').value = settings.parsingDelay;
            document.getElementById('export').checked = settings.autoExcelOpen;
            document.getElementById('okved').checked = settings.onlyMainOKVED;
            document.getElementById('remember').checked = settings.rememberParsingPosition;
            document.getElementById('proxy').value = settings.proxy || '';
            document.getElementById('login-proxy').value = settings.proxyLogin || '';
            document.getElementById('password-proxy').value = settings.proxyPassword || '';

            // Обновляем регионы и города
            updateRegionsAndCities(settings.cities, settings.regions);
        })
        .catch((error) => {


        });
}

// Функция для обновления списка городов и регионов на странице
function updateRegionsAndCities(cities, regions) {
    // Сбрасываем предыдущие выборы
    document.querySelectorAll('.city input[type="checkbox"]').forEach((checkbox) => {
        checkbox.checked = false;
    });
    document.querySelectorAll('.region input[type="checkbox"]').forEach((checkbox) => {
        checkbox.checked = false;
    });
    // Устанавливаем выбранные города
    cities.forEach((cityName) => {
        document.querySelectorAll('.city label').forEach((label) => {
            if (label.textContent.trim().toLowerCase() === cityName.trim().toLowerCase()) {

                const checkbox = label.querySelector('input[type="checkbox"]');
                if (checkbox) checkbox.checked = true;
            }
        });
    });

    // Устанавливаем выбранные регионы
    regions.forEach((regionName) => {
        document.querySelectorAll('.region label').forEach((label) => {
            if (label.textContent.trim().toLowerCase() === regionName.trim().toLowerCase()) {
                const checkbox = label.querySelector('input[type="checkbox"]');
                if (checkbox) checkbox.checked = true;
            }
        });
    });
}

// Вызываем загрузку настроек при загрузке страницы
document.addEventListener('DOMContentLoaded', () => {
    fetch("http://localhost:8081/api/getRegions").then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    })
        .then(data => {
            regions = data

            generateRegions();
        }).then(data => {
        loadSettingsFromServer();
    })
});

// Сохраняет настройки на сервере
function refreshSettings() {
    const settings = {
        onlyInOperation: document.getElementById('active-only').checked,
        partOfGovernmentProcurement: document.getElementById('tenders').checked,
        pagesDeep: parseInt(document.getElementById('depth').value, 10),
        parsingDelay: parseFloat(document.getElementById('pause').value),
        autoExcelOpen: document.getElementById('export').checked,
        onlyMainOKVED: document.getElementById('okved').checked,
        rememberParsingPosition: document.getElementById('remember').checked,
        proxy: document.getElementById('proxy').value,
        proxyLogin: document.getElementById('login-proxy').value,
        proxyPassword: document.getElementById('password-proxy').value,
        cities: getSelectedCities(), // Добавьте логику для сбора данных по городам
        regions: getSelectedRegions(),// Добавьте логику для сбора данных по рубрикам
    };
    saveRubrics()
    // Отправка данных на сервер
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
        })
        .catch(error => {

        });
}

// Функция для получения выбранных городов
function getSelectedCities() {
    const selectedCities = [];
    document.querySelectorAll('.city input[type="checkbox"]:checked').forEach((checkbox) => {
        selectedCities.push(checkbox.parentNode.textContent.trim());
    });
    return selectedCities;
}

// Функция для получения выбранных регионов
function getSelectedRegions() {
    const selectedRegions = [];
    document.querySelectorAll('.region > label > input[type="checkbox"]:checked').forEach((checkbox) => {
        let regionName = checkbox.parentNode.textContent.trim();
        selectedRegions.push(regionName.trim());
    });
    return selectedRegions;
}

// Вызываем загрузку настроек при загрузке страницы
document.addEventListener('DOMContentLoaded', () => {
    loadSettingsFromServer();
});

// Глобальный массив для хранения рубрик
let rubrics = [];

/**
 * Загрузка рубрик с сервера, заполнение массива rubrics
 * и отрисовка таблицы.
 */
function loadRubricsFromServer() {
    fetch("/api/getCategories", {
        method: "GET",
        headers: { "Content-Type": "application/json" }
    })
        .then((response) => {
            if (!response.ok) {
                throw new Error("Failed to fetch rubrics");
            }
            return response.json();
        })
        .then((data) => {
            // Здесь data — массив объектов вида {id, name, active, level}
            rubrics = data;
            console.log("Загруженные rubrics:", rubrics);

            renderRubricTable(rubrics);

            // Опционально: загрузить и отобразить "активные" рубрики (пример)
            fetch("/api/getActiveCategories")
                .then((response) => {
                    if (!response.ok) {
                        throw new Error("Failed to fetch active rubrics");
                    }
                    return response.json();
                })
                .then((activeData) => {
                    console.log("Активные рубрики:", activeData);
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

/**
 * Рендерим таблицу рубрик. В каждой строке создаём <input>
 * для 'name', 'active' (checkbox), 'level' (number).
 * Обратите внимание, что при onchange вызываем updateRubric(rubric.id, ...)
 */
function renderRubricTable(rubricsData) {
    console.log("Рендер таблицы рубрик");
    const tableBody = document.getElementById("rubric-table");
    tableBody.innerHTML = "";

    rubricsData.forEach((rubric) => {
        // Каждая rubric имеет поля: { id, name, active, level }
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
addEventListener("DOMContentLoaded",()=>{
    loadRubricsFromServer()
})
/**
 * Функция обновления рубрики в массиве rubrics по её ID.
 * Мы ищем нужный объект по rubric.id и выставляем новое значение.
 */
function updateRubric(rubricId, field, newValue) {
    // Ищем индекс в массиве rubrics по совпадению id
    const index = rubrics.findIndex(r => r.id === rubricId);
    if (index === -1) {
        console.error(`Rubric with id=${rubricId} не найден`);
        return;
    }

    // Обновляем нужное поле (name, active, level) в объекте
    rubrics[index][field] = newValue;
    console.log("Обновили rubrics:", rubrics);
}

/**
 * Сохранение (обновление) рубрик на сервере.
 * Отправляем массив rubrics (со всеми изменениями) методом POST.
 */
function saveRubrics() {
    console.log("Сохраняем (обновляем) рубрики на сервер:", rubrics);

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
            console.log("Успешно обновили рубрики на сервере");
            // Если нужно, можно перезагрузить таблицу или показать уведомление
            // loadRubricsFromServer();
        })
        .catch((error) => {
            console.error("Ошибка при обновлении рубрик:", error);
        });
}

/**
 * Пример функции поиска. Ищем по полю name в локальном массиве rubrics.
 * Если rubrics пуст, можно либо ничего не делать, либо сделать запрос на сервер.
 */
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
        // Если rubrics пуст, можно (при желании) обратиться к серверу:
        /*
        fetch(`/api/searchCategories?query=${encodeURIComponent(query)}`)
            .then(resp => resp.json())
            .then(filtered => renderRubricTable(filtered))
            .catch(err => console.error("Ошибка при поиске:", err));
        */
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
        //функция рендера таблицы городов
        fetchCompanies().then(response => {
            if (response.ok) {
                logArea.textContent = "Очищено"
            } else {
                logArea.textContent = "Ошибка при отображении компаний"
            }
        })
    })
}

async function fetchCompanies() {
    const response = await fetch('http://localhost:8081/api/getAllCompanies');
    const companies = await response.json();
    const tableBody = document.querySelector('#company-table tbody');
    companies.forEach(company => {
        const row = document.createElement('tr');
        const phoneOptions = company.phones
            ? company.phones.split(',').map(phone => `<option>${phone.trim()}</option>`).join('')
            : '';
        row.innerHTML = `
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
                <td>
                    <select class="dropdown">${phoneOptions}</select>
                </td>
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
                <td>${company.numberOfEmployees || ''}</td>
                <td>${company.okvedCode || ''}</td>
            `;
        tableBody.appendChild(row);
    });
}

window.onload = fetchCompanies;

let pollingIntervalId = null; // будет хранить идентификатор setInterval

function startParsing() {
    logArea.textContent = "Парсинг компаний...";

    // Запускаем парсинг на сервере
    fetch('api/startParsingCompanies', {
        method: "POST"
    })
        .then(response => {
            // Когда запрос завершится, начинаем периодический опрос
            startPollingStatus();
            // Если нужно, можно вызывать какую-то логику рендера, например fetchCompanies()
            // fetchCompanies();
        })
        .catch(err => {
            console.error("Ошибка при старте парсинга:", err);
        });
}

// Функция, которая раз в N секунд опрашивает сервер о статусе
function startPollingStatus() {
    // На всякий случай, если вдруг был запущен предыдущий опрос, останавливаем его
    stopPollingStatus();

    // Интервал опроса в миллисекундах, например 2000 мс = 2 секунды
    const intervalMs = 2000;
    setTimeout(() => {
        // Периодический опрос
        pollingIntervalId = setInterval(() => {
            fetch('api/parsingStatus')
                .then(response => response.json())
                .then(data => {
                    const {isParsed} = data;

                    // Если сервер говорит, что парсинг закончен, прекращаем опрос
                    if (isParsed) {
                        logArea.textContent += "\nПарсинг завершён";
                        stopPollingStatus();
                    }
                })
                .catch(err => {
                    console.error("Ошибка при получении статуса парсинга:", err);
                });
        }, intervalMs);
    }, 5000)
}

// Останавливаем периодический опрос
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
        logArea.textContent="Парсинг выключен"
    })
}

function shutdown() {
    fetch('api/shutdown', {
        method: "POST"
    }).then(response => {
        logArea.textContent="Выход..."
    })
}
