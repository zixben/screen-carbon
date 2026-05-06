let server = ""
let jwt = ""
let imgServer = "https://image.tmdb.org/t/p/original/"
let imgServer2 = "https://image.tmdb.org/t/p/w500/"

const tmdbApiBase = "https://api.themoviedb.org/3"

function tmdbUrl(url) {
    let normalizedUrl = url.trim()
    if (normalizedUrl.startsWith(tmdbApiBase)) {
        return server + "/tmdb" + normalizedUrl.substring(tmdbApiBase.length)
    }
    return server + "/tmdb" + (normalizedUrl.startsWith("/") ? normalizedUrl : "/" + normalizedUrl)
}

if (window.jQuery) {
    const originalAjax = window.jQuery.ajax
    window.jQuery.ajax = function(urlOrOptions, options) {
        const ajaxOptions = typeof urlOrOptions === "string"
            ? Object.assign({}, options, { url: urlOrOptions })
            : Object.assign({}, urlOrOptions)

        if (typeof ajaxOptions.url === "string" && ajaxOptions.url.trim().startsWith(tmdbApiBase)) {
            ajaxOptions.url = tmdbUrl(ajaxOptions.url)
            if (ajaxOptions.headers) {
                delete ajaxOptions.headers.Authorization
            }
        }

        return originalAjax.call(this, ajaxOptions)
    }
}
