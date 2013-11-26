// File taken from bora/apps/vmplayer/npplugin/test/test.js@1321728

//
// constants:
//

var MKS_ID = "mks";
var PARENT_ID = "pluginPanel";



//
// globals:
//

var isIE = /MSIE (\d+\.\d+);/.test(navigator.userAgent);
var mks = null;

// alert control vars
var showOnConnectionStateChangeAlerts = true;
var showOnSizeChangeAlerts = true;
var showOnWindowStateChangeAlerts = true;
var showOnGrabStateChangeAlerts = true;
var showOnMessageAlerts = true;
var showOnDeviceConnectionStateChangeAlerts = true;
var showOnPhysicalClientDevicesChangeAlerts = true;

function createAxEventHandler(strEvent, strBody) {
    var elt = document.createElement("SCRIPT");
    elt.setAttribute("type", "text/javascript");
    elt.setAttribute("for", MKS_ID);
    elt.setAttribute("event", strEvent);
    elt.text = strBody;

    var head = document.getElementsByTagName("head")[0];
    head.appendChild(elt);

    return elt;
}

function vmrcInit() {
    mks = $("#vmrc")[0];
    if (isIE) {
        ieInit();
    } else {
        ffInit();
    }
}


// event handlers
function onConnectionStateChangeHandler(cs, host, vmId, userRequested, reason) {
    if (showOnConnectionStateChangeAlerts) {
        alert('onConnectionStateChange - connectionState: ' + cs +
              ' , host: ' + host +
              ' , vmId: ' + vmId +
              ' , userRequested: ' + userRequested +
              ' , reason: ' + reason);
    }
}
function onSizeChangeHandler(width, height) {
    if (showOnSizeChangeAlerts) {
        alert('onSizeChange - width: ' + width +
              ' ,height: ' + height);
    }
}
function onWindowStateChangeHandler(windowState) {
    if (showOnWindowStateChangeAlerts) {
        alert('onWindowStateChange - windowState: ' + windowState);
    }
}
function onGrabStateChangeHandler(grabState) {
    if (showOnGrabStateChangeAlerts) {
        alert('onGrabStateChange - grabState: ' + grabState);
    }
}
function onMessageHandler(msgType, message) {
    if (showOnMessageAlerts) {
        alert('onMessage - msgType: ' + msgType +
              ' ,message: ' + message);
    }
}
function onDeviceConnectionStateChangeHandler(deviceConnectionState,
                                              hostname,
                                              vmID,
                                              virtualDeviceKey,
                                              physicalClientDeviceKey,
                                              userRequested,
                                              reason) {
    if (showOnDeviceConnectionStateChangeAlerts) {
        alert('onDeviceConnectionStateChange - deviceConnectionState: ' + deviceConnectionState +
              ' ,hostname: ' + hostname +
              ' ,vmID: ' + vmID +
              ' ,virtualDeviceKey: ' + virtualDeviceKey +
              ' ,physicalClientDeviceKey: ' + physicalClientDeviceKey +
              ' ,userRequested: ' + userRequested +
              ' ,reason: ' + reason);
    }
}
function onPhysicalClientDevicesChangeHandler() {
    if (showOnPhysicalClientDevicesChangeAlerts) {
        alert('onPhysicalClientDevicesChange');
    }
}


// event enable/disable handlers
function enableOnSizeChange() {
    showOnSizeChangeAlerts = true;
}
function disableOnSizeChange() {
    showOnSizeChangeAlerts = false;
}
function enableOnConnectionStateChange() {
    showOnConnectionStateChangeAlerts = true;
}
function disableOnConnectionStateChange() {
    showOnConnectionStateChangeAlerts = false;
}
function enableOnWindowStateChange() {
    showOnWindowStateChangeAlerts = true;
}
function disableOnWindowStateChange() {
    showOnWindowStateChangeAlerts = false;
}
function enableOnGrabStateChange() {
    showOnGrabStateChangeAlerts = true;
}
function disableOnGrabStateChange() {
    showOnGrabStateChangeAlerts = false;
}
function enableOnMessage() {
    showOnMessageAlerts = true;
}
function disableOnMessage() {
    showOnMessageAlerts = false;
}
function enableOnDeviceConnectionStateChange() {
    showOnDeviceConnectionStateChangeAlerts = true;
}
function disableOnDeviceConnectionStateChange() {
    showOnDeviceConnectionStateChangeAlerts = false;
}
function enableOnPhysicalClientDevicesChange() {
    showOnPhysicalClientDevicesChangeAlerts = true;
}
function disableOnPhysicalClientDevicesChange() {
    showOnPhysicalClientDevicesChangeAlerts = false;
}


