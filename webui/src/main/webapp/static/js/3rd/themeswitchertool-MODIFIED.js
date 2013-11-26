/*
 * -----------------------------------------------------------------------------
 * VMware ThinApp Factory
 * (c) 2011 VMware, Inc. All rights reserved.
 * -----------------------------------------------------------------------------
 */

/* jQuery plugin themeswitcher
 ---------------------------------------------------------------------*/
$.fn.themeswitcher = function(settings){
   var options = jQuery.extend({
      loadTheme: null,
      initialText: 'Switch Theme',
      width: 150,
      height: 600,
      buttonPreText: 'Theme: ',
      closeOnSelect: false,
      buttonHeight: 14,
      cookieName: 'jquery-ui-theme',
      onOpen: function(){},
      onClose: function(){},
      onSelect: function(){}
   }, settings);

   //markup
   var button = $('<a href="#" class="jquery-ui-themeswitcher-trigger"><span class="jquery-ui-themeswitcher-icon"></span><span class="jquery-ui-themeswitcher-title">'+ options.initialText +'</span></a>');
   var switcherpane = $('<div class="jquery-ui-themeswitcher"><div id="themeGallery"><ul>' +
      '<li><a href="'+window.VmTAF.contextPath+'/static/css/skins/jquery/ui-lightness/jquery-ui-1.8.16.custom.css"><img src="'+window.VmTAF.contextPath+'/static/css/skins/jquery/index/theme_90_ui_light.png" alt="UI Lightness" title="UI Lightness" /><span class="themeName">UI lightness</span></a></li>'+
      '<li><a href="'+window.VmTAF.contextPath+'/static/css/skins/jquery/ui-darkness/jquery-ui-1.8.16.custom.css"><img src="'+window.VmTAF.contextPath+'/static/css/skins/jquery/index/theme_90_ui_dark.png" alt="UI Darkness" title="UI Darkness" /><span class="themeName">UI darkness</span></a></li>' +
      '<li><a href="'+window.VmTAF.contextPath+'/static/css/skins/jquery/smoothness/jquery-ui-1.8.16.custom.css"><img src="'+window.VmTAF.contextPath+'/static/css/skins/jquery/index/theme_90_smoothness.png" alt="Smoothness" title="Smoothness" /><span class="themeName">Smoothness</span></a></li>' +
      '<li><a href="'+window.VmTAF.contextPath+'/static/css/skins/jquery/start/jquery-ui-1.8.16.custom.css"><img src="'+window.VmTAF.contextPath+'/static/css/skins/jquery/index/theme_90_start_menu.png" alt="Start" title="Start" /><span class="themeName">Start</span></a></li>'+
      '<li><a href="'+window.VmTAF.contextPath+'/static/css/skins/jquery/redmond/jquery-ui-1.8.16.custom.css"><img src="'+window.VmTAF.contextPath+'/static/css/skins/jquery/index/theme_90_windoze.png" alt="Redmond" title="Redmond" /><span class="themeName">Redmond</span></a></li>' +
      '<li><a href="'+window.VmTAF.contextPath+'/static/css/skins/jquery/sunny/jquery-ui-1.8.16.custom.css"><img src="'+window.VmTAF.contextPath+'/static/css/skins/jquery/index/theme_90_sunny.png" alt="Sunny" title="Sunny" /><span class="themeName">Sunny</span></a></li>' +
      '<li><a href="'+window.VmTAF.contextPath+'/static/css/skins/jquery/overcast/jquery-ui-1.8.16.custom.css"><img src="'+window.VmTAF.contextPath+'/static/css/skins/jquery/index/theme_90_overcast.png" alt="Overcast" title="Overcast" /><span class="themeName">Overcast</span></a></li>' +
      '<li><a href="'+window.VmTAF.contextPath+'/static/css/skins/jquery/flick/jquery-ui-1.8.16.custom.css"><img src="'+window.VmTAF.contextPath+'/static/css/skins/jquery/index/theme_90_flick.png" alt="Flick" title="Flick" /><span class="themeName">Flick</span></a></li>'+
      '<li><a href="'+window.VmTAF.contextPath+'/static/css/skins/jquery/pepper-grinder/jquery-ui-1.8.16.custom.css"><img src="'+window.VmTAF.contextPath+'/static/css/skins/jquery/index/theme_90_pepper_grinder.png" alt="Pepper Grinder" title="Pepper Grinder" /><span class="themeName">Pepper Grinder</span></a></li>' +
      '<li><a href="'+window.VmTAF.contextPath+'/static/css/skins/jquery/eggplant/jquery-ui-1.8.16.custom.css"><img src="'+window.VmTAF.contextPath+'/static/css/skins/jquery/index/theme_90_eggplant.png" alt="Eggplant" title="Eggplant" /><span class="themeName">Eggplant</span></a></li>' +
      '<li><a href="'+window.VmTAF.contextPath+'/static/css/skins/jquery/cupertino/jquery-ui-1.8.16.custom.css"><img src="'+window.VmTAF.contextPath+'/static/css/skins/jquery/index/theme_90_cupertino.png" alt="Cupertino" title="Cupertino" /><span class="themeName">Cupertino</span></a></li>'+
      '<li><a href="'+window.VmTAF.contextPath+'/static/css/skins/jquery/excite-bike/jquery-ui-1.8.16.custom.css"><img src="'+window.VmTAF.contextPath+'/static/css/skins/jquery/index/theme_90_excite_bike.png" alt="Excite Bike" title="Excite Bike" /><span class="themeName">Excite Bike</span></a></li>'
   );

   //button events
   button.click(
      function(){
         if(switcherpane.is(':visible')){ switcherpane.spHide(); }
         else{ switcherpane.spShow(); }
         return false;
      }
   );

   //menu events (mouseout didn't work...)
   switcherpane.hover(
      function(){},
      function(){if(switcherpane.is(':visible')){$(this).spHide();}}
   );

   //show/hide panel functions
   $.fn.spShow = function(){ $(this).css({top: button.offset().top + options.buttonHeight + 6, left: button.offset().left}).slideDown(50); button.css(button_active); options.onOpen(); }
   $.fn.spHide = function(){ $(this).slideUp(50, function(){options.onClose();}); button.css(button_default); }


   /* Theme Loading
    ---------------------------------------------------------------------*/
   switcherpane.find('a').click(function(){
      updateCSS( $(this).attr('href') );
      var themeName = $(this).find('span').text();
      button.find('.jquery-ui-themeswitcher-title').text( options.buttonPreText + themeName );
      $.cookie(options.cookieName, themeName, {path:'/'});
      options.onSelect();
      if(options.closeOnSelect && switcherpane.is(':visible')){ switcherpane.spHide(); }
      return false;
   });

   //function to append a new theme stylesheet with the new style changes
   function updateCSS(locStr){
      var cssLink = $('<link href="'+locStr+'" type="text/css" rel="Stylesheet" class="ui-theme" />');
      $("head").append(cssLink);

      if( $("link.ui-theme").size() > 3){
         $("link.ui-theme:first").remove();
      }
   }

   /* Inline CSS
    ---------------------------------------------------------------------*/
   var button_default = {
      fontFamily: 'Trebuchet MS, Verdana, sans-serif',
      fontSize: '11px',
      color: '#666',
      background: '#eee url(' + window.VmTAF.contextPath + '/static/images/switchertool/buttonbg.png) 50% 50% repeat-x',
      border: '1px solid #ccc',
      '-moz-border-radius': '6px',
      '-webkit-border-radius': '6px',
      textDecoration: 'none',
      padding: '3px 3px 3px 8px',
      width: options.width - 11,//minus must match left and right padding
      display: 'block',
      height: options.buttonHeight,
      outline: '0'
   };
   var button_hover = {
      'borderColor':'#bbb',
      'background': '#f0f0f0',
      cursor: 'pointer',
      color: '#444'
   };
   var button_active = {
      color: '#aaa',
      background: '#000',
      border: '1px solid #ccc',
      borderBottom: 0,
      '-moz-border-radius-bottomleft': 0,
      '-webkit-border-bottom-left-radius': 0,
      '-moz-border-radius-bottomright': 0,
      '-webkit-border-bottom-right-radius': 0,
      outline: '0'
   };



   //button css
   button.css(button_default)
      .hover(
      function(){
         $(this).css(button_hover);
      },
      function(){
         if( !switcherpane.is(':animated') && switcherpane.is(':hidden') ){$(this).css(button_default);  }
      }
   )
      .find('.jquery-ui-themeswitcher-icon').css({
         "float": 'right',
         width: '16px',
         height: '16px',
         background: 'url(' + window.VmTAF.contextPath + '/static/images/switchertool/icon_color_arrow.gif) 50% 50% no-repeat'
      });
   //pane css
   switcherpane.css({
      position: 'absolute',
      "float": 'left',
      fontFamily: 'Trebuchet MS, Verdana, sans-serif',
      fontSize: '12px',
      background: '#000',
      color: '#fff',
      padding: '8px 3px 3px',
      border: '1px solid #ccc',
      '-moz-border-radius-bottomleft': '6px',
      '-webkit-border-bottom-left-radius': '6px',
      '-moz-border-radius-bottomright': '6px',
      '-webkit-border-bottom-right-radius': '6px',
      borderTop: 0,
      zIndex: 999999,
      width: options.width-6//minus must match left and right padding
   })
      .find('ul').css({
         listStyle: 'none',
         margin: '0',
         padding: '0',
         overflow: 'auto',
         height: options.height
      }).end()
      .find('li').hover(
      function(){
         $(this).css({
            'borderColor':'#555',
            'background': 'url(' + window.VmTAF.contextPath + '/static/images/switchertool/menuhoverbg.png) 50% 50% repeat-x',
            cursor: 'pointer'
         });
      },
      function(){
         $(this).css({
            'borderColor':'#111',
            'background': '#000',
            cursor: 'auto'
         });
      }
   ).css({
         width: options.width-30,
         height: '',
         padding: '2px',
         margin: '1px',
         border: '1px solid #111',
         '-moz-border-radius': '4px',
         clear: 'left',
         "float": 'left'
      }).end()
      .find('a').css({
         color: '#aaa',
         textDecoration: 'none',
         "float": 'left',
         width: '100%',
         outline: '0'
      }).end()
      .find('img').css({
         "float": 'left',
         border: '1px solid #333',
         margin: '0 2px'
      }).end()
      .find('.themeName').css({
         "float": 'left',
         margin: '3px 0'
      }).end();



   $(this).append(button);
   $('body').append(switcherpane);
   switcherpane.hide();
   if( $.cookie(options.cookieName) || options.loadTheme ){
      var themeName = $.cookie(options.cookieName) || options.loadTheme;
      switcherpane.find('a:contains('+ themeName +')').trigger('click');
   }

   return this;
};




