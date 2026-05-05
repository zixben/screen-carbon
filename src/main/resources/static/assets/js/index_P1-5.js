$(document).ready(function() {

	$(".moveInput").on("keyup", function(e) {
		const inputValue = $(this).val().trim();
		if (e.key === "Enter" && inputValue.length > 0) {
			window.location.href = server + "/search-results?value=" + inputValue;
		} else if (e.key === "Enter") {
			// If the Enter key was pressed but the input is empty, show an alert
			alert("The input is empty!");
		}
	});

	let popularityData = []; // Array to store items with popularity
	let rankedClimateItems = []; // Items rendered in the first carousel
	let scoreData = []; // Items from server + "/score/getOrderAvg"

	let page = 1; // Initial page for TMDB trending request

	// Show the loading spinner initially for each carousel
	showLoadingIndicator('#highestRankedClimate', 'rankedClimateSpinner');
	showLoadingIndicator('#trendingMoviesCarousel', 'trendingMoviesSpinner');

	// Function to fetch and render the first carousel (Top 20 based on popularity)
	function fetchAndRenderTopRanked() {
		$.ajax({
			url: server + "/score/getOrderAvg",
			method: "get",
			headers: {
				"Authorization": jwt,
				"accept": "application/json",
			},
			success: function(response) {
				scoreData = response; // Store score data for use in both carousels
				const rankedClimateInner = document.querySelector('#highestRankedClimate .carousel-inner');
				let requests = response.map((item) => getPopularity(item)); // Collect promises

				// Wait for all getPopularity requests to complete
				Promise.all(requests).then(() => {
					// Sort the items based on popularity in descending order
					popularityData.sort((a, b) => b.popularity - a.popularity);

					// Render only the first 20 sorted items
					rankedClimateItems = popularityData.slice(0, 20);
					renderItems(rankedClimateInner, rankedClimateItems);

					// Restore the carousel state
					restoreCarouselState('#highestRankedClimate');

					// Hide loading spinner for the first carousel
					hideLoadingIndicator('rankedClimateSpinner');

					// Fetch and render the second carousel (Trending)
					fetchAndRenderTrending();
				});
			},
			error: function(xhr, status, error) {
				console.error("An error occurred: " + status + ", " + error + ", " + xhr);
				hideLoadingIndicator('rankedClimateSpinner'); // Hide the loading indicator if an error occurs
			},
		});
	}
	
	function fetchAndRenderTrending() {
    $.ajax({
        url: `https://api.themoviedb.org/3/trending/all/day?language=en-US&page=${page}`,
        cache: true,
        method: "get",
        headers: {
            "Authorization": jwt,
            "accept": "application/json",
        },
        success: function(response) {
            const trendingItems = response.results; // Get items from the trending response
         

            // Render whatever items were collected, even if less than 20
            renderTrendingItems(trendingItems);
            restoreCarouselState('#trendingMoviesCarousel');
            hideLoadingIndicator('trendingMoviesSpinner'); // Hide the loading indicator once items are rendered
        },
        error: function(xhr, status, error) {
            console.error("An error occurred: " + status + ", " + error);
            hideLoadingIndicator('trendingMoviesSpinner'); // Hide the loading indicator if an error occurs
        },
    });
	}

	// Function to render the collected trending items into the carousel
	function renderTrendingItems(trendingItems) {
		const trendingMoviesInner = document.querySelector('#trendingMoviesCarousel .carousel-inner');
		let firstItem = true;
		let renderedItems = [];

		// Render exactly 20 items
		trendingItems.forEach((item) => {
			let id = item.id;
			let title = item.title || item.name;
			let vote_average = item.score || 0; // Use the score from the backend, not the TMDB vote_average
			let poster_path = item.poster_path;
			let media_type = item.media_type;

			let itemHTML = '';
			
			// Check if the trending item exists in scoreData
        let matchedRankedItem = scoreData.find((rankedItem) => rankedItem.vId === id);

        if (matchedRankedItem) {

			//if (item.isInScoreData) {
				vote_average = matchedRankedItem.score || 0;
				let borderColor = determineBorderColor(vote_average);
				let iconPath = determineIconPath(vote_average);
				let voteAveragePercentage = (vote_average * 10).toString();

				itemHTML = `
                    <div class="carousel-item ${firstItem ? 'active' : ''}">
                        <div class="col-md-3">
                            <div class="card" data-id="${id}" data-type="${media_type}" data-name="${title}">
                                <div class="card-img" style="border-color: ${borderColor};">
                                    <img src="${imgServer + poster_path}" class="img-fluid" alt="movie-img">
                                </div>
                                <div class="card-body">
                                    <p class="card-text"><img class="card-icon" src="${iconPath}">${voteAveragePercentage.substring(0, 5)}%</p>
                                    <h5 class="card-title">${title}</h5>
                                </div>
                            </div>
                        </div>
                    </div>
                `;
			} else {
				// Item is not in the score data
				let borderImageStyle = "style='border: 2px solid black; padding: 10px; background-color: white;'";
				itemHTML = `
                    <div class="carousel-item ${firstItem ? 'active' : ''}">
                        <div class="col-md-3">
                            <div class="card" data-id="${id}" data-type="${media_type}" data-name="${title}">
                                <div class="card-img" ${borderImageStyle}>
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
			}

			renderedItems.push(itemHTML);
			firstItem = false; // Set firstItem to false after the first render
		});

		// Render the collected items to the carousel
		trendingMoviesInner.innerHTML = renderedItems.join('');
		cloneAndAppendItemsForCarousel(trendingMoviesInner); // Ensures display of 4 items at a time
	}

	// Initial fetch and render for the first carousel
	fetchAndRenderTopRanked();

	// Function to fetch popularity data
	function getPopularity(item) {
		return $.ajax({
			url: `https://api.themoviedb.org/3/${item.videoType}/${item.vId}`,
			cache: true,
			method: "get",
			headers: {
				"Authorization": jwt,
				"accept": "application/json",
			},
			success: function(response) {
				if (response) {
					popularityData.push({
						...item,
						popularity: response.popularity || 0, // Use 0 if popularity is undefined
					});
				}
			},
			error: function(xhr, status, error) {
				console.error("An error occurred: " + status + ", " + error + ", " + xhr);
			},
		});
	}

	// Function to render items in the first carousel
	function renderItems(carouselInner, items) {
		let firstItem = true;
		items.forEach((item) => {
			let id = item.vId;
			let title = item.videoName;
			let vote_average = item.score;
			let voteAveragePercentage = (vote_average * 10).toString();
			let poster_path = item.vImg;
			let media_type = item.videoType;
			let borderColor = determineBorderColor(vote_average);
			let iconPath = determineIconPath(vote_average);

			let itemHTML = `
                <div class="carousel-item ${firstItem ? 'active' : ''}">
                    <div class="col-md-3">
                        <div class="card" data-id="${id}" data-type="${media_type}" data-name="${title}">
                            <div class="card-img" style="border-color: ${borderColor};">
                                <img src="${imgServer + poster_path}" class="img-fluid" alt="movie-img">
                            </div>
                            <div class="card-body">
                                <p class="card-text"><img class="card-icon" src="${iconPath}">${voteAveragePercentage.substring(0, 5)}%</p>
                                <h5 class="card-title">${title}</h5>
                            </div>
                        </div>
                    </div>
                </div>
            `;

			carouselInner.innerHTML += itemHTML;
			firstItem = false; // Set firstItem to false after the first render
		});
		cloneAndAppendItemsForCarousel(carouselInner);
		bindImageLoadEvents();
	}

	// Function to show loading indicator for a specific carousel
	function showLoadingIndicator(carouselId, spinnerId) {
		$(carouselId).prepend(`<div id="${spinnerId}"><div class="spinner-border text-success"></div><div>Loading...</div></div>`);
	}

	// Function to hide loading indicator for a specific carousel
	function hideLoadingIndicator(spinnerId) {
		$(`#${spinnerId}`).remove();
	}

	// Function to restore the carousel state without triggering the slide animation
	function restoreCarouselState(carouselId) {
		var activeIndex = localStorage.getItem(carouselId + '_activeIndex');
		if (activeIndex !== null) {
			$(carouselId + ' .carousel-item')
				.eq(parseInt(activeIndex))
				.addClass('active')
				.siblings()
				.removeClass('active');
		}
	}

	// Function to save the carousel state (index of the active item)
	function saveCarouselState(carouselId) {
		$(carouselId).on('slid.bs.carousel', function() {
			var activeItemIndex = $(carouselId + ' .carousel-item.active').index();
			localStorage.setItem(carouselId + '_activeIndex', activeItemIndex);
		});
	}

	// Function to determine border color based on rating
	function determineBorderColor(vote_average) {
		if (vote_average >= 8) return '#669900'; // Green
		else if (vote_average >= 6) return '#aec000'; // Light green
		else if (vote_average >= 4) return '#ff9900'; // Orange
		else if (vote_average >= 2) return '#cc0100'; // Red
		else return '#808080'; // Grey
	}

	// Function to determine icon path based on rating
	function determineIconPath(vote_average) {
		if (vote_average >= 8)
			return "assets/images/ranking_icons/ICONS_0000_Green.png";
		else if (vote_average >= 6)
			return "assets/images/ranking_icons/ICONS_0001_LightGreen.png";
		else if (vote_average >= 4)
			return "assets/images/ranking_icons/ICONS_0002_Orange.png";
		else if (vote_average >= 2)
			return "assets/images/ranking_icons/ICONS_0003_Red.png";
		else return "assets/images/ranking_icons/ICONS_0004_Grey.png";
	}

	// Function to bind image load events
	function bindImageLoadEvents() {
		$('.carousel-inner img')
			.on('load', function() { })
			.each(function() {
				if (this.complete) $(this).trigger('load');
			});
	}

	// Function to clone and append items for a seamless carousel
	function cloneAndAppendItemsForCarousel(carouselInner) {
		let items = carouselInner.querySelectorAll('.carousel-item');
		items.forEach((el) => {
			const minPerSlide = 4;
			let next = el.nextElementSibling;
			for (var i = 1; i < minPerSlide; i++) {
				if (!next) {
					next = items[0];
				}
				let cloneChild = next.cloneNode(true);
				el.appendChild(cloneChild.children[0]);
				next = next.nextElementSibling;
			}
		});
	}

	// Bind event listeners to save each carousel's state after it has been initialized
	saveCarouselState('#trendingMoviesCarousel');
	saveCarouselState('#highestRankedClimate');

	// Event delegation for handling clicks on the carousel items
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
