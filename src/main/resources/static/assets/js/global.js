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

    /*********************************
    /*  Scroll Top Bar
    *********************************/
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