// initialization functions
function ieInit() {
    mks.attachEvent("onConnectionStateChange", function(cs,
                                                        host,
                                                        vmId,
                                                        userRequested,
                                                        reason) {
        onConnectionStateChangeHandler(cs, host, vmId, userRequested, reason);
    });

    mks.attachEvent("onSizeChange", function(width, height) {
        onSizeChangeHandler(width, height);
    });

    mks.attachEvent("onWindowStateChange", function(windowState) {
        onWindowStateChangeHandler(windowState);
    });

    mks.attachEvent("onGrabStateChange", function(grabState) {
        onGrabStateChangeHandler(grabState);
    });

    mks.attachEvent("onMessage", function(msgType, message) {
        onMessageHandler(msgType, message);
    });

    mks.attachEvent("onDeviceConnectionStateChange",
                    function(deviceConnectionState,
                             hostname,
                             vmID,
                             virtualDeviceKey,
                             physicalClientDeviceKey,
                             userRequested,
                             reason) {
        onDeviceConnectionStateChangeHandler(deviceConnectionState,
                      hostname,
                      vmID,
                      virtualDeviceKey,
                      physicalClientDeviceKey,
                      userRequested,
                      reason);
    });

    mks.attachEvent("onPhysicalClientDevicesChange", function() {
        onPhysicalClientDevicesChangeHandler();
    });
}
function ffInit() {
    mks.onConnectionStateChange = function(cs,
                                           host,
                                           vmId,
                                           userRequested,
                                           reason) {
        onConnectionStateChangeHandler(cs, host, vmId, userRequested, reason);
    };

    mks.onSizeChange = function(width, height) {
        onSizeChangeHandler(width, height);
    };

    mks.onWindowStateChange = function(windowState) {
        onWindowStateChangeHandler(windowState);
    };

    mks.onGrabStateChange = function(grabState) {
        onGrabStateChangeHandler(grabState);
    };

    mks.onMessage = function(msgType, message) {
        onMessageHandler(msgType, message);
    };

    mks.onDeviceConnectionStateChange = function(deviceConnectionState,
                                                 hostname,
                                                 vmID,
                                                 virtualDeviceKey,
                                                 physicalClientDeviceKey,
                                                 userRequested,
                                                 reason) {
        onDeviceConnectionStateChangeHandler(deviceConnectionState,
                      hostname,
                      vmID,
                      virtualDeviceKey,
                      physicalClientDeviceKey,
                      userRequested,
                      reason);
    };

    mks.onPhysicalClientDevicesChange = function() {
        onPhysicalClientDevicesChangeHandler();
    };
}


function startup() {
    var uiMode = parseInt($V('startup_uiMode'));
    var msgMode = parseInt($V('startup_msgMode'));
    var pers = $('startup_persistent').checked;
    var advancedConfig = $V('startup_advancedConfig');

    alert('startup returned "' + mks.startup(uiMode, msgMode, pers, advancedConfig) + '"');
}

function attach() {
    var id = $V('attach_id');

    alert(mks.attach(id));
}

function shutdown() {
    alert(mks.shutdown());
}

function isReadyToStart() {
    alert('isReadyToStart returned "' + mks.isReadyToStart() + '"');
}

function connect() {
    var host = $V('connect_host');
    var ticket = $V('connect_ticket');
    var user = $V('connect_user');
    var pwd = $V('connect_pwd');
    var moid = $V('connect_moid');
    var dc = $V('connect_dc');
    var vmPath = $V('connect_vmpath');

    alert('connect returned "' + mks.connect(host, ticket, user, pwd, moid, dc, vmPath) + '"');
}

function disconnect() {
    alert('disconnect returned "' + mks.disconnect() + '"');
}

function getConnectionState() {
    alert('getConnectionState returned "' + mks.getConnectionState() + '"');
}

function screenWidth() {
    alert('screenWidth returned "' + mks.screenWidth() + '"');
}

function screenHeight() {
    alert('screenHeight returned "' + mks.screenHeight() + '"');
}

function setFullscreen() {
    var fs = $('fs_value').checked;
    alert('setFullscreen returned "' + mks.setFullscreen(fs) + '"');
}

function exitFullscreen() {
    alert('exitFullscreen returned "' + mks.exitFullscreen() + '"');
}

function sendCAD() {
    alert('sendCAD returned "' + mks.sendCAD() + '"');
}

function enumDevices() {
    var mask = parseInt($V('enum_type'));
    if (isIE) {
        var devices = new VBArray(mks.getPhysicalClientDevices(mask)).toArray();
        alert('getPhysicalClientDevices returned "' + devices + '"');
    } else {
        alert('getPhysicalClientDevices returned "' + mks.getPhysicalClientDevices(mask) + '"');
    }
}

function getDeviceDetails() {
    var key = $V('device_details_key');
    var deviceDetails;
    if (isIE) {
        deviceDetails = new VBArray(mks.getPhysicalClientDeviceDetails(key)).toArray();
    } else {
        deviceDetails = mks.getPhysicalClientDeviceDetails(key);
    }
    alert('getPhysicalClientDeviceDetails returned "' + deviceDetails + '"');
}

function connectDevice() {
    var virtualKey = $V('connect_dev_key');
    var physicalKey = $V('connect_dev_path');
    var filebacking = $('connect_dev_filebacking').checked;

    alert('connectDevice returned "' + mks.connectDevice(virtualKey, filebacking, physicalKey) + '"');
}

function newConnectDevice() {
    var virtualKey = $V('connect_dev_key');
    var physicalKey = $V('connect_dev_path');
    var filebacking = $('connect_dev_filebacking').checked;

    alert('newConnectDevice returned "' + mks.newConnectDevice(virtualKey, physicalKey, filebacking ? 1 : 0) + '"');
}

function disconnectDevice() {
    var key = $V('disconnect_dev_key');

    alert('disconnectDevice returned "' + mks.disconnectDevice(key) + '"');
}

function initializeSSPI() {
    var pkgName = $V('sspi_package');
    alert('initializeSSPI returned "' + mks.initializeSSPI(pkgName) + '"');
}

function negotiateSSPI() {
    var token = $V('sspi_token');
    alert('negotiateSSPI returned "' + mks.negotiateSSPI(token) + '"');
}

function getVersion() {
    alert('getVersion returned "' + mks.getVersion() + '"');
}

function getSupportedApi() {
    var api;
    if (isIE) {
        api = new VBArray(mks.getSupportedApi()).toArray();
    } else {
        api = mks.getSupportedApi();
    }
    alert('getSupportedApi returned "' + api + '"');
}
