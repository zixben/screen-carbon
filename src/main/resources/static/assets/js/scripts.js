(function ($) {
    "use strict";

    /*********************************
    /* Table of Context
    ¸ß¶ËÄ£°å£ºhttp://www.bootstrapmb.com
    /* *******************************
    /* 
    /* Preloader
    /* Sticky Navbar
    /* Scroll Top Bar
    /* Mobile Menu Flyout Menu
    /* Mobile Menu Expand
    /* Nice Select
    /* Movie Slider
    /* Thriller Slider
    /*  Gallery Slider
    /* Add/Minus Quantity
    /* CountDown 
    /*  Swiper Hero Slider 
    /* Testimonial Slider
    /* Map

    *********************************/

    /*********************************
    /* Preloader Start
    *********************************/
    $(window).on("load", function () {
        $("#status").fadeOut();
        $("#preloader").delay(500).fadeOut("slow");
        $("body").delay(500).css({ overflow: "visible" });
    });

    /*********************************
    /* Sticky Navbar
    *********************************/
    $(window).scroll(function () {
        var scrolling = $(this).scrollTop();
        var stikey = $(".header-sticky");

        if (scrolling >= 50) {
            $(stikey).addClass("nav-bg");
        } else {
            $(stikey).removeClass("nav-bg");
        }
    });

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

    /*********************************
    /*  Mobile Menu Flyout Menu
    *********************************/
    $(".toggler__btn").on("click", function (event) {
        event.preventDefault();
        $(".flyoutMenu").toggleClass("active");
    });
    $(".closest__btn").on("click", function (event) {
        event.preventDefault();
        $(".flyoutMenu").toggleClass("active");
    });

    $(document).on("click", function (e) {
        if ($(e.target).closest(".flyout__inner").length === 0 && $(e.target).closest(".toggler__btn").length === 0) {
            $(".flyoutMenu").removeClass("active");
        }
    });

    /*********************************
    /*  Mobile Menu Expand
    *********************************/
    $(".flyout-main__menu .nav__link").on("click", function (event) {
        event.preventDefault();
        // $(".has__dropdown").find(".sub__menu").slideUp();
        $(this).parent(".has__dropdown").find(".sub__menu").slideToggle();
    });

    $(".flyout-main__menu .sub__menu .nav__link").on("click", function (event) {
        event.preventDefault();
        $(this).parent(".has__dropdown").find(".sub__sub-menu").slideToggle();
    });

    /*********************************
    /*  Nice Select 2
    *********************************/
    $(".blog-select").select2({
        tags: true,
    });

    /*********************************
    /*  Movies Slider
    *********************************/
    if ($(".movies__Swiper").length > 0) {
        var moviesSwiper = new Swiper(".movies__Swiper", {
            // slidesPerView: 8,
            loop: false,
            spaceBetween: 30,
            pagination: {
                el: ".swiper-pagination",
                type: "progressbar",
            },
            navigation: {
                nextEl: ".swiper-button-next",
                prevEl: ".swiper-button-prev",
            },
            breakpoints: {
                300: {
                    slidesPerView: 1,
                },
                375: {
                    slidesPerView: 2,
                    spaceBetween: 20,
                },

                767: {
                    slidesPerView: 3,
                },
                991: {
                    slidesPerView: 4,
                },
                1199: {
                    slidesPerView: 4,
                },
                1400: {
                    slidesPerView: 4,
                },
            },
        });
    }

    /*********************************
    /*  Thriller Slider
    *********************************/
    if ($(".thriller__Swiper").length > 0) {
        var thrillerSwiper = new Swiper(".thriller__Swiper", {
            // slidesPerView: 8,
            loop: false,
            spaceBetween: 30,
            pagination: {
                el: ".swiper-dots",
                // type: "progressbar",
            },
            navigation: {
                nextEl: ".swiper-button-next",
                prevEl: ".swiper-button-prev",
            },
            breakpoints: {
                300: {
                    slidesPerView: 1,
                },
                375: {
                    slidesPerView: 2,
                    spaceBetween: 20,
                },

                767: {
                    slidesPerView: 3,
                },
                991: {
                    slidesPerView: 3,
                },
                1199: {
                    slidesPerView: 3,
                },
                1400: {
                    slidesPerView: 3,
                },
            },
        });
    }

    /*********************************
    /*  Gallery Slider
    *********************************/
    if ($(".gallery__Swiper").length > 0) {
        var thrillerSwiper = new Swiper(".gallery__Swiper", {
            // slidesPerView: 8,
            grabCursor: true,
            loop: true,
            spaceBetween: 30,
            centeredSlides: true,
            navigation: {
                nextEl: ".swiper-button-next",
                prevEl: ".swiper-button-prev",
            },
            breakpoints: {
                300: {
                    slidesPerView: 1,
                },
                375: {
                    slidesPerView: 1,
                },

                575: {
                    slidesPerView: 2,
                },
                991: {
                    slidesPerView: 2,
                },
            },
        });
    }

    /*********************************
    /* Add/Minus Quantity
    *********************************/
    $(".incressQnt").on("click", function () {
        var $qty = $(this).closest("div").find(".qnttinput");
        var currentVal = parseInt($qty.val());
        if (!isNaN(currentVal)) {
            $qty.val(currentVal + 1);
        }
    });
    $(".decressQnt").on("click", function () {
        var $qty = $(this).closest("div").find(".qnttinput");
        var currentVal = parseInt($qty.val());
        if (!isNaN(currentVal) && currentVal > 1) {
            $qty.val(currentVal - 1);
        } else {
            $(this).parents(".cart__action__btn").find(".cart__quantity").css("display", "none");
        }
    });

    /*********************************
    /* CountDown 
    *********************************/
    if ($("#salesCountdown").length > 0) {
        var austDay = new Date();
        austDay = new Date(austDay.getFullYear() + 1, 1 - 1, 1);
        $("#salesCountdown").countdown({
            until: austDay,
            padZeroes: true,
            labels: ["Year", "Mo", "Wk", "Days", "Hours", "Mins", "Sec"],
        });
    }

    /*********************************
    /* Swiper Hero Slider 
    *********************************/
    var swiper = new Swiper(".banner__Swiper", {
        loop: false,
        // effect: "cards",
        grabCursor: true,
        centeredSlides: true,
        pagination: {
            el: ".swiper__dots",
            // type: "progressbar",
        },
    });

    /*********************************
    /*  Customer Slide Start
    *********************************/
    /*********************************
    /*  Testimonial Section Start
    *********************************/
    if ($(".testimonial__Swiper").length > 0) {
        var testimonialSwiper = new Swiper(".testimonial__Swiper", {
            direction: "horizontal",
            loop: "false",
            grabCursor: true,
            slidesPerView: 1,
            spaceBetween: 30,
            speed: 2000,
            pagination: {
                el: ".swiper-pagination",
                // type: "fraction",
                type: "progressbar",
            },
            navigation: {
                nextEl: ".swiper-next",
                prevEl: ".swiper-prev",
            },

            // centerInsufficientSlides: true,
        });
    }

    /*********************************
    /* Google map
    *********************************/

    function init() {
        var map = new google.maps.Map(document.getElementById("map"), {
            zoom: 10,
            center: new google.maps.LatLng(23.810331, 90.412521),
            mapTypeId: google.maps.MapTypeId.ROADMAP,

            styles: [
                { featureType: "water", elementType: "geometry", stylers: [{ color: "#e9e9e9" }, { lightness: 17 }] },
                { featureType: "landscape", elementType: "geometry", stylers: [{ color: "#f5f5f5" }, { lightness: 20 }] },
                { featureType: "road.highway", elementType: "geometry.fill", stylers: [{ color: "#ffffff" }, { lightness: 17 }] },
                { featureType: "road.highway", elementType: "geometry.stroke", stylers: [{ color: "#ffffff" }, { lightness: 29 }, { weight: 0.2 }] },
                { featureType: "road.arterial", elementType: "geometry", stylers: [{ color: "#ffffff" }, { lightness: 18 }] },
                { featureType: "road.local", elementType: "geometry", stylers: [{ color: "#ffffff" }, { lightness: 16 }] },
                { featureType: "poi", elementType: "geometry", stylers: [{ color: "#f5f5f5" }, { lightness: 21 }] },
                { featureType: "poi.park", elementType: "geometry", stylers: [{ color: "#dedede" }, { lightness: 21 }] },
                { elementType: "labels.text.stroke", stylers: [{ visibility: "on" }, { color: "#ffffff" }, { lightness: 16 }] },
                { elementType: "labels.text.fill", stylers: [{ saturation: 36 }, { color: "#333333" }, { lightness: 40 }] },
                { elementType: "labels.icon", stylers: [{ visibility: "off" }] },
                { featureType: "transit", elementType: "geometry", stylers: [{ color: "#f2f2f2" }, { lightness: 19 }] },
                { featureType: "administrative", elementType: "geometry.fill", stylers: [{ color: "#fefefe" }, { lightness: 20 }] },
                { featureType: "administrative", elementType: "geometry.stroke", stylers: [{ color: "#fefefe" }, { lightness: 17 }, { weight: 1.2 }] },
            ],
        });

        var infowindow = new google.maps.InfoWindow();
    }

    if ($("#map").length > 0) {
        google.maps.event.addDomListener(window, "load", init);
    }

    /*********************************
    /* Google map
    *********************************/
})(jQuery);
