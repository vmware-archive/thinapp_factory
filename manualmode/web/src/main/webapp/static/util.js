jQuery.extend({
   // jQuery makes it easy to *receive* JSON via $.post but make it hard to send
   // it. Because, you know, who would ever want to do that.
   postJSON : function(url, data) {
      var send = data;

      if (data instanceof Object) {
         send = JSON.stringify(data);
      }

      var req = $.ajax({
         url : url,
         type : "POST",
         data : send,
         contentType : "application/json; charset=utf8",
         dataType : "json"
      });

      return req;
   }
});