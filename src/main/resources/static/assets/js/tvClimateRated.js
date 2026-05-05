$(document).ready(function() {
	loadClimateRatedOptions();
	const pageSize = 20;
	const defaultPage = 1;
	const defaultSortValue = "avg_desc";
	const defaultSortUrl = server + "/score/getTVAvgDesc";

	function fetchVideos(url, page) {
		const offset = (page - 1) * pageSize;
		const country = window.sessionStorage.getItem("selectedCountry") || "";
		const genre = window.sessionStorage.getItem("selectedGenre") || "";
		const year = window.sessionStorage.getItem("selectedYear") || "";

		$.ajax({
			url: url,
			method: 'GET',
			data: {
				limit: pageSize,
				offset: offset,
				country: country,
				genre: genre,
				year: year
			},
			success: function(response) {
				if (response && response.length) {

					renderVideos(response);
				} else {
					$("#tv-shows").html('<p>No results found</p>');
				}
				updatePageNumber(page);
			},
			error: function(error) {
				console.error("Error fetching videos:", error);
			}
		});
	}

	function renderVideos(videos) {
		let html = '';
		videos.forEach(function(video) {
			const borderColor = determineBorderColor(video.score);
			const iconPath = determineIconPath(video.score);
			const voteAveragePercentage = (video.score * 10).toFixed(1);
			const title = video.videoName;

			html += `
                <div onclick='toDesc(${video.vId})' class="videoCar">
                    <div class="VideoImage" style="border-color: ${borderColor};">
                        <img alt='image' src='${video.vImg}'>
                    </div>
                    <div>
                        <p>
                            <span><img class="card-icon" src='${iconPath}'></span>
                            <span>${voteAveragePercentage}%</span>
                        </p>
                        <h5>${title}</h5>
                    </div>
                </div>`;
		});

		$("#tv-shows").html(html);
	}

	function determineBorderColor(vote_average) {
		if (vote_average >= 8) return '#669900'; // Green
		else if (vote_average >= 6) return '#aec000'; // Light green
		else if (vote_average >= 4) return '#ff9900'; // Orange
		else if (vote_average >= 2) return '#cc0100'; // Red
		else return '#808080';  // Grey
	}

	function determineIconPath(vote_average) {
		if (vote_average >= 8) return 'assets/images/ranking_icons/ICONS_0000_Green.png';
		else if (vote_average >= 6) return 'assets/images/ranking_icons/ICONS_0001_LightGreen.png';
		else if (vote_average >= 4) return 'assets/images/ranking_icons/ICONS_0002_Orange.png';
		else if (vote_average >= 2) return 'assets/images/ranking_icons/ICONS_0003_Red.png';
		else return 'assets/images/ranking_icons/ICONS_0004_Grey.png';
	}

	// Event listener for genre selection
	$('.genre-select').change(function() {
		const selectedGenre = $(this).val();

		window.sessionStorage.setItem("selectedGenre", selectedGenre);
		window.sessionStorage.setItem("tvNumPage", defaultPage);
		handleDropdownChange();
	});

	// Event listener for year selection
	$('.year-select').change(function() {
		const selectedYear = $(this).val();

		window.sessionStorage.setItem("selectedYear", selectedYear);
		window.sessionStorage.setItem("tvNumPage", defaultPage);
		handleDropdownChange();
	});

	function handleDropdownChange() {
		const selectedOption = $('.sort-select').val() || defaultSortValue;
		let url = defaultSortUrl;

		switch (selectedOption) {
			case 'avg_desc':
				url = server + "/score/getTVAvgDesc";
				break;
			case 'avg_asc':
				url = server + "/score/getTVAvgAsc";
				break;
			case 'score_count_desc':
				url = server + "/score/getTVScoreCountDesc";
				break;
			case 'score_count_asc':
				url = server + "/score/getTVScoreCountAsc";
				break;
		}

		const page = Number(window.sessionStorage.getItem("tvNumPage")) || defaultPage;
		window.sessionStorage.setItem("sortValue", selectedOption);
		window.sessionStorage.setItem("sortUrl", url);

		fetchVideos(url, page);
	}

	function updatePageNumber(page) {
		$('#pageNum').text(page);
	}

	function prePage() {
		let num = Number(window.sessionStorage.getItem("tvNumPage")) || defaultPage;
		if (num > 1) {
			num -= 1;
			window.sessionStorage.setItem("tvNumPage", num);
			handleDropdownChange();
		}
	}

	function nextPage() {
		let num = Number(window.sessionStorage.getItem("tvNumPage")) || defaultPage;
		num += 1;
		window.sessionStorage.setItem("tvNumPage", num);
		handleDropdownChange();
	}

	$('.sort-select').change(function() {
		window.sessionStorage.setItem("tvNumPage", defaultPage);
		handleDropdownChange();
	});

	$('.page-item').click(function() {
		const action = $(this).data('action');
		if (action === 'prev') {
			prePage();
		} else if (action === 'next') {
			nextPage();
		}
	});

	// Event listener for country selection
	$('.country-select').change(function() {
		const selectedCountry = $(this).val();

		window.sessionStorage.setItem("selectedCountry", selectedCountry);
		window.sessionStorage.setItem("tvNumPage", defaultPage);
		handleDropdownChange();
	});

	function initialize() {
		const page = Number(window.sessionStorage.getItem("tvNumPage")) || defaultPage;
		const url = window.sessionStorage.getItem("sortUrl") || defaultSortUrl;
		const country = window.sessionStorage.getItem("selectedCountry") || "";
		const genre = window.sessionStorage.getItem("selectedGenre") || "";
		const year = window.sessionStorage.getItem("selectedYear") || "";
		const sortValue = window.sessionStorage.getItem("sortValue") || defaultSortValue;

		$('.sort-select').val(sortValue);
		$('.country-select').val(country);
		$('.genre-select').val(genre);
		$('.year-select').val(year);

		fetchVideos(url, page);
	}

	initialize();
});

function toDesc(id) {
	window.location.href = server + "/tv?id=" + id + "&type=tv";
}

$(".moveInput").on("keyup", function(e) {
	const inputValue = $(this).val().trim();
	const videoType = "tv";

	if (e.key === "Enter" && inputValue.length > 0) {
		window.location.href = server + "/search-results?value=" + inputValue + "&type=" + videoType;
	} else if (e.key === "Enter") {
		// If the Enter key was pressed but the input is empty, show an alert
		alert("The input is empty!");
	}
});

function loadClimateRatedOptions() {
	let climateRatedOptions = `
        <select class="form-select sort-select" aria-label="Default select example">
            <option value="avg_desc" selected>Sort</option>
            <option value="avg_asc">Rating average (asc)</option>
            <option value="avg_desc">Rating average (desc)</option>
            <option value="score_count_asc">Number of rates on each TV show (asc)</option>
            <option value="score_count_desc">Number of rates on each TV show (desc)</option>
        </select>
    `;
	$('#sortSelectContainer').html(climateRatedOptions);
}
