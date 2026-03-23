var CONTEXT_PATH="/community";
var FORUM_CSRF_COOKIE="forum_csrf";
var FORUM_CSRF_HEADER="X-CSRF-Token";

function getCookieValue(name) {
	var nameEq = name + "=";
	var parts = document.cookie ? document.cookie.split(";") : [];
	for (var i = 0; i < parts.length; i++) {
		var part = parts[i].trim();
		if (part.indexOf(nameEq) === 0) {
			return decodeURIComponent(part.substring(nameEq.length));
		}
	}
	return "";
}

function isSafeMethod(method) {
	var normalized = (method || "GET").toUpperCase();
	return normalized === "GET" || normalized === "HEAD" || normalized === "OPTIONS" || normalized === "TRACE";
}

function isSameOriginUrl(url) {
	if (!url || url.indexOf("//") === -1) {
		return true;
	}
	try {
		return new URL(url, window.location.href).origin === window.location.origin;
	} catch (e) {
		return false;
	}
}

function attachCsrfHeader(xhr) {
	var token = getCookieValue(FORUM_CSRF_COOKIE);
	if (token) {
		xhr.setRequestHeader(FORUM_CSRF_HEADER, token);
	}
}

function communityLogout(options) {
	var redirect = options && options.redirect ? options.redirect : CONTEXT_PATH + "/login";
	$.ajax({
		url: CONTEXT_PATH + "/api/session/logout",
		method: "POST",
		xhrFields: { withCredentials: true }
	}).always(function() {
		window.location.href = redirect;
	});
	return false;
}

$(document).ajaxSend(function(event, xhr, settings) {
	if (!settings) {
		return;
	}
	var method = settings.type || settings.method || "GET";
	if (isSafeMethod(method) || !isSameOriginUrl(settings.url)) {
		return;
	}
	attachCsrfHeader(xhr);
});

window.alert = function(message) {
	if(!$(".alert-box").length) {
		$("body").append(
			'<div class="modal alert-box" tabindex="-1" role="dialog">'+
				'<div class="modal-dialog" role="document">'+
				'<div class="modal-content">'+
					'<div class="modal-header">'+
						'<h5 class="modal-title">鎻愮ず</h5>'+
						'<button type="button" class="close" data-dismiss="modal" aria-label="Close">'+
							'<span aria-hidden="true">&times;</span>'+
						'</button>'+
					'</div>'+
					'<div class="modal-body">'+
						'<p></p>'+
					'</div>'+
					'<div class="modal-footer">'+
						'<button type="button" class="btn btn-secondary" data-dismiss="modal">纭畾</button>'+
					'</div>'+
					'</div>'+
				'</div>'+
			'</div>'
		);
	}

    var h = $(".alert-box").height();
	var y = h / 2 - 100;
	if(h > 600) y -= 100;
    $(".alert-box .modal-dialog").css("margin", (y < 0 ? 0 : y) + "px auto");
	
	$(".alert-box .modal-body p").text(message);
	$(".alert-box").modal("show");
}
