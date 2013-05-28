// Set of initialization actions fun on page load to make UI features live

$(function() {


    // Anything marked as popinfo will have a popover (data-trigger, data-placement, data-content)
    // enable popups
    $(".popinfo").popover();

    // General ajax forms which reload the page when actioned. Errors are displayed in elts of class "ajax-error".
    $(".ajax-form").each(function(){
        var form = $(this);
        var returnURL = form.attr('data-return');
        form. ajaxForm({
            success:
                function(data, status, xhr){
                    if (returnURL) {
                        window.location.href = returnURL;
                    } else {
                      location.reload();
                    }
                },

            error:
              function(xhr, status, error){
                 $(".ajax-error").html("<div class='alert'> <button type='button' class='close' data-dismiss='alert'>&times;</button>Action failed: " + error + " - " + xhr.responseText + "</div>");
              }
          });
    });

    $(".upload-form").each(function(){
        var form = $(this);
        var view = form.attr('data-view');
        form. ajaxForm({
            success:
                function(data, status, xhr){
                    window.location.href = view + "?upload=" + data;
                },

            error:
              function(xhr, status, error){
                 $(".ajax-error").html("<div class='alert'> <button type='button' class='close' data-dismiss='alert'>&times;</button>Action failed: " + error + " - " + xhr.responseText + "</div>");
              }
          });
    });

    // Action on reporting
    $(".report").click(function(){
//        $.post("request/save?upload=" + $(this).attr('data-upload'));
        var href = $(this).attr('href');
        $.ajax("request/save?upload=" + $(this).attr('data-upload'), {
            type: "POST",
            data: "no data",
            success: function(data, status, xhr){
                window.location.href = href;
                }
        });
        return false;
    });

    // Run QB integrity checks
    $("#qb-result-table").each(function(){
        var body = $(this).find("tbody");
        var upload = $(this).attr('data-upload');
        var checks = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19a', '19b', '20', '21'];
        var next = function() {
            if (checks.length > 0) {
                $.getJSON("request/validate?upload=" + upload + "&check=" + checks.shift(), function(datalist) {
                    for (var i = 0; i < datalist.length; i++) {
                        var data = datalist[i];
                        body.append("<tr><td><a href='" + data.url + "'>" + data.ic + "</a></td><td>" + data.description + "</td><td><img src='"
                            + (data.passed ? "img/tick-16.png" : "img/fail-16.png")
                            + "' alt='" + + (data.passed ? "passed" : "failed")
                            + "' /></td></tr>");
                        if (!data.passed) {
                            var reportLink = $("#qb-result-report a");
                            reportLink.attr("href", reportLink.attr("href").replace(/-->/, "Failed " + data.ic + "%0D%0A%0D%0A-->"));
                        }
                    }
                    next();
                });
            } else {
                $("#qb-result-report").show();
            }
        };
        next();
    });

});
