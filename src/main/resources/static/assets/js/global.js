function escapeHtml(value) {
    if (value === null || value === undefined) {
        return "";
    }

    return String(value).replace(/[&<>"']/g, function (character) {
        return {
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;"
        }[character];
    });
}

function escapeHtmlAttribute(value) {
    return escapeHtml(value);
}

function safePositiveInteger(value) {
    const parsedValue = Number(value);
    return Number.isInteger(parsedValue) && parsedValue > 0 ? parsedValue : null;
}

function safeTmdbImagePath(path) {
    if (typeof path !== "string" || !/^\/[A-Za-z0-9._/-]+$/.test(path)) {
        return "";
    }
    return path;
}

function safeTmdbImageUrl(path) {
    const safePath = safeTmdbImagePath(path);
    return safePath ? escapeHtmlAttribute(imgServer + safePath) : "";
}

function safeTmdbStoredImageUrl(url) {
    if (typeof url !== "string" || url.trim() === "") {
        return "";
    }

    try {
        const parsedUrl = new URL(url);
        return parsedUrl.protocol === "https:" && parsedUrl.hostname === "image.tmdb.org"
            ? parsedUrl.href
            : "";
    } catch (e) {
        return "";
    }
}

function safeVideoType(value) {
    return value === "movie" || value === "tv" ? value : "";
}

function isSafeTmdbMedia(media) {
    return !(media && media.adult === true);
}

function filterSafeTmdbResults(results) {
    return Array.isArray(results) ? results.filter(isSafeTmdbMedia) : [];
}

function enhanceFilterSelects(root) {
    const rootElement = typeof root === "string" ? document.querySelector(root) : (root || document);
    if (!rootElement) {
        return;
    }

    rootElement.querySelectorAll("select.form-select").forEach(function (select) {
        enhanceFilterSelect(select);
    });
}

function enhanceFilterSelect(select) {
    if (!(select instanceof HTMLSelectElement) || select.dataset.filterEnhanced === "true") {
        return;
    }

    select.dataset.filterEnhanced = "true";
    const selectId = select.id || "filter-select-" + Math.random().toString(36).slice(2);
    select.id = selectId;

    const wrapper = document.createElement("div");
    wrapper.className = "filter-combobox";
    select.parentNode.insertBefore(wrapper, select);
    wrapper.appendChild(select);
    select.classList.add("filter-combobox__native");

    const button = document.createElement("button");
    button.type = "button";
    button.className = "filter-combobox__button";
    button.setAttribute("aria-haspopup", "listbox");
    button.setAttribute("aria-expanded", "false");

    const buttonText = document.createElement("span");
    buttonText.className = "filter-combobox__text";

    const buttonLabel = document.createElement("span");
    buttonLabel.className = "filter-combobox__label";

    const buttonValue = document.createElement("span");
    buttonValue.className = "filter-combobox__value";

    const chevron = document.createElement("i");
    chevron.className = "bi bi-chevron-down filter-combobox__chevron";
    chevron.setAttribute("aria-hidden", "true");

    buttonText.appendChild(buttonLabel);
    buttonText.appendChild(buttonValue);
    button.appendChild(buttonText);
    button.appendChild(chevron);

    const panel = document.createElement("div");
    panel.className = "filter-combobox__panel";

    const search = document.createElement("input");
    search.type = "search";
    search.className = "filter-combobox__search";
    search.autocomplete = "off";

    const list = document.createElement("div");
    list.className = "filter-combobox__list";
    list.id = selectId + "-listbox";
    list.setAttribute("role", "listbox");

    panel.appendChild(search);
    panel.appendChild(list);
    wrapper.appendChild(button);
    wrapper.appendChild(panel);

    const placeholder = getFilterSelectPlaceholder(select);
    search.placeholder = "Search " + placeholder.toLowerCase();
    button.setAttribute("aria-controls", list.id);

    function updateButton() {
        const selectedOption = select.options[select.selectedIndex] || select.options[0];
        const hasActiveValue = select.selectedIndex > 0 && selectedOption && selectedOption.value !== "";
        buttonLabel.textContent = hasActiveValue ? placeholder : "";
        buttonValue.textContent = selectedOption ? selectedOption.textContent : placeholder;
        wrapper.classList.toggle("has-value", hasActiveValue);
    }

    function renderOptions(query) {
        const normalizedQuery = String(query || "").trim().toLowerCase();
        const matchingOptions = Array.from(select.options).filter(function (option) {
            return option.textContent.toLowerCase().includes(normalizedQuery);
        });

        list.textContent = "";
        if (!matchingOptions.length) {
            const empty = document.createElement("div");
            empty.className = "filter-combobox__empty";
            empty.textContent = "No matches";
            list.appendChild(empty);
            return;
        }

        matchingOptions.forEach(function (option) {
            const item = document.createElement("button");
            item.type = "button";
            item.className = "filter-combobox__option";
            item.textContent = option.textContent;
            item.setAttribute("role", "option");
            item.setAttribute("aria-selected", String(option.index === select.selectedIndex));
            item.addEventListener("click", function () {
                select.selectedIndex = option.index;
                select.dispatchEvent(new Event("change", { bubbles: true }));
                updateButton();
                closeFilterSelect(wrapper);
                button.focus();
            });
            list.appendChild(item);
        });
    }

    button.addEventListener("click", function () {
        if (wrapper.classList.contains("is-open")) {
            closeFilterSelect(wrapper);
        } else {
            openFilterSelect(wrapper, search, list, renderOptions);
        }
    });

    button.addEventListener("keydown", function (event) {
        if (event.key === "ArrowDown" || event.key === "Enter" || event.key === " ") {
            event.preventDefault();
            openFilterSelect(wrapper, search, list, renderOptions);
        }
    });

    search.addEventListener("input", function () {
        renderOptions(search.value);
    });

    search.addEventListener("keydown", function (event) {
        if (event.key === "Escape") {
            closeFilterSelect(wrapper);
            button.focus();
        } else if (event.key === "ArrowDown") {
            event.preventDefault();
            focusFilterOption(list, "next");
        }
    });

    list.addEventListener("keydown", function (event) {
        if (event.key === "Escape") {
            closeFilterSelect(wrapper);
            button.focus();
        } else if (event.key === "ArrowDown") {
            event.preventDefault();
            focusFilterOption(list, "next");
        } else if (event.key === "ArrowUp") {
            event.preventDefault();
            focusFilterOption(list, "previous");
        }
    });

    document.addEventListener("click", function (event) {
        if (!wrapper.contains(event.target)) {
            closeFilterSelect(wrapper);
        }
    });

    select.addEventListener("change", updateButton);
    updateButton();
    renderOptions("");
}

