com_company_itpearls_extension_GalleryConnector = function () {
    var connector =  this;
    var element = connector.getElement();

    element.innerHTML = '<div id="gallery-connector"></div>';

    console.log('TAAG init');

    function addImage(uri) {
        var img = document.createElement('ico');
        var url = connector.translateVaadinUri(uri);
        img.src = url;

        console.log('TAAG ' + url);

        img.style.width = "100px";
        img.style.height = "100px";
        element.append(img);
    }

    connector.onStateChange = function() {
        var state = connector.getState();

        if (!state.resources) {
            return;
        }
        // Whether images are attached to the connector
        if (Object.keys(state.resources).length) {
            for (var resource in state.resources) {
                addImage(state.resources[resource].uRL);
            }
        }
    }
}