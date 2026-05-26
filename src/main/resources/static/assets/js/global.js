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