/**
 * Cookie plugin
 *
 * Copyright (c) 2006 Klaus Hartl (stilbuero.de)
 * Dual licensed under the MIT and GPL licenses:
 * http://www.opensource.org/licenses/mit-license.php
 * http://www.gnu.org/licenses/gpl.html
 *
 */
jQuery.cookie = function(name, value, options) {
   if (typeof value != 'undefined') { // name and value given, set cookie
      options = options || {};
      if (value === null) {
         value = '';
         options.expires = -1;
      }
      var expires = '';
      if (options.expires && (typeof options.expires == 'number' || options.expires.toUTCString)) {
         var date;
         if (typeof options.expires == 'number') {
            date = new Date();
            date.setTime(date.getTime() + (options.expires * 24 * 60 * 60 * 1000));
         } else {
            date = options.expires;
         }
         expires = '; expires=' + date.toUTCString(); // use expires attribute, max-age is not supported by IE
      }
      // CAUTION: Needed to parenthesize options.path and options.domain
      // in the following expressions, otherwise they evaluate to undefined
      // in the packed version for some reason...
      var path = options.path ? '; path=' + (options.path) : '';
      var domain = options.domain ? '; domain=' + (options.domain) : '';
      var secure = options.secure ? '; secure' : '';
      document.cookie = [name, '=', encodeURIComponent(value), expires, path, domain, secure].join('');
   } else { // only name given, get cookie
      var cookieValue = null;
      if (document.cookie && document.cookie != '') {
         var cookies = document.cookie.split(';');
         for (var i = 0; i < cookies.length; i++) {
            var cookie = jQuery.trim(cookies[i]);
            // Does this cookie string begin with the name we want?
            if (cookie.substring(0, name.length + 1) == (name + '=')) {
               cookieValue = decodeURIComponent(cookie.substring(name.length + 1));
               break;
            }
         }
      }
      return cookieValue;
   }
};