function getFilterSelectPlaceholder(select) {
    return select.options[0] ? select.options[0].textContent.trim() : "Filter";
}

function openFilterSelect(wrapper, search, list, renderOptions) {
    closeAllFilterSelects(wrapper);
    wrapper.classList.add("is-open");
    const button = wrapper.querySelector(".filter-combobox__button");
    if (button) {
        button.setAttribute("aria-expanded", "true");
    }
    search.value = "";
    renderOptions("");
    window.setTimeout(function () {
        search.focus();
        const selectedOption = list.querySelector('[aria-selected="true"]');
        if (selectedOption) {
            selectedOption.scrollIntoView({ block: "nearest" });
        }
    }, 0);
}

function closeFilterSelect(wrapper) {
    wrapper.classList.remove("is-open");
    const button = wrapper.querySelector(".filter-combobox__button");
    if (button) {
        button.setAttribute("aria-expanded", "false");
    }
}

function closeAllFilterSelects(exceptWrapper) {
    document.querySelectorAll(".filter-combobox.is-open").forEach(function (wrapper) {
        if (wrapper !== exceptWrapper) {
            closeFilterSelect(wrapper);
        }
    });
}

function focusFilterOption(list, direction) {
    const options = Array.from(list.querySelectorAll(".filter-combobox__option"));
    if (!options.length) {
        return;
    }

    const activeIndex = options.indexOf(document.activeElement);
    let nextIndex = 0;
    if (activeIndex >= 0) {
        nextIndex = direction === "previous"
            ? (activeIndex - 1 + options.length) % options.length
            : (activeIndex + 1) % options.length;
    }
    options[nextIndex].focus();
}

function createImageElement(src, alt, options) {
    if (!src) {
        return null;
    }

    const image = document.createElement("img");
    image.src = src;
    image.alt = alt || "";

    if (options && options.className) {
        image.className = options.className;
    }
    if (options && options.style) {
        Object.assign(image.style, options.style);
    }

    return image;
}

function setImageContent(target, src, alt, options) {
    const element = target instanceof HTMLElement ? target : $(target).get(0);
    if (!element) {
        return null;
    }

    element.textContent = "";
    const image = createImageElement(src, alt, options);
    if (image) {
        element.appendChild(image);
    }
    return image;
}

