window.sessionStorage.clear();
window.sessionStorage.setItem("Sort", "popularity.desc");
window.sessionStorage.setItem("tvNumPage", 1);
window.sessionStorage.setItem("Year", '');
getMovies();

$(".form-select").on("change", () => {
	let element = window.event.target;
	let index = element.selectedIndex;
	let value = element.options[index].value;
	let key = element.options[0].text;
	window.sessionStorage.setItem(key, value);
	window.sessionStorage.setItem("tvNumPage", 1);
	getMovies();
})

function prePage() {
	let num = Number(window.sessionStorage.getItem("tvNumPage"));
	window.sessionStorage.setItem("tvNumPage", num - 1);
	getMovies();
}

function nextPage() {
	let num = Number(window.sessionStorage.getItem("tvNumPage"));
	window.sessionStorage.setItem("tvNumPage", num + 1);
	getMovies();
}

function getMovies() {
	let sort = sessionStorage.getItem("Sort");
	if (sort == null) sort = "";
	let Genre = sessionStorage.getItem("Genre");
	if (Genre == null) Genre = "";
	let Country = sessionStorage.getItem("Country");
	if (Country == null) Country = "";

	let Year = sessionStorage.getItem("Year");
	if (Year == null) Year = "";

	let num = Number(window.sessionStorage.getItem("tvNumPage"));
	if (num < 1) {
		return;
	}
	$("#pageNum").html(num)


	var climateMovies = [];
	// AJAX call for the Highest Ranked Climate-friendly Films & TV Shows
	$.ajax({
		url: server + "/score/getOrderAvg",
		method: "get",
		headers: {
			"Authorization": jwt,
			"accept": "application/json"
		},
		success: function(response) {
			climateMovies = response;
		},
		error: function(xhr, status, error) {
			console.error("An error occurred: " + status + ", " + error + ", " + xhr);
		}
	});

	// "&Country="+Country+
	$.ajax({
		url: "https://api.themoviedb.org/3/discover/tv?include_adult=false&include_video=false&language=en-US&page=" + num + "&sort_by=" + sort + "&with_genres=" + Genre + "&with_origin_country=" + Country + "&first_air_date_year=" + Year,
		cache: false,
		method: "get",
		headers: {
			"Authorization": jwt,
			"accept": "application/json"
		},
		success: function(resp) {

			let html = ""

			for (const respElement of resp.results) {
				let title = respElement.title || respElement.name;

				if (climateMovies.some(m => (m.vId === respElement.id && m.videoName === title))) {

					climateMovies.forEach((item) => {
						if (item.vId === respElement.id && item.videoName === title) {
							vote_average = item.score;
							voteAveragePercentage = (vote_average * 10).toString().substring(0, 5);
						}
					})
					borderColor = determineBorderColor(vote_average);
					iconPath = determineIconPath(vote_average);


					html += "   <div onclick = 'toDesc(" + respElement.id + ")' class=\"videoCar\">\n" +
						"                <div class=\"VideoImage\" style='border-color: " + borderColor + ";'>\n" +
						"                    <img alt='no image' src='" + imgServer + respElement.poster_path + "'>\n" +
						"                </div>\n" +
						"                <div><p><span><img class=\"card-icon\" src='" + iconPath + "'></span>\n" +
						"                    <span>" + voteAveragePercentage + "%</span>\n" +
						"                </p>\n" +
						"                <h5>" + title + "</h5></div>\n" +
						"            </div>"

				} else {

					html += "   <div onclick = 'toDesc(" + respElement.id + ")' class=\"videoCar\">\n" +
						"                <div class=\"VideoImage\" style='border: none;'>\n" +
						"                    <img alt='no image' src='" + imgServer + respElement.poster_path + "'>\n" +
						"                </div>\n" +
						"                <div><p>Not yet rated" +
						"                </p>\n" +
						"                <h5>" + respElement.name + "</h5></div>\n" +
						"            </div>"
				}
			}
			$(".videoStyle").html(html)

		}
	})
}

function toDesc(id) {
	window.location.href = server + "/tv?id=" + id + "&type=tv";
}

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