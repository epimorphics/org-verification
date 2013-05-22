// Set of initialization actions fun on page load to make UI features live

$(function() {

    // Set up ajax loading tabs
    $('.action-tab').bind('show', function(e) {
        var pattern=/#.+/gi
        var contentID = e.target.toString().match(pattern)[0];
        var action = $(contentID).attr('data-action');
        var uri = $(contentID).attr('data-uri');
        if (action) {
          //var url = '$uiroot/' + action +'?uri=$lib.pathEncode($uri)&requestor=$requestor';
          var url = '/ui/' + action + '?uri=' + uri;
          var args = $(contentID).attr('data-args');
          if (args) {
             url = url + "&" + args;
          }
          $(contentID).load(url, function(){
             $('.action-tab').tab(); //reinitialize tabs
             $('.datatable').dataTable();
             processQueryForms();
           });
        };
     });


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
        form. ajaxForm({
            success:
                function(data, status, xhr){
                    window.location.href = "/view?upload=" + data;
                },

            error:
              function(xhr, status, error){
                 $(".ajax-error").html("<div class='alert'> <button type='button' class='close' data-dismiss='alert'>&times;</button>Action failed: " + error + " - " + xhr.responseText + "</div>");
              }
          });
    });

});
