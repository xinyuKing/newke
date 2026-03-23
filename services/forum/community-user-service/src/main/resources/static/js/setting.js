$(function() {
    $("#uploadForm").submit(upload);
    $("#updatePassword").submit(updatePassword);
});

function showAlert(msg) {
    alert(msg);

    setTimeout(function() {
        close();
    }, 3000);
}

function updatePassword() {
    var oldPassword = $("#old-password").val();
    var newPassword = $("#new-password").val();
    $.post(
        CONTEXT_PATH + "/user/setting/updatepassword",
        {"oldPassword": oldPassword, "newPassword": newPassword},
        function(data) {
            data = $.parseJSON(data);
            if (data.code == 0) {
                showAlert("密码修改成功，请重新登录");
                communityLogout({ redirect: CONTEXT_PATH + "/login" });
            } else {
                showAlert(data.msg);
            }
        }
    )
}

function upload() {
    // 上传入口从后端配置注入，避免脚本写死七牛地域域名。
    var uploadHost = $("#uploadHost").val() || "https://upload-cn-east-2.qiniup.com";

    $.ajax({
        url: uploadHost,
        method: "post",
        processData: false,
        contentType: false,
        data: new FormData($("#uploadForm")[0]),
        success: function(data) {
            if (data && data.code == 0) {
                $.post(
                    CONTEXT_PATH + "/user/header/url",
                    {"fileName": $("input[name='key']").val()},
                    function(data) {
                        data = $.parseJSON(data);
                        if (data.code == 0) {
                            window.location.reload();
                        } else {
                            alert(data.msg);
                        }
                    }
                )
            } else {
                alert("上传失败");
            }
        }
    });
    return false;
}

