/*$(".moveInput").on("keyup", function(e) {
	// Check if the key pressed is the Enter key and the input is not empty
	if (e.key === "Enter" && $(this).val().length > 0) {
		// Redirect to the search page with the input value as a query parameter
		window.location.href = server + "/search-results?value=" + $(this).val();
	} else if (e.key === "Enter") {
		// If the Enter key was pressed but the input is empty, show an alert
		alert("The input is empty!");
	}
});
*/
$(document).ready(function() {

	$(".moveInput").on("keyup", function(e) {
		const inputValue = $(this).val().trim();
		if (e.key === "Enter" && inputValue.length > 0) {
			redirectToSearch(inputValue);
		} else if (e.key === "Enter") {
			// If the Enter key was pressed but the input is empty, show an alert
			redirectToSearch(inputValue);
		}
	});

	// Function to restore the carousel state without triggering the slide animation
	function restoreCarouselState(carouselId) {
		var activeIndex = localStorage.getItem(carouselId + '_activeIndex');
		if (activeIndex !== null) {
			// Directly add 'active' class to the saved index item without using carousel methods to avoid triggering animation
			$(carouselId + ' .carousel-item').eq(parseInt(activeIndex)).addClass('active').siblings().removeClass('active');
		}
	}

	// Function to save the carousel state (index of the active item)
	function saveCarouselState(carouselId) {
		$(carouselId).on('slid.bs.carousel', function() {
			var activeItemIndex = $(carouselId + ' .carousel-item.active').index();
			localStorage.setItem(carouselId + '_activeIndex', activeItemIndex);
		});
	}

	// Restore carousel states for your carousels immediately before any other interactions
	//restoreCarouselState('#trendingMoviesCarousel');
	//restoreCarouselState('#highestRankedClimate');


	var climateMovies = [];

	// AJAX call for the Highest Ranked Climate-friendly Films & TV Shows
	$.ajax({
		url: server + "/score/getTop20Popularity",
		method: "get",
		headers: {
			"Authorization": jwt,
			"accept": "application/json"
		},
		success: function(response) {
			//console.log(response);
			climateMovies = response;
			const rankedClimateInner = document.querySelector('#highestRankedClimate .carousel-inner');
			updateCarousel(rankedClimateInner, climateMovies, true);
			restoreCarouselState('#highestRankedClimate');
		},
		error: function(xhr, status, error) {
			console.error("An error occurred: " + status + ", " + error + ", " + xhr);
		}
	});

	// AJAX call for the trending movies carousel
	$.ajax({
		url: "https://api.themoviedb.org/3/trending/all/day?language=en-US",
		cache: false,
		method: "get",
		headers: {
			"Authorization": jwt,
			"accept": "application/json"
		},
		success: function(response) {
			//console.log(response);
			const trendingMoviesInner = document.querySelector('#trendingMoviesCarousel .carousel-inner');  
			updateCarousel(trendingMoviesInner, response.results, false);
			restoreCarouselState('#trendingMoviesCarousel');
		},
		error: function(xhr, status, error) {
			console.error("An error occurred: " + status + ", " + error);
		}
	});

	// Function to update the carousel with fetched movies
	function updateCarousel(carouselInner, movies, isCustomData) {
		if (!carouselInner) {
			return;
		}

		carouselInner.textContent = "";
		let firstItem = true;
		movies.forEach((movie, index) => {
			// Process movie data depending on the source
			let id, media_type, title, vote_average, poster_path;
			if (isCustomData) {
				// Handling custom server data
				id = safePositiveInteger(movie.vId);
				title = movie.videoName || "";
				vote_average = Number(movie.score);
				poster_path = movie.vImg;
				media_type = safeVideoType(movie.videoType);

			} else {
				// Handling TMDb API data
				id = safePositiveInteger(movie.id);
				media_type = movie.media_type === "tv" ? "tv" : movie.media_type === "movie" ? "movie" : "person";
				title = movie.title || movie.name || "";
				vote_average = Number(movie.vote_average);
				poster_path = movie.poster_path;

			}

			if (id === null || !media_type) {
				return;
			}

			// Determine border color and icon path
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
			const image = createImageElement(posterUrl, "movie-img", { className: "img-fluid" });
			if (image) {
				cardImage.appendChild(image);
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
		// After updating the carousel, bind load event to new images
		bindImageLoadEvents();
	}

	// Bind event listeners to save each carousel's state after it has been initialized
	saveCarouselState('#trendingMoviesCarousel');
	saveCarouselState('#highestRankedClimate');

	// New function to bind load events to images and log their height
	function bindImageLoadEvents() {
		// Select all images within the carousel and bind load event
		$('.carousel-inner img').on('load', function() {
			// Log the image's height and the data-id of the enclosing card
			//let cardId = $(this).closest('.card').data('id'); // Get the data-id attribute
			//console.log('Image with ID ' + cardId + ' loaded with height: ' + $(this).height());
		}).each(function() {
			// If the image is already loaded (from cache), trigger the load event manually
			if (this.complete) $(this).trigger('load');
		});
	}

	// Function to check if the movie is in the database
	/*function checkMovieInDatabase(movieId, callback) {
		$.ajax({
			url: server + "/{id}",
			method: "get",
			data: { id: movieId },
			success: function(response) {
				callback(response.isInDatabase);  // Expecting response in the form of { isInDatabase: true/false }
			},
			error: function(xhr, status, error) {
				console.error("An error occurred during the database check: " + error + " " + xhr + " " + status);
				callback(false);  // Assume not in database if there's an error
			}
		}); 
	}*/


	// Function to determine border color based on rating
	function determineBorderColor(vote_average) {
		if (vote_average >= 8) return '#669900'; // Green
		else if (vote_average >= 6) return '#aec000'; // Old Yellow '#ffcc02'; Now Light green
		else if (vote_average >= 4) return '#ff9900'; // Orange
		else if (vote_average >= 2) return '#cc0100'; // Old_Brown '#9a6601'; Now Red
		else return '#808080';  // Old_red '#cc3401'; // Now Grey
	}

	// Function to determine icon path based on rating
	function determineIconPath(vote_average) {
		if (vote_average >= 8) return 'assets/images/ranking_icons/ICONS_0000_Green.png';
		else if (vote_average >= 6) return 'assets/images/ranking_icons/ICONS_0001_LightGreen.png';
		else if (vote_average >= 4) return 'assets/images/ranking_icons/ICONS_0002_Orange.png';
		else if (vote_average >= 2) return 'assets/images/ranking_icons/ICONS_0003_Red.png';
		else return 'assets/images/ranking_icons/ICONS_0004_Grey.png';
	}

	// Function to clone and append items for a seamless carousel
	function cloneAndAppendItemsForCarousel(carouselInner) {
		let items = carouselInner.querySelectorAll('.carousel-item');
		items.forEach((el) => {
			const minPerSlide = 4;
			let next = el.nextElementSibling;
			for (var i = 1; i < minPerSlide; i++) {
				if (!next) {
					// Wrap carousel by using first child
					next = items[0];
				}
				let cloneChild = next.cloneNode(true); // Clone the next item
				el.appendChild(cloneChild.children[0]); // Append cloned item to the current item
				next = next.nextElementSibling;
			}
		});
	}


	// Delegate click event from .carousel-inner to each carousel-item
	$('.carousel-inner').on('click', '.card', function() {

		let id = safePositiveInteger($(this).data('id'));
		//let name = $(this).data('name');
		let type = $(this).data('type');

		if (id === null) {
			return;
		}

		if (type === "tv") {
			window.location.href = server + `/tv?id=${id}&type=tv`;
		} else if (type === "movie") {
			window.location.href = server + `/movie?id=${id}&type=movie`;
		}

		/*if (type == 'undefined') {
			//console.log(name);

			// First, try to get it as a movie
			$.ajax({
				url: "https://api.themoviedb.org/3/movie/" + id,
				method: "get",
				headers: {
					"Authorization": jwt,
					"accept": "application/json"
				},
				success: function(movieResponse) {
					let movieName = movieResponse.title || movieResponse.name;
					// If the request is successful, compare the title to confirm it's the correct type
					if (movieName === name) {
						// If the name matches, it's a movie
						window.location.href = server + "/movie?id=" + id + "&type=movie";
					} else {
						// If the name does not match, try to get it as a TV show
						checkTvShow(id, name);
					}
				},
				error: function() {
					// If the request fails, directly check if it's a TV show
					checkTvShow(id, name);
				}
			});

			function checkTvShow(id, name) {
				$.ajax({
					url: "https://api.themoviedb.org/3/tv/" + id,
					method: "get",
					headers: {
						"Authorization": jwt,
						"accept": "application/json"
					},
					success: function(tvResponse) {
						let tvName = tvResponse.title || tvResponse.name;
						// If the request is successful, compare the name to confirm it's the correct type
						if (tvName === name) {
							// If the name matches, it's a TV show
							window.location.href = server + "/tv?id=" + id + "&type=tv";
						} else {
							// If neither matched correctly, log an error or handle accordingly
							console.error("Mismatch: Neither movie nor TV show matches the stored name.");
						}
					},
					error: function() {
						// If the TV show request also fails, log an error or handle accordingly
						console.error("Error: The ID does not correspond to a known movie or TV show.");
					}
				});
			}

		} else {
			if (type === "tv") {
				window.location.href = server + `/tv?id=${id}&type=tv`;
			} else if (type === "movie") {
				window.location.href = server + `/movie?id=${id}&type=movie`;
			}
		}*/

	});


});