function setRatingIcon(target, iconPath, size) {
    const safePath = typeof iconPath === "string" && /^assets\/images\/ranking_icons\/[A-Za-z0-9._-]+\.png$/.test(iconPath)
        ? iconPath
        : "";

    return setImageContent(target, safePath, "rating icon", {
        style: {
            width: size + "px",
            height: size + "px"
        }
    });
}

function showTextMessage(target, message) {
    const element = target instanceof HTMLElement ? target : $(target).get(0);
    if (!element) {
        return;
    }

    element.textContent = "";
    const paragraph = document.createElement("p");
    paragraph.textContent = message;
    element.appendChild(paragraph);
}

function showLoadingMessage(target, message) {
    const element = target instanceof HTMLElement ? target : $(target).get(0);
    if (!element) {
        return;
    }

    element.textContent = "";
    const loading = document.createElement("div");
    loading.className = "media-loading";
    loading.setAttribute("role", "status");
    loading.setAttribute("aria-live", "polite");

    const spinner = document.createElement("span");
    spinner.className = "media-loading__spinner";
    spinner.setAttribute("aria-hidden", "true");

    const label = document.createElement("span");
    label.className = "media-loading__label";
    label.textContent = message || "Loading results...";

    loading.appendChild(spinner);
    loading.appendChild(label);
    element.appendChild(loading);
}

function redirectToSearch(inputValue, videoType) {
    const value = String(inputValue || "").trim();
    if (value.length === 0) {
        alert("The input is empty!");
        return;
    }

    const searchParams = new URLSearchParams({ value: value });
    const safeType = safeVideoType(videoType);
    if (safeType) {
        searchParams.set("type", safeType);
    }

    window.location.href = server + "/search-results?" + searchParams.toString();
}

function fetchCurrentUser() {
    return $.ajax({
        url: server + "/user/me",
        method: "GET",
        headers: {
            "accept": "application/json"
        }
    });
}

function redirectToLogin() {
    window.location.href = server + "/login";
}

function toggleContent(isChecked) {
    window.location.href = "?toggle=" + Boolean(isChecked);
}

function updateMediaPagination(page, options) {
    const settings = options || {};
    const currentPage = Math.max(1, Number(page) || 1);
    const totalPages = Number(settings.totalPages);
    const isLoading = Boolean(settings.isLoading);
    const hasTotalPages = Number.isInteger(totalPages) && totalPages > 0;
    const hasPrevious = !isLoading && (settings.hasPrevious !== undefined ? Boolean(settings.hasPrevious) : currentPage > 1);
    const hasNext = !isLoading && (settings.hasNext !== undefined
        ? Boolean(settings.hasNext)
        : !hasTotalPages || currentPage < totalPages);
    const pageNumbers = document.querySelectorAll("[data-pagination-current], #pageNum");
    const pageTotals = document.querySelectorAll("[data-pagination-total], #pageTotal");

    pageNumbers.forEach(function (pageNumber) {
        pageNumber.textContent = currentPage;
    });

    document.querySelectorAll('.media-pagination [data-action="prev"]').forEach(function (button) {
        setMediaPaginationButtonState(button, !hasPrevious);
    });

    document.querySelectorAll('.media-pagination [data-action="next"]').forEach(function (button) {
        setMediaPaginationButtonState(button, !hasNext);
    });

    pageTotals.forEach(function (pageTotal) {
        if (isLoading) {
            pageTotal.textContent = "";
        } else if (hasTotalPages) {
            pageTotal.textContent = "of " + totalPages;
        } else {
            pageTotal.textContent = hasNext ? "" : "last page";
        }
    });
}

function setMediaPaginationButtonState(button, disabled) {
    button.disabled = disabled;
    button.classList.toggle("is-disabled", disabled);
    button.setAttribute("aria-disabled", String(disabled));
}

/*
 * Scroll Top Bar
 */
$(window).on("scroll", function () {
    var scroll = $(window).scrollTop();
    if (scroll < 245) {
        $(".scroll-to-target").removeClass("open");
    } else {
        $(".scroll-to-target").addClass("open");
    }
});

if ($(".scroll-to-target").length) {
    $(".scroll-to-target").on("click", function () {
        var target = $(this).attr("data-target");
        // animate
        $("html, body").animate(
            {
                scrollTop: $(target).offset().top,
            },
            500
        );
    });
}
