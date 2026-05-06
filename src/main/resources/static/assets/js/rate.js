var queryString = decodeURIComponent(window.location.search);
var params = new URLSearchParams(queryString);
var id = params.get('id');
var type = params.get('type');
var imgPath = "";
var vType = "";

let questionGroup1 = document.querySelector(".questionGroup1");
let questionGroup2 = document.querySelector(".questionGroup2");
let questionGroup3 = document.querySelector(".questionGroup3");

questionGroup2.style.display = "none";
questionGroup3.style.display = "none";
let page = document.querySelector(".page")

let status = 1;

let selectedAnswers = [];
let releaseYear = null;
let genreIds = [];
let countryShortNames = [];
// CSRF token setup for AJAX requests
var csrfToken = $('input[name="_csrf"]').val();

page.addEventListener("click", () => {
	if (status == 1) {
		let answers = collectAnswerOptions(1, 5);

		if (answers.length != 5) {
			//alert("Please enter");
			alert("You have unselected options");
			return;
		}
		selectedAnswers = answers;
		questionGroup1.style.display = "none";
		questionGroup2.style.display = "block";
		questionGroup3.style.display = "none";
	} else if (status == 2) {
		let answers = collectAnswerOptions(6, 10);
		if (answers.length != 5) {
			//alert("Please enter");
			alert("You have unselected options");
			return;
		}

		selectedAnswers = selectedAnswers.slice(0, 5).concat(answers);
		questionGroup1.style.display = "none";
		questionGroup2.style.display = "none";
		questionGroup3.style.display = "block";
		page.innerText = "Finish"
	} else if (status == 3) {
		let answers = collectAnswerOptions(11, 15);

		if (answers.length != 5) {
			//alert("Please enter");
			alert("You have unselected options");
			return;
		}
		selectedAnswers = selectedAnswers.slice(0, 10).concat(answers);

		submitScore(type, selectedAnswers)
	}

	// Scroll to the "Question:" part
	document.getElementById("introduction-title").scrollIntoView({
		behavior: 'smooth'
	});

	status += 1;
})

function collectAnswerOptions(startQuestion, endQuestion) {
	let answers = [];
	for (let questionNumber = startQuestion; questionNumber <= endQuestion; questionNumber++) {
		let selected = document.querySelector(`input[name="question${questionNumber}"]:checked`);
		if (selected == null) {
			return [];
		}
		let match = selected.id.match(/Option([1-5])$/);
		if (match == null) {
			return [];
		}
		answers.push(Number(match[1]));
	}
	return answers;
}


// Determine which menu item should be active and add the 'active' class
if (type === 'movie') {
	document.getElementById('movies-menu').classList.add('active');
} else if (type === 'tv') {
	document.getElementById('tv-shows-menu').classList.add('active');
}


let url = "https://api.themoviedb.org/3/movie/" + id
if (type == 'tv') {
	url = "https://api.themoviedb.org/3/tv/" + id
}

$.ajax({
	url: url,
	method: "get",
	headers: {
		"Authorization": jwt,
		"accept": "application/json"
	},
	success: function(resp) {

		if (type == 'movie') {
			$("#title").html(resp.title);
		} else {
			$("#title").html(resp.name);
		}
		
		$("#overview").html(resp.overview);
		imgPath = imgServer + resp.poster_path;
		vType = type;

		if (type == 'movie') {
			releaseYear = Number(resp.release_date.substring(0, 4));
		} else {
			releaseYear = Number(resp.first_air_date.substring(0, 4));
		}
		let genres = resp.genres;

		// Extract the ids into an array
		genreIds = genres.map(function(genre) {
			return genre.id;
		});


		let countries = resp.production_countries;

		// Extract the ids into an array
		countryShortNames = countries.map(function(country) {
			return country.iso_3166_1;
		});

		$("#poster_path").html("<img src='" + imgServer + resp.poster_path + "' alt='img'>")
	}
})

$.ajax({
	url: server + "/score/getScoreAvg/" + id + "/" + type,
	method: "get",
	headers: {
		"accept": "application/json"
	},
	success: function(response) {
		response.forEach((avgScore) => {

			let vote_average = avgScore;
			if (vote_average !== null) {


				// Determine border color based on rating
				borderColor = determineBorderColor(vote_average);
				iconPath = determineIconPath(vote_average);

				$("#poster_path").css("border-color", borderColor);
				$("#icon-path").html("<img style=\"width: 45px; height: 45px;\" src='" + server + iconPath + "' alt='img'>")

				let avgScorePercentage = (avgScore * 10).toString();

				$("#currentRatingPerc").html(Math.round(avgScorePercentage) + "%");
			} else {
				$("#poster_path").css("border-color", "none");
				$(".rating-layout").html("<h1>No rated yet.</h1>");
			}
		});
	},
	error: function(xhr, status, error) {
		console.error("An error occurred: " + status + ", " + error + ", " + xhr);
	}
});

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

function submitScore(vType, answers) {
	let title = $("#title").text();
	let obj = JSON.stringify({
		vId: Number(id),
		vImg: imgPath,
		videoType: vType,
		videoName: title,
		releaseYear: releaseYear,
		genres: genreIds,
		countries: countryShortNames,
		answers: answers
	});


	$.ajax({
		url: server + "/score/add",
		method: "POST",
		data: obj,
		headers: {
			"accept": "application/json",
			"Content-Type": "application/json",
			'X-CSRF-TOKEN': csrfToken
		},
		success: (resp) => {
			window.location.href = server + "/finish-rating?id=" + id + "&score=" + resp.score + "&type=" + type;
		},
		error: function(error) {
			console.error("Error inserting data", error);
			let message = error.responseJSON && error.responseJSON.message ? error.responseJSON.message : "Unable to submit rating.";
			alert(message);
		}
	});
}
