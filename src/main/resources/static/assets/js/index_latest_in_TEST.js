$(document).ready(function () {
    $(".moveInput").on("keyup", function (e) {
        const inputValue = $(this).val().trim();
        if (e.key === "Enter" && inputValue.length > 0) {
            window.location.href = server + "/search-results?value=" + inputValue;
        } else if (e.key === "Enter") {
            alert("The input is empty!");
        }
    });

    let popularityData = [];
    let rankedClimateItems = [];
    let scoreData = [];
    let collectedTrendingItems = [];
    let page = 1;

    showLoadingIndicator('#highestRankedClimate', 'rankedClimateSpinner');
    showLoadingIndicator('#trendingMoviesCarousel', 'trendingMoviesSpinner');

    const ONE_WEEK_IN_MS = 7 * 24 * 60 * 60 * 1000;
    const POPULARITY_CACHE_KEY = "popularityData";
    const POPULARITY_CACHE_TIMESTAMP_KEY = "popularityDataTimestamp";

    function fetchAndRenderTopRanked() {
        fetchScoreData().then((response) => {
            scoreData = response;
            const rankedClimateInner = document.querySelector('#highestRankedClimate .carousel-inner');

            getPopularityDataWeekly().then((cachedPopularityData) => {
                popularityData = cachedPopularityData;
                rankedClimateItems = popularityData.slice(0, 20);
                renderItems(rankedClimateInner, rankedClimateItems);
                restoreCarouselState('#highestRankedClimate');
                hideLoadingIndicator('rankedClimateSpinner');
                fetchAndRenderTrending();
            }).catch((error) => {
                console.error("An error occurred while fetching popularity data:", error);
                hideLoadingIndicator('rankedClimateSpinner');
            });
        }).catch((error) => {
            console.error("An error occurred while fetching score data:", error);
            hideLoadingIndicator('rankedClimateSpinner');
        });
    }

    function fetchAndRenderTrending() {
        $.ajax({
            url: `https://api.themoviedb.org/3/trending/all/day?language=en-US&page=${page}`,
            cache: false,
            method: "get",
            headers: {
                "Authorization": jwt,
                "accept": "application/json",
            },
            success: function (response) {
                processTrendingItems(response.results);
                if (collectedTrendingItems.length < 20) {
                    page++;
                    fetchAndRenderTrending();
                } else {
                    renderTrendingItems();
                    restoreCarouselState('#trendingMoviesCarousel');
                    hideLoadingIndicator('trendingMoviesSpinner');
                }
            },
            error: function (xhr, status, error) {
                console.error("An error occurred while fetching trending items:", status, error);
                hideLoadingIndicator('trendingMoviesSpinner');
            },
        });
    }

    function processTrendingItems(trendingItems) {
        trendingItems.forEach((item) => {
            let id = item.id;
            let isRanked = rankedClimateItems.some((rankedItem) => rankedItem.vId === id);

            if (isRanked) return;

            let scoreItem = scoreData.find((score) => score.vId === id);

            if (!collectedTrendingItems.some((collectedItem) => collectedItem.id === id)) {
                collectedTrendingItems.push({
                    ...item,
                    score: scoreItem ? scoreItem.score : null,
                    isInScoreData: !!scoreItem,
                });
            }
        });
    }

    function renderTrendingItems() {
        const trendingMoviesInner = document.querySelector('#trendingMoviesCarousel .carousel-inner');
        let firstItem = true;
        let renderedItems = [];

        collectedTrendingItems.slice(0, 20).forEach((item) => {
            let id = item.id;
            let title = item.title || item.name;
            let vote_average = item.score || 0;
            let poster_path = item.poster_path;
            let media_type = item.media_type;

            let itemHTML = item.isInScoreData
                ? `
                    <div class="carousel-item ${firstItem ? 'active' : ''}">
                        <div class="col-md-3">
                            <div class="card" data-id="${id}" data-type="${media_type}" data-name="${title}">
                                <div class="card-img" style="border-color: ${determineBorderColor(vote_average)};">
                                    <img src="${imgServer + poster_path}" class="img-fluid" alt="movie-img">
                                </div>
                                <div class="card-body">
                                    <p class="card-text"><img class="card-icon" src="${determineIconPath(vote_average)}">${(vote_average * 10).toFixed(1)}%</p>
                                    <h5 class="card-title">${title}</h5>
                                </div>
                            </div>
                        </div>
                    </div>
                `
                : `
                    <div class="carousel-item ${firstItem ? 'active' : ''}">
                        <div class="col-md-3">
                            <div class="card" data-id="${id}" data-type="${media_type}" data-name="${title}">
                                <div class="card-img" style="border: 2px solid black; padding: 10px; background-color: white;">
                                    <img src="${imgServer + poster_path}" class="img-fluid" alt="movie-img">
                                </div>
                                <div class="card-body">
                                    <p class="card-text">Not yet rated</p>
                                    <h5 class="card-title">${title}</h5>
                                </div>
                            </div>
                        </div>
                    </div>
                `;

            renderedItems.push(itemHTML);
            firstItem = false;
        });

        trendingMoviesInner.innerHTML = renderedItems.join('');
        cloneAndAppendItemsForCarousel(trendingMoviesInner);
    }

    function getPopularityDataWeekly() {
        return new Promise((resolve, reject) => {
            const cachedData = localStorage.getItem(POPULARITY_CACHE_KEY);
            const cachedTimestamp = localStorage.getItem(POPULARITY_CACHE_TIMESTAMP_KEY);

            if (cachedData && cachedTimestamp && Date.now() - cachedTimestamp < ONE_WEEK_IN_MS) {
                resolve(JSON.parse(cachedData));
            } else {
                let requests = scoreData.map((item) => getPopularity(item));
                Promise.all(requests).then((fetchedData) => {
                    localStorage.setItem(POPULARITY_CACHE_KEY, JSON.stringify(fetchedData));
                    localStorage.setItem(POPULARITY_CACHE_TIMESTAMP_KEY, Date.now());
                    resolve(fetchedData);
                }).catch((error) => {
                    reject(error);
                });
            }
        });
    }

    function getPopularity(item) {
        return new Promise((resolve, reject) => {
            $.ajax({
                url: `https://api.themoviedb.org/3/${item.videoType}/${item.vId}`,
                cache: false,
                method: "get",
                headers: {
                    "Authorization": jwt,
                    "accept": "application/json",
                },
            })
                .done((response) => {
                    resolve({
                        ...item,
                        popularity: response.popularity || 0,
                    });
                })
                .fail((xhr, status, error) => {
                    console.error("Error fetching popularity data:", status, error);
                    reject(error);
                });
        });
    }

    function fetchScoreData() {
        return fetchAndCache(server + "/score/getOrderAvg", "scoreDataCache", 3600);
    }

    function fetchAndCache(url, cacheKey, expiry = 3600) {
        const cachedData = localStorage.getItem(cacheKey);
        const cachedTime = localStorage.getItem(`${cacheKey}_time`);

        if (cachedData && cachedTime && Date.now() - cachedTime < expiry * 1000) {
            return Promise.resolve(JSON.parse(cachedData));
        }

        return $.ajax({
            url,
            method: "get",
            headers: { "Authorization": jwt, "accept": "application/json" },
        }).then((data) => {
            localStorage.setItem(cacheKey, JSON.stringify(data));
            localStorage.setItem(`${cacheKey}_time`, Date.now());
            return data;
        });
    }

    function renderItems(carouselInner, items) {
        let firstItem = true;
        items.forEach((item) => {
            let id = item.vId;
            let title = item.videoName;
            let vote_average = item.score;
            let poster_path = item.vImg;
            let media_type = item.videoType;

            const itemHTML = `
                <div class="carousel-item ${firstItem ? 'active' : ''}">
                    <div class="col-md-3">
                        <div class="card" data-id="${id}" data-type="${media_type}" data-name="${title}">
                            <div class="card-img" style="border-color: ${determineBorderColor(vote_average)};">
                                <img src="${imgServer + poster_path}" class="img-fluid" alt="movie-img">
                            </div>
                            <div class="card-body">
                                <p class="card-text"><img class="card-icon" src="${determineIconPath(vote_average)}">${(vote_average * 10).toFixed(1)}%</p>
                                <h5 class="card-title">${title}</h5>
                            </div>
                        </div>
                    </div>
                </div>
            `;

            carouselInner.innerHTML += itemHTML;
            firstItem = false;
        });
        cloneAndAppendItemsForCarousel(carouselInner);
        bindImageLoadEvents();
    }

    function showLoadingIndicator(carouselId, spinnerId) {
        $(carouselId).prepend(`<div id="${spinnerId}"><div class="spinner-border text-success"></div><div>Loading...</div></div>`);
    }

    function hideLoadingIndicator(spinnerId) {
        $(`#${spinnerId}`).remove();
    }

    function restoreCarouselState(carouselId) {
        const activeIndex = localStorage.getItem(carouselId + '_activeIndex');
        if (activeIndex !== null) {
            $(carouselId + ' .carousel-item')
                .eq(parseInt(activeIndex))
                .addClass('active')
                .siblings()
                .removeClass('active');
        }
    }

    function saveCarouselState(carouselId) {
        $(carouselId).on('slid.bs.carousel', function () {
            const activeItemIndex = $(carouselId + ' .carousel-item.active').index();
            localStorage.setItem(carouselId + '_activeIndex', activeItemIndex);
        });
    }

    function determineBorderColor(vote_average) {
        if (vote_average >= 8) return '#669900'; // Green
        else if (vote_average >= 6) return '#aec000'; // Light green
        else if (vote_average >= 4) return '#ff9900'; // Orange
        else if (vote_average >= 2) return '#cc0100'; // Red
        else return '#808080'; // Grey
    }

    function determineIconPath(vote_average) {
        if (vote_average >= 8) return "assets/images/ranking_icons/ICONS_0000_Green.png";
        else if (vote_average >= 6) return "assets/images/ranking_icons/ICONS_0001_LightGreen.png";
        else if (vote_average >= 4) return "assets/images/ranking_icons/ICONS_0002_Orange.png";
        else if (vote_average >= 2) return "assets/images/ranking_icons/ICONS_0003_Red.png";
        else return "assets/images/ranking_icons/ICONS_0004_Grey.png";
    }

    function cloneAndAppendItemsForCarousel(carouselInner) {
        const items = carouselInner.querySelectorAll('.carousel-item');
        items.forEach((el) => {
            const minPerSlide = 4;
            let next = el.nextElementSibling;
            for (let i = 1; i < minPerSlide; i++) {
                if (!next) next = items[0];
                const cloneChild = next.cloneNode(true);
                el.appendChild(cloneChild.children[0]);
                next = next.nextElementSibling;
            }
        });
    }

    fetchAndRenderTopRanked();

    saveCarouselState('#trendingMoviesCarousel');
    saveCarouselState('#highestRankedClimate');
	
	// Function to bind image load events
	function bindImageLoadEvents() {
		$('.carousel-inner img')
			.on('load', function() { })
			.each(function() {
				if (this.complete) $(this).trigger('load');
			});
	}
	
    $('.carousel-inner').on('click', '.card', function() {
        let id = $(this).data('id');
        let type = $(this).data('type');

        if (type === "tv") {
            window.location.href = server + `/tv?id=${id}&type=tv`;
        } else if (type === "movie") {
            window.location.href = server + `/movie?id=${id}&type=movie`;
        }
    });
});
