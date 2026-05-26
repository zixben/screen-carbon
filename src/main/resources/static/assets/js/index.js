$(document).ready(function() {

	$(".moveInput").on("keyup", function(e) {
		const inputValue = $(this).val().trim();
		if (e.key === "Enter" && inputValue.length > 0) {
			redirectToSearch(inputValue);
		} else if (e.key === "Enter") {
			redirectToSearch(inputValue);
		}
	});

	function restoreCarouselState(carouselId) {
		var activeIndex = localStorage.getItem(carouselId + '_activeIndex');
		if (activeIndex !== null) {
			$(carouselId + ' .carousel-item').eq(parseInt(activeIndex)).addClass('active').siblings().removeClass('active');
		}
	}

	function saveCarouselState(carouselId) {
		$(carouselId).on('slid.bs.carousel', function() {
			var activeItemIndex = $(carouselId + ' .carousel-item.active').index();
			localStorage.setItem(carouselId + '_activeIndex', activeItemIndex);
		});
	}

	var climateMovies = [];
	loadClimateMovies();

	function loadClimateMovies() {
		$.ajax({
			url: server + "/score/getTop20Popularity",
			method: "get",
			headers: {
				"Authorization": jwt,
				"accept": "application/json"
			},
			success: function(response) {
				climateMovies = Array.isArray(response) ? response : [];
				const rankedClimateInner = document.querySelector('#highestRankedClimate .carousel-inner');
				updateCarousel(rankedClimateInner, climateMovies, true);
				restoreCarouselState('#highestRankedClimate');
				loadTrendingMovies();
			},
			error: function(xhr, status, error) {
				console.error("An error occurred: " + status + ", " + error + ", " + xhr);
				climateMovies = [];
				loadTrendingMovies();
			}
		});
	}

	function loadTrendingMovies() {
		$.ajax({
			url: "https://api.themoviedb.org/3/trending/all/day?language=en-US",
			cache: false,
			method: "get",
			headers: {
				"Authorization": jwt,
				"accept": "application/json"
			},
			success: function(response) {
				const trendingMoviesInner = document.querySelector('#trendingMoviesCarousel .carousel-inner');
				updateCarousel(trendingMoviesInner, filterSafeTmdbResults(response.results), false);
				restoreCarouselState('#trendingMoviesCarousel');
			},
			error: function(xhr, status, error) {
				console.error("An error occurred: " + status + ", " + error);
			}
		});
	}

	function updateCarousel(carouselInner, movies, isCustomData) {
		if (!carouselInner) {
			return;
		}

		carouselInner.textContent = "";
		let firstItem = true;
		movies.forEach((movie, index) => {
			let id, media_type, title, vote_average, poster_path;
			if (isCustomData) {
				id = safePositiveInteger(movie.vId);
				title = movie.videoName || "";
				vote_average = Number(movie.score);
				poster_path = movie.vImg;
				media_type = safeVideoType(movie.videoType);

			} else {
				if (!isSafeTmdbMedia(movie)) {
					return;
				}
				id = safePositiveInteger(movie.id);
				media_type = movie.media_type === "tv" ? "tv" : movie.media_type === "movie" ? "movie" : "person";
				title = movie.title || movie.name || "";
				vote_average = Number(movie.vote_average);
				poster_path = movie.poster_path;

			}

			if (id === null || !media_type) {
				return;
			}

			const matchedClimateMovie = climateMovies.find(m => (Number(m.vId) === id && m.videoName === title));
			if (matchedClimateMovie) {
				vote_average = Number(matchedClimateMovie.score);
			}
			const isRated = (isCustomData || Boolean(matchedClimateMovie)) && Number.isFinite(vote_average);
			const voteAveragePercentage = isRated ? (vote_average * 10).toFixed(1).replace(/\.0$/, "") : "";
			const posterUrl = isCustomData ? safeTmdbStoredImageUrl(poster_path) : safeTmdbImageUrl(poster_path);

			const carouselItem = document.createElement("div");
			carouselItem.className = "carousel-item" + (firstItem ? " active" : "");
			const column = document.createElement("div");
			column.className = "col-md-3";
			const card = document.createElement("div");
			card.className = "card";
			card.dataset.id = String(id);
			card.dataset.type = media_type;
			card.dataset.name = title;

			const cardImage = document.createElement("div");
			cardImage.className = "card-img";
			if (isRated) {
				cardImage.style.borderColor = determineBorderColor(vote_average);
			} else {
				cardImage.style.border = "2px solid black";
				cardImage.style.padding = "10px";
				cardImage.style.backgroundColor = "white";
			}
			const placeholderOptions = {
				className: "img-fluid",
				placeholderLabel: "No poster",
				placeholderIcon: media_type === "tv" ? "bi bi-tv" : "bi bi-film",
				placeholderClassName: "media-placeholder--poster media-placeholder--carousel"
			};
			const image = createImageElement(posterUrl, title ? "Poster for " + title : "Media poster", placeholderOptions);
			if (image) {
				cardImage.appendChild(image);
			} else {
				cardImage.appendChild(createImagePlaceholder(placeholderOptions.placeholderLabel, placeholderOptions));
			}

			const cardBody = document.createElement("div");
			cardBody.className = "card-body";
			const ratingText = document.createElement("p");
			ratingText.className = "card-text";
			if (isRated) {
				const icon = createImageElement(determineIconPath(vote_average), "rating icon", { className: "card-icon" });
				if (icon) {
					ratingText.appendChild(icon);
				}
				ratingText.appendChild(document.createTextNode(voteAveragePercentage + "%"));
			} else {
				ratingText.textContent = "Not yet rated";
			}
			const titleElement = document.createElement("h5");
			titleElement.className = "card-title";
			titleElement.textContent = title;

			cardBody.appendChild(ratingText);
			cardBody.appendChild(titleElement);
			card.appendChild(cardImage);
			card.appendChild(cardBody);
			column.appendChild(card);
			carouselItem.appendChild(column);
			carouselInner.appendChild(carouselItem);
			firstItem = false;
		});

		cloneAndAppendItemsForCarousel(carouselInner);
	}

	saveCarouselState('#trendingMoviesCarousel');
	saveCarouselState('#highestRankedClimate');

	function determineBorderColor(vote_average) {
		if (vote_average >= 8) return '#669900';
		else if (vote_average >= 6) return '#aec000';
		else if (vote_average >= 4) return '#ff9900';
		else if (vote_average >= 2) return '#cc0100';
		else return '#808080';
	}

	function determineIconPath(vote_average) {
		if (vote_average >= 8) return 'assets/images/ranking_icons/ICONS_0000_Green.png';
		else if (vote_average >= 6) return 'assets/images/ranking_icons/ICONS_0001_LightGreen.png';
		else if (vote_average >= 4) return 'assets/images/ranking_icons/ICONS_0002_Orange.png';
		else if (vote_average >= 2) return 'assets/images/ranking_icons/ICONS_0003_Red.png';
		else return 'assets/images/ranking_icons/ICONS_0004_Grey.png';
	}

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


	$('.carousel-inner').on('click', '.card', function() {

		let id = safePositiveInteger($(this).data('id'));
		let type = $(this).data('type');

		if (id === null) {
			return;
		}

		if (type === "tv") {
			window.location.href = server + `/tv?id=${id}&type=tv`;
		} else if (type === "movie") {
			window.location.href = server + `/movie?id=${id}&type=movie`;
		}

	});


});
