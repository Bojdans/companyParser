let viewFilter = {
    rubric: ""
}
let rubrics = [];
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

function saveRubrics() {
    const updatedRubrics = [];
    const rows = document.querySelectorAll("#rubric-table tr");

    rows.forEach((row) => {
        const rubric = {
            id: parseInt(row.querySelector("input[type='hidden']").value, 10),
            name: row.querySelector("input[type='text']").value,
            active: row.querySelector("input[type='checkbox']").checked,
            level: parseInt(row.querySelector("input[type='number']").value, 10),
        };
        updatedRubrics.push(rubric);
    });

    // Отправка данных на сервер
    fetch("/api/updateCategories", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(updatedRubrics),
    })
        .then((response) => {
            if (!response.ok) {
                throw new Error("Failed to update rubrics");
            }
            
        })
        .catch((error) => {

            
        });
}

function updateRubric(index, key, value) {
    rubrics[index][key] = value; // Обновляем локальные данные
}

function renderRubricTable(rubrics) {
    console.log("рендер таблицы")
    const tableBody = document.getElementById("rubric-table");
    tableBody.innerHTML = ""; // Очищаем таблицу перед повторным рендерингом
    rubrics.forEach((rubric) => {
        const row = document.createElement("tr");

        row.innerHTML = `
        <td>
          <input type="hidden" value="${rubric.id}" />
          <input type="text" value="${rubric.name}" onchange="updateRubric(${rubric.id}, 'name', this.value)" />
        </td>
        <td class="checkbox-cell">
          <input type="checkbox" ${rubric.active ? "checked" : ""} onchange="updateRubric(${rubric.id}, 'active', this.checked)" />
        </td>
        <td>
          <input type="number" min="1" max="5" value="${rubric.level}" onchange="updateRubric(${rubric.id}, 'level', parseInt(this.value))" />
        </td>
      `;

        tableBody.appendChild(row);
    });
}

function loadRubricsFromServer() {
    fetch('/api/getCategories', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
        },
    })
        .then((response) => {
            if (!response.ok) {
                throw new Error('Failed to fetch rubrics');
            }
            return response.json();
        })
        .then((data) => {
            rubrics = data; // Заполняем глобальный массив rubrics данными с сервера
            renderRubricTable(rubrics); // Отображаем таблицу
            fetch("/api/getActiveCategories").then((response) => {
                if (!response.ok) {
                    throw new Error('Failed to fetch rubrics');
                }
                return response.json();
            })
                .then((data) => {
                     // Заполняем глобальный массив rubrics данными с сервера
                    renderSideRubrics(data); // Отображаем таблицу
                })
        })
        .catch((error) => {
            
        });
}

// Вызываем загрузку рубрик при загрузке страницы
document.addEventListener('DOMContentLoaded', () => {
    loadRubricsFromServer();
});

// Attach event listener for the search input
function searchCategories() {
    const query = document.getElementById("rubric-search").value;

    fetch(`/api/searchCategories?query=${encodeURIComponent(query)}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to fetch categories');
            }
            return response.json();
        })
        .then(categories => {
            console.log(categories)
            renderRubricTable(categories);
        })
        .catch(error => {
            console.error("Error fetching categories:", error);
        });
}

document.getElementById("rubric-search").addEventListener("input", searchCategories);

function generateMenu(){
    logArea.textContent = "Сбор категорий...."
    fetch('api/startParsingCategories',{
        method:"POST"
    }).then(response => {
        if (!response.ok) {
            throw new Error('Failed to fetch categories');
        }else {
            logArea.textContent = "Категории собраны"
            loadRubricsFromServer()
        }
    })
}